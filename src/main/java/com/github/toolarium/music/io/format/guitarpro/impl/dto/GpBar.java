/*
 * GpBar.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * The bar of one track in one master bar: clef and voices.
 *
 * @author patrick
 */
public class GpBar {
    private String clef;
    private final List<List<GpBeat>> voices = new ArrayList<>();


    /**
     * Constructor for GpBar.
     */
    public GpBar() {
        // default constructor
    }


    /**
     * Gets the clef (G2, F4, C3, C4, Neutral) or null.
     *
     * @return the clef (G2, F4, C3, C4, Neutral) or null
     */
    public String getClef() {
        return clef;
    }


    /**
     * Sets the clef (G2, F4, C3, C4, Neutral) or null.
     *
     * @param clef the clef (G2, F4, C3, C4, Neutral) or null
     */
    public void setClef(String clef) {
        this.clef = clef;
    }


    /**
     * Gets the beats of each voice (mutable).
     *
     * @return the beats of each voice (mutable)
     */
    public List<List<GpBeat>> getVoices() {
        return voices;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(clef, voices);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a GpBar with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GpBar other = (GpBar) obj;
        return Objects.equals(clef, other.clef)
                && Objects.equals(voices, other.voices);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "GpBar ["
                + "clef=" + clef
                + ", voices=" + voices
                + "]";
    }
}
