/*
 * LilyPondWriterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.SoundingNoteComparator;
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
 * Tests for the LilyPond (.ly) writer: the written source reads back with the same sounding notes.
 *
 * @author patrick
 */
public class LilyPondWriterTest {
    private static final Path LILYPOND_DIR = TestResourceUtil.getInstance().getSamplesDir().resolve("lilypond");
    private static final Path DISNEY = TestResourceUtil.getInstance().getSamplesDir().resolve(TestResourceUtil.PROTECTED_SUBDIR).resolve("quintet")
            .resolve("filmmusic").resolve("Disney Love Medley").resolve("Disney Love Medley.musx");

    /**
     * Samples with ties that LilyPond cannot write: Goldberg ties C sharp to D flat (an enharmonic tie), Reunion
     * ties a note of one voice to a note of another voice. LilyPond only ties notes of the same pitch in one voice.
     */
    private static final List<String> TIES_NOT_IN_LILYPOND = Arrays.asList("goldberg.mxl", "goldberg.mscz", "Reunion.mxl", "Reunion.mscz");


    /**
     * Tests that the factory returns a writer.
     */
    @Test
    void testFactory() {
        assertNotNull(LilyPondIOFactory.getInstance().getWriter());
    }


    /**
     * Writes the samples of the text and XML formats as LilyPond and reads them back: the sounding notes are the
     * same.
     *
     * @throws IOException if the samples cannot be found
     */
    @Test
    void testWriteSamplesAndReadBack() throws IOException {
        List<Path> files = TestResourceUtil.getInstance().findFilesByExtension(".musicxml", ".mxl", ".mei", ".abc", ".ly", ".capx", ".mscz");
        assertTrue(files.size() >= 10, "samples: " + files.size());
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        List<String> failures = TestResourceUtil.getInstance().processFiles(files, file -> {
            if (TIES_NOT_IN_LILYPOND.contains(file.getFileName().toString())) {
                return null;
            }
            IScore original = MusicFileFactory.getInstance().read(file);
            IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(write(original)), MusicFileFormat.LILYPOND);
            assertEquals(original.getParts().size(), readBack.getParts().size(), file.toString());
            return comparator.compare(file.getFileName().toString(), comparator.readConverted(original), comparator.readConverted(readBack));
        });
        assertTrue(failures.isEmpty(), String.join(System.lineSeparator(), failures));
    }


    /**
     * Tests the content of a written score: header, tuplets, voices, chord names and lyrics.
     *
     * @throws IOException if reading or writing fails
     */
    @Test
    void testWriteContent() throws IOException {
        String tuplets = new String(write(MusicFileFactory.getInstance().read(LILYPOND_DIR.resolve("tuplets-chords.ly"))), StandardCharsets.UTF_8);
        assertTrue(tuplets.startsWith("\\version \"2.24.0\""));
        assertTrue(tuplets.contains("title = \"Tuplets and Chords\""));
        assertTrue(tuplets.contains("composer = \"toolarium\""));
        assertTrue(tuplets.contains("\\tuplet 3/2 { c'8 e'8 g'8 }"));
        assertTrue(tuplets.contains("\\tuplet 5/4 { b'16 a'16 g'16 f'16 e'16 }"));
        assertTrue(tuplets.contains("\\new ChordNames \\chordmode {"));
        assertTrue(tuplets.contains("g1:7"));
        assertTrue(tuplets.contains("\\new Voice = \"staff1voice2\""));

        String lyrics = new String(write(MusicFileFactory.getInstance().read(LILYPOND_DIR.resolve("lyrics.ly"))), StandardCharsets.UTF_8);
        assertTrue(lyrics.contains("\\new Lyrics \\lyricsto \"staff1voice1\""));
        assertTrue(lyrics.contains("\"Sing\" -- \"ing\""));
        assertTrue(lyrics.contains("\\time 3/4"));
    }


    /**
     * Tests an inline score: transposition, repeat with endings, dynamics, hairpins and a pickup survive the round
     * trip.
     *
     * @throws IOException if reading or writing fails
     */
    @Test
    void testWriteRepeatsAndTransposition() throws IOException {
        String source = "\\score { \\new Staff \\with { instrumentName = \"Trumpet\" midiInstrument = \"trumpet\" } { \\transposition bes "
                + "\\time 4/4 \\partial 4 g'4\\p | \\repeat volta 2 { c''2\\< d''2 | e''1\\! } \\alternative { { f''1 } { g''1 } } \\bar \"|.\" } }";
        IScore original = MusicFileFactory.getInstance().read(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), MusicFileFormat.LILYPOND);
        String written = new String(write(original), StandardCharsets.UTF_8);
        assertTrue(written.contains("\\transposition bes"), written);
        assertTrue(written.contains("midiInstrument = \"trumpet\""), written);
        assertTrue(written.contains("\\partial 4"), written);
        assertTrue(written.contains("\\repeat volta 2 {"), written);
        assertTrue(written.contains("\\alternative {"), written);

        IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(written.getBytes(StandardCharsets.UTF_8)), MusicFileFormat.LILYPOND);
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        assertNull(comparator.compare("inline", comparator.readConverted(original), comparator.readConverted(readBack)));
        assertEquals(-2, readBack.getParts().get(0).getMeasures().get(0).getAttributes().get(0).getTransposes().get(0).getChromatic());
    }


    /**
     * Writes the Disney Love Medley (Finale source, protected sample of the integration tests) as LilyPond: it
     * reads back with the same parts and sounding notes.
     *
     * @throws IOException if reading or writing fails
     */
    @Test
    void testWriteDisneyLoveMedley() throws IOException {
        TestResourceUtil.getInstance().assumeSamples(TestResourceUtil.getInstance().isIntegrationTest() && Files.exists(DISNEY),
                "Disney Love Medley is a protected sample of the integration tests.");
        IScore original = MusicFileFactory.getInstance().read(DISNEY);
        byte[] source = write(original);
        String text = new String(source, StandardCharsets.UTF_8);
        assertTrue(text.contains("title = \"Disney Love Medley\""));
        assertTrue(text.contains("instrumentName = \"Horn in F\""));
        assertTrue(text.contains("\\transposition f"));

        IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(source), MusicFileFormat.LILYPOND);
        assertEquals(original.getParts().size(), readBack.getParts().size());
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        assertNull(comparator.compare("Disney Love Medley", comparator.readConverted(original), comparator.readConverted(readBack)));
    }


    /**
     * Writes a score as LilyPond source.
     *
     * @param score the score
     * @return the source
     * @throws IOException if writing fails
     */
    private byte[] write(IScore score) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MusicFileFactory.getInstance().write(score, out, MusicFileFormat.LILYPOND);
        return out.toByteArray();
    }
}
