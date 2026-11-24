/*
 * ICredit.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score;

import java.util.List;

/**
 * Defines the Credit interface.
 *
 * @author patrick
 */
public interface ICredit {

    /**
     * Gets the page.
     *
     * @return the page
     */
    Integer getPage();


    /**
     * Gets the credit type.
     *
     * @return the credit type
     */
    String getCreditType();


    /**
     * Gets the first credit words: the words with the position of the credit.
     *
     * @return the first credit words, or null if there are none
     */
    ICreditWords getCreditWords();


    /**
     * Gets all credit words: the words after the first one continue its text with their own font.
     *
     * @return the credit words, or null if there are none
     */
    List<? extends ICreditWords> getCreditWordsList();
}
