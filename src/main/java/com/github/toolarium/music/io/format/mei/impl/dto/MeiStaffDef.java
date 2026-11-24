/*
 * MeiStaffDef.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl.dto;

import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.Objects;


/**
 * The definition of one MEI staff: its part, position in the part, clef, key, meter and transposition.
 *
 * @author patrick
 */
public class MeiStaffDef {
    private final String number;
    private int partIndex;
    private int staffInPart;
    private String label;
    private String abbreviation;
    private Integer lines;
    private ClefImpl clef;
    private Integer keyFifths;
    private String keyMode;
    private int transposeSemitones;
    private int transposeDiatonic;
    private Integer program;


    /**
     * Constructor for MeiStaffDef.
     *
     * @param number the staff number (@n)
     */
    public MeiStaffDef(String number) {
        this.number = number;
    }


    /**
     * Gets the staff number (@n).
     *
     * @return the staff number (@n)
     */
    public String getNumber() {
        return number;
    }


    /**
     * Gets the index of the part (0-based).
     *
     * @return the index of the part (0-based)
     */
    public int getPartIndex() {
        return partIndex;
    }


    /**
     * Sets the index of the part (0-based).
     *
     * @param partIndex the index of the part (0-based)
     */
    public void setPartIndex(int partIndex) {
        this.partIndex = partIndex;
    }


    /**
     * Gets the staff number within the part (1-based).
     *
     * @return the staff number within the part (1-based)
     */
    public int getStaffInPart() {
        return staffInPart;
    }


    /**
     * Sets the staff number within the part (1-based).
     *
     * @param staffInPart the staff number within the part (1-based)
     */
    public void setStaffInPart(int staffInPart) {
        this.staffInPart = staffInPart;
    }


    /**
     * Gets the staff label.
     *
     * @return the staff label
     */
    public String getLabel() {
        return label;
    }


    /**
     * Sets the staff label.
     *
     * @param label the staff label
     */
    public void setLabel(String label) {
        this.label = label;
    }


    /**
     * Gets the staff label abbreviation.
     *
     * @return the staff label abbreviation
     */
    public String getAbbreviation() {
        return abbreviation;
    }


    /**
     * Sets the staff label abbreviation.
     *
     * @param abbreviation the staff label abbreviation
     */
    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }


    /**
     * Gets the number of staff lines.
     *
     * @return the number of staff lines
     */
    public Integer getLines() {
        return lines;
    }


    /**
     * Sets the number of staff lines.
     *
     * @param lines the number of staff lines
     */
    public void setLines(Integer lines) {
        this.lines = lines;
    }


    /**
     * Gets the current clef.
     *
     * @return the current clef
     */
    public ClefImpl getClef() {
        return clef;
    }


    /**
     * Sets the current clef.
     *
     * @param clef the current clef
     */
    public void setClef(ClefImpl clef) {
        this.clef = clef;
    }


    /**
     * Gets the current key signature.
     *
     * @return the current key signature
     */
    public Integer getKeyFifths() {
        return keyFifths;
    }


    /**
     * Sets the current key signature.
     *
     * @param keyFifths the current key signature
     */
    public void setKeyFifths(Integer keyFifths) {
        this.keyFifths = keyFifths;
    }


    /**
     * Gets the current key mode.
     *
     * @return the current key mode
     */
    public String getKeyMode() {
        return keyMode;
    }


    /**
     * Sets the current key mode.
     *
     * @param keyMode the current key mode
     */
    public void setKeyMode(String keyMode) {
        this.keyMode = keyMode;
    }


    /**
     * Gets the transposition in semitones (sounding minus written).
     *
     * @return the transposition in semitones (sounding minus written)
     */
    public int getTransposeSemitones() {
        return transposeSemitones;
    }


    /**
     * Sets the transposition in semitones (sounding minus written).
     *
     * @param transposeSemitones the transposition in semitones (sounding minus written)
     */
    public void setTransposeSemitones(int transposeSemitones) {
        this.transposeSemitones = transposeSemitones;
    }


    /**
     * Gets the transposition in diatonic steps.
     *
     * @return the transposition in diatonic steps
     */
    public int getTransposeDiatonic() {
        return transposeDiatonic;
    }


    /**
     * Sets the transposition in diatonic steps.
     *
     * @param transposeDiatonic the transposition in diatonic steps
     */
    public void setTransposeDiatonic(int transposeDiatonic) {
        this.transposeDiatonic = transposeDiatonic;
    }


    /**
     * Gets the General MIDI program (1-based).
     *
     * @return the General MIDI program (1-based)
     */
    public Integer getProgram() {
        return program;
    }


    /**
     * Sets the General MIDI program (1-based).
     *
     * @param program the General MIDI program (1-based)
     */
    public void setProgram(Integer program) {
        this.program = program;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                number, partIndex, staffInPart, label, abbreviation, lines, clef, keyFifths, keyMode, transposeSemitones,
                transposeDiatonic, program);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a MeiStaffDef with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MeiStaffDef other = (MeiStaffDef) obj;
        return Objects.equals(number, other.number)
                && Objects.equals(partIndex, other.partIndex)
                && Objects.equals(staffInPart, other.staffInPart)
                && Objects.equals(label, other.label)
                && Objects.equals(abbreviation, other.abbreviation)
                && Objects.equals(lines, other.lines)
                && Objects.equals(clef, other.clef)
                && Objects.equals(keyFifths, other.keyFifths)
                && Objects.equals(keyMode, other.keyMode)
                && Objects.equals(transposeSemitones, other.transposeSemitones)
                && Objects.equals(transposeDiatonic, other.transposeDiatonic)
                && Objects.equals(program, other.program);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "MeiStaffDef ["
                + "number=" + number
                + ", partIndex=" + partIndex
                + ", staffInPart=" + staffInPart
                + ", label=" + label
                + ", abbreviation=" + abbreviation
                + ", lines=" + lines
                + ", clef=" + clef
                + ", keyFifths=" + keyFifths
                + ", keyMode=" + keyMode
                + ", transposeSemitones=" + transposeSemitones
                + ", transposeDiatonic=" + transposeDiatonic
                + ", program=" + program
                + "]";
    }
}
