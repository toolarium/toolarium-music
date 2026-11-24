/*
 * LilyPondToken.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl.dto;

import java.util.Objects;


/**
 * A token of a LilyPond source: a command (\name), a word, a number, a string, a symbol or a Scheme expression.
 *
 * @author patrick
 */
public class LilyPondToken {
    /** A command, for example \new or \\ (the text includes the backslash). */
    public static final int COMMAND = 1;

    /** A word: a note name, an identifier or a keyword without backslash. */
    public static final int WORD = 2;

    /** A number (digits only). */
    public static final int NUMBER = 3;

    /** A string in double quotes (the text is the content without quotes). */
    public static final int STRING = 4;

    /** A symbol: a brace, a bracket, a mark or an operator (for example {, &lt;&lt;, ~, --). */
    public static final int SYMBOL = 5;

    /** A Scheme expression after # (the text includes the #). */
    public static final int SCHEME = 6;

    private final int type;
    private final String text;
    private final int start;
    private final int end;


    /**
     * Constructor for LilyPondToken.
     *
     * @param type the type
     * @param text the text
     * @param start the offset of the first character in the source
     * @param end the offset after the last character in the source
     */
    public LilyPondToken(int type, String text, int start, int end) {
        this.type = type;
        this.text = text;
        this.start = start;
        this.end = end;
    }


    /**
     * Gets the type.
     *
     * @return the type (COMMAND, WORD, NUMBER, STRING, SYMBOL or SCHEME)
     */
    public int getType() {
        return type;
    }


    /**
     * Gets the text.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }


    /**
     * Gets the offset of the first character in the source.
     *
     * @return the offset
     */
    public int getStart() {
        return start;
    }


    /**
     * Gets the offset after the last character in the source.
     *
     * @return the offset
     */
    public int getEnd() {
        return end;
    }


    /**
     * Checks whether the token is of a type and has a text.
     *
     * @param expectedType the type
     * @param expectedText the text
     * @return true if both match
     */
    public boolean is(int expectedType, String expectedText) {
        return type == expectedType && text.equals(expectedText);
    }


    /**
     * Returns a hash code over all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, text, start, end);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a LilyPondToken with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LilyPondToken other = (LilyPondToken) obj;
        return type == other.type && start == other.start && end == other.end && Objects.equals(text, other.text);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "LilyPondToken ["
                + "type=" + type
                + ", text=" + text
                + ", start=" + start
                + ", end=" + end
                + "]";
    }
}
