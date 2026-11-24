/*
 * MeiWriterControl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * A control event to write into a measure (for example dynam, hairpin, slur, tempo, dir or harm): its element name,
 * its attributes in order and its text.
 *
 * @author patrick
 */
public class MeiWriterControl {
    private final String name;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private String text;


    /**
     * Constructor for MeiWriterControl.
     *
     * @param name the element name
     */
    public MeiWriterControl(String name) {
        this.name = name;
    }


    /**
     * Gets the element name.
     *
     * @return the element name
     */
    public String getName() {
        return name;
    }


    /**
     * Gets the attributes in order.
     *
     * @return the attribute values by name
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }


    /**
     * Sets an attribute; a null value is ignored.
     *
     * @param key the attribute name
     * @param value the attribute value, or null
     * @return this control
     */
    public MeiWriterControl put(String key, String value) {
        if (value != null) {
            attributes.put(key, value);
        }
        return this;
    }


    /**
     * Gets the text.
     *
     * @return the text, or null for an empty element
     */
    public String getText() {
        return text;
    }


    /**
     * Sets the text.
     *
     * @param text the text, or null for an empty element
     */
    public void setText(String text) {
        this.text = text;
    }


    /**
     * Calculates the hash code of all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, attributes, text);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MeiWriterControl other = (MeiWriterControl) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(attributes, other.attributes)
                && Objects.equals(text, other.text);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "MeiWriterControl ["
                + "name=" + name
                + ", attributes=" + attributes
                + ", text=" + text
                + "]";
    }
}
