/*
 * CapellaConverterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.SpelledPitch;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import org.junit.jupiter.api.Test;


/**
 * Tests for the capella value conversions: clefs, time signatures, durations, tuplets, pitches and articulations.
 *
 * @author patrick
 */
public class CapellaConverterTest {
    private static final String SIGN_C = "C";
    private static final String SIGN_F = "F";
    private static final String SIGN_G = "G";
    private static final String EMPTY = "";


    /**
     * Tests the named clefs.
     */
    @Test
    void testNamedClefs() {
        assertClef("treble", SIGN_G, 2, null);
        assertClef("bass", SIGN_F, 4, null);
        assertClef("alto", SIGN_C, 3, null);
        assertClef("tenor", SIGN_C, 4, null);
        assertClef("soprano", SIGN_C, 1, null);
        assertClef("mezzoSoprano", SIGN_C, 2, null);
        assertClef("baritone", SIGN_F, 3, null);

        ClefImpl percussion = CapellaConverter.getInstance().clefOf("percussion");
        assertEquals("percussion", percussion.getSign());
        assertNull(percussion.getLine());
    }


    /**
     * Tests clefs given as sign, line and octave, and invalid clefs.
     */
    @Test
    void testSignClefs() {
        assertClef("G2", SIGN_G, 2, null);
        assertClef("G2-", SIGN_G, 2, -1);
        assertClef("G2+", SIGN_G, 2, 1);
        assertClef("F4", SIGN_F, 4, null);
        assertClef("C3", SIGN_C, 3, null);

        CapellaConverter converter = CapellaConverter.getInstance();
        assertNull(converter.clefOf(null));
        assertNull(converter.clefOf("G6"));
        assertNull(converter.clefOf("unknown"));
    }


    /**
     * Tests the time signatures: numeric, common time, alla breve, free time and invalid values.
     */
    @Test
    void testTimeSignatures() {
        CapellaConverter converter = CapellaConverter.getInstance();
        assertArrayEquals(new int[] {3, 4, 0}, converter.timeOf("3/4"));
        assertArrayEquals(new int[] {6, 8, 0}, converter.timeOf(" 6 / 8 "));
        assertArrayEquals(new int[] {4, 4, 1}, converter.timeOf("C"));
        assertArrayEquals(new int[] {4, 4, 1}, converter.timeOf("common"));
        assertArrayEquals(new int[] {2, 2, 2}, converter.timeOf("allaBreve"));
        assertArrayEquals(new int[] {2, 2, 2}, converter.timeOf("C|"));
        assertArrayEquals(new int[] {0, 4, 0}, converter.timeOf("infinite"));

        assertNull(converter.timeOf(null));
        assertNull(converter.timeOf("4"));
        assertNull(converter.timeOf("/4"));
        assertNull(converter.timeOf("0/4"));
        assertNull(converter.timeOf("4/0"));
        assertNull(converter.timeOf("a/b"));
    }


    /**
     * Tests the duration bases.
     */
    @Test
    void testDurationBases() {
        CapellaConverter converter = CapellaConverter.getInstance();
        assertEquals(Fraction.of(1, 4), converter.baseOf("1/4"));
        assertEquals(Fraction.of(2, 1), converter.baseOf("2/1"));
        assertNull(converter.baseOf(null));
        assertNull(converter.baseOf("0/4"));
        assertNull(converter.baseOf("quarter"));
    }


    /**
     * Tests the normal notes of tuplets: duplets and triplets, tripartite and prolonged tuplets.
     */
    @Test
    void testTupletNormal() {
        CapellaConverter converter = CapellaConverter.getInstance();
        assertEquals(2, converter.tupletNormal(3, false, false));
        assertEquals(4, converter.tupletNormal(5, false, false));
        assertEquals(4, converter.tupletNormal(7, false, false));
        assertEquals(8, converter.tupletNormal(9, false, false));
        assertEquals(1, converter.tupletNormal(1, false, false));

        // tripartite: the largest 3 * 2^k below the count
        assertEquals(3, converter.tupletNormal(2, true, false));
        assertEquals(3, converter.tupletNormal(4, true, false));
        assertEquals(6, converter.tupletNormal(7, true, false));

        // prolonged: the next value above the count
        assertEquals(4, converter.tupletNormal(3, false, true));
        assertEquals(8, converter.tupletNormal(5, false, true));
        assertEquals(6, converter.tupletNormal(4, true, true));
    }


    /**
     * Tests the pitches: capella octave 5 is the MusicXML octave 4.
     */
    @Test
    void testPitches() {
        CapellaConverter converter = CapellaConverter.getInstance();
        SpelledPitch middleC = converter.pitchOf("C5");
        assertEquals("C", middleC.getStep());
        assertEquals(0, middleC.getAlter());
        assertEquals(4, middleC.getOctave());
        assertEquals("B", converter.pitchOf(" b3 ").getStep());
        assertEquals(2, converter.pitchOf("b3").getOctave());
        assertEquals(-2, converter.pitchOf("A-1").getOctave());

        assertNull(converter.pitchOf(null));
        assertNull(converter.pitchOf("H5"));
        assertNull(converter.pitchOf("C"));
    }


    /**
     * Tests the articulations.
     */
    @Test
    void testArticulations() {
        assertEquals(EMPTY, articulated("staccato").articulations().getStaccato());
        assertEquals(EMPTY, articulated("tenuto").articulations().getTenuto());
        assertEquals(EMPTY, articulated("staccTenuto").articulations().getDetachedLegato());
        assertEquals(EMPTY, articulated("portato").articulations().getDetachedLegato());
        assertEquals(EMPTY, articulated("staccatissimo").articulations().getStaccatissimo());
        assertEquals(EMPTY, articulated("normalAccent").articulations().getAccent());
        assertEquals(EMPTY, articulated("accent").articulations().getAccent());
        assertEquals(EMPTY, articulated("strongAccent").articulations().getStrongAccent());
        assertEquals(EMPTY, articulated("weakBeat").articulations().getUnstress());
        assertEquals(EMPTY, articulated("strongBeat").articulations().getStress());
        assertEquals(1, articulated("fermata").getFermatas().size());

        EventData unknown = articulated("unknown");
        assertNull(unknown.articulations().getStaccato());
        assertTrue(unknown.getFermatas().isEmpty());
        assertNotNull(articulated(null));
    }


    /**
     * Asserts a clef.
     *
     * @param name the capella clef name
     * @param sign the expected sign
     * @param line the expected line
     * @param octaveChange the expected octave change, or null
     */
    private void assertClef(String name, String sign, int line, Integer octaveChange) {
        ClefImpl clef = CapellaConverter.getInstance().clefOf(name);
        assertEquals(sign, clef.getSign(), name);
        assertEquals(line, clef.getLine(), name);
        assertEquals(octaveChange, clef.getClefOctaveChange(), name);
    }


    /**
     * Creates an event with one articulation applied.
     *
     * @param type the capella articulation type
     * @return the event
     */
    private EventData articulated(String type) {
        EventData event = new EventData();
        CapellaConverter.getInstance().applyArticulation(type, event);
        return event;
    }
}
