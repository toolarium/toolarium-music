/*
 * SibeliusEvent.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl.dto;

import com.github.toolarium.music.io.dto.EventData;
import java.util.Objects;


/**
 * A note, chord or rest of a staff with its voice and its position from the start of the score, used to attach
 * slurs to the events.
 *
 * @author patrick
 */
public class SibeliusEvent {
    private final int voice;
    private final long position;
    private final EventData event;


    /**
     * Constructor for SibeliusEvent.
     *
     * @param voice the voice
     * @param position the position from the start of the score in 1/256 quarter notes
     * @param event the event
     */
    public SibeliusEvent(int voice, long position, EventData event) {
        this.voice = voice;
        this.position = position;
        this.event = event;
    }


    /**
     * Gets the voice.
     *
     * @return the voice
     */
    public int getVoice() {
        return voice;
    }


    /**
     * Gets the position from the start of the score.
     *
     * @return the position in 1/256 quarter notes
     */
    public long getPosition() {
        return position;
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
     * Calculates the hash code.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(voice, position, event);
    }


    /**
     * Compares this event with another object.
     *
     * @param obj the other object
     * @return true if both are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SibeliusEvent other = (SibeliusEvent) obj;
        return voice == other.voice && position == other.position && Objects.equals(event, other.event);
    }


    /**
     * Gets the string representation.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SibeliusEvent [voice=" + voice + ", position=" + position + ", event=" + event + "]";
    }
}
