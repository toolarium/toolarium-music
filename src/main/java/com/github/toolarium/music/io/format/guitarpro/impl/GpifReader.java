/*
 * GpifReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl;

import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.SpelledPitch;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpBar;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpBeat;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpLyricLine;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpMasterBar;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpNote;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpSong;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpTempo;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpTrack;
import com.github.toolarium.music.io.xml.XmlDocumentReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;


/**
 * Reads the GPIF document (score.gpif) of Guitar Pro 6, 7 and 8 files into a {@link GpSong}.
 *
 * <p>The GPIF document stores the elements of a score in flat lists that reference each other by
 * ID: master bars reference bars (one per track and staff), bars reference voices, voices reference
 * beats, beats reference notes and rhythms.</p>
 *
 * @author patrick
 */
final class GpifReader {
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int DRUM_CHANNEL = 9;
    private static final int MAX_MIDI = 127;
    private static final int QUARTERS_PER_WHOLE = 4;
    private static final String NAME = "name";
    private static final String WHITESPACE = "\\s+";

    private final XmlDocumentReader xml = XmlDocumentReader.getInstance();
    private final GuitarProConverter converter = GuitarProConverter.getInstance();
    private final Map<String, Element> bars = new HashMap<>();
    private final Map<String, Element> voices = new HashMap<>();
    private final Map<String, Element> beats = new HashMap<>();
    private final Map<String, Element> notes = new HashMap<>();
    private final Map<String, Element> rhythms = new HashMap<>();
    private final List<Map<String, String>> chordNames = new ArrayList<>();


    /**
     * Constructor for GpifReader.
     */
    GpifReader() {
        // default constructor
    }


    /**
     * Reads the GPIF document.
     *
     * @param root the GPIF root element
     * @param defaultVersion the version if the document has none (6 for .gpx files)
     * @return the song
     */
    GpSong read(Element root, String defaultVersion) {
        GpSong song = new GpSong();
        String version = xml.childText(root, "GPVersion");
        if (version == null) {
            version = defaultVersion;
        }
        song.setVersion(version);
        readScore(xml.child(root, "Score"), song);
        index(xml.child(root, "Bars"), "Bar", bars);
        index(xml.child(root, "Voices"), "Voice", voices);
        index(xml.child(root, "Beats"), "Beat", beats);
        index(xml.child(root, "Notes"), "Note", notes);
        index(xml.child(root, "Rhythms"), "Rhythm", rhythms);

        for (Element track : xml.children(xml.child(root, "Tracks"), "Track")) {
            song.getTracks().add(readTrack(track, song));
        }
        for (Element masterBar : xml.children(xml.child(root, "MasterBars"), "MasterBar")) {
            readMasterBar(masterBar, song);
        }
        readAutomations(xml.child(root, "MasterTrack"), song);
        return song;
    }


    /**
     * Reads the score metadata.
     *
     * @param score the Score element (may be null)
     * @param song the song
     */
    private void readScore(Element score, GpSong song) {
        song.setTitle(xml.childText(score, "Title"));
        song.setSubtitle(xml.childText(score, "SubTitle"));
        song.setArtist(xml.childText(score, "Artist"));
        song.setAlbum(xml.childText(score, "Album"));
        song.setWords(firstNonNull(xml.childText(score, "Words"), xml.childText(score, "WordsAndMusic")));
        song.setMusic(firstNonNull(xml.childText(score, "Music"), xml.childText(score, "WordsAndMusic")));
        song.setCopyright(xml.childText(score, "Copyright"));
        song.setTab(xml.childText(score, "Tabber"));
        song.setInstructions(xml.childText(score, "Instructions"));
    }


