/*
 * MeasureImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IMeasure} interface.
 *
 * @author patrick
 */
public class MeasureImpl implements IMeasure {

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private String number;

    @JacksonXmlProperty(isAttribute = true, localName = "width")
    private String width;

    @JacksonXmlProperty(isAttribute = true, localName = "implicit")
    private String implicit;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "attributes")
    private List<AttributesImpl> attributes;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "note")
    private List<NoteImpl> notes;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "forward")
    private List<ForwardImpl> forwards;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "backup")
    private List<BackupImpl> backups;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "direction")
    private List<DirectionImpl> directions;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "barline")
    private List<BarlineImpl> barlines;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "harmony")
    private List<HarmonyImpl> harmonies;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "print")
    private List<PrintImpl> prints;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "sound")
    private List<SoundImpl> sounds;


    /**
     * Gets the number.
     *
     * @return the number
     */
    @Override
    public String getNumber() {
        return number;
    }


    /**
     * Sets the number.
     *
     * @param number the number
     */
    public void setNumber(String number) {
        this.number = number;
    }


    /**
     * Gets the width.
     *
     * @return the width
     */
    @Override
    public String getWidth() {
        return width;
    }


    /**
     * Sets the width.
     *
     * @param width the width
     */
    public void setWidth(String width) {
        this.width = width;
    }


    /**
     * Gets the implicit.
     *
     * @return the implicit
     */
    @Override
    public String getImplicit() {
        return implicit;
    }


    /**
     * Sets the implicit.
     *
     * @param implicit the implicit
     */
    public void setImplicit(String implicit) {
        this.implicit = implicit;
    }


    /**
     * Gets the attributes.
     *
     * @return the attributes
     */
    @Override
    public List<AttributesImpl> getAttributes() {
        return attributes;
    }


    /**
     * Sets the attributes.
     *
     * @param attributes the attributes
     */
    public void setAttributes(List<AttributesImpl> attributes) {
        this.attributes = attributes;
    }


    /**
     * Gets the notes.
     *
     * @return the notes
     */
    @Override
    public List<NoteImpl> getNotes() {
        return notes;
    }


    /**
     * Sets the notes.
     *
     * @param notes the notes
     */
    public void setNotes(List<NoteImpl> notes) {
        this.notes = notes;
    }


    /**
     * Gets the forwards.
     *
     * @return the forwards
     */
    @Override
    public List<ForwardImpl> getForwards() {
        return forwards;
    }


    /**
     * Sets the forwards.
     *
     * @param forwards the forwards
     */
    public void setForwards(List<ForwardImpl> forwards) {
        this.forwards = forwards;
    }


    /**
     * Gets the backups.
     *
     * @return the backups
     */
    @Override
    public List<BackupImpl> getBackups() {
        return backups;
    }


    /**
     * Sets the backups.
     *
     * @param backups the backups
     */
    public void setBackups(List<BackupImpl> backups) {
        this.backups = backups;
    }


    /**
     * Gets the directions.
     *
     * @return the directions
     */
    @Override
    public List<DirectionImpl> getDirections() {
        return directions;
    }


    /**
     * Sets the directions.
     *
     * @param directions the directions
     */
    public void setDirections(List<DirectionImpl> directions) {
        this.directions = directions;
    }


    /**
     * Gets the barlines.
     *
     * @return the barlines
     */
    @Override
    public List<BarlineImpl> getBarlines() {
        return barlines;
    }


    /**
     * Sets the barlines.
     *
     * @param barlines the barlines
     */
    public void setBarlines(List<BarlineImpl> barlines) {
        this.barlines = barlines;
    }


    /**
     * Gets the harmonies.
     *
     * @return the harmonies
     */
    @Override
    public List<HarmonyImpl> getHarmonies() {
        return harmonies;
    }


    /**
     * Sets the harmonies.
     *
     * @param harmonies the harmonies
     */
    public void setHarmonies(List<HarmonyImpl> harmonies) {
        this.harmonies = harmonies;
    }


    /**
     * Gets the prints.
     *
     * @return the prints
     */
    @Override
    public List<PrintImpl> getPrints() {
        return prints;
    }


    /**
     * Sets the prints.
     *
     * @param prints the prints
     */
    public void setPrints(List<PrintImpl> prints) {
        this.prints = prints;
    }


    /**
     * Gets the sounds.
     *
     * @return the sounds
     */
    @Override
    public List<SoundImpl> getSounds() {
        return sounds;
    }


    /**
     * Sets the sounds.
     *
     * @param sounds the sounds
     */
    public void setSounds(List<SoundImpl> sounds) {
        this.sounds = sounds;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(number, width, implicit, attributes, notes, forwards, backups,
                directions, barlines, harmonies, prints, sounds);
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
        MeasureImpl other = (MeasureImpl) obj;
        return Objects.equals(number, other.number)
                && Objects.equals(width, other.width)
                && Objects.equals(implicit, other.implicit)
                && Objects.equals(attributes, other.attributes)
                && Objects.equals(notes, other.notes)
                && Objects.equals(forwards, other.forwards)
                && Objects.equals(backups, other.backups)
                && Objects.equals(directions, other.directions)
                && Objects.equals(barlines, other.barlines)
                && Objects.equals(harmonies, other.harmonies)
                && Objects.equals(prints, other.prints)
                && Objects.equals(sounds, other.sounds);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "MeasureImpl [number=" + number + ", width=" + width + ", implicit=" + implicit
                + ", attributes=" + attributes + ", notes=" + notes + ", forwards=" + forwards
                + ", backups=" + backups + ", directions=" + directions + ", barlines=" + barlines
                + ", harmonies=" + harmonies + ", prints=" + prints + ", sounds=" + sounds + "]";
    }
}
