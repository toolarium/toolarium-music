/*
 * FinaleFrame.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl.dto;

import java.util.Objects;


/**
 * A Finale frame: the entries of one layer of a staff in one measure. A frame consists of one or more
 * frameSpec records with the same number; they give the first and the last entry of the linked entry
 * list and the start time of the first entry within the measure (a pick-up measure starts later than
 * the beginning of the measure).
 *
 * @author patrick
 */
public class FinaleFrame {
    private final int startEntry;
    private final int endEntry;
    private final int startTime;


    /**
     * Constructor for FinaleFrame.
     *
     * @param startEntry the first entry, or 0 if the frame has no entries
     * @param endEntry the last entry, or 0 if unknown
     * @param startTime the start time of the first entry within the measure in EDU
     */
    public FinaleFrame(int startEntry, int endEntry, int startTime) {
        this.startEntry = startEntry;
        this.endEntry = endEntry;
        this.startTime = startTime;
    }


    /**
     * Gets the first entry.
     *
     * @return the first entry, or 0 if the frame has no entries
     */
    public int getStartEntry() {
        return startEntry;
    }


    /**
     * Gets the last entry.
     *
     * @return the last entry, or 0 if unknown
     */
    public int getEndEntry() {
        return endEntry;
    }


    /**
     * Gets the start time of the first entry within the measure.
     *
     * @return the start time in EDU
     */
    public int getStartTime() {
        return startTime;
    }


    /**
     * Merges the values of another record of the same frame: values that are set win over unset ones.
     *
     * @param other the other record
     * @return the merged frame
     */
    public FinaleFrame merge(FinaleFrame other) {
        int start = startEntry;
        int end = endEntry;
        int time = startTime;
        if (start == 0) {
            start = other.startEntry;
            end = other.endEntry;
        }
        if (time == 0) {
            time = other.startTime;
        }
        return new FinaleFrame(start, end, time);
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(startEntry, endEntry, startTime);
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
        FinaleFrame other = (FinaleFrame) obj;
        return startEntry == other.startEntry && endEntry == other.endEntry && startTime == other.startTime;
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FinaleFrame [startEntry=" + startEntry + ", endEntry=" + endEntry + ", startTime=" + startTime + "]";
    }
}
