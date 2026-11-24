/*
 * MuseScorePitchConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;


/**
 * Converts between the MuseScore pitch representation and MusicXML style pitch spelling.
 *
 * <p>MuseScore stores a note as a MIDI pitch (always sounding / concert pitch) plus a
 * tonal pitch class (tpc) that defines the spelling. The tpc is the position on the line of fifths:
 * 14 is C, 15 is G, 13 is F, 12 is B flat and so on. Each step of +7 adds a sharp, each step of
 * -7 adds a flat. The valid range is -1 (F double flat) to 33 (B double sharp).</p>
 *
 * @author patrick
 */
public final class MuseScorePitchConverter {

    /** The tpc value of C natural. */
    public static final int TPC_C = 14;

    /** The lowest valid tpc value (F double flat). */
    public static final int TPC_MIN = -1;

    /** The highest valid tpc value (B double sharp). */
    public static final int TPC_MAX = 33;

    private static final String FIFTHS_ORDER = "FCGDAEB";
    private static final String DIATONIC_ORDER = "CDEFGAB";
    private static final int[] DIATONIC_SEMITONES = {0, 2, 4, 5, 7, 9, 11};
    private static final String[] SHARP_STEPS = {"C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B"};
    private static final int[] SHARP_ALTERS = {0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0};
    private static final int STEPS_PER_OCTAVE = 7;
    private static final int SEMITONES_PER_OCTAVE = 12;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MuseScorePitchConverter INSTANCE = new MuseScorePitchConverter();
    }


    /**
     * Constructor.
     */
    private MuseScorePitchConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MuseScorePitchConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Checks whether a tpc value is in the valid range.
     *
     * @param tpc the tonal pitch class
     * @return true if valid
     */
    public boolean isValidTpc(int tpc) {
        return tpc >= TPC_MIN && tpc <= TPC_MAX;
    }


    /**
     * Gets the note step (C, D, E, F, G, A, B) of a tonal pitch class.
     *
     * @param tpc the tonal pitch class
     * @return the step letter
     */
    public String tpcToStep(int tpc) {
        return String.valueOf(FIFTHS_ORDER.charAt(Math.floorMod(tpc + 1, STEPS_PER_OCTAVE)));
    }


    /**
     * Gets the chromatic alteration (-2 to +2) of a tonal pitch class.
     *
     * @param tpc the tonal pitch class
     * @return the alteration in semitones
     */
    public int tpcToAlter(int tpc) {
        return Math.floorDiv(tpc + 1, STEPS_PER_OCTAVE) - 2;
    }


    /**
     * Converts a step and an alteration into a tonal pitch class.
     *
     * @param step the step letter (C, D, E, F, G, A, B)
     * @param alter the alteration in semitones
     * @return the tonal pitch class, or {@link #TPC_C} if the step is unknown
     */
    public int stepAlterToTpc(String step, int alter) {
        int index = stepIndexInFifths(step);
        if (index < 0) {
            return TPC_C;
        }
        return index - 1 + (alter + 2) * STEPS_PER_OCTAVE;
    }


    /**
     * Gets the diatonic index (C=0 ... B=6) of a step letter.
     *
     * @param step the step letter
     * @return the diatonic index, or -1 if unknown
     */
    public int diatonicIndex(String step) {
        if (step == null || step.isEmpty()) {
            return -1;
        }
        return DIATONIC_ORDER.indexOf(Character.toUpperCase(step.charAt(0)));
    }


    /**
     * Gets the step letter of a diatonic index (C=0 ... B=6).
     *
     * @param index the diatonic index, values outside 0..6 are wrapped
     * @return the step letter
     */
    public String diatonicStep(int index) {
        return String.valueOf(DIATONIC_ORDER.charAt(Math.floorMod(index, STEPS_PER_OCTAVE)));
    }


    /**
     * Gets the semitone offset of a natural step within an octave (C=0, D=2 ... B=11).
     *
     * @param step the step letter
     * @return the semitone offset, or 0 if unknown
     */
    public int stepSemitone(String step) {
        int index = diatonicIndex(step);
        if (index < 0) {
            return 0;
        }
        return DIATONIC_SEMITONES[index];
    }


    /**
     * Computes the MIDI pitch of a spelled note.
     *
     * @param step the step letter
     * @param alter the alteration in semitones
     * @param octave the octave (4 is the octave starting with middle C)
     * @return the MIDI pitch
     */
    public int toMidi(String step, int alter, int octave) {
        return (octave + 1) * SEMITONES_PER_OCTAVE + stepSemitone(step) + alter;
    }


    /**
     * Computes the octave of a spelled note from its MIDI pitch.
     *
     * @param midi the MIDI pitch
     * @param step the step letter
     * @param alter the alteration in semitones
     * @return the octave
     */
    public int octaveOf(int midi, String step, int alter) {
        return Math.floorDiv(midi - alter - stepSemitone(step), SEMITONES_PER_OCTAVE) - 1;
    }


    /**
     * Computes a tonal pitch class for a MIDI pitch without spelling information (sharps preferred).
     *
     * @param midi the MIDI pitch
     * @return the tonal pitch class
     */
    public int midiToTpc(int midi) {
        int semitone = Math.floorMod(midi, SEMITONES_PER_OCTAVE);
        return stepAlterToTpc(SHARP_STEPS[semitone], SHARP_ALTERS[semitone]);
    }


    /**
     * Transposes a tonal pitch class by an interval.
     *
     * @param tpc the tonal pitch class
     * @param diatonic the diatonic steps of the interval
     * @param chromatic the semitones of the interval
     * @return the transposed tonal pitch class
     */
    public int transposeTpc(int tpc, int diatonic, int chromatic) {
        if (diatonic == 0 && chromatic == 0) {
            return tpc;
        }

        String step = tpcToStep(tpc);
        int alter = tpcToAlter(tpc);
        int index = diatonicIndex(step);
        int target = index + diatonic;
        int octaveShift = Math.floorDiv(target, STEPS_PER_OCTAVE);
        int targetIndex = Math.floorMod(target, STEPS_PER_OCTAVE);
        int naturalDiff = DIATONIC_SEMITONES[targetIndex] + octaveShift * SEMITONES_PER_OCTAVE - DIATONIC_SEMITONES[index];
        int targetAlter = alter + chromatic - naturalDiff;
        return stepAlterToTpc(String.valueOf(DIATONIC_ORDER.charAt(targetIndex)), targetAlter);
    }


    /**
     * Transposes a key signature, expressed as number of fifths, by an interval.
     * The result is normalized into the range -7..7 by enharmonic substitution.
     *
     * @param fifths the number of sharps (positive) or flats (negative)
     * @param diatonic the diatonic steps of the interval
     * @param chromatic the semitones of the interval
     * @return the transposed key in fifths
     */
    public int transposeKey(int fifths, int diatonic, int chromatic) {
        int result = transposeTpc(TPC_C + fifths, diatonic, chromatic) - TPC_C;
        while (result > 7) {
            result -= SEMITONES_PER_OCTAVE;
        }
        while (result < -7) {
            result += SEMITONES_PER_OCTAVE;
        }
        return result;
    }


    /**
     * Gets the index of a step letter in the order of fifths (F=0 ... B=6).
     *
     * @param step the step letter
     * @return the index, or -1 if unknown
     */
    private int stepIndexInFifths(String step) {
        if (step == null || step.isEmpty()) {
            return -1;
        }
        return FIFTHS_ORDER.indexOf(Character.toUpperCase(step.charAt(0)));
    }
}
