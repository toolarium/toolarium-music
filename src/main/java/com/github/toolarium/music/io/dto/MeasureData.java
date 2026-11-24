/*
 * MeasureData.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * One measure of a part, as collected by a format reader for the {@link ScoreBuilder}. Time and
 * key are only set where they change; the builder carries them forward.
 *
 * @author patrick
 */
public class MeasureData {
    private final List<VoiceData> voices = new ArrayList<>();
    private final List<DirectionData> directions = new ArrayList<>();
    private final List<HarmonyImpl> harmonies = new ArrayList<>();
    private final List<ClefChange> clefs = new ArrayList<>();
    private Integer beats;
    private Integer beatType;
    private String timeSymbol;
    private Integer keyFifths;
    private String keyMode;
    private Fraction length;
    private boolean implicit;
    private boolean repeatStart;
    private Integer repeatEndTimes;
    private boolean repeatEnd;
    private String endingNumber;
    private String rightBarStyle;
    private String leftBarStyle;


    /**
     * Constructor for MeasureData.
     */
    public MeasureData() {
        // default constructor
    }


    /**
     * Gets the voices.
     *
     * @return the voices (mutable)
     */
    public List<VoiceData> getVoices() {
        return voices;
    }


    /**
     * Gets a voice, creating it if needed.
     *
     * @param staff the staff (1-based)
     * @param voice the MusicXML voice number
     * @return the voice
     */
    public VoiceData voice(int staff, int voice) {
        for (VoiceData data : voices) {
            if (data.getStaff() == staff && data.getVoice() == voice) {
                return data;
            }
        }
        VoiceData data = new VoiceData(staff, voice);
        voices.add(data);
        return data;
    }


    /**
     * Gets the directions with a position in the measure.
     *
     * @return the directions (mutable)
     */
    public List<DirectionData> getDirections() {
        return directions;
    }


    /**
     * Adds a direction at a position.
     *
     * @param direction the direction
     * @param position the position from the measure start in whole notes
     * @param staff the staff (1-based)
     */
    public void addDirection(DirectionImpl direction, Fraction position, int staff) {
        directions.add(new DirectionData(direction, position, staff));
    }


    /**
     * Gets the chord symbols of the measure.
     *
     * @return the harmonies (mutable)
     */
    public List<HarmonyImpl> getHarmonies() {
        return harmonies;
    }


    /**
     * Gets the clefs of the measure (at the start and within).
     *
     * @return the clef changes (mutable)
     */
    public List<ClefChange> getClefs() {
        return clefs;
    }


    /**
     * Adds a clef.
     *
     * @param staff the staff (1-based)
     * @param position the position from the measure start in whole notes
     * @param clef the clef
     */
    public void addClef(int staff, Fraction position, ClefImpl clef) {
        clefs.add(new ClefChange(staff, position, clef));
    }


    /**
     * Sets a time signature (change).
     *
     * @param beats the beats
     * @param beatType the beat type
     * @param symbol the MusicXML time symbol (common or cut), or null
     */
    public void setTime(int beats, int beatType, String symbol) {
        this.beats = beats;
        this.beatType = beatType;
        this.timeSymbol = symbol;
    }


    /**
     * Gets the beats of a time signature (change).
     *
     * @return the beats, or null without a time signature
     */
    public Integer getBeats() {
        return beats;
    }


    /**
     * Gets the beat type of a time signature (change).
     *
     * @return the beat type, or null without a time signature
     */
    public Integer getBeatType() {
        return beatType;
    }


    /**
     * Gets the time symbol.
     *
     * @return the time symbol (common or cut), or null
     */
    public String getTimeSymbol() {
        return timeSymbol;
    }


    /**
     * Sets a key signature (change).
     *
     * @param fifths the number of sharps (positive) or flats (negative)
     * @param mode the mode (major, minor and so on), or null
     */
    public void setKey(int fifths, String mode) {
        this.keyFifths = fifths;
        this.keyMode = mode;
    }


    /**
     * Gets the key signature (change).
     *
     * @return the fifths, or null without a key signature
     */
    public Integer getKeyFifths() {
        return keyFifths;
    }


    /**
     * Gets the key mode.
     *
     * @return the mode, or null
     */
    public String getKeyMode() {
        return keyMode;
    }


    /**
     * Gets the explicit length of the measure.
     *
     * @return the length in whole notes, or null to use the time signature and the content
     */
    public Fraction getLength() {
        return length;
    }


    /**
     * Sets the explicit length of the measure (for example of a pickup measure).
     *
     * @param length the length in whole notes, or null
     */
    public void setLength(Fraction length) {
        this.length = length;
    }


    /**
     * Checks if the measure is implicit (a pickup measure not counted in the numbering).
     *
     * @return true if implicit
     */
    public boolean isImplicit() {
        return implicit;
    }


