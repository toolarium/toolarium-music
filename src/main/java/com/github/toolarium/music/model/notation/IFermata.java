/*
 * IFermata.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation;


/**
 * Defines the Fermata interface.
 *
 * @author patrick
 */
public interface IFermata {

    /**
     * Gets the type.
     *
     * @return the type (upright, inverted)
     */
    String getType();

    /**
     * Gets the value.
     *
     * @return the value (normal, angled, square, double-angled, double-square, double-dot, half-curve, curlew)
     */
    String getValue();
}
