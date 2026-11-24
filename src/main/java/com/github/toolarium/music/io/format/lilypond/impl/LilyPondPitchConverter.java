/*
 * LilyPondPitchConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondPitch;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.HashMap;
import java.util.Map;


/**
 * Converts LilyPond pitch names, octaves, keys, clefs and chord names: note names in the languages nederlands (the
 * default), english and deutsch, absolute and relative octaves, keys with their mode, clef names and chord names.
 *
 * @author patrick
 */
public final class LilyPondPitchConverter {
    /** The language of the note names nederlands (c, cis, es, bes). */
    public static final String NEDERLANDS = "nederlands";

    /** The language of the note names english (c, cs, ef, bf). */
    public static final String ENGLISH = "english";

    /** The language of the note names deutsch (c, cis, es, h, b). */
    public static final String DEUTSCH = "deutsch";

    private static final String STEPS = "CDEFGAB";
    private static final String C_SIGN = "C";
    private static final int STEPS_PER_OCTAVE = 7;
    private static final int OCTAVE_OF_C = 3;
    private static final int MAX_DISTANCE = 3;
    private static final int[] MAJOR_FIFTHS = {0, 2, 4, -1, 1, 3, 5};
    private static final int FIFTHS_PER_ALTER = 7;
    private static final Map<String, Integer> MODES = createModes();
    private static final Map<String, Object[]> CLEFS = createClefs();
    private static final Map<String, String> CHORD_SUFFIXES = createChordSuffixes();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final LilyPondPitchConverter INSTANCE = new LilyPondPitchConverter();
    }


    /**
     * Constructor.
     */
    private LilyPondPitchConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static LilyPondPitchConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Parses a note name.
     *
     * @param name the name, for example cis, bes or es (nederlands)
     * @param language the language of the note names (nederlands, english or deutsch)
     * @return the step index (0 for C to 6 for B) and the alteration, or null if the name is no note name
     */
    public int[] parseNoteName(String name, String language) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        char first = name.charAt(0);
        final String rest = name.substring(1);
        int step = STEPS.indexOf(Character.toUpperCase(first));
        if (Character.isUpperCase(first)) {
            return null;
        }
        if (DEUTSCH.equals(language) && first == 'h') {
            step = STEPS.indexOf('B');
        } else if (DEUTSCH.equals(language) && "b".equals(name)) {
            return new int[] {STEPS.indexOf('B'), -1};
        }
        if (step < 0) {
            return null;
        }
        Integer alter;
        if (ENGLISH.equals(language)) {
            alter = englishAlter(rest);
        } else {
            alter = dutchAlter(first, rest);
        }
        if (alter == null) {
            return null;
        }
        return new int[] {step, alter};
    }


    /**
     * Creates the pitch of a note with an absolute octave: c is the C below the middle C, c' the middle C.
     *
     * @param note the step index and the alteration
     * @param octaveMarks the octave marks (+1 for each ', -1 for each ,)
     * @return the pitch
     */
    public LilyPondPitch absolute(int[] note, int octaveMarks) {
        return new LilyPondPitch(String.valueOf(STEPS.charAt(note[0])), note[1], OCTAVE_OF_C + octaveMarks);
    }


    /**
     * Creates the pitch of a note in relative mode: the octave is chosen so that the note is at most a fourth away
     * from the previous pitch, then the octave marks move it.
     *
     * @param reference the previous pitch
     * @param note the step index and the alteration
     * @param octaveMarks the octave marks (+1 for each ', -1 for each ,)
     * @return the pitch
     */
    public LilyPondPitch relative(LilyPondPitch reference, int[] note, int octaveMarks) {
        int ref = reference.getDiatonic();
        int diatonic = Math.floorDiv(ref, STEPS_PER_OCTAVE) * STEPS_PER_OCTAVE + note[0];
        if (diatonic - ref > MAX_DISTANCE) {
            diatonic -= STEPS_PER_OCTAVE;
        } else if (ref - diatonic > MAX_DISTANCE) {
            diatonic += STEPS_PER_OCTAVE;
        }
        return LilyPondPitch.ofDiatonic(diatonic + octaveMarks * STEPS_PER_OCTAVE, note[1]);
    }


    /**
     * Transposes a pitch by an interval.
     *
     * @param pitch the pitch
     * @param steps the interval in diatonic steps
     * @param semitones the interval in semitones
     * @return the transposed pitch
     */
    public LilyPondPitch transpose(LilyPondPitch pitch, int steps, int semitones) {
        LilyPondPitch moved = LilyPondPitch.ofDiatonic(pitch.getDiatonic() + steps, 0);
        int alter = semitones - (semitoneOf(moved) - semitoneOf(new LilyPondPitch(pitch.getStep(), 0, pitch.getOctave())));
        return new LilyPondPitch(moved.getStep(), pitch.getAlter() + alter, moved.getOctave());
    }


    /**
     * Gets the semitone of a pitch (the MIDI note number).
     *
     * @param pitch the pitch
     * @return the semitone
     */
    public int semitoneOf(LilyPondPitch pitch) {
        int[] scale = {0, 2, 4, 5, 7, 9, 11};
        return (pitch.getOctave() + 1) * 12 + scale[STEPS.indexOf(pitch.getStep())] + pitch.getAlter();
    }


    /**
     * Gets the fifths of a key.
     *
     * @param tonic the step index and the alteration of the tonic
     * @param mode the mode without backslash (major, minor, dorian, ...), or null for major
     * @return the fifths (positive for sharps, negative for flats)
     */
    public int keyFifths(int[] tonic, String mode) {
        int offset = 0;
        if (mode != null && MODES.containsKey(mode)) {
            offset = MODES.get(mode);
        }
        return MAJOR_FIFTHS[tonic[0]] + tonic[1] * FIFTHS_PER_ALTER + offset;
    }


    /**
     * Checks whether a word is a mode of a key.
     *
     * @param mode the mode without backslash
     * @return true for major, minor and the church modes
     */
    public boolean isMode(String mode) {
        return MODES.containsKey(mode);
    }


    /**
     * Creates the clef of a clef name: treble, bass, alto, tenor, ... with an optional octave (treble_8, bass^15).
     *
     * @param name the clef name
     * @return the clef, or null if the name is unknown
     */
    public ClefImpl clef(String name) {
        String base = name;
        int octaveChange = 0;
        int index = Math.max(name.indexOf('_'), name.indexOf('^'));
        if (index > 0) {
            base = name.substring(0, index);
            String number = name.substring(index + 1).replace("(", "").replace(")", "");
            int octaves = 1;
            if ("15".equals(number)) {
                octaves = 2;
            }
            if (name.charAt(index) == '_') {
                octaveChange = -octaves;
            } else {
                octaveChange = octaves;
            }
        }
        Object[] clef = CLEFS.get(base);
        if (clef == null) {
            return null;
        }
        return ScoreBuilder.getInstance().createClef((String) clef[0], (Integer) clef[1], octaveChange);
    }


    /**
     * Creates the text of a chord name (\chordmode), for example Am7 or G7/B.
     *
     * @param root the step index and the alteration of the root
     * @param modifier the modifier after the colon (m, 7, maj7, dim, ...), or null
     * @param bass the step index and the alteration of the bass after the slash, or null
     * @return the chord text
     */
    public String chordText(int[] root, String modifier, int[] bass) {
        StringBuilder text = new StringBuilder(noteText(root));
        if (modifier != null && !modifier.isEmpty()) {
            String suffix = CHORD_SUFFIXES.get(modifier);
            if (suffix == null) {
                suffix = modifier;
            }
            text.append(suffix);
        }
        if (bass != null) {
            text.append('/').append(noteText(bass));
        }
        return text.toString();
    }


    /**
     * Gets the text of a note for a chord symbol: the step and # or b.
     *
     * @param note the step index and the alteration
     * @return the text, for example F# or Bb
     */
    private String noteText(int[] note) {
        StringBuilder text = new StringBuilder().append(STEPS.charAt(note[0]));
        for (int i = 0; i < Math.abs(note[1]); i++) {
            if (note[1] > 0) {
                text.append('#');
            } else {
                text.append('b');
            }
        }
        return text.toString();
    }


    /**
     * Gets the alteration of a nederlands or deutsch note name.
     *
     * @param first the letter of the note
     * @param rest the suffix after the letter
     * @return the alteration, or null if the suffix is no alteration
     */
    private Integer dutchAlter(char first, String rest) {
        String suffix = rest;
        // es, eses, as, ases: the e of the flat suffix is missing after the vowels e and a
        if ((first == 'e' || first == 'a') && suffix.startsWith("s")) {
            suffix = "e" + suffix;
        }
        int alter = 0;
        while (!suffix.isEmpty()) {
            if (suffix.startsWith("is")) {
                alter++;
            } else if (suffix.startsWith("es")) {
                alter--;
            } else {
                return null;
            }
            suffix = suffix.substring(2);
        }
        return alter;
    }


    /**
     * Gets the alteration of an english note name.
     *
     * @param rest the suffix after the letter
     * @return the alteration, or null if the suffix is no alteration
     */
    private Integer englishAlter(String rest) {
        switch (rest) {
            case "":
                return 0;
            case "s":
            case "sharp":
                return 1;
            case "ss":
            case "x":
            case "sharpsharp":
                return 2;
            case "f":
            case "flat":
                return -1;
            case "ff":
            case "flatflat":
                return -2;
            default:
                return null;
        }
    }


    /**
     * Creates the fifths offset of the modes relative to major.
     *
     * @return the offset by mode
     */
    private static Map<String, Integer> createModes() {
        Map<String, Integer> result = new HashMap<>();
        result.put("major", 0);
        result.put("ionian", 0);
        result.put("dorian", -2);
        result.put("phrygian", -4);
        result.put("lydian", 1);
        result.put("mixolydian", -1);
        result.put("minor", -3);
        result.put("aeolian", -3);
        result.put("locrian", -5);
        return result;
    }


    /**
     * Creates the sign and line of the clef names.
     *
     * @return the sign and line by clef name
     */
    private static Map<String, Object[]> createClefs() {
        Map<String, Object[]> result = new HashMap<>();
        Object[] treble = {"G", 2};
        result.put("treble", treble);
        result.put("violin", treble);
        result.put("G", treble);
        result.put("G2", treble);
        result.put("french", new Object[] {"G", 1});
        Object[] bass = {"F", 4};
        result.put("bass", bass);
        result.put("F", bass);
        result.put("varbaritone", new Object[] {"F", 3});
        result.put("subbass", new Object[] {"F", 5});
        Object[] alto = {C_SIGN, 3};
        result.put("alto", alto);
        result.put(C_SIGN, alto);
        result.put("tenor", new Object[] {C_SIGN, 4});
        result.put("soprano", new Object[] {C_SIGN, 1});
        result.put("mezzosoprano", new Object[] {C_SIGN, 2});
        result.put("baritone", new Object[] {C_SIGN, 5});
        Object[] percussion = {"percussion", null};
        result.put("percussion", percussion);
        result.put("tab", new Object[] {"TAB", null});
        return result;
    }


    /**
     * Creates the chord symbol suffixes of the chord modifiers.
     *
     * @return the suffix by modifier
     */
    private static Map<String, String> createChordSuffixes() {
        Map<String, String> result = new HashMap<>();
        result.put("m", "m");
        result.put("min", "m");
        result.put("maj", "maj7");
        result.put("maj7", "maj7");
        result.put("m7", "m7");
        result.put("dim", "dim");
        result.put("dim7", "dim7");
        result.put("aug", "aug");
        result.put("sus", "sus4");
        result.put("sus4", "sus4");
        result.put("sus2", "sus2");
        result.put("m7.5-", "m7b5");
        result.put("m7+", "mMaj7");
        result.put("1.5", "5");
        return result;
    }
}
