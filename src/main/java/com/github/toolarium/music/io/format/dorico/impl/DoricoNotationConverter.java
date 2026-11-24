/*
 * DoricoNotationConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import com.github.toolarium.music.io.converter.DynamicsConverter;
import com.github.toolarium.music.model.direction.impl.DirectionTypeImpl;
import com.github.toolarium.music.model.direction.impl.WordsImpl;
import com.github.toolarium.music.model.notation.impl.ArticulationsImpl;
import com.github.toolarium.music.model.notation.impl.DynamicsImpl;
import com.github.toolarium.music.model.notation.impl.OrnamentsImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Converts Dorico notation items (dynamics, articulations, ornaments, lyrics, text and repeat
 * markers) into MusicXML model objects, and reads Dorico property tables.
 *
 * @author patrick
 */
public final class DoricoNotationConverter {
    private static final String EMPTY = "";
    private static final Pattern TRANSPOSITION_SUFFIX = Pattern.compile("\\s*\\([A-G](\\{@(flat|sharp|natural)@\\})?\\)");
    private static final String STRING_PREFIX = "string: \"";
    private static final String INT_PREFIX = "int: ";
    private static final String PROPERTY_TYPE = "propertyType";
    private static final String PIANO = "p";
    private static final String FORTE = "f";
    private static final int MAX_DYNAMIC_LETTERS = 4;
    private static final int FAMILY_PIANO = 1;
    private static final char PRIVATE_USE_START = '';
    private static final char PRIVATE_USE_END = '';
    private static final String FORCE_DYNAMIC_PREFIX = "kForceDynamic";
    private static final Pattern INTENSITY_PATTERN = Pattern.compile(
            "\\{int: (\\d+)\\}, \\{[^}]*\\}, \\{bool: (true|false)\\}, \\{(?:int: (\\d+))?\\}");
    private static final Map<String, String> ORNAMENTS = createOrnaments();
    private static final Map<String, String> REPEAT_MARKERS = createRepeatMarkers();
    private static final Map<String, String> SYLLABIC = createSyllabic();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final DoricoNotationConverter INSTANCE = new DoricoNotationConverter();
    }


    /**
     * Constructor.
     */
    private DoricoNotationConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static DoricoNotationConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the entries of a property table with the given property type.
     *
     * @param node the node owning the property table (may be null)
     * @param propertyType the property type, for example kArticulationDuration
     * @return the matching entries
     */
    public List<DoricoTreeNode> getProperties(DoricoTreeNode node, String propertyType) {
        if (node == null) {
            return Collections.emptyList();
        }
        DoricoTreeNode entries = node.getPath("propertyTable", "entries");
        if (entries == null) {
            return Collections.emptyList();
        }

        List<DoricoTreeNode> result = new ArrayList<>();
        for (DoricoTreeNode entry : entries.getArrayItems()) {
            if (propertyType.equals(entry.getPathValue(PROPERTY_TYPE))) {
                result.add(entry);
            }
        }
        return result;
    }


    /**
     * Gets the plain value of the first property with the given type.
     *
     * @param node the node owning the property table (may be null)
     * @param propertyType the property type
     * @return the plain value (see {@link #plainValue(String)}), or null if the property is not set
     */
    public String getProperty(DoricoTreeNode node, String propertyType) {
        List<DoricoTreeNode> entries = getProperties(node, propertyType);
        if (entries.isEmpty()) {
            return null;
        }
        return plainValue(entries.get(0).getPathValue("value"));
    }


    /**
     * Removes the type prefix of a property value: {@code string: "kStaccato"} becomes kStaccato,
     * {@code int: 2} becomes 2 and {@code null: } becomes an empty string.
     *
     * @param value the property value (may be null)
     * @return the plain value, or null if the value is null
     */
    public String plainValue(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith(STRING_PREFIX) && value.endsWith("\"") && value.length() > STRING_PREFIX.length()) {
            return value.substring(STRING_PREFIX.length(), value.length() - 1);
        }
        if (value.startsWith(INT_PREFIX)) {
            return value.substring(INT_PREFIX.length()).trim();
        }
        if (value.startsWith("null:")) {
            return EMPTY;
        }
        return value;
    }


    /**
     * Gets the marking of an immediate dynamic, for example pp or mf.
     *
     * @param definition the ImmediateChangeDynamicDefinition node
     * @return the marking, or null if the intensity cannot be read
     */
    public String getImmediateDynamic(DoricoTreeNode definition) {
        String intensity = definition.getPathValue("intensity");
        if (intensity == null) {
            return null;
        }
        Matcher matcher = INTENSITY_PATTERN.matcher(intensity);
        if (!matcher.find()) {
            return null;
        }

        boolean piano = Integer.parseInt(matcher.group(1)) == FAMILY_PIANO;
        String letter = FORTE;
        if (piano) {
            letter = PIANO;
        }
        if ("true".equals(matcher.group(2))) {
            return "m" + letter;
        }

        int count = 1;
        if (matcher.group(3) != null) {
            count = Integer.parseInt(matcher.group(3));
        }
        return repeat(letter, count);
    }


    /**
     * Gets the marking of a force dynamic, for example sfz.
     *
     * @param definition the ForceDynamicDefinition node
     * @return the marking, or null if the type is unknown
     */
    public String getForceDynamic(DoricoTreeNode definition) {
        String type = definition.getPathValue("type");
        if (type == null || !type.startsWith(FORCE_DYNAMIC_PREFIX) || type.length() == FORCE_DYNAMIC_PREFIX.length()) {
            return null;
        }
        return type.substring(FORCE_DYNAMIC_PREFIX.length()).toLowerCase(Locale.ROOT);
    }


    /**
     * Gets the marking of a combined dynamic, for example fp.
     *
     * @param definition the CombinedDynamicDefinition node
     * @return the marking
     */
    public String getCombinedDynamic(DoricoTreeNode definition) {
        int start = parseCount(definition.getPathValue("startIntensity"));
        int end = parseCount(definition.getPathValue("endIntensity"));
        if ("kUp".equals(definition.getPathValue("direction"))) {
            return repeat(PIANO, start) + repeat(FORTE, end);
        }
        return repeat(FORTE, start) + repeat(PIANO, end);
    }


    /**
     * Creates the MusicXML dynamics for a marking such as pp, mf or sfz. Markings without a
     * MusicXML element become other-dynamics.
     *
     * @param marking the marking
     * @return the dynamics
     */
    public DynamicsImpl createDynamics(String marking) {
        return DynamicsConverter.getInstance().createDynamics(marking);
    }


    /**
     * Creates the articulations of a note from its properties kArticulationDuration,
     * kArticulationForce and kArticulationStress.
     *
     * @param noteEvent the note event node
     * @return the articulations, or null if the note has none
     */
    public ArticulationsImpl createArticulations(DoricoTreeNode noteEvent) {
        ArticulationsImpl articulations = new ArticulationsImpl();
        boolean found = false;
        for (String propertyType : new String[] {"kArticulationDuration", "kArticulationForce", "kArticulationStress"}) {
            for (DoricoTreeNode entry : getProperties(noteEvent, propertyType)) {
                found |= applyArticulation(articulations, plainValue(entry.getPathValue("value")));
            }
        }
        if (!found) {
            return null;
        }
        return articulations;
    }


    /**
     * Creates the ornament of a Dorico ornament entity.
     *
     * @param entityId the ornament entity ID, for example ornament.trill
     * @return the ornaments, or null if the ornament has no MusicXML equivalent
     */
    public OrnamentsImpl createOrnament(String entityId) {
        String ornament = ORNAMENTS.get(entityId);
        if (ornament == null) {
            return null;
        }

        OrnamentsImpl ornaments = new OrnamentsImpl();
        switch (ornament) {
            case "mordent":
                ornaments.setMordent(EMPTY);
                break;
            case "inverted-mordent":
                ornaments.setInvertedMordent(EMPTY);
                break;
            case "turn":
                ornaments.setTurn(EMPTY);
                break;
            case "inverted-turn":
                ornaments.setInvertedTurn(EMPTY);
                break;
            default:
                ornaments.setTrillMark(EMPTY);
                break;
        }
        return ornaments;
    }


    /**
     * Creates a trill mark.
     *
     * @return the ornaments with a trill mark
     */
    public OrnamentsImpl createTrill() {
        OrnamentsImpl ornaments = new OrnamentsImpl();
        ornaments.setTrillMark(EMPTY);
        return ornaments;
    }


    /**
     * Creates a lyric from a lyric element.
     *
     * @param element the LyricElementDefinition node
     * @return the lyric, or null if the element has no text
     */
    public LyricImpl createLyric(DoricoTreeNode element) {
        String text = element.getPathValue("text");
        if (text == null || text.isEmpty()) {
            return null;
        }

        int line = 0;
        try {
            line = Integer.parseInt(element.getPathValue("lineNumber").trim());
        } catch (RuntimeException e) {
            line = 0;
        }

        LyricImpl lyric = new LyricImpl();
        lyric.setNumber(String.valueOf(Math.max(line, 0) + 1));
        String syllabic = SYLLABIC.get(element.getPathValue("syllableType"));
        if (syllabic != null) {
            lyric.setSyllabic(syllabic);
        }
        lyric.setText(text);
        if ("kChorus".equals(element.getPathValue("lineType"))) {
            lyric.setName("chorus");
        }
        return lyric;
    }


    /**
     * Gets the plain text of a text event: the fragments of all paragraphs, without SMuFL symbols
     * from the private use area.
     *
     * @param textEvent the TextEventDefinition node
     * @return the text, or null if the event has no visible text
     */
    public String getText(DoricoTreeNode textEvent) {
        StringBuilder sb = new StringBuilder();
        collectFragments(textEvent.getChild("content"), sb);
        return cleanText(sb.toString());
    }


    /**
     * Creates the direction type of a repeat marker such as Fine, D.C. or a segno.
     *
     * @param sectionType the section type, for example kFine
     * @param labelOverride the label override (may be null)
     * @return the direction type, or null if the marker type is unknown
     */
    public DirectionTypeImpl createRepeatMarker(String sectionType, String labelOverride) {
        DirectionTypeImpl type = new DirectionTypeImpl();
        String label = cleanText(labelOverride);
        if (label != null) {
            type.setWords(Collections.singletonList(createWords(label, null)));
            return type;
        }
        if ("kSegno".equals(sectionType)) {
            type.setSegno(EMPTY);
            return type;
        }
        if ("kCoda".equals(sectionType)) {
            type.setCoda(EMPTY);
            return type;
        }

        String text = REPEAT_MARKERS.get(sectionType);
        if (text == null) {
            return null;
        }
        type.setWords(Collections.singletonList(createWords(text, null)));
        return type;
    }


    /**
     * Creates words.
     *
     * @param text the text
     * @param fontStyle the font style (for example italic), or null
     * @return the words
     */
    public WordsImpl createWords(String text, String fontStyle) {
        WordsImpl words = new WordsImpl();
        words.setValue(text);
        if (fontStyle != null) {
            words.setFontStyle(fontStyle);
        }
        return words;
    }


    /**
     * Applies one articulation value.
     *
     * @param articulations the articulations
     * @param value the Dorico value, for example kStaccato
     * @return true if the value is known
     */
    private boolean applyArticulation(ArticulationsImpl articulations, String value) {
        if (value == null) {
            return false;
        }
        switch (value) {
            case "kStaccato":
                articulations.setStaccato(EMPTY);
                return true;
            case "kStaccatissimo":
                articulations.setStaccatissimo(EMPTY);
                return true;
            case "kTenuto":
                articulations.setTenuto(EMPTY);
                return true;
            case "kStaccatoTenuto":
                articulations.setDetachedLegato(EMPTY);
                return true;
            case "kAccent":
                articulations.setAccent(EMPTY);
                return true;
            case "kMarcato":
                articulations.setStrongAccent(EMPTY);
                return true;
            case "kStress":
                articulations.setStress(EMPTY);
                return true;
            case "kUnstress":
                articulations.setUnstress(EMPTY);
                return true;
            default:
                return false;
        }
    }


    /**
     * Collects the text fragments below a rich text node.
     *
     * @param node the node (may be null)
     * @param sb the text receiving the fragments
     */
    private void collectFragments(DoricoTreeNode node, StringBuilder sb) {
        if (node == null) {
            return;
        }
        for (DoricoTreeNode child : node.getChildren()) {
            if ("fragments".equals(child.getName())) {
                for (DoricoTreeNode fragment : child.getArrayItems()) {
                    String content = fragment.getPathValue("content");
                    if (content != null) {
                        sb.append(content);
                    }
                }
            } else {
                collectFragments(child, sb);
            }
        }
    }


    /**
     * Removes SMuFL symbols (private use area) and surrounding white space.
     *
     * @param text the text (may be null)
     * @return the cleaned text, or null if nothing remains
     */
    private String cleanText(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < PRIVATE_USE_START || c > PRIVATE_USE_END) {
                sb.append(c);
            }
        }
        String result = sb.toString().trim();
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }


    /**
     * Parses a letter count of a dynamic (1 to 4).
     *
     * @param value the value (may be null)
     * @return the count
     */
    private int parseCount(String value) {
        int count = 1;
        try {
            count = Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            count = 1;
        }
        return Math.max(1, Math.min(count, MAX_DYNAMIC_LETTERS));
    }


    /**
     * Repeats a letter.
     *
     * @param letter the letter
     * @param count the count (limited to 1 to 4)
     * @return the repeated letter
     */
    private String repeat(String letter, int count) {
        StringBuilder sb = new StringBuilder();
        int limited = Math.max(1, Math.min(count, MAX_DYNAMIC_LETTERS));
        for (int i = 0; i < limited; i++) {
            sb.append(letter);
        }
        return sb.toString();
    }


    /**
     * Creates the mapping from Dorico ornament entities to MusicXML ornaments.
     *
     * @return the map
     */
    private static Map<String, String> createOrnaments() {
        Map<String, String> map = new HashMap<>();
        String trill = "trill-mark";
        map.put("ornament.trill", trill);
        map.put("ornament.tremblement", trill);
        map.put("ornament.precompAppoggTrill", trill);
        map.put("ornament.precompCadence", trill);
        map.put("ornament.precompSlideTrillBach", trill);
        map.put("ornament.precompSlideTrillDAnglebert", trill);
        map.put("ornament.precompTrillWithMordent", trill);
        map.put("ornament.shortTrill", "inverted-mordent");
        map.put("ornament.precompInvertedMordentUpperPrefix", "inverted-mordent");
        map.put("ornament.mordent", "mordent");
        map.put("ornament.precompMordentUpperPrefix", "mordent");
        map.put("ornament.turn", "turn");
        map.put("ornament.invertedTurn", "inverted-turn");
        return map;
    }


    /**
     * Creates the texts of the repeat marker section types.
     *
     * @return the map
     */
    private static Map<String, String> createRepeatMarkers() {
        Map<String, String> map = new HashMap<>();
        map.put("kFine", "Fine");
        map.put("kDaCapo", "D.C.");
        map.put("kDalSegno", "D.S.");
        map.put("kDaCapoAlFine", "D.C. al Fine");
        map.put("kDalSegnoAlFine", "D.S. al Fine");
        map.put("kDaCapoAlCoda", "D.C. al Coda");
        map.put("kDalSegnoAlCoda", "D.S. al Coda");
        map.put("kToCoda", "To Coda");
        return map;
    }


    /**
     * Creates the mapping from Dorico syllable types to MusicXML syllabic values.
     *
     * @return the map
     */
    private static Map<String, String> createSyllabic() {
        Map<String, String> map = new HashMap<>();
        map.put("kWholeWord", "single");
        map.put("kStart", "begin");
        map.put("kMiddle", "middle");
        map.put("kEnd", "end");
        return map;
    }


    /**
     * Cleans a player or instrument name: Dorico writes accidentals in names as tokens such as
     * {@literal {@flat@}}, which become the accidental characters.
     *
     * @param value the name (may be null)
     * @return the cleaned name, or null if it is empty
     */
    public String getName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().replace("{@flat@}", "♭").replace("{@sharp@}", "♯").replace("{@natural@}", "♮");
    }


    /**
     * Cleans the name of a player: Dorico adds the key of a transposing instrument to the name of its player (for
     * example Posaune (B{@literal {@flat@}})), which is removed, as the other notation applications do not have it.
     *
     * @param value the name of the player (may be null)
     * @return the cleaned name, or null if it is empty
     */
    public String getPlayerName(String value) {
        if (value == null) {
            return null;
        }
        return getName(TRANSPOSITION_SUFFIX.matcher(value).replaceAll(EMPTY).replaceAll("\\s+", " "));
    }
}
