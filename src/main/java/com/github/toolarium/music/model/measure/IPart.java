/*
 * IPart.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;

import java.util.List;


/**
 * Defines the Part interface.
 *
 * @author patrick
 */
public interface IPart {

    /**
     * Gets the id.
     *
     * @return the id
     */
    String getId();

    /**
     * Gets the measures.
     *
     * @return the measures
     */
    List<? extends IMeasure> getMeasures();
}
