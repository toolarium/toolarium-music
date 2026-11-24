/*
 * MuseScoreWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.barline.IBarline;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.IDirection;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.harmony.IHarmony;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IBackup;
import com.github.toolarium.music.model.measure.IClef;
import com.github.toolarium.music.model.measure.IForward;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.measure.ITranspose;
import com.github.toolarium.music.model.notation.IArticulations;
import com.github.toolarium.music.model.notation.IFermata;
import com.github.toolarium.music.model.notation.INotations;
import com.github.toolarium.music.model.notation.IOrnaments;
import com.github.toolarium.music.model.notation.IPlayingTechnique;
import com.github.toolarium.music.model.notation.ISlur;
import com.github.toolarium.music.model.notation.ITied;
import com.github.toolarium.music.model.notation.ITuplet;
import com.github.toolarium.music.model.note.ILyric;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.note.ITie;
import com.github.toolarium.music.model.part.IScorePart;
import com.github.toolarium.music.model.score.ICreator;
import com.github.toolarium.music.model.score.IScore;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Writes an {@link IScore} as a MuseScore 4 score document (.mscx content).
 *
 * <p>Every staff is written as a sequence of measures with up to four voices. Pitches are stored
 * as concert MIDI pitches with tonal pitch classes (concert and written spelling); ties, slurs,
 * hairpins and voltas are written as spanners with relative locations, as MuseScore 4 does.</p>
 *
 * @author patrick
 */
