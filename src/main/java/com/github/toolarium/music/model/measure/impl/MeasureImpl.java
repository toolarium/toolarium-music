/*
 * MeasureImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IMeasure} interface.
 *
 * <p>The content is kept in one list in document order ({@link #getMusicData()}). The typed getters
 * return read-only lists of the elements of one type (empty if there are none). A typed setter replaces the elements of its
 * type at the position of the first one; if there are none, attributes and print elements are
 * inserted at the start and all other elements are appended.</p>
 *
 * @author patrick
 */
@JsonSerialize(using = MeasureSerializer.class)
@JsonDeserialize(using = MeasureDeserializer.class)
public class MeasureImpl implements IMeasure {
    private String number;
    private String width;
    private String implicit;
    private List<IMusicDataElement> musicData = new ArrayList<>();


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
     * Gets the content of the measure in document order.
     *
     * @return the content (read-only), never null
     */
    @Override
    public List<IMusicDataElement> getMusicData() {
        return Collections.unmodifiableList(musicData);
    }


    /**
     * Sets the content of the measure in document order.
     *
     * @param musicData the content (null entries are ignored)
     */
    public void setMusicData(List<? extends IMusicDataElement> musicData) {
        this.musicData = new ArrayList<>();
        if (musicData != null) {
            for (IMusicDataElement element : musicData) {
                addMusicData(element);
            }
        }
    }


    /**
     * Adds an element at the end of the content.
     *
     * @param element the element (ignored if null)
     */
    public void addMusicData(IMusicDataElement element) {
        if (element != null) {
            musicData.add(element);
        }
    }


    /**
     * Gets the attributes.
     *
     * @return the attributes (read-only), empty if there are none
     */
    @Override
    public List<AttributesImpl> getAttributes() {
        return elements(AttributesImpl.class);
    }


    /**
     * Sets the attributes. Without existing attributes they are inserted at the start of the measure (after print elements).
     *
     * @param attributes the attributes
     */
    public void setAttributes(List<AttributesImpl> attributes) {
        replace(AttributesImpl.class, attributes, true);
    }


    /**
     * Gets the notes.
     *
     * @return the notes (read-only), empty if there are none
     */
    @Override
    public List<NoteImpl> getNotes() {
        return elements(NoteImpl.class);
    }


    /**
     * Sets the notes.
     *
     * @param notes the notes
     */
    public void setNotes(List<NoteImpl> notes) {
        replace(NoteImpl.class, notes, false);
    }


    /**
     * Gets the forwards.
     *
     * @return the forwards (read-only), empty if there are none
     */
    @Override
    public List<ForwardImpl> getForwards() {
        return elements(ForwardImpl.class);
    }


    /**
     * Sets the forwards.
     *
     * @param forwards the forwards
     */
    public void setForwards(List<ForwardImpl> forwards) {
        replace(ForwardImpl.class, forwards, false);
    }


    /**
     * Gets the backups.
     *
     * @return the backups (read-only), empty if there are none
     */
    @Override
    public List<BackupImpl> getBackups() {
        return elements(BackupImpl.class);
    }


    /**
     * Sets the backups.
     *
     * @param backups the backups
     */
    public void setBackups(List<BackupImpl> backups) {
        replace(BackupImpl.class, backups, false);
    }


    /**
     * Gets the directions.
     *
     * @return the directions (read-only), empty if there are none
     */
    @Override
    public List<DirectionImpl> getDirections() {
        return elements(DirectionImpl.class);
    }


    /**
     * Sets the directions.
     *
     * @param directions the directions
     */
    public void setDirections(List<DirectionImpl> directions) {
        replace(DirectionImpl.class, directions, false);
    }


    /**
     * Gets the barlines.
     *
     * @return the barlines (read-only), empty if there are none
     */
    @Override
    public List<BarlineImpl> getBarlines() {
        return elements(BarlineImpl.class);
    }


    /**
     * Sets the barlines.
     *
     * @param barlines the barlines
     */
    public void setBarlines(List<BarlineImpl> barlines) {
        replace(BarlineImpl.class, barlines, false);
    }


    /**
     * Gets the harmonies.
     *
     * @return the harmonies (read-only), empty if there are none
     */
    @Override
    public List<HarmonyImpl> getHarmonies() {
        return elements(HarmonyImpl.class);
    }


    /**
     * Sets the harmonies.
     *
     * @param harmonies the harmonies
     */
    public void setHarmonies(List<HarmonyImpl> harmonies) {
        replace(HarmonyImpl.class, harmonies, false);
    }


    /**
     * Gets the prints.
     *
     * @return the prints (read-only), empty if there are none
     */
    @Override
    public List<PrintImpl> getPrints() {
        return elements(PrintImpl.class);
    }


    /**
     * Sets the prints. Without existing print elements they are inserted at the start of the measure.
     *
     * @param prints the prints
     */
    public void setPrints(List<PrintImpl> prints) {
        replace(PrintImpl.class, prints, true);
    }


    /**
     * Gets the sounds.
     *
     * @return the sounds (read-only), empty if there are none
     */
    @Override
    public List<SoundImpl> getSounds() {
        return elements(SoundImpl.class);
    }


    /**
     * Sets the sounds.
     *
     * @param sounds the sounds
     */
    public void setSounds(List<SoundImpl> sounds) {
        replace(SoundImpl.class, sounds, false);
    }


    /**
     * Gets the elements of one type in document order.
     *
     * @param <T> the element type
     * @param type the element class
     * @return the elements (read-only), empty if there are none
     */
    private <T extends IMusicDataElement> List<T> elements(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (IMusicDataElement element : musicData) {
            if (type.isInstance(element)) {
                result.add(type.cast(element));
            }
        }
        return Collections.unmodifiableList(result);
    }


    /**
     * Replaces the elements of one type at the position of the first existing one.
     *
     * @param type the element class
     * @param values the new elements, or null to remove them
     * @param atStart true to insert at the start of the measure (after print elements) if there are no elements of the type
     */
    private void replace(Class<? extends IMusicDataElement> type, List<? extends IMusicDataElement> values, boolean atStart) {
        int index = -1;
        for (int i = 0; i < musicData.size(); i++) {
            if (type.isInstance(musicData.get(i))) {
                index = i;
                break;
            }
        }
        musicData.removeIf(type::isInstance);
        if (values == null) {
            return;
        }

        if (index < 0) {
            index = musicData.size();
            if (atStart) {
                index = 0;
                while (index < musicData.size() && musicData.get(index) instanceof PrintImpl) {
                    index++;
                }
            }
        }
        for (IMusicDataElement value : values) {
            if (value != null) {
                musicData.add(index++, value);
            }
        }
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(number, width, implicit, musicData);
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
                && Objects.equals(musicData, other.musicData);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "MeasureImpl [number=" + number + ", width=" + width + ", implicit=" + implicit + ", musicData=" + musicData + "]";
    }
}
