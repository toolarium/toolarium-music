/*
 * CapellaSpan.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl.dto;

import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import java.util.Objects;


/**
 * An open slur or wedge that ends after a number of further notes.
 *
 * @author patrick
 */
public class CapellaSpan {
    private int remaining;
    private final int slurNumber;
    private final DirectionImpl stopDirection;


    /**
     * Constructor for CapellaSpan.
     *
     * @param remaining the number of further notes until the end
     * @param slurNumber the slur number (0 for a wedge)
     * @param stopDirection the direction that ends a wedge (null for a slur)
     */
    public CapellaSpan(int remaining, int slurNumber, DirectionImpl stopDirection) {
        this.remaining = remaining;
        this.slurNumber = slurNumber;
        this.stopDirection = stopDirection;
    }


    /**
     * Gets the number of further notes until the end.
     *
     * @return the number of further notes until the end
     */
    public int getRemaining() {
        return remaining;
    }


    /**
     * Sets the number of further notes until the end.
     *
     * @param remaining the number of further notes until the end
     */
    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }


    /**
     * Gets the slur number (0 for a wedge).
     *
     * @return the slur number (0 for a wedge)
     */
    public int getSlurNumber() {
        return slurNumber;
    }


    /**
     * Gets the direction that ends a wedge (null for a slur).
     *
     * @return the direction that ends a wedge (null for a slur)
     */
    public DirectionImpl getStopDirection() {
        return stopDirection;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(remaining, slurNumber, stopDirection);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a CapellaSpan with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CapellaSpan other = (CapellaSpan) obj;
        return Objects.equals(remaining, other.remaining)
                && Objects.equals(slurNumber, other.slurNumber)
                && Objects.equals(stopDirection, other.stopDirection);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "CapellaSpan ["
                + "remaining=" + remaining
                + ", slurNumber=" + slurNumber
                + ", stopDirection=" + stopDirection
                + "]";
    }
}
