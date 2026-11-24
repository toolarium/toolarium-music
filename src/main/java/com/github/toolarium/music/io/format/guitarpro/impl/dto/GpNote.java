/*
 * GpNote.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl.dto;

import com.github.toolarium.music.io.dto.SpelledPitch;
import java.util.Objects;


/**
 * A Guitar Pro note: string, fret or pitch and note effects.
 *
 * @author patrick
 */
public class GpNote {
    private int string = -1;
    private int fret = -1;
    private Integer midi;
    private SpelledPitch writtenPitch;
    private SpelledPitch concertPitch;
    private Integer drumArticulation;
    private boolean tieOrigin;
    private boolean tieDestination;
    private boolean dead;
    private boolean ghost;
    private int accent;
    private boolean staccato;
    private boolean hammerOrigin;
    private boolean slideOrigin;
    private boolean harmonic;
    private boolean trill;
    private boolean vibrato;
    private boolean letRing;
    private boolean palmMute;
    private boolean bend;
    private Integer leftFinger;


    /**
     * Constructor for GpNote.
     */
    public GpNote() {
        // default constructor
    }


    /**
     * Gets the string index (0 for the highest string).
     *
     * @return the string index (0 for the highest string)
     */
    public int getString() {
        return string;
    }


    /**
     * Sets the string index (0 for the highest string).
     *
     * @param string the string index (0 for the highest string)
     */
    public void setString(int string) {
        this.string = string;
    }


    /**
     * Gets the fret.
     *
     * @return the fret
     */
    public int getFret() {
        return fret;
    }


    /**
     * Sets the fret.
     *
     * @param fret the fret
     */
    public void setFret(int fret) {
        this.fret = fret;
    }


    /**
     * Gets the sounding MIDI pitch.
     *
     * @return the sounding MIDI pitch
     */
    public Integer getMidi() {
        return midi;
    }


    /**
     * Sets the sounding MIDI pitch.
     *
     * @param midi the sounding MIDI pitch
     */
    public void setMidi(Integer midi) {
        this.midi = midi;
    }


    /**
     * Gets the written (transposed) pitch as stored in the file, or null.
     *
     * @return the written (transposed) pitch as stored in the file, or null
     */
    public SpelledPitch getWrittenPitch() {
        return writtenPitch;
    }


    /**
     * Sets the written (transposed) pitch as stored in the file, or null.
     *
     * @param writtenPitch the written (transposed) pitch as stored in the file, or null
     */
    public void setWrittenPitch(SpelledPitch writtenPitch) {
        this.writtenPitch = writtenPitch;
    }


    /**
     * Gets the concert (sounding) pitch as stored in the file, or null.
     *
     * @return the concert (sounding) pitch as stored in the file, or null
     */
    public SpelledPitch getConcertPitch() {
        return concertPitch;
    }


    /**
     * Sets the concert (sounding) pitch as stored in the file, or null.
     *
     * @param concertPitch the concert (sounding) pitch as stored in the file, or null
     */
    public void setConcertPitch(SpelledPitch concertPitch) {
        this.concertPitch = concertPitch;
    }


    /**
     * Gets the drum articulation index.
     *
     * @return the drum articulation index
     */
    public Integer getDrumArticulation() {
        return drumArticulation;
    }


    /**
     * Sets the drum articulation index.
     *
     * @param drumArticulation the drum articulation index
     */
    public void setDrumArticulation(Integer drumArticulation) {
        this.drumArticulation = drumArticulation;
    }


    /**
     * Gets the flag for a tie to the next note.
     *
     * @return the flag for a tie to the next note
     */
    public boolean isTieOrigin() {
        return tieOrigin;
    }


    /**
     * Sets the flag for a tie to the next note.
     *
     * @param tieOrigin the flag for a tie to the next note
     */
    public void setTieOrigin(boolean tieOrigin) {
        this.tieOrigin = tieOrigin;
    }


    /**
     * Gets the flag for a tie from the previous note.
     *
     * @return the flag for a tie from the previous note
     */
    public boolean isTieDestination() {
        return tieDestination;
    }


    /**
     * Sets the flag for a tie from the previous note.
     *
     * @param tieDestination the flag for a tie from the previous note
     */
    public void setTieDestination(boolean tieDestination) {
        this.tieDestination = tieDestination;
    }


    /**
     * Gets the flag for a dead (muted) note.
     *
     * @return the flag for a dead (muted) note
     */
    public boolean isDead() {
        return dead;
    }


    /**
     * Sets the flag for a dead (muted) note.
     *
     * @param dead the flag for a dead (muted) note
     */
    public void setDead(boolean dead) {
        this.dead = dead;
    }


    /**
     * Gets the flag for a ghost note.
     *
     * @return the flag for a ghost note
     */
    public boolean isGhost() {
        return ghost;
    }


