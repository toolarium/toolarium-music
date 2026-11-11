/*
 * DirectionTypeImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.direction.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.notation.impl.DynamicsImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IDirectionType} interface.
 *
 * @author patrick
 */
public class DirectionTypeImpl implements IDirectionType {

    @JacksonXmlProperty(localName = "rehearsal")
    private String rehearsal;

    @JacksonXmlProperty(localName = "segno")
    private String segno;

    @JacksonXmlProperty(localName = "coda")
    private String coda;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "words")
    private List<WordsImpl> words;

    @JacksonXmlProperty(localName = "wedge")
    private WedgeImpl wedge;

    @JacksonXmlProperty(localName = "dashes")
    private String dashes;

    @JacksonXmlProperty(localName = "bracket")
    private String bracket;

    @JacksonXmlProperty(localName = "pedal")
    private PedalImpl pedal;

    @JacksonXmlProperty(localName = "metronome")
    private MetronomeImpl metronome;

    @JacksonXmlProperty(localName = "octave-shift")
    private OctaveShiftImpl octaveShift;

    @JacksonXmlProperty(localName = "dynamics")
    private DynamicsImpl dynamics;


    /**
     * Constructor.
     */
    public DirectionTypeImpl() {
        this.words = new ArrayList<>();
    }


    /**
     * Gets the rehearsal.
     *
     * @return the rehearsal
     */
    @Override
    public String getRehearsal() {
        return rehearsal;
    }


    /**
     * Sets the rehearsal.
     *
     * @param rehearsal the rehearsal
     */
    public void setRehearsal(String rehearsal) {
        this.rehearsal = rehearsal;
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
     * Gets the list of words elements.
     *
     * @return the list of words elements
     */
    @Override
    public List<WordsImpl> getWords() {
        return words;
    }


    /**
     * Sets the list of words elements.
     *
     * @param words the list of words elements
     */
    public void setWords(List<WordsImpl> words) {
        this.words = words;
    }


    /**
     * Gets the wedge.
     *
     * @return the wedge
     */
    @Override
    public WedgeImpl getWedge() {
        return wedge;
    }


    /**
     * Sets the wedge.
     *
     * @param wedge the wedge
     */
    public void setWedge(WedgeImpl wedge) {
        this.wedge = wedge;
    }


    /**
     * Gets the dashes.
     *
     * @return the dashes
     */
    @Override
    public String getDashes() {
        return dashes;
    }


    /**
     * Sets the dashes.
     *
     * @param dashes the dashes
     */
    public void setDashes(String dashes) {
        this.dashes = dashes;
    }


    /**
     * Gets the bracket.
     *
     * @return the bracket
     */
    @Override
    public String getBracket() {
        return bracket;
    }


    /**
     * Sets the bracket.
     *
     * @param bracket the bracket
     */
    public void setBracket(String bracket) {
        this.bracket = bracket;
    }


    /**
     * Gets the pedal.
     *
     * @return the pedal
     */
    @Override
    public PedalImpl getPedal() {
        return pedal;
    }


    /**
     * Sets the pedal.
     *
     * @param pedal the pedal
     */
    public void setPedal(PedalImpl pedal) {
        this.pedal = pedal;
    }


    /**
     * Gets the metronome.
     *
     * @return the metronome
     */
    @Override
    public MetronomeImpl getMetronome() {
        return metronome;
    }


    /**
     * Sets the metronome.
     *
     * @param metronome the metronome
     */
    public void setMetronome(MetronomeImpl metronome) {
        this.metronome = metronome;
    }


    /**
     * Gets the octave shift.
     *
     * @return the octave shift
     */
    @Override
    public OctaveShiftImpl getOctaveShift() {
        return octaveShift;
    }


    /**
     * Sets the octave shift.
     *
     * @param octaveShift the octave shift
     */
    public void setOctaveShift(OctaveShiftImpl octaveShift) {
        this.octaveShift = octaveShift;
    }


    /**
     * Gets the dynamics.
     *
     * @return the dynamics
     */
    @Override
    public DynamicsImpl getDynamics() {
        return dynamics;
    }


    /**
     * Sets the dynamics.
     *
     * @param dynamics the dynamics
     */
    public void setDynamics(DynamicsImpl dynamics) {
        this.dynamics = dynamics;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(rehearsal, segno, coda, words, wedge, dashes,
                bracket, pedal, metronome, octaveShift, dynamics);
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
        DirectionTypeImpl other = (DirectionTypeImpl) obj;
        return Objects.equals(rehearsal, other.rehearsal)
                && Objects.equals(segno, other.segno)
                && Objects.equals(coda, other.coda)
                && Objects.equals(words, other.words)
                && Objects.equals(wedge, other.wedge)
                && Objects.equals(dashes, other.dashes)
                && Objects.equals(bracket, other.bracket)
                && Objects.equals(pedal, other.pedal)
                && Objects.equals(metronome, other.metronome)
                && Objects.equals(octaveShift, other.octaveShift)
                && Objects.equals(dynamics, other.dynamics);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "DirectionTypeImpl [rehearsal=" + rehearsal
                + ", segno=" + segno
                + ", coda=" + coda
                + ", words=" + words
                + ", wedge=" + wedge
                + ", dashes=" + dashes
                + ", bracket=" + bracket
                + ", pedal=" + pedal
                + ", metronome=" + metronome
                + ", octaveShift=" + octaveShift
                + ", dynamics=" + dynamics + "]";
    }
}
