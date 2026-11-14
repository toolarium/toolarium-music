/*
 * MidiWriterImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.midi.impl;

import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IKey;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.measure.ITime;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.part.IMidiInstrument;
import com.github.toolarium.music.model.part.IScorePart;
import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link IMusicWriter} for Standard MIDI File (SMF) output.
 * Produces Type 1 MIDI files with one track per part.
 *
 * @author patrick
 */
public class MidiWriterImpl implements IMusicWriter {

    private static final Logger LOG = LoggerFactory.getLogger(MidiWriterImpl.class);
    private static final int META_TEMPO = 0x51;
    private static final int META_TIME_SIGNATURE = 0x58;
    private static final int META_KEY_SIGNATURE = 0x59;
    private static final int META_TRACK_NAME = 0x03;
    private static final int DEFAULT_TEMPO_BPM = 120;
    private static final int DEFAULT_VELOCITY = 80;
    private static final int MICROSECONDS_PER_MINUTE = 60_000_000;


    /**
     * Constructor for MidiWriterImpl.
     */
    public MidiWriterImpl() {
        // default constructor
    }


    /**
     * Writes a score to a MIDI file at the given path.
     *
     * @param score the score to write
     * @param path the path to write to
     * @throws IOException in case of write errors
     */
    @Override
    public void write(IScore score, Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing score to MIDI path: {}", path);
        }

