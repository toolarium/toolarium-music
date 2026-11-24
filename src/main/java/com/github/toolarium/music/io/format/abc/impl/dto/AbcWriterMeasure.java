/*
 * AbcWriterMeasure.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


/**
 * A measure to write: its length and meter, its barlines, repeats and endings, the events of every staff and
 * voice, the marks of every staff and the chord symbols of every part.
 *
 * @author patrick
 */
public class AbcWriterMeasure {
    private final Map<Integer, Map<Integer, List<AbcWriterEvent>>> staves = new TreeMap<>();
    private final Map<Integer, List<AbcWriterMark>> marks = new TreeMap<>();
    private final Map<Integer, List<AbcWriterMark>> harmonies = new TreeMap<>();
    private Fraction length = Fraction.ZERO;
    private int meterCount = 4;
    private int meterUnit = 4;
    private boolean meterChange;
    private String meterSymbol;
    private boolean repeatStart;
    private boolean repeatEnd;
    private int repeatTimes = 2;
    private String endingStart;
    private boolean endingStop;
    private String barStyle;


    /**
     * Constructor for AbcWriterMeasure.
     */
    public AbcWriterMeasure() {
        // empty measure
    }


    /**
     * Gets the events by staff number and voice.
     *
     * @return the events
     */
    public Map<Integer, Map<Integer, List<AbcWriterEvent>>> getStaves() {
        return staves;
    }


    /**
     * Gets the events of a voice, created if needed.
     *
     * @param staff the staff number
     * @param voice the voice
     * @return the events
     */
    public List<AbcWriterEvent> voice(int staff, int voice) {
        return staves.computeIfAbsent(staff, key -> new TreeMap<>()).computeIfAbsent(voice, key -> new ArrayList<>());
    }


    /**
     * Gets the marks by staff number.
     *
     * @return the marks
     */
    public Map<Integer, List<AbcWriterMark>> getMarks() {
        return marks;
    }


    /**
     * Adds a mark to a staff.
     *
     * @param staff the staff number
     * @param mark the mark
     */
    public void addMark(int staff, AbcWriterMark mark) {
        marks.computeIfAbsent(staff, key -> new ArrayList<>()).add(mark);
    }


    /**
     * Gets the chord symbols by part index (the text is the chord in chord mode without duration).
     *
     * @return the chord symbols
     */
    public Map<Integer, List<AbcWriterMark>> getHarmonies() {
        return harmonies;
    }


    /**
     * Gets the length of the measure.
     *
     * @return the length in whole notes
     */
    public Fraction getLength() {
        return length;
    }


    /**
     * Sets the length of the measure.
     *
     * @param length the length in whole notes
     */
    public void setLength(Fraction length) {
        this.length = length;
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
     * Gets the symbol of the time signature.
     *
     * @return common, cut or null for a numeric time signature
     */
    public String getMeterSymbol() {
        return meterSymbol;
    }


    /**
     * Sets the meter of the measure.
     *
     * @param count the beats
     * @param unit the beat unit
     * @param symbol common, cut or null
     * @param change true if the meter changes with this measure
     */
    public void setMeter(int count, int unit, String symbol, boolean change) {
        this.meterCount = count;
        this.meterUnit = unit;
        this.meterSymbol = symbol;
        this.meterChange = change;
    }


    /**
     * Checks whether the meter changes with this measure.
     *
     * @return true for a meter change
     */
    public boolean isMeterChange() {
        return meterChange;
    }


    /**
     * Checks whether a repeat starts with this measure.
     *
     * @return true for a forward repeat
     */
    public boolean isRepeatStart() {
        return repeatStart;
    }


    /**
     * Sets whether a repeat starts with this measure.
     *
     * @param repeatStart true for a forward repeat
     */
    public void setRepeatStart(boolean repeatStart) {
        this.repeatStart = repeatStart;
    }


    /**
     * Checks whether a repeat ends with this measure.
     *
     * @return true for a backward repeat
     */
    public boolean isRepeatEnd() {
        return repeatEnd;
    }


    /**
     * Gets how often the repeated music is played.
     *
     * @return the times
     */
    public int getRepeatTimes() {
        return repeatTimes;
    }


    /**
     * Sets the end of a repeat.
     *
     * @param repeatEnd true for a backward repeat
     * @param times how often the repeated music is played
     */
    public void setRepeatEnd(boolean repeatEnd, int times) {
        this.repeatEnd = repeatEnd;
        this.repeatTimes = times;
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
     * Gets the ABC bar line at the end of the measure.
     *
     * @return the bar line (for example |] or ||), or null for a regular barline
     */
    public String getBarStyle() {
        return barStyle;
    }


    /**
     * Sets the ABC bar line at the end of the measure.
     *
     * @param barStyle the bar line, or null for a regular barline
     */
    public void setBarStyle(String barStyle) {
        this.barStyle = barStyle;
    }


    /**
     * Calculates the hash code of all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(staves, marks, harmonies, length, meterCount, meterUnit, meterChange, meterSymbol, repeatStart, repeatEnd, repeatTimes,
                endingStart, endingStop, barStyle);
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
        AbcWriterMeasure other = (AbcWriterMeasure) obj;
        return Objects.equals(staves, other.staves)
                && Objects.equals(marks, other.marks)
                && Objects.equals(harmonies, other.harmonies)
                && Objects.equals(length, other.length)
                && meterCount == other.meterCount
                && meterUnit == other.meterUnit
                && meterChange == other.meterChange
                && Objects.equals(meterSymbol, other.meterSymbol)
                && repeatStart == other.repeatStart
                && repeatEnd == other.repeatEnd
                && repeatTimes == other.repeatTimes
                && Objects.equals(endingStart, other.endingStart)
                && endingStop == other.endingStop
                && Objects.equals(barStyle, other.barStyle);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "AbcWriterMeasure ["
                + "staves=" + staves.keySet()
                + ", marks=" + marks
                + ", harmonies=" + harmonies
                + ", length=" + length
                + ", meterCount=" + meterCount
                + ", meterUnit=" + meterUnit
                + ", meterChange=" + meterChange
                + ", meterSymbol=" + meterSymbol
                + ", repeatStart=" + repeatStart
                + ", repeatEnd=" + repeatEnd
                + ", repeatTimes=" + repeatTimes
                + ", endingStart=" + endingStart
                + ", endingStop=" + endingStop
                + ", barStyle=" + barStyle
                + "]";
    }
}
