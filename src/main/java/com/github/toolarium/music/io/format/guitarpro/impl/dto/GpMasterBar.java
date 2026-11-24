/*
 * GpMasterBar.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * A Guitar Pro master bar (measure header): time, key, repeats, endings, marker and tempo changes.
 *
 * @author patrick
 */
public class GpMasterBar {
    private int beats = 4;
    private int beatType = 4;
    private int keyFifths;
    private boolean minor;
    private boolean repeatStart;
    private int repeatCount;
    private String alternateEndings;
    private String marker;
    private boolean doubleBar;
    private boolean anacrusis;
    private final List<String> directions = new ArrayList<>();
    private final List<GpTempo> tempoChanges = new ArrayList<>();
    private final List<Fraction> fermatas = new ArrayList<>();


    /**
     * Constructor for GpMasterBar.
     */
    public GpMasterBar() {
        // default constructor
    }


    /**
     * Gets the beats of the time signature.
     *
     * @return the beats of the time signature
     */
    public int getBeats() {
        return beats;
    }


    /**
     * Sets the beats of the time signature.
     *
     * @param beats the beats of the time signature
     */
    public void setBeats(int beats) {
        this.beats = beats;
    }


    /**
     * Gets the beat type of the time signature.
     *
     * @return the beat type of the time signature
     */
    public int getBeatType() {
        return beatType;
    }


    /**
     * Sets the beat type of the time signature.
     *
     * @param beatType the beat type of the time signature
     */
    public void setBeatType(int beatType) {
        this.beatType = beatType;
    }


    /**
     * Gets the key signature.
     *
     * @return the key signature
     */
    public int getKeyFifths() {
        return keyFifths;
    }


    /**
     * Sets the key signature.
     *
     * @param keyFifths the key signature
     */
    public void setKeyFifths(int keyFifths) {
        this.keyFifths = keyFifths;
    }


    /**
     * Gets the flag for a minor key.
     *
     * @return the flag for a minor key
     */
    public boolean isMinor() {
        return minor;
    }


    /**
     * Sets the flag for a minor key.
     *
     * @param minor the flag for a minor key
     */
    public void setMinor(boolean minor) {
        this.minor = minor;
    }


    /**
     * Gets the flag for a repeat start.
     *
     * @return the flag for a repeat start
     */
    public boolean isRepeatStart() {
        return repeatStart;
    }


    /**
     * Sets the flag for a repeat start.
     *
     * @param repeatStart the flag for a repeat start
     */
    public void setRepeatStart(boolean repeatStart) {
        this.repeatStart = repeatStart;
    }


    /**
     * Gets the number of repeats at the end (0 for no repeat end).
     *
     * @return the number of repeats at the end (0 for no repeat end)
     */
    public int getRepeatCount() {
        return repeatCount;
    }


    /**
     * Sets the number of repeats at the end (0 for no repeat end).
     *
     * @param repeatCount the number of repeats at the end (0 for no repeat end)
     */
    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }


    /**
     * Gets the ending numbers, for example 1 or 1, 3.
     *
     * @return the ending numbers, for example 1 or 1, 3
     */
    public String getAlternateEndings() {
        return alternateEndings;
    }


    /**
     * Sets the ending numbers, for example 1 or 1, 3.
     *
     * @param alternateEndings the ending numbers, for example 1 or 1, 3
     */
    public void setAlternateEndings(String alternateEndings) {
        this.alternateEndings = alternateEndings;
    }


    /**
     * Gets the marker (section) text.
     *
     * @return the marker (section) text
     */
    public String getMarker() {
        return marker;
    }


    /**
     * Sets the marker (section) text.
     *
     * @param marker the marker (section) text
     */
    public void setMarker(String marker) {
        this.marker = marker;
    }


    /**
     * Gets the flag for a double bar at the end.
     *
     * @return the flag for a double bar at the end
     */
    public boolean isDoubleBar() {
        return doubleBar;
    }


    /**
     * Sets the flag for a double bar at the end.
     *
     * @param doubleBar the flag for a double bar at the end
     */
    public void setDoubleBar(boolean doubleBar) {
        this.doubleBar = doubleBar;
    }


    /**
     * Gets the flag for a pickup measure.
     *
     * @return the flag for a pickup measure
     */
    public boolean isAnacrusis() {
        return anacrusis;
    }


    /**
     * Sets the flag for a pickup measure.
     *
     * @param anacrusis the flag for a pickup measure
     */
    public void setAnacrusis(boolean anacrusis) {
        this.anacrusis = anacrusis;
    }


    /**
     * Gets the jumps and targets (Segno, Coda, DaCapo, Fine and so on) (mutable).
     *
     * @return the jumps and targets (Segno, Coda, DaCapo, Fine and so on) (mutable)
     */
    public List<String> getDirections() {
        return directions;
    }


    /**
     * Gets the tempo changes (mutable).
     *
     * @return the tempo changes (mutable)
     */
    public List<GpTempo> getTempoChanges() {
        return tempoChanges;
    }


    /**
     * Gets the positions of fermatas in whole notes (mutable).
     *
     * @return the positions of fermatas in whole notes (mutable)
     */
    public List<Fraction> getFermatas() {
        return fermatas;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                beats, beatType, keyFifths, minor, repeatStart, repeatCount, alternateEndings, marker, doubleBar, anacrusis,
                directions, tempoChanges, fermatas);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a GpMasterBar with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GpMasterBar other = (GpMasterBar) obj;
        return Objects.equals(beats, other.beats)
                && Objects.equals(beatType, other.beatType)
                && Objects.equals(keyFifths, other.keyFifths)
                && Objects.equals(minor, other.minor)
                && Objects.equals(repeatStart, other.repeatStart)
                && Objects.equals(repeatCount, other.repeatCount)
                && Objects.equals(alternateEndings, other.alternateEndings)
                && Objects.equals(marker, other.marker)
                && Objects.equals(doubleBar, other.doubleBar)
                && Objects.equals(anacrusis, other.anacrusis)
                && Objects.equals(directions, other.directions)
                && Objects.equals(tempoChanges, other.tempoChanges)
                && Objects.equals(fermatas, other.fermatas);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "GpMasterBar ["
                + "beats=" + beats
                + ", beatType=" + beatType
                + ", keyFifths=" + keyFifths
                + ", minor=" + minor
                + ", repeatStart=" + repeatStart
                + ", repeatCount=" + repeatCount
                + ", alternateEndings=" + alternateEndings
                + ", marker=" + marker
                + ", doubleBar=" + doubleBar
                + ", anacrusis=" + anacrusis
                + ", directions=" + directions
                + ", tempoChanges=" + tempoChanges
                + ", fermatas=" + fermatas
                + "]";
    }
}
