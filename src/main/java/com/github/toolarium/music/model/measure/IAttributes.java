/*
 * IAttributes.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;

import com.github.toolarium.music.model.common.IMusicDataElement;
import java.util.List;


/**
 * Represents the MusicXML attributes element.
 *
 * @author patrick
 */
public interface IAttributes extends IMusicDataElement {

    /**
     * Gets the divisions.
     *
     * @return the divisions
     */
    Integer getDivisions();

    /**
     * Gets the keys.
     *
     * @return the keys
     */
    List<? extends IKey> getKeys();

    /**
     * Gets the times.
     *
     * @return the times
     */
    List<? extends ITime> getTimes();

    /**
     * Gets the staves.
     *
     * @return the staves
     */
    Integer getStaves();

    /**
     * Gets the clefs.
     *
     * @return the clefs
     */
    List<? extends IClef> getClefs();

    /**
     * Gets the transposes.
     *
     * @return the transposes
     */
    List<? extends ITranspose> getTransposes();

    /**
     * Gets the staff details.
     *
     * @return the staff details
     */
    List<? extends IStaffDetails> getStaffDetails();

    /**
     * Gets the instruments.
     *
     * @return the instruments
     */
    String getInstruments();
}
