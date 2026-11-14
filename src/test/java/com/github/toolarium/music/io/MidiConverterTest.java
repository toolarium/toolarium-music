/*
 * MidiConverterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.io.midi.impl.MidiDurationConverter;
import com.github.toolarium.music.io.midi.impl.MidiNoteConverter;
import com.github.toolarium.music.model.note.IPitch;
import org.junit.jupiter.api.Test;


/**
 * Tests for the MIDI converter utility classes.
 *
 * @author patrick
 */
public class MidiConverterTest {

    /**
     * Tests that a quarter note converts to the resolution value in ticks.
     */
    @Test
    void testQuarterNoteToTicks() {
        long ticks = MidiDurationConverter.toTicks("quarter", 0, 480);
        assertEquals(480, ticks);
    }


    /**
     * Tests that a half note is twice the resolution.
     */
    @Test
    void testHalfNoteToTicks() {
        long ticks = MidiDurationConverter.toTicks("half", 0, 480);
        assertEquals(960, ticks);
    }


    /**
     * Tests that a whole note is four times the resolution.
     */
    @Test
    void testWholeNoteToTicks() {
        long ticks = MidiDurationConverter.toTicks("whole", 0, 480);
        assertEquals(1920, ticks);
    }


    /**
     * Tests that an eighth note is half the resolution.
     */
    @Test
    void testEighthNoteToTicks() {
        long ticks = MidiDurationConverter.toTicks("eighth", 0, 480);
        assertEquals(240, ticks);
    }


    /**
     * Tests that a dotted quarter note is 1.5 times the resolution.
     */
    @Test
    void testDottedQuarterToTicks() {
        long ticks = MidiDurationConverter.toTicks("quarter", 1, 480);
        assertEquals(720, ticks);
    }


    /**
     * Tests that any non-zero dot count applies a single dot factor (1.5x).
     */
    @Test
    void testMultipleDotsTreatedAsSingleDot() {
        long ticks = MidiDurationConverter.toTicks("quarter", 2, 480);
        assertEquals(720, ticks);
    }


    /**
     * Tests round-trip: ticks to note type and back.
     */
    @Test
    void testQuantizeQuarterNote() {
        String[] result = MidiDurationConverter.quantize(480, 480);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("quarter", result[0]);
        assertEquals("0", result[1]);
    }


    /**
     * Tests quantization of a half note.
     */
    @Test
    void testQuantizeHalfNote() {
        String[] result = MidiDurationConverter.quantize(960, 480);
        assertNotNull(result);
        assertEquals("half", result[0]);
    }


    /**
     * Tests divisions-to-ticks conversion.
     */
    @Test
    void testDivisionsToTicks() {
        long ticks = MidiDurationConverter.divisionsToTicks(1, 1, 480);
        assertEquals(480, ticks);

        long ticksHalf = MidiDurationConverter.divisionsToTicks(2, 1, 480);
        assertEquals(960, ticksHalf);
    }


    /**
     * Tests that middle C (MIDI 60) converts to pitch C4.
     */
    @Test
    void testMidiNoteToPitchMiddleC() {
        IPitch pitch = MidiNoteConverter.midiNoteToPitch(60);
        assertNotNull(pitch);
        assertEquals("C", pitch.getStep());
        assertEquals(4, pitch.getOctave());
    }


    /**
     * Tests that MIDI note 69 converts to A4.
     */
    @Test
    void testMidiNoteToPitchA4() {
        IPitch pitch = MidiNoteConverter.midiNoteToPitch(69);
        assertNotNull(pitch);
        assertEquals("A", pitch.getStep());
        assertEquals(4, pitch.getOctave());
    }


    /**
     * Tests that MIDI note 0 (lowest) converts to a valid pitch.
     */
    @Test
    void testMidiNoteToPitchLowest() {
        IPitch pitch = MidiNoteConverter.midiNoteToPitch(0);
        assertNotNull(pitch);
        assertNotNull(pitch.getStep());
    }


    /**
     * Tests that MIDI note 127 (highest) converts to a valid pitch.
     */
    @Test
    void testMidiNoteToPitchHighest() {
        IPitch pitch = MidiNoteConverter.midiNoteToPitch(127);
        assertNotNull(pitch);
        assertNotNull(pitch.getStep());
    }


    /**
     * Tests round-trip: pitch to MIDI note and back.
     */
    @Test
    void testPitchToMidiNoteRoundTrip() {
        IPitch pitch = MidiNoteConverter.midiNoteToPitch(60);
        int midiNote = MidiNoteConverter.pitchToMidiNote(pitch);
        assertEquals(60, midiNote);
    }


    /**
     * Tests that null pitch returns middle C (60).
     */
    @Test
    void testPitchToMidiNoteNull() {
        int midiNote = MidiNoteConverter.pitchToMidiNote(null);
        assertEquals(60, midiNote);
    }


    /**
     * Tests MIDI note clamping: the result is always in the 0-127 range.
     */
    @Test
    void testPitchToMidiNoteClampRange() {
        for (int i = 0; i <= 127; i++) {
            IPitch pitch = MidiNoteConverter.midiNoteToPitch(i);
            int result = MidiNoteConverter.pitchToMidiNote(pitch);
            assertTrue(result >= 0 && result <= 127, "MIDI note out of range: " + result);
        }
    }
}
