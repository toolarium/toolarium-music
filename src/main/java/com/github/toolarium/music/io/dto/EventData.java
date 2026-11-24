/*
 * EventData.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.notation.impl.ArticulationsImpl;
import com.github.toolarium.music.model.notation.impl.FermataImpl;
import com.github.toolarium.music.model.notation.impl.OrnamentsImpl;
import com.github.toolarium.music.model.notation.impl.PlayingTechniqueImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * A rhythmic event of a voice: a note, a chord or a rest, as collected by a format reader for
 * the {@link ScoreBuilder}. An event without notes is a rest.
 *
 * @author patrick
 */
public class EventData {
    private final List<NoteData> notes = new ArrayList<>();
    private final List<LyricImpl> lyrics = new ArrayList<>();
    private final List<FermataImpl> fermatas = new ArrayList<>();
    private final List<Integer> slurStarts = new ArrayList<>();
    private final List<Integer> slurStops = new ArrayList<>();
    private final List<DirectionImpl> directions = new ArrayList<>();
    private final List<HarmonyImpl> harmonies = new ArrayList<>();
    private Fraction duration;
    private String type;
    private int dots;
    private int tupletActual;
    private int tupletNormal;
    private String tupletNormalType;
    private boolean tupletStart;
    private boolean tupletStop;
    private boolean grace;
    private boolean graceSlash;
    private boolean measureRest;
    private boolean invisible;
    private Integer staff;
    private String stem;
    private ArticulationsImpl articulations;
    private OrnamentsImpl ornaments;
    private PlayingTechniqueImpl technical;


    /**
     * Constructor for EventData.
     */
    public EventData() {
        // default constructor
    }


    /**
     * Gets the notes. An event without notes is a rest.
     *
     * @return the notes (mutable)
     */
    public List<NoteData> getNotes() {
        return notes;
    }


    /**
     * Checks if the event is a rest.
     *
     * @return true if the event has no notes
     */
    public boolean isRest() {
        return notes.isEmpty();
    }


    /**
     * Gets the lyrics of the event.
     *
     * @return the lyrics (mutable)
     */
    public List<LyricImpl> getLyrics() {
        return lyrics;
    }


    /**
     * Gets the fermatas of the event.
     *
     * @return the fermatas (mutable)
     */
    public List<FermataImpl> getFermatas() {
        return fermatas;
    }


    /**
     * Gets the numbers of the slurs that start at this event.
     *
     * @return the slur numbers (mutable)
     */
    public List<Integer> getSlurStarts() {
        return slurStarts;
    }


    /**
     * Gets the numbers of the slurs that end at this event.
     *
     * @return the slur numbers (mutable)
     */
    public List<Integer> getSlurStops() {
        return slurStops;
    }


    /**
     * Gets the directions placed at the start of this event.
     *
     * @return the directions (mutable)
     */
    public List<DirectionImpl> getDirections() {
        return directions;
    }


    /**
     * Gets the chord symbols placed at this event.
     *
     * @return the harmonies (mutable)
     */
    public List<HarmonyImpl> getHarmonies() {
        return harmonies;
    }


    /**
     * Gets the sounding duration (tuplets included). Grace notes have no duration.
     *
     * @return the duration in whole notes, or null
     */
    public Fraction getDuration() {
        return duration;
    }


    /**
     * Sets the sounding duration (tuplets included).
     *
     * @param duration the duration in whole notes
     */
    public void setDuration(Fraction duration) {
        this.duration = duration;
    }


    /**
     * Gets the note type.
     *
     * @return the MusicXML note type, or null to derive it from the duration
     */
    public String getType() {
        return type;
    }


    /**
     * Sets the note type.
     *
     * @param type the MusicXML note type
     */
    public void setType(String type) {
        this.type = type;
    }


    /**
     * Gets the number of dots.
     *
     * @return the number of dots
     */
    public int getDots() {
        return dots;
    }


