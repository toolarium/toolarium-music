/*
 * OrnamentsImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.notation.IOrnaments;
import java.util.Objects;

/**
 * Implements the {@link IOrnaments} interface.
 *
 * @author patrick
 */
public class OrnamentsImpl implements IOrnaments {

    @JacksonXmlProperty(localName = "trill-mark")
    private String trillMark;

    @JacksonXmlProperty(localName = "turn")
    private String turn;

    @JacksonXmlProperty(localName = "delayed-turn")
    private String delayedTurn;

    @JacksonXmlProperty(localName = "inverted-turn")
    private String invertedTurn;

    @JacksonXmlProperty(localName = "delayed-inverted-turn")
    private String delayedInvertedTurn;

    @JacksonXmlProperty(localName = "shake")
    private String shake;

    @JacksonXmlProperty(localName = "mordent")
    private String mordent;

    @JacksonXmlProperty(localName = "inverted-mordent")
    private String invertedMordent;

    @JacksonXmlProperty(localName = "schleifer")
    private String schleifer;

    @JacksonXmlProperty(localName = "wavy-line")
    private String wavyLine;


    /**
     * Gets the trill mark.
     *
     * @return the trill mark
     */
    @Override
    public String getTrillMark() {
        return trillMark;
    }


    /**
     * Sets the trill mark.
     *
     * @param trillMark the trill mark
     */
    public void setTrillMark(String trillMark) {
        this.trillMark = trillMark;
    }


    /**
     * Gets the turn.
     *
     * @return the turn
     */
    @Override
    public String getTurn() {
        return turn;
    }


    /**
     * Sets the turn.
     *
     * @param turn the turn
     */
    public void setTurn(String turn) {
        this.turn = turn;
    }


    /**
     * Gets the delayed turn.
     *
     * @return the delayed turn
     */
    @Override
    public String getDelayedTurn() {
        return delayedTurn;
    }


    /**
     * Sets the delayed turn.
     *
     * @param delayedTurn the delayed turn
     */
    public void setDelayedTurn(String delayedTurn) {
        this.delayedTurn = delayedTurn;
    }


    /**
     * Gets the inverted turn.
     *
     * @return the inverted turn
     */
    @Override
    public String getInvertedTurn() {
        return invertedTurn;
    }


    /**
     * Sets the inverted turn.
     *
     * @param invertedTurn the inverted turn
     */
    public void setInvertedTurn(String invertedTurn) {
        this.invertedTurn = invertedTurn;
    }


    /**
     * Gets the delayed inverted turn.
     *
     * @return the delayed inverted turn
     */
    @Override
    public String getDelayedInvertedTurn() {
        return delayedInvertedTurn;
    }


    /**
     * Sets the delayed inverted turn.
     *
     * @param delayedInvertedTurn the delayed inverted turn
     */
    public void setDelayedInvertedTurn(String delayedInvertedTurn) {
        this.delayedInvertedTurn = delayedInvertedTurn;
    }


    /**
     * Gets the shake.
     *
     * @return the shake
     */
    @Override
    public String getShake() {
        return shake;
    }


    /**
     * Sets the shake.
     *
     * @param shake the shake
     */
    public void setShake(String shake) {
        this.shake = shake;
    }


    /**
     * Gets the mordent.
     *
     * @return the mordent
     */
    @Override
    public String getMordent() {
        return mordent;
    }


    /**
     * Sets the mordent.
     *
     * @param mordent the mordent
     */
    public void setMordent(String mordent) {
        this.mordent = mordent;
    }


    /**
     * Gets the inverted mordent.
     *
     * @return the inverted mordent
     */
    @Override
    public String getInvertedMordent() {
        return invertedMordent;
    }


    /**
     * Sets the inverted mordent.
     *
     * @param invertedMordent the inverted mordent
     */
    public void setInvertedMordent(String invertedMordent) {
        this.invertedMordent = invertedMordent;
    }


    /**
     * Gets the schleifer.
     *
     * @return the schleifer
     */
    @Override
    public String getSchleifer() {
        return schleifer;
    }


    /**
     * Sets the schleifer.
     *
     * @param schleifer the schleifer
     */
    public void setSchleifer(String schleifer) {
        this.schleifer = schleifer;
    }


    /**
     * Gets the wavy line.
     *
     * @return the wavy line
     */
    @Override
    public String getWavyLine() {
        return wavyLine;
    }


    /**
     * Sets the wavy line.
     *
     * @param wavyLine the wavy line
     */
    public void setWavyLine(String wavyLine) {
        this.wavyLine = wavyLine;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(trillMark, turn, delayedTurn, invertedTurn, delayedInvertedTurn,
                shake, mordent, invertedMordent, schleifer, wavyLine);
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
        OrnamentsImpl other = (OrnamentsImpl) obj;
        return Objects.equals(trillMark, other.trillMark)
                && Objects.equals(turn, other.turn)
                && Objects.equals(delayedTurn, other.delayedTurn)
                && Objects.equals(invertedTurn, other.invertedTurn)
                && Objects.equals(delayedInvertedTurn, other.delayedInvertedTurn)
                && Objects.equals(shake, other.shake)
                && Objects.equals(mordent, other.mordent)
                && Objects.equals(invertedMordent, other.invertedMordent)
                && Objects.equals(schleifer, other.schleifer)
                && Objects.equals(wavyLine, other.wavyLine);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "OrnamentsImpl [trillMark=" + trillMark
                + ", turn=" + turn
                + ", delayedTurn=" + delayedTurn
                + ", invertedTurn=" + invertedTurn
                + ", delayedInvertedTurn=" + delayedInvertedTurn
                + ", shake=" + shake
                + ", mordent=" + mordent
                + ", invertedMordent=" + invertedMordent
                + ", schleifer=" + schleifer
                + ", wavyLine=" + wavyLine + "]";
    }
}
