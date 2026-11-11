/*
 * PlayingTechniqueImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.notation.IPlayingTechnique;
import java.util.Objects;

/**
 * Implements the {@link IPlayingTechnique} interface.
 *
 * @author patrick
 */
public class PlayingTechniqueImpl implements IPlayingTechnique {

    @JacksonXmlProperty(localName = "up-bow")
    private String upBow;

    @JacksonXmlProperty(localName = "down-bow")
    private String downBow;

    @JacksonXmlProperty(localName = "harmonic")
    private String harmonic;

    @JacksonXmlProperty(localName = "open-string")
    private String openString;

    @JacksonXmlProperty(localName = "thumb-position")
    private String thumbPosition;

    @JacksonXmlProperty(localName = "fingering")
    private String fingering;

    @JacksonXmlProperty(localName = "pluck")
    private String pluck;

    @JacksonXmlProperty(localName = "double-tongue")
    private String doubleTongue;

    @JacksonXmlProperty(localName = "triple-tongue")
    private String tripleTongue;

    @JacksonXmlProperty(localName = "stopped")
    private String stopped;

    @JacksonXmlProperty(localName = "snap-pizzicato")
    private String snapPizzicato;

    @JacksonXmlProperty(localName = "fret")
    private String fret;

    @JacksonXmlProperty(localName = "string")
    private String string;

    @JacksonXmlProperty(localName = "hammer-on")
    private String hammerOn;

    @JacksonXmlProperty(localName = "pull-off")
    private String pullOff;


    /**
     * Gets the up bow.
     *
     * @return the up bow
     */
    @Override
    public String getUpBow() {
        return upBow;
    }


    /**
     * Sets the up bow.
     *
     * @param upBow the up bow
     */
    public void setUpBow(String upBow) {
        this.upBow = upBow;
    }


    /**
     * Gets the down bow.
     *
     * @return the down bow
     */
    @Override
    public String getDownBow() {
        return downBow;
    }


    /**
     * Sets the down bow.
     *
     * @param downBow the down bow
     */
    public void setDownBow(String downBow) {
        this.downBow = downBow;
    }


    /**
     * Gets the harmonic.
     *
     * @return the harmonic
     */
    @Override
    public String getHarmonic() {
        return harmonic;
    }


    /**
     * Sets the harmonic.
     *
     * @param harmonic the harmonic
     */
    public void setHarmonic(String harmonic) {
        this.harmonic = harmonic;
    }


    /**
     * Gets the open string.
     *
     * @return the open string
     */
    @Override
    public String getOpenString() {
        return openString;
    }


    /**
     * Sets the open string.
     *
     * @param openString the open string
     */
    public void setOpenString(String openString) {
        this.openString = openString;
    }


    /**
     * Gets the thumb position.
     *
     * @return the thumb position
     */
    @Override
    public String getThumbPosition() {
        return thumbPosition;
    }


    /**
     * Sets the thumb position.
     *
     * @param thumbPosition the thumb position
     */
    public void setThumbPosition(String thumbPosition) {
        this.thumbPosition = thumbPosition;
    }


    /**
     * Gets the fingering.
     *
     * @return the fingering
     */
    @Override
    public String getFingering() {
        return fingering;
    }


    /**
     * Sets the fingering.
     *
     * @param fingering the fingering
     */
    public void setFingering(String fingering) {
        this.fingering = fingering;
    }


    /**
     * Gets the pluck.
     *
     * @return the pluck
     */
    @Override
    public String getPluck() {
        return pluck;
    }


    /**
     * Sets the pluck.
     *
     * @param pluck the pluck
     */
    public void setPluck(String pluck) {
        this.pluck = pluck;
    }


    /**
     * Gets the double tongue.
     *
     * @return the double tongue
     */
    @Override
    public String getDoubleTongue() {
        return doubleTongue;
    }


    /**
     * Sets the double tongue.
     *
     * @param doubleTongue the double tongue
     */
    public void setDoubleTongue(String doubleTongue) {
        this.doubleTongue = doubleTongue;
    }


