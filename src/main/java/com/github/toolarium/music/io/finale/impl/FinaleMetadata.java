/*
 * FinaleMetadata.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.finale.impl;

import com.github.toolarium.music.io.finale.IFinaleMetadata;
import java.util.Objects;

/**
 * Implementation of {@link IFinaleMetadata}. Simple POJO holding metadata
 * extracted from NotationMetadata.xml inside a .musx file.
 *
 * @author patrick
 */
public class FinaleMetadata implements IFinaleMetadata {
    private String keySignature;
    private Double initialTempo;
    private Double scoreDuration;
    private String creatorString;

    /**
     * Default constructor.
     */
    public FinaleMetadata() {
        // default
    }

    /**
     * Gets the key signature.
     *
     * @return the key signature
     */
    @Override
    public String getKeySignature() {
        return keySignature;
    }

    /**
     * Sets the key signature.
     *
     * @param keySignature the key signature to set
     */
    public void setKeySignature(String keySignature) {
        this.keySignature = keySignature;
    }

    /**
     * Gets the initial tempo.
     *
     * @return the initial tempo, or null if not available
     */
    @Override
    public Double getInitialTempo() {
        return initialTempo;
    }

    /**
     * Sets the initial tempo.
     *
     * @param initialTempo the initial tempo to set
     */
    public void setInitialTempo(Double initialTempo) {
        this.initialTempo = initialTempo;
    }

    /**
     * Gets the score duration.
     *
     * @return the score duration, or null if not available
     */
    @Override
    public Double getScoreDuration() {
        return scoreDuration;
    }

    /**
     * Sets the score duration.
     *
     * @param scoreDuration the score duration to set
     */
    public void setScoreDuration(Double scoreDuration) {
        this.scoreDuration = scoreDuration;
    }

    /**
     * Gets the creator string.
     *
     * @return the creator string
     */
    @Override
    public String getCreatorString() {
        return creatorString;
    }

    /**
     * Sets the creator string.
     *
     * @param creatorString the creator string to set
     */
    public void setCreatorString(String creatorString) {
        this.creatorString = creatorString;
    }

    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(keySignature, initialTempo, scoreDuration, creatorString);
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
        FinaleMetadata other = (FinaleMetadata) obj;
        return Objects.equals(keySignature, other.keySignature)
                && Objects.equals(initialTempo, other.initialTempo)
                && Objects.equals(scoreDuration, other.scoreDuration)
                && Objects.equals(creatorString, other.creatorString);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FinaleMetadata [keySignature=" + keySignature
                + ", initialTempo=" + initialTempo
                + ", scoreDuration=" + scoreDuration
                + ", creatorString=" + creatorString + "]";
    }
}
