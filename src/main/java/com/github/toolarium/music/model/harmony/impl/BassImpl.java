/*
 * BassImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.harmony.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.harmony.IBass;
import java.util.Objects;

/**
 * Implements the {@link IBass} interface.
 *
 * @author patrick
 */
public class BassImpl implements IBass {

    @JacksonXmlProperty(localName = "bass-step")
    private String bassStep;

    @JacksonXmlProperty(localName = "bass-alter")
    private Double bassAlter;


    /**
     * Gets the bass step.
     *
     * @return the bass step
     */
    @Override
    public String getBassStep() {
        return bassStep;
    }


    /**
     * Sets the bass step.
     *
     * @param bassStep the bass step
     */
    public void setBassStep(String bassStep) {
        this.bassStep = bassStep;
    }


    /**
     * Gets the bass alter.
     *
     * @return the bass alter
     */
    @Override
    public Double getBassAlter() {
        return bassAlter;
    }


    /**
     * Sets the bass alter.
     *
     * @param bassAlter the bass alter
     */
    public void setBassAlter(Double bassAlter) {
        this.bassAlter = bassAlter;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(bassStep, bassAlter);
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
        BassImpl other = (BassImpl) obj;
        return Objects.equals(bassStep, other.bassStep)
                && Objects.equals(bassAlter, other.bassAlter);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "BassImpl [bassStep=" + bassStep + ", bassAlter=" + bassAlter + "]";
    }
}
