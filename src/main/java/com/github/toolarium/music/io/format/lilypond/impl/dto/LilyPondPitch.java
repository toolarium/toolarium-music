/*
 * LilyPondPitch.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl.dto;

import java.util.Objects;


/**
 * A pitch of a LilyPond note: the step (C to B), the alteration in semitones and the octave (MusicXML octave: 4 is
 * the octave of the middle C, which LilyPond writes as c').
 *
 * @author patrick
 */
public class LilyPondPitch {
    private static final String STEPS = "CDEFGAB";
    private static final int STEPS_PER_OCTAVE = 7;
    private final String step;
    private final int alter;
    private final int octave;


    /**
     * Constructor for LilyPondPitch.
     *
     * @param step the step (C, D, E, F, G, A or B)
     * @param alter the alteration in semitones (-2 to 2)
     * @param octave the octave (4 is the octave of the middle C)
     */
    public LilyPondPitch(String step, int alter, int octave) {
        this.step = step;
        this.alter = alter;
        this.octave = octave;
    }


    /**
     * Gets the step.
     *
     * @return the step (C to B)
     */
    public String getStep() {
        return step;
    }


    /**
     * Gets the alteration.
     *
     * @return the alteration in semitones
     */
    public int getAlter() {
        return alter;
    }


    /**
     * Gets the octave.
     *
     * @return the octave (4 is the octave of the middle C)
     */
    public int getOctave() {
        return octave;
    }


    /**
     * Gets the diatonic position: the number of steps above C0.
     *
     * @return the diatonic position
     */
    public int getDiatonic() {
        return octave * STEPS_PER_OCTAVE + STEPS.indexOf(step);
    }


    /**
     * Creates a pitch from a diatonic position and an alteration.
     *
     * @param diatonic the number of steps above C0
     * @param alter the alteration in semitones
     * @return the pitch
     */
    public static LilyPondPitch ofDiatonic(int diatonic, int alter) {
        return new LilyPondPitch(String.valueOf(STEPS.charAt(Math.floorMod(diatonic, STEPS_PER_OCTAVE))), alter,
                Math.floorDiv(diatonic, STEPS_PER_OCTAVE));
    }


    /**
     * Returns a hash code over all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(step, alter, octave);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a LilyPondPitch with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LilyPondPitch other = (LilyPondPitch) obj;
        return alter == other.alter && octave == other.octave && Objects.equals(step, other.step);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "LilyPondPitch ["
                + "step=" + step
                + ", alter=" + alter
                + ", octave=" + octave
                + "]";
    }
}
