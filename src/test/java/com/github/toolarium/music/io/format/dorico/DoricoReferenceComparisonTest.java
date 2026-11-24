/*
 * DoricoReferenceComparisonTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.note.IPitch;
import com.github.toolarium.music.model.note.ITie;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Compares the Dorico reader with Dorico's own MusicXML export of the same project.
 *
 * <p>For every sample {@code <name>.dorico} the reference is the full score export of each flow in
 * the folder {@code <name>/} (files {@code <name> - Full score - <flow number> <flow title>.mxl}),
 * or {@code <name>.musicxml} or {@code <name>.mxl} in the same folder. Dorico exports one file per flow; such files are named
 * {@code <name>-<flow number>.musicxml} (1-based) and are joined in flow order. Projects without a
 * reference are skipped.</p>
 *
 * <p>The comparison is made on the sounding notes of each part: onset, MIDI pitch, length and
 * staff, with tied notes merged. It does not depend on how notes are split, beamed or assigned to
 * voices. Rests and grace notes are ignored.</p>
 *
 * @author patrick
 */
public class DoricoReferenceComparisonTest {
    private static final Path SAMPLE_DIR = TestResourceUtil.getInstance().getSamplesDir().resolve("dorico");
    private static final Pattern FLOW_FILE = Pattern.compile("-(\\d+)\\.(musicxml|mxl)$");
    private static final Pattern FULL_SCORE_FILE = Pattern.compile(" - Full score - (\\d+) .*\\.(musicxml|mxl)$");
    private static final String DORICO_EXTENSION = ".dorico";
    private static final String MUSICXML_EXTENSION = ".musicxml";
    private static final String MXL_EXTENSION = ".mxl";
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String DURATION = "duration";
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int MAX_REPORTED_DIFFERENCES = 10;
    private static final Map<String, Integer> STEP_SEMITONES = createStepSemitones();


    /**
     * Compares every sample project with its reference export.
     *
     * @throws Exception if reading fails
     */
    @Test
    void testCompareWithDoricoExport() throws Exception {
        List<Path> projects = new ArrayList<>();
        try (Stream<Path> files = Files.list(SAMPLE_DIR)) {
            for (Path project : (Iterable<Path>) files.filter(p -> p.toString().endsWith(DORICO_EXTENSION)).sorted()::iterator) {
                if (!findReferences(project).isEmpty()) {
                    projects.add(project);
                }
            }
        }
        List<String> failures = TestResourceUtil.getInstance().processFiles(projects, project -> {
            List<List<SoundingNote>> expected = readReference(findReferences(project));
            List<List<SoundingNote>> actual = readConverted(MusicFileFactory.getInstance().read(project));
            return compare(project.getFileName().toString(), expected, actual);
        });

        assumeTrue(projects.size() > 0, "No Dorico MusicXML exports (<name>.musicxml) found next to the samples.");
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }


