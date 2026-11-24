/*
 * MuseScoreSymbolConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Maps MuseScore symbol and type names to their MusicXML counterparts and back.
 * Covers clefs, barlines, accidentals, noteheads, dynamics, fermatas, ottavas, articulations,
 * ornaments, technical marks and chord symbol kinds.
 *
 * @author patrick
 */
public final class MuseScoreSymbolConverter {

    /** Articulation group: the mark is a MusicXML articulation. */
    public static final String GROUP_ARTICULATION = "articulation";

    /** Articulation group: the mark is a MusicXML technical mark. */
    public static final String GROUP_TECHNICAL = "technical";

    /** Articulation group: the mark is a MusicXML ornament. */
    public static final String GROUP_ORNAMENT = "ornament";

    private static final String ABOVE = "Above";
    private static final String BELOW = "Below";
    private static final String OTTAVA_8VA = "8va";
    private static final String KIND_MAJOR_SEVENTH = "major-seventh";
    private static final String KIND_HALF_DIMINISHED = "half-diminished";

    private static final Map<String, String> BARLINE_TO_XML;
    private static final Map<String, String> BARLINE_FROM_XML;
    private static final Map<String, String> ACCIDENTAL_TO_XML;
    private static final Map<String, String> ACCIDENTAL_FROM_XML;
    private static final Map<String, String> NOTEHEAD_TO_XML;
    private static final Map<String, String> NOTEHEAD_FROM_XML;
    private static final Map<String, String[]> ARTICULATION_TO_XML;
    private static final Map<String, String> HARMONY_TO_KIND;
    private static final Map<String, String> HARMONY_FROM_KIND;
    private static final String[] DYNAMICS = {"pppppp", "ppppp", "pppp", "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff", "ffff",
                                              "fffff", "ffffff", "fp", "pf", "sf", "sfz", "sff", "sffz", "sfp", "sfpp", "rfz", "rf",
                                              "fz", "m", "r", "s", "z", "n"};
    private static final String[] OTTAVA_NAMES = {OTTAVA_8VA, "8vb", "15ma", "15mb", "22ma", "22mb"};

