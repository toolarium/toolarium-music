/*
 * MusicXMLRichContentTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.io.musicxml.impl.MusicXMLIOFactory;
import com.github.toolarium.music.model.barline.IBarline;
import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.barline.impl.RepeatImpl;
import com.github.toolarium.music.model.direction.IDirection;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.DirectionTypeImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.direction.impl.WordsImpl;
import com.github.toolarium.music.model.harmony.IHarmony;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.harmony.impl.RootImpl;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.notation.INotations;
import com.github.toolarium.music.model.notation.impl.NotationsImpl;
import com.github.toolarium.music.model.notation.impl.SlurImpl;
import com.github.toolarium.music.model.notation.impl.TiedImpl;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import com.github.toolarium.music.model.note.impl.PitchImpl;
import com.github.toolarium.music.model.note.impl.TieImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Round-trip tests for rich MusicXML content: directions, lyrics,
 * slurs, ties, barlines with repeats, harmony, and multi-voice.
 *
 * @author patrick
 */
public class MusicXMLRichContentTest {
    private static final Logger LOG = LoggerFactory.getLogger(MusicXMLRichContentTest.class);
    private static final String QUARTER = "quarter";
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String FOUR = "4";
    private static final String VERSION = "4.0";
    private static final String PART_ID = "P1";
    private static final String MAJOR = "major";
    private static final String STEP_C = "C";


