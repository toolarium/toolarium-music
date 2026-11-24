/*
 * GuitarProScoreAssembler.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl;

import com.github.toolarium.music.io.converter.ChordSymbolParser;
import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.converter.PitchSpeller;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.MeasureData;
import com.github.toolarium.music.io.dto.NoteData;
import com.github.toolarium.music.io.dto.PartData;
import com.github.toolarium.music.io.dto.ScoreData;
import com.github.toolarium.music.io.dto.SpelledPitch;
import com.github.toolarium.music.io.dto.VoiceData;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpBar;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpBeat;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpLyricLine;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpMasterBar;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpNote;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpSong;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpTempo;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpTrack;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Converts a Guitar Pro song (read from GP3 to GP8 files) into the format independent {@link ScoreData}.
 * Each track becomes a part with a standard notation staff; string and fret are kept as technical
 * notations. Ties, hammer-ons, pull-offs and slides connect a note with the next note on the same string.
 *
 * @author patrick
 */
final class GuitarProScoreAssembler {
    private static final String EMPTY = "";
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int STEPS_PER_OCTAVE = 7;

    private final ScoreBuilder builder = ScoreBuilder.getInstance();
    private final GuitarProConverter converter = GuitarProConverter.getInstance();
    private final PitchSpeller speller = PitchSpeller.getInstance();


    /**
     * Constructor for GuitarProScoreAssembler.
     */
    GuitarProScoreAssembler() {
        // default constructor
    }


    /**
     * Converts a song.
     *
     * @param song the song
     * @return the score data
     */
    ScoreData assemble(GpSong song) {
        ScoreData score = new ScoreData();
        score.setTitle(song.getTitle());
        score.setSubtitle(song.getSubtitle());
        String composer = song.getMusic();
        if (composer == null) {
            composer = song.getArtist();
        }
        score.addCreator("composer", composer);
        score.addCreator("lyricist", song.getWords());
        score.addCreator("transcriber", song.getTab());
        score.setRights(song.getCopyright());
        score.setSoftware("Guitar Pro " + song.getVersion());

        for (int t = 0; t < song.getTracks().size(); t++) {
            score.getParts().add(buildPart(song, song.getTracks().get(t), t));
        }
        return score;
    }


