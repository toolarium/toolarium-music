/*
 * WordsImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.github.toolarium.music.model.direction.IWords;
import java.util.Objects;

/**
 * Implements the {@link IWords} interface.
 *
 * @author patrick
 */
public class WordsImpl implements IWords {

    @JacksonXmlText
    private String value;

    @JacksonXmlProperty(isAttribute = true, localName = "default-x")
    private String defaultX;

    @JacksonXmlProperty(isAttribute = true, localName = "default-y")
    private String defaultY;

    @JacksonXmlProperty(isAttribute = true, localName = "font-size")
    private String fontSize;

    @JacksonXmlProperty(isAttribute = true, localName = "font-weight")
    private String fontWeight;

    @JacksonXmlProperty(isAttribute = true, localName = "font-style")
    private String fontStyle;


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
     * Gets the default x position.
     *
     * @return the default x position
     */
    @Override
    public String getDefaultX() {
        return defaultX;
    }


    /**
     * Sets the default x position.
     *
     * @param defaultX the default x position
     */
    public void setDefaultX(String defaultX) {
        this.defaultX = defaultX;
    }


    /**
     * Gets the default y position.
     *
     * @return the default y position
     */
    @Override
    public String getDefaultY() {
        return defaultY;
    }


    /**
     * Sets the default y position.
     *
     * @param defaultY the default y position
     */
    public void setDefaultY(String defaultY) {
        this.defaultY = defaultY;
    }


    /**
     * Gets the font size.
     *
     * @return the font size
     */
    @Override
    public String getFontSize() {
        return fontSize;
    }


    /**
     * Sets the font size.
     *
     * @param fontSize the font size
     */
    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }


    /**
     * Gets the font weight.
     *
     * @return the font weight
     */
    @Override
    public String getFontWeight() {
        return fontWeight;
    }


    /**
     * Sets the font weight.
     *
     * @param fontWeight the font weight
     */
    public void setFontWeight(String fontWeight) {
        this.fontWeight = fontWeight;
    }


    /**
     * Gets the font style.
     *
     * @return the font style
     */
    @Override
    public String getFontStyle() {
        return fontStyle;
    }


    /**
     * Sets the font style.
     *
     * @param fontStyle the font style
     */
    public void setFontStyle(String fontStyle) {
        this.fontStyle = fontStyle;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(value, defaultX, defaultY, fontSize, fontWeight, fontStyle);
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
        WordsImpl other = (WordsImpl) obj;
        return Objects.equals(value, other.value)
                && Objects.equals(defaultX, other.defaultX)
                && Objects.equals(defaultY, other.defaultY)
                && Objects.equals(fontSize, other.fontSize)
                && Objects.equals(fontWeight, other.fontWeight)
                && Objects.equals(fontStyle, other.fontStyle);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "WordsImpl [value=" + value
                + ", defaultX=" + defaultX
                + ", defaultY=" + defaultY
                + ", fontSize=" + fontSize
                + ", fontWeight=" + fontWeight
                + ", fontStyle=" + fontStyle + "]";
    }
}
