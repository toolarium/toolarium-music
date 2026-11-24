/*
 * GpTrack.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * A Guitar Pro track: instrument, tuning and the bars of each master bar.
 *
 * @author patrick
 */
public class GpTrack {
    private String name;
    private String shortName;
    private Integer program;
    private Integer channel;
    private Double volume;
    private Double pan;
    private boolean percussion;
    private int transpose;
    private int capo;
    private int staffCount = 1;
    private final List<Integer> tuning = new ArrayList<>();
    private final Map<Integer, Integer> drumMidi = new HashMap<>();
    private final List<GpBar> bars = new ArrayList<>();


    /**
     * Constructor for GpTrack.
     */
    public GpTrack() {
        // default constructor
    }


    /**
     * Gets the track name.
     *
     * @return the track name
     */
    public String getName() {
        return name;
    }


    /**
     * Sets the track name.
     *
     * @param name the track name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Gets the short name.
     *
     * @return the short name
     */
    public String getShortName() {
        return shortName;
    }


    /**
     * Sets the short name.
     *
     * @param shortName the short name
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }


    /**
     * Gets the General MIDI program (1-based).
     *
     * @return the General MIDI program (1-based)
     */
    public Integer getProgram() {
        return program;
    }


    /**
     * Sets the General MIDI program (1-based).
     *
     * @param program the General MIDI program (1-based)
     */
    public void setProgram(Integer program) {
        this.program = program;
    }


    /**
     * Gets the MIDI channel (1-based).
     *
     * @return the MIDI channel (1-based)
     */
    public Integer getChannel() {
        return channel;
    }


    /**
     * Sets the MIDI channel (1-based).
     *
     * @param channel the MIDI channel (1-based)
     */
    public void setChannel(Integer channel) {
        this.channel = channel;
    }


    /**
     * Gets the volume (0 to 100).
     *
     * @return the volume (0 to 100)
     */
    public Double getVolume() {
        return volume;
    }


    /**
     * Sets the volume (0 to 100).
     *
     * @param volume the volume (0 to 100)
     */
    public void setVolume(Double volume) {
        this.volume = volume;
    }


    /**
     * Gets the pan (-90 to 90).
     *
     * @return the pan (-90 to 90)
     */
    public Double getPan() {
        return pan;
    }


    /**
     * Sets the pan (-90 to 90).
     *
     * @param pan the pan (-90 to 90)
     */
    public void setPan(Double pan) {
        this.pan = pan;
    }


    /**
     * Gets the flag for a drum track.
     *
     * @return the flag for a drum track
     */
    public boolean isPercussion() {
        return percussion;
    }


    /**
     * Sets the flag for a drum track.
     *
     * @param percussion the flag for a drum track
     */
    public void setPercussion(boolean percussion) {
        this.percussion = percussion;
    }


    /**
     * Gets the transposition from written to sounding pitch in semitones.
     *
     * @return the transposition from written to sounding pitch in semitones
     */
    public int getTranspose() {
        return transpose;
    }


    /**
     * Sets the transposition from written to sounding pitch in semitones.
     *
     * @param transpose the transposition from written to sounding pitch in semitones
     */
    public void setTranspose(int transpose) {
        this.transpose = transpose;
    }


    /**
     * Gets the capo fret.
     *
     * @return the capo fret
     */
    public int getCapo() {
        return capo;
    }


    /**
     * Sets the capo fret.
     *
     * @param capo the capo fret
     */
    public void setCapo(int capo) {
        this.capo = capo;
    }


    /**
     * Gets the number of staves.
     *
     * @return the number of staves
     */
    public int getStaffCount() {
        return staffCount;
    }


    /**
     * Sets the number of staves.
     *
     * @param staffCount the number of staves
     */
    public void setStaffCount(int staffCount) {
        this.staffCount = staffCount;
    }


    /**
     * Gets the MIDI pitch of each open string, highest string first (mutable).
     *
     * @return the MIDI pitch of each open string, highest string first (mutable)
     */
    public List<Integer> getTuning() {
        return tuning;
    }


    /**
     * Gets the MIDI note of each drum articulation (mutable).
     *
     * @return the MIDI note of each drum articulation (mutable)
     */
    public Map<Integer, Integer> getDrumMidi() {
        return drumMidi;
    }


    /**
     * Gets the bars, one for each master bar and staff (index = master bar * staff count + staff) (mutable).
     *
     * @return the bars, one for each master bar and staff (index = master bar * staff count + staff) (mutable)
     */
    public List<GpBar> getBars() {
        return bars;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, shortName, program, channel, volume, pan, percussion, transpose, capo, staffCount, tuning, drumMidi, bars);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a GpTrack with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GpTrack other = (GpTrack) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(shortName, other.shortName)
                && Objects.equals(program, other.program)
                && Objects.equals(channel, other.channel)
                && Objects.equals(volume, other.volume)
                && Objects.equals(pan, other.pan)
                && Objects.equals(percussion, other.percussion)
                && Objects.equals(transpose, other.transpose)
                && Objects.equals(capo, other.capo)
                && Objects.equals(staffCount, other.staffCount)
                && Objects.equals(tuning, other.tuning)
                && Objects.equals(drumMidi, other.drumMidi)
                && Objects.equals(bars, other.bars);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "GpTrack ["
                + "name=" + name
                + ", shortName=" + shortName
                + ", program=" + program
                + ", channel=" + channel
                + ", volume=" + volume
                + ", pan=" + pan
                + ", percussion=" + percussion
                + ", transpose=" + transpose
                + ", capo=" + capo
                + ", staffCount=" + staffCount
                + ", tuning=" + tuning
                + ", drumMidi=" + drumMidi
                + ", bars=" + bars
                + "]";
    }
}
