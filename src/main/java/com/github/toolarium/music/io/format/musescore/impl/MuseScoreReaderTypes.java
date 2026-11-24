/*
 * MuseScoreReaderTypes.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Package-private companion types used by {@link MuseScoreReader}.
 *
 * @author patrick
 */
final class MuseScoreReaderTypes {
    private static final int SEMITONES_PER_OCTAVE = 12;

    /**
     * Private constructor.
     */
    private MuseScoreReaderTypes() {
        // utility class
    }


    /**
     * The state of a read operation.
     *
     * @author patrick
     */
    static final class ReadContext {
        private final String version;
        private final int division;
        private final boolean voiceBased;
        private List<MeasureFrame> frames = new ArrayList<>();
        private final TreeMap<Integer, TimeSignature> sigList = new TreeMap<>();
        private final TreeMap<Integer, Double> tempoList = new TreeMap<>();


        /**
         * Constructor for ReadContext.
         *
         * @param version the file format version
         * @param division the ticks per quarter note
         */
        ReadContext(String version, int division) {
            this.version = version;
            if (division <= 0) {
                this.division = MuseScoreReader.DEFAULT_DIVISION;
            } else {
                this.division = division;
            }
            double number = 0;
            try {
                number = Double.parseDouble(version);
            } catch (RuntimeException e) {
                number = 0;
            }
            this.voiceBased = number >= 3.0;
        }


        /**
         * Gets the file format version.
         *
         * @return the version
         */
        String getVersion() {
            return version;
        }


        /**
         * Gets the ticks per quarter note.
         *
         * @return the division
         */
        int getDivision() {
            return division;
        }


        /**
         * Checks whether the file groups elements by voice (version 3 and later).
         *
         * @return true for version 3 and later
         */
        boolean isVoiceBased() {
            return voiceBased;
        }


        /**
         * Gets the time signatures of a version 1 file by tick.
         *
         * @return the time signatures
         */
        TreeMap<Integer, TimeSignature> getSigList() {
            return sigList;
        }


        /**
         * Gets the tempos of a version 1 file by tick (quarter notes per second).
         *
         * @return the tempos
         */
        TreeMap<Integer, Double> getTempoList() {
            return tempoList;
        }


        /**
         * Gets the measure frames.
         *
         * @return the frames
         */
        List<MeasureFrame> getFrames() {
            return frames;
        }


        /**
         * Sets the measure frames.
         *
         * @param frames the frames
         */
        void setFrames(List<MeasureFrame> frames) {
            this.frames = frames;
        }


        /**
         * Gets the start of a measure (the end of the score for an index after the last measure).
         *
         * @param index the measure index
         * @return the start in whole notes
         */
        Fraction measureStart(int index) {
            if (index < frames.size()) {
                return frames.get(index).getStart();
            }
            if (frames.isEmpty()) {
                return Fraction.ZERO;
            }
            MeasureFrame last = frames.get(frames.size() - 1);
            return last.getStart().add(last.getLength());
        }


        /**
         * Calculates the divisions per quarter note for the staves of a part.
         *
         * @param staves the staves
         * @return the divisions
         */
        int getDivisions(List<StaffData> staves) {
            long divisions = 1;
            for (MeasureFrame frame : frames) {
                divisions = lcm(divisions, frame.getStart().multiply(MuseScoreReader.QUARTERS_PER_WHOLE).getDenominator());
                divisions = lcm(divisions, frame.getLength().multiply(MuseScoreReader.QUARTERS_PER_WHOLE).getDenominator());
            }
            for (StaffData staff : staves) {
                for (int voice = 0; voice < MuseScoreReader.VOICES_PER_STAFF; voice++) {
                    for (Event event : staff.getEvents(voice)) {
                        divisions = lcm(divisions, event.getStart().multiply(MuseScoreReader.QUARTERS_PER_WHOLE).getDenominator());
                        divisions = lcm(divisions, event.getDuration().multiply(MuseScoreReader.QUARTERS_PER_WHOLE).getDenominator());
                    }
                }
            }
            if (divisions > MuseScoreReader.MAX_DIVISIONS) {
                return division;
            }
            return (int) divisions;
        }


        /**
         * Calculates the least common multiple.
         *
         * @param a the first value
         * @param b the second value
         * @return the least common multiple, capped above the maximum
         */
        private static long lcm(long a, long b) {
            if (a <= 0 || b <= 0) {
                return Math.max(a, b);
            }
            long x = a;
            long y = b;
            while (y != 0) {
                long t = x % y;
                x = y;
                y = t;
            }
            long result = a / x * b;
            if (result > MuseScoreReader.MAX_DIVISIONS || result < 0) {
                return MuseScoreReader.MAX_DIVISIONS + 1;
            }
            return result;
        }
    }


    /**
     * The start, length and time signature of a measure.
     *
     * @author patrick
     */
    static final class MeasureFrame {
        private final Fraction start;
        private final Fraction length;
        private final TimeSignature time;
        private final boolean timeChange;


        /**
         * Constructor for MeasureFrame.
         *
         * @param start the start in whole notes
         * @param length the length in whole notes
         * @param time the time signature
         * @param timeChange true if the time signature starts in this measure
         */
        MeasureFrame(Fraction start, Fraction length, TimeSignature time, boolean timeChange) {
            this.start = start;
            this.length = length;
            this.time = time;
            this.timeChange = timeChange;
        }


        /**
         * Gets the start.
         *
         * @return the start in whole notes
         */
        Fraction getStart() {
            return start;
        }


        /**
         * Gets the length.
         *
         * @return the length in whole notes
         */
        Fraction getLength() {
            return length;
        }


        /**
         * Gets the time signature.
         *
         * @return the time signature
         */
        TimeSignature getTime() {
            return time;
        }


        /**
         * Checks whether the time signature starts in this measure.
         *
         * @return true for a time signature change
         */
        boolean isTimeChange() {
            return timeChange;
        }
    }


    /**
     * A time signature.
     *
     * @author patrick
     */
    static final class TimeSignature {
        private final int beats;
        private final int beatType;


