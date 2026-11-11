/*
 * WedgeType.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML wedge-type values.
 *
 * @author patrick
 */
public enum WedgeType {

    /** The crescendo wedge type. */
    @JsonProperty("crescendo")
    CRESCENDO("crescendo"),

    /** The diminuendo wedge type. */
    @JsonProperty("diminuendo")
    DIMINUENDO("diminuendo"),

    /** The stop wedge type. */
    @JsonProperty("stop")
    STOP("stop"),

    /** The continue wedge type. */
    @JsonProperty("continue")
    CONTINUE("continue");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    WedgeType(String value) {
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
