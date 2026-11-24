/*
 * LilyPondNotationWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl;

import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.TypeAndDots;
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
 * Converts notation of the model into LilyPond text (Dutch note names, absolute octaves): pitches, durations, clefs,
 * keys, articulations, ornaments, dynamics, tempo marks, chord names and strings.
 *
 * @author patrick
 */
public final class LilyPondNotationWriter {
    private static final String STEPS = "CDEFGAB";
    private static final String QUOTE = "\"";
    private static final int[] STEP_SEMITONES = {0, 2, 4, 5, 7, 9, 11};
    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11};
    private static final String[] MODES = {"major", "dorian", "phrygian", "lydian", "mixolydian", "minor", "locrian"};
    private static final int LILYPOND_OCTAVE_OF_C = 3;
    private static final Map<String, String> DURATIONS = createDurations();
    private static final Map<String, String> CHORD_MODIFIERS = createChordModifiers();
    private static final Map<String, String> DYNAMICS = createDynamics();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final LilyPondNotationWriter INSTANCE = new LilyPondNotationWriter();
    }


    /**
     * Constructor.
     */
    private LilyPondNotationWriter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static LilyPondNotationWriter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the Dutch note name of a step and alteration (for example fis, bes, es, as).
     *
     * @param step the step (C to B)
     * @param alter the alteration in semitones, or null
     * @return the note name
     */
    public String noteName(String step, Double alter) {
        String name = step.toLowerCase();
        double value = 0;
        if (alter != null) {
            value = alter;
        }
        if (value == 1) {
            return name + "is";
        }
        if (value == 2) {
            return name + "isis";
        }
        if (value == 0.5) {
            return name + "ih";
        }
        if (value == 1.5) {
            return name + "isih";
        }
        if (value == -0.5) {
            return name + "eh";
        }
        if (value == -1.5) {
            return name + "eseh";
        }
        if (value == -1 || value == -2) {
            String flat = name + "es";
            if ("e".equals(name) || "a".equals(name)) {
                flat = name + "s";
            }
            if (value == -2) {
                flat = flat + "es";
            }
            return flat;
        }
        return name;
    }


    /**
     * Gets a pitch with absolute octave (c is the octave below middle C, c' middle C).
     *
     * @param step the step
     * @param alter the alteration, or null
     * @param octave the MusicXML octave (4 is the octave of middle C)
     * @return the pitch
     */
    public String pitch(String step, Double alter, int octave) {
        return noteName(step, alter) + octaveMarks(octave);
    }


    /**
     * Gets the octave marks of a MusicXML octave.
     *
     * @param octave the MusicXML octave
     * @return the marks (' up, , down)
     */
    public String octaveMarks(int octave) {
        StringBuilder result = new StringBuilder();
        int marks = octave - LILYPOND_OCTAVE_OF_C;
        for (int i = 0; i < Math.abs(marks); i++) {
            if (marks > 0) {
                result.append('\'');
            } else {
                result.append(',');
            }
        }
        return result.toString();
    }


    /**
     * Gets the LilyPond duration of a note type with dots.
     *
     * @param type the MusicXML note type
     * @param dots the dots
     * @return the duration (for example 4. or \breve), or null if the type is unknown
     */
    public String duration(String type, int dots) {
        String base = DURATIONS.get(type);
        if (base == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(base);
        for (int i = 0; i < dots; i++) {
            result.append('.');
        }
        return result.toString();
    }


    /**
     * Gets the LilyPond duration of a length: a note value, or a whole note with a factor (1*5/4).
     *
     * @param length the length in whole notes
     * @return the duration
     */
    public String duration(Fraction length) {
        TypeAndDots value = NoteValueConverter.getInstance().fromFraction(length);
        if (value != null) {
            String result = duration(value.getType(), value.getDots());
            if (result != null) {
                return result;
            }
        }
        return "1*" + length.getNumerator() + "/" + length.getDenominator();
    }


    /**
     * Gets the clef command argument of a clef (for example treble, bass or "treble_8").
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
            return "percussion";
        } else if ("TAB".equalsIgnoreCase(sign)) {
            return "tab";
        } else if ("F".equals(sign)) {
            name = clefName(line, 4, new String[] {"varbaritone", "bass", "subbass"});
        } else if ("C".equals(sign)) {
            if (line < 1 || line > 5) {
                name = "alto";
            } else {
                name = new String[] {"soprano", "mezzosoprano", "alto", "tenor", "baritone"}[line - 1];
            }
        } else if (line == 1) {
            name = "french";
        } else {
            name = "treble";
        }
        int octave = 0;
        if (clef.getClefOctaveChange() != null) {
            octave = clef.getClefOctaveChange();
        }
        if (octave == 0) {
            return name;
        }
        String suffix = "8";
        if (Math.abs(octave) >= 2) {
            suffix = "15";
        }
        if (octave > 0) {
            return QUOTE + name + "^" + suffix + QUOTE;
        }
        return QUOTE + name + "_" + suffix + QUOTE;
    }


    /**
     * Gets the name of a clef by its line.
     *
     * @param line the line, or 0
     * @param normal the normal line
     * @param names the names of the lines normal - 1, normal and normal + 1
     * @return the name
     */
    private String clefName(int line, int normal, String[] names) {
        if (line == normal - 1) {
            return names[0];
        }
        if (line == normal + 1) {
            return names[2];
        }
        return names[1];
    }


    /**
     * Gets the key command of a key signature (for example \key d \major).
     *
     * @param fifths the fifths (positive for sharps)
     * @param mode the MusicXML mode, or null for major
     * @return the key command
     */
    public String key(int fifths, String mode) {
        int degree = 0;
        String lilypondMode = "major";
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equals(mode) || "aeolian".equals(mode) && i == 5 || "ionian".equals(mode) && i == 0) {
                degree = i;
                lilypondMode = MODES[i];
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
        return "\\key " + noteName(String.valueOf(STEPS.charAt(step)), (double) alter) + " \\" + lilypondMode;
    }


    /**
     * Gets the transposition command of a staff (\transposition bes for a B flat instrument).
     *
     * @param diatonic the diatonic steps (sounding minus written)
     * @param semitones the semitones (sounding minus written)
     * @return the command
     */
    public String transposition(int diatonic, int semitones) {
        int middleC = 4 * 7;
        int target = middleC + diatonic;
        int step = Math.floorMod(target, 7);
        int octave = Math.floorDiv(target, 7);
        int naturalSemitone = octave * 12 + STEP_SEMITONES[step];
        int alter = (4 * 12 + semitones) - naturalSemitone;
        return "\\transposition " + pitch(String.valueOf(STEPS.charAt(step)), (double) alter, octave);
    }


    /**
     * Gets the post-events of the articulations, fermata and ornaments of a note.
     *
     * @param notations the notations, or null
     * @return the post-events (for example -. or \fermata)
     */
    public List<String> marks(INotations notations) {
        List<String> result = new ArrayList<>();
        if (notations == null) {
            return result;
        }
        IArticulations articulations = notations.getArticulations();
        if (articulations != null) {
            addIf(result, articulations.getAccent(), "->");
            addIf(result, articulations.getStrongAccent(), "-^");
            addIf(result, articulations.getStaccato(), "-.");
            addIf(result, articulations.getTenuto(), "--");
            addIf(result, articulations.getDetachedLegato(), "-_");
            addIf(result, articulations.getStaccatissimo(), "-!");
            addIf(result, articulations.getSpiccato(), "\\staccatissimo");
            addIf(result, articulations.getStress(), "\\accent");
        }
        if (notations.getFermatas() != null && !notations.getFermatas().isEmpty()) {
            if ("inverted".equals(notations.getFermatas().get(0).getType())) {
                result.add("_\\fermata");
            } else {
                result.add("\\fermata");
            }
        }
        IOrnaments ornaments = notations.getOrnaments();
        if (ornaments != null) {
            addIf(result, ornaments.getTrillMark(), "\\trill");
            addIf(result, ornaments.getMordent(), "\\mordent");
            addIf(result, ornaments.getInvertedMordent(), "\\prall");
            addIf(result, ornaments.getTurn(), "\\turn");
            addIf(result, ornaments.getInvertedTurn(), "\\reverseturn");
            addIf(result, ornaments.getDelayedTurn(), "\\turn");
        }
        if (notations.getTechnical() != null && notations.getTechnical().getFingering() != null) {
            String fingering = notations.getTechnical().getFingering().trim();
            if (fingering.matches("[0-9]")) {
                result.add("-" + fingering);
            } else if (!fingering.isEmpty()) {
                result.add("-\\finger " + string(fingering));
            }
        }
        String dynamics = dynamics(notations.getDynamics());
        if (dynamics != null) {
            result.add(dynamics);
        }
        return result;
    }


    /**
     * Gets the post-event of dynamics (for example \mf, or a markup for unknown dynamics).
     *
     * @param dynamics the dynamics, or null
     * @return the post-event, or null
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
                return dynamicsText(entry.getKey());
            }
        }
        if (dynamics.getOtherDynamics() != null && !dynamics.getOtherDynamics().trim().isEmpty()) {
            return dynamicsText(dynamics.getOtherDynamics().trim());
        }
        return null;
    }


    /**
     * Gets the post-event of a dynamics text: a LilyPond dynamics command, or a dynamics markup.
     *
     * @param text the dynamics (for example mf)
     * @return the post-event
     */
    private String dynamicsText(String text) {
        String command = DYNAMICS.get(text);
        if (command != null) {
            return command;
        }
        return "-\\markup { \\dynamic " + string(text) + " }";
    }


    /**
     * Gets the tempo command (for example \tempo "Allegro" 4 = 120).
     *
     * @param text the tempo text, or null
     * @param metronome the metronome mark, or null
     * @param soundTempo the playback tempo in quarter notes per minute, or null
     * @return the command, or null if there is nothing to write
     */
    public String tempo(String text, IMetronome metronome, String soundTempo) {
        StringBuilder result = new StringBuilder("\\tempo");
        if (text != null) {
            result.append(' ').append(string(text));
        }
        String perMinute = null;
        String unit = "4";
        if (metronome != null && metronome.getPerMinute() != null) {
            perMinute = number(metronome.getPerMinute());
            String type = DURATIONS.get(metronome.getBeatUnit());
            if (type != null) {
                unit = type;
            }
            if (metronome.getBeatUnitDot() != null) {
                unit = unit + ".";
            }
        }
        boolean hidden = false;
        if (perMinute == null && soundTempo != null) {
            perMinute = number(soundTempo);
            hidden = true;
        }
        if (perMinute != null) {
            result.append(' ').append(unit).append(" = ").append(perMinute);
        } else if (text == null) {
            return null;
        }
        if (hidden && text != null) {
            // a playback tempo without metronome mark
            return "\\once \\set Score.tempoHideNote = ##t " + result;
        }
        return result.toString();
    }


    /**
     * Gets a chord name in chord mode without duration parts (for example the root a, the modifier :m7 and the bass
     * /e of a:m7/e).
     *
     * @param harmony the chord symbol
     * @return the root, the modifier (empty or :...) and the bass (empty or /...), or null without root
     */
    public String[] chord(IHarmony harmony) {
        if (harmony.getRoot() == null || harmony.getRoot().getRootStep() == null) {
            return null;
        }
        String root = noteName(harmony.getRoot().getRootStep().toUpperCase(), harmony.getRoot().getRootAlter());
        String modifier = CHORD_MODIFIERS.get(harmony.getKind());
        if (modifier == null || modifier.isEmpty()) {
            modifier = "";
        } else {
            modifier = ":" + modifier;
        }
        String bass = "";
        if (harmony.getBass() != null && harmony.getBass().getBassStep() != null) {
            bass = "/" + noteName(harmony.getBass().getBassStep().toUpperCase(), harmony.getBass().getBassAlter());
        }
        return new String[] {root, modifier, bass};
    }


    /**
     * Quotes a text as LilyPond string.
     *
     * @param text the text
     * @return the string with quotes and escapes
     */
    public String string(String text) {
        return QUOTE + text.replace("\\", "\\\\").replace(QUOTE, "\\\"").replace("\r", "").replace("\n", " ") + QUOTE;
    }


    /**
     * Formats a number (a tempo) without decimals if it is a whole number.
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
     * Adds a value if a mark is set.
     *
     * @param result the values
     * @param mark the mark, or null
     * @param value the value
     */
    private void addIf(List<String> result, String mark, String value) {
        if (mark != null) {
            result.add(value);
        }
    }


    /**
     * Creates the LilyPond durations of the MusicXML note types.
     *
     * @return the duration by note type
     */
    private static Map<String, String> createDurations() {
        Map<String, String> result = new HashMap<>();
        result.put("maxima", "\\maxima");
        result.put("long", "\\longa");
        result.put("breve", "\\breve");
        result.put("whole", "1");
        result.put("half", "2");
        result.put("quarter", "4");
        result.put("eighth", "8");
        result.put("16th", "16");
        result.put("32nd", "32");
        result.put("64th", "64");
        result.put("128th", "128");
        result.put("256th", "256");
        result.put("512th", "512");
        result.put("1024th", "1024");
        return result;
    }


    /**
     * Creates the chord mode modifiers of the MusicXML chord kinds.
     *
     * @return the modifier by kind
     */
    private static Map<String, String> createChordModifiers() {
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
        result.put("half-diminished", "m7.5-");
        result.put("major-minor", "m7+");
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
        result.put("power", "1.5");
        return result;
    }


    /**
     * Creates the LilyPond commands of the dynamics.
     *
     * @return the command by dynamics
     */
    private static Map<String, String> createDynamics() {
        Map<String, String> result = new HashMap<>();
        for (String name : new String[] {"ppppp", "pppp", "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff", "ffff", "fffff", "fp", "sf", "sff", "sp",
            "spp", "sfz", "rfz", "sfp"}) {
            result.put(name, "\\" + name);
        }
        return result;
    }
}
