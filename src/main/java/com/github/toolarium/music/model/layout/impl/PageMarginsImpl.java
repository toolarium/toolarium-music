/*
 * PageMarginsImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.layout.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.layout.IPageMargins;
import java.util.Objects;

/**
 * Implements the {@link IPageMargins} interface.
 *
 * @author patrick
 */
public class PageMarginsImpl implements IPageMargins {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(localName = "left-margin")
    private Double leftMargin;

    @JacksonXmlProperty(localName = "right-margin")
    private Double rightMargin;

    @JacksonXmlProperty(localName = "top-margin")
    private Double topMargin;

    @JacksonXmlProperty(localName = "bottom-margin")
    private Double bottomMargin;


    /**
     * Constructor.
     */
    public PageMarginsImpl() {
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
     * Gets the top margin.
     *
     * @return the top margin
     */
    @Override
    public Double getTopMargin() {
        return topMargin;
    }


    /**
     * Sets the top margin.
     *
     * @param topMargin the top margin
     */
    public void setTopMargin(Double topMargin) {
        this.topMargin = topMargin;
    }


    /**
     * Gets the bottom margin.
     *
     * @return the bottom margin
     */
    @Override
    public Double getBottomMargin() {
        return bottomMargin;
    }


    /**
     * Sets the bottom margin.
     *
     * @param bottomMargin the bottom margin
     */
    public void setBottomMargin(Double bottomMargin) {
        this.bottomMargin = bottomMargin;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, leftMargin, rightMargin, topMargin, bottomMargin);
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
        PageMarginsImpl other = (PageMarginsImpl) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(leftMargin, other.leftMargin)
                && Objects.equals(rightMargin, other.rightMargin)
                && Objects.equals(topMargin, other.topMargin)
                && Objects.equals(bottomMargin, other.bottomMargin);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "PageMarginsImpl [type=" + type + ", leftMargin=" + leftMargin + ", rightMargin=" + rightMargin
                + ", topMargin=" + topMargin + ", bottomMargin=" + bottomMargin + "]";
    }
}
