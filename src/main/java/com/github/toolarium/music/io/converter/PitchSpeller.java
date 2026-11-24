/*
 * PitchSpeller.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.converter;

import com.github.toolarium.music.io.dto.SpelledPitch;

/**
 * Converts between MIDI note numbers and spelled pitches (step, alteration and octave).
 * MIDI note numbers are spelled in the context of a key signature.
 *
 * @author patrick
 */
public final class PitchSpeller {
    private static final String STEPS = "CDEFGAB";
    private static final int[] STEP_SEMITONES = {0, 2, 4, 5, 7, 9, 11};
    private static final String[] SHARP_STEPS = {"C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B"};
    private static final int[] SHARP_ALTERS = {0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0};
    private static final String[] FLAT_STEPS = {"C", "D", "D", "E", "E", "F", "G", "G", "A", "A", "B", "B"};
    private static final int[] FLAT_ALTERS = {0, -1, 0, -1, 0, 0, -1, 0, -1, 0, -1, 0};
    private static final String SHARP_ORDER = "FCGDAEB";
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int OCTAVE_OFFSET = 1;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final PitchSpeller INSTANCE = new PitchSpeller();
    }


    /**
     * Constructor.
     */
    private PitchSpeller() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static PitchSpeller getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Spells a MIDI note number in a key. Notes of the scale use the spelling of the key, other
     * notes use sharps in sharp keys (and C major) and flats in flat keys.
     *
     * @param midi the MIDI note number (60 = middle C)
     * @param fifths the key signature (negative for flats)
     * @return the spelled pitch
     */
    public SpelledPitch spell(int midi, int fifths) {
        int pitchClass = Math.floorMod(midi, SEMITONES_PER_OCTAVE);
        for (int s = 0; s < STEPS.length(); s++) {
            String step = String.valueOf(STEPS.charAt(s));
            int alter = keyAlter(step, fifths);
            if (Math.floorMod(STEP_SEMITONES[s] + alter, SEMITONES_PER_OCTAVE) == pitchClass) {
                return new SpelledPitch(step, alter, octaveOf(midi, step, alter));
            }
        }
        String step;
        int alter;
        if (fifths < 0) {
            step = FLAT_STEPS[pitchClass];
            alter = FLAT_ALTERS[pitchClass];
        } else {
            step = SHARP_STEPS[pitchClass];
            alter = SHARP_ALTERS[pitchClass];
        }
        return new SpelledPitch(step, alter, octaveOf(midi, step, alter));
    }


    /**
     * Gets the alteration of a step in a key signature.
     *
     * @param step the step (C to B)
     * @param fifths the key signature (negative for flats)
     * @return the alteration in semitones
     */
    public int keyAlter(String step, int fifths) {
        int index = SHARP_ORDER.indexOf(step);
        if (index < 0) {
            return 0;
        }
        int alter = 0;
        if (fifths > 0) {
            alter = (fifths - index + 6) / 7;
        } else if (fifths < 0) {
            alter = -((-fifths - (6 - index) + 6) / 7);
        }
        return alter;
    }


    /**
     * Calculates the MIDI note number of a spelled pitch.
     *
     * @param step the step (C to B)
     * @param alter the alteration in semitones
     * @param octave the octave (4 is the octave of middle C)
     * @return the MIDI note number
     */
    public int toMidi(String step, int alter, int octave) {
        int index = STEPS.indexOf(step);
        if (index < 0) {
            index = 0;
        }
        return (octave + OCTAVE_OFFSET) * SEMITONES_PER_OCTAVE + STEP_SEMITONES[index] + alter;
    }


    /**
     * Gets the diatonic index of a step (C = 0 to B = 6).
     *
     * @param step the step
     * @return the index, or -1 if the step is unknown
     */
    public int stepIndex(String step) {
        if (step == null || step.length() != 1) {
            return -1;
        }
        return STEPS.indexOf(step.toUpperCase());
    }


    /**
     * Gets the step of a diatonic index.
     *
     * @param index the index (any integer, taken modulo 7)
     * @return the step (C to B)
     */
    public String stepOf(int index) {
        return String.valueOf(STEPS.charAt(Math.floorMod(index, STEPS.length())));
    }


    /**
     * Gets the MusicXML accidental name of an alteration.
     *
     * @param alter the alteration in semitones
     * @return the accidental name, for example sharp
     */
    public String accidentalName(int alter) {
        switch (alter) {
            case -2:
                return "flat-flat";
            case -1:
                return "flat";
            case 1:
                return "sharp";
            case 2:
                return "double-sharp";
            default:
                return "natural";
        }
    }


    /**
     * Calculates the octave of a MIDI note number for a spelling.
     *
     * @param midi the MIDI note number
     * @param step the step
     * @param alter the alteration
     * @return the octave
     */
    private int octaveOf(int midi, String step, int alter) {
        int natural = midi - alter;
        int index = STEPS.indexOf(step);
        return Math.floorDiv(natural - STEP_SEMITONES[index], SEMITONES_PER_OCTAVE) - OCTAVE_OFFSET;
    }
}
