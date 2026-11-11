/*
 * WorkImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.score.IWork;
import java.util.Objects;

/**
 * Implements the {@link IWork} interface.
 *
 * @author patrick
 */
public class WorkImpl implements IWork {

    @JacksonXmlProperty(localName = "work-number")
    private String workNumber;

    @JacksonXmlProperty(localName = "work-title")
    private String workTitle;


    /**
     * Gets the work number.
     *
     * @return the work number
     */
    @Override
    public String getWorkNumber() {
        return workNumber;
    }


    /**
     * Sets the work number.
     *
     * @param workNumber the work number
     */
    public void setWorkNumber(String workNumber) {
        this.workNumber = workNumber;
    }


    /**
     * Gets the work title.
     *
     * @return the work title
     */
    @Override
    public String getWorkTitle() {
        return workTitle;
    }


    /**
     * Sets the work title.
     *
     * @param workTitle the work title
     */
    public void setWorkTitle(String workTitle) {
        this.workTitle = workTitle;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(workNumber, workTitle);
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
        WorkImpl other = (WorkImpl) obj;
        return Objects.equals(workNumber, other.workNumber)
                && Objects.equals(workTitle, other.workTitle);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "WorkImpl [workNumber=" + workNumber + ", workTitle=" + workTitle + "]";
    }
}
