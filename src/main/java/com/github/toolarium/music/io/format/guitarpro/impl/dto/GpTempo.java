/*
 * GpTempo.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.Objects;


/**
 * A tempo change at a position of a measure.
 *
 * @author patrick
 */
public class GpTempo {
    private final Fraction position;
    private final double bpm;
    private final String text;


    /**
     * Constructor for GpTempo.
     *
     * @param position the position from the measure start in whole notes
     * @param bpm the tempo in quarter notes per minute
     * @param text the tempo text
     */
    public GpTempo(Fraction position, double bpm, String text) {
        this.position = position;
        this.bpm = bpm;
        this.text = text;
    }


    /**
     * Gets the position from the measure start in whole notes.
     *
     * @return the position from the measure start in whole notes
     */
    public Fraction getPosition() {
        return position;
    }


    /**
     * Gets the tempo in quarter notes per minute.
     *
     * @return the tempo in quarter notes per minute
     */
    public double getBpm() {
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
        return Objects.hash(position, bpm, text);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a GpTempo with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GpTempo other = (GpTempo) obj;
        return Objects.equals(position, other.position)
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
        return "GpTempo ["
                + "position=" + position
                + ", bpm=" + bpm
                + ", text=" + text
                + "]";
    }
}
