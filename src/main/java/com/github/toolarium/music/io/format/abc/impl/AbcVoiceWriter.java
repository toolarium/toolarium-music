/*
 * AbcVoiceWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl;

import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.TypeAndDots;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcKey;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterEvent;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterMark;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterMeasure;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterPlan;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterStaff;
import com.github.toolarium.music.model.notation.ISlur;
import com.github.toolarium.music.model.notation.ITied;
import com.github.toolarium.music.model.notation.ITuplet;
import com.github.toolarium.music.model.note.IBeam;
import com.github.toolarium.music.model.note.ILyric;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.note.ITie;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Writes the music of one ABC voice (one voice of a staff), measure by measure and four measures per line: notes,
 * chords, rests, grace notes and tuplets with accidentals, ties, slurs, beams, decorations, chord symbols and
 * annotations, and after every line the lyrics of the line (one w: line per verse). The accidentals follow the
 * ABC rules: an accidental is valid to the end of the measure, a tied note keeps the accidental of its start.
 *
 * @author patrick
 */
public class AbcVoiceWriter {
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String SKIP = "*";
    private static final String EXTEND = "_";
    private static final String KEY_FIELD = "[K:";
    private static final int MEASURES_PER_LINE = 4;
    private final AbcWriterPlan plan;
    private final AbcWriterStaff staff;
    private final int voice;
    private final boolean primary;
    private final String id;
    private final AbcNotationWriter writer = AbcNotationWriter.getInstance();
    private final Map<String, Integer> measureAccidentals = new HashMap<>();
    private Map<String, Integer> pendingTies = new HashMap<>();
    private final Map<String, List<String>> lineVerses = new TreeMap<>();
    private final Map<String, Boolean> extending = new HashMap<>();
    private final List<String> verseNames = new ArrayList<>();
    private int lineNotes;
    private AbcKey key = AbcFieldParser.getInstance().parseKey("C");


    /**
     * Constructor for AbcVoiceWriter.
     *
     * @param plan the plan
     * @param staff the staff
     * @param voice the voice number
     * @param primary true for the first voice of the staff (with the marks of the staff)
     * @param id the ABC voice ID
     */
    public AbcVoiceWriter(AbcWriterPlan plan, AbcWriterStaff staff, int voice, boolean primary, String id) {
        this.plan = plan;
        this.staff = staff;
        this.voice = voice;
        this.primary = primary;
        this.id = id;
    }


    /**
     * Writes the voice.
     *
     * @return the lines (music lines, each followed by its lyrics lines)
     */
    public List<String> write() {
        collectVerses();
        List<String> lines = new ArrayList<>();
        List<AbcWriterMeasure> measures = plan.getMeasures();
        StringBuilder line = new StringBuilder();
        boolean previousIrregular = false;
        for (int m = 0; m < measures.size(); m++) {
            AbcWriterMeasure measure = measures.get(m);
            measureAccidentals.clear();
            if (m == 0 && measure.isRepeatStart()) {
                line.append("|: ");
            }
            if (measure.getEndingStart() != null) {
                line.append('[').append(measure.getEndingStart().replaceAll("\\s+", "")).append(' ');
            }
            previousIrregular = writeMeter(line, m, previousIrregular);
            line.append(measureContent(measure));
            line.append(' ').append(barline(m));
            if ((m + 1) % MEASURES_PER_LINE == 0 || m + 1 == measures.size()) {
                lines.add(line.toString().trim());
                writeLyrics(lines);
                line.setLength(0);
            } else {
                line.append(' ');
            }
        }
        return lines;
    }


