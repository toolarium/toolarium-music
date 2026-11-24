/*
 * ScoreData.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.model.score.impl.CreatorImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * A score with metadata and parts, as collected by a format reader for the {@link ScoreBuilder}.
 *
 * @author patrick
 */
public class ScoreData {
    private final List<PartData> parts = new ArrayList<>();
    private final List<CreatorImpl> creators = new ArrayList<>();
    private String title;
    private String subtitle;
    private String movementNumber;
    private String rights;
    private String software;
    private String encodingDate;
    private String source;


    /**
     * Constructor for ScoreData.
     */
    public ScoreData() {
        // default constructor
    }


    /**
     * Gets the parts.
     *
     * @return the parts (mutable)
     */
    public List<PartData> getParts() {
        return parts;
    }


    /**
     * Gets the creators.
     *
     * @return the creators (mutable)
     */
    public List<CreatorImpl> getCreators() {
        return creators;
    }


    /**
     * Adds a creator if the name is not empty.
     *
     * @param type the creator type (composer, lyricist, arranger and so on)
     * @param name the name
     */
    public void addCreator(String type, String name) {
        if (name != null && !name.trim().isEmpty()) {
            CreatorImpl creator = new CreatorImpl();
            creator.setType(type);
            creator.setValue(name.trim());
            creators.add(creator);
        }
    }


    /**
     * Gets the title.
     *
     * @return the title, or null
     */
    public String getTitle() {
        return title;
    }


    /**
     * Sets the title.
     *
     * @param title the title, or null
     */
    public void setTitle(String title) {
        this.title = title;
    }


    /**
     * Gets the subtitle.
     *
     * @return the subtitle, or null
     */
    public String getSubtitle() {
        return subtitle;
    }


    /**
     * Sets the subtitle.
     *
     * @param subtitle the subtitle, or null
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }


    /**
     * Gets the movement number.
     *
     * @return the movement number, or null
     */
    public String getMovementNumber() {
        return movementNumber;
    }


    /**
     * Sets the movement number.
     *
     * @param movementNumber the movement number, or null
     */
    public void setMovementNumber(String movementNumber) {
        this.movementNumber = movementNumber;
    }


    /**
     * Gets the copyright.
     *
     * @return the rights, or null
     */
    public String getRights() {
        return rights;
    }


    /**
     * Sets the copyright.
     *
     * @param rights the rights, or null
     */
    public void setRights(String rights) {
        this.rights = rights;
    }


    /**
     * Gets the encoding software.
     *
     * @return the software, or null
     */
    public String getSoftware() {
        return software;
    }


    /**
     * Sets the encoding software.
     *
     * @param software the software, or null
     */
    public void setSoftware(String software) {
        this.software = software;
    }


    /**
     * Gets the encoding date.
     *
     * @return the date (yyyy-mm-dd), or null
     */
    public String getEncodingDate() {
        return encodingDate;
    }


    /**
     * Sets the encoding date.
     *
     * @param encodingDate the date (yyyy-mm-dd), or null
     */
    public void setEncodingDate(String encodingDate) {
        this.encodingDate = encodingDate;
    }


    /**
     * Gets the source description.
     *
     * @return the source, or null
     */
    public String getSource() {
        return source;
    }


    /**
     * Sets the source description.
     *
     * @param source the source, or null
     */
    public void setSource(String source) {
        this.source = source;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(parts, creators, title, subtitle, movementNumber, rights, software, encodingDate, source);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a ScoreData with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ScoreData other = (ScoreData) obj;
        return Objects.equals(parts, other.parts)
                && Objects.equals(creators, other.creators)
                && Objects.equals(title, other.title)
                && Objects.equals(subtitle, other.subtitle)
                && Objects.equals(movementNumber, other.movementNumber)
                && Objects.equals(rights, other.rights)
                && Objects.equals(software, other.software)
                && Objects.equals(encodingDate, other.encodingDate)
                && Objects.equals(source, other.source);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "ScoreData ["
                + "parts=" + parts
                + ", creators=" + creators
                + ", title=" + title
                + ", subtitle=" + subtitle
                + ", movementNumber=" + movementNumber
                + ", rights=" + rights
                + ", software=" + software
                + ", encodingDate=" + encodingDate
                + ", source=" + source
                + "]";
    }
}
