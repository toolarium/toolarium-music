/*
 * ScoreImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.score.IScore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Implements the {@link IScore} interface.
 *
 * @author patrick
 */
@JacksonXmlRootElement(localName = "score-partwise")
public class ScoreImpl implements IScore {

    @JacksonXmlProperty(isAttribute = true, localName = "version")
    private String version;

    @JacksonXmlProperty(localName = "work")
    private WorkImpl work;

    @JacksonXmlProperty(localName = "movement-number")
    private String movementNumber;

    @JacksonXmlProperty(localName = "movement-title")
    private String movementTitle;

    @JacksonXmlProperty(localName = "identification")
    private IdentificationImpl identification;

    @JacksonXmlProperty(localName = "defaults")
    private DefaultsImpl defaults;

    @JacksonXmlProperty(localName = "credit")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<CreditImpl> credits;

    @JacksonXmlProperty(localName = "part-list")
    private PartListImpl partList;

    @JacksonXmlProperty(localName = "part")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<PartImpl> parts;


    /**
     * Constructor.
     */
    public ScoreImpl() {
        this.credits = new ArrayList<>();
        this.parts = new ArrayList<>();
    }


    /**
     * Gets the version.
     *
     * @return the version
     */
    @Override
    public String getVersion() {
        return version;
    }


    /**
     * Sets the version.
     *
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
    }


    /**
     * Gets the work.
     *
     * @return the work
     */
    @Override
    public WorkImpl getWork() {
        return work;
    }


    /**
     * Sets the work.
     *
     * @param work the work
     */
    public void setWork(WorkImpl work) {
        this.work = work;
    }


    /**
     * Gets the movement number.
     *
     * @return the movement number
     */
    @Override
    public String getMovementNumber() {
        return movementNumber;
    }


    /**
     * Sets the movement number.
     *
     * @param movementNumber the movement number
     */
    public void setMovementNumber(String movementNumber) {
        this.movementNumber = movementNumber;
    }


    /**
     * Gets the movement title.
     *
     * @return the movement title
     */
    @Override
    public String getMovementTitle() {
        return movementTitle;
    }


    /**
     * Sets the movement title.
     *
     * @param movementTitle the movement title
     */
    public void setMovementTitle(String movementTitle) {
        this.movementTitle = movementTitle;
    }


    /**
     * Gets the identification.
     *
     * @return the identification
     */
    @Override
    public IdentificationImpl getIdentification() {
        return identification;
    }


    /**
     * Sets the identification.
     *
     * @param identification the identification
     */
    public void setIdentification(IdentificationImpl identification) {
        this.identification = identification;
    }


    /**
     * Gets the defaults.
     *
     * @return the defaults
     */
    @Override
    public DefaultsImpl getDefaults() {
        return defaults;
    }


    /**
     * Sets the defaults.
     *
     * @param defaults the defaults
     */
    public void setDefaults(DefaultsImpl defaults) {
        this.defaults = defaults;
    }


    /**
     * Gets the credits.
     *
     * @return the credits
     */
    @Override
    public List<CreditImpl> getCredits() {
        return credits;
    }


    /**
     * Sets the credits.
     *
     * @param credits the credits
     */
    public void setCredits(List<CreditImpl> credits) {
        this.credits = credits;
    }


    /**
     * Gets the part list.
     *
     * @return the part list
     */
    @Override
    public PartListImpl getPartList() {
        return partList;
    }


    /**
     * Sets the part list.
     *
     * @param partList the part list
     */
    public void setPartList(PartListImpl partList) {
        this.partList = partList;
    }


    /**
     * Gets the parts.
     *
     * @return the parts
     */
    @Override
    public List<PartImpl> getParts() {
        return parts;
    }


    /**
     * Sets the parts.
     *
     * @param parts the parts
     */
    public void setParts(List<PartImpl> parts) {
        this.parts = parts;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(version, work, movementNumber, movementTitle,
                identification, defaults, credits, partList, parts);
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
        ScoreImpl other = (ScoreImpl) obj;
        return Objects.equals(version, other.version)
                && Objects.equals(work, other.work)
                && Objects.equals(movementNumber, other.movementNumber)
                && Objects.equals(movementTitle, other.movementTitle)
                && Objects.equals(identification, other.identification)
                && Objects.equals(defaults, other.defaults)
                && Objects.equals(credits, other.credits)
                && Objects.equals(partList, other.partList)
                && Objects.equals(parts, other.parts);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "ScoreImpl [version=" + version
                + ", work=" + work
                + ", movementNumber=" + movementNumber
                + ", movementTitle=" + movementTitle
                + ", identification=" + identification
                + ", defaults=" + defaults
                + ", credits=" + credits
                + ", partList=" + partList
                + ", parts=" + parts + "]";
    }
}
