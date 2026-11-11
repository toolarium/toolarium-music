/*
 * ITimeModification.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note;


/**
 * Represents the MusicXML time-modification element.
 *
 * @author patrick
 */
public interface ITimeModification {

    /**
     * Gets the actual notes.
     *
     * @return the actual notes
     */
    Integer getActualNotes();

    /**
     * Gets the normal notes.
     *
     * @return the normal notes
     */
    Integer getNormalNotes();

    /**
     * Gets the normal type.
     *
     * @return the normal type
     */
    String getNormalType();
}
