/*
 * LilyPondEvent.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * A note, chord or rest of a LilyPond voice with its position in the voice and its marks.
 *
 * @author patrick
 */
public class LilyPondEvent {
    private final Fraction start;
    private final Fraction duration;
    private final List<LilyPondPitch> pitches = new ArrayList<>();
    private final List<Integer> slurStarts = new ArrayList<>();
    private final List<Integer> slurStops = new ArrayList<>();
    private final List<DirectionImpl> directions = new ArrayList<>();
    private final List<String> marks = new ArrayList<>();
    private final List<LyricImpl> lyrics = new ArrayList<>();
    private final List<LilyPondPitch> tiedPitches = new ArrayList<>();
    private boolean invisible;
    private boolean measureRest;
    private boolean grace;
    private boolean graceSlash;
    private boolean tieStart;
    private int tupletActual;
    private int tupletNormal;
    private boolean tupletStart;
    private boolean tupletStop;


    /**
     * Constructor for LilyPondEvent.
     *
     * @param start the position in the voice, in whole notes
     * @param duration the sounding length in whole notes (the nominal length for a grace note)
     */
    public LilyPondEvent(Fraction start, Fraction duration) {
        this.start = start;
        this.duration = duration;
    }


    /**
     * Gets the position in the voice.
     *
     * @return the position in whole notes
     */
    public Fraction getStart() {
        return start;
    }


    /**
     * Gets the length.
     *
     * @return the sounding length in whole notes (the nominal length for a grace note)
     */
    public Fraction getDuration() {
        return duration;
    }


    /**
     * Gets the pitches: one for a note, several for a chord, none for a rest.
     *
     * @return the pitches
     */
    public List<LilyPondPitch> getPitches() {
        return pitches;
    }


    /**
     * Checks whether the event is a rest.
     *
     * @return true for a rest
     */
    public boolean isRest() {
        return pitches.isEmpty();
    }


    /**
     * Gets the numbers of the slurs that start on the event.
     *
     * @return the slur numbers
     */
    public List<Integer> getSlurStarts() {
        return slurStarts;
    }


    /**
     * Gets the numbers of the slurs that end on the event.
     *
     * @return the slur numbers
     */
    public List<Integer> getSlurStops() {
        return slurStops;
    }


    /**
     * Gets the directions at the event (dynamics, hairpins, texts).
     *
     * @return the directions
     */
    public List<DirectionImpl> getDirections() {
        return directions;
    }


    /**
     * Gets the marks of the event: articulations and ornaments by their MusicXML name (staccato, accent, fermata, ...).
     *
     * @return the marks
     */
    public List<String> getMarks() {
        return marks;
    }


    /**
     * Gets the lyrics of the event.
     *
     * @return the lyrics
     */
    public List<LyricImpl> getLyrics() {
        return lyrics;
    }


    /**
     * Checks whether the event is invisible (a spacer rest).
     *
     * @return true for an invisible rest
     */
    public boolean isInvisible() {
        return invisible;
    }


