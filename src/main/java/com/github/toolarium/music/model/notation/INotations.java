/*
 * INotations.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation;

import java.util.List;


/**
 * Defines the Notations interface.
 *
 * @author patrick
 */
public interface INotations {

    /**
     * Gets the list of tied elements.
     *
     * @return the list of tied elements
     */
    List<? extends ITied> getTieds();

    /**
     * Gets the list of slur elements.
     *
     * @return the list of slur elements
     */
    List<? extends ISlur> getSlurs();

    /**
     * Gets the list of tuplet elements.
     *
     * @return the list of tuplet elements
     */
    List<? extends ITuplet> getTuplets();

    /**
     * Gets the list of fermata elements.
     *
     * @return the list of fermata elements
     */
    List<? extends IFermata> getFermatas();

    /**
     * Gets the articulations.
     *
     * @return the articulations
     */
    IArticulations getArticulations();

    /**
     * Gets the dynamics.
     *
     * @return the dynamics
     */
    IDynamics getDynamics();

    /**
     * Gets the ornaments.
     *
     * @return the ornaments
     */
    IOrnaments getOrnaments();

    /**
     * Gets the technical.
     *
     * @return the technical
     */
    IPlayingTechnique getTechnical();

    /**
     * Gets the list of glissando elements.
     *
     * @return the list of glissando elements
     */
    List<? extends IGlissando> getGlissandos();
}
