/*
 * ISystemLayout.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.layout;


/**
 * Represents the MusicXML system-layout element.
 *
 * @author patrick
 */
public interface ISystemLayout {

    /**
     * Gets the system margins.
     *
     * @return the system margins
     */
    ISystemMargins getSystemMargins();

    /**
     * Gets the system distance.
     *
     * @return the system distance
     */
    Double getSystemDistance();

    /**
     * Gets the top system distance.
     *
     * @return the top system distance
     */
    Double getTopSystemDistance();
}
