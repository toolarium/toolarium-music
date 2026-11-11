/*
 * MidiInstrumentImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.part.IMidiInstrument;
import java.util.Objects;

/**
 * Implements the {@link IMidiInstrument} interface.
 *
 * @author patrick
 */
public class MidiInstrumentImpl implements IMidiInstrument {

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "midi-channel")
    private Integer midiChannel;

    @JacksonXmlProperty(localName = "midi-name")
    private String midiName;

    @JacksonXmlProperty(localName = "midi-bank")
    private Integer midiBank;

    @JacksonXmlProperty(localName = "midi-program")
    private Integer midiProgram;

    @JacksonXmlProperty(localName = "midi-unpitched")
    private Integer midiUnpitched;

    @JacksonXmlProperty(localName = "volume")
    private Double volume;

    @JacksonXmlProperty(localName = "pan")
    private Double pan;

    @JacksonXmlProperty(localName = "elevation")
    private Double elevation;


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
     * Gets the MIDI channel.
     *
     * @return the MIDI channel
     */
    @Override
    public Integer getMidiChannel() {
        return midiChannel;
    }


    /**
     * Sets the MIDI channel.
     *
     * @param midiChannel the MIDI channel
     */
    public void setMidiChannel(Integer midiChannel) {
        this.midiChannel = midiChannel;
    }


    /**
     * Gets the MIDI name.
     *
     * @return the MIDI name
     */
    @Override
    public String getMidiName() {
        return midiName;
    }


    /**
     * Sets the MIDI name.
     *
     * @param midiName the MIDI name
     */
    public void setMidiName(String midiName) {
        this.midiName = midiName;
    }


    /**
     * Gets the MIDI bank.
     *
     * @return the MIDI bank
     */
    @Override
    public Integer getMidiBank() {
        return midiBank;
    }


    /**
     * Sets the MIDI bank.
     *
     * @param midiBank the MIDI bank
     */
    public void setMidiBank(Integer midiBank) {
        this.midiBank = midiBank;
    }


    /**
     * Gets the MIDI program.
     *
     * @return the MIDI program
     */
    @Override
    public Integer getMidiProgram() {
        return midiProgram;
    }


    /**
     * Sets the MIDI program.
     *
     * @param midiProgram the MIDI program
     */
    public void setMidiProgram(Integer midiProgram) {
        this.midiProgram = midiProgram;
    }


    /**
     * Gets the MIDI unpitched value.
     *
     * @return the MIDI unpitched value
     */
    @Override
    public Integer getMidiUnpitched() {
        return midiUnpitched;
    }


    /**
     * Sets the MIDI unpitched value.
     *
     * @param midiUnpitched the MIDI unpitched value
     */
    public void setMidiUnpitched(Integer midiUnpitched) {
        this.midiUnpitched = midiUnpitched;
    }


    /**
     * Gets the volume.
     *
     * @return the volume
     */
    @Override
    public Double getVolume() {
        return volume;
    }


    /**
     * Sets the volume.
     *
     * @param volume the volume
     */
    public void setVolume(Double volume) {
        this.volume = volume;
    }


    /**
     * Gets the pan.
     *
     * @return the pan
     */
    @Override
    public Double getPan() {
        return pan;
    }


    /**
     * Sets the pan.
     *
     * @param pan the pan
     */
    public void setPan(Double pan) {
        this.pan = pan;
    }


    /**
     * Gets the elevation.
     *
     * @return the elevation
     */
    @Override
    public Double getElevation() {
        return elevation;
    }


    /**
     * Sets the elevation.
     *
     * @param elevation the elevation
     */
    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, midiChannel, midiName, midiBank, midiProgram, midiUnpitched, volume, pan, elevation);
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
        MidiInstrumentImpl other = (MidiInstrumentImpl) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(midiChannel, other.midiChannel)
                && Objects.equals(midiName, other.midiName)
                && Objects.equals(midiBank, other.midiBank)
                && Objects.equals(midiProgram, other.midiProgram)
                && Objects.equals(midiUnpitched, other.midiUnpitched)
                && Objects.equals(volume, other.volume)
                && Objects.equals(pan, other.pan)
                && Objects.equals(elevation, other.elevation);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "MidiInstrumentImpl ["
                + "id=" + id
                + ", midiChannel=" + midiChannel
                + ", midiName=" + midiName
                + ", midiBank=" + midiBank
                + ", midiProgram=" + midiProgram
                + ", midiUnpitched=" + midiUnpitched
                + ", volume=" + volume
                + ", pan=" + pan
                + ", elevation=" + elevation
                + "]";
    }
}
