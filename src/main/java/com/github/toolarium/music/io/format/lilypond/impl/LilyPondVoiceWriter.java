/*
 * LilyPondVoiceWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl;

import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondWriterEvent;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondWriterMark;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondWriterMeasure;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondWriterPlan;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondWriterStaff;
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
import java.util.Objects;
import java.util.TreeMap;


/**
 * Writes the music of one voice of a staff, measure by measure: notes, chords, rests, grace notes and tuplets with
 * ties, slurs, beams and articulations. The first voice of a staff also carries the clef, key, time signature,
 * barlines and the marks of the staff (placed at their position, a gap is filled with spacer rests). The lyrics of
 * the voice are collected with the LilyPond rules: a note gets no syllable if it continues a tie or lies inside a
 * slur.
 *
 * @author patrick
 */
public class LilyPondVoiceWriter {
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String OMIT_TIME = "\\once \\omit Staff.TimeSignature ";
    private static final String SKIP = "_";
    private final LilyPondWriterPlan plan;
    private final LilyPondWriterStaff staff;
    private final int voice;
    private final boolean primary;
    private final LilyPondNotationWriter writer = LilyPondNotationWriter.getInstance();
    private final Map<Integer, String> openSlurs = new HashMap<>();
    private final Map<String, List<String>> verses = new TreeMap<>();
    private int singable;
    private int slurDepth;
    private boolean previousTie;
    private int measureIndex;
    private LilyPondWriterEvent nextEvent;


    /**
     * Constructor for LilyPondVoiceWriter.
     *
     * @param plan the plan
     * @param staff the staff
     * @param voice the voice number
     * @param primary true for the first voice of the staff (with the staff marks)
     */
    public LilyPondVoiceWriter(LilyPondWriterPlan plan, LilyPondWriterStaff staff, int voice, boolean primary) {
        this.plan = plan;
        this.staff = staff;
        this.voice = voice;
        this.primary = primary;
    }


    /**
     * Gets the lyrics of the voice collected while writing.
     *
     * @return the syllables (with -- and __) by verse
     */
    public Map<String, List<String>> getVerses() {
        return verses;
    }


    /**
     * Writes the voice.
     *
     * @param structure the repeat structure (tokens before and after every measure)
     * @return the lines, one per measure
     */
    public List<String> write(String[][] structure) {
        List<String> lines = new ArrayList<>();
        boolean previousIrregular = false;
        List<LilyPondWriterMeasure> measures = plan.getMeasures();
        for (int m = 0; m < measures.size(); m++) {
            LilyPondWriterMeasure measure = measures.get(m);
            measureIndex = m;
            StringBuilder line = new StringBuilder(structure[0][m]);
            if (primary) {
                previousIrregular = writeTiming(line, measure, m, previousIrregular);
            }
            line.append(measureContent(measure));
            if (primary && measure.getBarStyle() != null && structure[1][m].isEmpty()) {
                line.append(" \\bar ").append(writer.string(measure.getBarStyle()));
            }
            line.append(" |").append(structure[1][m]);
            lines.add(line.toString().trim());
        }
        return lines;
    }


