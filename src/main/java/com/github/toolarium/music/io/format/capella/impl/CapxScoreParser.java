/*
 * CapxScoreParser.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl;

import com.github.toolarium.music.io.converter.PitchSpeller;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.MeasureData;
import com.github.toolarium.music.io.dto.NoteData;
import com.github.toolarium.music.io.dto.PartData;
import com.github.toolarium.music.io.dto.ScoreData;
import com.github.toolarium.music.io.dto.SpelledPitch;
import com.github.toolarium.music.io.format.capella.impl.dto.CapellaSpan;
import com.github.toolarium.music.io.format.capella.impl.dto.CapellaStaffInfo;
import com.github.toolarium.music.io.format.capella.impl.dto.CapellaVoiceState;
import com.github.toolarium.music.io.xml.XmlDocumentReader;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;


/**
 * Reads a capella CapXML document (score.xml of a .capx file) into the format independent
 * {@link ScoreData}.
 *
 * <p>capella stores the music system by system. Each staff of a system has voices with a sequence
 * of note objects (clefs, keys, time signatures, chords, rests and barlines). Barlines are placed
 * automatically by the time signature unless an explicit barline ends a measure earlier; the end of
 * a system also ends a measure. Each staff layout becomes a part; staves joined by a curly bracket
 * form one part with several staves.</p>
 *
 * @author patrick
 */
final class CapxScoreParser {
    private static final int VOICES_PER_STAFF = 4;
    private static final int MAX_NUMBER = 6;
    private static final int TITLE_FONT_HEIGHT = 16;
    private static final String TRUE = "true";
    private static final String EMPTY = "";

    private final XmlDocumentReader xml = XmlDocumentReader.getInstance();
    private final CapellaConverter converter = CapellaConverter.getInstance();
    private final ScoreBuilder builder = ScoreBuilder.getInstance();
    private final ScoreData score = new ScoreData();
    private final List<CapellaStaffInfo> staves = new ArrayList<>();
    private final Map<String, CapellaStaffInfo> staffByLayout = new HashMap<>();
    private final List<String> largeTexts = new ArrayList<>();
    private int measureCount;
    private int slurNumber;
    private int wedgeNumber;
    private int tempo;


    /**
     * Constructor for CapxScoreParser.
     */
    CapxScoreParser() {
        // default constructor
    }


    /**
     * Reads the score element.
     *
     * @param root the score element
     * @return the score data
     */
    ScoreData parse(Element root) {
        Element info = xml.child(root, "info");
        score.setTitle(xml.childText(info, "title"));
        score.addCreator("composer", xml.childText(info, "author"));
        score.addCreator("composer", xml.childText(info, "composer"));
        String software = xml.childText(info, "encodingSoftware");
        if (software == null) {
            score.setSoftware("capella");
        } else {
            score.setSoftware(software);
        }
        readLayout(xml.child(root, "layout"));

        Element systems = xml.child(root, "systems");
        for (Element system : xml.children(systems, "system")) {
            readSystem(system);
        }
        for (PartData part : score.getParts()) {
            for (int m = 0; m < measureCount; m++) {
                part.measure(m);
            }
            while (part.getMeasures().size() > measureCount && part.getMeasures().get(part.getMeasures().size() - 1).getVoices().isEmpty()) {
                part.getMeasures().remove(part.getMeasures().size() - 1);
            }
        }
        if (score.getTitle() == null && !largeTexts.isEmpty()) {
            score.setTitle(largeTexts.get(0));
            if (largeTexts.size() > 1) {
                score.setSubtitle(largeTexts.get(1));
            }
        }
        return score;
    }


