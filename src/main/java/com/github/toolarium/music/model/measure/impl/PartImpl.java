/*
 * PartImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.measure.IPart;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IPart} interface.
 *
 * @author patrick
 */
public class PartImpl implements IPart {

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    private String id;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "measure")
    private List<MeasureImpl> measures;


    /**
     * Gets the id.
     *
     * @return the id
     */
    @Override
    public String getId() {
        return id;
    }


    /**
     * Sets the id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }


    /**
     * Gets the measures.
     *
     * @return the measures
     */
    @Override
    public List<MeasureImpl> getMeasures() {
        return measures;
    }


    /**
     * Sets the measures.
     *
     * @param measures the measures
     */
    public void setMeasures(List<MeasureImpl> measures) {
        this.measures = measures;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, measures);
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
        PartImpl other = (PartImpl) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(measures, other.measures);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "PartImpl [id=" + id + ", measures=" + measures + "]";
    }
}
