/*
 * KeyImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.measure.IKey;
import java.util.Objects;

/**
 * Implements the {@link IKey} interface.
 *
 * @author patrick
 */
public class KeyImpl implements IKey {

    @JacksonXmlProperty(localName = "fifths")
    private Integer fifths;

    @JacksonXmlProperty(localName = "mode")
    private String mode;

    @JacksonXmlProperty(localName = "cancel")
    private Integer cancel;


    /**
     * Gets the fifths.
     *
     * @return the fifths
     */
    @Override
    public Integer getFifths() {
        return fifths;
    }


    /**
     * Sets the fifths.
     *
     * @param fifths the fifths
     */
    public void setFifths(Integer fifths) {
        this.fifths = fifths;
    }


    /**
     * Gets the mode.
     *
     * @return the mode
     */
    @Override
    public String getMode() {
        return mode;
    }


    /**
     * Sets the mode.
     *
     * @param mode the mode
     */
    public void setMode(String mode) {
        this.mode = mode;
    }


    /**
     * Gets the cancel.
     *
     * @return the cancel
     */
    @Override
    public Integer getCancel() {
        return cancel;
    }


    /**
     * Sets the cancel.
     *
     * @param cancel the cancel
     */
    public void setCancel(Integer cancel) {
        this.cancel = cancel;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(fifths, mode, cancel);
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
        KeyImpl other = (KeyImpl) obj;
        return Objects.equals(fifths, other.fifths)
                && Objects.equals(mode, other.mode)
                && Objects.equals(cancel, other.cancel);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "KeyImpl [fifths=" + fifths + ", mode=" + mode + ", cancel=" + cancel + "]";
    }
}