    /**
     * Reads a track: name, instrument, MIDI settings, tuning, transposition, drum kit and lyrics.
     *
     * @param track the Track element
     * @param song the song (lyrics are added)
     * @return the track
     */
    private GpTrack readTrack(Element track, GpSong song) {
        GpTrack result = new GpTrack();
        result.setName(xml.childText(track, "Name"));
        result.setShortName(xml.childText(track, "ShortName"));

        Element instrumentSet = xml.child(track, "InstrumentSet");
        String type = xml.childText(instrumentSet, "Type");
        Element instrument = xml.child(track, "Instrument");
        if ("drumKit".equals(type) || "drumKit".equals(xml.attr(instrument, "ref"))) {
            result.setPercussion(true);
        }
        int articulation = 0;
        for (Element element : xml.children(xml.child(instrumentSet, "Elements"), "Element")) {
            for (Element entry : xml.children(xml.child(element, "Articulations"), "Articulation")) {
                int midi = intValue(xml.childText(entry, "OutputMidiNumber"), -1);
                if (midi >= 0) {
                    result.getDrumMidi().put(articulation, midi);
                }
                articulation++;
            }
        }

        Element sound = xml.child(xml.child(track, "Sounds"), "Sound");
        Integer program = null;
        if (sound != null) {
            program = intOrNull(xml.childText(xml.child(sound, "MIDI"), "Program"));
        }
        Element generalMidi = xml.child(track, "GeneralMidi");
        if (program == null && generalMidi != null) {
            program = intOrNull(xml.childText(generalMidi, "Program"));
        }
        Integer channel = intOrNull(xml.childText(xml.child(track, "MidiConnection"), "PrimaryChannel"));
        if (channel == null && generalMidi != null) {
            channel = intOrNull(xml.childText(generalMidi, "PrimaryChannel"));
        }
        if (channel != null && channel >= 0 && channel < 16) {
            result.setChannel(channel + 1);
            if (channel == DRUM_CHANNEL) {
                result.setPercussion(true);
            }
        }
        if (program != null && program >= 0 && program <= MAX_MIDI && !result.isPercussion()) {
            result.setProgram(program + 1);
        }

        Element transpose = xml.child(track, "Transpose");
        if (transpose != null) {
            result.setTranspose(intValue(xml.childText(transpose, "Chromatic"), 0)
                    + SEMITONES_PER_OCTAVE * intValue(xml.childText(transpose, "Octave"), 0));
        } else if (!result.isPercussion() && converter.isOctaveTransposing(result.getProgram())) {
            result.setTranspose(-SEMITONES_PER_OCTAVE);
        }

        List<Element> staves = xml.children(xml.child(track, "Staves"), "Staff");
        result.setStaffCount(Math.max(1, staves.size()));
        Element properties = xml.child(track, "Properties");
        if (!staves.isEmpty()) {
            properties = xml.child(staves.get(0), "Properties");
        }
        readTrackProperties(properties, result);
        readLyrics(xml.child(track, "Lyrics"), song, result);
        return result;
    }


    /**
     * Reads the track or staff properties: tuning, capo and chord diagram names.
     *
     * @param properties the Properties element (may be null)
     * @param track the track
     */
    private void readTrackProperties(Element properties, GpTrack track) {
        Map<String, String> names = new HashMap<>();
        for (Element property : xml.children(properties, "Property")) {
            String name = xml.attr(property, NAME);
            if ("Tuning".equals(name)) {
                String pitches = xml.childText(property, "Pitches");
                if (pitches != null) {
                    List<Integer> tuning = new ArrayList<>();
                    for (String pitch : pitches.trim().split(WHITESPACE)) {
                        tuning.add(intValue(pitch, 0));
                    }
                    Collections.reverse(tuning);
                    track.getTuning().clear();
                    track.getTuning().addAll(tuning);
                }
            } else if ("CapoFret".equals(name)) {
                track.setCapo(intValue(xml.childText(property, "Fret"), 0));
            } else if ("DiagramCollection".equals(name) || "ChordCollection".equals(name)) {
                for (Element item : xml.children(xml.child(property, "Items"), "Item")) {
                    String id = xml.attr(item, "id");
                    String chordName = xml.attr(item, NAME);
                    if (id != null && chordName != null && !chordName.trim().isEmpty()) {
                        names.put(id, chordName.trim());
                    }
                }
            }
        }
        chordNames.add(names);
    }


