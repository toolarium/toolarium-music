/*
 * SpelledPitch.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import java.util.Objects;


/**
 * A spelled pitch.
 *
 * @author patrick
 */
public class SpelledPitch {
    private final String step;
    private final int alter;
    private final int octave;


    /**
     * Constructor for SpelledPitch.
     *
     * @param step the step (C to B)
     * @param alter the alteration in semitones
     * @param octave the octave (4 is the octave of middle C)
     */
    public SpelledPitch(String step, int alter, int octave) {
        this.step = step;
        this.alter = alter;
        this.octave = octave;
    }


    /**
     * Gets the step.
     *
     * @return the step (C to B)
     */
    public String getStep() {
        return step;
    }


    /**
     * Gets the alteration.
     *
     * @return the alteration in semitones
     */
    public int getAlter() {
        return alter;
    }


    /**
     * Gets the octave.
     *
     * @return the octave (4 is the octave of middle C)
     */
    public int getOctave() {
        return octave;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(step, alter, octave);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a SpelledPitch with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SpelledPitch other = (SpelledPitch) obj;
        return Objects.equals(step, other.step)
                && Objects.equals(alter, other.alter)
                && Objects.equals(octave, other.octave);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "SpelledPitch ["
                + "step=" + step
                + ", alter=" + alter
                + ", octave=" + octave
                + "]";
    }
}
