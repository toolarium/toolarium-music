/*
 * LilyPondMeasureBuilder.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl;

import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.MeasureData;
import com.github.toolarium.music.io.dto.NoteData;
import com.github.toolarium.music.io.dto.PartData;
import com.github.toolarium.music.io.dto.ScoreData;
import com.github.toolarium.music.io.dto.TypeAndDots;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondEvent;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondMark;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondPitch;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondScore;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondStaff;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Builds the score data of a parsed LilyPond score: LilyPond has no measures, so the voices are divided into
 * measures by the time signatures (and the pickup of \partial). A note over a barline is split and tied, a length
 * without a single note value is split into tied notes, a gap in a voice is filled with an invisible rest.
 *
 * @author patrick
 */
public final class LilyPondMeasureBuilder {
    private static final int MAX_MEASURES = 100_000;
    private static final int VOICES_PER_STAFF = 4;
    private static final int EXTRA_VOICES = 64;
    private static final int[] DEFAULT_TIME = {4, 4};
    private static final Map<String, String> BAR_STYLES = createBarStyles();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final LilyPondMeasureBuilder INSTANCE = new LilyPondMeasureBuilder();
    }


    /**
     * Constructor.
     */
    private LilyPondMeasureBuilder() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static LilyPondMeasureBuilder getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Builds the score data.
     *
     * @param score the parsed score
     * @return the score data
     * @throws IOException if the score has no notes or too many measures
     */
    public ScoreData build(LilyPondScore score) throws IOException {
        ScoreData data = new ScoreData();
        Map<String, String> header = score.getHeader();
        data.setTitle(header.get("title"));
        data.setSubtitle(header.get("subtitle"));
        data.setRights(header.get("copyright"));
        data.addCreator("composer", header.get("composer"));
        data.addCreator("arranger", header.get("arranger"));
        data.addCreator("lyricist", header.get("poet"));

        Fraction end = end(score);
        if (!end.isPositive()) {
            throw new IOException("Invalid LilyPond file: the music has no notes.");
        }
        List<Fraction[]> measures = measures(score, end);
        TreeMap<Fraction, int[]> times = times(score);
        int partNumber = 1;
        for (List<LilyPondStaff> staves : score.getParts()) {
            if (staves.isEmpty() || isEmpty(staves)) {
                continue;
            }
            PartData part = new PartData(partName(staves, partNumber));
            part.setAbbreviation(staves.get(0).getAbbreviation());
            part.setMidiProgram(staves.get(0).getProgram());
            if (staves.get(0).getTranspose() != null) {
                part.setTranspose(staves.get(0).getTranspose()[1], staves.get(0).getTranspose()[0]);
            }
            part.setStaves(staves.size());
            for (int m = 0; m < measures.size(); m++) {
                MeasureData measure = part.measure(m);
                measure.setLength(measures.get(m)[1]);
                measure.setImplicit(m == 0 && score.getPartial() != null);
                int[] time = times.get(measures.get(m)[0]);
                if (m == 0 && time == null) {
                    time = times.floorEntry(measures.get(m)[0]).getValue();
                }
                if (time != null) {
                    measure.setTime(time[0], time[1], null);
                }
            }
            for (int s = 0; s < staves.size(); s++) {
                // the chord symbols belong to the first staff of the first part
                List<LilyPondMark> harmonies = null;
                if (partNumber == 1 && s == 0) {
                    harmonies = score.getHarmonies();
                }
                buildStaff(part, staves.get(s), s + 1, measures, harmonies);
            }
            data.getParts().add(part);
            partNumber++;
        }
        return data;
    }


    /**
     * Builds one staff of a part: its marks and its voices.
     *
     * @param part the part
     * @param staff the staff
     * @param staffNumber the staff number in the part (1-based)
     * @param measures the start and length of the measures
     * @param harmonies the chord symbols to attach to the notes of this staff, or null
     */
    private void buildStaff(PartData part, LilyPondStaff staff, int staffNumber, List<Fraction[]> measures, List<LilyPondMark> harmonies) {
        applyMarks(part, staff, staffNumber, measures);
        Map<Fraction, EventData> starts = new HashMap<>();
        for (Map.Entry<Integer, List<LilyPondEvent>> voice : staff.getVoices().entrySet()) {
            int voiceNumber = (staffNumber - 1) * VOICES_PER_STAFF + voice.getKey();
            if (voice.getKey() > VOICES_PER_STAFF) {
                // more than four voices: the further voices get numbers after the voices of all staves
                voiceNumber = EXTRA_VOICES * staffNumber + voice.getKey();
            }
            Map<Fraction, EventData> voiceStarts = buildVoice(part, staffNumber, voiceNumber, voice.getValue(), measures);
            if (voice.getKey() == 1) {
                starts.putAll(voiceStarts);
            }
        }
        if (harmonies != null) {
            for (LilyPondMark mark : harmonies) {
                EventData event = starts.get(mark.getPosition());
                if (event != null) {
                    event.getHarmonies().add((HarmonyImpl) mark.getValue());
                }
            }
        }
    }


    /**
     * Builds a voice: its events divided into the measures.
     *
     * @param part the part
     * @param staffNumber the staff number (1-based)
     * @param voiceNumber the voice number in the part
     * @param events the events of the voice
     * @param measures the start and length of the measures
     * @return the first event data of each event start (for the chord symbols)
     */
    private Map<Fraction, EventData> buildVoice(PartData part, int staffNumber, int voiceNumber, List<LilyPondEvent> events, List<Fraction[]> measures) {
        Map<Fraction, EventData> starts = new HashMap<>();
        Fraction cursor = Fraction.ZERO;
        LilyPondEvent previous = null;
        for (LilyPondEvent event : events) {
            if (event.isGrace()) {
                EventData grace = createEvent(event, null, null, previous);
                TypeAndDots value = NoteValueConverter.getInstance().fromFraction(event.getDuration());
                if (value != null) {
                    grace.setType(value.getType());
                    grace.setDots(value.getDots());
                }
                grace.setDuration(event.getDuration());
                grace.setGrace(true);
                grace.setGraceSlash(event.isGraceSlash());
                int m = measureIndex(measures, event.getStart(), false);
                part.measure(m).voice(staffNumber, voiceNumber).getEvents().add(grace);
                continue;
            }
            if (event.getStart().compareTo(cursor) > 0) {
                addPieces(part, staffNumber, voiceNumber, null, cursor, event.getStart().subtract(cursor), measures, previous);
            } else if (event.getStart().compareTo(cursor) < 0) {
                // overlapping events of one voice (for example a chord written as two voices): the later one is dropped
                continue;
            }
            EventData first = addPieces(part, staffNumber, voiceNumber, event, event.getStart(), event.getDuration(), measures, previous);
            if (first != null) {
                starts.putIfAbsent(event.getStart(), first);
            }
            cursor = event.getStart().add(event.getDuration());
            previous = event;
        }
        return starts;
    }


    /**
     * Adds an event (or a gap) to the measures: it is split at the barlines and into note values.
     *
     * @param part the part
     * @param staffNumber the staff number (1-based)
     * @param voiceNumber the voice number in the part
     * @param event the event, or null for an invisible rest in a gap
     * @param start the start
     * @param length the length
     * @param measures the start and length of the measures
     * @param previous the previous event of the voice (for the ties), or null
     * @return the first event data, or null
     */
    private EventData addPieces(PartData part, int staffNumber, int voiceNumber, LilyPondEvent event, Fraction start, Fraction length,
            List<Fraction[]> measures, LilyPondEvent previous) {
        List<EventData> pieces = new ArrayList<>();
        Fraction position = start;
        Fraction remaining = length;
        while (remaining.isPositive()) {
            int m = measureIndex(measures, position, false);
            Fraction measureEnd = measures.get(m)[0].add(measures.get(m)[1]);
            Fraction piece = remaining.min(measureEnd.subtract(position));
            if (!piece.isPositive()) {
                piece = remaining;
            }
            boolean wholeMeasure = position.equals(measures.get(m)[0]) && piece.equals(measures.get(m)[1]);
            for (EventData value : values(event, piece, wholeMeasure, pieces.isEmpty(), previous)) {
                part.measure(m).voice(staffNumber, voiceNumber).getEvents().add(value);
                pieces.add(value);
            }
            position = position.add(piece);
            remaining = remaining.subtract(piece);
        }
        if (pieces.isEmpty()) {
            return null;
        }
        finishPieces(event, pieces);
        return pieces.get(0);
    }


    /**
     * Creates the event data of one piece of an event within a measure: a tuplet note, a full measure rest or the
     * note values of the length.
     *
     * @param event the event, or null for an invisible rest
     * @param length the length of the piece
     * @param wholeMeasure true if the piece fills the measure
     * @param first true for the first piece of the event
     * @param previous the previous event of the voice, or null
     * @return the event data
     */
    private List<EventData> values(LilyPondEvent event, Fraction length, boolean wholeMeasure, boolean first, LilyPondEvent previous) {
        List<EventData> result = new ArrayList<>();
        // only the first piece of an event ends the tie of the previous event
        LilyPondEvent tieOrigin = null;
        if (first) {
            tieOrigin = previous;
        }
        if (event != null && (event.getTupletActual() > 0 || event.isMeasureRest() && wholeMeasure)) {
            EventData data = createEvent(event, length, null, tieOrigin);
            if (event.getTupletActual() > 0) {
                data.setTuplet(event.getTupletActual(), event.getTupletNormal());
                data.setTupletStart(event.isTupletStart() && first);
                data.setTupletStop(event.isTupletStop());
            } else {
                data.setMeasureRest(true);
            }
            result.add(data);
            return result;
        }
        List<TypeAndDots> parts = NoteValueConverter.getInstance().split(length);
        if (parts.isEmpty()) {
            result.add(createEvent(event, length, null, tieOrigin));
            return result;
        }
        for (int i = 0; i < parts.size(); i++) {
            TypeAndDots value = parts.get(i);
            Fraction duration = NoteValueConverter.getInstance().toFraction(value.getType(), value.getDots());
            LilyPondEvent origin = null;
            if (i == 0) {
                origin = tieOrigin;
            }
            result.add(createEvent(event, duration, value, origin));
        }
        return result;
    }


    /**
     * Creates the event data of a piece: its notes (or a rest) with type and duration.
     *
     * @param event the event, or null for an invisible rest
     * @param duration the duration of the piece, or null to keep it unset
     * @param value the note value, or null to derive it from the duration
     * @param previous the previous event if this piece starts the event (for a tie stop), or null
     * @return the event data
     */
    private EventData createEvent(LilyPondEvent event, Fraction duration, TypeAndDots value, LilyPondEvent previous) {
        EventData data = new EventData();
        if (duration != null) {
            data.setDuration(duration);
        }
        if (value != null) {
            data.setType(value.getType());
            data.setDots(value.getDots());
        }
        if (event == null) {
            data.setInvisible(true);
            return data;
        }
        data.setInvisible(event.isInvisible());
        for (LilyPondPitch pitch : event.getPitches()) {
            NoteData note = new NoteData(pitch.getStep(), pitch.getAlter(), pitch.getOctave());
            if (previous != null && previous.isTied(pitch) && previous.getPitches().contains(pitch)) {
                note.setTieStop(true);
            }
            data.getNotes().add(note);
        }
        return data;
    }


    /**
     * Finishes the pieces of an event: the pieces are tied, the marks, directions, slurs and lyrics are set on the
     * first piece (slur ends on the last), the tie of the event on the last piece.
     *
     * @param event the event, or null for an invisible rest
     * @param pieces the pieces in order
     */
    private void finishPieces(LilyPondEvent event, List<EventData> pieces) {
        if (event == null) {
            return;
        }
        EventData first = pieces.get(0);
        EventData last = pieces.get(pieces.size() - 1);
        for (int i = 0; i + 1 < pieces.size(); i++) {
            for (int n = 0; n < pieces.get(i).getNotes().size(); n++) {
                pieces.get(i).getNotes().get(n).setTieStart(true);
                pieces.get(i + 1).getNotes().get(n).setTieStop(true);
            }
        }
        for (int n = 0; n < last.getNotes().size() && n < event.getPitches().size(); n++) {
            if (event.isTied(event.getPitches().get(n))) {
                last.getNotes().get(n).setTieStart(true);
            }
        }
        first.getSlurStarts().addAll(event.getSlurStarts());
        last.getSlurStops().addAll(event.getSlurStops());
        first.getLyrics().addAll(event.getLyrics());
        for (DirectionImpl direction : event.getDirections()) {
            first.getDirections().add(direction);
        }
        for (String mark : event.getMarks()) {
            applyMark(first, mark);
        }
    }


    /**
     * Applies an articulation or ornament to an event.
     *
     * @param data the event data
     * @param mark the MusicXML name of the mark
     */
    private void applyMark(EventData data, String mark) {
        switch (mark) {
            case "staccato":
                data.articulations().setStaccato("");
                break;
            case "accent":
                data.articulations().setAccent("");
                break;
            case "tenuto":
                data.articulations().setTenuto("");
                break;
            case "strong-accent":
                data.articulations().setStrongAccent("");
                break;
            case "staccatissimo":
                data.articulations().setStaccatissimo("");
                break;
            case "detached-legato":
                data.articulations().setDetachedLegato("");
                break;
            case "fermata":
                data.getFermatas().add(ScoreBuilder.getInstance().createFermata(null, false));
                break;
            case "trill-mark":
                data.ornaments().setTrillMark("");
                break;
            case "mordent":
                data.ornaments().setMordent("");
                break;
            case "inverted-mordent":
                data.ornaments().setInvertedMordent("");
                break;
            case "turn":
                data.ornaments().setTurn("");
                break;
            default:
                break;
        }
    }


    /**
     * Applies the marks of a staff: clefs, keys, barlines, repeats, endings and directions.
     *
     * @param part the part
     * @param staff the staff
     * @param staffNumber the staff number in the part (1-based)
     * @param measures the start and length of the measures
     */
    private void applyMarks(PartData part, LilyPondStaff staff, int staffNumber, List<Fraction[]> measures) {
        boolean keySet = false;
        for (LilyPondMark mark : staff.getMarks()) {
            Fraction position = mark.getPosition();
            int m = measureIndex(measures, position, false);
            MeasureData measure = part.measure(m);
            Fraction offset = position.subtract(measures.get(m)[0]);
            switch (mark.getKind()) {
                case LilyPondMark.CLEF:
                    if (position.isZero()) {
                        part.getDefaultClefs().put(staffNumber, (ClefImpl) mark.getValue());
                    } else {
                        measure.addClef(staffNumber, offset, (ClefImpl) mark.getValue());
                    }
                    break;
                case LilyPondMark.KEY:
                    if (staffNumber == 1) {
                        measure.setKey(mark.getNumber(), mark.getText());
                        keySet = keySet || m == 0;
                    }
                    break;
                case LilyPondMark.BAR:
                    applyBar(part, measures, position, mark.getText());
                    break;
                case LilyPondMark.REPEAT_START:
                    part.measure(measureIndex(measures, position, false)).setRepeatStart(true);
                    break;
                case LilyPondMark.REPEAT_END:
                    part.measure(measureIndex(measures, position, true)).setRepeatEnd(true, mark.getNumber());
                    break;
                case LilyPondMark.ENDING_START:
                    applyEnding(part, staff, measures, mark);
                    break;
                case LilyPondMark.DIRECTION:
                    measure.addDirection((DirectionImpl) mark.getValue(), offset, staffNumber);
                    break;
                default:
                    break;
            }
        }
        if (staffNumber == 1 && !keySet) {
            part.measure(0).setKey(0, "major");
        }
        if (!part.getDefaultClefs().containsKey(staffNumber)) {
            part.getDefaultClefs().put(staffNumber, LilyPondPitchConverter.getInstance().clef("treble"));
        }
    }


    /**
     * Applies a barline: a bar type at the start of a measure belongs to the end of the previous measure.
     *
     * @param part the part
     * @param measures the start and length of the measures
     * @param position the position of the barline
     * @param type the LilyPond bar type (for example |., ||, .|:, :|.)
     */
    private void applyBar(PartData part, List<Fraction[]> measures, Fraction position, String type) {
        int before = measureIndex(measures, position, true);
        int after = measureIndex(measures, position, false);
        boolean atBarline = position.isPositive() && measures.get(after)[0].equals(position);
        if (type.startsWith(":")) {
            part.measure(before).setRepeatEnd(true, null);
        }
        if (type.endsWith(":") && atBarline) {
            part.measure(after).setRepeatStart(true);
        }
        String style = BAR_STYLES.get(type);
        if (style != null && !type.contains(":")) {
            part.measure(before).setRightBarStyle(style);
        }
    }


    /**
     * Applies an ending (alternative): the measures from its start to its end get the ending number.
     *
     * @param part the part
     * @param staff the staff with the end mark
     * @param measures the start and length of the measures
     * @param start the start mark
     */
    private void applyEnding(PartData part, LilyPondStaff staff, List<Fraction[]> measures, LilyPondMark start) {
        Fraction end = start.getPosition();
        for (LilyPondMark mark : staff.getMarks()) {
            if (mark.getKind() == LilyPondMark.ENDING_STOP && start.getText().equals(mark.getText()) && mark.getPosition().compareTo(end) >= 0) {
                end = mark.getPosition();
                break;
            }
        }
        int first = measureIndex(measures, start.getPosition(), false);
        int last = measureIndex(measures, end, true);
        for (int m = first; m <= last; m++) {
            part.measure(m).setEndingNumber(start.getText());
        }
    }


    /**
     * Gets the index of the measure that contains a position.
     *
     * @param measures the start and length of the measures
     * @param position the position
     * @param endInclusive true to take the measure that ends at the position (for marks at the end of a measure)
     * @return the index of the measure
     */
    private int measureIndex(List<Fraction[]> measures, Fraction position, boolean endInclusive) {
        int low = 0;
        int high = measures.size() - 1;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            int compare = measures.get(middle)[0].compareTo(position);
            if (compare < 0 || compare == 0 && !endInclusive) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }


    /**
     * Creates the measures: their start and length from the time signatures; the first measure has the length of
     * the pickup (\partial).
     *
     * @param score the score
     * @param end the end of the music
     * @return the start and length of every measure
     * @throws IOException if the music has too many measures
     */
    private List<Fraction[]> measures(LilyPondScore score, Fraction end) throws IOException {
        TreeMap<Fraction, int[]> times = times(score);
        List<Fraction[]> result = new ArrayList<>();
        Fraction position = Fraction.ZERO;
        if (score.getPartial() != null && score.getPartial().isPositive()) {
            result.add(new Fraction[] {Fraction.ZERO, score.getPartial()});
            position = score.getPartial();
        }
        while (position.compareTo(end) < 0 || result.isEmpty()) {
            int[] time = times.floorEntry(position).getValue();
            Fraction length = Fraction.of(time[0], time[1]);
            Fraction change = times.higherKey(position);
            if (change != null && change.compareTo(position.add(length)) < 0) {
                length = change.subtract(position);
            }
            result.add(new Fraction[] {position, length});
            position = position.add(length);
            if (result.size() > MAX_MEASURES) {
                throw new IOException("LilyPond music exceeds maximum measure count (" + MAX_MEASURES + ").");
            }
        }
        return result;
    }


    /**
     * Collects the time signatures of all staves by position (the first one at a position counts).
     *
     * @param score the score
     * @return the beats and beat type by position, with 4/4 at the start if there is no time signature there
     */
    private TreeMap<Fraction, int[]> times(LilyPondScore score) {
        TreeMap<Fraction, int[]> result = new TreeMap<>();
        for (List<LilyPondStaff> part : score.getParts()) {
            for (LilyPondStaff staff : part) {
                for (LilyPondMark mark : staff.getMarks()) {
                    if (mark.getKind() == LilyPondMark.TIME) {
                        result.putIfAbsent(mark.getPosition(), new int[] {mark.getNumber(), mark.getSecondNumber()});
                    }
                }
            }
        }
        result.putIfAbsent(Fraction.ZERO, DEFAULT_TIME);
        return result;
    }


    /**
     * Gets the end of the music: the end of the last event.
     *
     * @param score the score
     * @return the end in whole notes
     */
    private Fraction end(LilyPondScore score) {
        Fraction result = Fraction.ZERO;
        for (List<LilyPondStaff> part : score.getParts()) {
            for (LilyPondStaff staff : part) {
                for (List<LilyPondEvent> voice : staff.getVoices().values()) {
                    for (LilyPondEvent event : voice) {
                        if (!event.isGrace()) {
                            result = result.max(event.getStart().add(event.getDuration()));
                        }
                    }
                }
            }
        }
        return result;
    }


    /**
     * Checks whether the staves of a part have no events.
     *
     * @param staves the staves
     * @return true if no staff has an event
     */
    private boolean isEmpty(List<LilyPondStaff> staves) {
        for (LilyPondStaff staff : staves) {
            for (List<LilyPondEvent> voice : staff.getVoices().values()) {
                if (!voice.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Gets the name of a part: the instrument name of its first named staff.
     *
     * @param staves the staves of the part
     * @param partNumber the number of the part (1-based)
     * @return the name
     */
    private String partName(List<LilyPondStaff> staves, int partNumber) {
        for (LilyPondStaff staff : staves) {
            if (staff.getName() != null) {
                return staff.getName();
            }
        }
        return "Part " + partNumber;
    }


    /**
     * Creates the MusicXML bar styles of the LilyPond bar types.
     *
     * @return the bar style by bar type
     */
    private static Map<String, String> createBarStyles() {
        Map<String, String> result = new HashMap<>();
        result.put("|.", "light-heavy");
        result.put("||", "light-light");
        result.put(".|", "heavy-light");
        result.put(".", "heavy");
        result.put("..", "heavy-heavy");
        result.put("!", "dashed");
        result.put(";", "dotted");
        result.put("'", "tick");
        result.put("", "none");
        return result;
    }
}