    /**
     * Reads the track lyrics (lines with a measure offset).
     *
     * @param lyrics the Lyrics element (may be null)
     * @param song the song
     * @param track the track
     */
    private void readLyrics(Element lyrics, GpSong song, GpTrack track) {
        if (lyrics == null || song.getLyricsTrack() >= 0) {
            return;
        }
        List<GpLyricLine> lines = new ArrayList<>();
        for (Element line : xml.children(lyrics, "Line")) {
            String text = xml.childText(line, "Text");
            if (text != null) {
                lines.add(new GpLyricLine(intValue(xml.childText(line, "Offset"), 0), text));
            }
        }
        if (!lines.isEmpty()) {
            song.setLyricsTrack(song.getTracks().size());
            song.getLyrics().addAll(lines);
        }
    }


    /**
     * Reads a master bar with the bars of all tracks.
     *
     * @param masterBar the MasterBar element
     * @param song the song
     */
    private void readMasterBar(Element masterBar, GpSong song) {
        GpMasterBar result = new GpMasterBar();
        Element key = xml.child(masterBar, "Key");
        result.setKeyFifths(intValue(xml.childText(key, "AccidentalCount"), 0));
        result.setMinor("Minor".equals(xml.childText(key, "Mode")));
        String time = xml.childText(masterBar, "Time");
        if (time != null && time.contains("/")) {
            result.setBeats(intValue(time.substring(0, time.indexOf('/')), 4));
            result.setBeatType(intValue(time.substring(time.indexOf('/') + 1), 4));
        }
        Element repeat = xml.child(masterBar, "Repeat");
        if (repeat != null) {
            result.setRepeatStart("true".equals(xml.attr(repeat, "start")));
            if ("true".equals(xml.attr(repeat, "end"))) {
                result.setRepeatCount(Math.max(1, intValue(xml.attr(repeat, "count"), 1)));
            }
        }
        String endings = xml.childText(masterBar, "AlternateEndings");
        if (endings != null) {
            result.setAlternateEndings(String.join(", ", endings.trim().split(WHITESPACE)));
        }
        Element section = xml.child(masterBar, "Section");
        if (section != null) {
            String letter = xml.childText(section, "Letter");
            String text = xml.childText(section, "Text");
            String marker = firstNonNull(text, letter);
            if (letter != null && text != null) {
                marker = letter + " " + text;
            }
            result.setMarker(marker);
        }
        result.setDoubleBar(xml.child(masterBar, "DoubleBar") != null);
        result.setAnacrusis(xml.child(masterBar, "Anacrusis") != null);
        Element directions = xml.child(masterBar, "Directions");
        for (Element direction : xml.elements(directions)) {
            String text = xml.text(direction);
            if (!text.isEmpty()) {
                result.getDirections().add(text);
            }
        }
        for (Element fermata : xml.children(xml.child(masterBar, "Fermatas"), "Fermata")) {
            Fraction offset = Fraction.parse(xml.childText(fermata, "Offset"));
            if (offset != null) {
                result.getFermatas().add(offset.multiply(1, QUARTERS_PER_WHOLE));
            }
        }
        song.getMasterBars().add(result);

        String barIds = xml.childText(masterBar, "Bars");
        String[] ids = new String[0];
        if (barIds != null) {
            ids = barIds.trim().split(WHITESPACE);
        }
        int index = 0;
        for (int t = 0; t < song.getTracks().size(); t++) {
            GpTrack track = song.getTracks().get(t);
            for (int s = 0; s < track.getStaffCount(); s++) {
                String id = null;
                if (index < ids.length) {
                    id = ids[index];
                }
                index++;
                track.getBars().add(readBar(bars.get(id), track, t));
            }
        }
    }


