/*
 * DoricoReaderTypes.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.notation.impl.ArticulationsImpl;
import com.github.toolarium.music.model.notation.impl.OrnamentsImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import com.github.toolarium.music.model.note.impl.PitchImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


/**
 * Package-private companion types used by {@link DoricoReaderImpl}.
 *
 * @author patrick
 */
final class DoricoReaderTypes {

    /**
     * Private constructor.
     */
    private DoricoReaderTypes() {
        // utility class
    }


    /**
     * An event of a stream with its absolute position in the flow.
     *
     * @author patrick
     */
    static final class EventRef implements Comparable<EventRef> {
        private final DoricoTreeNode node;
        private final Fraction position;
        private final Fraction duration;
        private final Fraction secondaryPosition;


        /**
         * Constructor for EventRef.
         *
         * @param node the event node
         * @param position the absolute position in quarter notes
         * @param duration the duration in quarter notes
         * @param secondaryPosition the secondary position (ordering of events at the same position)
         */
        EventRef(DoricoTreeNode node, Fraction position, Fraction duration, Fraction secondaryPosition) {
            this.node = node;
            this.position = position;
            this.duration = duration;
            this.secondaryPosition = secondaryPosition;
        }


        /**
         * Gets the event node.
         *
         * @return the node
         */
        DoricoTreeNode getNode() {
            return node;
        }


        /**
         * Gets the absolute position.
         *
         * @return the position in quarter notes
         */
        Fraction getPosition() {
            return position;
        }


        /**
         * Gets the duration.
         *
         * @return the duration in quarter notes
         */
        Fraction getDuration() {
            return duration;
        }


        /**
         * Orders events by position, then by secondary position.
         *
         * @param other the other event
         * @return a negative, zero or positive value
         */
        @Override
        public int compareTo(EventRef other) {
            int result = position.compareTo(other.position);
            if (result != 0) {
                return result;
            }
            return secondaryPosition.compareTo(other.secondaryPosition);
        }


        /**
         * Checks equality by position and secondary position (consistent with {@link #compareTo(EventRef)}).
         *
         * @param obj the other object
         * @return true if both events have the same positions
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof EventRef)) {
                return false;
            }
            EventRef other = (EventRef) obj;
            return position.equals(other.position) && secondaryPosition.equals(other.secondaryPosition);
        }


        /**
         * Calculates the hash code from the positions.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(position, secondaryPosition);
        }
    }


    /**
     * Staves and voices of a part.
     *
     * @author patrick
     */
    static final class PartLayout {
        private final int staves;
        private final List<VoiceLayout> voices;
        private final TreeMap<Fraction, List<ClefImpl>> clefChanges;
        private final TreeMap<Fraction, List<DirectionImpl>> directions;
        private final Fraction[] wedgeEnds;


        /**
         * Constructor for PartLayout.
         *
         * @param staves the number of staves
         */
        PartLayout(int staves) {
            this.staves = staves;
            this.voices = new ArrayList<>();
            this.clefChanges = new TreeMap<>();
            this.directions = new TreeMap<>();
            this.wedgeEnds = new Fraction[DoricoReaderImpl.MAX_SLUR_NUMBER];
        }


        /**
         * Adds a clef change.
         *
         * @param position the position
         * @param clef the clef (with staff number for multi-staff parts)
         */
        void addClef(Fraction position, ClefImpl clef) {
            clefChanges.computeIfAbsent(position, k -> new ArrayList<>()).add(clef);
        }


        /**
         * Gets the clef changes at a position.
         *
         * @param position the position
         * @return the clefs (may be empty)
         */
        List<ClefImpl> getClefs(Fraction position) {
            List<ClefImpl> result = clefChanges.get(position);
            if (result == null) {
                return Collections.emptyList();
            }
            return result;
        }


        /**
         * Gets all clef changes.
         *
         * @return the clef changes by position
         */
        TreeMap<Fraction, List<ClefImpl>> getClefChanges() {
            return clefChanges;
        }


        /**
         * Adds a direction.
         *
         * @param position the position
         * @param direction the direction
         */
        void addDirection(Fraction position, DirectionImpl direction) {
            directions.computeIfAbsent(position, k -> new ArrayList<>()).add(direction);
        }


