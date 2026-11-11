/*
 * UnpitchedImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.note.IUnpitched;
import java.util.Objects;

/**
 * Implements the {@link IUnpitched} interface.
 *
 * @author patrick
 */
public class UnpitchedImpl implements IUnpitched {

    @JacksonXmlProperty(localName = "display-step")
    private String displayStep;

    @JacksonXmlProperty(localName = "display-octave")
    private Integer displayOctave;


    /**
     * Gets the display step.
     *
     * @return the display step
     */
    @Override
    public String getDisplayStep() {
        return displayStep;
    }


    /**
     * Sets the display step.
     *
     * @param displayStep the display step
     */
    public void setDisplayStep(String displayStep) {
        this.displayStep = displayStep;
    }


    /**
     * Gets the display octave.
     *
     * @return the display octave
     */
    @Override
    public Integer getDisplayOctave() {
        return displayOctave;
    }


    /**
     * Sets the display octave.
     *
     * @param displayOctave the display octave
     */
    public void setDisplayOctave(Integer displayOctave) {
        this.displayOctave = displayOctave;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(displayStep, displayOctave);
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
        UnpitchedImpl other = (UnpitchedImpl) obj;
        return Objects.equals(displayStep, other.displayStep)
                && Objects.equals(displayOctave, other.displayOctave);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "UnpitchedImpl [displayStep=" + displayStep + ", displayOctave=" + displayOctave + "]";
    }
}
