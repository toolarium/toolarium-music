/*
 * MidiNoteConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.midi.impl;

import com.github.toolarium.music.model.note.IPitch;
import com.github.toolarium.music.model.note.impl.PitchImpl;


/**
 * Converts between MIDI note numbers and the {@link IPitch} model representation.
 * MIDI note 60 corresponds to middle C (C4). Accidentals default to sharps.
 *
 * @author patrick
 */
public final class MidiNoteConverter {

    private static final String[] STEP_NAMES = {"C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B"};
    private static final double[] STEP_ALTERS = {0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0};
    private static final int SEMITONE_C = 0;
    private static final int SEMITONE_D = 2;
    private static final int SEMITONE_E = 4;
    private static final int SEMITONE_F = 5;
    private static final int SEMITONE_G = 7;
    private static final int SEMITONE_A = 9;
    private static final int SEMITONE_B = 11;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int MIDI_OCTAVE_OFFSET = 1;


    /**
     * Private constructor for utility class.
     */
    private MidiNoteConverter() {
        // utility class
    }


    /**
     * Converts a MIDI note number (0-127) to a pitch representation.
     *
     * @param midiNote the MIDI note number
     * @return the pitch with step, alter, and octave
     */
    public static IPitch midiNoteToPitch(int midiNote) {
        int clampedNote = Math.max(0, Math.min(127, midiNote));
        int semitone = clampedNote % SEMITONES_PER_OCTAVE;
        int octave = (clampedNote / SEMITONES_PER_OCTAVE) - MIDI_OCTAVE_OFFSET;

        PitchImpl pitch = new PitchImpl();
        pitch.setStep(STEP_NAMES[semitone]);
        pitch.setOctave(octave);
        double alter = STEP_ALTERS[semitone];
        if (alter != 0) {
            pitch.setAlter(alter);
        }
        return pitch;
    }


    /**
     * Converts a pitch representation to a MIDI note number.
     *
     * @param pitch the pitch to convert
     * @return the MIDI note number (0-127)
     */
    public static int pitchToMidiNote(IPitch pitch) {
        if (pitch == null || pitch.getStep() == null) {
            return 60;
        }

        int semitone = stepToSemitone(pitch.getStep());
        int octave = 4;
        if (pitch.getOctave() != null) {
            octave = pitch.getOctave();
        }

        int alter = 0;
        if (pitch.getAlter() != null) {
            alter = (int) Math.round(pitch.getAlter());
        }

        int midiNote = (octave + MIDI_OCTAVE_OFFSET) * SEMITONES_PER_OCTAVE + semitone + alter;
        return Math.max(0, Math.min(127, midiNote));
    }


    /**
     * Converts a step letter to its semitone offset within an octave.
     *
     * @param step the step letter (C, D, E, F, G, A, B)
     * @return the semitone offset
     */
    private static int stepToSemitone(String step) {
        if (step == null || step.isEmpty()) {
            return SEMITONE_C;
        }

        switch (step.charAt(0)) {
            case 'C': return SEMITONE_C;
            case 'D': return SEMITONE_D;
            case 'E': return SEMITONE_E;
            case 'F': return SEMITONE_F;
            case 'G': return SEMITONE_G;
            case 'A': return SEMITONE_A;
            case 'B': return SEMITONE_B;
            default: return SEMITONE_C;
        }
    }
}
