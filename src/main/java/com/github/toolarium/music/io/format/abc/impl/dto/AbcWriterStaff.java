/*
 * AbcWriterStaff.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl.dto;

import com.github.toolarium.music.model.measure.IClef;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;


/**
 * A staff to write: its number over all parts, its part, the name of the part, the initial clef, the transposition,
 * the MIDI program and the voices used in the staff.
 *
 * @author patrick
 */
public class AbcWriterStaff {
    private final int number;
    private final int partIndex;
    private final int staffInPart;
    private final Set<Integer> voices = new TreeSet<>();
    private String label;
    private String abbreviation;
    private IClef clef;
    private Integer transposeDiatonic;
    private Integer transposeSemitones;
    private Integer program;


    /**
     * Constructor for AbcWriterStaff.
     *
     * @param number the staff number (1-based over all parts)
     * @param partIndex the index of the part
     * @param staffInPart the staff number in the part (1-based)
     */
    public AbcWriterStaff(int number, int partIndex, int staffInPart) {
        this.number = number;
        this.partIndex = partIndex;
        this.staffInPart = staffInPart;
    }


    /**
     * Gets the staff number.
     *
     * @return the staff number (1-based over all parts)
     */
    public int getNumber() {
        return number;
    }


    /**
     * Gets the index of the part.
     *
     * @return the part index
     */
    public int getPartIndex() {
        return partIndex;
    }


    /**
     * Gets the staff number in the part.
     *
     * @return the staff number (1-based)
     */
    public int getStaffInPart() {
        return staffInPart;
    }


    /**
     * Gets the voices used in the staff.
     *
     * @return the voice numbers in order
     */
    public Set<Integer> getVoices() {
        return voices;
    }


    /**
     * Gets the name of the part.
     *
     * @return the name, or null
     */
    public String getLabel() {
        return label;
    }


    /**
     * Sets the name of the part.
     *
     * @param label the name, or null
     */
    public void setLabel(String label) {
        this.label = label;
    }


    /**
     * Gets the abbreviated name of the part.
     *
     * @return the abbreviation, or null
     */
    public String getAbbreviation() {
        return abbreviation;
    }


    /**
     * Sets the abbreviated name of the part.
     *
     * @param abbreviation the abbreviation, or null
     */
    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }


    /**
     * Gets the initial clef.
     *
     * @return the clef, or null for a treble clef
     */
    public IClef getClef() {
        return clef;
    }


    /**
     * Sets the initial clef.
     *
     * @param clef the clef
     */
    public void setClef(IClef clef) {
        this.clef = clef;
    }


    /**
     * Gets the diatonic transposition (sounding minus written).
     *
     * @return the steps, or null without transposition
     */
    public Integer getTransposeDiatonic() {
        return transposeDiatonic;
    }


    /**
     * Gets the chromatic transposition (sounding minus written).
     *
     * @return the semitones, or null without transposition
     */
    public Integer getTransposeSemitones() {
        return transposeSemitones;
    }


    /**
     * Sets the transposition (sounding minus written).
     *
     * @param diatonic the steps
     * @param semitones the semitones
     */
    public void setTranspose(Integer diatonic, Integer semitones) {
        this.transposeDiatonic = diatonic;
        this.transposeSemitones = semitones;
    }


    /**
     * Gets the General MIDI program.
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
     * Calculates the hash code of all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(number, partIndex, staffInPart, voices, label, abbreviation, clef, transposeDiatonic, transposeSemitones, program);
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
        AbcWriterStaff other = (AbcWriterStaff) obj;
        return number == other.number
                && partIndex == other.partIndex
                && staffInPart == other.staffInPart
                && Objects.equals(voices, other.voices)
                && Objects.equals(label, other.label)
                && Objects.equals(abbreviation, other.abbreviation)
                && Objects.equals(clef, other.clef)
                && Objects.equals(transposeDiatonic, other.transposeDiatonic)
                && Objects.equals(transposeSemitones, other.transposeSemitones)
                && Objects.equals(program, other.program);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "AbcWriterStaff ["
                + "number=" + number
                + ", partIndex=" + partIndex
                + ", staffInPart=" + staffInPart
                + ", voices=" + voices
                + ", label=" + label
                + ", abbreviation=" + abbreviation
                + ", clef=" + clef
                + ", transposeDiatonic=" + transposeDiatonic
                + ", transposeSemitones=" + transposeSemitones
                + ", program=" + program
                + "]";
    }
}
