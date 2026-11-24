/*
 * MeiMeasurePlanner.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl;

import com.github.toolarium.music.io.converter.ChordSymbolFormatter;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterControl;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterEvent;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterMeasure;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterPlan;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterStaff;
import com.github.toolarium.music.model.barline.IBarline;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.IDirection;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.direction.IWords;
import com.github.toolarium.music.model.harmony.IHarmony;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IBackup;
import com.github.toolarium.music.model.measure.IClef;
import com.github.toolarium.music.model.measure.IForward;
import com.github.toolarium.music.model.measure.IKey;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.measure.IStaffDetails;
import com.github.toolarium.music.model.measure.ITime;
import com.github.toolarium.music.model.measure.ITranspose;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.part.IMidiInstrument;
import com.github.toolarium.music.model.part.IScorePart;
import com.github.toolarium.music.model.score.IScore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Prepares a score for writing as MEI: the staff definitions of the parts, and per measure the events of every
 * staff and voice (with the chords joined), the key, meter and clef changes, the barlines and endings, and the
 * control events. Directions are placed by time stamp (beat), slurs reference their start and end note by ID.
 *
 * @author patrick
 */
public final class MeiMeasurePlanner {
    /** The attribute with the MEI staff number. */
    static final String STAFF = "staff";

    /** The attribute with the time stamp (beat) of a control event. */
    static final String TSTAMP = "tstamp";

    /** The attribute with the end time stamp of a spanner. */
    static final String TSTAMP2 = "tstamp2";

    /** The attribute with the start note of a control event. */
    static final String STARTID = "startid";

    /** The attribute with the placement of a control event. */
    static final String PLACE = "place";

    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String ABOVE = "above";
    private static final String BELOW = "below";
    private static final String FORM = "form";
    private static final String MEASURES_PLUS = "m+";
    private static final int DEFAULT_COUNT = 4;
    private static final int DEFAULT_UNIT = 4;
    private static final int MIDI_PROGRAMS = 128;
    private static final int BEAT_SCALE = 4;


    /**
     * The state of the planning of a score: the plan and the next ID.
     *
     * @author patrick
     */
    private static final class Context {
        private final MeiWriterPlan plan = new MeiWriterPlan();
        private int nextId = 1;


        /**
         * Creates the next ID.
         *
         * @return the ID
         */
        String id() {
            String id = "e" + nextId;
            nextId++;
            return id;
        }
    }


    /**
     * The state of the planning of a part: divisions, staves, current key and the open spanners.
     *
     * @author patrick
     */
    private static final class PartState {
        private final int partIndex;
        private final int firstStaff;
        private final int staffCount;
        private final Map<Integer, Object[]> openSlurs = new HashMap<>();
        private final Map<Integer, Object[]> openWedges = new HashMap<>();
        private final Map<Integer, Object[]> openOctaves = new HashMap<>();
        private final List<Object[]> clefChanges = new ArrayList<>();
        private int divisions = 1;
        private String keySig;
        private String keyMode;


        /**
         * Constructor for PartState.
         *
         * @param partIndex the index of the part
         * @param firstStaff the MEI number of the first staff of the part
         * @param staffCount the number of staves of the part
         */
        PartState(int partIndex, int firstStaff, int staffCount) {
            this.partIndex = partIndex;
            this.firstStaff = firstStaff;
            this.staffCount = staffCount;
        }


