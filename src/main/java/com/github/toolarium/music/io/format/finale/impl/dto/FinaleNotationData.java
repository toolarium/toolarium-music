/*
 * FinaleNotationData.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * The notation of a Finale score that belongs to its entries and measures: tuplets, octave lines, hairpins,
 * note alterations and the marks of the entries.
 *
 * @author patrick
 */
public class FinaleNotationData {
    private final Map<Integer, FinaleTuplet> tuplets;
    private final List<FinaleOctaveShift> octaveShifts;
    private final List<FinaleHairpin> hairpins;
    private final Set<String> noteAlterations;
    private final Map<Integer, List<String>> entryMarks;
    private final Map<String, Integer> openSlurs;
    private Map<Integer, String> breaks;


    /**
     * Constructor for FinaleNotationData.
     *
     * @param tuplets the tuplet definitions by first entry
     * @param octaveShifts the octave lines
     * @param hairpins the crescendo and decrescendo hairpins
     * @param noteAlterations the altered notes (enharmonic respelling, reduced size)
     * @param entryMarks the articulations, fermatas, ornaments and slurs by entry number
     */
    public FinaleNotationData(Map<Integer, FinaleTuplet> tuplets, List<FinaleOctaveShift> octaveShifts, List<FinaleHairpin> hairpins,
                              Set<String> noteAlterations, Map<Integer, List<String>> entryMarks) {
        this.tuplets = tuplets;
        this.octaveShifts = octaveShifts;
        this.hairpins = hairpins;
        this.noteAlterations = noteAlterations;
        this.entryMarks = entryMarks;
        this.openSlurs = new HashMap<>();
    }


    /**
     * Starts a slur: it gets the lowest slur number that no open slur uses.
     *
     * @param id the ID of the slur
     * @return the slur number
     */
    public int startSlur(String id) {
        int number = 1;
        while (openSlurs.containsValue(number)) {
            number++;
        }
        openSlurs.put(id, number);
        return number;
    }


    /**
     * Ends a slur and frees its number.
     *
     * @param id the ID of the slur
     * @return the slur number, or null if the slur was not started
     */
    public Integer stopSlur(String id) {
        return openSlurs.remove(id);
    }


    /**
     * Gets the page and system breaks.
     *
     * @return page or system by the first measure of a system
     */
    public Map<Integer, String> getBreaks() {
        return breaks;
    }


    /**
     * Sets the page and system breaks.
     *
     * @param breaks page or system by the first measure of a system
     */
    public void setBreaks(Map<Integer, String> breaks) {
        this.breaks = breaks;
    }


    /**
     * Gets the tuplet definitions.
     *
     * @return the tuplet definitions by first entry
     */
    public Map<Integer, FinaleTuplet> getTuplets() {
        return tuplets;
    }


    /**
     * Gets the octave lines.
     *
     * @return the octave lines
     */
    public List<FinaleOctaveShift> getOctaveShifts() {
        return octaveShifts;
    }


    /**
     * Gets the hairpins.
     *
     * @return the crescendo and decrescendo hairpins
     */
    public List<FinaleHairpin> getHairpins() {
        return hairpins;
    }


    /**
     * Gets the altered notes.
     *
     * @return the altered notes (enharmonic respelling, reduced size)
     */
    public Set<String> getNoteAlterations() {
        return noteAlterations;
    }


    /**
     * Gets the marks of the entries.
     *
     * @return the articulations, fermatas, ornaments and slurs by entry number
     */
    public Map<Integer, List<String>> getEntryMarks() {
        return entryMarks;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(tuplets, octaveShifts, hairpins, noteAlterations, entryMarks, breaks);
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
        FinaleNotationData other = (FinaleNotationData) obj;
        return Objects.equals(tuplets, other.tuplets) && Objects.equals(octaveShifts, other.octaveShifts) && Objects.equals(hairpins, other.hairpins)
                && Objects.equals(noteAlterations, other.noteAlterations) && Objects.equals(entryMarks, other.entryMarks)
                && Objects.equals(breaks, other.breaks);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FinaleNotationData [tuplets=" + tuplets + ", octaveShifts=" + octaveShifts + ", hairpins=" + hairpins
                + ", noteAlterations=" + noteAlterations + ", entryMarks=" + entryMarks + ", breaks=" + breaks + "]";
    }
}
