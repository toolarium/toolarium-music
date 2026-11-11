/*
 * GroupSymbolValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML group-symbol values.
 *
 * @author patrick
 */
public enum GroupSymbolValue {

    /** The none group symbol. */
    @JsonProperty("none")
    NONE("none"),

    /** The brace group symbol. */
    @JsonProperty("brace")
    BRACE("brace"),

    /** The line group symbol. */
    @JsonProperty("line")
    LINE("line"),

    /** The bracket group symbol. */
    @JsonProperty("bracket")
    BRACKET("bracket"),

    /** The square group symbol. */
    @JsonProperty("square")
    SQUARE("square");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    GroupSymbolValue(String value) {
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
