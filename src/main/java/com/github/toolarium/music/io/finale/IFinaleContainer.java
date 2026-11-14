/*
 * IFinaleContainer.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.io.finale;


/**
 * Defines the FinaleContainer interface.
 *
 * @author patrick
 */
public interface IFinaleContainer {

    /**
     * Gets the mimetype of the container, e.g., "application/vnd.makemusic.notation".
     *
     * @return the mimetype
     */
    String getMimetype();

    /**
     * Gets the container version from the container.xml version attribute.
     *
     * @return the container version
     */
    String getContainerVersion();

    /**
     * Gets the root file path, e.g., "score.dat".
     *
     * @return the root file path
     */
    String getRootFilePath();

    /**
     * Gets the root media type, e.g., "application/vnd.makemusic.notation.dat.1".
     *
     * @return the root media type
     */
    String getRootMediaType();

    /**
     * Gets the notation metadata. May be null for .mus files.
     *
     * @return the metadata, or null if not available
     */
    IFinaleMetadata getMetadata();

    /**
     * Gets the raw score.dat binary data (Enigma format).
     *
     * @return the score data as a byte array
     */
    byte[] getScoreData();

    /**
     * Checks whether this is a .musx (ZIP container) format.
     *
     * @return true if .musx, false if .mus (raw Enigma)
     */
    boolean isMUSX();

    /**
     * Gets the Enigma header for .mus files.
     *
     * @return "ENIGMA BINARY FILE" for .mus files, null for .musx
     */
    String getEnigmaHeader();
}
