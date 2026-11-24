/*
 * FinaleExpression.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl.dto;

import java.util.Objects;


/**
 * A Finale expression assigned to a measure: a dynamic, a tempo mark (metronome), a rehearsal mark or a text.
 *
 * @author patrick
 */
public class FinaleExpression {
    private final int measure;
    private final int staff;
    private final int eduOffset;
    private final String dynamic;
    private final int beatsPerMinute;
    private final int beatUnitEdu;
    private final String text;
    private final boolean rehearsal;
    private final int soundTempo;


    /**
     * Constructor for FinaleExpression.
     *
     * @param measure the measure
     * @param staff the staff, or -1 for the score (the top staff)
     * @param eduOffset the position within the measure in EDU
     * @param dynamic the dynamic (for example mf), or null
     * @param beatsPerMinute the metronome value, or 0
     * @param beatUnitEdu the beat unit of the metronome value in EDU
     * @param text the text, or null
     */
    public FinaleExpression(int measure, int staff, int eduOffset, String dynamic, int beatsPerMinute, int beatUnitEdu, String text) {
        this.measure = measure;
        this.staff = staff;
        this.eduOffset = eduOffset;
        this.dynamic = dynamic;
        this.beatsPerMinute = beatsPerMinute;
        this.beatUnitEdu = beatUnitEdu;
        this.text = text;
        this.rehearsal = false;
        this.soundTempo = 0;
    }


    /**
     * Constructor for a text or a rehearsal mark.
     *
     * @param measure the measure
     * @param staff the staff, or -1 for the score (the top staff)
     * @param eduOffset the position within the measure in EDU
     * @param text the text
     * @param rehearsal true for a rehearsal mark
     * @param soundTempo the tempo of a text which changes the tempo (a tempo) in quarters per minute, or 0
     */
    public FinaleExpression(int measure, int staff, int eduOffset, String text, boolean rehearsal, int soundTempo) {
        this.measure = measure;
        this.staff = staff;
        this.eduOffset = eduOffset;
        this.dynamic = null;
        this.beatsPerMinute = 0;
        this.beatUnitEdu = 0;
        this.text = text;
        this.rehearsal = rehearsal;
        this.soundTempo = soundTempo;
    }


    /**
     * Gets the measure.
     *
     * @return the measure
     */
    public int getMeasure() {
        return measure;
    }


    /**
     * Gets the staff.
     *
     * @return the staff, or -1 for the score (the top staff)
     */
    public int getStaff() {
        return staff;
    }


    /**
     * Gets the position within the measure.
     *
     * @return the position in EDU
     */
    public int getEduOffset() {
        return eduOffset;
    }


    /**
     * Gets the dynamic.
     *
     * @return the dynamic, or null
     */
    public String getDynamic() {
        return dynamic;
    }


    /**
     * Gets the metronome value.
     *
     * @return the beats per minute, or 0
     */
    public int getBeatsPerMinute() {
        return beatsPerMinute;
    }


    /**
     * Gets the beat unit of the metronome value.
     *
     * @return the beat unit in EDU
     */
    public int getBeatUnitEdu() {
        return beatUnitEdu;
    }


    /**
     * Gets the text.
     *
     * @return the text, or null
     */
    public String getText() {
        return text;
    }


    /**
     * Tells whether the expression is a rehearsal mark.
     *
     * @return true for a rehearsal mark
     */
    public boolean isRehearsal() {
        return rehearsal;
    }


    /**
     * Gets the tempo of a text which changes the tempo (a tempo).
     *
     * @return the tempo in quarters per minute, or 0
     */
    public int getSoundTempo() {
        return soundTempo;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(measure, staff, eduOffset, dynamic, beatsPerMinute, beatUnitEdu, text, rehearsal, soundTempo);
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
        FinaleExpression other = (FinaleExpression) obj;
        return measure == other.measure && staff == other.staff && eduOffset == other.eduOffset && beatsPerMinute == other.beatsPerMinute
                && beatUnitEdu == other.beatUnitEdu && Objects.equals(dynamic, other.dynamic) && Objects.equals(text, other.text)
                && rehearsal == other.rehearsal && soundTempo == other.soundTempo;
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FinaleExpression [measure=" + measure + ", staff=" + staff + ", eduOffset=" + eduOffset + ", dynamic=" + dynamic
                + ", beatsPerMinute=" + beatsPerMinute + ", beatUnitEdu=" + beatUnitEdu + ", text=" + text + ", rehearsal=" + rehearsal + ", soundTempo=" + soundTempo + "]";
    }
}
