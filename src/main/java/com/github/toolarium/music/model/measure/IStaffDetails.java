/*
 * IStaffDetails.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;


/**
 * Represents the MusicXML staff-details element.
 *
 * @author patrick
 */
public interface IStaffDetails {

    /**
     * Gets the number.
     *
     * @return the number
     */
    Integer getNumber();

    /**
     * Gets the staff lines.
     *
     * @return the staff lines
     */
    Integer getStaffLines();

    /**
     * Gets the staff type.
     *
     * @return the staff type
     */
    String getStaffType();
}
