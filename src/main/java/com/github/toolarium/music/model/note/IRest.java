/*
 * IRest.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note;


/**
 * Defines the Rest interface.
 *
 * @author patrick
 */
public interface IRest {

    /**
     * Gets the display step.
     *
     * @return the display step
     */
    String getDisplayStep();

    /**
     * Gets the display octave.
     *
     * @return the display octave
     */
    Integer getDisplayOctave();

    /**
     * Gets the measure.
     *
     * @return the measure
     */
    String getMeasure();
}
