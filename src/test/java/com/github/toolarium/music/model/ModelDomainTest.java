/*
 * ModelDomainTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.barline.impl.EndingImpl;
import com.github.toolarium.music.model.barline.impl.RepeatImpl;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.harmony.impl.BassImpl;
import com.github.toolarium.music.model.harmony.impl.DegreeImpl;
import com.github.toolarium.music.model.harmony.impl.FrameImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.harmony.impl.RootImpl;
import com.github.toolarium.music.model.notation.impl.NotationsImpl;
import com.github.toolarium.music.model.notation.impl.SlurImpl;
import com.github.toolarium.music.model.notation.impl.TiedImpl;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Tests the equals, hashCode, and toString contracts for model classes
 * in the harmony, barline, direction, and notation packages.
 *
 * @author patrick
 */
public class ModelDomainTest {

    /**
     * Tests the equals/hashCode/toString contract for RootImpl.
     */
    @Test
    void testRootImplContract() {
        RootImpl a = new RootImpl();
        RootImpl b = new RootImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setRootStep("C");
        a.setRootAlter(-1.0);
        assertFalse(a.equals(b));

        b.setRootStep("C");
        b.setRootAlter(-1.0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        RootImpl c = new RootImpl();
        c.setRootStep("D");
        assertFalse(a.equals(c));
    }


    /**
     * Tests the equals/hashCode/toString contract for BassImpl.
     */
    @Test
    void testBassImplContract() {
        BassImpl a = new BassImpl();
        BassImpl b = new BassImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setBassStep("E");
        a.setBassAlter(1.0);
        assertFalse(a.equals(b));

        b.setBassStep("E");
        b.setBassAlter(1.0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        BassImpl c = new BassImpl();
        c.setBassStep("G");
        assertFalse(a.equals(c));
    }


    /**
     * Tests the equals/hashCode/toString contract for HarmonyImpl,
     * including nested RootImpl and BassImpl.
     */
    @Test
    void testHarmonyImplContract() {
        HarmonyImpl a = new HarmonyImpl();
        HarmonyImpl b = new HarmonyImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        RootImpl root = new RootImpl();
        root.setRootStep("C");
        BassImpl bass = new BassImpl();
        bass.setBassStep("G");

        a.setType("explicit");
        a.setKind("major");
        a.setRoot(root);
        a.setBass(bass);
        a.setInversion(1);
        a.setStaff(1);
        assertFalse(a.equals(b));

        RootImpl rootB = new RootImpl();
        rootB.setRootStep("C");
        BassImpl bassB = new BassImpl();
        bassB.setBassStep("G");

        b.setType("explicit");
        b.setKind("major");
        b.setRoot(rootB);
        b.setBass(bassB);
        b.setInversion(1);
        b.setStaff(1);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that HarmonyImpl with different kind values are not equal.
     */
    @Test
    void testHarmonyImplInequality() {
        HarmonyImpl a = new HarmonyImpl();
        a.setKind("major");

        HarmonyImpl b = new HarmonyImpl();
        b.setKind("minor");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for RepeatImpl.
     */
    @Test
    void testRepeatImplContract() {
        RepeatImpl a = new RepeatImpl();
        RepeatImpl b = new RepeatImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setDirection("forward");
        a.setTimes(2);
        assertFalse(a.equals(b));

        b.setDirection("forward");
        b.setTimes(2);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        RepeatImpl c = new RepeatImpl();
        c.setDirection("backward");
        assertFalse(a.equals(c));
    }


    /**
     * Tests the equals/hashCode/toString contract for BarlineImpl,
     * including nested RepeatImpl.
     */
    @Test
    void testBarlineImplContract() {
        BarlineImpl a = new BarlineImpl();
        BarlineImpl b = new BarlineImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        RepeatImpl repeat = new RepeatImpl();
        repeat.setDirection("forward");

        a.setLocation("left");
        a.setBarStyle("heavy-light");
        a.setRepeat(repeat);
        a.setSegno("segno1");
        a.setCoda("coda1");
        assertFalse(a.equals(b));

        RepeatImpl repeatB = new RepeatImpl();
        repeatB.setDirection("forward");

        b.setLocation("left");
        b.setBarStyle("heavy-light");
        b.setRepeat(repeatB);
        b.setSegno("segno1");
        b.setCoda("coda1");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that BarlineImpl with different bar styles are not equal.
     */
    @Test
    void testBarlineImplInequality() {
        BarlineImpl a = new BarlineImpl();
        a.setBarStyle("light-heavy");

        BarlineImpl b = new BarlineImpl();
        b.setBarStyle("heavy-light");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for SoundImpl.
     */
    @Test
    void testSoundImplContract() {
        SoundImpl a = new SoundImpl();
        SoundImpl b = new SoundImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setTempo("120");
        a.setDynamics("80");
        a.setDacapo("yes");
        a.setSegno("segno");
        a.setFine("yes");
        assertFalse(a.equals(b));

        b.setTempo("120");
        b.setDynamics("80");
        b.setDacapo("yes");
        b.setSegno("segno");
        b.setFine("yes");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        SoundImpl c = new SoundImpl();
        c.setTempo("60");
        assertFalse(a.equals(c));
    }


    /**
     * Tests the equals/hashCode/toString contract for DirectionImpl,
     * including a nested SoundImpl.
     */
    @Test
    void testDirectionImplContract() {
        DirectionImpl a = new DirectionImpl();
        DirectionImpl b = new DirectionImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        SoundImpl sound = new SoundImpl();
        sound.setTempo("120");

        a.setPlacement("above");
        a.setVoice(1);
        a.setStaff(1);
        a.setSound(sound);
        assertFalse(a.equals(b));

        SoundImpl soundB = new SoundImpl();
        soundB.setTempo("120");

        b.setPlacement("above");
        b.setVoice(1);
        b.setStaff(1);
        b.setSound(soundB);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that DirectionImpl instances with different placements are not equal.
     */
    @Test
    void testDirectionImplInequality() {
        DirectionImpl a = new DirectionImpl();
        a.setPlacement("above");

        DirectionImpl b = new DirectionImpl();
        b.setPlacement("below");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for NotationsImpl.
     */
    @Test
    void testNotationsImplContract() {
        NotationsImpl a = new NotationsImpl();
        NotationsImpl b = new NotationsImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        List<TiedImpl> tieds = new ArrayList<>();
        tieds.add(new TiedImpl());
        a.setTieds(tieds);
        assertFalse(a.equals(b));

        List<TiedImpl> tiedsB = new ArrayList<>();
        tiedsB.add(new TiedImpl());
        b.setTieds(tiedsB);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that NotationsImpl with different slur lists are not equal.
     */
    @Test
    void testNotationsImplWithSlurs() {
        NotationsImpl a = new NotationsImpl();
        List<SlurImpl> slurs = new ArrayList<>();
        slurs.add(new SlurImpl());
        a.setSlurs(slurs);

        NotationsImpl b = new NotationsImpl();
        assertNotEquals(a, b);

        List<SlurImpl> slursB = new ArrayList<>();
        slursB.add(new SlurImpl());
        b.setSlurs(slursB);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotNull(a.toString());
    }


    /**
     * Tests the equals/hashCode/toString contract for EndingImpl.
     */
    @Test
    void testEndingImplContract() {
        EndingImpl a = new EndingImpl();
        EndingImpl b = new EndingImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setNumber("1");
        a.setType("start");
        a.setValue("1.");
        assertFalse(a.equals(b));

        b.setNumber("1");
        b.setType("start");
        b.setValue("1.");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that EndingImpl with different numbers are not equal.
     */
    @Test
    void testEndingImplInequality() {
        EndingImpl a = new EndingImpl();
        a.setNumber("1");

        EndingImpl b = new EndingImpl();
        b.setNumber("2");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for DegreeImpl.
     */
    @Test
    void testDegreeImplContract() {
        DegreeImpl a = new DegreeImpl();
        DegreeImpl b = new DegreeImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setDegreeValue("5");
        a.setDegreeAlter("-1");
        a.setDegreeType("alter");
        assertFalse(a.equals(b));

        b.setDegreeValue("5");
        b.setDegreeAlter("-1");
        b.setDegreeType("alter");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for FrameImpl.
     */
    @Test
    void testFrameImplContract() {
        FrameImpl a = new FrameImpl();
        FrameImpl b = new FrameImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setFrameStrings(6);
        a.setFrameFrets(5);
        assertFalse(a.equals(b));

        b.setFrameStrings(6);
        b.setFrameFrets(5);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }
}
