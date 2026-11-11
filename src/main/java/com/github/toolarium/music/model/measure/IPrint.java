/*
 * IPrint.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.measure;

import com.github.toolarium.music.model.common.IMusicDataElement;


/**
 * Represents the MusicXML print element.
 *
 * @author patrick
 */
public interface IPrint extends IMusicDataElement {

    /**
     * Gets the new system.
     *
     * @return the new system
     */
    String getNewSystem();

    /**
     * Gets the new page.
     *
     * @return the new page
     */
    String getNewPage();

    /**
     * Gets the blank page.
     *
     * @return the blank page
     */
    Integer getBlankPage();

    /**
     * Gets the page number.
     *
     * @return the page number
     */
    Integer getPageNumber();
}
