/*
 * ITuplet.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation;


/**
 * Defines the Tuplet interface.
 *
 * @author patrick
 */
public interface ITuplet {

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
     * Gets the bracket.
     *
     * @return the bracket (yes, no)
     */
    String getBracket();

    /**
     * Gets the show number.
     *
     * @return the show number
     */
    String getShowNumber();

    /**
     * Gets the show type.
     *
     * @return the show type
     */
    String getShowType();
}
