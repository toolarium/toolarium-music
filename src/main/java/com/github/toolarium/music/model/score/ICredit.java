/*
 * ICredit.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score;


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
     * Gets the credit words.
     *
     * @return the credit words
     */
    ICreditWords getCreditWords();
}
