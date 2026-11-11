/*
 * IBackup.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;

import com.github.toolarium.music.model.common.IMusicDataElement;


/**
 * Represents the MusicXML backup element.
 *
 * @author patrick
 */
public interface IBackup extends IMusicDataElement {

    /**
     * Gets the duration.
     *
     * @return the duration
     */
    Integer getDuration();
}
