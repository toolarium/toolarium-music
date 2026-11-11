/*
 * LyricImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.note.ILyric;
import java.util.Objects;

/**
 * Implements the {@link ILyric} interface.
 *
 * @author patrick
 */
public class LyricImpl implements ILyric {

    @JacksonXmlProperty(isAttribute = true, localName = "number")
    private String number;

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    private String name;

    @JacksonXmlProperty(localName = "syllabic")
    private String syllabic;

    @JacksonXmlProperty(localName = "text")
    private String text;

    @JacksonXmlProperty(localName = "elision")
    private String elision;

    @JacksonXmlProperty(localName = "extend")
    private String extend;


    /**
     * Gets the number.
     *
     * @return the number
     */
    @Override
    public String getNumber() {
        return number;
    }


    /**
     * Sets the number.
     *
     * @param number the number
     */
    public void setNumber(String number) {
        this.number = number;
    }


    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }


    /**
     * Sets the name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Gets the syllabic.
     *
     * @return the syllabic
     */
    @Override
    public String getSyllabic() {
        return syllabic;
    }


    /**
     * Sets the syllabic.
     *
     * @param syllabic the syllabic
     */
    public void setSyllabic(String syllabic) {
        this.syllabic = syllabic;
    }


    /**
     * Gets the text.
     *
     * @return the text
     */
    @Override
    public String getText() {
        return text;
    }


    /**
     * Sets the text.
     *
     * @param text the text
     */
    public void setText(String text) {
        this.text = text;
    }


    /**
     * Gets the elision.
     *
     * @return the elision
     */
    @Override
    public String getElision() {
        return elision;
    }


    /**
     * Sets the elision.
     *
     * @param elision the elision
     */
    public void setElision(String elision) {
        this.elision = elision;
    }


    /**
     * Gets the extend.
     *
     * @return the extend
     */
    @Override
    public String getExtend() {
        return extend;
    }


    /**
     * Sets the extend.
     *
     * @param extend the extend
     */
    public void setExtend(String extend) {
        this.extend = extend;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(number, name, syllabic, text, elision, extend);
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
        LyricImpl other = (LyricImpl) obj;
        return Objects.equals(number, other.number)
                && Objects.equals(name, other.name)
                && Objects.equals(syllabic, other.syllabic)
                && Objects.equals(text, other.text)
                && Objects.equals(elision, other.elision)
                && Objects.equals(extend, other.extend);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "LyricImpl [number=" + number + ", name=" + name + ", syllabic=" + syllabic
                + ", text=" + text + ", elision=" + elision + ", extend=" + extend + "]";
    }
}
