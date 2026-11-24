/*
 * GpBeat.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl.dto;

import com.github.toolarium.music.io.dto.Fraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * A Guitar Pro beat: duration, notes and beat effects.
 *
 * @author patrick
 */
public class GpBeat {
    private Fraction duration;
    private String type;
    private int dots;
    private int tupletActual;
    private int tupletNormal;
    private boolean grace;
    private boolean empty;
    private String text;
    private String chordName;
    private String dynamic;
    private GpTempo tempo;
    private boolean fermata;
    private String pickStroke;
    private boolean slap;
    private boolean pop;
    private boolean tap;
    private final List<String> lyrics = new ArrayList<>();
    private final List<GpNote> notes = new ArrayList<>();


    /**
     * Constructor for GpBeat.
     */
    public GpBeat() {
        // default constructor
    }


    /**
     * Gets the sounding duration in whole notes (tuplets included).
     *
     * @return the sounding duration in whole notes (tuplets included)
     */
    public Fraction getDuration() {
        return duration;
    }


    /**
     * Sets the sounding duration in whole notes (tuplets included).
     *
     * @param duration the sounding duration in whole notes (tuplets included)
     */
    public void setDuration(Fraction duration) {
        this.duration = duration;
    }


    /**
     * Gets the MusicXML note type.
     *
     * @return the MusicXML note type
     */
    public String getType() {
        return type;
    }


    /**
     * Sets the MusicXML note type.
     *
     * @param type the MusicXML note type
     */
    public void setType(String type) {
        this.type = type;
    }


    /**
     * Gets the number of dots.
     *
     * @return the number of dots
     */
    public int getDots() {
        return dots;
    }


    /**
     * Sets the number of dots.
     *
     * @param dots the number of dots
     */
    public void setDots(int dots) {
        this.dots = dots;
    }


    /**
     * Gets the actual notes of the tuplet (0 for none).
     *
     * @return the actual notes of the tuplet (0 for none)
     */
    public int getTupletActual() {
        return tupletActual;
    }


    /**
     * Sets the actual notes of the tuplet (0 for none).
     *
     * @param tupletActual the actual notes of the tuplet (0 for none)
     */
    public void setTupletActual(int tupletActual) {
        this.tupletActual = tupletActual;
    }


    /**
     * Gets the normal notes of the tuplet.
     *
     * @return the normal notes of the tuplet
     */
    public int getTupletNormal() {
        return tupletNormal;
    }


    /**
     * Sets the normal notes of the tuplet.
     *
     * @param tupletNormal the normal notes of the tuplet
     */
    public void setTupletNormal(int tupletNormal) {
        this.tupletNormal = tupletNormal;
    }


    /**
     * Gets the flag for a grace beat.
     *
     * @return the flag for a grace beat
     */
    public boolean isGrace() {
        return grace;
    }


    /**
     * Sets the flag for a grace beat.
     *
     * @param grace the flag for a grace beat
     */
    public void setGrace(boolean grace) {
        this.grace = grace;
    }


    /**
     * Gets the flag for an empty (invisible) beat.
     *
     * @return the flag for an empty (invisible) beat
     */
    public boolean isEmpty() {
        return empty;
    }


    /**
     * Sets the flag for an empty (invisible) beat.
     *
     * @param empty the flag for an empty (invisible) beat
     */
    public void setEmpty(boolean empty) {
        this.empty = empty;
    }


    /**
     * Gets the beat text.
     *
     * @return the beat text
     */
    public String getText() {
        return text;
    }


    /**
     * Sets the beat text.
     *
     * @param text the beat text
     */
    public void setText(String text) {
        this.text = text;
    }


    /**
     * Gets the chord name.
     *
     * @return the chord name
     */
    public String getChordName() {
        return chordName;
    }


    /**
     * Sets the chord name.
     *
     * @param chordName the chord name
     */
    public void setChordName(String chordName) {
        this.chordName = chordName;
    }


    /**
     * Gets the dynamics marking (ppp to fff).
     *
     * @return the dynamics marking (ppp to fff)
     */
    public String getDynamic() {
        return dynamic;
    }


    /**
     * Sets the dynamics marking (ppp to fff).
     *
     * @param dynamic the dynamics marking (ppp to fff)
     */
    public void setDynamic(String dynamic) {
        this.dynamic = dynamic;
    }


