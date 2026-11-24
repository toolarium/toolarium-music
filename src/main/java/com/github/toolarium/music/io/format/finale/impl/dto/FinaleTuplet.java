/*
 * FinaleTuplet.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl.dto;

import java.util.Objects;


/**
 * A Finale tuplet definition: {@code symbolicNumber} notes of {@code symbolicDuration} EDU take the
 * time of {@code referenceNumber} notes of {@code referenceDuration} EDU (a triplet of eighths is
 * 3 x 512 in the time of 2 x 512).
 *
 * @author patrick
 */
public class FinaleTuplet {
    private final int symbolicNumber;
    private final int symbolicDuration;
    private final int referenceNumber;
    private final int referenceDuration;


    /**
     * Constructor for FinaleTuplet.
     *
     * @param symbolicNumber the number of notes
     * @param symbolicDuration the duration of one note in EDU
     * @param referenceNumber the number of notes whose time the tuplet takes
     * @param referenceDuration the duration of one of these notes in EDU
     */
    public FinaleTuplet(int symbolicNumber, int symbolicDuration, int referenceNumber, int referenceDuration) {
        this.symbolicNumber = symbolicNumber;
        this.symbolicDuration = symbolicDuration;
        this.referenceNumber = referenceNumber;
        this.referenceDuration = referenceDuration;
    }


    /**
     * Gets the number of notes.
     *
     * @return the number of notes
     */
    public int getSymbolicNumber() {
        return symbolicNumber;
    }


    /**
     * Gets the duration of one note in EDU.
     *
     * @return the duration
     */
    public int getSymbolicDuration() {
        return symbolicDuration;
    }


    /**
     * Gets the number of notes whose time the tuplet takes.
     *
     * @return the number of notes
     */
    public int getReferenceNumber() {
        return referenceNumber;
    }


    /**
     * Gets the duration of one of the notes whose time the tuplet takes, in EDU.
     *
     * @return the duration
     */
    public int getReferenceDuration() {
        return referenceDuration;
    }


    /**
     * Gets the nominal length of the tuplet: the sum of the written durations of its notes in EDU.
     *
     * @return the nominal length
     */
    public long getSymbolicLength() {
        return (long) symbolicNumber * symbolicDuration;
    }


    /**
     * Gets the real length of the tuplet in EDU.
     *
     * @return the real length
     */
    public long getReferenceLength() {
        return (long) referenceNumber * referenceDuration;
    }


    /**
     * Checks if the definition is usable (all values positive).
     *
     * @return true if valid
     */
    public boolean isValid() {
        return symbolicNumber > 0 && symbolicDuration > 0 && referenceNumber > 0 && referenceDuration > 0;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(symbolicNumber, symbolicDuration, referenceNumber, referenceDuration);
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
        FinaleTuplet other = (FinaleTuplet) obj;
        return symbolicNumber == other.symbolicNumber && symbolicDuration == other.symbolicDuration
                && referenceNumber == other.referenceNumber && referenceDuration == other.referenceDuration;
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FinaleTuplet [symbolicNumber=" + symbolicNumber + ", symbolicDuration=" + symbolicDuration
                + ", referenceNumber=" + referenceNumber + ", referenceDuration=" + referenceDuration + "]";
    }
}