    /**
     * Builds the part of a track.
     *
     * @param song the song
     * @param track the track
     * @param trackIndex the track index
     * @return the part
     */
    private PartData buildPart(GpSong song, GpTrack track, int trackIndex) {
        String name = track.getName();
        if (name == null) {
            name = track.getShortName();
        }
        PartData part = new PartData(name);
        part.setAbbreviation(track.getShortName());
        part.setMidiProgram(track.getProgram());
        part.setMidiChannel(track.getChannel());
        part.setVolume(track.getVolume());
        part.setPan(track.getPan());
        part.setPercussion(track.isPercussion());
        if (track.getTranspose() != 0) {
            part.setTranspose(track.getTranspose(), diatonicOf(track.getTranspose()));
        }
        if (track.isPercussion()) {
            part.getDefaultClefs().put(1, builder.createClef("percussion", null, 0));
        }

        int staffCount = Math.max(1, track.getStaffCount());
        part.setStaves(staffCount);
        Map<String, NoteData> lastNotes = new HashMap<>();
        Map<String, Integer> lastFrets = new HashMap<>();
        Map<String, GpNote> origins = new HashMap<>();
        String[] dynamic = new String[1];
        String[] currentClefs = new String[staffCount];
        List<EventData> lyricTargets = new ArrayList<>();
        List<Integer> lyricMeasures = new ArrayList<>();

        for (int m = 0; m < song.getMasterBars().size(); m++) {
            GpMasterBar masterBar = song.getMasterBars().get(m);
            MeasureData measure = part.measure(m);
            applyMasterBar(measure, masterBar, m, trackIndex == 0, song);
            for (int s = 0; s < staffCount; s++) {
                int barIndex = m * staffCount + s;
                if (barIndex >= track.getBars().size()) {
                    continue;
                }
                GpBar bar = track.getBars().get(barIndex);
                if (bar.getClef() != null && !track.isPercussion() && !bar.getClef().equals(currentClefs[s])) {
                    ClefImpl clef = converter.clefOf(bar.getClef(), 0);
                    if (clef != null) {
                        measure.addClef(s + 1, Fraction.ZERO, clef);
                    }
                    currentClefs[s] = bar.getClef();
                }
                for (int v = 0; v < bar.getVoices().size(); v++) {
                    List<GpBeat> beats = bar.getVoices().get(v);
                    if (beats.isEmpty() || allEmpty(beats)) {
                        continue;
                    }
                    VoiceData voice = measure.voice(s + 1, s * 4 + v + 1);
                    Fraction tupletRemaining = null;
                    Fraction position = Fraction.ZERO;
                    for (GpBeat beat : beats) {
                        EventData event = buildEvent(beat, track, masterBar, s * 4 + v, lastNotes, lastFrets, origins, dynamic);
                        tupletRemaining = applyTupletBracket(event, beat, tupletRemaining);
                        if (!beat.isGrace() && s == 0 && v == 0 && masterBar.getFermatas().contains(position)) {
                            event.getFermatas().add(builder.createFermata(null, false));
                        }
                        voice.getEvents().add(event);
                        if (s == 0 && v == 0 && !event.isRest() && !event.isGrace() && !isTieDestination(beat)) {
                            lyricTargets.add(event);
                            lyricMeasures.add(m);
                        }
                        if (!beat.isGrace() && beat.getDuration() != null) {
                            position = position.add(beat.getDuration());
                        }
                    }
                }
            }
        }
        if (trackIndex == song.getLyricsTrack()) {
            applyLyrics(song.getLyrics(), lyricTargets, lyricMeasures);
        }
        return part;
    }


    /**
     * Applies time, key, repeats, endings, markers, jumps and tempo changes of a master bar to a measure.
     *
     * @param measure the measure
     * @param masterBar the master bar
     * @param index the measure index
     * @param firstTrack true for the first track (markers and tempo are added once)
     * @param song the song
     */
    private void applyMasterBar(MeasureData measure, GpMasterBar masterBar, int index, boolean firstTrack, GpSong song) {
        if (masterBar.getBeats() > 0 && masterBar.getBeatType() > 0) {
            measure.setTime(masterBar.getBeats(), masterBar.getBeatType(), null);
            if (!masterBar.isAnacrusis()) {
                measure.setLength(Fraction.of(masterBar.getBeats(), masterBar.getBeatType()));
            }
        }
        String mode = "major";
        if (masterBar.isMinor()) {
            mode = "minor";
        }
        measure.setKey(masterBar.getKeyFifths(), mode);
        measure.setRepeatStart(masterBar.isRepeatStart());
        if (masterBar.getRepeatCount() > 0) {
            measure.setRepeatEnd(true, masterBar.getRepeatCount());
        }
        measure.setEndingNumber(masterBar.getAlternateEndings());
        if (masterBar.isDoubleBar() && masterBar.getRepeatCount() == 0) {
            measure.setRightBarStyle("light-light");
        }
        if (masterBar.isAnacrusis() && index == 0) {
            measure.setImplicit(true);
        }
        if (!firstTrack) {
            return;
        }
        if (index == 0 && song.getTempo() > 0 && !hasTempoAtStart(masterBar)) {
            measure.addDirection(builder.createTempo(song.getTempo(), "quarter", 0, song.getTempoText()), Fraction.ZERO, 1);
        }
        if (masterBar.getMarker() != null) {
            measure.addDirection(builder.createRehearsal(masterBar.getMarker()), Fraction.ZERO, 1);
        }
        for (GpTempo tempo : masterBar.getTempoChanges()) {
            measure.addDirection(builder.createTempo(tempo.getBpm(), "quarter", 0, tempo.getText()), tempo.getPosition(), 1);
        }
        for (String direction : masterBar.getDirections()) {
            DirectionImpl jump = jump(direction);
            if (jump != null) {
                measure.addDirection(jump, Fraction.ZERO, 1);
            }
        }
    }


