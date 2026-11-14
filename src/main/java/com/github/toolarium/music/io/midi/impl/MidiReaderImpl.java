/*
 * MidiReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.midi.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import com.github.toolarium.music.model.note.impl.RestImpl;
import com.github.toolarium.music.model.part.impl.MidiInstrumentImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.part.impl.ScoreInstrumentImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link IMusicReader} for Standard MIDI Files (SMF).
 * Supports Type 0 (single track) and Type 1 (multi-track) MIDI files.
 * Converts MIDI events into the IScore model with quantized note durations.
 *
 * @author patrick
 */
public class MidiReaderImpl implements IMusicReader {

    private static final Logger LOG = LoggerFactory.getLogger(MidiReaderImpl.class);
    private static final int META_TEMPO = 0x51;
    private static final int META_TIME_SIGNATURE = 0x58;
    private static final int META_KEY_SIGNATURE = 0x59;
    private static final int META_TRACK_NAME = 0x03;
    private static final int MICROSECONDS_PER_MINUTE = 60_000_000;
    private static final int DEFAULT_BEATS = 4;
    private static final int DEFAULT_BEAT_TYPE = 4;


    /**
     * Constructor for MidiReaderImpl.
     */
    public MidiReaderImpl() {
        // default constructor
    }


    /**
     * Reads a MIDI file from a path.
     *
     * @param path the path to the MIDI file
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading MIDI from path: {}", path);
        }

        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


    /**
     * Reads a MIDI file from an input stream.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        try {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            Sequence sequence = MidiSystem.getSequence(bis);
            IScore score = convertSequence(sequence);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully read MIDI sequence, {} tracks.", sequence.getTracks().length);
            }

            return score;
        } catch (InvalidMidiDataException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read MIDI: {}", e.getMessage(), e);
            }
            throw new IOException("Failed to read MIDI file", e);
        }
    }


    /**
     * Converts a MIDI Sequence to an IScore.
     * Detects Type 0 (single track with multiple channels) vs Type 1 (multi-track)
     * by checking whether the sequence has exactly one track.
     *
     * @param sequence the MIDI sequence
     * @return the converted score
     */
    private IScore convertSequence(Sequence sequence) {
        int resolution = sequence.getResolution();

        GlobalMeta meta = extractGlobalMeta(sequence);

        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");

        PartListImpl partList = new PartListImpl();
        List<PartImpl> parts = new ArrayList<>();

        Track[] tracks = sequence.getTracks();
        if (tracks.length == 1) {
            convertType0(tracks[0], resolution, meta, partList, parts);
        } else {
            convertType1(tracks, resolution, meta, partList, parts);
        }

        score.setPartList(partList);
        score.setParts(parts);
        return score;
    }


    /**
     * Converts a Type 0 MIDI file by splitting channels into separate parts.
     *
     * @param track the single MIDI track
     * @param resolution the MIDI resolution
     * @param meta the global meta information
     * @param partList the part list to populate
     * @param parts the parts list to populate
     */
    private void convertType0(Track track, int resolution, GlobalMeta meta,
                              PartListImpl partList, List<PartImpl> parts) {
        Map<Integer, List<NoteOnOff>> channelNotes = new LinkedHashMap<>();

        for (int i = 0; i < track.size(); i++) {
            MidiEvent event = track.get(i);
            MidiMessage message = event.getMessage();
            if (message instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) message;
                collectNoteEvent(sm, event.getTick(), channelNotes);
            }
        }

