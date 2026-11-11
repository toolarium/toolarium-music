/*
 * PitchImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.note.IPitch;
import java.util.Objects;

/**
 * Implements the {@link IPitch} interface.
 *
 * @author patrick
 */
public class PitchImpl implements IPitch {

    @JacksonXmlProperty(localName = "step")
    private String step;

    @JacksonXmlProperty(localName = "alter")
    private Double alter;

    @JacksonXmlProperty(localName = "octave")
    private Integer octave;


    /**
     * Gets the step.
     *
     * @return the step
     */
    @Override
    public String getStep() {
        return step;
    }


    /**
     * Sets the step.
     *
     * @param step the step
     */
    public void setStep(String step) {
        this.step = step;
    }


    /**
     * Gets the alter.
     *
     * @return the alter
     */
    @Override
    public Double getAlter() {
        return alter;
    }


    /**
     * Sets the alter.
     *
     * @param alter the alter
     */
    public void setAlter(Double alter) {
        this.alter = alter;
    }


    /**
     * Gets the octave.
     *
     * @return the octave
     */
    @Override
    public Integer getOctave() {
        return octave;
    }


    /**
     * Sets the octave.
     *
     * @param octave the octave
     */
    public void setOctave(Integer octave) {
        this.octave = octave;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(step, alter, octave);
    }


    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param obj the reference object to compare
     * @return true if this object is the same as the obj argument, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PitchImpl other = (PitchImpl) obj;
        return Objects.equals(step, other.step)
                && Objects.equals(alter, other.alter)
                && Objects.equals(octave, other.octave);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "PitchImpl [step=" + step + ", alter=" + alter + ", octave=" + octave + "]";
    }
}
