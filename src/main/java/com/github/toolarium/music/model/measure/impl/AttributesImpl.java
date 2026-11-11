/*
 * AttributesImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.measure.IAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Implements the {@link IAttributes} interface.
 *
 * @author patrick
 */
public class AttributesImpl implements IAttributes {

    @JacksonXmlProperty(localName = "divisions")
    private Integer divisions;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "key")
    private List<KeyImpl> keys;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "time")
    private List<TimeImpl> times;

    @JacksonXmlProperty(localName = "staves")
    private Integer staves;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "clef")
    private List<ClefImpl> clefs;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "transpose")
    private List<TransposeImpl> transposes;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "staff-details")
    private List<StaffDetailsImpl> staffDetails;

    @JacksonXmlProperty(localName = "instruments")
    private String instruments;


    /**
     * Constructor.
     */
    public AttributesImpl() {
        this.keys = new ArrayList<>();
        this.times = new ArrayList<>();
        this.clefs = new ArrayList<>();
        this.transposes = new ArrayList<>();
        this.staffDetails = new ArrayList<>();
    }


    /**
     * Gets the divisions.
     *
     * @return the divisions
     */
    @Override
    public Integer getDivisions() {
        return divisions;
    }


    /**
     * Sets the divisions.
     *
     * @param divisions the divisions
     */
    public void setDivisions(Integer divisions) {
        this.divisions = divisions;
    }


    /**
     * Gets the keys.
     *
     * @return the keys
     */
    @Override
    public List<KeyImpl> getKeys() {
        return keys;
    }


    /**
     * Sets the keys.
     *
     * @param keys the keys
     */
    public void setKeys(List<KeyImpl> keys) {
        this.keys = keys;
    }


    /**
     * Gets the times.
     *
     * @return the times
     */
    @Override
    public List<TimeImpl> getTimes() {
        return times;
    }


    /**
     * Sets the times.
     *
     * @param times the times
     */
    public void setTimes(List<TimeImpl> times) {
        this.times = times;
    }


    /**
     * Gets the staves.
     *
     * @return the staves
     */
    @Override
    public Integer getStaves() {
        return staves;
    }


    /**
     * Sets the staves.
     *
     * @param staves the staves
     */
    public void setStaves(Integer staves) {
        this.staves = staves;
    }


    /**
     * Gets the clefs.
     *
     * @return the clefs
     */
    @Override
    public List<ClefImpl> getClefs() {
        return clefs;
    }


    /**
     * Sets the clefs.
     *
     * @param clefs the clefs
     */
    public void setClefs(List<ClefImpl> clefs) {
        this.clefs = clefs;
    }


    /**
     * Gets the transposes.
     *
     * @return the transposes
     */
    @Override
    public List<TransposeImpl> getTransposes() {
        return transposes;
    }


    /**
     * Sets the transposes.
     *
     * @param transposes the transposes
     */
    public void setTransposes(List<TransposeImpl> transposes) {
        this.transposes = transposes;
    }


    /**
     * Gets the staff details.
     *
     * @return the staff details
     */
    @Override
    public List<StaffDetailsImpl> getStaffDetails() {
        return staffDetails;
    }


    /**
     * Sets the staff details.
     *
     * @param staffDetails the staff details
     */
    public void setStaffDetails(List<StaffDetailsImpl> staffDetails) {
        this.staffDetails = staffDetails;
    }


    /**
     * Gets the instruments.
     *
     * @return the instruments
     */
    @Override
    public String getInstruments() {
        return instruments;
    }


    /**
     * Sets the instruments.
     *
     * @param instruments the instruments
     */
    public void setInstruments(String instruments) {
        this.instruments = instruments;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(divisions, keys, times, staves, clefs, transposes, staffDetails, instruments);
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
        AttributesImpl other = (AttributesImpl) obj;
        return Objects.equals(divisions, other.divisions)
                && Objects.equals(keys, other.keys)
                && Objects.equals(times, other.times)
                && Objects.equals(staves, other.staves)
                && Objects.equals(clefs, other.clefs)
                && Objects.equals(transposes, other.transposes)
                && Objects.equals(staffDetails, other.staffDetails)
                && Objects.equals(instruments, other.instruments);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "AttributesImpl [divisions=" + divisions + ", keys=" + keys + ", times=" + times
                + ", staves=" + staves + ", clefs=" + clefs + ", transposes=" + transposes
                + ", staffDetails=" + staffDetails + ", instruments=" + instruments + "]";
    }
}
