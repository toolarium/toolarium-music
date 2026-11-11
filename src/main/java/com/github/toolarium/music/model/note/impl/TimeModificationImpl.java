/*
 * TimeModificationImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.note.ITimeModification;
import java.util.Objects;

/**
 * Implements the {@link ITimeModification} interface.
 *
 * @author patrick
 */
public class TimeModificationImpl implements ITimeModification {

    @JacksonXmlProperty(localName = "actual-notes")
    private Integer actualNotes;

    @JacksonXmlProperty(localName = "normal-notes")
    private Integer normalNotes;

    @JacksonXmlProperty(localName = "normal-type")
    private String normalType;


    /**
     * Gets the actual notes.
     *
     * @return the actual notes
     */
    @Override
    public Integer getActualNotes() {
        return actualNotes;
    }


    /**
     * Sets the actual notes.
     *
     * @param actualNotes the actual notes
     */
    public void setActualNotes(Integer actualNotes) {
        this.actualNotes = actualNotes;
    }


    /**
     * Gets the normal notes.
     *
     * @return the normal notes
     */
    @Override
    public Integer getNormalNotes() {
        return normalNotes;
    }


    /**
     * Sets the normal notes.
     *
     * @param normalNotes the normal notes
     */
    public void setNormalNotes(Integer normalNotes) {
        this.normalNotes = normalNotes;
    }


    /**
     * Gets the normal type.
     *
     * @return the normal type
     */
    @Override
    public String getNormalType() {
        return normalType;
    }


    /**
     * Sets the normal type.
     *
     * @param normalType the normal type
     */
    public void setNormalType(String normalType) {
        this.normalType = normalType;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(actualNotes, normalNotes, normalType);
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
        TimeModificationImpl other = (TimeModificationImpl) obj;
        return Objects.equals(actualNotes, other.actualNotes)
                && Objects.equals(normalNotes, other.normalNotes)
                && Objects.equals(normalType, other.normalType);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "TimeModificationImpl [actualNotes=" + actualNotes + ", normalNotes=" + normalNotes + ", normalType=" + normalType + "]";
    }
}
