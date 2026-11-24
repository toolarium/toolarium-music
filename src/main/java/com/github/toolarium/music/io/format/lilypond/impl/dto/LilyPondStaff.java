/*
 * LilyPondStaff.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


/**
 * A LilyPond staff: its voices with their events and its marks (clefs, keys, time signatures, barlines, ...).
 *
 * @author patrick
 */
public class LilyPondStaff {
    private final Map<Integer, List<LilyPondEvent>> voices = new TreeMap<>();
    private final Map<String, Integer> voiceNames = new HashMap<>();
    private final List<LilyPondMark> marks = new ArrayList<>();
    private String name;
    private String abbreviation;
    private int[] transpose;
    private Integer program;


    /**
     * Constructor for LilyPondStaff.
     */
    public LilyPondStaff() {
        // empty staff
    }


    /**
     * Gets the voices by number (1-based).
     *
     * @return the voices
     */
    public Map<Integer, List<LilyPondEvent>> getVoices() {
        return voices;
    }


    /**
     * Gets the events of a voice; the voice is created if it is new.
     *
     * @param voice the voice number (1-based)
     * @return the events of the voice
     */
    public List<LilyPondEvent> voice(int voice) {
        return voices.computeIfAbsent(voice, k -> new ArrayList<>());
    }


    /**
     * Gets the voice numbers by voice name (\new Voice = "name").
     *
     * @return the voice numbers by name
     */
    public Map<String, Integer> getVoiceNames() {
        return voiceNames;
    }


    /**
     * Gets the marks.
     *
     * @return the marks
     */
    public List<LilyPondMark> getMarks() {
        return marks;
    }


    /**
     * Gets the name (instrumentName).
     *
     * @return the name, or null
     */
    public String getName() {
        return name;
    }


    /**
     * Sets the name (instrumentName).
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Gets the abbreviation (shortInstrumentName).
     *
     * @return the abbreviation, or null
     */
    public String getAbbreviation() {
        return abbreviation;
    }


    /**
     * Sets the abbreviation (shortInstrumentName).
     *
     * @param abbreviation the abbreviation
     */
    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }


    /**
     * Gets the transposition (the transposition command): sounding minus written pitch.
     *
     * @return the diatonic steps and the semitones, or null without transposition
     */
    public int[] getTranspose() {
        return transpose;
    }


    /**
     * Sets the transposition: sounding minus written pitch.
     *
     * @param transpose the diatonic steps and the semitones, or null
     */
    public void setTranspose(int[] transpose) {
        this.transpose = transpose;
    }


    /**
     * Gets the General MIDI program (midiInstrument).
     *
     * @return the program (1 to 128), or null
     */
    public Integer getProgram() {
        return program;
    }


    /**
     * Sets the General MIDI program.
     *
     * @param program the program (1 to 128), or null
     */
    public void setProgram(Integer program) {
        this.program = program;
    }


    /**
     * Returns a hash code over all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(voices, voiceNames, marks, name, abbreviation, Arrays.hashCode(transpose), program);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a LilyPondStaff with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LilyPondStaff other = (LilyPondStaff) obj;
        return Objects.equals(voices, other.voices) && Objects.equals(voiceNames, other.voiceNames) && Objects.equals(marks, other.marks)
                && Objects.equals(name, other.name) && Objects.equals(abbreviation, other.abbreviation)
                && Arrays.equals(transpose, other.transpose) && Objects.equals(program, other.program);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "LilyPondStaff ["
                + "voices=" + voices
                + ", voiceNames=" + voiceNames
                + ", marks=" + marks
                + ", name=" + name
                + ", abbreviation=" + abbreviation
                + ", transpose=" + Arrays.toString(transpose)
                + ", program=" + program
                + "]";
    }
}
