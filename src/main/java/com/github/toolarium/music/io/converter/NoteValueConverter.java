/*
 * NoteValueConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.converter;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.TypeAndDots;
import java.util.ArrayList;
import java.util.List;


/**
 * Converts between note values (MusicXML note types with dots) and lengths in whole notes.
 *
 * @author patrick
 */
public final class NoteValueConverter {
    private static final String[] TYPES = {"maxima", "long", "breve", "whole", "half", "quarter", "eighth", "16th", "32nd", "64th", "128th", "256th", "512th", "1024th"};
    private static final int WHOLE_INDEX = 3;
    private static final int MAX_DOTS = 3;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final NoteValueConverter INSTANCE = new NoteValueConverter();
    }


    /**
     * Constructor.
     */
    private NoteValueConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static NoteValueConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the length of an undotted note type.
     *
     * @param type the MusicXML note type
     * @return the length in whole notes, or null if the type is unknown
     */
    public Fraction toFraction(String type) {
        int index = indexOf(type);
        if (index < 0) {
            return null;
        }
        return lengthOfIndex(index);
    }


    /**
     * Gets the length of a dotted note type.
     *
     * @param type the MusicXML note type
     * @param dots the number of dots
     * @return the length in whole notes, or null if the type is unknown
     */
    public Fraction toFraction(String type, int dots) {
        Fraction base = toFraction(type);
        if (base == null) {
            return null;
        }
        Fraction result = base;
        Fraction add = base;
        for (int i = 0; i < dots; i++) {
            add = add.multiply(1, 2);
            result = result.add(add);
        }
        return result;
    }


    /**
     * Finds the note type and dots for a length.
     *
     * @param length the length in whole notes
     * @return the note type and dots, or null if the length has no single note value
     */
    public TypeAndDots fromFraction(Fraction length) {
        if (length == null || !length.isPositive()) {
            return null;
        }
        for (int index = 0; index < TYPES.length; index++) {
            for (int dots = 0; dots <= MAX_DOTS; dots++) {
                if (toFraction(TYPES[index], dots).equals(length)) {
                    return new TypeAndDots(TYPES[index], dots);
                }
            }
        }
        return null;
    }


    /**
     * Splits a length into note values, largest first. A remainder below the shortest note value is dropped.
     *
     * @param length the length in whole notes
     * @return the note values
     */
    public List<TypeAndDots> split(Fraction length) {
        List<TypeAndDots> result = new ArrayList<>();
        if (length == null || !length.isPositive()) {
            return result;
        }
        TypeAndDots single = fromFraction(length);
        if (single != null) {
            result.add(single);
            return result;
        }
        Fraction rest = length;
        for (int index = WHOLE_INDEX; index < TYPES.length && rest.isPositive(); index++) {
            Fraction value = lengthOfIndex(index);
            while (rest.compareTo(value) >= 0) {
                result.add(new TypeAndDots(TYPES[index], 0));
                rest = rest.subtract(value);
            }
        }
        return result;
    }


    /**
     * Gets the note type of a duration denominator, for example 4 for a quarter note.
     *
     * @param denominator the denominator (1 = whole, 2 = half, 4 = quarter and so on; 0 = breve)
     * @return the note type, or null if the value is not a power of two
     */
    public String typeOfDenominator(int denominator) {
        if (denominator == 0) {
            return TYPES[WHOLE_INDEX - 1];
        }
        int index = WHOLE_INDEX;
        int value = 1;
        while (value < denominator && index < TYPES.length - 1) {
            value *= 2;
            index++;
        }
        if (value != denominator) {
            return null;
        }
        return TYPES[index];
    }


    /**
     * Gets the index of a note type.
     *
     * @param type the note type
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
     * Gets the length of the note type at an index.
     *
     * @param index the index in the type table
     * @return the length in whole notes
     */
    private Fraction lengthOfIndex(int index) {
        if (index <= WHOLE_INDEX) {
            return Fraction.of(1L << (WHOLE_INDEX - index), 1);
        }
        return Fraction.of(1, 1L << (index - WHOLE_INDEX));
    }
}
