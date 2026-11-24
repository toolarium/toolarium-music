/*
 * IMiscellaneousField.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score;


/**
 * Defines the miscellaneous field interface: a named value of the identification that has no own element,
 * for example the creation date of the score.
 *
 * @author patrick
 */
public interface IMiscellaneousField {

    /**
     * Gets the name.
     *
     * @return the name
     */
    String getName();


    /**
     * Gets the value.
     *
     * @return the value
     */
    String getValue();
}
