/*
 * WedgeImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.direction.IWedge;
import java.util.Objects;

/**
 * Implements the {@link IWedge} interface.
 *
 * @author patrick
 */
public class WedgeImpl implements IWedge {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private Integer number;

    @JacksonXmlProperty(isAttribute = true, localName = "spread")
    private String spread;


    /**
     * Gets the type.
     *
     * @return the type (crescendo, diminuendo, stop, continue)
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type.
     *
     * @param type the type (crescendo, diminuendo, stop, continue)
     */
    public void setType(String type) {
        this.type = type;
    }


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
     * Gets the spread.
     *
     * @return the spread
     */
    @Override
    public String getSpread() {
        return spread;
    }


    /**
     * Sets the spread.
     *
     * @param spread the spread
     */
    public void setSpread(String spread) {
        this.spread = spread;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, number, spread);
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
        WedgeImpl other = (WedgeImpl) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(number, other.number)
                && Objects.equals(spread, other.spread);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "WedgeImpl [type=" + type
                + ", number=" + number
                + ", spread=" + spread + "]";
    }
}