    /**
     * Tests the reference reader and the comparison with a small MusicXML file: tied notes are
     * merged, chords share their onset, backup and forward move the position.
     *
     * @throws Exception if reading fails
     */
    @Test
    void testReferenceReader() throws Exception {
        String xml = "<score-partwise><part id=\"P1\">"
                + "<measure number=\"1\"><attributes><divisions>2</divisions></attributes>"
                + note("C", 4, 4, START) + note("E", 4, 4, null).replace("<pitch>", "<chord/><pitch>")
                + "<backup><duration>4</duration></backup>"
                + "<forward><duration>2</duration></forward>"
                + note("G", 3, 2, null)
                + "</measure><measure number=\"2\">"
                + note("C", 4, 2, STOP) + "<note><grace/><pitch><step>D</step><octave>4</octave></pitch><type>16th</type></note>"
                + note("D", 4, 2, null)
                + "</measure></part></score-partwise>";

        List<List<SoundingNote>> parts = readReferenceDocument(parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))), Fraction.ZERO);
        assertEquals(1, parts.size());
        List<SoundingNote> notes = parts.get(0);
        Collections.sort(notes);

        // C is tied over the barline (2 + 1 quarters), G starts after the forward, the grace note is ignored
        assertEquals("[0/1 C60 3/1 s1, 0/1 E64 2/1 s1, 1/1 G55 1/1 s1, 3/1 D62 1/1 s1]", notes.toString());
    }


    /**
     * Finds the reference exports of a project.
     *
     * @param project the .dorico file
     * @return the reference files in flow order (may be empty)
     * @throws IOException if the folder cannot be listed
     */
    private List<Path> findReferences(Path project) throws IOException {
        String name = project.getFileName().toString();
        String base = name.substring(0, name.length() - DORICO_EXTENSION.length());
        Path folder = project.resolveSibling(base);
        if (Files.isDirectory(folder)) {
            Map<Integer, Path> fullScore = new HashMap<>();
            try (Stream<Path> files = Files.list(folder)) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    String fileName = file.getFileName().toString();
                    Matcher matcher = FULL_SCORE_FILE.matcher(fileName);
                    if (fileName.startsWith(base + " - ") && matcher.find()) {
                        fullScore.put(Integer.parseInt(matcher.group(1)), file);
                    }
                }
            }
            if (!fullScore.isEmpty()) {
                List<Path> result = new ArrayList<>();
                for (int flow = 1; fullScore.containsKey(flow); flow++) {
                    result.add(fullScore.get(flow));
                }
                return result;
            }
        }
        for (String extension : new String[] {MUSICXML_EXTENSION, MXL_EXTENSION}) {
            Path single = project.resolveSibling(base + extension);
            if (Files.exists(single)) {
                return Collections.singletonList(single);
            }
        }

        Map<Integer, Path> flows = new HashMap<>();
        try (Stream<Path> files = Files.list(project.getParent())) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String fileName = file.getFileName().toString();
                Matcher matcher = FLOW_FILE.matcher(fileName);
                if (fileName.startsWith(base + "-") && matcher.find() && matcher.start() == base.length()) {
                    flows.put(Integer.parseInt(matcher.group(1)), file);
                }
            }
        }
        List<Path> result = new ArrayList<>();
        for (int flow = 1; flows.containsKey(flow); flow++) {
            result.add(flows.get(flow));
        }
        return result;
    }


    /**
     * Reads the sounding notes of the reference exports; the flows are joined one after another.
     *
     * @param references the reference files in flow order
     * @return the sounding notes per part
     * @throws Exception if reading fails
     */
    private List<List<SoundingNote>> readReference(List<Path> references) throws Exception {
        List<List<SoundingNote>> result = new ArrayList<>();
        Fraction offset = Fraction.ZERO;
        for (Path reference : references) {
            Document document;
            try (InputStream in = openReference(reference)) {
                document = parse(in);
            }
            List<List<SoundingNote>> flow = readReferenceDocument(document, offset);
            Fraction flowEnd = offset;
            for (int p = 0; p < flow.size(); p++) {
                while (result.size() <= p) {
                    result.add(new ArrayList<>());
                }
                result.get(p).addAll(flow.get(p));
            }
            for (List<SoundingNote> notes : flow) {
                for (SoundingNote note : notes) {
                    flowEnd = flowEnd.max(note.getEnd());
                }
            }
            offset = flowEnd.max(offset.add(measureTotal(document)));
        }
        return result;
    }


    /**
     * Gets the total length of all measures of the first part of a document, in quarter notes.
     *
     * @param document the MusicXML document
     * @return the length
     */
    private Fraction measureTotal(Document document) {
        NodeList parts = document.getElementsByTagName("part");
        if (parts.getLength() == 0) {
            return Fraction.ZERO;
        }
        Fraction total = Fraction.ZERO;
        int divisions = 1;
        for (Element measure : children((Element) parts.item(0), "measure")) {
            int position = 0;
            int maximum = 0;
            for (Element child : children(measure, null)) {
                divisions = readDivisions(child, divisions);
                position = advance(child, position);
                maximum = Math.max(maximum, position);
            }
            total = total.add(Fraction.of(maximum, divisions));
        }
        return total;
    }


    /**
     * Reads the sounding notes of a MusicXML document in document order (backup and forward included).
     *
     * @param document the MusicXML document
     * @param offset the start of the document in the joined flows, in quarter notes
     * @return the sounding notes per part
     */
    private List<List<SoundingNote>> readReferenceDocument(Document document, Fraction offset) {
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
            Fraction measureStart = offset;
            for (Element measure : children(part, "measure")) {
                int position = 0;
                int maximum = 0;
                int lastOnset = 0;
                for (Element child : children(measure, null)) {
                    divisions = readDivisions(child, divisions);
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
                            int midi = midi(text(first(pitch, "step")), doubleValue(first(pitch, "alter")), intValue(first(pitch, "octave")));
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
     * Reads the sounding notes of a converted score. The converter starts every voice at the start
     * of the bar and lists the notes of a voice in order, so the onsets follow from the durations.
     *
     * @param score the converted score
     * @return the sounding notes per part
     */
    private List<List<SoundingNote>> readConverted(IScore score) {
        List<List<SoundingNote>> result = new ArrayList<>();
        for (IPart part : score.getParts()) {
            List<SoundingNote> notes = new ArrayList<>();
            Map<String, SoundingNote> openTies = new HashMap<>();
            int divisions = 1;
            Fraction measureStart = Fraction.ZERO;
            for (IMeasure measure : part.getMeasures()) {
                for (IAttributes attributes : nonNull(measure.getAttributes())) {
                    if (attributes.getDivisions() != null) {
                        divisions = attributes.getDivisions();
                    }
                }

                Map<Integer, Integer> voicePositions = new LinkedHashMap<>();
                int lastOnset = 0;
                int maximum = 0;
                for (INote note : measure.getNotes()) {
                    if (note.getGrace() != null) {
                        continue;
                    }
                    int position = voicePositions.getOrDefault(note.getVoice(), 0);
                    int onset = position;
                    if (note.getChord() != null) {
                        onset = lastOnset;
                    } else {
                        position += note.getDuration();
                        voicePositions.put(note.getVoice(), position);
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
                                midi(pitch.getStep(), alter, pitch.getOctave()), Fraction.of(note.getDuration(), divisions), staff),
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
     * Compares the sounding notes of the reference and the converted score.
     *
     * @param name the project name
     * @param expected the reference notes per part
     * @param actual the converted notes per part
     * @return a description of the differences, or null if both are equal
     */
    private String compare(String name, List<List<SoundingNote>> expected, List<List<SoundingNote>> actual) {
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
    private Document parse(InputStream in) throws Exception {
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
     * Creates a MusicXML note element for the reference reader test.
     *
     * @param step the step
     * @param octave the octave
     * @param duration the duration in divisions
     * @param tie the tie type, or null
     * @return the note element
     */
    private static String note(String step, int octave, int duration, String tie) {
        String tieElement = "";
        if (tie != null) {
            tieElement = "<tie type=\"" + tie + "\"/>";
        }
        return "<note><pitch><step>" + step + "</step><octave>" + octave + "</octave></pitch><duration>" + duration
                + "</duration>" + tieElement + "<voice>1</voice></note>";
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
    private static final class SoundingNote implements Comparable<SoundingNote> {
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
        Fraction getOnset() {
            return onset;
        }


        /**
         * Gets the MIDI pitch.
         *
         * @return the pitch
         */
        int getMidi() {
            return midi;
        }


        /**
         * Gets the length.
         *
         * @return the length in quarter notes
         */
        Fraction getLength() {
            return length;
        }


        /**
         * Gets the end.
         *
         * @return the end in quarter notes
         */
        Fraction getEnd() {
            return onset.add(length);
        }


        /**
         * Gets the staff.
         *
         * @return the staff
         */
        int getStaff() {
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
