/*
 * GlissandoImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.github.toolarium.music.model.notation.IGlissando;
import java.util.Objects;

/**
 * Implements the {@link IGlissando} interface.
 *
 * @author patrick
 */
public class GlissandoImpl implements IGlissando {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private Integer number;

    @JacksonXmlProperty(isAttribute = true, localName = "line-type")
    private String lineType;

    @JacksonXmlText
    private String value;


    /**
     * Gets the type.
     *
     * @return the type (start, stop)
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type.
     *
     * @param type the type (start, stop)
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
     * Gets the value.
     *
     * @return the value
     */
    @Override
    public String getValue() {
        return value;
    }


    /**
     * Sets the value.
     *
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, number, lineType, value);
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
        GlissandoImpl other = (GlissandoImpl) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(number, other.number)
                && Objects.equals(lineType, other.lineType)
                && Objects.equals(value, other.value);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "GlissandoImpl [type=" + type
                + ", number=" + number
                + ", lineType=" + lineType
                + ", value=" + value + "]";
    }
}
