/*
 * ClefSign.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML clef-sign values.
 *
 * @author patrick
 */
public enum ClefSign {

    /** The G clef sign. */
    @JsonProperty("G")
    G("G"),

    /** The F clef sign. */
    @JsonProperty("F")
    F("F"),

    /** The C clef sign. */
    @JsonProperty("C")
    C("C"),

    /** The percussion clef sign. */
    @JsonProperty("percussion")
    PERCUSSION("percussion"),

    /** The TAB clef sign. */
    @JsonProperty("TAB")
    TAB("TAB"),

    /** The jianpu clef sign. */
    @JsonProperty("jianpu")
    JIANPU("jianpu"),

    /** The none clef sign. */
    @JsonProperty("none")
    NONE("none");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    ClefSign(String value) {
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
