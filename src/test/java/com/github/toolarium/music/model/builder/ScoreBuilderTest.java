/*
 * ScoreBuilderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.part.IScorePart;
import com.github.toolarium.music.model.score.IScore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link ScoreBuilder}.
 *
 * @author patrick
 */
public class ScoreBuilderTest {

    private static final String BEATS_4 = "4";
    private static final String KEY_MAJOR = "major";
    private static final String NOTE_E = "E";
    private static final String NOTE_F = "F";
    private static final String NOTE_G = "G";
    private static final String TYPE_QUARTER = "quarter";

    /**
     * Builds a score with one part and one measure and verifies metadata, attributes and notes.
     */
    @Test
    void testBuildSimpleScore() {
        IScore score = ScoreBuilder.builder()
                .title("Ode to Joy")
                .composer("Ludwig van Beethoven")
                .part("Violin")
                    .measure(1)
                        .time(BEATS_4, BEATS_4)
                        .key(0, KEY_MAJOR)
                        .clef(NOTE_G, 2)
                        .note(NOTE_E, 4, TYPE_QUARTER)
                        .note(NOTE_E, 4, TYPE_QUARTER)
                        .note(NOTE_F, 4, TYPE_QUARTER)
                        .note(NOTE_G, 4, TYPE_QUARTER)
                    .end()
                .end()
                .build();

        assertEquals("4.0", score.getVersion());
        assertEquals("Ode to Joy", score.getMovementTitle());
        assertNotNull(score.getIdentification());
        assertEquals(1, score.getIdentification().getCreators().size());
        assertEquals("composer", score.getIdentification().getCreators().get(0).getType());
        assertEquals("Ludwig van Beethoven", score.getIdentification().getCreators().get(0).getValue());

        List<? extends IScorePart> scoreParts = score.getPartList().getScoreParts();
        assertEquals(1, scoreParts.size());
        assertEquals("P1", scoreParts.get(0).getId());
        assertEquals("Violin", scoreParts.get(0).getPartName());

        List<? extends IPart> parts = score.getParts();
        assertEquals(1, parts.size());
        IPart part = parts.get(0);
        assertEquals("P1", part.getId());
        assertEquals(1, part.getMeasures().size());

        IMeasure measure = part.getMeasures().get(0);
        assertEquals("1", measure.getNumber());

        List<? extends IAttributes> attrs = measure.getAttributes();
        assertEquals(1, attrs.size());
        assertEquals(MeasureBuilder.DIVISIONS, attrs.get(0).getDivisions().intValue());
        assertEquals(0, attrs.get(0).getKeys().get(0).getFifths().intValue());
        assertEquals(KEY_MAJOR, attrs.get(0).getKeys().get(0).getMode());
        assertEquals(BEATS_4, attrs.get(0).getTimes().get(0).getBeats());
        assertEquals(BEATS_4, attrs.get(0).getTimes().get(0).getBeatType());
        assertEquals(NOTE_G, attrs.get(0).getClefs().get(0).getSign());
        assertEquals(2, attrs.get(0).getClefs().get(0).getLine().intValue());

        List<? extends INote> notes = measure.getNotes();
        assertEquals(4, notes.size());
        assertEquals(NOTE_E, notes.get(0).getPitch().getStep());
        assertEquals(4, notes.get(0).getPitch().getOctave().intValue());
        assertEquals(TYPE_QUARTER, notes.get(0).getType());
        assertEquals(MeasureBuilder.DIVISIONS, notes.get(0).getDuration().intValue());
        assertEquals(NOTE_F, notes.get(2).getPitch().getStep());
        assertEquals(NOTE_G, notes.get(3).getPitch().getStep());
    }


    /**
     * Tests rests, dotted notes and accidentals.
     */
    @Test
    void testDotsAlterAndRest() {
        IScore score = ScoreBuilder.builder()
                .part("Piano")
                    .measure(1)
                        .time(BEATS_4, BEATS_4)
                        .key(0, KEY_MAJOR)
                        .clef(NOTE_G, 2)
                        .note(NOTE_F, 4, 1.0, "eighth")
                        .note(NOTE_G, 4, "eighth", 1)
                        .rest("quarter")
                    .end()
                .end()
                .build();

        IMeasure measure = score.getParts().get(0).getMeasures().get(0);
        List<? extends INote> notes = measure.getNotes();
        assertEquals(3, notes.size());

        INote fsharp = notes.get(0);
        assertEquals(NOTE_F, fsharp.getPitch().getStep());
        assertEquals(1.0, fsharp.getPitch().getAlter().doubleValue(), 0.001);
        assertEquals("eighth", fsharp.getType());
        assertEquals(MeasureBuilder.DIVISIONS / 2, fsharp.getDuration().intValue());

        INote dottedG = notes.get(1);
        assertEquals(NOTE_G, dottedG.getPitch().getStep());
        assertNull(dottedG.getPitch().getAlter());
        assertEquals("eighth", dottedG.getType());
        int expectedDotted = MeasureBuilder.DIVISIONS / 2 + MeasureBuilder.DIVISIONS / 4;
        assertEquals(expectedDotted, dottedG.getDuration().intValue());
        assertEquals(1, dottedG.getDots().size());

        INote rest = notes.get(2);
        assertNotNull(rest.getRest());
        assertNull(rest.getPitch());
        assertEquals("quarter", rest.getType());
    }


