/*
 * AbcVoice.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl.dto;

import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.MeasureData;
import com.github.toolarium.music.io.dto.NoteData;
import com.github.toolarium.music.io.dto.VoiceData;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * The parse state of one ABC voice.
 *
 * @author patrick
 */
public class AbcVoice {
    private final String id;
    private final List<MeasureData> measures = new ArrayList<>();
    private final List<Integer> lineBars = new ArrayList<>();
    private final List<EventData> lineEvents = new ArrayList<>();
    private final Map<String, Integer> measureAccidentals = new HashMap<>();
    private final Deque<Integer> slurs = new ArrayDeque<>();
    private final List<String> decorations = new ArrayList<>();
    private final List<HarmonyImpl> harmonies = new ArrayList<>();
    private final List<DirectionImpl> directions = new ArrayList<>();
    private List<NoteData> pendingTies = new ArrayList<>();
    private String name;
    private String shortName;
    private ClefImpl clef;
    private int transpose;
    private Integer program;
    private Integer channel;
    private AbcKey key;
    private AbcMeter meter;
    private Fraction unit;
    private MeasureData measure;
    private VoiceData voiceData;
    private int overlay;
    private int maxOverlay;
    private int slurStarts;
    private int slurCounter;
    private int tupletRemaining;
    private int tupletActual;
    private int tupletNormal;
    private boolean tupletFirst;
    private EventData lastEvent;
    private Fraction brokenFactor;
    private boolean grace;
    private boolean graceSlash;
    private boolean lineStarted;
    private int verse;
    private String ending;
    private boolean endingAfterRepeat;
    private boolean lastBarRepeatEnd;
    private boolean repeatStartPending;
    private AbcKey pendingKey;
    private AbcMeter pendingMeter;
    private int wedgeNumber;


    /**
     * Constructor for AbcVoice.
     *
     * @param id the voice ID
     */
    public AbcVoice(String id) {
        this.id = id;
    }


    /**
     * Gets the voice ID.
     *
     * @return the voice ID
     */
    public String getId() {
        return id;
    }


    /**
     * Gets the measures of the voice (mutable).
     *
     * @return the measures of the voice (mutable)
     */
    public List<MeasureData> getMeasures() {
        return measures;
    }


    /**
     * Gets the measure index of each note event of the current music line (mutable).
     *
     * @return the measure index of each note event of the current music line (mutable)
     */
    public List<Integer> getLineBars() {
        return lineBars;
    }


    /**
     * Gets the note events of the current music line used for lyrics (mutable).
     *
     * @return the note events of the current music line used for lyrics (mutable)
     */
    public List<EventData> getLineEvents() {
        return lineEvents;
    }


    /**
     * Gets the accidentals of the current measure by pitch (mutable).
     *
     * @return the accidentals of the current measure by pitch (mutable)
     */
    public Map<String, Integer> getMeasureAccidentals() {
        return measureAccidentals;
    }


    /**
     * Gets the numbers of the open slurs (mutable).
     *
     * @return the numbers of the open slurs (mutable)
     */
    public Deque<Integer> getSlurs() {
        return slurs;
    }


    /**
     * Gets the decorations waiting for the next event (mutable).
     *
     * @return the decorations waiting for the next event (mutable)
     */
    public List<String> getDecorations() {
        return decorations;
    }


    /**
     * Gets the chord symbols waiting for the next event (mutable).
     *
     * @return the chord symbols waiting for the next event (mutable)
     */
    public List<HarmonyImpl> getHarmonies() {
        return harmonies;
    }


    /**
     * Gets the directions waiting for the next event (mutable).
     *
     * @return the directions waiting for the next event (mutable)
     */
    public List<DirectionImpl> getDirections() {
        return directions;
    }


    /**
     * Gets the notes with a tie to the next note.
     *
     * @return the notes with a tie to the next note
     */
    public List<NoteData> getPendingTies() {
        return pendingTies;
    }


    /**
     * Sets the notes with a tie to the next note.
     *
     * @param pendingTies the notes with a tie to the next note
     */
    public void setPendingTies(List<NoteData> pendingTies) {
        this.pendingTies = pendingTies;
    }


