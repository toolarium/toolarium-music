/*
 * CapellaConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.SpelledPitch;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Converts capella values (clefs, time signatures, durations, tuplets, pitches and articulations)
 * into the values of the score model.
 *
 * @author patrick
 */
final class CapellaConverter {
    private static final Pattern CLEF = Pattern.compile("^([GFC])([1-5])([+-]?)$");
    private static final Pattern PITCH = Pattern.compile("^([A-Ga-g])(-?\\d+)$");
    private static final String EMPTY = "";
    private static final int THREE = 3;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final CapellaConverter INSTANCE = new CapellaConverter();
    }


    /**
     * Constructor.
     */
    private CapellaConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    static CapellaConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Creates a clef from a capella clef name: treble, bass, alto, tenor, soprano, mezzoSoprano,
     * baritone or a sign with line and octave such as {@code G2}, {@code F4} or {@code G2-}.
     *
     * @param name the clef name
     * @return the clef, or null if unknown
     */
    ClefImpl clefOf(String name) {
        if (name == null) {
            return null;
        }
        ScoreBuilder builder = ScoreBuilder.getInstance();
        switch (name) {
            case "treble":
                return builder.createClef("G", 2, 0);
            case "bass":
                return builder.createClef("F", 4, 0);
            case "alto":
                return builder.createClef("C", 3, 0);
            case "tenor":
                return builder.createClef("C", 4, 0);
            case "soprano":
                return builder.createClef("C", 1, 0);
            case "mezzoSoprano":
                return builder.createClef("C", 2, 0);
            case "baritone":
                return builder.createClef("F", 3, 0);
            case "percussion":
                return builder.createClef("percussion", null, 0);
            default:
                break;
        }
        Matcher matcher = CLEF.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        int octave = 0;
        if ("+".equals(matcher.group(3))) {
            octave = 1;
        } else if ("-".equals(matcher.group(3))) {
            octave = -1;
        }
        return builder.createClef(matcher.group(1), Integer.parseInt(matcher.group(2)), octave);
    }


    /**
     * Parses a capella time signature: {@code 4/4}, {@code C}, {@code allaBreve} or {@code infinite}.
     *
     * @param time the time signature
     * @return beats and beat type (0 beats for free time), or null if invalid
     */
    int[] timeOf(String time) {
        if (time == null) {
            return null;
        }
        switch (time) {
            case "C":
            case "common":
                return new int[] {4, 4, 1};
            case "allaBreve":
            case "C|":
                return new int[] {2, 2, 2};
            case "infinite":
                return new int[] {0, 4, 0};
            default:
                break;
        }
        int slash = time.indexOf('/');
        if (slash <= 0) {
            return null;
        }
        try {
            int beats = Integer.parseInt(time.substring(0, slash).trim());
            int beatType = Integer.parseInt(time.substring(slash + 1).trim());
            if (beats <= 0 || beatType <= 0) {
                return null;
            }
            return new int[] {beats, beatType, 0};
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Gets the length of a duration base such as {@code 1/4} or {@code 2/1}.
     *
     * @param base the base
     * @return the length in whole notes, or null if invalid
     */
    Fraction baseOf(String base) {
        Fraction value = Fraction.parse(base);
        if (value == null || !value.isPositive()) {
            return null;
        }
        return value;
    }


    /**
     * Gets the normal notes of a capella tuplet: the largest power of two below the count, or the
     * largest multiple of three times a power of two for tripartite tuplets. Prolonged tuplets use
     * the next value above the count.
     *
     * @param count the number of notes
     * @param tripartite true for a tuplet in a ternary context
     * @param prolong true for a prolonged tuplet
     * @return the normal notes
     */
    int tupletNormal(int count, boolean tripartite, boolean prolong) {
        int unit = 1;
        if (tripartite) {
            unit = THREE;
        }
        int value = unit;
        if (prolong) {
            while (value <= count) {
                value *= 2;
            }
            return value;
        }
        if (value >= count) {
            return value;
        }
        while (value * 2 < count) {
            value *= 2;
        }
        return value;
    }


    /**
     * Parses a capella pitch such as {@code C5} (capella octave 5 is the octave of middle C).
     *
     * @param pitch the pitch
     * @return the pitch with the MusicXML octave (no alteration), or null if invalid
     */
    SpelledPitch pitchOf(String pitch) {
        if (pitch == null) {
            return null;
        }
        Matcher matcher = PITCH.matcher(pitch.trim());
        if (!matcher.matches()) {
            return null;
        }
        return new SpelledPitch(matcher.group(1).toUpperCase(), 0, Integer.parseInt(matcher.group(2)) - 1);
    }


    /**
     * Applies a capella articulation to an event.
     *
     * @param type the articulation type
     * @param event the event
     */
    void applyArticulation(String type, EventData event) {
        if (type == null) {
            return;
        }
        switch (type) {
            case "staccato":
                event.articulations().setStaccato(EMPTY);
                break;
            case "tenuto":
                event.articulations().setTenuto(EMPTY);
                break;
            case "staccTenuto":
            case "portato":
                event.articulations().setDetachedLegato(EMPTY);
                break;
            case "staccatissimo":
                event.articulations().setStaccatissimo(EMPTY);
                break;
            case "normalAccent":
            case "accent":
                event.articulations().setAccent(EMPTY);
                break;
            case "strongAccent":
                event.articulations().setStrongAccent(EMPTY);
                break;
            case "weakBeat":
                event.articulations().setUnstress(EMPTY);
                break;
            case "strongBeat":
                event.articulations().setStress(EMPTY);
                break;
            case "fermata":
                event.getFermatas().add(ScoreBuilder.getInstance().createFermata(null, false));
                break;
            default:
                break;
        }
    }
}
