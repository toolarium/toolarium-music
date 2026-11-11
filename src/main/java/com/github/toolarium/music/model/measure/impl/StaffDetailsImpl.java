/*
 * StaffDetailsImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.measure.IStaffDetails;
import java.util.Objects;


/**
 * Implements the {@link IStaffDetails} interface.
 *
 * @author patrick
 */
public class StaffDetailsImpl implements IStaffDetails {

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private Integer number;

    @JacksonXmlProperty(localName = "staff-lines")
    private Integer staffLines;

    @JacksonXmlProperty(localName = "staff-type")
    private String staffType;


    /**
     * Constructor.
     */
    public StaffDetailsImpl() {
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
     * Gets the staff lines.
     *
     * @return the staff lines
     */
    @Override
    public Integer getStaffLines() {
        return staffLines;
    }


    /**
     * Sets the staff lines.
     *
     * @param staffLines the staff lines
     */
    public void setStaffLines(Integer staffLines) {
        this.staffLines = staffLines;
    }


    /**
     * Gets the staff type.
     *
     * @return the staff type
     */
    @Override
    public String getStaffType() {
        return staffType;
    }


    /**
     * Sets the staff type.
     *
     * @param staffType the staff type
     */
    public void setStaffType(String staffType) {
        this.staffType = staffType;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(number, staffLines, staffType);
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
        StaffDetailsImpl other = (StaffDetailsImpl) obj;
        return Objects.equals(number, other.number)
                && Objects.equals(staffLines, other.staffLines)
                && Objects.equals(staffType, other.staffType);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "StaffDetailsImpl [number=" + number + ", staffLines=" + staffLines + ", staffType=" + staffType + "]";
    }
}
