/*
 * CapellaVoiceState.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl.dto;

import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * The state while reading the note objects of one capella voice in one system.
 *
 * @author patrick
 */
public class CapellaVoiceState {
    private int measureIndex;
    private Fraction cursor = Fraction.ZERO;
    private final List<CapellaSpan> spans = new ArrayList<>();
    private final Map<Integer, Boolean> hyphens = new HashMap<>();
    private Integer pendingFifths;
    private boolean pendingTime;
    private boolean repeatStartPending;
    private int tupletCount;
    private int tupletNormal;
    private Fraction tupletRemaining;
    private EventData tupletLast;


    /**
     * Constructor for CapellaVoiceState.
     */
    public CapellaVoiceState() {
        // default constructor
    }


    /**
     * Gets the index of the current measure.
     *
     * @return the index of the current measure
     */
    public int getMeasureIndex() {
        return measureIndex;
    }


    /**
     * Sets the index of the current measure.
     *
     * @param measureIndex the index of the current measure
     */
    public void setMeasureIndex(int measureIndex) {
        this.measureIndex = measureIndex;
    }


    /**
     * Gets the position in the current measure.
     *
     * @return the position in the current measure
     */
    public Fraction getCursor() {
        return cursor;
    }


    /**
     * Sets the position in the current measure.
     *
     * @param cursor the position in the current measure
     */
    public void setCursor(Fraction cursor) {
        this.cursor = cursor;
    }


    /**
     * Gets the open slurs and wedges (mutable).
     *
     * @return the open slurs and wedges (mutable)
     */
    public List<CapellaSpan> getSpans() {
        return spans;
    }


    /**
     * Gets the hyphen state of the last syllable by verse (mutable).
     *
     * @return the hyphen state of the last syllable by verse (mutable)
     */
    public Map<Integer, Boolean> getHyphens() {
        return hyphens;
    }


    /**
     * Gets the key change for the next measure.
     *
     * @return the key change for the next measure
     */
    public Integer getPendingFifths() {
        return pendingFifths;
    }


    /**
     * Sets the key change for the next measure.
     *
     * @param pendingFifths the key change for the next measure
     */
    public void setPendingFifths(Integer pendingFifths) {
        this.pendingFifths = pendingFifths;
    }


    /**
     * Gets the flag for a time change for the next measure.
     *
     * @return the flag for a time change for the next measure
     */
    public boolean isPendingTime() {
        return pendingTime;
    }


    /**
     * Sets the flag for a time change for the next measure.
     *
     * @param pendingTime the flag for a time change for the next measure
     */
    public void setPendingTime(boolean pendingTime) {
        this.pendingTime = pendingTime;
    }


    /**
     * Gets the flag for a repeat that starts at the next measure.
     *
     * @return the flag for a repeat that starts at the next measure
     */
    public boolean isRepeatStartPending() {
        return repeatStartPending;
    }


    /**
     * Sets the flag for a repeat that starts at the next measure.
     *
     * @param repeatStartPending the flag for a repeat that starts at the next measure
     */
    public void setRepeatStartPending(boolean repeatStartPending) {
        this.repeatStartPending = repeatStartPending;
    }


    /**
     * Gets the actual notes of the open tuplet group.
     *
     * @return the actual notes of the open tuplet group
     */
    public int getTupletCount() {
        return tupletCount;
    }


    /**
     * Sets the actual notes of the open tuplet group.
     *
     * @param tupletCount the actual notes of the open tuplet group
     */
    public void setTupletCount(int tupletCount) {
        this.tupletCount = tupletCount;
    }


    /**
     * Gets the normal notes of the open tuplet group.
     *
     * @return the normal notes of the open tuplet group
     */
    public int getTupletNormal() {
        return tupletNormal;
    }


    /**
     * Sets the normal notes of the open tuplet group.
     *
     * @param tupletNormal the normal notes of the open tuplet group
     */
    public void setTupletNormal(int tupletNormal) {
        this.tupletNormal = tupletNormal;
    }


    /**
     * Gets the remaining length of the open tuplet group (null if none is open).
     *
     * @return the remaining length of the open tuplet group (null if none is open)
     */
    public Fraction getTupletRemaining() {
        return tupletRemaining;
    }


    /**
     * Sets the remaining length of the open tuplet group (null if none is open).
     *
     * @param tupletRemaining the remaining length of the open tuplet group (null if none is open)
     */
    public void setTupletRemaining(Fraction tupletRemaining) {
        this.tupletRemaining = tupletRemaining;
    }


    /**
     * Gets the last event of the open tuplet group.
     *
     * @return the last event of the open tuplet group
     */
    public EventData getTupletLast() {
        return tupletLast;
    }


    /**
     * Sets the last event of the open tuplet group.
     *
     * @param tupletLast the last event of the open tuplet group
     */
    public void setTupletLast(EventData tupletLast) {
        this.tupletLast = tupletLast;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                measureIndex, cursor, spans, hyphens, pendingFifths, pendingTime, repeatStartPending, tupletCount, tupletNormal,
                tupletRemaining, tupletLast);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a CapellaVoiceState with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CapellaVoiceState other = (CapellaVoiceState) obj;
        return Objects.equals(measureIndex, other.measureIndex)
                && Objects.equals(cursor, other.cursor)
                && Objects.equals(spans, other.spans)
                && Objects.equals(hyphens, other.hyphens)
                && Objects.equals(pendingFifths, other.pendingFifths)
                && Objects.equals(pendingTime, other.pendingTime)
                && Objects.equals(repeatStartPending, other.repeatStartPending)
                && Objects.equals(tupletCount, other.tupletCount)
                && Objects.equals(tupletNormal, other.tupletNormal)
                && Objects.equals(tupletRemaining, other.tupletRemaining)
                && Objects.equals(tupletLast, other.tupletLast);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "CapellaVoiceState ["
                + "measureIndex=" + measureIndex
                + ", cursor=" + cursor
                + ", spans=" + spans
                + ", hyphens=" + hyphens
                + ", pendingFifths=" + pendingFifths
                + ", pendingTime=" + pendingTime
                + ", repeatStartPending=" + repeatStartPending
                + ", tupletCount=" + tupletCount
                + ", tupletNormal=" + tupletNormal
                + ", tupletRemaining=" + tupletRemaining
                + ", tupletLast=" + tupletLast
                + "]";
    }
}