    /**
     * Gets the tempo change at this beat.
     *
     * @return the tempo change at this beat
     */
    public GpTempo getTempo() {
        return tempo;
    }


    /**
     * Sets the tempo change at this beat.
     *
     * @param tempo the tempo change at this beat
     */
    public void setTempo(GpTempo tempo) {
        this.tempo = tempo;
    }


    /**
     * Gets the flag for a fermata.
     *
     * @return the flag for a fermata
     */
    public boolean isFermata() {
        return fermata;
    }


    /**
     * Sets the flag for a fermata.
     *
     * @param fermata the flag for a fermata
     */
    public void setFermata(boolean fermata) {
        this.fermata = fermata;
    }


    /**
     * Gets the pick stroke (up or down).
     *
     * @return the pick stroke (up or down)
     */
    public String getPickStroke() {
        return pickStroke;
    }


    /**
     * Sets the pick stroke (up or down).
     *
     * @param pickStroke the pick stroke (up or down)
     */
    public void setPickStroke(String pickStroke) {
        this.pickStroke = pickStroke;
    }


    /**
     * Gets the flag for slapping.
     *
     * @return the flag for slapping
     */
    public boolean isSlap() {
        return slap;
    }


    /**
     * Sets the flag for slapping.
     *
     * @param slap the flag for slapping
     */
    public void setSlap(boolean slap) {
        this.slap = slap;
    }


    /**
     * Gets the flag for popping.
     *
     * @return the flag for popping
     */
    public boolean isPop() {
        return pop;
    }


    /**
     * Sets the flag for popping.
     *
     * @param pop the flag for popping
     */
    public void setPop(boolean pop) {
        this.pop = pop;
    }


    /**
     * Gets the flag for tapping.
     *
     * @return the flag for tapping
     */
    public boolean isTap() {
        return tap;
    }


    /**
     * Sets the flag for tapping.
     *
     * @param tap the flag for tapping
     */
    public void setTap(boolean tap) {
        this.tap = tap;
    }


    /**
     * Gets the lyrics of each line (mutable).
     *
     * @return the lyrics of each line (mutable)
     */
    public List<String> getLyrics() {
        return lyrics;
    }


    /**
     * Gets the notes (mutable).
     *
     * @return the notes (mutable)
     */
    public List<GpNote> getNotes() {
        return notes;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                duration, type, dots, tupletActual, tupletNormal, grace, empty, text, chordName, dynamic, tempo, fermata,
                pickStroke, slap, pop, tap, lyrics, notes);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a GpBeat with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GpBeat other = (GpBeat) obj;
        return Objects.equals(duration, other.duration)
                && Objects.equals(type, other.type)
                && Objects.equals(dots, other.dots)
                && Objects.equals(tupletActual, other.tupletActual)
                && Objects.equals(tupletNormal, other.tupletNormal)
                && Objects.equals(grace, other.grace)
                && Objects.equals(empty, other.empty)
                && Objects.equals(text, other.text)
                && Objects.equals(chordName, other.chordName)
                && Objects.equals(dynamic, other.dynamic)
                && Objects.equals(tempo, other.tempo)
                && Objects.equals(fermata, other.fermata)
                && Objects.equals(pickStroke, other.pickStroke)
                && Objects.equals(slap, other.slap)
                && Objects.equals(pop, other.pop)
                && Objects.equals(tap, other.tap)
                && Objects.equals(lyrics, other.lyrics)
                && Objects.equals(notes, other.notes);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "GpBeat ["
                + "duration=" + duration
                + ", type=" + type
                + ", dots=" + dots
                + ", tupletActual=" + tupletActual
                + ", tupletNormal=" + tupletNormal
                + ", grace=" + grace
                + ", empty=" + empty
                + ", text=" + text
                + ", chordName=" + chordName
                + ", dynamic=" + dynamic
                + ", tempo=" + tempo
                + ", fermata=" + fermata
                + ", pickStroke=" + pickStroke
                + ", slap=" + slap
                + ", pop=" + pop
                + ", tap=" + tap
                + ", lyrics=" + lyrics
                + ", notes=" + notes
                + "]";
    }
}
