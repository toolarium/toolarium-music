/*
 * PartGroupImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.part.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.part.IPartGroup;
import java.util.Objects;

/**
 * Implements the {@link IPartGroup} interface.
 *
 * @author patrick
 */
public class PartGroupImpl implements IPartGroup {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private Integer number;

    @JacksonXmlProperty(localName = "group-name")
    private String groupName;

    @JacksonXmlProperty(localName = "group-abbreviation")
    private String groupAbbreviation;

    @JacksonXmlProperty(localName = "group-symbol")
    private String groupSymbol;

    @JacksonXmlProperty(localName = "group-barline")
    private String groupBarline;


    /**
     * Gets the type (start or stop).
     *
     * @return the type
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type (start or stop).
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }


    /**
     * Gets the number.
     *
     * @return the number
     */
    @Override
    public Integer getNumber() {
        return number;
    }


    /**
     * Sets the number.
     *
     * @param number the number
     */
    public void setNumber(Integer number) {
        this.number = number;
    }


    /**
     * Gets the group name.
     *
     * @return the group name
     */
    @Override
    public String getGroupName() {
        return groupName;
    }


    /**
     * Sets the group name.
     *
     * @param groupName the group name
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }


    /**
     * Gets the group abbreviation.
     *
     * @return the group abbreviation
     */
    @Override
    public String getGroupAbbreviation() {
        return groupAbbreviation;
    }


    /**
     * Sets the group abbreviation.
     *
     * @param groupAbbreviation the group abbreviation
     */
    public void setGroupAbbreviation(String groupAbbreviation) {
        this.groupAbbreviation = groupAbbreviation;
    }


    /**
     * Gets the group symbol (none, brace, line, bracket, square).
     *
     * @return the group symbol
     */
    @Override
    public String getGroupSymbol() {
        return groupSymbol;
    }


    /**
     * Sets the group symbol (none, brace, line, bracket, square).
     *
     * @param groupSymbol the group symbol
     */
    public void setGroupSymbol(String groupSymbol) {
        this.groupSymbol = groupSymbol;
    }


    /**
     * Gets the group barline (yes, no, Mensurstrich).
     *
     * @return the group barline
     */
    @Override
    public String getGroupBarline() {
        return groupBarline;
    }


    /**
     * Sets the group barline (yes, no, Mensurstrich).
     *
     * @param groupBarline the group barline
     */
    public void setGroupBarline(String groupBarline) {
        this.groupBarline = groupBarline;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, number, groupName, groupAbbreviation, groupSymbol, groupBarline);
    }


    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param obj the reference object to compare
     * @return true if this object is the same as the obj argument, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PartGroupImpl other = (PartGroupImpl) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(number, other.number)
                && Objects.equals(groupName, other.groupName)
                && Objects.equals(groupAbbreviation, other.groupAbbreviation)
                && Objects.equals(groupSymbol, other.groupSymbol)
                && Objects.equals(groupBarline, other.groupBarline);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "PartGroupImpl ["
                + "type=" + type
                + ", number=" + number
                + ", groupName=" + groupName
                + ", groupAbbreviation=" + groupAbbreviation
                + ", groupSymbol=" + groupSymbol
                + ", groupBarline=" + groupBarline
                + "]";
    }
}
