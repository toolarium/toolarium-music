/*
 * DoricoReaderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.model.barline.IBarline;
import com.github.toolarium.music.model.direction.IDirection;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.direction.IWords;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IClef;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.notation.IDynamics;
import com.github.toolarium.music.model.notation.ISlur;
import com.github.toolarium.music.model.notation.ITuplet;
import com.github.toolarium.music.model.note.ILyric;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.note.ITie;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;


/**
 * Tests for the Dorico (.dorico) reader.
 *
 * @author patrick
 */
public class DoricoReaderTest {
    private static final Path SAMPLE_DIR = TestResourceUtil.getInstance().getSamplesDir().resolve("dorico");
    private static final String CONTAINER_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
            + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\"><rootfiles>"
            + "<rootfile full-path=\"score.xml\" media-type=\"application/vnd.steinberg.dorico-project\"/>"
            + "</rootfiles></container>";
    private static final String FOUR = "4";
    private static final String ONE = "1";
    private static final String START = "start";
    private static final String EMPTY = "";
    private static final int SAMPLE_COUNT = 7;
    private static final int BAND_PARTS = 31;


    /**
     * Tests that the factory returns a reader and that the format is detected by extension.
     */
    @Test
    void testFactoryAndFormatDetection() {
        assertNotNull(DoricoIOFactory.getInstance().getReader());
        assertEquals(MusicFileFormat.DORICO, MusicFileFormat.fromFileName("Test.Dorico"));
    }


