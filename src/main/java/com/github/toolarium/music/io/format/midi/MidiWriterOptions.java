/*
 * MidiWriterOptions.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.midi;

import java.util.Objects;


/**
 * The options of the MIDI writer. The sound is always the same: the options only decide how much of the notation
 * the MIDI file keeps. {@link #highQuality()} (the default) keeps all information; {@link #obfuscated()} keeps only
 * what is needed for the sound (and the copyright), so that the notation is hard to recreate from the file.
 *
 * @author patrick
 */
public class MidiWriterOptions {
    private static final int OBFUSCATED_TIMING_MILLIS = 8;
    private static final int OBFUSCATED_VELOCITY = 6;
    private boolean copyright;
    private boolean scoreInfo;
    private boolean markers;
    private boolean texts;
    private boolean signatures;
    private boolean tempoMap;
    private boolean singleTrack;
    private boolean channelMixing;
    private int timingVariation;
    private int velocityVariation;
    private long seed;


    /**
     * Constructor for MidiWriterOptions with all information (high quality).
     */
    public MidiWriterOptions() {
        this.copyright = true;
        this.scoreInfo = true;
        this.markers = true;
        this.texts = true;
        this.signatures = true;
        this.tempoMap = true;
        this.singleTrack = false;
        this.channelMixing = false;
        this.timingVariation = 0;
        this.velocityVariation = 0;
        this.seed = 0;
    }


    /**
     * Gets the options with all information: copyright, title, composer, names, markers, texts, time and key
     * signatures and the tempo map in one track per part.
     *
     * @return the options
     */
    public static MidiWriterOptions highQuality() {
        return new MidiWriterOptions();
    }


    /**
     * Gets the options that keep only the sound and the copyright: no names, markers, texts and signatures, a
     * constant tempo with the notes moved to their real time, small variations of timing and velocity, all
     * parts in one track and the parts on mixed channels.
     *
     * @return the options
     */
    public static MidiWriterOptions obfuscated() {
        return new MidiWriterOptions().setScoreInfo(false).setMarkers(false).setTexts(false).setSignatures(false).setTempoMap(false)
                .setSingleTrack(true).setChannelMixing(true).setTimingVariation(OBFUSCATED_TIMING_MILLIS).setVelocityVariation(OBFUSCATED_VELOCITY);
    }


    /**
     * Checks whether the copyright is written.
     *
     * @return true to write the copyright (default)
     */
    public boolean isCopyright() {
        return copyright;
    }


    /**
     * Sets whether the copyright is written.
     *
     * @param copyright true to write the copyright
     * @return the options
     */
    public MidiWriterOptions setCopyright(boolean copyright) {
        this.copyright = copyright;
        return this;
    }


    /**
     * Checks whether the information of the score is written: title, composer, arranger, track and instrument names.
     *
     * @return true to write the information (default)
     */
    public boolean isScoreInfo() {
        return scoreInfo;
    }


    /**
     * Sets whether the information of the score is written: title, composer, arranger, track and instrument names.
     *
     * @param scoreInfo true to write the information
     * @return the options
     */
    public MidiWriterOptions setScoreInfo(boolean scoreInfo) {
        this.scoreInfo = scoreInfo;
        return this;
    }


    /**
     * Checks whether the rehearsal marks are written as markers.
     *
     * @return true to write the markers (default)
     */
    public boolean isMarkers() {
        return markers;
    }


    /**
     * Sets whether the rehearsal marks are written as markers.
     *
     * @param markers true to write the markers
     * @return the options
     */
    public MidiWriterOptions setMarkers(boolean markers) {
        this.markers = markers;
        return this;
    }


    /**
     * Checks whether the texts of the score (tempo and expression texts) are written as text events.
     *
     * @return true to write the texts (default)
     */
    public boolean isTexts() {
        return texts;
    }


    /**
     * Sets whether the texts of the score (tempo and expression texts) are written as text events.
     *
     * @param texts true to write the texts
     * @return the options
     */
    public MidiWriterOptions setTexts(boolean texts) {
        this.texts = texts;
        return this;
    }


    /**
     * Checks whether the time and key signatures are written.
     *
     * @return true to write the signatures (default)
     */
    public boolean isSignatures() {
        return signatures;
    }


