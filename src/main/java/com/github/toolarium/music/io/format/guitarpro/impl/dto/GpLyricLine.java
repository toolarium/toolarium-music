/*
 * GpLyricLine.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl.dto;

import java.util.Objects;


/**
 * A Guitar Pro lyrics line: the first measure and the text.
 *
 * @author patrick
 */
public class GpLyricLine {
    private final int startMeasure;
    private final String text;


    /**
     * Constructor for GpLyricLine.
     *
     * @param startMeasure the first measure (0-based)
     * @param text the lyrics text
     */
    public GpLyricLine(int startMeasure, String text) {
        this.startMeasure = startMeasure;
        this.text = text;
    }


    /**
     * Gets the first measure (0-based).
     *
     * @return the first measure (0-based)
     */
    public int getStartMeasure() {
        return startMeasure;
    }


    /**
     * Gets the lyrics text.
     *
     * @return the lyrics text
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
        return Objects.hash(startMeasure, text);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a GpLyricLine with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GpLyricLine other = (GpLyricLine) obj;
        return Objects.equals(startMeasure, other.startMeasure)
                && Objects.equals(text, other.text);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "GpLyricLine ["
                + "startMeasure=" + startMeasure
                + ", text=" + text
                + "]";
    }
}
