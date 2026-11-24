/*
 * MeiWriterPlan.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * The content of a score prepared for writing as MEI: the staff definitions of all parts and the measures with the
 * events of all staves. The first measure holds the initial key and meter.
 *
 * @author patrick
 */
public class MeiWriterPlan {
    private final List<MeiWriterStaff> staves = new ArrayList<>();
    private final List<MeiWriterMeasure> measures = new ArrayList<>();


    /**
     * Constructor for MeiWriterPlan.
     */
    public MeiWriterPlan() {
        // empty plan
    }


    /**
     * Gets the staff definitions in score order.
     *
     * @return the staff definitions
     */
    public List<MeiWriterStaff> getStaves() {
        return staves;
    }


    /**
     * Gets the measures.
     *
     * @return the measures
     */
    public List<MeiWriterMeasure> getMeasures() {
        return measures;
    }


    /**
     * Calculates the hash code of all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(staves, measures);
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
        MeiWriterPlan other = (MeiWriterPlan) obj;
        return Objects.equals(staves, other.staves) && Objects.equals(measures, other.measures);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "MeiWriterPlan [staves=" + staves + ", measures=" + measures.size() + "]";
    }
}
