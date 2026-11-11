/*
 * ISound.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction;

import com.github.toolarium.music.model.common.IMusicDataElement;


/**
 * Represents the MusicXML sound element.
 *
 * @author patrick
 */
public interface ISound extends IMusicDataElement {

    /**
     * Gets the tempo.
     *
     * @return the tempo
     */
    String getTempo();

    /**
     * Gets the dynamics.
     *
     * @return the dynamics
     */
    String getDynamics();

    /**
     * Gets the dacapo.
     *
     * @return the dacapo
     */
    String getDacapo();

    /**
     * Gets the segno.
     *
     * @return the segno
     */
    String getSegno();

    /**
     * Gets the dalsegno.
     *
     * @return the dalsegno
     */
    String getDalsegno();

    /**
     * Gets the coda.
     *
     * @return the coda
     */
    String getCoda();

    /**
     * Gets the tocoda.
     *
     * @return the tocoda
     */
    String getTocoda();

    /**
     * Gets the fine.
     *
     * @return the fine
     */
    String getFine();

    /**
     * Gets the forward repeat.
     *
     * @return the forward repeat
     */
    String getForwardRepeat();
}
