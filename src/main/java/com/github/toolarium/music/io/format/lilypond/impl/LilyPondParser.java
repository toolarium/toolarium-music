/*
 * LilyPondParser.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl;

import com.github.toolarium.music.io.converter.ChordSymbolParser;
import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondEvent;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondMark;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondPitch;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondScore;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondStaff;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondToken;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Parses LilyPond tokens into a LilyPondScore: the first \score (or the music at the top level), its staves and
 * voices with their notes, the marks of the staves, the header, the chord names and the lyrics. Variables are
 * expanded where they are used. Layout settings (\set, \override, \layout, Scheme expressions) are skipped.
 *
 * @author patrick
 */
public class LilyPondParser {
    private static final Logger LOG = LoggerFactory.getLogger(LilyPondParser.class);
    private static final int MAX_EXPANSIONS = 100_000;
    private static final int MAX_EVENTS = 1_000_000;
    private static final int VOICE_LIMIT = 16;
    private static final String OPEN = "{";
    private static final String CLOSE = "}";
    private static final String OPEN_SIMULTANEOUS = "<<";
    private static final String CLOSE_SIMULTANEOUS = ">>";
    private static final String EQUALS = "=";
    private static final String SLASH = "/";
    private static final String DOT = ".";
    private static final String BACKSLASH_SEPARATOR = "\\\\";
    private static final String ABOVE = "above";
    private static final String NOT_CLOSED = "Invalid LilyPond file: a block is not closed.";
    private static final String BELOW = "below";
    private static final Fraction DEFAULT_DURATION = Fraction.of(1, 4);
    private static final Set<String> DYNAMICS = new HashSet<>(Arrays.asList("ppppp", "pppp", "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff",
            "ffff", "fffff", "fp", "sf", "sff", "sp", "spp", "sfz", "rfz", "fz"));
    private static final Map<String, String> ARTICULATIONS = createArticulations();
    private static final Map<String, String> SHORT_ARTICULATIONS = createShortArticulations();
    private static final Set<String> STAFF_CONTEXTS = new HashSet<>(Arrays.asList("Staff", "RhythmicStaff", "TabStaff", "DrumStaff",
            "MensuralStaff", "VaticanaStaff", "PetrucciStaff", "KievanStaff", "GregorianTranscriptionStaff"));
    private static final Set<String> GROUP_CONTEXTS = new HashSet<>(Arrays.asList("PianoStaff", "GrandStaff"));

    private final String source;
    private final List<LilyPondToken> tokens;
    private final Map<String, List<LilyPondToken>> variables = new HashMap<>();
    private final LilyPondScore score = new LilyPondScore();
    private final LilyPondPitchConverter pitches = LilyPondPitchConverter.getInstance();
    private int pos;
    private int expansions;
    private int events;
    private String language = LilyPondPitchConverter.NEDERLANDS;
    private boolean scoreRead;
    private LilyPondStaff defaultStaff;
    private LilyPondStaff lastStaff;
    private int lastVoice;
    private boolean hideNextTempo;
    private char rehearsal = 'A';
    private final Map<String, Integer> verses = new HashMap<>();


    /**
     * The state of the music being parsed: position, last duration, octave mode, target staff and voice, tuplet and
     * grace state. Sequential music shares the state, simultaneous music works on copies.
     *
     * @author patrick
     */
    private static final class MusicState {
        private Fraction time = Fraction.ZERO;
        private Fraction duration = DEFAULT_DURATION;
        private LilyPondPitch relative;
        private int octaveOffset;
        private int[] transpose;
        private LilyPondStaff staff;
        private int voice = 1;
        private List<LilyPondStaff> group;
        private Fraction scale = Fraction.of(1, 1);
        private int tupletActual;
        private int tupletNormal;
        private boolean tupletPending;
        private boolean grace;
        private boolean graceSlash;
        private boolean chordMode;
        private List<int[]> lastChord;
        private int wedge;


        /**
         * Creates a copy of the state.
         *
         * @return the copy
         */
        MusicState copy() {
            MusicState copy = new MusicState();
            copy.time = time;
            copy.duration = duration;
            copy.relative = relative;
            copy.octaveOffset = octaveOffset;
            copy.transpose = transpose;
            copy.staff = staff;
            copy.voice = voice;
            copy.group = group;
            copy.scale = scale;
            copy.tupletActual = tupletActual;
            copy.tupletNormal = tupletNormal;
            copy.tupletPending = tupletPending;
            copy.grace = grace;
            copy.graceSlash = graceSlash;
            copy.chordMode = chordMode;
            copy.lastChord = lastChord;
            copy.wedge = wedge;
            return copy;
        }
    }


    /**
     * Constructor for LilyPondParser.
     *
     * @param source the LilyPond source
     * @param tokens the tokens of the source
     */
    public LilyPondParser(String source, List<LilyPondToken> tokens) {
        this.source = source;
        this.tokens = new ArrayList<>(tokens);
    }


    /**
     * Parses the tokens.
     *
     * @return the score
     * @throws IOException if the source is invalid or has no music
     */
    public LilyPondScore parse() throws IOException {
        while (pos < tokens.size()) {
            LilyPondToken token = tokens.get(pos);
            if (token.getType() == LilyPondToken.WORD && pos + 1 < tokens.size() && tokens.get(pos + 1).is(LilyPondToken.SYMBOL, EQUALS)) {
                readAssignment();
            } else if (token.getType() == LilyPondToken.COMMAND) {
                readTopLevelCommand(token.getText());
            } else if (isMusicStart(token)) {
                if (!scoreRead) {
                    parseMusic(new MusicState());
                    scoreRead = true;
                } else {
                    skipExpression();
                }
            } else {
                pos++;
            }
        }
        if (score.getParts().isEmpty()) {
            throw new IOException("Invalid LilyPond file: no music found.");
        }
        return score;
    }


    /**
     * Reads a command at the top level: \version, \language, \header, \score, blocks to skip and music.
     *
     * @param command the command with backslash
     * @throws IOException if the source is invalid
     */
    private void readTopLevelCommand(String command) throws IOException {
        switch (command) {
            case "\\version":
            case "\\include":
                pos++;
                skipIf(LilyPondToken.STRING);
                break;
            case "\\language":
                pos++;
                if (peekType() == LilyPondToken.STRING) {
                    language = next().getText();
                }
                break;
            case "\\header":
                pos++;
                readHeader();
                break;
            case "\\score":
                pos++;
                readScoreBlock();
                break;
            case "\\paper":
            case "\\layout":
            case "\\midi":
            case "\\book":
            case "\\bookpart":
                pos++;
                skipExpression();
                break;
            default:
                if (variables.containsKey(command.substring(1)) || isMusicStart(peek())) {
                    if (!scoreRead) {
                        parseMusic(new MusicState());
                        scoreRead = true;
                    } else {
                        skipExpression();
                    }
                } else {
                    pos++;
                }
                break;
        }
    }


    /**
     * Reads a \score block: the first one is parsed, further ones are skipped.
     *
     * @throws IOException if the source is invalid
     */
    private void readScoreBlock() throws IOException {
        if (scoreRead || !peekIs(LilyPondToken.SYMBOL, OPEN)) {
            skipExpression();
            return;
        }
        pos++;
        MusicState state = new MusicState();
        while (pos < tokens.size() && !peekIs(LilyPondToken.SYMBOL, CLOSE)) {
            String text = peek().getText();
            if (peekType() == LilyPondToken.COMMAND && ("\\layout".equals(text) || "\\midi".equals(text))) {
                pos++;
                skipExpression();
            } else if (peekType() == LilyPondToken.COMMAND && "\\header".equals(text)) {
                pos++;
                readHeader();
            } else {
                final int before = pos;
                parseElement(state);
                if (pos == before) {
                    pos++;
                }
            }
        }
        consumeClose();
        scoreRead = true;
    }


