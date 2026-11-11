/*
 * IRepeat.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.barline;


/**
 * Defines the Repeat interface.
 *
 * @author patrick
 */
public interface IRepeat {

    /**
     * Gets the direction.
     *
     * @return the direction
     */
    String getDirection();

    /**
     * Gets the times.
     *
     * @return the times
     */
    Integer getTimes();

    /**
     * Gets the winged.
     *
     * @return the winged
     */
    String getWinged();
}
