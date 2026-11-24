/*
 * VoiceData.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * The events of one voice in one measure, in time order, as collected by a format reader for the
 * {@link ScoreBuilder}. The first event starts at the measure start; gaps can be filled with
 * invisible rests.
 *
 * @author patrick
 */
public class VoiceData {
    private final int staff;
    private final int voice;
    private final List<EventData> events = new ArrayList<>();


    /**
     * Constructor for VoiceData.
     *
     * @param staff the staff within the part (1-based)
     * @param voice the MusicXML voice number (unique within the part)
     */
    public VoiceData(int staff, int voice) {
        this.staff = staff;
        this.voice = voice;
    }


    /**
     * Gets the staff within the part.
     *
     * @return the staff (1-based)
     */
    public int getStaff() {
        return staff;
    }


    /**
     * Gets the MusicXML voice number.
     *
     * @return the voice number
     */
    public int getVoice() {
        return voice;
    }


    /**
     * Gets the events in time order.
     *
     * @return the events (mutable)
     */
    public List<EventData> getEvents() {
        return events;
    }


    /**
     * Calculates the length of the voice: the sum of all event durations.
     *
     * @return the length in whole notes
     */
    public Fraction getLength() {
        Fraction length = Fraction.ZERO;
        for (EventData event : events) {
            if (!event.isGrace() && event.getDuration() != null) {
                length = length.add(event.getDuration());
            }
        }
        return length;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(staff, voice, events);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a VoiceData with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        VoiceData other = (VoiceData) obj;
        return Objects.equals(staff, other.staff)
                && Objects.equals(voice, other.voice)
                && Objects.equals(events, other.events);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "VoiceData ["
                + "staff=" + staff
                + ", voice=" + voice
                + ", events=" + events
                + "]";
    }
}