    /**
     * Sets the number of dots.
     *
     * @param dots the number of dots
     */
    public void setDots(int dots) {
        this.dots = dots;
    }


    /**
     * Sets the tuplet ratio: actual notes in the time of normal notes, for example 3 in the time of 2.
     *
     * @param actual the actual notes
     * @param normal the normal notes
     */
    public void setTuplet(int actual, int normal) {
        this.tupletActual = actual;
        this.tupletNormal = normal;
    }


    /**
     * Gets the actual notes of the tuplet ratio.
     *
     * @return the actual notes, 0 if the event is not in a tuplet
     */
    public int getTupletActual() {
        return tupletActual;
    }


    /**
     * Gets the normal notes of the tuplet ratio.
     *
     * @return the normal notes, 0 if the event is not in a tuplet
     */
    public int getTupletNormal() {
        return tupletNormal;
    }


    /**
     * Gets the normal note type of the tuplet if it differs from the note type.
     *
     * @return the normal note type, or null
     */
    public String getTupletNormalType() {
        return tupletNormalType;
    }


    /**
     * Sets the normal note type of the tuplet.
     *
     * @param tupletNormalType the normal note type, or null
     */
    public void setTupletNormalType(String tupletNormalType) {
        this.tupletNormalType = tupletNormalType;
    }


    /**
     * Checks if a tuplet bracket starts at this event.
     *
     * @return true if a tuplet starts
     */
    public boolean isTupletStart() {
        return tupletStart;
    }


    /**
     * Sets the tuplet start flag.
     *
     * @param tupletStart true if a tuplet starts
     */
    public void setTupletStart(boolean tupletStart) {
        this.tupletStart = tupletStart;
    }


    /**
     * Checks if a tuplet bracket ends at this event.
     *
     * @return true if a tuplet ends
     */
    public boolean isTupletStop() {
        return tupletStop;
    }


    /**
     * Sets the tuplet stop flag.
     *
     * @param tupletStop true if a tuplet ends
     */
    public void setTupletStop(boolean tupletStop) {
        this.tupletStop = tupletStop;
    }


    /**
     * Checks if the event is a grace note (or grace chord).
     *
     * @return true for a grace note
     */
    public boolean isGrace() {
        return grace;
    }


    /**
     * Sets the grace flag.
     *
     * @param grace true for a grace note
     */
    public void setGrace(boolean grace) {
        this.grace = grace;
    }


    /**
     * Checks if the grace note is shown with a slash (acciaccatura).
     *
     * @return true with a slash
     */
    public boolean isGraceSlash() {
        return graceSlash;
    }


    /**
     * Sets the grace slash flag.
     *
     * @param graceSlash true with a slash
     */
    public void setGraceSlash(boolean graceSlash) {
        this.graceSlash = graceSlash;
    }


    /**
     * Checks if the rest fills the whole measure.
     *
     * @return true for a measure rest
     */
    public boolean isMeasureRest() {
        return measureRest;
    }


    /**
     * Sets the measure rest flag. The duration is then set by the builder.
     *
     * @param measureRest true for a measure rest
     */
    public void setMeasureRest(boolean measureRest) {
        this.measureRest = measureRest;
    }


    /**
     * Checks if the event is invisible (a spacer).
     *
     * @return true if invisible
     */
    public boolean isInvisible() {
        return invisible;
    }


