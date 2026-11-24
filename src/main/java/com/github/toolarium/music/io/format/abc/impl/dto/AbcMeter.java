/*
 * AbcMeter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.Objects;


/**
 * An ABC meter: beats, beat type and an optional symbol. Free meter has zero beats.
 *
 * @author patrick
 */
public class AbcMeter {
    private final int beats;
    private final int beatType;
    private final String symbol;


    /**
     * Constructor for AbcMeter.
     *
     * @param beats the beats (0 for free meter)
     * @param beatType the beat type
     * @param symbol the time symbol (common, cut or null)
     */
    public AbcMeter(int beats, int beatType, String symbol) {
        this.beats = beats;
        this.beatType = beatType;
        this.symbol = symbol;
    }


    /**
     * Gets the beats (0 for free meter).
     *
     * @return the beats (0 for free meter)
     */
    public int getBeats() {
        return beats;
    }


    /**
     * Gets the beat type.
     *
     * @return the beat type
     */
    public int getBeatType() {
        return beatType;
    }


    /**
     * Gets the time symbol (common, cut or null).
     *
     * @return the time symbol (common, cut or null)
     */
    public String getSymbol() {
        return symbol;
    }


    /**
     * Checks if the meter is free (M:none).
     *
     * @return true for free meter
     */
    public boolean isFree() {
        return beats <= 0;
    }


    /**
     * Checks if the meter is compound (6/8, 9/8, 12/8 and so on).
     *
     * @return true for a compound meter
     */
    public boolean isCompound() {
        return beats > 3 && beats % 3 == 0 && beatType >= 8;
    }


    /**
     * Gets the length of a measure.
     *
     * @return the length in whole notes (zero for free meter)
     */
    public Fraction getLength() {
        if (isFree()) {
            return Fraction.ZERO;
        }
        return Fraction.of(beats, beatType);
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(beats, beatType, symbol);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a AbcMeter with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbcMeter other = (AbcMeter) obj;
        return Objects.equals(beats, other.beats)
                && Objects.equals(beatType, other.beatType)
                && Objects.equals(symbol, other.symbol);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "AbcMeter ["
                + "beats=" + beats
                + ", beatType=" + beatType
                + ", symbol=" + symbol
                + "]";
    }
}
