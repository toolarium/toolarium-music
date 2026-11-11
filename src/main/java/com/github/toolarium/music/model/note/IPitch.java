/*
 * IPitch.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note;


/**
 * Defines the Pitch interface.
 *
 * @author patrick
 */
public interface IPitch {

    /**
     * Gets the step.
     *
     * @return the step
     */
    String getStep();

    /**
     * Gets the alter.
     *
     * @return the alter
     */
    Double getAlter();

    /**
     * Gets the octave.
     *
     * @return the octave
     */
    Integer getOctave();
}