        /**
         * Gets the directions.
         *
         * @return the directions by position
         */
        TreeMap<Fraction, List<DirectionImpl>> getDirections() {
            return directions;
        }


        /**
         * Allocates a number for a hairpin. Numbers are reused once a hairpin has ended.
         *
         * @param start the start of the hairpin
         * @param end the end of the hairpin
         * @return the number (1-based)
         */
        int allocateWedgeNumber(Fraction start, Fraction end) {
            for (int i = 0; i < wedgeEnds.length; i++) {
                if (wedgeEnds[i] == null || wedgeEnds[i].compareTo(start) <= 0) {
                    wedgeEnds[i] = end;
                    return i + 1;
                }
            }
            return 1;
        }


        /**
         * Gets the number of staves.
         *
         * @return the number of staves
         */
        int getStaves() {
            return staves;
        }


        /**
         * Adds a voice.
         *
         * @param voice the voice
         */
        void addVoice(VoiceLayout voice) {
            voices.add(voice);
        }


        /**
         * Gets the voices whose default stave is the given staff.
         *
         * @param staff the staff number (1-based)
         * @return the voices in order
         */
        List<VoiceLayout> getVoices(int staff) {
            List<VoiceLayout> result = new ArrayList<>();
            for (VoiceLayout voice : voices) {
                if (voice.getStaff() == staff) {
                    result.add(voice);
                }
            }
            return result;
        }
    }


    /**
     * A voice of a part with its chords.
     *
     * @author patrick
     */
    static final class VoiceLayout {
        private final int staff;
        private final int voiceNumber;
        private final boolean primary;
        private List<ChordInfo> chords;
        private final List<TupletInfo> tuplets;


        /**
         * Constructor for VoiceLayout.
         *
         * @param staff the default staff (1-based)
         * @param voiceNumber the MusicXML voice number
         * @param primary true for the first voice of the staff
         */
        VoiceLayout(int staff, int voiceNumber, boolean primary) {
            this.staff = staff;
            this.voiceNumber = voiceNumber;
            this.primary = primary;
            this.chords = new ArrayList<>();
            this.tuplets = new ArrayList<>();
        }


        /**
         * Sets the chords.
         *
         * @param chords the chords sorted by position
         */
        void setChords(List<ChordInfo> chords) {
            this.chords = chords;
        }


        /**
         * Adds a tuplet.
         *
         * @param tuplet the tuplet
         */
        void addTuplet(TupletInfo tuplet) {
            tuplets.add(tuplet);
        }


        /**
         * Finds the innermost tuplet that contains a position.
         *
         * @param position the position
         * @return the tuplet, or null
         */
        TupletInfo findTuplet(Fraction position) {
            TupletInfo result = null;
            for (TupletInfo tuplet : tuplets) {
                if (tuplet.getStart().compareTo(position) <= 0 && tuplet.getEnd().compareTo(position) > 0
                        && (result == null || tuplet.getStart().compareTo(result.getStart()) > 0)) {
                    result = tuplet;
                }
            }
            return result;
        }


        /**
         * Finds the start of the next tuplet after a position.
         *
         * @param position the position
         * @return the start of the next tuplet, or null
         */
        Fraction findNextTupletStart(Fraction position) {
            Fraction result = null;
            for (TupletInfo tuplet : tuplets) {
                if (tuplet.getStart().compareTo(position) > 0 && (result == null || tuplet.getStart().compareTo(result) < 0)) {
                    result = tuplet.getStart();
                }
            }
            return result;
        }


        /**
         * Gets the default staff.
         *
         * @return the staff (1-based)
         */
        int getStaff() {
            return staff;
        }


        /**
         * Gets the MusicXML voice number.
         *
         * @return the voice number
         */
        int getVoiceNumber() {
            return voiceNumber;
        }


        /**
         * Checks whether this is the first voice of its staff.
         *
         * @return true for the primary voice
         */
        boolean isPrimary() {
            return primary;
        }


        /**
         * Gets the chords.
         *
         * @return the chords sorted by position
         */
        List<ChordInfo> getChords() {
            return chords;
        }
    }


