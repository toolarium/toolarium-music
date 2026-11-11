/*
 * IArticulations.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation;


/**
 * Defines the Articulations interface.
 *
 * @author patrick
 */
public interface IArticulations {

    /**
     * Gets the accent.
     *
     * @return the accent
     */
    String getAccent();

    /**
     * Gets the strong accent.
     *
     * @return the strong accent
     */
    String getStrongAccent();

    /**
     * Gets the staccato.
     *
     * @return the staccato
     */
    String getStaccato();

    /**
     * Gets the tenuto.
     *
     * @return the tenuto
     */
    String getTenuto();

    /**
     * Gets the detached legato.
     *
     * @return the detached legato
     */
    String getDetachedLegato();

    /**
     * Gets the staccatissimo.
     *
     * @return the staccatissimo
     */
    String getStaccatissimo();

    /**
     * Gets the spiccato.
     *
     * @return the spiccato
     */
    String getSpiccato();

    /**
     * Gets the breath mark.
     *
     * @return the breath mark
     */
    String getBreathMark();

    /**
     * Gets the caesura.
     *
     * @return the caesura
     */
    String getCaesura();

    /**
     * Gets the stress.
     *
     * @return the stress
     */
    String getStress();

    /**
     * Gets the unstress.
     *
     * @return the unstress
     */
    String getUnstress();
}
