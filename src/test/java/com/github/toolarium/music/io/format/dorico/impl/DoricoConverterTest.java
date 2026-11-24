/*
 * DoricoConverterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.note.impl.PitchImpl;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Tests for the {@link DoricoPitchConverter} and the {@link DoricoDurationConverter}.
 *
 * @author patrick
 */
public class DoricoConverterTest {
    private static final String QUARTER = "quarter";
    private static final String EIGHTH = "eighth";
    private static final String NOTE_C = "C";

    /**
     * Tests the conversion of MIDI pitch and letter spelling into step, alter and octave.
     */
    @Test
    void testPitchSpelling() {
        assertPitch(DoricoPitchConverter.getInstance().toPitch(60, NOTE_C, false), NOTE_C, null, 4);
        assertPitch(DoricoPitchConverter.getInstance().toPitch(73, NOTE_C, false), NOTE_C, 1.0, 5);
        assertPitch(DoricoPitchConverter.getInstance().toPitch(73, "D", false), "D", -1.0, 5);
        assertPitch(DoricoPitchConverter.getInstance().toPitch(72, "B", false), "B", 1.0, 4);
        assertPitch(DoricoPitchConverter.getInstance().toPitch(59, NOTE_C, false), NOTE_C, -1.0, 4);
        assertPitch(DoricoPitchConverter.getInstance().toPitch(74, NOTE_C, false), NOTE_C, 2.0, 5);
    }


    /**
     * Tests the default spelling used when a note has no spelling property.
     */
    @Test
    void testDefaultSpelling() {
        assertPitch(DoricoPitchConverter.getInstance().toPitch(61, null, false), NOTE_C, 1.0, 4);
        assertPitch(DoricoPitchConverter.getInstance().toPitch(61, null, true), "D", -1.0, 4);
        assertPitch(DoricoPitchConverter.getInstance().toPitch(61, "X", false), NOTE_C, 1.0, 4);
    }


    /**
     * Tests letter shifting and interval to key signature conversion for transposing instruments.
     */
    @Test
    void testTransposition() {
        assertEquals("D", DoricoPitchConverter.getInstance().shiftLetter(NOTE_C, 1));
        assertEquals("B", DoricoPitchConverter.getInstance().shiftLetter(NOTE_C, -1));
        assertEquals(NOTE_C, DoricoPitchConverter.getInstance().shiftLetter(NOTE_C, 7));
        assertNull(DoricoPitchConverter.getInstance().shiftLetter(null, 1));
        assertEquals(2, DoricoPitchConverter.getInstance().intervalToFifths(2, 1));
        assertEquals(1, DoricoPitchConverter.getInstance().intervalToFifths(7, 4));
        assertEquals(3, DoricoPitchConverter.getInstance().intervalToFifths(9, 5));
        assertEquals(0, DoricoPitchConverter.getInstance().intervalToFifths(12, 7));
    }


    /**
     * Tests splitting lengths into notatable values aligned within the bar.
     */
    @Test
    void testSplitDurations() {
        // dotted half at the bar start
        assertPieces(DoricoDurationConverter.getInstance().split(Fraction.ZERO, Fraction.of(3, 1), true, 4), "half:1");
        // 11/4 at the bar start: half tied to a dotted eighth (as Dorico notates it)
        assertPieces(DoricoDurationConverter.getInstance().split(Fraction.ZERO, Fraction.of(11, 4), true, 4), "half:0", "eighth:1");
        // a quarter starting on an off-beat eighth is split into two eighths
        assertPieces(DoricoDurationConverter.getInstance().split(Fraction.of(1, 2), Fraction.of(1, 1), true, 2), "eighth:0", "eighth:0");
        // rests are never dotted
        assertPieces(DoricoDurationConverter.getInstance().split(Fraction.ZERO, Fraction.of(3, 2), false, 2), "quarter:0", "eighth:0");
        // a triplet eighth cannot be expressed with binary values and stays one piece
        assertPieces(DoricoDurationConverter.getInstance().split(Fraction.ZERO, Fraction.of(1, 3), true, 3), "eighth:0");
    }


    /**
     * Tests the Dorico note value indexes used by tempo and tuplet definitions.
     */
    @Test
    void testNoteValues() {
        assertEquals(QUARTER, DoricoDurationConverter.getInstance().noteValueToType(8));
        assertEquals(EIGHTH, DoricoDurationConverter.getInstance().noteValueToType(7));
        assertEquals("half", DoricoDurationConverter.getInstance().noteValueToType(9));
        assertEquals(QUARTER, DoricoDurationConverter.getInstance().noteValueToType(99));
        assertEquals(Fraction.of(3, 2), DoricoDurationConverter.getInstance().noteValueToLength(8, 1));
        assertEquals(Fraction.of(1, 2), DoricoDurationConverter.getInstance().noteValueToLength(7, 0));
    }


    /**
     * Verifies a pitch.
     *
     * @param pitch the pitch
     * @param step the expected step
     * @param alter the expected alteration (null for none)
     * @param octave the expected octave
     */
    private void assertPitch(PitchImpl pitch, String step, Double alter, int octave) {
        assertEquals(step, pitch.getStep());
        assertEquals(alter, pitch.getAlter());
        assertEquals(octave, pitch.getOctave());
    }


    /**
     * Verifies split pieces given as "type:dots".
     *
     * @param pieces the pieces
     * @param expected the expected pieces
     */
    private void assertPieces(List<DoricoDurationConverter.Piece> pieces, String... expected) {
        assertEquals(expected.length, pieces.size(), pieces.toString());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], pieces.get(i).getType() + ":" + pieces.get(i).getDots());
        }
    }
}
