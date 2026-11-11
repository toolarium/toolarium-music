/*
 * SlurImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.notation.ISlur;
import java.util.Objects;

/**
 * Implements the {@link ISlur} interface.
 *
 * @author patrick
 */
public class SlurImpl implements ISlur {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private Integer number;

    @JacksonXmlProperty(isAttribute = true, localName = "line-type")
    private String lineType;

    @JacksonXmlProperty(isAttribute = true, localName = "placement")
    private String placement;


    /**
     * Gets the type.
     *
     * @return the type (start, stop, continue)
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type.
     *
     * @param type the type (start, stop, continue)
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
     * Gets the line type.
     *
     * @return the line type
     */
    @Override
    public String getLineType() {
        return lineType;
    }


    /**
     * Sets the line type.
     *
     * @param lineType the line type
     */
    public void setLineType(String lineType) {
        this.lineType = lineType;
    }


    /**
     * Gets the placement.
     *
     * @return the placement (above, below)
     */
    @Override
    public String getPlacement() {
        return placement;
    }


    /**
     * Sets the placement.
     *
     * @param placement the placement (above, below)
     */
    public void setPlacement(String placement) {
        this.placement = placement;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, number, lineType, placement);
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
        SlurImpl other = (SlurImpl) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(number, other.number)
                && Objects.equals(lineType, other.lineType)
                && Objects.equals(placement, other.placement);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "SlurImpl [type=" + type
                + ", number=" + number
                + ", lineType=" + lineType
                + ", placement=" + placement + "]";
    }
}
