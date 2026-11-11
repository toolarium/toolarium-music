/*
 * BeamImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.github.toolarium.music.model.note.IBeam;
import java.util.Objects;

/**
 * Implements the {@link IBeam} interface.
 *
 * @author patrick
 */
public class BeamImpl implements IBeam {

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private Integer number;

    @JacksonXmlText
    private String value;


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
        return Objects.hash(number, value);
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
        BeamImpl other = (BeamImpl) obj;
        return Objects.equals(number, other.number)
                && Objects.equals(value, other.value);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "BeamImpl [number=" + number + ", value=" + value + "]";
    }
}
