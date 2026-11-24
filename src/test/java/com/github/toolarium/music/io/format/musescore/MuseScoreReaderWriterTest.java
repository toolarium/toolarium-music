/*
 * MuseScoreReaderWriterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.SoundingNoteComparator;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.note.ILyric;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;


/**
 * Tests for the MuseScore (.mscz, .mscx) reader and writer.
 *
 * <p>All MuseScore files under the samples folder are used, including local folders that are not
 * part of the repository. The tracked samples of {@code samples/musescore} cover the file versions
 * 1.x to 3.x; their MusicXML export by MuseScore 4 is stored in {@code samples/musescore/reference}.</p>
 *
 * @author patrick
 */
public class MuseScoreReaderWriterTest {
    private static final Path MUSESCORE_DIR = TestResourceUtil.getInstance().getSamplesDir().resolve("musescore");
    private static final Path REFERENCE_DIR = MUSESCORE_DIR.resolve("reference");
    private static final String MSCZ = ".mscz";
    private static final String MSCX = ".mscx";
    private static final String GOLDBERG = "goldberg.mscz";
    private static final int TRACKED_SAMPLES = 15;

    /** MuseScore's MusicXML export does not scale local time signatures; the goldberg reference is compared up to variation 26. */
    private static final Fraction GOLDBERG_LOCAL_TIME = Fraction.of(4817, 2);


    /**
     * Tests the factory and the format detection.
     */
    @Test
    void testFactoryAndFormatDetection() {
        assertNotNull(MuseScoreIOFactory.getInstance().getReader());
        assertNotNull(MuseScoreIOFactory.getInstance().getWriter());
        assertNotNull(MuseScoreIOFactory.getInstance().getMscxReader());
        assertNotNull(MuseScoreIOFactory.getInstance().getMscxWriter());
        assertEquals(MusicFileFormat.MUSESCORE, MusicFileFormat.fromFileName("Score.MSCZ"));
        assertEquals(MusicFileFormat.MUSESCORE_XML, MusicFileFormat.fromFileName("score.mscx"));
    }


    /**
     * Reads every MuseScore file under the samples folder and checks that the voices of every
     * measure have the same length.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadAllSamples() throws IOException {
        List<Path> files = TestResourceUtil.getInstance().findFilesByExtension(MSCZ, MSCX);
        assertTrue(files.size() >= TRACKED_SAMPLES, "MuseScore samples: " + files.size());
        List<String> failures = TestResourceUtil.getInstance().processFiles(files, file -> {
            IScore score = MusicFileFactory.getInstance().read(file);
            assertTrue(score.getParts().size() > 0, file.toString());
            for (IPart part : score.getParts()) {
                assertTrue(part.getMeasures().size() > 0, file.toString());
                assertVoicesComplete(part, file.getFileName() + " " + part.getId());
            }
            return null;
        });
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }


    /**
     * Compares the reader with MuseScore's own MusicXML export of the tracked samples.
     *
     * @throws Exception if reading fails
     */
    @Test
    void testCompareWithMuseScoreExport() throws Exception {
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        List<Path> references = new ArrayList<>();
        for (Path reference : TestResourceUtil.getInstance().findFilesByExtension(".mxl")) {
            if (reference.startsWith(REFERENCE_DIR)) {
                references.add(reference);
            }
        }
        List<String> failures = TestResourceUtil.getInstance().processFiles(references, reference -> {
            String name = reference.getFileName().toString().replace(".mxl", MSCZ);
            List<List<SoundingNoteComparator.SoundingNote>> expected = comparator.readReference(reference);
            List<List<SoundingNoteComparator.SoundingNote>> actual = comparator.readConverted(MusicFileFactory.getInstance().read(MUSESCORE_DIR.resolve(name)));
            if (GOLDBERG.equals(name)) {
                expected = comparator.limit(expected, GOLDBERG_LOCAL_TIME);
                actual = comparator.limit(actual, GOLDBERG_LOCAL_TIME);
            }
            return comparator.compare(name, expected, actual);
        });
        assertTrue(references.size() >= 10, "references: " + references.size());
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }


    /**
     * Writes every MuseScore sample as .mscz, reads it back and compares the sounding notes.
     *
     * @throws IOException if reading or writing fails
     */
    @Test
    void testWriteAndReadBack() throws IOException {
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        List<Path> files = TestResourceUtil.getInstance().findFilesByExtension(MSCZ, MSCX);
        List<String> failures = TestResourceUtil.getInstance().processFiles(files, file -> {
            IScore original = MusicFileFactory.getInstance().read(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MusicFileFactory.getInstance().write(original, out, MusicFileFormat.MUSESCORE);
            IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(out.toByteArray()), MusicFileFormat.MUSESCORE);
            assertEquals(original.getParts().size(), readBack.getParts().size(), file.toString());
            return comparator.compare(file.getFileName().toString(), comparator.readConverted(original), comparator.readConverted(readBack));
        });
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }


