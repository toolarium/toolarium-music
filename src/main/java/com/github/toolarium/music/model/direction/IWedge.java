/*
 * IWedge.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction;


/**
 * Defines the Wedge interface.
 *
 * @author patrick
 */
public interface IWedge {

    /**
     * Gets the type.
     *
     * @return the type (crescendo, diminuendo, stop, continue)
     */
    String getType();

    /**
     * Gets the number.
     *
     * @return the number
     */
    Integer getNumber();

    /**
     * Gets the spread.
     *
     * @return the spread
     */
    String getSpread();
}
