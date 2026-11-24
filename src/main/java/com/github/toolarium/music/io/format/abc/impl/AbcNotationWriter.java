/*
 * AbcNotationWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl;

import com.github.toolarium.music.io.converter.ChordSymbolFormatter;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.direction.IMetronome;
import com.github.toolarium.music.model.harmony.IHarmony;
import com.github.toolarium.music.model.measure.IClef;
import com.github.toolarium.music.model.notation.IArticulations;
import com.github.toolarium.music.model.notation.IDynamics;
import com.github.toolarium.music.model.notation.INotations;
import com.github.toolarium.music.model.notation.IOrnaments;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Converts notation of the model into ABC 2.1 text: pitches, accidentals, lengths, keys, clefs, decorations,
 * tempo fields, chord symbols and annotations.
 *
 * @author patrick
 */
public final class AbcNotationWriter {
    /** The unit note length (L:) of the written tunes. */
    public static final Fraction UNIT = Fraction.of(1, 8);

    private static final String STEPS = "CDEFGAB";
    private static final int[] STEP_SEMITONES = {0, 2, 4, 5, 7, 9, 11};
    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11};
    private static final String[] MODES = {"major", "dorian", "phrygian", "lydian", "mixolydian", "minor", "locrian"};
    private static final String[] MODE_SUFFIXES = {"", "dor", "phr", "lyd", "mix", "m", "loc"};
    private static final String QUOTE = "\"";
    private static final String BANG = "!";
    private static final int MIDDLE_C_OCTAVE = 4;
    private static final Map<String, String> TYPES = createTypes();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final AbcNotationWriter INSTANCE = new AbcNotationWriter();
    }


    /**
     * Constructor.
     */
    private AbcNotationWriter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static AbcNotationWriter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the ABC note letter with octave marks (C is middle C, c one octave higher).
     *
     * @param step the step (C to B)
     * @param octave the MusicXML octave
     * @return the note, for example C, c' or A,,
     */
    public String note(String step, int octave) {
        StringBuilder result = new StringBuilder();
        if (octave > MIDDLE_C_OCTAVE) {
            result.append(step.toLowerCase());
            for (int i = MIDDLE_C_OCTAVE + 1; i < octave; i++) {
                result.append('\'');
            }
        } else {
            result.append(step.toUpperCase());
            for (int i = octave; i < MIDDLE_C_OCTAVE; i++) {
                result.append(',');
            }
        }
        return result.toString();
    }


    /**
     * Gets the ABC accidental of an alteration.
     *
     * @param alter the alteration in semitones
     * @return ^, ^^, _, __ or =
     */
    public String accidental(int alter) {
        switch (alter) {
            case 1:
                return "^";
            case 2:
                return "^^";
            case -1:
                return "_";
            case -2:
                return "__";
            default:
                return "=";
        }
    }


    /**
     * Gets the length of a note as a multiple of the unit note length (2, /2, 3/2 or empty for the unit).
     *
     * @param length the length in whole notes
     * @return the length text
     */
    public String length(Fraction length) {
        Fraction factor = Fraction.of(length.getNumerator() * UNIT.getDenominator(), length.getDenominator() * UNIT.getNumerator());
        long numerator = factor.getNumerator();
        long denominator = factor.getDenominator();
        if (numerator == denominator) {
            return "";
        }
        if (denominator == 1) {
            return String.valueOf(numerator);
        }
        if (numerator == 1) {
            return "/" + denominator;
        }
        return numerator + "/" + denominator;
    }


    /**
     * Gets the written length of a note type with dots.
     *
     * @param type the MusicXML note type
     * @param dots the dots
     * @return the length in whole notes, or null if the type is unknown
     */
    public Fraction typeLength(String type, int dots) {
        Fraction base = null;
        if (type != null) {
            String value = TYPES.get(type);
            if (value != null) {
                String[] parts = value.split("/");
                base = Fraction.of(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
            }
        }
        if (base == null) {
            return null;
        }
        Fraction result = base;
        Fraction add = base;
        for (int i = 0; i < dots; i++) {
            add = add.multiply(1, 2);
            result = result.add(add);
        }
        return result;
    }


    /**
     * Gets the key of a key signature (for example D, Bbm, Gmix).
     *
     * @param fifths the fifths (positive for sharps)
     * @param mode the MusicXML mode, or null for major
     * @return the key
     */
    public String key(int fifths, String mode) {
        int degree = 0;
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equals(mode) || "aeolian".equals(mode) && i == 5 || "ionian".equals(mode) && i == 0) {
                degree = i;
            }
        }
        int majorStep = Math.floorMod(fifths * 4, 7);
        int majorSemitone = Math.floorMod(fifths * 7, 12);
        int step = (majorStep + degree) % 7;
        int semitone = (majorSemitone + MAJOR_SCALE[degree]) % 12;
        int alter = semitone - STEP_SEMITONES[step];
        if (alter > 6) {
            alter -= 12;
        } else if (alter < -6) {
            alter += 12;
        }
        StringBuilder result = new StringBuilder().append(STEPS.charAt(step));
        if (alter > 0) {
            result.append('#');
        } else if (alter < 0) {
            result.append('b');
        }
        return result.append(MODE_SUFFIXES[degree]).toString();
    }


    /**
     * Gets the ABC clef name of a clef (for example treble, bass, alto, tenor, treble-8).
     *
     * @param clef the clef, or null for a treble clef
     * @return the clef name
     */
    public String clef(IClef clef) {
        if (clef == null || clef.getSign() == null) {
            return "treble";
        }
        String sign = clef.getSign();
        int line = 0;
        if (clef.getLine() != null) {
            line = clef.getLine();
        }
        String name;
        if ("percussion".equals(sign)) {
            return "perc";
        } else if ("TAB".equalsIgnoreCase(sign)) {
            return "tab";
        } else if ("F".equals(sign)) {
            name = "bass";
            if (line != 0 && line != 4) {
                name = "bass" + line;
            }
        } else if ("C".equals(sign)) {
            name = "alto";
            if (line == 4) {
                name = "tenor";
            } else if (line != 0 && line != 3) {
                name = "alto" + line;
            }
        } else {
            name = "treble";
            if (line != 0 && line != 2) {
                name = "treble" + line;
            }
        }
        if (clef.getClefOctaveChange() != null && clef.getClefOctaveChange() < 0) {
            return name + "-8";
        }
        if (clef.getClefOctaveChange() != null && clef.getClefOctaveChange() > 0) {
            return name + "+8";
        }
        return name;
    }


    /**
     * Gets the decorations of the articulations, fermata, ornaments and fingering of a note.
     *
     * @param notations the notations, or null
     * @return the decorations (for example !staccato!)
     */
    public List<String> decorations(INotations notations) {
        List<String> result = new ArrayList<>();
        if (notations == null) {
            return result;
        }
        IArticulations articulations = notations.getArticulations();
        if (articulations != null) {
            addIf(result, articulations.getAccent(), "accent");
            addIf(result, articulations.getStrongAccent(), "marcato");
            addIf(result, articulations.getStaccato(), "staccato");
            addIf(result, articulations.getTenuto(), "tenuto");
            if (articulations.getDetachedLegato() != null) {
                addOnce(result, "tenuto");
                addOnce(result, "staccato");
            }
            addIf(result, articulations.getStaccatissimo(), "wedge");
            addIf(result, articulations.getBreathMark(), "breath");
            addIf(result, articulations.getCaesura(), "caesura");
        }
        if (notations.getFermatas() != null && !notations.getFermatas().isEmpty()) {
            if ("inverted".equals(notations.getFermatas().get(0).getType())) {
                result.add(decoration("invertedfermata"));
            } else {
                result.add(decoration("fermata"));
            }
        }
        IOrnaments ornaments = notations.getOrnaments();
        if (ornaments != null) {
            addIf(result, ornaments.getTrillMark(), "trill");
            addIf(result, ornaments.getMordent(), "lowermordent");
            addIf(result, ornaments.getInvertedMordent(), "uppermordent");
            addIf(result, ornaments.getTurn(), "turn");
            addIf(result, ornaments.getInvertedTurn(), "invertedturn");
        }
        if (notations.getTechnical() != null && notations.getTechnical().getFingering() != null
                && notations.getTechnical().getFingering().trim().matches("[0-5]")) {
            result.add(decoration(notations.getTechnical().getFingering().trim()));
        }
        String dynamics = dynamics(notations.getDynamics());
        if (dynamics != null) {
            result.add(dynamics);
        }
        return result;
    }


    /**
     * Gets the decoration of dynamics (for example !mf!).
     *
     * @param dynamics the dynamics, or null
     * @return the decoration, or null
     */
    public String dynamics(IDynamics dynamics) {
        if (dynamics == null) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put("pppp", dynamics.getPppp());
        values.put("ppp", dynamics.getPpp());
        values.put("pp", dynamics.getPp());
        values.put("p", dynamics.getPiano());
        values.put("mp", dynamics.getMp());
        values.put("mf", dynamics.getMf());
        values.put("f", dynamics.getForte());
        values.put("ff", dynamics.getFf());
        values.put("fff", dynamics.getFff());
        values.put("ffff", dynamics.getFfff());
        values.put("sf", dynamics.getSf());
        values.put("sfp", dynamics.getSfp());
        values.put("sfz", dynamics.getSfz());
        values.put("fp", dynamics.getFp());
        values.put("rf", dynamics.getRf());
        values.put("rfz", dynamics.getRfz());
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                return decoration(entry.getKey());
            }
        }
        if (dynamics.getOtherDynamics() != null && dynamics.getOtherDynamics().trim().matches("[a-z]+")) {
            return decoration(dynamics.getOtherDynamics().trim());
        }
        return null;
    }


    /**
     * Gets a long decoration.
     *
     * @param name the name
     * @return the decoration, for example !trill!
     */
    public String decoration(String name) {
        return BANG + name + BANG;
    }


    /**
     * Gets the tempo field value (for example "Allegro" 1/4=120).
     *
     * @param text the tempo text, or null
     * @param metronome the metronome mark, or null
     * @param soundTempo the playback tempo in quarter notes per minute, or null
     * @return the value, or null if there is nothing to write
     */
    public String tempo(String text, IMetronome metronome, String soundTempo) {
        List<String> parts = new ArrayList<>();
        if (text != null) {
            parts.add(QUOTE + clean(text) + QUOTE);
        }
        String perMinute = null;
        String beat = "1/4";
        if (metronome != null && metronome.getPerMinute() != null) {
            perMinute = number(metronome.getPerMinute());
            Fraction length = typeLength(metronome.getBeatUnit(), 0);
            if (length != null) {
                if (metronome.getBeatUnitDot() != null) {
                    length = length.multiply(3, 2);
                }
                beat = length.getNumerator() + "/" + length.getDenominator();
            }
        }
        if (perMinute == null && soundTempo != null) {
            perMinute = number(soundTempo);
        }
        if (perMinute != null) {
            parts.add(beat + "=" + perMinute);
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(" ", parts);
    }


    /**
     * Gets a chord symbol in quotes (for example "Am7/G").
     *
     * @param harmony the chord symbol
     * @return the chord symbol, or null without root
     */
    public String chord(IHarmony harmony) {
        String text = ChordSymbolFormatter.getInstance().format(harmony);
        if (text == null) {
            return null;
        }
        return QUOTE + text + QUOTE;
    }


    /**
     * Gets an annotation above or below the note.
     *
     * @param text the text
     * @param below true for below the note
     * @return the annotation, for example "^dolce"
     */
    public String annotation(String text, boolean below) {
        String prefix = "^";
        if (below) {
            prefix = "_";
        }
        return QUOTE + prefix + clean(text) + QUOTE;
    }


    /**
     * Cleans a text for a quoted ABC string: quotes become apostrophes, line breaks spaces.
     *
     * @param text the text
     * @return the cleaned text
     */
    public String clean(String text) {
        return text.replace('"', '\'').replace("\r", "").replace('\n', ' ').trim();
    }


    /**
     * Formats a number (a tempo) without decimals.
     *
     * @param text the number text
     * @return the number, or null if the text is no number
     */
    private String number(String text) {
        String digits = text.replaceAll("[^0-9.]", "");
        if (digits.isEmpty() || digits.startsWith(".") || digits.indexOf('.') != digits.lastIndexOf('.')) {
            return null;
        }
        BigDecimal value = new BigDecimal(digits);
        if (value.signum() <= 0) {
            return null;
        }
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }


    /**
     * Adds a decoration if a mark is set.
     *
     * @param result the decorations
     * @param mark the mark, or null
     * @param name the decoration name
     */
    private void addIf(List<String> result, String mark, String name) {
        if (mark != null) {
            addOnce(result, name);
        }
    }


    /**
     * Adds a decoration if it is not in the list yet.
     *
     * @param result the decorations
     * @param name the decoration name
     */
    private void addOnce(List<String> result, String name) {
        String value = decoration(name);
        if (!result.contains(value)) {
            result.add(value);
        }
    }


    /**
     * Creates the lengths of the MusicXML note types.
     *
     * @return the length (numerator/denominator) by note type
     */
    private static Map<String, String> createTypes() {
        Map<String, String> result = new HashMap<>();
        result.put("long", "4/1");
        result.put("breve", "2/1");
        result.put("whole", "1/1");
        result.put("half", "1/2");
        result.put("quarter", "1/4");
        result.put("eighth", "1/8");
        result.put("16th", "1/16");
        result.put("32nd", "1/32");
        result.put("64th", "1/64");
        result.put("128th", "1/128");
        result.put("256th", "1/256");
        return result;
    }
}
