/*
 * IBarline.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.barline;

import com.github.toolarium.music.model.notation.IFermata;


/**
 * Defines the Barline interface.
 *
 * @author patrick
 */
public interface IBarline {

    /**
     * Gets the location.
     *
     * @return the location
     */
    String getLocation();

    /**
     * Gets the bar style.
     *
     * @return the bar style
     */
    String getBarStyle();

    /**
     * Gets the repeat.
     *
     * @return the repeat
     */
    IRepeat getRepeat();

    /**
     * Gets the ending.
     *
     * @return the ending
     */
    IEnding getEnding();

    /**
     * Gets the fermata.
     *
     * @return the fermata
     */
    IFermata getFermata();

    /**
     * Gets the segno.
     *
     * @return the segno
     */
    String getSegno();

    /**
     * Gets the coda.
     *
     * @return the coda
     */
    String getCoda();
}
