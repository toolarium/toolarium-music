/*
 * DirectionModelTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.direction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.direction.impl.DirectionTypeImpl;
import com.github.toolarium.music.model.direction.impl.MetronomeImpl;
import com.github.toolarium.music.model.direction.impl.OctaveShiftImpl;
import com.github.toolarium.music.model.direction.impl.PedalImpl;
import com.github.toolarium.music.model.direction.impl.WedgeImpl;
import com.github.toolarium.music.model.direction.impl.WordsImpl;
import org.junit.jupiter.api.Test;


/**
 * Tests the equals, hashCode, and toString contracts for model classes in the direction package.
 *
 * @author patrick
 */
public class DirectionModelTest {

    private static final String YES = "yes";


    /**
     * Tests the equals/hashCode/toString contract for MetronomeImpl.
     */
    @Test
    void testMetronomeImplContract() {
        MetronomeImpl a = new MetronomeImpl();
        MetronomeImpl b = new MetronomeImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setBeatUnit("quarter");
        a.setBeatUnitDot("dot");
        a.setPerMinute("120");
        a.setParentheses(YES);
        assertFalse(a.equals(b));

        b.setBeatUnit("quarter");
        b.setBeatUnitDot("dot");
        b.setPerMinute("120");
        b.setParentheses(YES);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that MetronomeImpl with different tempos are not equal.
     */
    @Test
    void testMetronomeImplInequality() {
        MetronomeImpl a = new MetronomeImpl();
        a.setPerMinute("120");

        MetronomeImpl b = new MetronomeImpl();
        b.setPerMinute("60");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for OctaveShiftImpl.
     */
    @Test
    void testOctaveShiftImplContract() {
        OctaveShiftImpl a = new OctaveShiftImpl();
        OctaveShiftImpl b = new OctaveShiftImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setType("down");
        a.setSize(8);
        assertFalse(a.equals(b));

        b.setType("down");
        b.setSize(8);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for PedalImpl.
     */
    @Test
    void testPedalImplContract() {
        PedalImpl a = new PedalImpl();
        PedalImpl b = new PedalImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setType("start");
        a.setLine(YES);
        a.setSign(YES);
        assertFalse(a.equals(b));

        b.setType("start");
        b.setLine(YES);
        b.setSign(YES);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for WedgeImpl.
     */
    @Test
    void testWedgeImplContract() {
        WedgeImpl a = new WedgeImpl();
        WedgeImpl b = new WedgeImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setType("crescendo");
        a.setNumber(1);
        a.setSpread("0");
        assertFalse(a.equals(b));

        b.setType("crescendo");
        b.setNumber(1);
        b.setSpread("0");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that WedgeImpl with different types are not equal.
     */
    @Test
    void testWedgeImplInequality() {
        WedgeImpl a = new WedgeImpl();
        a.setType("crescendo");

        WedgeImpl b = new WedgeImpl();
        b.setType("diminuendo");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for WordsImpl.
     */
    @Test
    void testWordsImplContract() {
        WordsImpl a = new WordsImpl();
        WordsImpl b = new WordsImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setValue("dolce");
        a.setDefaultX("10");
        a.setDefaultY("20");
        a.setFontSize("12");
        a.setFontWeight("bold");
        a.setFontStyle("italic");
        assertFalse(a.equals(b));

        b.setValue("dolce");
        b.setDefaultX("10");
        b.setDefaultY("20");
        b.setFontSize("12");
        b.setFontWeight("bold");
        b.setFontStyle("italic");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for DirectionTypeImpl with nested objects.
     */
    @Test
    void testDirectionTypeImplContract() {
        DirectionTypeImpl a = new DirectionTypeImpl();
        DirectionTypeImpl b = new DirectionTypeImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        MetronomeImpl metronome = new MetronomeImpl();
        metronome.setBeatUnit("quarter");
        metronome.setPerMinute("120");

        a.setRehearsal("A");
        a.setSegno("segno");
        a.setCoda("coda");
        a.setMetronome(metronome);
        assertFalse(a.equals(b));

        MetronomeImpl metronomeB = new MetronomeImpl();
        metronomeB.setBeatUnit("quarter");
        metronomeB.setPerMinute("120");

        b.setRehearsal("A");
        b.setSegno("segno");
        b.setCoda("coda");
        b.setMetronome(metronomeB);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }
}