    /**
     * Writes the staff settings at the start and the time signature of a measure.
     *
     * @param line the line of the measure
     * @param measure the measure
     * @param m the measure index
     * @param previousIrregular true if the previous measure had a hidden time signature of its own length
     * @return true if this measure has a hidden time signature of its own length
     */
    private boolean writeTiming(StringBuilder line, LilyPondWriterMeasure measure, int m, boolean previousIrregular) {
        Fraction meter = Fraction.of(measure.getMeterCount(), measure.getMeterUnit());
        String time = "\\time " + measure.getMeterCount() + "/" + measure.getMeterUnit() + " ";
        boolean irregular = !measure.getLength().equals(meter);
        if (m == 0) {
            if (staff.getTransposeSemitones() != null) {
                line.append(writer.transposition(staff.getTransposeDiatonic(), staff.getTransposeSemitones())).append(' ');
            }
            line.append("\\clef ").append(writer.clef(staff.getClef())).append(' ');
            boolean commonOrCut = measure.getMeterCount() == measure.getMeterUnit() && (measure.getMeterUnit() == 4 || measure.getMeterUnit() == 2);
            if (measure.getMeterSymbol() == null && commonOrCut) {
                line.append("\\numericTimeSignature ");
            }
            line.append(time);
            if (irregular && measure.getLength().compareTo(meter) < 0) {
                line.append("\\partial ").append(writer.duration(measure.getLength())).append(' ');
                return false;
            }
        }
        if (irregular) {
            line.append(OMIT_TIME).append("\\time ").append(timeOf(measure.getLength(), measure.getMeterUnit())).append(' ');
            return true;
        }
        if (m > 0 && measure.isMeterChange()) {
            line.append(time);
        } else if (previousIrregular) {
            line.append(OMIT_TIME).append(time);
        }
        return false;
    }


    /**
     * Gets the time signature of a measure length (for example 2/4 or 5/8).
     *
     * @param length the length in whole notes
     * @param unit the beat unit of the meter
     * @return the time signature
     */
    private String timeOf(Fraction length, int unit) {
        long numerator = length.getNumerator() * unit;
        if (numerator % length.getDenominator() == 0) {
            return numerator / length.getDenominator() + "/" + unit;
        }
        return length.getNumerator() + "/" + length.getDenominator();
    }


    /**
     * Writes the content of a measure: the events with the marks of the staff, the gaps filled with spacer rests.
     *
     * @param measure the measure
     * @return the content
     */
    private String measureContent(LilyPondWriterMeasure measure) {
        List<LilyPondWriterEvent> events = new ArrayList<>();
        Map<Integer, List<LilyPondWriterEvent>> voices = measure.getStaves().get(staff.getNumber());
        if (voices != null && voices.get(voice) != null) {
            events = voices.get(voice);
        }
        List<LilyPondWriterMark> marks = new ArrayList<>();
        if (primary && measure.getMarks().get(staff.getNumber()) != null) {
            marks.addAll(measure.getMarks().get(staff.getNumber()));
            marks.sort(Comparator.comparing(LilyPondWriterMark::getPosition));
        }
        MarkCursor cursorMarks = new MarkCursor(marks);
        List<String> tokens = new ArrayList<>();
        List<int[]> tuplets = tuplets(events);
        Map<Integer, String> beams = beams(events);
        List<String> graces = new ArrayList<>();
        String graceCommand = "\\grace";
        Fraction cursor = Fraction.ZERO;
        for (int i = 0; i < events.size(); i++) {
            LilyPondWriterEvent event = events.get(i);
            if (event.isGrace()) {
                if (graces.isEmpty() && "yes".equals(event.getFirst().getGrace().getSlash())) {
                    graceCommand = "\\acciaccatura";
                } else if (graces.isEmpty()) {
                    graceCommand = "\\grace";
                }
                nextEvent = null;
                graces.add(eventText(event, measure, false, null));
                continue;
            }
            if (event.getStart().compareTo(cursor) > 0) {
                gap(tokens, cursorMarks, cursor, event.getStart());
            }
            cursorMarks.before(tokens, event.getStart());
            flushGraces(tokens, graces, graceCommand);
            for (int[] tuplet : tuplets) {
                if (tuplet[0] == i) {
                    tokens.add("\\tuplet " + tuplet[2] + "/" + tuplet[3] + " {");
                }
            }
            Fraction end = event.getStart().add(event.getLength());
            nextEvent = nextEvent(events, i);
            String text = eventText(event, measure, true, beams.get(i)) + cursorMarks.after(end);
            tokens.add(text);
            for (int[] tuplet : tuplets) {
                if (tuplet[1] == i) {
                    tokens.add("}");
                }
            }
            cursor = cursor.max(end);
        }
        if (cursor.compareTo(measure.getLength()) < 0) {
            gap(tokens, cursorMarks, cursor, measure.getLength());
        }
        cursorMarks.before(tokens, null);
        flushGraces(tokens, graces, graceCommand);
        String remaining = cursorMarks.after(null);
        if (!remaining.isEmpty()) {
            if (tokens.isEmpty()) {
                tokens.add("s" + writer.duration(measure.getLength()));
            }
            tokens.set(tokens.size() - 1, tokens.get(tokens.size() - 1) + remaining);
        }
        return String.join(" ", tokens);
    }


