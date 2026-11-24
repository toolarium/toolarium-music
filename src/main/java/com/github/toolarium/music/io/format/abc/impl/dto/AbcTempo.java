/*
 * AbcTempo.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.Objects;


/**
 * An ABC tempo: beats per minute of a beat unit and an optional text.
 *
 * @author patrick
 */
public class AbcTempo {
    private final Fraction beat;
    private final int bpm;
    private final String text;


    /**
     * Constructor for AbcTempo.
     *
     * @param beat the beat unit in whole notes, or null for the unit note length
     * @param bpm the beats per minute (0 for text only)
     * @param text the tempo text, or null
     */
    public AbcTempo(Fraction beat, int bpm, String text) {
        this.beat = beat;
        this.bpm = bpm;
        this.text = text;
    }


    /**
     * Gets the beat unit in whole notes (null for the unit note length).
     *
     * @return the beat unit in whole notes (null for the unit note length)
     */
    public Fraction getBeat() {
        return beat;
    }


    /**
     * Gets the beats per minute (0 for text only).
     *
     * @return the beats per minute (0 for text only)
     */
    public int getBpm() {
        return bpm;
    }


    /**
     * Gets the tempo text.
     *
     * @return the tempo text
     */
    public String getText() {
        return text;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(beat, bpm, text);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a AbcTempo with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbcTempo other = (AbcTempo) obj;
        return Objects.equals(beat, other.beat)
                && Objects.equals(bpm, other.bpm)
                && Objects.equals(text, other.text);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "AbcTempo ["
                + "beat=" + beat
                + ", bpm=" + bpm
                + ", text=" + text
                + "]";
    }
}
