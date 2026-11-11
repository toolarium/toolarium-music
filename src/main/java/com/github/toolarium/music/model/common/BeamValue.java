/*
 * BeamValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML beam values.
 *
 * @author patrick
 */
public enum BeamValue {

    /** The begin beam value. */
    @JsonProperty("begin")
    BEGIN("begin"),

    /** The continue beam value. */
    @JsonProperty("continue")
    CONTINUE("continue"),

    /** The end beam value. */
    @JsonProperty("end")
    END("end"),

    /** The forward hook beam value. */
    @JsonProperty("forward hook")
    FORWARD_HOOK("forward hook"),

    /** The backward hook beam value. */
    @JsonProperty("backward hook")
    BACKWARD_HOOK("backward hook");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    BeamValue(String value) {
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
