/*
 * ClefChange.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.Objects;


/**
 * A clef change at a position of a staff.
 *
 * @author patrick
 */
public class ClefChange {
    private final int staff;
    private final Fraction position;
    private final ClefImpl clef;


    /**
     * Constructor for ClefChange.
     *
     * @param staff the staff (1-based)
     * @param position the position from the measure start in whole notes
     * @param clef the clef (sign, line and octave change)
     */
    public ClefChange(int staff, Fraction position, ClefImpl clef) {
        this.staff = staff;
        this.position = position;
        this.clef = clef;
    }


    /**
     * Gets the staff.
     *
     * @return the staff (1-based)
     */
    public int getStaff() {
        return staff;
    }


    /**
     * Gets the position.
     *
     * @return the position from the measure start in whole notes
     */
    public Fraction getPosition() {
        return position;
    }


    /**
     * Gets the clef.
     *
     * @return the clef
     */
    public ClefImpl getClef() {
        return clef;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(staff, position, clef);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a ClefChange with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ClefChange other = (ClefChange) obj;
        return Objects.equals(staff, other.staff)
                && Objects.equals(position, other.position)
                && Objects.equals(clef, other.clef);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "ClefChange ["
                + "staff=" + staff
                + ", position=" + position
                + ", clef=" + clef
                + "]";
    }
}