    /**
     * Reads a bar: clef and voices.
     *
     * @param bar the Bar element (may be null)
     * @param track the track
     * @param trackIndex the track index
     * @return the bar
     */
    private GpBar readBar(Element bar, GpTrack track, int trackIndex) {
        GpBar result = new GpBar();
        if (bar == null) {
            return result;
        }
        result.setClef(xml.childText(bar, "Clef"));
        String voiceIds = xml.childText(bar, "Voices");
        if (voiceIds == null) {
            return result;
        }
        for (String voiceId : voiceIds.trim().split(WHITESPACE)) {
            List<GpBeat> beatList = new ArrayList<>();
            Element voice = voices.get(voiceId);
            String beatIds = xml.childText(voice, "Beats");
            if (beatIds != null) {
                for (String beatId : beatIds.trim().split(WHITESPACE)) {
                    Element beat = beats.get(beatId);
                    if (beat != null) {
                        beatList.add(readBeat(beat, track, trackIndex));
                    }
                }
            }
            result.getVoices().add(beatList);
        }
        return result;
    }


    /**
     * Reads a beat: rhythm, notes and beat effects.
     *
     * @param beat the Beat element
     * @param track the track
     * @param trackIndex the track index
     * @return the beat
     */
    private GpBeat readBeat(Element beat, GpTrack track, int trackIndex) {
        GpBeat result = new GpBeat();
        Element rhythm = rhythms.get(xml.attr(xml.child(beat, "Rhythm"), "ref"));
        applyRhythm(rhythm, result);
        result.setGrace(xml.childText(beat, "GraceNotes") != null);
        result.setDynamic(converter.dynamicOf(xml.childText(beat, "Dynamic")));
        result.setText(xml.childText(beat, "FreeText"));
        String chord = xml.childText(beat, "Chord");
        if (chord != null && trackIndex < chordNames.size()) {
            result.setChordName(chordNames.get(trackIndex).get(chord));
        }
        for (Element line : xml.children(xml.child(beat, "Lyrics"), "Line")) {
            result.getLyrics().add(xml.text(line));
        }
        for (Element property : xml.children(xml.child(beat, "Properties"), "Property")) {
            String name = xml.attr(property, NAME);
            if ("PickStroke".equals(name)) {
                String direction = xml.childText(property, "Direction");
                if ("Up".equals(direction)) {
                    result.setPickStroke("up");
                } else if ("Down".equals(direction)) {
                    result.setPickStroke("down");
                }
            } else if ("Slapped".equals(name)) {
                result.setSlap(true);
            } else if ("Popped".equals(name)) {
                result.setPop(true);
            }
        }
        String noteIds = xml.childText(beat, "Notes");
        if (noteIds != null) {
            for (String noteId : noteIds.trim().split(WHITESPACE)) {
                Element note = notes.get(noteId);
                if (note != null) {
                    result.getNotes().add(readNote(note, track));
                }
            }
        }
        return result;
    }


    /**
     * Applies a rhythm (note value, dots and tuplet) to a beat.
     *
     * @param rhythm the Rhythm element (may be null for a quarter note)
     * @param beat the beat
     */
    private void applyRhythm(Element rhythm, GpBeat beat) {
        String noteValue = xml.childText(rhythm, "NoteValue");
        Fraction base = converter.noteValueOf(noteValue);
        if (base == null) {
            base = Fraction.of(1, 4);
        }
        String type = NoteValueConverter.getInstance().fromFraction(base).getType();
        int dots = intValue(xml.attr(xml.child(rhythm, "AugmentationDot"), "count"), 0);
        int actual = 0;
        int normal = 0;
        Element tuplet = xml.child(rhythm, "PrimaryTuplet");
        if (tuplet != null) {
            actual = intValue(xml.attr(tuplet, "num"), 0);
            normal = intValue(xml.attr(tuplet, "den"), 0);
        }
        Element secondary = xml.child(rhythm, "SecondaryTuplet");
        if (secondary != null && actual > 0) {
            actual *= intValue(xml.attr(secondary, "num"), 1);
            normal *= intValue(xml.attr(secondary, "den"), 1);
        }
        converter.applyDuration(beat, base, type, dots, actual, normal);
    }


