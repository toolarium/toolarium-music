/*
 * AbcTuneParser.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl;

import com.github.toolarium.music.io.converter.ChordSymbolParser;
import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.converter.PitchSpeller;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.ClefChange;
import com.github.toolarium.music.io.dto.DirectionData;
import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.MeasureData;
import com.github.toolarium.music.io.dto.NoteData;
import com.github.toolarium.music.io.dto.PartData;
import com.github.toolarium.music.io.dto.ScoreData;
import com.github.toolarium.music.io.dto.TypeAndDots;
import com.github.toolarium.music.io.dto.VoiceData;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcKey;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcMeter;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcParsedNote;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcTempo;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcVoice;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Parses one ABC tune (from the X: line to the end of the tune) into the format independent
 * {@link ScoreData}. Each voice becomes a part unless a %%score or %%staves directive groups
 * voices on one staff or several staves into one part.
 *
 * @author patrick
 */
final class AbcTuneParser {
    private static final Pattern FIELD_LINE = Pattern.compile("^[A-Za-z+]:.*");
    private static final String DEFAULT_VOICE = "1";
    private static final String NOTE_LETTERS = "ABCDEFGabcdefg";
    private static final String DECORATION_SYMBOLS = ".~HLMOPSTuv";
    private static final int MIDDLE_OCTAVE = 4;
    private static final int VOICES_PER_STAFF = 4;
    private static final Fraction THREE_QUARTERS = Fraction.of(3, 4);

    private final AbcFieldParser fieldParser = AbcFieldParser.getInstance();
    private final AbcDecorationConverter decorationConverter = AbcDecorationConverter.getInstance();
    private final ScoreData score = new ScoreData();
    private final Map<String, AbcVoice> voices = new LinkedHashMap<>();
    private final List<String> titles = new ArrayList<>();
    private List<List<List<String>>> layout;
    private Fraction unit;
    private AbcMeter meter = new AbcMeter(4, 4, null);
    private AbcKey key = AbcFieldParser.getInstance().parseKey("C");
    private ClefImpl clef;
    private AbcTempo tempo;
    private Integer program;
    private AbcVoice current;
    private boolean body;


    /**
     * Constructor for AbcTuneParser.
     */
    AbcTuneParser() {
        // default constructor
    }


    /**
     * Parses the lines of one tune.
     *
     * @param lines the lines (the X: line included)
     * @return the score data
     */
    ScoreData parse(List<String> lines) {
        for (String raw : lines) {
            String line = raw;
            String trimmed = line.trim();
            if (trimmed.startsWith("%%")) {
                readDirective(trimmed.substring(2).trim());
                continue;
            }
            if (trimmed.startsWith("%") || trimmed.isEmpty()) {
                continue;
            }
            if (FIELD_LINE.matcher(trimmed).matches() && !(body && isMusicStartingWithField(trimmed))) {
                readField(trimmed.charAt(0), stripComment(trimmed.substring(2)).trim());
                continue;
            }
            if (!body) {
                // music without a K: field: start the body with the defaults
                body = true;
            }
            readMusicLine(line);
        }
        return finish();
    }


    /**
     * Checks if a body line that looks like a field is music (for example a line starting with a
     * note followed by a colon in a bar such as {@code A:|}).
     *
     * @param line the trimmed line
     * @return true if the line is music
     */
    private boolean isMusicStartingWithField(String line) {
        return line.length() > 2 && (line.charAt(2) == '|' || line.charAt(2) == ':');
    }


    /**
     * Reads an information field.
     *
     * @param field the field letter
     * @param value the field value (comment removed)
     */
    private void readField(char field, String value) {
        switch (field) {
            case 'X':
                break;
            case 'T':
                if (body) {
                    voice().getDirections().add(ScoreBuilder.getInstance().createWords(value, ScoreBuilder.ABOVE, null, "bold"));
                } else {
                    titles.add(value);
                }
                break;
            case 'C':
                readComposer(value);
                break;
            case 'N':
                if (!body && (value.trim().startsWith("©") || value.trim().toLowerCase(Locale.ROOT).startsWith("copyright"))) {
                    if (score.getRights() == null) {
                        score.setRights(value.trim());
                    } else {
                        score.setRights(score.getRights() + "\n" + value.trim());
                    }
                }
                break;
            case 'A':
                if (score.getSource() == null) {
                    score.setSource(value);
                }
                break;
            case 'S':
                score.setSource(value);
                break;
            case 'Z':
                score.addCreator("transcriber", value);
                break;
            case 'K':
                readKey(value);
                break;
            case 'M':
                readMeter(value);
                break;
            case 'L':
                readUnit(value);
                break;
            case 'Q':
                readTempo(value);
                break;
            case 'V':
                readVoice(value);
                break;
            case 'P':
                if (body) {
                    voice().getDirections().add(ScoreBuilder.getInstance().createRehearsal(value));
                }
                break;
            case 'w':
                if (body && current != null) {
                    readLyrics(current, value);
                }
                break;
            case 'I':
                if (value.startsWith("score") || value.startsWith("staves")) {
                    readDirective(value);
                }
                break;
            default:
                break;
        }
    }


