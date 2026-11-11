/*
 * IOctaveShift.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction;


/**
 * Represents the MusicXML octave-shift element.
 *
 * @author patrick
 */
public interface IOctaveShift {

    /**
     * Gets the type.
     *
     * @return the type (up, down, stop, continue)
     */
    String getType();

    /**
     * Gets the size.
     *
     * @return the size (8, 15)
     */
    Integer getSize();
}
