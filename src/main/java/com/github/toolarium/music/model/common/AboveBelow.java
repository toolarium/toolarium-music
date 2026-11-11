/*
 * AboveBelow.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML above-below values.
 *
 * @author patrick
 */
public enum AboveBelow {

    /** The above value. */
    @JsonProperty("above")
    ABOVE("above"),

    /** The below value. */
    @JsonProperty("below")
    BELOW("below");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    AboveBelow(String value) {
        this.value = value;
    }


    /**
     * Gets the XML value.
     *
     * @return the XML value
     */
    public String getValue() {
        return value;
    }
}