        /**
         * Constructor for TimeSignature.
         *
         * @param beats the numerator
         * @param beatType the denominator
         */
        TimeSignature(int beats, int beatType) {
            this.beats = beats;
            this.beatType = beatType;
        }


        /**
         * Gets the numerator.
         *
         * @return the beats
         */
        int getBeats() {
            return beats;
        }


        /**
         * Gets the denominator.
         *
         * @return the beat type
         */
        int getBeatType() {
            return beatType;
        }


        /**
         * Gets the bar length.
         *
         * @return the length in whole notes
         */
        Fraction getLength() {
            return Fraction.of(beats, beatType);
        }


        /**
         * Checks whether another time signature is equal in value.
         *
         * @param other the other time signature
         * @return true if both are equal
         */
        boolean isSameAs(TimeSignature other) {
            return other != null && beats == other.beats && beatType == other.beatType;
        }
    }


    /**
     * The definition of a part.
     *
     * @author patrick
     */
    static final class PartInfo {
        private String name;
        private String abbreviation;
        private int transposeChromatic;
        private int transposeDiatonic;
        private int program = -1;
        private String sound;
        private final List<String> staffIds = new ArrayList<>();
        private final List<String> defaultClefs = new ArrayList<>();
        private final List<Boolean> percussion = new ArrayList<>();
        private final List<TreeMap<Integer, Integer>> keyLists = new ArrayList<>();
        private final List<TreeMap<Integer, String>> clefLists = new ArrayList<>();


        /**
         * Gets the key signatures of version 1 files per staff (fifths by tick).
         *
         * @return the key lists
         */
        List<TreeMap<Integer, Integer>> getKeyLists() {
            return keyLists;
        }


        /**
         * Gets the clefs of version 1 files per staff (clef type by tick).
         *
         * @return the clef lists
         */
        List<TreeMap<Integer, String>> getClefLists() {
            return clefLists;
        }


        /**
         * Gets the name.
         *
         * @return the name, or null
         */
        String getName() {
            return name;
        }


        /**
         * Sets the name.
         *
         * @param name the name
         */
        void setName(String name) {
            this.name = name;
        }


        /**
         * Gets the abbreviation.
         *
         * @return the abbreviation, or null
         */
        String getAbbreviation() {
            return abbreviation;
        }


        /**
         * Sets the abbreviation.
         *
         * @param abbreviation the abbreviation
         */
        void setAbbreviation(String abbreviation) {
            this.abbreviation = abbreviation;
        }


        /**
         * Gets the chromatic transposition (written to sounding).
         *
         * @return the semitones
         */
        int getTransposeChromatic() {
            return transposeChromatic;
        }


        /**
         * Sets the chromatic transposition.
         *
         * @param transposeChromatic the semitones
         */
        void setTransposeChromatic(int transposeChromatic) {
            this.transposeChromatic = transposeChromatic;
        }


        /**
         * Gets the diatonic transposition (written to sounding).
         *
         * @return the steps
         */
        int getTransposeDiatonic() {
            return transposeDiatonic;
        }


        /**
         * Sets the diatonic transposition.
         *
         * @param transposeDiatonic the steps
         */
        void setTransposeDiatonic(int transposeDiatonic) {
            this.transposeDiatonic = transposeDiatonic;
        }


        /**
         * Gets the MIDI program (0 to 127).
         *
         * @return the program, or -1
         */
        int getProgram() {
            return program;
        }


        /**
         * Sets the MIDI program.
         *
         * @param program the program (0 to 127)
         */
        void setProgram(int program) {
            this.program = program;
        }


        /**
         * Gets the instrument sound (for example brass.trumpet.bflat).
         *
         * @return the sound, or null
         */
        String getSound() {
            return sound;
        }


        /**
         * Sets the instrument sound.
         *
         * @param sound the sound (for example brass.trumpet.bflat), or null
         */
        void setSound(String sound) {
            this.sound = sound;
        }


        /**
         * Gets the staff IDs.
         *
         * @return the IDs in order
         */
        List<String> getStaffIds() {
            return staffIds;
        }


        /**
         * Gets the default clefs per staff.
         *
         * @return the clef types (may be empty strings)
         */
        List<String> getDefaultClefs() {
            return defaultClefs;
        }


        /**
         * Gets the percussion flags per staff.
         *
         * @return the flags
         */
        List<Boolean> getPercussion() {
            return percussion;
        }


        /**
         * Checks whether a staff is a percussion staff.
         *
         * @param staffIndex the staff index
         * @return true for percussion
         */
        boolean isPercussion(int staffIndex) {
            return staffIndex < percussion.size() && percussion.get(staffIndex);
        }


        /**
         * Gets the default clef of a staff.
         *
         * @param staffIndex the staff index
         * @return the clef type
         */
        String getDefaultClef(int staffIndex) {
            if (staffIndex < defaultClefs.size() && !defaultClefs.get(staffIndex).isEmpty()) {
                return defaultClefs.get(staffIndex);
            }
            if (isPercussion(staffIndex)) {
                return "PERC";
            }
            return MuseScoreReader.CLEF_TREBLE;
        }
    }


    /**
     * The music of one staff: events per voice, measure data and spanners.
     *
     * @author patrick
     */
    static final class StaffData {
        private final int staffIndex;
        private final boolean percussion;
        private int transposeChromatic;
        private int transposeDiatonic;
        private final List<MeasureData> measures = new ArrayList<>();
        private final List<List<Event>> events = new ArrayList<>();
        private final List<SpannerData> spanners = new ArrayList<>();
        private final Map<String, SpannerData> openSpanners = new HashMap<>();
        private final Map<String, Event> openSlurs = new HashMap<>();
        private final List<Event[]> legacySlurs = new ArrayList<>();
        private final List<Object[]> breaths = new ArrayList<>();
        private TimeSignature localTime;


