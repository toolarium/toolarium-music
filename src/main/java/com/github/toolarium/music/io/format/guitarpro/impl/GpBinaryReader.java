/*
 * GpBinaryReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpBar;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpBeat;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpLyricLine;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpMasterBar;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpNote;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpSong;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpTempo;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpTrack;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Reads the binary Guitar Pro formats GP3 (.gp3), GP4 (.gp4) and GP5 (.gp5) into a {@link GpSong}.
 *
 * @author patrick
 */
final class GpBinaryReader {
    private static final Pattern VERSION = Pattern.compile("v(\\d)\\.(\\d)(\\d)?");
    private static final int VERSION_4 = 400;
    private static final int VERSION_406 = 406;
    private static final int VERSION_5 = 500;
    private static final int VERSION_510 = 510;
    private static final int MIDI_CHANNELS = 64;
    private static final int LYRICS_LINES = 5;
    private static final int TUNING_STRINGS = 7;
    private static final int PAGE_STRINGS = 10;
    private static final int MAX_COUNT = 100_000;
    private static final int DRUM_CHANNEL = 10;
    private static final int MAX_MIDI = 127;
    private static final double PERCENT = 100.0;
    private static final double PAN_RANGE = 90.0;
    private static final int PAN_CENTER = 64;
    private static final String[] DYNAMICS = {"ppp", "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff"};

    private final GpBinaryInput in;
    private final GuitarProConverter converter = GuitarProConverter.getInstance();
    private final int[][] channels = new int[MIDI_CHANNELS][];
    private int version;


    /**
     * Constructor for GpBinaryReader.
     *
     * @param data the file content
     */
    GpBinaryReader(byte[] data) {
        this.in = new GpBinaryInput(data);
    }


    /**
     * Reads the song.
     *
     * @return the song
     * @throws IOException if the file is invalid
     */
    GpSong read() throws IOException {
        final GpSong song = new GpSong();
        String versionText = in.readByteSizeString(30);
        Matcher matcher = VERSION.matcher(versionText);
        if (!versionText.contains("GUITAR PRO") || !matcher.find()) {
            throw new IOException("Unsupported Guitar Pro version: " + versionText);
        }
        version = Integer.parseInt(matcher.group(1)) * 100 + Integer.parseInt(matcher.group(2)) * 10;
        if (matcher.group(3) != null) {
            version += Integer.parseInt(matcher.group(3));
        }
        song.setVersion(matcher.group(1) + "." + matcher.group(2) + valueOr(matcher.group(3), "0"));

        readInfo(song);
        if (version < VERSION_5) {
            in.readBool();
        }
        if (version >= VERSION_4) {
            readLyrics(song);
        }
        if (version >= VERSION_510) {
            in.skip(19);
        }
        if (version >= VERSION_5) {
            in.skip(30);
            for (int i = 0; i < PAGE_STRINGS; i++) {
                in.readIntByteSizeString();
            }
            song.setTempoText(emptyToNull(in.readIntByteSizeString()));
        }
        song.setTempo(in.readInt());
        if (version >= VERSION_510) {
            in.readBool();
        }
        final int key = in.readInt();
        if (version >= VERSION_4) {
            in.readSignedByte();
        }
        readMidiChannels();
        if (version >= VERSION_5) {
            in.skip(42);
        }
        int measureCount = count(in.readInt());
        int trackCount = count(in.readInt());
        GpMasterBar previous = new GpMasterBar();
        previous.setKeyFifths((byte) key);
        for (int i = 0; i < measureCount; i++) {
            previous = readMasterBar(i, previous);
            song.getMasterBars().add(previous);
        }
        for (int i = 0; i < trackCount; i++) {
            song.getTracks().add(readTrack(i));
        }
        if (version >= VERSION_5) {
            if (version == VERSION_5) {
                in.skip(2);
            } else {
                in.skip(1);
            }
        }
        for (int m = 0; m < measureCount; m++) {
            for (GpTrack track : song.getTracks()) {
                track.getBars().add(readBar(track, song.getMasterBars().get(m)));
            }
        }
        return song;
    }