    /**
     * Notes of one voice that start at the same position.
     *
     * @author patrick
     */
    static final class ChordInfo {
        private final Fraction start;
        private Fraction end;
        private final List<NoteSpec> notes;
        private final List<Integer> slurStarts;
        private final List<Integer> slurStops;
        private final List<GraceGroup> graces;
        private final List<LyricImpl> lyrics;
        private String fingering;
        private ArticulationsImpl articulations;
        private OrnamentsImpl ornaments;
        private boolean fermata;


        /**
         * Constructor for ChordInfo.
         *
         * @param start the start position
         * @param end the end position
         */
        ChordInfo(Fraction start, Fraction end) {
            this.start = start;
            this.end = end;
            this.notes = new ArrayList<>();
            this.slurStarts = new ArrayList<>();
            this.slurStops = new ArrayList<>();
            this.graces = new ArrayList<>();
            this.lyrics = new ArrayList<>();
        }


        /**
         * Gets the grace note groups played before the chord.
         *
         * @return the grace note groups in order
         */
        List<GraceGroup> getGraces() {
            return graces;
        }


        /**
         * Gets the lyrics of the chord.
         *
         * @return the lyrics
         */
        List<LyricImpl> getLyrics() {
            return lyrics;
        }


        /**
         * Gets the articulations.
         *
         * @return the articulations, or null
         */
        ArticulationsImpl getArticulations() {
            return articulations;
        }


        /**
         * Sets the articulations.
         *
         * @param articulations the articulations (may be null)
         */
        void setArticulations(ArticulationsImpl articulations) {
            this.articulations = articulations;
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
         * Sets the fingering.
         *
         * @param fingering the fingering (numbers separated by spaces), or null
         */
        void setFingering(String fingering) {
            this.fingering = fingering;
        }


        /**
         * Gets the ornaments.
         *
         * @return the ornaments, or null
         */
        OrnamentsImpl getOrnaments() {
            return ornaments;
        }


        /**
         * Sets the ornaments.
         *
         * @param ornaments the ornaments
         */
        void setOrnaments(OrnamentsImpl ornaments) {
            this.ornaments = ornaments;
        }


        /**
         * Checks whether the chord has a fermata.
         *
         * @return true if the chord has a fermata
         */
        boolean isFermata() {
            return fermata;
        }


        /**
         * Sets whether the chord has a fermata.
         *
         * @param fermata true for a fermata
         */
        void setFermata(boolean fermata) {
            this.fermata = fermata;
        }


        /**
         * Gets the start position.
         *
         * @return the start in quarter notes
         */
        Fraction getStart() {
            return start;
        }


        /**
         * Gets the end position.
         *
         * @return the end in quarter notes
         */
        Fraction getEnd() {
            return end;
        }


        /**
         * Sets the end position.
         *
         * @param end the end in quarter notes
         */
        void setEnd(Fraction end) {
            this.end = end;
        }


        /**
         * Adds a note.
         *
         * @param note the note
         */
        void addNote(NoteSpec note) {
            notes.add(note);
        }


        /**
         * Gets the notes.
         *
         * @return the notes
         */
        List<NoteSpec> getNotes() {
            return notes;
        }


        /**
         * Adds a slur starting at this chord.
         *
         * @param number the slur number
         */
        void addSlurStart(int number) {
            slurStarts.add(number);
        }


        /**
         * Adds a slur ending at this chord.
         *
         * @param number the slur number
         */
        void addSlurStop(int number) {
            slurStops.add(number);
        }


        /**
         * Gets the numbers of the slurs starting at this chord.
         *
         * @return the slur numbers
         */
        List<Integer> getSlurStarts() {
            return slurStarts;
        }


        /**
         * Gets the numbers of the slurs ending at this chord.
         *
         * @return the slur numbers
         */
        List<Integer> getSlurStops() {
            return slurStops;
        }
    }


    /**
     * A converted note pitch with an optional staff override (cross-staff notes).
     *
     * @author patrick
     */
    static final class NoteSpec {
        private final PitchImpl pitch;
        private final Integer staff;
        private Fraction end;
        private boolean tiedFromPrevious;
        private boolean tiedToNext;
        private boolean cue;


        /**
         * Constructor for NoteSpec.
         *
         * @param pitch the written pitch
         * @param staff the staff override, or null
         */
        NoteSpec(PitchImpl pitch, Integer staff) {
            this.pitch = pitch;
            this.staff = staff;
        }