    /**
     * Reads a note: string, fret, pitch, tie and note effects.
     *
     * @param note the Note element
     * @param track the track
     * @return the note
     */
    private GpNote readNote(Element note, GpTrack track) {
        GpNote result = new GpNote();
        Element tie = xml.child(note, "Tie");
        result.setTieOrigin("true".equals(xml.attr(tie, "origin")));
        result.setTieDestination("true".equals(xml.attr(tie, "destination")));
        result.setVibrato(xml.child(note, "Vibrato") != null);
        result.setLetRing(xml.child(note, "LetRing") != null);
        result.setTrill(xml.child(note, "Trill") != null);
        String articulation = xml.childText(note, "InstrumentArticulation");
        if (articulation != null && track.isPercussion()) {
            result.setDrumArticulation(intValue(articulation, 0));
        }
        int accent = intValue(xml.childText(note, "Accent"), 0);
        result.setStaccato((accent & 0x01) != 0);
        if ((accent & 0x04) != 0) {
            result.setAccent(2);
        } else if ((accent & 0x08) != 0) {
            result.setAccent(1);
        } else if ((accent & 0x10) != 0) {
            result.setAccent(3);
        }
        String finger = xml.childText(note, "LeftFingering");
        if (finger != null) {
            int index = "PIMAC".indexOf(finger.charAt(0));
            if (index >= 0) {
                result.setLeftFinger(index);
            }
        }

        Integer string = null;
        Integer tone = null;
        Integer octave = null;
        for (Element property : xml.children(xml.child(note, "Properties"), "Property")) {
            String name = xml.attr(property, NAME);
            if (name == null) {
                continue;
            }
            switch (name) {
                case "String":
                    string = intOrNull(xml.childText(property, "String"));
                    break;
                case "Fret":
                    result.setFret(intValue(xml.childText(property, "Fret"), -1));
                    break;
                case "Midi":
                    result.setMidi(intOrNull(xml.childText(property, "Number")));
                    break;
                case "Tone":
                    tone = intOrNull(xml.childText(property, "Step"));
                    break;
                case "Octave":
                    octave = intOrNull(xml.childText(property, "Number"));
                    break;
                case "TransposedPitch":
                    result.setWrittenPitch(pitchOf(xml.child(property, "Pitch")));
                    break;
                case "ConcertPitch":
                    result.setConcertPitch(pitchOf(xml.child(property, "Pitch")));
                    break;
                case "Muted":
                    result.setDead(true);
                    break;
                case "PalmMuted":
                    result.setPalmMute(true);
                    break;
                case "HopoOrigin":
                    result.setHammerOrigin(true);
                    break;
                case "Slide":
                    int flags = intValue(xml.childText(property, "Flags"), 0);
                    result.setSlideOrigin((flags & 0x03) != 0);
                    break;
                case "HarmonicType":
                case "HarmonicFret":
                    result.setHarmonic(true);
                    break;
                case "Bended":
                    result.setBend(true);
                    break;
                default:
                    break;
            }
        }
        if (string != null) {
            int stringCount = track.getTuning().size();
            result.setString(Math.max(0, stringCount - 1 - string));
            if (result.getMidi() == null && result.getFret() >= 0 && string < stringCount) {
                result.setMidi(track.getTuning().get(stringCount - 1 - string) + result.getFret());
            }
        }
        if (result.getMidi() == null && tone != null && octave != null) {
            result.setMidi(octave * SEMITONES_PER_OCTAVE + tone);
        }
        if (track.isPercussion()) {
            result.setWrittenPitch(null);
            result.setConcertPitch(null);
        }
        return result;
    }


