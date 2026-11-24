/*
 * InstrumentSoundConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.converter;


/**
 * Converts a General MIDI program into the standard MusicXML instrument sound (for example brass.trumpet), which
 * notation applications use to choose the instrument when they import a score.
 *
 * @author patrick
 */
public final class InstrumentSoundConverter {
    private static final String PIANO_ELECTRIC = "keyboard.piano.electric";
    private static final String GUITAR_ELECTRIC = "pluck.guitar.electric";
    private static final String BASS_ELECTRIC = "pluck.bass.electric";
    private static final String STRINGS = "strings.group";
    private static final String STRINGS_SYNTH = "strings.group.synth";
    private static final String BRASS_SYNTH = "brass.group.synth";
    private static final String TRUMPET = "brass.trumpet";
    private static final String CLARINET = "wind.reed.clarinet";
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int C = 0;
    private static final int D = 2;
    private static final int E_FLAT = 3;
    private static final int A = 9;
    private static final int B_FLAT = 10;

    // the sounds of the General MIDI programs 1 to 128, null for a program without standard sound
    private static final String[] SOUNDS = {
        "keyboard.piano.grand", "keyboard.piano", PIANO_ELECTRIC, "keyboard.piano.honky-tonk", PIANO_ELECTRIC, PIANO_ELECTRIC,
        "keyboard.harpsichord", "keyboard.clavichord",
        "keyboard.celesta", "pitched-percussion.glockenspiel", "pitched-percussion.music-box", "pitched-percussion.vibraphone",
        "pitched-percussion.marimba", "pitched-percussion.xylophone", "pitched-percussion.tubular-bells", "pitched-percussion.hammer-dulcimer",
        "keyboard.organ.drawbar", "keyboard.organ.percussive", "keyboard.organ.rotary", "keyboard.organ.pipe", "keyboard.organ.reed",
        "wind.reed.accordion", "wind.reed.harmonica", "wind.reed.bandoneon",
        "pluck.guitar.nylon-string", "pluck.guitar.steel-string", GUITAR_ELECTRIC, GUITAR_ELECTRIC, GUITAR_ELECTRIC, GUITAR_ELECTRIC,
        GUITAR_ELECTRIC, GUITAR_ELECTRIC,
        "pluck.bass.acoustic", BASS_ELECTRIC, BASS_ELECTRIC, "pluck.bass.fretless", BASS_ELECTRIC, BASS_ELECTRIC, "pluck.bass.synth",
        "pluck.bass.synth",
        "strings.violin", "strings.viola", "strings.cello", "strings.contrabass", STRINGS, STRINGS, "pluck.harp", "drum.timpani",
        STRINGS, STRINGS, STRINGS_SYNTH, STRINGS_SYNTH, "voice.aa", "voice.oo", "voice.synth", null,
        "brass.trumpet", "brass.trombone", "brass.tuba", "brass.trumpet", "brass.french-horn", "brass.group", BRASS_SYNTH, BRASS_SYNTH,
        "wind.reed.saxophone.soprano", "wind.reed.saxophone.alto", "wind.reed.saxophone.tenor", "wind.reed.saxophone.baritone",
        "wind.reed.oboe", "wind.reed.english-horn", "wind.reed.bassoon", "wind.reed.clarinet",
        "wind.flutes.flute.piccolo", "wind.flutes.flute", "wind.flutes.recorder", "wind.flutes.panpipes", "wind.flutes.blown-bottle",
        "wind.flutes.shakuhachi", "wind.flutes.whistle", "wind.flutes.ocarina",
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null,
        "pluck.sitar", "pluck.banjo", "pluck.shamisen", "pluck.koto", "pitched-percussion.kalimba", "wind.pipes.bagpipes",
        "strings.fiddle", "wind.reed.shenai",
        null, "metal.bells.agogo", "pitched-percussion.steel-drums", "wood.wood-block", "drum.taiko", "drum.tom-tom", "drum.tom-tom.synth",
        "metal.cymbal.reverse",
        null, null, null, null, null, null, null, null};


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final InstrumentSoundConverter INSTANCE = new InstrumentSoundConverter();
    }


    /**
     * Constructor for InstrumentSoundConverter.
     */
    private InstrumentSoundConverter() {
        // NOP
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static InstrumentSoundConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the standard MusicXML sound of a General MIDI program.
     *
     * @param program the General MIDI program, 1 to 128
     * @return the sound, for example brass.trumpet, or null if the program has no standard sound
     */
    public String getSound(int program) {
        if (program < 1 || program > SOUNDS.length) {
            return null;
        }
        return SOUNDS[program - 1];
    }


    /**
     * Gets the standard MusicXML sound of a General MIDI program played by a transposing instrument: trumpets and
     * clarinets get the sound of their key (for example brass.trumpet.bflat), which notation applications need to
     * choose the transposing instrument.
     *
     * @param program the General MIDI program, 1 to 128
     * @param chromatic the transposition from written to sounding pitch in semitones (for example -2 for B flat)
     * @return the sound, or null if the program has no standard sound
     */
    public String getSound(int program, int chromatic) {
        String sound = getSound(program);
        int key = Math.floorMod(chromatic, SEMITONES_PER_OCTAVE);
        if (TRUMPET.equals(sound)) {
            if (key == B_FLAT) {
                return TRUMPET + ".bflat";
            } else if (key == C) {
                return TRUMPET + ".c";
            } else if (key == D) {
                return TRUMPET + ".d";
            }
        } else if (CLARINET.equals(sound)) {
            if (key == B_FLAT) {
                return CLARINET + ".bflat";
            } else if (key == A) {
                return CLARINET + ".a";
            } else if (key == E_FLAT) {
                return CLARINET + ".eflat";
            }
        }
        return sound;
    }


    /**
     * Gets the General MIDI program of a standard MusicXML sound. A more specific sound (for example
     * brass.trumpet.bflat) gets the program of its general sound (brass.trumpet).
     *
     * @param sound the sound, or null
     * @return the General MIDI program, 1 to 128, or 0 if the sound has no program
     */
    public int getProgram(String sound) {
        String name = sound;
        while (name != null && !name.isEmpty()) {
            for (int i = 0; i < SOUNDS.length; i++) {
                if (name.equals(SOUNDS[i])) {
                    return i + 1;
                }
            }
            int dot = name.lastIndexOf('.');
            if (dot < 0) {
                break;
            }
            name = name.substring(0, dot);
        }
        return 0;
    }

}