        /**
         * Gets the written pitch.
         *
         * @return the pitch
         */
        PitchImpl getPitch() {
            return pitch;
        }


        /**
         * Gets the staff override.
         *
         * @return the staff, or null
         */
        Integer getStaff() {
            return staff;
        }


        /**
         * Gets the end of the note.
         *
         * @return the end (absolute), or null if the note ends with its chord
         */
        Fraction getEnd() {
            return end;
        }


        /**
         * Sets the end of the note.
         *
         * @param end the end (absolute)
         */
        void setEnd(Fraction end) {
            this.end = end;
        }


        /**
         * Checks whether the note continues a note of the chord before (a longer note of a chord).
         *
         * @return true if the note is tied from the note before
         */
        boolean isTiedFromPrevious() {
            return tiedFromPrevious;
        }


        /**
         * Sets whether the note continues a note of the chord before.
         *
         * @param tiedFromPrevious true if the note is tied from the note before
         */
        void setTiedFromPrevious(boolean tiedFromPrevious) {
            this.tiedFromPrevious = tiedFromPrevious;
        }


        /**
         * Checks whether the note is continued in the next chord (it is longer than its chord).
         *
         * @return true if the note is tied to the next note
         */
        boolean isTiedToNext() {
            return tiedToNext;
        }


        /**
         * Sets whether the note is continued in the next chord.
         *
         * @param tiedToNext true if the note is tied to the next note
         */
        void setTiedToNext(boolean tiedToNext) {
            this.tiedToNext = tiedToNext;
        }


        /**
         * Checks whether the note is a small (cue sized) note.
         *
         * @return true for a cue sized note
         */
        boolean isCue() {
            return cue;
        }


        /**
         * Sets whether the note is a small (cue sized) note.
         *
         * @param cue true for a cue sized note
         */
        void setCue(boolean cue) {
            this.cue = cue;
        }
    }


    /**
     * Source data of one converted part (one instrument of a player).
     *
     * @author patrick
     */
    static final class PartSource {
        private final String name;
        private final String abbreviation;
        private final DoricoTreeNode instrument;
        private final DoricoTreeNode entity;
        private final String playerId;


        /**
         * Constructor for PartSource.
         *
         * @param name the part name
         * @param abbreviation the part abbreviation (may be null)
         * @param instrument the score instrument node
         * @param entity the library instrument entity (may be null)
         * @param playerId the Dorico player ID
         */
        PartSource(String name, String abbreviation, DoricoTreeNode instrument, DoricoTreeNode entity, String playerId) {
            this.name = name;
            this.abbreviation = abbreviation;
            this.instrument = instrument;
            this.entity = entity;
            this.playerId = playerId;
        }


        /**
         * Gets the Dorico player ID.
         *
         * @return the player ID
         */
        String getPlayerId() {
            return playerId;
        }


        /**
         * Gets the part name.
         *
         * @return the part name
         */
        String getName() {
            return name;
        }


        /**
         * Gets the part abbreviation.
         *
         * @return the abbreviation, or null
         */
        String getAbbreviation() {
            return abbreviation;
        }


        /**
         * Gets the score instrument node.
         *
         * @return the instrument node
         */
        DoricoTreeNode getInstrument() {
            return instrument;
        }


        /**
         * Gets the library instrument entity.
         *
         * @return the entity, or null
         */
        DoricoTreeNode getEntity() {
            return entity;
        }
    }


    /**
     * Analysed data of a flow: bar grid and divisions per quarter note.
     *
     * @author patrick
     */
    static final class FlowContext {
        private List<BarInfo> bars = new ArrayList<>();
        private int divisions = 1;
        private String title;
        private Map<String, List<EventRef>> streamEvents = new HashMap<>();
        private TreeMap<Fraction, DoricoTreeNode> tempoChanges = new TreeMap<>();
        private TreeMap<Fraction, KeyInfo> keyChanges = new TreeMap<>();
        private Map<String, DoricoTreeNode> flowPlayers = new HashMap<>();
        private final Map<String, DoricoTreeNode> dynamicGroups = new HashMap<>();
        private final Map<String, DoricoTreeNode> lyricElements = new HashMap<>();
        private final TreeMap<Fraction, String> barlineTypes = new TreeMap<>();
        private final List<EventRef> globalEvents = new ArrayList<>();


