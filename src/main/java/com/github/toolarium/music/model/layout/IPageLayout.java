/*
 * IPageLayout.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.layout;

import java.util.List;


/**
 * Represents the MusicXML page-layout element.
 *
 * @author patrick
 */
public interface IPageLayout {

    /**
     * Gets the page height.
     *
     * @return the page height
     */
    Double getPageHeight();

    /**
     * Gets the page width.
     *
     * @return the page width
     */
    Double getPageWidth();

    /**
     * Gets the page margins.
     *
     * @return the page margins
     */
    List<? extends IPageMargins> getPageMargins();
}
