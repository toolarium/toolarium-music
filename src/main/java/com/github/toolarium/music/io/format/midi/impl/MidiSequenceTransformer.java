/*
 * MidiSequenceTransformer.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.midi.impl;

import com.github.toolarium.music.io.format.midi.MidiWriterOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;


/**
 * Transforms a MIDI sequence without changing its sound: a constant tempo with the events moved to their real
 * time (the beats and bars cannot be recognized any more), small random variations of the timing and the
 * velocity of the notes, the parts on randomly chosen channels and all tracks merged into one.
 *
 * @author patrick
 */
public final class MidiSequenceTransformer {
    private static final int META_TEMPO = 0x51;
    private static final int META_END_OF_TRACK = 0x2F;
    private static final long DEFAULT_MPQN = 500_000L;
    private static final double MICROS_PER_MILLI = 1000.0;
    private static final int TEMPO_BYTES = 3;
    private static final int BYTE_MASK = 0xFF;
    private static final int BYTE_BITS = 8;
    private static final int MAX_VELOCITY = 127;
    private static final int NOTES = 128;
    private static final int CHANNELS = 16;
    private static final int DRUM_CHANNEL = 9;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MidiSequenceTransformer INSTANCE = new MidiSequenceTransformer();
    }


    /**
     * Constructor for MidiSequenceTransformer.
     */
    private MidiSequenceTransformer() {
        // NOP
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MidiSequenceTransformer getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Transforms a sequence according to the options.
     *
     * @param sequence the sequence (track 0 is the conductor track with the tempo changes)
     * @param options the options
     * @return the transformed sequence, or the sequence itself if nothing is to transform
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    public Sequence transform(Sequence sequence, MidiWriterOptions options) throws InvalidMidiDataException {
        if (options.isTempoMap() && !options.isSingleTrack() && !options.isChannelMixing() && options.getTimingVariation() == 0
                && options.getVelocityVariation() == 0) {
            return sequence;
        }
        int resolution = sequence.getResolution();
        TreeMap<Long, Long> tempoMap = tempoMap(sequence);
        Random random = new Random(options.getSeed());
        int[] channels = channelMap(options, random);
        Track[] tracks = sequence.getTracks();
        Sequence result = new Sequence(Sequence.PPQ, resolution);
        Track single = null;
        if (options.isSingleTrack()) {
            single = result.createTrack();
        }
        for (Track track : tracks) {
            Track target = single;
            if (target == null) {
                target = result.createTrack();
            }
            copyTrack(track, target, new Object[] {tempoMap, channels}, resolution, options, random);
        }
        if (!options.isTempoMap()) {
            // one constant tempo: the default tempo of 120 quarters per minute
            result.getTracks()[0].add(new MidiEvent(tempoMessage(DEFAULT_MPQN), 0));
        }
        return result;
    }


    /**
     * Copies the events of a track, moved to real time and with the variations.
     *
     * @param source the source track
     * @param target the target track
     * @param maps the tempo (microseconds per quarter) by tick and the new channel by channel
     * @param resolution the resolution
     * @param options the options
     * @param random the random numbers
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    @SuppressWarnings("unchecked")
    private void copyTrack(Track source, Track target, Object[] maps, int resolution, MidiWriterOptions options, Random random)
            throws InvalidMidiDataException {
        TreeMap<Long, Long> tempoMap = (TreeMap<Long, Long>) maps[0];
        int[] channels = (int[]) maps[1];
        // the start of the sounding notes and the end of the last notes by channel and note: a note never ends
        // before it starts and never starts before the note before it ended
        Map<Integer, Long> starts = new HashMap<>();
        Map<Integer, Long> ends = new HashMap<>();
        List<MidiEvent> events = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            events.add(source.get(i));
        }
        for (MidiEvent event : events) {
            MidiMessage message = event.getMessage();
            if (message instanceof MetaMessage) {
                int type = ((MetaMessage) message).getType();
                if (type == META_END_OF_TRACK || (type == META_TEMPO && !options.isTempoMap())) {
                    continue;
                }
            }
            long tick = event.getTick();
            if (!options.isTempoMap()) {
                tick = realTimeTick(tick, tempoMap, resolution);
            }
            MidiMessage copy = message;
            if (message instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) message;
                int key = shortMessage.getChannel() * NOTES + shortMessage.getData1();
                int channel = channels[shortMessage.getChannel()];
                if (shortMessage.getCommand() < ShortMessage.MIDI_TIME_CODE) {
                    ShortMessage moved = new ShortMessage();
                    moved.setMessage(shortMessage.getCommand(), channel, shortMessage.getData1(), shortMessage.getData2());
                    copy = moved;
                }
                boolean noteOn = shortMessage.getCommand() == ShortMessage.NOTE_ON && shortMessage.getData2() > 0;
                boolean noteOff = shortMessage.getCommand() == ShortMessage.NOTE_OFF
                        || (shortMessage.getCommand() == ShortMessage.NOTE_ON && shortMessage.getData2() == 0);
                if (noteOn || noteOff) {
                    tick = Math.max(0, tick + variation(random, options.getTimingVariation(), tick, tempoMap, resolution, options.isTempoMap()));
                }
                if (noteOn) {
                    int velocity = shortMessage.getData2();
                    if (options.getVelocityVariation() > 0) {
                        velocity += random.nextInt(2 * options.getVelocityVariation() + 1) - options.getVelocityVariation();
                    }
                    ShortMessage on = new ShortMessage();
                    on.setMessage(ShortMessage.NOTE_ON, channel, shortMessage.getData1(), Math.max(1, Math.min(MAX_VELOCITY, velocity)));
                    copy = on;
                    tick = Math.max(tick, ends.getOrDefault(key, 0L));
                    starts.put(key, tick);
                } else if (noteOff && starts.containsKey(key)) {
                    tick = Math.max(starts.remove(key) + 1, tick);
                    ends.put(key, tick);
                }
            }
            target.add(new MidiEvent(copy, tick));
        }
    }


    /**
     * Gets the new channel of every channel: a random order of the channels without the drum channel 10, or the
     * channels themselves if the channels are not mixed.
     *
     * @param options the options
     * @param random the random numbers
     * @return the new channel by channel
     */
    private int[] channelMap(MidiWriterOptions options, Random random) {
        int[] result = new int[CHANNELS];
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < CHANNELS; i++) {
            result[i] = i;
            if (i != DRUM_CHANNEL) {
                free.add(i);
            }
        }
        if (!options.isChannelMixing()) {
            return result;
        }
        Collections.shuffle(free, random);
        int next = 0;
        for (int i = 0; i < CHANNELS; i++) {
            if (i != DRUM_CHANNEL) {
                result[i] = free.get(next++);
            }
        }
        return result;
    }


    /**
     * Gets a random change of the time of a note.
     *
     * @param random the random numbers
     * @param millis the largest change in milliseconds
     * @param tick the tick of the note
     * @param tempoMap the tempo by tick
     * @param resolution the resolution
     * @param musicalTime true if the ticks follow the tempo map, false for real time at the default tempo
     * @return the change in ticks
     */
    private long variation(Random random, int millis, long tick, TreeMap<Long, Long> tempoMap, int resolution, boolean musicalTime) {
        if (millis <= 0) {
            return 0;
        }
        long mpqn = DEFAULT_MPQN;
        if (musicalTime && tempoMap.floorEntry(tick) != null) {
            mpqn = tempoMap.floorEntry(tick).getValue();
        }
        double ticksPerMilli = resolution * MICROS_PER_MILLI / mpqn;
        double change = (random.nextDouble() * 2 - 1) * millis * ticksPerMilli;
        return Math.round(change);
    }


    /**
     * Gets the tick in real time at the default tempo of a tick in the tempo map.
     *
     * @param tick the tick in the tempo map
     * @param tempoMap the tempo (microseconds per quarter) by tick
     * @param resolution the resolution
     * @return the tick at the default tempo
     */
    private long realTimeTick(long tick, TreeMap<Long, Long> tempoMap, int resolution) {
        double micros = 0;
        long last = 0;
        long mpqn = DEFAULT_MPQN;
        for (Map.Entry<Long, Long> tempo : tempoMap.entrySet()) {
            if (tempo.getKey() >= tick) {
                break;
            }
            micros += (double) (tempo.getKey() - last) * mpqn / resolution;
            last = tempo.getKey();
            mpqn = tempo.getValue();
        }
        micros += (double) (tick - last) * mpqn / resolution;
        return Math.round(micros * resolution / DEFAULT_MPQN);
    }


    /**
     * Reads the tempo changes of a sequence.
     *
     * @param sequence the sequence
     * @return the tempo (microseconds per quarter) by tick
     */
    private TreeMap<Long, Long> tempoMap(Sequence sequence) {
        TreeMap<Long, Long> result = new TreeMap<>();
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiMessage message = track.get(i).getMessage();
                if (message instanceof MetaMessage && ((MetaMessage) message).getType() == META_TEMPO) {
                    byte[] data = ((MetaMessage) message).getData();
                    long mpqn = 0;
                    for (int b = 0; b < TEMPO_BYTES && b < data.length; b++) {
                        mpqn = (mpqn << BYTE_BITS) | (data[b] & BYTE_MASK);
                    }
                    result.put(track.get(i).getTick(), mpqn);
                }
            }
        }
        return result;
    }


    /**
     * Creates a tempo message.
     *
     * @param mpqn the microseconds per quarter
     * @return the message
     * @throws InvalidMidiDataException if MIDI data is invalid
     */
    private MetaMessage tempoMessage(long mpqn) throws InvalidMidiDataException {
        byte[] data = {(byte) (mpqn >> (2 * BYTE_BITS)), (byte) (mpqn >> BYTE_BITS), (byte) mpqn};
        MetaMessage message = new MetaMessage();
        message.setMessage(META_TEMPO, data, data.length);
        return message;
    }
}
