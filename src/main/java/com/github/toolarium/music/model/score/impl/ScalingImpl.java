/*
 * ScalingImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.score.IScaling;
import java.util.Objects;

/**
 * Implements the {@link IScaling} interface.
 *
 * @author patrick
 */
public class ScalingImpl implements IScaling {

    @JacksonXmlProperty(localName = "millimeters")
    private Double millimeters;

    @JacksonXmlProperty(localName = "tenths")
    private Double tenths;


    /**
     * Gets the millimeters.
     *
     * @return the millimeters
     */
    @Override
    public Double getMillimeters() {
        return millimeters;
    }


    /**
     * Sets the millimeters.
     *
     * @param millimeters the millimeters
     */
    public void setMillimeters(Double millimeters) {
        this.millimeters = millimeters;
    }


    /**
     * Gets the tenths.
     *
     * @return the tenths
     */
    @Override
    public Double getTenths() {
        return tenths;
    }


    /**
     * Sets the tenths.
     *
     * @param tenths the tenths
     */
    public void setTenths(Double tenths) {
        this.tenths = tenths;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(millimeters, tenths);
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
        ScalingImpl other = (ScalingImpl) obj;
        return Objects.equals(millimeters, other.millimeters)
                && Objects.equals(tenths, other.tenths);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "ScalingImpl [millimeters=" + millimeters + ", tenths=" + tenths + "]";
    }
}