    /**
     * Reads the score information (title, artist, and so on).
     *
     * @param song the song
     * @throws IOException if the file is invalid
     */
    private void readInfo(GpSong song) throws IOException {
        song.setTitle(emptyToNull(in.readIntByteSizeString()));
        song.setSubtitle(emptyToNull(in.readIntByteSizeString()));
        song.setArtist(emptyToNull(in.readIntByteSizeString()));
        song.setAlbum(emptyToNull(in.readIntByteSizeString()));
        song.setWords(emptyToNull(in.readIntByteSizeString()));
        if (version >= VERSION_5) {
            song.setMusic(emptyToNull(in.readIntByteSizeString()));
        } else {
            song.setMusic(song.getWords());
        }
        song.setCopyright(emptyToNull(in.readIntByteSizeString()));
        song.setTab(emptyToNull(in.readIntByteSizeString()));
        song.setInstructions(emptyToNull(in.readIntByteSizeString()));
        int notices = count(in.readInt());
        for (int i = 0; i < notices; i++) {
            in.readIntByteSizeString();
        }
    }


    /**
     * Reads the lyrics (GP4 and GP5).
     *
     * @param song the song
     * @throws IOException if the file is invalid
     */
    private void readLyrics(GpSong song) throws IOException {
        song.setLyricsTrack(in.readInt() - 1);
        for (int i = 0; i < LYRICS_LINES; i++) {
            int start = in.readInt() - 1;
            String text = in.readIntSizeString();
            if (!text.trim().isEmpty()) {
                song.getLyrics().add(new GpLyricLine(Math.max(0, start), text));
            }
        }
    }


    /**
     * Reads the 64 MIDI channels (program, volume and balance).
     *
     * @throws IOException if the file is invalid
     */
    private void readMidiChannels() throws IOException {
        for (int i = 0; i < MIDI_CHANNELS; i++) {
            int program = in.readInt();
            int volume = in.readSignedByte();
            int balance = in.readSignedByte();
            in.skip(6);
            channels[i] = new int[] {program, volume, balance};
        }
    }


    /**
     * Reads a master bar (measure header).
     *
     * @param index the measure index
     * @param previous the previous master bar (time and key are carried forward)
     * @return the master bar
     * @throws IOException if the file is invalid
     */
    private GpMasterBar readMasterBar(int index, GpMasterBar previous) throws IOException {
        if (version >= VERSION_5 && index > 0) {
            in.skip(1);
        }
        GpMasterBar bar = new GpMasterBar();
        bar.setKeyFifths(previous.getKeyFifths());
        bar.setMinor(previous.isMinor());
        bar.setBeats(previous.getBeats());
        bar.setBeatType(previous.getBeatType());
        int flags = in.readByte();
        if ((flags & 0x01) != 0) {
            bar.setBeats(in.readSignedByte());
        }
        if ((flags & 0x02) != 0) {
            bar.setBeatType(in.readSignedByte());
        }
        bar.setRepeatStart((flags & 0x04) != 0);
        if ((flags & 0x08) != 0) {
            bar.setRepeatCount(Math.max(1, in.readSignedByte()));
        }
        if (version < VERSION_5 && (flags & 0x10) != 0) {
            bar.setAlternateEndings(String.valueOf(in.readByte()));
        }
        if ((flags & 0x20) != 0) {
            bar.setMarker(emptyToNull(in.readIntByteSizeString()));
            in.skip(4);
        }
        if ((flags & 0x40) != 0) {
            bar.setKeyFifths(in.readSignedByte());
            bar.setMinor(in.readSignedByte() != 0);
        }
        if (version >= VERSION_5 && (flags & 0x10) != 0) {
            bar.setAlternateEndings(endingsOfMask(in.readByte()));
        }
        bar.setDoubleBar((flags & 0x80) != 0);
        if (version >= VERSION_5) {
            if ((flags & 0x03) != 0) {
                in.skip(4);
            }
            if ((flags & 0x10) == 0) {
                in.skip(1);
            }
            in.readByte();
        }
        return bar;
    }


    /**
     * Reads a track.
     *
     * @param index the track index
     * @return the track
     * @throws IOException if the file is invalid
     */
    private GpTrack readTrack(int index) throws IOException {
        if (version >= VERSION_5 && (index == 0 || version == VERSION_5)) {
            in.skip(1);
        }
        GpTrack track = new GpTrack();
        final int flags = in.readByte();
        track.setName(emptyToNull(in.readByteSizeString(40)));
        int stringCount = in.readInt();
        for (int i = 0; i < TUNING_STRINGS; i++) {
            int tuning = in.readInt();
            if (i < stringCount) {
                track.getTuning().add(tuning);
            }
        }
        in.readInt();
        final int channel = in.readInt();
        in.readInt();
        in.readInt();
        track.setCapo(in.readInt());
        in.skip(4);
        track.setPercussion((flags & 0x01) != 0 || channel == DRUM_CHANNEL);
        if (channel >= 1 && channel <= MIDI_CHANNELS) {
            int[] info = channels[channel - 1];
            track.setChannel(((channel - 1) % 16) + 1);
            if (!track.isPercussion() && info[0] >= 0 && info[0] <= MAX_MIDI) {
                track.setProgram(info[0] + 1);
            }
            track.setVolume(Math.round(info[1] * PERCENT / MAX_MIDI * 10) / 10.0);
            track.setPan(Math.round((info[2] - PAN_CENTER) * PAN_RANGE / PAN_CENTER * 10) / 10.0);
        }
        if (!track.isPercussion() && converter.isOctaveTransposing(track.getProgram())) {
            track.setTranspose(-12);
        }
        if (version >= VERSION_5) {
            in.skip(2 + 1 + 1);
            in.skip(1 + 12 + 12);
            in.skip(12);
            if (version == VERSION_5) {
                in.skip(3);
            } else {
                in.skip(4);
                in.skip(4);
                in.readIntByteSizeString();
                in.readIntByteSizeString();
            }
        }
        return track;
    }


