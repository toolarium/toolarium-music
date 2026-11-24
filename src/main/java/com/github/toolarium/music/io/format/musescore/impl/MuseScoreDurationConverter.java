/*
 * MuseScoreDurationConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.ArrayList;
import java.util.List;


/**
 * Converts between MuseScore duration types, MusicXML note types and fractions of a whole note.
 * MuseScore and MusicXML share the type names (whole, half, quarter, eighth, 16th, ...).
 * MuseScore additionally knows the pseudo type "measure" for full measure rests.
 *
 * @author patrick
 */
public final class MuseScoreDurationConverter {

    /** The MuseScore duration type for a full measure rest. */
    public static final String MEASURE = "measure";

    private static final String[] TYPES = {"long", "breve", "whole", "half", "quarter", "eighth",
                                           "16th", "32nd", "64th", "128th", "256th", "512th", "1024th"};
    private static final Fraction[] TYPE_FRACTIONS = {Fraction.of(4, 1), Fraction.of(2, 1), Fraction.of(1, 1), Fraction.of(1, 2),
                                                      Fraction.of(1, 4), Fraction.of(1, 8), Fraction.of(1, 16), Fraction.of(1, 32),
                                                      Fraction.of(1, 64), Fraction.of(1, 128), Fraction.of(1, 256), Fraction.of(1, 512),
                                                      Fraction.of(1, 1024)};
    private static final int EIGHTH_INDEX = 5;
    private static final int MAX_DOTS = 4;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MuseScoreDurationConverter INSTANCE = new MuseScoreDurationConverter();
    }


    /**
     * Constructor.
     */
    private MuseScoreDurationConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MuseScoreDurationConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Checks whether the given name is a known note type.
     *
     * @param type the type name
     * @return true if the type is known (not including "measure")
     */
    public boolean isKnownType(String type) {
        return indexOf(type) >= 0;
    }


    /**
     * Normalizes a type name read from MusicXML or MuseScore into the common type name.
     * MusicXML uses "maxima" which MuseScore does not support; it is mapped to "long".
     *
     * @param type the type name
     * @return the normalized name, or null if unknown
     */
    public String normalizeType(String type) {
        if (type == null) {
            return null;
        }

        String trimmed = type.trim();
        if ("maxima".equals(trimmed)) {
            return "long";
        }
        if ("128".equals(trimmed)) {
            return "128th";
        }
        if (indexOf(trimmed) >= 0) {
            return trimmed;
        }
        return null;
    }


    /**
     * Computes the duration of a type with dots as fraction of a whole note.
     *
     * @param type the type name
     * @param dots the number of augmentation dots
     * @return the duration, or null if the type is unknown
     */
    public Fraction toFraction(String type, int dots) {
        int index = indexOf(type);
        if (index < 0) {
            return null;
        }

        Fraction base = TYPE_FRACTIONS[index];
        Fraction result = base;
        Fraction add = base;
        for (int i = 0; i < dots; i++) {
            add = add.multiply(1, 2);
            result = result.add(add);
        }
        return result;
    }


    /**
     * Finds the type and dots that represent a duration exactly.
     *
     * @param duration the duration as fraction of a whole note
     * @return an array {typeIndex, dots}, or null if not representable
     */
    public TypeAndDots fromFraction(Fraction duration) {
        if (duration == null || !duration.isPositive()) {
            return null;
        }

        for (int dots = 0; dots <= MAX_DOTS; dots++) {
            for (int i = 0; i < TYPES.length; i++) {
                Fraction candidate = toFraction(TYPES[i], dots);
                if (candidate != null && candidate.equals(duration)) {
                    return new TypeAndDots(TYPES[i], dots);
                }
            }
        }
        return null;
    }


    /**
     * Splits a duration into a sequence of representable durations (largest first).
     * This is used to fill gaps with rests.
     *
     * @param duration the duration as fraction of a whole note
     * @return the list of type and dots, empty if the duration is not positive
     */
    public List<TypeAndDots> split(Fraction duration) {
        List<TypeAndDots> result = new ArrayList<>();
        if (duration == null || !duration.isPositive()) {
            return result;
        }

        TypeAndDots exact = fromFraction(duration);
        if (exact != null) {
            result.add(exact);
            return result;
        }

        Fraction remaining = duration;
        for (int i = 0; i < TYPES.length && remaining.isPositive(); i++) {
            while (remaining.compareTo(TYPE_FRACTIONS[i]) >= 0) {
                result.add(new TypeAndDots(TYPES[i], 0));
                remaining = remaining.subtract(TYPE_FRACTIONS[i]);
            }
        }
        return result;
    }


    /**
     * Gets the number of beams (flags) a type has: eighth 1, 16th 2, 32nd 3 and so on.
     *
     * @param type the type name
     * @return the number of beams, 0 for quarter and longer or unknown types
     */
    public int beamCount(String type) {
        int index = indexOf(type);
        if (index < EIGHTH_INDEX) {
            return 0;
        }
        return index - EIGHTH_INDEX + 1;
    }


    /**
     * Gets the index of a type name.
     *
     * @param type the type name
     * @return the index, or -1 if unknown
     */
    private int indexOf(String type) {
        if (type == null) {
            return -1;
        }
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(type)) {
                return i;
            }
        }
        return -1;
    }


    /**
     * A note type combined with a number of augmentation dots.
     *
     * @author patrick
     */
    public static final class TypeAndDots {
        private final String type;
        private final int dots;


        /**
         * Constructor.
         *
         * @param type the type name
         * @param dots the number of dots
         */
        public TypeAndDots(String type, int dots) {
            this.type = type;
            this.dots = dots;
        }


        /**
         * Gets the type name.
         *
         * @return the type name
         */
        public String getType() {
            return type;
        }


        /**
         * Gets the number of dots.
         *
         * @return the number of dots
         */
        public int getDots() {
            return dots;
        }


        /**
         * Gets the duration of this type and dots.
         *
         * @return the duration as fraction of a whole note
         */
        public Fraction toFraction() {
            return MuseScoreDurationConverter.getInstance().toFraction(type, dots);
        }


        /**
         * Returns a string representation of this object.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "TypeAndDots [type=" + type + ", dots=" + dots + "]";
        }
    }
}