    /**
     * Writes an uncompressed MuseScore 4 file and reads it back.
     *
     * @throws IOException if reading or writing fails
     */
    @Test
    void testWriteMscx() throws IOException {
        IScore original = MusicFileFactory.getInstance().read(MUSESCORE_DIR.resolve("Amazing_grace.mscz"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MusicFileFactory.getInstance().write(original, out, MusicFileFormat.MUSESCORE_XML);
        String mscx = out.toString(StandardCharsets.UTF_8);
        assertTrue(mscx.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<museScore version=\"4.40\">"));
        assertTrue(mscx.contains("<sigN>9</sigN>"));
        assertTrue(mscx.contains("<syllabic>begin</syllabic>"));

        IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(out.toByteArray()), MusicFileFormat.MUSESCORE_XML);
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        assertNull(comparator.compare("Amazing_grace", comparator.readConverted(original), comparator.readConverted(readBack)));
        assertEquals("Soprano", readBack.getPartList().getScoreParts().get(0).getPartName());
    }


    /**
     * Tests the content of a version 2 file: parts, time signature and lyrics.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadAmazingGrace() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(MUSESCORE_DIR.resolve("Amazing_grace.mscz"));
        assertEquals(4, score.getParts().size());
        assertEquals("Soprano", score.getPartList().getScoreParts().get(0).getPartName());
        assertEquals("Bass", score.getPartList().getScoreParts().get(3).getPartName());

        IPart soprano = score.getParts().get(0);
        IAttributes attributes = soprano.getMeasures().get(0).getAttributes().get(0);
        assertEquals("9", attributes.getTimes().get(0).getBeats());
        assertEquals("8", attributes.getTimes().get(0).getBeatType());
        assertEquals(1, attributes.getKeys().get(0).getFifths());
        assertEquals("A- ma- zing grace how", lyrics(soprano, 0, 3));
    }


    /**
     * Tests the local time signatures of the Goldberg variations (variation 26: 18/16 over 3/4).
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadGoldbergLocalTimeSignature() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(MUSESCORE_DIR.resolve(GOLDBERG));
        IPart part = score.getParts().get(0);
        assertEquals(990, part.getMeasures().size());

        // the 18/16 staff is stretched into the 3/4 measure: its sixteenths are 3 in the time of 2
        IMeasure measure = part.getMeasures().get(812);
        INote first = measure.getNotes().get(0);
        assertEquals("16th", first.getType());
        assertEquals(3, first.getTimeModification().getActualNotes());
        assertEquals(2, first.getTimeModification().getNormalNotes());
        assertVoicesComplete(part, GOLDBERG);
    }


    /**
     * Tests that invalid input is rejected.
     *
     * @throws IOException if the test data cannot be created
     */
    @Test
    void testRejectInvalidInput() throws IOException {
        IMusicReader reader = MuseScoreIOFactory.getInstance().getReader();
        assertThrows(IOException.class, () -> reader.read(new ByteArrayInputStream("not a zip".getBytes(StandardCharsets.UTF_8))));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("readme.txt"));
            zip.write("no score".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        assertThrows(IOException.class, () -> reader.read(new ByteArrayInputStream(bytes.toByteArray())));

        IMusicReader mscxReader = MuseScoreIOFactory.getInstance().getMscxReader();
        assertThrows(IOException.class, () -> mscxReader.read(new ByteArrayInputStream("<score/>".getBytes(StandardCharsets.UTF_8))));
        assertThrows(IOException.class, () -> mscxReader.read(new ByteArrayInputStream(
                "<!DOCTYPE museScore [<!ENTITY x \"y\">]><museScore/>".getBytes(StandardCharsets.UTF_8))));
    }


    /**
     * Checks that all voices of a measure have the same length (the first measure may be a pickup).
     *
     * @param part the part
     * @param name the name for the messages
     */
    private void assertVoicesComplete(IPart part, String name) {
        for (IMeasure measure : part.getMeasures()) {
            Map<Integer, Integer> sums = new HashMap<>();
            for (INote note : measure.getNotes()) {
                if (note.getChord() == null && note.getGrace() == null && note.getDuration() != null) {
                    sums.merge(note.getVoice(), note.getDuration(), Integer::sum);
                }
            }
            if (sums.size() > 1) {
                int expected = sums.values().iterator().next();
                for (Map.Entry<Integer, Integer> entry : sums.entrySet()) {
                    assertEquals(expected, (int) entry.getValue(), name + " measure " + measure.getNumber() + " voice " + entry.getKey());
                }
            }
        }
    }


    /**
     * Gets the lyrics of the first measures of a part, syllables of a word joined with "- ".
     *
     * @param part the part
     * @param from the first measure index
     * @param to the measure index after the last one
     * @return the lyrics
     */
    private String lyrics(IPart part, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (IMeasure measure : part.getMeasures().subList(from, to)) {
            for (INote note : measure.getNotes()) {
                if (note.getLyrics() == null) {
                    continue;
                }
                for (ILyric lyric : note.getLyrics()) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(lyric.getText());
                    if ("begin".equals(lyric.getSyllabic()) || "middle".equals(lyric.getSyllabic())) {
                        sb.append('-');
                    }
                }
            }
        }
        return sb.toString();
    }


    /**
     * Checks that the reference folder exists.
     */
    @Test
    void testReferenceFolder() {
        assertTrue(Files.isDirectory(REFERENCE_DIR), REFERENCE_DIR.toString());
    }
}