    /**
     * Fills a gap with spacer rests: the gap is divided at the positions of marks, so a mark is placed exactly.
     *
     * @param tokens the tokens (added to)
     * @param marks the marks
     * @param from the start of the gap
     * @param to the end of the gap
     */
    private void gap(List<String> tokens, MarkCursor marks, Fraction from, Fraction to) {
        List<Fraction> points = marks.positionsBetween(from, to);
        points.add(to);
        Fraction position = from;
        for (Fraction point : points) {
            if (point.compareTo(position) <= 0) {
                continue;
            }
            marks.before(tokens, position);
            List<String> spacers = new ArrayList<>();
            LilyPondScoreWriter.spacers(spacers, "s", point.subtract(position));
            spacers.set(0, spacers.get(0) + marks.after(point));
            tokens.addAll(spacers);
            position = point;
        }
    }


    /**
     * Writes the collected grace notes before the next note.
     *
     * @param tokens the tokens (added to)
     * @param graces the grace notes (cleared)
     * @param command \grace or \acciaccatura
     */
    private void flushGraces(List<String> tokens, List<String> graces, String command) {
        if (graces.isEmpty()) {
            return;
        }
        tokens.add(command + " { " + String.join(" ", graces) + " }");
        graces.clear();
    }


    /**
     * Writes an event: a rest, a note or a chord with its duration and post-events; a main note also adds its lyrics.
     *
     * @param event the event
     * @param measure the measure
     * @param main true for a main note (false for a grace note)
     * @param beam the beam mark ([ or ]), or null
     * @return the text
     */
    private String eventText(LilyPondWriterEvent event, LilyPondWriterMeasure measure, boolean main, String beam) {
        INote first = event.getFirst();
        StringBuilder text = new StringBuilder();
        if (event.isRest()) {
            boolean wholeMeasure = event.getStart().isZero() && event.getLength().equals(measure.getLength())
                    && ("yes".equals(first.getRest().getMeasure()) || "whole".equals(first.getType()));
            if ("no".equals(first.getPrintObject())) {
                if (wholeMeasure) {
                    text.append('s').append(writer.duration(measure.getLength()));
                } else {
                    text.append('s').append(duration(event));
                }
            } else if (wholeMeasure) {
                text.append('R').append(writer.duration(measure.getLength()));
            } else {
                text.append('r').append(duration(event));
            }
            if (main) {
                previousTie = false;
            }
        } else if (event.getNotes().size() == 1) {
            text.append(pitch(first)).append(duration(event));
        } else {
            text.append('<');
            boolean allTied = allTied(event);
            for (int n = 0; n < event.getNotes().size(); n++) {
                if (n > 0) {
                    text.append(' ');
                }
                text.append(pitch(event.getNotes().get(n)));
                if (!allTied && tied(event.getNotes().get(n))) {
                    // only some notes of the chord are tied
                    text.append('~');
                }
            }
            text.append('>').append(duration(event));
        }
        boolean tie = false;
        for (INote note : event.getNotes()) {
            tie = tie || tied(note);
        }
        if (tie && !event.isRest() && allTied(event)) {
            text.append('~');
        }
        if (beam != null) {
            text.append(beam);
        }
        int[] slurs = slurs(text, event);
        for (String mark : writer.marks(first.getNotations())) {
            text.append(mark);
        }
        if (main && !event.isRest()) {
            lyrics(first, slurs);
            previousTie = tie;
        }
        return text.toString();
    }


