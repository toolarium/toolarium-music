/*
 * IEncoding.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score;

import java.util.List;


/**
 * Defines the Encoding interface.
 *
 * @author patrick
 */
public interface IEncoding {

    /**
     * Gets the encoding date.
     *
     * @return the encoding date
     */
    String getEncodingDate();


    /**
     * Gets the software list.
     *
     * @return the software list
     */
    List<String> getSoftware();


    /**
     * Gets the encoder list.
     *
     * @return the encoder list
     */
    List<String> getEncoder();


    /**
     * Gets the encoding description.
     *
     * @return the encoding description
     */
    String getEncodingDescription();
}
