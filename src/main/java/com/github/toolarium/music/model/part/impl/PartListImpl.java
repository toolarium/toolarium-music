/*
 * PartListImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.part.IPartList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IPartList} interface.
 *
 * @author patrick
 */
public class PartListImpl implements IPartList {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "part-group")
    private List<PartGroupImpl> partGroups;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "score-part")
    private List<ScorePartImpl> scoreParts;


    /**
     * Constructor.
     */
    public PartListImpl() {
        this.partGroups = new ArrayList<>();
        this.scoreParts = new ArrayList<>();
    }


    /**
     * Gets the part groups.
     *
     * @return the part groups
     */
    @Override
    public List<PartGroupImpl> getPartGroups() {
        return partGroups;
    }


    /**
     * Sets the part groups.
     *
     * @param partGroups the part groups
     */
    public void setPartGroups(List<PartGroupImpl> partGroups) {
        this.partGroups = partGroups;
    }


    /**
     * Gets the score parts.
     *
     * @return the score parts
     */
    @Override
    public List<ScorePartImpl> getScoreParts() {
        return scoreParts;
    }


    /**
     * Sets the score parts.
     *
     * @param scoreParts the score parts
     */
    public void setScoreParts(List<ScorePartImpl> scoreParts) {
        this.scoreParts = scoreParts;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(partGroups, scoreParts);
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
        PartListImpl other = (PartListImpl) obj;
        return Objects.equals(partGroups, other.partGroups)
                && Objects.equals(scoreParts, other.scoreParts);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "PartListImpl ["
                + "partGroups=" + partGroups
                + ", scoreParts=" + scoreParts
                + "]";
    }
}
