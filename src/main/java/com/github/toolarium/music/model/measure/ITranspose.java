/*
 * ITranspose.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;


/**
 * Defines the Transpose interface.
 *
 * @author patrick
 */
public interface ITranspose {

    /**
     * Gets the diatonic.
     *
     * @return the diatonic
     */
    Integer getDiatonic();

    /**
     * Gets the chromatic.
     *
     * @return the chromatic
     */
    Integer getChromatic();

    /**
     * Gets the octave change.
     *
     * @return the octave change
     */
    Integer getOctaveChange();

    /**
     * Gets the double apply.
     *
     * @return the double apply
     */
    String getDoubleApply();
}
