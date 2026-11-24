/*
 * AbcKey.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl.dto;

import com.github.toolarium.music.io.converter.PitchSpeller;
import java.util.Map;
import java.util.Objects;


/**
 * An ABC key signature: fifths, mode and explicit accidentals per step.
 *
 * @author patrick
 */
public class AbcKey {
    private final int fifths;
    private final String mode;
    private final Map<String, Integer> explicit;


    /**
     * Constructor for AbcKey.
     *
     * @param fifths the number of sharps (positive) or flats (negative)
     * @param mode the mode, or null
     * @param explicit the explicit accidentals by step
     */
    public AbcKey(int fifths, String mode, Map<String, Integer> explicit) {
        this.fifths = fifths;
        this.mode = mode;
        this.explicit = explicit;
    }


    /**
     * Gets the number of sharps (positive) or flats (negative).
     *
     * @return the number of sharps (positive) or flats (negative)
     */
    public int getFifths() {
        return fifths;
    }


    /**
     * Gets the mode, or null.
     *
     * @return the mode, or null
     */
    public String getMode() {
        return mode;
    }


    /**
     * Gets the explicit accidentals by step.
     *
     * @return the explicit accidentals by step
     */
    public Map<String, Integer> getExplicit() {
        return explicit;
    }


    /**
     * Gets the alteration of a step in this key.
     *
     * @param step the step (C to B)
     * @return the alteration in semitones
     */
    public int alterOf(String step) {
        Integer value = explicit.get(step);
        if (value != null) {
            return value;
        }
        return PitchSpeller.getInstance().keyAlter(step, fifths);
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(fifths, mode, explicit);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a AbcKey with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbcKey other = (AbcKey) obj;
        return Objects.equals(fifths, other.fifths)
                && Objects.equals(mode, other.mode)
                && Objects.equals(explicit, other.explicit);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "AbcKey ["
                + "fifths=" + fifths
                + ", mode=" + mode
                + ", explicit=" + explicit
                + "]";
    }
}
