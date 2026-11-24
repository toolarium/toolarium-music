/*
 * GuitarProConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl;

import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.SpelledPitch;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpBeat;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Converts Guitar Pro values (durations, tuplets, note values, dynamics, clefs and drum notes)
 * into the values of the score model.
 *
 * @author patrick
 */
final class GuitarProConverter {
    private static final int GUITAR_FIRST = 25;
    private static final int BASS_LAST = 40;
    private static final String STEP_C = "C";
    private static final String STEP_F = "F";
    private static final String CROSS = "x";
    private static final Map<Integer, Object[]> DRUMS = createDrums();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final GuitarProConverter INSTANCE = new GuitarProConverter();
    }


    /**
     * Constructor.
     */
    private GuitarProConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    static GuitarProConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Sets the duration of a beat from a binary duration code (-2 whole to 4 64th), dots and tuplet.
     *
     * @param beat the beat
     * @param value the duration code
     * @param dots the number of dots
     * @param tuplet the tuplet number (0 or 1 for none)
     */
    void applyBinaryDuration(GpBeat beat, int value, int dots, int tuplet) {
        int code = Math.max(-2, Math.min(6, value));
        int denominator = 1 << (code + 2);
        applyDuration(beat, Fraction.of(1, denominator), NoteValueConverter.getInstance().typeOfDenominator(denominator), dots, tuplet, tupletNormal(tuplet));
    }


    /**
     * Sets the duration of a beat.
     *
     * @param beat the beat
     * @param base the undotted note value in whole notes
     * @param type the MusicXML note type
     * @param dots the number of dots
     * @param actual the actual notes of the tuplet (0 or 1 for none)
     * @param normal the normal notes of the tuplet
     */
    void applyDuration(GpBeat beat, Fraction base, String type, int dots, int actual, int normal) {
        Fraction length = base;
        Fraction add = base;
        for (int i = 0; i < dots; i++) {
            add = add.multiply(1, 2);
            length = length.add(add);
        }
        if (actual > 1 && normal > 0 && actual != normal) {
            length = length.multiply(normal, actual);
            beat.setTupletActual(actual);
            beat.setTupletNormal(normal);
        }
        beat.setDuration(length);
        beat.setType(type);
        beat.setDots(dots);
    }


    /**
     * Gets the normal notes of a Guitar Pro tuplet.
     *
     * @param tuplet the tuplet number
     * @return the normal notes
     */
    int tupletNormal(int tuplet) {
        switch (tuplet) {
            case 2:
                return 3;
            case 3:
                return 2;
            case 4:
                return 6;
            case 5:
            case 6:
            case 7:
                return 4;
            case 8:
                return 12;
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
                return 8;
            default:
                return 1;
        }
    }


    /**
     * Gets the length of a GPIF note value.
     *
     * @param noteValue the note value (Long, DoubleWhole, Whole, Half, Quarter, Eighth, 16th ... 256th)
     * @return the length in whole notes, or null if unknown
     */
    Fraction noteValueOf(String noteValue) {
        if (noteValue == null) {
            return null;
        }
        switch (noteValue) {
            case "Long":
                return Fraction.of(4, 1);
            case "DoubleWhole":
                return Fraction.of(2, 1);
            case "Whole":
                return Fraction.of(1, 1);
            case "Half":
                return Fraction.of(1, 2);
            case "Quarter":
                return Fraction.of(1, 4);
            case "Eighth":
                return Fraction.of(1, 8);
            default:
                break;
        }
        String digits = noteValue.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        int value = Integer.parseInt(digits);
        if (value <= 0 || Integer.bitCount(value) != 1) {
            return null;
        }
        return Fraction.of(1, value);
    }


    /**
     * Gets the dynamics marking of a GPIF dynamic (PPP to FFF).
     *
     * @param dynamic the GPIF dynamic
     * @return the marking, or null
     */
    String dynamicOf(String dynamic) {
        if (dynamic == null || dynamic.trim().isEmpty()) {
            return null;
        }
        return dynamic.trim().toLowerCase(Locale.ROOT);
    }


    /**
     * Creates the clef of a Guitar Pro clef name.
     *
     * @param name the clef name (G2, F4, C3, C4, Neutral)
     * @param octave the octave change (Ottavia), 0 for none
     * @return the clef, or null
     */
    ClefImpl clefOf(String name, int octave) {
        if (name == null) {
            return null;
        }
        ScoreBuilder builder = ScoreBuilder.getInstance();
        switch (name) {
            case "G2":
                return builder.createClef("G", 2, octave);
            case "F4":
                return builder.createClef(STEP_F, 4, octave);
            case "C3":
                return builder.createClef(STEP_C, 3, octave);
            case "C4":
                return builder.createClef(STEP_C, 4, octave);
            case "Neutral":
                return builder.createClef("percussion", null, 0);
            default:
                return null;
        }
    }


    /**
     * Checks if a General MIDI program is written an octave higher than it sounds (guitars and basses).
     *
     * @param program the program (1-based), may be null
     * @return true for guitars and basses
     */
    boolean isOctaveTransposing(Integer program) {
        return program != null && program >= GUITAR_FIRST && program <= BASS_LAST;
    }


    /**
     * Gets the display position of a General MIDI drum note on a percussion staff.
     *
     * @param midi the drum note
     * @return the display pitch
     */
    SpelledPitch drumDisplay(int midi) {
        Object[] drum = DRUMS.get(midi);
        if (drum == null) {
            return new SpelledPitch(STEP_C, 0, 5);
        }
        return new SpelledPitch((String) drum[0], 0, (Integer) drum[1]);
    }


    /**
     * Gets the notehead of a General MIDI drum note.
     *
     * @param midi the drum note
     * @return the notehead (x for cymbals), or null for the normal notehead
     */
    String drumNotehead(int midi) {
        Object[] drum = DRUMS.get(midi);
        if (drum == null) {
            return null;
        }
        return (String) drum[2];
    }


    /**
     * Creates the display positions of the General MIDI drum notes.
     *
     * @return the map from MIDI note to step, octave and notehead
     */
    private static Map<Integer, Object[]> createDrums() {
        Map<Integer, Object[]> drums = new HashMap<>();
        drums.put(35, new Object[] {"E", 4, null});
        drums.put(36, new Object[] {STEP_F, 4, null});
        drums.put(37, new Object[] {STEP_C, 5, CROSS});
        drums.put(38, new Object[] {STEP_C, 5, null});
        drums.put(39, new Object[] {STEP_C, 5, CROSS});
        drums.put(40, new Object[] {STEP_C, 5, null});
        drums.put(41, new Object[] {"G", 4, null});
        drums.put(42, new Object[] {"G", 5, CROSS});
        drums.put(43, new Object[] {"A", 4, null});
        drums.put(44, new Object[] {"D", 4, CROSS});
        drums.put(45, new Object[] {"B", 4, null});
        drums.put(46, new Object[] {"G", 5, "circle-x"});
        drums.put(47, new Object[] {"D", 5, null});
        drums.put(48, new Object[] {"E", 5, null});
        drums.put(49, new Object[] {"A", 5, CROSS});
        drums.put(50, new Object[] {STEP_F, 5, null});
        drums.put(51, new Object[] {STEP_F, 5, CROSS});
        drums.put(52, new Object[] {"B", 5, CROSS});
        drums.put(53, new Object[] {STEP_F, 5, "diamond"});
        drums.put(54, new Object[] {"E", 5, CROSS});
        drums.put(55, new Object[] {"B", 5, CROSS});
        drums.put(56, new Object[] {"A", 4, "triangle"});
        drums.put(57, new Object[] {"A", 5, CROSS});
        drums.put(59, new Object[] {STEP_F, 5, CROSS});
        return drums;
    }
}
