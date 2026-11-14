/*
 * FinaleContainer.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.finale.impl;

import com.github.toolarium.music.io.finale.IFinaleContainer;
import com.github.toolarium.music.io.finale.IFinaleMetadata;
import java.util.Arrays;
import java.util.Objects;

/**
 * Implementation of {@link IFinaleContainer}. Simple POJO holding the contents
 * of a Finale .musx or .mus file container.
 *
 * @author patrick
 */
public class FinaleContainer implements IFinaleContainer {
    private String mimetype;
    private String containerVersion;
    private String rootFilePath;
    private String rootMediaType;
    private IFinaleMetadata metadata;
    private byte[] scoreData;
    private boolean musx;
    private String enigmaHeader;

    /**
     * Default constructor.
     */
    public FinaleContainer() {
        // default
    }

    /**
     * Gets the mimetype of the container, e.g., "application/vnd.makemusic.notation".
     *
     * @return the mimetype
     */
    @Override
    public String getMimetype() {
        return mimetype;
    }

    /**
     * Sets the mimetype.
     *
     * @param mimetype the mimetype to set
     */
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Gets the container version from the container.xml version attribute.
     *
     * @return the container version
     */
    @Override
    public String getContainerVersion() {
        return containerVersion;
    }

    /**
     * Sets the container version.
     *
     * @param containerVersion the container version to set
     */
    public void setContainerVersion(String containerVersion) {
        this.containerVersion = containerVersion;
    }

    /**
     * Gets the root file path, e.g., "score.dat".
     *
     * @return the root file path
     */
    @Override
    public String getRootFilePath() {
        return rootFilePath;
    }

    /**
     * Sets the root file path.
     *
     * @param rootFilePath the root file path to set
     */
    public void setRootFilePath(String rootFilePath) {
        this.rootFilePath = rootFilePath;
    }

    /**
     * Gets the root media type, e.g., "application/vnd.makemusic.notation.dat.1".
     *
     * @return the root media type
     */
    @Override
    public String getRootMediaType() {
        return rootMediaType;
    }

    /**
     * Sets the root media type.
     *
     * @param rootMediaType the root media type to set
     */
    public void setRootMediaType(String rootMediaType) {
        this.rootMediaType = rootMediaType;
    }

    /**
     * Gets the notation metadata. May be null for .mus files.
     *
     * @return the metadata, or null if not available
     */
    @Override
    public IFinaleMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata.
     *
     * @param metadata the metadata to set
     */
    public void setMetadata(IFinaleMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets the raw score.dat binary data (Enigma format).
     *
     * @return the score data as a byte array
     */
    @Override
    public byte[] getScoreData() {
        return scoreData;
    }

    /**
     * Sets the raw score data.
     *
     * @param scoreData the score data to set
     */
    public void setScoreData(byte[] scoreData) {
        this.scoreData = scoreData;
    }

    /**
     * Checks whether this is a .musx (ZIP container) format.
     *
     * @return true if .musx, false if .mus (raw Enigma)
     */
    @Override
    public boolean isMUSX() {
        return musx;
    }

    /**
     * Sets whether this is a .musx format container.
     *
     * @param musx true if .musx, false if .mus
     */
    public void setMUSX(boolean musx) {
        this.musx = musx;
    }

    /**
     * Gets the Enigma header for .mus files.
     *
     * @return "ENIGMA BINARY FILE" for .mus files, null for .musx
     */
    @Override
    public String getEnigmaHeader() {
        return enigmaHeader;
    }

    /**
     * Sets the Enigma header.
     *
     * @param enigmaHeader the Enigma header to set
     */
    public void setEnigmaHeader(String enigmaHeader) {
        this.enigmaHeader = enigmaHeader;
    }

    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(mimetype, containerVersion, rootFilePath, rootMediaType, metadata, musx, enigmaHeader);
        result = 31 * result + Arrays.hashCode(scoreData);
        return result;
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
        FinaleContainer other = (FinaleContainer) obj;
        return musx == other.musx
                && Objects.equals(mimetype, other.mimetype)
                && Objects.equals(containerVersion, other.containerVersion)
                && Objects.equals(rootFilePath, other.rootFilePath)
                && Objects.equals(rootMediaType, other.rootMediaType)
                && Objects.equals(metadata, other.metadata)
                && Arrays.equals(scoreData, other.scoreData)
                && Objects.equals(enigmaHeader, other.enigmaHeader);
    }

    /**
     * Formats the score data length for display.
     *
     * @return the formatted score data length, or "null" if no data
     */
    private String formatScoreDataLength() {
        if (scoreData != null) {
            return scoreData.length + " bytes";
        }
        return "null";
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FinaleContainer [mimetype=" + mimetype
                + ", containerVersion=" + containerVersion
                + ", rootFilePath=" + rootFilePath
                + ", rootMediaType=" + rootMediaType
                + ", metadata=" + metadata
                + ", scoreData=" + formatScoreDataLength()
                + ", musx=" + musx
                + ", enigmaHeader=" + enigmaHeader + "]";
    }
}
