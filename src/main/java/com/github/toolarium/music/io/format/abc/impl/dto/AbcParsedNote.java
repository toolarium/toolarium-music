/*
 * AbcParsedNote.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.Objects;


/**
 * The result of parsing one ABC note: pitch, length multiplier and tie.
 *
 * @author patrick
 */
public class AbcParsedNote {
    private String accidental;
    private String step;
    private int octave;
    private Fraction multiplier;
    private boolean tie;
    private int end;


    /**
     * Constructor for AbcParsedNote.
     */
    public AbcParsedNote() {
        // default constructor
    }


    /**
     * Gets the accidental (^, _, = and so on), or null.
     *
     * @return the accidental (^, _, = and so on), or null
     */
    public String getAccidental() {
        return accidental;
    }


    /**
     * Sets the accidental (^, _, = and so on), or null.
     *
     * @param accidental the accidental (^, _, = and so on), or null
     */
    public void setAccidental(String accidental) {
        this.accidental = accidental;
    }


    /**
     * Gets the step (C to B).
     *
     * @return the step (C to B)
     */
    public String getStep() {
        return step;
    }


    /**
     * Sets the step (C to B).
     *
     * @param step the step (C to B)
     */
    public void setStep(String step) {
        this.step = step;
    }


    /**
     * Gets the octave (4 is the octave of middle C).
     *
     * @return the octave (4 is the octave of middle C)
     */
    public int getOctave() {
        return octave;
    }


    /**
     * Sets the octave (4 is the octave of middle C).
     *
     * @param octave the octave (4 is the octave of middle C)
     */
    public void setOctave(int octave) {
        this.octave = octave;
    }


    /**
     * Gets the length multiplier of the unit note length.
     *
     * @return the length multiplier of the unit note length
     */
    public Fraction getMultiplier() {
        return multiplier;
    }


    /**
     * Sets the length multiplier of the unit note length.
     *
     * @param multiplier the length multiplier of the unit note length
     */
    public void setMultiplier(Fraction multiplier) {
        this.multiplier = multiplier;
    }


    /**
     * Gets the flag for a tie to the next note.
     *
     * @return the flag for a tie to the next note
     */
    public boolean isTie() {
        return tie;
    }


    /**
     * Sets the flag for a tie to the next note.
     *
     * @param tie the flag for a tie to the next note
     */
    public void setTie(boolean tie) {
        this.tie = tie;
    }


    /**
     * Gets the index after the note in the line.
     *
     * @return the index after the note in the line
     */
    public int getEnd() {
        return end;
    }


    /**
     * Sets the index after the note in the line.
     *
     * @param end the index after the note in the line
     */
    public void setEnd(int end) {
        this.end = end;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(accidental, step, octave, multiplier, tie, end);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a AbcParsedNote with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbcParsedNote other = (AbcParsedNote) obj;
        return Objects.equals(accidental, other.accidental)
                && Objects.equals(step, other.step)
                && Objects.equals(octave, other.octave)
                && Objects.equals(multiplier, other.multiplier)
                && Objects.equals(tie, other.tie)
                && Objects.equals(end, other.end);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "AbcParsedNote ["
                + "accidental=" + accidental
                + ", step=" + step
                + ", octave=" + octave
                + ", multiplier=" + multiplier
                + ", tie=" + tie
                + ", end=" + end
                + "]";
    }
}
