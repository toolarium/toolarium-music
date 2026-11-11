/*
 * IMetronome.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction;


/**
 * Defines the Metronome interface.
 *
 * @author patrick
 */
public interface IMetronome {

    /**
     * Gets the beat unit.
     *
     * @return the beat unit
     */
    String getBeatUnit();

    /**
     * Gets the beat unit dot.
     *
     * @return the beat unit dot
     */
    String getBeatUnitDot();

    /**
     * Gets the per minute.
     *
     * @return the per minute
     */
    String getPerMinute();

    /**
     * Gets the parentheses.
     *
     * @return the parentheses
     */
    String getParentheses();
}
