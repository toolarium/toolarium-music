/*
 * IIdentification.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score;

import java.util.List;


/**
 * Defines the Identification interface.
 *
 * @author patrick
 */
public interface IIdentification {

    /**
     * Gets the creators.
     *
     * @return the creators
     */
    List<? extends ICreator> getCreators();


    /**
     * Gets the rights.
     *
     * @return the rights
     */
    String getRights();


    /**
     * Gets the encoding.
     *
     * @return the encoding
     */
    IEncoding getEncoding();


    /**
     * Gets the source.
     *
     * @return the source
     */
    String getSource();


    /**
     * Gets the miscellaneous fields: named values without an own element, for example the creation date.
     *
     * @return the miscellaneous fields, or null if there are none
     */
    List<? extends IMiscellaneousField> getMiscellaneous();
}
