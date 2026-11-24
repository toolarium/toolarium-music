/*
 * CapellaReaderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.SoundingNoteComparator;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;


/**
 * Tests for the capella (.capx) reader.
 *
 * @author patrick
 */
public class CapellaReaderTest {
    private static final Path CAPELLA_DIR = TestResourceUtil.getInstance().getSamplesDir().resolve("capella");
    private static final Path REFERENCE_DIR = CAPELLA_DIR.resolve("reference");
    private static final String CAPX = ".capx";

    /**
     * Samples where the MuseScore reference differs from the capella content: MuseScore ignores the
     * default time of a staff without time signature, and MuseScore 5 stores a tie on its end note.
     */
    private static final Set<String> REFERENCE_DIFFERENCES = new HashSet<>(Arrays.asList("testScaleC4C5.capx", "testSlurTie.capx"));


    /**
     * Tests that the factory returns a reader and that the formats are detected by extension.
     */
    @Test
    void testFactoryAndFormatDetection() {
        assertNotNull(CapellaIOFactory.getInstance().getReader());
        assertEquals(MusicFileFormat.CAPELLA, MusicFileFormat.fromFileName("Song.capx"));
        assertEquals(MusicFileFormat.CAPELLA, MusicFileFormat.fromFileName("Song.CAP"));
    }


    /**
     * Tests that every capx sample can be read.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadAllSamples() throws IOException {
        List<Path> files = TestResourceUtil.getInstance().findFilesByExtension(CAPX);
        assertTrue(files.size() >= 18, "samples: " + files.size());
        List<String> failures = TestResourceUtil.getInstance().processFiles(files, file -> {
            IScore score = MusicFileFactory.getInstance().read(file);
            assertFalse(score.getParts().isEmpty(), file.toString());
            return null;
        });
        assertTrue(failures.isEmpty(), String.join(System.lineSeparator(), failures));
    }


    /**
     * Compares the sounding notes of every sample with the MuseScore reference (capella import by MuseScore).
     *
     * @throws Exception if reading fails
     */
    @Test
    void testCompareWithMuseScoreReference() throws Exception {
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        List<Path> compareFiles = new ArrayList<>();
        for (Path file : TestResourceUtil.getInstance().findFilesByExtension(CAPX)) {
            String name = file.getFileName().toString();
            Path reference = REFERENCE_DIR.resolve(name + ".mscx");
            if (!Files.exists(reference) || REFERENCE_DIFFERENCES.contains(name)) {
                continue;
            }
            compareFiles.add(file);
        }
        List<String> failures = TestResourceUtil.getInstance().processFiles(compareFiles, file -> {
            String name = file.getFileName().toString();
            Path reference = REFERENCE_DIR.resolve(name + ".mscx");
            IScore expected = MusicFileFactory.getInstance().read(reference, MusicFileFormat.MUSESCORE_XML);
            return comparator.compare(name, comparator.readConverted(expected), comparator.readConverted(MusicFileFactory.getInstance().read(file)));
        });
        assertTrue(compareFiles.size() >= 15, "compared: " + compareFiles.size());
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }


