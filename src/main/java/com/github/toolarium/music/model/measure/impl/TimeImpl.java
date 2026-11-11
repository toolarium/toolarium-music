/*
 * TimeImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.measure.ITime;
import java.util.Objects;

/**
 * Implements the {@link ITime} interface.
 *
 * @author patrick
 */
public class TimeImpl implements ITime {

    @JacksonXmlProperty(isAttribute = true, localName = "symbol")
    private String symbol;

    @JacksonXmlProperty(localName = "beats")
    private String beats;

    @JacksonXmlProperty(localName = "beat-type")
    private String beatType;


    /**
     * Gets the symbol.
     *
     * @return the symbol
     */
    @Override
    public String getSymbol() {
        return symbol;
    }


    /**
     * Sets the symbol.
     *
     * @param symbol the symbol
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }


    /**
     * Gets the beats.
     *
     * @return the beats
     */
    @Override
    public String getBeats() {
        return beats;
    }


    /**
     * Sets the beats.
     *
     * @param beats the beats
     */
    public void setBeats(String beats) {
        this.beats = beats;
    }


    /**
     * Gets the beat type.
     *
     * @return the beat type
     */
    @Override
    public String getBeatType() {
        return beatType;
    }


    /**
     * Sets the beat type.
     *
     * @param beatType the beat type
     */
    public void setBeatType(String beatType) {
        this.beatType = beatType;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(symbol, beats, beatType);
    }


    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param obj the reference object to compare
     * @return true if this object is the same as the obj argument, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TimeImpl other = (TimeImpl) obj;
        return Objects.equals(symbol, other.symbol)
                && Objects.equals(beats, other.beats)
                && Objects.equals(beatType, other.beatType);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "TimeImpl [symbol=" + symbol + ", beats=" + beats + ", beatType=" + beatType + "]";
    }
}
