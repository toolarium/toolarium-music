/*
 * IDynamics.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation;


/**
 * Defines the Dynamics interface.
 *
 * @author patrick
 */
public interface IDynamics {

    /**
     * Gets the piano (p) dynamic.
     *
     * @return the piano dynamic
     */
    String getPiano();

    /**
     * Gets the pp dynamic.
     *
     * @return the pp dynamic
     */
    String getPp();

    /**
     * Gets the ppp dynamic.
     *
     * @return the ppp dynamic
     */
    String getPpp();

    /**
     * Gets the pppp dynamic.
     *
     * @return the pppp dynamic
     */
    String getPppp();

    /**
     * Gets the forte (f) dynamic.
     *
     * @return the forte dynamic
     */
    String getForte();

    /**
     * Gets the ff dynamic.
     *
     * @return the ff dynamic
     */
    String getFf();

    /**
     * Gets the fff dynamic.
     *
     * @return the fff dynamic
     */
    String getFff();

    /**
     * Gets the ffff dynamic.
     *
     * @return the ffff dynamic
     */
    String getFfff();

    /**
     * Gets the mp dynamic.
     *
     * @return the mp dynamic
     */
    String getMp();

    /**
     * Gets the mf dynamic.
     *
     * @return the mf dynamic
     */
    String getMf();

    /**
     * Gets the sf dynamic.
     *
     * @return the sf dynamic
     */
    String getSf();

    /**
     * Gets the sfp dynamic.
     *
     * @return the sfp dynamic
     */
    String getSfp();

    /**
     * Gets the sfz dynamic.
     *
     * @return the sfz dynamic
     */
    String getSfz();

    /**
     * Gets the fp dynamic.
     *
     * @return the fp dynamic
     */
    String getFp();

    /**
     * Gets the rf dynamic.
     *
     * @return the rf dynamic
     */
    String getRf();

    /**
     * Gets the rfz dynamic.
     *
     * @return the rfz dynamic
     */
    String getRfz();

    /**
     * Gets the other dynamics.
     *
     * @return the other dynamics
     */
    String getOtherDynamics();

    /**
     * Gets the placement.
     *
     * @return the placement
     */
    String getPlacement();
}
