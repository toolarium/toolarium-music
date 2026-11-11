/*
 * IPartList.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part;

import java.util.List;


/**
 * Represents the MusicXML part-list element.
 *
 * @author patrick
 */
public interface IPartList {

    /**
     * Gets the part groups.
     *
     * @return the part groups
     */
    List<? extends IPartGroup> getPartGroups();

    /**
     * Gets the score parts.
     *
     * @return the score parts
     */
    List<? extends IScorePart> getScoreParts();
}
