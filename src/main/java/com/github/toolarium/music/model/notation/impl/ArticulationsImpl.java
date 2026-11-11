/*
 * ArticulationsImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.notation.IArticulations;
import java.util.Objects;

/**
 * Implements the {@link IArticulations} interface.
 *
 * @author patrick
 */
public class ArticulationsImpl implements IArticulations {

    @JacksonXmlProperty(localName = "accent")
    private String accent;

    @JacksonXmlProperty(localName = "strong-accent")
    private String strongAccent;

    @JacksonXmlProperty(localName = "staccato")
    private String staccato;

    @JacksonXmlProperty(localName = "tenuto")
    private String tenuto;

    @JacksonXmlProperty(localName = "detached-legato")
    private String detachedLegato;

    @JacksonXmlProperty(localName = "staccatissimo")
    private String staccatissimo;

    @JacksonXmlProperty(localName = "spiccato")
    private String spiccato;

    @JacksonXmlProperty(localName = "breath-mark")
    private String breathMark;

    @JacksonXmlProperty(localName = "caesura")
    private String caesura;

    @JacksonXmlProperty(localName = "stress")
    private String stress;

    @JacksonXmlProperty(localName = "unstress")
    private String unstress;


    /**
     * Gets the accent.
     *
     * @return the accent
     */
    @Override
    public String getAccent() {
        return accent;
    }


    /**
     * Sets the accent.
     *
     * @param accent the accent
     */
    public void setAccent(String accent) {
        this.accent = accent;
    }


    /**
     * Gets the strong accent.
     *
     * @return the strong accent
     */
    @Override
    public String getStrongAccent() {
        return strongAccent;
    }


    /**
     * Sets the strong accent.
     *
     * @param strongAccent the strong accent
     */
    public void setStrongAccent(String strongAccent) {
        this.strongAccent = strongAccent;
    }


    /**
     * Gets the staccato.
     *
     * @return the staccato
     */
    @Override
    public String getStaccato() {
        return staccato;
    }


    /**
     * Sets the staccato.
     *
     * @param staccato the staccato
     */
    public void setStaccato(String staccato) {
        this.staccato = staccato;
    }


    /**
     * Gets the tenuto.
     *
     * @return the tenuto
     */
    @Override
    public String getTenuto() {
        return tenuto;
    }


    /**
     * Sets the tenuto.
     *
     * @param tenuto the tenuto
     */
    public void setTenuto(String tenuto) {
        this.tenuto = tenuto;
    }


    /**
     * Gets the detached legato.
     *
     * @return the detached legato
     */
    @Override
    public String getDetachedLegato() {
        return detachedLegato;
    }


    /**
     * Sets the detached legato.
     *
     * @param detachedLegato the detached legato
     */
    public void setDetachedLegato(String detachedLegato) {
        this.detachedLegato = detachedLegato;
    }


    /**
     * Gets the staccatissimo.
     *
     * @return the staccatissimo
     */
    @Override
    public String getStaccatissimo() {
        return staccatissimo;
    }


    /**
     * Sets the staccatissimo.
     *
     * @param staccatissimo the staccatissimo
     */
    public void setStaccatissimo(String staccatissimo) {
        this.staccatissimo = staccatissimo;
    }


    /**
     * Gets the spiccato.
     *
     * @return the spiccato
     */
    @Override
    public String getSpiccato() {
        return spiccato;
    }


    /**
     * Sets the spiccato.
     *
     * @param spiccato the spiccato
     */
    public void setSpiccato(String spiccato) {
        this.spiccato = spiccato;
    }


    /**
     * Gets the breath mark.
     *
     * @return the breath mark
     */
    @Override
    public String getBreathMark() {
        return breathMark;
    }


    /**
     * Sets the breath mark.
     *
     * @param breathMark the breath mark
     */
    public void setBreathMark(String breathMark) {
        this.breathMark = breathMark;
    }


    /**
     * Gets the caesura.
     *
     * @return the caesura
     */
    @Override
    public String getCaesura() {
        return caesura;
    }


    /**
     * Sets the caesura.
     *
     * @param caesura the caesura
     */
    public void setCaesura(String caesura) {
        this.caesura = caesura;
    }


    /**
     * Gets the stress.
     *
     * @return the stress
     */
    @Override
    public String getStress() {
        return stress;
    }


    /**
     * Sets the stress.
     *
     * @param stress the stress
     */
    public void setStress(String stress) {
        this.stress = stress;
    }


    /**
     * Gets the unstress.
     *
     * @return the unstress
     */
    @Override
    public String getUnstress() {
        return unstress;
    }


    /**
     * Sets the unstress.
     *
     * @param unstress the unstress
     */
    public void setUnstress(String unstress) {
        this.unstress = unstress;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(accent, strongAccent, staccato, tenuto, detachedLegato,
                staccatissimo, spiccato, breathMark, caesura, stress, unstress);
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
        ArticulationsImpl other = (ArticulationsImpl) obj;
        return Objects.equals(accent, other.accent)
                && Objects.equals(strongAccent, other.strongAccent)
                && Objects.equals(staccato, other.staccato)
                && Objects.equals(tenuto, other.tenuto)
                && Objects.equals(detachedLegato, other.detachedLegato)
                && Objects.equals(staccatissimo, other.staccatissimo)
                && Objects.equals(spiccato, other.spiccato)
                && Objects.equals(breathMark, other.breathMark)
                && Objects.equals(caesura, other.caesura)
                && Objects.equals(stress, other.stress)
                && Objects.equals(unstress, other.unstress);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "ArticulationsImpl [accent=" + accent
                + ", strongAccent=" + strongAccent
                + ", staccato=" + staccato
                + ", tenuto=" + tenuto
                + ", detachedLegato=" + detachedLegato
                + ", staccatissimo=" + staccatissimo
                + ", spiccato=" + spiccato
                + ", breathMark=" + breathMark
                + ", caesura=" + caesura
                + ", stress=" + stress
                + ", unstress=" + unstress + "]";
    }
}
