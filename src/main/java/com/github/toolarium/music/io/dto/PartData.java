/*
 * PartData.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * One part (instrument) of a score, as collected by a format reader for the {@link ScoreBuilder}.
 *
 * @author patrick
 */
public class PartData {
    private final List<MeasureData> measures = new ArrayList<>();
    private final Map<Integer, ClefImpl> defaultClefs = new HashMap<>();
    private final Map<Integer, Integer> staffLines = new HashMap<>();
    private String name;
    private String abbreviation;
    private int staves = 1;
    private Integer midiProgram;
    private Integer midiChannel;
    private Integer midiBank;
    private Double volume;
    private Double pan;
    private boolean percussion;
    private int transposeChromatic;
    private int transposeDiatonic;


    /**
     * Constructor for PartData.
     *
     * @param name the part name
     */
    public PartData(String name) {
        this.name = name;
    }


    /**
     * Gets the measures.
     *
     * @return the measures (mutable)
     */
    public List<MeasureData> getMeasures() {
        return measures;
    }


    /**
     * Gets a measure, creating it and all measures before it if needed.
     *
     * @param index the measure index (0-based)
     * @return the measure
     */
    public MeasureData measure(int index) {
        while (measures.size() <= index) {
            measures.add(new MeasureData());
        }
        return measures.get(index);
    }


    /**
     * Gets the clef of each staff used when the first measure has no clef.
     *
     * @return the default clefs by staff (mutable)
     */
    public Map<Integer, ClefImpl> getDefaultClefs() {
        return defaultClefs;
    }


    /**
     * Gets the number of staff lines of staves that do not have five lines.
     *
     * @return the staff lines by staff (mutable)
     */
    public Map<Integer, Integer> getStaffLines() {
        return staffLines;
    }


    /**
     * Gets the part name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }


    /**
     * Sets the part name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Gets the abbreviation.
     *
     * @return the abbreviation, or null
     */
    public String getAbbreviation() {
        return abbreviation;
    }


    /**
     * Sets the abbreviation.
     *
     * @param abbreviation the abbreviation, or null
     */
    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }


    /**
     * Gets the number of staves.
     *
     * @return the number of staves
     */
    public int getStaves() {
        return staves;
    }


    /**
     * Sets the number of staves.
     *
     * @param staves the number of staves
     */
    public void setStaves(int staves) {
        this.staves = staves;
    }


    /**
     * Gets the General MIDI program.
     *
     * @return the program (1 to 128 as in MusicXML), or null
     */
    public Integer getMidiProgram() {
        return midiProgram;
    }


    /**
     * Sets the General MIDI program.
     *
     * @param midiProgram the program (1 to 128 as in MusicXML), or null
     */
    public void setMidiProgram(Integer midiProgram) {
        this.midiProgram = midiProgram;
    }


    /**
     * Gets the MIDI channel.
     *
     * @return the channel (1 to 16), or null
     */
    public Integer getMidiChannel() {
        return midiChannel;
    }


    /**
     * Sets the MIDI channel.
     *
     * @param midiChannel the channel (1 to 16), or null
     */
    public void setMidiChannel(Integer midiChannel) {
        this.midiChannel = midiChannel;
    }


    /**
     * Gets the MIDI bank.
     *
     * @return the bank (1-based as in MusicXML), or null
     */
    public Integer getMidiBank() {
        return midiBank;
    }


    /**
     * Sets the MIDI bank.
     *
     * @param midiBank the bank (1-based as in MusicXML), or null
     */
    public void setMidiBank(Integer midiBank) {
        this.midiBank = midiBank;
    }


    /**
     * Gets the volume.
     *
     * @return the volume (0 to 100), or null
     */
    public Double getVolume() {
        return volume;
    }


    /**
     * Sets the volume.
     *
     * @param volume the volume (0 to 100), or null
     */
    public void setVolume(Double volume) {
        this.volume = volume;
    }


    /**
     * Gets the pan position.
     *
     * @return the pan (-90 left to 90 right), or null
     */
    public Double getPan() {
        return pan;
    }


    /**
     * Sets the pan position.
     *
     * @param pan the pan (-90 left to 90 right), or null
     */
    public void setPan(Double pan) {
        this.pan = pan;
    }


    /**
     * Checks if the part is a percussion part with unpitched notes.
     *
     * @return true for percussion
     */
    public boolean isPercussion() {
        return percussion;
    }


    /**
     * Sets the percussion flag.
     *
     * @param percussion true for percussion
     */
    public void setPercussion(boolean percussion) {
        this.percussion = percussion;
    }


    /**
     * Gets the chromatic transposition (sounding pitch minus written pitch).
     *
     * @return the transposition in semitones
     */
    public int getTransposeChromatic() {
        return transposeChromatic;
    }


    /**
     * Gets the diatonic transposition.
     *
     * @return the transposition in diatonic steps
     */
    public int getTransposeDiatonic() {
        return transposeDiatonic;
    }


    /**
     * Sets the transposition (sounding pitch minus written pitch).
     *
     * @param chromatic the transposition in semitones
     * @param diatonic the transposition in diatonic steps
     */
    public void setTranspose(int chromatic, int diatonic) {
        this.transposeChromatic = chromatic;
        this.transposeDiatonic = diatonic;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                measures, defaultClefs, staffLines, name, abbreviation, staves, midiProgram, midiChannel, midiBank, volume, pan,
                percussion, transposeChromatic, transposeDiatonic);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a PartData with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PartData other = (PartData) obj;
        return Objects.equals(measures, other.measures)
                && Objects.equals(defaultClefs, other.defaultClefs)
                && Objects.equals(staffLines, other.staffLines)
                && Objects.equals(name, other.name)
                && Objects.equals(abbreviation, other.abbreviation)
                && Objects.equals(staves, other.staves)
                && Objects.equals(midiProgram, other.midiProgram)
                && Objects.equals(midiChannel, other.midiChannel)
                && Objects.equals(midiBank, other.midiBank)
                && Objects.equals(volume, other.volume)
                && Objects.equals(pan, other.pan)
                && Objects.equals(percussion, other.percussion)
                && Objects.equals(transposeChromatic, other.transposeChromatic)
                && Objects.equals(transposeDiatonic, other.transposeDiatonic);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "PartData ["
                + "measures=" + measures
                + ", defaultClefs=" + defaultClefs
                + ", staffLines=" + staffLines
                + ", name=" + name
                + ", abbreviation=" + abbreviation
                + ", staves=" + staves
                + ", midiProgram=" + midiProgram
                + ", midiChannel=" + midiChannel
                + ", midiBank=" + midiBank
                + ", volume=" + volume
                + ", pan=" + pan
                + ", percussion=" + percussion
                + ", transposeChromatic=" + transposeChromatic
                + ", transposeDiatonic=" + transposeDiatonic
                + "]";
    }
}
