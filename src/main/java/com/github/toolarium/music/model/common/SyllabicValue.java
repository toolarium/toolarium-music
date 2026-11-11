/*
 * SyllabicValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML syllabic values.
 *
 * @author patrick
 */
public enum SyllabicValue {

    /** The single syllabic value. */
    @JsonProperty("single")
    SINGLE("single"),

    /** The begin syllabic value. */
    @JsonProperty("begin")
    BEGIN("begin"),

    /** The end syllabic value. */
    @JsonProperty("end")
    END("end"),

    /** The middle syllabic value. */
    @JsonProperty("middle")
    MIDDLE("middle");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    SyllabicValue(String value) {
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
