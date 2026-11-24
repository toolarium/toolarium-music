/*
 * FinalePitchConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl;

import com.github.toolarium.music.io.converter.PitchSpeller;
import com.github.toolarium.music.io.dto.SpelledPitch;


/**
 * Converts Finale pitches and keys.
 *
 * <p>Finale stores the concert pitch of a note as {@code harmLev}, the diatonic displacement from
 * the tonic of the key in the octave of middle C, and {@code harmAlt}, the chromatic alteration
 * relative to the key signature. The key of a measure is a 16-bit value: the low byte is the
 * signed number of fifths, the high byte the mode (0 major, 1 minor). A transposing staff
 * (for example a B-flat trumpet) adds a diatonic interval and a key adjustment in fifths to get
 * the written pitch and key.</p>
 *
 * @author patrick
 */
public final class FinalePitchConverter {
    private static final int STEPS_PER_OCTAVE = 7;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int MIDDLE_C_OCTAVE = 4;
    private static final int FIFTH_SEMITONES = 7;
    private static final int FIFTH_STEPS = 4;
    private static final int MINOR_TONIC_STEPS = 5;
    private static final int MODE_MINOR = 1;
    private static final int BYTE_MASK = 0xFF;
    private static final int BYTE_BITS = 8;
    private static final int MAX_FIFTHS = 7;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final FinalePitchConverter INSTANCE = new FinalePitchConverter();
    }


    /**
     * Constructor for FinalePitchConverter.
     */
    private FinalePitchConverter() {
        // NOP
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static FinalePitchConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the number of fifths of a Finale key value (the signed low byte).
     *
     * @param key the Finale key value
     * @return the number of fifths
     */
    public int fifthsOf(int key) {
        return (byte) (key & BYTE_MASK);
    }


    /**
     * Checks if a Finale key value is a minor key (mode 1 in the high byte).
     *
     * @param key the Finale key value
     * @return true for a minor key
     */
    public boolean isMinor(int key) {
        return ((key >> BYTE_BITS) & BYTE_MASK) == MODE_MINOR;
    }


    /**
     * Gets the concert pitch of a note.
     *
     * @param harmLev the diatonic displacement from the tonic in the octave of middle C
     * @param harmAlt the chromatic alteration relative to the key signature
     * @param fifths the number of fifths of the key
     * @param minor true for a minor key
     * @return the concert pitch
     */
    public SpelledPitch concertPitch(int harmLev, int harmAlt, int fifths, boolean minor) {
        PitchSpeller speller = PitchSpeller.getInstance();
        int tonicStep = Math.floorMod(FIFTH_STEPS * fifths, STEPS_PER_OCTAVE);
        if (minor) {
            tonicStep = Math.floorMod(tonicStep + MINOR_TONIC_STEPS, STEPS_PER_OCTAVE);
        }

        int diatonic = MIDDLE_C_OCTAVE * STEPS_PER_OCTAVE + tonicStep + harmLev;
        String step = speller.stepOf(Math.floorMod(diatonic, STEPS_PER_OCTAVE));
        int octave = Math.floorDiv(diatonic, STEPS_PER_OCTAVE);
        return new SpelledPitch(step, speller.keyAlter(step, fifths) + harmAlt, octave);
    }


    /**
     * Gets the chromatic size of a transposition interval from its diatonic size and its key
     * adjustment: an interval of {@code keyAdjust} fifths has {@code 7 * keyAdjust} semitones
     * within the octave.
     *
     * @param diatonic the diatonic interval in steps (8 is a ninth up)
     * @param keyAdjust the key adjustment in fifths (2 for a major second)
     * @return the interval in semitones
     */
    public int chromaticInterval(int diatonic, int keyAdjust) {
        int withinOctave = Math.floorMod(FIFTH_SEMITONES * keyAdjust, SEMITONES_PER_OCTAVE);
        int steps = Math.floorMod(diatonic, STEPS_PER_OCTAVE);
        if (steps == 0 && withinOctave > SEMITONES_PER_OCTAVE / 2) {
            // a diminished octave belongs to the octave below
            withinOctave -= SEMITONES_PER_OCTAVE;
        }
        return withinOctave + Math.floorDiv(diatonic, STEPS_PER_OCTAVE) * SEMITONES_PER_OCTAVE;
    }


    /**
     * Transposes a pitch by a diatonic and chromatic interval.
     *
     * @param pitch the pitch
     * @param diatonic the diatonic interval in steps
     * @param chromatic the chromatic interval in semitones
     * @return the transposed pitch
     */
    public SpelledPitch transpose(SpelledPitch pitch, int diatonic, int chromatic) {
        if (diatonic == 0 && chromatic == 0) {
            return pitch;
        }
        PitchSpeller speller = PitchSpeller.getInstance();
        int index = pitch.getOctave() * STEPS_PER_OCTAVE + speller.stepIndex(pitch.getStep()) + diatonic;
        String step = speller.stepOf(Math.floorMod(index, STEPS_PER_OCTAVE));
        int octave = Math.floorDiv(index, STEPS_PER_OCTAVE);
        int midi = speller.toMidi(pitch.getStep(), pitch.getAlter(), pitch.getOctave()) + chromatic;
        return new SpelledPitch(step, midi - speller.toMidi(step, 0, octave), octave);
    }


    /**
     * Respells a pitch enharmonically (Finale's enharmonic flip): the same pitch written on the neighbouring
     * step with the smallest alteration, for example B sharp as C, E double flat as D or C sharp as D flat.
     *
     * @param pitch the pitch
     * @return the respelled pitch, or the pitch itself if there is no other spelling within a double alteration
     */
    public SpelledPitch respell(SpelledPitch pitch) {
        PitchSpeller speller = PitchSpeller.getInstance();
        int midi = speller.toMidi(pitch.getStep(), pitch.getAlter(), pitch.getOctave());
        int index = pitch.getOctave() * STEPS_PER_OCTAVE + speller.stepIndex(pitch.getStep());
        SpelledPitch result = pitch;
        int best = Integer.MAX_VALUE;
        for (int direction = -1; direction <= 1; direction += 2) {
            int candidate = index + direction;
            String step = speller.stepOf(Math.floorMod(candidate, STEPS_PER_OCTAVE));
            int octave = Math.floorDiv(candidate, STEPS_PER_OCTAVE);
            int alter = midi - speller.toMidi(step, 0, octave);
            if (Math.abs(alter) <= 2 && Math.abs(alter) < best) {
                best = Math.abs(alter);
                result = new SpelledPitch(step, alter, octave);
            }
        }
        return result;
    }


    /**
     * Transposes a key by a number of fifths, keeping the result within seven fifths.
     *
     * @param fifths the number of fifths
     * @param keyAdjust the key adjustment in fifths
     * @return the transposed number of fifths
     */
    public int transposeKey(int fifths, int keyAdjust) {
        int result = fifths + keyAdjust;
        while (result > MAX_FIFTHS) {
            result -= SEMITONES_PER_OCTAVE;
        }
        while (result < -MAX_FIFTHS) {
            result += SEMITONES_PER_OCTAVE;
        }
        return result;
    }
}
