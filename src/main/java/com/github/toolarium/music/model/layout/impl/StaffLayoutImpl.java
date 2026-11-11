/*
 * StaffLayoutImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.layout.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.layout.IStaffLayout;
import java.util.Objects;

/**
 * Implements the {@link IStaffLayout} interface.
 *
 * @author patrick
 */
public class StaffLayoutImpl implements IStaffLayout {

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private Integer number;

    @JacksonXmlProperty(localName = "staff-distance")
    private Double staffDistance;


    /**
     * Constructor.
     */
    public StaffLayoutImpl() {
    }


    /**
     * Gets the number.
     *
     * @return the number
     */
    @Override
    public Integer getNumber() {
        return number;
    }


    /**
     * Sets the number.
     *
     * @param number the number
     */
    public void setNumber(Integer number) {
        this.number = number;
    }


    /**
     * Gets the staff distance.
     *
     * @return the staff distance
     */
    @Override
    public Double getStaffDistance() {
        return staffDistance;
    }


    /**
     * Sets the staff distance.
     *
     * @param staffDistance the staff distance
     */
    public void setStaffDistance(Double staffDistance) {
        this.staffDistance = staffDistance;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(number, staffDistance);
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
        StaffLayoutImpl other = (StaffLayoutImpl) obj;
        return Objects.equals(number, other.number)
                && Objects.equals(staffDistance, other.staffDistance);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "StaffLayoutImpl [number=" + number + ", staffDistance=" + staffDistance + "]";
    }
}
