/*
 * MidiReaderWriterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.io.midi.MidiIOFactory;
import com.github.toolarium.music.io.midi.impl.MidiDurationConverter;
import com.github.toolarium.music.io.midi.impl.MidiNoteConverter;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.note.IPitch;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import com.github.toolarium.music.model.note.impl.PitchImpl;
import com.github.toolarium.music.model.part.impl.MidiInstrumentImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.part.impl.ScoreInstrumentImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for the MIDI reader, writer, and converter utilities.
 *
 * @author patrick
 */
public class MidiReaderWriterTest {
    private static final Logger LOG = LoggerFactory.getLogger(MidiReaderWriterTest.class);


    /**
     * Tests that MidiNoteConverter correctly converts middle C (MIDI 60) to C4.
     */
    @Test
    void testMidiNoteToMiddleC() {
        IPitch pitch = MidiNoteConverter.midiNoteToPitch(60);
        assertNotNull(pitch);
        assertEquals("C", pitch.getStep());
        assertEquals(Integer.valueOf(4), pitch.getOctave());
    }


    /**
     * Tests that MidiNoteConverter correctly round-trips various pitches.
     */
    @Test
    void testMidiNoteRoundTrip() {
        for (int note = 0; note < 128; note++) {
            IPitch pitch = MidiNoteConverter.midiNoteToPitch(note);
            int result = MidiNoteConverter.pitchToMidiNote(pitch);
            assertEquals(note, result, "Round-trip failed for MIDI note " + note);
        }
    }


    /**
     * Tests specific pitch-to-MIDI conversions.
     */
    @Test
    void testPitchToMidiNote() {
        PitchImpl pitch = new PitchImpl();
        pitch.setStep("A");
        pitch.setOctave(4);
        assertEquals(69, MidiNoteConverter.pitchToMidiNote(pitch));

        pitch.setStep("C");
        pitch.setOctave(4);
        pitch.setAlter(null);
        assertEquals(60, MidiNoteConverter.pitchToMidiNote(pitch));

        pitch.setStep("F");
        pitch.setOctave(4);
        pitch.setAlter(1.0);
        assertEquals(66, MidiNoteConverter.pitchToMidiNote(pitch));
    }


    /**
     * Tests that MidiDurationConverter quantizes common durations correctly.
     */
    @Test
    void testDurationQuantization() {
        int resolution = 480;

        // Quarter note
        String[] q = MidiDurationConverter.quantize(480, resolution);
        assertEquals("quarter", q[0]);
        assertEquals("0", q[1]);

        // Half note
        String[] h = MidiDurationConverter.quantize(960, resolution);
        assertEquals("half", h[0]);
        assertEquals("0", h[1]);

        // Eighth note
        String[] e = MidiDurationConverter.quantize(240, resolution);
        assertEquals("eighth", e[0]);
        assertEquals("0", e[1]);

        // Dotted quarter
        String[] dq = MidiDurationConverter.quantize(720, resolution);
        assertEquals("quarter", dq[0]);
        assertEquals("1", dq[1]);

        // Whole note
        String[] w = MidiDurationConverter.quantize(1920, resolution);
        assertEquals("whole", w[0]);
        assertEquals("0", w[1]);
    }


    /**
     * Tests writing a simple score to MIDI and reading it back.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteAndReadBack() throws IOException {
        ScoreImpl score = createSimpleScore();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MidiIOFactory.getInstance().getWriter().write(score, baos);

        byte[] midiBytes = baos.toByteArray();
        assertTrue(midiBytes.length > 0, "MIDI output should not be empty");
        LOG.info("Written MIDI size: {} bytes", midiBytes.length);

        IScore readBack = MidiIOFactory.getInstance().getReader().read(new ByteArrayInputStream(midiBytes));
        assertNotNull(readBack);
        assertNotNull(readBack.getParts());
        assertTrue(readBack.getParts().size() > 0, "Read-back score should have parts");

        LOG.info("Read back {} parts, first part has {} measures",
                readBack.getParts().size(),
                readBack.getParts().get(0).getMeasures().size());
    }


    /**
     * Tests round-trip via MusicFileFactory with file path.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testFacadeRoundTrip() throws IOException {
        ScoreImpl score = createSimpleScore();

        Path tempFile = Files.createTempFile("toolarium-midi-test-", ".mid");
        try {
            MusicFileFactory.getInstance().write(score, tempFile);
            assertTrue(Files.size(tempFile) > 0, "MIDI file should not be empty");

            IScore readBack = MusicFileFactory.getInstance().read(tempFile);
            assertNotNull(readBack);
            assertNotNull(readBack.getParts());
            assertTrue(readBack.getParts().size() > 0);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }


    /**
     * Tests format detection for MIDI extensions.
     */
    @Test
    void testFormatDetection() {
        assertEquals(MusicFileFormat.MIDI, MusicFileFormat.fromFileName("song.mid"));
        assertEquals(MusicFileFormat.MIDI, MusicFileFormat.fromFileName("song.midi"));
        assertEquals(MusicFileFormat.MIDI, MusicFileFormat.fromFileName("Song.MID"));
    }