    /**
     * Checks if a master bar has a tempo change at its start.
     *
     * @param masterBar the master bar
     * @return true if a tempo change starts the bar
     */
    private boolean hasTempoAtStart(GpMasterBar masterBar) {
        for (GpTempo tempo : masterBar.getTempoChanges()) {
            if (tempo.getPosition().isZero()) {
                return true;
            }
        }
        return false;
    }


    /**
     * Creates the direction of a jump or target (Segno, Coda, Fine, DaCapo, DalSegno and so on).
     *
     * @param name the Guitar Pro name
     * @return the direction, or null
     */
    private DirectionImpl jump(String name) {
        switch (name) {
            case "Segno":
            case "SegnoSegno":
                return builder.createSegno();
            case "Coda":
            case "DoubleCoda":
                return builder.createCoda();
            default:
                break;
        }
        SoundImpl sound = new SoundImpl();
        String text;
        if (name.startsWith("DaCapo")) {
            text = "D.C.";
            sound.setDacapo("yes");
        } else if (name.startsWith("DalSegno")) {
            text = "D.S.";
            sound.setDalsegno("segno");
        } else if ("Fine".equals(name)) {
            text = "Fine";
            sound.setFine("yes");
        } else if (name.startsWith("DaCoda") || name.startsWith("DaDoubleCoda")) {
            text = "To Coda";
            sound.setTocoda("coda");
        } else {
            return null;
        }
        if (name.endsWith("AlFine")) {
            text += " al Fine";
        } else if (name.endsWith("AlCoda") || name.endsWith("AlDoubleCoda")) {
            text += " al Coda";
        }
        DirectionImpl direction = builder.createWords(text, ScoreBuilder.ABOVE, "italic", null);
        direction.setSound(sound);
        return direction;
    }


    /**
     * Builds the event of a beat.
     *
     * @param beat the beat
     * @param track the track
     * @param masterBar the master bar
     * @param voiceIndex the voice index
     * @param lastNotes the last note of each string and voice (updated)
     * @param lastFrets the fret of the last note of each string and voice (updated)
     * @param origins the open hammer-on and slide origins of each string and voice (updated)
     * @param dynamic the current dynamics marking (updated)
     * @return the event
     */
    private EventData buildEvent(GpBeat beat, GpTrack track, GpMasterBar masterBar, int voiceIndex, Map<String, NoteData> lastNotes,
                                 Map<String, Integer> lastFrets, Map<String, GpNote> origins, String[] dynamic) {
        EventData event = new EventData();
        event.setDuration(beat.getDuration());
        event.setType(beat.getType());
        event.setDots(beat.getDots());
        if (beat.getTupletActual() > 1) {
            event.setTuplet(beat.getTupletActual(), beat.getTupletNormal());
        }
        event.setGrace(beat.isGrace());
        event.setGraceSlash(beat.isGrace());
        event.setInvisible(beat.isEmpty() && beat.getNotes().isEmpty());

        for (GpNote gpNote : beat.getNotes()) {
            NoteData note = buildNote(gpNote, track, masterBar, voiceIndex, lastNotes, lastFrets, origins);
            if (note == null) {
                continue;
            }
            event.getNotes().add(note);
            applyNoteArticulations(gpNote, event);
        }

        if (beat.getText() != null) {
            event.getDirections().add(builder.createWords(beat.getText(), ScoreBuilder.ABOVE, null, null));
        }
        if (beat.getChordName() != null) {
            HarmonyImpl harmony = ChordSymbolParser.getInstance().parse(beat.getChordName());
            if (harmony != null) {
                event.getHarmonies().add(harmony);
            } else {
                event.getDirections().add(builder.createWords(beat.getChordName(), ScoreBuilder.ABOVE, null, "bold"));
            }
        }
        if (beat.getDynamic() != null && !event.isRest() && !beat.getDynamic().equals(dynamic[0])) {
            event.getDirections().add(builder.createDynamics(beat.getDynamic()));
            dynamic[0] = beat.getDynamic();
        }
        if (beat.getTempo() != null) {
            event.getDirections().add(builder.createTempo(beat.getTempo().getBpm(), "quarter", 0, beat.getTempo().getText()));
        }
        if (beat.isFermata()) {
            event.getFermatas().add(builder.createFermata(null, false));
        }
        if ("up".equals(beat.getPickStroke())) {
            event.technical().setUpBow(EMPTY);
        } else if ("down".equals(beat.getPickStroke())) {
            event.technical().setDownBow(EMPTY);
        }
        for (int line = 0; line < beat.getLyrics().size(); line++) {
            String text = beat.getLyrics().get(line);
            if (text != null && !text.trim().isEmpty() && !event.isRest()) {
                event.getLyrics().add(builder.createLyric(line + 1, text.trim(), null, false));
            }
        }
        return event;
    }


