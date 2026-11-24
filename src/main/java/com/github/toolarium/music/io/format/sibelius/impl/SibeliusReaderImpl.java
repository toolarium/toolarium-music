/*
 * SibeliusReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.DirectionTypeImpl;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.notation.impl.DynamicsImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import com.github.toolarium.music.model.note.impl.PitchImpl;
import com.github.toolarium.music.model.note.impl.RestImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.CreatorImpl;
import com.github.toolarium.music.model.score.impl.EncodingImpl;
import com.github.toolarium.music.model.score.impl.IdentificationImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Reads Sibelius .sib, .sco, and .si7 files and converts them to an {@link IScore} model.
 *
 * <p>The conversion pipeline is: container parsing, stream cipher decryption,
 * zlib decompression, and payload field extraction to IScore model.</p>
 *
 * <p>The payload parser currently extracts score metadata (title, composer, copyright)
 * and basic structure. Full note-level parsing will be extended progressively as the
 * payload field schema is mapped.</p>
 *
 * @author patrick
 */
public class SibeliusReaderImpl implements IMusicReader {
    private static final Logger LOG = LoggerFactory.getLogger(SibeliusReaderImpl.class);
    private static final int DIVISIONS = 256;
    private static final String ENCODING_SOFTWARE = "Sibelius";
    private static final String COPYRIGHT_SYMBOL = "©";
    private static final int METADATA_REGION_END = 0x0400;
    private static final int MAX_STRING_BYTE_LENGTH = 4000;
    private static final int MAX_EXPECTED_PARTS = 100;


    /**
     * Constructor.
     */
    public SibeliusReaderImpl() {
        // NOP
    }


    /**
     * Reads a Sibelius file from the given path and converts it to an {@link IScore} model.
     *
     * @param path the path to the Sibelius file
     * @return the parsed score
     * @throws IOException if the file cannot be read or parsed
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (path == null) {
            throw new IOException("Path must not be null.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading Sibelius file: {}", path);
        }

        try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
            return read(is);
        }
    }


    /**
     * Reads a Sibelius file from the given input stream and converts it to an {@link IScore} model.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException if the stream cannot be read or the data cannot be parsed
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream must not be null.");
        }

        SibeliusContainerReader containerReader = new SibeliusContainerReader();
        byte[] payload = containerReader.readAndDecompress(inputStream);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Payload size: {} bytes", payload.length);
        }

        return parsePayload(payload);
    }


    /**
     * Parses the decompressed payload bytes into an IScore model.
     *
     * <p>The payload contains a hierarchical structure of named fields. Strings
     * are length-prefixed UTF-16BE. Numeric values use little-endian IEEE 754
     * floats and big-endian integer pairs. The payload includes a type dictionary
     * region that lists all object types used in the score.</p>
     *
     * @param payload the decompressed payload bytes
     * @return the parsed score
     * @throws IOException if the payload cannot be parsed
     */
    private IScore parsePayload(byte[] payload) throws IOException {
        IScore decoded = SibeliusScoreDecoder.getInstance().decode(payload);
        if (decoded != null) {
            return decoded;
        }

        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");

        // Extract strings from the header region (first ~0x400 bytes contain metadata)
        List<PayloadString> headerStrings = extractStringsInRange(payload, 0, Math.min(payload.length, METADATA_REGION_END));
        // Extract instrument info from the payload
        List<InstrumentInfo> instruments = extractInstruments(payload, headerStrings);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Extracted {} header strings, {} instruments", headerStrings.size(), instruments.size());
        }