    /**
     * Writes a meter change, or a meter of the measure length for a measure in the middle that does not fill its
     * meter (the meter of the next measure is written again).
     *
     * @param line the line
     * @param m the measure index
     * @param previousIrregular true if the previous measure had a meter of its own length
     * @return true if this measure has a meter of its own length
     */
    private boolean writeMeter(StringBuilder line, int m, boolean previousIrregular) {
        List<AbcWriterMeasure> measures = plan.getMeasures();
        AbcWriterMeasure measure = measures.get(m);
        Fraction meter = Fraction.of(measure.getMeterCount(), measure.getMeterUnit());
        boolean atRepeat = measure.isRepeatStart() || measure.isRepeatEnd() || measure.getEndingStart() != null
                || m + 1 < measures.size() && (measures.get(m + 1).isRepeatStart() || measures.get(m + 1).getEndingStart() != null)
                || m > 0 && measures.get(m - 1).isRepeatEnd();
        boolean irregular = m > 0 && m + 1 < measures.size() && !measure.getLength().equals(meter) && !atRepeat;
        if (irregular) {
            line.append("[M:").append(timeOf(measure.getLength(), measure.getMeterUnit())).append("] ");
            return true;
        }
        if (m > 0 && (measure.isMeterChange() || previousIrregular)) {
            line.append("[M:").append(meter(measure)).append("] ");
        }
        return false;
    }


    /**
     * Gets the meter of a measure (C and C| for the common and cut time symbols).
     *
     * @param measure the measure
     * @return the meter
     */
    static String meter(AbcWriterMeasure measure) {
        if ("common".equals(measure.getMeterSymbol())) {
            return "C";
        }
        if ("cut".equals(measure.getMeterSymbol())) {
            return "C|";
        }
        return measure.getMeterCount() + "/" + measure.getMeterUnit();
    }


    /**
     * Gets the meter of a measure length (for example 2/4 or 5/8).
     *
     * @param length the length in whole notes
     * @param unit the beat unit of the meter
     * @return the meter
     */
    private String timeOf(Fraction length, int unit) {
        long numerator = length.getNumerator() * unit;
        if (numerator % length.getDenominator() == 0) {
            return numerator / length.getDenominator() + "/" + unit;
        }
        return length.getNumerator() + "/" + length.getDenominator();
    }


    /**
     * Gets the bar line after a measure: repeats, the bar style or a single bar line.
     *
     * @param m the measure index
     * @return the bar line
     */
    private String barline(int m) {
        List<AbcWriterMeasure> measures = plan.getMeasures();
        AbcWriterMeasure measure = measures.get(m);
        boolean nextRepeatStart = m + 1 < measures.size() && measures.get(m + 1).isRepeatStart();
        if (measure.isRepeatEnd() && nextRepeatStart) {
            return "::";
        }
        if (measure.isRepeatEnd()) {
            return ":|";
        }
        if (nextRepeatStart) {
            return "|:";
        }
        if (measure.getBarStyle() != null) {
            return measure.getBarStyle();
        }
        return "|";
    }


