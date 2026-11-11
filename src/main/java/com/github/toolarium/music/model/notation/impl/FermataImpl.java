/*
 * FermataImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.github.toolarium.music.model.notation.IFermata;
import java.util.Objects;

/**
 * Implements the {@link IFermata} interface.
 *
 * @author patrick
 */
public class FermataImpl implements IFermata {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlText
    private String value;


    /**
     * Gets the type.
     *
     * @return the type (upright, inverted)
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type.
     *
     * @param type the type (upright, inverted)
     */
    public void setType(String type) {
        this.type = type;
    }


    /**
     * Gets the value.
     *
     * @return the value (normal, angled, square, double-angled, double-square, double-dot, half-curve, curlew)
     */
    @Override
    public String getValue() {
        return value;
    }


    /**
     * Sets the value.
     *
     * @param value the value (normal, angled, square, double-angled, double-square, double-dot, half-curve, curlew)
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
        return Objects.hash(type, value);
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
        FermataImpl other = (FermataImpl) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(value, other.value);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FermataImpl [type=" + type + ", value=" + value + "]";
    }
}
