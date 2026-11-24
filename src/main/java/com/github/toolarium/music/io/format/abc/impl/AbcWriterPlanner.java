/*
 * AbcWriterPlanner.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterEvent;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterMark;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterMeasure;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterPlan;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterStaff;
import com.github.toolarium.music.model.barline.IBarline;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.IDirection;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.direction.IMetronome;
import com.github.toolarium.music.model.direction.IWords;
import com.github.toolarium.music.model.harmony.IHarmony;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IBackup;
import com.github.toolarium.music.model.measure.IClef;
import com.github.toolarium.music.model.measure.IForward;
import com.github.toolarium.music.model.measure.IKey;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.measure.ITime;
import com.github.toolarium.music.model.measure.ITranspose;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.part.IMidiInstrument;
import com.github.toolarium.music.model.part.IScorePart;
import com.github.toolarium.music.model.score.IScore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Prepares a score for writing as ABC: the staves of the parts, and per measure the events of every staff and
 * voice (with the chords joined), the meter, barlines, repeats and endings, and the marks of every staff at their
 * position (clef and key changes, tempo, rehearsal marks, dynamics, hairpins, texts, pedals and octave lines).
 *
 * @author patrick
 */
public final class AbcWriterPlanner {
    /** The prefix of a clef change mark: the voice writer writes it as inline voice field with the clef. */
    static final String CLEF_MARK = "clef=";

    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String BELOW = "below";
    private static final int MIDI_PROGRAMS = 128;
    private static final Map<String, String> BAR_STYLES = createBarStyles();


    /**
     * The state of the planning of a part: divisions, staves and current key.
     *
     * @author patrick
     */
    private static final class PartState {
        private final int partIndex;
        private final int firstStaff;
        private final int staffCount;
        private final Map<Integer, String> openWedges = new HashMap<>();
        private int divisions = 1;
        private String key;
        private String octave;


        /**
         * Constructor for PartState.
         *
         * @param partIndex the index of the part
         * @param firstStaff the number of the first staff of the part
         * @param staffCount the number of staves of the part
         */
        PartState(int partIndex, int firstStaff, int staffCount) {
            this.partIndex = partIndex;
            this.firstStaff = firstStaff;
            this.staffCount = staffCount;
        }


