/*
 * AbcWriterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.SoundingNoteComparator;
import com.github.toolarium.music.model.score.ICreator;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Tests for the ABC notation (.abc) writer: the written tune reads back with the same sounding notes.
 *
 * @author patrick
 */
public class AbcWriterTest {
    private static final Path SAMPLES = TestResourceUtil.getInstance().getSamplesDir();
    private static final Path DISNEY = SAMPLES.resolve(TestResourceUtil.PROTECTED_SUBDIR).resolve("quintet").resolve("filmmusic")
            .resolve("Disney Love Medley").resolve("Disney Love Medley.musx");

    /**
     * Samples with ties that ABC cannot express: Goldberg ties C sharp to D flat (an enharmonic tie), Reunion ties a
     * note of one voice to a note of another voice. ABC only ties notes of the same pitch in one voice.
     */
    private static final List<String> TIES_NOT_IN_ABC = Arrays.asList("goldberg.mxl", "goldberg.mscz", "Reunion.mxl", "Reunion.mscz");


    /**
     * Tests that the factory returns a writer.
     */
    @Test
    void testFactory() {
        assertNotNull(AbcIOFactory.getInstance().getWriter());
    }


    /**
     * Writes the samples of the text and XML formats as ABC and reads them back: the sounding notes are the same.
     *
     * @throws IOException if the samples cannot be found
     */
    @Test
    void testWriteSamplesAndReadBack() throws IOException {
        List<Path> files = TestResourceUtil.getInstance().findFilesByExtension(".musicxml", ".mxl", ".mei", ".abc", ".ly", ".capx", ".mscz");
        assertTrue(files.size() >= 10, "samples: " + files.size());
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        List<String> failures = TestResourceUtil.getInstance().processFiles(files, file -> {
            if (TIES_NOT_IN_ABC.contains(file.getFileName().toString())) {
                return null;
            }
            IScore original = MusicFileFactory.getInstance().read(file);
            IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(write(original)), MusicFileFormat.ABC);
            assertEquals(original.getParts().size(), readBack.getParts().size(), file.toString());
            return comparator.compare(file.getFileName().toString(), comparator.readConverted(original), comparator.readConverted(readBack));
        });
        assertTrue(failures.isEmpty(), String.join(System.lineSeparator(), failures));
    }


    /**
     * Tests the content of a written tune: header, voices, tuplets, chord symbols and lyrics.
     *
     * @throws IOException if reading or writing fails
     */
    @Test
    void testWriteContent() throws IOException {
        String tuplets = new String(write(MusicFileFactory.getInstance().read(SAMPLES.resolve("lilypond").resolve("tuplets-chords.ly"))),
                StandardCharsets.UTF_8);
        assertTrue(tuplets.startsWith("X:1\nT:Tuplets and Chords\nC:toolarium\nM:4/4\nL:1/8\n"), tuplets);
        assertTrue(tuplets.contains("%%score (S1V1 S1V2)"), tuplets);
        assertTrue(tuplets.contains("(3:2:3\"C\"C E G"), tuplets);
        assertTrue(tuplets.contains("(5:4:5B/2 A/2 G/2 F/2 E/2"), tuplets);
        assertTrue(tuplets.contains("\"G7\"G4"), tuplets);

        String lyrics = new String(write(MusicFileFactory.getInstance().read(SAMPLES.resolve("lilypond").resolve("lyrics.ly"))),
                StandardCharsets.UTF_8);
        assertTrue(lyrics.contains("M:3/4"), lyrics);
        assertTrue(lyrics.contains("w:Sing- ing a tune,"), lyrics);
    }


    /**
     * Tests a tune with transposition, repeats with endings, an arranger and the rights: they survive the round
     * trip.
     *
     * @throws IOException if reading or writing fails
     */
    @Test
    void testWriteRepeatsAndHeader() throws IOException {
        String source = "X:1\nT:Repeats\nC:Composer\nC:arr. Arranger\nN:© 2026 toolarium\nM:4/4\nL:1/4\n"
                + "V:T name=\"Trumpet\" transpose=-2\nK:D\nV:T\n|: d e f g | a4 |1 b4 :|2 c'4 |]\n";
        IScore original = MusicFileFactory.getInstance().read(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), MusicFileFormat.ABC);
        String written = new String(write(original), StandardCharsets.UTF_8);
        assertTrue(written.contains("C:arr. Arranger"), written);
        assertTrue(written.contains("N:© 2026 toolarium"), written);
        assertTrue(written.contains("transpose=-2"), written);
        assertTrue(written.contains(":|"), written);
        assertTrue(written.contains("[2"), written);

        IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(written.getBytes(StandardCharsets.UTF_8)), MusicFileFormat.ABC);
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        assertNull(comparator.compare("inline", comparator.readConverted(original), comparator.readConverted(readBack)));
        assertEquals("© 2026 toolarium", readBack.getIdentification().getRights());
        boolean arranger = false;
        for (ICreator creator : readBack.getIdentification().getCreators()) {
            arranger = arranger || "arranger".equals(creator.getType()) && "Arranger".equals(creator.getValue());
        }
        assertTrue(arranger);
    }


    /**
     * Writes the Disney Love Medley (Finale source, protected sample of the integration tests) as ABC: it reads back
     * with the same parts and sounding notes.
     *
     * @throws IOException if reading or writing fails
     */
    @Test
    void testWriteDisneyLoveMedley() throws IOException {
        TestResourceUtil.getInstance().assumeSamples(TestResourceUtil.getInstance().isIntegrationTest() && Files.exists(DISNEY),
                "Disney Love Medley is a protected sample of the integration tests.");
        IScore original = MusicFileFactory.getInstance().read(DISNEY);
        byte[] tune = write(original);
        String text = new String(tune, StandardCharsets.UTF_8);
        assertTrue(text.contains("T:Disney Love Medley"));
        assertTrue(text.contains("name=\"Horn in F\" clef=treble transpose=-7"));

        IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(tune), MusicFileFormat.ABC);
        assertEquals(original.getParts().size(), readBack.getParts().size());
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        assertNull(comparator.compare("Disney Love Medley", comparator.readConverted(original), comparator.readConverted(readBack)));
    }


    /**
     * Writes a score as ABC tune.
     *
     * @param score the score
     * @return the ABC text
     * @throws IOException if writing fails
     */
    private byte[] write(IScore score) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MusicFileFactory.getInstance().write(score, out, MusicFileFormat.ABC);
        return out.toByteArray();
    }
}