        /**
         * Gets the flow title.
         *
         * @return the title, or null
         */
        String getTitle() {
            return title;
        }


        /**
         * Sets the flow title.
         *
         * @param title the title (may be null)
         */
        void setTitle(String title) {
            this.title = title;
        }


        /**
         * Gets the dynamic group elements by element ID.
         *
         * @return the dynamic groups
         */
        Map<String, DoricoTreeNode> getDynamicGroups() {
            return dynamicGroups;
        }


        /**
         * Gets the lyric elements by element ID.
         *
         * @return the lyric elements
         */
        Map<String, DoricoTreeNode> getLyricElements() {
            return lyricElements;
        }


        /**
         * Gets the barline types of the bar divisions by position.
         *
         * @return the barline types, for example kEndRepeat
         */
        TreeMap<Fraction, String> getBarlineTypes() {
            return barlineTypes;
        }


        /**
         * Gets the global pause, repeat ending and repeat marker events.
         *
         * @return the events sorted by position
         */
        List<EventRef> getGlobalEvents() {
            return globalEvents;
        }


        /**
         * Gets the events by stream ID.
         *
         * @return the events by stream ID, each list sorted by position
         */
        Map<String, List<EventRef>> getStreamEvents() {
            return streamEvents;
        }


        /**
         * Sets the events by stream ID.
         *
         * @param streamEvents the events by stream ID
         */
        void setStreamEvents(Map<String, List<EventRef>> streamEvents) {
            this.streamEvents = streamEvents;
        }


        /**
         * Gets the tempo change events by position.
         *
         * @return the tempo changes
         */
        TreeMap<Fraction, DoricoTreeNode> getTempoChanges() {
            return tempoChanges;
        }


        /**
         * Sets the tempo change events by position.
         *
         * @param tempoChanges the tempo changes
         */
        void setTempoChanges(TreeMap<Fraction, DoricoTreeNode> tempoChanges) {
            this.tempoChanges = tempoChanges;
        }


        /**
         * Gets the concert key signatures by position.
         *
         * @return the key changes
         */
        TreeMap<Fraction, KeyInfo> getKeyChanges() {
            return keyChanges;
        }


        /**
         * Sets the concert key signatures by position.
         *
         * @param keyChanges the key changes
         */
        void setKeyChanges(TreeMap<Fraction, KeyInfo> keyChanges) {
            this.keyChanges = keyChanges;
        }


        /**
         * Gets the flow players by player ID.
         *
         * @return the flow players
         */
        Map<String, DoricoTreeNode> getFlowPlayers() {
            return flowPlayers;
        }


        /**
         * Sets the flow players by player ID.
         *
         * @param flowPlayers the flow players
         */
        void setFlowPlayers(Map<String, DoricoTreeNode> flowPlayers) {
            this.flowPlayers = flowPlayers;
        }


        /**
         * Gets the bars.
         *
         * @return the bars
         */
        List<BarInfo> getBars() {
            return bars;
        }


        /**
         * Sets the bars.
         *
         * @param bars the bars
         */
        void setBars(List<BarInfo> bars) {
            this.bars = bars;
        }


        /**
         * Gets the divisions per quarter note.
         *
         * @return the divisions
         */
        int getDivisions() {
            return divisions;
        }


        /**
         * Sets the divisions per quarter note.
         *
         * @param divisions the divisions
         */
        void setDivisions(int divisions) {
            this.divisions = divisions;
        }
    }


    /**
     * One bar of the bar grid.
     *
     * @author patrick
     */
    static final class BarInfo {
        private final Fraction start;
        private final Fraction length;
        private final TimeInfo time;
        private final TimeInfo timeChange;
        private final KeyInfo keyChange;
        private final boolean pickup;


        /**
         * Constructor for BarInfo.
         *
         * @param start the start in quarter notes
         * @param length the length in quarter notes
         * @param time the time signature valid in this bar
         * @param timeChange the time signature starting in this bar, or null
         * @param keyChange the key signature starting in this bar, or null
         * @param pickup true if the bar is a pick-up bar (at the start of the flow or after a repeat barline in the middle of a bar)
         */
        BarInfo(Fraction start, Fraction length, TimeInfo time, TimeInfo timeChange, KeyInfo keyChange, boolean pickup) {
            this.start = start;
            this.length = length;
            this.time = time;
            this.timeChange = timeChange;
            this.keyChange = keyChange;
            this.pickup = pickup;
        }


