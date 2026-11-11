/*
 * IGlissando.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation;


/**
 * Defines the Glissando interface.
 *
 * @author patrick
 */
public interface IGlissando {

    /**
     * Gets the type.
     *
     * @return the type (start, stop)
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
     * Gets the value.
     *
     * @return the value
     */
    String getValue();
}
