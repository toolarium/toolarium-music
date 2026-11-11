/*
 * EndingImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.barline.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.github.toolarium.music.model.barline.IEnding;
import java.util.Objects;

/**
 * Implements the {@link IEnding} interface.
 *
 * @author patrick
 */
public class EndingImpl implements IEnding {

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private String number;

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlText
    private String value;


    /**
     * Gets the number.
     *
     * @return the number
     */
    @Override
    public String getNumber() {
        return number;
    }


    /**
     * Sets the number.
     *
     * @param number the number
     */
    public void setNumber(String number) {
        this.number = number;
    }


    /**
     * Gets the type.
     *
     * @return the type
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
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
        return Objects.hash(number, type, value);
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
        EndingImpl other = (EndingImpl) obj;
        return Objects.equals(number, other.number)
                && Objects.equals(type, other.type)
                && Objects.equals(value, other.value);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "EndingImpl [number=" + number + ", type=" + type + ", value=" + value + "]";
    }
}
