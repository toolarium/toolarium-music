/*
 * MeiControlEvent.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.Objects;
import org.w3c.dom.Element;


/**
 * A control event (tie, slur, dynamics, hairpin and so on) with the measure that contains it.
 *
 * @author patrick
 */
public class MeiControlEvent {
    private final Element element;
    private final int measureIndex;
    private final Fraction beatUnit;


    /**
     * Constructor for MeiControlEvent.
     *
     * @param element the control event element
     * @param measureIndex the index of the measure
     * @param beatUnit the length of one beat (tstamp unit) in whole notes
     */
    public MeiControlEvent(Element element, int measureIndex, Fraction beatUnit) {
        this.element = element;
        this.measureIndex = measureIndex;
        this.beatUnit = beatUnit;
    }


    /**
     * Gets the control event element.
     *
     * @return the control event element
     */
    public Element getElement() {
        return element;
    }


    /**
     * Gets the index of the measure.
     *
     * @return the index of the measure
     */
    public int getMeasureIndex() {
        return measureIndex;
    }


    /**
     * Gets the length of one beat (tstamp unit) in whole notes.
     *
     * @return the length of one beat (tstamp unit) in whole notes
     */
    public Fraction getBeatUnit() {
        return beatUnit;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(element, measureIndex, beatUnit);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a MeiControlEvent with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MeiControlEvent other = (MeiControlEvent) obj;
        return Objects.equals(element, other.element)
                && Objects.equals(measureIndex, other.measureIndex)
                && Objects.equals(beatUnit, other.beatUnit);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "MeiControlEvent ["
                + "element=" + element
                + ", measureIndex=" + measureIndex
                + ", beatUnit=" + beatUnit
                + "]";
    }
}