    /**
     * Reads a composer field (C:): "arr. name" is the arranger, "words: name" the lyricist.
     *
     * @param value the field value
     */
    private void readComposer(String value) {
        String text = value.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("arr. ") || lower.startsWith("arr: ")) {
            score.addCreator("arranger", text.substring(5).trim());
        } else if (lower.startsWith("words: ") || lower.startsWith("lyrics: ")) {
            score.addCreator("lyricist", text.substring(text.indexOf(':') + 1).trim());
        } else {
            score.addCreator("composer", text);
        }
    }


    /**
     * Reads a stylesheet directive (%%). Only %%score, %%staves and %%MIDI program/channel are used.
     *
     * @param directive the directive without the leading %%
     */
    private void readDirective(String directive) {
        String text = stripComment(directive);
        if (text.startsWith("score") || text.startsWith("staves")) {
            int space = text.indexOf(' ');
            if (space > 0) {
                layout = fieldParser.parseScore(text.substring(space + 1));
            }
            return;
        }
        if (text.startsWith("MIDI")) {
            String[] words = text.split("\\s+");
            if (words.length >= 3 && "program".equals(words[1])) {
                int value = fieldParser.parseInt(words[words.length - 1], -1);
                if (value >= 0 && value < 128) {
                    if (current != null) {
                        current.setProgram(value + 1);
                    } else {
                        program = value + 1;
                    }
                }
            } else if (words.length >= 3 && "channel".equals(words[1]) && current != null) {
                int value = fieldParser.parseInt(words[2], 0);
                if (value >= 1 && value <= 16) {
                    current.setChannel(value);
                }
            }
        }
    }


    /**
     * Reads a key field (K:). In the header it ends the header.
     *
     * @param value the field value
     */
    private void readKey(String value) {
        AbcKey parsed = fieldParser.parseKey(value);
        ClefImpl parsedClef = fieldParser.parseClef(value);
        int transpose = fieldParser.parseInt(fieldParser.parseProperties(value).get("transpose"), 0);
        if (!body) {
            key = parsed;
            clef = parsedClef;
            body = true;
            for (AbcVoice voice : voices.values()) {
                voice.setKey(parsed);
                if (voice.getClef() == null) {
                    voice.setClef(parsedClef);
                }
                if (transpose != 0 && voice.getTranspose() == 0) {
                    voice.setTranspose(transpose);
                }
            }
            return;
        }
        AbcVoice voice = voice();
        changeKey(voice, parsed);
        if (parsedClef != null) {
            changeClef(voice, parsedClef);
        }
        if (voices.size() == 1) {
            key = parsed;
        }
    }


    /**
     * Changes the key of a voice: at the start of an empty measure or from the next measure.
     *
     * @param voice the voice
     * @param parsed the new key
     */
    private void changeKey(AbcVoice voice, AbcKey parsed) {
        voice.setKey(parsed);
        if (measureEmpty(voice)) {
            voice.getMeasure().setKey(parsed.getFifths(), parsed.getMode());
        } else {
            voice.setPendingKey(parsed);
        }
    }


    /**
     * Changes the clef of a voice at the current position.
     *
     * @param voice the voice
     * @param newClef the clef
     */
    private void changeClef(AbcVoice voice, ClefImpl newClef) {
        voice.setClef(newClef);
        voice.getMeasure().addClef(1, voice.getVoiceData().getLength(), newClef);
    }


    /**
     * Reads a meter field (M:).
     *
     * @param value the field value
     */
    private void readMeter(String value) {
        AbcMeter parsed = fieldParser.parseMeter(value);
        if (!body) {
            meter = parsed;
            return;
        }
        AbcVoice voice = voice();
        voice.setMeter(parsed);
        if (measureEmpty(voice)) {
            applyMeter(voice.getMeasure(), parsed);
        } else {
            voice.setPendingMeter(parsed);
        }
        if (voices.size() == 1) {
            meter = parsed;
        }
    }


    /**
     * Reads a unit note length field (L:).
     *
     * @param value the field value
     */
    private void readUnit(String value) {
        Fraction parsed = fieldParser.parseLength(value);
        if (parsed == null) {
            return;
        }
        if (!body) {
            unit = parsed;
            return;
        }
        voice().setUnit(parsed);
    }


    /**
     * Reads a tempo field (Q:).
     *
     * @param value the field value
     */
    private void readTempo(String value) {
        AbcTempo parsed = fieldParser.parseTempo(value);
        if (!body) {
            tempo = parsed;
            return;
        }
        AbcVoice voice = voice();
        voice.getDirections().add(tempoDirection(parsed, voice.getUnit()));
    }


    /**
     * Creates the direction of a tempo.
     *
     * @param parsed the tempo
     * @param unitLength the unit note length (beat unit of a tempo without one)
     * @return the direction
     */
    private DirectionImpl tempoDirection(AbcTempo parsed, Fraction unitLength) {
        Fraction beat = parsed.getBeat();
        if (beat == null) {
            beat = unitLength;
        }
        TypeAndDots unitType = NoteValueConverter.getInstance().fromFraction(beat);
        String beatUnit = "quarter";
        int dots = 0;
        double bpm = parsed.getBpm();
        if (unitType != null) {
            beatUnit = unitType.getType();
            dots = unitType.getDots();
        } else if (bpm > 0) {
            bpm = bpm * beat.getNumerator() * 4 / beat.getDenominator();
        }
        return ScoreBuilder.getInstance().createTempo(bpm, beatUnit, dots, parsed.getText());
    }


    /**
     * Reads a voice field (V:): defines the voice properties and, in the body, switches to the voice.
     *
     * @param value the field value
     */
    private void readVoice(String value) {
        String[] parts = fieldParser.splitVoice(value);
        if (parts[0].isEmpty()) {
            return;
        }
        AbcVoice voice = voices.get(parts[0]);
        if (voice == null) {
            voice = createVoice(parts[0]);
        }
        Map<String, String> properties = fieldParser.parseProperties(parts[1]);
        String name = firstNonNull(properties.get("name"), properties.get("nm"));
        if (name != null) {
            voice.setName(name);
        }
        String shortName = firstNonNull(properties.get("subname"), firstNonNull(properties.get("sname"), properties.get("snm")));
        if (shortName != null) {
            voice.setShortName(shortName);
        }
        ClefImpl voiceClef = fieldParser.parseClef(parts[1]);
        if (voiceClef != null) {
            if (body && voice.getMeasure() != null && !voice.getMeasures().isEmpty()) {
                changeClef(voice, voiceClef);
            } else {
                voice.setClef(voiceClef);
            }
        }
        int transpose = fieldParser.parseInt(properties.get("transpose"), 0);
        if (transpose != 0) {
            voice.setTranspose(transpose);
        }
        if (body) {
            current = voice;
            voice();
        }
    }


    /**
     * Creates a voice with the current defaults.
     *
     * @param id the voice ID
     * @return the voice
     */
    private AbcVoice createVoice(String id) {
        AbcVoice voice = new AbcVoice(id);
        if (body) {
            voice.setKey(key);
            voice.setMeter(meter);
            voice.setUnit(unit);
            voice.setClef(clef);
        }
        voice.setProgram(program);
        voices.put(id, voice);
        return voice;
    }


    /**
     * Gets the current voice, creating the default voice and its first measure if needed.
     *
     * @return the current voice
     */
    private AbcVoice voice() {
        if (current == null) {
            if (voices.isEmpty()) {
                current = createVoice(DEFAULT_VOICE);
            } else {
                current = voices.values().iterator().next();
            }
        }
        if (current.getUnit() == null) {
            current.setUnit(defaultUnit());
        }
        if (current.getKey() == null) {
            current.setKey(key);
        }
        if (current.getMeter() == null) {
            current.setMeter(meter);
        }
        if (current.getMeasure() == null) {
            newMeasure(current);
            MeasureData first = current.getMeasure();
            if (!current.getMeter().isFree()) {
                applyMeter(first, current.getMeter());
            }
            first.setKey(current.getKey().getFifths(), current.getKey().getMode());
            if (current.getClef() != null) {
                first.addClef(1, Fraction.ZERO, current.getClef());
            }
            if (tempo != null && current == voices.values().iterator().next()) {
                current.getDirections().add(tempoDirection(tempo, current.getUnit()));
            }
        }
        return current;
    }


    /**
     * Gets the default unit note length: the L: field, or 1/16 for meters below 3/4 and 1/8 otherwise.
     *
     * @return the unit length
     */
    private Fraction defaultUnit() {
        if (unit != null) {
            return unit;
        }
        if (!meter.isFree() && meter.getLength().compareTo(THREE_QUARTERS) < 0) {
            return Fraction.of(1, 16);
        }
        return Fraction.of(1, 8);
    }


    /**
     * Sets the time signature of a measure.
     *
     * @param measure the measure
     * @param value the meter
     */
    private void applyMeter(MeasureData measure, AbcMeter value) {
        if (!value.isFree()) {
            measure.setTime(value.getBeats(), value.getBeatType(), value.getSymbol());
        }
    }


    /**
     * Starts a new measure in a voice and applies pending key, meter, repeat and ending.
     *
     * @param voice the voice
     */
    private void newMeasure(AbcVoice voice) {
        MeasureData measure = new MeasureData();
        voice.getMeasures().add(measure);
        voice.setMeasure(measure);
        voice.setOverlay(0);
        voice.setVoiceData(measure.voice(1, 1));
        voice.getMeasureAccidentals().clear();
        if (voice.getPendingKey() != null) {
            measure.setKey(voice.getPendingKey().getFifths(), voice.getPendingKey().getMode());
            voice.setPendingKey(null);
        }
        if (voice.getPendingMeter() != null) {
            applyMeter(measure, voice.getPendingMeter());
            voice.setPendingMeter(null);
        }
        if (voice.isRepeatStartPending()) {
            measure.setRepeatStart(true);
            voice.setRepeatStartPending(false);
        }
        if (voice.getEnding() != null) {
            measure.setEndingNumber(voice.getEnding());
        }
    }


    /**
     * Checks if the current measure of a voice has no events yet.
     *
     * @param voice the voice
     * @return true if the measure is empty
     */
    private boolean measureEmpty(AbcVoice voice) {
        for (VoiceData data : voice.getMeasure().getVoices()) {
            if (!data.getEvents().isEmpty()) {
                return false;
            }
        }
        return true;
    }


    /**
     * Reads one line of music.
     *
     * @param line the line
     */
    private void readMusicLine(String line) {
        for (AbcVoice voice : voices.values()) {
            voice.setLineStarted(false);
        }
        AbcVoice voice = voice();
        List<NoteData> chord = null;
        Fraction chordLength = null;
        List<NoteData> chordTies = null;
        int i = 0;
        int n = line.length();
        while (i < n) {
            char c = line.charAt(i);
            if (c == '%') {
                break;
            }
            if (c == '\\' && line.substring(i + 1).trim().isEmpty()) {
                break;
            }
            if (c == '"') {
                int end = line.indexOf('"', i + 1);
                if (end < 0) {
                    end = n;
                }
                readQuoted(voice, line.substring(i + 1, end));
                i = end + 1;
                continue;
            }
            if (c == '!' || (c == '+' && line.indexOf('+', i + 1) > i + 1)) {
                int end = line.indexOf(c, i + 1);
                if (end > i) {
                    voice.getDecorations().add(line.substring(i + 1, end));
                    i = end + 1;
                    continue;
                }
                i++;
                continue;
            }
            if (c == '[' && i + 2 < n && Character.isLetter(line.charAt(i + 1)) && line.charAt(i + 2) == ':') {
                int end = line.indexOf(']', i);
                if (end < 0) {
                    end = n;
                }
                char field = line.charAt(i + 1);
                readField(field, line.substring(i + 3, end).trim());
                voice = voice();
                i = end + 1;
                continue;
            }
            if (c == '[' && i + 1 < n && Character.isDigit(line.charAt(i + 1))) {
                i = readEnding(voice, line, i + 1);
                continue;
            }
            if (c == '|' || (c == ':' && i + 1 < n && (line.charAt(i + 1) == '|' || line.charAt(i + 1) == ':'))
                    || (c == '[' && i + 1 < n && line.charAt(i + 1) == '|') || (c == '.' && i + 1 < n && line.charAt(i + 1) == '|')) {
                i = readBar(voice, line, i);
                continue;
            }
            if (c == '[') {
                chord = new ArrayList<>();
                chordLength = null;
                chordTies = new ArrayList<>();
                i++;
                continue;
            }
            if (c == ']' && chord != null) {
                i++;
                int[] position = {i};
                Fraction multiplier = readLength(line, position);
                i = position[0];
                boolean tie = false;
                if (i < n && line.charAt(i) == '-') {
                    tie = true;
                    i++;
                }
                if (!chord.isEmpty()) {
                    if (chordLength == null) {
                        chordLength = Fraction.of(1, 1);
                    }
                    addNotes(voice, chord, voice.getUnit().multiply(chordLength).multiply(multiplier), chordTies, tie);
                }
                chord = null;
                continue;
            }
            int next = readSymbol(voice, line, i);
            if (next >= 0) {
                i = next;
                continue;
            }
            if (NOTE_LETTERS.indexOf(c) >= 0 || c == '^' || c == '_' || c == '=') {
                AbcParsedNote parsed = readNote(line, i);
                if (parsed == null) {
                    i++;
                    continue;
                }
                i = parsed.getEnd();
                NoteData note = createNote(voice, parsed);
                if (chord != null) {
                    if (chordLength == null) {
                        chordLength = parsed.getMultiplier();
                    }
                    chord.add(note);
                    if (parsed.isTie()) {
                        chordTies.add(note);
                    }
                } else {
                    List<NoteData> notes = new ArrayList<>();
                    notes.add(note);
                    List<NoteData> ties = new ArrayList<>();
                    if (parsed.isTie()) {
                        ties.add(note);
                    }
                    addNotes(voice, notes, voice.getUnit().multiply(parsed.getMultiplier()), ties, false);
                }
                continue;
            }
            if (c == 'z' || c == 'x' || c == 'Z' || c == 'X') {
                i = readRest(voice, line, i);
                continue;
            }
            i++;
        }
    }


    /**
     * Reads a symbol of a music line: grace note braces, slurs, tuplets, ties, broken rhythm,
     * voice overlays and one-character decorations.
     *
     * @param voice the voice
     * @param line the line
     * @param start the index of the symbol
     * @return the index after the symbol, or -1 if the character is not such a symbol
     */
    private int readSymbol(AbcVoice voice, String line, int start) {
        int i = start;
        int n = line.length();
        char c = line.charAt(i);
        if (c == '{') {
            voice.setGrace(true);
            voice.setGraceSlash(i + 1 < n && line.charAt(i + 1) == '/');
            if (voice.isGraceSlash()) {
                i++;
            }
            i++;
            return i;
        }
        if (c == '}') {
            voice.setGrace(false);
            i++;
            return i;
        }
        if (c == '(' && i + 1 < n && Character.isDigit(line.charAt(i + 1))) {
            return readTuplet(voice, line, i + 1);
        }
        if (c == '(') {
            voice.setSlurStarts(voice.getSlurStarts() + 1);
            i++;
            return i;
        }
        if (c == ')') {
            if (voice.getLastEvent() != null && !voice.getSlurs().isEmpty()) {
                voice.getLastEvent().getSlurStops().add(voice.getSlurs().pop());
            }
            i++;
            return i;
        }
        if (c == '-') {
            i++;
            return i;
        }
        if (c == '>' || c == '<') {
            int count = 0;
            while (i < n && line.charAt(i) == c) {
                count++;
                i++;
            }
            applyBroken(voice, c == '>', count);
            return i;
        }
        if (c == '&') {
            voice.setOverlay(voice.getOverlay() + 1);
            voice.setMaxOverlay(Math.max(voice.getMaxOverlay(), voice.getOverlay()));
            voice.setVoiceData(voice.getMeasure().voice(1, 1 + voice.getOverlay()));
            i++;
            return i;
        }
        if (DECORATION_SYMBOLS.indexOf(c) >= 0 && i + 1 < n && !(c == '.' && line.charAt(i + 1) == '|')) {
            voice.getDecorations().add(decorationConverter.nameOf(c));
            i++;
            return i;
        }
        return -1;
    }


    /**
     * Reads a rest (z), an invisible rest (x) or a multi-measure rest (Z, X).
     *
     * @param voice the voice
     * @param line the line
     * @param start the index of the rest
     * @return the index after the rest
     */
    private int readRest(AbcVoice voice, String line, int start) {
        int i = start;
        int n = line.length();
        char c = line.charAt(i);
        if (c == 'z' || c == 'x') {
            int[] position = {i + 1};
            Fraction multiplier = readLength(line, position);
            i = position[0];
            EventData rest = new EventData();
            rest.setDuration(voice.getUnit().multiply(multiplier));
            rest.setInvisible(c == 'x');
            addEvent(voice, rest);
            return i;
        }
        if (c == 'Z' || c == 'X') {
            int digits = i + 1;
            int end = digits;
            while (end < n && Character.isDigit(line.charAt(end))) {
                end++;
            }
            int count = Math.max(1, fieldParser.parseInt(line.substring(digits, end), 1));
            for (int k = 0; k < count; k++) {
                if (k > 0) {
                    closeMeasure(voice, null, false, false);
                }
                EventData rest = new EventData();
                rest.setMeasureRest(true);
                rest.setInvisible(c == 'X');
                addEvent(voice, rest);
            }
            i = end;
            return i;
        }
        return i;
    }


    /**
     * Reads a quoted string: an annotation (starting with ^, _, &lt;, &gt; or @) or a chord symbol.
     *
     * @param voice the voice
     * @param text the text between the quotes
     */
    private void readQuoted(AbcVoice voice, String text) {
        if (text.isEmpty()) {
            return;
        }
        char first = text.charAt(0);
        ScoreBuilder builder = ScoreBuilder.getInstance();
        if (first == '^' || first == '<' || first == '>' || first == '@') {
            voice.getDirections().add(builder.createWords(text.substring(1), ScoreBuilder.ABOVE, null, null));
            return;
        }
        if (first == '_') {
            voice.getDirections().add(builder.createWords(text.substring(1), ScoreBuilder.BELOW, null, null));
            return;
        }
        HarmonyImpl harmony = ChordSymbolParser.getInstance().parse(text);
        if (harmony != null) {
            voice.getHarmonies().add(harmony);
        } else {
            voice.getDirections().add(builder.createWords(text, ScoreBuilder.ABOVE, null, null));
        }
    }


    /**
     * Reads a note: accidentals, letter, octave marks, length and tie.
     *
     * @param line the line
     * @param start the start index
     * @return the parsed note, or null if no note letter follows the accidentals
     */
    private AbcParsedNote readNote(String line, int start) {
        int i = start;
        int n = line.length();
        StringBuilder accidental = new StringBuilder();
        while (i < n && (line.charAt(i) == '^' || line.charAt(i) == '_' || line.charAt(i) == '=')) {
            accidental.append(line.charAt(i));
            i++;
            if (i < n && line.charAt(i) == '/') {
                // microtonal accidental: approximated by the plain accidental
                i++;
                while (i < n && Character.isDigit(line.charAt(i))) {
                    i++;
                }
            }
        }
        if (i >= n || NOTE_LETTERS.indexOf(line.charAt(i)) < 0) {
            return null;
        }
        char letter = line.charAt(i);
        i++;
        AbcParsedNote note = new AbcParsedNote();
        if (accidental.length() > 0) {
            note.setAccidental(accidental.toString());
        }
        note.setStep(String.valueOf(Character.toUpperCase(letter)));
        note.setOctave(MIDDLE_OCTAVE);
        if (Character.isLowerCase(letter)) {
            note.setOctave(note.getOctave() + 1);
        }
        while (i < n && (line.charAt(i) == '\'' || line.charAt(i) == ',')) {
            if (line.charAt(i) == '\'') {
                note.setOctave(note.getOctave() + 1);
            } else {
                note.setOctave(note.getOctave() - 1);
            }
            i++;
        }
        int[] position = {i};
        note.setMultiplier(readLength(line, position));
        i = position[0];
        if (i < n && line.charAt(i) == '-') {
            note.setTie(true);
            i++;
        }
        note.setEnd(i);
        return note;
    }


    /**
     * Reads a length multiplier: {@code 2}, {@code 3/2}, {@code /2}, {@code /} or {@code //}.
     *
     * @param line the line
     * @param position the position (updated)
     * @return the multiplier of the unit note length
     */
    private Fraction readLength(String line, int[] position) {
        int i = position[0];
        int n = line.length();
        int start = i;
        while (i < n && Character.isDigit(line.charAt(i))) {
            i++;
        }
        long numerator = 1;
        if (i > start) {
            numerator = Math.max(1, fieldParser.parseInt(line.substring(start, i), 1));
        }
        long denominator = 1;
        while (i < n && line.charAt(i) == '/') {
            i++;
            int digits = i;
            while (i < n && Character.isDigit(line.charAt(i))) {
                i++;
            }
            if (i > digits) {
                denominator *= Math.max(1, fieldParser.parseInt(line.substring(digits, i), 2));
            } else {
                denominator *= 2;
            }
        }
        position[0] = i;
        return Fraction.of(numerator, denominator);
    }


    /**
     * Creates the note data of a parsed note: resolves the alteration from the accidental, the
     * accidentals earlier in the measure, a tie from the previous note and the key.
     *
     * @param voice the voice
     * @param parsed the parsed note
     * @return the note data
     */
    private NoteData createNote(AbcVoice voice, AbcParsedNote parsed) {
        String pitchKey = parsed.getStep() + parsed.getOctave();
        int alter;
        NoteData note = new NoteData(parsed.getStep(), 0, parsed.getOctave());
        if (parsed.getAccidental() != null) {
            alter = fieldParser.accidentalAlter(parsed.getAccidental());
            voice.getMeasureAccidentals().put(pitchKey, alter);
            note.setAccidental(PitchSpeller.getInstance().accidentalName(alter));
        } else if (voice.getMeasureAccidentals().containsKey(pitchKey)) {
            alter = voice.getMeasureAccidentals().get(pitchKey);
        } else {
            NoteData tieOrigin = findTie(voice, parsed.getStep(), parsed.getOctave());
            if (tieOrigin != null) {
                alter = tieOrigin.getAlter();
            } else {
                alter = voice.getKey().alterOf(parsed.getStep());
            }
        }
        note.setAlter(alter);
        return note;
    }


    /**
     * Finds the pending tie of a pitch.
     *
     * @param voice the voice
     * @param step the step
     * @param octave the octave
     * @return the note where the tie starts, or null
     */
    private NoteData findTie(AbcVoice voice, String step, int octave) {
        for (NoteData origin : voice.getPendingTies()) {
            if (origin.getStep().equals(step) && origin.getOctave() == octave) {
                return origin;
            }
        }
        return null;
    }


    /**
     * Adds a note or chord event.
     *
     * @param voice the voice
     * @param notes the notes
     * @param duration the duration (before tuplet and broken rhythm)
     * @param ties the notes with a tie to the next note
     * @param tieAll true if all notes are tied (tie after a chord)
     */
    private void addNotes(AbcVoice voice, List<NoteData> notes, Fraction duration, List<NoteData> ties, boolean tieAll) {
        EventData event = new EventData();
        event.getNotes().addAll(notes);
        event.setDuration(duration);
        if (voice.isGrace()) {
            event.setGrace(true);
            event.setGraceSlash(voice.isGraceSlash());
            voice.getVoiceData().getEvents().add(event);
            return;
        }

        List<NoteData> origins = voice.getPendingTies();
        voice.setPendingTies(new ArrayList<>());
        for (NoteData note : notes) {
            NoteData origin = null;
            for (NoteData candidate : origins) {
                if (candidate.getStep().equals(note.getStep()) && candidate.getOctave() == note.getOctave()) {
                    origin = candidate;
                    break;
                }
            }
            if (origin != null) {
                removeIdentical(origins, origin);
                note.setTieStop(true);
                note.setAlter(origin.getAlter());
            }
        }
        for (NoteData unmatched : origins) {
            unmatched.setTieStart(false);
        }
        for (NoteData note : notes) {
            if (tieAll || containsIdentical(ties, note)) {
                note.setTieStart(true);
                voice.getPendingTies().add(note);
            }
        }
        addEvent(voice, event);
    }


    /**
     * Adds an event to the current voice and applies pending decorations, chord symbols,
     * annotations, slurs, tuplets and broken rhythm.
     *
     * @param voice the voice
     * @param event the event
     */
    private void addEvent(AbcVoice voice, EventData event) {
        for (String decoration : voice.getDecorations()) {
            if (decoration.endsWith("(") && (decoration.startsWith("crescendo") || decoration.startsWith("diminuendo")
                    || decoration.startsWith("<") || decoration.startsWith(">"))) {
                voice.setWedgeNumber(voice.getWedgeNumber() % 6 + 1);
            }
            decorationConverter.apply(decoration, event, Math.max(1, voice.getWedgeNumber()));
        }
        voice.getDecorations().clear();
        event.getHarmonies().addAll(voice.getHarmonies());
        voice.getHarmonies().clear();
        event.getDirections().addAll(voice.getDirections());
        voice.getDirections().clear();

        while (voice.getSlurStarts() > 0) {
            voice.setSlurCounter(voice.getSlurCounter() % 6 + 1);
            voice.getSlurs().push(voice.getSlurCounter());
            event.getSlurStarts().add(voice.getSlurCounter());
            voice.setSlurStarts(voice.getSlurStarts() - 1);
        }

        if (voice.getTupletRemaining() > 0 && !event.isMeasureRest()) {
            event.setDuration(event.getDuration().multiply(voice.getTupletNormal(), voice.getTupletActual()));
            event.setTuplet(voice.getTupletActual(), voice.getTupletNormal());
            if (voice.isTupletFirst()) {
                event.setTupletStart(true);
                voice.setTupletFirst(false);
            }
            voice.setTupletRemaining(voice.getTupletRemaining() - 1);
            if (voice.getTupletRemaining() == 0) {
                event.setTupletStop(true);
            }
        }

        if (voice.getBrokenFactor() != null && event.getDuration() != null) {
            event.setDuration(event.getDuration().multiply(voice.getBrokenFactor()));
            voice.setBrokenFactor(null);
        }

        voice.getVoiceData().getEvents().add(event);
        voice.setLastEvent(event);
        voice.setLastBarRepeatEnd(false);
        if (!event.isRest()) {
            if (!voice.isLineStarted()) {
                voice.getLineEvents().clear();
                voice.getLineBars().clear();
                voice.setVerse(0);
                voice.setLineStarted(true);
            }
            voice.getLineEvents().add(event);
            voice.getLineBars().add(voice.getMeasures().size());
        }
    }


    /**
     * Applies a broken rhythm between the last event and the next one.
     *
     * @param voice the voice
     * @param dotted true for &gt; (first note longer), false for &lt;
     * @param count the number of &gt; or &lt; characters
     */
    private void applyBroken(AbcVoice voice, boolean dotted, int count) {
        if (voice.getLastEvent() == null || voice.getLastEvent().getDuration() == null) {
            return;
        }
        long power = 1L << count;
        Fraction shorter = Fraction.of(1, power);
        Fraction longer = Fraction.of(2 * power - 1, power);
        if (dotted) {
            voice.getLastEvent().setDuration(voice.getLastEvent().getDuration().multiply(longer));
            voice.setBrokenFactor(shorter);
        } else {
            voice.getLastEvent().setDuration(voice.getLastEvent().getDuration().multiply(shorter));
            voice.setBrokenFactor(longer);
        }
    }


    /**
     * Reads a tuplet start {@code (p:q:r}.
     *
     * @param voice the voice
     * @param line the line
     * @param start the index after the parenthesis
     * @return the index after the tuplet
     */
    private int readTuplet(AbcVoice voice, String line, int start) {
        int[] values = {0, 0, 0};
        int i = start;
        int n = line.length();
        for (int k = 0; k < values.length; k++) {
            int digits = i;
            while (i < n && Character.isDigit(line.charAt(i))) {
                i++;
            }
            values[k] = fieldParser.parseInt(line.substring(digits, i), 0);
            if (k < values.length - 1 && i < n && line.charAt(i) == ':') {
                i++;
            } else {
                break;
            }
        }
        int p = values[0];
        if (p <= 1) {
            return i;
        }
        int q = values[1];
        if (q <= 0) {
            q = defaultTupletNormal(p, voice.getMeter());
        }
        int r = values[2];
        if (r <= 0) {
            r = p;
        }
        voice.setTupletActual(p);
        voice.setTupletNormal(q);
        voice.setTupletRemaining(r);
        voice.setTupletFirst(true);
        return i;
    }


    /**
     * Gets the default number of normal notes of a tuplet.
     *
     * @param p the number of actual notes
     * @param voiceMeter the meter
     * @return the number of normal notes
     */
    private int defaultTupletNormal(int p, AbcMeter voiceMeter) {
        switch (p) {
            case 2:
            case 4:
            case 8:
                return 3;
            case 3:
            case 6:
                return 2;
            default:
                if (voiceMeter != null && voiceMeter.isCompound()) {
                    return 3;
                }
                return 2;
        }
    }


    /**
     * Reads an ending (volta) start such as {@code [1}, {@code [1,3} or {@code [1-2}.
     *
     * @param voice the voice
     * @param line the line
     * @param start the index of the first digit
     * @return the index after the ending
     */
    private int readEnding(AbcVoice voice, String line, int start) {
        int i = start;
        int n = line.length();
        while (i < n && (Character.isDigit(line.charAt(i)) || line.charAt(i) == ',' || line.charAt(i) == '-')) {
            i++;
        }
        startEnding(voice, line.substring(start, i));
        return i;
    }


    /**
     * Starts an ending in the current measure (if empty) or the next measure.
     *
     * @param voice the voice
     * @param numbers the ending numbers, for example 1 or 1,3 or 1-2
     */
    private void startEnding(AbcVoice voice, String numbers) {
        String normalized = expandEnding(numbers);
        if (normalized.isEmpty()) {
            return;
        }
        voice.setEnding(normalized);
        voice.setEndingAfterRepeat(voice.isLastBarRepeatEnd());
        if (measureEmpty(voice)) {
            voice.getMeasure().setEndingNumber(normalized);
        }
    }


    /**
     * Expands an ending number list into the MusicXML form, for example 1-3 into "1, 2, 3".
     *
     * @param numbers the ABC ending numbers
     * @return the MusicXML ending number
     */
    private String expandEnding(String numbers) {
        List<String> result = new ArrayList<>();
        for (String part : numbers.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int dash = trimmed.indexOf('-');
            if (dash > 0) {
                int from = fieldParser.parseInt(trimmed.substring(0, dash), 0);
                int to = fieldParser.parseInt(trimmed.substring(dash + 1), 0);
                for (int k = from; k <= to && from > 0; k++) {
                    result.add(String.valueOf(k));
                }
            } else {
                result.add(trimmed);
            }
        }
        return String.join(", ", result);
    }


    /**
     * Reads a bar line with repeats and an optional ending number.
     *
     * @param voice the voice
     * @param line the line
     * @param start the index of the first bar character
     * @return the index after the bar line
     */
    private int readBar(AbcVoice voice, String line, int start) {
        int i = start;
        int n = line.length();
        StringBuilder bar = new StringBuilder();
        while (i < n && "|:[].".indexOf(line.charAt(i)) >= 0) {
            if (line.charAt(i) == '[' && i + 1 < n && Character.isDigit(line.charAt(i + 1))) {
                break;
            }
            if (line.charAt(i) == '.' && !(i + 1 < n && line.charAt(i + 1) == '|')) {
                break;
            }
            if (line.charAt(i) == '[' && bar.length() > 0 && !(i + 1 < n && line.charAt(i + 1) == '|')) {
                break;
            }
            bar.append(line.charAt(i));
            i++;
        }
        String text = bar.toString();
        boolean repeatEnd = text.startsWith(":");
        boolean repeatStart = text.endsWith(":");
        String core = text.replace(":", "");
        String style = null;
        if ("||".equals(core)) {
            style = "light-light";
        } else if ("|]".equals(core) || "|]|".equals(core)) {
            style = "light-heavy";
        } else if ("[|".equals(core)) {
            style = "heavy-light";
        } else if (".|".equals(core)) {
            style = "dashed";
        } else if ("[|]".equals(core) || "[]".equals(core)) {
            style = "none";
        }
        if (repeatEnd && repeatStart && core.isEmpty()) {
            // "::" is an end repeat followed by a start repeat
            style = null;
        }

        String endingNumbers = null;
        int digits = i;
        while (i < n && (Character.isDigit(line.charAt(i)) || (i > digits && (line.charAt(i) == ',' || line.charAt(i) == '-')))) {
            i++;
        }
        if (i > digits) {
            endingNumbers = line.substring(digits, i);
        }
        voice.setLastBarRepeatEnd(repeatEnd);
        boolean endsEnding = repeatEnd || "light-light".equals(style) || "light-heavy".equals(style) || endingNumbers != null;
        closeMeasure(voice, style, repeatEnd, repeatStart);
        if (endsEnding && endingNumbers == null && voice.getEnding() != null) {
            voice.setEnding(null);
            if (measureEmpty(voice)) {
                voice.getMeasure().setEndingNumber(null);
            }
        }
        if (endingNumbers != null) {
            startEnding(voice, endingNumbers);
        }
        return i;
    }


    /**
     * Closes the current measure at a bar line. A bar line at the start of an empty measure
     * modifies the previous measure (right side) and the current measure (left side).
     *
     * @param voice the voice
     * @param style the bar style of the right barline, or null
     * @param repeatEnd true for an end repeat
     * @param repeatStart true for a start repeat
     */
    private void closeMeasure(AbcVoice voice, String style, boolean repeatEnd, boolean repeatStart) {
        if (measureEmpty(voice) && voice.getMeasure().getDirections().isEmpty()) {
            if (voice.getMeasures().size() > 1) {
                MeasureData previous = voice.getMeasures().get(voice.getMeasures().size() - 2);
                if (repeatEnd) {
                    previous.setRepeatEnd(true, null);
                }
                if (style != null) {
                    previous.setRightBarStyle(style);
                }
                if (repeatEnd && voice.getEnding() == null) {
                    voice.getMeasure().setEndingNumber(null);
                }
            }
            if (repeatStart) {
                voice.getMeasure().setRepeatStart(true);
            }
            return;
        }
        if (repeatEnd) {
            voice.getMeasure().setRepeatEnd(true, null);
        }
        if (style != null) {
            voice.getMeasure().setRightBarStyle(style);
        }
        if (repeatEnd || "light-light".equals(style) || "light-heavy".equals(style) || voice.isEndingAfterRepeat()) {
            voice.setEnding(null);
            voice.setEndingAfterRepeat(false);
        }
        voice.setRepeatStartPending(repeatStart);
        flushDirections(voice);
        newMeasure(voice);
    }


    /**
     * Moves directions and chord symbols that are not followed by an event in the measure to the
     * end of the measure.
     *
     * @param voice the voice
     */
    private void flushDirections(AbcVoice voice) {
        Fraction end = voice.getVoiceData().getLength();
        for (DirectionImpl direction : voice.getDirections()) {
            voice.getMeasure().getDirections().add(new DirectionData(direction, end, 1));
        }
        voice.getDirections().clear();
        voice.getMeasure().getHarmonies().addAll(voice.getHarmonies());
        voice.getHarmonies().clear();
    }


    /**
     * Reads a lyrics line (w:) and aligns its syllables with the notes of the last music line of the voice.
     *
     * @param voice the voice
     * @param text the lyrics
     */
    private void readLyrics(AbcVoice voice, String text) {
        int verse = voice.getVerse() + 1;
        voice.setVerse(verse);
        List<String[]> items = tokenizeLyrics(text);
        int target = 0;
        boolean hyphenBefore = false;
        LyricImpl previous = null;
        for (String[] item : items) {
            String kind = item[0];
            if ("|".equals(kind)) {
                if (target > 0 && target < voice.getLineEvents().size()) {
                    int bar = voice.getLineBars().get(target - 1);
                    while (target < voice.getLineEvents().size() && voice.getLineBars().get(target) == bar) {
                        target++;
                    }
                }
                continue;
            }
            if (target >= voice.getLineEvents().size()) {
                break;
            }
            if ("_".equals(kind)) {
                if (previous != null) {
                    previous.setExtend("");
                }
                target++;
                continue;
            }
            if ("*".equals(kind)) {
                target++;
                continue;
            }
            boolean hyphenAfter = "-".equals(item[2]);
            String syllabic = "single";
            if (hyphenBefore && hyphenAfter) {
                syllabic = "middle";
            } else if (hyphenBefore) {
                syllabic = "end";
            } else if (hyphenAfter) {
                syllabic = "begin";
            }
            if (!item[1].isEmpty()) {
                previous = ScoreBuilder.getInstance().createLyric(verse, item[1], syllabic, false);
                voice.getLineEvents().get(target).getLyrics().add(previous);
            }
            hyphenBefore = hyphenAfter;
            target++;
        }
    }


    /**
     * Splits a lyrics line into items: syllables (with the separator that follows them), extension
     * underscores, skip stars and bar separators.
     *
     * @param text the lyrics
     * @return the items as {kind, text, separator}
     */
    private List<String[]> tokenizeLyrics(String text) {
        List<String[]> items = new ArrayList<>();
        StringBuilder syllable = new StringBuilder();
        boolean hasSyllable = false;
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < n && text.charAt(i + 1) == '-') {
                syllable.append('-');
                hasSyllable = true;
                i += 2;
                continue;
            }
            if (c == '~') {
                syllable.append(' ');
                hasSyllable = true;
                i++;
                continue;
            }
            if (c == '-') {
                items.add(new String[] {"s", syllable.toString(), "-"});
                syllable.setLength(0);
                hasSyllable = false;
                i++;
                continue;
            }
            if (Character.isWhitespace(c) || c == '_' || c == '*' || c == '|') {
                if (hasSyllable) {
                    items.add(new String[] {"s", syllable.toString(), " "});
                    syllable.setLength(0);
                    hasSyllable = false;
                }
                if (c != ' ' && !Character.isWhitespace(c)) {
                    items.add(new String[] {String.valueOf(c), "", ""});
                }
                i++;
                continue;
            }
            syllable.append(c);
            hasSyllable = true;
            i++;
        }
        if (hasSyllable) {
            items.add(new String[] {"s", syllable.toString(), " "});
        }
        return items;
    }


    /**
     * Finishes the tune: closes open voices and assembles the parts.
     *
     * @return the score data
     */
    private ScoreData finish() {
        if (!titles.isEmpty()) {
            score.setTitle(titles.get(0));
            if (titles.size() > 1) {
                score.setSubtitle(String.join(" / ", titles.subList(1, titles.size())));
            }
        }
        score.setSoftware("ABC notation");
        if (voices.isEmpty()) {
            voice();
        }
        for (AbcVoice voice : voices.values()) {
            if (voice.getMeasure() != null) {
                flushDirections(voice);
                if (measureEmpty(voice) && voice.getMeasures().size() > 1 && voice.getMeasure().getDirections().isEmpty()) {
                    voice.getMeasures().remove(voice.getMeasures().size() - 1);
                }
            }
            for (NoteData origin : voice.getPendingTies()) {
                origin.setTieStart(false);
            }
        }

        List<List<List<String>>> parts = new ArrayList<>();
        Set<String> used = new HashSet<>();
        if (layout != null) {
            for (List<List<String>> part : layout) {
                List<List<String>> staves = new ArrayList<>();
                for (List<String> staff : part) {
                    List<String> ids = new ArrayList<>();
                    for (String id : staff) {
                        if (voices.containsKey(id) && voices.get(id).getMeasure() != null && used.add(id)) {
                            ids.add(id);
                        }
                    }
                    if (!ids.isEmpty()) {
                        staves.add(ids);
                    }
                }
                if (!staves.isEmpty()) {
                    parts.add(staves);
                }
            }
        }
        for (AbcVoice voice : voices.values()) {
            if (voice.getMeasure() != null && used.add(voice.getId())) {
                List<List<String>> part = new ArrayList<>();
                List<String> staff = new ArrayList<>();
                staff.add(voice.getId());
                part.add(staff);
                parts.add(part);
            }
        }
        for (List<List<String>> part : parts) {
            score.getParts().add(buildPart(part));
        }
        return score;
    }


    /**
     * Builds a part from its staves and voices.
     *
     * @param staves the voice IDs of each staff
     * @return the part data
     */
    private PartData buildPart(List<List<String>> staves) {
        AbcVoice first = voices.get(staves.get(0).get(0));
        String name = first.getName();
        if (name == null && voices.size() > 1) {
            name = "AbcVoice " + first.getId();
        }
        if (name == null) {
            name = "Music";
        }
        PartData part = new PartData(name);
        part.setAbbreviation(first.getShortName());
        part.setStaves(staves.size());
        part.setMidiProgram(first.getProgram());
        part.setMidiChannel(first.getChannel());
        if (first.getTranspose() != 0) {
            part.setTranspose(first.getTranspose(), 0);
        }
        int measureCount = 0;
        for (List<String> staff : staves) {
            for (String id : staff) {
                measureCount = Math.max(measureCount, voices.get(id).getMeasures().size());
            }
        }
        for (int s = 0; s < staves.size(); s++) {
            int staffNumber = s + 1;
            int voiceNumber = s * VOICES_PER_STAFF + 1;
            for (String id : staves.get(s)) {
                AbcVoice voice = voices.get(id);
                if (voice.getClef() != null && "percussion".equals(voice.getClef().getSign())) {
                    part.setPercussion(true);
                }
                for (int m = 0; m < voice.getMeasures().size(); m++) {
                    MeasureData source = voice.getMeasures().get(m);
                    MeasureData target = part.measure(m);
                    if (id.equals(first.getId())) {
                        copyMeasureAttributes(source, target);
                    } else if (s > 0 && id.equals(staves.get(s).get(0))) {
                        target.getClefs().clear();
                    }
                    for (VoiceData data : source.getVoices()) {
                        VoiceData copy = target.voice(staffNumber, voiceNumber + data.getVoice() - 1);
                        copy.getEvents().addAll(data.getEvents());
                    }
                    for (DirectionData direction : source.getDirections()) {
                        target.getDirections().add(new DirectionData(direction.getDirection(), direction.getPosition(), staffNumber));
                    }
                    target.getHarmonies().addAll(source.getHarmonies());
                    for (ClefChange change : source.getClefs()) {
                        if (id.equals(staves.get(s).get(0))) {
                            target.addClef(staffNumber, change.getPosition(), change.getClef());
                        }
                    }
                }
                voiceNumber += voice.getMaxOverlay() + 1;
            }
        }
        for (int m = 0; m < measureCount; m++) {
            part.measure(m);
        }
        applyPartialMeasures(part);
        return part;
    }


    /**
     * Keeps the written length of partial measures at section boundaries: the first and last
     * measure, measures at repeats and measures next to special barlines (an anacrusis and its
     * complement). Other measures are filled to the time signature by the builder.
     *
     * @param part the part
     */
    private void applyPartialMeasures(PartData part) {
        List<MeasureData> measures = part.getMeasures();
        int beats = meter.getBeats();
        int beatType = meter.getBeatType();
        for (int m = 0; m < measures.size(); m++) {
            MeasureData current = measures.get(m);
            if (current.getBeats() != null) {
                beats = current.getBeats();
                beatType = current.getBeatType();
            }
            if (beats <= 0) {
                current.setLength(current.getContentLength());
                continue;
            }
            Fraction content = current.getContentLength();
            if (!content.isPositive() || content.compareTo(Fraction.of(beats, beatType)) >= 0) {
                continue;
            }
            MeasureData previous = null;
            if (m > 0) {
                previous = measures.get(m - 1);
            }
            MeasureData next = null;
            if (m + 1 < measures.size()) {
                next = measures.get(m + 1);
            }
            boolean boundary = m == 0 || next == null || current.isRepeatStart() || current.isRepeatEnd()
                    || current.getRightBarStyle() != null || (next != null && next.isRepeatStart())
                    || (previous != null && (previous.isRepeatEnd() || previous.getRightBarStyle() != null));
            if (boundary) {
                current.setLength(content);
                current.setImplicit(m == 0 || (current.isRepeatStart() && previous != null && previous.getLength() != null));
            }
        }
    }


    /**
     * Copies the measure level attributes (time, key, repeats, endings, bar styles, clefs of staff 1).
     *
     * @param source the measure of the first voice
     * @param target the measure of the part
     */
    private void copyMeasureAttributes(MeasureData source, MeasureData target) {
        if (source.getBeats() != null) {
            target.setTime(source.getBeats(), source.getBeatType(), source.getTimeSymbol());
        }
        if (source.getKeyFifths() != null) {
            target.setKey(source.getKeyFifths(), source.getKeyMode());
        }
        target.setRepeatStart(source.isRepeatStart());
        target.setRepeatEnd(source.isRepeatEnd(), source.getRepeatEndTimes());
        target.setEndingNumber(source.getEndingNumber());
        target.setRightBarStyle(source.getRightBarStyle());
        target.setLeftBarStyle(source.getLeftBarStyle());
    }


    /**
     * Removes a trailing comment from a field value.
     *
     * @param value the value
     * @return the value without comment
     */
    private String stripComment(String value) {
        int percent = value.indexOf('%');
        if (percent >= 0 && (percent == 0 || value.charAt(percent - 1) != '\\')) {
            return value.substring(0, percent);
        }
        return value;
    }


    /**
     * Returns the first non-null value.
     *
     * @param first the first value
     * @param second the second value
     * @return the first non-null value, or null
     */
    private String firstNonNull(String first, String second) {
        if (first != null) {
            return first;
        }
        return second;
    }

    /**
     * Checks if a list contains an object (compared by identity).
     *
     * @param list the list
     * @param item the object
     * @return true if the list contains the object
     */
    private boolean containsIdentical(List<NoteData> list, NoteData item) {
        for (NoteData entry : list) {
            if (entry == item) {
                return true;
            }
        }
        return false;
    }


    /**
     * Removes an object (compared by identity) from a list.
     *
     * @param list the list
     * @param item the object
     */
    private void removeIdentical(List<NoteData> list, NoteData item) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == item) {
                list.remove(i);
                return;
            }
        }
    }
}
