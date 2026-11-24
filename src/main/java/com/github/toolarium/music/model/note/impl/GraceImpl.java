/*
 * GraceImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.note.IGrace;
import java.util.Objects;

/**
 * Implements the {@link IGrace} interface.
 *
 * @author patrick
 */
public class GraceImpl implements IGrace {

    @JacksonXmlProperty(isAttribute = true, localName = "slash")
    private String slash;

    @JacksonXmlProperty(isAttribute = true, localName = "steal-time-previous")
    private String stealTimePrevious;

    @JacksonXmlProperty(isAttribute = true, localName = "steal-time-following")
    private String stealTimeFollowing;

    @JacksonXmlProperty(isAttribute = true, localName = "make-time")
    private String makeTime;


    /**
     * Gets the slash attribute.
     *
     * @return the slash value
     */
    @Override
    public String getSlash() {
        return slash;
    }


    /**
     * Sets the slash attribute.
     *
     * @param slash the slash value
     */
    public void setSlash(String slash) {
        this.slash = slash;
    }


    /**
     * Gets the steal-time-previous attribute.
     *
     * @return the steal-time-previous value
     */
    @Override
    public String getStealTimePrevious() {
        return stealTimePrevious;
    }


    /**
     * Sets the steal-time-previous attribute.
     *
     * @param stealTimePrevious the steal-time-previous value
     */
    public void setStealTimePrevious(String stealTimePrevious) {
        this.stealTimePrevious = stealTimePrevious;
    }


    /**
     * Gets the steal-time-following attribute.
     *
     * @return the steal-time-following value
     */
    @Override
    public String getStealTimeFollowing() {
        return stealTimeFollowing;
    }


    /**
     * Sets the steal-time-following attribute.
     *
     * @param stealTimeFollowing the steal-time-following value
     */
    public void setStealTimeFollowing(String stealTimeFollowing) {
        this.stealTimeFollowing = stealTimeFollowing;
    }


    /**
     * Gets the make-time attribute.
     *
     * @return the make-time value
     */
    @Override
    public String getMakeTime() {
        return makeTime;
    }


    /**
     * Sets the make-time attribute.
     *
     * @param makeTime the make-time value
     */
    public void setMakeTime(String makeTime) {
        this.makeTime = makeTime;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(slash, stealTimePrevious, stealTimeFollowing, makeTime);
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
        GraceImpl other = (GraceImpl) obj;
        return Objects.equals(slash, other.slash)
                && Objects.equals(stealTimePrevious, other.stealTimePrevious)
                && Objects.equals(stealTimeFollowing, other.stealTimeFollowing)
                && Objects.equals(makeTime, other.makeTime);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "GraceImpl [slash=" + slash + ", stealTimePrevious=" + stealTimePrevious
                + ", stealTimeFollowing=" + stealTimeFollowing + ", makeTime=" + makeTime + "]";
    }
}