    /**
     * Writes the content of a measure: the events with the marks of the staff, the gaps filled with invisible rests.
     *
     * @param measure the measure
     * @return the content
     */
    private String measureContent(AbcWriterMeasure measure) {
        List<AbcWriterEvent> events = new ArrayList<>();
        Map<Integer, List<AbcWriterEvent>> voices = measure.getStaves().get(staff.getNumber());
        if (voices != null && voices.get(voice) != null) {
            events = voices.get(voice);
        }
        List<AbcWriterMark> marks = new ArrayList<>();
        if (measure.getMarks().get(staff.getNumber()) != null) {
            for (AbcWriterMark mark : measure.getMarks().get(staff.getNumber())) {
                // the key and clef belong to every voice of the staff, the other marks to its first voice
                if (primary || mark.getText().startsWith(KEY_FIELD) || mark.getText().startsWith(AbcWriterPlanner.CLEF_MARK)) {
                    marks.add(mark);
                }
            }
            marks.sort(Comparator.comparing(AbcWriterMark::getPosition));
        }
        boolean[] used = new boolean[marks.size()];
        StringBuilder out = new StringBuilder();
        List<int[]> tuplets = tuplets(events);
        Map<Integer, String> beams = beams(events);
        boolean inBeam = false;
        List<String> graces = new ArrayList<>();
        boolean graceSlash = false;
        Fraction cursor = Fraction.ZERO;
        for (int i = 0; i < events.size(); i++) {
            AbcWriterEvent event = events.get(i);
            if (event.isGrace()) {
                graceSlash = graces.isEmpty() && "yes".equals(event.getFirst().getGrace().getSlash()) || graceSlash;
                graces.add(noteText(event, measure, false));
                continue;
            }
            if (event.getStart().compareTo(cursor) > 0) {
                gap(out, marks, used, cursor, event.getStart());
            }
            Fraction end = event.getStart().add(event.getLength());
            if (!inBeam && out.length() > 0) {
                out.append(' ');
            }
            // inline fields before the tuplet, decorations, chord symbols and annotations after it
            out.append(marksBefore(marks, used, end, true));
            for (int[] tuplet : tuplets) {
                if (tuplet[0] == i) {
                    out.append('(').append(tuplet[2]).append(':').append(tuplet[3]).append(':').append(tuplet[4]);
                }
            }
            out.append(marksBefore(marks, used, end, false));
            if (!graces.isEmpty()) {
                out.append('{');
                if (graceSlash) {
                    out.append('/');
                }
                out.append(String.join("", graces)).append('}');
                graces.clear();
                graceSlash = false;
            }
            out.append(noteText(event, measure, true));
            String beam = beams.get(i);
            inBeam = "[".equals(beam) || inBeam && !"]".equals(beam);
            cursor = cursor.max(end);
        }
        if (cursor.compareTo(measure.getLength()) < 0) {
            gap(out, marks, used, cursor, measure.getLength());
        }
        String remaining = marksBefore(marks, used, null, true) + marksBefore(marks, used, null, false);
        if (!remaining.isEmpty()) {
            out.append(' ').append(remaining);
        }
        // grace notes at the end of a measure have no main note in ABC: they are left out
        return out.toString().trim();
    }


