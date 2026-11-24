/*
 * MeiWriterMeasure.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl.dto;

import com.github.toolarium.music.model.measure.IClef;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


/**
 * A measure to write: its number and barlines, the key and meter changes before it, the events of every staff and
 * voice, the clef changes at its end and its control events.
 *
 * @author patrick
 */
public class MeiWriterMeasure {
    private final String number;
    private final Map<Integer, Map<Integer, List<MeiWriterEvent>>> staves = new TreeMap<>();
    private final Map<Integer, List<IClef>> clefsAtEnd = new TreeMap<>();
    private final List<MeiWriterControl> controls = new ArrayList<>();
    private boolean implicit;
    private int meterCount;
    private int meterUnit;
    private boolean meterChange;
    private final Map<Integer, String> keySigs = new TreeMap<>();
    private final Map<Integer, String> keyModes = new TreeMap<>();
    private String left;
    private String right;
    private String endingStart;
    private boolean endingStop;


    /**
     * Constructor for MeiWriterMeasure.
     *
     * @param number the measure number
     */
    public MeiWriterMeasure(String number) {
        this.number = number;
    }


    /**
     * Gets the measure number.
     *
     * @return the number
     */
    public String getNumber() {
        return number;
    }


    /**
     * Gets the events by MEI staff number and voice.
     *
     * @return the events
     */
    public Map<Integer, Map<Integer, List<MeiWriterEvent>>> getStaves() {
        return staves;
    }


    /**
     * Gets the events of a voice, created if needed.
     *
     * @param staff the MEI staff number
     * @param voice the voice
     * @return the events
     */
    public List<MeiWriterEvent> voice(int staff, int voice) {
        return staves.computeIfAbsent(staff, key -> new TreeMap<>()).computeIfAbsent(voice, key -> new ArrayList<>());
    }


    /**
     * Gets the clef changes at the end of the measure by MEI staff number.
     *
     * @return the clefs
     */
    public Map<Integer, List<IClef>> getClefsAtEnd() {
        return clefsAtEnd;
    }


    /**
     * Gets the control events.
     *
     * @return the control events
     */
    public List<MeiWriterControl> getControls() {
        return controls;
    }


    /**
     * Checks whether the measure is incomplete (a pickup).
     *
     * @return true for an incomplete measure
     */
    public boolean isImplicit() {
        return implicit;
    }


    /**
     * Sets whether the measure is incomplete (a pickup).
     *
     * @param implicit true for an incomplete measure
     */
    public void setImplicit(boolean implicit) {
        this.implicit = implicit;
    }


    /**
     * Gets the number of beats.
     *
     * @return the beats
     */
    public int getMeterCount() {
        return meterCount;
    }


    /**
     * Gets the beat unit.
     *
     * @return the beat unit (4 for a quarter)
     */
    public int getMeterUnit() {
        return meterUnit;
    }


    /**
     * Sets the meter of the measure.
     *
     * @param count the beats
     * @param unit the beat unit
     * @param change true if the meter changes before this measure
     */
    public void setMeter(int count, int unit, boolean change) {
        this.meterCount = count;
        this.meterUnit = unit;
        this.meterChange = change;
    }


    /**
     * Checks whether the meter changes before this measure.
     *
     * @return true for a meter change
     */
    public boolean isMeterChange() {
        return meterChange;
    }


    /**
     * Gets the key signatures that start before this measure (in the first measure: the initial ones).
     *
     * @return the MEI key signature (for example "2s", "3f" or "0") by MEI staff number
     */
    public Map<Integer, String> getKeySigs() {
        return keySigs;
    }


    /**
     * Gets the modes of the keys that start before this measure.
     *
     * @return the mode by MEI staff number
     */
    public Map<Integer, String> getKeyModes() {
        return keyModes;
    }


    /**
     * Gets the left barline.
     *
     * @return the MEI bar rendition, or null
     */
    public String getLeft() {
        return left;
    }


    /**
     * Sets the left barline.
     *
     * @param left the MEI bar rendition, or null
     */
    public void setLeft(String left) {
        this.left = left;
    }


    /**
     * Gets the right barline.
     *
     * @return the MEI bar rendition, or null
     */
    public String getRight() {
        return right;
    }


    /**
     * Sets the right barline.
     *
     * @param right the MEI bar rendition, or null
     */
    public void setRight(String right) {
        this.right = right;
    }


    /**
     * Gets the number of the ending that starts with this measure.
     *
     * @return the ending number, or null
     */
    public String getEndingStart() {
        return endingStart;
    }


    /**
     * Sets the number of the ending that starts with this measure.
     *
     * @param endingStart the ending number, or null
     */
    public void setEndingStart(String endingStart) {
        this.endingStart = endingStart;
    }


    /**
     * Checks whether an ending ends with this measure.
     *
     * @return true if an ending ends here
     */
    public boolean isEndingStop() {
        return endingStop;
    }


    /**
     * Sets whether an ending ends with this measure.
     *
     * @param endingStop true if an ending ends here
     */
    public void setEndingStop(boolean endingStop) {
        this.endingStop = endingStop;
    }


    /**
     * Calculates the hash code of all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(number, staves, clefsAtEnd, controls, implicit, meterCount, meterUnit, meterChange, keySigs, keyModes, left, right,
                endingStart, endingStop);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if all fields are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MeiWriterMeasure other = (MeiWriterMeasure) obj;
        return Objects.equals(number, other.number)
                && Objects.equals(staves, other.staves)
                && Objects.equals(clefsAtEnd, other.clefsAtEnd)
                && Objects.equals(controls, other.controls)
                && implicit == other.implicit
                && meterCount == other.meterCount
                && meterUnit == other.meterUnit
                && meterChange == other.meterChange
                && Objects.equals(keySigs, other.keySigs)
                && Objects.equals(keyModes, other.keyModes)
                && Objects.equals(left, other.left)
                && Objects.equals(right, other.right)
                && Objects.equals(endingStart, other.endingStart)
                && endingStop == other.endingStop;
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "MeiWriterMeasure ["
                + "number=" + number
                + ", staves=" + staves.keySet()
                + ", clefsAtEnd=" + clefsAtEnd
                + ", controls=" + controls.size()
                + ", implicit=" + implicit
                + ", meterCount=" + meterCount
                + ", meterUnit=" + meterUnit
                + ", meterChange=" + meterChange
                + ", keySigs=" + keySigs
                + ", keyModes=" + keyModes
                + ", left=" + left
                + ", right=" + right
                + ", endingStart=" + endingStart
                + ", endingStop=" + endingStop
                + "]";
    }
}
