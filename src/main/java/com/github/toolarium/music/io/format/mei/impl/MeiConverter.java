/*
 * MeiConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.util.Locale;


/**
 * Converts MEI attribute values (durations, accidentals, articulations, clefs and key signatures)
 * into the values of the score model.
 *
 * @author patrick
 */
final class MeiConverter {
    private static final String EMPTY = "";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MeiConverter INSTANCE = new MeiConverter();
    }


    /**
     * Constructor.
     */
    private MeiConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    static MeiConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the length of an MEI duration without dots.
     *
     * @param dur the MEI duration (1, 2, 4, 8, ..., breve, long, maxima)
     * @return the length in whole notes, or null if invalid
     */
    Fraction durationOf(String dur) {
        if (dur == null || dur.isEmpty()) {
            return null;
        }
        switch (dur) {
            case "breve":
                return Fraction.of(2, 1);
            case "long":
                return Fraction.of(4, 1);
            case "maxima":
                return Fraction.of(8, 1);
            default:
                break;
        }
        try {
            int value = Integer.parseInt(dur.trim());
            if (value <= 0 || Integer.bitCount(value) != 1) {
                return null;
            }
            return Fraction.of(1, value);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Adds dots to a length.
     *
     * @param base the undotted length
     * @param dots the number of dots
     * @return the dotted length
     */
    Fraction dotted(Fraction base, int dots) {
        Fraction result = base;
        Fraction add = base;
        for (int i = 0; i < dots; i++) {
            add = add.multiply(1, 2);
            result = result.add(add);
        }
        return result;
    }


    /**
     * Gets the MusicXML note type of an MEI duration.
     *
     * @param dur the MEI duration
     * @return the note type, or null
     */
    String typeOf(String dur) {
        if (dur == null) {
            return null;
        }
        switch (dur) {
            case "maxima":
                return "maxima";
            case "long":
                return "long";
            case "breve":
                return "breve";
            case "1":
                return "whole";
            case "2":
                return "half";
            case "4":
                return "quarter";
            case "8":
                return "eighth";
            case "16":
                return "16th";
            case "32":
                return "32nd";
            case "64":
                return "64th";
            case "128":
                return "128th";
            case "256":
                return "256th";
            case "512":
                return "512th";
            case "1024":
                return "1024th";
            default:
                return null;
        }
    }


    /**
     * Gets the alteration of an MEI accidental value.
     *
     * @param accid the accidental (s, f, ss, x, ff, n, su, fd and so on)
     * @return the alteration in semitones, or null if the value is unknown
     */
    Integer alterOf(String accid) {
        if (accid == null) {
            return null;
        }
        switch (accid) {
            case "s":
            case "ns":
            case "su":
            case "sd":
                return 1;
            case "f":
            case "nf":
            case "fu":
            case "fd":
                return -1;
            case "ss":
            case "x":
            case "xs":
            case "sx":
                return 2;
            case "ff":
                return -2;
            case "ts":
            case "sxs":
                return 3;
            case "tf":
                return -3;
            case "n":
            case "nu":
            case "nd":
                return 0;
            default:
                return null;
        }
    }


    /**
     * Gets the MusicXML accidental of an MEI accidental value.
     *
     * @param accid the MEI accidental
     * @return the MusicXML accidental, or null
     */
    String accidentalName(String accid) {
        if (accid == null) {
            return null;
        }
        switch (accid) {
            case "s":
                return "sharp";
            case "f":
                return "flat";
            case "ss":
                return "sharp-sharp";
            case "x":
                return "double-sharp";
            case "ff":
                return "flat-flat";
            case "n":
                return "natural";
            case "ns":
                return "natural-sharp";
            case "nf":
                return "natural-flat";
            case "ts":
                return "triple-sharp";
            case "tf":
                return "triple-flat";
            case "su":
                return "sharp-up";
            case "sd":
                return "sharp-down";
            case "fu":
                return "flat-up";
            case "fd":
                return "flat-down";
            case "nu":
                return "natural-up";
            case "nd":
                return "natural-down";
            default:
                return null;
        }
    }


    /**
     * Parses an MEI key signature value such as {@code 2s}, {@code 3f} or {@code 0}.
     *
     * @param sig the key signature
     * @return the fifths, or null if invalid
     */
    Integer fifthsOf(String sig) {
        if (sig == null) {
            return null;
        }
        String text = sig.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty() || "mixed".equals(text)) {
            return null;
        }
        if ("0".equals(text) || "none".equals(text)) {
            return 0;
        }
        try {
            int count = Integer.parseInt(text.substring(0, text.length() - 1));
            if (text.endsWith("s")) {
                return count;
            }
            if (text.endsWith("f")) {
                return -count;
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Creates a clef from MEI shape, line and displacement values.
     *
     * @param shape the clef shape (G, F, C, perc, TAB, GG)
     * @param line the line, or null
     * @param dis the octave displacement (8, 15, 22), or null
     * @param disPlace the displacement direction (above or below), or null
     * @return the clef, or null if the shape is missing
     */
    ClefImpl clefOf(String shape, String line, String dis, String disPlace) {
        if (shape == null) {
            return null;
        }
        ScoreBuilder builder = ScoreBuilder.getInstance();
        int octave = 0;
        if (dis != null) {
            int steps = 0;
            switch (dis.trim()) {
                case "8":
                    steps = 1;
                    break;
                case "15":
                    steps = 2;
                    break;
                case "22":
                    steps = 3;
                    break;
                default:
                    break;
            }
            if ("below".equals(disPlace)) {
                octave = -steps;
            } else {
                octave = steps;
            }
        }
        Integer lineValue = null;
        if (line != null) {
            try {
                lineValue = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                lineValue = null;
            }
        }
        switch (shape) {
            case "G":
                return builder.createClef("G", valueOr(lineValue, 2), octave);
            case "GG":
                return builder.createClef("G", valueOr(lineValue, 2), -1);
            case "F":
                return builder.createClef("F", valueOr(lineValue, 4), octave);
            case "C":
                return builder.createClef("C", valueOr(lineValue, 3), octave);
            case "perc":
                return builder.createClef("percussion", null, 0);
            case "TAB":
                return builder.createClef("TAB", null, 0);
            default:
                return null;
        }
    }


    /**
     * Applies a list of MEI articulations (for example {@code stacc acc}) to an event.
     *
     * @param artic the articulation list
     * @param event the event
     */
    void applyArticulations(String artic, EventData event) {
        if (artic == null) {
            return;
        }
        for (String value : artic.trim().split("\\s+")) {
            applyArticulation(value, event);
        }
    }


    /**
     * Applies one MEI articulation.
     *
     * @param value the articulation
     * @param event the event
     */
    private void applyArticulation(String value, EventData event) {
        switch (value) {
            case "acc":
            case "acc-soft":
                event.articulations().setAccent(EMPTY);
                break;
            case "stacc":
                event.articulations().setStaccato(EMPTY);
                break;
            case "ten":
                event.articulations().setTenuto(EMPTY);
                break;
            case "ten-stacc":
                event.articulations().setDetachedLegato(EMPTY);
                break;
            case "stacciss":
                event.articulations().setStaccatissimo(EMPTY);
                break;
            case "marc":
            case "marc-stacc":
                event.articulations().setStrongAccent(EMPTY);
                break;
            case "spicc":
                event.articulations().setSpiccato(EMPTY);
                break;
            case "stress":
                event.articulations().setStress(EMPTY);
                break;
            case "unstress":
                event.articulations().setUnstress(EMPTY);
                break;
            case "dnbow":
                event.technical().setDownBow(EMPTY);
                break;
            case "upbow":
                event.technical().setUpBow(EMPTY);
                break;
            case "harm":
                event.technical().setHarmonic(EMPTY);
                break;
            case "snap":
                event.technical().setSnapPizzicato(EMPTY);
                break;
            case "stop":
                event.technical().setStopped(EMPTY);
                break;
            case "open":
                event.technical().setOpenString(EMPTY);
                break;
            case "dbltongue":
                event.technical().setDoubleTongue(EMPTY);
                break;
            case "trpltongue":
                event.technical().setTripleTongue(EMPTY);
                break;
            default:
                break;
        }
    }


    /**
     * Returns a value or a default.
     *
     * @param value the value (may be null)
     * @param defaultValue the default
     * @return the value or the default
     */
    private int valueOr(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