        /**
         * Checks if the bar is a pick-up bar (at the start of the flow or after a repeat barline in the middle of a bar).
         *
         * @return true for a pick-up bar
         */
        boolean isPickup() {
            return pickup;
        }


        /**
         * Gets the start in quarter notes.
         *
         * @return the start
         */
        Fraction getStart() {
            return start;
        }


        /**
         * Gets the length in quarter notes.
         *
         * @return the length
         */
        Fraction getLength() {
            return length;
        }


        /**
         * Gets the time signature valid in this bar.
         *
         * @return the time signature
         */
        TimeInfo getTime() {
            return time;
        }


        /**
         * Gets the time signature starting in this bar.
         *
         * @return the time signature, or null
         */
        TimeInfo getTimeChange() {
            return timeChange;
        }


        /**
         * Gets the key signature starting in this bar.
         *
         * @return the key signature, or null
         */
        KeyInfo getKeyChange() {
            return keyChange;
        }
    }


    /**
     * A tuplet of a voice: its time span and ratio.
     *
     * @author patrick
     */
    static final class TupletInfo {
        private final Fraction start;
        private final Fraction end;
        private final int actual;
        private final int normal;
        private final String normalType;
        private final boolean showNumber;
        private final boolean showBracket;


        /**
         * Constructor for TupletInfo.
         *
         * @param start the start position
         * @param end the end position
         * @param actual the number of notes played (3 for a triplet)
         * @param normal the number of notes in the normal time (2 for a triplet)
         * @param normalType the note type of the tuplet unit, or null
         * @param showNumber true if the number is shown
         * @param showBracket true if the bracket is shown
         */
        TupletInfo(Fraction start, Fraction end, int actual, int normal, String normalType, boolean showNumber, boolean showBracket) {
            this.start = start;
            this.end = end;
            this.actual = actual;
            this.normal = normal;
            this.normalType = normalType;
            this.showNumber = showNumber;
            this.showBracket = showBracket;
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
         * Gets the number of notes played.
         *
         * @return the actual notes
         */
        int getActual() {
            return actual;
        }


        /**
         * Gets the number of notes in the normal time.
         *
         * @return the normal notes
         */
        int getNormal() {
            return normal;
        }


        /**
         * Gets the note type of the tuplet unit.
         *
         * @return the type, or null
         */
        String getNormalType() {
            return normalType;
        }


        /**
         * Checks whether the tuplet number is shown.
         *
         * @return true if the number is shown
         */
        boolean isShowNumber() {
            return showNumber;
        }


        /**
         * Checks whether the tuplet bracket is shown.
         *
         * @return true if the bracket is shown
         */
        boolean isShowBracket() {
            return showBracket;
        }
    }


    /**
     * Grace notes at the same secondary position (a grace chord).
     *
     * @author patrick
     */
    static final class GraceGroup {
        private final Fraction notatedDuration;
        private final boolean slashed;
        private final List<NoteSpec> notes;


        /**
         * Constructor for GraceGroup.
         *
         * @param notatedDuration the written value in quarter notes
         * @param slashed true for an acciaccatura
         */
        GraceGroup(Fraction notatedDuration, boolean slashed) {
            this.notatedDuration = notatedDuration;
            this.slashed = slashed;
            this.notes = new ArrayList<>();
        }


        /**
         * Gets the written value.
         *
         * @return the written value in quarter notes
         */
        Fraction getNotatedDuration() {
            return notatedDuration;
        }


        /**
         * Checks whether the grace notes are slashed.
         *
         * @return true for an acciaccatura
         */
        boolean isSlashed() {
            return slashed;
        }


        /**
         * Adds a note.
         *
         * @param note the note
         */
        void addNote(NoteSpec note) {
            notes.add(note);
        }


        /**
         * Gets the notes.
         *
         * @return the notes
         */
        List<NoteSpec> getNotes() {
            return notes;
        }
    }


    /**
     * A notatable piece with its absolute time span and its tuplet.
     *
     * @author patrick
     */
    static final class TimedPiece {
        private final DoricoDurationConverter.Piece piece;
        private final Fraction start;
        private final Fraction end;
        private final TupletInfo tuplet;