        /**
         * Gets the staff number of a staff of the part.
         *
         * @param staff the staff in the part (1-based), or null for the first
         * @return the staff number over all parts
         */
        int staff(Integer staff) {
            int value = 1;
            if (staff != null && staff >= 1 && staff <= staffCount) {
                value = staff;
            }
            return firstStaff + value - 1;
        }
    }


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final AbcWriterPlanner INSTANCE = new AbcWriterPlanner();
    }


    /**
     * Constructor.
     */
    private AbcWriterPlanner() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static AbcWriterPlanner getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Prepares a score for writing.
     *
     * @param score the score
     * @return the plan
     */
    public AbcWriterPlan plan(IScore score) {
        AbcWriterPlan plan = new AbcWriterPlan();
        List<? extends IPart> parts = score.getParts();
        int measureCount = 0;
        for (IPart part : parts) {
            measureCount = Math.max(measureCount, part.getMeasures().size());
        }
        for (int m = 0; m < measureCount; m++) {
            plan.getMeasures().add(new AbcWriterMeasure());
        }
        Fraction[] ends = new Fraction[measureCount];
        Arrays.fill(ends, Fraction.ZERO);
        int firstStaff = 1;
        for (int p = 0; p < parts.size(); p++) {
            IPart part = parts.get(p);
            PartState state = new PartState(p, firstStaff, staffCount(part));
            IScorePart scorePart = scorePart(score, part, p);
            for (int s = 1; s <= state.staffCount; s++) {
                AbcWriterStaff staff = new AbcWriterStaff(firstStaff + s - 1, p, s);
                if (scorePart != null) {
                    staff.setLabel(trimToNull(scorePart.getPartName()));
                    staff.setAbbreviation(trimToNull(scorePart.getPartAbbreviation()));
                    staff.setProgram(program(scorePart));
                }
                plan.getStaves().add(staff);
            }
            for (int m = 0; m < part.getMeasures().size(); m++) {
                ends[m] = ends[m].max(planMeasure(plan, state, part.getMeasures().get(m), m));
            }
            firstStaff += state.staffCount;
        }
        for (int m = 0; m < measureCount; m++) {
            AbcWriterMeasure measure = plan.getMeasures().get(m);
            Fraction meter = Fraction.of(measure.getMeterCount(), measure.getMeterUnit());
            if (ends[m].isPositive()) {
                measure.setLength(ends[m]);
            } else {
                measure.setLength(meter);
            }
        }
        moveMarksAtBarline(plan);
        for (AbcWriterStaff staff : plan.getStaves()) {
            for (AbcWriterMeasure measure : plan.getMeasures()) {
                Map<Integer, List<AbcWriterEvent>> voices = measure.getStaves().get(staff.getNumber());
                if (voices != null) {
                    staff.getVoices().addAll(voices.keySet());
                }
            }
            if (staff.getVoices().isEmpty()) {
                staff.getVoices().add(1);
            }
        }
        return plan;
    }


    /**
     * Moves the marks at the end of a measure (for example the end of a hairpin at the barline) to the start of the
     * next measure, so they are written on the first note there.
     *
     * @param plan the plan
     */
    private void moveMarksAtBarline(AbcWriterPlan plan) {
        List<AbcWriterMeasure> measures = plan.getMeasures();
        for (int m = 0; m + 1 < measures.size(); m++) {
            AbcWriterMeasure measure = measures.get(m);
            for (Map.Entry<Integer, List<AbcWriterMark>> entry : measure.getMarks().entrySet()) {
                List<AbcWriterMark> moved = new ArrayList<>();
                for (AbcWriterMark mark : entry.getValue()) {
                    if (mark.getPosition().compareTo(measure.getLength()) >= 0) {
                        moved.add(mark);
                    }
                }
                entry.getValue().removeAll(moved);
                for (AbcWriterMark mark : moved) {
                    Fraction position = mark.getPosition().subtract(measure.getLength());
                    measures.get(m + 1).addMark(entry.getKey(), new AbcWriterMark(position, mark.getText(), mark.isBefore()));
                }
            }
        }
    }


    /**
     * Plans a measure of a part: walks its content in order with the time position.
     *
     * @param plan the plan
     * @param state the part state
     * @param source the measure
     * @param m the measure index
     * @return the end of the content of the measure
     */
    private Fraction planMeasure(AbcWriterPlan plan, PartState state, IMeasure source, int m) {
        AbcWriterMeasure target = plan.getMeasures().get(m);
        Fraction cursor = Fraction.ZERO;
        Fraction end = Fraction.ZERO;
        AbcWriterEvent last = null;
        Map<String, AbcWriterEvent> crossStaff = new HashMap<>();
        for (IMusicDataElement element : source.getMusicData()) {
            if (element instanceof INote) {
                INote note = (INote) element;
                if (note.getChord() != null && last != null) {
                    chordEvent(state, target, crossStaff, last, note).getNotes().add(note);
                    continue;
                }
                last = addEvent(state, target, note, cursor);
                cursor = cursor.add(last.getLength());
                end = end.max(cursor);
            } else if (element instanceof IBackup) {
                cursor = cursor.subtract(length(state, ((IBackup) element).getDuration())).max(Fraction.ZERO);
                last = null;
            } else if (element instanceof IForward) {
                cursor = cursor.add(length(state, ((IForward) element).getDuration()));
                end = end.max(cursor);
                last = null;
            } else if (element instanceof IAttributes) {
                planAttributes(plan, state, (IAttributes) element, m, cursor);
            } else if (element instanceof IDirection) {
                IDirection direction = (IDirection) element;
                Fraction position = cursor;
                if (direction.getOffset() != null) {
                    position = cursor.add(length(state, direction.getOffset())).max(Fraction.ZERO);
                }
                planDirection(target, state, direction, position);
            } else if (element instanceof IHarmony) {
                String chord = AbcNotationWriter.getInstance().chord((IHarmony) element);
                if (chord != null) {
                    target.addMark(state.staff(((IHarmony) element).getStaff()), new AbcWriterMark(cursor, chord, true));
                }
            } else if (element instanceof IBarline) {
                planBarline(target, (IBarline) element, state.partIndex == 0);
            }
        }
        return end;
    }


    /**
     * Gets the event of a chord note: the event of the chord, or for a note on another staff of the part an event at
     * the same time on that staff.
     *
     * @param state the part state
     * @param target the measure
     * @param crossStaff the cross-staff events of the measure by staff and chord
     * @param last the event of the chord
     * @param note the chord note
     * @return the event to add the note to
     */
    private AbcWriterEvent chordEvent(PartState state, AbcWriterMeasure target, Map<String, AbcWriterEvent> crossStaff,
            AbcWriterEvent last, INote note) {
        int staff = state.staff(note.getStaff());
        INote first = last.getFirst();
        if (staff == state.staff(first.getStaff())) {
            return last;
        }
        String key = staff + ":" + System.identityHashCode(last);
        AbcWriterEvent event = crossStaff.get(key);
        if (event == null) {
            event = new AbcWriterEvent(last.getStart(), last.getLength());
            target.voice(staff, voice(first)).add(event);
            crossStaff.put(key, event);
        }
        return event;
    }


    /**
     * Adds an event for a note (or rest) that starts a new event.
     *
     * @param state the part state
     * @param target the measure
     * @param note the note
     * @param cursor the time position
     * @return the event
     */
    private AbcWriterEvent addEvent(PartState state, AbcWriterMeasure target, INote note, Fraction cursor) {
        Fraction length = Fraction.ZERO;
        if (note.getGrace() == null) {
            length = length(state, note.getDuration());
        }
        AbcWriterEvent event = new AbcWriterEvent(cursor, length);
        event.getNotes().add(note);
        target.voice(state.staff(note.getStaff()), voice(note)).add(event);
        return event;
    }


    /**
     * Plans attributes: divisions, the initial staff definitions, key and meter changes and clef changes.
     *
     * @param plan the plan
     * @param state the part state
     * @param attributes the attributes
     * @param m the measure index
     * @param cursor the time position
     */
    private void planAttributes(AbcWriterPlan plan, PartState state, IAttributes attributes, int m, Fraction cursor) {
        AbcWriterMeasure target = plan.getMeasures().get(m);
        if (attributes.getDivisions() != null && attributes.getDivisions() > 0) {
            state.divisions = attributes.getDivisions();
        }
        if (state.partIndex == 0) {
            for (ITime time : nonNull(attributes.getTimes())) {
                int count = beats(time.getBeats());
                int unit = intValue(time.getBeatType(), 0);
                if (count > 0 && unit > 0) {
                    applyMeter(plan, m, count, unit, time.getSymbol());
                }
            }
        }
        for (IKey key : nonNull(attributes.getKeys())) {
            if (key.getFifths() == null) {
                continue;
            }
            String text = "[K:" + AbcNotationWriter.getInstance().key(key.getFifths(), key.getMode()) + "]";
            if (!text.equals(state.key)) {
                for (int s = 0; s < state.staffCount; s++) {
                    target.addMark(state.firstStaff + s, new AbcWriterMark(cursor, text, true));
                }
                state.key = text;
            }
        }
        for (IClef clef : nonNull(attributes.getClefs())) {
            AbcWriterStaff staff = plan.getStaves().get(state.staff(clef.getNumber()) - 1);
            if (m == 0 && cursor.isZero() && staff.getClef() == null) {
                staff.setClef(clef);
            } else {
                target.addMark(staff.getNumber(), new AbcWriterMark(cursor, CLEF_MARK + AbcNotationWriter.getInstance().clef(clef), true));
            }
        }
        if (m == 0) {
            for (ITranspose transpose : nonNull(attributes.getTransposes())) {
                int octaves = intValue(transpose.getOctaveChange(), 0);
                int diatonic = intValue(transpose.getDiatonic(), 0) + octaves * 7;
                int semitones = intValue(transpose.getChromatic(), 0) + octaves * 12;
                if (diatonic == 0 && semitones == 0) {
                    continue;
                }
                for (int s = 0; s < state.staffCount; s++) {
                    plan.getStaves().get(state.firstStaff + s - 1).setTranspose(diatonic, semitones);
                }
            }
        }
    }


    /**
     * Applies a meter from a measure on (the meter of the first part is the meter of the score).
     *
     * @param plan the plan
     * @param m the measure index
     * @param count the beats
     * @param unit the beat unit
     * @param symbol common, cut or null
     */
    private void applyMeter(AbcWriterPlan plan, int m, int count, int unit, String symbol) {
        List<AbcWriterMeasure> measures = plan.getMeasures();
        boolean change = m == 0 || measures.get(m - 1).getMeterCount() != count || measures.get(m - 1).getMeterUnit() != unit
                || !Objects.equals(measures.get(m - 1).getMeterSymbol(), symbol);
        for (int i = m; i < measures.size(); i++) {
            measures.get(i).setMeter(count, unit, symbol, i == m && (change || measures.get(i).isMeterChange()));
        }
    }


    /**
     * Plans a direction as marks of the staff: dynamics, hairpins, words, tempo, rehearsal marks, segno and coda,
     * pedals and octave lines. In ABC every mark is written before the note it belongs to.
     *
     * @param target the measure
     * @param state the part state
     * @param direction the direction
     * @param position the time position
     */
    private void planDirection(AbcWriterMeasure target, PartState state, IDirection direction, Fraction position) {
        int staff = state.staff(direction.getStaff());
        final AbcNotationWriter writer = AbcNotationWriter.getInstance();
        StringBuilder words = new StringBuilder();
        IMetronome metronome = null;
        for (IDirectionType type : nonNull(direction.getDirectionTypes())) {
            for (IWords word : nonNull(type.getWords())) {
                if (word.getValue() != null && !word.getValue().trim().isEmpty()) {
                    if (words.length() > 0) {
                        words.append(' ');
                    }
                    words.append(word.getValue().trim());
                }
            }
            if (type.getMetronome() != null) {
                metronome = type.getMetronome();
            }
            planDirectionType(target, state, staff, type, position);
        }
        String tempo = null;
        if (direction.getSound() != null) {
            tempo = trimToNull(direction.getSound().getTempo());
        }
        String text = null;
        if (words.length() > 0) {
            text = words.toString();
        }
        if (metronome != null || tempo != null) {
            String value = writer.tempo(text, metronome, tempo);
            if (value != null) {
                target.addMark(staff, new AbcWriterMark(position, "[Q:" + value + "]", true));
                return;
            }
        }
        if (text != null) {
            target.addMark(staff, new AbcWriterMark(position, writer.annotation(text, BELOW.equals(direction.getPlacement())), true));
        }
    }


    /**
     * Plans one direction type except words and metronome.
     *
     * @param target the measure
     * @param state the part state (for the open hairpins and octave lines)
     * @param staff the staff number
     * @param type the direction type
     * @param position the time position
     */
    private void planDirectionType(AbcWriterMeasure target, PartState state, int staff, IDirectionType type, Fraction position) {
        AbcNotationWriter writer = AbcNotationWriter.getInstance();
        String dynamics = writer.dynamics(type.getDynamics());
        if (dynamics != null) {
            target.addMark(staff, new AbcWriterMark(position, dynamics, true));
        }
        if (type.getRehearsal() != null && !type.getRehearsal().trim().isEmpty()) {
            target.addMark(staff, new AbcWriterMark(position, "[P:" + writer.clean(type.getRehearsal()).replace("]", "") + "]", true));
        }
        if (type.getSegno() != null) {
            target.addMark(staff, new AbcWriterMark(position, writer.decoration("segno"), true));
        }
        if (type.getCoda() != null) {
            target.addMark(staff, new AbcWriterMark(position, writer.decoration("coda"), true));
        }
        if (type.getWedge() != null) {
            String kind = null;
            if ("crescendo".equals(type.getWedge().getType())) {
                kind = "<";
            } else if ("diminuendo".equals(type.getWedge().getType())) {
                kind = ">";
            }
            int number = 1;
            if (type.getWedge().getNumber() != null) {
                number = type.getWedge().getNumber();
            }
            if (kind != null) {
                state.openWedges.put(number, kind);
                target.addMark(staff, new AbcWriterMark(position, writer.decoration(kind + "("), true));
            } else if (STOP.equals(type.getWedge().getType()) && state.openWedges.containsKey(number)) {
                target.addMark(staff, new AbcWriterMark(position, writer.decoration(state.openWedges.remove(number) + ")"), true));
            }
        }
        if (type.getPedal() != null) {
            if (START.equals(type.getPedal().getType())) {
                target.addMark(staff, new AbcWriterMark(position, writer.decoration("ped"), true));
            } else if (STOP.equals(type.getPedal().getType())) {
                target.addMark(staff, new AbcWriterMark(position, writer.decoration("ped-up"), true));
            }
        }
        if (type.getOctaveShift() != null) {
            if ("down".equals(type.getOctaveShift().getType())) {
                state.octave = "8va";
                target.addMark(staff, new AbcWriterMark(position, writer.decoration("8va("), true));
            } else if ("up".equals(type.getOctaveShift().getType())) {
                state.octave = "8vb";
                target.addMark(staff, new AbcWriterMark(position, writer.decoration("8vb("), true));
            } else if (STOP.equals(type.getOctaveShift().getType()) && state.octave != null) {
                target.addMark(staff, new AbcWriterMark(position, writer.decoration(state.octave + ")"), true));
                state.octave = null;
            }
        }
    }


    /**
     * Plans a barline: its style, repeat and ending.
     *
     * @param target the measure
     * @param barline the barline
     * @param firstPart true for the first part (the barlines of the score)
     */
    private void planBarline(AbcWriterMeasure target, IBarline barline, boolean firstPart) {
        if (!firstPart) {
            return;
        }
        if (barline.getRepeat() != null) {
            if ("forward".equals(barline.getRepeat().getDirection())) {
                target.setRepeatStart(true);
            } else {
                int times = 2;
                if (barline.getRepeat().getTimes() != null && barline.getRepeat().getTimes() > 1) {
                    times = barline.getRepeat().getTimes();
                }
                target.setRepeatEnd(true, times);
            }
        } else if (!"left".equals(barline.getLocation()) && barline.getBarStyle() != null && BAR_STYLES.containsKey(barline.getBarStyle())) {
            target.setBarStyle(BAR_STYLES.get(barline.getBarStyle()));
        }
        if (barline.getEnding() != null && barline.getEnding().getNumber() != null) {
            if (START.equals(barline.getEnding().getType())) {
                target.setEndingStart(barline.getEnding().getNumber());
            } else {
                target.setEndingStop(true);
            }
        }
    }


    /**
     * Gets the voice of a note.
     *
     * @param note the note
     * @return the voice (1 if unknown)
     */
    private int voice(INote note) {
        if (note.getVoice() != null && note.getVoice() > 0) {
            return note.getVoice();
        }
        return 1;
    }


    /**
     * Gets the length of a duration in divisions.
     *
     * @param state the part state
     * @param duration the duration in divisions, or null
     * @return the length in whole notes
     */
    private Fraction length(PartState state, Integer duration) {
        if (duration == null || duration <= 0) {
            return Fraction.ZERO;
        }
        return Fraction.of(duration, 4L * state.divisions);
    }


    /**
     * Gets the number of beats of a time signature (2+3 is 5).
     *
     * @param beats the beats text
     * @return the beats, or 0 if invalid
     */
    private int beats(String beats) {
        if (beats == null) {
            return 0;
        }
        int result = 0;
        for (String part : beats.split("\\+")) {
            int value = intValue(part.trim(), -1);
            if (value <= 0) {
                return 0;
            }
            result += value;
        }
        return result;
    }


    /**
     * Gets the number of staves of a part.
     *
     * @param part the part
     * @return the staves (at least 1)
     */
    private int staffCount(IPart part) {
        int result = 1;
        for (IMeasure measure : part.getMeasures()) {
            for (IAttributes attributes : nonNull(measure.getAttributes())) {
                if (attributes.getStaves() != null) {
                    result = Math.max(result, attributes.getStaves());
                }
            }
        }
        return result;
    }


    /**
     * Gets the part list entry of a part.
     *
     * @param score the score
     * @param part the part
     * @param index the index of the part
     * @return the part list entry, or null
     */
    private IScorePart scorePart(IScore score, IPart part, int index) {
        if (score.getPartList() == null || score.getPartList().getScoreParts() == null) {
            return null;
        }
        List<? extends IScorePart> scoreParts = score.getPartList().getScoreParts();
        for (IScorePart scorePart : scoreParts) {
            if (scorePart.getId() != null && scorePart.getId().equals(part.getId())) {
                return scorePart;
            }
        }
        if (index < scoreParts.size()) {
            return scoreParts.get(index);
        }
        return null;
    }


    /**
     * Gets the General MIDI program of a part.
     *
     * @param scorePart the part list entry
     * @return the program (1 to 128), or null
     */
    private Integer program(IScorePart scorePart) {
        for (IMidiInstrument instrument : nonNull(scorePart.getMidiInstruments())) {
            if (instrument.getMidiProgram() != null && instrument.getMidiProgram() >= 1 && instrument.getMidiProgram() <= MIDI_PROGRAMS) {
                return instrument.getMidiProgram();
            }
        }
        return null;
    }


    /**
     * Parses an integer.
     *
     * @param value the text, or null
     * @param defaultValue the value if the text is no integer
     * @return the integer
     */
    private int intValue(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * Gets an integer or a default value.
     *
     * @param value the value, or null
     * @param defaultValue the default value
     * @return the integer
     */
    private int intValue(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }


    /**
     * Trims a text.
     *
     * @param value the text, or null
     * @return the trimmed text, or null if empty
     */
    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }


    /**
     * Gets a list or an empty list.
     *
     * @param <T> the element type
     * @param list the list, or null
     * @return the list
     */
    private <T> List<? extends T> nonNull(List<? extends T> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list;
    }


    /**
     * Creates the ABC bar lines of the MusicXML bar styles.
     *
     * @return the bar line by bar style
     */
    private static Map<String, String> createBarStyles() {
        Map<String, String> result = new HashMap<>();
        result.put("light-heavy", "|]");
        result.put("light-light", "||");
        result.put("heavy-heavy", "||");
        result.put("heavy-light", "[|");
        result.put("dashed", ".|");
        result.put("dotted", ".|");
        return result;
    }
}