    /**
     * Creates the written pitch of a GPIF pitch element (Guitar Pro octave 4 is the octave below middle C).
     *
     * @param pitch the Pitch element (may be null)
     * @return the pitch, or null
     */
    private SpelledPitch pitchOf(Element pitch) {
        String step = xml.childText(pitch, "Step");
        String octave = xml.childText(pitch, "Octave");
        if (step == null || octave == null) {
            return null;
        }
        String accidental = xml.childText(pitch, "Accidental");
        int alter = 0;
        if (accidental != null) {
            switch (accidental) {
                case "#":
                    alter = 1;
                    break;
                case "b":
                    alter = -1;
                    break;
                case "x":
                case "##":
                    alter = 2;
                    break;
                case "bb":
                    alter = -2;
                    break;
                default:
                    alter = 0;
                    break;
            }
        }
        return new SpelledPitch(step.toUpperCase(), alter, intValue(octave, 5) - 1);
    }


    /**
     * Reads the tempo automations of the master track.
     *
     * @param masterTrack the MasterTrack element (may be null)
     * @param song the song
     */
    private void readAutomations(Element masterTrack, GpSong song) {
        for (Element automation : xml.children(xml.child(masterTrack, "Automations"), "Automation")) {
            if (!"Tempo".equals(xml.childText(automation, "Type"))) {
                continue;
            }
            String value = xml.childText(automation, "Value");
            if (value == null) {
                continue;
            }
            String[] parts = value.trim().split(WHITESPACE);
            double bpm = doubleValue(parts[0]);
            if (parts.length > 1) {
                bpm = toQuarterBpm(bpm, intValue(parts[1], 2));
            }
            int bar = intValue(xml.childText(automation, "Bar"), 0);
            String text = xml.childText(automation, "Text");
            if (bar < 0 || bar >= song.getMasterBars().size() || bpm <= 0) {
                continue;
            }
            GpMasterBar masterBar = song.getMasterBars().get(bar);
            double ratio = doubleValue(xml.childText(automation, "Position"));
            Fraction position = Fraction.of(Math.round(ratio * masterBar.getBeats() * 1000), 1000L * masterBar.getBeatType());
            if (bar == 0 && position.isZero()) {
                song.setTempo((int) Math.round(bpm));
                song.setTempoText(text);
            } else {
                masterBar.getTempoChanges().add(new GpTempo(position, bpm, text));
            }
        }
    }


    /**
     * Converts a tempo of a reference note value into quarter notes per minute.
     *
     * @param bpm the tempo
     * @param reference the reference (1 eighth, 2 quarter, 3 dotted quarter, 4 half, 5 dotted half)
     * @return the tempo in quarter notes per minute
     */
    private double toQuarterBpm(double bpm, int reference) {
        switch (reference) {
            case 1:
                return bpm / 2;
            case 3:
                return bpm * 3 / 2;
            case 4:
                return bpm * 2;
            case 5:
                return bpm * 3;
            default:
                return bpm;
        }
    }


    /**
     * Indexes the child elements of a list by their id attribute.
     *
     * @param list the list element (may be null)
     * @param name the child element name
     * @param index the index (filled)
     */
    private void index(Element list, String name, Map<String, Element> index) {
        for (Element element : xml.children(list, name)) {
            String id = xml.attr(element, "id");
            if (id != null) {
                index.put(id, element);
            }
        }
    }


    /**
     * Parses an integer.
     *
     * @param text the text (may be null)
     * @param defaultValue the default for invalid text
     * @return the value
     */
    private int intValue(String text, int defaultValue) {
        Integer value = intOrNull(text);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }


    /**
     * Parses an integer.
     *
     * @param text the text (may be null)
     * @return the value, or null if invalid
     */
    private Integer intOrNull(String text) {
        if (text == null) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Parses a decimal number.
     *
     * @param text the text (may be null)
     * @return the value, or 0 if invalid
     */
    private double doubleValue(String text) {
        if (text == null) {
            return 0;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    /**
     * Returns the first non-null value.
     *
     * @param first the first value
     * @param second the second value
     * @return the first non-null value, or null
     */
    private String firstNonNull(String first, String second) {
        if (first != null) {
            return first;
        }
        return second;
    }
}