    /**
     * Gets the voice name.
     *
     * @return the voice name
     */
    public String getName() {
        return name;
    }


    /**
     * Sets the voice name.
     *
     * @param name the voice name
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Gets the short voice name.
     *
     * @return the short voice name
     */
    public String getShortName() {
        return shortName;
    }


    /**
     * Sets the short voice name.
     *
     * @param shortName the short voice name
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
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
     * Gets the transposition in semitones.
     *
     * @return the transposition in semitones
     */
    public int getTranspose() {
        return transpose;
    }


    /**
     * Sets the transposition in semitones.
     *
     * @param transpose the transposition in semitones
     */
    public void setTranspose(int transpose) {
        this.transpose = transpose;
    }


    /**
     * Gets the MIDI program (1-based).
     *
     * @return the MIDI program (1-based)
     */
    public Integer getProgram() {
        return program;
    }


    /**
     * Sets the MIDI program (1-based).
     *
     * @param program the MIDI program (1-based)
     */
    public void setProgram(Integer program) {
        this.program = program;
    }


    /**
     * Gets the MIDI channel.
     *
     * @return the MIDI channel
     */
    public Integer getChannel() {
        return channel;
    }


    /**
     * Sets the MIDI channel.
     *
     * @param channel the MIDI channel
     */
    public void setChannel(Integer channel) {
        this.channel = channel;
    }


    /**
     * Gets the current key.
     *
     * @return the current key
     */
    public AbcKey getKey() {
        return key;
    }


    /**
     * Sets the current key.
     *
     * @param key the current key
     */
    public void setKey(AbcKey key) {
        this.key = key;
    }


    /**
     * Gets the current meter.
     *
     * @return the current meter
     */
    public AbcMeter getMeter() {
        return meter;
    }


    /**
     * Sets the current meter.
     *
     * @param meter the current meter
     */
    public void setMeter(AbcMeter meter) {
        this.meter = meter;
    }


    /**
     * Gets the unit note length.
     *
     * @return the unit note length
     */
    public Fraction getUnit() {
        return unit;
    }


    /**
     * Sets the unit note length.
     *
     * @param unit the unit note length
     */
    public void setUnit(Fraction unit) {
        this.unit = unit;
    }


    /**
     * Gets the current measure.
     *
     * @return the current measure
     */
    public MeasureData getMeasure() {
        return measure;
    }


    /**
     * Sets the current measure.
     *
     * @param measure the current measure
     */
    public void setMeasure(MeasureData measure) {
        this.measure = measure;
    }


    /**
     * Gets the voice data (the voice or an overlay) of the current measure.
     *
     * @return the voice data (the voice or an overlay) of the current measure
     */
    public VoiceData getVoiceData() {
        return voiceData;
    }


    /**
     * Sets the voice data (the voice or an overlay) of the current measure.
     *
     * @param voiceData the voice data (the voice or an overlay) of the current measure
     */
    public void setVoiceData(VoiceData voiceData) {
        this.voiceData = voiceData;
    }


    /**
     * Gets the index of the current overlay (0 for the voice itself).
     *
     * @return the index of the current overlay (0 for the voice itself)
     */
    public int getOverlay() {
        return overlay;
    }


    /**
     * Sets the index of the current overlay (0 for the voice itself).
     *
     * @param overlay the index of the current overlay (0 for the voice itself)
     */
    public void setOverlay(int overlay) {
        this.overlay = overlay;
    }


    /**
     * Gets the highest overlay index used.
     *
     * @return the highest overlay index used
     */
    public int getMaxOverlay() {
        return maxOverlay;
    }


    /**
     * Sets the highest overlay index used.
     *
     * @param maxOverlay the highest overlay index used
     */
    public void setMaxOverlay(int maxOverlay) {
        this.maxOverlay = maxOverlay;
    }


    /**
     * Gets the number of slurs that start at the next event.
     *
     * @return the number of slurs that start at the next event
     */
    public int getSlurStarts() {
        return slurStarts;
    }


