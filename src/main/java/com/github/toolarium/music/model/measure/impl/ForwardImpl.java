/*
 * ForwardImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.measure.IForward;
import java.util.Objects;


/**
 * Implements the {@link IForward} interface.
 *
 * @author patrick
 */
public class ForwardImpl implements IForward {

    @JacksonXmlProperty(localName = "duration")
    private Integer duration;

    @JacksonXmlProperty(localName = "voice")
    private Integer voice;

    @JacksonXmlProperty(localName = "staff")
    private Integer staff;


    /**
     * Constructor.
     */
    public ForwardImpl() {
    }


    /**
     * Gets the duration.
     *
     * @return the duration
     */
    @Override
    public Integer getDuration() {
        return duration;
    }


    /**
     * Sets the duration.
     *
     * @param duration the duration
     */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }


    /**
     * Gets the voice.
     *
     * @return the voice
     */
    @Override
    public Integer getVoice() {
        return voice;
    }


    /**
     * Sets the voice.
     *
     * @param voice the voice
     */
    public void setVoice(Integer voice) {
        this.voice = voice;
    }


    /**
     * Gets the staff.
     *
     * @return the staff
     */
    @Override
    public Integer getStaff() {
        return staff;
    }


    /**
     * Sets the staff.
     *
     * @param staff the staff
     */
    public void setStaff(Integer staff) {
        this.staff = staff;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(duration, voice, staff);
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
        ForwardImpl other = (ForwardImpl) obj;
        return Objects.equals(duration, other.duration)
                && Objects.equals(voice, other.voice)
                && Objects.equals(staff, other.staff);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "ForwardImpl [duration=" + duration + ", voice=" + voice + ", staff=" + staff + "]";
    }
}
