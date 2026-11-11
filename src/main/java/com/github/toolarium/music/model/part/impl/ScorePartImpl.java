/*
 * ScorePartImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.part.IScorePart;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IScorePart} interface.
 *
 * @author patrick
 */
public class ScorePartImpl implements IScorePart {

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "part-name")
    private String partName;

    @JacksonXmlProperty(localName = "part-abbreviation")
    private String partAbbreviation;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "score-instrument")
    private List<ScoreInstrumentImpl> scoreInstruments;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "midi-device")
    private List<MidiDeviceImpl> midiDevices;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "midi-instrument")
    private List<MidiInstrumentImpl> midiInstruments;


    /**
     * Constructor.
     */
    public ScorePartImpl() {
        this.scoreInstruments = new ArrayList<>();
        this.midiDevices = new ArrayList<>();
        this.midiInstruments = new ArrayList<>();
    }


    /**
     * Gets the id.
     *
     * @return the id
     */
    @Override
    public String getId() {
        return id;
    }


    /**
     * Sets the id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }


    /**
     * Gets the part name.
     *
     * @return the part name
     */
    @Override
    public String getPartName() {
        return partName;
    }


    /**
     * Sets the part name.
     *
     * @param partName the part name
     */
    public void setPartName(String partName) {
        this.partName = partName;
    }


    /**
     * Gets the part abbreviation.
     *
     * @return the part abbreviation
     */
    @Override
    public String getPartAbbreviation() {
        return partAbbreviation;
    }


    /**
     * Sets the part abbreviation.
     *
     * @param partAbbreviation the part abbreviation
     */
    public void setPartAbbreviation(String partAbbreviation) {
        this.partAbbreviation = partAbbreviation;
    }


    /**
     * Gets the score instruments.
     *
     * @return the score instruments
     */
    @Override
    public List<ScoreInstrumentImpl> getScoreInstruments() {
        return scoreInstruments;
    }


    /**
     * Sets the score instruments.
     *
     * @param scoreInstruments the score instruments
     */
    public void setScoreInstruments(List<ScoreInstrumentImpl> scoreInstruments) {
        this.scoreInstruments = scoreInstruments;
    }


    /**
     * Gets the MIDI devices.
     *
     * @return the MIDI devices
     */
    @Override
    public List<MidiDeviceImpl> getMidiDevices() {
        return midiDevices;
    }


    /**
     * Sets the MIDI devices.
     *
     * @param midiDevices the MIDI devices
     */
    public void setMidiDevices(List<MidiDeviceImpl> midiDevices) {
        this.midiDevices = midiDevices;
    }


    /**
     * Gets the MIDI instruments.
     *
     * @return the MIDI instruments
     */
    @Override
    public List<MidiInstrumentImpl> getMidiInstruments() {
        return midiInstruments;
    }


    /**
     * Sets the MIDI instruments.
     *
     * @param midiInstruments the MIDI instruments
     */
    public void setMidiInstruments(List<MidiInstrumentImpl> midiInstruments) {
        this.midiInstruments = midiInstruments;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, partName, partAbbreviation, scoreInstruments, midiDevices, midiInstruments);
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
        ScorePartImpl other = (ScorePartImpl) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(partName, other.partName)
                && Objects.equals(partAbbreviation, other.partAbbreviation)
                && Objects.equals(scoreInstruments, other.scoreInstruments)
                && Objects.equals(midiDevices, other.midiDevices)
                && Objects.equals(midiInstruments, other.midiInstruments);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "ScorePartImpl ["
                + "id=" + id
                + ", partName=" + partName
                + ", partAbbreviation=" + partAbbreviation
                + ", scoreInstruments=" + scoreInstruments
                + ", midiDevices=" + midiDevices
                + ", midiInstruments=" + midiInstruments
                + "]";
    }
}
