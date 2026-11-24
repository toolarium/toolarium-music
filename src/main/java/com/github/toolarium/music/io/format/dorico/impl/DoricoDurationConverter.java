/*
 * DoricoDurationConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.ArrayList;
import java.util.List;


/**
 * Converts Dorico durations (exact fractions of a quarter note) into notatable MusicXML values.
 *
 * <p>Dorico stores the played length of a note and decides at render time how to notate it.
 * This converter splits a length into a sequence of notatable values (with up to two dots for
 * notes), each aligned to its own value within the bar, so that the pieces can be joined with
 * ties. Lengths that cannot be expressed with binary note values (tuplets) are emitted as one
 * piece with the closest larger note type.</p>
 *
 * @author patrick
 */
public final class DoricoDurationConverter {
    private static final String[] TYPES = {"breve", "whole", "half", "quarter", "eighth", "16th", "32nd", "64th", "128th", "256th"};
    private static final Fraction[] VALUES = {
        Fraction.of(8, 1), Fraction.of(4, 1), Fraction.of(2, 1), Fraction.of(1, 1), Fraction.of(1, 2),
        Fraction.of(1, 4), Fraction.of(1, 8), Fraction.of(1, 16), Fraction.of(1, 32), Fraction.of(1, 64)};
    private static final Fraction SINGLE_DOT = Fraction.of(3, 2);
    private static final Fraction DOUBLE_DOT = Fraction.of(7, 4);


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final DoricoDurationConverter INSTANCE = new DoricoDurationConverter();
    }


    /**
     * Constructor.
     */
    private DoricoDurationConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static DoricoDurationConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Splits a length into notatable pieces.
     *
     * @param offsetInBar the start of the length relative to the start of the bar, in quarter notes
     * @param length the length in quarter notes
     * @param allowDots true to allow dotted values (notes), false for undotted values only (rests)
     * @param divisions the MusicXML divisions per quarter note; every piece must be a whole number of divisions
     * @return the pieces in time order
     */
    public List<Piece> split(Fraction offsetInBar, Fraction length, boolean allowDots, int divisions) {
        List<Piece> pieces = new ArrayList<>();
        Fraction position = offsetInBar;
        Fraction remaining = length;

        while (remaining.isPositive()) {
            Piece piece = findPiece(position, remaining, allowDots, divisions);
            if (piece == null) {
                // not expressible with binary values (for example a tuplet): keep the length as one piece
                pieces.add(new Piece(remaining, closestType(remaining), 0));
                break;
            }
            pieces.add(piece);
            position = position.add(piece.getLength());
            remaining = remaining.subtract(piece.getLength());
        }
        return pieces;
    }


    /**
     * Gets the note value of exactly a length: an undotted or a dotted value.
     *
     * @param length the length in quarter notes
     * @param divisions the divisions per quarter note
     * @return the piece, or null if no undotted or dotted value has the length
     */
    public Piece single(Fraction length, int divisions) {
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i].equals(length) && isWholeDivisions(length, divisions)) {
                return new Piece(length, TYPES[i], 0);
            }
            if (VALUES[i].multiply(SINGLE_DOT).equals(length) && isWholeDivisions(length, divisions)) {
                return new Piece(length, TYPES[i], 1);
            }
        }
        return null;
    }


    /**
     * Gets the longest undotted or dotted value that is not longer than a length.
     *
     * @param length the length in quarter notes
     * @param divisions the divisions per quarter note
     * @return the longest value, or null if no value fits
     */
    public Fraction longestSingle(Fraction length, int divisions) {
        for (Fraction value : VALUES) {
            Fraction dotted = value.multiply(SINGLE_DOT);
            if (fits(dotted, length, divisions)) {
                return dotted;
            }
            if (fits(value, length, divisions)) {
                return value;
            }
        }
        return null;
    }


    /**
     * Converts a Dorico note value index (used in tempo and tuplet definitions) into a MusicXML type.
     * Index 8 is a quarter note, 7 an eighth note, 9 a half note, and so on.
     *
     * @param index the note value index
     * @return the MusicXML type, or "quarter" if the index is out of range
     */
    public String noteValueToType(int index) {
        int typeIndex = 11 - index;
        if (typeIndex < 0 || typeIndex >= TYPES.length) {
            return TYPES[3];
        }
        return TYPES[typeIndex];
    }


    /**
     * Gets the length of a Dorico note value index in quarter notes.
     *
     * @param index the note value index (8 is a quarter note)
     * @param dots the number of dots
     * @return the length in quarter notes
     */
    public Fraction noteValueToLength(int index, int dots) {
        int typeIndex = 11 - index;
        if (typeIndex < 0 || typeIndex >= VALUES.length) {
            typeIndex = 3;
        }
        Fraction base = VALUES[typeIndex];
        Fraction result = base;
        Fraction add = base;
        for (int i = 0; i < dots && i < 3; i++) {
            add = add.multiply(1, 2);
            result = result.add(add);
        }
        return result;
    }


    /**
     * Finds the largest notatable piece that starts at the given position. A value may start at
     * a multiple of its undotted value; a dotted value may also start at a multiple of its own
     * length (for example a dotted quarter on the second beat of a 6/8 bar).
     *
     * @param position the position relative to the bar start
     * @param remaining the remaining length
     * @param allowDots true to allow dotted values
     * @param divisions the divisions per quarter note
     * @return the piece, or null if no binary value fits
     */
    private Piece findPiece(Fraction position, Fraction remaining, boolean allowDots, int divisions) {
        for (int i = 0; i < VALUES.length; i++) {
            Fraction value = VALUES[i];
            boolean alignedToValue = isMultipleOf(position, value);

            if (allowDots) {
                Fraction doubleDotted = value.multiply(DOUBLE_DOT);
                if ((alignedToValue || isMultipleOf(position, doubleDotted)) && fits(doubleDotted, remaining, divisions)) {
                    return new Piece(doubleDotted, TYPES[i], 2);
                }
                Fraction dotted = value.multiply(SINGLE_DOT);
                if ((alignedToValue || isMultipleOf(position, dotted)) && fits(dotted, remaining, divisions)) {
                    return new Piece(dotted, TYPES[i], 1);
                }
            }
            if (alignedToValue && fits(value, remaining, divisions)) {
                return new Piece(value, TYPES[i], 0);
            }
        }
        return null;
    }


    /**
     * Checks whether a candidate fits into the remaining length and the divisions grid.
     *
     * @param candidate the candidate length
     * @param remaining the remaining length
     * @param divisions the divisions per quarter note
     * @return true if the candidate can be used
     */
    private boolean fits(Fraction candidate, Fraction remaining, int divisions) {
        return candidate.compareTo(remaining) <= 0 && isWholeDivisions(candidate, divisions);
    }


    /**
     * Checks whether a length is a whole number of divisions.
     *
     * @param length the length in quarter notes
     * @param divisions the divisions per quarter note
     * @return true if the length is a whole number of divisions
     */
    private boolean isWholeDivisions(Fraction length, int divisions) {
        return (length.getNumerator() * divisions) % length.getDenominator() == 0;
    }


    /**
     * Checks whether a position is a multiple of a value.
     *
     * @param position the position
     * @param value the value
     * @return true if the position is a whole multiple of the value
     */
    private boolean isMultipleOf(Fraction position, Fraction value) {
        Fraction quotient = position.multiply(value.getDenominator(), value.getNumerator());
        return quotient.getDenominator() == 1;
    }


    /**
     * Gets the type of the shortest binary value not shorter than the given length. Tuplet notes
     * are shorter than their written value (a triplet eighth lasts a third of a quarter note).
     *
     * @param length the length
     * @return the MusicXML type
     */
    private String closestType(Fraction length) {
        for (int i = VALUES.length - 1; i >= 0; i--) {
            if (VALUES[i].compareTo(length) >= 0) {
                return TYPES[i];
            }
        }
        return TYPES[0];
    }


    /**
     * A notatable piece of a length.
     *
     * @author patrick
     */
    public static final class Piece {
        private final Fraction length;
        private final String type;
        private final int dots;


        /**
         * Constructor for Piece.
         *
         * @param length the length in quarter notes
         * @param type the MusicXML note type
         * @param dots the number of dots
         */
        Piece(Fraction length, String type, int dots) {
            this.length = length;
            this.type = type;
            this.dots = dots;
        }


        /**
         * Gets the length in quarter notes.
         *
         * @return the length
         */
        public Fraction getLength() {
            return length;
        }


        /**
         * Gets the MusicXML note type.
         *
         * @return the type
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
         * Returns a short description of the piece.
         *
         * @return the description
         */
        @Override
        public String toString() {
            return "Piece [length=" + length + ", type=" + type + ", dots=" + dots + "]";
        }
    }
}