        /**
         * Gets the MEI staff number of a staff of the part.
         *
         * @param staff the staff in the part (1-based), or null for the first
         * @return the MEI staff number
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
        static final MeiMeasurePlanner INSTANCE = new MeiMeasurePlanner();
    }


    /**
     * Constructor.
     */
    private MeiMeasurePlanner() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MeiMeasurePlanner getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Prepares a score for writing.
     *
     * @param score the score
     * @return the plan
     */
    public MeiWriterPlan plan(IScore score) {
        Context context = new Context();
        List<? extends IPart> parts = score.getParts();
        int measureCount = 0;
        for (IPart part : parts) {
            measureCount = Math.max(measureCount, part.getMeasures().size());
        }
        for (int m = 0; m < measureCount; m++) {
            MeiWriterMeasure measure = new MeiWriterMeasure(measureNumber(parts, m));
            measure.setMeter(DEFAULT_COUNT, DEFAULT_UNIT, false);
            context.plan.getMeasures().add(measure);
        }
        Fraction[] ends = new Fraction[measureCount];
        Arrays.fill(ends, Fraction.ZERO);
        int firstStaff = 1;
        for (int p = 0; p < parts.size(); p++) {
            IPart part = parts.get(p);
            PartState state = new PartState(p, firstStaff, staffCount(part));
            IScorePart scorePart = scorePart(score, part, p);
            for (int s = 1; s <= state.staffCount; s++) {
                MeiWriterStaff staff = new MeiWriterStaff(firstStaff + s - 1, p, s);
                if (scorePart != null) {
                    staff.setLabel(trimToNull(scorePart.getPartName()));
                    staff.setAbbreviation(trimToNull(scorePart.getPartAbbreviation()));
                    staff.setProgram(program(scorePart));
                }
                context.plan.getStaves().add(staff);
            }
            for (int m = 0; m < part.getMeasures().size(); m++) {
                ends[m] = ends[m].max(planMeasure(context, state, part.getMeasures().get(m), m));
            }
            closeOpenSpanners(context, state);
            firstStaff += state.staffCount;
        }
        for (int m = 0; m < measureCount; m++) {
            // a measure that does not fill its meter (a pickup, the rest of a measure before a repeat, free meter)
            MeiWriterMeasure measure = context.plan.getMeasures().get(m);
            Fraction meter = Fraction.of(measure.getMeterCount(), measure.getMeterUnit());
            if (ends[m].isPositive() && !ends[m].equals(meter)) {
                measure.setImplicit(true);
            }
        }
        return context.plan;
    }


    /**
     * Plans a measure of a part: walks its content in order with the time position.
     *
     * @param context the context
     * @param state the part state
     * @param source the measure
     * @param m the measure index
     * @return the end of the content of the measure
     */
    private Fraction planMeasure(Context context, PartState state, IMeasure source, int m) {
        MeiWriterMeasure target = context.plan.getMeasures().get(m);
        if (state.partIndex == 0) {
            target.setImplicit("yes".equals(source.getImplicit()));
        }
        Fraction cursor = Fraction.ZERO;
        Fraction end = Fraction.ZERO;
        MeiWriterEvent last = null;
        Map<String, MeiWriterEvent> crossStaff = new HashMap<>();
        state.clefChanges.clear();
        for (IMusicDataElement element : source.getMusicData()) {
            if (element instanceof INote) {
                INote note = (INote) element;
                if (note.getChord() != null && last != null) {
                    MeiWriterEvent chord = chordEvent(context, state, target, crossStaff, last, note);
                    chord.getNotes().add(note);
                    planNoteControls(context, state, m, chord, note);
                    continue;
                }
                last = addEvent(context, state, target, m, note, cursor);
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
                planAttributes(context, state, (IAttributes) element, m, cursor);
            } else if (element instanceof IDirection) {
                IDirection direction = (IDirection) element;
                Fraction position = cursor;
                if (direction.getOffset() != null) {
                    position = cursor.add(length(state, direction.getOffset())).max(Fraction.ZERO);
                }
                planDirection(context, state, m, direction, position);
            } else if (element instanceof IHarmony) {
                planHarmony(target, state, (IHarmony) element, cursor);
            } else if (element instanceof IBarline) {
                planBarline(target, (IBarline) element);
            }
        }
        placeClefChanges(target, state);
        return end;
    }


    /**
     * Gets the event of a chord note: the event of the chord, or for a note on another staff of the part (a
     * cross-staff chord) an event at the same time on that staff.
     *
     * @param context the context
     * @param state the part state
     * @param target the measure
     * @param crossStaff the cross-staff events of the measure by staff and chord
     * @param last the event of the chord
     * @param note the chord note
     * @return the event to add the note to
     */
    private MeiWriterEvent chordEvent(Context context, PartState state, MeiWriterMeasure target, Map<String, MeiWriterEvent> crossStaff,
            MeiWriterEvent last, INote note) {
        int staff = state.staff(note.getStaff());
        INote first = last.getFirst();
        if (first == null || staff == state.staff(first.getStaff())) {
            return last;
        }
        String key = staff + ":" + last.getId();
        MeiWriterEvent event = crossStaff.get(key);
        if (event == null) {
            event = new MeiWriterEvent(context.id(), last.getStart(), last.getLength());
            int voice = 1;
            if (first.getVoice() != null && first.getVoice() > 0) {
                voice = first.getVoice();
            }
            target.voice(staff, voice).add(event);
            crossStaff.put(key, event);
        }
        return event;
    }