    /**
     * Sets whether the time and key signatures are written.
     *
     * @param signatures true to write the signatures
     * @return the options
     */
    public MidiWriterOptions setSignatures(boolean signatures) {
        this.signatures = signatures;
        return this;
    }


    /**
     * Checks whether the tempo map is written: the tempo changes with the notes on the beats. Without the tempo map
     * the file has one constant tempo and the notes are moved to their real time, so it sounds the same but the
     * beats and bars cannot be recognized.
     *
     * @return true to write the tempo map (default)
     */
    public boolean isTempoMap() {
        return tempoMap;
    }


    /**
     * Sets whether the tempo map is written.
     *
     * @param tempoMap true to write the tempo map, false for a constant tempo with the notes in real time
     * @return the options
     */
    public MidiWriterOptions setTempoMap(boolean tempoMap) {
        this.tempoMap = tempoMap;
        return this;
    }


    /**
     * Checks whether all parts are written into one track (MIDI file type 0) instead of one track per part.
     *
     * @return true for one track
     */
    public boolean isSingleTrack() {
        return singleTrack;
    }


    /**
     * Sets whether all parts are written into one track (MIDI file type 0); the parts keep their channels.
     *
     * @param singleTrack true for one track
     * @return the options
     */
    public MidiWriterOptions setSingleTrack(boolean singleTrack) {
        this.singleTrack = singleTrack;
        return this;
    }


    /**
     * Checks whether the parts are moved to randomly chosen channels, so that the order of the channels does not
     * show the order of the parts. The drum channel 10 is not changed.
     *
     * @return true to mix the channels
     */
    public boolean isChannelMixing() {
        return channelMixing;
    }


    /**
     * Sets whether the parts are moved to randomly chosen channels (the drum channel 10 is not changed).
     *
     * @param channelMixing true to mix the channels
     * @return the options
     */
    public MidiWriterOptions setChannelMixing(boolean channelMixing) {
        this.channelMixing = channelMixing;
        return this;
    }


    /**
     * Gets the largest random change of the start and the end of the notes.
     *
     * @return the variation in milliseconds, 0 for none (default)
     */
    public int getTimingVariation() {
        return timingVariation;
    }


    /**
     * Sets the largest random change of the start and the end of the notes.
     *
     * @param timingVariation the variation in milliseconds, 0 for none
     * @return the options
     */
    public MidiWriterOptions setTimingVariation(int timingVariation) {
        this.timingVariation = Math.max(0, timingVariation);
        return this;
    }


    /**
     * Gets the largest random change of the velocity of the notes.
     *
     * @return the variation in velocity steps, 0 for none (default)
     */
    public int getVelocityVariation() {
        return velocityVariation;
    }


    /**
     * Sets the largest random change of the velocity of the notes.
     *
     * @param velocityVariation the variation in velocity steps, 0 for none
     * @return the options
     */
    public MidiWriterOptions setVelocityVariation(int velocityVariation) {
        this.velocityVariation = Math.max(0, velocityVariation);
        return this;
    }


    /**
     * Gets the seed of the random variations: the same seed gives the same file.
     *
     * @return the seed
     */
    public long getSeed() {
        return seed;
    }


    /**
     * Sets the seed of the random variations: the same seed gives the same file.
     *
     * @param seed the seed
     * @return the options
     */
    public MidiWriterOptions setSeed(long seed) {
        this.seed = seed;
        return this;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(copyright, scoreInfo, markers, texts, signatures, tempoMap, singleTrack, channelMixing, timingVariation, velocityVariation, seed);
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
        MidiWriterOptions other = (MidiWriterOptions) obj;
        return copyright == other.copyright && scoreInfo == other.scoreInfo && markers == other.markers && texts == other.texts
                && signatures == other.signatures && tempoMap == other.tempoMap && singleTrack == other.singleTrack
                && channelMixing == other.channelMixing
                && timingVariation == other.timingVariation && velocityVariation == other.velocityVariation && seed == other.seed;
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "MidiWriterOptions [copyright=" + copyright + ", scoreInfo=" + scoreInfo + ", markers=" + markers + ", texts=" + texts
                + ", signatures=" + signatures + ", tempoMap=" + tempoMap + ", singleTrack=" + singleTrack + ", channelMixing=" + channelMixing + ", timingVariation="
                + timingVariation + ", velocityVariation=" + velocityVariation + ", seed=" + seed + "]";
    }
}
