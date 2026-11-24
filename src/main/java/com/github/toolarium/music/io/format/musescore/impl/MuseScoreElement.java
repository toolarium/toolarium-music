/*
 * MuseScoreElement.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;


/**
 * An element of a MuseScore document that writes itself.
 *
 * @author patrick
 */
interface MuseScoreElement {

    /**
     * Writes the element.
     *
     * @param out the output
     */
    void write(MuseScoreXmlOutput out);
}
