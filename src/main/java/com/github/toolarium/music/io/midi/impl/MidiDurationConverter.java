/*
 * MidiDurationConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.midi.impl;


/**
 * Converts between MIDI tick durations and musical note type values.
 * Uses the MIDI resolution (ticks per quarter note) as the reference unit.
 *
 * @author patrick
 */
public final class MidiDurationConverter {

    /** Default MIDI resolution when none is specified. */
    public static final int DEFAULT_RESOLUTION = 480;

    private static final double DOT_FACTOR = 1.5;
    private static final double[] NOTE_FACTORS = {
        16.0,   // maxima (4 whole notes)
        8.0,    // long (2 whole notes)
        4.0,    // whole
        2.0,    // half
        1.0,    // quarter
        0.5,    // eighth
        0.25,   // 16th
        0.125,  // 32nd
        0.0625  // 64th
    };
    private static final String[] NOTE_TYPES = {
        "maxima", "long", "whole", "half", "quarter",
        "eighth", "16th", "32nd", "64th"
    };


    /**
     * Private constructor for utility class.
     */
    private MidiDurationConverter() {
        // utility class
    }


    /**
     * Quantizes a tick duration to the nearest standard note type.
     * Returns a two-element array: [noteTypeIndex, dotCount].
     *
     * @param ticks the duration in MIDI ticks
     * @param resolution the MIDI resolution (ticks per quarter note)
     * @return an array where index 0 is the note type name and index 1 is the dot count as a string
     */
    public static String[] quantize(long ticks, int resolution) {
        if (ticks <= 0 || resolution <= 0) {
            return new String[]{"quarter", "0"};
        }

        double quarterNotes = (double) ticks / resolution;
        double bestDiff = Double.MAX_VALUE;
        int bestIndex = 4; // quarter
        int bestDots = 0;

        for (int i = 0; i < NOTE_FACTORS.length; i++) {
            // Plain duration
            double diff = Math.abs(quarterNotes - NOTE_FACTORS[i]);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIndex = i;
                bestDots = 0;
            }

            // Dotted duration
            double dottedFactor = NOTE_FACTORS[i] * DOT_FACTOR;
            diff = Math.abs(quarterNotes - dottedFactor);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIndex = i;
                bestDots = 1;
            }
        }

        return new String[]{NOTE_TYPES[bestIndex], String.valueOf(bestDots)};
    }


    /**
     * Converts a note type name and dot count to a duration in ticks.
     *
     * @param noteType the note type name (e.g., "quarter", "eighth")
     * @param dots the number of dots
     * @param resolution the MIDI resolution (ticks per quarter note)
     * @return the duration in MIDI ticks
     */
    public static long toTicks(String noteType, int dots, int resolution) {
        double factor = noteTypeToFactor(noteType);
        if (dots > 0) {
            factor = factor * DOT_FACTOR;
        }
        return Math.round(factor * resolution);
    }


    /**
     * Converts a MusicXML duration value to MIDI ticks.
     *
     * @param duration the MusicXML duration in divisions
     * @param divisions the MusicXML divisions per quarter note
     * @param resolution the MIDI resolution (ticks per quarter note)
     * @return the duration in MIDI ticks
     */
    public static long divisionsToTicks(int duration, int divisions, int resolution) {
        if (divisions <= 0) {
            return duration;
        }
        return Math.round((double) duration / divisions * resolution);
    }


    /**
     * Converts a note type name to its factor relative to a quarter note.
     *
     * @param noteType the note type name
     * @return the factor (1.0 for quarter, 2.0 for half, etc.)
     */
    private static double noteTypeToFactor(String noteType) {
        if (noteType == null) {
            return 1.0;
        }

        for (int i = 0; i < NOTE_TYPES.length; i++) {
            if (NOTE_TYPES[i].equals(noteType)) {
                return NOTE_FACTORS[i];
            }
        }

        return 1.0;
    }
}