    /**
     * Sets the number of slurs that start at the next event.
     *
     * @param slurStarts the number of slurs that start at the next event
     */
    public void setSlurStarts(int slurStarts) {
        this.slurStarts = slurStarts;
    }


    /**
     * Gets the last slur number.
     *
     * @return the last slur number
     */
    public int getSlurCounter() {
        return slurCounter;
    }


    /**
     * Sets the last slur number.
     *
     * @param slurCounter the last slur number
     */
    public void setSlurCounter(int slurCounter) {
        this.slurCounter = slurCounter;
    }


    /**
     * Gets the number of events left in the current tuplet.
     *
     * @return the number of events left in the current tuplet
     */
    public int getTupletRemaining() {
        return tupletRemaining;
    }


    /**
     * Sets the number of events left in the current tuplet.
     *
     * @param tupletRemaining the number of events left in the current tuplet
     */
    public void setTupletRemaining(int tupletRemaining) {
        this.tupletRemaining = tupletRemaining;
    }


    /**
     * Gets the actual notes of the current tuplet.
     *
     * @return the actual notes of the current tuplet
     */
    public int getTupletActual() {
        return tupletActual;
    }


    /**
     * Sets the actual notes of the current tuplet.
     *
     * @param tupletActual the actual notes of the current tuplet
     */
    public void setTupletActual(int tupletActual) {
        this.tupletActual = tupletActual;
    }


    /**
     * Gets the normal notes of the current tuplet.
     *
     * @return the normal notes of the current tuplet
     */
    public int getTupletNormal() {
        return tupletNormal;
    }


    /**
     * Sets the normal notes of the current tuplet.
     *
     * @param tupletNormal the normal notes of the current tuplet
     */
    public void setTupletNormal(int tupletNormal) {
        this.tupletNormal = tupletNormal;
    }


    /**
     * Gets the flag for the first event of the current tuplet.
     *
     * @return the flag for the first event of the current tuplet
     */
    public boolean isTupletFirst() {
        return tupletFirst;
    }


    /**
     * Sets the flag for the first event of the current tuplet.
     *
     * @param tupletFirst the flag for the first event of the current tuplet
     */
    public void setTupletFirst(boolean tupletFirst) {
        this.tupletFirst = tupletFirst;
    }


    /**
     * Gets the last event.
     *
     * @return the last event
     */
    public EventData getLastEvent() {
        return lastEvent;
    }


    /**
     * Sets the last event.
     *
     * @param lastEvent the last event
     */
    public void setLastEvent(EventData lastEvent) {
        this.lastEvent = lastEvent;
    }


    /**
     * Gets the broken rhythm factor of the next event.
     *
     * @return the broken rhythm factor of the next event
     */
    public Fraction getBrokenFactor() {
        return brokenFactor;
    }


    /**
     * Sets the broken rhythm factor of the next event.
     *
     * @param brokenFactor the broken rhythm factor of the next event
     */
    public void setBrokenFactor(Fraction brokenFactor) {
        this.brokenFactor = brokenFactor;
    }


    /**
     * Gets the flag for notes inside a grace group.
     *
     * @return the flag for notes inside a grace group
     */
    public boolean isGrace() {
        return grace;
    }


    /**
     * Sets the flag for notes inside a grace group.
     *
     * @param grace the flag for notes inside a grace group
     */
    public void setGrace(boolean grace) {
        this.grace = grace;
    }


    /**
     * Gets the flag for a grace group with a slash.
     *
     * @return the flag for a grace group with a slash
     */
    public boolean isGraceSlash() {
        return graceSlash;
    }


    /**
     * Sets the flag for a grace group with a slash.
     *
     * @param graceSlash the flag for a grace group with a slash
     */
    public void setGraceSlash(boolean graceSlash) {
        this.graceSlash = graceSlash;
    }


    /**
     * Gets the flag for a voice with events in the current music line.
     *
     * @return the flag for a voice with events in the current music line
     */
    public boolean isLineStarted() {
        return lineStarted;
    }


    /**
     * Sets the flag for a voice with events in the current music line.
     *
     * @param lineStarted the flag for a voice with events in the current music line
     */
    public void setLineStarted(boolean lineStarted) {
        this.lineStarted = lineStarted;
    }