        int partIndex = 0;
        for (Map.Entry<Integer, List<NoteOnOff>> entry : channelNotes.entrySet()) {
            int channel = entry.getKey();
            List<NoteOnOff> notes = entry.getValue();
            String partId = "P" + (partIndex + 1);
            String partName = "Channel " + (channel + 1);

            ScorePartImpl scorePart = createScorePart(partId, partName, channel, 0);
            partList.getScoreParts().add(scorePart);

            PartImpl part = buildPart(partId, notes, resolution, meta);
            parts.add(part);
            partIndex++;
        }
    }


    /**
     * Converts a Type 1 MIDI file with one part per track.
     *
     * @param tracks the MIDI tracks
     * @param resolution the MIDI resolution
     * @param meta the global meta information
     * @param partList the part list to populate
     * @param parts the parts list to populate
     */
    private void convertType1(Track[] tracks, int resolution, GlobalMeta meta,
                              PartListImpl partList, List<PartImpl> parts) {
        int partIndex = 0;

        for (int t = 0; t < tracks.length; t++) {
            Track track = tracks[t];
            String trackName = extractTrackName(track);
            int channel = -1;
            int program = 0;
            List<NoteOnOff> notes = new ArrayList<>();

            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();

                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    if (sm.getCommand() == ShortMessage.NOTE_ON || sm.getCommand() == ShortMessage.NOTE_OFF) {
                        if (channel < 0) {
                            channel = sm.getChannel();
                        }
                        collectNoteEventSingle(sm, event.getTick(), notes);
                    } else if (sm.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                        program = sm.getData1();
                        if (channel < 0) {
                            channel = sm.getChannel();
                        }
                    }
                }
            }

            if (notes.isEmpty()) {
                continue;
            }

            String partId = "P" + (partIndex + 1);
            if (trackName == null) {
                trackName = "Track " + (t + 1);
            }
            if (channel < 0) {
                channel = 0;
            }

            ScorePartImpl scorePart = createScorePart(partId, trackName, channel, program);
            partList.getScoreParts().add(scorePart);

            PartImpl part = buildPart(partId, notes, resolution, meta);
            parts.add(part);
            partIndex++;
        }
    }


    /**
     * Collects note-on/off events from a ShortMessage into channel-separated lists.
     *
     * @param sm the short message
     * @param tick the event tick
     * @param channelNotes the map of channel to note events
     */
    private void collectNoteEvent(ShortMessage sm, long tick, Map<Integer, List<NoteOnOff>> channelNotes) {
        int channel = sm.getChannel();
        List<NoteOnOff> notes = channelNotes.computeIfAbsent(channel, k -> new ArrayList<>());
        collectNoteEventSingle(sm, tick, notes);
    }


    /**
     * Collects a single note-on/off event, pairing note-off with the matching note-on.
     *
     * @param sm the short message
     * @param tick the event tick
     * @param notes the note list
     */
    private void collectNoteEventSingle(ShortMessage sm, long tick, List<NoteOnOff> notes) {
        int pitch = sm.getData1();
        int velocity = sm.getData2();

        boolean isNoteOff = (sm.getCommand() == ShortMessage.NOTE_OFF)
                || (sm.getCommand() == ShortMessage.NOTE_ON && velocity == 0);

        if (isNoteOff) {
            // Find the matching note-on (last unpaired with same pitch)
            for (int i = notes.size() - 1; i >= 0; i--) {
                NoteOnOff n = notes.get(i);
                if (n.getPitch() == pitch && n.getEndTick() < 0) {
                    n.setEndTick(tick);
                    break;
                }
            }
        } else {
            NoteOnOff n = new NoteOnOff();
            n.setPitch(pitch);
            n.setVelocity(velocity);
            n.setStartTick(tick);
            n.setEndTick(-1);
            notes.add(n);
        }
    }


    /**
     * Builds a part from a list of note events, organizing them into measures.
     *
     * @param partId the part identifier
     * @param notes the note events
     * @param resolution the MIDI resolution
     * @param meta the global meta information
     * @return the constructed part
     */
    private PartImpl buildPart(String partId, List<NoteOnOff> notes, int resolution, GlobalMeta meta) {
        PartImpl part = new PartImpl();
        part.setId(partId);

        int beats = meta.getBeats();
        int beatType = meta.getBeatType();
        long ticksPerMeasure = (long) beats * resolution * 4 / beatType;

        // Close any unpaired notes
        long maxTick = 0;
        for (NoteOnOff n : notes) {
            if (n.getEndTick() < 0) {
                n.setEndTick(n.getStartTick() + resolution);
            }
            if (n.getEndTick() > maxTick) {
                maxTick = n.getEndTick();
            }
        }

        int measureCount = (int) Math.ceil((double) maxTick / ticksPerMeasure);
        if (measureCount == 0) {
            measureCount = 1;
        }

        Collections.sort(notes, (a, b) -> {
            int cmp = Long.compare(a.getStartTick(), b.getStartTick());
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(a.getPitch(), b.getPitch());
        });

        List<MeasureImpl> measures = new ArrayList<>();
        for (int m = 0; m < measureCount; m++) {
            MeasureImpl measure = new MeasureImpl();
            measure.setNumber(String.valueOf(m + 1));

            if (m == 0) {
                measure.setAttributes(new ArrayList<>());
                AttributesImpl attr = new AttributesImpl();
                attr.setDivisions(resolution);

                TimeImpl time = new TimeImpl();
                time.setBeats(String.valueOf(beats));
                time.setBeatType(String.valueOf(beatType));
                List<TimeImpl> times = new ArrayList<>();
                times.add(time);
                attr.setTimes(times);

                if (meta.getKeyFifths() != null) {
                    KeyImpl key = new KeyImpl();
                    key.setFifths(meta.getKeyFifths());
                    key.setMode(meta.getKeyMode());
                    List<KeyImpl> keys = new ArrayList<>();
                    keys.add(key);
                    attr.setKeys(keys);
                }

                ClefImpl clef = new ClefImpl();
                clef.setSign("G");
                clef.setLine(2);
                List<ClefImpl> clefs = new ArrayList<>();
                clefs.add(clef);
                attr.setClefs(clefs);

                measure.getAttributes().add(attr);
            }

            long measureStart = (long) m * ticksPerMeasure;
            long measureEnd = measureStart + ticksPerMeasure;

            List<NoteImpl> measureNotes = buildMeasureNotes(notes, measureStart, measureEnd, resolution);
            if (measureNotes.isEmpty()) {
                NoteImpl wholeRest = createRest(ticksPerMeasure, resolution);
                wholeRest.setVoice(1);
                measureNotes.add(wholeRest);
            }

            measure.setNotes(measureNotes);
            measures.add(measure);
        }

        part.setMeasures(measures);
        return part;
    }


    /**
     * Builds note elements for a single measure from the note events that fall within it.
     *
     * @param allNotes all note events for the part
     * @param measureStart the start tick of the measure
     * @param measureEnd the end tick of the measure
     * @param resolution the MIDI resolution
     * @return the list of notes for this measure
     */
    private List<NoteImpl> buildMeasureNotes(List<NoteOnOff> allNotes, long measureStart,
                                             long measureEnd, int resolution) {
        List<NoteImpl> result = new ArrayList<>();
        long currentTick = measureStart;

        int startIdx = lowerBound(allNotes, measureStart);
        List<NoteOnOff> measureNoteEvents = new ArrayList<>();
        for (int idx = startIdx; idx < allNotes.size(); idx++) {
            NoteOnOff n = allNotes.get(idx);
            if (n.getStartTick() >= measureEnd) {
                break;
            }
            measureNoteEvents.add(n);
        }

        int i = 0;
        while (i < measureNoteEvents.size()) {
            NoteOnOff n = measureNoteEvents.get(i);

            // Insert rest if there is a gap
            if (n.getStartTick() > currentTick) {
                long restDuration = n.getStartTick() - currentTick;
                NoteImpl rest = createRest(restDuration, resolution);
                rest.setVoice(1);
                result.add(rest);
                currentTick = n.getStartTick();
            }

            // Collect chord notes (same start tick)
            boolean isFirst = true;
            long chordStartTick = n.getStartTick();
            while (i < measureNoteEvents.size() && measureNoteEvents.get(i).getStartTick() == chordStartTick) {
                NoteOnOff cn = measureNoteEvents.get(i);
                long endTick = Math.min(cn.getEndTick(), measureEnd);
                long duration = endTick - cn.getStartTick();

                NoteImpl note = createPitchedNote(cn.getPitch(), duration, resolution);
                note.setVoice(1);
                if (!isFirst) {
                    note.setChord("");
                }
                result.add(note);
                isFirst = false;

                long noteEnd = cn.getStartTick() + duration;
                if (noteEnd > currentTick) {
                    currentTick = noteEnd;
                }
                i++;
            }
        }

        return result;
    }


    /**
     * Finds the index of the first note with a start tick greater than or equal to the target.
     * Uses binary search on the sorted notes list.
     *
     * @param notes the sorted list of notes
     * @param targetTick the target tick value
     * @return the index of the first matching note, or notes.size() if none
     */
    private int lowerBound(List<NoteOnOff> notes, long targetTick) {
        int low = 0;
        int high = notes.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (notes.get(mid).getStartTick() < targetTick) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }


    /**
     * Creates a pitched note from a MIDI note number and duration.
     *
     * @param midiNote the MIDI note number
     * @param durationTicks the duration in ticks
     * @param resolution the MIDI resolution
     * @return the constructed note
     */
    private NoteImpl createPitchedNote(int midiNote, long durationTicks, int resolution) {
        NoteImpl note = new NoteImpl();
        note.setPitch(
            (com.github.toolarium.music.model.note.impl.PitchImpl) MidiNoteConverter.midiNoteToPitch(midiNote));
        note.setDuration((int) durationTicks);

        String[] quantized = MidiDurationConverter.quantize(durationTicks, resolution);
        note.setType(quantized[0]);
        int dots = Integer.parseInt(quantized[1]);
        if (dots > 0) {
            List<String> dotList = new ArrayList<>();
            for (int d = 0; d < dots; d++) {
                dotList.add("");
            }
            note.setDots(dotList);
        }

        return note;
    }


    /**
     * Creates a rest note with the given duration.
     *
     * @param durationTicks the duration in ticks
     * @param resolution the MIDI resolution
     * @return the constructed rest note
     */
    private NoteImpl createRest(long durationTicks, int resolution) {
        NoteImpl note = new NoteImpl();
        note.setRest(new RestImpl());
        note.setDuration((int) durationTicks);

        String[] quantized = MidiDurationConverter.quantize(durationTicks, resolution);
        note.setType(quantized[0]);
        int dots = Integer.parseInt(quantized[1]);
        if (dots > 0) {
            List<String> dotList = new ArrayList<>();
            for (int d = 0; d < dots; d++) {
                dotList.add("");
            }
            note.setDots(dotList);
        }

        return note;
    }


    /**
     * Creates a ScorePart definition for a MIDI channel.
     *
     * @param partId the part identifier
     * @param name the part name
     * @param channel the MIDI channel (0-based)
     * @param program the MIDI program number (0-based)
     * @return the constructed score part
     */
    private ScorePartImpl createScorePart(String partId, String name, int channel, int program) {
        ScorePartImpl scorePart = new ScorePartImpl();
        scorePart.setId(partId);
        scorePart.setPartName(name);

        ScoreInstrumentImpl instrument = new ScoreInstrumentImpl();
        instrument.setId(partId + "-I1");
        instrument.setInstrumentName(name);
        scorePart.getScoreInstruments().add(instrument);

        MidiInstrumentImpl midi = new MidiInstrumentImpl();
        midi.setId(partId + "-I1");
        midi.setMidiChannel(channel + 1);
        midi.setMidiProgram(program + 1);
        scorePart.getMidiInstruments().add(midi);

        return scorePart;
    }


    /**
     * Extracts global meta information (tempo, time signature, key signature) from all tracks.
     *
     * @param sequence the MIDI sequence
     * @return the extracted meta information
     */
    private GlobalMeta extractGlobalMeta(Sequence sequence) {
        GlobalMeta meta = new GlobalMeta();

        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();

                if (message instanceof MetaMessage) {
                    MetaMessage mm = (MetaMessage) message;
                    parseMetaMessage(mm, meta);
                }
            }
        }

        return meta;
    }


    /**
     * Parses a meta message and populates the global meta info.
     *
     * @param mm the meta message
     * @param meta the meta info to populate
     */
    private void parseMetaMessage(MetaMessage mm, GlobalMeta meta) {
        byte[] data = mm.getData();

        if (mm.getType() == META_TEMPO && data.length >= 3) {
            int mpqn = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
            if (mpqn > 0) {
                meta.setTempoBpm(MICROSECONDS_PER_MINUTE / mpqn);
            }
        } else if (mm.getType() == META_TIME_SIGNATURE && data.length >= 2) {
            meta.setBeats(data[0] & 0xFF);
            int denomPower = data[1] & 0xFF;
            if (denomPower > 8) {
                denomPower = 2;
            }
            meta.setBeatType(1 << denomPower);
        } else if (mm.getType() == META_KEY_SIGNATURE && data.length >= 2) {
            meta.setKeyFifths((int) data[0]);
            if (data[1] == 0) {
                meta.setKeyMode("major");
            } else {
                meta.setKeyMode("minor");
            }
        }
    }


    /**
     * Extracts the track name from a MIDI track.
     *
     * @param track the track
     * @return the track name, or null if not found
     */
    private String extractTrackName(Track track) {
        for (int i = 0; i < track.size(); i++) {
            MidiEvent event = track.get(i);
            if (event.getMessage() instanceof MetaMessage) {
                MetaMessage mm = (MetaMessage) event.getMessage();
                if (mm.getType() == META_TRACK_NAME) {
                    return new String(mm.getData(), StandardCharsets.US_ASCII).trim();
                }
            }
        }
        return null;
    }


    /**
     * Holds global meta information extracted from the MIDI sequence.
     */
    private static final class GlobalMeta {
        private int tempoBpm = 120;
        private int beats = DEFAULT_BEATS;
        private int beatType = DEFAULT_BEAT_TYPE;
        private Integer keyFifths;
        private String keyMode;

        /**
         * Gets the tempo in BPM.
         *
         * @return the tempo
         */
        int getTempoBpm() {
            return tempoBpm;
        }

        /**
         * Gets the beats per measure.
         *
         * @return the beats
         */
        int getBeats() {
            return beats;
        }

        /**
         * Gets the beat type (denominator).
         *
         * @return the beat type
         */
        int getBeatType() {
            return beatType;
        }

        /**
         * Gets the key fifths value.
         *
         * @return the key fifths, or null
         */
        Integer getKeyFifths() {
            return keyFifths;
        }

        /**
         * Gets the key mode.
         *
         * @return the key mode
         */
        String getKeyMode() {
            return keyMode;
        }

        /**
         * Sets the tempo in BPM.
         *
         * @param tempoBpm the tempo
         */
        void setTempoBpm(int tempoBpm) {
            this.tempoBpm = tempoBpm;
        }

        /**
         * Sets the beats per measure.
         *
         * @param beats the beats
         */
        void setBeats(int beats) {
            this.beats = beats;
        }

        /**
         * Sets the beat type (denominator).
         *
         * @param beatType the beat type
         */
        void setBeatType(int beatType) {
            this.beatType = beatType;
        }

        /**
         * Sets the key fifths value.
         *
         * @param keyFifths the key fifths
         */
        void setKeyFifths(Integer keyFifths) {
            this.keyFifths = keyFifths;
        }

        /**
         * Sets the key mode.
         *
         * @param keyMode the key mode
         */
        void setKeyMode(String keyMode) {
            this.keyMode = keyMode;
        }
    }


    /**
     * Holds a paired note-on/note-off event.
     */
    private static final class NoteOnOff {
        private int pitch;
        private int velocity;
        private long startTick;
        private long endTick;

        /**
         * Gets the MIDI pitch number.
         *
         * @return the pitch
         */
        int getPitch() {
            return pitch;
        }

        /**
         * Gets the velocity.
         *
         * @return the velocity
         */
        int getVelocity() {
            return velocity;
        }

        /**
         * Gets the start tick.
         *
         * @return the start tick
         */
        long getStartTick() {
            return startTick;
        }

        /**
         * Gets the end tick.
         *
         * @return the end tick
         */
        long getEndTick() {
            return endTick;
        }

        /**
         * Sets the MIDI pitch number.
         *
         * @param pitch the pitch
         */
        void setPitch(int pitch) {
            this.pitch = pitch;
        }

        /**
         * Sets the velocity.
         *
         * @param velocity the velocity
         */
        void setVelocity(int velocity) {
            this.velocity = velocity;
        }

        /**
         * Sets the start tick.
         *
         * @param startTick the start tick
         */
        void setStartTick(long startTick) {
            this.startTick = startTick;
        }

        /**
         * Sets the end tick.
         *
         * @param endTick the end tick
         */
        void setEndTick(long endTick) {
            this.endTick = endTick;
        }
    }
}
