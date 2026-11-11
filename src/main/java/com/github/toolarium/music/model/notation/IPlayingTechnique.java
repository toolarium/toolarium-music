/*
 * IPlayingTechnique.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation;


/**
 * Defines the Technical interface.
 *
 * @author patrick
 */
public interface IPlayingTechnique {

    /**
     * Gets the up bow.
     *
     * @return the up bow
     */
    String getUpBow();

    /**
     * Gets the down bow.
     *
     * @return the down bow
     */
    String getDownBow();

    /**
     * Gets the harmonic.
     *
     * @return the harmonic
     */
    String getHarmonic();

    /**
     * Gets the open string.
     *
     * @return the open string
     */
    String getOpenString();

    /**
     * Gets the thumb position.
     *
     * @return the thumb position
     */
    String getThumbPosition();

    /**
     * Gets the fingering.
     *
     * @return the fingering
     */
    String getFingering();

    /**
     * Gets the pluck.
     *
     * @return the pluck
     */
    String getPluck();

    /**
     * Gets the double tongue.
     *
     * @return the double tongue
     */
    String getDoubleTongue();

    /**
     * Gets the triple tongue.
     *
     * @return the triple tongue
     */
    String getTripleTongue();

    /**
     * Gets the stopped.
     *
     * @return the stopped
     */
    String getStopped();

    /**
     * Gets the snap pizzicato.
     *
     * @return the snap pizzicato
     */
    String getSnapPizzicato();

    /**
     * Gets the fret.
     *
     * @return the fret
     */
    String getFret();

    /**
     * Gets the string.
     *
     * @return the string
     */
    String getString();

    /**
     * Gets the hammer on.
     *
     * @return the hammer on
     */
    String getHammerOn();

    /**
     * Gets the pull off.
     *
     * @return the pull off
     */
    String getPullOff();
}
