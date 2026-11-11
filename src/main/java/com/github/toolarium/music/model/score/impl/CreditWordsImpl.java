/*
 * CreditWordsImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.github.toolarium.music.model.score.ICreditWords;
import java.util.Objects;

/**
 * Implements the {@link ICreditWords} interface.
 *
 * @author patrick
 */
public class CreditWordsImpl implements ICreditWords {

    @JacksonXmlText
    private String value;

    @JacksonXmlProperty(isAttribute = true, localName = "default-x")
    private String defaultX;

    @JacksonXmlProperty(isAttribute = true, localName = "default-y")
    private String defaultY;

    @JacksonXmlProperty(isAttribute = true, localName = "font-size")
    private String fontSize;

    @JacksonXmlProperty(isAttribute = true, localName = "justify")
    private String justify;

    @JacksonXmlProperty(isAttribute = true, localName = "valign")
    private String valign;


    /**
     * Gets the value.
     *
     * @return the value
     */
    @Override
    public String getValue() {
        return value;
    }


    /**
     * Sets the value.
     *
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }


    /**
     * Gets the default-x attribute.
     *
     * @return the default-x attribute
     */
    @Override
    public String getDefaultX() {
        return defaultX;
    }


    /**
     * Sets the default-x attribute.
     *
     * @param defaultX the default-x attribute
     */
    public void setDefaultX(String defaultX) {
        this.defaultX = defaultX;
    }


    /**
     * Gets the default-y attribute.
     *
     * @return the default-y attribute
     */
    @Override
    public String getDefaultY() {
        return defaultY;
    }


    /**
     * Sets the default-y attribute.
     *
     * @param defaultY the default-y attribute
     */
    public void setDefaultY(String defaultY) {
        this.defaultY = defaultY;
    }


    /**
     * Gets the font-size attribute.
     *
     * @return the font-size attribute
     */
    @Override
    public String getFontSize() {
        return fontSize;
    }


    /**
     * Sets the font-size attribute.
     *
     * @param fontSize the font-size attribute
     */
    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }


    /**
     * Gets the justify attribute.
     *
     * @return the justify attribute
     */
    @Override
    public String getJustify() {
        return justify;
    }


    /**
     * Sets the justify attribute.
     *
     * @param justify the justify attribute
     */
    public void setJustify(String justify) {
        this.justify = justify;
    }


    /**
     * Gets the valign attribute.
     *
     * @return the valign attribute
     */
    @Override
    public String getValign() {
        return valign;
    }


    /**
     * Sets the valign attribute.
     *
     * @param valign the valign attribute
     */
    public void setValign(String valign) {
        this.valign = valign;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(value, defaultX, defaultY, fontSize, justify, valign);
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
        CreditWordsImpl other = (CreditWordsImpl) obj;
        return Objects.equals(value, other.value)
                && Objects.equals(defaultX, other.defaultX)
                && Objects.equals(defaultY, other.defaultY)
                && Objects.equals(fontSize, other.fontSize)
                && Objects.equals(justify, other.justify)
                && Objects.equals(valign, other.valign);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "CreditWordsImpl [value=" + value
                + ", defaultX=" + defaultX
                + ", defaultY=" + defaultY
                + ", fontSize=" + fontSize
                + ", justify=" + justify
                + ", valign=" + valign + "]";
    }
}
