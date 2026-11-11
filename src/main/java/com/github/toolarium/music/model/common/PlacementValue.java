/*
 * PlacementValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML placement values.
 *
 * @author patrick
 */
public enum PlacementValue {

    /** The above placement. */
    @JsonProperty("above")
    ABOVE("above"),

    /** The below placement. */
    @JsonProperty("below")
    BELOW("below");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    PlacementValue(String value) {
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
