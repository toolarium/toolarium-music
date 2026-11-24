/*
 * DefaultsImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.layout.impl.PageLayoutImpl;
import com.github.toolarium.music.model.score.IDefaults;
import java.util.Objects;

/**
 * Implements the {@link IDefaults} interface.
 *
 * @author patrick
 */
public class DefaultsImpl implements IDefaults {

    @JacksonXmlProperty(localName = "scaling")
    private ScalingImpl scaling;

    @JacksonXmlProperty(localName = "page-layout")
    private PageLayoutImpl pageLayout;


    /**
     * Gets the scaling.
     *
     * @return the scaling
     */
    @Override
    public ScalingImpl getScaling() {
        return scaling;
    }


    /**
     * Sets the scaling.
     *
     * @param scaling the scaling
     */
    public void setScaling(ScalingImpl scaling) {
        this.scaling = scaling;
    }


    /**
     * Gets the page layout.
     *
     * @return the page layout, or null
     */
    @Override
    public PageLayoutImpl getPageLayout() {
        return pageLayout;
    }


    /**
     * Sets the page layout.
     *
     * @param pageLayout the page layout
     */
    public void setPageLayout(PageLayoutImpl pageLayout) {
        this.pageLayout = pageLayout;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(scaling, pageLayout);
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
        DefaultsImpl other = (DefaultsImpl) obj;
        return Objects.equals(scaling, other.scaling) && Objects.equals(pageLayout, other.pageLayout);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "DefaultsImpl [scaling=" + scaling + ", pageLayout=" + pageLayout + "]";
    }
}
