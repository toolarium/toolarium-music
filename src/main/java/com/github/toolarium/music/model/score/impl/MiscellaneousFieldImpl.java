/*
 * MiscellaneousFieldImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.github.toolarium.music.model.score.IMiscellaneousField;
import java.util.Objects;

/**
 * Implements the {@link IMiscellaneousField} interface.
 *
 * @author patrick
 */
public class MiscellaneousFieldImpl implements IMiscellaneousField {

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlText
    private String value;


    /**
     * Constructor for MiscellaneousFieldImpl.
     */
    public MiscellaneousFieldImpl() {
        // default constructor
    }


    /**
     * Constructor for MiscellaneousFieldImpl.
     *
     * @param name the name
     * @param value the value
     */
    public MiscellaneousFieldImpl(String name, String value) {
        this.name = name;
        this.value = value;
    }


    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }


    /**
     * Sets the name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
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
        return Objects.hash(name, value);
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
        MiscellaneousFieldImpl other = (MiscellaneousFieldImpl) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(value, other.value);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "MiscellaneousFieldImpl [name=" + name + ", value=" + value + "]";
    }
}
