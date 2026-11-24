/*
 * LilyPondLyricsConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * Converts LilyPond lyrics: the text of a lyrics block is split into syllables, hyphens (--), extenders (__) and
 * skips (_), and the syllables are assigned to the notes of a voice. A note gets no syllable if it continues a tie
 * or lies inside a slur (a melisma), as in LilyPond.
 *
 * @author patrick
 */
public final class LilyPondLyricsConverter {
    /** The hyphen between two syllables of a word. */
    public static final String HYPHEN = "--";

    /** The extender after a syllable that lasts over several notes. */
    public static final String EXTENDER = "__";

    /** A skip: a note without syllable. */
    public static final String SKIP = "_";

    private static final String SINGLE = "single";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final LilyPondLyricsConverter INSTANCE = new LilyPondLyricsConverter();
    }


    /**
     * Constructor.
     */
    private LilyPondLyricsConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static LilyPondLyricsConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Splits the text of a lyrics block into items: syllables, hyphens, extenders and skips. Strings in quotes are
     * one syllable, commands (\set ...) and Scheme expressions are removed, a duration after a syllable (la4) too.
     *
     * @param text the text between the braces of the lyrics block
     * @return the items in order
     */
    public List<String> split(String text) {
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == '{' || c == '}') {
                i++;
            } else if (c == '"') {
                int end = text.indexOf('"', i + 1);
                if (end < 0) {
                    end = text.length();
                }
                result.add(text.substring(i + 1, end));
                i = end + 1;
            } else {
                int end = i;
                while (end < text.length() && !Character.isWhitespace(text.charAt(end)) && text.charAt(end) != '{' && text.charAt(end) != '}') {
                    end++;
                }
                String item = text.substring(i, end);
                i = end;
                if (item.startsWith("\\") || item.startsWith("#") || "=".equals(item) || item.startsWith("%")) {
                    continue;
                }
                if (!HYPHEN.equals(item) && !EXTENDER.equals(item) && !SKIP.equals(item)) {
                    item = item.replaceAll("(?<=\\D)\\d+\\.*$", "").replace('~', ' ').replace('_', ' ');
                }
                if (!item.isEmpty()) {
                    result.add(item);
                }
            }
        }
        return result;
    }


    /**
     * Assigns the syllables to the notes of a voice.
     *
     * @param voice the events of the voice in order
     * @param items the syllables, hyphens, extenders and skips
     * @param verse the verse number (1-based)
     */
    public void assign(List<LilyPondEvent> voice, List<String> items, int verse) {
        List<LilyPondEvent> notes = singableNotes(voice);
        int note = 0;
        boolean hyphenBefore = false;
        for (int i = 0; i < items.size() && note < notes.size(); i++) {
            String item = items.get(i);
            if (HYPHEN.equals(item) || EXTENDER.equals(item)) {
                continue;
            }
            if (SKIP.equals(item)) {
                note++;
                hyphenBefore = false;
                continue;
            }
            boolean hyphenAfter = i + 1 < items.size() && HYPHEN.equals(items.get(i + 1));
            boolean extend = i + 1 < items.size() && EXTENDER.equals(items.get(i + 1));
            notes.get(note).getLyrics().add(ScoreBuilder.getInstance().createLyric(verse, item, syllabic(hyphenBefore, hyphenAfter), extend));
            hyphenBefore = hyphenAfter;
            note++;
        }
    }


    /**
     * Gets the notes that get a syllable: no rests, no grace notes, no continued ties, no notes inside a slur.
     *
     * @param voice the events of the voice in order
     * @return the notes
     */
    private List<LilyPondEvent> singableNotes(List<LilyPondEvent> voice) {
        List<LilyPondEvent> result = new ArrayList<>();
        int slurDepth = 0;
        LilyPondEvent previous = null;
        for (LilyPondEvent event : voice) {
            if (event.isGrace()) {
                continue;
            }
            boolean tied = previous != null && previous.hasTie() && !event.isRest();
            if (!event.isRest() && !tied && slurDepth == 0) {
                result.add(event);
            }
            slurDepth = Math.max(0, slurDepth + event.getSlurStarts().size() - event.getSlurStops().size());
            previous = event;
        }
        return result;
    }


    /**
     * Gets the syllabic of a syllable from the hyphens around it.
     *
     * @param hyphenBefore true if a hyphen is before the syllable
     * @param hyphenAfter true if a hyphen is after the syllable
     * @return single, begin, middle or end
     */
    private String syllabic(boolean hyphenBefore, boolean hyphenAfter) {
        if (hyphenBefore && hyphenAfter) {
            return "middle";
        }
        if (hyphenAfter) {
            return "begin";
        }
        if (hyphenBefore) {
            return "end";
        }
        return SINGLE;
    }
}
