/*
 * ClefImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.measure.IClef;
import java.util.Objects;

/**
 * Implements the {@link IClef} interface.
 *
 * @author patrick
 */
public class ClefImpl implements IClef {

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private Integer number;

    @JacksonXmlProperty(localName = "sign")
    private String sign;

    @JacksonXmlProperty(localName = "line")
    private Integer line;

    @JacksonXmlProperty(localName = "clef-octave-change")
    private Integer clefOctaveChange;


    /**
     * Gets the number.
     *
     * @return the number
     */
    @Override
    public Integer getNumber() {
        return number;
    }


    /**
     * Sets the number.
     *
     * @param number the number
     */
    public void setNumber(Integer number) {
        this.number = number;
    }


    /**
     * Gets the sign.
     *
     * @return the sign
     */
    @Override
    public String getSign() {
        return sign;
    }


    /**
     * Sets the sign.
     *
     * @param sign the sign
     */
    public void setSign(String sign) {
        this.sign = sign;
    }


    /**
     * Gets the line.
     *
     * @return the line
     */
    @Override
    public Integer getLine() {
        return line;
    }


    /**
     * Sets the line.
     *
     * @param line the line
     */
    public void setLine(Integer line) {
        this.line = line;
    }


    /**
     * Gets the clef octave change.
     *
     * @return the clef octave change
     */
    @Override
    public Integer getClefOctaveChange() {
        return clefOctaveChange;
    }


    /**
     * Sets the clef octave change.
     *
     * @param clefOctaveChange the clef octave change
     */
    public void setClefOctaveChange(Integer clefOctaveChange) {
        this.clefOctaveChange = clefOctaveChange;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(number, sign, line, clefOctaveChange);
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
        ClefImpl other = (ClefImpl) obj;
        return Objects.equals(number, other.number)
                && Objects.equals(sign, other.sign)
                && Objects.equals(line, other.line)
                && Objects.equals(clefOctaveChange, other.clefOctaveChange);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "ClefImpl [number=" + number + ", sign=" + sign + ", line=" + line + ", clefOctaveChange=" + clefOctaveChange + "]";
    }
}