    static {
        Map<String, String> barline = new HashMap<>();
        barline.put("double", "light-light");
        barline.put("end", "light-heavy");
        barline.put("reverse-end", "heavy-light");
        barline.put("heavy", "heavy");
        barline.put("double-heavy", "heavy-heavy");
        barline.put("dashed", "dashed");
        barline.put("dotted", "dotted");
        barline.put("normal", "regular");
        BARLINE_TO_XML = Collections.unmodifiableMap(barline);
        BARLINE_FROM_XML = Collections.unmodifiableMap(invert(barline));

        Map<String, String> accidental = new HashMap<>();
        accidental.put("accidentalSharp", "sharp");
        accidental.put("accidentalFlat", "flat");
        accidental.put("accidentalNatural", "natural");
        accidental.put("accidentalDoubleSharp", "double-sharp");
        accidental.put("accidentalDoubleFlat", "flat-flat");
        accidental.put("accidentalTripleSharp", "triple-sharp");
        accidental.put("accidentalTripleFlat", "triple-flat");
        accidental.put("accidentalNaturalSharp", "natural-sharp");
        accidental.put("accidentalNaturalFlat", "natural-flat");
        accidental.put("accidentalSharpSharp", "sharp-sharp");
        accidental.put("accidentalQuarterToneFlatStein", "quarter-flat");
        accidental.put("accidentalQuarterToneSharpStein", "quarter-sharp");
        accidental.put("accidentalThreeQuarterTonesFlatZimmermann", "three-quarters-flat");
        accidental.put("accidentalThreeQuarterTonesSharpStein", "three-quarters-sharp");
        ACCIDENTAL_FROM_XML = Collections.unmodifiableMap(invert(accidental));
        accidental.put("sharp", "sharp");
        accidental.put("flat", "flat");
        accidental.put("natural", "natural");
        accidental.put("double sharp", "double-sharp");
        accidental.put("double flat", "flat-flat");
        ACCIDENTAL_TO_XML = Collections.unmodifiableMap(accidental);

        Map<String, String> notehead = new HashMap<>();
        notehead.put("cross", "x");
        notehead.put("plus", "cross");
        notehead.put("xcircle", "circle-x");
        notehead.put("triangle-up", "triangle");
        notehead.put("triangle-down", "inverted triangle");
        notehead.put("slashed1", "slashed");
        notehead.put("slashed2", "back slashed");
        notehead.put("diamond", "diamond");
        notehead.put("circled", "circled");
        notehead.put("large-arrow", "arrow up");
        notehead.put("slash", "slash");
        notehead.put("do", "do");
        notehead.put("re", "re");
        notehead.put("mi", "mi");
        notehead.put("fa", "fa");
        notehead.put("sol", "so");
        notehead.put("la", "la");
        notehead.put("ti", "ti");
        NOTEHEAD_FROM_XML = Collections.unmodifiableMap(invert(notehead));
        notehead.put("withx", "x");
        notehead.put("diamond-old", "diamond");
        notehead.put("circled-large", "circled");
        NOTEHEAD_TO_XML = Collections.unmodifiableMap(notehead);

        Map<String, String[]> articulation = new HashMap<>();
        articulation.put("articStaccato", new String[] {GROUP_ARTICULATION, "staccato"});
        articulation.put("articAccent", new String[] {GROUP_ARTICULATION, "accent"});
        articulation.put("articTenuto", new String[] {GROUP_ARTICULATION, "tenuto"});
        articulation.put("articMarcato", new String[] {GROUP_ARTICULATION, "strong-accent"});
        articulation.put("articStaccatissimo", new String[] {GROUP_ARTICULATION, "staccatissimo"});
        articulation.put("articStaccatissimoWedge", new String[] {GROUP_ARTICULATION, "staccatissimo"});
        articulation.put("articStaccatissimoStroke", new String[] {GROUP_ARTICULATION, "spiccato"});
        articulation.put("articAccentStaccato", new String[] {GROUP_ARTICULATION, "accent", "staccato"});
        articulation.put("articTenutoStaccato", new String[] {GROUP_ARTICULATION, "detached-legato"});
        articulation.put("articMarcatoStaccato", new String[] {GROUP_ARTICULATION, "strong-accent", "staccato"});
        articulation.put("articMarcatoTenuto", new String[] {GROUP_ARTICULATION, "strong-accent", "tenuto"});
        articulation.put("articTenutoAccent", new String[] {GROUP_ARTICULATION, "accent", "tenuto"});
        articulation.put("articStress", new String[] {GROUP_ARTICULATION, "stress"});
        articulation.put("articUnstress", new String[] {GROUP_ARTICULATION, "unstress"});
        articulation.put("stringsUpBow", new String[] {GROUP_TECHNICAL, "up-bow"});
        articulation.put("stringsDownBow", new String[] {GROUP_TECHNICAL, "down-bow"});
        articulation.put("stringsHarmonic", new String[] {GROUP_TECHNICAL, "harmonic"});
        articulation.put("stringsThumbPosition", new String[] {GROUP_TECHNICAL, "thumb-position"});
        articulation.put("brassMuteOpen", new String[] {GROUP_TECHNICAL, "open-string"});
        articulation.put("brassMuteClosed", new String[] {GROUP_TECHNICAL, "stopped"});
        articulation.put("pluckedSnapPizzicato", new String[] {GROUP_TECHNICAL, "snap-pizzicato"});
        articulation.put("doubleTongue", new String[] {GROUP_TECHNICAL, "double-tongue"});
        articulation.put("tripleTongue", new String[] {GROUP_TECHNICAL, "triple-tongue"});
        articulation.put("ornamentTrill", new String[] {GROUP_ORNAMENT, "trill-mark"});
        articulation.put("ornamentTurn", new String[] {GROUP_ORNAMENT, "turn"});
        articulation.put("ornamentTurnInverted", new String[] {GROUP_ORNAMENT, "inverted-turn"});
        articulation.put("ornamentMordent", new String[] {GROUP_ORNAMENT, "mordent"});
        articulation.put("ornamentShortTrill", new String[] {GROUP_ORNAMENT, "inverted-mordent"});
        articulation.put("ornamentMordentInverted", new String[] {GROUP_ORNAMENT, "inverted-mordent"});
        articulation.put("ornamentPrecompSlide", new String[] {GROUP_ORNAMENT, "schleifer"});
        articulation.put("ornamentShake3", new String[] {GROUP_ORNAMENT, "shake"});
        ARTICULATION_TO_XML = Collections.unmodifiableMap(articulation);

        Map<String, String> harmony = new HashMap<>();
        harmony.put("", "major");
        harmony.put("m", "minor");
        harmony.put("+", "augmented");
        harmony.put("dim", "diminished");
        harmony.put("7", "dominant");
        harmony.put("Maj7", KIND_MAJOR_SEVENTH);
        harmony.put("m7", "minor-seventh");
        harmony.put("dim7", "diminished-seventh");
        harmony.put("+7", "augmented-seventh");
        harmony.put("m7b5", KIND_HALF_DIMINISHED);
        harmony.put("m(Maj7)", "major-minor");
        harmony.put("6", "major-sixth");
        harmony.put("m6", "minor-sixth");
        harmony.put("9", "dominant-ninth");
        harmony.put("Maj9", "major-ninth");
        harmony.put("m9", "minor-ninth");
        harmony.put("11", "dominant-11th");
        harmony.put("Maj11", "major-11th");
        harmony.put("m11", "minor-11th");
        harmony.put("13", "dominant-13th");
        harmony.put("Maj13", "major-13th");
        harmony.put("m13", "minor-13th");
        harmony.put("sus2", "suspended-second");
        harmony.put("sus4", "suspended-fourth");
        harmony.put("5", "power");
        harmony.put("N.C.", "none");
        HARMONY_FROM_KIND = Collections.unmodifiableMap(invert(harmony));
        harmony.put("M", "major");
        harmony.put("maj", "major");
        harmony.put("min", "minor");
        harmony.put("mi", "minor");
        harmony.put("-", "minor");
        harmony.put("aug", "augmented");
        harmony.put("o", "diminished");
        harmony.put("maj7", KIND_MAJOR_SEVENTH);
        harmony.put("ma7", KIND_MAJOR_SEVENTH);
        harmony.put("M7", KIND_MAJOR_SEVENTH);
        harmony.put("^7", KIND_MAJOR_SEVENTH);
        harmony.put("Δ", KIND_MAJOR_SEVENTH);
        harmony.put("Δ7", KIND_MAJOR_SEVENTH);
        harmony.put("min7", "minor-seventh");
        harmony.put("mi7", "minor-seventh");
        harmony.put("-7", "minor-seventh");
        harmony.put("o7", "diminished-seventh");
        harmony.put("aug7", "augmented-seventh");
        harmony.put("7#5", "augmented-seventh");
        harmony.put("ø", KIND_HALF_DIMINISHED);
        harmony.put("ø7", KIND_HALF_DIMINISHED);
        harmony.put("mi7b5", KIND_HALF_DIMINISHED);
        harmony.put("min7b5", KIND_HALF_DIMINISHED);
        harmony.put("-7b5", KIND_HALF_DIMINISHED);
        harmony.put("mMaj7", "major-minor");
        harmony.put("maj9", "major-ninth");
        harmony.put("maj13", "major-13th");
        harmony.put("sus", "suspended-fourth");
        HARMONY_TO_KIND = Collections.unmodifiableMap(harmony);
    }


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MuseScoreSymbolConverter INSTANCE = new MuseScoreSymbolConverter();
    }


    /**
     * Constructor.
     */
    private MuseScoreSymbolConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MuseScoreSymbolConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Converts a MuseScore clef type name into the MusicXML clef sign, line and octave change.
     *
     * @param clefType the MuseScore clef type name (e.g. G, F8vb, C3, PERC, TAB)
     * @return an array {sign, line, octaveChange} where line and octaveChange may be null, or null if unknown
     */
    public String[] clefToXml(String clefType) {
        if (clefType == null || clefType.trim().isEmpty()) {
            return null;
        }

        String type = clefType.trim();
        if (type.startsWith("PERC")) {
            return new String[] {"percussion", null, null};
        }
        if (type.startsWith("TAB")) {
            return new String[] {"TAB", "5", null};
        }

        String octave = null;
        if (type.contains(OTTAVA_8VA) || type.contains("_8va")) {
            octave = "1";
        } else if (type.contains("15ma")) {
            octave = "2";
        } else if (type.contains("8vb")) {
            octave = "-1";
        } else if (type.contains("15mb")) {
            octave = "-2";
        }

        char sign = type.charAt(0);
        if (sign == 'G') {
            if (type.startsWith("G_1")) {
                return new String[] {"G", "1", octave};
            }
            return new String[] {"G", "2", octave};
        }
        if (sign == 'F') {
            if (type.startsWith("F_B")) {
                return new String[] {"F", "3", octave};
            }
            if ("F_C".equals(type)) {
                return new String[] {"F", "5", octave};
            }
            return new String[] {"F", "4", octave};
        }
        if (sign == 'C') {
            String line = "3";
            if (type.length() > 1 && Character.isDigit(type.charAt(1))) {
                line = String.valueOf(type.charAt(1));
            } else if (type.startsWith("C_19C")) {
                line = "3";
            }
            return new String[] {"C", line, octave};
        }
        return null;
    }


    /**
     * Converts a MusicXML clef into a MuseScore clef type name.
     *
     * @param sign the clef sign (G, F, C, percussion, TAB, none)
     * @param line the clef line, may be null
     * @param octaveChange the octave change, may be null
     * @return the MuseScore clef type name, or null if the clef cannot be represented
     */
    public String clefFromXml(String sign, Integer line, Integer octaveChange) {
        if (sign == null) {
            return null;
        }

        int octave = 0;
        if (octaveChange != null) {
            octave = octaveChange;
        }

        String upper = sign.trim().toUpperCase(Locale.ROOT);
        if ("PERCUSSION".equals(upper)) {
            return "PERC";
        }
        if ("TAB".equals(upper)) {
            return "TAB";
        }
        if ("G".equals(upper)) {
            if (line != null && line == 1) {
                return "G_1";
            }
            return octaveSuffix("G", octave, OTTAVA_8VA, "15ma", "8vb", "15mb");
        }
        if ("F".equals(upper)) {
            if (line != null && line == 3) {
                return "F_B";
            }
            if (line != null && line == 5) {
                return "F_C";
            }
            return octaveSuffix("F", octave, "_8va", "_15ma", "8vb", "15mb");
        }
        if ("C".equals(upper)) {
            int l = 3;
            if (line != null && line >= 1 && line <= 5) {
                l = line;
            }
            if (l == 4 && octave == -1) {
                return "C4_8vb";
            }
            return "C" + l;
        }
        return null;
    }


    /**
     * Converts a MuseScore barline subtype into a MusicXML bar style.
     *
     * @param subtype the MuseScore barline subtype
     * @return the bar style, or null if unknown
     */
    public String barlineToXml(String subtype) {
        if (subtype == null) {
            return null;
        }
        return BARLINE_TO_XML.get(subtype.trim());
    }


    /**
     * Converts a MusicXML bar style into a MuseScore barline subtype.
     *
     * @param barStyle the MusicXML bar style
     * @return the MuseScore subtype, or null if unknown
     */
    public String barlineFromXml(String barStyle) {
        if (barStyle == null) {
            return null;
        }
        return BARLINE_FROM_XML.get(barStyle.trim());
    }


    /**
     * Converts a MuseScore accidental subtype into a MusicXML accidental.
     *
     * @param subtype the MuseScore accidental subtype
     * @return the MusicXML accidental, or null if unknown
     */
    public String accidentalToXml(String subtype) {
        if (subtype == null) {
            return null;
        }
        return ACCIDENTAL_TO_XML.get(subtype.trim());
    }


    /**
     * Converts a MusicXML accidental into a MuseScore accidental subtype.
     *
     * @param accidental the MusicXML accidental
     * @return the MuseScore subtype, or null if unknown
     */
    public String accidentalFromXml(String accidental) {
        if (accidental == null) {
            return null;
        }
        String value = accidental.trim();
        if ("double-flat".equals(value)) {
            value = "flat-flat";
        }
        return ACCIDENTAL_FROM_XML.get(value);
    }


    /**
     * Converts a MuseScore notehead group name into a MusicXML notehead value.
     *
     * @param head the MuseScore notehead group name
     * @return the MusicXML notehead value, or null for normal or unknown heads
     */
    public String noteheadToXml(String head) {
        if (head == null) {
            return null;
        }
        return NOTEHEAD_TO_XML.get(head.trim());
    }


    /**
     * Converts a MusicXML notehead value into a MuseScore notehead group name.
     *
     * @param notehead the MusicXML notehead value
     * @return the MuseScore notehead group name, or null for normal or unknown heads
     */
    public String noteheadFromXml(String notehead) {
        if (notehead == null) {
            return null;
        }
        return NOTEHEAD_FROM_XML.get(notehead.trim());
    }


    /**
     * Checks whether a dynamic subtype is one of the predefined MuseScore dynamics.
     *
     * @param subtype the dynamic subtype
     * @return true if it is a known dynamic
     */
    public boolean isKnownDynamic(String subtype) {
        if (subtype == null) {
            return false;
        }
        for (String d : DYNAMICS) {
            if (d.equals(subtype)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Gets the default MIDI velocity MuseScore assigns to a dynamic.
     *
     * @param subtype the dynamic subtype
     * @return the velocity, or -1 if the dynamic has no defined velocity
     */
    public int dynamicVelocity(String subtype) {
        if (subtype == null) {
            return -1;
        }
        switch (subtype) {
            case "pppppp":
                return 1;
            case "ppppp":
                return 5;
            case "pppp":
                return 10;
            case "ppp":
                return 16;
            case "pp":
                return 33;
            case "p":
                return 49;
            case "mp":
                return 64;
            case "mf":
                return 80;
            case "f":
                return 96;
            case "ff":
                return 112;
            case "fff":
                return 126;
            case "ffff":
            case "fffff":
            case "ffffff":
                return 127;
            case "fp":
            case "sf":
            case "sfz":
            case "sff":
            case "sffz":
            case "sfp":
            case "sfpp":
            case "rfz":
            case "rf":
            case "fz":
                return 96;
            case "pf":
                return 49;
            default:
                return -1;
        }
    }


    /**
     * Converts a MuseScore fermata subtype into the MusicXML fermata type and shape.
     *
     * @param subtype the MuseScore fermata subtype (e.g. fermataAbove, fermataShortBelow)
     * @return an array {type, shape} where shape may be null for the normal shape
     */
    public String[] fermataToXml(String subtype) {
        String type = "upright";
        String shape = null;
        if (subtype == null) {
            return new String[] {type, shape};
        }

        String s = subtype.trim();
        if (s.endsWith(BELOW)) {
            type = "inverted";
        }
        if (s.startsWith("fermataVeryShort")) {
            shape = "double-angled";
        } else if (s.startsWith("fermataShortHenze")) {
            shape = "half-curve";
        } else if (s.startsWith("fermataShort")) {
            shape = "angled";
        } else if (s.startsWith("fermataVeryLong")) {
            shape = "double-square";
        } else if (s.startsWith("fermataLongHenze")) {
            shape = "double-dot";
        } else if (s.startsWith("fermataLong")) {
            shape = "square";
        }
        return new String[] {type, shape};
    }


    /**
     * Converts a MusicXML fermata into a MuseScore fermata subtype.
     *
     * @param type the fermata type (upright or inverted), may be null
     * @param shape the fermata shape, may be null
     * @return the MuseScore fermata subtype
     */
    public String fermataFromXml(String type, String shape) {
        String base = "fermata";
        if (shape != null) {
            switch (shape.trim()) {
                case "angled":
                    base = "fermataShort";
                    break;
                case "double-angled":
                    base = "fermataVeryShort";
                    break;
                case "square":
                    base = "fermataLong";
                    break;
                case "double-square":
                    base = "fermataVeryLong";
                    break;
                case "double-dot":
                    base = "fermataLongHenze";
                    break;
                case "half-curve":
                    base = "fermataShortHenze";
                    break;
                default:
                    break;
            }
        }

        if (type != null && "inverted".equals(type.trim())) {
            return base + BELOW;
        }
        return base + ABOVE;
    }


    /**
     * Converts a MuseScore ottava subtype into the MusicXML octave shift type and size.
     *
     * @param subtype the ottava subtype (name such as 8va or a numeric index)
     * @return an array {type, size} for the start of the shift
     */
    public String[] ottavaToXml(String subtype) {
        String name = OTTAVA_8VA;
        if (subtype != null) {
            String s = subtype.trim();
            if (!s.isEmpty() && Character.isDigit(s.charAt(0))) {
                try {
                    int index = Integer.parseInt(s);
                    if (index >= 0 && index < OTTAVA_NAMES.length) {
                        name = OTTAVA_NAMES[index];
                    }
                } catch (NumberFormatException e) {
                    name = OTTAVA_8VA;
                }
            } else {
                name = s;
            }
        }

        String size = "8";
        if (name.startsWith("15")) {
            size = "15";
        } else if (name.startsWith("22")) {
            size = "22";
        }

        String type = "down";
        if (name.endsWith("b") || name.endsWith("bassa")) {
            type = "up";
        }
        return new String[] {type, size};
    }


    /**
     * Converts a MusicXML octave shift into a MuseScore ottava subtype.
     *
     * @param type the octave shift type (up or down)
     * @param size the octave shift size (8, 15 or 22), may be null
     * @return the MuseScore ottava subtype
     */
    public String ottavaFromXml(String type, Integer size) {
        boolean below = type != null && "up".equals(type.trim());
        int s = 8;
        if (size != null) {
            s = size;
        }

        if (s == 15) {
            if (below) {
                return "15mb";
            }
            return "15ma";
        }
        if (s == 22) {
            if (below) {
                return "22mb";
            }
            return "22ma";
        }
        if (below) {
            return "8vb";
        }
        return OTTAVA_8VA;
    }


    /**
     * Converts a MuseScore articulation or ornament subtype into MusicXML marks.
     * The returned array starts with the group ({@link #GROUP_ARTICULATION}, {@link #GROUP_TECHNICAL}
     * or {@link #GROUP_ORNAMENT}) followed by one or more MusicXML element names.
     *
     * @param subtype the MuseScore symbol name (e.g. articStaccatoAbove)
     * @return the group and marks, or null if unknown
     */
    public String[] articulationToXml(String subtype) {
        if (subtype == null) {
            return null;
        }
        return ARTICULATION_TO_XML.get(stripPlacement(subtype.trim()));
    }


    /**
     * Gets the placement encoded in a MuseScore articulation subtype.
     *
     * @param subtype the MuseScore symbol name
     * @return "above", "below" or null
     */
    public String articulationPlacement(String subtype) {
        if (subtype == null) {
            return null;
        }
        if (subtype.endsWith(ABOVE)) {
            return "above";
        }
        if (subtype.endsWith(BELOW)) {
            return "below";
        }
        return null;
    }


    /**
     * Converts a MusicXML chord kind into a MuseScore chord symbol extension.
     *
     * @param kind the MusicXML kind value
     * @return the extension text, or null if unknown
     */
    public String harmonyFromKind(String kind) {
        if (kind == null) {
            return "";
        }
        return HARMONY_FROM_KIND.get(kind.trim());
    }


    /**
     * Converts a MuseScore chord symbol extension into a MusicXML chord kind.
     *
     * @param name the extension text (e.g. m7, Maj7, sus4)
     * @return the kind, or null if unknown
     */
    public String harmonyToKind(String name) {
        if (name == null) {
            return "major";
        }
        return HARMONY_TO_KIND.get(name.trim());
    }


    /**
     * Removes the Above/Below placement suffix from a symbol name.
     *
     * @param subtype the symbol name
     * @return the symbol name without placement suffix
     */
    private String stripPlacement(String subtype) {
        if (subtype.endsWith(ABOVE)) {
            return subtype.substring(0, subtype.length() - ABOVE.length());
        }
        if (subtype.endsWith(BELOW)) {
            return subtype.substring(0, subtype.length() - BELOW.length());
        }
        return subtype;
    }


    /**
     * Builds a clef name from a base and an octave change.
     *
     * @param base the clef base (G or F)
     * @param octave the octave change
     * @param up1 the suffix for one octave up
     * @param up2 the suffix for two octaves up
     * @param down1 the suffix for one octave down
     * @param down2 the suffix for two octaves down
     * @return the clef name
     */
    private String octaveSuffix(String base, int octave, String up1, String up2, String down1, String down2) {
        if (octave == 1) {
            return base + up1;
        }
        if (octave >= 2) {
            return base + up2;
        }
        if (octave == -1) {
            return base + down1;
        }
        if (octave <= -2) {
            return base + down2;
        }
        return base;
    }


    /**
     * Inverts a map (values become keys). For duplicate values the first entry wins.
     *
     * @param map the map to invert
     * @return the inverted map
     */
    private static Map<String, String> invert(Map<String, String> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            result.putIfAbsent(e.getValue(), e.getKey());
        }
        return result;
    }
}
