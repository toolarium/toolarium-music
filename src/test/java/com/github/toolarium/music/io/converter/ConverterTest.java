/*
 * ConverterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.SpelledPitch;
import com.github.toolarium.music.io.dto.TypeAndDots;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Tests for the shared converters: chord symbols, pitch spelling and note values.
 *
 * @author patrick
 */
public class ConverterTest {

    /**
     * Tests the chord symbol parser.
     */
    @Test
    void testChordSymbols() {
        ChordSymbolParser parser = ChordSymbolParser.getInstance();
        HarmonyImpl minorSeventh = parser.parse("F#m7b5/C");
        assertEquals("F", minorSeventh.getRoot().getRootStep());
        assertEquals(1.0, minorSeventh.getRoot().getRootAlter());
        assertEquals("half-diminished", minorSeventh.getKind());
        assertEquals("C", minorSeventh.getBass().getBassStep());
        assertEquals("dominant", parser.parse("Bb7").getKind());
        assertEquals(-1.0, parser.parse("Bb7").getRoot().getRootAlter());
        assertEquals("major-seventh", parser.parse("Cmaj7").getKind());
        assertEquals("suspended-fourth", parser.parse("Dsus4").getKind());
        assertEquals("major", parser.parse("G").getKind());
        assertEquals("add", parser.parse("Cadd9").getDegrees().get(0).getDegreeType());
        assertNull(parser.parse("dolce"));
    }


    /**
     * Tests the spelling of MIDI pitches in keys and the key alterations.
     */
    @Test
    void testPitchSpelling() {
        PitchSpeller speller = PitchSpeller.getInstance();
        SpelledPitch sharp = speller.spell(66, 2);
        assertEquals("F", sharp.getStep());
        assertEquals(1, sharp.getAlter());
        assertEquals(4, sharp.getOctave());
        SpelledPitch flat = speller.spell(70, -2);
        assertEquals("B", flat.getStep());
        assertEquals(-1, flat.getAlter());
        SpelledPitch flatC = speller.spell(59, -7);
        assertEquals("C", flatC.getStep());
        assertEquals(4, flatC.getOctave());
        assertEquals(60, speller.toMidi("C", 0, 4));
        assertEquals(1, speller.keyAlter("F", 1));
        assertEquals(0, speller.keyAlter("C", 1));
        assertEquals(-1, speller.keyAlter("E", -2));
        assertEquals(0, speller.keyAlter("A", -2));
    }


    /**
     * Tests the note value conversion.
     */
    @Test
    void testNoteValues() {
        NoteValueConverter converter = NoteValueConverter.getInstance();
        TypeAndDots dotted = converter.fromFraction(Fraction.of(3, 8));
        assertEquals("quarter", dotted.getType());
        assertEquals(1, dotted.getDots());
        assertNull(converter.fromFraction(Fraction.of(5, 8)));
        List<TypeAndDots> parts = converter.split(Fraction.of(5, 8));
        assertEquals("half", parts.get(0).getType());
        assertEquals("eighth", parts.get(1).getType());
        assertEquals("16th", converter.typeOfDenominator(16));
        assertEquals("breve", converter.typeOfDenominator(0));
        assertEquals(Fraction.of(7, 16), converter.toFraction("quarter", 2));
    }
}
