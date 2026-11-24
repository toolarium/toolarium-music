/*
 * LilyPondReaderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.model.barline.IBarline;
import com.github.toolarium.music.model.direction.IDirection;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.note.ILyric;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Tests for the LilyPond (.ly) reader.
 *
 * @author patrick
 */
public class LilyPondReaderTest {
    private static final Path LILYPOND_DIR = TestResourceUtil.getInstance().getSamplesDir().resolve("lilypond");
    private static final String LILYPOND = ".ly";


    /**
     * Tests that the factory returns a reader and that the format is detected by extension.
     */
    @Test
    void testFactoryAndFormatDetection() {
        assertNotNull(LilyPondIOFactory.getInstance().getReader());
        assertEquals(MusicFileFormat.LILYPOND, MusicFileFormat.fromFileName("Song.ly"));
        assertEquals(MusicFileFormat.LILYPOND, MusicFileFormat.fromFileName("part.ILY"));
    }


    /**
     * Tests that every LilyPond sample can be read and has notes.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadAllSamples() throws IOException {
        List<Path> files = TestResourceUtil.getInstance().findFilesByExtension(LILYPOND);
        assertTrue(files.size() >= 4, "samples: " + files.size());
        List<String> failures = TestResourceUtil.getInstance().processFiles(files, file -> {
            IScore score = MusicFileFactory.getInstance().read(file);
            assertFalse(score.getParts().isEmpty(), file.toString());
            assertTrue(notes(score.getParts().get(0)).size() > 0, file.toString());
            return null;
        });
        assertTrue(failures.isEmpty(), String.join(System.lineSeparator(), failures));
    }


    /**
     * Tests the basics sample: header, time, key, clef and tempo, key and clef changes, double accidentals and the
     * final barline.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadBasics() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(LILYPOND_DIR.resolve("basics.ly"));
        assertEquals("Basics", score.getWork().getWorkTitle());
        assertEquals("toolarium", score.getIdentification().getCreators().get(0).getValue());

        IPart part = score.getParts().get(0);
        assertEquals(9, part.getMeasures().size());
        IMeasure first = part.getMeasures().get(0);
        IAttributes attributes = first.getAttributes().get(0);
        assertEquals(2, attributes.getKeys().get(0).getFifths());
        assertEquals("4", attributes.getTimes().get(0).getBeats());
        assertEquals("G", attributes.getClefs().get(0).getSign());
        assertEquals("96", first.getDirections().get(0).getSound().getTempo());
        assertEquals(1.0, first.getNotes().get(2).getPitch().getAlter());

        assertEquals(-2, part.getMeasures().get(3).getAttributes().get(0).getKeys().get(0).getFifths());
        assertEquals("F", part.getMeasures().get(5).getAttributes().get(0).getClefs().get(0).getSign());
        List<? extends INote> eighth = part.getMeasures().get(7).getNotes();
        assertEquals(2.0, eighth.get(0).getPitch().getAlter());
        assertEquals(-2.0, eighth.get(1).getPitch().getAlter());
        assertEquals("light-heavy", part.getMeasures().get(8).getBarlines().get(0).getBarStyle());
    }


    /**
     * Tests the relative sample: the octaves of \relative and the durations that carry over.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadRelative() throws IOException {
        IPart part = MusicFileFactory.getInstance().read(LILYPOND_DIR.resolve("relative.ly")).getParts().get(0);
        assertEquals(5, part.getMeasures().size());
        List<? extends INote> first = part.getMeasures().get(0).getNotes();
        assertEquals("A", first.get(0).getPitch().getStep());
        assertEquals(4, first.get(0).getPitch().getOctave());
        assertEquals("A", first.get(3).getPitch().getStep());
        assertEquals(5, first.get(3).getPitch().getOctave());
        assertEquals("16th", first.get(2).getType());
        assertEquals(6, part.getMeasures().get(2).getNotes().get(3).getPitch().getOctave());
    }


    /**
     * Tests the tuplets and chords sample: triplets and quintuplets, two voices and chord symbols.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadTupletsAndChords() throws IOException {
        IPart part = MusicFileFactory.getInstance().read(LILYPOND_DIR.resolve("tuplets-chords.ly")).getParts().get(0);
        assertEquals(4, part.getMeasures().size());
        List<? extends INote> first = part.getMeasures().get(0).getNotes();
        assertEquals(3, first.get(0).getTimeModification().getActualNotes());
        assertEquals(5, first.get(6).getTimeModification().getActualNotes());
        assertEquals("C", part.getMeasures().get(0).getHarmonies().get(0).getRoot().getRootStep());
        assertEquals("dominant", part.getMeasures().get(1).getHarmonies().get(0).getKind());

        List<Integer> voices = new ArrayList<>();
        for (INote note : part.getMeasures().get(2).getNotes()) {
            if (!voices.contains(note.getVoice())) {
                voices.add(note.getVoice());
            }
        }
        assertEquals(2, voices.size());
    }


    /**
     * Tests the lyrics sample: syllables with hyphens on the notes of the melody.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadLyrics() throws IOException {
        IPart part = MusicFileFactory.getInstance().read(LILYPOND_DIR.resolve("lyrics.ly")).getParts().get(0);
        List<INote> notes = notes(part);
        ILyric lyric = notes.get(0).getLyrics().get(0);
        assertEquals("Sing", lyric.getText());
        assertEquals("begin", lyric.getSyllabic());
        assertEquals("end", notes.get(1).getLyrics().get(0).getSyllabic());
        assertEquals("single", notes.get(2).getLyrics().get(0).getSyllabic());
    }


    /**
     * Tests an inline source: variables, a pickup, a note over the barline, dynamics and hairpins, articulations,
     * slurs, a volta repeat with alternatives and a piano staff.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadInlineScore() throws IOException {
        String source = "melody = \\relative c'' { \\partial 4 g4\\p | c2.-> d4~ | d2( e4) f-. |\n"
                + "  \\repeat volta 2 { g1\\< | a1\\!\\f } \\alternative { { b1 } { c1 } } \\bar \"|.\" }\n"
                + "bass = { \\clef bass c1 c1 c1 c1 c1 c1 }\n"
                + "\\score { \\new PianoStaff \\with { instrumentName = \"Piano\" } << \\new Staff \\melody \\new Staff \\bass >> }\n";
        IScore score = MusicFileFactory.getInstance().read(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), MusicFileFormat.LILYPOND);
        assertEquals(1, score.getParts().size());
        IPart part = score.getParts().get(0);
        assertEquals(2, part.getMeasures().get(0).getAttributes().get(0).getStaves());
        assertEquals("yes", part.getMeasures().get(0).getImplicit());
        assertTrue(hasDynamics(part.getMeasures().get(0)));

        List<? extends INote> second = part.getMeasures().get(1).getNotes();
        assertNotNull(second.get(0).getNotations().getArticulations().getAccent());
        assertEquals("start", second.get(1).getTies().get(0).getType());
        assertEquals("stop", part.getMeasures().get(2).getNotes().get(0).getTies().get(0).getType());
        assertNotNull(part.getMeasures().get(2).getNotes().get(2).getNotations().getArticulations().getStaccato());

        assertEquals("forward", repeat(part.getMeasures().get(3)));
        assertEquals("1", ending(part.getMeasures().get(5)));
        assertEquals("backward", repeat(part.getMeasures().get(5)));
        assertEquals("2", ending(part.getMeasures().get(6)));
    }


    /**
     * Tests that input without music is rejected.
     */
    @Test
    void testRejectEmptyInput() {
        assertThrows(IOException.class, () -> MusicFileFactory.getInstance().read(new ByteArrayInputStream(new byte[0]), MusicFileFormat.LILYPOND));
        byte[] unclosed = "\\score { { c4 d4".getBytes(StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> MusicFileFactory.getInstance().read(new ByteArrayInputStream(unclosed), MusicFileFormat.LILYPOND));
    }


