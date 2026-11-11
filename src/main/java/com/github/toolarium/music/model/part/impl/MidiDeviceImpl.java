/*
 * MidiDeviceImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.github.toolarium.music.model.part.IMidiDevice;
import java.util.Objects;

/**
 * Implements the {@link IMidiDevice} interface.
 *
 * @author patrick
 */
public class MidiDeviceImpl implements IMidiDevice {

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    private String id;

    @JacksonXmlProperty(isAttribute = true, localName = "port")
    private Integer port;

    @JacksonXmlText
    private String value;


    /**
     * Gets the id.
     *
     * @return the id
     */
    @Override
    public String getId() {
        return id;
    }


    /**
     * Sets the id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }


    /**
     * Gets the port.
     *
     * @return the port
     */
    @Override
    public Integer getPort() {
        return port;
    }


    /**
     * Sets the port.
     *
     * @param port the port
     */
    public void setPort(Integer port) {
        this.port = port;
    }


    /**
     * Gets the text content value.
     *
     * @return the text content value
     */
    @Override
    public String getValue() {
        return value;
    }


    /**
     * Sets the text content value.
     *
     * @param value the text content value
     */
    public void setValue(String value) {
        this.value = value;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, port, value);
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
        MidiDeviceImpl other = (MidiDeviceImpl) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(port, other.port)
                && Objects.equals(value, other.value);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "MidiDeviceImpl ["
                + "id=" + id
                + ", port=" + port
                + ", value=" + value
                + "]";
    }
}