    /**
     * Gets the triple tongue.
     *
     * @return the triple tongue
     */
    @Override
    public String getTripleTongue() {
        return tripleTongue;
    }


    /**
     * Sets the triple tongue.
     *
     * @param tripleTongue the triple tongue
     */
    public void setTripleTongue(String tripleTongue) {
        this.tripleTongue = tripleTongue;
    }


    /**
     * Gets the stopped.
     *
     * @return the stopped
     */
    @Override
    public String getStopped() {
        return stopped;
    }


    /**
     * Sets the stopped.
     *
     * @param stopped the stopped
     */
    public void setStopped(String stopped) {
        this.stopped = stopped;
    }


    /**
     * Gets the snap pizzicato.
     *
     * @return the snap pizzicato
     */
    @Override
    public String getSnapPizzicato() {
        return snapPizzicato;
    }


    /**
     * Sets the snap pizzicato.
     *
     * @param snapPizzicato the snap pizzicato
     */
    public void setSnapPizzicato(String snapPizzicato) {
        this.snapPizzicato = snapPizzicato;
    }


    /**
     * Gets the fret.
     *
     * @return the fret
     */
    @Override
    public String getFret() {
        return fret;
    }


    /**
     * Sets the fret.
     *
     * @param fret the fret
     */
    public void setFret(String fret) {
        this.fret = fret;
    }


    /**
     * Gets the string.
     *
     * @return the string
     */
    @Override
    public String getString() {
        return string;
    }


    /**
     * Sets the string.
     *
     * @param string the string
     */
    public void setString(String string) {
        this.string = string;
    }


    /**
     * Gets the hammer on.
     *
     * @return the hammer on
     */
    @Override
    public String getHammerOn() {
        return hammerOn;
    }


    /**
     * Sets the hammer on.
     *
     * @param hammerOn the hammer on
     */
    public void setHammerOn(String hammerOn) {
        this.hammerOn = hammerOn;
    }


    /**
     * Gets the pull off.
     *
     * @return the pull off
     */
    @Override
    public String getPullOff() {
        return pullOff;
    }


    /**
     * Sets the pull off.
     *
     * @param pullOff the pull off
     */
    public void setPullOff(String pullOff) {
        this.pullOff = pullOff;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(upBow, downBow, harmonic, openString, thumbPosition,
                fingering, pluck, doubleTongue, tripleTongue, stopped,
                snapPizzicato, fret, string, hammerOn, pullOff);
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
        PlayingTechniqueImpl other = (PlayingTechniqueImpl) obj;
        return Objects.equals(upBow, other.upBow)
                && Objects.equals(downBow, other.downBow)
                && Objects.equals(harmonic, other.harmonic)
                && Objects.equals(openString, other.openString)
                && Objects.equals(thumbPosition, other.thumbPosition)
                && Objects.equals(fingering, other.fingering)
                && Objects.equals(pluck, other.pluck)
                && Objects.equals(doubleTongue, other.doubleTongue)
                && Objects.equals(tripleTongue, other.tripleTongue)
                && Objects.equals(stopped, other.stopped)
                && Objects.equals(snapPizzicato, other.snapPizzicato)
                && Objects.equals(fret, other.fret)
                && Objects.equals(string, other.string)
                && Objects.equals(hammerOn, other.hammerOn)
                && Objects.equals(pullOff, other.pullOff);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "PlayingTechniqueImpl [upBow=" + upBow
                + ", downBow=" + downBow
                + ", harmonic=" + harmonic
                + ", openString=" + openString
                + ", thumbPosition=" + thumbPosition
                + ", fingering=" + fingering
                + ", pluck=" + pluck
                + ", doubleTongue=" + doubleTongue
                + ", tripleTongue=" + tripleTongue
                + ", stopped=" + stopped
                + ", snapPizzicato=" + snapPizzicato
                + ", fret=" + fret
                + ", string=" + string
                + ", hammerOn=" + hammerOn
                + ", pullOff=" + pullOff + "]";
    }
}