        /**
         * Constructor for TimedPiece.
         *
         * @param piece the written value
         * @param start the start position
         * @param end the end position
         * @param tuplet the tuplet, or null
         */
        TimedPiece(DoricoDurationConverter.Piece piece, Fraction start, Fraction end, TupletInfo tuplet) {
            this.piece = piece;
            this.start = start;
            this.end = end;
            this.tuplet = tuplet;
        }


        /**
         * Gets the written value.
         *
         * @return the piece
         */
        DoricoDurationConverter.Piece getPiece() {
            return piece;
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
         * Gets the tuplet.
         *
         * @return the tuplet, or null
         */
        TupletInfo getTuplet() {
            return tuplet;
        }
    }


    /**
     * The notation marks of one output note.
     *
     * @author patrick
     */
    static final class NoteMarks {
        private boolean tieStart;
        private boolean tieStop;
        private List<Integer> slurStarts = Collections.emptyList();
        private List<Integer> slurStops = Collections.emptyList();
        private TimedPiece tuplet;
        private ChordInfo chord;


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
         * Gets the numbers of the slurs starting at the note.
         *
         * @return the slur numbers
         */
        List<Integer> getSlurStarts() {
            return slurStarts;
        }


        /**
         * Sets the numbers of the slurs starting at the note.
         *
         * @param slurStarts the slur numbers
         */
        void setSlurStarts(List<Integer> slurStarts) {
            this.slurStarts = slurStarts;
        }


        /**
         * Gets the numbers of the slurs ending at the note.
         *
         * @return the slur numbers
         */
        List<Integer> getSlurStops() {
            return slurStops;
        }


        /**
         * Sets the numbers of the slurs ending at the note.
         *
         * @param slurStops the slur numbers
         */
        void setSlurStops(List<Integer> slurStops) {
            this.slurStops = slurStops;
        }


        /**
         * Gets the piece whose tuplet brackets are notated on the note.
         *
         * @return the piece, or null
         */
        TimedPiece getTuplet() {
            return tuplet;
        }


        /**
         * Sets the piece whose tuplet brackets are notated on the note.
         *
         * @param tuplet the piece
         */
        void setTuplet(TimedPiece tuplet) {
            this.tuplet = tuplet;
        }


        /**
         * Gets the chord whose articulations, ornaments and fermata are notated on the note.
         *
         * @return the chord, or null
         */
        ChordInfo getChord() {
            return chord;
        }


        /**
         * Sets the chord whose articulations, ornaments and fermata are notated on the note.
         *
         * @param chord the chord
         */
        void setChord(ChordInfo chord) {
            this.chord = chord;
        }
    }


    /**
     * The pitch conversion data of a part: stave mapping and instrument transposition.
     *
     * @author patrick
     */
    static final class VoiceSource {
        private final Map<String, Integer> staveIdToStaff;
        private final int writtenChromatic;
        private final int writtenDiatonic;


        /**
         * Constructor for VoiceSource.
         *
         * @param staveIdToStaff the staff number by Dorico stave ID
         * @param writtenChromatic the semitones the written pitch lies above the sounding pitch
         * @param writtenDiatonic the diatonic steps the written pitch lies above the sounding pitch
         */
        VoiceSource(Map<String, Integer> staveIdToStaff, int writtenChromatic, int writtenDiatonic) {
            this.staveIdToStaff = staveIdToStaff;
            this.writtenChromatic = writtenChromatic;
            this.writtenDiatonic = writtenDiatonic;
        }


        /**
         * Gets the staff number by Dorico stave ID.
         *
         * @return the mapping
         */
        Map<String, Integer> getStaveIdToStaff() {
            return staveIdToStaff;
        }


        /**
         * Gets the chromatic transposition.
         *
         * @return the semitones the written pitch lies above the sounding pitch
         */
        int getWrittenChromatic() {
            return writtenChromatic;
        }


        /**
         * Gets the diatonic transposition.
         *
         * @return the diatonic steps the written pitch lies above the sounding pitch
         */
        int getWrittenDiatonic() {
            return writtenDiatonic;
        }
    }


