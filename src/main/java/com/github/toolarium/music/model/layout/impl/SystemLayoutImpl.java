/*
 * SystemLayoutImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.layout.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.layout.ISystemLayout;
import java.util.Objects;

/**
 * Implements the {@link ISystemLayout} interface.
 *
 * @author patrick
 */
public class SystemLayoutImpl implements ISystemLayout {

    @JacksonXmlProperty(localName = "system-margins")
    private SystemMarginsImpl systemMargins;

    @JacksonXmlProperty(localName = "system-distance")
    private Double systemDistance;

    @JacksonXmlProperty(localName = "top-system-distance")
    private Double topSystemDistance;


    /**
     * Constructor.
     */
    public SystemLayoutImpl() {
    }


    /**
     * Gets the system margins.
     *
     * @return the system margins
     */
    @Override
    public SystemMarginsImpl getSystemMargins() {
        return systemMargins;
    }


    /**
     * Sets the system margins.
     *
     * @param systemMargins the system margins
     */
    public void setSystemMargins(SystemMarginsImpl systemMargins) {
        this.systemMargins = systemMargins;
    }


    /**
     * Gets the system distance.
     *
     * @return the system distance
     */
    @Override
    public Double getSystemDistance() {
        return systemDistance;
    }


    /**
     * Sets the system distance.
     *
     * @param systemDistance the system distance
     */
    public void setSystemDistance(Double systemDistance) {
        this.systemDistance = systemDistance;
    }


    /**
     * Gets the top system distance.
     *
     * @return the top system distance
     */
    @Override
    public Double getTopSystemDistance() {
        return topSystemDistance;
    }


    /**
     * Sets the top system distance.
     *
     * @param topSystemDistance the top system distance
     */
    public void setTopSystemDistance(Double topSystemDistance) {
        this.topSystemDistance = topSystemDistance;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(systemMargins, systemDistance, topSystemDistance);
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
        SystemLayoutImpl other = (SystemLayoutImpl) obj;
        return Objects.equals(systemMargins, other.systemMargins)
                && Objects.equals(systemDistance, other.systemDistance)
                && Objects.equals(topSystemDistance, other.topSystemDistance);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "SystemLayoutImpl [systemMargins=" + systemMargins + ", systemDistance=" + systemDistance
                + ", topSystemDistance=" + topSystemDistance + "]";
    }
}
