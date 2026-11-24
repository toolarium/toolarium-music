/*
 * NoteData.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.model.notation.impl.PlayingTechniqueImpl;
import java.util.Objects;


/**
 * One note of an event, as collected by a format reader for the {@link ScoreBuilder}.
 *
 * @author patrick
 */
public class NoteData {
    private String step;
    private int alter;
    private int octave;
    private boolean unpitched;
    private String accidental;
    private boolean tieStart;
    private boolean tieStop;
    private String notehead;
    private PlayingTechniqueImpl technical;
    private boolean glissandoStart;
    private boolean glissandoStop;


    /**
     * Constructor for NoteData.
     */
    public NoteData() {
        // default constructor
    }


    /**
     * Constructor for NoteData.
     *
     * @param step the step (C to B)
     * @param alter the alteration in semitones
     * @param octave the octave (4 is the octave of middle C)
     */
    public NoteData(String step, int alter, int octave) {
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
     * Sets the step.
     *
     * @param step the step (C to B)
     */
    public void setStep(String step) {
        this.step = step;
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
     * Sets the alteration.
     *
     * @param alter the alteration in semitones
     */
    public void setAlter(int alter) {
        this.alter = alter;
    }


    /**
     * Gets the octave.
     *
     * @return the octave (4 is the octave of middle C)
     */
    public int getOctave() {
        return octave;
    }


    /**
     * Sets the octave.
     *
     * @param octave the octave (4 is the octave of middle C)
     */
    public void setOctave(int octave) {
        this.octave = octave;
    }


    /**
     * Checks if the note is unpitched (percussion). Step and octave are then the display position.
     *
     * @return true if unpitched
     */
    public boolean isUnpitched() {
        return unpitched;
    }


    /**
     * Sets the unpitched flag.
     *
     * @param unpitched true if unpitched
     */
    public void setUnpitched(boolean unpitched) {
        this.unpitched = unpitched;
    }


    /**
     * Gets the displayed accidental.
     *
     * @return the MusicXML accidental, for example sharp, or null
     */
    public String getAccidental() {
        return accidental;
    }


    /**
     * Sets the displayed accidental.
     *
     * @param accidental the MusicXML accidental, or null
     */
    public void setAccidental(String accidental) {
        this.accidental = accidental;
    }


    /**
     * Checks if a tie starts at this note.
     *
     * @return true if a tie starts
     */
    public boolean isTieStart() {
        return tieStart;
    }


    /**
     * Sets the tie start flag.
     *
     * @param tieStart true if a tie starts
     */
    public void setTieStart(boolean tieStart) {
        this.tieStart = tieStart;
    }


    /**
     * Checks if a tie ends at this note.
     *
     * @return true if a tie ends
     */
    public boolean isTieStop() {
        return tieStop;
    }


    /**
     * Sets the tie stop flag.
     *
     * @param tieStop true if a tie ends
     */
    public void setTieStop(boolean tieStop) {
        this.tieStop = tieStop;
    }


    /**
     * Gets the notehead.
     *
     * @return the MusicXML notehead, for example x, or null
     */
    public String getNotehead() {
        return notehead;
    }


    /**
     * Sets the notehead.
     *
     * @param notehead the MusicXML notehead, or null
     */
    public void setNotehead(String notehead) {
        this.notehead = notehead;
    }


    /**
     * Gets the technical notations of this note (string, fret, hammer-on and so on).
     *
     * @return the technical notations, or null
     */
    public PlayingTechniqueImpl getTechnical() {
        return technical;
    }


    /**
     * Gets the technical notations of this note, creating them if needed.
     *
     * @return the technical notations
     */
    public PlayingTechniqueImpl technical() {
        if (technical == null) {
            technical = new PlayingTechniqueImpl();
        }
        return technical;
    }


    /**
     * Checks if a glissando (or slide) starts at this note.
     *
     * @return true if a glissando starts
     */
    public boolean isGlissandoStart() {
        return glissandoStart;
    }


    /**
     * Sets the glissando start flag.
     *
     * @param glissandoStart true if a glissando starts
     */
    public void setGlissandoStart(boolean glissandoStart) {
        this.glissandoStart = glissandoStart;
    }


    /**
     * Checks if a glissando (or slide) ends at this note.
     *
     * @return true if a glissando ends
     */
    public boolean isGlissandoStop() {
        return glissandoStop;
    }


    /**
     * Sets the glissando stop flag.
     *
     * @param glissandoStop true if a glissando ends
     */
    public void setGlissandoStop(boolean glissandoStop) {
        this.glissandoStop = glissandoStop;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(step, alter, octave, unpitched, accidental, tieStart, tieStop, notehead, technical, glissandoStart, glissandoStop);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a NoteData with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NoteData other = (NoteData) obj;
        return Objects.equals(step, other.step)
                && Objects.equals(alter, other.alter)
                && Objects.equals(octave, other.octave)
                && Objects.equals(unpitched, other.unpitched)
                && Objects.equals(accidental, other.accidental)
                && Objects.equals(tieStart, other.tieStart)
                && Objects.equals(tieStop, other.tieStop)
                && Objects.equals(notehead, other.notehead)
                && Objects.equals(technical, other.technical)
                && Objects.equals(glissandoStart, other.glissandoStart)
                && Objects.equals(glissandoStop, other.glissandoStop);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "NoteData ["
                + "step=" + step
                + ", alter=" + alter
                + ", octave=" + octave
                + ", unpitched=" + unpitched
                + ", accidental=" + accidental
                + ", tieStart=" + tieStart
                + ", tieStop=" + tieStop
                + ", notehead=" + notehead
                + ", technical=" + technical
                + ", glissandoStart=" + glissandoStart
                + ", glissandoStop=" + glissandoStop
                + "]";
    }
}