        /**
         * Constructor for StaffData.
         *
         * @param staffIndex the index of the staff within the part
         * @param percussion true for a percussion staff
         */
        StaffData(int staffIndex, boolean percussion) {
            this.staffIndex = staffIndex;
            this.percussion = percussion;
            for (int v = 0; v < MuseScoreReader.VOICES_PER_STAFF; v++) {
                events.add(new ArrayList<>());
            }
        }


        /**
         * Gets the current time signature of the staff.
         *
         * @return the time signature, or null before the first one
         */
        TimeSignature getLocalTime() {
            return localTime;
        }


        /**
         * Sets the current time signature of the staff.
         *
         * @param localTime the time signature
         */
        void setLocalTime(TimeSignature localTime) {
            this.localTime = localTime;
        }


        /**
         * Gets the index of the staff within the part.
         *
         * @return the index
         */
        int getStaffIndex() {
            return staffIndex;
        }


        /**
         * Checks whether this is a percussion staff.
         *
         * @return true for percussion
         */
        boolean isPercussion() {
            return percussion;
        }


        /**
         * Gets the chromatic transposition of the part.
         *
         * @return the semitones
         */
        int getTransposeChromatic() {
            return transposeChromatic;
        }


        /**
         * Gets the diatonic transposition of the part.
         *
         * @return the steps
         */
        int getTransposeDiatonic() {
            return transposeDiatonic;
        }


        /**
         * Sets the transposition of the part.
         *
         * @param chromatic the semitones
         * @param diatonic the steps
         */
        void setTransposition(int chromatic, int diatonic) {
            this.transposeChromatic = chromatic;
            this.transposeDiatonic = diatonic;
        }


        /**
         * Gets the measures.
         *
         * @return the measure data
         */
        List<MeasureData> getMeasures() {
            return measures;
        }


        /**
         * Gets the data of a measure.
         *
         * @param index the measure index
         * @return the data, or null
         */
        MeasureData measure(int index) {
            if (index < measures.size()) {
                return measures.get(index);
            }
            return null;
        }


        /**
         * Gets the events of a voice.
         *
         * @param voice the voice index
         * @return the events in order
         */
        List<Event> getEvents(int voice) {
            return events.get(voice);
        }


        /**
         * Adds an event to a voice, keeping the time order (grace notes before their chord).
         *
         * @param voice the voice index
         * @param event the event
         */
        void addEvent(int voice, Event event) {
            List<Event> list = events.get(voice);
            int index = list.size();
            while (index > 0 && list.get(index - 1).getStart().compareTo(event.getStart()) > 0) {
                index--;
            }
            list.add(index, event);
        }


        /**
         * Gets the events of a voice that start in a measure.
         *
         * @param voice the voice index
         * @param frame the measure frame
         * @return the events
         */
        List<Event> eventsIn(int voice, MeasureFrame frame) {
            List<Event> result = new ArrayList<>();
            Fraction end = frame.getStart().add(frame.getLength());
            for (Event event : events.get(voice)) {
                if (event.getStart().compareTo(frame.getStart()) >= 0 && event.getStart().compareTo(end) < 0) {
                    result.add(event);
                }
            }
            return result;
        }


        /**
         * Gets the spanners.
         *
         * @return the spanners
         */
        List<SpannerData> getSpanners() {
            return spanners;
        }


        /**
         * Starts a spanner of a version 1 or 2 file.
         *
         * @param id the spanner ID
         * @param spanner the spanner
         */
        void startLegacySpanner(String id, SpannerData spanner) {
            openSpanners.put(id, spanner);
        }


        /**
         * Ends a spanner or a tie of a version 1 or 2 file.
         *
         * @param id the spanner ID
         * @param position the end position
         * @param voice the voice index
         */
        void endLegacySpanner(String id, Fraction position, int voice) {
            SpannerData spanner = openSpanners.remove(id);
            if (spanner != null) {
                spanners.add(new SpannerData(spanner.getType(), spanner.getDefinition(), spanner.getStart(), position, spanner.getVoice()));
            }
        }


        /**
         * Starts a slur of a version 1 or 2 file at a chord.
         *
         * @param id the slur ID
         * @param event the chord
         */
        void startLegacySlur(String id, Event event) {
            openSlurs.put(id, event);
        }


        /**
         * Ends a slur of a version 1 or 2 file at a chord.
         *
         * @param id the slur ID
         * @param event the chord
         */
        void stopLegacySlur(String id, Event event) {
            Event start = openSlurs.remove(id);
            if (start != null && start != event) {
                legacySlurs.add(new Event[] {start, event});
            }
        }


        /**
         * Marks a breath mark after the last chord of a voice before a position.
         *
         * @param voice the voice index
         * @param position the position of the breath mark
         */
        void markBreath(int voice, Fraction position) {
            breaths.add(new Object[] {voice, position});
        }


        /**
         * Finishes the staff: breath marks, ties and slurs.
         *
         * @param context the read context
         */
        void finish(ReadContext context) {
            applyOttavas();
            for (Object[] breath : breaths) {
                Event last = null;
                for (Event event : events.get((Integer) breath[0])) {
                    if (event.getStart().compareTo((Fraction) breath[1]) < 0 && event.getGrace() == null) {
                        last = event;
                    }
                }
                if (last != null) {
                    last.setBreath(true);
                }
            }
            breaths.clear();
            closeTies();
            assignSlurs();
        }


        /**
         * Shifts the notes under octave lines: MuseScore stores them at their written position, the
         * MusicXML pitch is the sounding pitch.
         */
        private void applyOttavas() {
            for (SpannerData spanner : spanners) {
                if (!"Ottava".equals(spanner.getType()) || spanner.getEnd() == null) {
                    continue;
                }
                String subtype = null;
                if (spanner.getDefinition() != null) {
                    NodeList subtypes = spanner.getDefinition().getElementsByTagName("subtype");
                    if (subtypes.getLength() > 0) {
                        subtype = subtypes.item(0).getTextContent().trim();
                    }
                }
                String[] ottava = MuseScoreSymbolConverter.getInstance().ottavaToXml(subtype);
                int shift = SEMITONES_PER_OCTAVE;
                if ("15".equals(ottava[1])) {
                    shift = 2 * SEMITONES_PER_OCTAVE;
                } else if ("22".equals(ottava[1])) {
                    shift = 3 * SEMITONES_PER_OCTAVE;
                }
                if ("up".equals(ottava[0])) {
                    shift = -shift;
                }
                for (List<Event> voice : events) {
                    for (Event event : voice) {
                        if (event.getStart().compareTo(spanner.getStart()) >= 0 && event.getStart().compareTo(spanner.getEnd()) < 0) {
                            for (NoteData note : event.getNotes()) {
                                note.setOctaveShift(shift);
                            }
                        }
                    }
                }
            }
        }