    /**
     * Sets the flag for a ghost note.
     *
     * @param ghost the flag for a ghost note
     */
    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }


    /**
     * Gets the accent (0 none, 1 accent, 2 heavy accent, 3 tenuto).
     *
     * @return the accent (0 none, 1 accent, 2 heavy accent, 3 tenuto)
     */
    public int getAccent() {
        return accent;
    }


    /**
     * Sets the accent (0 none, 1 accent, 2 heavy accent, 3 tenuto).
     *
     * @param accent the accent (0 none, 1 accent, 2 heavy accent, 3 tenuto)
     */
    public void setAccent(int accent) {
        this.accent = accent;
    }


    /**
     * Gets the flag for staccato.
     *
     * @return the flag for staccato
     */
    public boolean isStaccato() {
        return staccato;
    }


    /**
     * Sets the flag for staccato.
     *
     * @param staccato the flag for staccato
     */
    public void setStaccato(boolean staccato) {
        this.staccato = staccato;
    }


    /**
     * Gets the flag for a hammer-on or pull-off to the next note.
     *
     * @return the flag for a hammer-on or pull-off to the next note
     */
    public boolean isHammerOrigin() {
        return hammerOrigin;
    }


    /**
     * Sets the flag for a hammer-on or pull-off to the next note.
     *
     * @param hammerOrigin the flag for a hammer-on or pull-off to the next note
     */
    public void setHammerOrigin(boolean hammerOrigin) {
        this.hammerOrigin = hammerOrigin;
    }


    /**
     * Gets the flag for a slide to the next note.
     *
     * @return the flag for a slide to the next note
     */
    public boolean isSlideOrigin() {
        return slideOrigin;
    }


    /**
     * Sets the flag for a slide to the next note.
     *
     * @param slideOrigin the flag for a slide to the next note
     */
    public void setSlideOrigin(boolean slideOrigin) {
        this.slideOrigin = slideOrigin;
    }


    /**
     * Gets the flag for a harmonic.
     *
     * @return the flag for a harmonic
     */
    public boolean isHarmonic() {
        return harmonic;
    }


    /**
     * Sets the flag for a harmonic.
     *
     * @param harmonic the flag for a harmonic
     */
    public void setHarmonic(boolean harmonic) {
        this.harmonic = harmonic;
    }


    /**
     * Gets the flag for a trill.
     *
     * @return the flag for a trill
     */
    public boolean isTrill() {
        return trill;
    }


    /**
     * Sets the flag for a trill.
     *
     * @param trill the flag for a trill
     */
    public void setTrill(boolean trill) {
        this.trill = trill;
    }


    /**
     * Gets the flag for vibrato.
     *
     * @return the flag for vibrato
     */
    public boolean isVibrato() {
        return vibrato;
    }


    /**
     * Sets the flag for vibrato.
     *
     * @param vibrato the flag for vibrato
     */
    public void setVibrato(boolean vibrato) {
        this.vibrato = vibrato;
    }


    /**
     * Gets the flag for let ring.
     *
     * @return the flag for let ring
     */
    public boolean isLetRing() {
        return letRing;
    }


    /**
     * Sets the flag for let ring.
     *
     * @param letRing the flag for let ring
     */
    public void setLetRing(boolean letRing) {
        this.letRing = letRing;
    }


    /**
     * Gets the flag for palm muting.
     *
     * @return the flag for palm muting
     */
    public boolean isPalmMute() {
        return palmMute;
    }


    /**
     * Sets the flag for palm muting.
     *
     * @param palmMute the flag for palm muting
     */
    public void setPalmMute(boolean palmMute) {
        this.palmMute = palmMute;
    }


    /**
     * Gets the flag for a bend.
     *
     * @return the flag for a bend
     */
    public boolean isBend() {
        return bend;
    }


    /**
     * Sets the flag for a bend.
     *
     * @param bend the flag for a bend
     */
    public void setBend(boolean bend) {
        this.bend = bend;
    }


    /**
     * Gets the left hand finger (0 thumb to 4 little finger).
     *
     * @return the left hand finger (0 thumb to 4 little finger)
     */
    public Integer getLeftFinger() {
        return leftFinger;
    }


    /**
     * Sets the left hand finger (0 thumb to 4 little finger).
     *
     * @param leftFinger the left hand finger (0 thumb to 4 little finger)
     */
    public void setLeftFinger(Integer leftFinger) {
        this.leftFinger = leftFinger;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                string, fret, midi, writtenPitch, concertPitch, drumArticulation, tieOrigin, tieDestination, dead, ghost, accent,
                staccato, hammerOrigin, slideOrigin, harmonic, trill, vibrato, letRing, palmMute, bend, leftFinger);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a GpNote with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GpNote other = (GpNote) obj;
        return Objects.equals(string, other.string)
                && Objects.equals(fret, other.fret)
                && Objects.equals(midi, other.midi)
                && Objects.equals(writtenPitch, other.writtenPitch)
                && Objects.equals(concertPitch, other.concertPitch)
                && Objects.equals(drumArticulation, other.drumArticulation)
                && Objects.equals(tieOrigin, other.tieOrigin)
                && Objects.equals(tieDestination, other.tieDestination)
                && Objects.equals(dead, other.dead)
                && Objects.equals(ghost, other.ghost)
                && Objects.equals(accent, other.accent)
                && Objects.equals(staccato, other.staccato)
                && Objects.equals(hammerOrigin, other.hammerOrigin)
                && Objects.equals(slideOrigin, other.slideOrigin)
                && Objects.equals(harmonic, other.harmonic)
                && Objects.equals(trill, other.trill)
                && Objects.equals(vibrato, other.vibrato)
                && Objects.equals(letRing, other.letRing)
                && Objects.equals(palmMute, other.palmMute)
                && Objects.equals(bend, other.bend)
                && Objects.equals(leftFinger, other.leftFinger);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "GpNote ["
                + "string=" + string
                + ", fret=" + fret
                + ", midi=" + midi
                + ", writtenPitch=" + writtenPitch
                + ", concertPitch=" + concertPitch
                + ", drumArticulation=" + drumArticulation
                + ", tieOrigin=" + tieOrigin
                + ", tieDestination=" + tieDestination
                + ", dead=" + dead
                + ", ghost=" + ghost
                + ", accent=" + accent
                + ", staccato=" + staccato
                + ", hammerOrigin=" + hammerOrigin
                + ", slideOrigin=" + slideOrigin
                + ", harmonic=" + harmonic
                + ", trill=" + trill
                + ", vibrato=" + vibrato
                + ", letRing=" + letRing
                + ", palmMute=" + palmMute
                + ", bend=" + bend
                + ", leftFinger=" + leftFinger
                + "]";
    }
}
