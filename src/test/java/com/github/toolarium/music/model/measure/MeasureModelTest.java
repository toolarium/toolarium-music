/*
 * MeasureModelTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.measure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.measure.impl.BackupImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.measure.impl.ForwardImpl;
import com.github.toolarium.music.model.measure.impl.PrintImpl;
import com.github.toolarium.music.model.measure.impl.StaffDetailsImpl;
import com.github.toolarium.music.model.measure.impl.TransposeImpl;
import org.junit.jupiter.api.Test;


/**
 * Tests the equals, hashCode, and toString contracts for remaining model classes
 * in the measure package.
 *
 * @author patrick
 */
public class MeasureModelTest {

    /**
     * Tests the equals/hashCode/toString contract for BackupImpl.
     */
    @Test
    void testBackupImplContract() {
        BackupImpl a = new BackupImpl();
        BackupImpl b = new BackupImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setDuration(4);
        assertFalse(a.equals(b));

        b.setDuration(4);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that BackupImpl with different durations are not equal.
     */
    @Test
    void testBackupImplInequality() {
        BackupImpl a = new BackupImpl();
        a.setDuration(4);

        BackupImpl b = new BackupImpl();
        b.setDuration(2);

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for ClefImpl.
     */
    @Test
    void testClefImplContract() {
        ClefImpl a = new ClefImpl();
        ClefImpl b = new ClefImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setNumber(1);
        a.setSign("G");
        a.setLine(2);
        assertFalse(a.equals(b));

        b.setNumber(1);
        b.setSign("G");
        b.setLine(2);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests ClefImpl with octave change.
     */
    @Test
    void testClefImplWithOctaveChange() {
        ClefImpl a = new ClefImpl();
        a.setSign("G");
        a.setLine(2);
        a.setClefOctaveChange(-1);

        ClefImpl b = new ClefImpl();
        b.setSign("G");
        b.setLine(2);
        b.setClefOctaveChange(-1);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that ClefImpl with different signs are not equal.
     */
    @Test
    void testClefImplInequality() {
        ClefImpl a = new ClefImpl();
        a.setSign("G");

        ClefImpl b = new ClefImpl();
        b.setSign("F");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for ForwardImpl.
     */
    @Test
    void testForwardImplContract() {
        ForwardImpl a = new ForwardImpl();
        ForwardImpl b = new ForwardImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setDuration(4);
        a.setVoice(2);
        a.setStaff(1);
        assertFalse(a.equals(b));

        b.setDuration(4);
        b.setVoice(2);
        b.setStaff(1);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for PrintImpl.
     */
    @Test
    void testPrintImplContract() {
        PrintImpl a = new PrintImpl();
        PrintImpl b = new PrintImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setNewSystem("yes");
        a.setNewPage("yes");
        a.setBlankPage(0);
        a.setPageNumber(1);
        assertFalse(a.equals(b));

        b.setNewSystem("yes");
        b.setNewPage("yes");
        b.setBlankPage(0);
        b.setPageNumber(1);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for StaffDetailsImpl.
     */
    @Test
    void testStaffDetailsImplContract() {
        StaffDetailsImpl a = new StaffDetailsImpl();
        StaffDetailsImpl b = new StaffDetailsImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setNumber(1);
        a.setStaffLines(5);
        a.setStaffType("regular");
        assertFalse(a.equals(b));

        b.setNumber(1);
        b.setStaffLines(5);
        b.setStaffType("regular");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for TransposeImpl.
     */
    @Test
    void testTransposeImplContract() {
        TransposeImpl a = new TransposeImpl();
        TransposeImpl b = new TransposeImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setDiatonic(-1);
        a.setChromatic(-2);
        a.setOctaveChange(0);
        assertFalse(a.equals(b));

        b.setDiatonic(-1);
        b.setChromatic(-2);
        b.setOctaveChange(0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that TransposeImpl with different chromatic values are not equal.
     */
    @Test
    void testTransposeImplInequality() {
        TransposeImpl a = new TransposeImpl();
        a.setChromatic(-2);

        TransposeImpl b = new TransposeImpl();
        b.setChromatic(3);

        assertNotEquals(a, b);
    }
}
