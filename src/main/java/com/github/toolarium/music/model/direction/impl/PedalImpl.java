/*
 * PedalImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.direction.IPedal;
import java.util.Objects;

/**
 * Implements the {@link IPedal} interface.
 *
 * @author patrick
 */
public class PedalImpl implements IPedal {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "line")
    private String line;

    @JacksonXmlProperty(isAttribute = true, localName = "sign")
    private String sign;


    /**
     * Gets the type.
     *
     * @return the type (start, stop, continue, change, sostenuto)
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type.
     *
     * @param type the type (start, stop, continue, change, sostenuto)
     */
    public void setType(String type) {
        this.type = type;
    }


    /**
     * Gets the line.
     *
     * @return the line (yes, no)
     */
    @Override
    public String getLine() {
        return line;
    }


    /**
     * Sets the line.
     *
     * @param line the line (yes, no)
     */
    public void setLine(String line) {
        this.line = line;
    }


    /**
     * Gets the sign.
     *
     * @return the sign (yes, no)
     */
    @Override
    public String getSign() {
        return sign;
    }


    /**
     * Sets the sign.
     *
     * @param sign the sign (yes, no)
     */
    public void setSign(String sign) {
        this.sign = sign;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, line, sign);
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
        PedalImpl other = (PedalImpl) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(line, other.line)
                && Objects.equals(sign, other.sign);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "PedalImpl [type=" + type
                + ", line=" + line
                + ", sign=" + sign + "]";
    }
}