        /**
         * Closes ties: a tied note is tied to the note with the same pitch that starts where it ends,
         * preferably in the same voice, otherwise in another voice of the staff. Ties without such a
         * note are dropped.
         */
        private void closeTies() {
            for (int v = 0; v < events.size(); v++) {
                for (Event event : events.get(v)) {
                    if (event.getGrace() != null) {
                        continue;
                    }
                    Fraction end = event.getStart().add(event.getDuration());
                    for (NoteData note : event.getNotes()) {
                        if (!note.isTieStart()) {
                            continue;
                        }
                        NoteData target = findTieTarget(v, end, note.getPitch());
                        if (target == null) {
                            note.setTieStart(false);
                        } else {
                            target.setTieStop(true);
                        }
                    }
                }
            }
        }


        /**
         * Finds the note a tie ends on.
         *
         * @param voice the voice of the tied note
         * @param position the end of the tied note
         * @param pitch the pitch
         * @return the note, or null
         */
        private NoteData findTieTarget(int voice, Fraction position, int pitch) {
            NoteData result = findNote(events.get(voice), position, pitch);
            for (int v = 0; v < events.size() && result == null; v++) {
                if (v != voice) {
                    result = findNote(events.get(v), position, pitch);
                }
            }
            return result;
        }


        /**
         * Finds a note with a pitch in a chord that starts at a position.
         *
         * @param voice the events of a voice
         * @param position the position
         * @param pitch the pitch
         * @return the note, or null
         */
        private NoteData findNote(List<Event> voice, Fraction position, int pitch) {
            for (Event event : voice) {
                if (event.getGrace() == null && event.getStart().equals(position)) {
                    for (NoteData note : event.getNotes()) {
                        if (note.getPitch() == pitch) {
                            return note;
                        }
                    }
                }
            }
            return null;
        }


        /**
         * Attaches slurs to their first and last chord and numbers them.
         */
        private void assignSlurs() {
            List<Event[]> slurs = new ArrayList<>(legacySlurs);
            for (SpannerData spanner : spanners) {
                if (!"Slur".equals(spanner.getType()) || spanner.getEnd() == null) {
                    continue;
                }
                List<Event> voice = events.get(spanner.getVoice());
                Event first = null;
                Event last = null;
                for (Event event : voice) {
                    if (event.isRest() || event.getGrace() != null) {
                        continue;
                    }
                    if (event.getStart().compareTo(spanner.getStart()) >= 0 && event.getStart().compareTo(spanner.getEnd()) <= 0) {
                        if (first == null) {
                            first = event;
                        }
                        last = event;
                    }
                }
                if (first != null && last != null && first != last) {
                    slurs.add(new Event[] {first, last});
                }
            }
            slurs.sort((a, b) -> a[0].getStart().compareTo(b[0].getStart()));
            Fraction[] ends = new Fraction[MuseScoreReader.MAX_NUMBER];
            for (Event[] slur : slurs) {
                int number = 1;
                for (int i = 0; i < ends.length; i++) {
                    if (ends[i] == null || ends[i].compareTo(slur[0].getStart()) <= 0) {
                        number = i + 1;
                        ends[i] = slur[1].getStart();
                        break;
                    }
                }
                slur[0].getSlurStarts().add(number);
                slur[1].getSlurStops().add(number);
            }
        }
    }


    /**
     * The data of one measure of a staff.
     *
     * @author patrick
     */
    static final class MeasureData {
        private KeyData key;
        private final TreeMap<Fraction, String> clefs = new TreeMap<>();
        private final List<PositionedDirection> directions = new ArrayList<>();
        private final List<HarmonyImpl> harmonies = new ArrayList<>();
        private boolean startRepeat;
        private int endRepeat;
        private String barStyle;
        private boolean measureRepeat;
        private Fraction stretch = Fraction.of(1, 1);


        /**
         * Gets the stretch factor of a local time signature.
         *
         * @return the factor (1 without a local time signature)
         */
        Fraction getStretch() {
            return stretch;
        }


        /**
         * Sets the stretch factor of a local time signature.
         *
         * @param stretch the factor
         */
        void setStretch(Fraction stretch) {
            this.stretch = stretch;
        }


        /**
         * Gets the key signature starting in this measure.
         *
         * @return the key, or null
         */
        KeyData getKey() {
            return key;
        }


        /**
         * Sets the key signature.
         *
         * @param key the key
         */
        void setKey(KeyData key) {
            this.key = key;
        }


        /**
         * Gets the clef changes by position.
         *
         * @return the clef types
         */
        TreeMap<Fraction, String> getClefs() {
            return clefs;
        }


        /**
         * Gets the directions.
         *
         * @return the directions
         */
        List<PositionedDirection> getDirections() {
            return directions;
        }


        /**
         * Adds a direction.
         *
         * @param position the position
         * @param direction the direction
         * @param staffIndex the index of the staff within the part
         */
        void addDirection(Fraction position, DirectionImpl direction, int staffIndex) {
            directions.add(new PositionedDirection(position, direction, staffIndex, false));
        }


        /**
         * Adds a direction that is shown once per score (first part only).
         *
         * @param position the position
         * @param direction the direction
         */
        void addGlobalDirection(Fraction position, DirectionImpl direction) {
            directions.add(new PositionedDirection(position, direction, 0, true));
        }


