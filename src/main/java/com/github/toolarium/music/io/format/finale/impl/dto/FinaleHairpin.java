/*
 * FinaleHairpin.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl.dto;

import java.util.Objects;


/**
 * A Finale hairpin (crescendo or decrescendo smart shape) from its start to its end position.
 *
 * @author patrick
 */
public class FinaleHairpin {
    private final int staff;
    private final int startMeasure;
    private final int startEdu;
    private final int endMeasure;
    private final int endEdu;
    private final boolean crescendo;
    private final int number;


    /**
     * Constructor for FinaleHairpin.
     *
     * @param staff the staff
     * @param startMeasure the measure where the hairpin starts
     * @param startEdu the start position within the start measure in EDU
     * @param endMeasure the measure where the hairpin ends
     * @param endEdu the end position within the end measure in EDU
     * @param crescendo true for a crescendo, false for a decrescendo
     * @param number the number that connects the start and the end
     */
    public FinaleHairpin(int staff, int startMeasure, int startEdu, int endMeasure, int endEdu, boolean crescendo, int number) {
        this.staff = staff;
        this.startMeasure = startMeasure;
        this.startEdu = startEdu;
        this.endMeasure = endMeasure;
        this.endEdu = endEdu;
        this.crescendo = crescendo;
        this.number = number;
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
     * Gets the measure where the hairpin starts.
     *
     * @return the start measure
     */
    public int getStartMeasure() {
        return startMeasure;
    }


    /**
     * Gets the start position within the start measure.
     *
     * @return the start position in EDU
     */
    public int getStartEdu() {
        return startEdu;
    }


    /**
     * Gets the measure where the hairpin ends.
     *
     * @return the end measure
     */
    public int getEndMeasure() {
        return endMeasure;
    }


    /**
     * Gets the end position within the end measure.
     *
     * @return the end position in EDU
     */
    public int getEndEdu() {
        return endEdu;
    }


    /**
     * Tells whether the hairpin is a crescendo.
     *
     * @return true for a crescendo, false for a decrescendo
     */
    public boolean isCrescendo() {
        return crescendo;
    }


    /**
     * Gets the number that connects the start and the end.
     *
     * @return the number
     */
    public int getNumber() {
        return number;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(staff, startMeasure, startEdu, endMeasure, endEdu, crescendo, number);
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
        FinaleHairpin other = (FinaleHairpin) obj;
        return staff == other.staff && startMeasure == other.startMeasure && startEdu == other.startEdu && endMeasure == other.endMeasure
                && endEdu == other.endEdu && crescendo == other.crescendo && number == other.number;
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FinaleHairpin [staff=" + staff + ", startMeasure=" + startMeasure + ", startEdu=" + startEdu + ", endMeasure=" + endMeasure
                + ", endEdu=" + endEdu + ", crescendo=" + crescendo + ", number=" + number + "]";
    }
}