    /**
     * Adds an event for a note (or rest) that starts a new event.
     *
     * @param context the context
     * @param state the part state
     * @param target the measure
     * @param m the measure index
     * @param note the note
     * @param cursor the time position
     * @return the event
     */
    private MeiWriterEvent addEvent(Context context, PartState state, MeiWriterMeasure target, int m, INote note, Fraction cursor) {
        Fraction length = Fraction.ZERO;
        if (note.getGrace() == null) {
            length = length(state, note.getDuration());
        }
        MeiWriterEvent event = new MeiWriterEvent(context.id(), cursor, length);
        event.getNotes().add(note);
        int voice = 1;
        if (note.getVoice() != null && note.getVoice() > 0) {
            voice = note.getVoice();
        }
        target.voice(state.staff(note.getStaff()), voice).add(event);
        planNoteControls(context, state, m, event, note);
        return event;
    }


    /**
     * Plans the control events of a note: slurs, ornaments, fingerings, breath marks, caesuras and dynamics.
     *
     * @param context the context
     * @param state the part state
     * @param m the measure index
     * @param event the event of the note
     * @param note the note
     */
    private void planNoteControls(Context context, PartState state, int m, MeiWriterEvent event, INote note) {
        if (note.getNotations() == null) {
            return;
        }
        String staff = String.valueOf(state.staff(note.getStaff()));
        String startId = "#" + event.getId();
        List<MeiWriterControl> controls = context.plan.getMeasures().get(m).getControls();
        for (com.github.toolarium.music.model.notation.ISlur slur : nonNull(note.getNotations().getSlurs())) {
            int number = 1;
            if (slur.getNumber() != null) {
                number = slur.getNumber();
            }
            if (START.equals(slur.getType())) {
                state.openSlurs.put(number, new Object[] {m, event.getId(), slur.getPlacement()});
            } else if (STOP.equals(slur.getType())) {
                Object[] open = state.openSlurs.remove(number);
                if (open != null && !event.getId().equals(open[1])) {
                    MeiWriterControl control = new MeiWriterControl("slur").put(STAFF, staff).put(STARTID, "#" + open[1]).put("endid", startId);
                    control.put("curvedir", placement((String) open[2]));
                    context.plan.getMeasures().get((Integer) open[0]).getControls().add(control);
                }
            }
        }
        if (event.getFirst() == note) {
            for (MeiWriterControl control : MeiNotationConverter.getInstance().ornaments(note.getNotations())) {
                controls.add(control.put(STAFF, staff).put(STARTID, startId));
            }
        }
        for (MeiWriterControl control : MeiNotationConverter.getInstance().noteMarks(note.getNotations())) {
            controls.add(control.put(STAFF, staff).put(STARTID, startId));
        }
    }


    /**
     * Plans attributes: divisions, the initial staff definitions, key and meter changes and clef changes.
     *
     * @param context the context
     * @param state the part state
     * @param attributes the attributes
     * @param m the measure index
     * @param cursor the time position
     */
    private void planAttributes(Context context, PartState state, IAttributes attributes, int m, Fraction cursor) {
        MeiWriterMeasure target = context.plan.getMeasures().get(m);
        if (attributes.getDivisions() != null && attributes.getDivisions() > 0) {
            state.divisions = attributes.getDivisions();
        }
        if (state.partIndex == 0) {
            for (ITime time : nonNull(attributes.getTimes())) {
                int count = beats(time.getBeats());
                int unit = intValue(time.getBeatType(), 0);
                if (count > 0 && unit > 0) {
                    applyMeter(context, m, count, unit);
                }
            }
        }
        for (IKey key : nonNull(attributes.getKeys())) {
            if (key.getFifths() == null) {
                continue;
            }
            String sig = keySig(key.getFifths());
            if (m == 0 || !sig.equals(state.keySig) || key.getMode() != null && !key.getMode().equals(state.keyMode)) {
                for (int s = 0; s < state.staffCount; s++) {
                    target.getKeySigs().put(state.firstStaff + s, sig);
                    if (key.getMode() != null) {
                        target.getKeyModes().put(state.firstStaff + s, key.getMode());
                    }
                }
                state.keySig = sig;
                state.keyMode = key.getMode();
            }
        }
        for (IClef clef : nonNull(attributes.getClefs())) {
            MeiWriterStaff staff = context.plan.getStaves().get(state.staff(clef.getNumber()) - 1);
            if (m == 0 && cursor.isZero() && staff.getClef() == null) {
                staff.setClef(clef);
            } else {
                state.clefChanges.add(new Object[] {staff.getNumber(), cursor, clef});
            }
        }
        if (m == 0) {
            planStaffDetails(context, state, attributes);
        }
    }


