/*
 * DynamicsImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.notation.IDynamics;
import java.util.Objects;

/**
 * Implements the {@link IDynamics} interface.
 *
 * @author patrick
 */
public class DynamicsImpl implements IDynamics {

    @JacksonXmlProperty(localName = "p")
    private String piano;

    @JacksonXmlProperty(localName = "pp")
    private String pp;

    @JacksonXmlProperty(localName = "ppp")
    private String ppp;

    @JacksonXmlProperty(localName = "pppp")
    private String pppp;

    @JacksonXmlProperty(localName = "f")
    private String forte;

    @JacksonXmlProperty(localName = "ff")
    private String ff;

    @JacksonXmlProperty(localName = "fff")
    private String fff;

    @JacksonXmlProperty(localName = "ffff")
    private String ffff;

    @JacksonXmlProperty(localName = "mp")
    private String mp;

    @JacksonXmlProperty(localName = "mf")
    private String mf;

    @JacksonXmlProperty(localName = "sf")
    private String sf;

    @JacksonXmlProperty(localName = "sfp")
    private String sfp;

    @JacksonXmlProperty(localName = "sfz")
    private String sfz;

    @JacksonXmlProperty(localName = "fp")
    private String fp;

    @JacksonXmlProperty(localName = "rf")
    private String rf;

    @JacksonXmlProperty(localName = "rfz")
    private String rfz;

    @JacksonXmlProperty(localName = "other-dynamics")
    private String otherDynamics;

    @JacksonXmlProperty(isAttribute = true, localName = "placement")
    private String placement;


    /**
     * Gets the piano (p) dynamic.
     *
     * @return the piano dynamic
     */
    @Override
    public String getPiano() {
        return piano;
    }


    /**
     * Sets the piano dynamic.
     *
     * @param piano the piano dynamic
     */
    public void setPiano(String piano) {
        this.piano = piano;
    }


    /**
     * Gets the pp dynamic.
     *
     * @return the pp dynamic
     */
    @Override
    public String getPp() {
        return pp;
    }


    /**
     * Sets the pp dynamic.
     *
     * @param pp the pp dynamic
     */
    public void setPp(String pp) {
        this.pp = pp;
    }


    /**
     * Gets the ppp dynamic.
     *
     * @return the ppp dynamic
     */
    @Override
    public String getPpp() {
        return ppp;
    }


    /**
     * Sets the ppp dynamic.
     *
     * @param ppp the ppp dynamic
     */
    public void setPpp(String ppp) {
        this.ppp = ppp;
    }


    /**
     * Gets the pppp dynamic.
     *
     * @return the pppp dynamic
     */
    @Override
    public String getPppp() {
        return pppp;
    }


    /**
     * Sets the pppp dynamic.
     *
     * @param pppp the pppp dynamic
     */
    public void setPppp(String pppp) {
        this.pppp = pppp;
    }


    /**
     * Gets the forte (f) dynamic.
     *
     * @return the forte dynamic
     */
    @Override
    public String getForte() {
        return forte;
    }


    /**
     * Sets the forte dynamic.
     *
     * @param forte the forte dynamic
     */
    public void setForte(String forte) {
        this.forte = forte;
    }


    /**
     * Gets the ff dynamic.
     *
     * @return the ff dynamic
     */
    @Override
    public String getFf() {
        return ff;
    }


    /**
     * Sets the ff dynamic.
     *
     * @param ff the ff dynamic
     */
    public void setFf(String ff) {
        this.ff = ff;
    }


    /**
     * Gets the fff dynamic.
     *
     * @return the fff dynamic
     */
    @Override
    public String getFff() {
        return fff;
    }


    /**
     * Sets the fff dynamic.
     *
     * @param fff the fff dynamic
     */
    public void setFff(String fff) {
        this.fff = fff;
    }


    /**
     * Gets the ffff dynamic.
     *
     * @return the ffff dynamic
     */
    @Override
    public String getFfff() {
        return ffff;
    }


    /**
     * Sets the ffff dynamic.
     *
     * @param ffff the ffff dynamic
     */
    public void setFfff(String ffff) {
        this.ffff = ffff;
    }


    /**
     * Gets the mp dynamic.
     *
     * @return the mp dynamic
     */
    @Override
    public String getMp() {
        return mp;
    }


    /**
     * Sets the mp dynamic.
     *
     * @param mp the mp dynamic
     */
    public void setMp(String mp) {
        this.mp = mp;
    }


    /**
     * Gets the mf dynamic.
     *
     * @return the mf dynamic
     */
    @Override
    public String getMf() {
        return mf;
    }


