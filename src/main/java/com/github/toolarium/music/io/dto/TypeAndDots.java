/*
 * TypeAndDots.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import java.util.Objects;


/**
 * A note type with a number of dots.
 *
 * @author patrick
 */
public class TypeAndDots {
    private final String type;
    private final int dots;


    /**
     * Constructor for TypeAndDots.
     *
     * @param type the MusicXML note type
     * @param dots the number of dots
     */
    public TypeAndDots(String type, int dots) {
        this.type = type;
        this.dots = dots;
    }


    /**
     * Gets the MusicXML note type.
     *
     * @return the note type, for example quarter
     */
    public String getType() {
        return type;
    }


    /**
     * Gets the number of dots.
     *
     * @return the number of dots
     */
    public int getDots() {
        return dots;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, dots);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a TypeAndDots with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TypeAndDots other = (TypeAndDots) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(dots, other.dots);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "TypeAndDots ["
                + "type=" + type
                + ", dots=" + dots
                + "]";
    }
}
