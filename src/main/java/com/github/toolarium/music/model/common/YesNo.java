/*
 * YesNo.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML yes-no values.
 *
 * @author patrick
 */
public enum YesNo {

    /** The yes value. */
    @JsonProperty("yes")
    YES("yes"),

    /** The no value. */
    @JsonProperty("no")
    NO("no");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    YesNo(String value) {
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
