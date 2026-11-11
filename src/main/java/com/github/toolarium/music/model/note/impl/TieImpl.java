/*
 * TieImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.note.ITie;
import java.util.Objects;

/**
 * Implements the {@link ITie} interface.
 *
 * @author patrick
 */
public class TieImpl implements ITie {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;


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
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type);
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
        TieImpl other = (TieImpl) obj;
        return Objects.equals(type, other.type);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "TieImpl [type=" + type + "]";
    }
}