    /**
     * Reads the bar of a track in a measure: one voice (GP3, GP4) or two voices (GP5).
     *
     * @param track the track
     * @param masterBar the master bar
     * @return the bar
     * @throws IOException if the file is invalid
     */
    private GpBar readBar(GpTrack track, GpMasterBar masterBar) throws IOException {
        GpBar bar = new GpBar();
        int voices = 1;
        if (version >= VERSION_5) {
            voices = 2;
        }
        for (int v = 0; v < voices; v++) {
            List<GpBeat> beats = new ArrayList<>();
            int beatCount = count(in.readInt());
            Fraction position = Fraction.ZERO;
            for (int b = 0; b < beatCount; b++) {
                GpBeat beat = readBeat(track, beats, masterBar, position);
                beats.add(beat);
                if (!beat.isGrace()) {
                    position = position.add(beat.getDuration());
                }
            }
            bar.getVoices().add(beats);
        }
        if (version >= VERSION_5 && in.hasMore()) {
            in.readByte();
        }
        return bar;
    }


    /**
     * Reads a beat with its notes. Grace notes are added to the beat list before the beat.
     *
     * @param track the track
     * @param beats the beats of the voice (grace beats are added)
     * @param masterBar the master bar (tempo changes are added)
     * @param position the position of the beat in the measure
     * @return the beat
     * @throws IOException if the file is invalid
     */
    private GpBeat readBeat(GpTrack track, List<GpBeat> beats, GpMasterBar masterBar, Fraction position) throws IOException {
        GpBeat beat = new GpBeat();
        int flags = in.readByte();
        if ((flags & 0x40) != 0) {
            int status = in.readByte();
            beat.setEmpty((status & 0x02) == 0);
        }
        int value = in.readSignedByte();
        int dots = 0;
        if ((flags & 0x01) != 0) {
            dots = 1;
        }
        int tuplet = 0;
        if ((flags & 0x20) != 0) {
            tuplet = in.readInt();
        }
        converter.applyBinaryDuration(beat, value, dots, tuplet);
        if ((flags & 0x02) != 0) {
            beat.setChordName(readChord(track));
        }
        if ((flags & 0x04) != 0) {
            beat.setText(emptyToNull(in.readIntUnusedString()));
        }
        if ((flags & 0x08) != 0) {
            readBeatEffects(beat);
        }
        if ((flags & 0x10) != 0) {
            readMixTableChange(beat, masterBar, position);
        }
        List<GpBeat> graces = new ArrayList<>();
        int stringFlags = in.readByte();
        for (int i = 6; i >= 0; i--) {
            if ((stringFlags & (1 << i)) != 0 && 6 - i < track.getTuning().size()) {
                beat.getNotes().add(readNote(track, beat, 6 - i, graces));
            }
        }
        beats.addAll(graces);
        if (version >= VERSION_5) {
            int flags2 = in.readShort();
            if ((flags2 & 0x0800) != 0) {
                in.readByte();
            }
        }
        return beat;
    }