    /**
     * Sets the mf dynamic.
     *
     * @param mf the mf dynamic
     */
    public void setMf(String mf) {
        this.mf = mf;
    }


    /**
     * Gets the sf dynamic.
     *
     * @return the sf dynamic
     */
    @Override
    public String getSf() {
        return sf;
    }


    /**
     * Sets the sf dynamic.
     *
     * @param sf the sf dynamic
     */
    public void setSf(String sf) {
        this.sf = sf;
    }


    /**
     * Gets the sfp dynamic.
     *
     * @return the sfp dynamic
     */
    @Override
    public String getSfp() {
        return sfp;
    }


    /**
     * Sets the sfp dynamic.
     *
     * @param sfp the sfp dynamic
     */
    public void setSfp(String sfp) {
        this.sfp = sfp;
    }


    /**
     * Gets the sfz dynamic.
     *
     * @return the sfz dynamic
     */
    @Override
    public String getSfz() {
        return sfz;
    }


    /**
     * Sets the sfz dynamic.
     *
     * @param sfz the sfz dynamic
     */
    public void setSfz(String sfz) {
        this.sfz = sfz;
    }


    /**
     * Gets the fp dynamic.
     *
     * @return the fp dynamic
     */
    @Override
    public String getFp() {
        return fp;
    }


    /**
     * Sets the fp dynamic.
     *
     * @param fp the fp dynamic
     */
    public void setFp(String fp) {
        this.fp = fp;
    }


    /**
     * Gets the rf dynamic.
     *
     * @return the rf dynamic
     */
    @Override
    public String getRf() {
        return rf;
    }


    /**
     * Sets the rf dynamic.
     *
     * @param rf the rf dynamic
     */
    public void setRf(String rf) {
        this.rf = rf;
    }


    /**
     * Gets the rfz dynamic.
     *
     * @return the rfz dynamic
     */
    @Override
    public String getRfz() {
        return rfz;
    }


    /**
     * Sets the rfz dynamic.
     *
     * @param rfz the rfz dynamic
     */
    public void setRfz(String rfz) {
        this.rfz = rfz;
    }


    /**
     * Gets the other dynamics.
     *
     * @return the other dynamics
     */
    @Override
    public String getOtherDynamics() {
        return otherDynamics;
    }


    /**
     * Sets the other dynamics.
     *
     * @param otherDynamics the other dynamics
     */
    public void setOtherDynamics(String otherDynamics) {
        this.otherDynamics = otherDynamics;
    }


    /**
     * Gets the placement.
     *
     * @return the placement
     */
    @Override
    public String getPlacement() {
        return placement;
    }


    /**
     * Sets the placement.
     *
     * @param placement the placement
     */
    public void setPlacement(String placement) {
        this.placement = placement;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(piano, pp, ppp, pppp, forte, ff, fff, ffff, mp, mf,
                sf, sfp, sfz, fp, rf, rfz, otherDynamics, placement);
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
        DynamicsImpl other = (DynamicsImpl) obj;
        return Objects.equals(piano, other.piano)
                && Objects.equals(pp, other.pp)
                && Objects.equals(ppp, other.ppp)
                && Objects.equals(pppp, other.pppp)
                && Objects.equals(forte, other.forte)
                && Objects.equals(ff, other.ff)
                && Objects.equals(fff, other.fff)
                && Objects.equals(ffff, other.ffff)
                && Objects.equals(mp, other.mp)
                && Objects.equals(mf, other.mf)
                && Objects.equals(sf, other.sf)
                && Objects.equals(sfp, other.sfp)
                && Objects.equals(sfz, other.sfz)
                && Objects.equals(fp, other.fp)
                && Objects.equals(rf, other.rf)
                && Objects.equals(rfz, other.rfz)
                && Objects.equals(otherDynamics, other.otherDynamics)
                && Objects.equals(placement, other.placement);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "DynamicsImpl [piano=" + piano
                + ", pp=" + pp
                + ", ppp=" + ppp
                + ", pppp=" + pppp
                + ", forte=" + forte
                + ", ff=" + ff
                + ", fff=" + fff
                + ", ffff=" + ffff
                + ", mp=" + mp
                + ", mf=" + mf
                + ", sf=" + sf
                + ", sfp=" + sfp
                + ", sfz=" + sfz
                + ", fp=" + fp
                + ", rf=" + rf
                + ", rfz=" + rfz
                + ", otherDynamics=" + otherDynamics
                + ", placement=" + placement + "]";
    }
}