    /**
     * Builds a note: pitch (or drum position), tie, string and fret, hammer-on, pull-off and slide.
     *
     * @param gpNote the Guitar Pro note
     * @param track the track
     * @param masterBar the master bar (key for the spelling)
     * @param voiceIndex the voice index
     * @param lastNotes the last note of each string and voice (updated)
     * @param lastFrets the fret of the last note of each string and voice (updated)
     * @param origins the open hammer-on and slide origins of each string and voice (updated)
     * @return the note, or null if the pitch is unknown
     */
    private NoteData buildNote(GpNote gpNote, GpTrack track, GpMasterBar masterBar, int voiceIndex, Map<String, NoteData> lastNotes,
                               Map<String, Integer> lastFrets, Map<String, GpNote> origins) {
        String key = voiceIndex + "/" + gpNote.getString();
        if (gpNote.getString() < 0 && gpNote.getMidi() != null) {
            key = voiceIndex + "/m" + gpNote.getMidi();
        }
        NoteData previous = lastNotes.get(key);
        Integer midi = gpNote.getMidi();
        if (gpNote.isTieDestination() && previous != null && (midi == null || gpNote.getFret() < 0)) {
            midi = speller.toMidi(previous.getStep(), previous.getAlter(), previous.getOctave()) + track.getTranspose();
            if (lastFrets.get(key) != null) {
                gpNote.setFret(lastFrets.get(key));
            }
        }

        NoteData note;
        if (track.isPercussion()) {
            int drum = drumMidi(gpNote, track);
            SpelledPitch display = converter.drumDisplay(drum);
            note = new NoteData(display.getStep(), 0, display.getOctave());
            note.setUnpitched(true);
            note.setNotehead(converter.drumNotehead(drum));
        } else {
            SpelledPitch written = writtenPitch(gpNote, midi, track.getTranspose(), masterBar.getKeyFifths());
            if (written == null) {
                return null;
            }
            note = new NoteData(written.getStep(), written.getAlter(), written.getOctave());
        }

        if (gpNote.isTieDestination() && previous != null) {
            note.setTieStop(true);
            previous.setTieStart(true);
            if (!track.isPercussion()) {
                note.setStep(previous.getStep());
                note.setAlter(previous.getAlter());
                note.setOctave(previous.getOctave());
            }
        }
        if (gpNote.isTieOrigin()) {
            note.setTieStart(true);
        }
        if (gpNote.isDead()) {
            note.setNotehead("x");
        }
        if (!track.isPercussion() && gpNote.getString() >= 0 && gpNote.getFret() >= 0) {
            note.technical().setString(String.valueOf(gpNote.getString() + 1));
            note.technical().setFret(String.valueOf(gpNote.getFret()));
        }
        if (gpNote.isHarmonic()) {
            note.technical().setHarmonic(EMPTY);
        }
        if (gpNote.getLeftFinger() != null && gpNote.getLeftFinger() >= 0) {
            note.technical().setFingering(fingering(gpNote.getLeftFinger()));
        }

        GpNote origin = origins.remove(key);
        NoteData originNote = previous;
        if (origin != null && originNote != null && !gpNote.isTieDestination()) {
            if (origin.isHammerOrigin()) {
                boolean hammer = gpNote.getFret() >= origin.getFret();
                if (hammer) {
                    originNote.technical().setHammerOn(ScoreBuilder.START);
                    note.technical().setHammerOn(ScoreBuilder.STOP);
                } else {
                    originNote.technical().setPullOff(ScoreBuilder.START);
                    note.technical().setPullOff(ScoreBuilder.STOP);
                }
            }
            if (origin.isSlideOrigin()) {
                originNote.setGlissandoStart(true);
                note.setGlissandoStop(true);
            }
        }
        if (gpNote.isHammerOrigin() || gpNote.isSlideOrigin()) {
            origins.put(key, gpNote);
        }
        lastNotes.put(key, note);
        lastFrets.put(key, gpNote.getFret());
        return note;
    }


