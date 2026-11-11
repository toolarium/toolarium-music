/*
 * IOrnaments.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation;


/**
 * Defines the Ornaments interface.
 *
 * @author patrick
 */
public interface IOrnaments {

    /**
     * Gets the trill mark.
     *
     * @return the trill mark
     */
    String getTrillMark();

    /**
     * Gets the turn.
     *
     * @return the turn
     */
    String getTurn();

    /**
     * Gets the delayed turn.
     *
     * @return the delayed turn
     */
    String getDelayedTurn();

    /**
     * Gets the inverted turn.
     *
     * @return the inverted turn
     */
    String getInvertedTurn();

    /**
     * Gets the delayed inverted turn.
     *
     * @return the delayed inverted turn
     */
    String getDelayedInvertedTurn();

    /**
     * Gets the shake.
     *
     * @return the shake
     */
    String getShake();

    /**
     * Gets the mordent.
     *
     * @return the mordent
     */
    String getMordent();

    /**
     * Gets the inverted mordent.
     *
     * @return the inverted mordent
     */
    String getInvertedMordent();

    /**
     * Gets the schleifer.
     *
     * @return the schleifer
     */
    String getSchleifer();

    /**
     * Gets the wavy line.
     *
     * @return the wavy line
     */
    String getWavyLine();
}