    /**
     * Reads the staff layouts (instruments) and brackets, and creates the parts.
     *
     * @param layout the layout element (may be null)
     */
    private void readLayout(Element layout) {
        List<Element> layouts = xml.children(xml.child(layout, "staves"), "staffLayout");
        boolean[] joined = new boolean[layouts.size()];
        for (Element bracket : xml.children(xml.child(layout, "brackets"), "bracket")) {
            if (!TRUE.equals(xml.attr(bracket, "curly"))) {
                continue;
            }
            int from = intValue(xml.attr(bracket, "from"), -1);
            int to = intValue(xml.attr(bracket, "to"), -1);
            for (int i = from + 1; i <= to && i < joined.length && from >= 0; i++) {
                joined[i] = true;
            }
        }
        for (int i = 0; i < layouts.size(); i++) {
            Element staffLayout = layouts.get(i);
            String description = xml.attr(staffLayout, "description");
            if (description == null) {
                description = EMPTY;
            }
            CapellaStaffInfo staff = new CapellaStaffInfo(description);
            Element notation = xml.child(staffLayout, "notation");
            staff.setDefaultClef(converter.clefOf(xml.attr(notation, "defaultClef")));
            if (joined[i] && !score.getParts().isEmpty()) {
                PartData part = score.getParts().get(score.getParts().size() - 1);
                part.setStaves(part.getStaves() + 1);
                staff.setPartIndex(score.getParts().size() - 1);
                staff.setStaffInPart(part.getStaves());
            } else {
                PartData part = new PartData(instrumentName(staffLayout, description));
                part.setAbbreviation(instrumentAbbreviation(staffLayout));
                Element sound = xml.child(staffLayout, "sound");
                int program = intValue(xml.attr(sound, "instr"), -1);
                if (program >= 0 && program < 128) {
                    part.setMidiProgram(program + 1);
                }
                int transpose = intValue(xml.attr(xml.child(staffLayout, "instrument"), "transpose"), 0);
                if (transpose != 0) {
                    part.setTranspose(transpose, 0);
                }
                score.getParts().add(part);
                staff.setPartIndex(score.getParts().size() - 1);
                staff.setStaffInPart(1);
            }
            if (staff.getDefaultClef() != null) {
                score.getParts().get(staff.getPartIndex()).getDefaultClefs().put(staff.getStaffInPart(), staff.getDefaultClef());
            }
            staves.add(staff);
            if (!staffByLayout.containsKey(description)) {
                staffByLayout.put(description, staff);
            } else {
                staffByLayout.put(description, null);
            }
        }
    }


    /**
     * Gets the instrument name of a staff layout.
     *
     * @param staffLayout the staffLayout element
     * @param description the layout description
     * @return the name, or the description
     */
    private String instrumentName(Element staffLayout, String description) {
        Element instrument = xml.child(staffLayout, "instrument");
        String name = xml.childText(instrument, "name");
        if (name == null) {
            name = xml.attr(instrument, "name");
        }
        if (name == null && !description.isEmpty()) {
            name = description;
        }
        return name;
    }


    /**
     * Gets the instrument abbreviation of a staff layout.
     *
     * @param staffLayout the staffLayout element
     * @return the abbreviation, or null
     */
    private String instrumentAbbreviation(Element staffLayout) {
        Element instrument = xml.child(staffLayout, "instrument");
        String abbreviation = xml.childText(instrument, "abbrev");
        if (abbreviation == null) {
            abbreviation = xml.attr(instrument, "abbrev");
        }
        return abbreviation;
    }


