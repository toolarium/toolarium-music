/*
 * IDirection.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction;

import com.github.toolarium.music.model.common.IMusicDataElement;
import java.util.List;


/**
 * Represents the MusicXML direction element.
 *
 * @author patrick
 */
public interface IDirection extends IMusicDataElement {

    /**
     * Gets the placement.
     *
     * @return the placement (above, below)
     */
    String getPlacement();

    /**
     * Gets the list of direction type elements.
     *
     * @return the list of direction type elements
     */
    List<? extends IDirectionType> getDirectionTypes();

    /**
     * Gets the offset.
     *
     * @return the offset
     */
    Integer getOffset();

    /**
     * Gets the voice.
     *
     * @return the voice
     */
    Integer getVoice();

    /**
     * Gets the staff.
     *
     * @return the staff
     */
    Integer getStaff();

    /**
     * Gets the sound.
     *
     * @return the sound
     */
    ISound getSound();
}