    /**
     * Reads a variable assignment (name = value); the tokens of the value are kept for the expansion.
     *
     * @throws IOException if the value is invalid
     */
    private void readAssignment() throws IOException {
        String name = next().getText();
        pos++;
        int start = pos;
        skipExpression();
        variables.put(name, new ArrayList<>(tokens.subList(start, pos)));
    }


    /**
     * Reads a \header block into the header fields of the score (only the first header counts).
     *
     * @throws IOException if the block is invalid
     */
    private void readHeader() throws IOException {
        if (!peekIs(LilyPondToken.SYMBOL, OPEN)) {
            return;
        }
        pos++;
        while (pos < tokens.size() && !peekIs(LilyPondToken.SYMBOL, CLOSE)) {
            if (peekType() == LilyPondToken.WORD && pos + 1 < tokens.size() && tokens.get(pos + 1).is(LilyPondToken.SYMBOL, EQUALS)) {
                String key = next().getText();
                pos++;
                String value = readText();
                if (value != null && !value.trim().isEmpty()) {
                    score.getHeader().putIfAbsent(key, value.trim());
                }
            } else {
                pos++;
            }
        }
        pos++;
    }


    /**
     * Reads a text value: a string, a Scheme string, or the strings and words of a \markup.
     *
     * @return the text, or null if the value is no text
     * @throws IOException if the value is invalid
     */
    private String readText() throws IOException {
        LilyPondToken token = peek();
        if (token == null) {
            return null;
        }
        if (token.getType() == LilyPondToken.STRING) {
            pos++;
            return token.getText();
        }
        if (token.getType() == LilyPondToken.SCHEME) {
            pos++;
            String text = token.getText();
            if (text.startsWith("#\"") && text.endsWith("\"")) {
                return text.substring(2, text.length() - 1);
            }
            return null;
        }
        if (token.is(LilyPondToken.COMMAND, "\\markup")) {
            pos++;
            int start = pos;
            skipExpression();
            StringBuilder text = new StringBuilder();
            for (LilyPondToken part : tokens.subList(start, pos)) {
                if (part.getType() == LilyPondToken.STRING || part.getType() == LilyPondToken.WORD || part.getType() == LilyPondToken.NUMBER) {
                    if (text.length() > 0) {
                        text.append(' ');
                    }
                    text.append(part.getText());
                }
            }
            return text.toString();
        }
        skipExpression();
        return null;
    }


    /**
     * Parses one music expression and returns the time after it.
     *
     * @param state the music state (its time is updated)
     * @return the time after the expression
     * @throws IOException if the source is invalid
     */
    private Fraction parseMusic(MusicState state) throws IOException {
        expandVariable();
        LilyPondToken token = peek();
        if (token == null) {
            return state.time;
        }
        if (token.is(LilyPondToken.SYMBOL, OPEN)) {
            pos++;
            while (pos < tokens.size() && !peekIs(LilyPondToken.SYMBOL, CLOSE)) {
                final int before = pos;
                parseElement(state);
                if (pos == before) {
                    pos++;
                }
            }
            consumeClose();
            return state.time;
        }
        if (token.is(LilyPondToken.SYMBOL, OPEN_SIMULTANEOUS)) {
            parseSimultaneous(state);
            return state.time;
        }
        parseElement(state);
        return state.time;
    }


    /**
     * Parses simultaneous music (&lt;&lt; ... &gt;&gt;): every element starts at the same time, \\ separates voices.
     *
     * @param state the music state (its time is set to the end of the longest element)
     * @throws IOException if the source is invalid
     */
    private void parseSimultaneous(MusicState state) throws IOException {
        pos++;
        Fraction start = state.time;
        Fraction end = start;
        int voice = state.voice;
        MusicState first = null;
        while (pos < tokens.size() && !peekIs(LilyPondToken.SYMBOL, CLOSE_SIMULTANEOUS)) {
            if (peekIs(LilyPondToken.COMMAND, BACKSLASH_SEPARATOR)) {
                pos++;
                voice++;
                continue;
            }
            final int before = pos;
            MusicState branch = state.copy();
            branch.time = start;
            branch.voice = voice;
            parseMusic(branch);
            end = end.max(branch.time);
            if (first == null) {
                first = branch;
            }
            if (pos == before) {
                pos++;
            }
        }
        consumeClose();
        state.time = end;
        if (first != null) {
            state.relative = first.relative;
            state.duration = first.duration;
        }
    }


    /**
     * Parses one element of sequential music: a note, a chord, a rest, a command, a bar check or a mark that belongs
     * to the previous note.
     *
     * @param state the music state
     * @throws IOException if the source is invalid
     */
    private void parseElement(MusicState state) throws IOException {
        expandVariable();
        LilyPondToken token = peek();
        if (token == null) {
            return;
        }
        if (token.is(LilyPondToken.SYMBOL, OPEN) || token.is(LilyPondToken.SYMBOL, OPEN_SIMULTANEOUS)) {
            parseMusic(state);
        } else if (token.getType() == LilyPondToken.WORD) {
            if (state.chordMode) {
                parseChordName(state);
            } else {
                parseNote(state);
            }
        } else if (token.is(LilyPondToken.SYMBOL, "<")) {
            parseChord(state);
        } else if (token.getType() == LilyPondToken.COMMAND) {
            parseCommand(state, token.getText());
        } else if (token.getType() == LilyPondToken.SYMBOL) {
            LilyPondEvent last = lastEvent(state);
            if (last != null && isPostEventStart(token)) {
                parsePostEvents(state, last);
            } else {
                pos++;
            }
        } else {
            pos++;
        }
    }


    /**
     * Parses a note, a rest (r, R, s) or a repeated chord (q) with its duration and its marks.
     *
     * @param state the music state
     * @throws IOException if the source is invalid
     */
    private void parseNote(MusicState state) throws IOException {
        LilyPondToken word = next();
        String name = word.getText();
        List<LilyPondPitch> notes = new ArrayList<>();
        boolean rest = false;
        boolean spacer = false;
        boolean measureRest = false;
        if ("r".equals(name)) {
            rest = true;
        } else if ("s".equals(name)) {
            spacer = true;
        } else if ("R".equals(name)) {
            measureRest = true;
        } else if ("q".equals(name) && state.lastChord != null) {
            for (int[] note : state.lastChord) {
                notes.add(LilyPondPitch.ofDiatonic(note[0], note[1]));
            }
        } else {
            int[] note = pitches.parseNoteName(name, language);
            if (note == null) {
                // an unknown word (for example a context property): ignored
                return;
            }
            LilyPondPitch pitch = readPitch(state, note);
            notes.add(pitch);
            state.lastChord = Arrays.asList(new int[] {pitch.getDiatonic(), pitch.getAlter()});
        }
        Fraction nominal = readDuration(state);
        if (peekIs(LilyPondToken.COMMAND, "\\rest")) {
            pos++;
            notes.clear();
            rest = true;
        }
        LilyPondEvent event = addEvent(state, notes, nominal);
        event.setInvisible(spacer);
        event.setMeasureRest(measureRest && !rest);
        parsePostEvents(state, event);
    }