    /**
     * The special barlines of a flow: left barlines by bar start, right barlines by bar end.
     *
     * @author patrick
     */
    static final class BarlinePlan {
        private final Map<Fraction, BarlineImpl> leftBarlines = new HashMap<>();
        private final Map<Fraction, BarlineImpl> rightBarlines = new HashMap<>();


        /**
         * Gets or creates the left barline of the bar starting at a position.
         *
         * @param position the bar start
         * @return the barline
         */
        BarlineImpl left(Fraction position) {
            return leftBarlines.computeIfAbsent(position, k -> newBarline("left"));
        }


        /**
         * Gets or creates the right barline of the bar ending at a position.
         *
         * @param position the bar end
         * @return the barline
         */
        BarlineImpl right(Fraction position) {
            return rightBarlines.computeIfAbsent(position, k -> newBarline(DoricoReaderImpl.LOCATION_RIGHT));
        }


        /**
         * Gets the left barline of the bar starting at a position.
         *
         * @param position the bar start
         * @return the barline, or null
         */
        BarlineImpl getLeft(Fraction position) {
            return leftBarlines.get(position);
        }


        /**
         * Gets the right barline of the bar ending at a position.
         *
         * @param position the bar end
         * @return the barline, or null
         */
        BarlineImpl getRight(Fraction position) {
            return rightBarlines.get(position);
        }


        /**
         * Creates a barline.
         *
         * @param location left or right
         * @return the barline
         */
        private static BarlineImpl newBarline(String location) {
            BarlineImpl barline = new BarlineImpl();
            barline.setLocation(location);
            return barline;
        }
    }


    /**
     * A time signature.
     *
     * @author patrick
     */
    static final class TimeInfo {
        private final String beats;
        private final int beatType;
        private final Fraction barLength;


        /**
         * Constructor for TimeInfo.
         *
         * @param beats the numerator (may be additive, for example "2+3")
         * @param beatType the denominator
         * @param barLength the bar length in quarter notes
         */
        TimeInfo(String beats, int beatType, Fraction barLength) {
            this.beats = beats;
            this.beatType = beatType;
            this.barLength = barLength;
        }


        /**
         * Gets the numerator.
         *
         * @return the numerator
         */
        String getBeats() {
            return beats;
        }


        /**
         * Gets the denominator.
         *
         * @return the denominator
         */
        int getBeatType() {
            return beatType;
        }


        /**
         * Gets the bar length in quarter notes.
         *
         * @return the bar length
         */
        Fraction getBarLength() {
            return barLength;
        }


        /**
         * Checks whether another time signature has the same numerator and denominator.
         *
         * @param other the other time signature (may be null)
         * @return true if both are equal in value
         */
        boolean isSameAs(TimeInfo other) {
            return other != null && beatType == other.beatType && beats.equals(other.beats);
        }
    }


    /**
     * A key signature.
     *
     * @author patrick
     */
    static final class KeyInfo {
        private final int fifths;
        private final String mode;


        /**
         * Constructor for KeyInfo.
         *
         * @param fifths the position on the circle of fifths
         * @param mode the MusicXML mode, or null
         */
        KeyInfo(int fifths, String mode) {
            this.fifths = fifths;
            this.mode = mode;
        }


        /**
         * Gets the position on the circle of fifths.
         *
         * @return the fifths
         */
        int getFifths() {
            return fifths;
        }


        /**
         * Gets the MusicXML mode.
         *
         * @return the mode, or null
         */
        String getMode() {
            return mode;
        }
    }


    /**
     * A MusicXML clef description.
     *
     * @author patrick
     */
    static final class ClefInfo {
        private final String sign;
        private final int line;
        private final int octaveChange;


        /**
         * Constructor for ClefInfo.
         *
         * @param sign the clef sign
         * @param line the staff line (0 if not applicable)
         * @param octaveChange the octave change
         */
        ClefInfo(String sign, int line, int octaveChange) {
            this.sign = sign;
            this.line = line;
            this.octaveChange = octaveChange;
        }


        /**
         * Gets the clef sign.
         *
         * @return the sign
         */
        String getSign() {
            return sign;
        }


        /**
         * Gets the staff line.
         *
         * @return the line, 0 if not applicable
         */
        int getLine() {
            return line;
        }


        /**
         * Gets the octave change.
         *
         * @return the octave change
         */
        int getOctaveChange() {
            return octaveChange;
        }
    }
}