        /**
         * Gets the chord symbols.
         *
         * @return the harmonies
         */
        List<HarmonyImpl> getHarmonies() {
            return harmonies;
        }


        /**
         * Checks whether the measure starts a repeat.
         *
         * @return true for a start repeat
         */
        boolean isStartRepeat() {
            return startRepeat;
        }


        /**
         * Sets the start repeat.
         *
         * @param startRepeat true for a start repeat
         */
        void setStartRepeat(boolean startRepeat) {
            this.startRepeat = startRepeat;
        }


        /**
         * Gets the number of plays of an end repeat.
         *
         * @return the times, 0 if the measure does not end a repeat
         */
        int getEndRepeat() {
            return endRepeat;
        }


        /**
         * Sets the end repeat.
         *
         * @param endRepeat the number of plays
         */
        void setEndRepeat(int endRepeat) {
            this.endRepeat = endRepeat;
        }


        /**
         * Gets the bar style of the right barline.
         *
         * @return the MusicXML bar style, or null
         */
        String getBarStyle() {
            return barStyle;
        }


        /**
         * Sets the bar style of the right barline.
         *
         * @param barStyle the MusicXML bar style
         */
        void setBarStyle(String barStyle) {
            this.barStyle = barStyle;
        }


        /**
         * Checks whether the measure repeats the previous measure.
         *
         * @return true for a measure repeat sign
         */
        boolean isMeasureRepeat() {
            return measureRepeat;
        }


        /**
         * Sets whether the measure repeats the previous measure.
         *
         * @param measureRepeat true for a measure repeat sign
         */
        void setMeasureRepeat(boolean measureRepeat) {
            this.measureRepeat = measureRepeat;
        }
    }


    /**
     * A key signature.
     *
     * @author patrick
     */
    static final class KeyData {
        private final int fifths;
        private final String mode;
        private final boolean concert;


        /**
         * Constructor for KeyData.
         *
         * @param fifths the written fifths
         * @param mode the mode, or null
         * @param concert true if only the concert key is known
         */
        KeyData(int fifths, String mode, boolean concert) {
            this.fifths = fifths;
            this.mode = mode;
            this.concert = concert;
        }


        /**
         * Gets the fifths.
         *
         * @return the fifths
         */
        int getFifths() {
            return fifths;
        }


        /**
         * Gets the mode.
         *
         * @return the mode, or null
         */
        String getMode() {
            return mode;
        }


        /**
         * Checks whether only the concert key is known.
         *
         * @return true for a concert key
         */
        boolean isConcert() {
            return concert;
        }
    }


    /**
     * A direction with its position.
     *
     * @author patrick
     */
    static final class PositionedDirection {
        private final Fraction position;
        private final DirectionImpl direction;
        private final int staffIndex;
        private final boolean global;


        /**
         * Constructor for PositionedDirection.
         *
         * @param position the position in whole notes
         * @param direction the direction
         * @param staffIndex the index of the staff within the part
         * @param global true for directions shown once per system (first part only)
         */
        PositionedDirection(Fraction position, DirectionImpl direction, int staffIndex, boolean global) {
            this.position = position;
            this.direction = direction;
            this.staffIndex = staffIndex;
            this.global = global;
        }


        /**
         * Gets the position.
         *
         * @return the position in whole notes
         */
        Fraction getPosition() {
            return position;
        }


        /**
         * Gets the direction.
         *
         * @return the direction
         */
        DirectionImpl getDirection() {
            return direction;
        }


        /**
         * Gets the index of the staff within the part.
         *
         * @return the staff index
         */
        int getStaffIndex() {
            return staffIndex;
        }


        /**
         * Checks whether the direction is global.
         *
         * @return true for a global direction
         */
        boolean isGlobal() {
            return global;
        }
    }


    /**
     * The spanner directions of a part (hairpins, octave lines, pedals).
     *
     * @author patrick
     */
    static final class PartDirections {
        private static final Comparator<PositionedDirection> BY_POSITION = (a, b) -> a.getPosition().compareTo(b.getPosition());
        private final List<PositionedDirection> directions = new ArrayList<>();
        private final List<PositionedDirection> endDirections = new ArrayList<>();
        private boolean sorted;


        /**
         * Adds a direction.
         *
         * @param position the position
         * @param direction the direction
         * @param staffIndex the index of the staff within the part
         */
        void add(Fraction position, DirectionImpl direction, int staffIndex) {
            directions.add(new PositionedDirection(position, direction, staffIndex, false));
            sorted = false;
        }


        /**
         * Adds the end of a spanner: an end on a barline belongs to the measure before it, not to the next measure.
         *
         * @param position the position
         * @param direction the direction
         * @param staffIndex the index of the staff within the part
         */
        void addEnd(Fraction position, DirectionImpl direction, int staffIndex) {
            endDirections.add(new PositionedDirection(position, direction, staffIndex, false));
            sorted = false;
        }


        /**
         * Gets the directions of a measure in time order: at the same position a spanner ends before the next one
         * starts. The directions are sorted once, a measure finds its directions by binary search.
         *
         * @param frame the measure frame
         * @param lastMeasure true for the last measure (directions at the end of the score belong to it)
         * @return the directions
         */
        List<PositionedDirection> inMeasure(MeasureFrame frame, boolean lastMeasure) {
            if (!sorted) {
                // stable sort: directions at the same position keep their order
                directions.sort(BY_POSITION);
                endDirections.sort(BY_POSITION);
                sorted = true;
            }
            Fraction start = frame.getStart();
            Fraction end = start.add(frame.getLength());
            List<PositionedDirection> result = new ArrayList<>();
            // the ends: after the start of the measure, up to and including its end
            for (int i = firstIndex(endDirections, start, false); i < endDirections.size(); i++) {
                PositionedDirection direction = endDirections.get(i);
                if (!lastMeasure && direction.getPosition().compareTo(end) > 0) {
                    break;
                }
                result.add(direction);
            }
            // the other directions: from the start of the measure, before its end
            for (int i = firstIndex(directions, start, true); i < directions.size(); i++) {
                PositionedDirection direction = directions.get(i);
                if (!lastMeasure && direction.getPosition().compareTo(end) >= 0) {
                    break;
                }
                result.add(direction);
            }
            result.sort(BY_POSITION);
            return result;
        }