    /**
     * Parses a chord (&lt;c e g&gt;4) with its duration and its marks.
     *
     * @param state the music state
     * @throws IOException if the source is invalid
     */
    private void parseChord(MusicState state) throws IOException {
        pos++;
        List<LilyPondPitch> notes = new ArrayList<>();
        List<LilyPondPitch> tied = new ArrayList<>();
        LilyPondPitch firstPitch = null;
        while (pos < tokens.size() && !peekIs(LilyPondToken.SYMBOL, ">")) {
            LilyPondToken token = next();
            if (token.getType() != LilyPondToken.WORD) {
                continue;
            }
            int[] note = pitches.parseNoteName(token.getText(), language);
            if (note == null) {
                continue;
            }
            LilyPondPitch pitch = readPitch(state, note);
            if (firstPitch == null) {
                firstPitch = pitch;
            }
            notes.add(pitch);
            if (peekIs(LilyPondToken.SYMBOL, "~")) {
                pos++;
                tied.add(pitch);
            }
        }
        pos++;
        if (firstPitch != null && state.relative != null) {
            // after a chord, the next note is relative to the first note of the chord
            state.relative = untransposed(state, firstPitch);
        }
        List<int[]> chord = new ArrayList<>();
        for (LilyPondPitch pitch : notes) {
            chord.add(new int[] {pitch.getDiatonic(), pitch.getAlter()});
        }
        state.lastChord = chord;
        LilyPondEvent event = addEvent(state, notes, readDuration(state));
        event.getTiedPitches().addAll(tied);
        parsePostEvents(state, event);
    }


    /**
     * Reads the octave marks of a note and creates its pitch (absolute, relative or fixed, then transposed).
     *
     * @param state the music state (its relative pitch is updated)
     * @param note the step index and the alteration
     * @return the pitch
     */
    private LilyPondPitch readPitch(MusicState state, int[] note) {
        int marks = readOctaveMarks();
        while (peekIs(LilyPondToken.SYMBOL, "!") || peekIs(LilyPondToken.SYMBOL, "?")) {
            pos++;
        }
        if (peekIs(LilyPondToken.SYMBOL, EQUALS)) {
            // octave check: ignored
            pos++;
            readOctaveMarks();
        }
        LilyPondPitch pitch;
        if (state.relative != null) {
            pitch = pitches.relative(state.relative, note, marks);
            state.relative = pitch;
        } else {
            pitch = pitches.absolute(note, marks + state.octaveOffset);
        }
        if (state.transpose != null) {
            pitch = pitches.transpose(pitch, state.transpose[0], state.transpose[1]);
        }
        return pitch;
    }


    /**
     * Gets the pitch before the transposition (the relative reference is not transposed).
     *
     * @param state the music state
     * @param pitch the transposed pitch
     * @return the pitch before the transposition
     */
    private LilyPondPitch untransposed(MusicState state, LilyPondPitch pitch) {
        if (state.transpose == null) {
            return pitch;
        }
        return pitches.transpose(pitch, -state.transpose[0], -state.transpose[1]);
    }


    /**
     * Reads the octave marks (' and ,) directly after a note name.
     *
     * @return the octave change (+1 for each ', -1 for each ,)
     */
    private int readOctaveMarks() {
        int marks = 0;
        while (true) {
            if (peekIs(LilyPondToken.SYMBOL, "'")) {
                marks++;
            } else if (peekIs(LilyPondToken.SYMBOL, ",")) {
                marks--;
            } else {
                return marks;
            }
            pos++;
        }
    }


    /**
     * Reads a duration (4, 8., 2*3, \breve); without a duration the last one is used.
     *
     * @param state the music state (its last duration is updated)
     * @return the nominal duration in whole notes
     */
    private Fraction readDuration(MusicState state) {
        Fraction base = null;
        if (peekType() == LilyPondToken.NUMBER) {
            int value = Integer.parseInt(next().getText());
            if (value > 0) {
                base = Fraction.of(1, value);
            }
        } else if (peekIs(LilyPondToken.COMMAND, "\\breve")) {
            pos++;
            base = Fraction.of(2, 1);
        } else if (peekIs(LilyPondToken.COMMAND, "\\longa")) {
            pos++;
            base = Fraction.of(4, 1);
        }
        if (base == null) {
            return state.duration;
        }
        Fraction duration = base;
        Fraction dot = base;
        while (peekIs(LilyPondToken.SYMBOL, DOT)) {
            pos++;
            dot = dot.multiply(1, 2);
            duration = duration.add(dot);
        }
        while (peekIs(LilyPondToken.SYMBOL, "*") && pos + 1 < tokens.size() && tokens.get(pos + 1).getType() == LilyPondToken.NUMBER) {
            pos++;
            long numerator = Long.parseLong(next().getText());
            long denominator = 1;
            if (peekIs(LilyPondToken.SYMBOL, SLASH) && pos + 1 < tokens.size() && tokens.get(pos + 1).getType() == LilyPondToken.NUMBER) {
                pos++;
                denominator = Long.parseLong(next().getText());
            }
            if (numerator > 0 && denominator > 0) {
                duration = duration.multiply(numerator, denominator);
            }
        }
        state.duration = duration;
        return duration;
    }


    /**
     * Adds an event to the target voice: its sounding length follows the tuplet, a grace note takes no time.
     *
     * @param state the music state (its time is advanced)
     * @param notes the pitches (empty for a rest)
     * @param nominal the nominal duration
     * @return the event
     * @throws IOException if the music has too many events
     */
    private LilyPondEvent addEvent(MusicState state, List<LilyPondPitch> notes, Fraction nominal) throws IOException {
        events++;
        if (events > MAX_EVENTS) {
            throw new IOException("LilyPond music has too many notes.");
        }
        final LilyPondStaff staff = staff(state);
        Fraction length = nominal;
        if (!state.grace) {
            length = nominal.multiply(state.scale);
        }
        LilyPondEvent event = new LilyPondEvent(state.time, length);
        event.getPitches().addAll(notes);
        if (state.grace) {
            event.setGrace(true, state.graceSlash);
        } else {
            state.time = state.time.add(length);
            if (state.tupletActual > 0) {
                event.setTuplet(state.tupletActual, state.tupletNormal);
                event.setTupletStart(state.tupletPending);
                state.tupletPending = false;
            }
        }
        staff.voice(state.voice).add(event);
        lastStaff = staff;
        lastVoice = state.voice;
        return event;
    }


    /**
     * Gets the last event of the target voice.
     *
     * @param state the music state
     * @return the last event, or null
     */
    private LilyPondEvent lastEvent(MusicState state) {
        LilyPondStaff staff = state.staff;
        if (staff == null) {
            staff = defaultStaff;
        }
        if (staff == null || !staff.getVoices().containsKey(state.voice) || staff.voice(state.voice).isEmpty()) {
            return null;
        }
        List<LilyPondEvent> voice = staff.voice(state.voice);
        return voice.get(voice.size() - 1);
    }


    /**
     * Gets the target staff; music outside of a staff goes to a default staff (a part of its own).
     *
     * @param state the music state
     * @return the staff
     */
    private LilyPondStaff staff(MusicState state) {
        if (state.staff != null) {
            return state.staff;
        }
        if (defaultStaff == null) {
            defaultStaff = new LilyPondStaff();
            score.getParts().add(new ArrayList<>(Arrays.asList(defaultStaff)));
        }
        state.staff = defaultStaff;
        return defaultStaff;
    }


    /**
     * Checks whether a symbol starts a mark of the previous note.
     *
     * @param token the token
     * @return true for ~, (, ), [, ], -, ^, _ and --
     */
    private boolean isPostEventStart(LilyPondToken token) {
        return "~()[]-^_--".contains(token.getText()) && token.getText().length() <= 2;
    }


