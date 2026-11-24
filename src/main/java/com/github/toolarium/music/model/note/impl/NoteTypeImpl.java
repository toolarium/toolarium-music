/*
 * NoteTypeImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.util.Objects;


/**
 * The type element of a note: the graphic note type (for example quarter) and its size (for example cue).
 * It is the XML form of the type and type size of a {@link NoteImpl}.
 *
 * @author patrick
 */
final class NoteTypeImpl {

    @JacksonXmlProperty(isAttribute = true, localName = "size")
    private String size;

    @JacksonXmlText
    private String value;


    /**
     * Constructor for NoteTypeImpl.
     */
    NoteTypeImpl() {
        // NOP
    }


    /**
     * Constructor for NoteTypeImpl.
     *
     * @param value the note type
     * @param size the size, or null
     */
    NoteTypeImpl(String value, String size) {
        this.value = value;
        this.size = size;
    }


    /**
     * Gets the note type.
     *
     * @return the note type
     */
    String getValue() {
        return value;
    }


    /**
     * Gets the size.
     *
     * @return the size (full, cue, grace-cue or large), or null
     */
    String getSize() {
        return size;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(value, size);
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
        NoteTypeImpl other = (NoteTypeImpl) obj;
        return Objects.equals(value, other.value) && Objects.equals(size, other.size);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "NoteTypeImpl [value=" + value + ", size=" + size + "]";
    }
}
