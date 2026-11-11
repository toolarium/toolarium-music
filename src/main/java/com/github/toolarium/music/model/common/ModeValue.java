/*
 * ModeValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML mode values.
 *
 * @author patrick
 */
public enum ModeValue {

    /** The major mode. */
    @JsonProperty("major")
    MAJOR("major"),

    /** The minor mode. */
    @JsonProperty("minor")
    MINOR("minor"),

    /** The dorian mode. */
    @JsonProperty("dorian")
    DORIAN("dorian"),

    /** The phrygian mode. */
    @JsonProperty("phrygian")
    PHRYGIAN("phrygian"),

    /** The lydian mode. */
    @JsonProperty("lydian")
    LYDIAN("lydian"),

    /** The mixolydian mode. */
    @JsonProperty("mixolydian")
    MIXOLYDIAN("mixolydian"),

    /** The aeolian mode. */
    @JsonProperty("aeolian")
    AEOLIAN("aeolian"),

    /** The ionian mode. */
    @JsonProperty("ionian")
    IONIAN("ionian"),

    /** The locrian mode. */
    @JsonProperty("locrian")
    LOCRIAN("locrian"),

    /** The none mode. */
    @JsonProperty("none")
    NONE("none");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    ModeValue(String value) {
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