    /**
     * Fills a gap with invisible rests: the gap is divided at the positions of marks, so a mark is placed exactly.
     *
     * @param out the output
     * @param marks the marks
     * @param used the marks written already
     * @param from the start of the gap
     * @param to the end of the gap
     */
    private void gap(StringBuilder out, List<AbcWriterMark> marks, boolean[] used, Fraction from, Fraction to) {
        List<Fraction> points = new ArrayList<>();
        for (int i = 0; i < marks.size(); i++) {
            Fraction position = marks.get(i).getPosition();
            if (!used[i] && position.compareTo(from) > 0 && position.compareTo(to) < 0 && !points.contains(position)) {
                points.add(position);
            }
        }
        points.add(to);
        Fraction position = from;
        for (Fraction point : points) {
            if (point.compareTo(position) <= 0) {
                continue;
            }
            boolean first = true;
            for (TypeAndDots value : NoteValueConverter.getInstance().split(point.subtract(position))) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                if (first) {
                    out.append(marksBefore(marks, used, point, true)).append(marksBefore(marks, used, point, false));
                    first = false;
                }
                out.append('x').append(writer.length(NoteValueConverter.getInstance().toFraction(value.getType(), value.getDots())));
            }
            position = point;
        }
    }


    /**
     * Gets the marks before a position that are not written yet: inline fields, decorations, chord symbols and
     * annotations (a mark within a note is written before it).
     *
     * @param marks the marks
     * @param used the marks written already
     * @param to the end (exclusive), or null for all remaining marks
     * @param fields true for the inline fields (and clef changes), false for the other marks
     * @return the marks
     */
    private String marksBefore(List<AbcWriterMark> marks, boolean[] used, Fraction to, boolean fields) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < marks.size(); i++) {
            AbcWriterMark mark = marks.get(i);
            boolean field = mark.getText().startsWith("[") || mark.getText().startsWith(AbcWriterPlanner.CLEF_MARK);
            if (!used[i] && field == fields && (to == null || mark.getPosition().compareTo(to) < 0)) {
                String text = mark.getText();
                if (text.startsWith(AbcWriterPlanner.CLEF_MARK)) {
                    text = "[V:" + id + " " + text + "]";
                } else if (text.startsWith(KEY_FIELD)) {
                    key = AbcFieldParser.getInstance().parseKey(text.substring(KEY_FIELD.length(), text.length() - 1));
                    measureAccidentals.clear();
                }
                result.append(text);
                used[i] = true;
            }
        }
        return result.toString();
    }


    /**
     * Writes an event: a rest, a note or a chord with its accidentals, length, ties, slurs and decorations; a main
     * note also adds its lyrics.
     *
     * @param event the event
     * @param measure the measure
     * @param main true for a main note (false for a grace note)
     * @return the text
     */
    private String noteText(AbcWriterEvent event, AbcWriterMeasure measure, boolean main) {
        INote first = event.getFirst();
        StringBuilder text = new StringBuilder();
        List<ISlur> slurs = slurs(event);
        if (main) {
            for (String decoration : writer.decorations(first.getNotations())) {
                text.append(decoration);
            }
            for (ISlur slur : slurs) {
                if (START.equals(slur.getType())) {
                    text.append('(');
                }
            }
        }
        if (event.isRest()) {
            Fraction meter = Fraction.of(measure.getMeterCount(), measure.getMeterUnit());
            boolean wholeMeasure = event.getStart().isZero() && event.getLength().equals(measure.getLength()) && measure.getLength().equals(meter)
                    && ("yes".equals(first.getRest().getMeasure()) || "whole".equals(first.getType()));
            boolean invisible = "no".equals(first.getPrintObject());
            String rest = "z";
            if (wholeMeasure && invisible) {
                rest = "X";
            } else if (wholeMeasure) {
                rest = "Z";
            } else if (invisible) {
                rest = "x";
            }
            text.append(rest);
            if (!wholeMeasure) {
                text.append(writer.length(nominal(event)));
            }
            if (main) {
                pendingTies = new HashMap<>();
            }
        } else {
            Map<String, Integer> origins = pendingTies;
            Map<String, Integer> tied = new HashMap<>();
            boolean allTied = true;
            for (INote note : event.getNotes()) {
                allTied = allTied && tieStart(note);
            }
            if (event.getNotes().size() > 1) {
                text.append('[');
            }
            for (INote note : event.getNotes()) {
                text.append(pitch(note, origins));
                if (tieStart(note)) {
                    tied.put(pitchKey(note), alter(note));
                    if (event.getNotes().size() > 1 && !allTied) {
                        text.append('-');
                    }
                }
            }
            if (event.getNotes().size() > 1) {
                text.append(']');
            }
            text.append(writer.length(nominal(event)));
            if (allTied && !tied.isEmpty()) {
                text.append('-');
            }
            if (main) {
                pendingTies = tied;
                lyrics(first);
            }
        }
        if (main) {
            for (ISlur slur : slurs) {
                if (STOP.equals(slur.getType())) {
                    text.append(')');
                }
            }
        }
        return text.toString();
    }


    /**
     * Gets the pitch of a note with the accidental it needs: the accidental is written if the alteration differs
     * from the alteration of the key, an earlier accidental in the measure or the start of a tie, or if the note
     * shows an accidental.
     *
     * @param note the note
     * @param origins the alterations of the tied notes of the previous event by pitch
     * @return the pitch
     */
    private String pitch(INote note, Map<String, Integer> origins) {
        String step;
        int octave;
        if (note.getPitch() != null && note.getPitch().getStep() != null && note.getPitch().getOctave() != null) {
            step = note.getPitch().getStep();
            octave = note.getPitch().getOctave();
        } else if (note.getUnpitched() != null && note.getUnpitched().getDisplayStep() != null && note.getUnpitched().getDisplayOctave() != null) {
            step = note.getUnpitched().getDisplayStep();
            octave = note.getUnpitched().getDisplayOctave();
        } else {
            return "C";
        }
        String pitchKey = step + octave;
        int alter = alter(note);
        Integer effective = measureAccidentals.get(pitchKey);
        if (effective == null) {
            effective = origins.get(pitchKey);
        }
        if (effective == null) {
            effective = key.alterOf(step);
        }
        String accidental = "";
        if (alter != effective || note.getAccidental() != null && !measureAccidentals.containsKey(pitchKey) && origins.get(pitchKey) == null) {
            accidental = writer.accidental(alter);
            measureAccidentals.put(pitchKey, alter);
        }
        return accidental + writer.note(step, octave);
    }


    /**
     * Gets the written (nominal) length of an event: its note value, or the length scaled back by the tuplet ratio.
     *
     * @param event the event
     * @return the length in whole notes
     */
    private Fraction nominal(AbcWriterEvent event) {
        INote note = event.getFirst();
        int dots = 0;
        if (note.getDots() != null) {
            dots = note.getDots().size();
        }
        Fraction typed = writer.typeLength(note.getType(), dots);
        if (event.isGrace()) {
            if (typed != null) {
                return typed;
            }
            return Fraction.of(1, 16);
        }
        Fraction nominal = event.getLength();
        if (note.getTimeModification() != null && note.getTimeModification().getActualNotes() != null
                && note.getTimeModification().getNormalNotes() != null && note.getTimeModification().getNormalNotes() > 0) {
            nominal = nominal.multiply(note.getTimeModification().getActualNotes(), note.getTimeModification().getNormalNotes());
        }
        if (nominal.isPositive()) {
            return nominal;
        }
        if (typed != null) {
            return typed;
        }
        return AbcNotationWriter.UNIT;
    }


    /**
     * Collects the lyrics of a note for the line: a syllable (with - before the next syllable of the word), an
     * extension (_) or a skip (*) in every verse.
     *
     * @param note the first note of the event
     */
    private void lyrics(INote note) {
        if (verseNames.isEmpty()) {
            return;
        }
        Map<String, ILyric> lyrics = new HashMap<>();
        if (note.getLyrics() != null) {
            int index = 1;
            for (ILyric lyric : note.getLyrics()) {
                if (lyric.getText() != null) {
                    lyrics.put(verse(lyric, index), lyric);
                }
                index++;
            }
        }
        for (String verse : verseNames) {
            List<String> tokens = lineVerses.computeIfAbsent(verse, k -> new ArrayList<>());
            ILyric lyric = lyrics.get(verse);
            if (lyric == null) {
                if (Boolean.TRUE.equals(extending.get(verse))) {
                    tokens.add(EXTEND);
                } else {
                    tokens.add(SKIP);
                }
                continue;
            }
            String syllable = lyric.getText().trim().replace("-", "\\-").replace(' ', '~').replace('_', '~').replace('*', '~').replace('|', '~');
            if ("begin".equals(lyric.getSyllabic()) || "middle".equals(lyric.getSyllabic())) {
                syllable = syllable + "-";
            }
            tokens.add(syllable);
            extending.put(verse, lyric.getExtend() != null);
        }
        lineNotes++;
    }


    /**
     * Writes the lyrics of the current line (one w: line per verse) and starts a new line.
     *
     * @param lines the lines (added to)
     */
    private void writeLyrics(List<String> lines) {
        if (lineNotes > 0) {
            boolean any = false;
            for (List<String> tokens : lineVerses.values()) {
                for (String token : tokens) {
                    any = any || !SKIP.equals(token) && !EXTEND.equals(token);
                }
            }
            if (any) {
                for (String verse : verseNames) {
                    List<String> tokens = lineVerses.getOrDefault(verse, new ArrayList<>());
                    lines.add("w:" + String.join(" ", tokens));
                }
            }
        }
        lineVerses.clear();
        lineNotes = 0;
    }


    /**
     * Collects the verses of the voice (all lyric numbers of its notes) in order.
     */
    private void collectVerses() {
        for (AbcWriterMeasure measure : plan.getMeasures()) {
            Map<Integer, List<AbcWriterEvent>> voices = measure.getStaves().get(staff.getNumber());
            if (voices == null || voices.get(voice) == null) {
                continue;
            }
            for (AbcWriterEvent event : voices.get(voice)) {
                if (event.isGrace() || event.isRest() || event.getFirst().getLyrics() == null) {
                    continue;
                }
                int index = 1;
                for (ILyric lyric : event.getFirst().getLyrics()) {
                    String verse = verse(lyric, index);
                    if (lyric.getText() != null && !verseNames.contains(verse)) {
                        verseNames.add(verse);
                    }
                    index++;
                }
            }
        }
        verseNames.sort(Comparator.naturalOrder());
    }


    /**
     * Gets the verse of a lyric.
     *
     * @param lyric the lyric
     * @param index the index of the lyric at the note (1-based)
     * @return the verse
     */
    private String verse(ILyric lyric, int index) {
        if (lyric.getNumber() != null && !lyric.getNumber().trim().isEmpty()) {
            return lyric.getNumber().trim();
        }
        return String.valueOf(index);
    }


    /**
     * Gets the slurs of an event.
     *
     * @param event the event
     * @return the slur starts and stops of its notes
     */
    private List<ISlur> slurs(AbcWriterEvent event) {
        List<ISlur> result = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        for (INote note : event.getNotes()) {
            if (note.getNotations() != null && note.getNotations().getSlurs() != null) {
                for (ISlur slur : note.getNotations().getSlurs()) {
                    int number = 1;
                    if (slur.getNumber() != null) {
                        number = slur.getNumber();
                    }
                    int key = number * 2;
                    if (START.equals(slur.getType())) {
                        key++;
                    }
                    if (!numbers.contains(key)) {
                        numbers.add(key);
                        result.add(slur);
                    }
                }
            }
        }
        return result;
    }


    /**
     * Gets the key of a pitch for the accidentals of the measure.
     *
     * @param note the note
     * @return step and octave
     */
    private String pitchKey(INote note) {
        if (note.getPitch() != null && note.getPitch().getStep() != null) {
            return note.getPitch().getStep() + note.getPitch().getOctave();
        }
        return "";
    }


    /**
     * Gets the alteration of a note.
     *
     * @param note the note
     * @return the alteration in semitones
     */
    private int alter(INote note) {
        if (note.getPitch() == null || note.getPitch().getAlter() == null) {
            return 0;
        }
        return (int) Math.round(note.getPitch().getAlter());
    }


    /**
     * Checks whether a note starts a tie.
     *
     * @param note the note
     * @return true if the note is tied to the next note
     */
    private boolean tieStart(INote note) {
        if (note.getTies() != null) {
            for (ITie tie : note.getTies()) {
                if (START.equals(tie.getType())) {
                    return true;
                }
            }
        }
        if (note.getNotations() != null && note.getNotations().getTieds() != null) {
            for (ITied tied : note.getNotations().getTieds()) {
                if (START.equals(tied.getType())) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Finds the tuplets of the events: marked by tuplet start and stop, otherwise a run of notes with the same ratio
     * divided into groups of the length of the normal notes. ABC has no nested tuplets: a tuplet inside another one
     * is left out.
     *
     * @param events the events
     * @return the tuplets as {first index, last index, actual notes, normal notes, number of notes}
     */
    private List<int[]> tuplets(List<AbcWriterEvent> events) {
        List<int[]> ranges = new ArrayList<>();
        boolean[] covered = new boolean[events.size()];
        int open = -1;
        for (int i = 0; i < events.size(); i++) {
            INote note = events.get(i).getFirst();
            if (note.getNotations() == null || note.getNotations().getTuplets() == null) {
                continue;
            }
            for (ITuplet tuplet : note.getNotations().getTuplets()) {
                if (START.equals(tuplet.getType()) && open < 0) {
                    open = i;
                } else if (STOP.equals(tuplet.getType()) && open >= 0 && ratio(events.get(open).getFirst()) != null) {
                    ranges.add(new int[] {open, i});
                    for (int c = open; c <= i; c++) {
                        covered[c] = true;
                    }
                    open = -1;
                }
            }
        }
        addUnmarkedTuplets(events, covered, ranges);
        List<int[]> result = new ArrayList<>();
        for (int[] range : ranges) {
            int[] ratio = ratio(events.get(range[0]).getFirst());
            int count = 0;
            for (int i = range[0]; i <= range[1]; i++) {
                if (!events.get(i).isGrace()) {
                    count++;
                }
            }
            result.add(new int[] {range[0], range[1], ratio[0], ratio[1], count});
        }
        return result;
    }


    /**
     * Adds tuplets for the tuplet notes without tuplet marks.
     *
     * @param events the events
     * @param covered the events inside a marked tuplet
     * @param result the tuplet ranges (added to)
     */
    private void addUnmarkedTuplets(List<AbcWriterEvent> events, boolean[] covered, List<int[]> result) {
        int first = -1;
        Fraction sum = Fraction.ZERO;
        Fraction target = null;
        int[] ratio = null;
        for (int i = 0; i <= events.size(); i++) {
            int[] current = null;
            if (i < events.size() && !covered[i] && events.get(i).getLength().isPositive()) {
                current = ratio(events.get(i).getFirst());
            }
            boolean same = current != null && ratio != null && current[0] == ratio[0] && current[1] == ratio[1];
            if (first >= 0 && !same) {
                result.add(new int[] {first, i - 1});
                first = -1;
            }
            if (current == null) {
                continue;
            }
            if (first < 0) {
                first = i;
                sum = Fraction.ZERO;
                ratio = current;
                INote note = events.get(i).getFirst();
                String type = note.getTimeModification().getNormalType();
                if (type == null) {
                    type = note.getType();
                }
                Fraction unit = writer.typeLength(type, 0);
                target = null;
                if (unit != null) {
                    target = unit.multiply(ratio[1], 1);
                }
            }
            sum = sum.add(events.get(i).getLength());
            if (target != null && sum.compareTo(target) >= 0) {
                result.add(new int[] {first, i});
                first = -1;
                ratio = null;
            }
        }
    }


    /**
     * Gets the tuplet ratio of a note.
     *
     * @param note the note
     * @return the actual and normal notes, or null if the note is no tuplet note
     */
    private int[] ratio(INote note) {
        if (note.getTimeModification() == null || note.getTimeModification().getActualNotes() == null
                || note.getTimeModification().getNormalNotes() == null || note.getTimeModification().getActualNotes() <= 0
                || note.getTimeModification().getNormalNotes() <= 0
                || note.getTimeModification().getActualNotes().equals(note.getTimeModification().getNormalNotes())) {
            return null;
        }
        return new int[] {note.getTimeModification().getActualNotes(), note.getTimeModification().getNormalNotes()};
    }


    /**
     * Finds the primary beams of the events (grace notes excluded).
     *
     * @param events the events
     * @return [ at the first and ] at the last note of a beam by event index
     */
    private Map<Integer, String> beams(List<AbcWriterEvent> events) {
        Map<Integer, String> result = new HashMap<>();
        int open = -1;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).isGrace()) {
                continue;
            }
            String value = beam(events.get(i).getFirst());
            if ("begin".equals(value)) {
                open = i;
            } else if ("end".equals(value) && open >= 0) {
                result.put(open, "[");
                result.put(i, "]");
                open = -1;
            }
        }
        return result;
    }


    /**
     * Gets the primary beam value of a note.
     *
     * @param note the note
     * @return begin, continue, end or null
     */
    private String beam(INote note) {
        if (note.getBeams() == null) {
            return null;
        }
        for (IBeam beam : note.getBeams()) {
            if (beam.getNumber() == null || beam.getNumber() == 1) {
                return beam.getValue();
            }
        }
        return null;
    }
}