    /**
     * Plans the staff lines and the transposition of the staves of a part.
     *
     * @param context the context
     * @param state the part state
     * @param attributes the attributes of the first measure
     */
    private void planStaffDetails(Context context, PartState state, IAttributes attributes) {
        for (IStaffDetails details : nonNull(attributes.getStaffDetails())) {
            if (details.getStaffLines() != null && details.getStaffLines() >= 0) {
                context.plan.getStaves().get(state.staff(details.getNumber()) - 1).setLines(details.getStaffLines());
            }
        }
        for (ITranspose transpose : nonNull(attributes.getTransposes())) {
            int octaves = intValue(transpose.getOctaveChange(), 0);
            int diatonic = intValue(transpose.getDiatonic(), 0) + octaves * 7;
            int semitones = intValue(transpose.getChromatic(), 0) + octaves * 12;
            if (diatonic == 0 && semitones == 0) {
                continue;
            }
            for (int s = 0; s < state.staffCount; s++) {
                context.plan.getStaves().get(state.firstStaff + s - 1).setTranspose(diatonic, semitones);
            }
        }
    }


    /**
     * Applies a meter from a measure on (the meter of the first part is the meter of the score).
     *
     * @param context the context
     * @param m the measure index
     * @param count the beats
     * @param unit the beat unit
     */
    private void applyMeter(Context context, int m, int count, int unit) {
        List<MeiWriterMeasure> measures = context.plan.getMeasures();
        MeiWriterMeasure target = measures.get(m);
        boolean change = m > 0 && (measures.get(m - 1).getMeterCount() != count || measures.get(m - 1).getMeterUnit() != unit);
        target.setMeter(count, unit, change || target.isMeterChange());
        for (int i = m + 1; i < measures.size(); i++) {
            measures.get(i).setMeter(count, unit, false);
        }
    }


