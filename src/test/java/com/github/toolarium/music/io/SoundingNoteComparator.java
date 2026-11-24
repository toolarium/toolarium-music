/*
 * SoundingNoteComparator.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IBackup;
import com.github.toolarium.music.model.measure.IForward;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.measure.ITranspose;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.note.IPitch;
import com.github.toolarium.music.model.note.ITie;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Test helper that compares the sounding notes of two scores: the converted score of a reader and
 * a reference MusicXML file written by the original application.
 *
 * <p>For each part the comparison uses the onset, sounding MIDI pitch (written pitch plus the
 * transposition of the part), length and staff of every note, with tied notes merged. It does not depend on how notes are split, beamed or assigned to voices. Rests
 * and grace notes are ignored. The reference is read in document order (backup and forward
 * included); the converted score is read voice by voice, every voice starting at the measure start.</p>
 *
 * @author patrick
 */
public final class SoundingNoteComparator {
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String DURATION = "duration";
    private static final String MXL_EXTENSION = ".mxl";
    private static final String MUSICXML_EXTENSION = ".musicxml";
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final Fraction ROUNDING_TOLERANCE = Fraction.of(1, 64);
    private static final int MAX_REPORTED_DIFFERENCES = 10;
    private static final Map<String, Integer> STEP_SEMITONES = createStepSemitones();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final SoundingNoteComparator INSTANCE = new SoundingNoteComparator();
    }


    /**
     * Constructor.
     */
    private SoundingNoteComparator() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static SoundingNoteComparator getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Reads the sounding notes of a reference MusicXML file (.musicxml or .mxl).
     *
     * @param reference the reference file
     * @return the sounding notes per part
     * @throws Exception if reading fails
     */
    public List<List<SoundingNote>> readReference(Path reference) throws Exception {
        try (InputStream in = openReference(reference)) {
            return readReferenceDocument(parse(in), Fraction.ZERO);
        }
    }


    /**
     * Reads the sounding notes of a MusicXML document in document order (backup and forward included).
     *
     * @param document the MusicXML document
     * @param offset the start of the document in the joined flows, in quarter notes
     * @return the sounding notes per part
     */
    public List<List<SoundingNote>> readReferenceDocument(Document document, Fraction offset) {
        List<List<SoundingNote>> result = new ArrayList<>();
        NodeList parts = document.getElementsByTagName("part");
        for (int p = 0; p < parts.getLength(); p++) {
            Element part = (Element) parts.item(p);
            if (!"score-partwise".equals(((Element) part.getParentNode()).getTagName())) {
                continue;
            }
            List<SoundingNote> notes = new ArrayList<>();
            Map<String, SoundingNote> openTies = new HashMap<>();
            int divisions = 1;
            int transpose = 0;
            Fraction measureStart = offset;
            for (Element measure : children(part, "measure")) {
                int position = 0;
                int maximum = 0;
                int lastOnset = 0;
                for (Element child : children(measure, null)) {
                    divisions = readDivisions(child, divisions);
                    transpose = readTranspose(child, transpose);
                    if ("note".equals(child.getTagName()) && first(child, "grace") == null) {
                        int duration = intValue(first(child, DURATION));
                        int onset = position;
                        if (first(child, "chord") != null) {
                            onset = lastOnset;
                        } else {
                            position += duration;
                        }
                        lastOnset = onset;
                        Element pitch = first(child, "pitch");
                        if (pitch != null) {
                            int midi = midi(text(first(pitch, "step")), doubleValue(first(pitch, "alter")), intValue(first(pitch, "octave"))) + transpose;
                            int staff = Math.max(1, intValue(first(child, "staff")));
                            List<String> ties = new ArrayList<>();
                            for (Element tie : children(child, "tie")) {
                                ties.add(tie.getAttribute("type"));
                            }
                            addNote(notes, openTies, new SoundingNote(measureStart.add(Fraction.of(onset, divisions)), midi,
                                    Fraction.of(duration, divisions), staff), ties.contains(STOP), ties.contains(START));
                        }
                    } else {
                        position = advance(child, position);
                    }
                    maximum = Math.max(maximum, position);
                }
                measureStart = measureStart.add(Fraction.of(maximum, divisions));
            }
            result.add(notes);
        }
        return result;
    }


    /**
     * Reads the sounding notes of a score: the measure content is processed in document order, a note
     * advances the time by its duration (chord notes start with the previous note), a backup moves back
     * and a forward moves on.
     *
     * @param score the score
     * @return the sounding notes per part
     */
    public List<List<SoundingNote>> readConverted(IScore score) {
        List<List<SoundingNote>> result = new ArrayList<>();
        for (IPart part : score.getParts()) {
            List<SoundingNote> notes = new ArrayList<>();
            Map<String, SoundingNote> openTies = new HashMap<>();
            int divisions = 1;
            int transpose = 0;
            Fraction measureStart = Fraction.ZERO;
            for (IMeasure measure : part.getMeasures()) {
                for (IAttributes attributes : nonNull(measure.getAttributes())) {
                    if (attributes.getDivisions() != null) {
                        divisions = attributes.getDivisions();
                    }
                    for (ITranspose value : nonNull(attributes.getTransposes())) {
                        transpose = value.getChromatic() + octaves(value.getOctaveChange());
                    }
                }

                int position = 0;
                int lastOnset = 0;
                int maximum = 0;
                for (IMusicDataElement element : measure.getMusicData()) {
                    if (element instanceof IBackup) {
                        position -= intValue(((IBackup) element).getDuration());
                        continue;
                    }
                    if (element instanceof IForward) {
                        position += intValue(((IForward) element).getDuration());
                        maximum = Math.max(maximum, position);
                        continue;
                    }
                    if (!(element instanceof INote) || ((INote) element).getGrace() != null) {
                        continue;
                    }
                    INote note = (INote) element;
                    int onset = position;
                    if (note.getChord() != null) {
                        onset = lastOnset;
                    } else {
                        position += intValue(note.getDuration());
                    }
                    lastOnset = onset;
                    maximum = Math.max(maximum, position);

                    IPitch pitch = note.getPitch();
                    if (pitch != null && note.getRest() == null) {
                        double alter = 0;
                        if (pitch.getAlter() != null) {
                            alter = pitch.getAlter();
                        }
                        int staff = 1;
                        if (note.getStaff() != null) {
                            staff = note.getStaff();
                        }
                        List<String> ties = new ArrayList<>();
                        for (ITie tie : nonNull(note.getTies())) {
                            ties.add(tie.getType());
                        }
                        addNote(notes, openTies, new SoundingNote(measureStart.add(Fraction.of(onset, divisions)),
                                midi(pitch.getStep(), alter, pitch.getOctave()) + transpose, Fraction.of(note.getDuration(), divisions), staff),
                                ties.contains(STOP), ties.contains(START));
                    }
                }
                measureStart = measureStart.add(Fraction.of(maximum, divisions));
            }
            result.add(notes);
        }
        return result;
    }


    /**
     * Adds a note, merging it into the open tied note with the same pitch and staff.
     *
     * @param notes the notes
     * @param openTies the open tied notes by staff and pitch
     * @param note the note
     * @param tieStop true if the note is tied from a previous note
     * @param tieStart true if the note is tied to a following note
     */
    private void addNote(List<SoundingNote> notes, Map<String, SoundingNote> openTies, SoundingNote note, boolean tieStop, boolean tieStart) {
        String key = note.getStaff() + ":" + note.getMidi();
        SoundingNote target = note;
        SoundingNote open = openTies.remove(key);
        if (tieStop && open != null && open.getEnd().equals(note.getOnset())) {
            open.extend(note.getLength());
            target = open;
        } else {
            notes.add(note);
        }
        if (tieStart) {
            openTies.put(key, target);
        }
    }


    /**
     * Removes the notes that start at or after a position (to compare only the beginning of a score).
     *
     * @param parts the sounding notes per part
     * @param until the position in quarter notes
     * @return the notes before the position
     */
    public List<List<SoundingNote>> limit(List<List<SoundingNote>> parts, Fraction until) {
        List<List<SoundingNote>> result = new ArrayList<>();
        for (List<SoundingNote> notes : parts) {
            List<SoundingNote> kept = new ArrayList<>();
            for (SoundingNote note : notes) {
                if (note.getOnset().compareTo(until) < 0) {
                    kept.add(note);
                }
            }
            result.add(kept);
        }
        return result;
    }


    /**
     * Compares the sounding notes of the reference and the converted score.
     *
     * @param name the project name
     * @param expected the reference notes per part
     * @param actual the converted notes per part
     * @return a description of the differences, or null if both are equal
     */
    public String compare(String name, List<List<SoundingNote>> expected, List<List<SoundingNote>> actual) {
        if (expected.size() != actual.size()) {
            return name + ": " + expected.size() + " parts expected, " + actual.size() + " converted";
        }

        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < expected.size(); p++) {
            List<SoundingNote> missing = new ArrayList<>(expected.get(p));
            List<SoundingNote> extra = new ArrayList<>(actual.get(p));
            List<SoundingNote> matched = new ArrayList<>(missing);
            matched.retainAll(extra);
            for (SoundingNote note : matched) {
                missing.remove(note);
                extra.remove(note);
            }
            removeNearMatches(missing, extra);
            if (!missing.isEmpty() || !extra.isEmpty()) {
                Collections.sort(missing);
                Collections.sort(extra);
                sb.append(name).append(" part ").append(p + 1).append(": ").append(missing.size()).append(" notes missing ")
                        .append(head(missing)).append(", ").append(extra.size()).append(" notes not in the export ").append(head(extra)).append('\n');
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString().trim();
    }


    /**
     * Removes the notes that match except for a rounding difference: same pitch and staff, onset and
     * length differ by at most a 64th of a quarter note. Exports with too few divisions for tuplets
     * round the tuplet durations (683/1024 instead of 2/3 of a quarter note).
     *
     * @param missing the expected notes without exact match (updated)
     * @param extra the converted notes without exact match (updated)
     */
    private void removeNearMatches(List<SoundingNote> missing, List<SoundingNote> extra) {
        Iterator<SoundingNote> iterator = missing.iterator();
        while (iterator.hasNext()) {
            SoundingNote expected = iterator.next();
            for (SoundingNote actual : extra) {
                if (actual.getMidi() == expected.getMidi() && actual.getStaff() == expected.getStaff()
                        && near(actual.getOnset(), expected.getOnset()) && near(actual.getLength(), expected.getLength())) {
                    extra.remove(actual);
                    iterator.remove();
                    break;
                }
            }
        }
    }


    /**
     * Checks if two values differ by at most the rounding tolerance.
     *
     * @param a the first value in quarter notes
     * @param b the second value in quarter notes
     * @return true if near
     */
    private boolean near(Fraction a, Fraction b) {
        Fraction difference = a.subtract(b);
        if (difference.isNegative()) {
            difference = difference.negate();
        }
        return difference.compareTo(ROUNDING_TOLERANCE) <= 0;
    }


    /**
     * Gets the first differences of a list.
     *
     * @param notes the notes
     * @return the first notes as text
     */
    private String head(List<SoundingNote> notes) {
        return notes.subList(0, Math.min(notes.size(), MAX_REPORTED_DIFFERENCES)).toString();
    }


    /**
     * Opens a reference file: plain MusicXML or the root file of an MXL archive.
     *
     * @param reference the reference file
     * @return the MusicXML content
     * @throws IOException if the file cannot be read
     */
    private InputStream openReference(Path reference) throws IOException {
        if (!reference.toString().endsWith(MXL_EXTENSION)) {
            return Files.newInputStream(reference);
        }
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(reference))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (!entryName.startsWith("META-INF") && (entryName.endsWith(".xml") || entryName.endsWith(MUSICXML_EXTENSION))) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    zip.transferTo(bytes);
                    return new ByteArrayInputStream(bytes.toByteArray());
                }
            }
        }
        throw new IOException("No MusicXML file in " + reference);
    }


    /**
     * Parses a MusicXML document without loading the DTD.
     *
     * @param in the content
     * @return the document
     * @throws Exception if parsing fails
     */
    public Document parse(InputStream in) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(in);
    }


    /**
     * Reads the divisions of an attributes element.
     *
     * @param element the element
     * @param divisions the current divisions
     * @return the new divisions
     */
    private int readDivisions(Element element, int divisions) {
        if ("attributes".equals(element.getTagName()) && first(element, "divisions") != null) {
            return intValue(first(element, "divisions"));
        }
        return divisions;
    }


    /**
     * Reads the transposition of an attributes element (written to sounding pitch in semitones).
     *
     * @param element the element
     * @param transpose the current transposition
     * @return the new transposition
     */
    private int readTranspose(Element element, int transpose) {
        Element value = null;
        if ("attributes".equals(element.getTagName())) {
            value = first(element, "transpose");
        }
        if (value == null) {
            return transpose;
        }
        return intValue(first(value, "chromatic")) + SEMITONES_PER_OCTAVE * intValue(first(value, "octave-change"));
    }


    /**
     * Converts an octave change into semitones.
     *
     * @param octaveChange the octave change (may be null)
     * @return the semitones
     */
    private int octaves(Integer octaveChange) {
        if (octaveChange == null) {
            return 0;
        }
        return SEMITONES_PER_OCTAVE * octaveChange;
    }


    /**
     * Moves the position for a backup, forward or note element (chord notes and grace notes do not move it).
     *
     * @param element the element
     * @param position the current position in divisions
     * @return the new position
     */
    private int advance(Element element, int position) {
        String tag = element.getTagName();
        if ("backup".equals(tag)) {
            return position - intValue(first(element, DURATION));
        }
        if ("forward".equals(tag)) {
            return position + intValue(first(element, DURATION));
        }
        if ("note".equals(tag) && first(element, "chord") == null && first(element, "grace") == null) {
            return position + intValue(first(element, DURATION));
        }
        return position;
    }


    /**
     * Gets the child elements with a name.
     *
     * @param parent the parent element
     * @param name the name, or null for all child elements
     * @return the child elements
     */
    private List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && (name == null || name.equals(((Element) node).getTagName()))) {
                result.add((Element) node);
            }
        }
        return result;
    }


    /**
     * Gets the first child element with a name.
     *
     * @param parent the parent element
     * @param name the name
     * @return the element, or null
     */
    private Element first(Element parent, String name) {
        List<Element> result = children(parent, name);
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }


    /**
     * Gets the trimmed text of an element.
     *
     * @param element the element (may be null)
     * @return the text, or an empty string
     */
    private String text(Element element) {
        if (element == null) {
            return "";
        }
        return element.getTextContent().trim();
    }


    /**
     * Gets an integer value, 0 if it is not set.
     *
     * @param value the value
     * @return the value, or 0
     */
    private int intValue(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }


    /**
     * Gets the integer value of an element.
     *
     * @param element the element (may be null)
     * @return the value, or 0
     */
    private int intValue(Element element) {
        String text = text(element);
        if (text.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(text);
    }


    /**
     * Gets the decimal value of an element.
     *
     * @param element the element (may be null)
     * @return the value, or 0
     */
    private double doubleValue(Element element) {
        String text = text(element);
        if (text.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(text);
    }


    /**
     * Calculates the MIDI pitch of a written pitch.
     *
     * @param step the step
     * @param alter the alteration in semitones
     * @param octave the octave
     * @return the MIDI pitch
     */
    private int midi(String step, double alter, int octave) {
        return (octave + 1) * SEMITONES_PER_OCTAVE + STEP_SEMITONES.getOrDefault(step, 0) + (int) Math.round(alter);
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
     * Creates the semitones of the natural steps.
     *
     * @return the map
     */
    private static Map<String, Integer> createStepSemitones() {
        Map<String, Integer> map = new HashMap<>();
        map.put("C", 0);
        map.put("D", 2);
        map.put("E", 4);
        map.put("F", 5);
        map.put("G", 7);
        map.put("A", 9);
        map.put("B", 11);
        return map;
    }


    /**
     * A sounding note: onset, MIDI pitch, length and staff.
     *
     * @author patrick
     */
    public static final class SoundingNote implements Comparable<SoundingNote> {
        private final Fraction onset;
        private final int midi;
        private Fraction length;
        private final int staff;


        /**
         * Constructor for SoundingNote.
         *
         * @param onset the onset in quarter notes
         * @param midi the MIDI pitch
         * @param length the length in quarter notes
         * @param staff the staff
         */
        SoundingNote(Fraction onset, int midi, Fraction length, int staff) {
            this.onset = onset;
            this.midi = midi;
            this.length = length;
            this.staff = staff;
        }


        /**
         * Gets the onset.
         *
         * @return the onset in quarter notes
         */
        public Fraction getOnset() {
            return onset;
        }


        /**
         * Gets the MIDI pitch.
         *
         * @return the pitch
         */
        public int getMidi() {
            return midi;
        }


        /**
         * Gets the length.
         *
         * @return the length in quarter notes
         */
        public Fraction getLength() {
            return length;
        }


        /**
         * Gets the end.
         *
         * @return the end in quarter notes
         */
        public Fraction getEnd() {
            return onset.add(length);
        }


        /**
         * Gets the staff.
         *
         * @return the staff
         */
        public int getStaff() {
            return staff;
        }


        /**
         * Extends the note by a tied note.
         *
         * @param additional the length of the tied note
         */
        void extend(Fraction additional) {
            length = length.add(additional);
        }


        /**
         * Orders notes by onset, staff and pitch.
         *
         * @param other the other note
         * @return a negative, zero or positive value
         */
        @Override
        public int compareTo(SoundingNote other) {
            int result = onset.compareTo(other.onset);
            if (result == 0) {
                result = Integer.compare(staff, other.staff);
            }
            if (result == 0) {
                result = Integer.compare(midi, other.midi);
            }
            if (result == 0) {
                result = length.compareTo(other.length);
            }
            return result;
        }


        /**
         * Checks equality of all values.
         *
         * @param obj the other object
         * @return true if both notes are equal
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SoundingNote)) {
                return false;
            }
            SoundingNote other = (SoundingNote) obj;
            return compareTo(other) == 0;
        }


        /**
         * Calculates the hash code.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return onset.hashCode() * 31 * 31 + midi * 31 + staff;
        }


        /**
         * Describes the note, for example "3/2 C#61 1/2 s1" (onset, pitch name and MIDI pitch, length, staff).
         *
         * @return the description
         */
        @Override
        public String toString() {
            String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
            return onset + " " + names[Math.floorMod(midi, SEMITONES_PER_OCTAVE)] + midi + " " + length + " s" + staff;
        }
    }
}