    /**
     * Reads a chord diagram and returns its name.
     *
     * @param track the track
     * @return the chord name, or null
     * @throws IOException if the file is invalid
     */
    private String readChord(GpTrack track) throws IOException {
        String name;
        if (version >= VERSION_5) {
            in.skip(17);
            name = in.readByteSizeString(21);
            in.skip(4);
            in.readInt();
            in.skip(TUNING_STRINGS * 4);
            in.readByte();
            in.skip(5);
            in.skip(26);
            return emptyToNull(name);
        }
        if (in.readByte() != 0) {
            if (version >= VERSION_4) {
                in.skip(16);
                name = in.readByteSizeString(21);
                in.skip(4);
                in.readInt();
                in.skip(TUNING_STRINGS * 4);
                in.readByte();
                in.skip(5);
                in.skip(26);
            } else {
                in.skip(25);
                name = in.readByteSizeString(34);
                in.readInt();
                in.skip(6 * 4);
                in.skip(36);
            }
            return emptyToNull(name);
        }
        int strings = 6;
        if (version >= VERSION_406) {
            strings = TUNING_STRINGS;
        }
        name = in.readIntByteSizeString();
        int firstFret = in.readInt();
        if (firstFret > 0) {
            in.skip(strings * 4);
        }
        return emptyToNull(name);
    }


    /**
     * Reads the beat effects (stroke, slap, tremolo bar, pick stroke).
     *
     * @param beat the beat
     * @throws IOException if the file is invalid
     */
    private void readBeatEffects(GpBeat beat) throws IOException {
        int flags = in.readByte();
        int flags2 = 0;
        if (version >= VERSION_4) {
            flags2 = in.readByte();
        }
        if ((flags & 0x20) != 0) {
            int effect = in.readSignedByte();
            beat.setTap(effect == 1);
            beat.setSlap(effect == 2);
            beat.setPop(effect == 3);
            if (version < VERSION_4) {
                in.skip(4);
            }
        }
        if ((flags2 & 0x04) != 0) {
            skipBend();
        }
        if ((flags & 0x40) != 0) {
            in.readByte();
            in.readByte();
        }
        if ((flags2 & 0x02) != 0) {
            int stroke = in.readSignedByte();
            if (stroke == 1) {
                beat.setPickStroke("up");
            } else if (stroke == 2) {
                beat.setPickStroke("down");
            }
        }
    }


    /**
     * Reads a mix table change; a tempo change is added to the master bar.
     *
     * @param beat the beat
     * @param masterBar the master bar
     * @param position the position of the beat
     * @throws IOException if the file is invalid
     */
    private void readMixTableChange(GpBeat beat, GpMasterBar masterBar, Fraction position) throws IOException {
        in.readSignedByte();
        if (version >= VERSION_5) {
            in.skip(16);
        }
        int[] values = new int[6];
        for (int i = 0; i < values.length; i++) {
            values[i] = in.readSignedByte();
        }
        String tempoName = null;
        if (version >= VERSION_5) {
            tempoName = emptyToNull(in.readIntByteSizeString());
        }
        int tempo = in.readInt();
        for (int value : values) {
            if (value >= 0) {
                in.readByte();
            }
        }
        if (tempo >= 0) {
            in.readSignedByte();
            if (version >= VERSION_510) {
                in.readByte();
            }
            GpTempo change = new GpTempo(position, tempo, tempoName);
            beat.setTempo(change);
        }
        if (version >= VERSION_4) {
            in.readByte();
        }
        if (version >= VERSION_5) {
            in.readByte();
        }
        if (version >= VERSION_510) {
            in.readIntByteSizeString();
            in.readIntByteSizeString();
        }
    }


    /**
     * Reads a note and its effects.
     *
     * @param track the track
     * @param beat the beat
     * @param string the string index (0 for the highest string)
     * @param graces the grace beats before the beat (filled)
     * @return the note
     * @throws IOException if the file is invalid
     */
    private GpNote readNote(GpTrack track, GpBeat beat, int string, List<GpBeat> graces) throws IOException {
        GpNote note = new GpNote();
        note.setString(string);
        int flags = in.readByte();
        if ((flags & 0x02) != 0) {
            note.setAccent(2);
        } else if ((flags & 0x40) != 0) {
            note.setAccent(1);
        }
        note.setGhost((flags & 0x04) != 0);
        if ((flags & 0x20) != 0) {
            int type = in.readByte();
            note.setTieDestination(type == 2);
            note.setDead(type == 3);
        }
        if ((flags & 0x01) != 0 && version < VERSION_5) {
            in.skip(2);
        }
        if ((flags & 0x10) != 0) {
            beat.setDynamic(dynamicOf(in.readSignedByte()));
        }
        if ((flags & 0x20) != 0) {
            note.setFret(in.readSignedByte());
        }
        if ((flags & 0x80) != 0) {
            int left = in.readSignedByte();
            in.readSignedByte();
            if (left >= 0) {
                note.setLeftFinger(left);
            }
        }
        if (version >= VERSION_5) {
            if ((flags & 0x01) != 0) {
                in.skip(8);
            }
            in.readByte();
        }
        if ((flags & 0x08) != 0) {
            readNoteEffects(note, string, graces);
        }
        if (note.getFret() >= 0 && string < track.getTuning().size() && !track.isPercussion()) {
            note.setMidi(track.getTuning().get(string) + note.getFret());
        } else if (track.isPercussion()) {
            note.setMidi(Math.max(0, note.getFret()));
        }
        return note;
    }


