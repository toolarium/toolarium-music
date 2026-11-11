/*
 * IMidiDevice.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part;


/**
 * Represents the MusicXML midi-device element.
 *
 * @author patrick
 */
public interface IMidiDevice {

    /**
     * Gets the id.
     *
     * @return the id
     */
    String getId();

    /**
     * Gets the port.
     *
     * @return the port
     */
    Integer getPort();

    /**
     * Gets the text content value.
     *
     * @return the text content value
     */
    String getValue();
}
