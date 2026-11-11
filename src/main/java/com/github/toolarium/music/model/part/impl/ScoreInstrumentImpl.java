/*
 * ScoreInstrumentImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.part.IScoreInstrument;
import java.util.Objects;

/**
 * Implements the {@link IScoreInstrument} interface.
 *
 * @author patrick
 */
public class ScoreInstrumentImpl implements IScoreInstrument {

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "instrument-name")
    private String instrumentName;

    @JacksonXmlProperty(localName = "instrument-abbreviation")
    private String instrumentAbbreviation;

    @JacksonXmlProperty(localName = "instrument-sound")
    private String instrumentSound;


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
     * Gets the instrument name.
     *
     * @return the instrument name
     */
    @Override
    public String getInstrumentName() {
        return instrumentName;
    }


    /**
     * Sets the instrument name.
     *
     * @param instrumentName the instrument name
     */
    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }


    /**
     * Gets the instrument abbreviation.
     *
     * @return the instrument abbreviation
     */
    @Override
    public String getInstrumentAbbreviation() {
        return instrumentAbbreviation;
    }


    /**
     * Sets the instrument abbreviation.
     *
     * @param instrumentAbbreviation the instrument abbreviation
     */
    public void setInstrumentAbbreviation(String instrumentAbbreviation) {
        this.instrumentAbbreviation = instrumentAbbreviation;
    }


    /**
     * Gets the instrument sound.
     *
     * @return the instrument sound
     */
    @Override
    public String getInstrumentSound() {
        return instrumentSound;
    }


    /**
     * Sets the instrument sound.
     *
     * @param instrumentSound the instrument sound
     */
    public void setInstrumentSound(String instrumentSound) {
        this.instrumentSound = instrumentSound;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, instrumentName, instrumentAbbreviation, instrumentSound);
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
        ScoreInstrumentImpl other = (ScoreInstrumentImpl) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(instrumentName, other.instrumentName)
                && Objects.equals(instrumentAbbreviation, other.instrumentAbbreviation)
                && Objects.equals(instrumentSound, other.instrumentSound);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "ScoreInstrumentImpl ["
                + "id=" + id
                + ", instrumentName=" + instrumentName
                + ", instrumentAbbreviation=" + instrumentAbbreviation
                + ", instrumentSound=" + instrumentSound
                + "]";
    }
}