    /**
     * Parses the marks after a note: tie, slurs, beams, articulations, dynamics, hairpins, texts and fingerings.
     *
     * @param state the music state
     * @param event the note
     * @throws IOException if the source is invalid
     */
    private void parsePostEvents(MusicState state, LilyPondEvent event) throws IOException {
        while (pos < tokens.size()) {
            LilyPondToken token = peek();
            String text = token.getText();
            if (token.getType() == LilyPondToken.SYMBOL) {
                if ("~".equals(text)) {
                    event.setTieStart(true);
                } else if ("(".equals(text)) {
                    event.getSlurStarts().add(1);
                } else if (")".equals(text)) {
                    event.getSlurStops().add(1);
                } else if ("--".equals(text)) {
                    event.getMarks().add("tenuto");
                } else if ("-".equals(text) || "^".equals(text) || "_".equals(text)) {
                    pos++;
                    parseDirectedMark(event, text);
                    continue;
                } else if (!"[".equals(text) && !"]".equals(text) && !":".equals(text)) {
                    return;
                }
                pos++;
                if (":".equals(text) && peekType() == LilyPondToken.NUMBER) {
                    // tremolo: ignored
                    pos++;
                }
            } else if (token.getType() == LilyPondToken.COMMAND && parseCommandMark(state, event, text)) {
                pos++;
            } else {
                return;
            }
        }
    }


    /**
     * Parses a mark after -, ^ or _: a short articulation, a text, a markup, a fingering or an articulation command.
     *
     * @param event the note
     * @param direction -, ^ (above) or _ (below)
     * @throws IOException if the source is invalid
     */
    private void parseDirectedMark(LilyPondEvent event, String direction) throws IOException {
        LilyPondToken token = peek();
        if (token == null) {
            return;
        }
        String placement = ABOVE;
        if ("_".equals(direction)) {
            placement = BELOW;
        }
        if (token.getType() == LilyPondToken.SYMBOL && SHORT_ARTICULATIONS.containsKey(token.getText())) {
            pos++;
            String mark = SHORT_ARTICULATIONS.get(token.getText());
            if (!mark.isEmpty()) {
                event.getMarks().add(mark);
            }
        } else if (token.getType() == LilyPondToken.STRING || token.is(LilyPondToken.COMMAND, "\\markup")) {
            String text = readText();
            if (text != null && !text.trim().isEmpty()) {
                event.getDirections().add(ScoreBuilder.getInstance().createWords(text.trim(), placement, null, null));
            }
        } else if (token.getType() == LilyPondToken.NUMBER) {
            // fingering: ignored
            pos++;
        } else if (token.getType() == LilyPondToken.COMMAND && ARTICULATIONS.containsKey(token.getText().substring(1))) {
            pos++;
            event.getMarks().add(ARTICULATIONS.get(token.getText().substring(1)));
        }
    }


    /**
     * Applies a command after a note: an articulation, a dynamic, a hairpin or a phrasing slur.
     *
     * @param state the music state
     * @param event the note
     * @param command the command with backslash
     * @return true if the command was a mark of the note
     */
    private boolean parseCommandMark(MusicState state, LilyPondEvent event, String command) {
        String name = command.substring(1);
        if (ARTICULATIONS.containsKey(name)) {
            event.getMarks().add(ARTICULATIONS.get(name));
        } else if (DYNAMICS.contains(name)) {
            event.getDirections().add(ScoreBuilder.getInstance().createDynamics(name));
        } else if ("\\<".equals(command) || "\\cresc".equals(command)) {
            closeWedge(state, event);
            state.wedge = 1;
            event.getDirections().add(ScoreBuilder.getInstance().createWedge("crescendo", 1));
        } else if ("\\>".equals(command) || "\\decresc".equals(command) || "\\dim".equals(command)) {
            closeWedge(state, event);
            state.wedge = 1;
            event.getDirections().add(ScoreBuilder.getInstance().createWedge("diminuendo", 1));
        } else if ("\\!".equals(command)) {
            closeWedge(state, event);
        } else if ("\\(".equals(command)) {
            event.getSlurStarts().add(2);
        } else if ("\\)".equals(command)) {
            event.getSlurStops().add(2);
        } else {
            return false;
        }
        return true;
    }


    /**
     * Ends an open hairpin on a note.
     *
     * @param state the music state
     * @param event the note
     */
    private void closeWedge(MusicState state, LilyPondEvent event) {
        if (state.wedge > 0) {
            event.getDirections().add(ScoreBuilder.getInstance().createWedge("stop", state.wedge));
            state.wedge = 0;
        }
    }


    /**
     * Parses a command in music.
     *
     * @param state the music state
     * @param command the command with backslash
     * @throws IOException if the source is invalid
     */
    private void parseCommand(MusicState state, String command) throws IOException {
        pos++;
        switch (command) {
            case "\\relative":
                parseRelative(state);
                break;
            case "\\fixed":
            case "\\absolute":
            case "\\transpose":
                parsePitchMode(state, command);
                break;
            case "\\new":
            case "\\context":
                parseContext(state);
                break;
            case "\\tuplet":
            case "\\times":
                parseTuplet(state, "\\times".equals(command));
                break;
            case "\\grace":
            case "\\acciaccatura":
            case "\\appoggiatura":
            case "\\slashedGrace":
                parseGrace(state, "\\acciaccatura".equals(command) || "\\slashedGrace".equals(command));
                break;
            case "\\repeat":
                parseRepeat(state);
                break;
            case "\\chords":
            case "\\chordmode":
                parseChordMode(state);
                break;
            case "\\addlyrics":
                parseLyrics(lastStaff, lastVoice);
                break;
            case "\\lyricsto":
                parseLyricsTo();
                break;
            case "\\lyricmode":
            case "\\drummode":
            case "\\drums":
            case "\\figuremode":
            case "\\figures":
            case "\\markup":
            case "\\markuplist":
            case "\\layout":
            case "\\midi":
                skipExpression();
                break;
            default:
                parseSettingCommand(state, command);
                break;
        }
    }


    /**
     * Parses a command that sets a property of the staff: time, key, clef, tempo, bar, partial, mark, \set and the
     * commands to skip with their arguments (override, tweak, once, revert, unset).
     *
     * @param state the music state
     * @param command the command with backslash
     * @throws IOException if the source is invalid
     */
    private void parseSettingCommand(MusicState state, String command) throws IOException {
        switch (command) {
            case "\\transposition":
                parseTransposition(state);
                break;
            case "\\time":
                parseTime(state);
                break;
            case "\\key":
                parseKey(state);
                break;
            case "\\clef":
                parseClef(state);
                break;
            case "\\tempo":
                parseTempo(state);
                break;
            case "\\bar":
                if (peekType() == LilyPondToken.STRING) {
                    addMark(state, new LilyPondMark(LilyPondMark.BAR, state.time, next().getText(), null, null));
                }
                break;
            case "\\partial":
                Fraction partial = readDuration(state.copy());
                if (state.time.isZero()) {
                    score.setPartial(partial);
                }
                break;
            case "\\mark":
                parseRehearsalMark(state);
                break;
            case "\\set":
                parseSet(state);
                break;
            case "\\override":
            case "\\revert":
            case "\\unset":
            case "\\tweak":
                skipSetting(command);
                break;
            default:
                LilyPondEvent last = lastEvent(state);
                if (last != null && parseCommandMark(state, last, command)) {
                    return;
                }
                if (variables.containsKey(command.substring(1))) {
                    pos--;
                    parseMusic(state);
                }
                break;
        }
    }


