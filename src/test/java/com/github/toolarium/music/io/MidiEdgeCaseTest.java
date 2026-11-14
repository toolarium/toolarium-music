/*
 * MidiEdgeCaseTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.midi.MidiIOFactory;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.note.INote;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Edge-case tests for the MIDI reader and writer.
 * Covers real MIDI file reading, multi-part scores, chords,
 * dotted durations, empty scores, and format detection.
 *
 * @author patrick
 */
public class MidiEdgeCaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(MidiEdgeCaseTest.class);
    private static final String QUARTER = "quarter";


    /**
     * Discovers and reads all MIDI files (.mid, .midi) found under the test resources directory.
     * If no MIDI files are present, the test logs a message and passes gracefully.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadRealMidiFiles() throws IOException {
        List<Path> midiFiles = TestResourceUtil.getInstance().findFilesByExtension(".mid", ".midi");
        assertFalse(midiFiles.isEmpty(), "No MIDI sample files found under samples directory.");

        int passed = 0;
        List<String> failures = new ArrayList<>();

        for (Path midiPath : midiFiles) {
            try {
                IScore score = MidiIOFactory.getInstance().getReader().read(midiPath);
                assertNotNull(score, "Score should not be null for: " + midiPath);
                assertNotNull(score.getParts(), "Parts should not be null for: " + midiPath);
                assertTrue(score.getParts().size() > 0, "Score should have parts: " + midiPath);

                int measureCount = 0;
                if (score.getParts().get(0).getMeasures() != null) {
                    measureCount = score.getParts().get(0).getMeasures().size();
                }

                LOG.info("Read MIDI '{}': {} parts, first part has {} measures", midiPath.getFileName(), score.getParts().size(), measureCount);
                passed++;
            } catch (Exception e) {
                String msg = midiPath.getFileName() + ": " + e.getMessage();
                failures.add(msg);
                LOG.info("FAILED MIDI read '{}': {}", midiPath.getFileName(), e.getMessage());
            }
        }

        LOG.info("MIDI file read: {} passed, {} failed out of {} files.", passed, failures.size(), midiFiles.size());
        assertTrue(failures.isEmpty(), "Failed for " + failures.size() + " file(s): " + String.join("; ", failures));
    }


    /**
     * Tests writing a score with three separate parts and reading it back.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteMultiPartScore() throws IOException {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");
        PartListImpl partList = new PartListImpl();
        List<PartImpl> parts = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            String partId = "P" + (i + 1);
            String partName = "Part " + (i + 1);
            ScorePartImpl scorePart = createScorePart(partId, partName, i + 1);
            partList.getScoreParts().add(scorePart);
            parts.add(createSingleMeasurePart(partId));
        }

        score.setPartList(partList);
        score.setParts(parts);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MidiIOFactory.getInstance().getWriter().write(score, baos);

        byte[] midiBytes = baos.toByteArray();
        assertTrue(midiBytes.length > 0, "MIDI output should not be empty");

        IScore readBack = MidiIOFactory.getInstance().getReader().read(new ByteArrayInputStream(midiBytes));
        assertNotNull(readBack);
        assertNotNull(readBack.getParts());
        assertEquals(3, readBack.getParts().size(), "Read-back should have 3 parts");

        LOG.info("Multi-part round-trip: wrote 3 parts, read back {} parts.", readBack.getParts().size());
    }


    /**
     * Tests that chord notes (same start tick) survive a MIDI round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testChordNotesPreserved() throws IOException {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");

        ScorePartImpl scorePart = createScorePart("P1", "Piano", 1);
        PartListImpl partList = new PartListImpl();
        partList.getScoreParts().add(scorePart);
        score.setPartList(partList);

        PartImpl part = new PartImpl();
        part.setId("P1");

        MeasureImpl measure = new MeasureImpl();
        measure.setNumber("1");
        measure.setAttributes(createDefaultAttributes());

        List<NoteImpl> notes = new ArrayList<>();
        notes.add(createNote("C", 4, 1, QUARTER, false));
        notes.add(createNote("E", 4, 1, QUARTER, true));
        notes.add(createNote("G", 4, 1, QUARTER, true));
        measure.setNotes(notes);

        List<MeasureImpl> measures = new ArrayList<>();
        measures.add(measure);
        part.setMeasures(measures);

        List<PartImpl> parts = new ArrayList<>();
        parts.add(part);
        score.setParts(parts);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MidiIOFactory.getInstance().getWriter().write(score, baos);

        IScore readBack = MidiIOFactory.getInstance().getReader().read(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(readBack.getParts());
        assertTrue(readBack.getParts().size() > 0);

        int noteCount = 0;
        IPart firstPart = readBack.getParts().get(0);
        if (firstPart.getMeasures() != null && !firstPart.getMeasures().isEmpty()) {
            IMeasure firstMeasure = firstPart.getMeasures().get(0);
            if (firstMeasure.getNotes() != null) {
                noteCount = firstMeasure.getNotes().size();
            }
        }

        assertTrue(noteCount >= 3, "Chord should produce at least 3 notes, got: " + noteCount);
        LOG.info("Chord round-trip: {} notes in first measure.", noteCount);
    }


    /**
     * Tests that a dotted quarter note survives a MIDI round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testDottedDurationRoundTrip() throws IOException {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");

        ScorePartImpl scorePart = createScorePart("P1", "Piano", 1);
        PartListImpl partList = new PartListImpl();
        partList.getScoreParts().add(scorePart);
        score.setPartList(partList);

        PartImpl part = new PartImpl();
        part.setId("P1");

        MeasureImpl measure = new MeasureImpl();
        measure.setNumber("1");

        AttributesImpl attr = new AttributesImpl();
        attr.setDivisions(480);
        TimeImpl time = new TimeImpl();
        time.setBeats("4");
        time.setBeatType("4");
        List<TimeImpl> times = new ArrayList<>();
        times.add(time);
        attr.setTimes(times);
        List<AttributesImpl> attrs = new ArrayList<>();
        attrs.add(attr);
        measure.setAttributes(attrs);

        NoteImpl dottedNote = new NoteImpl();
        PitchImpl pitch = new PitchImpl();
        pitch.setStep("C");
        pitch.setOctave(4);
        dottedNote.setPitch(pitch);
        dottedNote.setDuration(720);
        dottedNote.setType(QUARTER);
        dottedNote.setVoice(1);
        List<String> dotList = new ArrayList<>();
        dotList.add("");
        dottedNote.setDots(dotList);

        List<NoteImpl> notes = new ArrayList<>();
        notes.add(dottedNote);
        measure.setNotes(notes);

        List<MeasureImpl> measures = new ArrayList<>();
        measures.add(measure);
        part.setMeasures(measures);

        List<PartImpl> parts = new ArrayList<>();
        parts.add(part);
        score.setParts(parts);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MidiIOFactory.getInstance().getWriter().write(score, baos);

        IScore readBack = MidiIOFactory.getInstance().getReader().read(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(readBack.getParts());

        INote firstNote = findFirstPitchedNote(readBack);
        assertNotNull(firstNote, "Should find a pitched note in read-back");
        assertEquals(QUARTER, firstNote.getType(), "Note type should be quarter");
        assertNotNull(firstNote.getDots(), "Dotted note should have dots list");
        assertTrue(firstNote.getDots().size() > 0, "Dotted note should have at least one dot");

        LOG.info("Dotted round-trip: type='{}', dots={}", firstNote.getType(), firstNote.getDots().size());
    }


    /**
     * Tests that writing a score with no parts does not throw an exception.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testEmptyScoreWrite() throws IOException {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MidiIOFactory.getInstance().getWriter().write(score, baos);

        byte[] midiBytes = baos.toByteArray();
        assertTrue(midiBytes.length > 0, "Even an empty score should produce MIDI output");

        LOG.info("Empty score MIDI: {} bytes.", midiBytes.length);
    }


    /**
     * Tests format detection for various MIDI file extensions.
     */
    @Test
    void testMidiFormatDetection() {
        assertEquals(MusicFileFormat.MIDI, MusicFileFormat.fromFileName("test.mid"));
        assertEquals(MusicFileFormat.MIDI, MusicFileFormat.fromFileName("test.midi"));
        assertEquals(MusicFileFormat.MIDI, MusicFileFormat.fromFileName("TEST.MID"));
        assertEquals(MusicFileFormat.MIDI, MusicFileFormat.fromFileName("test.MIDI"));

        LOG.info("All MIDI format detection variants passed.");
    }


    /**
     * Creates a ScorePart with a single MIDI instrument.
     *
     * @param partId the part identifier
     * @param name the part name
     * @param channel the MIDI channel (1-based)
     * @return the constructed score part
     */
    private ScorePartImpl createScorePart(String partId, String name, int channel) {
        ScorePartImpl scorePart = new ScorePartImpl();
        scorePart.setId(partId);
        scorePart.setPartName(name);

        ScoreInstrumentImpl si = new ScoreInstrumentImpl();
        si.setId(partId + "-I1");
        si.setInstrumentName(name);
        scorePart.getScoreInstruments().add(si);

        MidiInstrumentImpl mi = new MidiInstrumentImpl();
        mi.setId(partId + "-I1");
        mi.setMidiChannel(channel);
        mi.setMidiProgram(1);
        scorePart.getMidiInstruments().add(mi);
        return scorePart;
    }


    /**
     * Creates a part with one measure containing four quarter notes (C, D, E, F).
     *
     * @param partId the part identifier
     * @return the constructed part
     */
    private PartImpl createSingleMeasurePart(String partId) {
        PartImpl part = new PartImpl();
        part.setId(partId);

        MeasureImpl measure = new MeasureImpl();
        measure.setNumber("1");
        measure.setAttributes(createDefaultAttributes());

        List<NoteImpl> notes = new ArrayList<>();
        notes.add(createNote("C", 4, 1, QUARTER, false));
        notes.add(createNote("D", 4, 1, QUARTER, false));
        notes.add(createNote("E", 4, 1, QUARTER, false));
        notes.add(createNote("F", 4, 1, QUARTER, false));
        measure.setNotes(notes);

        List<MeasureImpl> measures = new ArrayList<>();
        measures.add(measure);
        part.setMeasures(measures);
        return part;
    }


    /**
     * Creates default attributes with 4/4 time, divisions=1, and C major key.
     *
     * @return the attributes list
     */
    private List<AttributesImpl> createDefaultAttributes() {
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
        return attrs;
    }


    /**
     * Creates a note with the given pitch, duration, and type.
     *
     * @param step the pitch step
     * @param octave the octave
     * @param duration the duration in divisions
     * @param type the note type name
     * @param isChord true if this note is part of a chord
     * @return the constructed note
     */
    private NoteImpl createNote(String step, int octave, int duration, String type, boolean isChord) {
        NoteImpl note = new NoteImpl();
        PitchImpl pitch = new PitchImpl();
        pitch.setStep(step);
        pitch.setOctave(octave);
        note.setPitch(pitch);
        note.setDuration(duration);
        note.setType(type);
        note.setVoice(1);
        if (isChord) {
            note.setChord("");
        }
        return note;
    }


    /**
     * Finds the first pitched note in a score.
     *
     * @param score the score to search
     * @return the first pitched note, or null if none found
     */
    private INote findFirstPitchedNote(IScore score) {
        if (score.getParts() == null) {
            return null;
        }

        for (IPart part : score.getParts()) {
            if (part.getMeasures() == null) {
                continue;
            }
            for (IMeasure m : part.getMeasures()) {
                if (m.getNotes() == null) {
                    continue;
                }
                for (INote n : m.getNotes()) {
                    if (n.getPitch() != null) {
                        return n;
                    }
                }
            }
        }

        return null;
    }
}
