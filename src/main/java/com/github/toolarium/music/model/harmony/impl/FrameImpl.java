/*
 * FrameImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.harmony.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.harmony.IFrame;
import java.util.Objects;

/**
 * Implements the {@link IFrame} interface.
 *
 * @author patrick
 */
public class FrameImpl implements IFrame {

    @JacksonXmlProperty(localName = "frame-strings")
    private Integer frameStrings;

    @JacksonXmlProperty(localName = "frame-frets")
    private Integer frameFrets;


    /**
     * Gets the frame strings.
     *
     * @return the frame strings
     */
    @Override
    public Integer getFrameStrings() {
        return frameStrings;
    }


    /**
     * Sets the frame strings.
     *
     * @param frameStrings the frame strings
     */
    public void setFrameStrings(Integer frameStrings) {
        this.frameStrings = frameStrings;
    }


    /**
     * Gets the frame frets.
     *
     * @return the frame frets
     */
    @Override
    public Integer getFrameFrets() {
        return frameFrets;
    }


    /**
     * Sets the frame frets.
     *
     * @param frameFrets the frame frets
     */
    public void setFrameFrets(Integer frameFrets) {
        this.frameFrets = frameFrets;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(frameStrings, frameFrets);
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
        FrameImpl other = (FrameImpl) obj;
        return Objects.equals(frameStrings, other.frameStrings)
                && Objects.equals(frameFrets, other.frameFrets);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FrameImpl [frameStrings=" + frameStrings + ", frameFrets=" + frameFrets + "]";
    }
}
