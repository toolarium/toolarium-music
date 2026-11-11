/*
 * NotationsImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.notation.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.notation.INotations;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link INotations} interface.
 *
 * @author patrick
 */
public class NotationsImpl implements INotations {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "tied")
    private List<TiedImpl> tieds;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "slur")
    private List<SlurImpl> slurs;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "tuplet")
    private List<TupletImpl> tuplets;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "fermata")
    private List<FermataImpl> fermatas;

    @JacksonXmlProperty(localName = "articulations")
    private ArticulationsImpl articulations;

    @JacksonXmlProperty(localName = "dynamics")
    private DynamicsImpl dynamics;

    @JacksonXmlProperty(localName = "ornaments")
    private OrnamentsImpl ornaments;

    @JacksonXmlProperty(localName = "technical")
    private PlayingTechniqueImpl technical;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "glissando")
    private List<GlissandoImpl> glissandos;


    /**
     * Gets the list of tied elements.
     *
     * @return the list of tied elements
     */
    @Override
    public List<TiedImpl> getTieds() {
        return tieds;
    }


    /**
     * Sets the list of tied elements.
     *
     * @param tieds the list of tied elements
     */
    public void setTieds(List<TiedImpl> tieds) {
        this.tieds = tieds;
    }


    /**
     * Gets the list of slur elements.
     *
     * @return the list of slur elements
     */
    @Override
    public List<SlurImpl> getSlurs() {
        return slurs;
    }


    /**
     * Sets the list of slur elements.
     *
     * @param slurs the list of slur elements
     */
    public void setSlurs(List<SlurImpl> slurs) {
        this.slurs = slurs;
    }


    /**
     * Gets the list of tuplet elements.
     *
     * @return the list of tuplet elements
     */
    @Override
    public List<TupletImpl> getTuplets() {
        return tuplets;
    }


    /**
     * Sets the list of tuplet elements.
     *
     * @param tuplets the list of tuplet elements
     */
    public void setTuplets(List<TupletImpl> tuplets) {
        this.tuplets = tuplets;
    }


    /**
     * Gets the list of fermata elements.
     *
     * @return the list of fermata elements
     */
    @Override
    public List<FermataImpl> getFermatas() {
        return fermatas;
    }


    /**
     * Sets the list of fermata elements.
     *
     * @param fermatas the list of fermata elements
     */
    public void setFermatas(List<FermataImpl> fermatas) {
        this.fermatas = fermatas;
    }


    /**
     * Gets the articulations.
     *
     * @return the articulations
     */
    @Override
    public ArticulationsImpl getArticulations() {
        return articulations;
    }


    /**
     * Sets the articulations.
     *
     * @param articulations the articulations
     */
    public void setArticulations(ArticulationsImpl articulations) {
        this.articulations = articulations;
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
     * Gets the ornaments.
     *
     * @return the ornaments
     */
    @Override
    public OrnamentsImpl getOrnaments() {
        return ornaments;
    }


    /**
     * Sets the ornaments.
     *
     * @param ornaments the ornaments
     */
    public void setOrnaments(OrnamentsImpl ornaments) {
        this.ornaments = ornaments;
    }


    /**
     * Gets the technical.
     *
     * @return the technical
     */
    @Override
    public PlayingTechniqueImpl getTechnical() {
        return technical;
    }


    /**
     * Sets the technical.
     *
     * @param technical the technical
     */
    public void setTechnical(PlayingTechniqueImpl technical) {
        this.technical = technical;
    }


    /**
     * Gets the list of glissando elements.
     *
     * @return the list of glissando elements
     */
    @Override
    public List<GlissandoImpl> getGlissandos() {
        return glissandos;
    }


    /**
     * Sets the list of glissando elements.
     *
     * @param glissandos the list of glissando elements
     */
    public void setGlissandos(List<GlissandoImpl> glissandos) {
        this.glissandos = glissandos;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(tieds, slurs, tuplets, fermatas, articulations,
                dynamics, ornaments, technical, glissandos);
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
        NotationsImpl other = (NotationsImpl) obj;
        return Objects.equals(tieds, other.tieds)
                && Objects.equals(slurs, other.slurs)
                && Objects.equals(tuplets, other.tuplets)
                && Objects.equals(fermatas, other.fermatas)
                && Objects.equals(articulations, other.articulations)
                && Objects.equals(dynamics, other.dynamics)
                && Objects.equals(ornaments, other.ornaments)
                && Objects.equals(technical, other.technical)
                && Objects.equals(glissandos, other.glissandos);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "NotationsImpl [tieds=" + tieds
                + ", slurs=" + slurs
                + ", tuplets=" + tuplets
                + ", fermatas=" + fermatas
                + ", articulations=" + articulations
                + ", dynamics=" + dynamics
                + ", ornaments=" + ornaments
                + ", technical=" + technical
                + ", glissandos=" + glissandos + "]";
    }
}
