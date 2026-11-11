/*
 * IScoreInstrument.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part;


/**
 * Represents the MusicXML score-instrument element.
 *
 * @author patrick
 */
public interface IScoreInstrument {

    /**
     * Gets the id.
     *
     * @return the id
     */
    String getId();

    /**
     * Gets the instrument name.
     *
     * @return the instrument name
     */
    String getInstrumentName();

    /**
     * Gets the instrument abbreviation.
     *
     * @return the instrument abbreviation
     */
    String getInstrumentAbbreviation();

    /**
     * Gets the instrument sound.
     *
     * @return the instrument sound
     */
    String getInstrumentSound();
}
