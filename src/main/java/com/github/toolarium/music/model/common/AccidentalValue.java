/*
 * AccidentalValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML accidental values.
 *
 * @author patrick
 */
public enum AccidentalValue {

    /** The sharp accidental. */
    @JsonProperty("sharp")
    SHARP("sharp"),

    /** The natural accidental. */
    @JsonProperty("natural")
    NATURAL("natural"),

    /** The flat accidental. */
    @JsonProperty("flat")
    FLAT("flat"),

    /** The double-sharp accidental. */
    @JsonProperty("double-sharp")
    DOUBLE_SHARP("double-sharp"),

    /** The sharp-sharp accidental. */
    @JsonProperty("sharp-sharp")
    SHARP_SHARP("sharp-sharp"),

    /** The flat-flat accidental. */
    @JsonProperty("flat-flat")
    FLAT_FLAT("flat-flat"),

    /** The natural-sharp accidental. */
    @JsonProperty("natural-sharp")
    NATURAL_SHARP("natural-sharp"),

    /** The natural-flat accidental. */
    @JsonProperty("natural-flat")
    NATURAL_FLAT("natural-flat"),

    /** The quarter-flat accidental. */
    @JsonProperty("quarter-flat")
    QUARTER_FLAT("quarter-flat"),

    /** The quarter-sharp accidental. */
    @JsonProperty("quarter-sharp")
    QUARTER_SHARP("quarter-sharp"),

    /** The three-quarters-flat accidental. */
    @JsonProperty("three-quarters-flat")
    THREE_QUARTERS_FLAT("three-quarters-flat"),

    /** The three-quarters-sharp accidental. */
    @JsonProperty("three-quarters-sharp")
    THREE_QUARTERS_SHARP("three-quarters-sharp"),

    /** The double-flat accidental. */
    @JsonProperty("double-flat")
    DOUBLE_FLAT("double-flat");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    AccidentalValue(String value) {
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