    /**
     * Gets the last lyrics verse of the current music line.
     *
     * @return the last lyrics verse of the current music line
     */
    public int getVerse() {
        return verse;
    }


    /**
     * Sets the last lyrics verse of the current music line.
     *
     * @param verse the last lyrics verse of the current music line
     */
    public void setVerse(int verse) {
        this.verse = verse;
    }


    /**
     * Gets the current ending number.
     *
     * @return the current ending number
     */
    public String getEnding() {
        return ending;
    }


    /**
     * Sets the current ending number.
     *
     * @param ending the current ending number
     */
    public void setEnding(String ending) {
        this.ending = ending;
    }


    /**
     * Gets the flag for an ending that started after an end repeat.
     *
     * @return the flag for an ending that started after an end repeat
     */
    public boolean isEndingAfterRepeat() {
        return endingAfterRepeat;
    }


    /**
     * Sets the flag for an ending that started after an end repeat.
     *
     * @param endingAfterRepeat the flag for an ending that started after an end repeat
     */
    public void setEndingAfterRepeat(boolean endingAfterRepeat) {
        this.endingAfterRepeat = endingAfterRepeat;
    }


    /**
     * Gets the flag for a last bar line that was an end repeat.
     *
     * @return the flag for a last bar line that was an end repeat
     */
    public boolean isLastBarRepeatEnd() {
        return lastBarRepeatEnd;
    }


    /**
     * Sets the flag for a last bar line that was an end repeat.
     *
     * @param lastBarRepeatEnd the flag for a last bar line that was an end repeat
     */
    public void setLastBarRepeatEnd(boolean lastBarRepeatEnd) {
        this.lastBarRepeatEnd = lastBarRepeatEnd;
    }


    /**
     * Gets the flag for a repeat that starts at the next measure.
     *
     * @return the flag for a repeat that starts at the next measure
     */
    public boolean isRepeatStartPending() {
        return repeatStartPending;
    }


    /**
     * Sets the flag for a repeat that starts at the next measure.
     *
     * @param repeatStartPending the flag for a repeat that starts at the next measure
     */
    public void setRepeatStartPending(boolean repeatStartPending) {
        this.repeatStartPending = repeatStartPending;
    }


    /**
     * Gets the key change for the next measure.
     *
     * @return the key change for the next measure
     */
    public AbcKey getPendingKey() {
        return pendingKey;
    }


    /**
     * Sets the key change for the next measure.
     *
     * @param pendingKey the key change for the next measure
     */
    public void setPendingKey(AbcKey pendingKey) {
        this.pendingKey = pendingKey;
    }


    /**
     * Gets the meter change for the next measure.
     *
     * @return the meter change for the next measure
     */
    public AbcMeter getPendingMeter() {
        return pendingMeter;
    }


    /**
     * Sets the meter change for the next measure.
     *
     * @param pendingMeter the meter change for the next measure
     */
    public void setPendingMeter(AbcMeter pendingMeter) {
        this.pendingMeter = pendingMeter;
    }


    /**
     * Gets the last wedge number.
     *
     * @return the last wedge number
     */
    public int getWedgeNumber() {
        return wedgeNumber;
    }


    /**
     * Sets the last wedge number.
     *
     * @param wedgeNumber the last wedge number
     */
    public void setWedgeNumber(int wedgeNumber) {
        this.wedgeNumber = wedgeNumber;
    }


