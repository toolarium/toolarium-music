/*
 * StemValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML stem values.
 *
 * @author patrick
 */
public enum StemValue {

    /** The up stem value. */
    @JsonProperty("up")
    UP("up"),

    /** The down stem value. */
    @JsonProperty("down")
    DOWN("down"),

    /** The double stem value. */
    @JsonProperty("double")
    DOUBLE("double"),

    /** The none stem value. */
    @JsonProperty("none")
    NONE("none");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    StemValue(String value) {
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
