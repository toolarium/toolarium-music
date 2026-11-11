/*
 * IKey.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;


/**
 * Defines the Key interface.
 *
 * @author patrick
 */
public interface IKey {

    /**
     * Gets the fifths.
     *
     * @return the fifths
     */
    Integer getFifths();

    /**
     * Gets the mode.
     *
     * @return the mode
     */
    String getMode();

    /**
     * Gets the cancel.
     *
     * @return the cancel
     */
    Integer getCancel();
}
