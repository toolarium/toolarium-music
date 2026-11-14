/*
 * ScoreModelTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.score.impl.CreatorImpl;
import com.github.toolarium.music.model.score.impl.CreditImpl;
import com.github.toolarium.music.model.score.impl.CreditWordsImpl;
import com.github.toolarium.music.model.score.impl.DefaultsImpl;
import com.github.toolarium.music.model.score.impl.EncodingImpl;
import com.github.toolarium.music.model.score.impl.IdentificationImpl;
import com.github.toolarium.music.model.score.impl.ScalingImpl;
import com.github.toolarium.music.model.score.impl.WorkImpl;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Tests the equals, hashCode, and toString contracts for model classes in the score package.
 *
 * @author patrick
 */
public class ScoreModelTest {

    /**
     * Tests the equals/hashCode/toString contract for CreatorImpl.
     */
    @Test
    void testCreatorImplContract() {
        CreatorImpl a = new CreatorImpl();
        CreatorImpl b = new CreatorImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setType("composer");
        a.setValue("J.S. Bach");
        assertFalse(a.equals(b));

        b.setType("composer");
        b.setValue("J.S. Bach");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that CreatorImpl with different types are not equal.
     */
    @Test
    void testCreatorImplInequality() {
        CreatorImpl a = new CreatorImpl();
        a.setType("composer");

        CreatorImpl b = new CreatorImpl();
        b.setType("lyricist");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for CreditWordsImpl.
     */
    @Test
    void testCreditWordsImplContract() {
        CreditWordsImpl a = new CreditWordsImpl();
        CreditWordsImpl b = new CreditWordsImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setValue("Title");
        a.setDefaultX("100");
        a.setDefaultY("200");
        a.setFontSize("24");
        a.setJustify("center");
        a.setValign("top");
        assertFalse(a.equals(b));

        b.setValue("Title");
        b.setDefaultX("100");
        b.setDefaultY("200");
        b.setFontSize("24");
        b.setJustify("center");
        b.setValign("top");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for CreditImpl with nested CreditWordsImpl.
     */
    @Test
    void testCreditImplContract() {
        CreditImpl a = new CreditImpl();
        CreditImpl b = new CreditImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        CreditWordsImpl cw = new CreditWordsImpl();
        cw.setValue("Title");
        a.setPage(1);
        a.setCreditType("title");
        a.setCreditWords(cw);
        assertFalse(a.equals(b));

        CreditWordsImpl cwB = new CreditWordsImpl();
        cwB.setValue("Title");
        b.setPage(1);
        b.setCreditType("title");
        b.setCreditWords(cwB);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for ScalingImpl.
     */
    @Test
    void testScalingImplContract() {
        ScalingImpl a = new ScalingImpl();
        ScalingImpl b = new ScalingImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setMillimeters(7.2319);
        a.setTenths(40.0);
        assertFalse(a.equals(b));

        b.setMillimeters(7.2319);
        b.setTenths(40.0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for DefaultsImpl.
     */
    @Test
    void testDefaultsImplContract() {
        DefaultsImpl a = new DefaultsImpl();
        DefaultsImpl b = new DefaultsImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        ScalingImpl scaling = new ScalingImpl();
        scaling.setMillimeters(7.0);
        scaling.setTenths(40.0);
        a.setScaling(scaling);
        assertFalse(a.equals(b));

        ScalingImpl scalingB = new ScalingImpl();
        scalingB.setMillimeters(7.0);
        scalingB.setTenths(40.0);
        b.setScaling(scalingB);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for WorkImpl.
     */
    @Test
    void testWorkImplContract() {
        WorkImpl a = new WorkImpl();
        WorkImpl b = new WorkImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        a.setWorkNumber("BWV 846");
        a.setWorkTitle("Prelude in C Major");
        assertFalse(a.equals(b));

        b.setWorkNumber("BWV 846");
        b.setWorkTitle("Prelude in C Major");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests that WorkImpl with different titles are not equal.
     */
    @Test
    void testWorkImplInequality() {
        WorkImpl a = new WorkImpl();
        a.setWorkTitle("Prelude");

        WorkImpl b = new WorkImpl();
        b.setWorkTitle("Fugue");

        assertNotEquals(a, b);
    }


    /**
     * Tests the equals/hashCode/toString contract for EncodingImpl.
     */
    @Test
    void testEncodingImplContract() {
        EncodingImpl a = new EncodingImpl();
        EncodingImpl b = new EncodingImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        List<String> software = new ArrayList<>();
        software.add("MuseScore 4.0");
        a.setEncodingDate("2024-01-15");
        a.setSoftware(software);
        a.setEncodingDescription("Standard export");
        assertFalse(a.equals(b));

        List<String> softwareB = new ArrayList<>();
        softwareB.add("MuseScore 4.0");
        b.setEncodingDate("2024-01-15");
        b.setSoftware(softwareB);
        b.setEncodingDescription("Standard export");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for IdentificationImpl with nested objects.
     */
    @Test
    void testIdentificationImplContract() {
        IdentificationImpl a = new IdentificationImpl();
        IdentificationImpl b = new IdentificationImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());

        CreatorImpl creator = new CreatorImpl();
        creator.setType("composer");
        creator.setValue("Mozart");
        List<CreatorImpl> creators = new ArrayList<>();
        creators.add(creator);

        EncodingImpl encoding = new EncodingImpl();
        encoding.setEncodingDate("2024-01-15");

        a.setCreators(creators);
        a.setRights("Copyright 2024");
        a.setEncoding(encoding);
        a.setSource("Breitkopf");
        assertFalse(a.equals(b));

        CreatorImpl creatorB = new CreatorImpl();
        creatorB.setType("composer");
        creatorB.setValue("Mozart");
        List<CreatorImpl> creatorsB = new ArrayList<>();
        creatorsB.add(creatorB);

        EncodingImpl encodingB = new EncodingImpl();
        encodingB.setEncodingDate("2024-01-15");

        b.setCreators(creatorsB);
        b.setRights("Copyright 2024");
        b.setEncoding(encodingB);
        b.setSource("Breitkopf");
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }
}