    /**
     * Parses \relative [pitch] music: the notes are relative to the previous note.
     *
     * @param state the music state
     * @throws IOException if the source is invalid
     */
    private void parseRelative(MusicState state) throws IOException {
        LilyPondPitch reference = new LilyPondPitch("F", 0, 3);
        if (peekType() == LilyPondToken.WORD) {
            int[] note = pitches.parseNoteName(peek().getText(), language);
            if (note != null) {
                pos++;
                reference = pitches.absolute(note, readOctaveMarks());
            }
        }
        LilyPondPitch saved = state.relative;
        state.relative = reference;
        parseMusic(state);
        state.relative = saved;
    }


    /**
     * Parses \fixed pitch music, \absolute music and \transpose from to music.
     *
     * @param state the music state
     * @param command the command with backslash
     * @throws IOException if the source is invalid
     */
    private void parsePitchMode(MusicState state, String command) throws IOException {
        LilyPondPitch savedRelative = state.relative;
        final int savedOffset = state.octaveOffset;
        int[] savedTranspose = state.transpose;
        if ("\\fixed".equals(command)) {
            LilyPondPitch fixed = readAbsolutePitch();
            state.relative = null;
            if (fixed != null) {
                state.octaveOffset = fixed.getOctave() - 3;
            }
        } else if ("\\absolute".equals(command)) {
            state.relative = null;
            state.octaveOffset = 0;
        } else {
            LilyPondPitch from = readAbsolutePitch();
            LilyPondPitch to = readAbsolutePitch();
            if (from != null && to != null) {
                int steps = to.getDiatonic() - from.getDiatonic();
                int semitones = pitches.semitoneOf(to) - pitches.semitoneOf(from);
                if (savedTranspose != null) {
                    steps += savedTranspose[0];
                    semitones += savedTranspose[1];
                }
                state.transpose = new int[] {steps, semitones};
            }
        }
        parseMusic(state);
        state.relative = savedRelative;
        state.octaveOffset = savedOffset;
        state.transpose = savedTranspose;
    }


    /**
     * Reads an absolute pitch (a note name with octave marks).
     *
     * @return the pitch, or null if the next token is no note name
     */
    private LilyPondPitch readAbsolutePitch() {
        if (peekType() != LilyPondToken.WORD) {
            return null;
        }
        int[] note = pitches.parseNoteName(peek().getText(), language);
        if (note == null) {
            return null;
        }
        pos++;
        return pitches.absolute(note, readOctaveMarks());
    }


    /**
     * Parses \new or \context: a staff, a voice, a group of staves, chord names or lyrics.
     *
     * @param state the music state
     * @throws IOException if the source is invalid
     */
    private void parseContext(MusicState state) throws IOException {
        if (peekType() != LilyPondToken.WORD) {
            return;
        }
        String type = next().getText();
        String id = null;
        if (peekIs(LilyPondToken.SYMBOL, EQUALS)) {
            pos++;
            if (peekType() == LilyPondToken.STRING || peekType() == LilyPondToken.WORD) {
                id = next().getText();
            }
        }
        LilyPondStaff staff = null;
        if (STAFF_CONTEXTS.contains(type)) {
            staff = new LilyPondStaff();
            if (state.group != null) {
                state.group.add(staff);
            } else {
                score.getParts().add(new ArrayList<>(Arrays.asList(staff)));
            }
        }
        if (peekIs(LilyPondToken.COMMAND, "\\with")) {
            pos++;
            parseWith(staff);
        }
        MusicState inner = state.copy();
        if (staff != null) {
            inner.staff = staff;
            inner.voice = 1;
        } else if (GROUP_CONTEXTS.contains(type)) {
            List<LilyPondStaff> group = new ArrayList<>();
            score.getParts().add(group);
            inner.group = group;
        } else if ("Voice".equals(type)) {
            LilyPondStaff target = staff(inner);
            int voice = target.getVoices().size() + 1;
            if (voice > VOICE_LIMIT) {
                voice = VOICE_LIMIT;
            }
            inner.voice = voice;
            target.voice(voice);
            if (id != null) {
                target.getVoiceNames().put(id, voice);
            }
        } else if ("ChordNames".equals(type)) {
            inner.chordMode = true;
        } else if ("Lyrics".equals(type)) {
            parseLyricsContext();
            return;
        } else if (!"StaffGroup".equals(type) && !"ChoirStaff".equals(type) && !"Score".equals(type) && !"Devnull".equals(type)) {
            skipExpression();
            return;
        }
        parseMusic(inner);
        state.time = state.time.max(inner.time);
    }


    /**
     * Parses the \with block of a context: instrumentName and shortInstrumentName name the staff.
     *
     * @param staff the staff, or null for other contexts
     * @throws IOException if the block is invalid
     */
    private void parseWith(LilyPondStaff staff) throws IOException {
        if (!peekIs(LilyPondToken.SYMBOL, OPEN)) {
            return;
        }
        pos++;
        while (pos < tokens.size() && !peekIs(LilyPondToken.SYMBOL, CLOSE)) {
            if (peekType() == LilyPondToken.WORD && pos + 1 < tokens.size() && tokens.get(pos + 1).is(LilyPondToken.SYMBOL, EQUALS)) {
                String key = next().getText();
                pos++;
                applyStaffProperty(staff, key, readText());
            } else if (peekIs(LilyPondToken.SYMBOL, OPEN)) {
                skipExpression();
            } else {
                pos++;
            }
        }
        pos++;
    }


    /**
     * Applies a property of a staff: instrumentName or shortInstrumentName.
     *
     * @param staff the staff, or null
     * @param key the property name
     * @param value the value, or null
     */
    private void applyStaffProperty(LilyPondStaff staff, String key, String value) {
        if (staff == null || value == null || value.trim().isEmpty()) {
            return;
        }
        if ("instrumentName".equals(key)) {
            staff.setName(value.trim());
        } else if ("shortInstrumentName".equals(key)) {
            staff.setAbbreviation(value.trim());
        } else if ("midiInstrument".equals(key)) {
            staff.setProgram(LilyPondInstrumentConverter.getInstance().getProgram(value));
        }
    }


    /**
     * Parses \set [Context.]property = value: the instrument names are kept, everything else is skipped.
     *
     * @param state the music state
     * @throws IOException if the value is invalid
     */
    private void parseSet(MusicState state) throws IOException {
        String key = null;
        while (pos < tokens.size() && !peekIs(LilyPondToken.SYMBOL, EQUALS)) {
            LilyPondToken token = next();
            if (token.getType() == LilyPondToken.WORD) {
                key = token.getText();
            } else if (!token.is(LilyPondToken.SYMBOL, DOT)) {
                return;
            }
        }
        pos++;
        if ("tempoHideNote".equals(key)) {
            // the next tempo mark is shown without its metronome mark
            hideNextTempo = true;
            readText();
            return;
        }
        applyStaffProperty(staff(state), key, readText());
    }


    /**
     * Skips a setting command with its arguments: \override Grob.property = value, \revert Grob.property,
     * unset property, \tweak property value.
     *
     * @param command the command with backslash
     * @throws IOException if the value is invalid
     */
    private void skipSetting(String command) throws IOException {
        while (pos < tokens.size() && (peekType() == LilyPondToken.WORD || peekIs(LilyPondToken.SYMBOL, DOT)
                || peekType() == LilyPondToken.SCHEME && peek().getText().startsWith("#'"))) {
            pos++;
        }
        if ("\\override".equals(command) && peekIs(LilyPondToken.SYMBOL, EQUALS)) {
            pos++;
            skipExpression();
        } else if ("\\tweak".equals(command)) {
            skipExpression();
        }
    }