        /**
         * Finds the first direction of a sorted list at or after a position.
         *
         * @param list the directions, sorted by position
         * @param position the position
         * @param inclusive true to include a direction at the position, false for the first one after it
         * @return the index, the size of the list if there is none
         */
        private int firstIndex(List<PositionedDirection> list, Fraction position, boolean inclusive) {
            int low = 0;
            int high = list.size();
            while (low < high) {
                int middle = (low + high) >>> 1;
                int compare = list.get(middle).getPosition().compareTo(position);
                if (compare < 0 || compare == 0 && !inclusive) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        }
    }


    /**
     * A volta (repeat ending).
     *
     * @author patrick
     */
    static final class VoltaData {
        private final Fraction start;
        private final Fraction end;
        private final String endings;
        private final String text;
        private final boolean closed;


        /**
         * Constructor for VoltaData.
         *
         * @param start the start position
         * @param end the end position
         * @param endings the ending numbers, for example "1,2"
         * @param text the text, or null
         * @param closed true if the ending has a closing hook
         */
        VoltaData(Fraction start, Fraction end, String endings, String text, boolean closed) {
            this.start = start;
            this.end = end;
            this.endings = endings;
            this.text = text;
            this.closed = closed;
        }


        /**
         * Gets the start position.
         *
         * @return the start
         */
        Fraction getStart() {
            return start;
        }


        /**
         * Gets the end position.
         *
         * @return the end
         */
        Fraction getEnd() {
            return end;
        }


        /**
         * Gets the ending numbers.
         *
         * @return the numbers
         */
        String getEndings() {
            return endings;
        }


        /**
         * Gets the text.
         *
         * @return the text, or null
         */
        String getText() {
            return text;
        }


        /**
         * Checks whether the ending has a closing hook.
         *
         * @return true if closed
         */
        boolean isClosed() {
            return closed;
        }
    }


    /**
     * A spanner: type, definition element, start and end position.
     *
     * @author patrick
     */
    static final class SpannerData {
        private final String type;
        private final Element definition;
        private final Fraction start;
        private final Fraction end;
        private final int voice;


        /**
         * Constructor for SpannerData.
         *
         * @param type the spanner type (Slur, HairPin, ...)
         * @param definition the definition element (may be null)
         * @param start the start position
         * @param end the end position, or null if open
         * @param voice the voice index
         */
        SpannerData(String type, Element definition, Fraction start, Fraction end, int voice) {
            this.type = type;
            this.definition = definition;
            this.start = start;
            this.end = end;
            this.voice = voice;
        }


        /**
         * Gets the type.
         *
         * @return the type
         */
        String getType() {
            return type;
        }


        /**
         * Gets the definition element.
         *
         * @return the definition, or null
         */
        Element getDefinition() {
            return definition;
        }


        /**
         * Gets the start position.
         *
         * @return the start
         */
        Fraction getStart() {
            return start;
        }


        /**
         * Gets the end position.
         *
         * @return the end, or null
         */
        Fraction getEnd() {
            return end;
        }


        /**
         * Gets the voice index.
         *
         * @return the voice
         */
        int getVoice() {
            return voice;
        }
    }


    /**
     * The reading state of a voice.
     *
     * @author patrick
     */
    static final class VoiceState {
        private int voice;
        private Fraction position;
        private final List<TupletState> tupletStack = new ArrayList<>();
        private final Map<String, TupletState> tuplets = new HashMap<>();
        private String pendingFermata;
        private Fraction stretch = Fraction.of(1, 1);


        /**
         * Constructor for VoiceState.
         *
         * @param voice the voice index
         * @param position the start position
         */
        VoiceState(int voice, Fraction position) {
            this.voice = voice;
            this.position = position;
        }


        /**
         * Gets the voice index.
         *
         * @return the voice
         */
        int getVoice() {
            return voice;
        }


        /**
         * Sets the voice index.
         *
         * @param voice the voice
         */
        void setVoice(int voice) {
            this.voice = voice;
        }


        /**
         * Gets the current position.
         *
         * @return the position in whole notes
         */
        Fraction getPosition() {
            return position;
        }


        /**
         * Sets the current position.
         *
         * @param position the position in whole notes
         */
        void setPosition(Fraction position) {
            this.position = position;
        }


        /**
         * Pushes a tuplet that applies to the following elements (version 4).
         *
         * @param tuplet the tuplet
         */
        void pushTuplet(TupletState tuplet) {
            tupletStack.add(tuplet);
        }


        /**
         * Ends the innermost tuplet (version 4).
         */
        void popTuplet() {
            if (!tupletStack.isEmpty()) {
                tupletStack.remove(tupletStack.size() - 1);
            }
        }


        /**
         * Gets the innermost open tuplet.
         *
         * @return the tuplet, or null
         */
        TupletState currentTuplet() {
            if (tupletStack.isEmpty()) {
                return null;
            }
            return tupletStack.get(tupletStack.size() - 1);
        }


        /**
         * Defines a tuplet that is referenced by ID (versions 1 to 3).
         *
         * @param id the ID
         * @param tuplet the tuplet
         */
        void defineTuplet(String id, TupletState tuplet) {
            tuplets.put(id, tuplet);
        }


        /**
         * Gets a tuplet by ID.
         *
         * @param id the ID
         * @return the tuplet, or null
         */
        TupletState getTuplet(String id) {
            return tuplets.get(id);
        }


        /**
         * Gets the stretch factor of a local time signature.
         *
         * @return the factor (1 without a local time signature)
         */
        Fraction getStretch() {
            return stretch;
        }


        /**
         * Sets the stretch factor of a local time signature.
         *
         * @param stretch the factor
         */
        void setStretch(Fraction stretch) {
            this.stretch = stretch;
        }


