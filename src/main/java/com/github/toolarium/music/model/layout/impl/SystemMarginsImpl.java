/*
 * SystemMarginsImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.layout.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.layout.ISystemMargins;
import java.util.Objects;

/**
 * Implements the {@link ISystemMargins} interface.
 *
 * @author patrick
 */
public class SystemMarginsImpl implements ISystemMargins {

    @JacksonXmlProperty(localName = "left-margin")
    private Double leftMargin;

    @JacksonXmlProperty(localName = "right-margin")
    private Double rightMargin;


    /**
     * Constructor.
     */
    public SystemMarginsImpl() {
    }


    /**
     * Gets the left margin.
     *
     * @return the left margin
     */
    @Override
    public Double getLeftMargin() {
        return leftMargin;
    }


    /**
     * Sets the left margin.
     *
     * @param leftMargin the left margin
     */
    public void setLeftMargin(Double leftMargin) {
        this.leftMargin = leftMargin;
    }


    /**
     * Gets the right margin.
     *
     * @return the right margin
     */
    @Override
    public Double getRightMargin() {
        return rightMargin;
    }


    /**
     * Sets the right margin.
     *
     * @param rightMargin the right margin
     */
    public void setRightMargin(Double rightMargin) {
        this.rightMargin = rightMargin;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(leftMargin, rightMargin);
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
        SystemMarginsImpl other = (SystemMarginsImpl) obj;
        return Objects.equals(leftMargin, other.leftMargin)
                && Objects.equals(rightMargin, other.rightMargin);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "SystemMarginsImpl [leftMargin=" + leftMargin + ", rightMargin=" + rightMargin + "]";
    }
}
