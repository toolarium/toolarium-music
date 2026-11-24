/*
 * LilyPondInstrumentConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl;

import java.util.Locale;


/**
 * Converts between the General MIDI programs and the LilyPond MIDI instrument names (midiInstrument), for example
 * 57 and "trumpet".
 *
 * @author patrick
 */
public final class LilyPondInstrumentConverter {
    private static final String[] NAMES = {
        "acoustic grand", "bright acoustic", "electric grand", "honky-tonk", "electric piano 1", "electric piano 2", "harpsichord", "clav",
        "celesta", "glockenspiel", "music box", "vibraphone", "marimba", "xylophone", "tubular bells", "dulcimer",
        "drawbar organ", "percussive organ", "rock organ", "church organ", "reed organ", "accordion", "harmonica", "concertina",
        "acoustic guitar (nylon)", "acoustic guitar (steel)", "electric guitar (jazz)", "electric guitar (clean)", "electric guitar (muted)",
        "overdriven guitar", "distorted guitar", "guitar harmonics",
        "acoustic bass", "electric bass (finger)", "electric bass (pick)", "fretless bass", "slap bass 1", "slap bass 2", "synth bass 1",
        "synth bass 2",
        "violin", "viola", "cello", "contrabass", "tremolo strings", "pizzicato strings", "orchestral harp", "timpani",
        "string ensemble 1", "string ensemble 2", "synthstrings 1", "synthstrings 2", "choir aahs", "voice oohs", "synth voice", "orchestra hit",
        "trumpet", "trombone", "tuba", "muted trumpet", "french horn", "brass section", "synthbrass 1", "synthbrass 2",
        "soprano sax", "alto sax", "tenor sax", "baritone sax", "oboe", "english horn", "bassoon", "clarinet",
        "piccolo", "flute", "recorder", "pan flute", "blown bottle", "shakuhachi", "whistle", "ocarina",
        "lead 1 (square)", "lead 2 (sawtooth)", "lead 3 (calliope)", "lead 4 (chiff)", "lead 5 (charang)", "lead 6 (voice)", "lead 7 (fifths)",
        "lead 8 (bass+lead)",
        "pad 1 (new age)", "pad 2 (warm)", "pad 3 (polysynth)", "pad 4 (choir)", "pad 5 (bowed)", "pad 6 (metallic)", "pad 7 (halo)",
        "pad 8 (sweep)",
        "fx 1 (rain)", "fx 2 (soundtrack)", "fx 3 (crystal)", "fx 4 (atmosphere)", "fx 5 (brightness)", "fx 6 (goblins)", "fx 7 (echoes)",
        "fx 8 (sci-fi)",
        "sitar", "banjo", "shamisen", "koto", "kalimba", "bagpipe", "fiddle", "shanai",
        "tinkle bell", "agogo", "steel drums", "woodblock", "taiko drum", "melodic tom", "synth drum", "reverse cymbal",
        "guitar fret noise", "breath noise", "seashore", "bird tweet", "telephone ring", "helicopter", "applause", "gunshot"
    };


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final LilyPondInstrumentConverter INSTANCE = new LilyPondInstrumentConverter();
    }


    /**
     * Constructor.
     */
    private LilyPondInstrumentConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static LilyPondInstrumentConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the LilyPond MIDI instrument name of a General MIDI program.
     *
     * @param program the program (1 to 128)
     * @return the name, or null if the program is invalid
     */
    public String getName(Integer program) {
        if (program == null || program < 1 || program > NAMES.length) {
            return null;
        }
        return NAMES[program - 1];
    }


    /**
     * Gets the General MIDI program of a LilyPond MIDI instrument name.
     *
     * @param name the name (case is ignored)
     * @return the program (1 to 128), or null if the name is unknown
     */
    public Integer getProgram(String name) {
        if (name == null) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(key)) {
                return i + 1;
            }
        }
        return null;
    }
}