        /**
         * Gets the fermata waiting for the next chord.
         *
         * @return the fermata subtype, or null
         */
        String getPendingFermata() {
            return pendingFermata;
        }


        /**
         * Sets the fermata waiting for the next chord.
         *
         * @param pendingFermata the fermata subtype, or null
         */
        void setPendingFermata(String pendingFermata) {
            this.pendingFermata = pendingFermata;
        }
    }


    /**
     * A tuplet with the events that belong to it.
     *
     * @author patrick
     */
    static final class TupletState {
        private final int actual;
        private final int normal;
        private final String baseNote;
        private final Fraction ratio;
        private final List<Event> events = new ArrayList<>();


        /**
         * Constructor for TupletState.
         *
         * @param actual the actual notes
         * @param normal the normal notes
         * @param baseNote the base note type
         * @param ratio the ratio of played to written length (including enclosing tuplets)
         */
        TupletState(int actual, int normal, String baseNote, Fraction ratio) {
            this.actual = actual;
            this.normal = normal;
            this.baseNote = baseNote;
            this.ratio = ratio;
        }


        /**
         * Gets the actual notes.
         *
         * @return the actual notes
         */
        int getActual() {
            return actual;
        }


        /**
         * Gets the normal notes.
         *
         * @return the normal notes
         */
        int getNormal() {
            return normal;
        }


        /**
         * Gets the base note type.
         *
         * @return the type (may be empty)
         */
        String getBaseNote() {
            return baseNote;
        }


        /**
         * Gets the ratio of played to written length.
         *
         * @return the ratio
         */
        Fraction getRatio() {
            return ratio;
        }


        /**
         * Adds an event.
         *
         * @param event the event
         */
        void addEvent(Event event) {
            events.add(event);
        }


        /**
         * Checks whether an event is the first of the tuplet.
         *
         * @param event the event
         * @return true for the first event
         */
        boolean isFirst(Event event) {
            return !events.isEmpty() && events.get(0) == event;
        }


        /**
         * Checks whether an event is the last of the tuplet.
         *
         * @param event the event
         * @return true for the last event
         */
        boolean isLast(Event event) {
            return !events.isEmpty() && events.get(events.size() - 1) == event;
        }
    }


    /**
     * A chord, rest or grace chord of a voice.
     *
     * @author patrick
     */
    static final class Event {
        private final Fraction start;
        private final Fraction duration;
        private final String type;
        private final int dots;
        private final boolean rest;
        private final String grace;
        private boolean measureRest;
        private int voice;
        private int staffMove;
        private boolean invisible;
        private boolean breath;
        private TupletState tuplet;
        private String fermata;
        private Fraction stretch;
        private final List<NoteData> notes = new ArrayList<>();
        private final List<String> articulations = new ArrayList<>();
        private final List<LyricImpl> lyrics = new ArrayList<>();
        private final List<Integer> slurStarts = new ArrayList<>();
        private final List<Integer> slurStops = new ArrayList<>();


        /**
         * Constructor for Event.
         *
         * @param start the start position
         * @param duration the played duration
         * @param type the duration type
         * @param dots the number of dots
         * @param rest true for a rest
         * @param grace the grace type, or null
         */
        Event(Fraction start, Fraction duration, String type, int dots, boolean rest, String grace) {
            this.start = start;
            this.duration = duration;
            this.type = type;
            this.dots = dots;
            this.rest = rest;
            this.grace = grace;
        }


        /**
         * Creates a copy shifted in time (measure repeat).
         *
         * @param shift the shift
         * @return the copy
         */
        Event copyShifted(Fraction shift) {
            Event copy = new Event(start.add(shift), duration, type, dots, rest, grace);
            copy.measureRest = measureRest;
            copy.voice = voice;
            copy.staffMove = staffMove;
            copy.invisible = invisible;
            copy.fermata = fermata;
            copy.stretch = stretch;
            for (NoteData note : notes) {
                copy.notes.add(note.copy());
            }
            copy.articulations.addAll(articulations);
            return copy;
        }


        /**
         * Gets the start position.
         *
         * @return the start
         */
        Fraction getStart() {
            return start;
        }


        /**
         * Gets the played duration.
         *
         * @return the duration in whole notes
         */
        Fraction getDuration() {
            if (grace != null) {
                return Fraction.ZERO;
            }
            return duration;
        }


        /**
         * Gets the duration type.
         *
         * @return the type
         */
        String getType() {
            return type;
        }


        /**
         * Gets the number of dots.
         *
         * @return the dots
         */
        int getDots() {
            return dots;
        }


        /**
         * Checks whether this is a rest.
         *
         * @return true for a rest
         */
        boolean isRest() {
            return rest;
        }


        /**
         * Gets the grace type.
         *
         * @return the grace type, or null
         */
        String getGrace() {
            return grace;
        }


        /**
         * Checks whether this is a measure rest.
         *
         * @return true for a measure rest
         */
        boolean isMeasureRest() {
            return measureRest;
        }


        /**
         * Sets whether this is a measure rest.
         *
         * @param measureRest true for a measure rest
         */
        void setMeasureRest(boolean measureRest) {
            this.measureRest = measureRest;
        }


        /**
         * Sets the voice index.
         *
         * @param voice the voice
         */
        void setVoice(int voice) {
            this.voice = voice;
        }


        /**
         * Gets the staff move (cross-staff notes).
         *
         * @return the move
         */
        int getStaffMove() {
            return staffMove;
        }


        /**
         * Sets the staff move.
         *
         * @param staffMove the move
         */
        void setStaffMove(int staffMove) {
            this.staffMove = staffMove;
        }


        /**
         * Checks whether the event is invisible.
         *
         * @return true if invisible
         */
        boolean isInvisible() {
            return invisible;
        }


        /**
         * Sets whether the event is invisible.
         *
         * @param invisible true if invisible
         */
        void setInvisible(boolean invisible) {
            this.invisible = invisible;
        }


        /**
         * Checks whether a breath mark follows the event.
         *
         * @return true for a breath mark
         */
        boolean isBreath() {
            return breath;
        }


