/*
 * DoricoPitchConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import com.github.toolarium.music.model.note.impl.PitchImpl;


/**
 * Converts Dorico pitches into MusicXML pitches.
 *
 * <p>Dorico stores the sounding pitch as a MIDI note number and the spelling as a letter name
 * only (property kNoteSpelling). Octave and alteration follow from these two values. Written
 * pitches of transposing instruments are derived by shifting the letter by the diatonic and the
 * MIDI pitch by the chromatic part of the instrument transposition.</p>
 *
 * @author patrick
 */
public final class DoricoPitchConverter {
    private static final String[] STEPS = {"C", "D", "E", "F", "G", "A", "B"};
    private static final int[] STEP_SEMITONES = {0, 2, 4, 5, 7, 9, 11};
    private static final String[] SHARP_SPELLING = {"C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B"};
    private static final String[] FLAT_SPELLING = {"C", "D", "D", "E", "E", "F", "G", "G", "A", "A", "B", "B"};
    private static final int MAX_ALTER = 2;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int STEPS_PER_OCTAVE = 7;
    private static final int FIFTHS_PER_SEMITONE = 7;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final DoricoPitchConverter INSTANCE = new DoricoPitchConverter();
    }


    /**
     * Constructor.
     */
    private DoricoPitchConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static DoricoPitchConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Converts a MIDI pitch and a letter spelling into a MusicXML pitch.
     *
     * @param midiPitch the MIDI note number (60 is middle C)
     * @param letter the spelled letter name (C to B), or null to use a default spelling
     * @param preferFlats true to prefer flats for the default spelling
     * @return the pitch
     */
    public PitchImpl toPitch(int midiPitch, String letter, boolean preferFlats) {
        int stepIndex = indexOfStep(letter);
        if (stepIndex < 0) {
            int pitchClass = Math.floorMod(midiPitch, SEMITONES_PER_OCTAVE);
            if (preferFlats) {
                stepIndex = indexOfStep(FLAT_SPELLING[pitchClass]);
            } else {
                stepIndex = indexOfStep(SHARP_SPELLING[pitchClass]);
            }
        }

        // choose the octave in which the letter lies closest to the MIDI pitch
        int octave = Math.floorDiv(midiPitch, SEMITONES_PER_OCTAVE) - 1;
        int alter = midiPitch - naturalMidiPitch(stepIndex, octave);
        if (alter > MAX_ALTER) {
            octave++;
            alter = midiPitch - naturalMidiPitch(stepIndex, octave);
        } else if (alter < -MAX_ALTER) {
            octave--;
            alter = midiPitch - naturalMidiPitch(stepIndex, octave);
        }

        PitchImpl pitch = new PitchImpl();
        pitch.setStep(STEPS[stepIndex]);
        pitch.setOctave(octave);
        if (alter != 0) {
            pitch.setAlter((double) alter);
        }
        return pitch;
    }


    /**
     * Shifts a letter name by a number of diatonic steps.
     *
     * @param letter the letter name (may be null)
     * @param diatonicSteps the number of steps (positive is upwards)
     * @return the shifted letter, or null if the letter is null or unknown
     */
    public String shiftLetter(String letter, int diatonicSteps) {
        int stepIndex = indexOfStep(letter);
        if (stepIndex < 0) {
            return null;
        }
        return STEPS[Math.floorMod(stepIndex + diatonicSteps, STEPS_PER_OCTAVE)];
    }


    /**
     * Calculates how far an interval moves a key signature on the circle of fifths.
     *
     * @param chromatic the interval in semitones
     * @param diatonic the interval in diatonic steps
     * @return the change of the key signature in fifths
     */
    public int intervalToFifths(int chromatic, int diatonic) {
        // a fifth is 7 semitones and 4 steps, an octave is 12 semitones and 7 steps
        return FIFTHS_PER_SEMITONE * chromatic - SEMITONES_PER_OCTAVE * diatonic;
    }


    /**
     * Gets the MIDI pitch of a natural note.
     *
     * @param stepIndex the step index (0 is C)
     * @param octave the octave (4 is the octave of middle C)
     * @return the MIDI pitch
     */
    private int naturalMidiPitch(int stepIndex, int octave) {
        return (octave + 1) * SEMITONES_PER_OCTAVE + STEP_SEMITONES[stepIndex];
    }


    /**
     * Gets the step index of a letter name.
     *
     * @param letter the letter name (may be null)
     * @return the step index (0 is C), or -1 if the letter is unknown
     */
    private int indexOfStep(String letter) {
        if (letter == null) {
            return -1;
        }
        for (int i = 0; i < STEPS.length; i++) {
            if (STEPS[i].equals(letter)) {
                return i;
            }
        }
        return -1;
    }
}