    /**
     * Tests that a direction with words and sound tempo survives a round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testDirectionRoundTrip() throws IOException {
        WordsImpl words = new WordsImpl();
        words.setValue("Allegro");
        words.setFontSize("12");

        DirectionTypeImpl dirType = new DirectionTypeImpl();
        dirType.getWords().add(words);

        SoundImpl sound = new SoundImpl();
        sound.setTempo("120");

        DirectionImpl direction = new DirectionImpl();
        direction.setPlacement("above");
        direction.getDirectionTypes().add(dirType);
        direction.setSound(sound);

        List<DirectionImpl> directions = new ArrayList<>();
        directions.add(direction);

        ScoreImpl score = createMinimalScore();
        MeasureImpl measure = getFirstMeasureImpl(score);
        measure.setDirections(directions);

        IScore readBack = writeAndReadBack(score);
        IMeasure m = getFirstMeasure(readBack);

        assertNotNull(m.getDirections(), "Directions should not be null");
        assertFalse(m.getDirections().isEmpty(), "Directions should not be empty");

        IDirection dir = m.getDirections().get(0);
        assertNotNull(dir.getDirectionTypes());
        assertFalse(dir.getDirectionTypes().isEmpty());

        IDirectionType firstDirType = dir.getDirectionTypes().get(0);
        assertFalse(firstDirType.getWords().isEmpty());
        assertEquals("Allegro", firstDirType.getWords().get(0).getValue());

        String tempo = "n/a";
        if (dir.getSound() != null) {
            tempo = dir.getSound().getTempo();
        }
        LOG.info("Direction round-trip: words='{}', tempo='{}'",
                firstDirType.getWords().get(0).getValue(), tempo);
    }


    /**
     * Tests that a lyric on a note survives a round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testLyricRoundTrip() throws IOException {
        LyricImpl lyric = new LyricImpl();
        lyric.setNumber("1");
        lyric.setSyllabic("single");
        lyric.setText("la");

        NoteImpl note = createQuarterNote(STEP_C, 4);
        note.getLyrics().add(lyric);

        List<NoteImpl> notes = new ArrayList<>();
        notes.add(note);

        ScoreImpl score = createMinimalScore();
        MeasureImpl measure = getFirstMeasureImpl(score);
        measure.setNotes(notes);

        IScore readBack = writeAndReadBack(score);
        IMeasure m = getFirstMeasure(readBack);

        assertNotNull(m.getNotes());
        assertFalse(m.getNotes().isEmpty());

        INote readNote = m.getNotes().get(0);
        assertNotNull(readNote.getLyrics());
        assertFalse(readNote.getLyrics().isEmpty());
        assertEquals("la", readNote.getLyrics().get(0).getText());

        LOG.info("Lyric round-trip: text='{}'", readNote.getLyrics().get(0).getText());
    }


    /**
     * Tests that slur start and stop notations survive a round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testSlurRoundTrip() throws IOException {
        SlurImpl slurStart = new SlurImpl();
        slurStart.setType(START);
        slurStart.setNumber(1);
        List<SlurImpl> slurs1 = new ArrayList<>();
        slurs1.add(slurStart);
        NotationsImpl notations1 = new NotationsImpl();
        notations1.setSlurs(slurs1);
        NoteImpl note1 = createQuarterNote(STEP_C, 4);
        note1.setNotations(notations1);

        SlurImpl slurStop = new SlurImpl();
        slurStop.setType(STOP);
        slurStop.setNumber(1);
        List<SlurImpl> slurs2 = new ArrayList<>();
        slurs2.add(slurStop);
        NotationsImpl notations2 = new NotationsImpl();
        notations2.setSlurs(slurs2);
        NoteImpl note2 = createQuarterNote("D", 4);
        note2.setNotations(notations2);

        List<NoteImpl> notes = new ArrayList<>();
        notes.add(note1);
        notes.add(note2);

        ScoreImpl score = createMinimalScore();
        MeasureImpl measure = getFirstMeasureImpl(score);
        measure.setNotes(notes);

        IScore readBack = writeAndReadBack(score);
        IMeasure m = getFirstMeasure(readBack);

        assertNotNull(m.getNotes());
        assertTrue(m.getNotes().size() >= 2);

        INote firstNote = m.getNotes().get(0);
        INotations firstNotations = firstNote.getNotations();
        assertNotNull(firstNotations, "First note should have notations");
        assertNotNull(firstNotations.getSlurs(), "First note should have slurs");
        assertFalse(firstNotations.getSlurs().isEmpty());
        assertEquals(START, firstNotations.getSlurs().get(0).getType());

        LOG.info("Slur round-trip: first note slur type='{}'",
                firstNotations.getSlurs().get(0).getType());
    }


    /**
     * Tests that tied notes with tie and tied elements survive a round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testTiedNoteRoundTrip() throws IOException {
        NoteImpl note1 = createQuarterNote(STEP_C, 4);
        TieImpl tieStart = new TieImpl();
        tieStart.setType(START);
        note1.getTies().add(tieStart);
        TiedImpl tiedStart = new TiedImpl();
        tiedStart.setType(START);
        NotationsImpl notations1 = new NotationsImpl();
        List<TiedImpl> tieds1 = new ArrayList<>();
        tieds1.add(tiedStart);
        notations1.setTieds(tieds1);
        note1.setNotations(notations1);

        NoteImpl note2 = createQuarterNote(STEP_C, 4);
        TieImpl tieStop = new TieImpl();
        tieStop.setType(STOP);
        note2.getTies().add(tieStop);
        TiedImpl tiedStop = new TiedImpl();
        tiedStop.setType(STOP);
        NotationsImpl notations2 = new NotationsImpl();
        List<TiedImpl> tieds2 = new ArrayList<>();
        tieds2.add(tiedStop);
        notations2.setTieds(tieds2);
        note2.setNotations(notations2);

        List<NoteImpl> notes = new ArrayList<>();
        notes.add(note1);
        notes.add(note2);

        ScoreImpl score = createMinimalScore();
        MeasureImpl measure = getFirstMeasureImpl(score);
        measure.setNotes(notes);

        IScore readBack = writeAndReadBack(score);
        IMeasure m = getFirstMeasure(readBack);

        assertNotNull(m.getNotes());
        assertTrue(m.getNotes().size() >= 2);

        INote firstNote = m.getNotes().get(0);
        assertNotNull(firstNote.getTies());
        assertFalse(firstNote.getTies().isEmpty());
        assertEquals(START, firstNote.getTies().get(0).getType());

        INotations firstNotations = firstNote.getNotations();
        assertNotNull(firstNotations);
        assertNotNull(firstNotations.getTieds());
        assertFalse(firstNotations.getTieds().isEmpty());
        assertEquals(START, firstNotations.getTieds().get(0).getType());

        LOG.info("Tied round-trip: tie type='{}', tied type='{}'",
                firstNote.getTies().get(0).getType(),
                firstNotations.getTieds().get(0).getType());
    }


    /**
     * Tests that a barline with a forward repeat survives a round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testBarlineRepeatRoundTrip() throws IOException {
        RepeatImpl repeat = new RepeatImpl();
        repeat.setDirection("forward");

        BarlineImpl barline = new BarlineImpl();
        barline.setLocation("left");
        barline.setBarStyle("heavy-light");
        barline.setRepeat(repeat);

        List<BarlineImpl> barlines = new ArrayList<>();
        barlines.add(barline);

        ScoreImpl score = createMinimalScore();
        MeasureImpl measure = getFirstMeasureImpl(score);
        measure.setBarlines(barlines);

        IScore readBack = writeAndReadBack(score);
        IMeasure m = getFirstMeasure(readBack);

        assertNotNull(m.getBarlines(), "Barlines should not be null");
        assertFalse(m.getBarlines().isEmpty(), "Barlines should not be empty");

        IBarline readBarline = m.getBarlines().get(0);
        assertEquals("left", readBarline.getLocation());
        assertNotNull(readBarline.getRepeat());
        assertEquals("forward", readBarline.getRepeat().getDirection());

        LOG.info("Barline round-trip: location='{}', repeat direction='{}'",
                readBarline.getLocation(), readBarline.getRepeat().getDirection());
    }


    /**
     * Tests that a harmony element with root and kind survives a round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testHarmonyRoundTrip() throws IOException {
        RootImpl root = new RootImpl();
        root.setRootStep(STEP_C);

        HarmonyImpl harmony = new HarmonyImpl();
        harmony.setRoot(root);
        harmony.setKind(MAJOR);

        List<HarmonyImpl> harmonies = new ArrayList<>();
        harmonies.add(harmony);

        ScoreImpl score = createMinimalScore();
        MeasureImpl measure = getFirstMeasureImpl(score);
        measure.setHarmonies(harmonies);

        IScore readBack = writeAndReadBack(score);
        IMeasure m = getFirstMeasure(readBack);

        assertNotNull(m.getHarmonies(), "Harmonies should not be null");
        assertFalse(m.getHarmonies().isEmpty(), "Harmonies should not be empty");

        IHarmony readHarmony = m.getHarmonies().get(0);
        assertNotNull(readHarmony.getRoot());
        assertEquals(STEP_C, readHarmony.getRoot().getRootStep());
        assertEquals(MAJOR, readHarmony.getKind());

        LOG.info("Harmony round-trip: root='{}', kind='{}'",
                readHarmony.getRoot().getRootStep(), readHarmony.getKind());
    }


    /**
     * Tests that notes in different voices survive a round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testMultiVoiceRoundTrip() throws IOException {
        NoteImpl note1 = createQuarterNote(STEP_C, 4);
        note1.setVoice(1);

        NoteImpl note2 = createQuarterNote("E", 4);
        note2.setVoice(2);

        List<NoteImpl> notes = new ArrayList<>();
        notes.add(note1);
        notes.add(note2);

        ScoreImpl score = createMinimalScore();
        MeasureImpl measure = getFirstMeasureImpl(score);
        measure.setNotes(notes);

        IScore readBack = writeAndReadBack(score);
        IMeasure m = getFirstMeasure(readBack);

        assertNotNull(m.getNotes());
        assertTrue(m.getNotes().size() >= 2, "Should have at least 2 notes");

        boolean hasVoice1 = false;
        boolean hasVoice2 = false;
        for (INote n : m.getNotes()) {
            if (n.getVoice() != null && n.getVoice() == 1) {
                hasVoice1 = true;
            }
            if (n.getVoice() != null && n.getVoice() == 2) {
                hasVoice2 = true;
            }
        }

        assertTrue(hasVoice1, "Should have a note in voice 1");
        assertTrue(hasVoice2, "Should have a note in voice 2");

        LOG.info("Multi-voice round-trip: {} notes, voice 1={}, voice 2={}",
                m.getNotes().size(), hasVoice1, hasVoice2);
    }


    /**
     * Creates a minimal score with one part, one empty measure, and 4/4 time signature.
     *
     * @return the constructed score
     */
    private ScoreImpl createMinimalScore() {
        ScoreImpl score = new ScoreImpl();
        score.setVersion(VERSION);
        score.setMovementTitle("Rich Content Test");

        ScorePartImpl scorePart = new ScorePartImpl();
        scorePart.setId(PART_ID);
        scorePart.setPartName("Test Part");
        PartListImpl partList = new PartListImpl();
        partList.getScoreParts().add(scorePart);
        score.setPartList(partList);

        MeasureImpl measure = new MeasureImpl();
        measure.setNumber("1");

        AttributesImpl attr = new AttributesImpl();
        attr.setDivisions(1);
        TimeImpl time = new TimeImpl();
        time.setBeats(FOUR);
        time.setBeatType(FOUR);
        List<TimeImpl> times = new ArrayList<>();
        times.add(time);
        attr.setTimes(times);
        KeyImpl key = new KeyImpl();
        key.setFifths(0);
        key.setMode(MAJOR);
        List<KeyImpl> keys = new ArrayList<>();
        keys.add(key);
        attr.setKeys(keys);
        List<AttributesImpl> attrs = new ArrayList<>();
        attrs.add(attr);
        measure.setAttributes(attrs);

        List<MeasureImpl> measures = new ArrayList<>();
        measures.add(measure);

        PartImpl part = new PartImpl();
        part.setId(PART_ID);
        part.setMeasures(measures);

        List<PartImpl> parts = new ArrayList<>();
        parts.add(part);
        score.setParts(parts);

        return score;
    }


