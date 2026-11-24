/*
 * MeiLayerContext.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl.dto;

import com.github.toolarium.music.io.dto.MeasureData;
import com.github.toolarium.music.io.dto.VoiceData;
import java.util.Map;
import java.util.Objects;


/**
 * The state while reading one MEI layer: target voice, staff, accidentals and the last duration.
 *
 * @author patrick
 */
public class MeiLayerContext {
    private final MeasureData measure;
    private final VoiceData voice;
    private final MeiStaffDef staffDef;
    private final int measureIndex;
    private final Map<String, Integer> accidentals;
    private String lastDur;
    private int multiRest;


    /**
     * Constructor for MeiLayerContext.
     *
     * @param measure the measure of the part
     * @param voice the voice that receives the events
     * @param staffDef the staff definition
     * @param measureIndex the index of the measure
     * @param accidentals the accidentals of the measure by pitch (mutable)
     */
    public MeiLayerContext(MeasureData measure, VoiceData voice, MeiStaffDef staffDef, int measureIndex, Map<String, Integer> accidentals) {
        this.measure = measure;
        this.voice = voice;
        this.staffDef = staffDef;
        this.measureIndex = measureIndex;
        this.accidentals = accidentals;
    }


    /**
     * Gets the measure of the part.
     *
     * @return the measure of the part
     */
    public MeasureData getMeasure() {
        return measure;
    }


    /**
     * Gets the voice that receives the events.
     *
     * @return the voice that receives the events
     */
    public VoiceData getVoice() {
        return voice;
    }


    /**
     * Gets the staff definition.
     *
     * @return the staff definition
     */
    public MeiStaffDef getStaffDef() {
        return staffDef;
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
     * Gets the accidentals of the measure by pitch (mutable).
     *
     * @return the accidentals of the measure by pitch (mutable)
     */
    public Map<String, Integer> getAccidentals() {
        return accidentals;
    }


    /**
     * Gets the last duration (used by notes without a duration).
     *
     * @return the last duration (used by notes without a duration)
     */
    public String getLastDur() {
        return lastDur;
    }


    /**
     * Sets the last duration (used by notes without a duration).
     *
     * @param lastDur the last duration (used by notes without a duration)
     */
    public void setLastDur(String lastDur) {
        this.lastDur = lastDur;
    }


    /**
     * Gets the number of measures of a multi-measure rest.
     *
     * @return the number of measures of a multi-measure rest
     */
    public int getMultiRest() {
        return multiRest;
    }


    /**
     * Sets the number of measures of a multi-measure rest.
     *
     * @param multiRest the number of measures of a multi-measure rest
     */
    public void setMultiRest(int multiRest) {
        this.multiRest = multiRest;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(measure, voice, staffDef, measureIndex, accidentals, lastDur, multiRest);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a MeiLayerContext with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MeiLayerContext other = (MeiLayerContext) obj;
        return Objects.equals(measure, other.measure)
                && Objects.equals(voice, other.voice)
                && Objects.equals(staffDef, other.staffDef)
                && Objects.equals(measureIndex, other.measureIndex)
                && Objects.equals(accidentals, other.accidentals)
                && Objects.equals(lastDur, other.lastDur)
                && Objects.equals(multiRest, other.multiRest);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "MeiLayerContext ["
                + "measure=" + measure
                + ", voice=" + voice
                + ", staffDef=" + staffDef
                + ", measureIndex=" + measureIndex
                + ", accidentals=" + accidentals
                + ", lastDur=" + lastDur
                + ", multiRest=" + multiRest
                + "]";
    }
}
