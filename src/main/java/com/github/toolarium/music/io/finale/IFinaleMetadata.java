/*
 * IFinaleMetadata.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.io.finale;


/**
 * Defines the FinaleMetadata interface.
 *
 * @author patrick
 */
public interface IFinaleMetadata {

    /**
     * Gets the key signature.
     *
     * @return the key signature
     */
    String getKeySignature();

    /**
     * Gets the initial tempo.
     *
     * @return the initial tempo, or null if not available
     */
    Double getInitialTempo();

    /**
     * Gets the score duration.
     *
     * @return the score duration, or null if not available
     */
    Double getScoreDuration();

    /**
     * Gets the creator string.
     *
     * @return the creator string
     */
    String getCreatorString();
}
