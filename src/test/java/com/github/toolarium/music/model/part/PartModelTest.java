/*
 * PartModelTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.part;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.part.impl.MidiDeviceImpl;
import com.github.toolarium.music.model.part.impl.MidiInstrumentImpl;
import com.github.toolarium.music.model.part.impl.PartGroupImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.part.impl.ScoreInstrumentImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Tests the equals, hashCode, and toString contracts for model classes in the part package.
 *
 * @author patrick
 */
public class PartModelTest {

    private static final String PIANO = "Piano";

    /**
     * Tests the equals/hashCode/toString contract for MidiDeviceImpl.
     */
    @Test
    void testMidiDeviceImplContract() {
        MidiDeviceImpl a = new MidiDeviceImpl();
        MidiDeviceImpl b = new MidiDeviceImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setId("device1");
        a.setPort(1);
        a.setValue("MIDI Out");
        assertFalse(a.equals(b));

        b.setId("device1");
        b.setPort(1);
        b.setValue("MIDI Out");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for MidiInstrumentImpl.
     */
    @Test
    void testMidiInstrumentImplContract() {
        MidiInstrumentImpl a = new MidiInstrumentImpl();
        MidiInstrumentImpl b = new MidiInstrumentImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setId("P1-I1");
        a.setMidiChannel(1);
        a.setMidiName("Acoustic Grand Piano");
        a.setMidiBank(0);
        a.setMidiProgram(1);
        a.setVolume(80.0);
        a.setPan(0.0);
        assertFalse(a.equals(b));

        b.setId("P1-I1");
        b.setMidiChannel(1);
        b.setMidiName("Acoustic Grand Piano");
        b.setMidiBank(0);
        b.setMidiProgram(1);
        b.setVolume(80.0);
        b.setPan(0.0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that MidiInstrumentImpl with different channels are not equal.
     */
    @Test
    void testMidiInstrumentImplInequality() {
        MidiInstrumentImpl a = new MidiInstrumentImpl();
        a.setMidiChannel(1);

        MidiInstrumentImpl b = new MidiInstrumentImpl();
        b.setMidiChannel(2);

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for PartGroupImpl.
     */
    @Test
    void testPartGroupImplContract() {
        PartGroupImpl a = new PartGroupImpl();
        PartGroupImpl b = new PartGroupImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setType("start");
        a.setNumber(1);
        a.setGroupName("Strings");
        a.setGroupAbbreviation("Str.");
        a.setGroupSymbol("bracket");
        a.setGroupBarline("yes");
        assertFalse(a.equals(b));

        b.setType("start");
        b.setNumber(1);
        b.setGroupName("Strings");
        b.setGroupAbbreviation("Str.");
        b.setGroupSymbol("bracket");
        b.setGroupBarline("yes");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for ScoreInstrumentImpl.
     */
    @Test
    void testScoreInstrumentImplContract() {
        ScoreInstrumentImpl a = new ScoreInstrumentImpl();
        ScoreInstrumentImpl b = new ScoreInstrumentImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setId("P1-I1");
        a.setInstrumentName(PIANO);
        a.setInstrumentAbbreviation("Pno.");
        a.setInstrumentSound("keyboard.piano");
        assertFalse(a.equals(b));

        b.setId("P1-I1");
        b.setInstrumentName(PIANO);
        b.setInstrumentAbbreviation("Pno.");
        b.setInstrumentSound("keyboard.piano");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for ScorePartImpl with nested lists.
     */
    @Test
    void testScorePartImplContract() {
        ScorePartImpl a = new ScorePartImpl();
        ScorePartImpl b = new ScorePartImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setId("P1");
        a.setPartName(PIANO);
        a.setPartAbbreviation("Pno.");
        assertFalse(a.equals(b));

        b.setId("P1");
        b.setPartName(PIANO);
        b.setPartAbbreviation("Pno.");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for PartListImpl.
     */
    @Test
    void testPartListImplContract() {
        PartListImpl a = new PartListImpl();
        PartListImpl b = new PartListImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        List<ScorePartImpl> parts = new ArrayList<>();
        ScorePartImpl sp = new ScorePartImpl();
        sp.setId("P1");
        sp.setPartName(PIANO);
        parts.add(sp);
        a.setScoreParts(parts);
        assertFalse(a.equals(b));

        List<ScorePartImpl> partsB = new ArrayList<>();
        ScorePartImpl spB = new ScorePartImpl();
        spB.setId("P1");
        spB.setPartName(PIANO);
        partsB.add(spB);
        b.setScoreParts(partsB);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that ScorePartImpl with different ids are not equal.
     */
    @Test
    void testScorePartImplInequality() {
        ScorePartImpl a = new ScorePartImpl();
        a.setId("P1");

        ScorePartImpl b = new ScorePartImpl();
        b.setId("P2");

        assertNotEquals(a, b);
    }
}