    /**
     * Gets the written pitch of a note. The sounding pitch comes from the MIDI pitch (or the concert
     * pitch); the spelling of the file is used if it matches the sounding pitch and the transposition
     * of the track (the transposed pitch of a file can include a capo).
     *
     * @param gpNote the note
     * @param midi the sounding MIDI pitch (may be null)
     * @param transpose the transposition of the track (sounding minus written)
     * @param fifths the key signature
     * @return the written pitch, or null if the pitch is unknown
     */
    private SpelledPitch writtenPitch(GpNote gpNote, Integer midi, int transpose, int fifths) {
        SpelledPitch written = gpNote.getWrittenPitch();
        SpelledPitch concert = gpNote.getConcertPitch();
        Integer sounding = midi;
        if (sounding == null && concert != null) {
            sounding = midiOf(concert);
        }
        if (sounding == null) {
            return written;
        }
        if (written != null && midiOf(written) == sounding - transpose) {
            return written;
        }
        if (concert != null && midiOf(concert) == sounding && transpose % SEMITONES_PER_OCTAVE == 0) {
            return new SpelledPitch(concert.getStep(), concert.getAlter(), concert.getOctave() - transpose / SEMITONES_PER_OCTAVE);
        }
        return speller.spell(sounding - transpose, fifths);
    }


    /**
     * Calculates the MIDI pitch of a spelled pitch.
     *
     * @param pitch the pitch
     * @return the MIDI pitch
     */
    private int midiOf(SpelledPitch pitch) {
        return speller.toMidi(pitch.getStep(), pitch.getAlter(), pitch.getOctave());
    }


    /**
     * Gets the MIDI note of a drum note: from the drum articulation of the track or the MIDI pitch.
     *
     * @param gpNote the note
     * @param track the track
     * @return the drum MIDI note
     */
    private int drumMidi(GpNote gpNote, GpTrack track) {
        if (gpNote.getDrumArticulation() != null) {
            Integer midi = track.getDrumMidi().get(gpNote.getDrumArticulation());
            if (midi != null) {
                return midi;
            }
        }
        if (gpNote.getMidi() != null) {
            return gpNote.getMidi();
        }
        return Math.max(0, gpNote.getFret());
    }


    /**
     * Applies the articulations of a note to its event (accent, staccato, trill).
     *
     * @param gpNote the note
     * @param event the event
     */
    private void applyNoteArticulations(GpNote gpNote, EventData event) {
        switch (gpNote.getAccent()) {
            case 1:
                event.articulations().setAccent(EMPTY);
                break;
            case 2:
                event.articulations().setStrongAccent(EMPTY);
                break;
            case 3:
                event.articulations().setTenuto(EMPTY);
                break;
            default:
                break;
        }
        if (gpNote.isStaccato()) {
            event.articulations().setStaccato(EMPTY);
        }
        if (gpNote.isTrill()) {
            event.ornaments().setTrillMark(EMPTY);
        }
    }


