/*
 * CapellaStaffInfo.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl.dto;

import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.Objects;


/**
 * A capella staff layout with its part, position in the part and the current time and key.
 *
 * @author patrick
 */
public class CapellaStaffInfo {
    private final String layout;
    private int partIndex;
    private int staffInPart;
    private ClefImpl defaultClef;
    private int beats = 4;
    private int beatType = 4;
    private String timeSymbol;
    private boolean timeSet;


    /**
     * Constructor for CapellaStaffInfo.
     *
     * @param layout the staff layout description
     */
    public CapellaStaffInfo(String layout) {
        this.layout = layout;
    }


    /**
     * Gets the staff layout description.
     *
     * @return the staff layout description
     */
    public String getLayout() {
        return layout;
    }


    /**
     * Gets the index of the part.
     *
     * @return the index of the part
     */
    public int getPartIndex() {
        return partIndex;
    }


    /**
     * Sets the index of the part.
     *
     * @param partIndex the index of the part
     */
    public void setPartIndex(int partIndex) {
        this.partIndex = partIndex;
    }


    /**
     * Gets the staff number within the part (1-based).
     *
     * @return the staff number within the part (1-based)
     */
    public int getStaffInPart() {
        return staffInPart;
    }


    /**
     * Sets the staff number within the part (1-based).
     *
     * @param staffInPart the staff number within the part (1-based)
     */
    public void setStaffInPart(int staffInPart) {
        this.staffInPart = staffInPart;
    }


    /**
     * Gets the default clef of the staff layout.
     *
     * @return the default clef of the staff layout
     */
    public ClefImpl getDefaultClef() {
        return defaultClef;
    }


    /**
     * Sets the default clef of the staff layout.
     *
     * @param defaultClef the default clef of the staff layout
     */
    public void setDefaultClef(ClefImpl defaultClef) {
        this.defaultClef = defaultClef;
    }


    /**
     * Gets the beats of the current time signature (0 for free time).
     *
     * @return the beats of the current time signature (0 for free time)
     */
    public int getBeats() {
        return beats;
    }


    /**
     * Sets the beats of the current time signature (0 for free time).
     *
     * @param beats the beats of the current time signature (0 for free time)
     */
    public void setBeats(int beats) {
        this.beats = beats;
    }


    /**
     * Gets the beat type of the current time signature.
     *
     * @return the beat type of the current time signature
     */
    public int getBeatType() {
        return beatType;
    }


    /**
     * Sets the beat type of the current time signature.
     *
     * @param beatType the beat type of the current time signature
     */
    public void setBeatType(int beatType) {
        this.beatType = beatType;
    }


    /**
     * Gets the symbol of the current time signature.
     *
     * @return the symbol of the current time signature
     */
    public String getTimeSymbol() {
        return timeSymbol;
    }


    /**
     * Sets the symbol of the current time signature.
     *
     * @param timeSymbol the symbol of the current time signature
     */
    public void setTimeSymbol(String timeSymbol) {
        this.timeSymbol = timeSymbol;
    }


    /**
     * Gets the flag for a time signature read in this staff.
     *
     * @return the flag for a time signature read in this staff
     */
    public boolean isTimeSet() {
        return timeSet;
    }


    /**
     * Sets the flag for a time signature read in this staff.
     *
     * @param timeSet the flag for a time signature read in this staff
     */
    public void setTimeSet(boolean timeSet) {
        this.timeSet = timeSet;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(layout, partIndex, staffInPart, defaultClef, beats, beatType, timeSymbol, timeSet);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a CapellaStaffInfo with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CapellaStaffInfo other = (CapellaStaffInfo) obj;
        return Objects.equals(layout, other.layout)
                && Objects.equals(partIndex, other.partIndex)
                && Objects.equals(staffInPart, other.staffInPart)
                && Objects.equals(defaultClef, other.defaultClef)
                && Objects.equals(beats, other.beats)
                && Objects.equals(beatType, other.beatType)
                && Objects.equals(timeSymbol, other.timeSymbol)
                && Objects.equals(timeSet, other.timeSet);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "CapellaStaffInfo ["
                + "layout=" + layout
                + ", partIndex=" + partIndex
                + ", staffInPart=" + staffInPart
                + ", defaultClef=" + defaultClef
                + ", beats=" + beats
                + ", beatType=" + beatType
                + ", timeSymbol=" + timeSymbol
                + ", timeSet=" + timeSet
                + "]";
    }
}
