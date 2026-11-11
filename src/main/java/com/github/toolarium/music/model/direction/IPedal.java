/*
 * IPedal.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction;


/**
 * Defines the Pedal interface.
 *
 * @author patrick
 */
public interface IPedal {

    /**
     * Gets the type.
     *
     * @return the type (start, stop, continue, change, sostenuto)
     */
    String getType();

    /**
     * Gets the line.
     *
     * @return the line (yes, no)
     */
    String getLine();

    /**
     * Gets the sign.
     *
     * @return the sign (yes, no)
     */
    String getSign();
}
