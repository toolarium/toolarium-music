/*
 * ITime.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;


/**
 * Defines the Time interface.
 *
 * @author patrick
 */
public interface ITime {

    /**
     * Gets the symbol.
     *
     * @return the symbol
     */
    String getSymbol();

    /**
     * Gets the beats.
     *
     * @return the beats
     */
    String getBeats();

    /**
     * Gets the beat type.
     *
     * @return the beat type
     */
    String getBeatType();
}
