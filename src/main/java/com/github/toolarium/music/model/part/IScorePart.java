/*
 * IScorePart.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part;

import java.util.List;


/**
 * Represents the MusicXML score-part element.
 *
 * @author patrick
 */
public interface IScorePart {

    /**
     * Gets the id.
     *
     * @return the id
     */
    String getId();

    /**
     * Gets the part name.
     *
     * @return the part name
     */
    String getPartName();

    /**
     * Gets the part abbreviation.
     *
     * @return the part abbreviation
     */
    String getPartAbbreviation();

    /**
     * Gets the score instruments.
     *
     * @return the score instruments
     */
    List<? extends IScoreInstrument> getScoreInstruments();

    /**
     * Gets the MIDI devices.
     *
     * @return the MIDI devices
     */
    List<? extends IMidiDevice> getMidiDevices();

    /**
     * Gets the MIDI instruments.
     *
     * @return the MIDI instruments
     */
    List<? extends IMidiInstrument> getMidiInstruments();
}
