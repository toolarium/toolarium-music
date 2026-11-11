/*
 * IdentificationImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.score.IIdentification;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IIdentification} interface.
 *
 * @author patrick
 */
public class IdentificationImpl implements IIdentification {

    @JacksonXmlProperty(localName = "creator")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<CreatorImpl> creators;

    @JacksonXmlProperty(localName = "rights")
    private String rights;

    @JacksonXmlProperty(localName = "encoding")
    private EncodingImpl encoding;

    @JacksonXmlProperty(localName = "source")
    private String source;


    /**
     * Constructor.
     */
    public IdentificationImpl() {
        this.creators = new ArrayList<>();
    }


    /**
     * Gets the creators.
     *
     * @return the creators
     */
    @Override
    public List<CreatorImpl> getCreators() {
        return creators;
    }


    /**
     * Sets the creators.
     *
     * @param creators the creators
     */
    public void setCreators(List<CreatorImpl> creators) {
        this.creators = creators;
    }


    /**
     * Gets the rights.
     *
     * @return the rights
     */
    @Override
    public String getRights() {
        return rights;
    }


    /**
     * Sets the rights.
     *
     * @param rights the rights
     */
    public void setRights(String rights) {
        this.rights = rights;
    }


    /**
     * Gets the encoding.
     *
     * @return the encoding
     */
    @Override
    public EncodingImpl getEncoding() {
        return encoding;
    }


    /**
     * Sets the encoding.
     *
     * @param encoding the encoding
     */
    public void setEncoding(EncodingImpl encoding) {
        this.encoding = encoding;
    }


    /**
     * Gets the source.
     *
     * @return the source
     */
    @Override
    public String getSource() {
        return source;
    }


    /**
     * Sets the source.
     *
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(creators, rights, encoding, source);
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
        IdentificationImpl other = (IdentificationImpl) obj;
        return Objects.equals(creators, other.creators)
                && Objects.equals(rights, other.rights)
                && Objects.equals(encoding, other.encoding)
                && Objects.equals(source, other.source);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "IdentificationImpl [creators=" + creators
                + ", rights=" + rights
                + ", encoding=" + encoding
                + ", source=" + source + "]";
    }
}
