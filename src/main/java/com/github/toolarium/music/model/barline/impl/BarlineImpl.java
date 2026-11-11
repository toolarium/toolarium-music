/*
 * BarlineImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.barline.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.barline.IBarline;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.notation.impl.FermataImpl;
import java.util.Objects;

/**
 * Implements the {@link IBarline} interface.
 *
 * @author patrick
 */
public class BarlineImpl implements IBarline, IMusicDataElement {

    @JacksonXmlProperty(isAttribute = true, localName = "location")
    private String location;

    @JacksonXmlProperty(localName = "bar-style")
    private String barStyle;

    @JacksonXmlProperty(localName = "repeat")
    private RepeatImpl repeat;

    @JacksonXmlProperty(localName = "ending")
    private EndingImpl ending;

    @JacksonXmlProperty(localName = "fermata")
    private FermataImpl fermata;

    @JacksonXmlProperty(localName = "segno")
    private String segno;

    @JacksonXmlProperty(localName = "coda")
    private String coda;


    /**
     * Gets the location.
     *
     * @return the location
     */
    @Override
    public String getLocation() {
        return location;
    }


    /**
     * Sets the location.
     *
     * @param location the location
     */
    public void setLocation(String location) {
        this.location = location;
    }


    /**
     * Gets the bar style.
     *
     * @return the bar style
     */
    @Override
    public String getBarStyle() {
        return barStyle;
    }


    /**
     * Sets the bar style.
     *
     * @param barStyle the bar style
     */
    public void setBarStyle(String barStyle) {
        this.barStyle = barStyle;
    }


    /**
     * Gets the repeat.
     *
     * @return the repeat
     */
    @Override
    public RepeatImpl getRepeat() {
        return repeat;
    }


    /**
     * Sets the repeat.
     *
     * @param repeat the repeat
     */
    public void setRepeat(RepeatImpl repeat) {
        this.repeat = repeat;
    }


    /**
     * Gets the ending.
     *
     * @return the ending
     */
    @Override
    public EndingImpl getEnding() {
        return ending;
    }


    /**
     * Sets the ending.
     *
     * @param ending the ending
     */
    public void setEnding(EndingImpl ending) {
        this.ending = ending;
    }


    /**
     * Gets the fermata.
     *
     * @return the fermata
     */
    @Override
    public FermataImpl getFermata() {
        return fermata;
    }


    /**
     * Sets the fermata.
     *
     * @param fermata the fermata
     */
    public void setFermata(FermataImpl fermata) {
        this.fermata = fermata;
    }


    /**
     * Gets the segno.
     *
     * @return the segno
     */
    @Override
    public String getSegno() {
        return segno;
    }


    /**
     * Sets the segno.
     *
     * @param segno the segno
     */
    public void setSegno(String segno) {
        this.segno = segno;
    }


    /**
     * Gets the coda.
     *
     * @return the coda
     */
    @Override
    public String getCoda() {
        return coda;
    }


    /**
     * Sets the coda.
     *
     * @param coda the coda
     */
    public void setCoda(String coda) {
        this.coda = coda;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(location, barStyle, repeat, ending, fermata, segno, coda);
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
        BarlineImpl other = (BarlineImpl) obj;
        return Objects.equals(location, other.location)
                && Objects.equals(barStyle, other.barStyle)
                && Objects.equals(repeat, other.repeat)
                && Objects.equals(ending, other.ending)
                && Objects.equals(fermata, other.fermata)
                && Objects.equals(segno, other.segno)
                && Objects.equals(coda, other.coda);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "BarlineImpl [location=" + location + ", barStyle=" + barStyle + ", repeat=" + repeat
                + ", ending=" + ending + ", fermata=" + fermata + ", segno=" + segno + ", coda=" + coda + "]";
    }
}
