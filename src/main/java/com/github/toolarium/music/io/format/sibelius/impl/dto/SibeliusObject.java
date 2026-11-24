/*
 * SibeliusObject.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl.dto;

import java.util.Objects;


/**
 * An object of the bar content stream of a Sibelius payload: the offset of the object in the payload, its type,
 * the staff (0 for the system staff), the bar (1 based), the position in the bar (in 1/256 quarter notes) and the voice
 * (1 to 4, or 15 for all voices).
 *
 * @author patrick
 */
public class SibeliusObject {
    private final int offset;
    private final int type;
    private final int staff;
    private final int bar;
    private final int position;
    private final int voice;


    /**
     * Constructor for SibeliusObject.
     *
     * @param offset the offset of the object in the payload
     * @param type the object type
     * @param staff the staff, 0 for the system staff
     * @param bar the bar, 1 based
     * @param position the position in the bar in 1/256 quarter notes
     * @param voice the voice (1 to 4, or 15 for all voices)
     */
    public SibeliusObject(int offset, int type, int staff, int bar, int position, int voice) {
        this.offset = offset;
        this.type = type;
        this.staff = staff;
        this.bar = bar;
        this.position = position;
        this.voice = voice;
    }


    /**
     * Gets the offset of the object in the payload.
     *
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }


    /**
     * Gets the object type (the first byte of the object).
     *
     * @return the type
     */
    public int getType() {
        return type;
    }


    /**
     * Gets the staff.
     *
     * @return the staff, 0 for the system staff
     */
    public int getStaff() {
        return staff;
    }


    /**
     * Gets the bar.
     *
     * @return the bar, 1 based
     */
    public int getBar() {
        return bar;
    }


    /**
     * Gets the position in the bar.
     *
     * @return the position in 1/256 quarter notes
     */
    public int getPosition() {
        return position;
    }


    /**
     * Gets the voice.
     *
     * @return the voice (1 to 4, or 15 for all voices)
     */
    public int getVoice() {
        return voice;
    }


    /**
     * Calculates the hash code.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(offset, type, staff, bar, position, voice);
    }


    /**
     * Compares this object with another object.
     *
     * @param obj the other object
     * @return true if both are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SibeliusObject other = (SibeliusObject) obj;
        return offset == other.offset && type == other.type && staff == other.staff && bar == other.bar
                && position == other.position && voice == other.voice;
    }


    /**
     * Gets the string representation.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SibeliusObject [offset=" + offset + ", type=" + type + ", staff=" + staff + ", bar=" + bar
                + ", position=" + position + ", voice=" + voice + "]";
    }
}