    /**
     * Collects the notes (without rests) of a part.
     *
     * @param part the part
     * @return the notes
     */
    private List<INote> notes(IPart part) {
        List<INote> result = new ArrayList<>();
        for (IMeasure measure : part.getMeasures()) {
            for (INote note : measure.getNotes()) {
                if (note.getRest() == null) {
                    result.add(note);
                }
            }
        }
        return result;
    }


    /**
     * Gets the ending number that starts in a measure.
     *
     * @param measure the measure
     * @return the ending number, or null
     */
    private String ending(IMeasure measure) {
        if (measure.getBarlines() == null) {
            return null;
        }
        for (IBarline barline : measure.getBarlines()) {
            if (barline.getEnding() != null && "start".equals(barline.getEnding().getType())) {
                return barline.getEnding().getNumber();
            }
        }
        return null;
    }


    /**
     * Gets the repeat direction of a measure (the backward repeat if it has both).
     *
     * @param measure the measure
     * @return forward, backward or null
     */
    private String repeat(IMeasure measure) {
        if (measure.getBarlines() == null) {
            return null;
        }
        String result = null;
        for (IBarline barline : measure.getBarlines()) {
            if (barline.getRepeat() != null && (result == null || "backward".equals(barline.getRepeat().getDirection()))) {
                result = barline.getRepeat().getDirection();
            }
        }
        return result;
    }


    /**
     * Checks if a measure has a dynamics direction.
     *
     * @param measure the measure
     * @return true if a dynamics direction exists
     */
    private boolean hasDynamics(IMeasure measure) {
        if (measure.getDirections() == null) {
            return false;
        }
        for (IDirection direction : measure.getDirections()) {
            for (IDirectionType type : direction.getDirectionTypes()) {
                if (type.getDynamics() != null) {
                    return true;
                }
            }
        }
        return false;
    }
}