    /**
     * Tests that reading from an explicit format works via input stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadWithExplicitFormat() throws IOException {
        ScoreImpl score = createSimpleScore();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MidiIOFactory.getInstance().getWriter().write(score, baos);

        IScore readBack = MusicFileFactory.getInstance().read(
                new ByteArrayInputStream(baos.toByteArray()), MusicFileFormat.MIDI);
        assertNotNull(readBack);
        assertNotNull(readBack.getParts());
    }


    /**
     * Tests that the MusicXML-to-MIDI conversion preserves pitch information.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testPitchPreservation() throws IOException {
        ScoreImpl score = createSimpleScore();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MidiIOFactory.getInstance().getWriter().write(score, baos);
        IScore readBack = MidiIOFactory.getInstance().getReader().read(new ByteArrayInputStream(baos.toByteArray()));

        assertNotNull(readBack.getParts());
        assertTrue(readBack.getParts().size() > 0);

        boolean foundNote = false;
        for (IPart part : readBack.getParts()) {
            if (part.getMeasures() != null) {
                for (IMeasure m : part.getMeasures()) {
                    if (m.getNotes() != null) {
                        for (INote n : m.getNotes()) {
                            if (n.getPitch() != null) {
                                foundNote = true;
                            }
                        }
                    }
                }
            }
        }

        assertTrue(foundNote, "Read-back score should contain pitched notes");
    }


    /**
     * Creates a simple score with one part and one measure containing four quarter notes (C-D-E-F).
     *
     * @return the constructed score
     */
    private ScoreImpl createSimpleScore() {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");

        ScorePartImpl scorePart = new ScorePartImpl();
        scorePart.setId("P1");
        scorePart.setPartName("Piano");
        ScoreInstrumentImpl si = new ScoreInstrumentImpl();
        si.setId("P1-I1");
        si.setInstrumentName("Piano");
        scorePart.getScoreInstruments().add(si);
        MidiInstrumentImpl mi = new MidiInstrumentImpl();
        mi.setId("P1-I1");
        mi.setMidiChannel(1);
        mi.setMidiProgram(1);
        scorePart.getMidiInstruments().add(mi);
        PartListImpl partList = new PartListImpl();
        partList.getScoreParts().add(scorePart);
        score.setPartList(partList);

        PartImpl part = new PartImpl();
        part.setId("P1");

        MeasureImpl measure = new MeasureImpl();
        measure.setNumber("1");

        AttributesImpl attr = new AttributesImpl();
        attr.setDivisions(1);
        TimeImpl time = new TimeImpl();
        time.setBeats("4");
        time.setBeatType("4");
        List<TimeImpl> times = new ArrayList<>();
        times.add(time);
        attr.setTimes(times);
        KeyImpl key = new KeyImpl();
        key.setFifths(0);
        key.setMode("major");
        List<KeyImpl> keys = new ArrayList<>();
        keys.add(key);
        attr.setKeys(keys);
        List<AttributesImpl> attrs = new ArrayList<>();
        attrs.add(attr);
        measure.setAttributes(attrs);

        List<NoteImpl> notes = new ArrayList<>();
        notes.add(createQuarterNote("C", 4, 1));
        notes.add(createQuarterNote("D", 4, 1));
        notes.add(createQuarterNote("E", 4, 1));
        notes.add(createQuarterNote("F", 4, 1));
        measure.setNotes(notes);

        List<MeasureImpl> measures = new ArrayList<>();
        measures.add(measure);
        part.setMeasures(measures);

        List<PartImpl> parts = new ArrayList<>();
        parts.add(part);
        score.setParts(parts);

        return score;
    }


    /**
     * Creates a quarter note with the given pitch.
     *
     * @param step the step letter
     * @param octave the octave
     * @param duration the duration in divisions
     * @return the constructed note
     */
    private NoteImpl createQuarterNote(String step, int octave, int duration) {
        NoteImpl note = new NoteImpl();
        PitchImpl pitch = new PitchImpl();
        pitch.setStep(step);
        pitch.setOctave(octave);
        note.setPitch(pitch);
        note.setDuration(duration);
        note.setType("quarter");
        note.setVoice(1);
        return note;
    }
}
