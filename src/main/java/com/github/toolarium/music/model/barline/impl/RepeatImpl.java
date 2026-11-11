/*
 * RepeatImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.barline.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.barline.IRepeat;
import java.util.Objects;

/**
 * Implements the {@link IRepeat} interface.
 *
 * @author patrick
 */
public class RepeatImpl implements IRepeat {

    @JacksonXmlProperty(isAttribute = true, localName = "direction")
    private String direction;

    @JacksonXmlProperty(isAttribute = true, localName = "times")
    private Integer times;

    @JacksonXmlProperty(isAttribute = true, localName = "winged")
    private String winged;


    /**
     * Gets the direction.
     *
     * @return the direction
     */
    @Override
    public String getDirection() {
        return direction;
    }


    /**
     * Sets the direction.
     *
     * @param direction the direction
     */
    public void setDirection(String direction) {
        this.direction = direction;
    }


    /**
     * Gets the times.
     *
     * @return the times
     */
    @Override
    public Integer getTimes() {
        return times;
    }


    /**
     * Sets the times.
     *
     * @param times the times
     */
    public void setTimes(Integer times) {
        this.times = times;
    }


    /**
     * Gets the winged.
     *
     * @return the winged
     */
    @Override
    public String getWinged() {
        return winged;
    }


    /**
     * Sets the winged.
     *
     * @param winged the winged
     */
    public void setWinged(String winged) {
        this.winged = winged;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(direction, times, winged);
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
        RepeatImpl other = (RepeatImpl) obj;
        return Objects.equals(direction, other.direction)
                && Objects.equals(times, other.times)
                && Objects.equals(winged, other.winged);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "RepeatImpl [direction=" + direction + ", times=" + times + ", winged=" + winged + "]";
    }
}
