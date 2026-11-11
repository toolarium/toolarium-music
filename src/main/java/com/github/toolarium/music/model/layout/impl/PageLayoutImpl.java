/*
 * PageLayoutImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.layout.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.layout.IPageLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IPageLayout} interface.
 *
 * @author patrick
 */
public class PageLayoutImpl implements IPageLayout {

    @JacksonXmlProperty(localName = "page-height")
    private Double pageHeight;

    @JacksonXmlProperty(localName = "page-width")
    private Double pageWidth;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "page-margins")
    private List<PageMarginsImpl> pageMargins;


    /**
     * Constructor.
     */
    public PageLayoutImpl() {
        this.pageMargins = new ArrayList<>();
    }


    /**
     * Gets the page height.
     *
     * @return the page height
     */
    @Override
    public Double getPageHeight() {
        return pageHeight;
    }


    /**
     * Sets the page height.
     *
     * @param pageHeight the page height
     */
    public void setPageHeight(Double pageHeight) {
        this.pageHeight = pageHeight;
    }


    /**
     * Gets the page width.
     *
     * @return the page width
     */
    @Override
    public Double getPageWidth() {
        return pageWidth;
    }


    /**
     * Sets the page width.
     *
     * @param pageWidth the page width
     */
    public void setPageWidth(Double pageWidth) {
        this.pageWidth = pageWidth;
    }


    /**
     * Gets the page margins.
     *
     * @return the page margins
     */
    @Override
    public List<PageMarginsImpl> getPageMargins() {
        return pageMargins;
    }


    /**
     * Sets the page margins.
     *
     * @param pageMargins the page margins
     */
    public void setPageMargins(List<PageMarginsImpl> pageMargins) {
        this.pageMargins = pageMargins;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(pageHeight, pageWidth, pageMargins);
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
        PageLayoutImpl other = (PageLayoutImpl) obj;
        return Objects.equals(pageHeight, other.pageHeight)
                && Objects.equals(pageWidth, other.pageWidth)
                && Objects.equals(pageMargins, other.pageMargins);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "PageLayoutImpl [pageHeight=" + pageHeight + ", pageWidth=" + pageWidth + ", pageMargins=" + pageMargins + "]";
    }
}
