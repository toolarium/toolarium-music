/*
 * BackwardForward.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML backward-forward values.
 *
 * @author patrick
 */
public enum BackwardForward {

    /** The backward value. */
    @JsonProperty("backward")
    BACKWARD("backward"),

    /** The forward value. */
    @JsonProperty("forward")
    FORWARD("forward");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    BackwardForward(String value) {
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