    /**
     * Tests a multi-part score and verifies that it can be written to MXL and read back.
     *
     * @throws Exception if the round-trip fails
     */
    @Test
    void testMultiPartRoundTrip() throws Exception {
        IScore score = ScoreBuilder.builder()
                .title("Test Score")
                .part("Violin")
                    .measure(1)
                        .time(BEATS_4, BEATS_4).key(0, KEY_MAJOR).clef(NOTE_G, 2)
                        .note("C", 5, TYPE_QUARTER)
                        .note("D", 5, TYPE_QUARTER)
                        .note(NOTE_E, 5, TYPE_QUARTER)
                        .note(NOTE_F, 5, TYPE_QUARTER)
                    .end()
                    .measure(2)
                        .note(NOTE_G, 5, "half")
                        .rest("half")
                    .end()
                .end()
                .part("Cello")
                    .measure(1)
                        .time(BEATS_4, BEATS_4).key(0, KEY_MAJOR).clef(NOTE_F, 4)
                        .note("C", 3, "whole")
                    .end()
                    .measure(2)
                        .note("G", 2, "whole")
                    .end()
                .end()
                .build();

        assertEquals(2, score.getParts().size());
        assertEquals("P1", score.getParts().get(0).getId());
        assertEquals("P2", score.getParts().get(1).getId());
        assertEquals(2, score.getParts().get(0).getMeasures().size());
        assertEquals(2, score.getParts().get(1).getMeasures().size());

        Path tmp = Files.createTempFile("scbuilder-", ".mxl");
        try {
            MusicFileFactory.getInstance().write(score, tmp);
            assertTrue(Files.size(tmp) > 0);
            IScore reread = MusicFileFactory.getInstance().read(tmp);
            assertNotNull(reread);
            assertEquals(2, reread.getParts().size());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }


    /**
     * Tests the {@link NoteBuilder} for voice and chord notes.
     */
    @Test
    void testNoteBuilderVoiceAndChord() {
        IScore score = ScoreBuilder.builder()
                .part("Piano")
                    .measure(1)
                        .time(BEATS_4, BEATS_4).key(0, KEY_MAJOR).clef(NOTE_G, 2)
                        .beginNote("C", 4, TYPE_QUARTER).voice(1).end()
                        .beginNote(NOTE_E, 4, TYPE_QUARTER).voice(1).chord().end()
                        .beginNote(NOTE_G, 4, TYPE_QUARTER).voice(1).chord().end()
                        .beginRest(TYPE_QUARTER).voice(2).end()
                    .end()
                .end()
                .build();

        IMeasure measure = score.getParts().get(0).getMeasures().get(0);
        List<? extends INote> notes = measure.getNotes();
        assertEquals(4, notes.size());

        assertEquals("C", notes.get(0).getPitch().getStep());
        assertNull(notes.get(0).getChord());
        assertEquals(1, notes.get(0).getVoice().intValue());

        assertEquals(NOTE_E, notes.get(1).getPitch().getStep());
        assertNotNull(notes.get(1).getChord());

        assertEquals(NOTE_G, notes.get(2).getPitch().getStep());
        assertNotNull(notes.get(2).getChord());

        assertNotNull(notes.get(3).getRest());
        assertNull(notes.get(3).getPitch());
        assertEquals(2, notes.get(3).getVoice().intValue());
    }


    /**
     * Tests the work title and multiple credits.
     */
    @Test
    void testWorkTitleAndMultipleCredits() {
        IScore score = ScoreBuilder.builder()
                .workTitle("Symphony No. 9")
                .title("IV. Finale")
                .composer("Ludwig van Beethoven")
                .lyricist("Friedrich Schiller")
                .arranger("Test Arranger")
                .part("Voice")
                    .measure(1)
                        .time(BEATS_4, BEATS_4).key(0, KEY_MAJOR).clef(NOTE_G, 2)
                        .note("A", 4, "whole")
                    .end()
                .end()
                .build();

        assertNotNull(score.getWork());
        assertEquals("Symphony No. 9", score.getWork().getWorkTitle());
        assertEquals("IV. Finale", score.getMovementTitle());
        assertEquals(3, score.getIdentification().getCreators().size());
        assertEquals("composer", score.getIdentification().getCreators().get(0).getType());
        assertEquals("lyricist", score.getIdentification().getCreators().get(1).getType());
        assertEquals("arranger", score.getIdentification().getCreators().get(2).getType());
    }
}
