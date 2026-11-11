/*
 * MetronomeImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.direction.IMetronome;
import java.util.Objects;

/**
 * Implements the {@link IMetronome} interface.
 *
 * @author patrick
 */
public class MetronomeImpl implements IMetronome {

    @JacksonXmlProperty(localName = "beat-unit")
    private String beatUnit;

    @JacksonXmlProperty(localName = "beat-unit-dot")
    private String beatUnitDot;

    @JacksonXmlProperty(localName = "per-minute")
    private String perMinute;

    @JacksonXmlProperty(isAttribute = true, localName = "parentheses")
    private String parentheses;


    /**
     * Gets the beat unit.
     *
     * @return the beat unit
     */
    @Override
    public String getBeatUnit() {
        return beatUnit;
    }


    /**
     * Sets the beat unit.
     *
     * @param beatUnit the beat unit
     */
    public void setBeatUnit(String beatUnit) {
        this.beatUnit = beatUnit;
    }


    /**
     * Gets the beat unit dot.
     *
     * @return the beat unit dot
     */
    @Override
    public String getBeatUnitDot() {
        return beatUnitDot;
    }


    /**
     * Sets the beat unit dot.
     *
     * @param beatUnitDot the beat unit dot
     */
    public void setBeatUnitDot(String beatUnitDot) {
        this.beatUnitDot = beatUnitDot;
    }


    /**
     * Gets the per minute.
     *
     * @return the per minute
     */
    @Override
    public String getPerMinute() {
        return perMinute;
    }


    /**
     * Sets the per minute.
     *
     * @param perMinute the per minute
     */
    public void setPerMinute(String perMinute) {
        this.perMinute = perMinute;
    }


    /**
     * Gets the parentheses.
     *
     * @return the parentheses
     */
    @Override
    public String getParentheses() {
        return parentheses;
    }


    /**
     * Sets the parentheses.
     *
     * @param parentheses the parentheses
     */
    public void setParentheses(String parentheses) {
        this.parentheses = parentheses;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(beatUnit, beatUnitDot, perMinute, parentheses);
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
        MetronomeImpl other = (MetronomeImpl) obj;
        return Objects.equals(beatUnit, other.beatUnit)
                && Objects.equals(beatUnitDot, other.beatUnitDot)
                && Objects.equals(perMinute, other.perMinute)
                && Objects.equals(parentheses, other.parentheses);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "MetronomeImpl [beatUnit=" + beatUnit
                + ", beatUnitDot=" + beatUnitDot
                + ", perMinute=" + perMinute
                + ", parentheses=" + parentheses + "]";
    }
}
