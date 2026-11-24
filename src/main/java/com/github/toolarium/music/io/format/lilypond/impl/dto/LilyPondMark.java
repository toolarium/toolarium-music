/*
 * LilyPondMark.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.Objects;


/**
 * A mark of a LilyPond staff at a position: a clef, a key, a time signature, a barline, a repeat, an ending, a
 * direction (tempo, rehearsal mark) or a chord symbol.
 *
 * @author patrick
 */
public class LilyPondMark {
    /** A clef: the value is the ClefImpl. */
    public static final int CLEF = 1;

    /** A key: the number is the fifths, the text the mode. */
    public static final int KEY = 2;

    /** A time signature: the number is the beats, the second number the beat type. */
    public static final int TIME = 3;

    /** A barline: the text is the LilyPond bar type (for example |. or ||). */
    public static final int BAR = 4;

    /** The start of a repeat. */
    public static final int REPEAT_START = 5;

    /** The end of a repeat: the number is the number of times. */
    public static final int REPEAT_END = 6;

    /** The start of an ending (alternative): the text is the ending number. */
    public static final int ENDING_START = 7;

    /** The end of an ending (alternative). */
    public static final int ENDING_STOP = 8;

    /** A direction (tempo, rehearsal mark): the value is the DirectionImpl. */
    public static final int DIRECTION = 9;

    /** A chord symbol: the value is the HarmonyImpl. */
    public static final int HARMONY = 10;

    private final int kind;
    private final Fraction position;
    private final String text;
    private final int number;
    private final int secondNumber;
    private final Object value;


    /**
     * Constructor for LilyPondMark.
     *
     * @param kind the kind (CLEF, KEY, TIME, BAR, REPEAT_START, REPEAT_END, ENDING_START, ENDING_STOP, DIRECTION or HARMONY)
     * @param position the position in whole notes
     * @param text the text, or null
     * @param numbers the number and the second number (missing numbers are 0)
     * @param value the value (a ClefImpl, DirectionImpl or HarmonyImpl), or null
     */
    public LilyPondMark(int kind, Fraction position, String text, int[] numbers, Object value) {
        this.kind = kind;
        this.position = position;
        this.text = text;
        int first = 0;
        int second = 0;
        if (numbers != null && numbers.length > 0) {
            first = numbers[0];
        }
        if (numbers != null && numbers.length > 1) {
            second = numbers[1];
        }
        this.number = first;
        this.secondNumber = second;
        this.value = value;
    }


    /**
     * Gets the kind.
     *
     * @return the kind
     */
    public int getKind() {
        return kind;
    }


    /**
     * Gets the position.
     *
     * @return the position in whole notes
     */
    public Fraction getPosition() {
        return position;
    }


    /**
     * Gets the text.
     *
     * @return the text, or null
     */
    public String getText() {
        return text;
    }


    /**
     * Gets the number.
     *
     * @return the number (fifths of a key, beats of a time signature, times of a repeat)
     */
    public int getNumber() {
        return number;
    }


    /**
     * Gets the second number.
     *
     * @return the second number (beat type of a time signature)
     */
    public int getSecondNumber() {
        return secondNumber;
    }


    /**
     * Gets the value.
     *
     * @return the value (a ClefImpl, DirectionImpl or HarmonyImpl), or null
     */
    public Object getValue() {
        return value;
    }


    /**
     * Returns a hash code over all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(kind, position, text, number, secondNumber, value);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a LilyPondMark with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LilyPondMark other = (LilyPondMark) obj;
        return kind == other.kind && number == other.number && secondNumber == other.secondNumber && Objects.equals(position, other.position)
                && Objects.equals(text, other.text) && Objects.equals(value, other.value);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "LilyPondMark ["
                + "kind=" + kind
                + ", position=" + position
                + ", text=" + text
                + ", number=" + number
                + ", secondNumber=" + secondNumber
                + ", value=" + value
                + "]";
    }
}