        /**
         * Sets whether a breath mark follows the event.
         *
         * @param breath true for a breath mark
         */
        void setBreath(boolean breath) {
            this.breath = breath;
        }


        /**
         * Gets the tuplet.
         *
         * @return the tuplet, or null
         */
        TupletState getTuplet() {
            return tuplet;
        }


        /**
         * Sets the tuplet.
         *
         * @param tuplet the tuplet
         */
        void setTuplet(TupletState tuplet) {
            this.tuplet = tuplet;
        }


        /**
         * Gets the stretch of a local time signature.
         *
         * @return the stretch, or null outside a local time signature
         */
        Fraction getStretch() {
            return stretch;
        }


        /**
         * Sets the stretch of a local time signature.
         *
         * @param stretch the stretch
         */
        void setStretch(Fraction stretch) {
            this.stretch = stretch;
        }


        /**
         * Gets the fermata subtype.
         *
         * @return the fermata, or null
         */
        String getFermata() {
            return fermata;
        }


        /**
         * Sets the fermata subtype.
         *
         * @param fermata the fermata
         */
        void setFermata(String fermata) {
            this.fermata = fermata;
        }


        /**
         * Gets the notes.
         *
         * @return the notes
         */
        List<NoteData> getNotes() {
            return notes;
        }


        /**
         * Gets the articulation subtypes.
         *
         * @return the subtypes
         */
        List<String> getArticulations() {
            return articulations;
        }


        /**
         * Gets the lyrics.
         *
         * @return the lyrics
         */
        List<LyricImpl> getLyrics() {
            return lyrics;
        }


        /**
         * Gets the numbers of the slurs starting at this event.
         *
         * @return the numbers
         */
        List<Integer> getSlurStarts() {
            return slurStarts;
        }


        /**
         * Gets the numbers of the slurs ending at this event.
         *
         * @return the numbers
         */
        List<Integer> getSlurStops() {
            return slurStops;
        }
    }


    /**
     * A note of a chord.
     *
     * @author patrick
     */
    static final class NoteData {
        private final int pitch;
        private int octaveShift;
        private final int tpc;
        private final int tpc2;
        private boolean tieStart;
        private boolean tieStop;
        private String accidental;
        private String notehead;
        private String fingering;
        private boolean small;


        /**
         * Constructor for NoteData.
         *
         * @param pitch the concert MIDI pitch
         * @param tpc the concert tonal pitch class (or Integer.MIN_VALUE)
         * @param tpc2 the written tonal pitch class (or Integer.MIN_VALUE)
         */
        NoteData(int pitch, int tpc, int tpc2) {
            this.pitch = pitch;
            this.tpc = tpc;
            this.tpc2 = tpc2;
        }


        /**
         * Creates a copy without ties.
         *
         * @return the copy
         */
        NoteData copy() {
            NoteData copy = new NoteData(pitch, tpc, tpc2);
            copy.accidental = accidental;
            copy.notehead = notehead;
            copy.small = small;
            copy.tieStart = tieStart;
            copy.octaveShift = octaveShift;
            return copy;
        }


        /**
         * Gets the octave shift of an octave line.
         *
         * @return the shift in semitones (0 without an octave line)
         */
        int getOctaveShift() {
            return octaveShift;
        }


        /**
         * Sets the octave shift of an octave line.
         *
         * @param octaveShift the shift in semitones
         */
        void setOctaveShift(int octaveShift) {
            this.octaveShift = octaveShift;
        }


        /**
         * Gets the concert MIDI pitch.
         *
         * @return the pitch
         */
        int getPitch() {
            return pitch;
        }


        /**
         * Gets the concert tonal pitch class.
         *
         * @return the tpc
         */
        int getTpc() {
            return tpc;
        }


        /**
         * Gets the written tonal pitch class.
         *
         * @return the tpc2
         */
        int getTpc2() {
            return tpc2;
        }


        /**
         * Checks whether the note is tied to the next note.
         *
         * @return true for a tie start
         */
        boolean isTieStart() {
            return tieStart;
        }


        /**
         * Sets whether the note is tied to the next note.
         *
         * @param tieStart true for a tie start
         */
        void setTieStart(boolean tieStart) {
            this.tieStart = tieStart;
        }


        /**
         * Checks whether the note is tied from the previous note.
         *
         * @return true for a tie stop
         */
        boolean isTieStop() {
            return tieStop;
        }


        /**
         * Sets whether the note is tied from the previous note.
         *
         * @param tieStop true for a tie stop
         */
        void setTieStop(boolean tieStop) {
            this.tieStop = tieStop;
        }


        /**
         * Gets the accidental.
         *
         * @return the MusicXML accidental, or null
         */
        String getAccidental() {
            return accidental;
        }


        /**
         * Sets the accidental.
         *
         * @param accidental the MusicXML accidental
         */
        void setAccidental(String accidental) {
            this.accidental = accidental;
        }


        /**
         * Gets the notehead.
         *
         * @return the MusicXML notehead, or null
         */
        String getNotehead() {
            return notehead;
        }


        /**
         * Sets the notehead.
         *
         * @param notehead the MusicXML notehead
         */
        void setNotehead(String notehead) {
            this.notehead = notehead;
        }


        /**
         * Gets the fingering.
         *
         * @return the fingering (numbers separated by spaces), or null
         */
        String getFingering() {
            return fingering;
        }


        /**
         * Adds a number to the fingering: several numbers of one note are separated by a space.
         *
         * @param finger the number
         */
        void addFingering(String finger) {
            if (fingering == null) {
                fingering = finger;
            } else {
                fingering = fingering + " " + finger;
            }
        }


        /**
         * Checks whether the note is shown small (cue size).
         *
         * @return true for a small note
         */
        boolean isSmall() {
            return small;
        }


        /**
         * Sets whether the note is shown small (cue size).
         *
         * @param small true for a small note
         */
        void setSmall(boolean small) {
            this.small = small;
        }
    }
}
