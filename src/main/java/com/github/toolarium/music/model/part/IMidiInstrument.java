/*
 * IMidiInstrument.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part;


/**
 * Represents the MusicXML midi-instrument element.
 *
 * @author patrick
 */
public interface IMidiInstrument {

    /**
     * Gets the id.
     *
     * @return the id
     */
    String getId();

    /**
     * Gets the MIDI channel.
     *
     * @return the MIDI channel
     */
    Integer getMidiChannel();

    /**
     * Gets the MIDI name.
     *
     * @return the MIDI name
     */
    String getMidiName();

    /**
     * Gets the MIDI bank.
     *
     * @return the MIDI bank
     */
    Integer getMidiBank();

    /**
     * Gets the MIDI program.
     *
     * @return the MIDI program
     */
    Integer getMidiProgram();

    /**
     * Gets the MIDI unpitched value.
     *
     * @return the MIDI unpitched value
     */
    Integer getMidiUnpitched();

    /**
     * Gets the volume.
     *
     * @return the volume
     */
    Double getVolume();

    /**
     * Gets the pan.
     *
     * @return the pan
     */
    Double getPan();

    /**
     * Gets the elevation.
     *
     * @return the elevation
     */
    Double getElevation();
}
