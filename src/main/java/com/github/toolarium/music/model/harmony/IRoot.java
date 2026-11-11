/*
 * IRoot.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.harmony;


/**
 * Defines the Root interface.
 *
 * @author patrick
 */
public interface IRoot {

    /**
     * Gets the root step.
     *
     * @return the root step
     */
    String getRootStep();

    /**
     * Gets the root alter.
     *
     * @return the root alter
     */
    Double getRootAlter();
}