    /**
     * Calculates the hash code from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                id, measures, lineBars, lineEvents, measureAccidentals, slurs, decorations, harmonies, directions, pendingTies,
                name, shortName, clef, transpose, program, channel, key, meter, unit, measure, voiceData, overlay, maxOverlay,
                slurStarts, slurCounter, tupletRemaining, tupletActual, tupletNormal, tupletFirst, lastEvent, brokenFactor,
                grace, graceSlash, lineStarted, verse, ending, endingAfterRepeat, lastBarRepeatEnd, repeatStartPending,
                pendingKey, pendingMeter, wedgeNumber);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a AbcVoice with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbcVoice other = (AbcVoice) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(measures, other.measures)
                && Objects.equals(lineBars, other.lineBars)
                && Objects.equals(lineEvents, other.lineEvents)
                && Objects.equals(measureAccidentals, other.measureAccidentals)
                && Objects.equals(slurs, other.slurs)
                && Objects.equals(decorations, other.decorations)
                && Objects.equals(harmonies, other.harmonies)
                && Objects.equals(directions, other.directions)
                && Objects.equals(pendingTies, other.pendingTies)
                && Objects.equals(name, other.name)
                && Objects.equals(shortName, other.shortName)
                && Objects.equals(clef, other.clef)
                && Objects.equals(transpose, other.transpose)
                && Objects.equals(program, other.program)
                && Objects.equals(channel, other.channel)
                && Objects.equals(key, other.key)
                && Objects.equals(meter, other.meter)
                && Objects.equals(unit, other.unit)
                && Objects.equals(measure, other.measure)
                && Objects.equals(voiceData, other.voiceData)
                && Objects.equals(overlay, other.overlay)
                && Objects.equals(maxOverlay, other.maxOverlay)
                && Objects.equals(slurStarts, other.slurStarts)
                && Objects.equals(slurCounter, other.slurCounter)
                && Objects.equals(tupletRemaining, other.tupletRemaining)
                && Objects.equals(tupletActual, other.tupletActual)
                && Objects.equals(tupletNormal, other.tupletNormal)
                && Objects.equals(tupletFirst, other.tupletFirst)
                && Objects.equals(lastEvent, other.lastEvent)
                && Objects.equals(brokenFactor, other.brokenFactor)
                && Objects.equals(grace, other.grace)
                && Objects.equals(graceSlash, other.graceSlash)
                && Objects.equals(lineStarted, other.lineStarted)
                && Objects.equals(verse, other.verse)
                && Objects.equals(ending, other.ending)
                && Objects.equals(endingAfterRepeat, other.endingAfterRepeat)
                && Objects.equals(lastBarRepeatEnd, other.lastBarRepeatEnd)
                && Objects.equals(repeatStartPending, other.repeatStartPending)
                && Objects.equals(pendingKey, other.pendingKey)
                && Objects.equals(pendingMeter, other.pendingMeter)
                && Objects.equals(wedgeNumber, other.wedgeNumber);
    }


    /**
     * Creates a text representation with all fields.
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "AbcVoice ["
                + "id=" + id
                + ", measures=" + measures
                + ", lineBars=" + lineBars
                + ", lineEvents=" + lineEvents
                + ", measureAccidentals=" + measureAccidentals
                + ", slurs=" + slurs
                + ", decorations=" + decorations
                + ", harmonies=" + harmonies
                + ", directions=" + directions
                + ", pendingTies=" + pendingTies
                + ", name=" + name
                + ", shortName=" + shortName
                + ", clef=" + clef
                + ", transpose=" + transpose
                + ", program=" + program
                + ", channel=" + channel
                + ", key=" + key
                + ", meter=" + meter
                + ", unit=" + unit
                + ", measure=" + measure
                + ", voiceData=" + voiceData
                + ", overlay=" + overlay
                + ", maxOverlay=" + maxOverlay
                + ", slurStarts=" + slurStarts
                + ", slurCounter=" + slurCounter
                + ", tupletRemaining=" + tupletRemaining
                + ", tupletActual=" + tupletActual
                + ", tupletNormal=" + tupletNormal
                + ", tupletFirst=" + tupletFirst
                + ", lastEvent=" + lastEvent
                + ", brokenFactor=" + brokenFactor
                + ", grace=" + grace
                + ", graceSlash=" + graceSlash
                + ", lineStarted=" + lineStarted
                + ", verse=" + verse
                + ", ending=" + ending
                + ", endingAfterRepeat=" + endingAfterRepeat
                + ", lastBarRepeatEnd=" + lastBarRepeatEnd
                + ", repeatStartPending=" + repeatStartPending
                + ", pendingKey=" + pendingKey
                + ", pendingMeter=" + pendingMeter
                + ", wedgeNumber=" + wedgeNumber
                + "]";
    }
}
