/*
 * NoteTypeValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML note-type values.
 *
 * @author patrick
 */
public enum NoteTypeValue {

    /** The maxima note type. */
    @JsonProperty("maxima")
    MAXIMA("maxima"),

    /** The long note type. */
    @JsonProperty("long")
    LONG("long"),

    /** The breve note type. */
    @JsonProperty("breve")
    BREVE("breve"),

    /** The whole note type. */
    @JsonProperty("whole")
    WHOLE("whole"),

    /** The half note type. */
    @JsonProperty("half")
    HALF("half"),

    /** The quarter note type. */
    @JsonProperty("quarter")
    QUARTER("quarter"),

    /** The eighth note type. */
    @JsonProperty("eighth")
    EIGHTH("eighth"),

    /** The 16th note type. */
    @JsonProperty("16th")
    SIXTEENTH("16th"),

    /** The 32nd note type. */
    @JsonProperty("32nd")
    THIRTY_SECOND("32nd"),

    /** The 64th note type. */
    @JsonProperty("64th")
    SIXTY_FOURTH("64th"),

    /** The 128th note type. */
    @JsonProperty("128th")
    ONE_HUNDRED_TWENTY_EIGHTH("128th"),

    /** The 256th note type. */
    @JsonProperty("256th")
    TWO_HUNDRED_FIFTY_SIXTH("256th"),

    /** The 512th note type. */
    @JsonProperty("512th")
    FIVE_HUNDRED_TWELFTH("512th"),

    /** The 1024th note type. */
    @JsonProperty("1024th")
    ONE_THOUSAND_TWENTY_FOURTH("1024th");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    NoteTypeValue(String value) {
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