    /**
     * Adds the slur ends and starts of an event: a slur inside another slur is a phrasing slur.
     *
     * @param text the text of the event (appended to)
     * @param event the event
     * @return the number of slur starts and stops
     */
    private int[] slurs(StringBuilder text, LilyPondWriterEvent event) {
        int starts = 0;
        int stops = 0;
        List<ISlur> slurs = new ArrayList<>();
        for (INote note : event.getNotes()) {
            if (note.getNotations() != null && note.getNotations().getSlurs() != null) {
                slurs.addAll(note.getNotations().getSlurs());
            }
        }
        for (ISlur slur : slurs) {
            int number = slurNumber(slur);
            if (STOP.equals(slur.getType()) && openSlurs.containsKey(number)) {
                text.append(openSlurs.remove(number));
                stops++;
            }
        }
        for (ISlur slur : slurs) {
            int number = slurNumber(slur);
            if (START.equals(slur.getType()) && !openSlurs.containsKey(number) && openSlurs.size() < 2) {
                if (openSlurs.containsValue(")")) {
                    text.append("\\(");
                    openSlurs.put(number, "\\)");
                } else {
                    text.append('(');
                    openSlurs.put(number, ")");
                }
                starts++;
            }
        }
        return new int[] {starts, stops};
    }


    /**
     * Gets the number of a slur.
     *
     * @param slur the slur
     * @return the number (1 if unknown)
     */
    private int slurNumber(ISlur slur) {
        if (slur.getNumber() == null) {
            return 1;
        }
        return slur.getNumber();
    }


    /**
     * Collects the lyrics of a note: a note that continues a tie or lies inside a slur gets no syllable (as in
     * LilyPond); every other note gets its syllable or a skip in every verse.
     *
     * @param note the first note of the event
     * @param slurs the slur starts and stops of the event
     */
    private void lyrics(INote note, int[] slurs) {
        boolean melisma = previousTie || slurDepth > 0;
        slurDepth = Math.max(0, slurDepth + slurs[0] - slurs[1]);
        if (melisma) {
            return;
        }
        Map<String, String> syllables = new HashMap<>();
        if (note.getLyrics() != null) {
            int index = 1;
            for (ILyric lyric : note.getLyrics()) {
                if (lyric.getText() != null) {
                    String verse = String.valueOf(index);
                    if (lyric.getNumber() != null && !lyric.getNumber().trim().isEmpty()) {
                        verse = lyric.getNumber().trim();
                    }
                    syllables.put(verse, syllable(lyric));
                }
                index++;
            }
        }
        for (String verse : syllables.keySet()) {
            if (!verses.containsKey(verse)) {
                List<String> list = new ArrayList<>();
                for (int i = 0; i < singable; i++) {
                    list.add(SKIP);
                }
                verses.put(verse, list);
            }
        }
        for (Map.Entry<String, List<String>> verse : verses.entrySet()) {
            verse.getValue().add(syllables.getOrDefault(verse.getKey(), SKIP));
        }
        singable++;
    }


    /**
     * Gets the syllable of a lyric with its hyphen (--) or extender (__).
     *
     * @param lyric the lyric
     * @return the syllable
     */
    private String syllable(ILyric lyric) {
        StringBuilder result = new StringBuilder(writer.string(lyric.getText()));
        if ("begin".equals(lyric.getSyllabic()) || "middle".equals(lyric.getSyllabic())) {
            result.append(" --");
        }
        if (lyric.getExtend() != null) {
            result.append(" __");
        }
        return result.toString();
    }


    /**
     * Gets the pitch of a note.
     *
     * @param note the note
     * @return the pitch (for example fis')
     */
    private String pitch(INote note) {
        if (note.getPitch() != null && note.getPitch().getStep() != null && note.getPitch().getOctave() != null) {
            return writer.pitch(note.getPitch().getStep(), note.getPitch().getAlter(), note.getPitch().getOctave());
        }
        if (note.getUnpitched() != null && note.getUnpitched().getDisplayStep() != null && note.getUnpitched().getDisplayOctave() != null) {
            return writer.pitch(note.getUnpitched().getDisplayStep(), null, note.getUnpitched().getDisplayOctave());
        }
        return "c'";
    }