        try (OutputStream os = Files.newOutputStream(path)) {
            write(score, os);
        }
    }


    /**
     * Writes a score to a MIDI output stream.
     *
     * @param score the score to write
     * @param outputStream the output stream to write to
     * @throws IOException in case of write errors
     */
    @Override
    public void write(IScore score, OutputStream outputStream) throws IOException {
        try {
            int resolution = resolveResolution(score);
            Sequence sequence = new Sequence(Sequence.PPQ, resolution);

            Track tempoTrack = sequence.createTrack();
            writeGlobalMeta(score, tempoTrack, resolution);

            List<? extends IPart> parts = score.getParts();
            if (parts != null) {
                for (int i = 0; i < parts.size(); i++) {
                    IPart part = parts.get(i);
                    Track track = sequence.createTrack();
                    int channel = resolveChannel(score, i);
                    writePartTrack(score, part, track, channel, resolution, i);
                }
            }

            MidiSystem.write(sequence, 1, outputStream);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully wrote MIDI sequence with {} tracks.", sequence.getTracks().length);
            }
        } catch (InvalidMidiDataException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to write MIDI: {}", e.getMessage(), e);
            }
            throw new IOException("Failed to write MIDI sequence", e);
        }
    }


    /**
     * Resolves the MIDI resolution from the score attributes.
     *
     * @param score the score
     * @return the resolution in ticks per quarter note
     */
    private int resolveResolution(IScore score) {
        if (score.getParts() != null) {
            for (IPart part : score.getParts()) {
                if (part.getMeasures() != null) {
                    for (IMeasure measure : part.getMeasures()) {
                        if (measure.getAttributes() != null) {
                            for (IAttributes attr : measure.getAttributes()) {
                                if (attr.getDivisions() != null && attr.getDivisions() > 0) {
                                    return attr.getDivisions();
                                }
                            }
                        }
                    }
                }
            }
        }
        return MidiDurationConverter.DEFAULT_RESOLUTION;
    }


    /**
     * Resolves the MIDI channel for a part. Skips channel 10 (percussion).
     *
     * @param score the score
     * @param partIndex the zero-based part index
     * @return the MIDI channel (0-15)
     */
    private int resolveChannel(IScore score, int partIndex) {
        if (score.getPartList() != null && score.getPartList().getScoreParts() != null) {
            List<? extends IScorePart> scoreParts = score.getPartList().getScoreParts();
            if (partIndex < scoreParts.size()) {
                IScorePart sp = scoreParts.get(partIndex);
                if (sp.getMidiInstruments() != null && !sp.getMidiInstruments().isEmpty()) {
                    Integer ch = sp.getMidiInstruments().get(0).getMidiChannel();
                    if (ch != null) {
                        return ch - 1;
                    }
                }
            }
        }

        // Auto-assign, skipping channel 9 (percussion)
        int ch = partIndex;
        if (ch >= 9) {
            ch++;
        }
        return ch % 16;
    }


    /**
     * Writes global meta events (tempo, time signature, key signature) to the tempo track.
     *
     * @param score the score
     * @param track the tempo track
     * @param resolution the MIDI resolution
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeGlobalMeta(IScore score, Track track, int resolution) throws InvalidMidiDataException {
        boolean tempoWritten = false;
        boolean timeWritten = false;
        boolean keyWritten = false;

        if (score.getParts() != null && !score.getParts().isEmpty()) {
            IPart firstPart = score.getParts().get(0);
            if (firstPart.getMeasures() != null && !firstPart.getMeasures().isEmpty()) {
                IMeasure firstMeasure = firstPart.getMeasures().get(0);

                if (firstMeasure.getSounds() != null) {
                    for (Object sound : firstMeasure.getSounds()) {
                        if (!(sound instanceof com.github.toolarium.music.model.direction.ISound)) {
                            continue;
                        }

                        com.github.toolarium.music.model.direction.ISound s = (com.github.toolarium.music.model.direction.ISound) sound;
                        int bpm = parseTempoBpm(s.getTempo());
                        if (bpm > 0) {
                            writeTempoEvent(track, 0, bpm);
                            tempoWritten = true;
                        }
                    }
                }

                if (firstMeasure.getAttributes() != null) {
                    for (IAttributes attr : firstMeasure.getAttributes()) {
                        if (!timeWritten && attr.getTimes() != null) {
                            for (ITime time : attr.getTimes()) {
                                writeTimeSignatureEvent(track, 0, time);
                                timeWritten = true;
                                break;
                            }
                        }
                        if (!keyWritten && attr.getKeys() != null) {
                            for (IKey key : attr.getKeys()) {
                                writeKeySignatureEvent(track, 0, key);
                                keyWritten = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (!tempoWritten) {
            writeTempoEvent(track, 0, DEFAULT_TEMPO_BPM);
        }
        if (!timeWritten) {
            writeDefaultTimeSignature(track);
        }
    }


    /**
     * Writes a single part as a MIDI track.
     *
     * @param score the score
     * @param part the part to write
     * @param track the MIDI track
     * @param channel the MIDI channel
     * @param resolution the MIDI resolution
     * @param partIndex the part index
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writePartTrack(IScore score, IPart part, Track track, int channel,
                                int resolution, int partIndex) throws InvalidMidiDataException {
        String trackName = resolveTrackName(score, partIndex);
        if (trackName != null) {
            writeTrackNameEvent(track, trackName);
        }

        writeProgramChange(score, track, channel, partIndex);

        long currentTick = 0;
        int divisions = resolution;

        if (part.getMeasures() == null) {
            return;
        }

        for (IMeasure measure : part.getMeasures()) {
            if (measure.getAttributes() != null) {
                for (IAttributes attr : measure.getAttributes()) {
                    if (attr.getDivisions() != null && attr.getDivisions() > 0) {
                        divisions = attr.getDivisions();
                    }
                }
            }

            if (measure.getNotes() != null) {
                long measureStartTick = currentTick;
                long noteOffsetTick = 0;

                for (INote note : measure.getNotes()) {
                    if (note.getChord() != null) {
                        // Chord note shares the same start tick as the previous note
                    } else {
                        noteOffsetTick = currentTick - measureStartTick;
                    }

                    long startTick = measureStartTick + noteOffsetTick;
                    int dur = 0;
                    if (note.getDuration() != null) {
                        dur = note.getDuration();
                    }
                    long durationTicks = MidiDurationConverter.divisionsToTicks(dur, divisions, resolution);

                    if (note.getRest() != null) {
                        // Rests just advance time
                        if (note.getChord() == null) {
                            currentTick = startTick + durationTicks;
                        }
                    } else if (note.getPitch() != null) {
                        int midiNote = MidiNoteConverter.pitchToMidiNote(note.getPitch());
                        int velocity = DEFAULT_VELOCITY;

                        ShortMessage noteOn = new ShortMessage();
                        noteOn.setMessage(ShortMessage.NOTE_ON, channel, midiNote, velocity);
                        track.add(new MidiEvent(noteOn, startTick));

                        ShortMessage noteOff = new ShortMessage();
                        noteOff.setMessage(ShortMessage.NOTE_OFF, channel, midiNote, 0);
                        track.add(new MidiEvent(noteOff, startTick + durationTicks));

                        if (note.getChord() == null) {
                            currentTick = startTick + durationTicks;
                        }
                    }
                }
            }
        }
    }


    /**
     * Resolves the track name for a part from the part list.
     *
     * @param score the score
     * @param partIndex the part index
     * @return the track name, or null
     */
    private String resolveTrackName(IScore score, int partIndex) {
        if (score.getPartList() != null && score.getPartList().getScoreParts() != null) {
            List<? extends IScorePart> scoreParts = score.getPartList().getScoreParts();
            if (partIndex < scoreParts.size()) {
                return scoreParts.get(partIndex).getPartName();
            }
        }
        return null;
    }


    /**
     * Writes a program change event for a part.
     *
     * @param score the score
     * @param track the MIDI track
     * @param channel the MIDI channel
     * @param partIndex the part index
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeProgramChange(IScore score, Track track, int channel, int partIndex)
            throws InvalidMidiDataException {
        int program = 0;

        if (score.getPartList() != null && score.getPartList().getScoreParts() != null) {
            List<? extends IScorePart> scoreParts = score.getPartList().getScoreParts();
            if (partIndex < scoreParts.size()) {
                IScorePart sp = scoreParts.get(partIndex);
                if (sp.getMidiInstruments() != null && !sp.getMidiInstruments().isEmpty()) {
                    IMidiInstrument mi = sp.getMidiInstruments().get(0);
                    if (mi.getMidiProgram() != null) {
                        program = mi.getMidiProgram() - 1;
                    }
                }
            }
        }

        ShortMessage pc = new ShortMessage();
        pc.setMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0);
        track.add(new MidiEvent(pc, 0));
    }


    /**
     * Parses a tempo string into a BPM integer.
     *
     * @param tempoStr the tempo string
     * @return the BPM value, or -1 if the string is null, empty, or not a valid positive number
     */
    private int parseTempoBpm(String tempoStr) {
        if (tempoStr == null || tempoStr.isEmpty()) {
            return -1;
        }

        try {
            return (int) Double.parseDouble(tempoStr);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }


    /**
     * Writes a tempo meta event.
     *
     * @param track the track
     * @param tick the tick position
     * @param bpm the tempo in beats per minute
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeTempoEvent(Track track, long tick, int bpm) throws InvalidMidiDataException {
        int effectiveBpm = bpm;
        if (effectiveBpm <= 0) {
            effectiveBpm = DEFAULT_TEMPO_BPM;
        }
        int mpqn = MICROSECONDS_PER_MINUTE / effectiveBpm;
        byte[] data = new byte[]{
            (byte) ((mpqn >> 16) & 0xFF),
            (byte) ((mpqn >> 8) & 0xFF),
            (byte) (mpqn & 0xFF)
        };

        MetaMessage msg = new MetaMessage();
        msg.setMessage(META_TEMPO, data, data.length);
        track.add(new MidiEvent(msg, tick));
    }


    /**
     * Writes a time signature meta event.
     *
     * @param track the track
     * @param tick the tick position
     * @param time the time signature
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeTimeSignatureEvent(Track track, long tick, ITime time) throws InvalidMidiDataException {
        int numerator = 4;
        int denomPower = 2; // 2^2 = 4

        if (time.getBeats() != null) {
            try {
                numerator = Integer.parseInt(time.getBeats());
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }
        if (time.getBeatType() != null) {
            try {
                int beatType = Integer.parseInt(time.getBeatType());
                denomPower = (int) (Math.log(beatType) / Math.log(2));
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }

        byte[] data = new byte[]{
            (byte) numerator,
            (byte) denomPower,
            24,  // MIDI clocks per metronome click
            8    // 32nd notes per 24 MIDI clocks
        };

        MetaMessage msg = new MetaMessage();
        msg.setMessage(META_TIME_SIGNATURE, data, data.length);
        track.add(new MidiEvent(msg, tick));
    }


    /**
     * Writes a default 4/4 time signature.
     *
     * @param track the track
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeDefaultTimeSignature(Track track) throws InvalidMidiDataException {
        byte[] data = new byte[]{4, 2, 24, 8};
        MetaMessage msg = new MetaMessage();
        msg.setMessage(META_TIME_SIGNATURE, data, data.length);
        track.add(new MidiEvent(msg, 0));
    }


    /**
     * Writes a key signature meta event.
     *
     * @param track the track
     * @param tick the tick position
     * @param key the key signature
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeKeySignatureEvent(Track track, long tick, IKey key) throws InvalidMidiDataException {
        int sf = 0;
        int mi = 0;

        if (key.getFifths() != null) {
            sf = key.getFifths();
        }
        if ("minor".equals(key.getMode())) {
            mi = 1;
        }

        byte[] data = new byte[]{(byte) sf, (byte) mi};
        MetaMessage msg = new MetaMessage();
        msg.setMessage(META_KEY_SIGNATURE, data, data.length);
        track.add(new MidiEvent(msg, tick));
    }


    /**
     * Writes a track name meta event.
     *
     * @param track the track
     * @param name the track name
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeTrackNameEvent(Track track, String name) throws InvalidMidiDataException {
        byte[] data = name.getBytes(StandardCharsets.US_ASCII);
        MetaMessage msg = new MetaMessage();
        msg.setMessage(META_TRACK_NAME, data, data.length);
        track.add(new MidiEvent(msg, 0));
    }
}
