/*
 * ISystemMargins.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.layout;


/**
 * Represents the MusicXML system-margins element.
 *
 * @author patrick
 */
public interface ISystemMargins {

    /**
     * Gets the left margin.
     *
     * @return the left margin
     */
    Double getLeftMargin();

    /**
     * Gets the right margin.
     *
     * @return the right margin
     */
    Double getRightMargin();
}