    /**
     * Sets the tuplet bracket start and stop of an event. A tuplet group lasts its first note value
     * times the normal notes.
     *
     * @param event the event
     * @param beat the beat
     * @param remaining the remaining length of the open tuplet group, or null
     * @return the remaining length after the event, or null if no group is open
     */
    private Fraction applyTupletBracket(EventData event, GpBeat beat, Fraction remaining) {
        if (beat.isGrace() || beat.getTupletActual() <= 1) {
            return null;
        }
        Fraction open = remaining;
        if (open == null || !open.isPositive()) {
            Fraction unit = NoteValueConverter.getInstance().toFraction(beat.getType());
            if (unit == null) {
                return null;
            }
            open = unit.multiply(beat.getTupletNormal(), 1);
            event.setTupletStart(true);
        }
        open = open.subtract(beat.getDuration());
        if (!open.isPositive()) {
            event.setTupletStop(true);
            return null;
        }
        return open;
    }


    /**
     * Distributes the lyrics of a track over its notes: each line starts at its measure, syllables
     * are separated by spaces and hyphens.
     *
     * @param lines the lyrics lines
     * @param targets the note events of the first voice in order
     * @param measures the measure index of each target
     */
    private void applyLyrics(List<GpLyricLine> lines, List<EventData> targets, List<Integer> measures) {
        for (int l = 0; l < lines.size(); l++) {
            GpLyricLine line = lines.get(l);
            int target = 0;
            while (target < targets.size() && measures.get(target) < line.getStartMeasure()) {
                target++;
            }
            boolean hyphenBefore = false;
            for (String word : line.getText().replace("\r", " ").replace("\n", " ").split("\\s+")) {
                if (word.isEmpty()) {
                    continue;
                }
                String[] syllables = word.split("-", -1);
                for (int s = 0; s < syllables.length; s++) {
                    if (target >= targets.size()) {
                        return;
                    }
                    String text = syllables[s];
                    if (text.isEmpty()) {
                        continue;
                    }
                    boolean hyphenAfter = s < syllables.length - 1;
                    String syllabic = "single";
                    if (hyphenBefore && hyphenAfter) {
                        syllabic = "middle";
                    } else if (hyphenBefore) {
                        syllabic = "end";
                    } else if (hyphenAfter) {
                        syllabic = "begin";
                    }
                    targets.get(target).getLyrics().add(builder.createLyric(l + 1, text, syllabic, false));
                    target++;
                    hyphenBefore = hyphenAfter;
                }
                hyphenBefore = false;
            }
        }
    }


    /**
     * Checks if all notes of a beat are tie destinations.
     *
     * @param beat the beat
     * @return true if the beat only continues tied notes
     */
    private boolean isTieDestination(GpBeat beat) {
        for (GpNote note : beat.getNotes()) {
            if (!note.isTieDestination()) {
                return false;
            }
        }
        return !beat.getNotes().isEmpty();
    }


    /**
     * Checks if all beats of a voice are empty (no notes and invisible).
     *
     * @param beats the beats
     * @return true if the voice has no content
     */
    private boolean allEmpty(List<GpBeat> beats) {
        for (GpBeat beat : beats) {
            if (!beat.isEmpty() || !beat.getNotes().isEmpty()) {
                return false;
            }
        }
        return true;
    }


    /**
     * Gets the fingering text of a left hand finger.
     *
     * @param finger the finger (0 thumb to 4 little finger)
     * @return the fingering
     */
    private String fingering(int finger) {
        if (finger == 0) {
            return "T";
        }
        return String.valueOf(finger);
    }


    /**
     * Calculates the diatonic steps of a chromatic transposition by octaves and the nearest step.
     *
     * @param chromatic the transposition in semitones
     * @return the diatonic steps
     */
    private int diatonicOf(int chromatic) {
        int octaves = Math.floorDiv(chromatic, SEMITONES_PER_OCTAVE);
        int rest = Math.floorMod(chromatic, SEMITONES_PER_OCTAVE);
        int[] steps = {0, 1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 6};
        return octaves * STEPS_PER_OCTAVE + steps[rest];
    }
}