    /**
     * Sets whether the event is invisible (a spacer rest).
     *
     * @param invisible true for an invisible rest
     */
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }


    /**
     * Checks whether the event is a full measure rest (R).
     *
     * @return true for a full measure rest
     */
    public boolean isMeasureRest() {
        return measureRest;
    }


    /**
     * Sets whether the event is a full measure rest (R).
     *
     * @param measureRest true for a full measure rest
     */
    public void setMeasureRest(boolean measureRest) {
        this.measureRest = measureRest;
    }


    /**
     * Checks whether the event is a grace note.
     *
     * @return true for a grace note
     */
    public boolean isGrace() {
        return grace;
    }


    /**
     * Sets whether the event is a grace note.
     *
     * @param grace true for a grace note
     * @param slash true for a grace note with a slash (acciaccatura)
     */
    public void setGrace(boolean grace, boolean slash) {
        this.grace = grace;
        this.graceSlash = slash;
    }


    /**
     * Checks whether the grace note has a slash.
     *
     * @return true for an acciaccatura
     */
    public boolean isGraceSlash() {
        return graceSlash;
    }


    /**
     * Checks whether a tie starts on the event.
     *
     * @return true if the notes are tied to the next event
     */
    public boolean isTieStart() {
        return tieStart;
    }


    /**
     * Sets whether a tie starts on the event.
     *
     * @param tieStart true if the notes are tied to the next event
     */
    public void setTieStart(boolean tieStart) {
        this.tieStart = tieStart;
    }


    /**
     * Gets the pitches of a chord that are tied to the next event (a tie inside the chord, for example &lt;c~ e&gt;).
     *
     * @return the tied pitches
     */
    public List<LilyPondPitch> getTiedPitches() {
        return tiedPitches;
    }


    /**
     * Checks whether a pitch of the event is tied to the next event.
     *
     * @param pitch the pitch
     * @return true if the event is tied (all pitches) or the pitch is tied
     */
    public boolean isTied(LilyPondPitch pitch) {
        return tieStart || tiedPitches.contains(pitch);
    }


    /**
     * Checks whether the event or one of its pitches is tied to the next event.
     *
     * @return true if a pitch is tied
     */
    public boolean hasTie() {
        return tieStart || !tiedPitches.isEmpty();
    }


    /**
     * Sets the tuplet of the event.
     *
     * @param actual the number of notes played (for example 3)
     * @param normal the number of notes in the time of which they are played (for example 2)
     */
    public void setTuplet(int actual, int normal) {
        this.tupletActual = actual;
        this.tupletNormal = normal;
    }


    /**
     * Gets the number of notes of the tuplet.
     *
     * @return the actual number, 0 without tuplet
     */
    public int getTupletActual() {
        return tupletActual;
    }


    /**
     * Gets the number of notes in the time of which the tuplet is played.
     *
     * @return the normal number, 0 without tuplet
     */
    public int getTupletNormal() {
        return tupletNormal;
    }


    /**
     * Checks whether the event is the first of a tuplet.
     *
     * @return true for the first event
     */
    public boolean isTupletStart() {
        return tupletStart;
    }


    /**
     * Sets whether the event is the first of a tuplet.
     *
     * @param tupletStart true for the first event
     */
    public void setTupletStart(boolean tupletStart) {
        this.tupletStart = tupletStart;
    }


    /**
     * Checks whether the event is the last of a tuplet.
     *
     * @return true for the last event
     */
    public boolean isTupletStop() {
        return tupletStop;
    }


    /**
     * Sets whether the event is the last of a tuplet.
     *
     * @param tupletStop true for the last event
     */
    public void setTupletStop(boolean tupletStop) {
        this.tupletStop = tupletStop;
    }


    /**
     * Returns a hash code over all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(start, duration, pitches, slurStarts, slurStops, directions, marks, lyrics, tiedPitches, invisible, measureRest, grace,
                graceSlash, tieStart, tupletActual, tupletNormal, tupletStart, tupletStop);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a LilyPondEvent with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LilyPondEvent other = (LilyPondEvent) obj;
        return invisible == other.invisible && measureRest == other.measureRest && grace == other.grace && graceSlash == other.graceSlash
                && tieStart == other.tieStart && tupletActual == other.tupletActual && tupletNormal == other.tupletNormal
                && tupletStart == other.tupletStart && tupletStop == other.tupletStop && Objects.equals(start, other.start)
                && Objects.equals(duration, other.duration) && Objects.equals(pitches, other.pitches)
                && Objects.equals(slurStarts, other.slurStarts) && Objects.equals(slurStops, other.slurStops)
                && Objects.equals(directions, other.directions) && Objects.equals(marks, other.marks) && Objects.equals(lyrics, other.lyrics)
                && Objects.equals(tiedPitches, other.tiedPitches);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "LilyPondEvent ["
                + "start=" + start
                + ", duration=" + duration
                + ", pitches=" + pitches
                + ", slurStarts=" + slurStarts
                + ", slurStops=" + slurStops
                + ", directions=" + directions
                + ", marks=" + marks
                + ", lyrics=" + lyrics
                + ", tiedPitches=" + tiedPitches
                + ", invisible=" + invisible
                + ", measureRest=" + measureRest
                + ", grace=" + grace
                + ", graceSlash=" + graceSlash
                + ", tieStart=" + tieStart
                + ", tupletActual=" + tupletActual
                + ", tupletNormal=" + tupletNormal
                + ", tupletStart=" + tupletStart
                + ", tupletStop=" + tupletStop
                + "]";
    }
}
