/*
 * IClef.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;


/**
 * Defines the Clef interface.
 *
 * @author patrick
 */
public interface IClef {

    /**
     * Gets the number.
     *
     * @return the number
     */
    Integer getNumber();

    /**
     * Gets the sign.
     *
     * @return the sign
     */
    String getSign();

    /**
     * Gets the line.
     *
     * @return the line
     */
    Integer getLine();

    /**
     * Gets the clef octave change.
     *
     * @return the clef octave change
     */
    Integer getClefOctaveChange();
}
