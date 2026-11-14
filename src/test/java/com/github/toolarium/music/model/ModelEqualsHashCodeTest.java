/*
 * ModelEqualsHashCodeTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import com.github.toolarium.music.model.note.impl.PitchImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import org.junit.jupiter.api.Test;


/**
 * Tests the equals, hashCode, and toString contracts on the most important model impl classes.
 *
 * @author patrick
 */
public class ModelEqualsHashCodeTest {

    /**
     * Tests the equals/hashCode/toString contract for ScoreImpl.
     */
    @Test
    void testScoreImplContract() {
        ScoreImpl a = new ScoreImpl();
        ScoreImpl b = new ScoreImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());
    }


    /**
     * Tests the equals/hashCode/toString contract for PartImpl.
     */
    @Test
    void testPartImplContract() {
        PartImpl a = new PartImpl();
        PartImpl b = new PartImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());
    }


    /**
     * Tests the equals/hashCode/toString contract for MeasureImpl.
     */
    @Test
    void testMeasureImplContract() {
        MeasureImpl a = new MeasureImpl();
        MeasureImpl b = new MeasureImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());
    }


    /**
     * Tests the equals/hashCode/toString contract for NoteImpl with populated fields.
     */
    @Test
    void testNoteImplContract() {
        PitchImpl pitchA = new PitchImpl();
        pitchA.setStep("C");
        pitchA.setOctave(4);
        NoteImpl a = new NoteImpl();
        a.setPitch(pitchA);
        a.setDuration(1);

        PitchImpl pitchB = new PitchImpl();
        pitchB.setStep("C");
        pitchB.setOctave(4);
        NoteImpl b = new NoteImpl();
        b.setPitch(pitchB);
        b.setDuration(1);

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());
    }


    /**
     * Tests the equals/hashCode/toString contract for PitchImpl with populated fields.
     */
    @Test
    void testPitchImplContract() {
        PitchImpl a = new PitchImpl();
        a.setStep("C");
        a.setOctave(4);

        PitchImpl b = new PitchImpl();
        b.setStep("C");
        b.setOctave(4);

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        PitchImpl c = new PitchImpl();
        c.setStep("D");
        c.setOctave(4);
        assertFalse(a.equals(c));
    }


    /**
     * Tests the equals/hashCode/toString contract for AttributesImpl.
     */
    @Test
    void testAttributesImplContract() {
        AttributesImpl a = new AttributesImpl();
        AttributesImpl b = new AttributesImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());
    }


    /**
     * Tests the equals/hashCode/toString contract for KeyImpl with populated fields.
     */
    @Test
    void testKeyImplContract() {
        KeyImpl a = new KeyImpl();
        a.setFifths(0);
        a.setMode("major");

        KeyImpl b = new KeyImpl();
        b.setFifths(0);
        b.setMode("major");

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        KeyImpl c = new KeyImpl();
        c.setFifths(2);
        c.setMode("major");
        assertFalse(a.equals(c));
    }


    /**
     * Tests the equals/hashCode/toString contract for TimeImpl with populated fields.
     */
    @Test
    void testTimeImplContract() {
        TimeImpl a = new TimeImpl();
        a.setBeats("4");
        a.setBeatType("4");

        TimeImpl b = new TimeImpl();
        b.setBeats("4");
        b.setBeatType("4");

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        TimeImpl c = new TimeImpl();
        c.setBeats("3");
        c.setBeatType("4");
        assertFalse(a.equals(c));
    }
}
