/*
 * AbcWriterPlan.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;


/**
 * The content of a score prepared for writing as ABC: the staves of all parts and the measures with the
 * events, marks and chord symbols.
 *
 * @author patrick
 */
public class AbcWriterPlan {
    private final List<AbcWriterStaff> staves = new ArrayList<>();
    private final List<AbcWriterMeasure> measures = new ArrayList<>();
    private final Set<Integer> partsWithHarmonies = new TreeSet<>();


    /**
     * Constructor for AbcWriterPlan.
     */
    public AbcWriterPlan() {
        // empty plan
    }


    /**
     * Gets the staves in score order.
     *
     * @return the staves
     */
    public List<AbcWriterStaff> getStaves() {
        return staves;
    }


    /**
     * Gets the measures.
     *
     * @return the measures
     */
    public List<AbcWriterMeasure> getMeasures() {
        return measures;
    }


    /**
     * Gets the parts that have chord symbols.
     *
     * @return the part indexes
     */
    public Set<Integer> getPartsWithHarmonies() {
        return partsWithHarmonies;
    }


    /**
     * Calculates the hash code of all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(staves, measures, partsWithHarmonies);
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
        AbcWriterPlan other = (AbcWriterPlan) obj;
        return Objects.equals(staves, other.staves) && Objects.equals(measures, other.measures)
                && Objects.equals(partsWithHarmonies, other.partsWithHarmonies);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "AbcWriterPlan [staves=" + staves + ", measures=" + measures.size() + ", partsWithHarmonies=" + partsWithHarmonies + "]";
    }
}