public final class MuseScoreWriter {
    private static final String VERSION = "4.40";
    private static final String PROGRAM_VERSION = "4.4.0";
    private static final int DIVISION = 480;
    private static final int VOICES_PER_STAFF = 4;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int STEPS_PER_OCTAVE = 7;
    private static final int QUARTERS_PER_WHOLE = 4;
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String SPANNER = "Spanner";
    private static final String TYPE = "type";
    private static final String SUBTYPE = "subtype";
    private static final String TEXT = "text";
    private static final String LOCATION = "location";
    private static final String MEASURES = "measures";
    private static final String FRACTIONS = "fractions";
    private static final String DURATION_TYPE = "durationType";
    private static final String VOICE = "voice";
    private static final String NEXT = "next";
    private static final String PREV = "prev";
    private static final String CHORD = "Chord";
    private static final String[] TUPLET_BASES = {"breve", "whole", "half", "quarter", "eighth", "16th", "32nd", "64th", "128th"};
    private static final Map<String, String> ARTICULATIONS = createArticulations();
    private static final Map<String, String> ORNAMENTS = createOrnaments();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MuseScoreWriter INSTANCE = new MuseScoreWriter();
    }


    /**
     * Constructor.
     */
    private MuseScoreWriter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MuseScoreWriter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Writes a score as MuseScore 4 document.
     *
     * @param score the score
     * @return the content of the .mscx file
     */
    public byte[] write(IScore score) {
        MuseScoreXmlOutput out = new MuseScoreXmlOutput();
        out.raw("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.open("museScore", "version", VERSION);
        out.leaf("programVersion", PROGRAM_VERSION);
        out.open("Score");
        out.leaf("Division", String.valueOf(DIVISION));
        String partName = MuseScorePageTextWriter.getInstance().writeStyle(out, score);
        writeMetaTags(out, score, partName);

        List<PartPlan> plans = new ArrayList<>();
        List<? extends IPart> parts = nonNull(score.getParts());
        for (int p = 0; p < parts.size(); p++) {
            plans.add(new PartPlan(parts.get(p), scorePart(score, parts.get(p))));
        }

        int staffId = 1;
        for (int p = 0; p < plans.size(); p++) {
            writePartDefinition(out, plans.get(p), p + 1, staffId);
            staffId += plans.get(p).getStaves();
        }

        // all staves are planned first: MuseScore requires the same length for a measure in every staff
        List<StaffTimeline> timelines = new ArrayList<>();
        for (PartPlan plan : plans) {
            for (int s = 0; s < plan.getStaves(); s++) {
                timelines.add(planStaff(plan, s));
            }
        }
        alignMeasureLengths(timelines);

        staffId = 1;
        for (StaffTimeline timeline : timelines) {
            out.open("Staff", "id", String.valueOf(staffId));
            if (staffId == 1) {
                MuseScorePageTextWriter.getInstance().writeTitleFrame(out, score);
            }
            writeStaff(out, timeline);
            out.close();
            staffId++;
        }
        out.close();
        out.close();
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }


    /**
     * Writes the meta tags (title, composer, ...).
     *
     * @param out the output
     * @param score the score
     * @param partName the part name (shown in the footer of the pages after the first page), or null
     */
    private void writeMetaTags(MuseScoreXmlOutput out, IScore score, String partName) {
        Map<String, String> tags = new TreeMap<>();
        if (partName != null) {
            tags.put("partName", partName);
        }
        if (score.getWork() != null && score.getWork().getWorkTitle() != null) {
            tags.put("workTitle", score.getWork().getWorkTitle());
        }
        if (score.getWork() != null && score.getWork().getWorkNumber() != null) {
            tags.put("workNumber", score.getWork().getWorkNumber());
        }
        if (score.getMovementTitle() != null) {
            tags.put("movementTitle", score.getMovementTitle());
        }
        if (score.getMovementNumber() != null) {
            tags.put("movementNumber", score.getMovementNumber());
        }
        if (score.getIdentification() != null) {
            for (ICreator creator : nonNull(score.getIdentification().getCreators())) {
                if (creator.getType() != null && creator.getValue() != null) {
                    tags.put(creator.getType(), creator.getValue());
                }
            }
            if (score.getIdentification().getRights() != null) {
                tags.put("copyright", score.getIdentification().getRights());
            }
        }
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            out.openInline("metaTag", "name", tag.getKey());
            out.text(tag.getValue());
            out.closeInline("metaTag");
        }
    }


    /**
     * Writes the definition of a part: staves and instrument.
     *
     * @param out the output
     * @param plan the part plan
     * @param partNumber the number of the part (1-based)
     * @param firstStaffId the ID of the first staff of the part
     */
    private void writePartDefinition(MuseScoreXmlOutput out, PartPlan plan, int partNumber, int firstStaffId) {
        out.open("Part", "id", String.valueOf(partNumber));
        for (int s = 0; s < plan.getStaves(); s++) {
            out.open("Staff", "id", String.valueOf(firstStaffId + s));
            if (plan.isPercussion()) {
                out.open("StaffType", "group", "percussion");
                out.leaf("name", "perc5Line");
            } else {
                out.open("StaffType", "group", "pitched");
                out.leaf("name", "stdNormal");
            }
            out.close();
            String clef = plan.getInitialClef(s);
            if (clef != null && !"G".equals(clef)) {
                out.leaf("defaultClef", clef);
            }
            out.close();
        }
        String name = plan.getName();
        out.leaf("trackName", name);
        String instrument = MuseScoreInstrumentConverter.getInstance().getInstrument(plan.getSound(), plan.getChromatic());
        if (instrument == null) {
            instrument = "instrument" + partNumber;
        }
        out.open("Instrument", "id", instrument);
        if (plan.getSound() != null) {
            out.leaf("instrumentId", plan.getSound());
        }
        out.leaf("longName", name);
        if (plan.getAbbreviation() != null) {
            out.leaf("shortName", plan.getAbbreviation());
        }
        out.leaf("trackName", name);
        if (plan.isPercussion()) {
            out.leaf("useDrumset", "1");
        }
        if (plan.getChromatic() != 0 || plan.getDiatonic() != 0) {
            out.leaf("transposeDiatonic", String.valueOf(plan.getDiatonic()));
            out.leaf("transposeChromatic", String.valueOf(plan.getChromatic()));
        }
        out.open("Channel");
        out.empty("program", "value", String.valueOf(plan.getProgram()));
        out.close();
        out.close();
        out.close();
    }


    /**
     * Plans one staff of a part: the time line with ties, slurs, tuplets, hairpins and voltas.
     *
     * @param plan the part plan
     * @param staffIndex the index of the staff within the part
     * @return the time line
     */
    private StaffTimeline planStaff(PartPlan plan, int staffIndex) {
        StaffTimeline timeline = buildTimeline(plan, staffIndex);
        linkTies(timeline);
        linkSlurs(timeline);
        linkTuplets(timeline);
        linkWedges(timeline);
        linkVoltas(timeline);
        return timeline;
    }


    /**
     * Sets the same length for every measure in all staves: the longest content of the measure.
     *
     * @param timelines the time lines of all staves
     */
    private void alignMeasureLengths(List<StaffTimeline> timelines) {
        int count = 0;
        for (StaffTimeline timeline : timelines) {
            count = Math.max(count, timeline.getMeasures().size());
        }
        for (int m = 0; m < count; m++) {
            Fraction length = Fraction.ZERO;
            Fraction nominal = null;
            for (StaffTimeline timeline : timelines) {
                if (m < timeline.getMeasures().size()) {
                    length = length.max(timeline.getMeasures().get(m).getLength());
                    if (nominal == null) {
                        nominal = timeline.getMeasures().get(m).getNominalLength();
                    }
                }
            }
            for (StaffTimeline timeline : timelines) {
                if (m < timeline.getMeasures().size()) {
                    timeline.getMeasures().get(m).setLength(length);
                    timeline.getMeasures().get(m).setNominalLength(nominal);
                }
            }
        }
    }


    /**
     * Writes the measures of one staff.
     *
     * @param out the output
     * @param timeline the time line of the staff
     */
    private void writeStaff(MuseScoreXmlOutput out, StaffTimeline timeline) {

        int[] previousStretch = null;
        for (int m = 0; m < timeline.getMeasures().size(); m++) {
            MeasureTimeline measure = timeline.getMeasures().get(m);
            int[] stretch = measure.getLocalStretch();
            if (stretch != null && (previousStretch == null || previousStretch[0] != stretch[0] || previousStretch[1] != stretch[1])) {
                measure.setLocalTime(true);
            } else if (stretch == null && previousStretch != null && measure.getTimeBeats() <= 0) {
                // the local time signature ends: the global time signature is shown again
                int[] global = nominalTime(measure.getNominalLength());
                measure.setTime(global[0], global[1]);
            }
            previousStretch = stretch;
            if (measure.getLength().equals(measure.getNominalLength())) {
                out.open("Measure");
            } else {
                out.open("Measure", "len", fraction(measure.getLength()));
            }
            if (measure.isStartRepeat()) {
                out.empty("startRepeat");
            }
            if (measure.getEndRepeat() > 0) {
                out.leaf("endRepeat", String.valueOf(measure.getEndRepeat()));
            }
            for (String marker : measure.getMarkers()) {
                out.open("Marker");
                out.leaf("label", marker);
                out.close();
            }
            int voices = Math.max(1, measure.getVoices().size());
            for (int v = 0; v < voices; v++) {
                out.open(VOICE);
                if (v == 0) {
                    for (MuseScoreElement element : measure.getStartItemsElements()) {
                        element.write(out);
                    }
                    writeMeasureStart(out, measure, m == 0);
                }
                List<WriterEvent> events = Collections.emptyList();
                if (v < measure.getVoices().size()) {
                    events = measure.getVoices().get(v);
                }
                writeVoice(out, measure, events, v == 0);
                if (v == 0) {
                    for (MuseScoreElement element : measure.getEndElements()) {
                        element.write(out);
                    }
                    if (measure.getBarStyle() != null) {
                        out.open("BarLine");
                        out.leaf(SUBTYPE, measure.getBarStyle());
                        out.close();
                    } else if (m == timeline.getMeasures().size() - 1 && measure.getEndRepeat() <= 0) {
                        // a normal barline: MuseScore would end the score with a final barline otherwise
                        out.open("BarLine");
                        out.close();
                    }
                }
                out.close();
            }
            out.close();
        }
    }


    /**
     * Writes the clef, key and time signature changes at the start of a measure.
     *
     * @param out the output
     * @param measure the measure
     * @param first true for the first measure: its clef is the initial clef of the staff, not a clef change
     */
    private void writeMeasureStart(MuseScoreXmlOutput out, MeasureTimeline measure, boolean first) {
        if (measure.getClef() != null) {
            out.open("Clef");
            out.leaf("concertClefType", measure.getClef());
            out.leaf("transposingClefType", measure.getClef());
            if (first) {
                out.leaf("isHeader", "1");
            }
            out.close();
        }
        if (measure.getWrittenKey() != null) {
            out.open("KeySig");
            out.leaf("concertKey", String.valueOf(measure.getConcertKey()));
            if (measure.getConcertKey() != measure.getWrittenKey()) {
                out.leaf("actualKey", String.valueOf(measure.getWrittenKey()));
            }
            out.close();
        }
        if (measure.isLocalTime()) {
            // local time signature: the nominal values are stretched to the measure length
            int[] stretch = measure.getLocalStretch();
            int[] nominal = nominalTime(measure.getLength().multiply(stretch[0], stretch[1]));
            out.open("TimeSig");
            out.leaf("sigN", String.valueOf(nominal[0]));
            out.leaf("sigD", String.valueOf(nominal[1]));
            out.leaf("stretchN", String.valueOf(stretch[0]));
            out.leaf("stretchD", String.valueOf(stretch[1]));
            out.close();
        } else if (measure.getTimeBeats() > 0) {
            out.open("TimeSig");
            out.leaf("sigN", String.valueOf(measure.getTimeBeats()));
            out.leaf("sigD", String.valueOf(measure.getTimeBeatType()));
            out.close();
        }
        for (MuseScoreElement element : measure.getStartElements()) {
            element.write(out);
        }
    }


    /**
     * Writes the events of a voice in one measure.
     *
     * @param out the output
     * @param measure the measure
     * @param events the events in time order
     * @param primary true for the first voice
     */
    private void writeVoice(MuseScoreXmlOutput out, MeasureTimeline measure, List<WriterEvent> events, boolean primary) {
        if (events.isEmpty() && primary) {
            out.open("Rest");
            out.leaf(DURATION_TYPE, "measure");
            out.leaf("duration", fraction(measure.getLength()));
            out.close();
            return;
        }
        Fraction cursor = Fraction.ZERO;
        for (WriterEvent event : events) {
            if (!event.isGrace() && event.getPosition().compareTo(cursor) > 0) {
                out.open(LOCATION);
                out.leaf(FRACTIONS, fraction(event.getPosition().subtract(cursor)));
                out.close();
                cursor = event.getPosition();
            }
            for (MuseScoreElement element : event.getBefore()) {
                element.write(out);
            }
            if (event.getTupletStart() != null) {
                writeTupletStart(out, event.getTupletStart());
            }
            writeEvent(out, event);
            if (event.isTupletEnd()) {
                out.empty("endTuplet");
            }
            if (!event.isGrace()) {
                cursor = cursor.add(event.getLength());
            }
        }
    }


    /**
     * Writes the start of a tuplet.
     *
     * @param out the output
     * @param tuplet the tuplet
     */
    private void writeTupletStart(MuseScoreXmlOutput out, TupletPlan tuplet) {
        out.open("Tuplet");
        out.leaf("normalNotes", String.valueOf(tuplet.getNormal()));
        out.leaf("actualNotes", String.valueOf(tuplet.getActual()));
        out.leaf("baseNote", tuplet.getBaseNote());
        out.open("Number");
        out.leaf("style", "tuplet");
        out.leaf(TEXT, String.valueOf(tuplet.getActual()));
        out.close();
        out.close();
    }


    /**
     * Writes a chord, rest or grace chord.
     *
     * @param out the output
     * @param event the event
     */
    private void writeEvent(MuseScoreXmlOutput out, WriterEvent event) {
        INote first = event.getNotes().get(0);
        if (event.isRest()) {
            out.open("Rest");
            if ("no".equals(first.getPrintObject())) {
                out.leaf("visible", "0");
            }
            if (event.isMeasureRest()) {
                out.leaf(DURATION_TYPE, "measure");
                out.leaf("duration", fraction(event.getLength()));
            } else {
                writeDuration(out, event);
            }
            out.close();
            return;
        }

        out.open(CHORD);
        writeDuration(out, event);
        if (event.getStaffMove() != 0) {
            out.leaf("staffMove", String.valueOf(event.getStaffMove()));
        }
        if (event.isGrace()) {
            if ("yes".equals(first.getGrace().getSlash())) {
                out.empty("acciaccatura");
            } else {
                out.empty("appoggiatura");
            }
        }
        for (ILyric lyric : nonNull(first.getLyrics())) {
            writeLyric(out, lyric);
        }
        writeArticulations(out, first.getNotations());
        for (MuseScoreElement element : event.getChordElements()) {
            element.write(out);
        }
        for (int n = 0; n < event.getNotes().size(); n++) {
            writeNote(out, event, n);
        }
        out.close();
    }


    /**
     * Writes the duration type and dots of an event.
     *
     * @param out the output
     * @param event the event
     */
    private void writeDuration(MuseScoreXmlOutput out, WriterEvent event) {
        if (event.getDots() > 0) {
            out.leaf("dots", String.valueOf(event.getDots()));
        }
        out.leaf(DURATION_TYPE, event.getDurationType());
    }


    /**
     * Writes a note of a chord.
     *
     * @param out the output
     * @param event the event
     * @param index the index of the note in the chord
     */
    private void writeNote(MuseScoreXmlOutput out, WriterEvent event, int index) {
        INote note = event.getNotes().get(index);
        MuseScorePitchConverter pitchConverter = MuseScorePitchConverter.getInstance();
        String step;
        int alter = 0;
        int octave;
        if (note.getPitch() != null) {
            step = note.getPitch().getStep();
            if (note.getPitch().getAlter() != null) {
                alter = (int) Math.round(note.getPitch().getAlter());
            }
            octave = intValue(note.getPitch().getOctave(), 4);
        } else if (note.getUnpitched() != null && note.getUnpitched().getDisplayStep() != null) {
            step = note.getUnpitched().getDisplayStep();
            octave = intValue(note.getUnpitched().getDisplayOctave(), 4);
        } else {
            step = "C";
            octave = 4;
        }

        int written = pitchConverter.toMidi(step, alter, octave);
        final int concert = written + event.getChromatic();
        int writtenTpc = pitchConverter.stepAlterToTpc(step, alter);
        int concertTpc = writtenTpc;
        if (event.getChromatic() != 0 || event.getDiatonic() != 0) {
            concertTpc = pitchConverter.transposeTpc(writtenTpc, event.getDiatonic(), event.getChromatic());
        }

        out.open("Note");
        if (note.getAccidental() != null) {
            String accidental = MuseScoreSymbolConverter.getInstance().accidentalFromXml(note.getAccidental());
            if (accidental != null) {
                out.open("Accidental");
                out.leaf(SUBTYPE, accidental);
                out.close();
            }
        }
        writeFingering(out, note);
        Location tieNext = event.getTieNext(index);
        if (tieNext != null) {
            out.open(SPANNER, TYPE, "Tie");
            out.empty("Tie");
            writeLocation(out, NEXT, tieNext);
            out.close();
        }
        Location tiePrev = event.getTiePrev(index);
        if (tiePrev != null) {
            out.open(SPANNER, TYPE, "Tie");
            writeLocation(out, PREV, tiePrev);
            out.close();
        }
        out.leaf("pitch", String.valueOf(Math.max(0, Math.min(127, concert))));
        out.leaf("tpc", String.valueOf(concertTpc));
        if (concertTpc != writtenTpc) {
            out.leaf("tpc2", String.valueOf(writtenTpc));
        }
        if (note.getNotehead() != null) {
            String head = MuseScoreSymbolConverter.getInstance().noteheadFromXml(note.getNotehead());
            if (head != null) {
                out.leaf("head", head);
            }
        }
        if ("cue".equals(note.getTypeSize()) || "grace-cue".equals(note.getTypeSize())) {
            out.leaf("small", "1");
        }
        out.close();
    }


    /**
     * Writes the fingering of a note, one element per number.
     *
     * @param out the output
     * @param note the note
     */
    private void writeFingering(MuseScoreXmlOutput out, INote note) {
        if (note.getNotations() == null || note.getNotations().getTechnical() == null || note.getNotations().getTechnical().getFingering() == null) {
            return;
        }
        for (String finger : note.getNotations().getTechnical().getFingering().trim().split("\\s+")) {
            if (!finger.isEmpty()) {
                out.open("Fingering");
                out.leaf(TEXT, finger);
                out.close();
            }
        }
    }


    /**
     * Writes a lyric.
     *
     * @param out the output
     * @param lyric the lyric
     */
    private void writeLyric(MuseScoreXmlOutput out, ILyric lyric) {
        if (lyric.getText() == null) {
            return;
        }
        out.open("Lyrics");
        int number = 1;
        try {
            number = Integer.parseInt(String.valueOf(lyric.getNumber()).trim());
        } catch (NumberFormatException e) {
            number = 1;
        }
        if (number > 1) {
            out.leaf("no", String.valueOf(number - 1));
        }
        if (lyric.getSyllabic() != null && !"single".equals(lyric.getSyllabic())) {
            out.leaf("syllabic", lyric.getSyllabic());
        }
        out.leaf(TEXT, lyric.getText());
        out.close();
    }


    /**
     * Writes the articulations, technical marks and ornaments of a chord.
     *
     * @param out the output
     * @param notations the notations of the first note (may be null)
     */
    private void writeArticulations(MuseScoreXmlOutput out, INotations notations) {
        if (notations == null) {
            return;
        }
        List<String> marks = new ArrayList<>();
        IArticulations articulations = notations.getArticulations();
        if (articulations != null) {
            addIf(marks, articulations.getStaccato(), "staccato");
            addIf(marks, articulations.getAccent(), "accent");
            addIf(marks, articulations.getTenuto(), "tenuto");
            addIf(marks, articulations.getStrongAccent(), "strong-accent");
            addIf(marks, articulations.getStaccatissimo(), "staccatissimo");
            addIf(marks, articulations.getSpiccato(), "spiccato");
            addIf(marks, articulations.getDetachedLegato(), "detached-legato");
            addIf(marks, articulations.getStress(), "stress");
            addIf(marks, articulations.getUnstress(), "unstress");
        }
        IPlayingTechnique technical = notations.getTechnical();
        if (technical != null) {
            addIf(marks, technical.getUpBow(), "up-bow");
            addIf(marks, technical.getDownBow(), "down-bow");
            addIf(marks, technical.getHarmonic(), "harmonic");
            addIf(marks, technical.getOpenString(), "open-string");
            addIf(marks, technical.getStopped(), "stopped");
        }
        for (String mark : marks) {
            String subtype = ARTICULATIONS.get(mark);
            if (subtype != null) {
                out.open("Articulation");
                out.leaf(SUBTYPE, subtype);
                out.close();
            }
        }
        IOrnaments ornaments = notations.getOrnaments();
        if (ornaments != null) {
            List<String> names = new ArrayList<>();
            addIf(names, ornaments.getTrillMark(), "trill-mark");
            addIf(names, ornaments.getTurn(), "turn");
            addIf(names, ornaments.getInvertedTurn(), "inverted-turn");
            addIf(names, ornaments.getMordent(), "mordent");
            addIf(names, ornaments.getInvertedMordent(), "inverted-mordent");
            addIf(names, ornaments.getShake(), "shake");
            for (String name : names) {
                out.open("Ornament");
                out.leaf(SUBTYPE, ORNAMENTS.get(name));
                out.close();
            }
        }
    }


    /**
     * Adds a name if a mark is set.
     *
     * @param names the names
     * @param value the mark value (null if not set)
     * @param name the name
     */
    private void addIf(List<String> names, String value, String name) {
        if (value != null) {
            names.add(name);
        }
    }


    /**
     * Writes a spanner location.
     *
     * @param out the output
     * @param tag next or prev
     * @param location the location
     */
    private void writeLocation(MuseScoreXmlOutput out, String tag, Location location) {
        out.open(tag);
        out.open(LOCATION);
        if (location.getVoices() != 0) {
            out.leaf("voices", String.valueOf(location.getVoices()));
        }
        if (location.getMeasures() != 0) {
            out.leaf(MEASURES, String.valueOf(location.getMeasures()));
        }
        if (!location.getFractions().equals(Fraction.ZERO)) {
            out.leaf(FRACTIONS, fraction(location.getFractions()));
        }
        out.close();
        out.close();
    }


    /**
     * Builds the time line of one staff: the events of every voice with their positions and the
     * measure attributes.
     *
     * @param plan the part plan
     * @param staffIndex the index of the staff within the part
     * @return the time line
     */
    private StaffTimeline buildTimeline(PartPlan plan, int staffIndex) {
        StaffTimeline timeline = new StaffTimeline();
        int divisions = 1;
        int beats = 4;
        int beatType = 4;
        String currentClef = null;
        Integer currentKey = null;
        List<Map.Entry<IDirection, Fraction>> carried = new ArrayList<>();
        List<? extends IMeasure> measures = nonNull(plan.getPart().getMeasures());
        for (int m = 0; m < measures.size(); m++) {
            IMeasure measure = measures.get(m);
            MeasureTimeline data = new MeasureTimeline();
            timeline.getMeasures().add(data);

            for (IAttributes attributes : nonNull(measure.getAttributes())) {
                if (attributes.getDivisions() != null && attributes.getDivisions() > 0) {
                    divisions = attributes.getDivisions();
                }
                if (!nonNull(attributes.getTimes()).isEmpty()) {
                    int[] time = parseTime(attributes.getTimes().get(0).getBeats(), attributes.getTimes().get(0).getBeatType());
                    if (time != null) {
                        beats = time[0];
                        beatType = time[1];
                        data.setTime(beats, beatType);
                    }
                }
                if (!nonNull(attributes.getKeys()).isEmpty() && attributes.getKeys().get(0).getFifths() != null) {
                    int written = attributes.getKeys().get(0).getFifths();
                    if (currentKey == null || currentKey != written) {
                        currentKey = written;
                        int concert = MuseScorePitchConverter.getInstance().transposeKey(written, plan.getDiatonic(), plan.getChromatic());
                        data.setKey(concert, written);
                    }
                }
                for (IClef clef : nonNull(attributes.getClefs())) {
                    if (clefStaff(clef) == staffIndex) {
                        String type = MuseScoreSymbolConverter.getInstance().clefFromXml(clef.getSign(), clef.getLine(), clef.getClefOctaveChange());
                        if (type != null && !type.equals(currentClef)) {
                            currentClef = type;
                            data.setClef(type);
                        }
                    }
                }
            }
            data.setNominalLength(Fraction.of(beats, beatType));

            // group the notes by voice; a voice is written on its home staff (cross-staff notes move)
            TreeMap<Integer, List<INote>> allVoices = new TreeMap<>();
            for (INote note : nonNull(measure.getNotes())) {
                int voice = 1;
                if (note.getVoice() != null) {
                    voice = note.getVoice();
                }
                allVoices.computeIfAbsent(voice, k -> new ArrayList<>()).add(note);
            }
            TreeMap<Integer, List<INote>> voices = new TreeMap<>();
            for (Map.Entry<Integer, List<INote>> entry : allVoices.entrySet()) {
                if (plan.getHomeStaff(entry.getKey()) == staffIndex) {
                    voices.put(entry.getKey(), entry.getValue());
                }
            }
            Fraction length = Fraction.ZERO;
            for (List<INote> notes : voices.values()) {
                if (data.getVoices().size() >= VOICES_PER_STAFF) {
                    break;
                }
                List<WriterEvent> events = buildEvents(notes, divisions, plan, m, data.getVoices().size(), staffIndex);
                data.getVoices().add(events);
                Fraction end = Fraction.ZERO;
                for (WriterEvent event : events) {
                    end = end.max(event.getPosition().add(event.getLength()));
                }
                length = length.max(end);
            }
            if (!length.isPositive()) {
                length = data.getNominalLength();
            }
            data.setLength(length);
            data.setLocalStretch(detectLocalStretch(data));
            collectMeasureMarks(data, measure, staffIndex, divisions, carried);
        }
        if (!timeline.getMeasures().isEmpty()) {
            // directions at the end of the last measure stay there
            MeasureTimeline last = timeline.getMeasures().get(timeline.getMeasures().size() - 1);
            for (Map.Entry<IDirection, Fraction> entry : carried) {
                addDirection(last, entry.getKey(), last.getLength());
            }
        }
        return timeline;
    }


    /**
     * Builds the events (chords, rests, grace chords) of one voice in one measure.
     *
     * @param notes the notes of the voice in order
     * @param divisions the divisions per quarter note
     * @param plan the part plan
     * @param measureIndex the index of the measure
     * @param voice the voice index
     * @param staffIndex the index of the staff the voice is written on
     * @return the events
     */
    private List<WriterEvent> buildEvents(List<INote> notes, int divisions, PartPlan plan, int measureIndex, int voice, int staffIndex) {
        List<WriterEvent> events = new ArrayList<>();
        Fraction position = Fraction.ZERO;
        WriterEvent current = null;
        for (INote note : notes) {
            if (note.getChord() != null && current != null) {
                current.getNotes().add(note);
                continue;
            }
            boolean grace = note.getGrace() != null;
            Fraction length = Fraction.ZERO;
            if (!grace) {
                length = Fraction.of(intValue(note.getDuration(), 0), (long) divisions * QUARTERS_PER_WHOLE);
            }
            current = new WriterEvent(measureIndex, voice, position, length, grace, note.getRest() != null);
            current.getNotes().add(note);
            current.setTransposition(plan.getChromatic(), plan.getDiatonic());
            current.setStaffMove(noteStaff(note) - staffIndex);
            String type = note.getType();
            int dots = nonNull(note.getDots()).size();
            if (note.getRest() != null && "yes".equals(note.getRest().getMeasure())) {
                current.setMeasureRest(true);
            }
            if (type == null) {
                MuseScoreDurationConverter.TypeAndDots typeAndDots = MuseScoreDurationConverter.getInstance().fromFraction(length);
                if (typeAndDots != null) {
                    type = typeAndDots.getType();
                    dots = typeAndDots.getDots();
                } else {
                    type = "quarter";
                }
            }
            current.setDurationType(MuseScoreDurationConverter.getInstance().normalizeType(type), dots);
            events.add(current);
            if (!grace) {
                position = position.add(length);
            }
        }
        return events;
    }


    /**
     * Detects a local time signature: all notes and rests of the staff in the measure share one time
     * modification and none of them belongs to a notated tuplet.
     *
     * @param data the measure time line
     * @return {actual, normal}, or null without a local time signature
     */
    private int[] detectLocalStretch(MeasureTimeline data) {
        String ratio = null;
        int[] result = null;
        for (List<WriterEvent> voice : data.getVoices()) {
            for (WriterEvent event : voice) {
                if (event.isGrace()) {
                    continue;
                }
                INote note = event.getNotes().get(0);
                String noteRatio = tupletRatio(note);
                if (noteRatio == null || hasTuplet(note, START) || hasTuplet(note, STOP) || ratio != null && !ratio.equals(noteRatio)) {
                    return null;
                }
                ratio = noteRatio;
                result = new int[] {note.getTimeModification().getActualNotes(), note.getTimeModification().getNormalNotes()};
            }
        }
        return result;
    }


    /**
     * Collects the directions, harmonies and barlines of a measure for one staff.
     *
     * @param data the measure time line
     * @param measure the measure
     * @param staffIndex the index of the staff within the part
     * @param divisions the divisions per quarter note
     * @param carried the directions at the end of the previous measure, placed at the start of this measure;
     *     replaced by the directions at the end of this measure
     */
    private void collectMeasureMarks(MeasureTimeline data, IMeasure measure, int staffIndex, int divisions, List<Map.Entry<IDirection, Fraction>> carried) {
        List<Map.Entry<IDirection, Fraction>> directions = new ArrayList<>(carried);
        carried.clear();
        Map<IDirection, Fraction> cursors = directionCursors(measure, divisions);
        for (IDirection direction : nonNull(measure.getDirections())) {
            int staff = 0;
            if (direction.getStaff() != null) {
                staff = direction.getStaff() - 1;
            }
            if (staff != staffIndex) {
                continue;
            }
            Fraction position = Fraction.of(intValue(direction.getOffset(), 0), (long) divisions * QUARTERS_PER_WHOLE);
            if (cursors.containsKey(direction)) {
                position = position.add(cursors.get(direction));
            }
            if (position.compareTo(data.getLength()) >= 0) {
                // a direction on the barline belongs to the next measure, as MuseScore places it
                carried.add(new AbstractMap.SimpleEntry<>(direction, position.subtract(data.getLength())));
            } else {
                directions.add(new AbstractMap.SimpleEntry<>(direction, position.max(Fraction.ZERO)));
            }
        }
        for (Map.Entry<IDirection, Fraction> entry : directions) {
            addDirection(data, entry.getKey(), entry.getValue());
        }
        if (staffIndex == 0) {
            for (IHarmony harmony : nonNull(measure.getHarmonies())) {
                MuseScoreElement element = harmonyElement(harmony);
                if (element != null) {
                    data.getStartElements().add(element);
                }
            }
        }
        for (IBarline barline : nonNull(measure.getBarlines())) {
            if (barline.getRepeat() != null && "forward".equals(barline.getRepeat().getDirection())) {
                data.setStartRepeat(true);
            }
            if (barline.getRepeat() != null && "backward".equals(barline.getRepeat().getDirection())) {
                int times = intValue(barline.getRepeat().getTimes(), 2);
                data.setEndRepeat(Math.max(2, times));
            }
            if (barline.getEnding() != null) {
                data.getEndings().add(barline.getEnding().getType() + "|" + barline.getEnding().getNumber() + "|" + barline.getEnding().getValue());
            }
            if ("right".equals(barline.getLocation()) && barline.getRepeat() == null && barline.getBarStyle() != null) {
                String style = MuseScoreSymbolConverter.getInstance().barlineFromXml(barline.getBarStyle());
                if (style != null && !"normal".equals(style)) {
                    data.setBarStyle(style);
                }
            }
        }
        for (INote note : nonNull(measure.getNotes())) {
            if (noteStaff(note) != staffIndex || note.getNotations() == null) {
                continue;
            }
            if (!nonNull(note.getNotations().getFermatas()).isEmpty()) {
                data.getFermatas().add(note);
            }
        }
    }


    /**
     * Adds a direction to a measure: its tempo, dynamics, texts, rehearsal mark, markers and hairpins.
     *
     * @param data the measure time line
     * @param direction the direction
     * @param position the position in the measure in whole notes
     */
    private void addDirection(MeasureTimeline data, IDirection direction, Fraction position) {
        MuseScoreDirectionWriter directions = MuseScoreDirectionWriter.getInstance();
        boolean tempo = directions.isTempo(direction);
        if (tempo) {
            data.addAt(position, directions.getTempoElement(direction));
        }
        for (IDirectionType type : nonNull(direction.getDirectionTypes())) {
            MuseScoreElement element = null;
            if (!tempo || type.getDynamics() != null || type.getRehearsal() != null) {
                element = directions.getDirectionElement(type);
            }
            if (element != null) {
                data.addAt(position, element);
            }
            if (type.getSegno() != null) {
                data.getMarkers().add("segno");
            }
            if (type.getCoda() != null) {
                data.getMarkers().add("coda");
            }
            if (type.getWedge() != null) {
                data.getWedges().add(new WedgeMark(position, type.getWedge().getType(), intValue(type.getWedge().getNumber(), 1)));
            }
        }
    }


    /**
     * Gets the position of every direction of a measure from the order of the measure content: the notes, forwards
     * and backups before a direction move the position.
     *
     * @param measure the measure
     * @param divisions the divisions per quarter note
     * @return the position of each direction in whole notes
     */
    private Map<IDirection, Fraction> directionCursors(IMeasure measure, int divisions) {
        Map<IDirection, Fraction> result = new IdentityHashMap<>();
        long cursor = 0;
        for (IMusicDataElement element : nonNull(measure.getMusicData())) {
            if (element instanceof INote) {
                INote note = (INote) element;
                if (note.getChord() == null && note.getGrace() == null) {
                    cursor += intValue(note.getDuration(), 0);
                }
            } else if (element instanceof IBackup) {
                cursor = Math.max(0, cursor - intValue(((IBackup) element).getDuration(), 0));
            } else if (element instanceof IForward) {
                cursor += intValue(((IForward) element).getDuration(), 0);
            } else if (element instanceof IDirection) {
                result.put((IDirection) element, Fraction.of(cursor, (long) divisions * QUARTERS_PER_WHOLE));
            }
        }
        return result;
    }


    /**
     * Creates a chord symbol element.
     *
     * @param harmony the harmony
     * @return the element, or null if the harmony has no root
     */
    private MuseScoreElement harmonyElement(IHarmony harmony) {
        if (harmony.getRoot() == null || harmony.getRoot().getRootStep() == null) {
            return null;
        }
        MuseScorePitchConverter pitch = MuseScorePitchConverter.getInstance();
        int root = pitch.stepAlterToTpc(harmony.getRoot().getRootStep(), alter(harmony.getRoot().getRootAlter()));
        String name = MuseScoreSymbolConverter.getInstance().harmonyFromKind(harmony.getKind());
        Integer bass = null;
        if (harmony.getBass() != null && harmony.getBass().getBassStep() != null) {
            bass = pitch.stepAlterToTpc(harmony.getBass().getBassStep(), alter(harmony.getBass().getBassAlter()));
        }
        Integer bassTpc = bass;
        return out -> {
            out.open("Harmony");
            out.leaf("root", String.valueOf(root));
            if (name != null && !name.isEmpty()) {
                out.leaf("name", name);
            }
            if (bassTpc != null) {
                out.leaf("base", String.valueOf(bassTpc));
            }
            out.close();
        };
    }


    /**
     * Links ties: a tied note is linked to the note with the same pitch that starts where it ends,
     * preferably in the same voice, otherwise in another voice of the staff.
     *
     * @param timeline the staff time line
     */
    private void linkTies(StaffTimeline timeline) {
        List<WriterEvent> all = new ArrayList<>();
        for (int voice = 0; voice < VOICES_PER_STAFF; voice++) {
            all.addAll(timeline.voiceEvents(voice));
        }
        for (WriterEvent event : all) {
            if (event.isRest() || event.isGrace()) {
                continue;
            }
            for (int n = 0; n < event.getNotes().size(); n++) {
                if (!isTieStart(event.getNotes().get(n))) {
                    continue;
                }
                int midi = writtenMidi(event.getNotes().get(n));
                int endMeasure = event.getMeasureIndex();
                Fraction end = event.getPosition().add(event.getLength());
                WriterEvent target = null;
                int targetNote = -1;
                for (WriterEvent candidate : all) {
                    if (candidate.isRest() || candidate.isGrace() || candidate == event || !reaches(timeline, endMeasure, end, candidate)) {
                        continue;
                    }
                    for (int k = 0; k < candidate.getNotes().size(); k++) {
                        if (writtenMidi(candidate.getNotes().get(k)) == midi
                                && (target == null || candidate.getVoice() == event.getVoice() && target.getVoice() != event.getVoice())) {
                            target = candidate;
                            targetNote = k;
                        }
                    }
                }
                if (target != null) {
                    event.setTieNext(n, location(event, target));
                    target.setTiePrev(targetNote, location(target, event));
                }
            }
        }
    }


    /**
     * Checks whether an event starts at the end of a tied note (the end may be the start of the next measure).
     *
     * @param timeline the staff time line
     * @param measure the measure of the tied note
     * @param end the end of the tied note in its measure
     * @param candidate the candidate event
     * @return true if the candidate starts at the end
     */
    private boolean reaches(StaffTimeline timeline, int measure, Fraction end, WriterEvent candidate) {
        if (candidate.getMeasureIndex() == measure) {
            return candidate.getPosition().equals(end);
        }
        return candidate.getMeasureIndex() == measure + 1 && candidate.getPosition().equals(Fraction.ZERO)
                && end.compareTo(timeline.getMeasures().get(measure).getLength()) >= 0;
    }


    /**
     * Links slurs by their number within a voice.
     *
     * @param timeline the staff time line
     */
    private void linkSlurs(StaffTimeline timeline) {
        for (int voice = 0; voice < VOICES_PER_STAFF; voice++) {
            Map<Integer, WriterEvent> open = new HashMap<>();
            for (WriterEvent event : timeline.voiceEvents(voice)) {
                if (event.isRest() || event.getNotes().get(0).getNotations() == null) {
                    continue;
                }
                List<? extends ISlur> slurs = nonNull(event.getNotes().get(0).getNotations().getSlurs());
                for (ISlur slur : slurs) {
                    if (STOP.equals(slur.getType())) {
                        WriterEvent start = open.remove(intValue(slur.getNumber(), 1));
                        if (start != null && start != event) {
                            Location next = location(start, event);
                            Location prev = location(event, start);
                            start.getChordElements().add(out -> {
                                out.open(SPANNER, TYPE, "Slur");
                                out.empty("Slur");
                                writeLocation(out, NEXT, next);
                                out.close();
                            });
                            event.getChordElements().add(out -> {
                                out.open(SPANNER, TYPE, "Slur");
                                writeLocation(out, PREV, prev);
                                out.close();
                            });
                        }
                    }
                }
                for (ISlur slur : slurs) {
                    if (START.equals(slur.getType())) {
                        open.put(intValue(slur.getNumber(), 1), event);
                    }
                }
            }
        }
    }


    /**
     * Groups the events of tuplets. Consecutive notes with the same time modification form a run
     * (a tuplet start notation starts a new run); every run is split into complete tuplets whose
     * actual notes of the base value are filled exactly by the written values.
     *
     * @param timeline the staff time line
     */
    private void linkTuplets(StaffTimeline timeline) {
        for (MeasureTimeline measure : timeline.getMeasures()) {
            if (measure.getLocalStretch() != null) {
                continue;
            }
            for (List<WriterEvent> events : measure.getVoices()) {
                List<WriterEvent> run = new ArrayList<>();
                String runRatio = null;
                for (WriterEvent event : events) {
                    if (event.isGrace()) {
                        continue;
                    }
                    String ratio = tupletRatio(event.getNotes().get(0));
                    boolean startNotation = hasTuplet(event.getNotes().get(0), START);
                    if (!run.isEmpty() && (ratio == null || !ratio.equals(runRatio) || startNotation)) {
                        splitTuplets(run);
                        run.clear();
                    }
                    if (ratio != null) {
                        run.add(event);
                        runRatio = ratio;
                        if (hasTuplet(event.getNotes().get(0), STOP)) {
                            splitTuplets(run);
                            run.clear();
                        }
                    }
                }
                if (!run.isEmpty()) {
                    splitTuplets(run);
                }
            }
        }
    }


    /**
     * Gets the tuplet ratio of a note.
     *
     * @param note the note
     * @return the ratio (actual:normal), or null without a time modification
     */
    private String tupletRatio(INote note) {
        if (note.getTimeModification() == null || note.getTimeModification().getActualNotes() == null
                || note.getTimeModification().getNormalNotes() == null) {
            return null;
        }
        return note.getTimeModification().getActualNotes() + ":" + note.getTimeModification().getNormalNotes();
    }


    /**
     * Splits a run of notes with the same time modification into complete tuplets.
     *
     * @param run the events of the run
     */
    private void splitTuplets(List<WriterEvent> run) {
        MuseScoreDurationConverter durations = MuseScoreDurationConverter.getInstance();
        int index = 0;
        while (index < run.size()) {
            INote note = run.get(index).getNotes().get(0);
            int actual = note.getTimeModification().getActualNotes();
            final int normal = note.getTimeModification().getNormalNotes();

            // the prefix sums of the written values from this event on
            List<Fraction> sums = new ArrayList<>();
            Fraction sum = Fraction.ZERO;
            for (int i = index; i < run.size(); i++) {
                Fraction written = durations.toFraction(run.get(i).getDurationType(), run.get(i).getDots());
                if (written == null) {
                    written = Fraction.ZERO;
                }
                sum = sum.add(written);
                sums.add(sum);
            }

            String base = null;
            int end = run.size() - 1;
            String normalType = note.getTimeModification().getNormalType();
            for (String candidate : TUPLET_BASES) {
                if (normalType != null && !candidate.equals(durations.normalizeType(normalType))) {
                    continue;
                }
                Fraction capacity = durations.toFraction(candidate, 0).multiply(actual, 1);
                int match = sums.indexOf(capacity);
                if (match >= 0) {
                    base = candidate;
                    end = index + match;
                    break;
                }
            }
            if (base == null) {
                // no complete tuplet: one tuplet with the value of the first note
                base = run.get(index).getDurationType();
                if (normalType != null) {
                    base = durations.normalizeType(normalType);
                }
            }
            run.get(index).setTupletStart(new TupletPlan(actual, normal, base));
            run.get(end).setTupletEnd(true);
            index = end + 1;
        }
    }


    /**
     * Checks whether a note has a tuplet notation of a type.
     *
     * @param note the note
     * @param type start or stop
     * @return true if the notation is present
     */
    private boolean hasTuplet(INote note, String type) {
        if (note.getNotations() == null) {
            return false;
        }
        for (ITuplet tuplet : nonNull(note.getNotations().getTuplets())) {
            if (type.equals(tuplet.getType())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Links the hairpins (wedges) of a staff and places the fermatas.
     *
     * @param timeline the staff time line
     */
    private void linkWedges(StaffTimeline timeline) {
        Map<Integer, Object[]> open = new HashMap<>();
        for (int m = 0; m < timeline.getMeasures().size(); m++) {
            MeasureTimeline measure = timeline.getMeasures().get(m);
            for (WedgeMark wedge : measure.getWedges()) {
                if (STOP.equals(wedge.getType()) || "continue".equals(wedge.getType())) {
                    Object[] start = open.remove(wedge.getNumber());
                    if (start != null) {
                        int startMeasure = (Integer) start[0];
                        Fraction startPosition = (Fraction) start[1];
                        String subtype = (String) start[2];
                        Location next = new Location(m - startMeasure, wedge.getPosition().subtract(startPosition));
                        Location prev = new Location(startMeasure - m, startPosition.subtract(wedge.getPosition()));
                        timeline.getMeasures().get(startMeasure).addAt(startPosition, out -> {
                            out.open(SPANNER, TYPE, "HairPin");
                            out.open("HairPin");
                            out.leaf(SUBTYPE, subtype);
                            out.close();
                            writeLocation(out, NEXT, next);
                            out.close();
                        });
                        measure.addAt(wedge.getPosition(), out -> {
                            out.open(SPANNER, TYPE, "HairPin");
                            writeLocation(out, PREV, prev);
                            out.close();
                        });
                    }
                } else {
                    String subtype = "0";
                    if ("diminuendo".equals(wedge.getType())) {
                        subtype = "1";
                    }
                    open.put(wedge.getNumber(), new Object[] {m, wedge.getPosition(), subtype});
                }
            }
            for (INote note : measure.getFermatas()) {
                WriterEvent event = measure.findEvent(note);
                if (event == null) {
                    continue;
                }
                IFermata fermata = note.getNotations().getFermatas().get(0);
                String subtype = MuseScoreSymbolConverter.getInstance().fermataFromXml(fermata.getType(), fermata.getValue());
                if (subtype == null) {
                    subtype = "fermataAbove";
                }
                String fermataSubtype = subtype;
                event.getBefore().add(out -> {
                    out.open("Fermata");
                    out.leaf(SUBTYPE, fermataSubtype);
                    out.close();
                });
            }
        }
        // the start of a hairpin is added to its measure when the stop is found in a later measure
        for (MeasureTimeline measure : timeline.getMeasures()) {
            measure.distribute();
        }
    }


    /**
     * Links the voltas (repeat endings) of a part: an ending starts at a left barline with an
     * ending start and ends at the right barline with an ending stop or discontinue.
     *
     * @param timeline the staff time line
     */
    private void linkVoltas(StaffTimeline timeline) {
        Integer startMeasure = null;
        String number = null;
        String text = null;
        for (int m = 0; m < timeline.getMeasures().size(); m++) {
            MeasureTimeline measure = timeline.getMeasures().get(m);
            for (String ending : measure.getEndings()) {
                String[] parts = ending.split("\\|", -1);
                if (START.equals(parts[0])) {
                    startMeasure = m;
                    number = parts[1];
                    text = parts[2];
                    if ("null".equals(text)) {
                        text = number + ".";
                    }
                } else if (startMeasure != null) {
                    int start = startMeasure;
                    int span = m - start + 1;
                    String endings = number;
                    String beginText = text;
                    String hook = "1";
                    if ("discontinue".equals(parts[0])) {
                        hook = "0";
                    }
                    String endHook = hook;
                    timeline.getMeasures().get(start).getStartItemsElements().add(out -> {
                        out.open(SPANNER, TYPE, "Volta");
                        out.open("Volta");
                        out.leaf("endHookType", endHook);
                        out.leaf("beginText", beginText);
                        out.leaf("endings", endings);
                        out.close();
                        writeLocation(out, NEXT, new Location(span, Fraction.ZERO));
                        out.close();
                    });
                    measure.getEndElements().add(out -> {
                        out.open(SPANNER, TYPE, "Volta");
                        writeLocation(out, PREV, new Location(-span, Fraction.ZERO));
                        out.close();
                    });
                    startMeasure = null;
                }
            }
        }
    }


    /**
     * Calculates the relative location from one event to another.
     *
     * @param from the start event
     * @param to the target event
     * @return the location
     */
    private Location location(WriterEvent from, WriterEvent to) {
        Location location = new Location(to.getMeasureIndex() - from.getMeasureIndex(), to.getPosition().subtract(from.getPosition()));
        location.setVoices(to.getVoice() - from.getVoice());
        return location;
    }


    /**
     * Checks whether a note starts a tie.
     *
     * @param note the note
     * @return true for a tie start
     */
    private boolean isTieStart(INote note) {
        for (ITie tie : nonNull(note.getTies())) {
            if (START.equals(tie.getType())) {
                return true;
            }
        }
        if (note.getNotations() != null) {
            for (ITied tied : nonNull(note.getNotations().getTieds())) {
                if (START.equals(tied.getType())) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Gets the written MIDI pitch of a note.
     *
     * @param note the note
     * @return the pitch, or -1 for notes without pitch
     */
    private int writtenMidi(INote note) {
        if (note.getPitch() == null || note.getPitch().getStep() == null) {
            return -1;
        }
        return MuseScorePitchConverter.getInstance().toMidi(note.getPitch().getStep(), alter(note.getPitch().getAlter()),
                intValue(note.getPitch().getOctave(), 4));
    }


    /**
     * Gets the staff index of a note within its part.
     *
     * @param note the note
     * @return the staff index (0-based)
     */
    private int noteStaff(INote note) {
        if (note.getStaff() == null) {
            return 0;
        }
        return Math.max(0, note.getStaff() - 1);
    }


    /**
     * Gets the home staff of a voice: the staff most of its notes are on.
     *
     * @param notes the notes of the voice
     * @return the staff index (0-based)
     */
    private int homeStaff(List<INote> notes) {
        Map<Integer, Integer> counts = new TreeMap<>();
        for (INote note : notes) {
            counts.merge(noteStaff(note), 1, Integer::sum);
        }
        int result = 0;
        int best = -1;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                result = entry.getKey();
            }
        }
        return result;
    }


    /**
     * Gets the staff index of a clef.
     *
     * @param clef the clef
     * @return the staff index (0-based)
     */
    private int clefStaff(IClef clef) {
        if (clef.getNumber() == null) {
            return 0;
        }
        return Math.max(0, clef.getNumber() - 1);
    }


    /**
     * Finds the score part of a part.
     *
     * @param score the score
     * @param part the part
     * @return the score part, or null
     */
    private IScorePart scorePart(IScore score, IPart part) {
        if (score.getPartList() == null) {
            return null;
        }
        for (IScorePart scorePart : nonNull(score.getPartList().getScoreParts())) {
            if (scorePart.getId() != null && scorePart.getId().equals(part.getId())) {
                return scorePart;
            }
        }
        return null;
    }


    /**
     * Parses a MusicXML time signature (additive numerators are summed).
     *
     * @param beats the numerator
     * @param beatType the denominator
     * @return {beats, beatType}, or null if invalid
     */
    private int[] parseTime(String beats, String beatType) {
        try {
            int sum = 0;
            for (String part : String.valueOf(beats).split("\\+")) {
                sum += Integer.parseInt(part.trim());
            }
            int type = Integer.parseInt(String.valueOf(beatType).trim());
            if (sum <= 0 || type <= 0) {
                return null;
            }
            return new int[] {sum, type};
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Converts a measure length into a time signature (denominator at least 4).
     *
     * @param length the length in whole notes
     * @return {beats, beatType}
     */
    private int[] nominalTime(Fraction length) {
        long beats = length.getNumerator();
        long beatType = length.getDenominator();
        while (beatType < QUARTERS_PER_WHOLE) {
            beats *= 2;
            beatType *= 2;
        }
        return new int[] {(int) beats, (int) beatType};
    }


    /**
     * Formats a fraction for MuseScore ("3/4", "-1/8").
     *
     * @param value the fraction
     * @return the text
     */
    private String fraction(Fraction value) {
        return value.getNumerator() + "/" + value.getDenominator();
    }


    /**
     * Writes a move of the write position within a voice.
     *
     * @param out the output
     * @param shift the move in whole notes (negative to move back)
     */
    private static void writeMove(MuseScoreXmlOutput out, Fraction shift) {
        out.open(LOCATION);
        out.leaf(FRACTIONS, shift.getNumerator() + "/" + shift.getDenominator());
        out.close();
    }


    /**
     * Converts an alteration.
     *
     * @param alter the alteration (may be null)
     * @return the alteration in semitones
     */
    private int alter(Double alter) {
        if (alter == null) {
            return 0;
        }
        return (int) Math.round(alter);
    }


    /**
     * Parses a decimal value.
     *
     * @param value the text (may be null)
     * @return the value, or 0
     */
    private double parseDouble(String value) {
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    /**
     * Gets an integer or a default.
     *
     * @param value the value (may be null)
     * @param defaultValue the default
     * @return the value
     */
    private int intValue(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }


    /**
     * Returns a list or an empty list if it is null.
     *
     * @param <T> the element type
     * @param list the list (may be null)
     * @return the list
     */
    private static <T> List<T> nonNull(List<? extends T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list);
    }


    /**
     * Creates the mapping of MusicXML articulation names to MuseScore symbols.
     *
     * @return the map
     */
    private static Map<String, String> createArticulations() {
        Map<String, String> map = new HashMap<>();
        map.put("staccato", "articStaccatoAbove");
        map.put("accent", "articAccentAbove");
        map.put("tenuto", "articTenutoAbove");
        map.put("strong-accent", "articMarcatoAbove");
        map.put("staccatissimo", "articStaccatissimoAbove");
        map.put("spiccato", "articStaccatissimoStrokeAbove");
        map.put("detached-legato", "articTenutoStaccatoAbove");
        map.put("stress", "articStressAbove");
        map.put("unstress", "articUnstressAbove");
        map.put("up-bow", "stringsUpBow");
        map.put("down-bow", "stringsDownBow");
        map.put("harmonic", "stringsHarmonic");
        map.put("open-string", "brassMuteOpen");
        map.put("stopped", "brassMuteClosed");
        return map;
    }


    /**
     * Creates the mapping of MusicXML ornament names to MuseScore symbols.
     *
     * @return the map
     */
    private static Map<String, String> createOrnaments() {
        Map<String, String> map = new HashMap<>();
        map.put("trill-mark", "ornamentTrill");
        map.put("turn", "ornamentTurn");
        map.put("inverted-turn", "ornamentTurnInverted");
        map.put("mordent", "ornamentMordent");
        map.put("inverted-mordent", "ornamentShortTrill");
        map.put("shake", "ornamentShake3");
        return map;
    }


    /**
     * A relative MuseScore location: measures and fractions of a whole note.
     *
     * @author patrick
     */
    private static final class Location {
        private final int measures;
        private final Fraction fractions;
        private int voices;


        /**
         * Constructor for Location.
         *
         * @param measures the number of measures
         * @param fractions the offset in whole notes
         */
        Location(int measures, Fraction fractions) {
            this.measures = measures;
            this.fractions = fractions;
        }


        /**
         * Gets the number of measures.
         *
         * @return the measures
         */
        int getMeasures() {
            return measures;
        }


        /**
         * Gets the offset.
         *
         * @return the offset in whole notes
         */
        Fraction getFractions() {
            return fractions;
        }


        /**
         * Gets the voice offset.
         *
         * @return the number of voices to move
         */
        int getVoices() {
            return voices;
        }


        /**
         * Sets the voice offset.
         *
         * @param voices the number of voices to move
         */
        void setVoices(int voices) {
            this.voices = voices;
        }
    }


    /**
     * A tuplet start.
     *
     * @author patrick
     */
    private static final class TupletPlan {
        private final int actual;
        private final int normal;
        private final String baseNote;


        /**
         * Constructor for TupletPlan.
         *
         * @param actual the actual notes
         * @param normal the normal notes
         * @param baseNote the base note type
         */
        TupletPlan(int actual, int normal, String baseNote) {
            this.actual = actual;
            this.normal = normal;
            this.baseNote = baseNote;
        }


        /**
         * Gets the actual notes.
         *
         * @return the actual notes
         */
        int getActual() {
            return actual;
        }


        /**
         * Gets the normal notes.
         *
         * @return the normal notes
         */
        int getNormal() {
            return normal;
        }


        /**
         * Gets the base note type.
         *
         * @return the type
         */
        String getBaseNote() {
            return baseNote;
        }
    }


    /**
     * A hairpin start or stop.
     *
     * @author patrick
     */
    private static final class WedgeMark {
        private final Fraction position;
        private final String type;
        private final int number;


        /**
         * Constructor for WedgeMark.
         *
         * @param position the position in the measure
         * @param type crescendo, diminuendo or stop
         * @param number the wedge number
         */
        WedgeMark(Fraction position, String type, int number) {
            this.position = position;
            this.type = type;
            this.number = number;
        }


        /**
         * Gets the position.
         *
         * @return the position in whole notes
         */
        Fraction getPosition() {
            return position;
        }


        /**
         * Gets the type.
         *
         * @return the type
         */
        String getType() {
            return type;
        }


        /**
         * Gets the number.
         *
         * @return the number
         */
        int getNumber() {
            return number;
        }
    }


    /**
     * The plan of a part: staves, names, transposition and program.
     *
     * @author patrick
     */
    private final class PartPlan {
        private final IPart part;
        private final String name;
        private final String abbreviation;
        private final int staves;
        private final int chromatic;
        private final int diatonic;
        private final int program;
        private final String sound;
        private final Map<Integer, String> initialClefs = new HashMap<>();
        private final Map<Integer, Integer> homeStaves = new HashMap<>();
        private boolean percussion;


        /**
         * Constructor for PartPlan.
         *
         * @param part the part
         * @param scorePart the score part (may be null)
         */
        PartPlan(IPart part, IScorePart scorePart) {
            this.part = part;
            String partName = null;
            String partAbbreviation = null;
            int midiProgram = 0;
            String instrumentSound = null;
            if (scorePart != null) {
                partName = scorePart.getPartName();
                partAbbreviation = scorePart.getPartAbbreviation();
                if (!nonNull(scorePart.getScoreInstruments()).isEmpty()) {
                    instrumentSound = scorePart.getScoreInstruments().get(0).getInstrumentSound();
                }
                if (!nonNull(scorePart.getMidiInstruments()).isEmpty() && scorePart.getMidiInstruments().get(0).getMidiProgram() != null) {
                    midiProgram = Math.max(0, scorePart.getMidiInstruments().get(0).getMidiProgram() - 1);
                }
            }
            if (partName == null) {
                partName = part.getId();
            }
            this.name = partName;
            this.abbreviation = partAbbreviation;
            this.program = midiProgram;
            this.sound = instrumentSound;

            int staffCount = 1;
            int chromaticValue = 0;
            int diatonicValue = 0;
            boolean transposeFound = false;
            for (IMeasure measure : nonNull(part.getMeasures())) {
                for (IAttributes attributes : nonNull(measure.getAttributes())) {
                    if (attributes.getStaves() != null) {
                        staffCount = Math.max(staffCount, attributes.getStaves());
                    }
                    if (!transposeFound && !nonNull(attributes.getTransposes()).isEmpty()) {
                        ITranspose transpose = attributes.getTransposes().get(0);
                        int octave = intValue(transpose.getOctaveChange(), 0);
                        chromaticValue = intValue(transpose.getChromatic(), 0) + octave * SEMITONES_PER_OCTAVE;
                        diatonicValue = intValue(transpose.getDiatonic(), 0) + octave * STEPS_PER_OCTAVE;
                        transposeFound = true;
                    }
                    for (IClef clef : nonNull(attributes.getClefs())) {
                        int index = clefStaff(clef);
                        if (!initialClefs.containsKey(index)) {
                            initialClefs.put(index, MuseScoreSymbolConverter.getInstance().clefFromXml(clef.getSign(), clef.getLine(), clef.getClefOctaveChange()));
                        }
                    }
                }
                for (INote note : nonNull(measure.getNotes())) {
                    staffCount = Math.max(staffCount, noteStaff(note) + 1);
                }
            }
            this.staves = staffCount;
            this.chromatic = chromaticValue;
            this.diatonic = diatonicValue;

            // the home staff of a voice is the staff most of its notes are on (cross-staff notes move)
            Map<Integer, List<INote>> voiceNotes = new HashMap<>();
            for (IMeasure measure : nonNull(part.getMeasures())) {
                for (INote note : nonNull(measure.getNotes())) {
                    if (note.getUnpitched() != null) {
                        percussion = true;
                    }
                    int voice = 1;
                    if (note.getVoice() != null) {
                        voice = note.getVoice();
                    }
                    voiceNotes.computeIfAbsent(voice, k -> new ArrayList<>()).add(note);
                }
            }
            for (Map.Entry<Integer, List<INote>> entry : voiceNotes.entrySet()) {
                homeStaves.put(entry.getKey(), homeStaff(entry.getValue()));
            }
        }


        /**
         * Checks whether the part is a percussion part (unpitched notes).
         *
         * @return true for percussion
         */
        boolean isPercussion() {
            return percussion;
        }


        /**
         * Gets the home staff of a voice.
         *
         * @param voice the MusicXML voice number
         * @return the staff index (0-based)
         */
        int getHomeStaff(int voice) {
            Integer staff = homeStaves.get(voice);
            if (staff == null) {
                return 0;
            }
            return staff;
        }


        /**
         * Gets the part.
         *
         * @return the part
         */
        IPart getPart() {
            return part;
        }


        /**
         * Gets the name.
         *
         * @return the name
         */
        String getName() {
            return name;
        }


        /**
         * Gets the abbreviation.
         *
         * @return the abbreviation, or null
         */
        String getAbbreviation() {
            return abbreviation;
        }


        /**
         * Gets the number of staves.
         *
         * @return the staves
         */
        int getStaves() {
            return staves;
        }


        /**
         * Gets the chromatic transposition (written to sounding).
         *
         * @return the semitones
         */
        int getChromatic() {
            return chromatic;
        }


        /**
         * Gets the diatonic transposition (written to sounding).
         *
         * @return the steps
         */
        int getDiatonic() {
            return diatonic;
        }


        /**
         * Gets the MIDI program (0 to 127).
         *
         * @return the program
         */
        int getProgram() {
            return program;
        }


        /**
         * Gets the instrument sound (for example brass.trumpet.bflat).
         *
         * @return the sound, or null
         */
        String getSound() {
            return sound;
        }


        /**
         * Gets the initial clef of a staff.
         *
         * @param staffIndex the staff index
         * @return the MuseScore clef type, or null
         */
        String getInitialClef(int staffIndex) {
            return initialClefs.get(staffIndex);
        }
    }


    /**
     * The measures of one staff.
     *
     * @author patrick
     */
    private static final class StaffTimeline {
        private final List<MeasureTimeline> measures = new ArrayList<>();


        /**
         * Gets the measures.
         *
         * @return the measures
         */
        List<MeasureTimeline> getMeasures() {
            return measures;
        }


        /**
         * Gets all events of a voice across the measures.
         *
         * @param voice the voice index
         * @return the events in time order
         */
        List<WriterEvent> voiceEvents(int voice) {
            List<WriterEvent> result = new ArrayList<>();
            for (MeasureTimeline measure : measures) {
                if (voice < measure.getVoices().size()) {
                    result.addAll(measure.getVoices().get(voice));
                }
            }
            return result;
        }
    }


    /**
     * One measure of a staff: attributes, voices and marks.
     *
     * @author patrick
     */
    private static final class MeasureTimeline {
        private final List<List<WriterEvent>> voices = new ArrayList<>();
        private final TreeMap<Fraction, List<MuseScoreElement>> positioned = new TreeMap<>();
        private final List<MuseScoreElement> startElements = new ArrayList<>();
        private final List<MuseScoreElement> startItemElements = new ArrayList<>();
        private final List<MuseScoreElement> endElements = new ArrayList<>();
        private final List<WedgeMark> wedges = new ArrayList<>();
        private final List<String> endings = new ArrayList<>();
        private final List<String> markers = new ArrayList<>();
        private final List<INote> fermatas = new ArrayList<>();
        private Fraction length = Fraction.ZERO;
        private Fraction nominalLength = Fraction.of(1, 1);
        private String clef;
        private Integer concertKey;
        private Integer writtenKey;
        private int timeBeats;
        private int timeBeatType;
        private boolean startRepeat;
        private int endRepeat;
        private String barStyle;
        private int[] localStretch;
        private boolean localTime;


        /**
         * Gets the stretch of a local time signature.
         *
         * @return {actual, normal}, or null without a local time signature
         */
        int[] getLocalStretch() {
            return localStretch;
        }


        /**
         * Sets the stretch of a local time signature.
         *
         * @param localStretch {actual, normal}, or null
         */
        void setLocalStretch(int[] localStretch) {
            this.localStretch = localStretch;
        }


        /**
         * Checks whether a local time signature starts in this measure.
         *
         * @return true if a local time signature is written
         */
        boolean isLocalTime() {
            return localTime;
        }


        /**
         * Sets whether a local time signature starts in this measure.
         *
         * @param localTime true to write a local time signature
         */
        void setLocalTime(boolean localTime) {
            this.localTime = localTime;
        }


        /**
         * Gets the voices.
         *
         * @return the events per voice
         */
        List<List<WriterEvent>> getVoices() {
            return voices;
        }


        /**
         * Adds an element at a position (written before the first event of the first voice at or after it).
         *
         * @param position the position in the measure
         * @param element the element
         */
        void addAt(Fraction position, MuseScoreElement element) {
            positioned.computeIfAbsent(position, k -> new ArrayList<>()).add(element);
        }


        /**
         * Distributes the positioned elements to the events of the first voice. An element within a note or rest
         * is written before it with a move to its position and back, as MuseScore does.
         */
        void distribute() {
            List<WriterEvent> first = Collections.emptyList();
            if (!voices.isEmpty()) {
                first = voices.get(0);
            }
            for (Map.Entry<Fraction, List<MuseScoreElement>> entry : positioned.entrySet()) {
                WriterEvent target = null;
                WriterEvent container = null;
                for (WriterEvent event : first) {
                    if (event.isGrace()) {
                        continue;
                    }
                    if (event.getPosition().compareTo(entry.getKey()) >= 0) {
                        target = event;
                        break;
                    }
                    if (event.getPosition().add(event.getLength()).compareTo(entry.getKey()) > 0) {
                        container = event;
                    }
                }
                if (container != null && (target == null || !target.getPosition().equals(entry.getKey()))) {
                    Fraction shift = entry.getKey().subtract(container.getPosition());
                    container.getBefore().add(out -> writeMove(out, shift));
                    container.getBefore().addAll(entry.getValue());
                    container.getBefore().add(out -> writeMove(out, shift.negate()));
                } else if (target == null) {
                    endElements.addAll(entry.getValue());
                } else if (target.getPosition().equals(Fraction.ZERO) && entry.getKey().equals(Fraction.ZERO)) {
                    startElements.addAll(entry.getValue());
                } else {
                    target.getBefore().addAll(entry.getValue());
                }
            }
            positioned.clear();
        }


        /**
         * Finds the event of a note.
         *
         * @param note the note
         * @return the event, or null
         */
        WriterEvent findEvent(INote note) {
            for (List<WriterEvent> voice : voices) {
                for (WriterEvent event : voice) {
                    if (event.getNotes().contains(note)) {
                        return event;
                    }
                }
            }
            return null;
        }


        /**
         * Gets the elements written after the attributes at the start of the first voice.
         *
         * @return the elements
         */
        List<MuseScoreElement> getStartElements() {
            return startElements;
        }


        /**
         * Gets the elements written first in the first voice (voltas).
         *
         * @return the elements
         */
        List<MuseScoreElement> getStartItemsElements() {
            return startItemElements;
        }


        /**
         * Gets the elements written at the end of the first voice.
         *
         * @return the elements
         */
        List<MuseScoreElement> getEndElements() {
            return endElements;
        }


        /**
         * Gets the hairpin marks.
         *
         * @return the marks
         */
        List<WedgeMark> getWedges() {
            return wedges;
        }


        /**
         * Gets the endings ("type|number|text").
         *
         * @return the endings
         */
        List<String> getEndings() {
            return endings;
        }


        /**
         * Gets the markers (segno, coda).
         *
         * @return the marker labels
         */
        List<String> getMarkers() {
            return markers;
        }


        /**
         * Gets the notes with fermatas.
         *
         * @return the notes
         */
        List<INote> getFermatas() {
            return fermatas;
        }


        /**
         * Gets the actual length.
         *
         * @return the length in whole notes
         */
        Fraction getLength() {
            return length;
        }


        /**
         * Sets the actual length.
         *
         * @param length the length in whole notes
         */
        void setLength(Fraction length) {
            this.length = length;
        }


        /**
         * Gets the length of the time signature.
         *
         * @return the length in whole notes
         */
        Fraction getNominalLength() {
            return nominalLength;
        }


        /**
         * Sets the length of the time signature.
         *
         * @param nominalLength the length in whole notes
         */
        void setNominalLength(Fraction nominalLength) {
            this.nominalLength = nominalLength;
        }


        /**
         * Gets the clef change.
         *
         * @return the clef type, or null
         */
        String getClef() {
            return clef;
        }


        /**
         * Sets the clef change.
         *
         * @param clef the clef type
         */
        void setClef(String clef) {
            this.clef = clef;
        }


        /**
         * Gets the concert key.
         *
         * @return the fifths
         */
        Integer getConcertKey() {
            return concertKey;
        }


        /**
         * Gets the written key.
         *
         * @return the fifths, or null without a key change
         */
        Integer getWrittenKey() {
            return writtenKey;
        }


        /**
         * Sets the key change.
         *
         * @param concert the concert fifths
         * @param written the written fifths
         */
        void setKey(int concert, int written) {
            this.concertKey = concert;
            this.writtenKey = written;
        }


        /**
         * Gets the numerator of a time signature change.
         *
         * @return the beats, 0 without a change
         */
        int getTimeBeats() {
            return timeBeats;
        }


        /**
         * Gets the denominator of a time signature change.
         *
         * @return the beat type
         */
        int getTimeBeatType() {
            return timeBeatType;
        }


        /**
         * Sets a time signature change.
         *
         * @param beats the numerator
         * @param beatType the denominator
         */
        void setTime(int beats, int beatType) {
            this.timeBeats = beats;
            this.timeBeatType = beatType;
        }


        /**
         * Checks whether the measure starts a repeat.
         *
         * @return true for a start repeat
         */
        boolean isStartRepeat() {
            return startRepeat;
        }


        /**
         * Sets the start repeat.
         *
         * @param startRepeat true for a start repeat
         */
        void setStartRepeat(boolean startRepeat) {
            this.startRepeat = startRepeat;
        }


        /**
         * Gets the number of plays of an end repeat.
         *
         * @return the times, 0 without an end repeat
         */
        int getEndRepeat() {
            return endRepeat;
        }


        /**
         * Sets the end repeat.
         *
         * @param endRepeat the number of plays
         */
        void setEndRepeat(int endRepeat) {
            this.endRepeat = endRepeat;
        }


        /**
         * Gets the barline subtype at the end of the measure.
         *
         * @return the subtype, or null
         */
        String getBarStyle() {
            return barStyle;
        }


        /**
         * Sets the barline subtype at the end of the measure.
         *
         * @param barStyle the subtype
         */
        void setBarStyle(String barStyle) {
            this.barStyle = barStyle;
        }
    }


    /**
     * A chord, rest or grace chord to write.
     *
     * @author patrick
     */
    private static final class WriterEvent {
        private final int measureIndex;
        private final int voice;
        private final Fraction position;
        private final Fraction length;
        private final boolean grace;
        private final boolean rest;
        private final List<INote> notes = new ArrayList<>();
        private final List<MuseScoreElement> before = new ArrayList<>();
        private final List<MuseScoreElement> chordElements = new ArrayList<>();
        private final Map<Integer, Location> tieNext = new HashMap<>();
        private final Map<Integer, Location> tiePrev = new HashMap<>();
        private boolean measureRest;
        private String durationType;
        private int dots;
        private int chromatic;
        private int diatonic;
        private int staffMove;
        private TupletPlan tupletStart;
        private boolean tupletEnd;


        /**
         * Constructor for WriterEvent.
         *
         * @param measureIndex the index of the measure
         * @param voice the voice index
         * @param position the position in the measure
         * @param length the played length
         * @param grace true for a grace chord
         * @param rest true for a rest
         */
        WriterEvent(int measureIndex, int voice, Fraction position, Fraction length, boolean grace, boolean rest) {
            this.measureIndex = measureIndex;
            this.voice = voice;
            this.position = position;
            this.length = length;
            this.grace = grace;
            this.rest = rest;
        }


        /**
         * Gets the measure index.
         *
         * @return the index
         */
        int getMeasureIndex() {
            return measureIndex;
        }


        /**
         * Gets the position in the measure.
         *
         * @return the position in whole notes
         */
        Fraction getPosition() {
            return position;
        }


        /**
         * Gets the played length.
         *
         * @return the length in whole notes
         */
        Fraction getLength() {
            return length;
        }


        /**
         * Checks whether this is a grace chord.
         *
         * @return true for a grace chord
         */
        boolean isGrace() {
            return grace;
        }


        /**
         * Checks whether this is a rest.
         *
         * @return true for a rest
         */
        boolean isRest() {
            return rest;
        }


        /**
         * Gets the notes.
         *
         * @return the notes of the chord
         */
        List<INote> getNotes() {
            return notes;
        }


        /**
         * Gets the elements written before the chord.
         *
         * @return the elements
         */
        List<MuseScoreElement> getBefore() {
            return before;
        }


        /**
         * Gets the elements written inside the chord (slurs).
         *
         * @return the elements
         */
        List<MuseScoreElement> getChordElements() {
            return chordElements;
        }


        /**
         * Gets the tie location to the next note.
         *
         * @param index the note index
         * @return the location, or null
         */
        Location getTieNext(int index) {
            return tieNext.get(index);
        }


        /**
         * Sets the tie location to the next note.
         *
         * @param index the note index
         * @param location the location
         */
        void setTieNext(int index, Location location) {
            tieNext.put(index, location);
        }


        /**
         * Gets the tie location to the previous note.
         *
         * @param index the note index
         * @return the location, or null
         */
        Location getTiePrev(int index) {
            return tiePrev.get(index);
        }


        /**
         * Sets the tie location to the previous note.
         *
         * @param index the note index
         * @param location the location
         */
        void setTiePrev(int index, Location location) {
            tiePrev.put(index, location);
        }


        /**
         * Checks whether this is a measure rest.
         *
         * @return true for a measure rest
         */
        boolean isMeasureRest() {
            return measureRest;
        }


        /**
         * Sets whether this is a measure rest.
         *
         * @param measureRest true for a measure rest
         */
        void setMeasureRest(boolean measureRest) {
            this.measureRest = measureRest;
        }


        /**
         * Gets the duration type.
         *
         * @return the type
         */
        String getDurationType() {
            return durationType;
        }


        /**
         * Gets the number of dots.
         *
         * @return the dots
         */
        int getDots() {
            return dots;
        }


        /**
         * Sets the duration type and dots.
         *
         * @param type the type
         * @param dotCount the number of dots
         */
        void setDurationType(String type, int dotCount) {
            this.durationType = type;
            this.dots = dotCount;
        }


        /**
         * Gets the chromatic transposition of the part.
         *
         * @return the semitones
         */
        int getChromatic() {
            return chromatic;
        }


        /**
         * Gets the diatonic transposition of the part.
         *
         * @return the steps
         */
        int getDiatonic() {
            return diatonic;
        }


        /**
         * Sets the transposition of the part.
         *
         * @param chromaticValue the semitones
         * @param diatonicValue the steps
         */
        void setTransposition(int chromaticValue, int diatonicValue) {
            this.chromatic = chromaticValue;
            this.diatonic = diatonicValue;
        }


        /**
         * Gets the staff move (cross-staff chords).
         *
         * @return the move
         */
        int getStaffMove() {
            return staffMove;
        }


        /**
         * Sets the staff move.
         *
         * @param staffMove the move
         */
        void setStaffMove(int staffMove) {
            this.staffMove = staffMove;
        }


        /**
         * Gets the voice index.
         *
         * @return the voice
         */
        int getVoice() {
            return voice;
        }


        /**
         * Gets the tuplet that starts with this event.
         *
         * @return the tuplet, or null
         */
        TupletPlan getTupletStart() {
            return tupletStart;
        }


        /**
         * Sets the tuplet that starts with this event.
         *
         * @param tupletStart the tuplet
         */
        void setTupletStart(TupletPlan tupletStart) {
            this.tupletStart = tupletStart;
        }


        /**
         * Checks whether a tuplet ends with this event.
         *
         * @return true at the end of a tuplet
         */
        boolean isTupletEnd() {
            return tupletEnd;
        }


        /**
         * Sets whether a tuplet ends with this event.
         *
         * @param tupletEnd true at the end of a tuplet
         */
        void setTupletEnd(boolean tupletEnd) {
            this.tupletEnd = tupletEnd;
        }
    }
}