    /**
     * Tests reading a synthetic project: metadata, parts, staves, clefs, transposition,
     * key and time changes and the bar grid.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadSyntheticProject() throws IOException {
        byte[] project = createProject(syntheticScore());
        IScore score = MusicFileFactory.getInstance().read(new ByteArrayInputStream(project), MusicFileFormat.DORICO);

        assertEquals("Synthetic", score.getMovementTitle());
        assertEquals("Synthetic", score.getWork().getWorkTitle());
        assertEquals("composer", score.getIdentification().getCreators().get(0).getType());
        assertEquals("J. Doe", score.getIdentification().getCreators().get(0).getValue());
        assertEquals("(c) Test", score.getIdentification().getRights());
        assertEquals("2022-01-10", score.getIdentification().getEncoding().getEncodingDate());

        assertEquals(2, score.getParts().size());
        assertEquals("Piano", score.getPartList().getScoreParts().get(0).getPartName());
        assertEquals("Tpt", score.getPartList().getScoreParts().get(1).getPartAbbreviation());

        // 3/4 for 6 quarters (2 bars), then 4/4 up to the minimum length of 14 quarters (2 bars)
        IPart piano = score.getParts().get(0);
        assertEquals(4, piano.getMeasures().size());

        IAttributes first = piano.getMeasures().get(0).getAttributes().get(0);
        assertEquals(2, first.getStaves());
        assertEquals(1, first.getDivisions());
        assertEquals("3", first.getTimes().get(0).getBeats());
        assertEquals(FOUR, first.getTimes().get(0).getBeatType());
        assertEquals(-3, first.getKeys().get(0).getFifths());
        assertEquals("minor", first.getKeys().get(0).getMode());
        assertEquals("G", first.getClefs().get(0).getSign());
        assertEquals("F", first.getClefs().get(1).getSign());
        assertEquals(4, first.getClefs().get(1).getLine());
        assertEquals(2, piano.getMeasures().get(0).getNotes().size());
        assertEquals(3, piano.getMeasures().get(0).getNotes().get(0).getDuration());

        IMeasure third = piano.getMeasures().get(2);
        assertEquals(FOUR, third.getAttributes().get(0).getTimes().get(0).getBeats());
        assertEquals(2, third.getAttributes().get(0).getKeys().get(0).getFifths());
        assertEquals(4, third.getNotes().get(0).getDuration());
        assertTrue(piano.getMeasures().get(1).getAttributes().isEmpty());
        assertEquals("light-heavy", piano.getMeasures().get(3).getBarlines().get(0).getBarStyle());

        IAttributes trumpet = score.getParts().get(1).getMeasures().get(0).getAttributes().get(0);
        assertNull(trumpet.getStaves());
        assertEquals(-2, trumpet.getTransposes().get(0).getChromatic());
        assertEquals(-1, trumpet.getTransposes().get(0).getDiatonic());
    }


    /**
     * Tests that a ZIP without a score tree and non-ZIP data are rejected.
     *
     * @throws IOException if the test data cannot be created
     */
    @Test
    void testRejectInvalidInput() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("readme.txt"));
            zip.write("no score".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        IMusicReader reader = DoricoIOFactory.getInstance().getReader();
        assertThrows(IOException.class, () -> reader.read(new ByteArrayInputStream(bytes.toByteArray())));
        assertThrows(IOException.class, () -> reader.read(new ByteArrayInputStream("not a zip".getBytes(StandardCharsets.UTF_8))));

        byte[] wrongRoot = createProject("<kScoreLibrary><fileVersion>1</fileVersion></kScoreLibrary>");
        assertThrows(IOException.class, () -> reader.read(new ByteArrayInputStream(wrongRoot)));
    }


    /**
     * Tests reading all Dorico sample projects (binary score.dtn) and checks the consistency of the
     * converted music: every voice fills its bar exactly, ties are closed, tuplets do not nest and
     * the score survives a MusicXML round trip.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadSampleProjects() throws IOException {
        List<Path> projects;
        try (Stream<Path> files = Files.list(SAMPLE_DIR)) {
            projects = files.filter(p -> p.toString().endsWith(".dorico")).collect(Collectors.toList());
        }
        List<String> failures = TestResourceUtil.getInstance().processFiles(projects, path -> {
            IScore score = MusicFileFactory.getInstance().read(path);
            assertNotNull(score.getMovementTitle(), path.toString());
            assertTrue(score.getParts().size() > 0, path.toString());
            for (IPart part : score.getParts()) {
                assertTrue(part.getMeasures().size() > 0, path.toString());
                assertConsistent(part, path.getFileName() + " " + part.getId());
            }

            // the converted score survives a MusicXML write and read cycle unchanged
            ByteArrayOutputStream xml = new ByteArrayOutputStream();
            MusicFileFactory.getInstance().write(score, xml, MusicFileFormat.MUSIC_XML);
            IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(xml.toByteArray()), MusicFileFormat.MUSIC_XML);
            assertEquals(score, readBack, path.getFileName() + ": MusicXML round trip");
            return null;
        });
        assertTrue(failures.isEmpty(), String.join(System.lineSeparator(), failures));
        assertEquals(SAMPLE_COUNT, projects.size());
    }


    /**
     * Tests Mozart's sonata K. 331: nine flows in one part, 6/8 in A major, grace notes, dynamics,
     * repeat barlines and the flow titles.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadMozartSonata() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(SAMPLE_DIR.resolve("Mozart_Sonata_K331.dorico"));
        assertEquals("Klaviersonate", score.getMovementTitle());
        assertEquals("Wolfgang Amadeus Mozart", score.getIdentification().getCreators().get(0).getValue());
        assertEquals(1, score.getParts().size());

        IPart part = score.getParts().get(0);
        assertEquals(377, part.getMeasures().size(), "the bars of all nine flows, including the pick-up bars of the rondo");
        IAttributes attributes = part.getMeasures().get(0).getAttributes().get(0);
        assertEquals(2, attributes.getStaves());
        assertEquals(3, attributes.getKeys().get(0).getFifths());
        assertEquals("6", attributes.getTimes().get(0).getBeats());
        assertEquals("8", attributes.getTimes().get(0).getBeatType());

        // the theme as printed: the second beat in 6/8 is a quarter and an eighth
        assertEquals("|v1 C#5/eighth.( D5/16th) C#5/eighth E5/quarter E5/eighth |v5 E4/quarter E4/eighth E4/quarter E4/eighth "
                + "|v6 A3/eighth. B3/16th A3/eighth C#4/quarter C#4/eighth", describe(part.getMeasures().get(0)));
        assertEquals("|v1 A4/quarter A4/eighth B4/quarter B4/eighth |v5 E4/quarter E4/eighth E4/quarter E4/eighth "
                + "|v6 F#3/quarter F#3/eighth G#3/quarter G#3/eighth", describe(part.getMeasures().get(2)));

        List<String> words = wordsOf(part.getMeasures().get(0));
        assertTrue(words.contains("Andante grazioso"), words.toString());
        assertTrue(words.contains("Thema"), "flow title at the start of the flow");
        assertEquals(EMPTY, dynamicsOf(part.getMeasures().get(0)).get(0).getPiano());
        assertEquals(EMPTY, dynamicsOf(part.getMeasures().get(6)).get(0).getSfz());

        // repeat at the end of the first half of the theme, grace notes in the second half
        assertEquals("backward", part.getMeasures().get(7).getBarlines().get(0).getRepeat().getDirection());
        assertEquals("forward", part.getMeasures().get(8).getBarlines().get(0).getRepeat().getDirection());
        assertEquals("|v1 gF#5/16th gG#5/16th A5/eighth. G#5/16th F#5/eighth F#5/eighth( E5/eighth) E5/eighth "
                + "|v5 A3/eighth D4/eighth F#4/eighth A3/eighth C#4/eighth E4/eighth", describe(part.getMeasures().get(9)));
    }


    /**
     * Tests Debussy's Clair de lune: 9/8 with duplets, dynamics, staff text and clef changes.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadClairDeLune() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(SAMPLE_DIR.resolve("Debussy_Clair_de_lune.dorico"));
        IPart part = score.getParts().get(0);
        IMeasure first = part.getMeasures().get(0);
        assertEquals("9", first.getAttributes().get(0).getTimes().get(0).getBeats());
        assertEquals(-5, first.getAttributes().get(0).getKeys().get(0).getFifths());
        assertEquals(EMPTY, dynamicsOf(first).get(0).getPp());
        List<String> words = wordsOf(first);
        assertTrue(words.contains("con sordina"), words.toString());

        // duplets in the upper voice, dotted values in compound meter in the other voices
        assertEquals("|v1 C5/eighth) +Eb5/eighth Bb4/eighth( +Db5/eighth C5/eighth +Eb5/eighth Db5/eighth{2:3}< F5/eighth{2:3}~> "
                + "F5/eighth{2:3}< Db5/eighth{2:3}> |v2 r/quarter. Bb4/half. |v5 F4/half.~ +Ab4/half.~ F4/quarter. +Ab4/quarter.",
                describe(part.getMeasures().get(2)));
        assertTrue(describe(part.getMeasures().get(7)).endsWith("Eb3/eighth{2:3}< Ab2/eighth{2:3}>"));

        // treble clef on the lower staff in bar 9
        IClef clef = part.getMeasures().get(8).getAttributes().get(0).getClefs().get(0);
        assertEquals(2, clef.getNumber());
        assertEquals("G", clef.getSign());
    }


    /**
     * Tests Jesus bleibet meine Freude: seven parts, the 9/8 display meter over 3/4 with hidden
     * triplets, and lyrics.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadJesuJoy() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(SAMPLE_DIR.resolve("Bach_Jesu_joy.dorico"));
        assertEquals(7, score.getParts().size());
        assertEquals("Violin I", score.getPartList().getScoreParts().get(1).getPartName());
        assertEquals("Soprano Alto", score.getPartList().getScoreParts().get(4).getPartName());

        IMeasure violin = score.getParts().get(1).getMeasures().get(0);
        assertEquals("3", violin.getAttributes().get(0).getTimes().get(0).getBeats());
        assertEquals("|v1 r/eighth{3:2}< G4/eighth{3:2} A4/eighth{3:2}> B4/eighth{3:2}<( D5/eighth{3:2} C5/eighth{3:2}>) "
                + "C5/eighth{3:2}<( E5/eighth{3:2} D5/eighth{3:2}>)", describe(violin));
        ITuplet tuplet = violin.getNotes().get(0).getNotations().getTuplets().get(0);
        assertEquals("none", tuplet.getShowNumber());
        assertEquals("no", tuplet.getBracket());

        IPart choir = score.getParts().get(4);
        assertEquals("|v1 B4/half\"Je-\" C5/quarter\"sus\" |v2 G4/half G4/quarter", describe(choir.getMeasures().get(8)));
        assertEquals("|v1 D5/half\"blei-\" D5/quarter\"bet\" |v2 A4/quarter( G4/quarter) F#4/quarter", describe(choir.getMeasures().get(9)));
    }


    /**
     * Tests Purcell's Thou knowest, Lord: lyrics in 4/2.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadPurcell() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(SAMPLE_DIR.resolve("Purcell_Thou_knowest.dorico"));
        IPart soprano = score.getParts().get(0);
        assertEquals(FOUR, soprano.getMeasures().get(0).getAttributes().get(0).getTimes().get(0).getBeats());
        assertEquals("|v1 r/half G4/half\"Thou\" G4/half\"know-\" G4/half\"est,\"", describe(soprano.getMeasures().get(0)));
        ILyric lyric = soprano.getMeasures().get(0).getNotes().get(2).getLyrics().get(0);
        assertEquals("begin", lyric.getSyllabic());
        assertEquals(ONE, lyric.getNumber());
    }


    /**
     * Tests the fingering test project (a MusicXML file with the fingerings 1 to 5, imported into Dorico): Dorico
     * stores the fingerings counted from 0, the reader returns them as 1 to 5.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadFingering() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(SAMPLE_DIR.resolve("fingering-test.dorico"));
        List<String> fingerings = new ArrayList<>();
        for (INote note : score.getParts().get(0).getMeasures().get(0).getNotes()) {
            if (note.getNotations() != null && note.getNotations().getTechnical() != null) {
                fingerings.add(note.getNotations().getTechnical().getFingering());
            }
        }
        assertEquals(List.of("1", "2", "3", "4", "5"), fingerings);
    }


    /**
     * Tests Sousa's Washington Post March: a band score with transposing instruments, dynamics,
     * hairpins, grace notes and repeat endings.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadSousaMarch() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(SAMPLE_DIR.resolve("Sousa_Washington_Post.dorico"));
        assertEquals(BAND_PARTS, score.getParts().size());
        assertEquals("Clarinet 1", score.getPartList().getScoreParts().get(4).getPartName());

        int dynamics = 0;
        int wedges = 0;
        int graces = 0;
        int endings = 0;
        for (IPart part : score.getParts()) {
            for (IMeasure measure : part.getMeasures()) {
                for (IDirection direction : safe(measure.getDirections())) {
                    for (IDirectionType type : safe(direction.getDirectionTypes())) {
                        if (type.getDynamics() != null) {
                            dynamics++;
                        }
                        if (type.getWedge() != null) {
                            wedges++;
                        }
                    }
                }
                for (INote note : measure.getNotes()) {
                    if (note.getGrace() != null) {
                        graces++;
                    }
                }
                for (IBarline barline : safe(measure.getBarlines())) {
                    if (barline.getEnding() != null) {
                        endings++;
                    }
                }
            }
        }
        assertTrue(dynamics > 500, "dynamics: " + dynamics);
        assertTrue(wedges > 600, "hairpin starts and stops: " + wedges);
        assertTrue(graces > 100, "grace notes: " + graces);
        assertEquals(BAND_PARTS * 4, endings, "1st and 2nd ending start and stop in every part");
    }


    /**
     * Checks that every voice of every bar adds up to the bar length, that ties are closed and that tuplets do not nest.
     *
     * @param part the part
     * @param name the name for the messages
     */
    private void assertConsistent(IPart part, String name) {
        int barLength = 0;
        int divisions = 1;
        Map<String, Integer> openTies = new HashMap<>();
        Map<Integer, Integer> openTuplets = new HashMap<>();
        List<? extends IMeasure> measures = part.getMeasures();
        for (int m = 0; m < measures.size(); m++) {
            IMeasure measure = measures.get(m);
            for (IAttributes attributes : safe(measure.getAttributes())) {
                if (attributes.getDivisions() != null) {
                    divisions = attributes.getDivisions();
                }
                if (!safe(attributes.getTimes()).isEmpty()) {
                    int beats = 0;
                    for (String value : attributes.getTimes().get(0).getBeats().split("[+]")) {
                        beats += Integer.parseInt(value.trim());
                    }
                    barLength = beats * 4 * divisions / Integer.parseInt(attributes.getTimes().get(0).getBeatType());
                }
            }

            String bar = name + " bar " + measure.getNumber();
            Map<Integer, Integer> voiceDurations = new HashMap<>();
            for (INote note : measure.getNotes()) {
                if (note.getGrace() != null) {
                    continue;
                }
                if (note.getChord() == null) {
                    voiceDurations.merge(note.getVoice(), note.getDuration(), Integer::sum);
                }
                for (ITuplet tuplet : tupletsOf(note)) {
                    int open = openTuplets.merge(note.getVoice(), direction(tuplet.getType()), Integer::sum);
                    assertTrue(open == 0 || open == 1, bar + ": tuplet nesting");
                }
                if (note.getPitch() != null) {
                    String key = note.getVoice() + note.getPitch().getStep() + note.getPitch().getAlter() + note.getPitch().getOctave();
                    for (ITie tie : safe(note.getTies())) {
                        int open = openTies.merge(key, direction(tie.getType()), Integer::sum);
                        assertTrue(open >= 0, bar + ": tie stop without start");
                    }
                }
            }
            boolean beforePickup = m + 1 < measures.size() && "yes".equals(measures.get(m + 1).getImplicit());
            boolean endRepeat = false;
            for (IBarline barline : safe(measure.getBarlines())) {
                endRepeat |= barline.getRepeat() != null && "backward".equals(barline.getRepeat().getDirection());
            }
            if ("yes".equals(measure.getImplicit()) || beforePickup || endRepeat) {
                // a pick-up bar, the bar before a pick-up bar and the last bar before an end repeat can be shorter
                // than the time signature, all voices have the same length
                int pickupLength = Collections.max(voiceDurations.values());
                assertTrue(pickupLength <= barLength, bar + ": pick-up bar not longer than the time signature");
                for (Map.Entry<Integer, Integer> entry : voiceDurations.entrySet()) {
                    assertEquals(pickupLength, entry.getValue(), bar + " voice " + entry.getKey());
                }
                continue;
            }
            for (Map.Entry<Integer, Integer> entry : voiceDurations.entrySet()) {
                assertEquals(barLength, entry.getValue(), bar + " voice " + entry.getKey());
            }
        }
        for (Map.Entry<String, Integer> entry : openTies.entrySet()) {
            assertEquals(0, entry.getValue(), name + ": open tie " + entry.getKey());
        }
    }


    /**
     * Gets the counting direction of a start or stop type.
     *
     * @param type the type
     * @return 1 for start, -1 otherwise
     */
    private int direction(String type) {
        if (START.equals(type)) {
            return 1;
        }
        return -1;
    }


    /**
     * Gets the tuplet notations of a note.
     *
     * @param note the note
     * @return the tuplets (may be empty)
     */
    private List<ITuplet> tupletsOf(INote note) {
        if (note.getNotations() == null) {
            return Collections.emptyList();
        }
        return safe(note.getNotations().getTuplets());
    }


    /**
     * Gets the words of the directions of a measure.
     *
     * @param measure the measure
     * @return the texts
     */
    private List<String> wordsOf(IMeasure measure) {
        List<String> result = new ArrayList<>();
        for (IDirection direction : safe(measure.getDirections())) {
            for (IDirectionType type : safe(direction.getDirectionTypes())) {
                for (IWords words : safe(type.getWords())) {
                    result.add(words.getValue());
                }
            }
        }
        return result;
    }


    /**
     * Gets the dynamics of the directions of a measure.
     *
     * @param measure the measure
     * @return the dynamics
     */
    private List<IDynamics> dynamicsOf(IMeasure measure) {
        List<IDynamics> result = new ArrayList<>();
        for (IDirection direction : safe(measure.getDirections())) {
            for (IDirectionType type : safe(direction.getDirectionTypes())) {
                if (type.getDynamics() != null) {
                    result.add(type.getDynamics());
                }
            }
        }
        return result;
    }


    /**
     * Returns a list or an empty list if it is null.
     *
     * @param <T> the element type
     * @param list the list (may be null)
     * @return the list
     */
    private static <T> List<T> safe(List<? extends T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list);
    }


    /**
     * Describes the notes of a measure in a compact form: voice markers, grace notes (g), chord marker (+),
     * pitch or rest (r), type, dots, tuplet ratio ({3:2}), tie start (~), tuplet start or stop (&lt; or &gt;),
     * slur start or stop and lyrics.
     *
     * @param measure the measure
     * @return the description
     */
    private String describe(IMeasure measure) {
        StringBuilder sb = new StringBuilder();
        Integer lastVoice = null;
        for (INote note : measure.getNotes()) {
            if (!note.getVoice().equals(lastVoice)) {
                sb.append(" |v").append(note.getVoice());
                lastVoice = note.getVoice();
            }
            sb.append(' ');
            if (note.getGrace() != null) {
                sb.append('g');
            }
            if (note.getChord() != null) {
                sb.append('+');
            }
            if (note.getRest() != null) {
                sb.append('r');
            } else {
                sb.append(note.getPitch().getStep());
                Double alter = note.getPitch().getAlter();
                if (alter != null && alter > 0) {
                    sb.append('#');
                } else if (alter != null && alter < 0) {
                    sb.append('b');
                }
                sb.append(note.getPitch().getOctave());
            }
            sb.append('/').append(note.getType());
            for (int i = 0; i < safe(note.getDots()).size(); i++) {
                sb.append('.');
            }
            if (note.getTimeModification() != null) {
                sb.append('{').append(note.getTimeModification().getActualNotes()).append(':')
                        .append(note.getTimeModification().getNormalNotes()).append('}');
            }
            for (ITie tie : safe(note.getTies())) {
                if (START.equals(tie.getType())) {
                    sb.append('~');
                }
            }
            for (ITuplet tuplet : tupletsOf(note)) {
                if (START.equals(tuplet.getType())) {
                    sb.append('<');
                } else {
                    sb.append('>');
                }
            }
            if (note.getNotations() != null) {
                for (ISlur slur : safe(note.getNotations().getSlurs())) {
                    if (START.equals(slur.getType())) {
                        sb.append('(');
                    } else {
                        sb.append(')');
                    }
                }
            }
            for (ILyric lyric : safe(note.getLyrics())) {
                sb.append('"').append(lyric.getText());
                if ("begin".equals(lyric.getSyllabic()) || "middle".equals(lyric.getSyllabic())) {
                    sb.append('-');
                }
                sb.append('"');
            }
        }
        return sb.toString().trim();
    }


    /**
     * Creates a .dorico ZIP container with the given XML score tree.
     *
     * @param scoreXml the score tree in XML encoding
     * @return the project bytes
     * @throws IOException if the ZIP cannot be written
     */
    private byte[] createProject(String scoreXml) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("META-INF/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("META-INF/container.xml"));
            zip.write(CONTAINER_XML.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("__MACOSX/._score.xml"));
            zip.write(new byte[] {0, 5, 22, 7});
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("score.xml"));
            zip.write(scoreXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }


    /**
     * Creates a synthetic score tree: piano (2 staves) and B flat trumpet, C minor in 3/4,
     * changing to D major in 4/4 at quarter 6.
     *
     * @return the score tree in XML encoding
     */
    private String syntheticScore() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?><kScore>"
                + "<fileVersion>1.1096</fileVersion>"
                + "<info><title>Synthetic</title><composer>J. Doe</composer><arranger/><copyright>(c) Test</copyright>"
                + "<lastSavedDate>1641774451</lastSavedDate><lastSavedVersion>4.0.5</lastSavedVersion></info>"
                + "<library><instruments><entities array=\"true\">"
                + "<InstrumentEntityDefinition><entityID>instrument.brass.trumpet.bflat</entityID>"
                + "<pitchedInstrumentData><transposedChromaticTransposition>2</transposedChromaticTransposition>"
                + "<transposedDiatonicTransposition>1</transposedDiatonicTransposition></pitchedInstrumentData>"
                + "</InstrumentEntityDefinition></entities></instruments></library>"
                + "<scorePlayers><array array=\"true\">"
                + player("0", "Piano", "Pno", "instrument.keyboard.piano", "2", "<clef><idForTransposingLayouts>clef.treble</idForTransposingLayouts></clef>"
                        + "<clef><idForTransposingLayouts>clef.bass</idForTransposingLayouts></clef>")
                + player(ONE, "Trumpet in B flat", "Tpt", "instrument.brass.trumpet.bflat", ONE,
                        "<clef><idForTransposingLayouts>clef.treble</idForTransposingLayouts></clef>")
                + player("2", "Not in flow", "X", "instrument.keyboard.piano", ONE, "")
                + "</array></scorePlayers>"
                + "<flows><array array=\"true\"><flow><flowID>0</flowID><flowName>Flow 1</flowName>"
                + "<minimumNotatedLength><length>14</length></minimumNotatedLength>"
                + "<flowPlayers><array array=\"true\"><flowPlayer><playerID>0</playerID></flowPlayer>"
                + "<flowPlayer><playerID>1</playerID></flowPlayer></array></flowPlayers>"
                + "<elementTables><array array=\"true\">"
                + "<BarDivisionElementTableDefinition><array array=\"true\">"
                + barDivision("0", "3", FOUR) + barDivision(ONE, FOUR, FOUR)
                + "</array></BarDivisionElementTableDefinition>"
                + "<TonalityDivisionElementTableDefinition><array array=\"true\">"
                + tonality("0", "kKeySigMinor", "C") + tonality(ONE, "kKeySigMajor", "D")
                + "</array></TonalityDivisionElementTableDefinition>"
                + "</array></elementTables>"
                + "<blocks><array array=\"true\"><block><blockID>0</blockID><events array=\"true\">"
                + "<BarDivisionEventDefinition><eventID>1</eventID><position>0</position><duration>0</duration>"
                + "<barDivisionElementID>0</barDivisionElementID></BarDivisionEventDefinition>"
                + "<TonalityDivisionEventDefinition><eventID>2</eventID><position>0</position><duration>0</duration>"
                + "<tonalityDivisionElementID>0</tonalityDivisionElementID></TonalityDivisionEventDefinition>"
                + "<BarDivisionEventDefinition><eventID>3</eventID><position>6</position><duration>0</duration>"
                + "<barDivisionElementID>1</barDivisionElementID></BarDivisionEventDefinition>"
                + "<TonalityDivisionEventDefinition><eventID>4</eventID><position>6</position><duration>0</duration>"
                + "<tonalityDivisionElementID>1</tonalityDivisionElementID></TonalityDivisionEventDefinition>"
                + "</events></block></array></blocks>"
                + "<blockInstances><array array=\"true\"><blockInstance><blockInstanceID>1000</blockInstanceID>"
                + "<blockID>0</blockID><streamID>0</streamID><position>0</position></blockInstance></array></blockInstances>"
                + "<eventStreams><array array=\"true\"><eventStream><streamID>0</streamID>"
                + "<blockInstanceIDs>1000</blockInstanceIDs><eventStreamType>kGlobalTimebaseStream</eventStreamType>"
                + "</eventStream></array></eventStreams>"
                + "</flow></array></flows></kScore>";
    }


    /**
     * Creates a score player XML fragment with one instrument.
     *
     * @param id the player ID
     * @param name the display name
     * @param shortName the display short name
     * @param entityId the instrument entity ID
     * @param staves the number of staves
     * @param clefs the clef elements
     * @return the XML fragment
     */
    private String player(String id, String name, String shortName, String entityId, String staves, String clefs) {
        return "<scorePlayer><playerID>" + id + "</playerID><displayName>" + name + "</displayName>"
                + "<displayShortName>" + shortName + "</displayShortName><instruments array=\"true\"><instrument>"
                + "<entityID>" + entityId + "</entityID><instrumentID>" + id + "</instrumentID>"
                + "<numStaves>" + staves + "</numStaves><clefs array=\"true\">" + clefs + "</clefs>"
                + "</instrument></instruments></scorePlayer>";
    }


    /**
     * Creates a bar division element XML fragment.
     *
     * @param id the element ID
     * @param numerator the time signature numerator
     * @param denominator the time signature denominator
     * @return the XML fragment
     */
    private String barDivision(String id, String numerator, String denominator) {
        return "<BarDivisionElementDefinition><elementID>" + id + "</elementID><barDivisionData><timeSignature>"
                + "<timeSignaturesAndDivisions array=\"true\"><timeSignatureAndDivision><timeSignature>"
                + "<numerator>" + numerator + "</numerator><denominator>" + denominator + "</denominator>"
                + "</timeSignature></timeSignatureAndDivision></timeSignaturesAndDivisions>"
                + "</timeSignature></barDivisionData></BarDivisionElementDefinition>";
    }


    /**
     * Creates a tonality division element XML fragment.
     *
     * @param id the element ID
     * @param type the tonality type
     * @param tonic the tonic note name
     * @return the XML fragment
     */
    private String tonality(String id, String type, String tonic) {
        return "<TonalityDivisionElementDefinition><elementID>" + id + "</elementID><tonalityDivisionData><keySignature>"
                + "<root><tonalityType>" + type + "</tonalityType><root><noteName>" + tonic + "</noteName>"
                + "<accidentalID>accidental.12et.western.natural</accidentalID></root></root>"
                + "</keySignature></tonalityDivisionData></TonalityDivisionElementDefinition>";
    }
}
