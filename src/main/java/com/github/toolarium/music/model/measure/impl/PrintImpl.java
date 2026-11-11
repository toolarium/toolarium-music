/*
 * PrintImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.measure.IPrint;
import java.util.Objects;


/**
 * Implements the {@link IPrint} interface.
 *
 * @author patrick
 */
public class PrintImpl implements IPrint {

    @JacksonXmlProperty(isAttribute = true, localName = "new-system")
    private String newSystem;

    @JacksonXmlProperty(isAttribute = true, localName = "new-page")
    private String newPage;

    @JacksonXmlProperty(isAttribute = true, localName = "blank-page")
    private Integer blankPage;

    @JacksonXmlProperty(isAttribute = true, localName = "page-number")
    private Integer pageNumber;


    /**
     * Constructor.
     */
    public PrintImpl() {
    }


    /**
     * Gets the new system.
     *
     * @return the new system
     */
    @Override
    public String getNewSystem() {
        return newSystem;
    }


    /**
     * Sets the new system.
     *
     * @param newSystem the new system
     */
    public void setNewSystem(String newSystem) {
        this.newSystem = newSystem;
    }


    /**
     * Gets the new page.
     *
     * @return the new page
     */
    @Override
    public String getNewPage() {
        return newPage;
    }


    /**
     * Sets the new page.
     *
     * @param newPage the new page
     */
    public void setNewPage(String newPage) {
        this.newPage = newPage;
    }


    /**
     * Gets the blank page.
     *
     * @return the blank page
     */
    @Override
    public Integer getBlankPage() {
        return blankPage;
    }


    /**
     * Sets the blank page.
     *
     * @param blankPage the blank page
     */
    public void setBlankPage(Integer blankPage) {
        this.blankPage = blankPage;
    }


    /**
     * Gets the page number.
     *
     * @return the page number
     */
    @Override
    public Integer getPageNumber() {
        return pageNumber;
    }


    /**
     * Sets the page number.
     *
     * @param pageNumber the page number
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(newSystem, newPage, blankPage, pageNumber);
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
        PrintImpl other = (PrintImpl) obj;
        return Objects.equals(newSystem, other.newSystem)
                && Objects.equals(newPage, other.newPage)
                && Objects.equals(blankPage, other.blankPage)
                && Objects.equals(pageNumber, other.pageNumber);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "PrintImpl [newSystem=" + newSystem + ", newPage=" + newPage + ", blankPage=" + blankPage + ", pageNumber=" + pageNumber + "]";
    }
}
