/*
 * MidiWriterImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.midi.impl;

import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.io.format.midi.MidiWriterOptions;
import com.github.toolarium.music.io.format.midi.impl.dto.MidiConductor;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.IDirection;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.direction.ISound;
import com.github.toolarium.music.model.direction.IWedge;
import com.github.toolarium.music.model.direction.IWords;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IBackup;
import com.github.toolarium.music.model.measure.IForward;
import com.github.toolarium.music.model.measure.IKey;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.measure.ITime;
import com.github.toolarium.music.model.measure.ITranspose;
import com.github.toolarium.music.model.notation.IArticulations;
import com.github.toolarium.music.model.notation.IDynamics;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.note.ITie;
import com.github.toolarium.music.model.part.IMidiInstrument;
import com.github.toolarium.music.model.part.IScorePart;
import com.github.toolarium.music.model.score.ICreator;
import com.github.toolarium.music.model.score.IIdentification;
import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
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
    private static final int FORTE_VELOCITY = 90;
    private static final int MAX_VELOCITY = 127;
    private static final int PERCENT = 100;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int META_TEXT = 0x01;
    private static final int META_COPYRIGHT = 0x02;
    private static final int META_INSTRUMENT_NAME = 0x04;
    private static final int META_MARKER = 0x06;
    private static final int CONTROL_EXPRESSION = 11;
    private static final int EXPRESSION_DEFAULT = 100;
    private static final int EXPRESSION_LOUD = 127;
    private static final int EXPRESSION_SOFT = 64;
    private static final int HAIRPIN_STEPS = 8;
    private static final int ACCENT_VELOCITY = 12;
    private static final int STRONG_ACCENT_VELOCITY = 20;
    private static final double STACCATO = 0.5;
    private static final double STACCATISSIMO = 0.25;
    private static final double SPICCATO = 0.4;
    private static final double DETACHED_LEGATO = 0.75;
    private static final int GRADUAL_PERCENT = 25;
    private static final int GRADUAL_QUARTERS = 8;
    private static final int FIFTHS_PER_SEMITONE = 7;
    private static final int MAX_FIFTHS = 6;
    private static final Pattern SLOWER = Pattern.compile("(^|\\s)(rit|rall|allarg)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FASTER = Pattern.compile("(^|\\s)(accel|string)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final String TIE_START = "start";
    private static final String TIE_STOP = "stop";
    private static final Map<String, Integer> DYNAMICS = new TreeMap<>();

    static {
        // the MIDI velocity of the dynamics marks
        DYNAMICS.put("pppp", 20);
        DYNAMICS.put("ppp", 30);
        DYNAMICS.put("pp", 40);
        DYNAMICS.put("p", 52);
        DYNAMICS.put("mp", 64);
        DYNAMICS.put("mf", 76);
        DYNAMICS.put("f", 88);
        DYNAMICS.put("ff", 100);
        DYNAMICS.put("fff", 112);
        DYNAMICS.put("ffff", 124);
        DYNAMICS.put("fp", 88);
        DYNAMICS.put("sf", 100);
        DYNAMICS.put("sfp", 100);
        DYNAMICS.put("sfz", 100);
        DYNAMICS.put("rf", 100);
        DYNAMICS.put("rfz", 100);
    }


    private final MidiWriterOptions options;


    /**
     * Constructor for MidiWriterImpl with all information (high quality).
     */
    public MidiWriterImpl() {
        this(MidiWriterOptions.highQuality());
    }


    /**
     * Constructor for MidiWriterImpl.
     *
     * @param options the options: which information the MIDI file keeps
     */
    public MidiWriterImpl(MidiWriterOptions options) {
        if (options == null) {
            this.options = MidiWriterOptions.highQuality();
        } else {
            this.options = options;
        }
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
            writeScoreInfo(score, tempoTrack);

            // the conductor events of all parts by tick; the first part that sets an event at a tick wins
            MidiConductor conductor = new MidiConductor();
            List<? extends IPart> parts = score.getParts();
            if (parts != null) {
                for (int i = 0; i < parts.size(); i++) {
                    IPart part = parts.get(i);
                    Track track = sequence.createTrack();
                    int channel = resolveChannel(score, i);
                    writePartTrack(score, part, track, new int[] {channel, resolution, i}, conductor);
                }
            }
            writeConductor(tempoTrack, conductor, resolution);

            Sequence result = MidiSequenceTransformer.getInstance().transform(sequence, options);
            int type = 1;
            if (options.isSingleTrack()) {
                type = 0;
            }
            MidiSystem.write(result, type, outputStream);

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
     * Writes the information of the score to the conductor track: the title as track name, the rights as copyright
     * and the creators (composer, arranger, lyricist) as texts.
     *
     * @param score the score
     * @param track the conductor track
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeScoreInfo(IScore score, Track track) throws InvalidMidiDataException {
        IIdentification identification = score.getIdentification();
        if (options.isCopyright() && identification != null && identification.getRights() != null) {
            writeMetaEvent(track, META_COPYRIGHT, identification.getRights(), 0);
        }
        if (!options.isScoreInfo()) {
            return;
        }
        String title = score.getMovementTitle();
        if (score.getWork() != null && score.getWork().getWorkTitle() != null) {
            title = score.getWork().getWorkTitle();
        }
        if (title != null) {
            writeMetaEvent(track, META_TRACK_NAME, title, 0);
        }
        if (identification != null && identification.getCreators() != null) {
            for (ICreator creator : identification.getCreators()) {
                if (creator.getValue() != null && creator.getType() != null) {
                    String type = Character.toUpperCase(creator.getType().charAt(0)) + creator.getType().substring(1);
                    writeMetaEvent(track, META_TEXT, type + ": " + creator.getValue().replace('\n', ' '), 0);
                }
            }
        }
    }


    /**
     * Writes the conductor track: time and key signatures, markers, tempo changes and the tempo steps of the gradual
     * tempo changes.
     *
     * @param track the conductor track
     * @param conductor the conductor events
     * @param resolution the MIDI resolution
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeConductor(Track track, MidiConductor conductor, int resolution) throws InvalidMidiDataException {
        if (options.isSignatures()) {
            if (conductor.getTimes().isEmpty()) {
                writeDefaultTimeSignature(track);
            }
            for (Map.Entry<Long, int[]> time : conductor.getTimes().entrySet()) {
                writeTimeSignatureEvent(track, time.getKey(), time.getValue()[0], time.getValue()[1]);
            }
            for (Map.Entry<Long, int[]> key : conductor.getKeys().entrySet()) {
                byte[] data = {(byte) key.getValue()[0], (byte) key.getValue()[1]};
                MetaMessage msg = new MetaMessage();
                msg.setMessage(META_KEY_SIGNATURE, data, data.length);
                track.add(new MidiEvent(msg, key.getKey()));
            }
        }
        if (options.isMarkers()) {
            for (Map.Entry<Long, String> marker : conductor.getMarkers().entrySet()) {
                writeMetaEvent(track, META_MARKER, marker.getValue(), marker.getKey());
            }
        }

        Map<Long, Integer> tempos = new TreeMap<>(conductor.getTempos());
        tempos.putIfAbsent(0L, DEFAULT_TEMPO_BPM);
        for (Map.Entry<Long, Integer> gradual : conductor.getGradualChanges().entrySet()) {
            addTempoSteps(tempos, conductor, gradual.getKey(), gradual.getValue(), resolution);
        }
        for (Map.Entry<Long, Integer> tempo : tempos.entrySet()) {
            writeTempoEvent(track, tempo.getKey(), tempo.getValue());
        }
    }


    /**
     * Adds the tempo steps of a gradual tempo change: a step on every quarter up to the next tempo change (at most
     * eight quarters). The tempo ends at the next tempo mark if it lies in the direction of the change, else it changes
     * by a quarter.
     *
     * @param tempos the tempo changes by tick (updated)
     * @param conductor the conductor events
     * @param tick the tick of the gradual change
     * @param direction -1 for slower, 1 for faster
     * @param resolution the MIDI resolution
     */
    private void addTempoSteps(Map<Long, Integer> tempos, MidiConductor conductor, long tick, int direction, int resolution) {
        int tempo = DEFAULT_TEMPO_BPM;
        long end = tick + (long) GRADUAL_QUARTERS * resolution;
        int target = 0;
        for (Map.Entry<Long, Integer> change : conductor.getTempos().entrySet()) {
            if (change.getKey() <= tick) {
                tempo = change.getValue();
            } else if (change.getKey() < end) {
                end = change.getKey();
                target = change.getValue();
                break;
            }
        }
        for (Long other : conductor.getGradualChanges().keySet()) {
            if (other > tick && other < end) {
                end = other;
                target = 0;
            }
        }
        if (target == 0 || (target - tempo) * direction <= 0) {
            target = tempo * (PERCENT + direction * GRADUAL_PERCENT) / PERCENT;
        }
        int steps = (int) Math.max(1, (end - tick) / resolution);
        for (int i = 0; i < steps; i++) {
            tempos.putIfAbsent(tick + (long) i * resolution, tempo + (target - tempo) * (i + 1) / steps);
        }
    }


    /**
     * Writes a single part as a MIDI track. The content of every measure is played in its order: notes and rests
     * advance the time, a backup goes back and a forward goes ahead (voices); tied notes are played as one note,
     * dynamics set the velocity, hairpins change the expression, texts are written as text events and the
     * conductor events are collected.
     *
     * @param score the score
     * @param part the part to write
     * @param track the MIDI track
     * @param setup the MIDI channel, the MIDI resolution and the part index
     * @param conductor the conductor events (updated)
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writePartTrack(IScore score, IPart part, Track track, int[] setup, MidiConductor conductor) throws InvalidMidiDataException {
        int channel = setup[0];
        int partIndex = setup[2];
        String trackName = resolveTrackName(score, partIndex);
        if (trackName != null && options.isScoreInfo()) {
            writeTrackNameEvent(track, trackName);
        }
        String instrumentName = resolveInstrumentName(score, partIndex);
        if (instrumentName != null && options.isScoreInfo()) {
            writeMetaEvent(track, META_INSTRUMENT_NAME, instrumentName, 0);
        }
        writeProgramChange(score, track, channel, partIndex);
        writeExpression(track, channel, EXPRESSION_DEFAULT, 0);
        if (part.getMeasures() == null) {
            return;
        }

        // the divisions, the velocity and the transposition from written to sounding pitch in semitones
        int resolution = setup[1];
        int[] state = {resolution, DEFAULT_VELOCITY, 0};
        Map<Integer, Long> openTies = new TreeMap<>();
        Map<Integer, Object[]> wedges = new TreeMap<>();
        long measureStart = 0;
        for (IMeasure measure : part.getMeasures()) {
            readAttributes(measure, state, measureStart, partIndex == 0, conductor);
            if (measure.getMusicData() == null) {
                continue;
            }
            long cursor = measureStart;
            long lastStart = measureStart;
            long measureEnd = measureStart;
            for (IMusicDataElement element : measure.getMusicData()) {
                if (element instanceof INote) {
                    INote note = (INote) element;
                    if (note.getGrace() != null) {
                        continue;
                    }
                    long duration = ticks(note.getDuration(), state[0], resolution);
                    long start = cursor;
                    if (note.getChord() != null) {
                        start = lastStart;
                    } else {
                        lastStart = cursor;
                        cursor += duration;
                    }
                    if (note.getRest() == null && note.getPitch() != null) {
                        writeNote(track, channel, note, start, duration, new int[] {state[1], state[2]}, openTies);
                    }
                } else if (element instanceof IBackup) {
                    cursor -= ticks(((IBackup) element).getDuration(), state[0], resolution);
                } else if (element instanceof IForward) {
                    cursor += ticks(((IForward) element).getDuration(), state[0], resolution);
                } else if (element instanceof IDirection) {
                    IDirection direction = (IDirection) element;
                    long tick = cursor + ticks(direction.getOffset(), state[0], resolution);
                    writeDirection(track, channel, direction, tick, state, wedges, conductor);
                    // the offset of the sound replaces the offset of the direction
                    long soundTick = tick;
                    if (direction.getSound() != null && direction.getSound().getOffset() != null) {
                        soundTick = cursor;
                    }
                    addTempo(conductor.getTempos(), direction.getSound(), soundTick, state[0], resolution);
                } else if (element instanceof ISound) {
                    addTempo(conductor.getTempos(), (ISound) element, cursor, state[0], resolution);
                }
                measureEnd = Math.max(measureEnd, cursor);
            }
            measureStart = measureEnd;
        }
        endTies(track, channel, openTies, Long.MAX_VALUE);
    }


    /**
     * Reads the attributes of a measure: divisions and transposition, and for the first part the time and key
     * signatures (in concert pitch) for the conductor track.
     *
     * @param measure the measure
     * @param state the divisions, the velocity and the transposition in semitones (updated)
     * @param tick the tick of the measure
     * @param firstPart true for the first part
     * @param conductor the conductor events (updated)
     */
    private void readAttributes(IMeasure measure, int[] state, long tick, boolean firstPart, MidiConductor conductor) {
        if (measure.getAttributes() == null) {
            return;
        }
        for (IAttributes attr : measure.getAttributes()) {
            if (attr.getDivisions() != null && attr.getDivisions() > 0) {
                state[0] = attr.getDivisions();
            }
            if (attr.getTransposes() != null && !attr.getTransposes().isEmpty()) {
                state[2] = semitones(attr.getTransposes().get(0));
            }
            if (!firstPart) {
                continue;
            }
            if (attr.getTimes() != null && !attr.getTimes().isEmpty()) {
                ITime time = attr.getTimes().get(0);
                conductor.getTimes().put(tick, new int[] {parseInt(time.getBeats(), 4), parseInt(time.getBeatType(), 4)});
            }
            if (attr.getKeys() != null && !attr.getKeys().isEmpty() && attr.getKeys().get(0).getFifths() != null) {
                IKey key = attr.getKeys().get(0);
                int minor = 0;
                if ("minor".equals(key.getMode())) {
                    minor = 1;
                }
                conductor.getKeys().put(tick, new int[] {concertFifths(key.getFifths(), state[2]), minor});
            }
        }
    }


    /**
     * Gets the key in concert pitch of a key in written pitch.
     *
     * @param fifths the written key
     * @param semitones the transposition from written to sounding pitch
     * @return the concert key, -6 to 6
     */
    private int concertFifths(int fifths, int semitones) {
        int result = Math.floorMod(fifths + semitones * FIFTHS_PER_SEMITONE + MAX_FIFTHS, SEMITONES_PER_OCTAVE) - MAX_FIFTHS;
        if (fifths == 0 && semitones == 0) {
            return 0;
        }
        return result;
    }


    /**
     * Writes the events of a direction: the dynamics (velocity and expression), the hairpins (expression ramps),
     * the texts (text events, gradual tempo changes) and the rehearsal marks (markers).
     *
     * @param track the MIDI track
     * @param channel the MIDI channel
     * @param direction the direction
     * @param tick the tick of the direction
     * @param state the divisions, the velocity and the transposition in semitones (updated)
     * @param wedges the open hairpins by number: start tick and crescendo flag (updated)
     * @param conductor the conductor events (updated)
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeDirection(Track track, int channel, IDirection direction, long tick, int[] state, Map<Integer, Object[]> wedges,
                                MidiConductor conductor) throws InvalidMidiDataException {
        int velocity = dynamicsVelocity(direction, state[1]);
        if (velocity != state[1] || hasDynamics(direction)) {
            state[1] = velocity;
            writeExpression(track, channel, EXPRESSION_DEFAULT, tick);
        }
        if (direction.getDirectionTypes() == null) {
            return;
        }
        for (IDirectionType type : direction.getDirectionTypes()) {
            if (type.getRehearsal() != null) {
                conductor.getMarkers().putIfAbsent(tick, type.getRehearsal());
            }
            if (type.getWedge() != null) {
                writeWedge(track, channel, type.getWedge(), tick, wedges);
            }
            if (type.getWords() == null) {
                continue;
            }
            for (IWords words : type.getWords()) {
                String text = words.getValue();
                if (text == null || text.trim().isEmpty() || NUMBER.matcher(text.trim()).matches()) {
                    continue;
                }
                if (options.isTexts()) {
                    writeMetaEvent(track, META_TEXT, text.trim(), tick);
                }
                if (SLOWER.matcher(text).find()) {
                    conductor.getGradualChanges().putIfAbsent(tick, -1);
                } else if (FASTER.matcher(text).find()) {
                    conductor.getGradualChanges().putIfAbsent(tick, 1);
                }
            }
        }
    }


    /**
     * Checks whether a direction has a dynamics mark.
     *
     * @param direction the direction
     * @return true if the direction has a dynamics mark
     */
    private boolean hasDynamics(IDirection direction) {
        if (direction.getDirectionTypes() == null) {
            return false;
        }
        for (IDirectionType type : direction.getDirectionTypes()) {
            if (dynamicsMark(type.getDynamics()) != null) {
                return true;
            }
        }
        return false;
    }


    /**
     * Writes a hairpin: the start is remembered, the stop writes the expression ramp from the start to the stop
     * (crescendo louder, diminuendo softer).
     *
     * @param track the MIDI track
     * @param channel the MIDI channel
     * @param wedge the wedge
     * @param tick the tick
     * @param wedges the open hairpins by number: start tick and crescendo flag (updated)
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeWedge(Track track, int channel, IWedge wedge, long tick, Map<Integer, Object[]> wedges) throws InvalidMidiDataException {
        int number = 1;
        if (wedge.getNumber() != null) {
            number = wedge.getNumber();
        }
        if ("crescendo".equals(wedge.getType()) || "diminuendo".equals(wedge.getType())) {
            wedges.put(number, new Object[] {tick, "crescendo".equals(wedge.getType())});
            return;
        }
        Object[] start = wedges.remove(number);
        if (start == null || "continue".equals(wedge.getType())) {
            return;
        }
        long from = (Long) start[0];
        int target = EXPRESSION_SOFT;
        if ((Boolean) start[1]) {
            target = EXPRESSION_LOUD;
        }
        for (int i = 1; i <= HAIRPIN_STEPS && tick > from; i++) {
            int value = EXPRESSION_DEFAULT + (target - EXPRESSION_DEFAULT) * i / HAIRPIN_STEPS;
            writeExpression(track, channel, value, from + (tick - from) * i / HAIRPIN_STEPS);
        }
    }


    /**
     * Writes an expression controller event.
     *
     * @param track the MIDI track
     * @param channel the MIDI channel
     * @param value the expression, 0 to 127
     * @param tick the tick
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeExpression(Track track, int channel, int value, long tick) throws InvalidMidiDataException {
        ShortMessage message = new ShortMessage();
        message.setMessage(ShortMessage.CONTROL_CHANGE, channel, CONTROL_EXPRESSION, value);
        track.add(new MidiEvent(message, tick));
    }


    /**
     * Writes a text meta event (text, copyright, track name, instrument name, marker).
     *
     * @param track the track
     * @param type the meta event type
     * @param text the text
     * @param tick the tick
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeMetaEvent(Track track, int type, String text, long tick) throws InvalidMidiDataException {
        byte[] data = text.getBytes(StandardCharsets.ISO_8859_1);
        MetaMessage message = new MetaMessage();
        message.setMessage(type, data, data.length);
        track.add(new MidiEvent(message, tick));
    }


    /**
     * Parses a number.
     *
     * @param text the text, or null
     * @param defaultValue the value for no number
     * @return the number
     */
    private int parseInt(String text, int defaultValue) {
        if (text == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * Writes a note. A tied note continues the note it is tied to, if it has the same pitch and starts where the
     * tied note ends; a tie that leads to no such note ends with its note.
     *
     * @param track the MIDI track
     * @param channel the MIDI channel
     * @param note the note
     * @param start the start tick
     * @param duration the duration in ticks
     * @param sound the velocity and the transposition from written to sounding pitch in semitones
     * @param openTies the end ticks of the MIDI notes that are tied to a following note (updated)
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeNote(Track track, int channel, INote note, long start, long duration, int[] sound, Map<Integer, Long> openTies)
            throws InvalidMidiDataException {
        final int midiNote = Math.max(0, Math.min(MAX_VELOCITY, MidiNoteConverter.getInstance().pitchToMidiNote(note.getPitch()) + sound[1]));
        int velocity = sound[0];
        long length = duration;
        IArticulations articulations = null;
        if (note.getNotations() != null) {
            articulations = note.getNotations().getArticulations();
        }
        if (articulations != null) {
            // staccato and similar marks shorten the note, accents make it louder
            length = (long) (duration * articulationLength(articulations));
            if (articulations.getStrongAccent() != null) {
                velocity += STRONG_ACCENT_VELOCITY;
            } else if (articulations.getAccent() != null) {
                velocity += ACCENT_VELOCITY;
            }
            velocity = Math.min(MAX_VELOCITY, velocity);
        }
        endTies(track, channel, openTies, start);
        Long tiedEnd = openTies.remove(midiNote);
        boolean continued = tiedEnd != null && tiedEnd == start && hasTie(note, TIE_STOP);
        if (tiedEnd != null && !continued) {
            noteOff(track, channel, midiNote, tiedEnd);
        }
        if (!continued) {
            ShortMessage noteOn = new ShortMessage();
            noteOn.setMessage(ShortMessage.NOTE_ON, channel, midiNote, velocity);
            track.add(new MidiEvent(noteOn, start));
        }
        if (hasTie(note, TIE_START)) {
            openTies.put(midiNote, start + duration);
        } else {
            noteOff(track, channel, midiNote, start + Math.max(1, length));
        }
    }


    /**
     * Gets the part of the written length a note sounds with its articulations.
     *
     * @param articulations the articulations
     * @return the factor of the length (1 for the full length)
     */
    private double articulationLength(IArticulations articulations) {
        if (articulations.getStaccatissimo() != null) {
            return STACCATISSIMO;
        }
        if (articulations.getSpiccato() != null) {
            return SPICCATO;
        }
        if (articulations.getStaccato() != null) {
            return STACCATO;
        }
        if (articulations.getDetachedLegato() != null) {
            return DETACHED_LEGATO;
        }
        return 1.0;
    }


    /**
     * Ends the tied notes that end before a tick: no note continues them.
     *
     * @param track the MIDI track
     * @param channel the MIDI channel
     * @param openTies the end ticks of the tied MIDI notes (updated)
     * @param tick the tick
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void endTies(Track track, int channel, Map<Integer, Long> openTies, long tick) throws InvalidMidiDataException {
        List<Integer> ended = new ArrayList<>();
        for (Map.Entry<Integer, Long> tie : openTies.entrySet()) {
            if (tie.getValue() < tick) {
                noteOff(track, channel, tie.getKey(), tie.getValue());
                ended.add(tie.getKey());
            }
        }
        for (Integer key : ended) {
            openTies.remove(key);
        }
    }


    /**
     * Writes a note off event.
     *
     * @param track the MIDI track
     * @param channel the MIDI channel
     * @param midiNote the MIDI note
     * @param tick the tick
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void noteOff(Track track, int channel, int midiNote, long tick) throws InvalidMidiDataException {
        ShortMessage noteOff = new ShortMessage();
        noteOff.setMessage(ShortMessage.NOTE_OFF, channel, midiNote, 0);
        track.add(new MidiEvent(noteOff, tick));
    }


    /**
     * Gets the transposition from written to sounding pitch in semitones.
     *
     * @param transpose the transposition
     * @return the semitones
     */
    private int semitones(ITranspose transpose) {
        int result = 0;
        if (transpose.getChromatic() != null) {
            result += transpose.getChromatic();
        }
        if (transpose.getOctaveChange() != null) {
            result += transpose.getOctaveChange() * SEMITONES_PER_OCTAVE;
        }
        return result;
    }


    /**
     * Checks whether a note has a tie of a type.
     *
     * @param note the note
     * @param type start or stop
     * @return true if the note has the tie
     */
    private boolean hasTie(INote note, String type) {
        if (note.getTies() == null) {
            return false;
        }
        for (ITie tie : note.getTies()) {
            if (type.equals(tie.getType())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Converts a duration in divisions to ticks.
     *
     * @param duration the duration in divisions, or null
     * @param divisions the divisions per quarter
     * @param resolution the MIDI resolution
     * @return the duration in ticks
     */
    private long ticks(Integer duration, int divisions, int resolution) {
        if (duration == null) {
            return 0;
        }
        return MidiDurationConverter.getInstance().divisionsToTicks(duration, divisions, resolution);
    }


    /**
     * Adds the tempo of a sound to the tempo changes. The offset of the sound moves the tempo change.
     *
     * @param tempos the tempo changes by tick (updated)
     * @param sound the sound, or null
     * @param tick the tick of the sound
     * @param divisions the divisions per quarter
     * @param resolution the MIDI resolution
     */
    private void addTempo(Map<Long, Integer> tempos, ISound sound, long tick, int divisions, int resolution) {
        if (sound == null) {
            return;
        }
        int bpm = parseTempoBpm(sound.getTempo());
        if (bpm > 0) {
            long at = tick;
            if (sound.getOffset() != null) {
                at = tick + ticks(sound.getOffset(), divisions, resolution);
            }
            tempos.putIfAbsent(Math.max(0, at), bpm);
        }
    }


    /**
     * Gets the velocity after a direction: the dynamics of its sound (percent of forte) or of its dynamics mark.
     *
     * @param direction the direction
     * @param velocity the current velocity
     * @return the new velocity
     */
    private int dynamicsVelocity(IDirection direction, int velocity) {
        if (direction.getSound() != null && direction.getSound().getDynamics() != null) {
            try {
                double percent = Double.parseDouble(direction.getSound().getDynamics());
                return (int) Math.max(1, Math.min(MAX_VELOCITY, Math.round(percent * FORTE_VELOCITY / PERCENT)));
            } catch (NumberFormatException e) {
                return velocity;
            }
        }
        if (direction.getDirectionTypes() == null) {
            return velocity;
        }
        for (IDirectionType type : direction.getDirectionTypes()) {
            String mark = dynamicsMark(type.getDynamics());
            if (mark != null && DYNAMICS.containsKey(mark)) {
                return DYNAMICS.get(mark);
            }
        }
        return velocity;
    }


    /**
     * Gets the name of a dynamics mark.
     *
     * @param dynamics the dynamics, or null
     * @return the name, for example mf, or null
     */
    private String dynamicsMark(IDynamics dynamics) {
        if (dynamics == null) {
            return null;
        }
        String[][] marks = {{dynamics.getPppp(), "pppp"}, {dynamics.getPpp(), "ppp"}, {dynamics.getPp(), "pp"},
            {dynamics.getPiano(), "p"}, {dynamics.getMp(), "mp"}, {dynamics.getMf(), "mf"}, {dynamics.getForte(), "f"},
            {dynamics.getFf(), "ff"}, {dynamics.getFff(), "fff"}, {dynamics.getFfff(), "ffff"}, {dynamics.getFp(), "fp"},
            {dynamics.getSf(), "sf"}, {dynamics.getSfp(), "sfp"}, {dynamics.getSfz(), "sfz"}, {dynamics.getRf(), "rf"},
            {dynamics.getRfz(), "rfz"}};
        for (String[] mark : marks) {
            if (mark[0] != null) {
                return mark[1];
            }
        }
        return null;
    }


    /**
     * Resolves the instrument name of a part from its score instrument.
     *
     * @param score the score
     * @param partIndex the part index
     * @return the instrument name, or null
     */
    private String resolveInstrumentName(IScore score, int partIndex) {
        if (score.getPartList() == null || score.getPartList().getScoreParts() == null || partIndex >= score.getPartList().getScoreParts().size()) {
            return null;
        }
        IScorePart scorePart = score.getPartList().getScoreParts().get(partIndex);
        if (scorePart.getScoreInstruments() == null || scorePart.getScoreInstruments().isEmpty()) {
            return null;
        }
        return scorePart.getScoreInstruments().get(0).getInstrumentName();
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
     * @param beats the beats
     * @param beatType the beat type
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeTimeSignatureEvent(Track track, long tick, int beats, int beatType) throws InvalidMidiDataException {
        int numerator = beats;
        int denomPower = Integer.numberOfTrailingZeros(Math.max(1, beatType));

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
     * Writes a track name meta event.
     *
     * @param track the track
     * @param name the track name
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private void writeTrackNameEvent(Track track, String name) throws InvalidMidiDataException {
        writeMetaEvent(track, META_TRACK_NAME, name, 0);
    }
}
