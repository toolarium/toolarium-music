/*
 * OctaveShiftImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.direction.IOctaveShift;
import java.util.Objects;

/**
 * Implements the {@link IOctaveShift} interface.
 *
 * @author patrick
 */
public class OctaveShiftImpl implements IOctaveShift {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "size")
    private Integer size;


    /**
     * Gets the type.
     *
     * @return the type (up, down, stop, continue)
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type.
     *
     * @param type the type (up, down, stop, continue)
     */
    public void setType(String type) {
        this.type = type;
    }


    /**
     * Gets the size.
     *
     * @return the size (8, 15)
     */
    @Override
    public Integer getSize() {
        return size;
    }


    /**
     * Sets the size.
     *
     * @param size the size (8, 15)
     */
    public void setSize(Integer size) {
        this.size = size;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, size);
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
        OctaveShiftImpl other = (OctaveShiftImpl) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(size, other.size);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "OctaveShiftImpl [type=" + type + ", size=" + size + "]";
    }
}