    /**
     * Gets the duration of an event: the note value and dots, or a duration of the nominal length if they do not
     * match.
     *
     * @param event the event
     * @return the duration
     */
    private String duration(LilyPondWriterEvent event) {
        INote note = event.getFirst();
        int dots = 0;
        if (note.getDots() != null) {
            dots = note.getDots().size();
        }
        Fraction nominal = event.getLength();
        if (note.getTimeModification() != null && note.getTimeModification().getActualNotes() != null
                && note.getTimeModification().getNormalNotes() != null && note.getTimeModification().getNormalNotes() > 0) {
            nominal = nominal.multiply(note.getTimeModification().getActualNotes(), note.getTimeModification().getNormalNotes());
        }
        Fraction typed = NoteValueConverter.getInstance().toFraction(note.getType(), dots);
        String text = writer.duration(note.getType(), dots);
        if (text != null && (event.isGrace() || typed != null && typed.equals(nominal))) {
            return text;
        }
        if (nominal.isPositive()) {
            return writer.duration(nominal);
        }
        if (text != null) {
            return text;
        }
        return "4";
    }


    /**
     * Checks whether all notes of an event start a tie.
     *
     * @param event the event
     * @return true if every note is tied to the next event
     */
    private boolean allTied(LilyPondWriterEvent event) {
        for (INote note : event.getNotes()) {
            if (!tied(note)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Checks whether a note is tied to the next note of the voice: a tie to another pitch (or to nothing) is left
     * out, LilyPond only ties notes of the same pitch.
     *
     * @param note the note
     * @return true if the note is tied
     */
    private boolean tied(INote note) {
        if (!tieStart(note)) {
            return false;
        }
        if (nextEvent == null || nextEvent.isRest() || note.getPitch() == null) {
            return nextEvent != null && !nextEvent.isRest();
        }
        for (INote next : nextEvent.getNotes()) {
            if (next.getPitch() != null && note.getPitch().getStep().equals(next.getPitch().getStep())
                    && Objects.equals(note.getPitch().getOctave(), next.getPitch().getOctave())
                    && alter(note) == alter(next)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Gets the alteration of a note.
     *
     * @param note the note
     * @return the alteration in semitones
     */
    private double alter(INote note) {
        if (note.getPitch().getAlter() == null) {
            return 0;
        }
        return note.getPitch().getAlter();
    }


    /**
     * Gets the next main event of the voice after an event: in the same measure or the first of the next measure.
     *
     * @param events the events of the measure
     * @param index the index of the event
     * @return the next event, or null
     */
    private LilyPondWriterEvent nextEvent(List<LilyPondWriterEvent> events, int index) {
        for (int i = index + 1; i < events.size(); i++) {
            if (!events.get(i).isGrace()) {
                return events.get(i);
            }
        }
        if (measureIndex + 1 >= plan.getMeasures().size()) {
            return null;
        }
        Map<Integer, List<LilyPondWriterEvent>> voices = plan.getMeasures().get(measureIndex + 1).getStaves().get(staff.getNumber());
        if (voices == null || voices.get(voice) == null) {
            return null;
        }
        for (LilyPondWriterEvent event : voices.get(voice)) {
            if (!event.isGrace()) {
                return event;
            }
        }
        return null;
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
     * divided into groups of the length of the normal notes.
     *
     * @param events the events
     * @return the tuplets as {first index, last index, actual notes, normal notes}
     */
    private List<int[]> tuplets(List<LilyPondWriterEvent> events) {
        List<int[]> result = new ArrayList<>();
        boolean[] covered = new boolean[events.size()];
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            INote note = events.get(i).getFirst();
            if (note.getNotations() == null || note.getNotations().getTuplets() == null) {
                continue;
            }
            for (ITuplet tuplet : note.getNotations().getTuplets()) {
                if (START.equals(tuplet.getType())) {
                    open.add(i);
                }
            }
            for (ITuplet tuplet : note.getNotations().getTuplets()) {
                if (STOP.equals(tuplet.getType()) && !open.isEmpty()) {
                    int first = open.remove(open.size() - 1);
                    int[] ratio = ratio(events.get(first).getFirst());
                    if (ratio != null) {
                        result.add(new int[] {first, i, ratio[0], ratio[1]});
                        for (int c = first; c <= i; c++) {
                            covered[c] = true;
                        }
                    }
                }
            }
        }
        addUnmarkedTuplets(events, covered, result);
        result.sort(Comparator.<int[]>comparingInt(range -> range[0]).thenComparing(range -> -range[1]));
        return result;
    }


    /**
     * Adds tuplets for the tuplet notes without tuplet marks.
     *
     * @param events the events
     * @param covered the events inside a marked tuplet
     * @param result the tuplets (added to)
     */
    private void addUnmarkedTuplets(List<LilyPondWriterEvent> events, boolean[] covered, List<int[]> result) {
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
                result.add(new int[] {first, i - 1, ratio[0], ratio[1]});
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
                Fraction unit = NoteValueConverter.getInstance().toFraction(type);
                target = null;
                if (unit != null) {
                    target = unit.multiply(ratio[1], 1);
                }
            }
            sum = sum.add(events.get(i).getLength());
            if (target != null && sum.compareTo(target) >= 0) {
                result.add(new int[] {first, i, ratio[0], ratio[1]});
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
     * Finds the primary beams of the events (grace notes excluded): [ on the first and ] on the last note.
     *
     * @param events the events
     * @return the beam mark by event index
     */
    private Map<Integer, String> beams(List<LilyPondWriterEvent> events) {
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


    /**
     * The marks of a staff in a measure that are not written yet.
     *
     * @author patrick
     */
    private static final class MarkCursor {
        private final List<LilyPondWriterMark> marks;
        private final boolean[] used;


        /**
         * Constructor for MarkCursor.
         *
         * @param marks the marks sorted by position
         */
        MarkCursor(List<LilyPondWriterMark> marks) {
            this.marks = marks;
            this.used = new boolean[marks.size()];
        }


        /**
         * Adds the commands at or before a position.
         *
         * @param tokens the tokens (added to)
         * @param position the position, or null for all remaining commands
         */
        void before(List<String> tokens, Fraction position) {
            for (int i = 0; i < marks.size(); i++) {
                LilyPondWriterMark mark = marks.get(i);
                if (!used[i] && mark.isBefore() && (position == null || mark.getPosition().compareTo(position) <= 0)) {
                    tokens.add(mark.getText());
                    used[i] = true;
                }
            }
        }


        /**
         * Gets the post-events before a position that are not written yet (a mark within a note is written on it).
         *
         * @param to the end (exclusive), or null for all remaining post-events
         * @return the post-events
         */
        String after(Fraction to) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < marks.size(); i++) {
                LilyPondWriterMark mark = marks.get(i);
                boolean inside = to == null || mark.getPosition().compareTo(to) < 0;
                if (!used[i] && !mark.isBefore() && inside) {
                    result.append(mark.getText());
                    used[i] = true;
                }
            }
            return result.toString();
        }


        /**
         * Gets the positions of the marks not written yet within a range.
         *
         * @param from the start (exclusive)
         * @param to the end (exclusive)
         * @return the positions in order
         */
        List<Fraction> positionsBetween(Fraction from, Fraction to) {
            List<Fraction> result = new ArrayList<>();
            for (int i = 0; i < marks.size(); i++) {
                Fraction position = marks.get(i).getPosition();
                if (!used[i] && position.compareTo(from) > 0 && position.compareTo(to) < 0 && !result.contains(position)) {
                    result.add(position);
                }
            }
            return result;
        }
    }
}
