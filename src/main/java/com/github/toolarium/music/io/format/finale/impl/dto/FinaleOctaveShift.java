/*
 * FinaleOctaveShift.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl.dto;

import java.util.Objects;


/**
 * A Finale octave line (smart shape octaveUp, octaveDown, twoOctaveUp or twoOctaveDown) on a staff:
 * the notes from the start to the end position sound the given number of semitones away from the
 * stored pitch (an 8va line raises them by 12, an 8vb line lowers them by 12).
 *
 * @author patrick
 */
public class FinaleOctaveShift {
    private final int staff;
    private final int startMeasure;
    private final int startEdu;
    private final int endMeasure;
    private final int endEdu;
    private final int semitones;


    /**
     * Constructor for FinaleOctaveShift.
     *
     * @param staff the staff
     * @param startMeasure the first measure
     * @param startEdu the start position within the first measure in EDU
     * @param endMeasure the last measure
     * @param endEdu the end position within the last measure in EDU (notes that start at it are included)
     * @param semitones the shift of the sounding pitch in semitones
     */
    public FinaleOctaveShift(int staff, int startMeasure, int startEdu, int endMeasure, int endEdu, int semitones) {
        this.staff = staff;
        this.startMeasure = startMeasure;
        this.startEdu = startEdu;
        this.endMeasure = endMeasure;
        this.endEdu = endEdu;
        this.semitones = semitones;
    }


    /**
     * Gets the staff.
     *
     * @return the staff
     */
    public int getStaff() {
        return staff;
    }


    /**
     * Gets the first measure.
     *
     * @return the first measure
     */
    public int getStartMeasure() {
        return startMeasure;
    }


    /**
     * Gets the start position within the first measure.
     *
     * @return the start position in EDU
     */
    public int getStartEdu() {
        return startEdu;
    }


    /**
     * Gets the last measure.
     *
     * @return the last measure
     */
    public int getEndMeasure() {
        return endMeasure;
    }


    /**
     * Gets the end position within the last measure.
     *
     * @return the end position in EDU
     */
    public int getEndEdu() {
        return endEdu;
    }


    /**
     * Gets the shift of the sounding pitch.
     *
     * @return the shift in semitones
     */
    public int getSemitones() {
        return semitones;
    }


    /**
     * Checks if a note at the given position is under the line.
     *
     * @param measure the measure
     * @param edu the position within the measure in EDU
     * @return true if the note is under the line
     */
    public boolean contains(int measure, long edu) {
        if (measure < startMeasure || measure > endMeasure) {
            return false;
        }
        if (measure == startMeasure && edu < startEdu) {
            return false;
        }
        return measure != endMeasure || edu <= endEdu;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(staff, startMeasure, startEdu, endMeasure, endEdu, semitones);
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
        FinaleOctaveShift other = (FinaleOctaveShift) obj;
        return staff == other.staff && startMeasure == other.startMeasure && startEdu == other.startEdu
                && endMeasure == other.endMeasure && endEdu == other.endEdu && semitones == other.semitones;
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FinaleOctaveShift [staff=" + staff + ", startMeasure=" + startMeasure + ", startEdu=" + startEdu + ", endMeasure=" + endMeasure
                + ", endEdu=" + endEdu + ", semitones=" + semitones + "]";
    }
}
