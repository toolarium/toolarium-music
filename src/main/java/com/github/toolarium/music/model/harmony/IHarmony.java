/*
 * IHarmony.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.harmony;

import java.util.List;


/**
 * Defines the Harmony interface.
 *
 * @author patrick
 */
public interface IHarmony {

    /**
     * Gets the type.
     *
     * @return the type
     */
    String getType();

    /**
     * Gets the print frame.
     *
     * @return the print frame
     */
    String getPrintFrame();

    /**
     * Gets the root.
     *
     * @return the root
     */
    IRoot getRoot();

    /**
     * Gets the function.
     *
     * @return the function
     */
    String getFunction();

    /**
     * Gets the kind.
     *
     * @return the kind
     */
    String getKind();

    /**
     * Gets the inversion.
     *
     * @return the inversion
     */
    Integer getInversion();

    /**
     * Gets the bass.
     *
     * @return the bass
     */
    IBass getBass();

    /**
     * Gets the degrees.
     *
     * @return the degrees
     */
    List<? extends IDegree> getDegrees();

    /**
     * Gets the frame.
     *
     * @return the frame
     */
    IFrame getFrame();

    /**
     * Gets the staff.
     *
     * @return the staff
     */
    Integer getStaff();
}
