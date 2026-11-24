/*
 * DirectionData.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import java.util.Objects;


/**
 * A direction (dynamics, text, tempo, wedge and so on) with its position in a measure, as
 * collected by a format reader for the {@link ScoreBuilder}.
 *
 * @author patrick
 */
public class DirectionData {
    private final DirectionImpl direction;
    private final Fraction position;
    private final int staff;


    /**
     * Constructor for DirectionData.
     *
     * @param direction the direction
     * @param position the position from the measure start in whole notes
     * @param staff the staff within the part (1-based)
     */
    public DirectionData(DirectionImpl direction, Fraction position, int staff) {
        this.direction = direction;
        this.position = position;
        this.staff = staff;
    }


    /**
     * Gets the direction.
     *
     * @return the direction
     */
    public DirectionImpl getDirection() {
        return direction;
    }


    /**
     * Gets the position from the measure start.
     *
     * @return the position in whole notes
     */
    public Fraction getPosition() {
        return position;
    }


    /**
     * Gets the staff within the part.
     *
     * @return the staff (1-based)
     */
    public int getStaff() {
        return staff;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(direction, position, staff);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a DirectionData with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DirectionData other = (DirectionData) obj;
        return Objects.equals(direction, other.direction)
                && Objects.equals(position, other.position)
                && Objects.equals(staff, other.staff);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "DirectionData ["
                + "direction=" + direction
                + ", position=" + position
                + ", staff=" + staff
                + "]";
    }
}