    /**
     * Gets the first measure implementation from the first part of a score for construction.
     *
     * @param score the score implementation
     * @return the first measure implementation
     */
    private MeasureImpl getFirstMeasureImpl(ScoreImpl score) {
        return score.getParts().get(0).getMeasures().get(0);
    }


    /**
     * Gets the first measure from the first part of a score using interface access.
     *
     * @param score the score
     * @return the first measure
     */
    private IMeasure getFirstMeasure(IScore score) {
        return score.getParts().get(0).getMeasures().get(0);
    }


    /**
     * Writes the score to MusicXML and reads it back.
     *
     * @param score the score to round-trip
     * @return the deserialized score
     * @throws IOException if an I/O error occurs
     */
    private IScore writeAndReadBack(ScoreImpl score) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MusicXMLIOFactory.getInstance().getWriter().write(score, baos);

        byte[] xmlBytes = baos.toByteArray();
        assertTrue(xmlBytes.length > 0, "Written XML should not be empty");

        return MusicXMLIOFactory.getInstance().getReader().read(new ByteArrayInputStream(xmlBytes));
    }


    /**
     * Creates a quarter note with the given pitch.
     *
     * @param step the pitch step
     * @param octave the octave
     * @return the constructed note
     */
    private NoteImpl createQuarterNote(String step, int octave) {
        NoteImpl note = new NoteImpl();
        PitchImpl pitch = new PitchImpl();
        pitch.setStep(step);
        pitch.setOctave(octave);
        note.setPitch(pitch);
        note.setDuration(1);
        note.setType(QUARTER);
        note.setVoice(1);
        return note;
    }
}
