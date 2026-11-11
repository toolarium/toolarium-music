/*
 * BarStyleValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML bar-style values.
 *
 * @author patrick
 */
public enum BarStyleValue {

    /** The regular bar style. */
    @JsonProperty("regular")
    REGULAR("regular"),

    /** The dotted bar style. */
    @JsonProperty("dotted")
    DOTTED("dotted"),

    /** The dashed bar style. */
    @JsonProperty("dashed")
    DASHED("dashed"),

    /** The heavy bar style. */
    @JsonProperty("heavy")
    HEAVY("heavy"),

    /** The light-light bar style. */
    @JsonProperty("light-light")
    LIGHT_LIGHT("light-light"),

    /** The light-heavy bar style. */
    @JsonProperty("light-heavy")
    LIGHT_HEAVY("light-heavy"),

    /** The heavy-light bar style. */
    @JsonProperty("heavy-light")
    HEAVY_LIGHT("heavy-light"),

    /** The heavy-heavy bar style. */
    @JsonProperty("heavy-heavy")
    HEAVY_HEAVY("heavy-heavy"),

    /** The tick bar style. */
    @JsonProperty("tick")
    TICK("tick"),

    /** The short bar style. */
    @JsonProperty("short")
    SHORT("short"),

    /** The none bar style. */
    @JsonProperty("none")
    NONE("none");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    BarStyleValue(String value) {
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
