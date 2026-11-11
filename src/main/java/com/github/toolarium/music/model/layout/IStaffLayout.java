/*
 * IStaffLayout.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.layout;


/**
 * Represents the MusicXML staff-layout element.
 *
 * @author patrick
 */
public interface IStaffLayout {

    /**
     * Gets the number.
     *
     * @return the number
     */
    Integer getNumber();

    /**
     * Gets the staff distance.
     *
     * @return the staff distance
     */
    Double getStaffDistance();
}
