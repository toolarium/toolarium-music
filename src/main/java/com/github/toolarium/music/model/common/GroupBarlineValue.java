/*
 * GroupBarlineValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML group-barline values.
 *
 * @author patrick
 */
public enum GroupBarlineValue {

    /** The yes group barline value. */
    @JsonProperty("yes")
    YES("yes"),

    /** The no group barline value. */
    @JsonProperty("no")
    NO("no"),

    /** The Mensurstrich group barline value. */
    @JsonProperty("Mensurstrich")
    MENSURSTRICH("Mensurstrich");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    GroupBarlineValue(String value) {
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
