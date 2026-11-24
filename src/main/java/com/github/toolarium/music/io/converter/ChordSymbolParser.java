/*
 * ChordSymbolParser.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.converter;

import com.github.toolarium.music.model.harmony.impl.BassImpl;
import com.github.toolarium.music.model.harmony.impl.DegreeImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.harmony.impl.RootImpl;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parses chord symbol text such as {@code Am7}, {@code F#m7b5} or {@code Bb/D} into a MusicXML harmony.
 *
 * @author patrick
 */
public final class ChordSymbolParser {
    private static final Pattern CHORD = Pattern.compile("^([A-Ga-g])([#♯]|b|♭)?([^/]*)(?:/([A-Ga-g])([#♯]|b|♭)?)?$");
    private static final Pattern ALTERATION = Pattern.compile("(add|[#b+\\-♯♭])(\\d+)");
    private static final Map<String, String> KINDS = createKinds();
    private static final String MAJOR = "major";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final ChordSymbolParser INSTANCE = new ChordSymbolParser();
    }


    /**
     * Constructor.
     */
    private ChordSymbolParser() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static ChordSymbolParser getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Parses a chord symbol.
     *
     * @param text the chord symbol text
     * @return the harmony, or null if the text is not a chord symbol
     */
    public HarmonyImpl parse(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        Matcher matcher = CHORD.matcher(trimmed);
        if (!matcher.matches()) {
            return null;
        }

        HarmonyImpl harmony = new HarmonyImpl();
        RootImpl root = new RootImpl();
        root.setRootStep(matcher.group(1).toUpperCase());
        int rootAlter = alterOf(matcher.group(2));
        if (rootAlter != 0) {
            root.setRootAlter((double) rootAlter);
        }
        harmony.setRoot(root);

        String suffix = matcher.group(3).trim();
        List<DegreeImpl> degrees = new ArrayList<>();
        String kind = parseKind(suffix, degrees);
        if (kind == null) {
            return null;
        }
        harmony.setKind(kind);
        if (!degrees.isEmpty()) {
            harmony.setDegrees(degrees);
        }

        if (matcher.group(4) != null) {
            BassImpl bass = new BassImpl();
            bass.setBassStep(matcher.group(4).toUpperCase());
            int bassAlter = alterOf(matcher.group(5));
            if (bassAlter != 0) {
                bass.setBassAlter((double) bassAlter);
            }
            harmony.setBass(bass);
        }
        return harmony;
    }


    /**
     * Parses the kind of a chord from its suffix. Added and altered degrees are collected.
     *
     * @param suffix the text after the root
     * @param degrees the degrees (filled)
     * @return the MusicXML kind, or null if the suffix is not a chord suffix
     */
    private String parseKind(String suffix, List<DegreeImpl> degrees) {
        if (suffix.isEmpty()) {
            return MAJOR;
        }
        for (Map.Entry<String, String> entry : KINDS.entrySet()) {
            if (suffix.startsWith(entry.getKey())) {
                String rest = suffix.substring(entry.getKey().length());
                if (!isDegreeList(rest)) {
                    continue;
                }
                addDegrees(rest, degrees);
                return entry.getValue();
            }
        }
        if (isDegreeList(suffix)) {
            addDegrees(suffix, degrees);
            return MAJOR;
        }
        return null;
    }


    /**
     * Checks if a text consists only of added and altered degrees (and brackets, commas and spaces).
     *
     * @param text the text
     * @return true if the text is a degree list or empty
     */
    private boolean isDegreeList(String text) {
        return ALTERATION.matcher(text).replaceAll("").replaceAll("[()\\[\\],\\s]", "").isEmpty();
    }


    /**
     * Adds the added and altered degrees of a chord suffix, for example {@code b9} or {@code add11}.
     *
     * @param text the remaining suffix
     * @param degrees the degrees (filled)
     */
    private void addDegrees(String text, List<DegreeImpl> degrees) {
        Matcher matcher = ALTERATION.matcher(text);
        while (matcher.find()) {
            DegreeImpl degree = new DegreeImpl();
            degree.setDegreeValue(matcher.group(2));
            String prefix = matcher.group(1);
            if ("add".equals(prefix)) {
                degree.setDegreeAlter("0");
                degree.setDegreeType("add");
            } else {
                degree.setDegreeAlter(String.valueOf(alterOf(prefix)));
                degree.setDegreeType("alter");
            }
            degrees.add(degree);
        }
    }


    /**
     * Gets the alteration of an accidental symbol.
     *
     * @param accidental the accidental (#, b, +, - or the Unicode signs), may be null
     * @return the alteration in semitones
     */
    private int alterOf(String accidental) {
        if (accidental == null) {
            return 0;
        }
        switch (accidental) {
            case "#":
            case "+":
            case "♯":
                return 1;
            case "b":
            case "-":
            case "♭":
                return -1;
            default:
                return 0;
        }
    }


    /**
     * Creates the chord suffixes and their MusicXML kinds. Longer suffixes come first.
     *
     * @return the map from suffix to kind
     */
    private static Map<String, String> createKinds() {
        Map<String, String> kinds = new LinkedHashMap<>();
        kinds.put("m7b5", "half-diminished");
        kinds.put("min7b5", "half-diminished");
        kinds.put("ø7", "half-diminished");
        kinds.put("ø", "half-diminished");
        kinds.put("mMaj7", "major-minor");
        kinds.put("mmaj7", "major-minor");
        kinds.put("mM7", "major-minor");
        kinds.put("maj13", "major-13th");
        kinds.put("maj11", "major-11th");
        kinds.put("maj9", "major-ninth");
        kinds.put("maj7", "major-seventh");
        kinds.put("Maj7", "major-seventh");
        kinds.put("M7", "major-seventh");
        kinds.put("Δ7", "major-seventh");
        kinds.put("Δ", "major-seventh");
        kinds.put("min13", "minor-13th");
        kinds.put("min11", "minor-11th");
        kinds.put("min9", "minor-ninth");
        kinds.put("min7", "minor-seventh");
        kinds.put("min6", "minor-sixth");
        kinds.put("min", "minor");
        kinds.put("m13", "minor-13th");
        kinds.put("m11", "minor-11th");
        kinds.put("m9", "minor-ninth");
        kinds.put("m7", "minor-seventh");
        kinds.put("m6", "minor-sixth");
        kinds.put("-7", "minor-seventh");
        kinds.put("dim7", "diminished-seventh");
        kinds.put("o7", "diminished-seventh");
        kinds.put("°7", "diminished-seventh");
        kinds.put("dim", "diminished");
        kinds.put("o", "diminished");
        kinds.put("°", "diminished");
        kinds.put("aug7", "augmented-seventh");
        kinds.put("+7", "augmented-seventh");
        kinds.put("aug", "augmented");
        kinds.put("+", "augmented");
        kinds.put("sus2", "suspended-second");
        kinds.put("sus4", "suspended-fourth");
        kinds.put("sus", "suspended-fourth");
        kinds.put("13", "dominant-13th");
        kinds.put("11", "dominant-11th");
        kinds.put("9", "dominant-ninth");
        kinds.put("7", "dominant");
        kinds.put("69", "major-sixth");
        kinds.put("6", "major-sixth");
        kinds.put("5", "power");
        kinds.put("maj", MAJOR);
        kinds.put("M", MAJOR);
        kinds.put("m", "minor");
        kinds.put("-", "minor");
        return kinds;
    }
}
