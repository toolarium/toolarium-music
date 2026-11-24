/*
 * LilyPondScore.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * A parsed LilyPond score: the header fields, the parts (each with its staves), the pickup and the chord symbols.
 *
 * @author patrick
 */
public class LilyPondScore {
    private final Map<String, String> header = new LinkedHashMap<>();
    private final List<List<LilyPondStaff>> parts = new ArrayList<>();
    private final List<LilyPondMark> harmonies = new ArrayList<>();
    private Fraction partial;


    /**
     * Constructor for LilyPondScore.
     */
    public LilyPondScore() {
        // empty score
    }


    /**
     * Gets the header fields (title, composer, ...).
     *
     * @return the header fields by name
     */
    public Map<String, String> getHeader() {
        return header;
    }


    /**
     * Gets the parts: each part has one staff, or several staves (a piano staff).
     *
     * @return the parts
     */
    public List<List<LilyPondStaff>> getParts() {
        return parts;
    }


    /**
     * Gets the chord symbols (\chordmode) with their positions.
     *
     * @return the chord symbols
     */
    public List<LilyPondMark> getHarmonies() {
        return harmonies;
    }


    /**
     * Gets the length of the pickup measure (\partial).
     *
     * @return the length in whole notes, or null without pickup
     */
    public Fraction getPartial() {
        return partial;
    }


    /**
     * Sets the length of the pickup measure (\partial).
     *
     * @param partial the length in whole notes
     */
    public void setPartial(Fraction partial) {
        this.partial = partial;
    }


    /**
     * Returns a hash code over all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(header, parts, harmonies, partial);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a LilyPondScore with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LilyPondScore other = (LilyPondScore) obj;
        return Objects.equals(header, other.header) && Objects.equals(parts, other.parts) && Objects.equals(harmonies, other.harmonies)
                && Objects.equals(partial, other.partial);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "LilyPondScore ["
                + "header=" + header
                + ", parts=" + parts
                + ", harmonies=" + harmonies
                + ", partial=" + partial
                + "]";
    }
}
