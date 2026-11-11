/*
 * EncodingImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.score.IEncoding;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IEncoding} interface.
 *
 * @author patrick
 */
public class EncodingImpl implements IEncoding {

    @JacksonXmlProperty(localName = "encoding-date")
    private String encodingDate;

    @JacksonXmlProperty(localName = "software")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<String> software;

    @JacksonXmlProperty(localName = "encoder")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<String> encoder;

    @JacksonXmlProperty(localName = "encoding-description")
    private String encodingDescription;


    /**
     * Constructor.
     */
    public EncodingImpl() {
        this.software = new ArrayList<>();
        this.encoder = new ArrayList<>();
    }


    /**
     * Gets the encoding date.
     *
     * @return the encoding date
     */
    @Override
    public String getEncodingDate() {
        return encodingDate;
    }


    /**
     * Sets the encoding date.
     *
     * @param encodingDate the encoding date
     */
    public void setEncodingDate(String encodingDate) {
        this.encodingDate = encodingDate;
    }


    /**
     * Gets the software list.
     *
     * @return the software list
     */
    @Override
    public List<String> getSoftware() {
        return software;
    }


    /**
     * Sets the software list.
     *
     * @param software the software list
     */
    public void setSoftware(List<String> software) {
        this.software = software;
    }


    /**
     * Gets the encoder list.
     *
     * @return the encoder list
     */
    @Override
    public List<String> getEncoder() {
        return encoder;
    }


    /**
     * Sets the encoder list.
     *
     * @param encoder the encoder list
     */
    public void setEncoder(List<String> encoder) {
        this.encoder = encoder;
    }


    /**
     * Gets the encoding description.
     *
     * @return the encoding description
     */
    @Override
    public String getEncodingDescription() {
        return encodingDescription;
    }


    /**
     * Sets the encoding description.
     *
     * @param encodingDescription the encoding description
     */
    public void setEncodingDescription(String encodingDescription) {
        this.encodingDescription = encodingDescription;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(encodingDate, software, encoder, encodingDescription);
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
        EncodingImpl other = (EncodingImpl) obj;
        return Objects.equals(encodingDate, other.encodingDate)
                && Objects.equals(software, other.software)
                && Objects.equals(encoder, other.encoder)
                && Objects.equals(encodingDescription, other.encodingDescription);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "EncodingImpl [encodingDate=" + encodingDate
                + ", software=" + software
                + ", encoder=" + encoder
                + ", encodingDescription=" + encodingDescription + "]";
    }
}