    /**
     * Sets the implicit flag.
     *
     * @param implicit true if implicit
     */
    public void setImplicit(boolean implicit) {
        this.implicit = implicit;
    }


    /**
     * Checks if a repeat starts at this measure.
     *
     * @return true if a repeat starts
     */
    public boolean isRepeatStart() {
        return repeatStart;
    }


    /**
     * Sets the repeat start flag.
     *
     * @param repeatStart true if a repeat starts
     */
    public void setRepeatStart(boolean repeatStart) {
        this.repeatStart = repeatStart;
    }


    /**
     * Checks if a repeat ends at this measure.
     *
     * @return true if a repeat ends
     */
    public boolean isRepeatEnd() {
        return repeatEnd;
    }


    /**
     * Sets a repeat end.
     *
     * @param repeatEnd true if a repeat ends
     * @param times the number of times the section is played, or null for the default
     */
    public void setRepeatEnd(boolean repeatEnd, Integer times) {
        this.repeatEnd = repeatEnd;
        this.repeatEndTimes = times;
    }


    /**
     * Gets the number of times the repeated section is played.
     *
     * @return the times, or null for the default
     */
    public Integer getRepeatEndTimes() {
        return repeatEndTimes;
    }


    /**
     * Gets the ending (volta) number of the measure. Consecutive measures with the same number
     * form one ending.
     *
     * @return the ending number, for example "1" or "1, 2", or null
     */
    public String getEndingNumber() {
        return endingNumber;
    }


    /**
     * Sets the ending (volta) number of the measure.
     *
     * @param endingNumber the ending number, or null
     */
    public void setEndingNumber(String endingNumber) {
        this.endingNumber = endingNumber;
    }


    /**
     * Gets the bar style of the right barline.
     *
     * @return the MusicXML bar style (for example light-light), or null for a regular barline
     */
    public String getRightBarStyle() {
        return rightBarStyle;
    }


    /**
     * Sets the bar style of the right barline.
     *
     * @param rightBarStyle the MusicXML bar style, or null
     */
    public void setRightBarStyle(String rightBarStyle) {
        this.rightBarStyle = rightBarStyle;
    }


    /**
     * Gets the bar style of the left barline.
     *
     * @return the MusicXML bar style, or null
     */
    public String getLeftBarStyle() {
        return leftBarStyle;
    }


    /**
     * Sets the bar style of the left barline.
     *
     * @param leftBarStyle the MusicXML bar style, or null
     */
    public void setLeftBarStyle(String leftBarStyle) {
        this.leftBarStyle = leftBarStyle;
    }


    /**
     * Calculates the length of the content: the longest voice.
     *
     * @return the length in whole notes
     */
    public Fraction getContentLength() {
        Fraction result = Fraction.ZERO;
        for (VoiceData voice : voices) {
            result = result.max(voice.getLength());
        }
        return result;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                voices, directions, harmonies, clefs, beats, beatType, timeSymbol, keyFifths, keyMode, length, implicit,
                repeatStart, repeatEndTimes, repeatEnd, endingNumber, rightBarStyle, leftBarStyle);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a MeasureData with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MeasureData other = (MeasureData) obj;
        return Objects.equals(voices, other.voices)
                && Objects.equals(directions, other.directions)
                && Objects.equals(harmonies, other.harmonies)
                && Objects.equals(clefs, other.clefs)
                && Objects.equals(beats, other.beats)
                && Objects.equals(beatType, other.beatType)
                && Objects.equals(timeSymbol, other.timeSymbol)
                && Objects.equals(keyFifths, other.keyFifths)
                && Objects.equals(keyMode, other.keyMode)
                && Objects.equals(length, other.length)
                && Objects.equals(implicit, other.implicit)
                && Objects.equals(repeatStart, other.repeatStart)
                && Objects.equals(repeatEndTimes, other.repeatEndTimes)
                && Objects.equals(repeatEnd, other.repeatEnd)
                && Objects.equals(endingNumber, other.endingNumber)
                && Objects.equals(rightBarStyle, other.rightBarStyle)
                && Objects.equals(leftBarStyle, other.leftBarStyle);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "MeasureData ["
                + "voices=" + voices
                + ", directions=" + directions
                + ", harmonies=" + harmonies
                + ", clefs=" + clefs
                + ", beats=" + beats
                + ", beatType=" + beatType
                + ", timeSymbol=" + timeSymbol
                + ", keyFifths=" + keyFifths
                + ", keyMode=" + keyMode
                + ", length=" + length
                + ", implicit=" + implicit
                + ", repeatStart=" + repeatStart
                + ", repeatEndTimes=" + repeatEndTimes
                + ", repeatEnd=" + repeatEnd
                + ", endingNumber=" + endingNumber
                + ", rightBarStyle=" + rightBarStyle
                + ", leftBarStyle=" + leftBarStyle
                + "]";
    }
}
