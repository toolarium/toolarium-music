/*
 * NoteheadValue.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the MusicXML notehead values.
 *
 * @author patrick
 */
public enum NoteheadValue {

    /** The slash notehead. */
    @JsonProperty("slash")
    SLASH("slash"),

    /** The triangle notehead. */
    @JsonProperty("triangle")
    TRIANGLE("triangle"),

    /** The diamond notehead. */
    @JsonProperty("diamond")
    DIAMOND("diamond"),

    /** The square notehead. */
    @JsonProperty("square")
    SQUARE("square"),

    /** The cross notehead. */
    @JsonProperty("cross")
    CROSS("cross"),

    /** The x notehead. */
    @JsonProperty("x")
    X("x"),

    /** The circle-x notehead. */
    @JsonProperty("circle-x")
    CIRCLE_X("circle-x"),

    /** The inverted triangle notehead. */
    @JsonProperty("inverted triangle")
    INVERTED_TRIANGLE("inverted triangle"),

    /** The arrow down notehead. */
    @JsonProperty("arrow down")
    ARROW_DOWN("arrow down"),

    /** The arrow up notehead. */
    @JsonProperty("arrow up")
    ARROW_UP("arrow up"),

    /** The circled notehead. */
    @JsonProperty("circled")
    CIRCLED("circled"),

    /** The slashed notehead. */
    @JsonProperty("slashed")
    SLASHED("slashed"),

    /** The back slashed notehead. */
    @JsonProperty("back slashed")
    BACK_SLASHED("back slashed"),

    /** The normal notehead. */
    @JsonProperty("normal")
    NORMAL("normal"),

    /** The cluster notehead. */
    @JsonProperty("cluster")
    CLUSTER("cluster"),

    /** The circle dot notehead. */
    @JsonProperty("circle dot")
    CIRCLE_DOT("circle dot"),

    /** The left triangle notehead. */
    @JsonProperty("left triangle")
    LEFT_TRIANGLE("left triangle"),

    /** The rectangle notehead. */
    @JsonProperty("rectangle")
    RECTANGLE("rectangle"),

    /** The none notehead. */
    @JsonProperty("none")
    NONE("none"),

    /** The do notehead. */
    @JsonProperty("do")
    DO("do"),

    /** The re notehead. */
    @JsonProperty("re")
    RE("re"),

    /** The mi notehead. */
    @JsonProperty("mi")
    MI("mi"),

    /** The fa notehead. */
    @JsonProperty("fa")
    FA("fa"),

    /** The fa up notehead. */
    @JsonProperty("fa up")
    FA_UP("fa up"),

    /** The so notehead. */
    @JsonProperty("so")
    SO("so"),

    /** The la notehead. */
    @JsonProperty("la")
    LA("la"),

    /** The ti notehead. */
    @JsonProperty("ti")
    TI("ti");

    private final String value;


    /**
     * Constructor.
     *
     * @param value the XML value
     */
    NoteheadValue(String value) {
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
