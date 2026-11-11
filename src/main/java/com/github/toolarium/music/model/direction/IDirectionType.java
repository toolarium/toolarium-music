/*
 * IDirectionType.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction;

import com.github.toolarium.music.model.notation.IDynamics;
import java.util.List;


/**
 * Represents the MusicXML direction-type element.
 *
 * @author patrick
 */
public interface IDirectionType {

    /**
     * Gets the rehearsal.
     *
     * @return the rehearsal
     */
    String getRehearsal();

    /**
     * Gets the segno.
     *
     * @return the segno
     */
    String getSegno();

    /**
     * Gets the coda.
     *
     * @return the coda
     */
    String getCoda();

    /**
     * Gets the list of words elements.
     *
     * @return the list of words elements
     */
    List<? extends IWords> getWords();

    /**
     * Gets the wedge.
     *
     * @return the wedge
     */
    IWedge getWedge();

    /**
     * Gets the dashes.
     *
     * @return the dashes
     */
    String getDashes();

    /**
     * Gets the bracket.
     *
     * @return the bracket
     */
    String getBracket();

    /**
     * Gets the pedal.
     *
     * @return the pedal
     */
    IPedal getPedal();

    /**
     * Gets the metronome.
     *
     * @return the metronome
     */
    IMetronome getMetronome();

    /**
     * Gets the octave shift.
     *
     * @return the octave shift
     */
    IOctaveShift getOctaveShift();

    /**
     * Gets the dynamics.
     *
     * @return the dynamics
     */
    IDynamics getDynamics();
}
