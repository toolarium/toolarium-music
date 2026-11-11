/*
 * SoundImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.ISound;
import java.util.Objects;

/**
 * Implements the {@link ISound} interface.
 *
 * @author patrick
 */
public class SoundImpl implements ISound, IMusicDataElement {

    @JacksonXmlProperty(isAttribute = true, localName = "tempo")
    private String tempo;

    @JacksonXmlProperty(isAttribute = true, localName = "dynamics")
    private String dynamics;

    @JacksonXmlProperty(isAttribute = true, localName = "dacapo")
    private String dacapo;

    @JacksonXmlProperty(isAttribute = true, localName = "segno")
    private String segno;

    @JacksonXmlProperty(isAttribute = true, localName = "dalsegno")
    private String dalsegno;

    @JacksonXmlProperty(isAttribute = true, localName = "coda")
    private String coda;

    @JacksonXmlProperty(isAttribute = true, localName = "tocoda")
    private String tocoda;

    @JacksonXmlProperty(isAttribute = true, localName = "fine")
    private String fine;

    @JacksonXmlProperty(isAttribute = true, localName = "forward-repeat")
    private String forwardRepeat;


    /**
     * Gets the tempo.
     *
     * @return the tempo
     */
    @Override
    public String getTempo() {
        return tempo;
    }


    /**
     * Sets the tempo.
     *
     * @param tempo the tempo
     */
    public void setTempo(String tempo) {
        this.tempo = tempo;
    }


    /**
     * Gets the dynamics.
     *
     * @return the dynamics
     */
    @Override
    public String getDynamics() {
        return dynamics;
    }


    /**
     * Sets the dynamics.
     *
     * @param dynamics the dynamics
     */
    public void setDynamics(String dynamics) {
        this.dynamics = dynamics;
    }


    /**
     * Gets the dacapo.
     *
     * @return the dacapo
     */
    @Override
    public String getDacapo() {
        return dacapo;
    }


    /**
     * Sets the dacapo.
     *
     * @param dacapo the dacapo
     */
    public void setDacapo(String dacapo) {
        this.dacapo = dacapo;
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
     * Gets the dalsegno.
     *
     * @return the dalsegno
     */
    @Override
    public String getDalsegno() {
        return dalsegno;
    }


    /**
     * Sets the dalsegno.
     *
     * @param dalsegno the dalsegno
     */
    public void setDalsegno(String dalsegno) {
        this.dalsegno = dalsegno;
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
     * Gets the tocoda.
     *
     * @return the tocoda
     */
    @Override
    public String getTocoda() {
        return tocoda;
    }


    /**
     * Sets the tocoda.
     *
     * @param tocoda the tocoda
     */
    public void setTocoda(String tocoda) {
        this.tocoda = tocoda;
    }


    /**
     * Gets the fine.
     *
     * @return the fine
     */
    @Override
    public String getFine() {
        return fine;
    }


    /**
     * Sets the fine.
     *
     * @param fine the fine
     */
    public void setFine(String fine) {
        this.fine = fine;
    }


    /**
     * Gets the forward repeat.
     *
     * @return the forward repeat
     */
    @Override
    public String getForwardRepeat() {
        return forwardRepeat;
    }


    /**
     * Sets the forward repeat.
     *
     * @param forwardRepeat the forward repeat
     */
    public void setForwardRepeat(String forwardRepeat) {
        this.forwardRepeat = forwardRepeat;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(tempo, dynamics, dacapo, segno, dalsegno,
                coda, tocoda, fine, forwardRepeat);
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
        SoundImpl other = (SoundImpl) obj;
        return Objects.equals(tempo, other.tempo)
                && Objects.equals(dynamics, other.dynamics)
                && Objects.equals(dacapo, other.dacapo)
                && Objects.equals(segno, other.segno)
                && Objects.equals(dalsegno, other.dalsegno)
                && Objects.equals(coda, other.coda)
                && Objects.equals(tocoda, other.tocoda)
                && Objects.equals(fine, other.fine)
                && Objects.equals(forwardRepeat, other.forwardRepeat);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "SoundImpl [tempo=" + tempo
                + ", dynamics=" + dynamics
                + ", dacapo=" + dacapo
                + ", segno=" + segno
                + ", dalsegno=" + dalsegno
                + ", coda=" + coda
                + ", tocoda=" + tocoda
                + ", fine=" + fine
                + ", forwardRepeat=" + forwardRepeat + "]";
    }
}
