/*
 * NoteImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.notation.impl.NotationsImpl;
import com.github.toolarium.music.model.note.INote;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link INote} interface.
 *
 * @author patrick
 */
public class NoteImpl implements INote, IMusicDataElement {

    @JacksonXmlProperty(localName = "pitch")
    private PitchImpl pitch;

    @JacksonXmlProperty(localName = "rest")
    private RestImpl rest;

    @JacksonXmlProperty(localName = "unpitched")
    private UnpitchedImpl unpitched;

    @JacksonXmlProperty(localName = "chord")
    private String chord;

    @JacksonXmlProperty(localName = "duration")
    private Integer duration;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "tie")
    private List<TieImpl> ties;

    @JacksonXmlProperty(localName = "voice")
    private Integer voice;

    @JacksonXmlProperty(localName = "type")
    private String type;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "dot")
    private List<String> dots;

    @JacksonXmlProperty(localName = "accidental")
    private String accidental;

    @JacksonXmlProperty(localName = "time-modification")
    private TimeModificationImpl timeModification;

    @JacksonXmlProperty(localName = "stem")
    private String stem;

    @JacksonXmlProperty(localName = "notehead")
    private String notehead;

    @JacksonXmlProperty(localName = "staff")
    private Integer staff;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "beam")
    private List<BeamImpl> beams;

    @JacksonXmlProperty(localName = "notations")
    private NotationsImpl notations;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "lyric")
    private List<LyricImpl> lyrics;

    @JacksonXmlProperty(isAttribute = true, localName = "default-x")
    private String defaultX;

    @JacksonXmlProperty(isAttribute = true, localName = "default-y")
    private String defaultY;

    @JacksonXmlProperty(isAttribute = true, localName = "print-object")
    private String printObject;


    /**
     * Constructor.
     */
    public NoteImpl() {
        this.ties = new ArrayList<>();
        this.dots = new ArrayList<>();
        this.beams = new ArrayList<>();
        this.lyrics = new ArrayList<>();
    }


    /**
     * Gets the pitch.
     *
     * @return the pitch
     */
    @Override
    public PitchImpl getPitch() {
        return pitch;
    }


    /**
     * Sets the pitch.
     *
     * @param pitch the pitch
     */
    public void setPitch(PitchImpl pitch) {
        this.pitch = pitch;
    }


    /**
     * Gets the rest.
     *
     * @return the rest
     */
    @Override
    public RestImpl getRest() {
        return rest;
    }


    /**
     * Sets the rest.
     *
     * @param rest the rest
     */
    public void setRest(RestImpl rest) {
        this.rest = rest;
    }


    /**
     * Gets the unpitched.
     *
     * @return the unpitched
     */
    @Override
    public UnpitchedImpl getUnpitched() {
        return unpitched;
    }


    /**
     * Sets the unpitched.
     *
     * @param unpitched the unpitched
     */
    public void setUnpitched(UnpitchedImpl unpitched) {
        this.unpitched = unpitched;
    }


    /**
     * Gets the chord.
     *
     * @return the chord
     */
    @Override
    public String getChord() {
        return chord;
    }


    /**
     * Sets the chord.
     *
     * @param chord the chord
     */
    public void setChord(String chord) {
        this.chord = chord;
    }


    /**
     * Gets the duration.
     *
     * @return the duration
     */
    @Override
    public Integer getDuration() {
        return duration;
    }


    /**
     * Sets the duration.
     *
     * @param duration the duration
     */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }


    /**
     * Gets the ties.
     *
     * @return the ties
     */
    @Override
    public List<TieImpl> getTies() {
        return ties;
    }


    /**
     * Sets the ties.
     *
     * @param ties the ties
     */
    public void setTies(List<TieImpl> ties) {
        this.ties = ties;
    }


    /**
     * Gets the voice.
     *
     * @return the voice
     */
    @Override
    public Integer getVoice() {
        return voice;
    }


    /**
     * Sets the voice.
     *
     * @param voice the voice
     */
    public void setVoice(Integer voice) {
        this.voice = voice;
    }


    /**
     * Gets the type.
     *
     * @return the type
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }


    /**
     * Gets the dots.
     *
     * @return the dots
     */
    @Override
    public List<String> getDots() {
        return dots;
    }


    /**
     * Sets the dots.
     *
     * @param dots the dots
     */
    public void setDots(List<String> dots) {
        this.dots = dots;
    }


    /**
     * Gets the accidental.
     *
     * @return the accidental
     */
    @Override
    public String getAccidental() {
        return accidental;
    }


    /**
     * Sets the accidental.
     *
     * @param accidental the accidental
     */
    public void setAccidental(String accidental) {
        this.accidental = accidental;
    }


    /**
     * Gets the time modification.
     *
     * @return the time modification
     */
    @Override
    public TimeModificationImpl getTimeModification() {
        return timeModification;
    }


    /**
     * Sets the time modification.
     *
     * @param timeModification the time modification
     */
    public void setTimeModification(TimeModificationImpl timeModification) {
        this.timeModification = timeModification;
    }


    /**
     * Gets the stem.
     *
     * @return the stem
     */
    @Override
    public String getStem() {
        return stem;
    }


    /**
     * Sets the stem.
     *
     * @param stem the stem
     */
    public void setStem(String stem) {
        this.stem = stem;
    }


    /**
     * Gets the notehead.
     *
     * @return the notehead
     */
    @Override
    public String getNotehead() {
        return notehead;
    }


    /**
     * Sets the notehead.
     *
     * @param notehead the notehead
     */
    public void setNotehead(String notehead) {
        this.notehead = notehead;
    }


    /**
     * Gets the staff.
     *
     * @return the staff
     */
    @Override
    public Integer getStaff() {
        return staff;
    }


    /**
     * Sets the staff.
     *
     * @param staff the staff
     */
    public void setStaff(Integer staff) {
        this.staff = staff;
    }


    /**
     * Gets the beams.
     *
     * @return the beams
     */
    @Override
    public List<BeamImpl> getBeams() {
        return beams;
    }


    /**
     * Sets the beams.
     *
     * @param beams the beams
     */
    public void setBeams(List<BeamImpl> beams) {
        this.beams = beams;
    }


    /**
     * Gets the notations.
     *
     * @return the notations
     */
    @Override
    public NotationsImpl getNotations() {
        return notations;
    }


    /**
     * Sets the notations.
     *
     * @param notations the notations
     */
    public void setNotations(NotationsImpl notations) {
        this.notations = notations;
    }


    /**
     * Gets the lyrics.
     *
     * @return the lyrics
     */
    @Override
    public List<LyricImpl> getLyrics() {
        return lyrics;
    }


    /**
     * Sets the lyrics.
     *
     * @param lyrics the lyrics
     */
    public void setLyrics(List<LyricImpl> lyrics) {
        this.lyrics = lyrics;
    }


    /**
     * Gets the default x.
     *
     * @return the default x
     */
    @Override
    public String getDefaultX() {
        return defaultX;
    }


    /**
     * Sets the default x.
     *
     * @param defaultX the default x
     */
    public void setDefaultX(String defaultX) {
        this.defaultX = defaultX;
    }


    /**
     * Gets the default y.
     *
     * @return the default y
     */
    @Override
    public String getDefaultY() {
        return defaultY;
    }


    /**
     * Sets the default y.
     *
     * @param defaultY the default y
     */
    public void setDefaultY(String defaultY) {
        this.defaultY = defaultY;
    }


    /**
     * Gets the print object.
     *
     * @return the print object
     */
    @Override
    public String getPrintObject() {
        return printObject;
    }


    /**
     * Sets the print object.
     *
     * @param printObject the print object
     */
    public void setPrintObject(String printObject) {
        this.printObject = printObject;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(pitch, rest, unpitched, chord, duration, ties, voice, type,
                dots, accidental, timeModification, stem, notehead, staff, beams,
                notations, lyrics, defaultX, defaultY, printObject);
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
        NoteImpl other = (NoteImpl) obj;
        return Objects.equals(pitch, other.pitch)
                && Objects.equals(rest, other.rest)
                && Objects.equals(unpitched, other.unpitched)
                && Objects.equals(chord, other.chord)
                && Objects.equals(duration, other.duration)
                && Objects.equals(ties, other.ties)
                && Objects.equals(voice, other.voice)
                && Objects.equals(type, other.type)
                && Objects.equals(dots, other.dots)
                && Objects.equals(accidental, other.accidental)
                && Objects.equals(timeModification, other.timeModification)
                && Objects.equals(stem, other.stem)
                && Objects.equals(notehead, other.notehead)
                && Objects.equals(staff, other.staff)
                && Objects.equals(beams, other.beams)
                && Objects.equals(notations, other.notations)
                && Objects.equals(lyrics, other.lyrics)
                && Objects.equals(defaultX, other.defaultX)
                && Objects.equals(defaultY, other.defaultY)
                && Objects.equals(printObject, other.printObject);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "NoteImpl [pitch=" + pitch + ", rest=" + rest + ", unpitched=" + unpitched
                + ", chord=" + chord + ", duration=" + duration + ", ties=" + ties
                + ", voice=" + voice + ", type=" + type + ", dots=" + dots
                + ", accidental=" + accidental + ", timeModification=" + timeModification
                + ", stem=" + stem + ", notehead=" + notehead + ", staff=" + staff
                + ", beams=" + beams + ", notations=" + notations + ", lyrics=" + lyrics
                + ", defaultX=" + defaultX + ", defaultY=" + defaultY
                + ", printObject=" + printObject + "]";
    }
}
