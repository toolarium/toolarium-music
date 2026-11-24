/*
 * AbcReaderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Tests for the ABC notation (.abc) reader.
 *
 * @author patrick
 */
public class AbcReaderTest {
    private static final Path ABC_DIR = TestResourceUtil.getInstance().getSamplesDir().resolve("abc");
    private static final String ABC = ".abc";


    /**
     * Tests that the factory returns a reader and that the format is detected by extension.
     */
    @Test
    void testFactoryAndFormatDetection() {
        assertNotNull(AbcIOFactory.getInstance().getReader());
        assertEquals(MusicFileFormat.ABC, MusicFileFormat.fromFileName("Tune.ABC"));
    }


    /**
     * Tests that every ABC sample can be read and has notes.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadAllSamples() throws IOException {
        List<Path> files = TestResourceUtil.getInstance().findFilesByExtension(ABC);
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
     * Tests the feature sample: header fields, key, meter, tempo, pickup, ties, broken rhythm,
     * tuplets, chords, grace notes, endings, parts, rests, lyrics and a meter change.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadFeatures() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(ABC_DIR.resolve("features.abc"));
        assertEquals("Feature Test", score.getWork().getWorkTitle());
        assertEquals("Test Composer", score.getIdentification().getCreators().get(0).getValue());
        assertEquals(1, score.getParts().size());

        IPart part = score.getParts().get(0);
        assertEquals(12, part.getMeasures().size());
        IMeasure first = part.getMeasures().get(0);
        assertEquals("yes", first.getImplicit());
        IAttributes attributes = first.getAttributes().get(0);
        assertEquals(2, attributes.getKeys().get(0).getFifths());
        assertEquals("3", attributes.getTimes().get(0).getBeats());
        assertEquals("96", first.getDirections().get(0).getSound().getTempo());
        assertEquals("D", first.getHarmonies().get(0).getRoot().getRootStep());

        // the key of D major sharpens F and C
        List<? extends INote> second = part.getMeasures().get(1).getNotes();
        assertEquals("F", second.get(1).getPitch().getStep());
        assertEquals(1.0, second.get(1).getPitch().getAlter());

        // broken rhythm B>c: dotted eighth and sixteenth; d2- d2 is tied
        List<? extends INote> third = part.getMeasures().get(2).getNotes();
        assertEquals("eighth", third.get(0).getType());
        assertEquals(1, third.get(0).getDots().size());
        assertEquals("16th", third.get(1).getType());
        assertEquals("start", third.get(2).getTies().get(0).getType());
        assertEquals("stop", third.get(3).getTies().get(0).getType());

        // triplet and chord
        List<? extends INote> fourth = part.getMeasures().get(3).getNotes();
        assertEquals(3, fourth.get(0).getTimeModification().getActualNotes());
        assertEquals("", fourth.get(5).getChord());

        // grace note and first ending, second ending
        assertNotNull(part.getMeasures().get(4).getNotes().get(0).getGrace());
        assertEquals("1", ending(part.getMeasures().get(4)));
        assertEquals("2", ending(part.getMeasures().get(5)));

        // accidentals with naturals, repeat, invisible rest and multi-measure rest
        List<? extends INote> seventh = part.getMeasures().get(6).getNotes();
        assertEquals("sharp", seventh.get(0).getAccidental());
        assertNull(seventh.get(1).getPitch().getAlter());
        assertEquals(-1.0, seventh.get(2).getPitch().getAlter());
        assertEquals("no", part.getMeasures().get(7).getNotes().get(1).getPrintObject());
        assertEquals("yes", part.getMeasures().get(9).getNotes().get(0).getRest().getMeasure());
        assertEquals("4", part.getMeasures().get(11).getAttributes().get(0).getTimes().get(0).getBeats());

        // lyrics: Hel-lo
        ILyric lyric = part.getMeasures().get(0).getNotes().get(0).getLyrics().get(0);
        assertEquals("Hel", lyric.getText());
        assertEquals("begin", lyric.getSyllabic());
    }


    /**
     * Tests a two-voice score grouped into one part with two staves (%%score {RH LH}).
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadTwoStaves() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(ABC_DIR.resolve("voices.abc"));
        assertEquals(1, score.getParts().size());
        assertEquals("Piano", score.getPartList().getScoreParts().get(0).getPartName());
        IAttributes attributes = score.getParts().get(0).getMeasures().get(0).getAttributes().get(0);
        assertEquals(2, attributes.getStaves());
        assertEquals("G", attributes.getClefs().get(0).getSign());
        assertEquals("F", attributes.getClefs().get(1).getSign());
        INote flatB = score.getParts().get(0).getMeasures().get(0).getNotes().get(2);
        assertEquals("B", flatB.getPitch().getStep());
        assertEquals(-1.0, flatB.getPitch().getAlter());
        assertEquals(2, score.getParts().get(0).getMeasures().get(0).getNotes().get(4).getStaff());
    }


    /**
     * Tests the partial measures of a jig: an anacrusis, its complement before a repeat and the
     * anacrusis of the second section, and the endings.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadPartialMeasuresAndEndings() throws IOException {
        IPart part = MusicFileFactory.getInstance().read(ABC_DIR.resolve("jig.abc")).getParts().get(0);
        assertEquals(11, part.getMeasures().size());
        assertEquals(1, part.getMeasures().get(0).getNotes().size());
        assertEquals(4, part.getMeasures().get(4).getNotes().size());
        assertEquals("yes", part.getMeasures().get(5).getImplicit());
        assertEquals("backward", repeat(part.getMeasures().get(9)));
        assertEquals("1", ending(part.getMeasures().get(9)));
        assertEquals("2", ending(part.getMeasures().get(10)));
    }


    /**
     * Tests two lyrics verses with hyphens and an extension line.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadLyricsVerses() throws IOException {
        IPart part = MusicFileFactory.getInstance().read(ABC_DIR.resolve("song.abc")).getParts().get(0);
        List<INote> notes = notes(part);
        assertEquals("Here", notes.get(0).getLyrics().get(0).getText());
        assertEquals("Now", notes.get(0).getLyrics().get(1).getText());
        assertEquals("2", notes.get(0).getLyrics().get(1).getNumber());
        assertEquals("begin", notes.get(2).getLyrics().get(1).getSyllabic());
        assertEquals("end", notes.get(3).getLyrics().get(1).getSyllabic());
        assertNotNull(notes.get(6).getLyrics().get(1).getExtend());
    }


    /**
     * Tests an inline tune: accidentals that last to the bar line, decorations, dynamics, a chord
     * symbol, an annotation and a voice overlay.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadInlineTune() throws IOException {
        String abc = "X:1\nT:Inline\nM:4/4\nL:1/4\nK:C\n"
                + "\"Am7\"^F F !p!.G !trill!A | F \"^dolce\"G2 z & c4 |]\n";
        IScore score = MusicFileFactory.getInstance().read(new ByteArrayInputStream(abc.getBytes(StandardCharsets.UTF_8)), MusicFileFormat.ABC);
        IMeasure first = score.getParts().get(0).getMeasures().get(0);
        assertEquals("minor-seventh", first.getHarmonies().get(0).getKind());
        assertEquals(1.0, first.getNotes().get(1).getPitch().getAlter());
        assertNotNull(first.getNotes().get(2).getNotations().getArticulations().getStaccato());
        assertNotNull(first.getNotes().get(3).getNotations().getOrnaments().getTrillMark());
        assertTrue(hasDynamics(first));

        IMeasure second = score.getParts().get(0).getMeasures().get(1);
        assertNull(second.getNotes().get(0).getPitch().getAlter());
        List<Integer> voices = new ArrayList<>();
        for (INote note : second.getNotes()) {
            if (!voices.contains(note.getVoice())) {
                voices.add(note.getVoice());
            }
        }
        assertEquals(2, voices.size());
        assertEquals("light-heavy", second.getBarlines().get(0).getBarStyle());
    }


    /**
     * Tests that input without music is rejected.
     */
    @Test
    void testRejectEmptyInput() {
        assertThrows(IOException.class, () -> MusicFileFactory.getInstance().read(new ByteArrayInputStream(new byte[0]), MusicFileFormat.ABC));
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
     * Gets the repeat direction of a measure.
     *
     * @param measure the measure
     * @return forward, backward or null
     */
    private String repeat(IMeasure measure) {
        if (measure.getBarlines() == null) {
            return null;
        }
        for (IBarline barline : measure.getBarlines()) {
            if (barline.getRepeat() != null) {
                return barline.getRepeat().getDirection();
            }
        }
        return null;
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
