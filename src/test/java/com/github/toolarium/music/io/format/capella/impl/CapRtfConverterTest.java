/*
 * CapRtfConverterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


/**
 * Tests for the RTF to text conversion of capella text objects.
 *
 * @author patrick
 */
public class CapRtfConverterTest {
    private static final String TITLE = "Title";
    private static final String TEXT_RTF = "{\\rtf1\\ansi{\\fonttbl{\\f0\\fswiss Arial;}}{\\colortbl;\\red0\\green0\\blue0;}"
            + "\\f0\\fs36 Title\\par}";


    /**
     * Tests that missing and plain texts are returned unchanged (trimmed).
     */
    @Test
    void testPlainText() {
        CapRtfConverter converter = CapRtfConverter.getInstance();
        assertEquals("", converter.toText(null));
        assertEquals("Allegro", converter.toText("  Allegro \r\n"));
    }


    /**
     * Tests that the font and color tables and control words are removed.
     */
    @Test
    void testTablesAndControlWords() {
        assertEquals(TITLE, CapRtfConverter.getInstance().toText(TEXT_RTF));
    }


    /**
     * Tests that paragraph marks, line breaks and tabs become single spaces.
     */
    @Test
    void testParagraphs() {
        assertEquals("Words by Someone Music by Anyone",
                CapRtfConverter.getInstance().toText("{\\rtf1 Words by\\par Someone\\line Music\\tab by\r\n Anyone\\par\\par}"));
    }


    /**
     * Tests escaped characters: hexadecimal characters and escaped braces and backslashes.
     */
    @Test
    void testEscapedCharacters() {
        assertEquals("Grüße {a} \\b", CapRtfConverter.getInstance().toText("{\\rtf1 Gr\\'fc\\'dfe \\{a\\} \\\\b}"));
    }


    /**
     * Tests that ignorable destinations, the style sheet and the document info are skipped, including
     * the escaped characters inside them.
     */
    @Test
    void testSkippedGroups() {
        String rtf = "{\\rtf1{\\stylesheet{\\s0 Normal;}}{\\info{\\title Hidden\\'fc}}{\\*\\generator capella;}"
                + "\\uc1\\b\\i-1 Visible\\b0}";
        assertEquals("Visible", CapRtfConverter.getInstance().toText(rtf));
    }


    /**
     * Tests incomplete RTF: a trailing backslash and an unclosed group.
     */
    @Test
    void testIncompleteRtf() {
        assertEquals("Text", CapRtfConverter.getInstance().toText("{\\rtf1 Text\\"));
    }


    /**
     * Tests the font size in points from the first {@code \fs} control word (half points).
     */
    @Test
    void testFontSize() {
        CapRtfConverter converter = CapRtfConverter.getInstance();
        assertEquals(18, converter.fontSize(TEXT_RTF));
        assertEquals(0, converter.fontSize("{\\rtf1 no size}"));
        assertEquals(0, converter.fontSize(null));
    }
}
