/*
 * TupletImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.notation.ITuplet;
import java.util.Objects;

/**
 * Implements the {@link ITuplet} interface.
 *
 * @author patrick
 */
public class TupletImpl implements ITuplet {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private Integer number;

    @JacksonXmlProperty(isAttribute = true, localName = "bracket")
    private String bracket;

    @JacksonXmlProperty(isAttribute = true, localName = "show-number")
    private String showNumber;

    @JacksonXmlProperty(isAttribute = true, localName = "show-type")
    private String showType;


    /**
     * Gets the type.
     *
     * @return the type (start, stop)
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type.
     *
     * @param type the type (start, stop)
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
     * Gets the bracket.
     *
     * @return the bracket (yes, no)
     */
    @Override
    public String getBracket() {
        return bracket;
    }


    /**
     * Sets the bracket.
     *
     * @param bracket the bracket (yes, no)
     */
    public void setBracket(String bracket) {
        this.bracket = bracket;
    }


    /**
     * Gets the show number.
     *
     * @return the show number
     */
    @Override
    public String getShowNumber() {
        return showNumber;
    }


    /**
     * Sets the show number.
     *
     * @param showNumber the show number
     */
    public void setShowNumber(String showNumber) {
        this.showNumber = showNumber;
    }


    /**
     * Gets the show type.
     *
     * @return the show type
     */
    @Override
    public String getShowType() {
        return showType;
    }


    /**
     * Sets the show type.
     *
     * @param showType the show type
     */
    public void setShowType(String showType) {
        this.showType = showType;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, number, bracket, showNumber, showType);
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
        TupletImpl other = (TupletImpl) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(number, other.number)
                && Objects.equals(bracket, other.bracket)
                && Objects.equals(showNumber, other.showNumber)
                && Objects.equals(showType, other.showType);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "TupletImpl [type=" + type
                + ", number=" + number
                + ", bracket=" + bracket
                + ", showNumber=" + showNumber
                + ", showType=" + showType + "]";
    }
}
