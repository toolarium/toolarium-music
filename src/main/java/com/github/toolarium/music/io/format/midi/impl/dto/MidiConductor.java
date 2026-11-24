/*
 * MidiConductor.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.midi.impl.dto;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


/**
 * The events of the conductor track of a MIDI file, collected from all parts by tick: tempo changes, gradual
 * tempo changes (rit., accel.), time and key signatures and markers (rehearsal marks).
 *
 * @author patrick
 */
public class MidiConductor {
    private final Map<Long, Integer> tempos;
    private final Map<Long, Integer> gradualChanges;
    private final Map<Long, int[]> times;
    private final Map<Long, int[]> keys;
    private final Map<Long, String> markers;


    /**
     * Constructor for MidiConductor.
     */
    public MidiConductor() {
        this.tempos = new TreeMap<>();
        this.gradualChanges = new TreeMap<>();
        this.times = new TreeMap<>();
        this.keys = new TreeMap<>();
        this.markers = new TreeMap<>();
    }


    /**
     * Gets the tempo changes.
     *
     * @return the tempo in quarters per minute by tick
     */
    public Map<Long, Integer> getTempos() {
        return tempos;
    }


    /**
     * Gets the gradual tempo changes.
     *
     * @return -1 for slower (rit., rall.) or 1 for faster (accel.) by tick
     */
    public Map<Long, Integer> getGradualChanges() {
        return gradualChanges;
    }


    /**
     * Gets the time signatures.
     *
     * @return the beats and the beat type by tick
     */
    public Map<Long, int[]> getTimes() {
        return times;
    }


    /**
     * Gets the key signatures (in concert pitch).
     *
     * @return the fifths and 1 for minor (else 0) by tick
     */
    public Map<Long, int[]> getKeys() {
        return keys;
    }


    /**
     * Gets the markers.
     *
     * @return the marker texts by tick
     */
    public Map<Long, String> getMarkers() {
        return markers;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(tempos, gradualChanges, times.keySet(), keys.keySet(), markers);
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
        MidiConductor other = (MidiConductor) obj;
        return Objects.equals(tempos, other.tempos) && Objects.equals(gradualChanges, other.gradualChanges)
                && equalArrays(times, other.times) && equalArrays(keys, other.keys) && Objects.equals(markers, other.markers);
    }


    /**
     * Compares two maps with array values.
     *
     * @param a the first map
     * @param b the second map
     * @return true if both maps have the same keys and equal arrays
     */
    private boolean equalArrays(Map<Long, int[]> a, Map<Long, int[]> b) {
        if (!a.keySet().equals(b.keySet())) {
            return false;
        }
        for (Map.Entry<Long, int[]> entry : a.entrySet()) {
            if (!Arrays.equals(entry.getValue(), b.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "MidiConductor [tempos=" + tempos + ", gradualChanges=" + gradualChanges + ", times=" + times.size()
                + ", keys=" + keys.size() + ", markers=" + markers + "]";
    }
}