    /**
     * Reads a system: all staves and voices, then aligns the measure count of all parts.
     *
     * @param system the system element
     */
    private void readSystem(Element system) {
        int start = measureCount;
        int end = start;
        int systemTempo = intValue(xml.attr(system, "tempo"), 0);
        if (systemTempo > 0 && systemTempo != tempo && !score.getParts().isEmpty()) {
            score.getParts().get(0).measure(start).addDirection(builder.createTempo(systemTempo, "quarter", 0, null), Fraction.ZERO, 1);
            tempo = systemTempo;
        }
        List<Element> systemStaves = xml.children(xml.child(system, "staves"), "staff");
        for (int s = 0; s < systemStaves.size(); s++) {
            Element staffElement = systemStaves.get(s);
            CapellaStaffInfo staff = staffOf(staffElement, s);
            if (staff == null) {
                continue;
            }
            PartData part = score.getParts().get(staff.getPartIndex());
            if (staff.getStaffInPart() == 1 && timeBefore(part, start + 1) == null) {
                int[] time = converter.timeOf(xml.attr(staffElement, "defaultTime"));
                if (time != null) {
                    setTime(staff, time);
                    applyTime(start, staff);
                }
            }
            List<Element> voices = xml.children(xml.child(staffElement, "voices"), "voice");
            for (int v = 0; v < voices.size() && v < VOICES_PER_STAFF; v++) {
                CapellaVoiceState state = new CapellaVoiceState();
                state.setMeasureIndex(start);
                readVoice(voices.get(v), staff, v, state, start);
                if (state.getCursor().isPositive()) {
                    state.setMeasureIndex(state.getMeasureIndex() + 1);
                }
                end = Math.max(end, state.getMeasureIndex());
            }
        }
        if (end == start) {
            end = start + 1;
        }
        measureCount = end;
    }


    /**
     * Finds the staff layout of a staff in a system: by its layout description, or by its position.
     *
     * @param staffElement the staff element
     * @param position the position of the staff in the system
     * @return the staff layout, or null
     */
    private CapellaStaffInfo staffOf(Element staffElement, int position) {
        String layout = xml.attr(staffElement, "layout");
        if (layout == null) {
            layout = EMPTY;
        }
        CapellaStaffInfo staff = staffByLayout.get(layout);
        if (staff == null && position < staves.size()) {
            staff = staves.get(position);
        }
        return staff;
    }