    /**
     * Tests the tuplets of all kinds (including tripartite and prolonged tuplets).
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadTuplets() throws IOException {
        IPart part = MusicFileFactory.getInstance().read(CAPELLA_DIR.resolve("testTuplet2.capx")).getParts().get(0);
        List<String> ratios = new ArrayList<>();
        for (int m = 0; m < part.getMeasures().size(); m++) {
            for (INote note : part.getMeasures().get(m).getNotes()) {
                if (note.getTimeModification() != null) {
                    String ratio = note.getTimeModification().getActualNotes() + ":" + note.getTimeModification().getNormalNotes();
                    if (ratios.isEmpty() || !ratios.get(ratios.size() - 1).equals(ratio)) {
                        ratios.add(ratio);
                    }
                }
            }
        }
        assertEquals("[3:2, 5:4, 7:4, 9:8, 6:4, 2:3, 4:3, 8:6, 10:6, 14:12, 11:6, 12:8, 13:8, 15:8]", ratios.toString());
    }


    /**
     * Tests an inline CapXML document: instrument, clefs, key, time, lyrics, ties, explicit barlines and a volta.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadInlineDocument() throws IOException {
        String capx = "<score xmlns=\"http://www.capella.de/CapXML/2.0\"><info><title>Inline</title><author>J. Doe</author></info>"
                + "<layout><staves><staffLayout description=\"S\"><notation defaultClef=\"treble\"/>"
                + "<instrument name=\"Soprano\" abbrev=\"S.\"/><sound instr=\"52\"/></staffLayout></staves></layout>"
                + "<systems><system><staves><staff layout=\"S\" defaultTime=\"3/4\"><voices><voice><noteObjects>"
                + "<clefSign clef=\"treble\"/><keySign fifths=\"-1\"/><timeSign time=\"3/4\"/>"
                + "<chord><duration base=\"1/4\"/><lyric><verse i=\"0\" hyphen=\"true\">Hal</verse></lyric><heads><head pitch=\"F5\"/></heads></chord>"
                + "<chord><duration base=\"1/4\"/><lyric><verse i=\"0\">le</verse></lyric><heads><head pitch=\"B5\"><alter step=\"-1\"/></head></heads></chord>"
                + "<chord><duration base=\"1/4\"/><heads><head pitch=\"A5\"><tie begin=\"true\"/></head></heads></chord>"
                + "<chord><duration base=\"1/2\" dots=\"1\"/><drawObjects><drawObj><volta firstNumber=\"1\"/></drawObj></drawObjects>"
                + "<heads><head pitch=\"A5\"><tie end=\"true\"/></head></heads></chord><barline type=\"repEnd\"/>"
                + "</noteObjects></voice></voices></staff></staves></system></systems></score>";
        IScore score = MusicFileFactory.getInstance().read(new ByteArrayInputStream(capx.getBytes(StandardCharsets.UTF_8)), MusicFileFormat.CAPELLA);
        assertEquals("Inline", score.getWork().getWorkTitle());
        assertEquals("Soprano", score.getPartList().getScoreParts().get(0).getPartName());
        assertEquals(53, score.getPartList().getScoreParts().get(0).getMidiInstruments().get(0).getMidiProgram());

        IPart part = score.getParts().get(0);
        assertEquals(2, part.getMeasures().size());
        IAttributes attributes = part.getMeasures().get(0).getAttributes().get(0);
        assertEquals(-1, attributes.getKeys().get(0).getFifths());
        assertEquals("3", attributes.getTimes().get(0).getBeats());
        List<? extends INote> first = part.getMeasures().get(0).getNotes();
        assertEquals(4, first.get(0).getPitch().getOctave());
        assertEquals("begin", first.get(0).getLyrics().get(0).getSyllabic());
        assertEquals(-1.0, first.get(1).getPitch().getAlter());
        assertEquals("start", first.get(2).getTies().get(0).getType());
        assertEquals("stop", part.getMeasures().get(1).getNotes().get(0).getTies().get(0).getType());
        assertEquals("1", part.getMeasures().get(1).getBarlines().get(0).getEnding().getNumber());
        assertEquals("backward", part.getMeasures().get(1).getBarlines().get(1).getRepeat().getDirection());
    }


    /**
     * Compares the sounding notes of every legacy binary .cap sample with the MuseScore reference.
     *
     * @throws Exception if reading fails
     */
    @Test
    void testCompareCapWithMuseScoreReference() throws Exception {
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        List<Path> compareFiles = new ArrayList<>();
        for (Path file : TestResourceUtil.getInstance().findFilesByExtension(".cap")) {
            String name = file.getFileName().toString();
            Path reference = REFERENCE_DIR.resolve(name + ".mscx");
            if (!Files.exists(reference)) {
                continue;
            }
            compareFiles.add(file);
        }
        List<String> failures = TestResourceUtil.getInstance().processFiles(compareFiles, file -> {
            String name = file.getFileName().toString();
            Path reference = REFERENCE_DIR.resolve(name + ".mscx");
            IScore expected = MusicFileFactory.getInstance().read(reference, MusicFileFormat.MUSESCORE_XML);
            return comparator.compare(name, comparator.readConverted(expected), comparator.readConverted(MusicFileFactory.getInstance().read(file)));
        });
        assertEquals(9, compareFiles.size());
        assertTrue(failures.isEmpty(), String.join(System.lineSeparator(), failures));
    }


    /**
     * Tests a legacy binary file with key changes: the pitches are relative to the key and the
     * key signatures are kept.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadCapKeys() throws IOException {
        IPart part = MusicFileFactory.getInstance().read(CAPELLA_DIR.resolve("test7.cap")).getParts().get(0);
        assertEquals(3, part.getMeasures().get(0).getAttributes().get(0).getKeys().get(0).getFifths());
        INote first = part.getMeasures().get(0).getNotes().get(0);
        assertEquals("C", first.getPitch().getStep());
        assertEquals(1.0, first.getPitch().getAlter());
        assertEquals(4, first.getPitch().getOctave());
        assertEquals(-7, part.getMeasures().get(10).getAttributes().get(0).getKeys().get(0).getFifths());
    }


    /**
     * Tests that corrupt legacy files and invalid input are rejected.
     */
    @Test
    void testRejectCorruptAndInvalidInput() {
        for (String name : new String[] {"test1-corrupt-metafile-size.cap", "test1-corrupt-stave-index.cap", "test1-corrupt-string-length.cap",
            "test1-corrupt-text-size.cap", "test7-corrupt-key.cap"}) {
            assertThrows(IOException.class, () -> MusicFileFactory.getInstance().read(CAPELLA_DIR.resolve(name)), name);
        }
        assertThrows(IOException.class, () -> MusicFileFactory.getInstance().read(new ByteArrayInputStream("<mei/>".getBytes(StandardCharsets.UTF_8)), MusicFileFormat.CAPELLA));
    }
}