    /**
     * Sets the invisible flag.
     *
     * @param invisible true if invisible
     */
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }


    /**
     * Gets the staff of a cross-staff event.
     *
     * @return the staff (1-based), or null for the staff of the voice
     */
    public Integer getStaff() {
        return staff;
    }


    /**
     * Sets the staff of a cross-staff event.
     *
     * @param staff the staff (1-based), or null for the staff of the voice
     */
    public void setStaff(Integer staff) {
        this.staff = staff;
    }


    /**
     * Gets the stem direction.
     *
     * @return the MusicXML stem (up or down), or null
     */
    public String getStem() {
        return stem;
    }


    /**
     * Sets the stem direction.
     *
     * @param stem the MusicXML stem (up or down), or null
     */
    public void setStem(String stem) {
        this.stem = stem;
    }


    /**
     * Gets the articulations.
     *
     * @return the articulations, or null
     */
    public ArticulationsImpl getArticulations() {
        return articulations;
    }


    /**
     * Gets the articulations, creating them if needed.
     *
     * @return the articulations
     */
    public ArticulationsImpl articulations() {
        if (articulations == null) {
            articulations = new ArticulationsImpl();
        }
        return articulations;
    }


    /**
     * Gets the ornaments.
     *
     * @return the ornaments, or null
     */
    public OrnamentsImpl getOrnaments() {
        return ornaments;
    }


    /**
     * Gets the ornaments, creating them if needed.
     *
     * @return the ornaments
     */
    public OrnamentsImpl ornaments() {
        if (ornaments == null) {
            ornaments = new OrnamentsImpl();
        }
        return ornaments;
    }


    /**
     * Gets the technical notations of the event (up-bow, fingering and so on).
     *
     * @return the technical notations, or null
     */
    public PlayingTechniqueImpl getTechnical() {
        return technical;
    }


    /**
     * Gets the technical notations of the event, creating them if needed.
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
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                notes, lyrics, fermatas, slurStarts, slurStops, directions, harmonies, duration, type, dots, tupletActual,
                tupletNormal, tupletNormalType, tupletStart, tupletStop, grace, graceSlash, measureRest, invisible, staff, stem,
                articulations, ornaments, technical);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a EventData with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EventData other = (EventData) obj;
        return Objects.equals(notes, other.notes)
                && Objects.equals(lyrics, other.lyrics)
                && Objects.equals(fermatas, other.fermatas)
                && Objects.equals(slurStarts, other.slurStarts)
                && Objects.equals(slurStops, other.slurStops)
                && Objects.equals(directions, other.directions)
                && Objects.equals(harmonies, other.harmonies)
                && Objects.equals(duration, other.duration)
                && Objects.equals(type, other.type)
                && Objects.equals(dots, other.dots)
                && Objects.equals(tupletActual, other.tupletActual)
                && Objects.equals(tupletNormal, other.tupletNormal)
                && Objects.equals(tupletNormalType, other.tupletNormalType)
                && Objects.equals(tupletStart, other.tupletStart)
                && Objects.equals(tupletStop, other.tupletStop)
                && Objects.equals(grace, other.grace)
                && Objects.equals(graceSlash, other.graceSlash)
                && Objects.equals(measureRest, other.measureRest)
                && Objects.equals(invisible, other.invisible)
                && Objects.equals(staff, other.staff)
                && Objects.equals(stem, other.stem)
                && Objects.equals(articulations, other.articulations)
                && Objects.equals(ornaments, other.ornaments)
                && Objects.equals(technical, other.technical);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "EventData ["
                + "notes=" + notes
                + ", lyrics=" + lyrics
                + ", fermatas=" + fermatas
                + ", slurStarts=" + slurStarts
                + ", slurStops=" + slurStops
                + ", directions=" + directions
                + ", harmonies=" + harmonies
                + ", duration=" + duration
                + ", type=" + type
                + ", dots=" + dots
                + ", tupletActual=" + tupletActual
                + ", tupletNormal=" + tupletNormal
                + ", tupletNormalType=" + tupletNormalType
                + ", tupletStart=" + tupletStart
                + ", tupletStop=" + tupletStop
                + ", grace=" + grace
                + ", graceSlash=" + graceSlash
                + ", measureRest=" + measureRest
                + ", invisible=" + invisible
                + ", staff=" + staff
                + ", stem=" + stem
                + ", articulations=" + articulations
                + ", ornaments=" + ornaments
                + ", technical=" + technical
                + "]";
    }
}
