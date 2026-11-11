/*
 * IScore.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score;

import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.part.IPartList;
import java.util.List;


/**
 * Represents a musical score. This is the top-level model interface containing
 * all parts, measures, notes, and metadata of a music document.
 *
 * @author patrick
 */
public interface IScore {

    /**
     * Gets the version.
     *
     * @return the version
     */
    String getVersion();


    /**
     * Gets the work.
     *
     * @return the work
     */
    IWork getWork();


    /**
     * Gets the movement number.
     *
     * @return the movement number
     */
    String getMovementNumber();


    /**
     * Gets the movement title.
     *
     * @return the movement title
     */
    String getMovementTitle();


    /**
     * Gets the identification.
     *
     * @return the identification
     */
    IIdentification getIdentification();


    /**
     * Gets the defaults.
     *
     * @return the defaults
     */
    IDefaults getDefaults();


    /**
     * Gets the credits.
     *
     * @return the credits
     */
    List<? extends ICredit> getCredits();


    /**
     * Gets the part list.
     *
     * @return the part list
     */
    IPartList getPartList();


    /**
     * Gets the parts.
     *
     * @return the parts
     */
    List<? extends IPart> getParts();
}
