/*
 * ISlur.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation;


/**
 * Defines the Slur interface.
 *
 * @author patrick
 */
public interface ISlur {

    /**
     * Gets the type.
     *
     * @return the type (start, stop, continue)
     */
    String getType();

    /**
     * Gets the number.
     *
     * @return the number
     */
    Integer getNumber();

    /**
     * Gets the line type.
     *
     * @return the line type
     */
    String getLineType();

    /**
     * Gets the placement.
     *
     * @return the placement (above, below)
     */
    String getPlacement();
}
