/*
 * DirectionImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.IDirection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IDirection} interface.
 *
 * @author patrick
 */
public class DirectionImpl implements IDirection, IMusicDataElement {

    @JacksonXmlProperty(isAttribute = true, localName = "placement")
    private String placement;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "direction-type")
    private List<DirectionTypeImpl> directionTypes;

    @JacksonXmlProperty(localName = "offset")
    private Integer offset;

    @JacksonXmlProperty(localName = "voice")
    private Integer voice;

    @JacksonXmlProperty(localName = "staff")
    private Integer staff;

    @JacksonXmlProperty(localName = "sound")
    private SoundImpl sound;


    /**
     * Constructor.
     */
    public DirectionImpl() {
        this.directionTypes = new ArrayList<>();
    }


    /**
     * Gets the placement.
     *
     * @return the placement (above, below)
     */
    @Override
    public String getPlacement() {
        return placement;
    }


    /**
     * Sets the placement.
     *
     * @param placement the placement (above, below)
     */
    public void setPlacement(String placement) {
        this.placement = placement;
    }


    /**
     * Gets the list of direction type elements.
     *
     * @return the list of direction type elements
     */
    @Override
    public List<DirectionTypeImpl> getDirectionTypes() {
        return directionTypes;
    }


    /**
     * Sets the list of direction type elements.
     *
     * @param directionTypes the list of direction type elements
     */
    public void setDirectionTypes(List<DirectionTypeImpl> directionTypes) {
        this.directionTypes = directionTypes;
    }


    /**
     * Gets the offset.
     *
     * @return the offset
     */
    @Override
    public Integer getOffset() {
        return offset;
    }


    /**
     * Sets the offset.
     *
     * @param offset the offset
     */
    public void setOffset(Integer offset) {
        this.offset = offset;
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
     * Gets the sound.
     *
     * @return the sound
     */
    @Override
    public SoundImpl getSound() {
        return sound;
    }


    /**
     * Sets the sound.
     *
     * @param sound the sound
     */
    public void setSound(SoundImpl sound) {
        this.sound = sound;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(placement, directionTypes, offset, voice, staff, sound);
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
        DirectionImpl other = (DirectionImpl) obj;
        return Objects.equals(placement, other.placement)
                && Objects.equals(directionTypes, other.directionTypes)
                && Objects.equals(offset, other.offset)
                && Objects.equals(voice, other.voice)
                && Objects.equals(staff, other.staff)
                && Objects.equals(sound, other.sound);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "DirectionImpl [placement=" + placement
                + ", directionTypes=" + directionTypes
                + ", offset=" + offset
                + ", voice=" + voice
                + ", staff=" + staff
                + ", sound=" + sound + "]";
    }
}
