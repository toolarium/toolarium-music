/*
 * StartStopContinue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML start-stop-continue values.
 *
 * @author patrick
 */
public enum StartStopContinue {

    /** The start value. */
    @JsonProperty("start")
    START("start"),

    /** The stop value. */
    @JsonProperty("stop")
    STOP("stop"),

    /** The continue value. */
    @JsonProperty("continue")
    CONTINUE("continue");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    StartStopContinue(String value) {
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
