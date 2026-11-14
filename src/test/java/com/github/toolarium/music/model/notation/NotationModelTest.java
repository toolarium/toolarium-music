/*
 * NotationModelTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.notation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.notation.impl.ArticulationsImpl;
import com.github.toolarium.music.model.notation.impl.DynamicsImpl;
import com.github.toolarium.music.model.notation.impl.FermataImpl;
import com.github.toolarium.music.model.notation.impl.GlissandoImpl;
import com.github.toolarium.music.model.notation.impl.OrnamentsImpl;
import com.github.toolarium.music.model.notation.impl.PlayingTechniqueImpl;
import com.github.toolarium.music.model.notation.impl.TupletImpl;
import org.junit.jupiter.api.Test;


/**
 * Tests the equals, hashCode, and toString contracts for model classes in the notation package.
 *
 * @author patrick
 */
public class NotationModelTest {

    /**
     * Tests the equals/hashCode/toString contract for ArticulationsImpl.
     */
    @Test
    void testArticulationsImplContract() {
        ArticulationsImpl a = new ArticulationsImpl();
        ArticulationsImpl b = new ArticulationsImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setAccent("accent");
        a.setStaccato("staccato");
        a.setTenuto("tenuto");
        assertFalse(a.equals(b));

        b.setAccent("accent");
        b.setStaccato("staccato");
        b.setTenuto("tenuto");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests ArticulationsImpl with additional fields.
     */
    @Test
    void testArticulationsImplAllFields() {
        ArticulationsImpl a = new ArticulationsImpl();
        a.setStrongAccent("strongAccent");
        a.setDetachedLegato("detachedLegato");
        a.setStaccatissimo("staccatissimo");
        a.setSpiccato("spiccato");
        a.setBreathMark("breathMark");
        a.setCaesura("caesura");
        a.setStress("stress");
        a.setUnstress("unstress");

        ArticulationsImpl b = new ArticulationsImpl();
        b.setStrongAccent("strongAccent");
        b.setDetachedLegato("detachedLegato");
        b.setStaccatissimo("staccatissimo");
        b.setSpiccato("spiccato");
        b.setBreathMark("breathMark");
        b.setCaesura("caesura");
        b.setStress("stress");
        b.setUnstress("unstress");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for DynamicsImpl.
     */
    @Test
    void testDynamicsImplContract() {
        DynamicsImpl a = new DynamicsImpl();
        DynamicsImpl b = new DynamicsImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setForte("forte");
        a.setPlacement("below");
        assertFalse(a.equals(b));

        b.setForte("forte");
        b.setPlacement("below");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests DynamicsImpl with piano-range dynamics.
     */
    @Test
    void testDynamicsImplPianoRange() {
        DynamicsImpl a = new DynamicsImpl();
        a.setPiano("piano");
        a.setPp("pp");
        a.setPpp("ppp");
        a.setMp("mp");

        DynamicsImpl b = new DynamicsImpl();
        b.setPiano("piano");
        b.setPp("pp");
        b.setPpp("ppp");
        b.setMp("mp");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that DynamicsImpl with different values are not equal.
     */
    @Test
    void testDynamicsImplInequality() {
        DynamicsImpl a = new DynamicsImpl();
        a.setForte("forte");

        DynamicsImpl b = new DynamicsImpl();
        b.setPiano("piano");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for FermataImpl.
     */
    @Test
    void testFermataImplContract() {
        FermataImpl a = new FermataImpl();
        FermataImpl b = new FermataImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setType("upright");
        a.setValue("normal");
        assertFalse(a.equals(b));

        b.setType("upright");
        b.setValue("normal");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for GlissandoImpl.
     */
    @Test
    void testGlissandoImplContract() {
        GlissandoImpl a = new GlissandoImpl();
        GlissandoImpl b = new GlissandoImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setType("start");
        a.setNumber(1);
        a.setLineType("solid");
        a.setValue("gliss.");
        assertFalse(a.equals(b));

        b.setType("start");
        b.setNumber(1);
        b.setLineType("solid");
        b.setValue("gliss.");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for OrnamentsImpl.
     */
    @Test
    void testOrnamentsImplContract() {
        OrnamentsImpl a = new OrnamentsImpl();
        OrnamentsImpl b = new OrnamentsImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setTrillMark("trillMark");
        a.setMordent("mordent");
        a.setTurn("turn");
        assertFalse(a.equals(b));

        b.setTrillMark("trillMark");
        b.setMordent("mordent");
        b.setTurn("turn");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for PlayingTechniqueImpl.
     */
    @Test
    void testPlayingTechniqueImplContract() {
        PlayingTechniqueImpl a = new PlayingTechniqueImpl();
        PlayingTechniqueImpl b = new PlayingTechniqueImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setUpBow("upBow");
        a.setDownBow("downBow");
        a.setHarmonic("harmonic");
        a.setSnapPizzicato("snapPizzicato");
        assertFalse(a.equals(b));

        b.setUpBow("upBow");
        b.setDownBow("downBow");
        b.setHarmonic("harmonic");
        b.setSnapPizzicato("snapPizzicato");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for TupletImpl.
     */
    @Test
    void testTupletImplContract() {
        TupletImpl a = new TupletImpl();
        TupletImpl b = new TupletImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setType("start");
        a.setNumber(1);
        a.setBracket("yes");
        a.setShowNumber("actual");
        a.setShowType("none");
        assertFalse(a.equals(b));

        b.setType("start");
        b.setNumber(1);
        b.setBracket("yes");
        b.setShowNumber("actual");
        b.setShowType("none");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that TupletImpl with different types are not equal.
     */
    @Test
    void testTupletImplInequality() {
        TupletImpl a = new TupletImpl();
        a.setType("start");

        TupletImpl b = new TupletImpl();
        b.setType("stop");

        assertNotEquals(a, b);
    }
}
