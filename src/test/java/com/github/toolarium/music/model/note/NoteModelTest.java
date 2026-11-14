/*
 * NoteModelTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.note;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.note.impl.BeamImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import com.github.toolarium.music.model.note.impl.RestImpl;
import com.github.toolarium.music.model.note.impl.TieImpl;
import com.github.toolarium.music.model.note.impl.TimeModificationImpl;
import com.github.toolarium.music.model.note.impl.UnpitchedImpl;
import org.junit.jupiter.api.Test;


/**
 * Tests the equals, hashCode, and toString contracts for model classes in the note package.
 *
 * @author patrick
 */
public class NoteModelTest {

    /**
     * Tests the equals/hashCode/toString contract for BeamImpl.
     */
    @Test
    void testBeamImplContract() {
        BeamImpl a = new BeamImpl();
        BeamImpl b = new BeamImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setNumber(1);
        a.setValue("begin");
        assertFalse(a.equals(b));

        b.setNumber(1);
        b.setValue("begin");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that BeamImpl with different values are not equal.
     */
    @Test
    void testBeamImplInequality() {
        BeamImpl a = new BeamImpl();
        a.setValue("begin");

        BeamImpl b = new BeamImpl();
        b.setValue("end");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for LyricImpl.
     */
    @Test
    void testLyricImplContract() {
        LyricImpl a = new LyricImpl();
        LyricImpl b = new LyricImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setNumber("1");
        a.setName("verse");
        a.setSyllabic("single");
        a.setText("La");
        assertFalse(a.equals(b));

        b.setNumber("1");
        b.setName("verse");
        b.setSyllabic("single");
        b.setText("La");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for RestImpl.
     */
    @Test
    void testRestImplContract() {
        RestImpl a = new RestImpl();
        RestImpl b = new RestImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setDisplayStep("B");
        a.setDisplayOctave(4);
        a.setMeasure("yes");
        assertFalse(a.equals(b));

        b.setDisplayStep("B");
        b.setDisplayOctave(4);
        b.setMeasure("yes");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for TieImpl.
     */
    @Test
    void testTieImplContract() {
        TieImpl a = new TieImpl();
        TieImpl b = new TieImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setType("start");
        assertFalse(a.equals(b));

        b.setType("start");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that TieImpl with different types are not equal.
     */
    @Test
    void testTieImplInequality() {
        TieImpl a = new TieImpl();
        a.setType("start");

        TieImpl b = new TieImpl();
        b.setType("stop");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for TimeModificationImpl.
     */
    @Test
    void testTimeModificationImplContract() {
        TimeModificationImpl a = new TimeModificationImpl();
        TimeModificationImpl b = new TimeModificationImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setActualNotes(3);
        a.setNormalNotes(2);
        a.setNormalType("eighth");
        assertFalse(a.equals(b));

        b.setActualNotes(3);
        b.setNormalNotes(2);
        b.setNormalType("eighth");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for UnpitchedImpl.
     */
    @Test
    void testUnpitchedImplContract() {
        UnpitchedImpl a = new UnpitchedImpl();
        UnpitchedImpl b = new UnpitchedImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setDisplayStep("E");
        a.setDisplayOctave(5);
        assertFalse(a.equals(b));

        b.setDisplayStep("E");
        b.setDisplayOctave(5);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that UnpitchedImpl with different steps are not equal.
     */
    @Test
    void testUnpitchedImplInequality() {
        UnpitchedImpl a = new UnpitchedImpl();
        a.setDisplayStep("E");

        UnpitchedImpl b = new UnpitchedImpl();
        b.setDisplayStep("F");

        assertNotEquals(a, b);
    }
}
