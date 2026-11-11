/*
 * ILyric.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note;


/**
 * Defines the Lyric interface.
 *
 * @author patrick
 */
public interface ILyric {

    /**
     * Gets the number.
     *
     * @return the number
     */
    String getNumber();

    /**
     * Gets the name.
     *
     * @return the name
     */
    String getName();

    /**
     * Gets the syllabic.
     *
     * @return the syllabic
     */
    String getSyllabic();

    /**
     * Gets the text.
     *
     * @return the text
     */
    String getText();

    /**
     * Gets the elision.
     *
     * @return the elision
     */
    String getElision();

    /**
     * Gets the extend.
     *
     * @return the extend
     */
    String getExtend();
}
