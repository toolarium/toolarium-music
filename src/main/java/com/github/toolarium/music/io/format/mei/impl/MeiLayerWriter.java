/*
 * MeiLayerWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl;

import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.TypeAndDots;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterEvent;
import com.github.toolarium.music.model.measure.IClef;
import com.github.toolarium.music.model.notation.ITied;
import com.github.toolarium.music.model.notation.ITuplet;
import com.github.toolarium.music.model.note.IBeam;
import com.github.toolarium.music.model.note.ILyric;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.note.ITie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


/**
 * Writes the layers of a staff in a measure: one layer per voice with its notes, chords, rests and spaces, grouped
 * in tuplets and beams, with ties, articulations, fermatas, grace notes and lyrics. A gap in a layer is filled with
 * a space.
 *
 * @author patrick
 */
public final class MeiLayerWriter {
    private static final String LAYER = "layer";
    private static final String DUR = "dur";
    private static final String DOTS = "dots";
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String NO = "no";
    private static final int TUPLET = 0;
    private static final int BEAM = 1;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MeiLayerWriter INSTANCE = new MeiLayerWriter();
    }


    /**
     * Constructor.
     */
    private MeiLayerWriter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MeiLayerWriter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Writes the layers of a staff.
     *
     * @param out the output
     * @param voices the events by voice (may be null for an empty staff)
     * @param clefsAtEnd the clef changes at the end of the measure (may be null)
     */
    void writeLayers(MeiXmlOutput out, Map<Integer, List<MeiWriterEvent>> voices, List<IClef> clefsAtEnd) {
        if (voices == null || voices.isEmpty()) {
            out.open(LAYER, "n", "1");
            writeClefs(out, clefsAtEnd);
            out.close();
            return;
        }
        int layer = 1;
        for (List<MeiWriterEvent> events : voices.values()) {
            out.open(LAYER, "n", String.valueOf(layer));
            List<MeiWriterEvent> filled = fillGaps(events);
            List<int[]> ranges = ranges(filled);
            writeRange(out, filled, 0, filled.size() - 1, ranges, new boolean[ranges.size()], false);
            if (layer == 1) {
                writeClefs(out, clefsAtEnd);
            }
            out.close();
            layer++;
        }
    }


    /**
     * Fills the gaps of a layer with spaces.
     *
     * @param events the events in order
     * @return the events with the spaces
     */
    private List<MeiWriterEvent> fillGaps(List<MeiWriterEvent> events) {
        List<MeiWriterEvent> result = new ArrayList<>();
        Fraction cursor = Fraction.ZERO;
        for (MeiWriterEvent event : events) {
            if (event.getStart().compareTo(cursor) > 0) {
                Fraction position = cursor;
                for (TypeAndDots value : NoteValueConverter.getInstance().split(event.getStart().subtract(cursor))) {
                    Fraction length = NoteValueConverter.getInstance().toFraction(value.getType(), value.getDots());
                    MeiWriterEvent space = new MeiWriterEvent(null, position, length);
                    space.setSpaceValue(value);
                    result.add(space);
                    position = position.add(length);
                }
            }
            result.add(event);
            cursor = cursor.max(event.getStart().add(event.getLength()));
        }
        return result;
    }


    /**
     * Finds the tuplets and beams of a layer as index ranges; beams that cross a tuplet and ranges of one event are
     * left out.
     *
     * @param events the events
     * @return the ranges as {first index, last index, kind}, outer ranges first
     */
    private List<int[]> ranges(List<MeiWriterEvent> events) {
        List<int[]> tuplets = new ArrayList<>();
        List<int[]> beams = new ArrayList<>();
        List<Integer> openTuplets = new ArrayList<>();
        int openBeam = -1;
        for (int i = 0; i < events.size(); i++) {
            INote note = events.get(i).getFirst();
            if (note == null) {
                continue;
            }
            for (ITuplet tuplet : tuplets(note)) {
                if (START.equals(tuplet.getType())) {
                    openTuplets.add(i);
                }
            }
            for (ITuplet tuplet : tuplets(note)) {
                if (STOP.equals(tuplet.getType()) && !openTuplets.isEmpty()) {
                    int first = openTuplets.remove(openTuplets.size() - 1);
                    tuplets.add(new int[] {first, i, TUPLET});
                }
            }
            String beam = beam(note);
            if ("begin".equals(beam)) {
                openBeam = i;
            } else if ("end".equals(beam) && openBeam >= 0) {
                beams.add(new int[] {openBeam, i, BEAM});
                openBeam = -1;
            }
        }
        addUnmarkedTuplets(events, tuplets);
        List<int[]> result = new ArrayList<>(tuplets);
        for (int[] beam : beams) {
            boolean crossing = false;
            for (int[] tuplet : tuplets) {
                boolean inside = beam[0] >= tuplet[0] && beam[1] <= tuplet[1];
                boolean around = beam[0] <= tuplet[0] && beam[1] >= tuplet[1];
                boolean apart = beam[1] < tuplet[0] || beam[0] > tuplet[1];
                crossing = crossing || !inside && !around && !apart;
            }
            if (!crossing && beam[1] > beam[0]) {
                result.add(beam);
            }
        }
        result.sort(Comparator.<int[]>comparingInt(range -> range[0]).thenComparing(range -> -range[1]).thenComparingInt(range -> range[2]));
        return result;
    }


    /**
     * Adds tuplets for the tuplet notes without tuplet marks: a run of notes with the same ratio is divided into
     * groups of the length of the normal notes (for example a quarter for triplet eighths).
     *
     * @param events the events
     * @param tuplets the tuplet ranges (added to)
     */
    private void addUnmarkedTuplets(List<MeiWriterEvent> events, List<int[]> tuplets) {
        boolean[] covered = new boolean[events.size()];
        for (int[] tuplet : tuplets) {
            for (int i = tuplet[0]; i <= tuplet[1]; i++) {
                covered[i] = true;
            }
        }
        int first = -1;
        Fraction sum = Fraction.ZERO;
        Fraction target = null;
        String ratio = null;
        for (int i = 0; i <= events.size(); i++) {
            String current = null;
            if (i < events.size() && !covered[i] && events.get(i).getFirst() != null && events.get(i).getLength().isPositive()) {
                current = ratio(events.get(i).getFirst());
            }
            if (first >= 0 && (current == null || !current.equals(ratio))) {
                tuplets.add(new int[] {first, i - 1, TUPLET});
                first = -1;
            }
            if (current == null) {
                continue;
            }
            if (first < 0) {
                first = i;
                sum = Fraction.ZERO;
                ratio = current;
                target = groupLength(events.get(i));
            }
            sum = sum.add(events.get(i).getLength());
            if (target != null && sum.compareTo(target) >= 0) {
                tuplets.add(new int[] {first, i, TUPLET});
                first = -1;
            }
        }
    }


    /**
     * Gets the tuplet ratio of a note.
     *
     * @param note the note
     * @return the ratio as text (for example 3:2), or null if the note is no tuplet note
     */
    private String ratio(INote note) {
        if (note.getTimeModification() == null || note.getTimeModification().getActualNotes() == null
                || note.getTimeModification().getNormalNotes() == null || note.getTimeModification().getActualNotes() <= 0
                || note.getTimeModification().getNormalNotes() <= 0
                || note.getTimeModification().getActualNotes().equals(note.getTimeModification().getNormalNotes())) {
            return null;
        }
        return note.getTimeModification().getActualNotes() + ":" + note.getTimeModification().getNormalNotes();
    }


    /**
     * Gets the sounding length of a tuplet group that starts with an event: the normal notes of the normal type.
     *
     * @param event the first event
     * @return the length, or null if unknown
     */
    private Fraction groupLength(MeiWriterEvent event) {
        INote note = event.getFirst();
        String type = note.getTimeModification().getNormalType();
        if (type == null) {
            type = note.getType();
        }
        Fraction unit = NoteValueConverter.getInstance().toFraction(type);
        if (unit == null) {
            return null;
        }
        return unit.multiply(note.getTimeModification().getNormalNotes(), 1);
    }


    /**
     * Writes the events of an index range with their tuplets and beams.
     *
     * @param out the output
     * @param events the events
     * @param from the first index
     * @param to the last index
     * @param ranges the tuplet and beam ranges
     * @param used the ranges that are written already
     * @param inTuplet true inside a tuplet element
     */
    private void writeRange(MeiXmlOutput out, List<MeiWriterEvent> events, int from, int to, List<int[]> ranges, boolean[] used, boolean inTuplet) {
        int i = from;
        while (i <= to) {
            int found = -1;
            for (int r = 0; r < ranges.size(); r++) {
                int[] range = ranges.get(r);
                if (!used[r] && range[0] == i && range[1] <= to && (found < 0 || range[1] > ranges.get(found)[1])) {
                    found = r;
                }
            }
            if (found < 0) {
                writeEvent(out, events.get(i), inTuplet);
                i++;
                continue;
            }
            used[found] = true;
            int[] range = ranges.get(found);
            if (range[2] == TUPLET) {
                out.open("tuplet", tupletAttributes(events.get(range[0]).getFirst()));
                writeRange(out, events, range[0], range[1], ranges, used, true);
            } else {
                out.open("beam");
                writeRange(out, events, range[0], range[1], ranges, used, inTuplet);
            }
            out.close();
            i = range[1] + 1;
        }
    }


    /**
     * Gets the attributes of a tuplet element: the ratio and the visibility of bracket and number.
     *
     * @param note the first note of the tuplet
     * @return the attribute names and values
     */
    private String[] tupletAttributes(INote note) {
        List<String> attributes = new ArrayList<>();
        int actual = 3;
        int normal = 2;
        if (note.getTimeModification() != null && note.getTimeModification().getActualNotes() != null
                && note.getTimeModification().getNormalNotes() != null) {
            actual = note.getTimeModification().getActualNotes();
            normal = note.getTimeModification().getNormalNotes();
        }
        attributes.add("num");
        attributes.add(String.valueOf(actual));
        attributes.add("numbase");
        attributes.add(String.valueOf(normal));
        for (ITuplet tuplet : tuplets(note)) {
            if (START.equals(tuplet.getType())) {
                if (NO.equals(tuplet.getBracket())) {
                    attributes.add("bracket.visible");
                    attributes.add(FALSE);
                }
                if ("none".equals(tuplet.getShowNumber())) {
                    attributes.add("num.visible");
                    attributes.add(FALSE);
                }
                break;
            }
        }
        return attributes.toArray(new String[0]);
    }


    /**
     * Writes an event: its clef changes and the space, rest, note or chord.
     *
     * @param out the output
     * @param event the event
     * @param inTuplet true inside a tuplet element
     */
    private void writeEvent(MeiXmlOutput out, MeiWriterEvent event, boolean inTuplet) {
        writeClefs(out, event.getClefsBefore());
        if (event.getSpaceValue() != null) {
            out.empty("space", durationAttributes(event.getSpaceValue().getType(), event.getSpaceValue().getDots(), event.getLength()));
            return;
        }
        INote first = event.getFirst();
        if (first.getRest() != null) {
            writeRest(out, event, first, inTuplet);
        } else if (event.getNotes().size() == 1) {
            writeNote(out, event, first, inTuplet);
        } else {
            writeChord(out, event, inTuplet);
        }
    }


    /**
     * Writes a rest: a measure rest, a rest, or a space if the rest is invisible.
     *
     * @param out the output
     * @param event the event
     * @param rest the rest
     * @param inTuplet true inside a tuplet element
     */
    private void writeRest(MeiXmlOutput out, MeiWriterEvent event, INote rest, boolean inTuplet) {
        boolean invisible = NO.equals(rest.getPrintObject());
        // a whole rest that does not last a whole note is a measure rest (for example in 3/4)
        boolean measureRest = "yes".equals(rest.getRest().getMeasure())
                || "whole".equals(rest.getType()) && event.getStart().isZero() && !event.getLength().equals(Fraction.of(1, 1));
        if (measureRest) {
            List<String> attributes = new ArrayList<>();
            add(attributes, "xml:id", event.getId());
            add(attributes, "fermata", MeiNotationConverter.getInstance().fermata(rest.getNotations()));
            add(attributes, "cue", cue(rest));
            if (invisible) {
                out.empty("mSpace", "xml:id", event.getId());
            } else {
                out.empty("mRest", attributes.toArray(new String[0]));
            }
            return;
        }
        List<String> attributes = new ArrayList<>();
        add(attributes, "xml:id", event.getId());
        attributes.addAll(Arrays.asList(durationAttributes(rest, event, inTuplet)));
        if (invisible) {
            out.empty("space", attributes.toArray(new String[0]));
            return;
        }
        add(attributes, "fermata", MeiNotationConverter.getInstance().fermata(rest.getNotations()));
        add(attributes, "cue", cue(rest));
        out.empty("rest", attributes.toArray(new String[0]));
    }


    /**
     * Writes a single note.
     *
     * @param out the output
     * @param event the event
     * @param note the note
     * @param inTuplet true inside a tuplet element
     */
    private void writeNote(MeiXmlOutput out, MeiWriterEvent event, INote note, boolean inTuplet) {
        List<String> attributes = new ArrayList<>();
        add(attributes, "xml:id", event.getId());
        pitchAttributes(attributes, note);
        attributes.addAll(Arrays.asList(durationAttributes(note, event, inTuplet)));
        eventAttributes(attributes, note);
        add(attributes, "tie", tie(note));
        add(attributes, "head.shape", MeiNotationConverter.getInstance().headShape(note.getNotehead()));
        if (NO.equals(note.getPrintObject())) {
            add(attributes, "visible", FALSE);
        }
        String[] array = attributes.toArray(new String[0]);
        if (!hasLyrics(note)) {
            out.empty("note", array);
            return;
        }
        out.open("note", array);
        writeLyrics(out, note);
        out.close();
    }


    /**
     * Writes a chord.
     *
     * @param out the output
     * @param event the event
     * @param inTuplet true inside a tuplet element
     */
    private void writeChord(MeiXmlOutput out, MeiWriterEvent event, boolean inTuplet) {
        INote first = event.getFirst();
        List<String> attributes = new ArrayList<>();
        add(attributes, "xml:id", event.getId());
        attributes.addAll(Arrays.asList(durationAttributes(first, event, inTuplet)));
        eventAttributes(attributes, first);
        out.open("chord", attributes.toArray(new String[0]));
        boolean lyricsWritten = false;
        for (INote note : event.getNotes()) {
            List<String> noteAttributes = new ArrayList<>();
            pitchAttributes(noteAttributes, note);
            add(noteAttributes, "tie", tie(note));
            add(noteAttributes, "head.shape", MeiNotationConverter.getInstance().headShape(note.getNotehead()));
            if (NO.equals(note.getPrintObject())) {
                add(noteAttributes, "visible", FALSE);
            }
            if (!lyricsWritten && hasLyrics(note)) {
                out.open("note", noteAttributes.toArray(new String[0]));
                writeLyrics(out, note);
                out.close();
                lyricsWritten = true;
            } else {
                out.empty("note", noteAttributes.toArray(new String[0]));
            }
        }
        out.close();
    }


    /**
     * Adds the pitch attributes of a note: pitch name, octave, written and gestural accidental.
     *
     * @param attributes the attributes
     * @param note the note
     */
    private void pitchAttributes(List<String> attributes, INote note) {
        String step = null;
        Integer octave = null;
        Double alter = null;
        if (note.getPitch() != null) {
            step = note.getPitch().getStep();
            octave = note.getPitch().getOctave();
            alter = note.getPitch().getAlter();
        } else if (note.getUnpitched() != null) {
            step = note.getUnpitched().getDisplayStep();
            octave = note.getUnpitched().getDisplayOctave();
        }
        if (step != null) {
            add(attributes, "pname", step.toLowerCase());
        }
        if (octave != null) {
            // MEI has no octave below 0 (MIDI notes below C0 are written in octave 0)
            add(attributes, "oct", String.valueOf(Math.max(0, octave)));
        }
        String written = MeiNotationConverter.getInstance().accidental(note.getAccidental());
        add(attributes, "accid", written);
        if (note.getPitch() != null) {
            // the sounding accidental is always given, so the reader does not apply the key or earlier accidentals
            String gestural = MeiNotationConverter.getInstance().gesturalAccidental(alter);
            if (gestural != null && !gestural.equals(written) && !("x".equals(written) && "ss".equals(gestural))) {
                add(attributes, "accid.ges", gestural);
            }
        }
    }


    /**
     * Adds the attributes of a note, chord or rest event: grace, stem, cue, articulations and fermata.
     *
     * @param attributes the attributes
     * @param note the (first) note
     */
    private void eventAttributes(List<String> attributes, INote note) {
        if (note.getGrace() != null) {
            if ("yes".equals(note.getGrace().getSlash())) {
                add(attributes, "grace", "unacc");
                add(attributes, "stem.mod", "1slash");
            } else {
                add(attributes, "grace", "acc");
            }
        }
        if ("up".equals(note.getStem()) || "down".equals(note.getStem())) {
            add(attributes, "stem.dir", note.getStem());
        }
        add(attributes, "cue", cue(note));
        add(attributes, "artic", MeiNotationConverter.getInstance().articulations(note.getNotations()));
        add(attributes, "fermata", MeiNotationConverter.getInstance().fermata(note.getNotations()));
    }


    /**
     * Gets the duration attributes of a note or rest: duration and dots.
     *
     * @param note the (first) note
     * @param event the event
     * @param inTuplet true inside a tuplet element
     * @return the attribute names and values
     */
    private String[] durationAttributes(INote note, MeiWriterEvent event, boolean inTuplet) {
        int dots = 0;
        if (note.getDots() != null) {
            dots = note.getDots().size();
        }
        String type = note.getType();
        Fraction nominal = event.getLength();
        if (note.getTimeModification() != null && note.getTimeModification().getActualNotes() != null
                && note.getTimeModification().getNormalNotes() != null && note.getTimeModification().getNormalNotes() > 0) {
            nominal = nominal.multiply(note.getTimeModification().getActualNotes(), note.getTimeModification().getNormalNotes());
        }
        Fraction typed = NoteValueConverter.getInstance().toFraction(type, dots);
        if (typed != null && nominal.isPositive() && !typed.equals(nominal) && NoteValueConverter.getInstance().fromFraction(nominal) != null) {
            // the note value does not match the length (for example a note of a tremolo): the length wins
            type = null;
        }
        return durationAttributes(type, dots, nominal);
    }


    /**
     * Gets the duration and dots attributes from a note type, or from the length if the type is unknown.
     *
     * @param type the MusicXML note type, or null
     * @param dots the dots
     * @param length the nominal length in whole notes
     * @return the attribute names and values
     */
    private String[] durationAttributes(String type, int dots, Fraction length) {
        String dur = MeiNotationConverter.getInstance().duration(type);
        int dotCount = dots;
        if (dur == null) {
            TypeAndDots value = NoteValueConverter.getInstance().fromFraction(length);
            if (value != null) {
                dur = MeiNotationConverter.getInstance().duration(value.getType());
                dotCount = value.getDots();
            }
        }
        if (dur == null) {
            dur = "4";
            dotCount = 0;
        }
        if (dotCount > 0) {
            return new String[] {DUR, dur, DOTS, String.valueOf(dotCount)};
        }
        return new String[] {DUR, dur};
    }


    /**
     * Writes the lyrics of a note: one verse per lyric with its syllable.
     *
     * @param out the output
     * @param note the note
     */
    private void writeLyrics(MeiXmlOutput out, INote note) {
        int index = 1;
        for (ILyric lyric : note.getLyrics()) {
            if (lyric.getText() == null) {
                index++;
                continue;
            }
            String number = String.valueOf(index);
            if (lyric.getNumber() != null && lyric.getNumber().matches("[0-9]+")) {
                number = lyric.getNumber();
            }
            out.open("verse", "n", number);
            List<String> attributes = new ArrayList<>();
            String syllabic = lyric.getSyllabic();
            if ("begin".equals(syllabic)) {
                add(attributes, "wordpos", "i");
            } else if ("middle".equals(syllabic)) {
                add(attributes, "wordpos", "m");
            } else if ("end".equals(syllabic)) {
                add(attributes, "wordpos", "t");
            }
            if (lyric.getExtend() != null) {
                add(attributes, "con", "u");
            } else if ("begin".equals(syllabic) || "middle".equals(syllabic)) {
                add(attributes, "con", "d");
            }
            out.openInline("syl", attributes.toArray(new String[0]));
            out.text(lyric.getText());
            out.closeInline("syl");
            out.close();
            index++;
        }
    }


    /**
     * Writes clef changes.
     *
     * @param out the output
     * @param clefs the clefs, or null
     */
    private void writeClefs(MeiXmlOutput out, List<IClef> clefs) {
        if (clefs == null) {
            return;
        }
        for (IClef clef : clefs) {
            out.empty("clef", MeiNotationConverter.getInstance().clefAttributes(clef, "").toArray(new String[0]));
        }
    }


    /**
     * Gets the tie attribute of a note: i (start), m (stop and start) or t (stop).
     *
     * @param note the note
     * @return the tie value, or null
     */
    private String tie(INote note) {
        boolean start = false;
        boolean stop = false;
        if (note.getTies() != null) {
            for (ITie tie : note.getTies()) {
                start = start || START.equals(tie.getType());
                stop = stop || STOP.equals(tie.getType());
            }
        }
        if (!start && !stop && note.getNotations() != null && note.getNotations().getTieds() != null) {
            for (ITied tied : note.getNotations().getTieds()) {
                start = start || START.equals(tied.getType());
                stop = stop || STOP.equals(tied.getType());
            }
        }
        if (start && stop) {
            return "m";
        }
        if (start) {
            return "i";
        }
        if (stop) {
            return "t";
        }
        return null;
    }


    /**
     * Gets the cue attribute of a note.
     *
     * @param note the note
     * @return true for a cue-size note, otherwise null
     */
    private String cue(INote note) {
        if ("cue".equals(note.getTypeSize())) {
            return TRUE;
        }
        return null;
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
     * Gets the tuplet marks of a note.
     *
     * @param note the note
     * @return the tuplets
     */
    private List<? extends ITuplet> tuplets(INote note) {
        if (note.getNotations() == null || note.getNotations().getTuplets() == null) {
            return new ArrayList<>();
        }
        return note.getNotations().getTuplets();
    }


    /**
     * Checks whether a note has lyrics with text.
     *
     * @param note the note
     * @return true if a lyric has text
     */
    private boolean hasLyrics(INote note) {
        if (note.getLyrics() == null) {
            return false;
        }
        for (ILyric lyric : note.getLyrics()) {
            if (lyric.getText() != null) {
                return true;
            }
        }
        return false;
    }


    /**
     * Adds an attribute if its value is set.
     *
     * @param attributes the attributes
     * @param name the name
     * @param value the value, or null
     */
    private void add(List<String> attributes, String name, String value) {
        if (value != null) {
            attributes.add(name);
            attributes.add(value);
        }
    }
}
