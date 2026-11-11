/*
 * INote.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note;

import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.notation.INotations;
import java.util.List;


/**
 * Represents the MusicXML note element.
 *
 * @author patrick
 */
public interface INote extends IMusicDataElement {

    /**
     * Gets the pitch.
     *
     * @return the pitch
     */
    IPitch getPitch();

    /**
     * Gets the rest.
     *
     * @return the rest
     */
    IRest getRest();

    /**
     * Gets the unpitched.
     *
     * @return the unpitched
     */
    IUnpitched getUnpitched();

    /**
     * Gets the chord.
     *
     * @return the chord
     */
    String getChord();

    /**
     * Gets the duration.
     *
     * @return the duration
     */
    Integer getDuration();

    /**
     * Gets the ties.
     *
     * @return the ties
     */
    List<? extends ITie> getTies();

    /**
     * Gets the voice.
     *
     * @return the voice
     */
    Integer getVoice();

    /**
     * Gets the type.
     *
     * @return the type
     */
    String getType();

    /**
     * Gets the dots.
     *
     * @return the dots
     */
    List<String> getDots();

    /**
     * Gets the accidental.
     *
     * @return the accidental
     */
    String getAccidental();

    /**
     * Gets the time modification.
     *
     * @return the time modification
     */
    ITimeModification getTimeModification();

    /**
     * Gets the stem.
     *
     * @return the stem
     */
    String getStem();

    /**
     * Gets the notehead.
     *
     * @return the notehead
     */
    String getNotehead();

    /**
     * Gets the staff.
     *
     * @return the staff
     */
    Integer getStaff();

    /**
     * Gets the beams.
     *
     * @return the beams
     */
    List<? extends IBeam> getBeams();

    /**
     * Gets the notations.
     *
     * @return the notations
     */
    INotations getNotations();

    /**
     * Gets the lyrics.
     *
     * @return the lyrics
     */
    List<? extends ILyric> getLyrics();

    /**
     * Gets the default x.
     *
     * @return the default x
     */
    String getDefaultX();

    /**
     * Gets the default y.
     *
     * @return the default y
     */
    String getDefaultY();

    /**
     * Gets the print object.
     *
     * @return the print object
     */
    String getPrintObject();
}
