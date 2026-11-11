/*
 * IForward.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;

import com.github.toolarium.music.model.common.IMusicDataElement;


/**
 * Represents the MusicXML forward element.
 *
 * @author patrick
 */
public interface IForward extends IMusicDataElement {

    /**
     * Gets the duration.
     *
     * @return the duration
     */
    Integer getDuration();

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
}
