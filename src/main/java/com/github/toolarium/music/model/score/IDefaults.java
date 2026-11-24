/*
 * IDefaults.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score;

import com.github.toolarium.music.model.layout.IPageLayout;

/**
 * Defines the Defaults interface.
 *
 * @author patrick
 */
public interface IDefaults {

    /**
     * Gets the scaling.
     *
     * @return the scaling
     */
    IScaling getScaling();


    /**
     * Gets the page layout: the page size and margins.
     *
     * @return the page layout, or null
     */
    IPageLayout getPageLayout();
}