    /**
     * Plans a direction: dynamics, hairpins, words, tempo, rehearsal marks, segno and coda, pedals and octave lines.
     *
     * @param context the context
     * @param state the part state
     * @param m the measure index
     * @param direction the direction
     * @param position the time position
     */
    private void planDirection(Context context, PartState state, int m, IDirection direction, Fraction position) {
        MeiWriterMeasure target = context.plan.getMeasures().get(m);
        String staff = String.valueOf(state.staff(direction.getStaff()));
        String tstamp = beat(target, position);
        String place = placement(direction.getPlacement());
        StringBuilder words = new StringBuilder();
        IDirectionType metronome = null;
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
                metronome = type;
            }
            planDirectionType(context, state, m, type, position, staff, place);
        }
        String tempo = null;
        if (direction.getSound() != null) {
            tempo = trimToNull(direction.getSound().getTempo());
        }
        if (metronome != null || tempo != null && words.length() > 0) {
            MeiWriterControl control = new MeiWriterControl("tempo").put(STAFF, staff).put(TSTAMP, tstamp).put(PLACE, place);
            MeiNotationConverter.getInstance().applyMetronome(control, metronome, tempo);
            if (words.length() > 0) {
                control.setText(words.toString());
            }
            target.getControls().add(control);
        } else if (words.length() > 0) {
            MeiWriterControl control = new MeiWriterControl("dir").put(STAFF, staff).put(TSTAMP, tstamp).put(PLACE, place);
            control.setText(words.toString());
            target.getControls().add(control);
        } else if (tempo != null) {
            target.getControls().add(new MeiWriterControl("tempo").put(STAFF, staff).put(TSTAMP, tstamp).put("midi.bpm", tempo));
        }
    }


    /**
     * Plans one direction type except words and metronome.
     *
     * @param context the context
     * @param state the part state
     * @param m the measure index
     * @param type the direction type
     * @param position the time position
     * @param staff the MEI staff number
     * @param place the placement, or null
     */
    private void planDirectionType(Context context, PartState state, int m, IDirectionType type, Fraction position, String staff, String place) {
        MeiWriterMeasure target = context.plan.getMeasures().get(m);
        String tstamp = beat(target, position);
        MeiNotationConverter converter = MeiNotationConverter.getInstance();
        String dynamics = converter.dynamics(type.getDynamics());
        if (dynamics != null) {
            MeiWriterControl control = new MeiWriterControl("dynam").put(STAFF, staff).put(TSTAMP, tstamp).put(PLACE, place);
            control.setText(dynamics);
            target.getControls().add(control);
        }
        if (type.getRehearsal() != null && !type.getRehearsal().trim().isEmpty()) {
            MeiWriterControl control = new MeiWriterControl("reh").put(STAFF, staff).put(TSTAMP, tstamp);
            control.setText(type.getRehearsal().trim());
            target.getControls().add(control);
        }
        if (type.getSegno() != null) {
            target.getControls().add(new MeiWriterControl("repeatMark").put("func", "segno").put(STAFF, staff).put(TSTAMP, tstamp));
        }
        if (type.getCoda() != null) {
            target.getControls().add(new MeiWriterControl("repeatMark").put("func", "coda").put(STAFF, staff).put(TSTAMP, tstamp));
        }
        if (type.getPedal() != null) {
            String dir = converter.pedal(type.getPedal().getType());
            if (dir != null) {
                target.getControls().add(new MeiWriterControl("pedal").put("dir", dir).put(STAFF, staff).put(TSTAMP, tstamp));
            }
        }
        if (type.getWedge() != null) {
            planSpanner(context, state.openWedges, m, position, type.getWedge().getType(), type.getWedge().getNumber(),
                    converter.wedge(type.getWedge().getType(), staff, tstamp, place));
        }
        if (type.getOctaveShift() != null) {
            planSpanner(context, state.openOctaves, m, position, type.getOctaveShift().getType(), null,
                    converter.octave(type.getOctaveShift().getType(), type.getOctaveShift().getSize(), staff, tstamp));
        }
    }


    /**
     * Plans the start or the end of a spanner (hairpin or octave line): the start is added to the measure, the end
     * sets the end time stamp of the open spanner with the same number.
     *
     * @param context the context
     * @param open the open spanners by number
     * @param m the measure index
     * @param position the time position
     * @param type the MusicXML type (stop ends the spanner)
     * @param number the spanner number, or null
     * @param start the control event of a start, or null
     */
    private void planSpanner(Context context, Map<Integer, Object[]> open, int m, Fraction position, String type, Integer number,
            MeiWriterControl start) {
        int key = 1;
        if (number != null) {
            key = number;
        }
        if (STOP.equals(type)) {
            Object[] started = open.remove(key);
            if (started != null) {
                int startMeasure = (Integer) started[0];
                ((MeiWriterControl) started[1]).put(TSTAMP2, (m - startMeasure) + MEASURES_PLUS + beat(context.plan.getMeasures().get(m), position));
            }
        } else if (start != null) {
            context.plan.getMeasures().get(m).getControls().add(start);
            open.put(key, new Object[] {m, start});
        }
    }


    /**
     * Ends the spanners that are still open at the end of a part: they end with their measure.
     *
     * @param context the context
     * @param state the part state
     */
    private void closeOpenSpanners(Context context, PartState state) {
        List<Object[]> open = new ArrayList<>(state.openWedges.values());
        open.addAll(state.openOctaves.values());
        for (Object[] started : open) {
            MeiWriterMeasure measure = context.plan.getMeasures().get((Integer) started[0]);
            ((MeiWriterControl) started[1]).put(TSTAMP2, "0" + MEASURES_PLUS + (measure.getMeterCount() + 1));
        }
        state.openWedges.clear();
        state.openOctaves.clear();
        state.openSlurs.clear();
    }


    /**
     * Plans a chord symbol.
     *
     * @param target the measure
     * @param state the part state
     * @param harmony the chord symbol
     * @param position the time position
     */
    private void planHarmony(MeiWriterMeasure target, PartState state, IHarmony harmony, Fraction position) {
        String text = ChordSymbolFormatter.getInstance().format(harmony);
        if (text == null) {
            return;
        }
        MeiWriterControl control = new MeiWriterControl("harm").put(STAFF, String.valueOf(state.staff(harmony.getStaff()))).put(TSTAMP, beat(target, position));
        control.setText(text);
        target.getControls().add(control);
    }


    /**
     * Plans a barline: its style, repeat and ending.
     *
     * @param target the measure
     * @param barline the barline
     */
    private void planBarline(MeiWriterMeasure target, IBarline barline) {
        boolean left = "left".equals(barline.getLocation());
        String rendition = MeiNotationConverter.getInstance().barRendition(barline.getBarStyle());
        if (barline.getRepeat() != null) {
            if ("forward".equals(barline.getRepeat().getDirection())) {
                target.setLeft("rptstart");
            } else {
                target.setRight("rptend");
            }
        } else if (rendition != null) {
            if (left) {
                target.setLeft(rendition);
            } else if (target.getRight() == null) {
                target.setRight(rendition);
            }
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
     * Places the clef changes of a measure before the first event of the staff at or after their position (or at the
     * end of the measure).
     *
     * @param target the measure
     * @param state the part state with the clef changes of the measure
     */
    private void placeClefChanges(MeiWriterMeasure target, PartState state) {
        for (Object[] change : state.clefChanges) {
            int staff = (Integer) change[0];
            Fraction position = (Fraction) change[1];
            MeiWriterEvent found = null;
            Map<Integer, List<MeiWriterEvent>> voices = target.getStaves().get(staff);
            if (voices != null && !voices.isEmpty()) {
                for (MeiWriterEvent event : voices.values().iterator().next()) {
                    if (event.getStart().compareTo(position) >= 0) {
                        found = event;
                        break;
                    }
                }
            }
            if (found != null) {
                found.getClefsBefore().add((IClef) change[2]);
            } else {
                target.getClefsAtEnd().computeIfAbsent(staff, key -> new ArrayList<>()).add((IClef) change[2]);
            }
        }
        state.clefChanges.clear();
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
     * Gets the time stamp (beat) of a position in a measure: 1 is the start, the unit is the beat of the meter.
     *
     * @param measure the measure
     * @param position the position in whole notes
     * @return the time stamp
     */
    static String beat(MeiWriterMeasure measure, Fraction position) {
        Fraction beat = position.multiply(Math.max(1, measure.getMeterUnit()), 1);
        BigDecimal value = BigDecimal.valueOf(beat.getNumerator()).divide(BigDecimal.valueOf(beat.getDenominator()), BEAT_SCALE, RoundingMode.HALF_UP)
                .add(BigDecimal.ONE).stripTrailingZeros();
        return value.toPlainString();
    }


    /**
     * Gets the MEI key signature of a number of fifths.
     *
     * @param fifths the fifths (positive for sharps)
     * @return the key signature, for example "2s", "3f" or "0"
     */
    static String keySig(int fifths) {
        if (fifths > 0) {
            return fifths + "s";
        }
        if (fifths < 0) {
            return -fifths + "f";
        }
        return "0";
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
     * Gets the number of a measure: the number of the first part that has the measure, as an MEI token.
     *
     * @param parts the parts
     * @param m the measure index
     * @return the number
     */
    private String measureNumber(List<? extends IPart> parts, int m) {
        for (IPart part : parts) {
            if (m < part.getMeasures().size() && trimToNull(part.getMeasures().get(m).getNumber()) != null) {
                return part.getMeasures().get(m).getNumber().trim().replaceAll("[^A-Za-z0-9._\\-]", "-");
            }
        }
        return String.valueOf(m + 1);
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
     * Gets the MEI placement.
     *
     * @param placement the MusicXML placement, or null
     * @return above, below or null
     */
    private String placement(String placement) {
        if (ABOVE.equals(placement) || BELOW.equals(placement)) {
            return placement;
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
}
