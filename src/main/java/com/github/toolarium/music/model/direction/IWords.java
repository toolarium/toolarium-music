/*
 * IWords.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction;


/**
 * Defines the Words interface.
 *
 * @author patrick
 */
public interface IWords {

    /**
     * Gets the value.
     *
     * @return the value
     */
    String getValue();

    /**
     * Gets the default x position.
     *
     * @return the default x position
     */
    String getDefaultX();

    /**
     * Gets the default y position.
     *
     * @return the default y position
     */
    String getDefaultY();

    /**
     * Gets the font size.
     *
     * @return the font size
     */
    String getFontSize();

    /**
     * Gets the font weight.
     *
     * @return the font weight
     */
    String getFontWeight();

    /**
     * Gets the font style.
     *
     * @return the font style
     */
    String getFontStyle();
}
