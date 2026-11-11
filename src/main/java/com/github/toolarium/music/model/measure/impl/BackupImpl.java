/*
 * BackupImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.measure.IBackup;
import java.util.Objects;


/**
 * Implements the {@link IBackup} interface.
 *
 * @author patrick
 */
public class BackupImpl implements IBackup {

    @JacksonXmlProperty(localName = "duration")
    private Integer duration;


    /**
     * Constructor.
     */
    public BackupImpl() {
    }


    /**
     * Gets the duration.
     *
     * @return the duration
     */
    @Override
    public Integer getDuration() {
        return duration;
    }


    /**
     * Sets the duration.
     *
     * @param duration the duration
     */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(duration);
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
        BackupImpl other = (BackupImpl) obj;
        return Objects.equals(duration, other.duration);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "BackupImpl [duration=" + duration + "]";
    }
}
