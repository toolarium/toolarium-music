/*
 * MeiWriterEvent.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.TypeAndDots;
import com.github.toolarium.music.model.measure.IClef;
import com.github.toolarium.music.model.note.INote;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * An event of a layer to write: a note, a chord (several notes), a rest, or a space that fills a gap in the layer,
 * with its ID, its start and length in whole notes and the clef changes before it.
 *
 * @author patrick
 */
public class MeiWriterEvent {
    private final String id;
    private final Fraction start;
    private final Fraction length;
    private final List<INote> notes = new ArrayList<>();
    private final List<IClef> clefsBefore = new ArrayList<>();
    private TypeAndDots spaceValue;


    /**
     * Constructor for MeiWriterEvent.
     *
     * @param id the xml:id
     * @param start the start in the measure in whole notes
     * @param length the sounding length in whole notes (zero for a grace note)
     */
    public MeiWriterEvent(String id, Fraction start, Fraction length) {
        this.id = id;
        this.start = start;
        this.length = length;
    }


    /**
     * Gets the xml:id.
     *
     * @return the ID
     */
    public String getId() {
        return id;
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
     * @return the first note, or null for a space
     */
    public INote getFirst() {
        if (notes.isEmpty()) {
            return null;
        }
        return notes.get(0);
    }


    /**
     * Gets the clef changes before the event.
     *
     * @return the clefs
     */
    public List<IClef> getClefsBefore() {
        return clefsBefore;
    }


    /**
     * Gets the note value of a space that fills a gap.
     *
     * @return the note value, or null if the event is no space
     */
    public TypeAndDots getSpaceValue() {
        return spaceValue;
    }


    /**
     * Sets the note value of a space that fills a gap.
     *
     * @param spaceValue the note value
     */
    public void setSpaceValue(TypeAndDots spaceValue) {
        this.spaceValue = spaceValue;
    }


    /**
     * Calculates the hash code of all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, start, length, notes, clefsBefore, spaceValue);
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
        MeiWriterEvent other = (MeiWriterEvent) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(start, other.start)
                && Objects.equals(length, other.length)
                && Objects.equals(notes, other.notes)
                && Objects.equals(clefsBefore, other.clefsBefore)
                && Objects.equals(spaceValue, other.spaceValue);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "MeiWriterEvent ["
                + "id=" + id
                + ", start=" + start
                + ", length=" + length
                + ", notes=" + notes.size()
                + ", clefsBefore=" + clefsBefore
                + ", spaceValue=" + spaceValue
                + "]";
    }
}