    /**
     * Parses \tuplet n/d [duration] music (or \times d/n music).
     *
     * @param state the music state
     * @param inverted true for \times (the fraction is the time scale, not the tuplet ratio)
     * @throws IOException if the source is invalid
     */
    private void parseTuplet(MusicState state, boolean inverted) throws IOException {
        int[] fraction = readFraction();
        if (fraction == null) {
            return;
        }
        int actual = fraction[0];
        int normal = fraction[1];
        if (inverted) {
            actual = fraction[1];
            normal = fraction[0];
        }
        if (peekType() == LilyPondToken.NUMBER) {
            // the duration of the tuplet groups: ignored
            readDuration(state.copy());
        }
        final Fraction savedScale = state.scale;
        final int savedActual = state.tupletActual;
        final int savedNormal = state.tupletNormal;
        state.scale = state.scale.multiply(normal, actual);
        state.tupletActual = actual;
        state.tupletNormal = normal;
        state.tupletPending = true;
        LilyPondEvent before = lastEvent(state);
        parseMusic(state);
        LilyPondEvent last = lastEvent(state);
        if (last != null && last != before) {
            last.setTupletStop(true);
        }
        state.scale = savedScale;
        state.tupletActual = savedActual;
        state.tupletNormal = savedNormal;
        state.tupletPending = false;
    }


    /**
     * Reads a fraction n/d.
     *
     * @return the numerator and the denominator, or null if there is no fraction
     */
    private int[] readFraction() {
        if (peekType() != LilyPondToken.NUMBER || pos + 2 >= tokens.size() || !tokens.get(pos + 1).is(LilyPondToken.SYMBOL, SLASH)
                || tokens.get(pos + 2).getType() != LilyPondToken.NUMBER) {
            return null;
        }
        int numerator = Integer.parseInt(next().getText());
        pos++;
        int denominator = Integer.parseInt(next().getText());
        if (numerator <= 0 || denominator <= 0) {
            return null;
        }
        return new int[] {numerator, denominator};
    }


    /**
     * Parses grace notes: \grace, \acciaccatura, \appoggiatura or \slashedGrace music.
     *
     * @param state the music state
     * @param slash true for grace notes with a slash
     * @throws IOException if the source is invalid
     */
    private void parseGrace(MusicState state, boolean slash) throws IOException {
        final boolean savedGrace = state.grace;
        final boolean savedSlash = state.graceSlash;
        state.grace = true;
        state.graceSlash = slash;
        parseMusic(state);
        state.grace = savedGrace;
        state.graceSlash = savedSlash;
    }


    /**
     * Parses \repeat type count music [\alternative { music ... }]: volta repeats are marked, unfold, percent and
     * tremolo repeats are written out.
     *
     * @param state the music state
     * @throws IOException if the source is invalid
     */
    private void parseRepeat(MusicState state) throws IOException {
        String type = "volta";
        if (peekType() == LilyPondToken.WORD) {
            type = next().getText();
        }
        int count = 2;
        if (peekType() == LilyPondToken.NUMBER) {
            count = Math.max(1, Math.min(Integer.parseInt(next().getText()), 100));
        }
        if (!"volta".equals(type) && !"segno".equals(type)) {
            int bodyStart = pos;
            for (int i = 0; i < count; i++) {
                pos = bodyStart;
                parseMusic(state);
            }
            return;
        }
        if (state.time.isPositive()) {
            addMark(state, new LilyPondMark(LilyPondMark.REPEAT_START, state.time, null, null, null));
        }
        parseMusic(state);
        expandVariable();
        if (!peekIs(LilyPondToken.COMMAND, "\\alternative")) {
            addMark(state, new LilyPondMark(LilyPondMark.REPEAT_END, state.time, null, new int[] {count}, null));
            return;
        }
        pos++;
        if (!peekIs(LilyPondToken.SYMBOL, OPEN)) {
            return;
        }
        pos++;
        List<Integer> alternatives = new ArrayList<>();
        int number = 1;
        while (pos < tokens.size() && !peekIs(LilyPondToken.SYMBOL, CLOSE)) {
            final int before = pos;
            addMark(state, new LilyPondMark(LilyPondMark.ENDING_START, state.time, String.valueOf(number), null, null));
            parseMusic(state);
            alternatives.add(number);
            addMark(state, new LilyPondMark(LilyPondMark.ENDING_STOP, state.time, String.valueOf(number), null, null));
            if (number < count) {
                addMark(state, new LilyPondMark(LilyPondMark.REPEAT_END, state.time, null, new int[] {count}, null));
            }
            number++;
            if (pos == before) {
                pos++;
            }
        }
        pos++;
    }


    /**
     * Parses \chordmode music (or \chords music): the chord names become chord symbols.
     *
     * @param state the music state
     * @throws IOException if the source is invalid
     */
    private void parseChordMode(MusicState state) throws IOException {
        MusicState inner = state.copy();
        inner.chordMode = true;
        parseMusic(inner);
    }


    /**
     * Parses a chord name in chord mode (c1, g4:7, a2:m7/e, r1): the root, the duration, the modifier and the bass.
     *
     * @param state the music state (its time is advanced)
     */
    private void parseChordName(MusicState state) {
        String name = next().getText();
        int[] root = null;
        if (!"r".equals(name) && !"s".equals(name) && !"R".equals(name)) {
            root = pitches.parseNoteName(name, language);
            readOctaveMarks();
        }
        final Fraction duration = readDuration(state);
        String modifier = null;
        if (peekIs(LilyPondToken.SYMBOL, ":")) {
            LilyPondToken colon = next();
            StringBuilder text = new StringBuilder();
            int end = colon.getEnd();
            while (pos < tokens.size() && peek().getStart() == end && !peekIs(LilyPondToken.SYMBOL, SLASH)) {
                LilyPondToken part = next();
                text.append(part.getText());
                end = part.getEnd();
            }
            modifier = text.toString();
        }
        int[] bass = null;
        if (peekIs(LilyPondToken.SYMBOL, SLASH)) {
            pos++;
            if (peekIs(LilyPondToken.SYMBOL, "+")) {
                pos++;
            }
            if (peekType() == LilyPondToken.WORD) {
                bass = pitches.parseNoteName(next().getText(), language);
                readOctaveMarks();
            }
        }
        if (root != null) {
            String text = pitches.chordText(root, modifier, bass);
            HarmonyImpl harmony = ChordSymbolParser.getInstance().parse(text);
            if (harmony != null) {
                score.getHarmonies().add(new LilyPondMark(LilyPondMark.HARMONY, state.time, text, null, harmony));
            }
        }
        state.time = state.time.add(duration.multiply(state.scale));
    }


    /**
     * Parses \time n/d (an optional beat structure before the fraction is skipped).
     *
     * @param state the music state
     */
    private void parseTime(MusicState state) {
        while (peekType() == LilyPondToken.NUMBER && pos + 1 < tokens.size() && tokens.get(pos + 1).is(LilyPondToken.SYMBOL, ",")) {
            pos += 2;
        }
        int[] fraction = readFraction();
        if (fraction != null) {
            addMark(state, new LilyPondMark(LilyPondMark.TIME, state.time, null, fraction, null));
        }
    }


