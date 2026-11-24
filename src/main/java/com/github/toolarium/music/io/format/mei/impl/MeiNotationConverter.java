/*
 * MeiNotationConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl;

import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterControl;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.direction.IMetronome;
import com.github.toolarium.music.model.measure.IClef;
import com.github.toolarium.music.model.notation.IArticulations;
import com.github.toolarium.music.model.notation.IDynamics;
import com.github.toolarium.music.model.notation.INotations;
import com.github.toolarium.music.model.notation.IOrnaments;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Converts notation of the model into MEI values: durations, accidentals, articulations, ornaments, dynamics,
 * hairpins, octave lines, pedals, tempo, clefs and barlines.
 *
 * @author patrick
 */
public final class MeiNotationConverter {
    private static final String FORM = "form";
    private static final String UPPER = "upper";
    private static final String LOWER = "lower";
    private static final String QUARTER = "4";
    private static final int QUARTER_UNIT = 4;
    private static final Map<String, String> DURATIONS = createDurations();
    private static final Map<String, String> ACCIDENTALS = createAccidentals();
    private static final Map<String, String> BAR_RENDITIONS = createBarRenditions();
    private static final Map<String, String> HEAD_SHAPES = createHeadShapes();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MeiNotationConverter INSTANCE = new MeiNotationConverter();
    }


    /**
     * Constructor.
     */
    private MeiNotationConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MeiNotationConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the MEI duration of a MusicXML note type.
     *
     * @param type the note type (for example quarter or 16th)
     * @return the MEI duration (for example 4 or 16), or null if unknown
     */
    public String duration(String type) {
        if (type == null) {
            return null;
        }
        return DURATIONS.get(type);
    }


    /**
     * Gets the MEI written accidental of a MusicXML accidental.
     *
     * @param accidental the MusicXML accidental (for example sharp or flat-flat)
     * @return the MEI accidental, or null
     */
    public String accidental(String accidental) {
        if (accidental == null) {
            return null;
        }
        return ACCIDENTALS.get(accidental);
    }


    /**
     * Gets the MEI gestural accidental of an alteration.
     *
     * @param alter the alteration in semitones, or null for none
     * @return the MEI accidental (n without alteration), or null for an alteration without MEI value
     */
    public String gesturalAccidental(Double alter) {
        if (alter == null || alter == 0) {
            return "n";
        }
        if (alter == 1) {
            return "s";
        }
        if (alter == -1) {
            return "f";
        }
        if (alter == 2) {
            return "ss";
        }
        if (alter == -2) {
            return "ff";
        }
        if (alter == 0.5) {
            return "qs";
        }
        if (alter == -0.5) {
            return "qf";
        }
        return null;
    }


    /**
     * Gets the MEI articulations of a note.
     *
     * @param notations the notations, or null
     * @return the articulations separated by spaces, or null
     */
    public String articulations(INotations notations) {
        if (notations == null || notations.getArticulations() == null) {
            return null;
        }
        IArticulations articulations = notations.getArticulations();
        List<String> result = new ArrayList<>();
        addIf(result, articulations.getAccent(), "acc");
        addIf(result, articulations.getStrongAccent(), "marc");
        addIf(result, articulations.getStaccato(), "stacc");
        addIf(result, articulations.getTenuto(), "ten");
        addIf(result, articulations.getStaccatissimo(), "stacciss");
        addIf(result, articulations.getSpiccato(), "spicc");
        addIf(result, articulations.getStress(), "stress");
        addIf(result, articulations.getUnstress(), "unstress");
        if (articulations.getDetachedLegato() != null) {
            // MEI has no portato: staccato and tenuto
            addOnce(result, "stacc");
            addOnce(result, "ten");
        }
        if (result.isEmpty()) {
            return null;
        }
        return String.join(" ", result);
    }


    /**
     * Gets the fermata of a note.
     *
     * @param notations the notations, or null
     * @return above, below (inverted) or null without fermata
     */
    public String fermata(INotations notations) {
        if (notations == null || notations.getFermatas() == null || notations.getFermatas().isEmpty()) {
            return null;
        }
        if ("inverted".equals(notations.getFermatas().get(0).getType())) {
            return "below";
        }
        return "above";
    }


    /**
     * Gets the ornaments of a note as control events (without staff and start note).
     *
     * @param notations the notations
     * @return the trills, mordents and turns
     */
    public List<MeiWriterControl> ornaments(INotations notations) {
        List<MeiWriterControl> result = new ArrayList<>();
        IOrnaments ornaments = notations.getOrnaments();
        if (ornaments == null) {
            return result;
        }
        if (ornaments.getTrillMark() != null) {
            result.add(new MeiWriterControl("trill"));
        }
        if (ornaments.getMordent() != null) {
            result.add(new MeiWriterControl("mordent").put(FORM, LOWER));
        }
        if (ornaments.getInvertedMordent() != null) {
            result.add(new MeiWriterControl("mordent").put(FORM, UPPER));
        }
        if (ornaments.getTurn() != null) {
            result.add(new MeiWriterControl("turn").put(FORM, UPPER));
        }
        if (ornaments.getInvertedTurn() != null) {
            result.add(new MeiWriterControl("turn").put(FORM, LOWER));
        }
        if (ornaments.getDelayedTurn() != null) {
            result.add(new MeiWriterControl("turn").put(FORM, UPPER).put("delayed", "true"));
        }
        if (ornaments.getDelayedInvertedTurn() != null) {
            result.add(new MeiWriterControl("turn").put(FORM, LOWER).put("delayed", "true"));
        }
        return result;
    }


    /**
     * Gets the marks of a note as control events (without staff and start note): fingering, breath mark, caesura
     * and dynamics attached to the note.
     *
     * @param notations the notations
     * @return the control events
     */
    public List<MeiWriterControl> noteMarks(INotations notations) {
        List<MeiWriterControl> result = new ArrayList<>();
        if (notations.getTechnical() != null && notations.getTechnical().getFingering() != null
                && !notations.getTechnical().getFingering().trim().isEmpty()) {
            MeiWriterControl fingering = new MeiWriterControl("fing");
            fingering.setText(notations.getTechnical().getFingering().trim());
            result.add(fingering);
        }
        if (notations.getArticulations() != null && notations.getArticulations().getBreathMark() != null) {
            result.add(new MeiWriterControl("breath"));
        }
        if (notations.getArticulations() != null && notations.getArticulations().getCaesura() != null) {
            result.add(new MeiWriterControl("caesura"));
        }
        String dynamics = dynamics(notations.getDynamics());
        if (dynamics != null) {
            MeiWriterControl dynam = new MeiWriterControl("dynam");
            dynam.setText(dynamics);
            result.add(dynam);
        }
        return result;
    }


    /**
     * Gets the text of dynamics.
     *
     * @param dynamics the dynamics, or null
     * @return the text (for example mf or sfz), or null
     */
    public String dynamics(IDynamics dynamics) {
        if (dynamics == null) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put("pppp", dynamics.getPppp());
        values.put("ppp", dynamics.getPpp());
        values.put("pp", dynamics.getPp());
        values.put("p", dynamics.getPiano());
        values.put("mp", dynamics.getMp());
        values.put("mf", dynamics.getMf());
        values.put("f", dynamics.getForte());
        values.put("ff", dynamics.getFf());
        values.put("fff", dynamics.getFff());
        values.put("ffff", dynamics.getFfff());
        values.put("sf", dynamics.getSf());
        values.put("sfp", dynamics.getSfp());
        values.put("sfz", dynamics.getSfz());
        values.put("fp", dynamics.getFp());
        values.put("rf", dynamics.getRf());
        values.put("rfz", dynamics.getRfz());
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                return entry.getKey();
            }
        }
        if (dynamics.getOtherDynamics() != null && !dynamics.getOtherDynamics().trim().isEmpty()) {
            return dynamics.getOtherDynamics().trim();
        }
        return null;
    }


    /**
     * Creates the hairpin of a wedge start.
     *
     * @param type the MusicXML wedge type
     * @param staff the MEI staff number
     * @param tstamp the time stamp
     * @param place the placement, or null
     * @return the hairpin (without end), or null for a stop or continue
     */
    public MeiWriterControl wedge(String type, String staff, String tstamp, String place) {
        String form = null;
        if ("crescendo".equals(type)) {
            form = "cres";
        } else if ("diminuendo".equals(type)) {
            form = "dim";
        }
        if (form == null) {
            return null;
        }
        return new MeiWriterControl("hairpin").put(FORM, form).put(MeiMeasurePlanner.STAFF, staff).put(MeiMeasurePlanner.TSTAMP, tstamp)
                .put(MeiMeasurePlanner.PLACE, place);
    }


    /**
     * Creates the octave line of an octave shift start: a MusicXML shift down (the notes are written lower) is an
     * octave line above the staff.
     *
     * @param type the MusicXML octave shift type (up, down, stop or continue)
     * @param size the size (8 or 15), or null for 8
     * @param staff the MEI staff number
     * @param tstamp the time stamp
     * @return the octave line (without end), or null for a stop or continue
     */
    public MeiWriterControl octave(String type, Integer size, String staff, String tstamp) {
        String place = null;
        if ("down".equals(type)) {
            place = "above";
        } else if ("up".equals(type)) {
            place = "below";
        }
        if (place == null) {
            return null;
        }
        String dis = "8";
        if (size != null && (size == 15 || size == 22)) {
            dis = String.valueOf(size);
        }
        return new MeiWriterControl("octave").put("dis", dis).put("dis.place", place).put(MeiMeasurePlanner.STAFF, staff)
                .put(MeiMeasurePlanner.TSTAMP, tstamp);
    }


    /**
     * Gets the direction of a pedal mark.
     *
     * @param type the MusicXML pedal type
     * @return down, up, bounce or null
     */
    public String pedal(String type) {
        if ("start".equals(type)) {
            return "down";
        }
        if ("stop".equals(type)) {
            return "up";
        }
        if ("change".equals(type)) {
            return "bounce";
        }
        return null;
    }


    /**
     * Sets the metronome mark and the MIDI tempo on a tempo control event.
     *
     * @param control the tempo control event
     * @param type the direction type with the metronome, or null
     * @param soundTempo the MIDI tempo in quarter notes per minute, or null
     */
    public void applyMetronome(MeiWriterControl control, IDirectionType type, String soundTempo) {
        BigDecimal bpm = number(soundTempo);
        if (type != null && type.getMetronome() != null) {
            IMetronome metronome = type.getMetronome();
            BigDecimal mm = number(metronome.getPerMinute());
            String unit = duration(metronome.getBeatUnit());
            if (unit == null) {
                unit = QUARTER;
            }
            boolean dotted = metronome.getBeatUnitDot() != null;
            if (mm != null) {
                control.put("mm", mm.stripTrailingZeros().toPlainString()).put("mm.unit", unit);
                if (dotted) {
                    control.put("mm.dots", "1");
                }
                if (bpm == null && isNumeric(unit)) {
                    BigDecimal factor = BigDecimal.valueOf(QUARTER_UNIT).divide(new BigDecimal(unit), 6, RoundingMode.HALF_UP);
                    if (dotted) {
                        factor = factor.multiply(new BigDecimal("1.5"));
                    }
                    bpm = mm.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        if (bpm != null && bpm.signum() > 0) {
            control.put("midi.bpm", bpm.stripTrailingZeros().toPlainString());
        }
    }


    /**
     * Gets the MEI bar rendition of a MusicXML bar style.
     *
     * @param barStyle the bar style, or null
     * @return the bar rendition, or null for a regular barline
     */
    public String barRendition(String barStyle) {
        if (barStyle == null) {
            return null;
        }
        return BAR_RENDITIONS.get(barStyle);
    }


    /**
     * Gets the MEI head shape of a MusicXML notehead.
     *
     * @param notehead the notehead, or null
     * @return the head shape, or null for a normal notehead
     */
    public String headShape(String notehead) {
        if (notehead == null) {
            return null;
        }
        return HEAD_SHAPES.get(notehead);
    }


    /**
     * Gets the clef attributes (shape, line, dis, dis.place) of a clef.
     *
     * @param clef the clef, or null for a treble clef
     * @param prefix the attribute prefix ("clef." in a staff definition, empty in a clef element)
     * @return the attribute names and values in order
     */
    public List<String> clefAttributes(IClef clef, String prefix) {
        List<String> result = new ArrayList<>();
        String sign = "G";
        Integer line = null;
        int octave = 0;
        if (clef != null && clef.getSign() != null) {
            sign = clef.getSign();
            line = clef.getLine();
            if (clef.getClefOctaveChange() != null) {
                octave = clef.getClefOctaveChange();
            }
        }
        if ("percussion".equals(sign)) {
            result.add(prefix + "shape");
            result.add("perc");
            return result;
        }
        if ("TAB".equals(sign) || "tab".equals(sign)) {
            result.add(prefix + "shape");
            result.add("TAB");
            return result;
        }
        if (!"G".equals(sign) && !"F".equals(sign) && !"C".equals(sign)) {
            sign = "G";
            line = null;
        }
        if (line == null) {
            line = defaultLine(sign);
        }
        result.add(prefix + "shape");
        result.add(sign);
        result.add(prefix + "line");
        result.add(String.valueOf(line));
        if (octave != 0) {
            result.add(prefix + "dis");
            if (Math.abs(octave) >= 2) {
                result.add("15");
            } else {
                result.add("8");
            }
            result.add(prefix + "dis.place");
            if (octave > 0) {
                result.add("above");
            } else {
                result.add("below");
            }
        }
        return result;
    }


    /**
     * Gets the default line of a clef sign.
     *
     * @param sign G, F or C
     * @return the line
     */
    private int defaultLine(String sign) {
        if ("F".equals(sign)) {
            return 4;
        }
        if ("C".equals(sign)) {
            return 3;
        }
        return 2;
    }


    /**
     * Adds a value if a mark is set.
     *
     * @param result the values
     * @param mark the mark, or null
     * @param value the value to add
     */
    private void addIf(List<String> result, String mark, String value) {
        if (mark != null) {
            result.add(value);
        }
    }


    /**
     * Adds a value if it is not in the list yet.
     *
     * @param result the values
     * @param value the value to add
     */
    private void addOnce(List<String> result, String value) {
        if (!result.contains(value)) {
            result.add(value);
        }
    }


    /**
     * Parses a number from a text (for example "c. 120" is 120).
     *
     * @param text the text, or null
     * @return the number, or null
     */
    private BigDecimal number(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("[^0-9.]", "");
        if (digits.isEmpty() || digits.startsWith(".") || digits.indexOf('.') != digits.lastIndexOf('.')) {
            return null;
        }
        BigDecimal value = new BigDecimal(digits);
        if (value.signum() <= 0) {
            return null;
        }
        return value;
    }


    /**
     * Checks whether a text is a positive integer.
     *
     * @param text the text
     * @return true for digits only
     */
    private boolean isNumeric(String text) {
        return text.matches("[0-9]+");
    }


    /**
     * Creates the MEI durations of the MusicXML note types.
     *
     * @return the duration by note type
     */
    private static Map<String, String> createDurations() {
        Map<String, String> result = new HashMap<>();
        result.put("maxima", "maxima");
        result.put("long", "long");
        result.put("breve", "breve");
        result.put("whole", "1");
        result.put("half", "2");
        result.put("quarter", QUARTER);
        result.put("eighth", "8");
        result.put("16th", "16");
        result.put("32nd", "32");
        result.put("64th", "64");
        result.put("128th", "128");
        result.put("256th", "256");
        result.put("512th", "512");
        result.put("1024th", "1024");
        return result;
    }


    /**
     * Creates the MEI accidentals of the MusicXML accidentals.
     *
     * @return the MEI accidental by MusicXML accidental
     */
    private static Map<String, String> createAccidentals() {
        Map<String, String> result = new HashMap<>();
        result.put("sharp", "s");
        result.put("flat", "f");
        result.put("natural", "n");
        result.put("double-sharp", "x");
        result.put("sharp-sharp", "ss");
        result.put("flat-flat", "ff");
        result.put("natural-sharp", "ns");
        result.put("natural-flat", "nf");
        result.put("quarter-flat", "qf");
        result.put("quarter-sharp", "qs");
        result.put("three-quarters-flat", "3qf");
        result.put("three-quarters-sharp", "3qs");
        result.put("triple-sharp", "ts");
        result.put("triple-flat", "tf");
        return result;
    }


    /**
     * Creates the MEI bar renditions of the MusicXML bar styles.
     *
     * @return the bar rendition by bar style
     */
    private static Map<String, String> createBarRenditions() {
        Map<String, String> result = new HashMap<>();
        result.put("light-heavy", "end");
        result.put("light-light", "dbl");
        result.put("heavy-heavy", "dblheavy");
        result.put("heavy", "heavy");
        result.put("dashed", "dashed");
        result.put("dotted", "dotted");
        result.put("none", "invis");
        return result;
    }


    /**
     * Creates the MEI head shapes of the MusicXML noteheads.
     *
     * @return the head shape by notehead
     */
    private static Map<String, String> createHeadShapes() {
        Map<String, String> result = new HashMap<>();
        result.put("x", "x");
        result.put("diamond", "diamond");
        result.put("slash", "slash");
        result.put("square", "square");
        return result;
    }
}
