/*
 * IPartGroup.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part;


/**
 * Represents the MusicXML part-group element.
 *
 * @author patrick
 */
public interface IPartGroup {

    /**
     * Gets the type (start or stop).
     *
     * @return the type
     */
    String getType();

    /**
     * Gets the number.
     *
     * @return the number
     */
    Integer getNumber();

    /**
     * Gets the group name.
     *
     * @return the group name
     */
    String getGroupName();

    /**
     * Gets the group abbreviation.
     *
     * @return the group abbreviation
     */
    String getGroupAbbreviation();

    /**
     * Gets the group symbol (none, brace, line, bracket, square).
     *
     * @return the group symbol
     */
    String getGroupSymbol();

    /**
     * Gets the group barline (yes, no, Mensurstrich).
     *
     * @return the group barline
     */
    String getGroupBarline();
}