    /**
     * Reads the note objects of a voice in a system.
     *
     * @param voice the voice element
     * @param staff the staff layout
     * @param voiceIndex the index of the voice in the staff
     * @param state the voice state
     * @param systemStart the index of the first measure of the system
     */
    private void readVoice(Element voice, CapellaStaffInfo staff, int voiceIndex, CapellaVoiceState state, int systemStart) {
        PartData part = score.getParts().get(staff.getPartIndex());
        int voiceNumber = (staff.getStaffInPart() - 1) * VOICES_PER_STAFF + voiceIndex + 1;
        boolean primary = voiceIndex == 0 && staff.getStaffInPart() == 1;
        for (Element object : xml.elements(xml.child(voice, "noteObjects"))) {
            MeasureData measure = part.measure(state.getMeasureIndex());
            switch (xml.name(object)) {
                case "clefSign":
                    ClefImpl clef = converter.clefOf(xml.attr(object, "clef"));
                    if (clef != null && voiceIndex == 0) {
                        measure.addClef(staff.getStaffInPart(), state.getCursor(), clef);
                    }
                    break;
                case "keySign":
                    int fifths = intValue(xml.attr(object, "fifths"), 0);
                    if (primary) {
                        if (state.getCursor().isZero()) {
                            measure.setKey(fifths, null);
                        } else {
                            state.setPendingFifths(fifths);
                        }
                    }
                    break;
                case "timeSign":
                    int[] time = converter.timeOf(xml.attr(object, "time"));
                    if (time != null) {
                        setTime(staff, time);
                        if (primary && state.getCursor().isZero()) {
                            applyTime(state.getMeasureIndex(), staff);
                        } else if (primary) {
                            state.setPendingTime(true);
                        }
                    }
                    break;
                case "chord":
                case "rest":
                    EventData event = readEvent(object, measure, state, measureLength(part, state.getMeasureIndex()));
                    measure.voice(staff.getStaffInPart(), voiceNumber).getEvents().add(event);
                    if (!event.isGrace()) {
                        advance(event, part, staff, state, primary);
                    }
                    break;
                case "barline":
                    readBarline(object, part, state, primary);
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * Moves the voice cursor after an event and starts a new measure when the measure is full.
     *
     * @param event the event
     * @param part the part
     * @param staff the staff layout
     * @param state the voice state
     * @param primary true for the first voice of the first staff of the part
     */
    private void advance(EventData event, PartData part, CapellaStaffInfo staff, CapellaVoiceState state, boolean primary) {
        Fraction duration = event.getDuration();
        if (event.isMeasureRest() || duration == null) {
            duration = measureLength(part, state.getMeasureIndex());
        }
        state.setCursor(state.getCursor().add(duration));
        Fraction length = measureLength(part, state.getMeasureIndex());
        if (length.isPositive() && state.getCursor().compareTo(length) >= 0) {
            nextMeasure(part, staff, state, primary);
        }
    }


    /**
     * Starts the next measure of a voice and applies pending key, time and repeat start.
     *
     * @param part the part
     * @param staff the staff layout
     * @param state the voice state
     * @param primary true for the first voice of the first staff of the part
     */
    private void nextMeasure(PartData part, CapellaStaffInfo staff, CapellaVoiceState state, boolean primary) {
        state.setMeasureIndex(state.getMeasureIndex() + 1);
        state.setCursor(Fraction.ZERO);
        boolean pending = state.getPendingFifths() != null || state.isPendingTime() || state.isRepeatStartPending();
        if (!primary || !pending) {
            return;
        }
        MeasureData measure = part.measure(state.getMeasureIndex());
        if (state.getPendingFifths() != null) {
            measure.setKey(state.getPendingFifths(), null);
            state.setPendingFifths(null);
        }
        if (state.isPendingTime()) {
            applyTime(state.getMeasureIndex(), staff);
            state.setPendingTime(false);
        }
        if (state.isRepeatStartPending()) {
            measure.setRepeatStart(true);
            state.setRepeatStartPending(false);
        }
    }


    /**
     * Reads a barline: ends the measure (unless it is empty) and sets repeats and bar styles.
     *
     * @param barline the barline element
     * @param part the part
     * @param state the voice state
     * @param primary true for the first voice of the first staff of the part
     */
    private void readBarline(Element barline, PartData part, CapellaVoiceState state, boolean primary) {
        String type = xml.attr(barline, "type");
        if (type == null) {
            type = "single";
        }
        boolean ended = state.getCursor().isPositive();
        int closedIndex = state.getMeasureIndex();
        if (!ended) {
            closedIndex = state.getMeasureIndex() - 1;
        }
        if (primary && closedIndex >= 0) {
            MeasureData closed = part.measure(closedIndex);
            switch (type) {
                case "double":
                    closed.setRightBarStyle("light-light");
                    break;
                case "end":
                    closed.setRightBarStyle("light-heavy");
                    break;
                case "dashed":
                    closed.setRightBarStyle("dashed");
                    break;
                case "repEnd":
                case "repEndStart":
                    closed.setRepeatEnd(true, null);
                    break;
                default:
                    break;
            }
        }
        boolean repeatStart = "repStart".equals(type) || "repEndStart".equals(type);
        if (ended) {
            state.setRepeatStartPending(repeatStart);
            state.setMeasureIndex(state.getMeasureIndex() + 1);
            state.setCursor(Fraction.ZERO);
            if (primary && repeatStart) {
                part.measure(state.getMeasureIndex()).setRepeatStart(true);
                state.setRepeatStartPending(false);
            }
        } else if (primary && repeatStart) {
            part.measure(state.getMeasureIndex()).setRepeatStart(true);
        }
    }


    /**
     * Reads a chord or rest with its duration, pitches, lyrics, articulations and drawing objects.
     *
     * @param object the chord or rest element
     * @param measure the measure
     * @param state the voice state
     * @param nominal the length of the measure by the time signature
     * @return the event
     */
    private EventData readEvent(Element object, MeasureData measure, CapellaVoiceState state, Fraction nominal) {
        EventData event = new EventData();
        Element duration = xml.child(object, "duration");
        Fraction base = converter.baseOf(xml.attr(duration, "base"));
        if (base == null) {
            base = Fraction.of(1, 4);
        }
        int dots = intValue(xml.attr(duration, "dots"), 0);
        Fraction length = base;
        Fraction add = base;
        for (int i = 0; i < dots; i++) {
            add = add.multiply(1, 2);
            length = length.add(add);
        }
        Element tuplet = xml.child(duration, "tuplet");
        event.setDuration(length);
        if (!TRUE.equals(xml.attr(duration, "noDuration"))) {
            applyTuplet(tuplet, base, event, state);
        }
        if (TRUE.equals(xml.attr(duration, "noDuration"))) {
            event.setGrace(true);
            event.setGraceSlash(true);
        }

        if ("rest".equals(xml.name(object))) {
            Element display = xml.child(object, "display");
            event.setInvisible(TRUE.equals(xml.attr(display, "invisible")));
            if (Fraction.of(1, 1).equals(base) && dots == 0 && tuplet == null && event.getTupletActual() == 0 && state.getCursor().isZero()
                    && !Fraction.of(1, 1).equals(nominal)) {
                event.setMeasureRest(true);
                event.setDuration(null);
            }
        } else {
            readHeads(object, event);
            readLyrics(object, event, state);
            for (Element articulation : xml.children(object, "articulation")) {
                converter.applyArticulation(xml.attr(articulation, "type"), event);
            }
            Element stem = xml.child(object, "stem");
            String direction = xml.attr(stem, "dir");
            if ("up".equals(direction) || "down".equals(direction)) {
                event.setStem(direction);
            }
        }
        applySpans(event, state);
        readDrawObjects(object, event, measure, state);
        return event;
    }


    /**
     * Applies the tuplet of a note. A tuplet group starts at a note with tuplet information and lasts
     * the length of its first note value times the normal notes; following notes without tuplet
     * information belong to the open group until it is complete.
     *
     * @param tuplet the tuplet element (may be null)
     * @param base the undotted note value
     * @param event the event (its duration is scaled)
     * @param state the voice state with the open tuplet group
     */
    private void applyTuplet(Element tuplet, Fraction base, EventData event, CapellaVoiceState state) {
        boolean open = state.getTupletRemaining() != null && state.getTupletRemaining().isPositive();
        if (tuplet != null && !open) {
            int count = intValue(xml.attr(tuplet, "count"), 0);
            if (count <= 1) {
                return;
            }
            int normal = converter.tupletNormal(count, TRUE.equals(xml.attr(tuplet, "tripartite")), TRUE.equals(xml.attr(tuplet, "prolong")));
            state.setTupletCount(count);
            state.setTupletNormal(normal);
            state.setTupletRemaining(base.multiply(normal, 1));
            event.setTupletStart(true);
            open = true;
        }
        if (!open) {
            return;
        }
        event.setDuration(event.getDuration().multiply(state.getTupletNormal(), state.getTupletCount()));
        event.setTuplet(state.getTupletCount(), state.getTupletNormal());
        state.setTupletRemaining(state.getTupletRemaining().subtract(event.getDuration()));
        state.setTupletLast(event);
        if (!state.getTupletRemaining().isPositive()) {
            event.setTupletStop(true);
            state.setTupletRemaining(null);
        }
    }


    /**
     * Reads the note heads of a chord: pitch, alteration and ties.
     *
     * @param chord the chord element
     * @param event the event
     */
    private void readHeads(Element chord, EventData event) {
        for (Element head : xml.children(xml.child(chord, "heads"), "head")) {
            SpelledPitch pitch = converter.pitchOf(xml.attr(head, "pitch"));
            if (pitch == null) {
                continue;
            }
            Element alter = xml.child(head, "alter");
            int step = intValue(xml.attr(alter, "step"), 0);
            NoteData note = new NoteData(pitch.getStep(), step, pitch.getOctave());
            if (alter != null && "force".equals(xml.attr(alter, "display"))) {
                note.setAccidental(PitchSpeller.getInstance().accidentalName(step));
            }
            Element tie = xml.child(head, "tie");
            note.setTieStart(TRUE.equals(xml.attr(tie, "begin")));
            note.setTieStop(TRUE.equals(xml.attr(tie, "end")));
            String shape = xml.attr(head, "shape");
            if ("cross".equals(shape)) {
                note.setNotehead("x");
            } else if ("diamond".equals(shape)) {
                note.setNotehead("diamond");
            }
            event.getNotes().add(note);
        }
    }


    /**
     * Reads the lyrics of a chord.
     *
     * @param chord the chord element
     * @param event the event
     * @param state the voice state (hyphen state of each verse)
     */
    private void readLyrics(Element chord, EventData event, CapellaVoiceState state) {
        for (Element verse : xml.children(xml.child(chord, "lyric"), "verse")) {
            int number = intValue(xml.attr(verse, "i"), 0) + 1;
            String text = xml.text(verse);
            if (text.isEmpty()) {
                continue;
            }
            boolean hyphenBefore = Boolean.TRUE.equals(state.getHyphens().get(number));
            boolean hyphenAfter = TRUE.equals(xml.attr(verse, "hyphen"));
            String syllabic = "single";
            if (hyphenBefore && hyphenAfter) {
                syllabic = "middle";
            } else if (hyphenBefore) {
                syllabic = "end";
            } else if (hyphenAfter) {
                syllabic = "begin";
            }
            state.getHyphens().put(number, hyphenAfter);
            event.getLyrics().add(builder.createLyric(number, text, syllabic, TRUE.equals(xml.attr(verse, "extender"))));
        }
    }


    /**
     * Counts the open slurs and wedges down and ends them at this event.
     *
     * @param event the event
     * @param state the voice state
     */
    private void applySpans(EventData event, CapellaVoiceState state) {
        Iterator<CapellaSpan> iterator = state.getSpans().iterator();
        while (iterator.hasNext()) {
            CapellaSpan span = iterator.next();
            span.setRemaining(span.getRemaining() - 1);
            if (span.getRemaining() > 0) {
                continue;
            }
            if (span.getStopDirection() != null) {
                event.getDirections().add(span.getStopDirection());
            } else {
                event.getSlurStops().add(span.getSlurNumber());
            }
            iterator.remove();
        }
    }


    /**
     * Reads the drawing objects of a chord or rest: slurs, wedges, voltas, texts and trills.
     *
     * @param object the chord or rest element
     * @param event the event
     * @param measure the measure
     * @param state the voice state
     */
    private void readDrawObjects(Element object, EventData event, MeasureData measure, CapellaVoiceState state) {
        for (Element drawObj : xml.children(xml.child(object, "drawObjects"), "drawObj")) {
            int range = intValue(xml.attr(xml.child(drawObj, "basic"), "noteRange"), 0);
            for (Element shape : xml.elements(drawObj)) {
                switch (xml.name(shape)) {
                    case "slur":
                        if (range > 0) {
                            slurNumber = slurNumber % MAX_NUMBER + 1;
                            event.getSlurStarts().add(slurNumber);
                            state.getSpans().add(new CapellaSpan(range, slurNumber, null));
                        }
                        break;
                    case "wedge":
                        if (range > 0) {
                            wedgeNumber = wedgeNumber % MAX_NUMBER + 1;
                            String type = "crescendo";
                            if (TRUE.equals(xml.attr(shape, "decresc"))) {
                                type = "diminuendo";
                            }
                            event.getDirections().add(builder.createWedge(type, wedgeNumber));
                            DirectionImpl stop = builder.createWedge(ScoreBuilder.STOP, wedgeNumber);
                            state.getSpans().add(new CapellaSpan(range, 0, stop));
                        }
                        break;
                    case "volta":
                        measure.setEndingNumber(endingNumber(shape));
                        break;
                    case "trill":
                        event.ornaments().setTrillMark(EMPTY);
                        break;
                    case "text":
                        readText(shape, event, state);
                        break;
                    default:
                        break;
                }
            }
        }
    }


    /**
     * Reads a text drawing object: large texts at the start become title and subtitle, others are directions.
     *
     * @param text the text element
     * @param event the event
     * @param state the voice state
     */
    private void readText(Element text, EventData event, CapellaVoiceState state) {
        String content = xml.childText(text, "content");
        if (content == null) {
            return;
        }
        int height = intValue(xml.attr(xml.child(text, "font"), "height"), 0);
        boolean start = state.getMeasureIndex() == 0 && state.getCursor().isZero();
        if (start && height >= TITLE_FONT_HEIGHT && largeTexts.size() < 2) {
            largeTexts.add(content);
            return;
        }
        String placement = ScoreBuilder.ABOVE;
        if (intValue(xml.attr(text, "y"), 0) > 0) {
            placement = ScoreBuilder.BELOW;
        }
        event.getDirections().add(builder.createWords(content, placement, null, null));
    }


    /**
     * Gets the ending number of a volta: firstNumber to lastNumber.
     *
     * @param volta the volta element
     * @return the ending number, for example "1" or "1, 2"
     */
    private String endingNumber(Element volta) {
        int first = intValue(xml.attr(volta, "firstNumber"), 1);
        int last = intValue(xml.attr(volta, "lastNumber"), first);
        StringBuilder sb = new StringBuilder();
        for (int i = first; i <= last; i++) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(i);
        }
        return sb.toString();
    }


    /**
     * Sets the time signature of a staff.
     *
     * @param staff the staff layout
     * @param time beats, beat type and symbol code (1 common, 2 cut)
     */
    private void setTime(CapellaStaffInfo staff, int[] time) {
        staff.setBeats(time[0]);
        staff.setBeatType(time[1]);
        staff.setTimeSymbol(null);
        if (time[2] == 1) {
            staff.setTimeSymbol("common");
        } else if (time[2] == 2) {
            staff.setTimeSymbol("cut");
        }
        staff.setTimeSet(true);
    }


    /**
     * Writes the time signature of a staff to a measure of all parts (capella time signatures apply to all staves).
     *
     * @param index the measure index
     * @param staff the staff layout
     */
    private void applyTime(int index, CapellaStaffInfo staff) {
        if (staff.getBeats() <= 0) {
            return;
        }
        for (PartData part : score.getParts()) {
            part.measure(index).setTime(staff.getBeats(), staff.getBeatType(), staff.getTimeSymbol());
        }
    }


    /**
     * Gets the measure length of the time signature in effect at a measure of a part.
     *
     * @param part the part
     * @param index the measure index
     * @return the length in whole notes (4/4 without time signature)
     */
    private Fraction measureLength(PartData part, int index) {
        MeasureData time = timeBefore(part, index + 1);
        if (time == null) {
            return Fraction.of(1, 1);
        }
        return Fraction.of(time.getBeats(), time.getBeatType());
    }


    /**
     * Finds the last measure before an index that sets a time signature.
     *
     * @param part the part
     * @param end the measure index after the last measure to check
     * @return the measure, or null if no time signature is set before
     */
    private MeasureData timeBefore(PartData part, int end) {
        for (int i = Math.min(end, part.getMeasures().size()) - 1; i >= 0; i--) {
            MeasureData measure = part.getMeasures().get(i);
            if (measure.getBeats() != null && measure.getBeatType() != null) {
                return measure;
            }
        }
        return null;
    }


    /**
     * Parses an integer.
     *
     * @param text the text (may be null)
     * @param defaultValue the default for invalid text
     * @return the value
     */
    private int intValue(String text, int defaultValue) {
        if (text == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
