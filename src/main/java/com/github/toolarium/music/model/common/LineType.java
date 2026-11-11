/*
 * LineType.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML line-type values.
 *
 * @author patrick
 */
public enum LineType {

    /** The solid line type. */
    @JsonProperty("solid")
    SOLID("solid"),

    /** The dashed line type. */
    @JsonProperty("dashed")
    DASHED("dashed"),

    /** The dotted line type. */
    @JsonProperty("dotted")
    DOTTED("dotted"),

    /** The wavy line type. */
    @JsonProperty("wavy")
    WAVY("wavy");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    LineType(String value) {
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
