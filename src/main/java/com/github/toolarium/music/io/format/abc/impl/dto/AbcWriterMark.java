/*
 * AbcWriterMark.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.Objects;


/**
 * A mark of a staff to write at a position of a measure: a command before a note (for example \clef bass or
 * \tempo "Allegro") or a post-event after a note (for example \p, \&lt; or ^"dolce").
 *
 * @author patrick
 */
public class AbcWriterMark {
    private final Fraction position;
    private final String text;
    private final boolean before;


    /**
     * Constructor for AbcWriterMark.
     *
     * @param position the position in the measure in whole notes
     * @param text the ABC text
     * @param before true for a command before a note, false for a post-event after a note
     */
    public AbcWriterMark(Fraction position, String text, boolean before) {
        this.position = position;
        this.text = text;
        this.before = before;
    }


    /**
     * Gets the position in the measure.
     *
     * @return the position in whole notes
     */
    public Fraction getPosition() {
        return position;
    }


    /**
     * Gets the ABC text.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }


    /**
     * Checks whether the mark is a command before a note.
     *
     * @return true for a command before a note, false for a post-event after a note
     */
    public boolean isBefore() {
        return before;
    }


    /**
     * Calculates the hash code of all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(position, text, before);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbcWriterMark other = (AbcWriterMark) obj;
        return Objects.equals(position, other.position) && Objects.equals(text, other.text) && before == other.before;
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "AbcWriterMark [position=" + position + ", text=" + text + ", before=" + before + "]";
    }
}
