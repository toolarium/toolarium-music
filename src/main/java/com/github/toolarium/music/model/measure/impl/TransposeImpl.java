/*
 * TransposeImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.measure.ITranspose;
import java.util.Objects;

/**
 * Implements the {@link ITranspose} interface.
 *
 * @author patrick
 */
public class TransposeImpl implements ITranspose {

    @JacksonXmlProperty(localName = "diatonic")
    private Integer diatonic;

    @JacksonXmlProperty(localName = "chromatic")
    private Integer chromatic;

    @JacksonXmlProperty(localName = "octave-change")
    private Integer octaveChange;

    @JacksonXmlProperty(localName = "double")
    private String doubleApply;


    /**
     * Gets the diatonic.
     *
     * @return the diatonic
     */
    @Override
    public Integer getDiatonic() {
        return diatonic;
    }


    /**
     * Sets the diatonic.
     *
     * @param diatonic the diatonic
     */
    public void setDiatonic(Integer diatonic) {
        this.diatonic = diatonic;
    }


    /**
     * Gets the chromatic.
     *
     * @return the chromatic
     */
    @Override
    public Integer getChromatic() {
        return chromatic;
    }


    /**
     * Sets the chromatic.
     *
     * @param chromatic the chromatic
     */
    public void setChromatic(Integer chromatic) {
        this.chromatic = chromatic;
    }


    /**
     * Gets the octave change.
     *
     * @return the octave change
     */
    @Override
    public Integer getOctaveChange() {
        return octaveChange;
    }


    /**
     * Sets the octave change.
     *
     * @param octaveChange the octave change
     */
    public void setOctaveChange(Integer octaveChange) {
        this.octaveChange = octaveChange;
    }


    /**
     * Gets the double apply.
     *
     * @return the double apply
     */
    @Override
    public String getDoubleApply() {
        return doubleApply;
    }


    /**
     * Sets the double apply.
     *
     * @param doubleApply the double apply
     */
    public void setDoubleApply(String doubleApply) {
        this.doubleApply = doubleApply;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(diatonic, chromatic, octaveChange, doubleApply);
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
        TransposeImpl other = (TransposeImpl) obj;
        return Objects.equals(diatonic, other.diatonic)
                && Objects.equals(chromatic, other.chromatic)
                && Objects.equals(octaveChange, other.octaveChange)
                && Objects.equals(doubleApply, other.doubleApply);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "TransposeImpl [diatonic=" + diatonic + ", chromatic=" + chromatic + ", octaveChange=" + octaveChange + ", doubleApply=" + doubleApply + "]";
    }
}
