/*
 * UpDown.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML up-down values.
 *
 * @author patrick
 */
public enum UpDown {

    /** The up value. */
    @JsonProperty("up")
    UP("up"),

    /** The down value. */
    @JsonProperty("down")
    DOWN("down");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    UpDown(String value) {
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
