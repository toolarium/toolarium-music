/*
 * AbcFieldParser.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl;

import com.github.toolarium.music.io.converter.PitchSpeller;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcKey;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcMeter;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcTempo;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parses the values of ABC information fields: key (K:), meter (M:), unit note length (L:),
 * tempo (Q:) and voice (V:) definitions with their clef properties.
 *
 * @author patrick
 */
final class AbcFieldParser {
    private static final Pattern KEY = Pattern.compile("^([A-Ga-g])([#b]?)\\s*([A-Za-z]*)(.*)$");
    private static final Pattern EXPLICIT_ACCIDENTAL = Pattern.compile("(\\^\\^|\\^|__|_|=)([A-Ga-g])");
    private static final Pattern PROPERTY = Pattern.compile("([A-Za-z]+)\\s*=\\s*(\"[^\"]*\"|\\S+)");
    private static final Pattern FRACTION = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern TEMPO_TEXT = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern NUMBER = Pattern.compile("^\\d+$");
    private static final Map<String, Integer> MODES = createModes();
    private static final int[] TONIC_FIFTHS = {0, 2, 4, -1, 1, 3, 5};
    private static final int FIFTHS_PER_SHARP = 7;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final AbcFieldParser INSTANCE = new AbcFieldParser();
    }


    /**
     * Constructor.
     */
    private AbcFieldParser() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    static AbcFieldParser getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Parses a key field value (K:), for example {@code G}, {@code F#m}, {@code Ddor} or {@code D exp ^f}.
     *
     * @param value the field value
     * @return the key
     */
    AbcKey parseKey(String value) {
        String text = stripProperties(value).trim();
        Map<String, Integer> explicit = new HashMap<>();
        Matcher accidentals = EXPLICIT_ACCIDENTAL.matcher(text);
        while (accidentals.find()) {
            explicit.put(accidentals.group(2).toUpperCase(Locale.ROOT), accidentalAlter(accidentals.group(1)));
        }
        if (text.isEmpty() || text.toLowerCase(Locale.ROOT).startsWith("none") || text.startsWith("^") || text.startsWith("_") || text.startsWith("=")) {
            return new AbcKey(0, null, explicit);
        }
        if (text.startsWith("HP")) {
            return new AbcKey(0, null, explicit);
        }
        if (text.startsWith("Hp")) {
            Map<String, Integer> pipes = new HashMap<>(explicit);
            pipes.put("F", 1);
            pipes.put("C", 1);
            return new AbcKey(2, null, pipes);
        }
        Matcher matcher = KEY.matcher(text);
        if (!matcher.matches()) {
            return new AbcKey(0, null, explicit);
        }
        int tonic = PitchSpeller.getInstance().stepIndex(matcher.group(1));
        int fifths = TONIC_FIFTHS[tonic];
        if ("#".equals(matcher.group(2))) {
            fifths += FIFTHS_PER_SHARP;
        } else if ("b".equals(matcher.group(2))) {
            fifths -= FIFTHS_PER_SHARP;
        }
        String modeText = matcher.group(3).toLowerCase(Locale.ROOT);
        String mode = "major";
        if (!modeText.isEmpty() && !"exp".equals(modeText)) {
            String prefix = modeText.substring(0, Math.min(3, modeText.length()));
            if ("m".equals(modeText) || "min".equals(prefix)) {
                prefix = "min";
            }
            Integer offset = MODES.get(prefix);
            if (offset != null) {
                fifths += offset;
                mode = modeName(prefix);
            }
        }
        if (modeText.startsWith("exp")) {
            return new AbcKey(0, null, explicit);
        }
        return new AbcKey(fifths, mode, explicit);
    }


    /**
     * Parses a meter field value (M:), for example {@code 6/8}, {@code C}, {@code C|}, {@code 2+3/8} or {@code none}.
     *
     * @param value the field value
     * @return the meter
     */
    AbcMeter parseMeter(String value) {
        String text = value.trim();
        if (text.isEmpty() || text.toLowerCase(Locale.ROOT).startsWith("none")) {
            return new AbcMeter(0, 4, null);
        }
        if ("C|".equals(text)) {
            return new AbcMeter(2, 2, "cut");
        }
        if ("C".equals(text)) {
            return new AbcMeter(4, 4, "common");
        }
        int slash = text.indexOf('/');
        if (slash < 0) {
            return new AbcMeter(0, 4, null);
        }
        String numerator = text.substring(0, slash).replace("(", "").replace(")", "").trim();
        String denominator = text.substring(slash + 1).trim();
        int beats = 0;
        for (String part : numerator.split("\\+")) {
            beats += parseInt(part.trim(), 0);
        }
        int beatType = parseInt(denominator, 4);
        if (beatType <= 0) {
            beatType = 4;
        }
        return new AbcMeter(beats, beatType, null);
    }


    /**
     * Parses a unit note length field value (L:), for example {@code 1/8}.
     *
     * @param value the field value
     * @return the unit length in whole notes, or null if invalid
     */
    Fraction parseLength(String value) {
        Matcher matcher = FRACTION.matcher(value);
        if (matcher.find()) {
            int numerator = parseInt(matcher.group(1), 1);
            int denominator = parseInt(matcher.group(2), 8);
            if (numerator > 0 && denominator > 0) {
                return Fraction.of(numerator, denominator);
            }
            return null;
        }
        if (NUMBER.matcher(value.trim()).matches() && parseInt(value.trim(), 0) > 0) {
            return Fraction.of(1, parseInt(value.trim(), 1));
        }
        return null;
    }


    /**
     * Parses a tempo field value (Q:), for example {@code 1/4=120}, {@code 120},
     * {@code "Allegro" 3/8=60} or {@code 1/8 3/8=50}.
     *
     * @param value the field value
     * @return the tempo
     */
    AbcTempo parseTempo(String value) {
        String text = null;
        String rest = value;
        Matcher textMatcher = TEMPO_TEXT.matcher(value);
        StringBuilder texts = new StringBuilder();
        while (textMatcher.find()) {
            if (texts.length() > 0) {
                texts.append(' ');
            }
            texts.append(textMatcher.group(1));
        }
        if (texts.length() > 0) {
            text = texts.toString();
            rest = TEMPO_TEXT.matcher(value).replaceAll(" ");
        }
        rest = rest.trim();
        int equals = rest.indexOf('=');
        if (equals < 0) {
            if (NUMBER.matcher(rest).matches()) {
                return new AbcTempo(null, parseInt(rest, 0), text);
            }
            return new AbcTempo(null, 0, text);
        }
        Fraction beat = Fraction.ZERO;
        Matcher beats = FRACTION.matcher(rest.substring(0, equals));
        while (beats.find()) {
            beat = beat.add(Fraction.of(parseInt(beats.group(1), 1), Math.max(1, parseInt(beats.group(2), 4))));
        }
        if (!beat.isPositive()) {
            beat = null;
        }
        return new AbcTempo(beat, parseInt(rest.substring(equals + 1).trim().split("\\s+")[0], 0), text);
    }


    /**
     * Parses the properties of a K: or V: field, for example {@code clef=bass name="Bass"}.
     *
     * @param value the field value
     * @return the properties by lower-case name, quotes removed
     */
    Map<String, String> parseProperties(String value) {
        Map<String, String> result = new LinkedHashMap<>();
        Matcher matcher = PROPERTY.matcher(value);
        while (matcher.find()) {
            String propertyValue = matcher.group(2);
            if (propertyValue.startsWith("\"") && propertyValue.endsWith("\"") && propertyValue.length() >= 2) {
                propertyValue = propertyValue.substring(1, propertyValue.length() - 1);
            }
            result.put(matcher.group(1).toLowerCase(Locale.ROOT), propertyValue);
        }
        return result;
    }


    /**
     * Finds the clef of a K: or V: field: the clef property or a clef name among the words.
     *
     * @param value the field value
     * @return the clef, or null if the field has none
     */
    ClefImpl parseClef(String value) {
        Map<String, String> properties = parseProperties(value);
        String name = properties.get("clef");
        if (name == null) {
            for (String word : stripProperties(value).trim().split("\\s+")) {
                if (word.length() > 1 && clefOf(word) != null) {
                    name = word;
                    break;
                }
            }
        }
        if (name == null) {
            return null;
        }
        ClefImpl clef = clefOf(name);
        if (clef == null) {
            return null;
        }
        String octave = properties.get("octave");
        if (octave != null && clef.getClefOctaveChange() == null) {
            int change = parseInt(octave.replace("+", ""), 0);
            if (change != 0) {
                clef.setClefOctaveChange(change);
            }
        }
        return clef;
    }


    /**
     * Gets the voice ID and the remaining text of a V: field.
     *
     * @param value the field value
     * @return the ID and the remaining text
     */
    String[] splitVoice(String value) {
        String text = value.trim();
        int space = 0;
        while (space < text.length() && !Character.isWhitespace(text.charAt(space))) {
            space++;
        }
        return new String[] {text.substring(0, space), text.substring(space).trim()};
    }


    /**
     * Parses the %%score or %%staves directive into parts. Each part is a list of staves and each
     * staff a list of voice IDs.
     *
     * @param value the directive value
     * @return the parts
     */
    List<List<List<String>>> parseScore(String value) {
        List<List<List<String>>> parts = new ArrayList<>();
        List<List<String>> brace = null;
        List<String> staff = null;
        StringBuilder id = new StringBuilder();
        String text = value + " ";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                id.append(c);
                continue;
            }
            if (id.length() > 0) {
                if (staff != null) {
                    staff.add(id.toString());
                } else if (brace != null) {
                    List<String> single = new ArrayList<>();
                    single.add(id.toString());
                    brace.add(single);
                } else {
                    List<List<String>> part = new ArrayList<>();
                    List<String> single = new ArrayList<>();
                    single.add(id.toString());
                    part.add(single);
                    parts.add(part);
                }
                id.setLength(0);
            }
            if (c == '{') {
                brace = new ArrayList<>();
            } else if (c == '}' && brace != null) {
                if (!brace.isEmpty()) {
                    parts.add(brace);
                }
                brace = null;
            } else if (c == '(') {
                staff = new ArrayList<>();
            } else if (c == ')' && staff != null) {
                if (!staff.isEmpty()) {
                    if (brace != null) {
                        brace.add(staff);
                    } else {
                        List<List<String>> part = new ArrayList<>();
                        part.add(staff);
                        parts.add(part);
                    }
                }
                staff = null;
            }
        }
        return parts;
    }


    /**
     * Gets the alteration of an ABC accidental.
     *
     * @param accidental the accidental (^^, ^, =, _ or __)
     * @return the alteration in semitones
     */
    int accidentalAlter(String accidental) {
        switch (accidental) {
            case "^^":
                return 2;
            case "^":
                return 1;
            case "_":
                return -1;
            case "__":
                return -2;
            default:
                return 0;
        }
    }


    /**
     * Parses an integer.
     *
     * @param text the text
     * @param defaultValue the value for invalid text
     * @return the integer
     */
    int parseInt(String text, int defaultValue) {
        if (text == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * Creates a clef from an ABC clef name, for example treble, bass, alto, tenor, bass3, treble-8 or perc.
     *
     * @param name the clef name
     * @return the clef, or null if the name is not a clef
     */
    private ClefImpl clefOf(String name) {
        String text = name.toLowerCase(Locale.ROOT);
        int octave = 0;
        if (text.endsWith("-8")) {
            octave = -1;
            text = text.substring(0, text.length() - 2);
        } else if (text.endsWith("+8")) {
            octave = 1;
            text = text.substring(0, text.length() - 2);
        }
        Integer line = null;
        if (!text.isEmpty() && Character.isDigit(text.charAt(text.length() - 1))) {
            line = text.charAt(text.length() - 1) - '0';
            text = text.substring(0, text.length() - 1);
        }
        ScoreBuilder builder = ScoreBuilder.getInstance();
        switch (text) {
            case "treble":
            case "g":
                return builder.createClef("G", valueOr(line, 2), octave);
            case "bass":
            case "f":
                return builder.createClef("F", valueOr(line, 4), octave);
            case "alto":
            case "c":
                return builder.createClef("C", valueOr(line, 3), octave);
            case "tenor":
                return builder.createClef("C", valueOr(line, 4), octave);
            case "baritone":
                return builder.createClef("F", valueOr(line, 3), octave);
            case "perc":
            case "percussion":
            case "drum":
                return builder.createClef("percussion", null, 0);
            case "tab":
                return builder.createClef("TAB", null, 0);
            default:
                return null;
        }
    }


    /**
     * Returns a value or a default.
     *
     * @param value the value (may be null)
     * @param defaultValue the default
     * @return the value or the default
     */
    private int valueOr(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }


    /**
     * Removes the name=value properties from a field value.
     *
     * @param value the field value
     * @return the value without properties
     */
    private String stripProperties(String value) {
        return PROPERTY.matcher(value).replaceAll(" ");
    }


    /**
     * Gets the MusicXML mode name of an ABC mode prefix.
     *
     * @param prefix the first three letters of the mode
     * @return the MusicXML mode
     */
    private String modeName(String prefix) {
        switch (prefix) {
            case "min":
            case "aeo":
                return "minor";
            case "mix":
                return "mixolydian";
            case "dor":
                return "dorian";
            case "phr":
                return "phrygian";
            case "lyd":
                return "lydian";
            case "loc":
                return "locrian";
            default:
                return "major";
        }
    }


    /**
     * Creates the fifths offsets of the modes relative to major.
     *
     * @return the map from mode prefix to offset
     */
    private static Map<String, Integer> createModes() {
        Map<String, Integer> modes = new HashMap<>();
        modes.put("maj", 0);
        modes.put("ion", 0);
        modes.put("mix", -1);
        modes.put("dor", -2);
        modes.put("min", -3);
        modes.put("aeo", -3);
        modes.put("phr", -4);
        modes.put("loc", -5);
        modes.put("lyd", 1);
        return modes;
    }
}
