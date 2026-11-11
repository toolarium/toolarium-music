/*
 * IMeasure.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;

import com.github.toolarium.music.model.barline.IBarline;
import com.github.toolarium.music.model.direction.IDirection;
import com.github.toolarium.music.model.direction.ISound;
import com.github.toolarium.music.model.harmony.IHarmony;
import com.github.toolarium.music.model.note.INote;
import java.util.List;


/**
 * Defines the Measure interface.
 *
 * @author patrick
 */
public interface IMeasure {

    /**
     * Gets the number.
     *
     * @return the number
     */
    String getNumber();

    /**
     * Gets the width.
     *
     * @return the width
     */
    String getWidth();

    /**
     * Gets the implicit.
     *
     * @return the implicit
     */
    String getImplicit();

    /**
     * Gets the attributes.
     *
     * @return the attributes
     */
    List<? extends IAttributes> getAttributes();

    /**
     * Gets the notes.
     *
     * @return the notes
     */
    List<? extends INote> getNotes();

    /**
     * Gets the forwards.
     *
     * @return the forwards
     */
    List<? extends IForward> getForwards();

    /**
     * Gets the backups.
     *
     * @return the backups
     */
    List<? extends IBackup> getBackups();

    /**
     * Gets the directions.
     *
     * @return the directions
     */
    List<? extends IDirection> getDirections();

    /**
     * Gets the barlines.
     *
     * @return the barlines
     */
    List<? extends IBarline> getBarlines();

    /**
     * Gets the harmonies.
     *
     * @return the harmonies
     */
    List<? extends IHarmony> getHarmonies();

    /**
     * Gets the prints.
     *
     * @return the prints
     */
    List<? extends IPrint> getPrints();

    /**
     * Gets the sounds.
     *
     * @return the sounds
     */
    List<? extends ISound> getSounds();
}
