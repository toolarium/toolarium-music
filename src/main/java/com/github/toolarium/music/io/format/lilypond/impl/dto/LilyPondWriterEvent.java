/*
 * LilyPondWriterEvent.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.note.INote;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * An event of a voice to write: a note, a chord (several notes) or a rest, with its start and sounding length in
 * whole notes. A grace note has no length.
 *
 * @author patrick
 */
public class LilyPondWriterEvent {
    private final Fraction start;
    private final Fraction length;
    private final List<INote> notes = new ArrayList<>();


    /**
     * Constructor for LilyPondWriterEvent.
     *
     * @param start the start in the measure in whole notes
     * @param length the sounding length in whole notes (zero for a grace note)
     */
    public LilyPondWriterEvent(Fraction start, Fraction length) {
        this.start = start;
        this.length = length;
    }


    /**
     * Gets the start in the measure.
     *
     * @return the start in whole notes
     */
    public Fraction getStart() {
        return start;
    }


    /**
     * Gets the sounding length.
     *
     * @return the length in whole notes
     */
    public Fraction getLength() {
        return length;
    }


    /**
     * Gets the notes (the first one carries the duration, a rest has one note).
     *
     * @return the notes
     */
    public List<INote> getNotes() {
        return notes;
    }


    /**
     * Gets the first note.
     *
     * @return the first note
     */
    public INote getFirst() {
        return notes.get(0);
    }


    /**
     * Checks whether the event is a rest.
     *
     * @return true for a rest
     */
    public boolean isRest() {
        return getFirst().getRest() != null;
    }


    /**
     * Checks whether the event is a grace note.
     *
     * @return true for a grace note
     */
    public boolean isGrace() {
        return getFirst().getGrace() != null;
    }


    /**
     * Calculates the hash code of all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(start, length, notes);
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
        LilyPondWriterEvent other = (LilyPondWriterEvent) obj;
        return Objects.equals(start, other.start) && Objects.equals(length, other.length) && Objects.equals(notes, other.notes);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "LilyPondWriterEvent [start=" + start + ", length=" + length + ", notes=" + notes.size() + "]";
    }
}