        // Extract notes from the payload
        SibeliusNoteExtractor extractor = new SibeliusNoteExtractor();
        List<SibeliusNoteExtractor.SibNote> notes = extractor.extractNotes(payload);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Extracted {} header strings, {} instruments, {} notes",
                    headerStrings.size(), instruments.size(), notes.size());
        }

        // Build identification from extracted metadata
        buildIdentification(score, headerStrings);

        // Build part structure from extracted instruments and notes
        buildParts(score, instruments, notes);

        return score;
    }


    /**
     * Builds the score identification (metadata) from header strings.
     *
     * <p>The header region contains title, composer, lyricist, and copyright
     * as consecutive length-prefixed UTF-16BE strings. Title and lyricist
     * strings are prefixed with an ampersand (&amp;) character.</p>
     *
     * @param score the score to populate
     * @param headerStrings strings extracted from the header region
     */
    private void buildIdentification(ScoreImpl score, List<PayloadString> headerStrings) {
        IdentificationImpl identification = new IdentificationImpl();
        EncodingImpl encoding = new EncodingImpl();
        encoding.setSoftware(Collections.singletonList(ENCODING_SOFTWARE));
        identification.setEncoding(encoding);

        String title = null;
        String composer = null;
        String lyricist = null;
        String copyright = null;

        for (PayloadString ps : headerStrings) {
            String text = ps.getText();

            if (copyright == null && (text.startsWith("Copyright") || text.contains(COPYRIGHT_SYMBOL))) {
                copyright = text;
                continue;
            }

            // Skip internal/technical strings
            if (isInternalString(text)) {
                continue;
            }

            // Strings prefixed with & are metadata (title, then lyricist)
            if (text.startsWith("&")) {
                String cleaned = text.substring(1);
                if (title == null) {
                    title = cleaned;
                } else if (lyricist == null) {
                    lyricist = cleaned;
                }
                continue;
            }

            // The first short standalone string after title is typically the composer
            if (composer == null && title != null && text.length() > 2 && text.length() < 100
                    && !text.contains("\\") && !text.contains(":") && !text.contains(".")) {
                composer = text;
            }
        }

        if (title != null) {
            score.setMovementTitle(title);
        }

        List<CreatorImpl> creators = new ArrayList<>();
        if (composer != null) {
            CreatorImpl composerCreator = new CreatorImpl();
            composerCreator.setType("composer");
            composerCreator.setValue(composer);
            creators.add(composerCreator);
        }
        if (lyricist != null) {
            CreatorImpl lyricistCreator = new CreatorImpl();
            lyricistCreator.setType("lyricist");
            lyricistCreator.setValue(lyricist);
            creators.add(lyricistCreator);
        }
        if (!creators.isEmpty()) {
            identification.setCreators(creators);
        }
        if (copyright != null) {
            identification.setRights(copyright);
        }

        score.setIdentification(identification);
    }


    /**
     * Extracts instrument/part information from the payload.
     *
     * <p>Sibelius stores instrument definitions in two locations:
     * (1) a combined part-name string in the header with instrument names
     * separated by literal "\n" sequences, and (2) detailed staff definitions
     * deeper in the payload with sound IDs, clef types, and staff types.</p>
     *
     * @param payload the decompressed payload
     * @param headerStrings previously extracted header strings
     * @return the list of instrument info records
     */
    private List<InstrumentInfo> extractInstruments(byte[] payload, List<PayloadString> headerStrings) {
        List<InstrumentInfo> result = new ArrayList<>();

        // First try: extract from combined part-name string in header
        List<String> partNames = extractPartNamesFromHeader(headerStrings);

        // Second: scan for "instrument." sound IDs in the staff definition region
        List<PayloadString> allStrings = extractStringsInRange(payload, 0, payload.length);
        List<String> soundIds = new ArrayList<>();
        List<String> clefIds = new ArrayList<>();
        List<String> staffTypes = new ArrayList<>();

        boolean inStaffDefs = false;
        for (PayloadString ps : allStrings) {
            String text = ps.getText();

            if (text.startsWith("stafftype.pitched.") || text.startsWith("stafftype.tab.")) {
                inStaffDefs = true;
            }

            // Collect instrument sound IDs that appear after the staff type definitions
            // These follow a pattern: each real staff has instrument., clef., stafftype., voice. strings
            if (text.startsWith("instrument.singers.") || text.startsWith("instrument.keyboard.")
                    || text.startsWith("instrument.strings.") || text.startsWith("instrument.woodwind.")
                    || text.startsWith("instrument.brass.") || text.startsWith("instrument.fretted.")
                    || text.startsWith("instrument.pitchedpercussion.")
                    || text.startsWith("instrument.unpitchedpercussion.")) {
                if (inStaffDefs) {
                    soundIds.add(text);
                }
            }

            if (inStaffDefs && text.startsWith("clef.") && !text.contains("percussion")) {
                clefIds.add(text);
            }
        }

        // Build instrument info from collected data
        if (!partNames.isEmpty()) {
            for (int i = 0; i < partNames.size(); i++) {
                String name = partNames.get(i);
                String soundId = null;
                if (i < soundIds.size()) {
                    soundId = soundIds.get(i);
                }
                String clefId = null;
                if (i < clefIds.size()) {
                    clefId = clefIds.get(i);
                }
                result.add(new InstrumentInfo(name, soundId, clefId));
            }
        } else if (!soundIds.isEmpty()) {
            // Derive part names from sound IDs
            for (int i = 0; i < soundIds.size(); i++) {
                String soundId = soundIds.get(i);
                String name = deriveNameFromSoundId(soundId);
                String clefId = null;
                if (i < clefIds.size()) {
                    clefId = clefIds.get(i);
                }
                result.add(new InstrumentInfo(name, soundId, clefId));
            }
        }

        if (result.isEmpty()) {
            result.add(new InstrumentInfo("Part 1", null, null));
        }

        return result;
    }


    /**
     * Extracts part names from the header region's combined name string.
     *
     * @param headerStrings strings from the header region
     * @return the list of part names
     */
    private List<String> extractPartNamesFromHeader(List<PayloadString> headerStrings) {
        List<String> result = new ArrayList<>();

        for (PayloadString ps : headerStrings) {
            String text = ps.getText();
            if (text.contains("\\n\\") && text.length() > 4) {
                String[] parts = text.split("\\\\n\\\\");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        result.add(trimmed);
                    }
                }
                break;
            }
        }

        return result;
    }


    /**
     * Derives a human-readable instrument name from a Sibelius sound ID.
     *
     * @param soundId the sound ID (e.g., "instrument.singers.soprano")
     * @return the derived name (e.g., "Soprano")
     */
    private String deriveNameFromSoundId(String soundId) {
        // Sound IDs follow the pattern: instrument.category.name[.variant]
        String[] parts = soundId.split("\\.");
        if (parts.length >= 3) {
            String name = parts[2];
            // Capitalize first letter
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return soundId;
    }


    /**
     * Builds the part list and part structure from extracted instrument info and notes.
     *
     * <p>When notes are available, they are distributed into a single part with
     * sequential measures. Without staff assignment data, all notes go into the
     * first part. Each measure holds up to one bar's worth of notes (based on 4/4 time).</p>
     *
     * @param score the score to populate
     * @param instruments the list of instrument info records
     * @param notes the extracted notes
     */
    private void buildParts(ScoreImpl score, List<InstrumentInfo> instruments,
                            List<SibeliusNoteExtractor.SibNote> notes) {
        List<ScorePartImpl> scoreParts = new ArrayList<>();
        List<PartImpl> parts = new ArrayList<>();

        for (int i = 0; i < instruments.size(); i++) {
            String id = "P" + (i + 1);
            InstrumentInfo inst = instruments.get(i);

            ScorePartImpl scorePart = new ScorePartImpl();
            scorePart.setId(id);
            scorePart.setPartName(inst.getName());
            scoreParts.add(scorePart);

            PartImpl part = new PartImpl();
            part.setId(id);

            if (i == 0 && !notes.isEmpty()) {
                part.setMeasures(buildMeasuresFromNotes(inst, notes));
            } else {
                part.setMeasures(Collections.singletonList(createDefaultMeasure(inst)));
            }

            parts.add(part);
        }

        PartListImpl partList = new PartListImpl();
        partList.setScoreParts(scoreParts);
        score.setPartList(partList);
        score.setParts(parts);
    }


    /**
     * Builds measures from extracted notes, grouping them by bar capacity.
     *
     * <p>Notes are placed sequentially into measures. A new measure starts when
     * the accumulated duration reaches the bar capacity (default 4/4 = 1024 ticks).
     * The first measure includes key, time, and clef attributes.</p>
     *
     * @param inst the instrument info for clef mapping
     * @param notes the extracted notes
     * @return the list of measures
     */
    private List<MeasureImpl> buildMeasuresFromNotes(InstrumentInfo inst,
                                                     List<SibeliusNoteExtractor.SibNote> notes) {
        List<MeasureImpl> measures = new ArrayList<>();
        int barCapacity = DIVISIONS * 4; // 4/4 time = 1024 ticks
        int currentTicks = 0;
        int measureNum = 1;
        List<NoteImpl> currentNotes = new ArrayList<>();
        List<DirectionImpl> currentDirections = new ArrayList<>();

        for (SibeliusNoteExtractor.SibNote sn : notes) {
            if (currentTicks + sn.getDuration() > barCapacity && !currentNotes.isEmpty()) {
                // Fill remaining space with rest
                int remaining = barCapacity - currentTicks;
                if (remaining > 0) {
                    currentNotes.add(createRest(remaining));
                }

                measures.add(buildMeasure(measureNum, inst, currentNotes, currentDirections, measureNum == 1));
                measureNum++;
                currentNotes = new ArrayList<>();
                currentDirections = new ArrayList<>();
                currentTicks = 0;
            }

            NoteImpl note = new NoteImpl();
            PitchImpl pitch = new PitchImpl();
            pitch.setStep(sn.getStep());
            pitch.setOctave(sn.getOctave());
            note.setPitch(pitch);
            note.setDuration(sn.getDuration());
            note.setType(sn.getType());

            // Attach lyric if present
            if (sn.getLyricText() != null) {
                LyricImpl lyric = new LyricImpl();
                lyric.setNumber("1");
                lyric.setSyllabic("single");
                lyric.setText(sn.getLyricText());
                note.setLyrics(Collections.singletonList(lyric));
            }

            currentNotes.add(note);

            // Add dynamic direction before the note if present
            if (sn.getDynamic() != null) {
                currentDirections.add(createDynamicDirection(sn.getDynamic()));
            }

            currentTicks += sn.getDuration();
        }

        // Flush remaining notes
        if (!currentNotes.isEmpty()) {
            int remaining = barCapacity - currentTicks;
            if (remaining > 0) {
                currentNotes.add(createRest(remaining));
            }
            measures.add(buildMeasure(measureNum, inst, currentNotes, currentDirections, measureNum == 1));
        }

        if (measures.isEmpty()) {
            measures.add(createDefaultMeasure(inst));
        }

        return measures;
    }


    /**
     * Builds a single measure from a list of notes and directions.
     *
     * @param number the measure number
     * @param inst the instrument info for attributes
     * @param notes the notes for this measure
     * @param directions the directions (dynamics) for this measure
     * @param includeAttributes true to include key/time/clef attributes
     * @return the measure
     */
    private MeasureImpl buildMeasure(int number, InstrumentInfo inst,
                                     List<NoteImpl> notes, List<DirectionImpl> directions,
                                     boolean includeAttributes) {
        MeasureImpl measure = new MeasureImpl();
        measure.setNumber(String.valueOf(number));

        if (includeAttributes) {
            AttributesImpl attrs = new AttributesImpl();
            attrs.setDivisions(DIVISIONS);
            TimeImpl time = new TimeImpl();
            time.setBeats("4");
            time.setBeatType("4");
            attrs.setTimes(Collections.singletonList(time));
            KeyImpl key = new KeyImpl();
            key.setFifths(0);
            attrs.setKeys(Collections.singletonList(key));
            attrs.setClefs(Collections.singletonList(mapClef(inst)));
            measure.setAttributes(Collections.singletonList(attrs));
        }

        if (!directions.isEmpty()) {
            measure.setDirections(new ArrayList<>(directions));
        }

        measure.setNotes(new ArrayList<>(notes));
        return measure;
    }


    /**
     * Creates a MusicXML direction element for a dynamic marking.
     *
     * @param dynamic the dynamic text (e.g., "f", "p", "mf", "ff")
     * @return the direction
     */
    private DirectionImpl createDynamicDirection(String dynamic) {
        DirectionImpl direction = new DirectionImpl();
        direction.setPlacement("below");

        DynamicsImpl dynamics = new DynamicsImpl();
        if ("f".equals(dynamic)) {
            dynamics.setForte("");
        } else if ("ff".equals(dynamic)) {
            dynamics.setFf("");
        } else if ("fff".equals(dynamic)) {
            dynamics.setFff("");
        } else if ("p".equals(dynamic)) {
            dynamics.setPiano("");
        } else if ("pp".equals(dynamic)) {
            dynamics.setPp("");
        } else if ("ppp".equals(dynamic)) {
            dynamics.setPpp("");
        } else if ("mp".equals(dynamic)) {
            dynamics.setMp("");
        } else if ("mf".equals(dynamic)) {
            dynamics.setMf("");
        } else if ("sf".equals(dynamic)) {
            dynamics.setSf("");
        } else if ("sfz".equals(dynamic)) {
            dynamics.setSfz("");
        } else if ("fp".equals(dynamic)) {
            dynamics.setFp("");
        } else {
            dynamics.setForte("");
        }

        DirectionTypeImpl dirType = new DirectionTypeImpl();
        dirType.setDynamics(dynamics);
        direction.setDirectionTypes(Collections.singletonList(dirType));

        return direction;
    }


    /**
     * Creates a rest note with the given duration.
     *
     * @param duration the duration in ticks
     * @return the rest note
     */
    private NoteImpl createRest(int duration) {
        NoteImpl rest = new NoteImpl();
        rest.setRest(new RestImpl());
        rest.setDuration(duration);
        rest.setType(ticksToType(duration));
        return rest;
    }


    /**
     * Converts a tick duration to the closest MusicXML note type name.
     *
     * @param ticks the duration in ticks (256 = quarter)
     * @return the note type name
     */
    private String ticksToType(int ticks) {
        if (ticks >= 2048) {
            return "breve";
        } else if (ticks >= 1024) {
            return "whole";
        } else if (ticks >= 512) {
            return "half";
        } else if (ticks >= 256) {
            return "quarter";
        } else if (ticks >= 128) {
            return "eighth";
        } else if (ticks >= 64) {
            return "16th";
        } else if (ticks >= 32) {
            return "32nd";
        }
        return "64th";
    }


    /**
     * Creates a default measure with attributes derived from the instrument info
     * and a whole rest as placeholder content.
     *
     * @param inst the instrument info with clef and sound ID
     * @return the default measure
     */
    private MeasureImpl createDefaultMeasure(InstrumentInfo inst) {
        MeasureImpl measure = new MeasureImpl();
        measure.setNumber("1");

        AttributesImpl attrs = new AttributesImpl();
        attrs.setDivisions(DIVISIONS);
        TimeImpl time = new TimeImpl();
        time.setBeats("4");
        time.setBeatType("4");
        attrs.setTimes(Collections.singletonList(time));
        KeyImpl key = new KeyImpl();
        key.setFifths(0);
        attrs.setKeys(Collections.singletonList(key));

        ClefImpl clef = mapClef(inst);
        attrs.setClefs(Collections.singletonList(clef));
        measure.setAttributes(Collections.singletonList(attrs));

        NoteImpl restNote = new NoteImpl();
        restNote.setRest(new RestImpl());
        restNote.setDuration(DIVISIONS * 4);
        restNote.setType("whole");
        measure.setNotes(Collections.singletonList(restNote));

        return measure;
    }


    /**
     * Maps a Sibelius clef ID to a MusicXML clef.
     *
     * @param inst the instrument info
     * @return the mapped clef
     */
    private ClefImpl mapClef(InstrumentInfo inst) {
        ClefImpl clef = new ClefImpl();
        String clefId = inst.getClefId();

        if (clefId != null) {
            if (clefId.startsWith("clef.bass")) {
                clef.setSign("F");
                clef.setLine(4);
                return clef;
            }
            if (clefId.startsWith("clef.alto") || clefId.equals("clef.c")) {
                clef.setSign("C");
                clef.setLine(3);
                return clef;
            }
            if (clefId.startsWith("clef.tenor")) {
                clef.setSign("C");
                clef.setLine(4);
                return clef;
            }
            if (clefId.startsWith("clef.treble.down.8")) {
                clef.setSign("G");
                clef.setLine(2);
                clef.setClefOctaveChange(-1);
                return clef;
            }
            if (clefId.startsWith("clef.treble.up.8")) {
                clef.setSign("G");
                clef.setLine(2);
                clef.setClefOctaveChange(1);
                return clef;
            }
            if (clefId.startsWith("clef.percussion")) {
                clef.setSign("percussion");
                clef.setLine(2);
                return clef;
            }
            if (clefId.startsWith("clef.tab")) {
                clef.setSign("TAB");
                clef.setLine(5);
                return clef;
            }
        }

        // Fallback: derive from instrument name or sound ID
        String name = inst.getName().toLowerCase();
        String sound = "";
        if (inst.getSoundId() != null) {
            sound = inst.getSoundId().toLowerCase();
        }
        if (name.contains("bass") || name.contains("cello") || name.contains("tuba")
                || name.contains("trombone") || name.contains("bassoon")
                || sound.contains(".bass") || sound.contains(".tuba")
                || sound.contains(".trombone") || sound.contains(".bassoon")) {
            clef.setSign("F");
            clef.setLine(4);
        } else {
            clef.setSign("G");
            clef.setLine(2);
        }

        return clef;
    }


    /**
     * Checks if a string is an internal/technical string that should be skipped
     * during metadata extraction.
     *
     * @param text the string to check
     * @return true if the string is internal and should be skipped
     */
    private boolean isInternalString(String text) {
        return text.startsWith("SCEW ") || text.startsWith("line.") || text.startsWith("text.")
                || text.startsWith("pen.") || text.startsWith("symbol.")
                || text.contains("Kontakt") || text.contains("Wavetable") || text.contains("General MIDI")
                || text.contains("detach") || text.contains("legato")
                || text.contains("Hairpin") || text.contains("Gliss")
                || text.equals("Full Score") || text.equals("Duration")
                || text.contains("New Roman") || text.contains("Opus")
                || text.equals("Pause") || text.equals("Swing");
    }


    /**
     * Extracts length-prefixed UTF-16BE strings from a specific range of the payload.
     *
     * <p>Strings in the Sibelius payload are stored as: a u16 BE byte-length
     * followed by that many bytes of UTF-16BE character data. The byte length
     * is always even.</p>
     *
     * @param payload the decompressed payload bytes
     * @param startOffset the start offset in the payload
     * @param endOffset the end offset in the payload (exclusive)
     * @return the list of extracted strings with their offsets
     */
    private List<PayloadString> extractStringsInRange(byte[] payload, int startOffset, int endOffset) {
        List<PayloadString> result = new ArrayList<>();
        int pos = startOffset;
        int limit = Math.min(endOffset, payload.length);

        while (pos < limit - 4) {
            int len = ((payload[pos] & 0xFF) << 8) | (payload[pos + 1] & 0xFF);

            if (len >= 2 && len <= MAX_STRING_BYTE_LENGTH && len % 2 == 0 && pos + 2 + len <= limit) {
                if (looksLikeUtf16BE(payload, pos + 2, len)) {
                    String text = new String(payload, pos + 2, len, StandardCharsets.UTF_16BE);
                    if (text.length() >= 1 && hasReadableContent(text)) {
                        result.add(new PayloadString(pos, text));
                        pos += 2 + len;
                        continue;
                    }
                }
            }

            pos += 2;
        }

        return result;
    }


    /**
     * Checks if a byte range looks like valid UTF-16BE text.
     *
     * @param data the byte array
     * @param offset the start offset
     * @param length the number of bytes to check
     * @return true if the bytes look like UTF-16BE encoded text
     */
    private boolean looksLikeUtf16BE(byte[] data, int offset, int length) {
        int checkLen = Math.min(length, 20);
        for (int j = 0; j < checkLen; j += 2) {
            int hi = data[offset + j] & 0xFF;
            if (hi > 1) {
                return false;
            }
        }
        return true;
    }


    /**
     * Checks if a string contains at least one printable character.
     *
     * @param text the string to check
     * @return true if the string has readable content
     */
    private boolean hasReadableContent(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x20 && c < 0xFFFE) {
                return true;
            }
        }
        return false;
    }


    /**
     * Holds instrument definition data extracted from the payload.
     *
     * @author patrick
     */
    private static final class InstrumentInfo {
        private final String name;
        private final String soundId;
        private final String clefId;

        /**
         * Constructor.
         *
         * @param name the instrument display name
         * @param soundId the Sibelius sound ID (e.g., "instrument.singers.soprano"), or null
         * @param clefId the Sibelius clef ID (e.g., "clef.treble"), or null
         */
        InstrumentInfo(String name, String soundId, String clefId) {
            this.name = name;
            this.soundId = soundId;
            this.clefId = clefId;
        }

        /**
         * Gets the instrument display name.
         *
         * @return the name
         */
        String getName() {
            return name;
        }

        /**
         * Gets the Sibelius sound ID.
         *
         * @return the sound ID, or null
         */
        String getSoundId() {
            return soundId;
        }

        /**
         * Gets the Sibelius clef ID.
         *
         * @return the clef ID, or null
         */
        String getClefId() {
            return clefId;
        }
    }


    /**
     * Represents a string extracted from the payload with its byte offset.
     *
     * @author patrick
     */
    private static final class PayloadString {
        private final int offset;
        private final String text;

        /**
         * Constructor.
         *
         * @param offset the byte offset in the payload
         * @param text the decoded string
         */
        PayloadString(int offset, String text) {
            this.offset = offset;
            this.text = text;
        }


        /**
         * Gets the byte offset in the payload.
         *
         * @return the byte offset
         */
        int getOffset() {
            return offset;
        }


        /**
         * Gets the decoded string.
         *
         * @return the decoded string
         */
        String getText() {
            return text;
        }
    }
}