    /**
     * Parses \key pitch \mode.
     *
     * @param state the music state
     */
    private void parseKey(MusicState state) {
        if (peekType() != LilyPondToken.WORD) {
            return;
        }
        int[] tonic = pitches.parseNoteName(next().getText(), language);
        readOctaveMarks();
        String mode = "major";
        if (peekType() == LilyPondToken.COMMAND && pitches.isMode(peek().getText().substring(1))) {
            mode = next().getText().substring(1);
        }
        if (tonic == null) {
            return;
        }
        if (state.transpose != null) {
            LilyPondPitch moved = pitches.transpose(pitches.absolute(tonic, 0), state.transpose[0], state.transpose[1]);
            tonic = new int[] {"CDEFGAB".indexOf(moved.getStep()), moved.getAlter()};
        }
        String musicXmlMode = "major";
        if ("minor".equals(mode) || "aeolian".equals(mode)) {
            musicXmlMode = "minor";
        } else if (!"major".equals(mode) && !"ionian".equals(mode)) {
            musicXmlMode = mode;
        }
        addMark(state, new LilyPondMark(LilyPondMark.KEY, state.time, musicXmlMode, new int[] {pitches.keyFifths(tonic, mode)}, null));
    }


    /**
     * Parses \clef name (a word, a word with octave such as treble_8, or a string).
     *
     * @param state the music state
     */
    private void parseClef(MusicState state) {
        String name = null;
        if (peekType() == LilyPondToken.STRING) {
            name = next().getText();
        } else if (peekType() == LilyPondToken.WORD) {
            LilyPondToken word = next();
            StringBuilder text = new StringBuilder(word.getText());
            int end = word.getEnd();
            while (pos < tokens.size() && peek().getStart() == end && (peekIs(LilyPondToken.SYMBOL, "_") || peekIs(LilyPondToken.SYMBOL, "^")
                    || peekType() == LilyPondToken.NUMBER)) {
                LilyPondToken part = next();
                text.append(part.getText());
                end = part.getEnd();
            }
            name = text.toString();
        }
        if (name == null) {
            return;
        }
        ClefImpl clef = pitches.clef(name);
        if (clef != null) {
            addMark(state, new LilyPondMark(LilyPondMark.CLEF, state.time, name, null, clef));
        }
    }


    /**
     * Parses \tempo [text] [unit = bpm]: \tempo "Allegro" 4 = 120, \tempo 4. = 60, \tempo "Andante".
     *
     * @param state the music state
     * @throws IOException if the text is invalid
     */
    private void parseTempo(MusicState state) throws IOException {
        String text = null;
        if (peekType() == LilyPondToken.STRING || peekIs(LilyPondToken.COMMAND, "\\markup")) {
            text = readText();
        }
        double bpm = 0;
        String beatUnit = "quarter";
        int dots = 0;
        if (peekType() == LilyPondToken.NUMBER && pos + 1 < tokens.size()
                && (tokens.get(pos + 1).is(LilyPondToken.SYMBOL, EQUALS) || tokens.get(pos + 1).is(LilyPondToken.SYMBOL, DOT))) {
            int unit = Integer.parseInt(next().getText());
            while (peekIs(LilyPondToken.SYMBOL, DOT)) {
                pos++;
                dots++;
            }
            String type = NoteValueConverter.getInstance().typeOfDenominator(Math.max(1, unit));
            if (type != null) {
                beatUnit = type;
            }
            if (peekIs(LilyPondToken.SYMBOL, EQUALS)) {
                pos++;
                if (peekType() == LilyPondToken.NUMBER) {
                    bpm = Double.parseDouble(next().getText());
                }
                if (peekIs(LilyPondToken.SYMBOL, "-") && pos + 1 < tokens.size() && tokens.get(pos + 1).getType() == LilyPondToken.NUMBER) {
                    // a tempo range (100-120): the lower value
                    pos += 2;
                }
            }
        }
        final boolean hidden = hideNextTempo;
        hideNextTempo = false;
        if (bpm <= 0 && text == null) {
            return;
        }
        DirectionImpl direction;
        if (hidden && bpm > 0 && text != null) {
            // a playback tempo without metronome mark
            direction = ScoreBuilder.getInstance().createTempo(0, beatUnit, 0, text);
            Fraction unit = NoteValueConverter.getInstance().toFraction(beatUnit, dots);
            double quarters = bpm;
            if (unit != null) {
                quarters = bpm * unit.getNumerator() * 4 / unit.getDenominator();
            }
            SoundImpl sound = new SoundImpl();
            sound.setTempo(BigDecimal.valueOf(quarters).stripTrailingZeros().toPlainString());
            direction.setSound(sound);
        } else {
            direction = ScoreBuilder.getInstance().createTempo(bpm, beatUnit, dots, text);
        }
        addMark(state, new LilyPondMark(LilyPondMark.DIRECTION, state.time, null, null, direction));
    }


    /**
     * Parses \transposition pitch: the written middle C sounds at the pitch (for example bes for a B flat
     * instrument).
     *
     * @param state the music state
     */
    private void parseTransposition(MusicState state) {
        LilyPondPitch sounding = readAbsolutePitch();
        if (sounding == null) {
            return;
        }
        LilyPondPitch middleC = new LilyPondPitch("C", 0, 4);
        int diatonic = sounding.getDiatonic() - middleC.getDiatonic();
        int semitones = pitches.semitoneOf(sounding) - pitches.semitoneOf(middleC);
        if (diatonic != 0 || semitones != 0) {
            staff(state).setTranspose(new int[] {diatonic, semitones});
        }
    }


    /**
     * Parses \mark: \default (the next letter), a string, a number or a markup.
     *
     * @param state the music state
     * @throws IOException if the text is invalid
     */
    private void parseRehearsalMark(MusicState state) throws IOException {
        String text = null;
        if (peekIs(LilyPondToken.COMMAND, "\\default")) {
            pos++;
            text = String.valueOf(rehearsal);
            rehearsal++;
        } else if (peekType() == LilyPondToken.NUMBER) {
            text = next().getText();
        } else if (peekType() == LilyPondToken.SCHEME) {
            text = next().getText().substring(1);
        } else {
            text = readText();
        }
        if (text != null && !text.trim().isEmpty()) {
            addMark(state, new LilyPondMark(LilyPondMark.DIRECTION, state.time, null, null, ScoreBuilder.getInstance().createRehearsal(text.trim())));
        }
    }


    /**
     * Adds a mark to the target staff.
     *
     * @param state the music state
     * @param mark the mark
     */
    private void addMark(MusicState state, LilyPondMark mark) {
        if (state.chordMode) {
            return;
        }
        staff(state).getMarks().add(mark);
    }


    /**
     * Parses \new Lyrics with \lyricsto "voice" { ... } or a lyrics block.
     *
     * @throws IOException if the source is invalid
     */
    private void parseLyricsContext() throws IOException {
        if (peekIs(LilyPondToken.COMMAND, "\\with")) {
            pos++;
            skipExpression();
        }
        expandVariable();
        if (peekIs(LilyPondToken.COMMAND, "\\lyricsto")) {
            pos++;
            parseLyricsTo();
        } else {
            skipExpression();
        }
    }


    /**
     * Parses \lyricsto "voice" lyrics: the lyrics belong to the named voice.
     *
     * @throws IOException if the source is invalid
     */
    private void parseLyricsTo() throws IOException {
        String name = null;
        if (peekType() == LilyPondToken.STRING || peekType() == LilyPondToken.WORD) {
            name = next().getText();
        }
        for (List<LilyPondStaff> part : score.getParts()) {
            for (LilyPondStaff staff : part) {
                Integer voice = staff.getVoiceNames().get(name);
                if (voice != null) {
                    parseLyrics(staff, voice);
                    return;
                }
            }
        }
        parseLyrics(lastStaff, lastVoice);
    }


