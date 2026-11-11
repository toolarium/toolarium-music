/*
 * DegreeImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.harmony.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.harmony.IDegree;
import java.util.Objects;

/**
 * Implements the {@link IDegree} interface.
 *
 * @author patrick
 */
public class DegreeImpl implements IDegree {

    @JacksonXmlProperty(localName = "degree-value")
    private String degreeValue;

    @JacksonXmlProperty(localName = "degree-alter")
    private String degreeAlter;

    @JacksonXmlProperty(localName = "degree-type")
    private String degreeType;


    /**
     * Gets the degree value.
     *
     * @return the degree value
     */
    @Override
    public String getDegreeValue() {
        return degreeValue;
    }


    /**
     * Sets the degree value.
     *
     * @param degreeValue the degree value
     */
    public void setDegreeValue(String degreeValue) {
        this.degreeValue = degreeValue;
    }


    /**
     * Gets the degree alter.
     *
     * @return the degree alter
     */
    @Override
    public String getDegreeAlter() {
        return degreeAlter;
    }


    /**
     * Sets the degree alter.
     *
     * @param degreeAlter the degree alter
     */
    public void setDegreeAlter(String degreeAlter) {
        this.degreeAlter = degreeAlter;
    }


    /**
     * Gets the degree type.
     *
     * @return the degree type
     */
    @Override
    public String getDegreeType() {
        return degreeType;
    }


    /**
     * Sets the degree type.
     *
     * @param degreeType the degree type
     */
    public void setDegreeType(String degreeType) {
        this.degreeType = degreeType;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(degreeValue, degreeAlter, degreeType);
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
        DegreeImpl other = (DegreeImpl) obj;
        return Objects.equals(degreeValue, other.degreeValue)
                && Objects.equals(degreeAlter, other.degreeAlter)
                && Objects.equals(degreeType, other.degreeType);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "DegreeImpl [degreeValue=" + degreeValue + ", degreeAlter=" + degreeAlter + ", degreeType=" + degreeType + "]";
    }
}