    /**
     * Reads the note effects (bend, grace note, slide, harmonic, trill and flags).
     *
     * @param note the note
     * @param string the string index
     * @param graces the grace beats (filled)
     * @throws IOException if the file is invalid
     */
    private void readNoteEffects(GpNote note, int string, List<GpBeat> graces) throws IOException {
        int flags = in.readByte();
        int flags2 = 0;
        if (version >= VERSION_4) {
            flags2 = in.readByte();
        }
        if ((flags & 0x01) != 0) {
            skipBend();
            note.setBend(true);
        }
        if ((flags & 0x10) != 0) {
            graces.add(readGrace(string));
        }
        if ((flags2 & 0x04) != 0) {
            in.readByte();
        }
        if ((flags2 & 0x08) != 0) {
            in.readByte();
            note.setSlideOrigin(true);
        } else if (version < VERSION_4 && (flags & 0x04) != 0) {
            note.setSlideOrigin(true);
        }
        if ((flags2 & 0x10) != 0) {
            readHarmonic();
            note.setHarmonic(true);
        }
        if ((flags2 & 0x20) != 0) {
            in.skip(2);
            note.setTrill(true);
        }
        note.setLetRing((flags & 0x08) != 0);
        note.setHammerOrigin((flags & 0x02) != 0);
        note.setVibrato((flags2 & 0x40) != 0);
        note.setPalmMute((flags2 & 0x02) != 0);
        if ((flags2 & 0x01) != 0) {
            note.setStaccato(true);
        }
    }


    /**
     * Reads a grace note.
     *
     * @param string the string of the main note
     * @return the grace beat
     * @throws IOException if the file is invalid
     */
    private GpBeat readGrace(int string) throws IOException {
        GpBeat grace = new GpBeat();
        grace.setGrace(true);
        grace.setDuration(Fraction.of(1, 32));
        grace.setType("32nd");
        GpNote note = new GpNote();
        note.setString(string);
        note.setFret(in.readSignedByte());
        in.readSignedByte();
        int transition = in.readSignedByte();
        in.skip(1);
        if (version >= VERSION_5) {
            int flags = in.readByte();
            note.setDead((flags & 0x01) != 0);
        }
        note.setSlideOrigin(transition == 1);
        note.setHammerOrigin(transition == 3);
        grace.getNotes().add(note);
        return grace;
    }


    /**
     * Reads a harmonic (GP5 artificial and tapped harmonics have extra bytes).
     *
     * @throws IOException if the file is invalid
     */
    private void readHarmonic() throws IOException {
        int type = in.readByte();
        if (version >= VERSION_5) {
            if (type == 2) {
                in.skip(3);
            } else if (type == 3) {
                in.skip(1);
            }
        }
    }


    /**
     * Skips a bend or tremolo bar definition.
     *
     * @throws IOException if the file is invalid
     */
    private void skipBend() throws IOException {
        in.readByte();
        in.readInt();
        int points = count(in.readInt());
        in.skip(points * 9);
    }


    /**
     * Converts the ending mask of GP5 (bit 0 = ending 1) into ending numbers.
     *
     * @param mask the mask
     * @return the ending numbers, or null
     */
    private String endingsOfMask(int mask) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) != 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(i + 1);
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }


    /**
     * Gets the dynamics marking of a Guitar Pro velocity number (1 ppp to 8 fff).
     *
     * @param value the value
     * @return the marking
     */
    private String dynamicOf(int value) {
        if (value < 1 || value >= DYNAMICS.length) {
            return "f";
        }
        return DYNAMICS[value];
    }


    /**
     * Validates a count.
     *
     * @param value the count
     * @return the count
     * @throws IOException if the count is negative or too large
     */
    private int count(int value) throws IOException {
        if (value < 0 || value > MAX_COUNT) {
            throw new IOException("Invalid Guitar Pro count: " + value);
        }
        return value;
    }


    /**
     * Returns null for empty text.
     *
     * @param text the text
     * @return the trimmed text, or null
     */
    private String emptyToNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }


    /**
     * Returns a value or a default.
     *
     * @param value the value (may be null)
     * @param defaultValue the default
     * @return the value or the default
     */
    private String valueOr(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
