/*
 * MeiEventRef.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl.dto;

import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.NoteData;
import com.github.toolarium.music.io.dto.VoiceData;
import java.util.Objects;


/**
 * A reference to an event (and one of its notes) by its MEI ID, with its place in the score.
 *
 * @author patrick
 */
public class MeiEventRef {
    private final EventData event;
    private final NoteData note;
    private final VoiceData voice;
    private final int partIndex;
    private final int measureIndex;
    private final int staff;


    /**
     * Constructor for MeiEventRef.
     *
     * @param event the event
     * @param note the note (null for a rest, chord or event reference)
     * @param voice the voice that contains the event
     * @param partIndex the index of the part
     * @param measureIndex the index of the measure
     * @param staff the staff within the part (1-based)
     */
    public MeiEventRef(EventData event, NoteData note, VoiceData voice, int partIndex, int measureIndex, int staff) {
        this.event = event;
        this.note = note;
        this.voice = voice;
        this.partIndex = partIndex;
        this.measureIndex = measureIndex;
        this.staff = staff;
    }


    /**
     * Gets the event.
     *
     * @return the event
     */
    public EventData getEvent() {
        return event;
    }


    /**
     * Gets the note (null for a rest, chord or event reference).
     *
     * @return the note (null for a rest, chord or event reference)
     */
    public NoteData getNote() {
        return note;
    }


    /**
     * Gets the voice that contains the event.
     *
     * @return the voice that contains the event
     */
    public VoiceData getVoice() {
        return voice;
    }


    /**
     * Gets the index of the part.
     *
     * @return the index of the part
     */
    public int getPartIndex() {
        return partIndex;
    }


    /**
     * Gets the index of the measure.
     *
     * @return the index of the measure
     */
    public int getMeasureIndex() {
        return measureIndex;
    }


    /**
     * Gets the staff within the part (1-based).
     *
     * @return the staff within the part (1-based)
     */
    public int getStaff() {
        return staff;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(event, note, voice, partIndex, measureIndex, staff);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a MeiEventRef with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MeiEventRef other = (MeiEventRef) obj;
        return Objects.equals(event, other.event)
                && Objects.equals(note, other.note)
                && Objects.equals(voice, other.voice)
                && Objects.equals(partIndex, other.partIndex)
                && Objects.equals(measureIndex, other.measureIndex)
                && Objects.equals(staff, other.staff);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "MeiEventRef ["
                + "event=" + event
                + ", note=" + note
                + ", voice=" + voice
                + ", partIndex=" + partIndex
                + ", measureIndex=" + measureIndex
                + ", staff=" + staff
                + "]";
    }
}