    /**
     * Parses a lyrics block and adds the syllables to the notes of a voice.
     *
     * @param staff the staff of the voice, or null
     * @param voice the voice number
     * @throws IOException if the source is invalid
     */
    private void parseLyrics(LilyPondStaff staff, int voice) throws IOException {
        expandVariable();
        if (peekIs(LilyPondToken.COMMAND, "\\lyricmode")) {
            pos++;
            expandVariable();
        }
        if (!peekIs(LilyPondToken.SYMBOL, OPEN)) {
            skipExpression();
            return;
        }
        int open = pos;
        skipExpression();
        LilyPondToken first = tokens.get(open);
        LilyPondToken last = tokens.get(pos - 1);
        if (staff == null || last.getStart() <= first.getEnd()) {
            return;
        }
        String key = System.identityHashCode(staff) + ":" + voice;
        int verse = verses.merge(key, 1, Integer::sum);
        List<String> syllables = LilyPondLyricsConverter.getInstance().split(source.substring(first.getEnd(), last.getStart()));
        LilyPondLyricsConverter.getInstance().assign(staff.voice(voice), syllables, verse);
    }


    /**
     * Expands a variable reference at the current position into the tokens of its value.
     *
     * @throws IOException if too many variables are expanded (for example a recursive definition)
     */
    private void expandVariable() throws IOException {
        while (pos < tokens.size() && peekType() == LilyPondToken.COMMAND && variables.containsKey(peek().getText().substring(1))) {
            expansions++;
            if (expansions > MAX_EXPANSIONS) {
                throw new IOException("LilyPond variables are expanded too often (recursive definition).");
            }
            List<LilyPondToken> value = variables.get(peek().getText().substring(1));
            tokens.remove(pos);
            tokens.addAll(pos, value);
        }
    }


    /**
     * Skips one expression: a block in braces or angle brackets with everything before it (commands and their
     * arguments), or a single string, number or Scheme expression.
     *
     * @throws IOException if a block is not closed
     */
    private void skipExpression() throws IOException {
        LilyPondToken token = peek();
        if (token == null) {
            return;
        }
        if (token.getType() == LilyPondToken.STRING || token.getType() == LilyPondToken.NUMBER || token.getType() == LilyPondToken.SCHEME) {
            pos++;
            return;
        }
        int limit = tokens.size();
        while (pos < limit) {
            token = peek();
            if (token.is(LilyPondToken.SYMBOL, OPEN) || token.is(LilyPondToken.SYMBOL, OPEN_SIMULTANEOUS)) {
                skipBlock();
                return;
            }
            if (token.is(LilyPondToken.SYMBOL, CLOSE) || token.is(LilyPondToken.SYMBOL, CLOSE_SIMULTANEOUS)) {
                return;
            }
            pos++;
            while (token.getType() == LilyPondToken.WORD && (peekIs(LilyPondToken.SYMBOL, "'") || peekIs(LilyPondToken.SYMBOL, ","))) {
                // the octave marks of a pitch argument (for example c'' after relative)
                pos++;
            }
            if (token.getType() == LilyPondToken.WORD && !peekIs(LilyPondToken.SYMBOL, EQUALS) && peekType() != LilyPondToken.COMMAND
                    && peekType() != LilyPondToken.WORD && !peekIs(LilyPondToken.SYMBOL, OPEN) && !peekIs(LilyPondToken.SYMBOL, OPEN_SIMULTANEOUS)) {
                return;
            }
        }
    }


    /**
     * Skips a block in braces or double angle brackets with all nested blocks.
     *
     * @throws IOException if the block is not closed
     */
    private void skipBlock() throws IOException {
        int depth = 0;
        while (pos < tokens.size()) {
            LilyPondToken token = next();
            if (token.is(LilyPondToken.SYMBOL, OPEN) || token.is(LilyPondToken.SYMBOL, OPEN_SIMULTANEOUS)) {
                depth++;
            } else if (token.is(LilyPondToken.SYMBOL, CLOSE) || token.is(LilyPondToken.SYMBOL, CLOSE_SIMULTANEOUS)) {
                depth--;
                if (depth == 0) {
                    return;
                }
            }
        }
        throw new IOException(NOT_CLOSED);
    }


    /**
     * Consumes the closing bracket of a block.
     *
     * @throws IOException if the source ends before the block is closed
     */
    private void consumeClose() throws IOException {
        if (pos >= tokens.size()) {
            throw new IOException(NOT_CLOSED);
        }
        pos++;
    }


    /**
     * Checks whether a token starts music at the top level.
     *
     * @param token the token
     * @return true for a block or a music command
     */
    private boolean isMusicStart(LilyPondToken token) {
        if (token == null) {
            return false;
        }
        if (token.is(LilyPondToken.SYMBOL, OPEN) || token.is(LilyPondToken.SYMBOL, OPEN_SIMULTANEOUS)) {
            return true;
        }
        if (token.getType() != LilyPondToken.COMMAND) {
            return false;
        }
        String text = token.getText();
        return "\\relative".equals(text) || "\\new".equals(text) || "\\context".equals(text) || "\\fixed".equals(text) || "\\transpose".equals(text)
                || "\\chords".equals(text) || "\\absolute".equals(text);
    }


    /**
     * Skips the next token if it has a type.
     *
     * @param type the type
     */
    private void skipIf(int type) {
        if (peekType() == type) {
            pos++;
        }
    }


    /**
     * Gets the token at the current position.
     *
     * @return the token, or null at the end
     */
    private LilyPondToken peek() {
        if (pos >= tokens.size()) {
            return null;
        }
        return tokens.get(pos);
    }


    /**
     * Gets the type of the token at the current position.
     *
     * @return the type, or 0 at the end
     */
    private int peekType() {
        LilyPondToken token = peek();
        if (token == null) {
            return 0;
        }
        return token.getType();
    }


    /**
     * Checks the token at the current position.
     *
     * @param type the type
     * @param text the text
     * @return true if the token has the type and the text
     */
    private boolean peekIs(int type, String text) {
        LilyPondToken token = peek();
        return token != null && token.is(type, text);
    }


    /**
     * Gets the token at the current position and moves to the next one.
     *
     * @return the token
     */
    private LilyPondToken next() {
        LilyPondToken token = tokens.get(pos);
        pos++;
        if (LOG.isDebugEnabled() && pos == tokens.size()) {
            LOG.debug("Reached the end of the LilyPond tokens.");
        }
        return token;
    }


    /**
     * Creates the articulations of the articulation commands (without backslash).
     *
     * @return the MusicXML mark by command name
     */
    private static Map<String, String> createArticulations() {
        Map<String, String> result = new HashMap<>();
        result.put("staccato", "staccato");
        result.put("accent", "accent");
        result.put("tenuto", "tenuto");
        result.put("marcato", "strong-accent");
        result.put("staccatissimo", "staccatissimo");
        result.put("portato", "detached-legato");
        result.put("fermata", "fermata");
        result.put("trill", "trill-mark");
        result.put("mordent", "mordent");
        result.put("prall", "inverted-mordent");
        result.put("turn", "turn");
        return result;
    }


    /**
     * Creates the articulations of the short forms after -, ^ or _ (an empty mark is skipped).
     *
     * @return the MusicXML mark by symbol
     */
    private static Map<String, String> createShortArticulations() {
        Map<String, String> result = new HashMap<>();
        result.put(DOT, "staccato");
        result.put(">", "accent");
        result.put("-", "tenuto");
        result.put("^", "strong-accent");
        result.put("!", "staccatissimo");
        result.put("_", "detached-legato");
        result.put("+", "");
        return result;
    }
}
