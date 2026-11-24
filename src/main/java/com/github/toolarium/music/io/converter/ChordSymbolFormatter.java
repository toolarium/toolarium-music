/*
 * ChordSymbolFormatter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.converter;

import com.github.toolarium.music.model.harmony.IHarmony;
import java.util.HashMap;
import java.util.Map;


/**
 * Formats a chord symbol of the model as text (for example "Cm7", "F#maj7/A", "Bbsus4"), the reverse of the
 * ChordSymbolParser. Formats that store chord symbols as text use it.
 *
 * @author patrick
 */
public final class ChordSymbolFormatter {
    private static final Map<String, String> SUFFIXES = createSuffixes();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final ChordSymbolFormatter INSTANCE = new ChordSymbolFormatter();
    }


    /**
     * Constructor.
     */
    private ChordSymbolFormatter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static ChordSymbolFormatter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Formats a chord symbol.
     *
     * @param harmony the chord symbol
     * @return the text, or null if the chord symbol has no root
     */
    public String format(IHarmony harmony) {
        if (harmony == null || harmony.getRoot() == null || harmony.getRoot().getRootStep() == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        text.append(harmony.getRoot().getRootStep().toUpperCase()).append(accidental(harmony.getRoot().getRootAlter()));
        String suffix = SUFFIXES.get(harmony.getKind());
        if (suffix != null) {
            text.append(suffix);
        }
        if (harmony.getBass() != null && harmony.getBass().getBassStep() != null) {
            text.append('/').append(harmony.getBass().getBassStep().toUpperCase()).append(accidental(harmony.getBass().getBassAlter()));
        }
        return text.toString();
    }


    /**
     * Gets the accidental of an alteration.
     *
     * @param alter the alteration in semitones, or null
     * @return "#", "b", "##", "bb" or an empty string
     */
    private String accidental(Double alter) {
        if (alter == null) {
            return "";
        }
        int value = (int) Math.round(alter);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.abs(value); i++) {
            if (value > 0) {
                result.append('#');
            } else {
                result.append('b');
            }
        }
        return result.toString();
    }


    /**
     * Creates the suffixes of the MusicXML chord kinds.
     *
     * @return the suffix by kind
     */
    private static Map<String, String> createSuffixes() {
        Map<String, String> result = new HashMap<>();
        result.put("major", "");
        result.put("minor", "m");
        result.put("augmented", "aug");
        result.put("diminished", "dim");
        result.put("dominant", "7");
        result.put("major-seventh", "maj7");
        result.put("minor-seventh", "m7");
        result.put("diminished-seventh", "dim7");
        result.put("augmented-seventh", "aug7");
        result.put("half-diminished", "m7b5");
        result.put("major-minor", "mMaj7");
        result.put("major-sixth", "6");
        result.put("minor-sixth", "m6");
        result.put("dominant-ninth", "9");
        result.put("major-ninth", "maj9");
        result.put("minor-ninth", "m9");
        result.put("dominant-11th", "11");
        result.put("major-11th", "maj11");
        result.put("minor-11th", "m11");
        result.put("dominant-13th", "13");
        result.put("major-13th", "maj13");
        result.put("minor-13th", "m13");
        result.put("suspended-second", "sus2");
        result.put("suspended-fourth", "sus4");
        result.put("power", "5");
        return result;
    }
}
