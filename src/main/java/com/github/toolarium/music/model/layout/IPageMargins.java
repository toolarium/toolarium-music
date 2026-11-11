/*
 * IPageMargins.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.layout;


/**
 * Represents the MusicXML page-margins element.
 *
 * @author patrick
 */
public interface IPageMargins {

    /**
     * Gets the type.
     *
     * @return the type
     */
    String getType();

    /**
     * Gets the left margin.
     *
     * @return the left margin
     */
    Double getLeftMargin();

    /**
     * Gets the right margin.
     *
     * @return the right margin
     */
    Double getRightMargin();

    /**
     * Gets the top margin.
     *
     * @return the top margin
     */
    Double getTopMargin();

    /**
     * Gets the bottom margin.
     *
     * @return the bottom margin
     */
    Double getBottomMargin();
}
