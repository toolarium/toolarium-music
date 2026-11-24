/*
 * MeiBodyParser.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl;

import com.github.toolarium.music.io.converter.ChordSymbolParser;
import com.github.toolarium.music.io.converter.PitchSpeller;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.MeasureData;
import com.github.toolarium.music.io.dto.NoteData;
import com.github.toolarium.music.io.dto.PartData;
import com.github.toolarium.music.io.dto.ScoreData;
import com.github.toolarium.music.io.dto.VoiceData;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiControlEvent;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiEventRef;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiLayerContext;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiStaffDef;
import com.github.toolarium.music.io.xml.XmlDocumentReader;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.w3c.dom.Element;


/**
 * Reads the music of an MEI document (score definitions, sections, measures, layers and control
 * events) into the format independent {@link ScoreData}.
 *
 * <p>Layers are read first and every element with an xml:id is registered. Control events (ties,
 * slurs, dynamics, hairpins and so on) are applied after the whole document is read, so that
 * references to later measures can be resolved.</p>
 *
 * @author patrick
 */
final class MeiBodyParser {
    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
    private static final Pattern DYNAMIC_MARKING = Pattern.compile("^[pmfsrzn]+$");
    private static final Pattern TEMPO_EQUATION = Pattern.compile("\\s*=\\s*\\d+(\\.\\d+)?");
    private static final Pattern PRIVATE_USE = Pattern.compile("[\\uE000-\\uF8FF\\u2669-\\u266C\\x{1D15D}-\\x{1D164}]");
    private static final Fraction ONE = Fraction.of(1, 1);
    private static final int VOICES_PER_STAFF = 4;
    private static final int EXTRA_VOICES = 64;
    private static final int MAX_NUMBER = 6;
    private static final String EMPTY = "";
    private static final String N = "n";
    private static final String ITALIC = "italic";

    private final MeiConverter converter = MeiConverter.getInstance();
    private final XmlDocumentReader xml = XmlDocumentReader.getInstance();
    private final ScoreBuilder builder = ScoreBuilder.getInstance();
    private final ScoreData score;
    private final Map<String, MeiStaffDef> staffDefs = new LinkedHashMap<>();
    private final Map<String, MeiEventRef> ids = new HashMap<>();
    private final List<MeiControlEvent> controlEvents = new ArrayList<>();
    private final Map<EventData, Fraction> positions = new IdentityHashMap<>();
    private final List<Fraction> beatUnits = new ArrayList<>();
    private final List<Integer> incompleteMeasures = new ArrayList<>();
    private final Map<String, Integer> pendingKeys = new HashMap<>();
    private final Map<String, String> pendingModes = new HashMap<>();
    private final Map<String, ClefImpl> pendingClefs = new HashMap<>();
    private final Map<Integer, int[]> slurNumbers = new HashMap<>();
    private final Map<Integer, int[]> wedgeNumbers = new HashMap<>();
    private int measureIndex;
    private Integer meterCount;
    private Integer meterUnit = 4;
    private String meterSymbol;
    private boolean meterChanged;
    private String ending;
    private boolean repeatStartNext;


    /**
     * Constructor for MeiBodyParser.
     *
     * @param score the score data (metadata already read; parts are added)
     */
    MeiBodyParser(ScoreData score) {
        this.score = score;
    }


    /**
     * Reads the music element.
     *
     * @param music the music element
     */
    void parse(Element music) {
        for (Element body : xml.children(music, "body")) {
            for (Element mdiv : xml.children(body, "mdiv")) {
                readMdiv(mdiv);
            }
        }
        applyTupletSpans();
        calculatePositions();
        for (MeiControlEvent event : controlEvents) {
            applyControlEvent(event);
        }
        for (Integer index : incompleteMeasures) {
            Fraction length = Fraction.ZERO;
            for (PartData part : score.getParts()) {
                if (index < part.getMeasures().size()) {
                    length = length.max(part.getMeasures().get(index).getContentLength());
                }
            }
            if (length.isPositive()) {
                for (PartData part : score.getParts()) {
                    part.measure(index).setLength(length);
                    part.measure(index).setImplicit(true);
                }
            }
        }
    }


    /**
     * Reads a movement division (mdiv): its score or nested divisions.
     *
     * @param mdiv the mdiv element
     */
    private void readMdiv(Element mdiv) {
        for (Element child : xml.elements(mdiv)) {
            String name = xml.name(child);
            if ("score".equals(name)) {
                readContainer(child);
            } else if ("mdiv".equals(name)) {
                readMdiv(child);
            }
        }
    }


    /**
     * Reads a container of score definitions, sections, endings and measures.
     *
     * @param parent the container
     */
    private void readContainer(Element parent) {
        for (Element child : xml.elements(parent)) {
            switch (xml.name(child)) {
                case "scoreDef":
                    readScoreDef(child);
                    break;
                case "staffDef":
                    readStaffDef(child);
                    break;
                case "section":
                case "expansion":
                case "lem":
                case "supplied":
                    readContainer(child);
                    break;
                case "ending":
                    String previous = ending;
                    ending = endingNumber(child);
                    readContainer(child);
                    ending = previous;
                    break;
                case "app":
                case "choice":
                    Element chosen = choose(child);
                    if (chosen != null) {
                        readContainer(chosen);
                    }
                    break;
                case "measure":
                    readMeasure(child);
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * Gets the number of an ending from its n or label attribute.
     *
     * @param element the ending element
     * @return the ending number, for example "1" or "1, 2"
     */
    private String endingNumber(Element element) {
        String text = xml.attr(element, N);
        if (text == null) {
            text = xml.attr(element, "label");
        }
        if (text == null) {
            return "1";
        }
        String digits = text.replaceAll("[^0-9,]", " ").trim().replaceAll("\\s*,\\s*", ",").replaceAll("\\s+", ",");
        if (digits.isEmpty()) {
            return "1";
        }
        return String.join(", ", digits.split(",+"));
    }


    /**
     * Chooses the reading of an app or choice element: the lemma, a corrected or regularized
     * reading, or the first child.
     *
     * @param element the app or choice element
     * @return the chosen child, or null
     */
    private Element choose(Element element) {
        List<Element> options = xml.elements(element);
        for (String preferred : new String[] {"lem", "corr", "reg", "expan"}) {
            for (Element option : options) {
                if (preferred.equals(xml.name(option))) {
                    return option;
                }
            }
        }
        if (options.isEmpty()) {
            return null;
        }
        return options.get(0);
    }


    /**
     * Reads a score definition: meter, key and (the first time) the staff groups that define the parts.
     *
     * @param scoreDef the scoreDef element
     */
    private void readScoreDef(Element scoreDef) {
        readMeter(scoreDef);
        Integer fifths = keyOf(scoreDef);
        if (fifths != null) {
            for (String n : staffDefs.keySet()) {
                pendingKeys.put(n, fifths);
                pendingModes.put(n, keyModeOf(scoreDef));
            }
        }
        if (score.getParts().isEmpty()) {
            for (Element group : xml.children(scoreDef, "staffGrp")) {
                buildGroup(group);
            }
            if (fifths != null) {
                for (String n : staffDefs.keySet()) {
                    pendingKeys.putIfAbsent(n, fifths);
                    pendingModes.putIfAbsent(n, keyModeOf(scoreDef));
                }
            }
        }
        for (Element staffDef : xml.descendants(scoreDef, "staffDef")) {
            readStaffDef(staffDef);
        }
    }


    /**
     * Reads the meter attributes or meterSig child of a scoreDef or staffDef.
     *
     * @param element the element
     */
    private void readMeter(Element element) {
        String count = xml.attr(element, "meter.count");
        String unit = xml.attr(element, "meter.unit");
        String symbol = xml.attr(element, "meter.sym");
        Element meterSig = xml.child(element, "meterSig");
        if (meterSig != null) {
            count = firstNonNull(xml.attr(meterSig, "count"), count);
            unit = firstNonNull(xml.attr(meterSig, "unit"), unit);
            symbol = firstNonNull(xml.attr(meterSig, "sym"), symbol);
        }
        if (count == null && symbol != null) {
            if ("cut".equals(symbol)) {
                count = "2";
                unit = "2";
            } else if ("common".equals(symbol)) {
                count = "4";
                unit = "4";
            }
        }
        if (count == null || unit == null) {
            return;
        }
        int beats = 0;
        for (String part : count.split("\\+")) {
            beats += intValue(part, 0);
        }
        int beatType = intValue(unit, 0);
        if (beats <= 0 || beatType <= 0) {
            return;
        }
        meterCount = beats;
        meterUnit = beatType;
        meterSymbol = null;
        if ("cut".equals(symbol) || "common".equals(symbol)) {
            meterSymbol = symbol;
        }
        meterChanged = true;
    }


    /**
     * Gets the key signature of a scoreDef, staffDef or keySig element.
     *
     * @param element the element
     * @return the fifths, or null without key signature
     */
    private Integer keyOf(Element element) {
        String sig = firstNonNull(xml.attr(element, "keysig"), xml.attr(element, "key.sig"));
        if ("keySig".equals(xml.name(element))) {
            sig = xml.attr(element, "sig");
        }
        Element keySig = xml.child(element, "keySig");
        if (keySig != null && xml.attr(keySig, "sig") != null) {
            sig = xml.attr(keySig, "sig");
        }
        return converter.fifthsOf(sig);
    }


    /**
     * Gets the key mode of a scoreDef, staffDef or keySig element.
     *
     * @param element the element
     * @return the mode, or null
     */
    private String keyModeOf(Element element) {
        String mode = firstNonNull(xml.attr(element, "key.mode"), xml.attr(element, "mode"));
        Element keySig = xml.child(element, "keySig");
        if (keySig != null && xml.attr(keySig, "mode") != null) {
            mode = xml.attr(keySig, "mode");
        }
        return mode;
    }


    /**
     * Builds the parts of a staff group. A braced group of unlabeled staves (or a labeled braced
     * group) is one part with several staves; other staves are parts of their own.
     *
     * @param group the staffGrp element
     */
    private void buildGroup(Element group) {
        List<Element> defs = xml.children(group, "staffDef");
        List<Element> groups = xml.children(group, "staffGrp");
        int labeled = 0;
        for (Element def : defs) {
            if (labelOf(def) != null) {
                labeled++;
            }
        }
        boolean braced = "brace".equals(xml.attr(group, "symbol")) && (labelOf(group) != null || labeled <= 1);
        boolean labeledGroup = labelOf(group) != null && labeled == 0;
        boolean singlePart = defs.size() >= 2 && groups.isEmpty() && (braced || labeledGroup);
        if (singlePart) {
            String name = labelOf(group);
            if (name == null && !defs.isEmpty()) {
                name = labelOf(defs.get(0));
            }
            PartData part = new PartData(name);
            part.setAbbreviation(firstNonNull(labelAbbrOf(group), labelAbbrOf(defs.get(0))));
            part.setStaves(defs.size());
            score.getParts().add(part);
            Integer program = programOf(group);
            for (int i = 0; i < defs.size(); i++) {
                registerStaffDef(defs.get(i), score.getParts().size() - 1, i + 1);
                if (program == null) {
                    program = staffDefs.get(xml.attr(defs.get(i), "n")).getProgram();
                }
            }
            part.setMidiProgram(program);
            return;
        }
        for (Element child : xml.elements(group)) {
            if ("staffDef".equals(xml.name(child))) {
                PartData part = new PartData(labelOf(child));
                part.setAbbreviation(labelAbbrOf(child));
                score.getParts().add(part);
                registerStaffDef(child, score.getParts().size() - 1, 1);
                part.setMidiProgram(staffDefs.get(xml.attr(child, N)).getProgram());
            } else if ("staffGrp".equals(xml.name(child))) {
                buildGroup(child);
            }
        }
    }


    /**
     * Registers a staff definition of the initial score definition.
     *
     * @param def the staffDef element
     * @param partIndex the part index
     * @param staffInPart the staff within the part (1-based)
     */
    private void registerStaffDef(Element def, int partIndex, int staffInPart) {
        String n = xml.attr(def, N);
        if (n == null) {
            n = String.valueOf(staffDefs.size() + 1);
        }
        MeiStaffDef staffDef = new MeiStaffDef(n);
        staffDef.setPartIndex(partIndex);
        staffDef.setStaffInPart(staffInPart);
        staffDef.setKeyFifths(0);
        staffDefs.put(n, staffDef);
        Element instrDef = xml.child(def, "instrDef");
        if (instrDef != null && xml.attr(instrDef, "midi.instrnum") != null) {
            int program = intValue(xml.attr(instrDef, "midi.instrnum"), -1);
            if (program >= 0 && program < 128) {
                staffDef.setProgram(program + 1);
            }
        }
        readStaffDef(def);
        PartData part = score.getParts().get(partIndex);
        if (staffInPart == 1) {
            part.setTranspose(staffDef.getTransposeSemitones(), staffDef.getTransposeDiatonic());
        }
        if (staffDef.getLines() != null && staffDef.getLines() != 5) {
            part.getStaffLines().put(staffInPart, staffDef.getLines());
        }
        if (staffDef.getClef() != null && "percussion".equals(staffDef.getClef().getSign())) {
            part.setPercussion(true);
        }
    }


    /**
     * Reads a staff definition (initial or change): label, lines, clef, key, meter and transposition.
     *
     * @param def the staffDef element
     */
    private void readStaffDef(Element def) {
        MeiStaffDef staffDef = staffDefs.get(xml.attr(def, N));
        if (staffDef == null) {
            return;
        }
        if (labelOf(def) != null) {
            staffDef.setLabel(labelOf(def));
        }
        if (labelAbbrOf(def) != null) {
            staffDef.setAbbreviation(labelAbbrOf(def));
        }
        if (xml.attr(def, "lines") != null) {
            staffDef.setLines(intValue(xml.attr(def, "lines"), 5));
        }
        ClefImpl clef = converter.clefOf(xml.attr(def, "clef.shape"), xml.attr(def, "clef.line"), xml.attr(def, "clef.dis"), xml.attr(def, "clef.dis.place"));
        Element clefElement = xml.child(def, "clef");
        if (clefElement != null) {
            clef = clefOf(clefElement);
        }
        if (clef != null) {
            staffDef.setClef(clef);
            pendingClefs.put(staffDef.getNumber(), clef);
        }
        Integer fifths = keyOf(def);
        if (fifths != null) {
            staffDef.setKeyFifths(fifths);
            staffDef.setKeyMode(keyModeOf(def));
            pendingKeys.put(staffDef.getNumber(), fifths);
            pendingModes.put(staffDef.getNumber(), keyModeOf(def));
        }
        if (xml.attr(def, "trans.semi") != null) {
            staffDef.setTransposeSemitones(intValue(xml.attr(def, "trans.semi"), 0));
            staffDef.setTransposeDiatonic(intValue(xml.attr(def, "trans.diat"), 0));
        }
        readMeter(def);
    }


    /**
     * Creates a clef from a clef element.
     *
     * @param element the clef element
     * @return the clef, or null
     */
    private ClefImpl clefOf(Element element) {
        return converter.clefOf(xml.attr(element, "shape"), xml.attr(element, "line"), xml.attr(element, "dis"), xml.attr(element, "dis.place"));
    }


    /**
     * Reads a measure: barlines, staves with their layers and the control events.
     *
     * @param measure the measure element
     */
    private void readMeasure(Element measure) {
        int index = measureIndex;
        for (PartData part : score.getParts()) {
            applyPending(part, part.measure(index));
        }
        pendingClefs.clear();
        pendingKeys.clear();
        pendingModes.clear();
        meterChanged = false;
        Fraction beatUnit = Fraction.of(1, Math.max(1, meterUnit));
        beatUnits.add(beatUnit);
        applyBarlines(measure, index);

        int multiRest = 1;
        for (Element child : xml.elements(measure)) {
            String name = xml.name(child);
            if ("staff".equals(name)) {
                multiRest = Math.max(multiRest, readStaff(child, index));
            } else if (!"pb".equals(name) && !"sb".equals(name)) {
                controlEvents.add(new MeiControlEvent(child, index, beatUnit));
            }
        }
        if ("false".equals(xml.attr(measure, "metcon"))) {
            incompleteMeasures.add(index);
        }
        measureIndex++;
        for (int k = 1; k < multiRest; k++) {
            for (PartData part : score.getParts()) {
                MeasureData extra = part.measure(measureIndex);
                if (ending != null) {
                    extra.setEndingNumber(ending);
                }
            }
            beatUnits.add(beatUnit);
            measureIndex++;
        }
    }


    /**
     * Applies the pending time, key and clef changes to the measure of a part.
     *
     * @param part the part
     * @param measure the measure of the part
     */
    private void applyPending(PartData part, MeasureData measure) {
        int partIndex = score.getParts().indexOf(part);
        if (meterChanged && meterCount != null) {
            measure.setTime(meterCount, meterUnit, meterSymbol);
        }
        for (MeiStaffDef staffDef : staffDefs.values()) {
            if (staffDef.getPartIndex() != partIndex) {
                continue;
            }
            Integer fifths = pendingKeys.get(staffDef.getNumber());
            if (fifths != null && staffDef.getStaffInPart() == 1) {
                measure.setKey(fifths, pendingModes.get(staffDef.getNumber()));
            }
            ClefImpl clef = pendingClefs.get(staffDef.getNumber());
            if (clef != null) {
                measure.addClef(staffDef.getStaffInPart(), Fraction.ZERO, clef);
            }
        }
    }


    /**
     * Applies the left and right barline attributes and the current ending to all parts.
     *
     * @param measure the measure element
     * @param index the measure index
     */
    private void applyBarlines(Element measure, int index) {
        String left = xml.attr(measure, "left");
        String right = xml.attr(measure, "right");
        for (PartData part : score.getParts()) {
            MeasureData data = part.measure(index);
            if (repeatStartNext || "rptstart".equals(left) || "rptboth".equals(left)) {
                data.setRepeatStart(true);
            } else if (left != null) {
                data.setLeftBarStyle(barStyle(left));
            }
            if ("rptend".equals(right) || "rptboth".equals(right)) {
                data.setRepeatEnd(true, null);
            } else if (right != null) {
                data.setRightBarStyle(barStyle(right));
            }
            if (ending != null) {
                data.setEndingNumber(ending);
            }
        }
        repeatStartNext = "rptboth".equals(right);
    }


    /**
     * Gets the MusicXML bar style of an MEI barline rendition.
     *
     * @param rendition the MEI value (dbl, end, dashed, dotted, invis, single and so on)
     * @return the MusicXML bar style, or null for a regular barline
     */
    private String barStyle(String rendition) {
        switch (rendition) {
            case "dbl":
                return "light-light";
            case "end":
                return "light-heavy";
            case "dashed":
                return "dashed";
            case "dotted":
                return "dotted";
            case "invis":
                return "none";
            case "heavy":
                return "heavy";
            case "dblheavy":
                return "heavy-heavy";
            default:
                return null;
        }
    }


    /**
     * Reads a staff of a measure with its layers.
     *
     * @param staff the staff element
     * @param index the measure index
     * @return the number of measures of a multi-measure rest (1 without)
     */
    private int readStaff(Element staff, int index) {
        MeiStaffDef staffDef = staffDefs.get(xml.attr(staff, N));
        if (staffDef == null) {
            return 1;
        }
        MeasureData measure = score.getParts().get(staffDef.getPartIndex()).measure(index);
        Map<String, Integer> accidentals = new HashMap<>();
        int multiRest = 1;
        List<Element> layers = xml.children(staff, "layer");
        Set<Integer> usedVoices = new HashSet<>();
        for (int l = 0; l < layers.size(); l++) {
            int layerNumber = intValue(xml.attr(layers.get(l), "n"), l + 1);
            if (layerNumber < 1 || layerNumber > VOICES_PER_STAFF) {
                layerNumber = Math.min(l + 1, VOICES_PER_STAFF);
            }
            int voiceNumber = (staffDef.getStaffInPart() - 1) * VOICES_PER_STAFF + layerNumber;
            if (!usedVoices.add(voiceNumber)) {
                // more than four layers: the further layers get voices after the voices of all staves
                voiceNumber = EXTRA_VOICES * staffDef.getStaffInPart() + l + 1;
                usedVoices.add(voiceNumber);
            }
            VoiceData voice = measure.voice(staffDef.getStaffInPart(), voiceNumber);
            MeiLayerContext context = new MeiLayerContext(measure, voice, staffDef, index, accidentals);
            context.setMultiRest(1);
            readLayer(layers.get(l), context, ONE, null, null);
            multiRest = Math.max(multiRest, context.getMultiRest());
        }
        return multiRest;
    }


    /**
     * Reads the children of a layer or of a container within a layer (beam, tuplet, graceGrp, tremolo).
     *
     * @param parent the parent element
     * @param context the layer context
     * @param ratio the tuplet ratio (normal / actual) of the enclosing tuplets
     * @param grace the grace type of an enclosing graceGrp, or null
     * @param collector the events of an enclosing tuplet or tremolo (filled), or null
     */
    private void readLayer(Element parent, MeiLayerContext context, Fraction ratio, String grace, List<EventData> collector) {
        for (Element child : xml.elements(parent)) {
            String name = xml.name(child);
            switch (name) {
                case "note":
                    addEvent(context, noteEvent(child, context, ratio, grace), collector, child);
                    break;
                case "chord":
                    addEvent(context, chordEvent(child, context, ratio, grace), collector, child);
                    break;
                case "rest":
                case "space":
                    addEvent(context, restEvent(child, context, ratio, "space".equals(name)), collector, child);
                    break;
                case "mRest":
                case "mSpace":
                case "multiRest":
                    EventData measureRest = new EventData();
                    measureRest.setMeasureRest(true);
                    measureRest.setInvisible("mSpace".equals(name));
                    if ("multiRest".equals(name)) {
                        context.setMultiRest(Math.max(1, intValue(xml.attr(child, "num"), 1)));
                    }
                    applyFermataAttribute(child, measureRest);
                    addEvent(context, measureRest, collector, child);
                    break;
                case "mRpt":
                case "mRpt2":
                case "halfmRpt":
                case "beatRpt":
                    if ("mRpt".equals(name) || "mRpt2".equals(name)) {
                        copyPreviousMeasure(context);
                    }
                    break;
                case "clef":
                    ClefImpl clef = clefOf(child);
                    if (clef != null) {
                        context.getMeasure().addClef(context.getStaffDef().getStaffInPart(), context.getVoice().getLength(), clef);
                        context.getStaffDef().setClef(clef);
                    }
                    break;
                case "tuplet":
                    readTuplet(child, context, ratio, grace, collector);
                    break;
                case "fTrem":
                    List<EventData> tremolo = new ArrayList<>();
                    readLayer(child, context, ratio, grace, tremolo);
                    for (EventData event : tremolo) {
                        if (event.getDuration() != null) {
                            event.setDuration(event.getDuration().multiply(1, Math.max(1, tremolo.size())));
                        }
                    }
                    addAll(collector, tremolo);
                    break;
                case "graceGrp":
                    String graceType = xml.attr(child, "grace");
                    if (graceType == null) {
                        graceType = "unacc";
                    }
                    readLayer(child, context, ratio, graceType, collector);
                    break;
                case "app":
                case "choice":
                    Element chosen = choose(child);
                    if (chosen != null) {
                        readLayer(chosen, context, ratio, grace, collector);
                    }
                    break;
                case "beam":
                case "bTrem":
                case "ligature":
                case "lem":
                case "supplied":
                case "reg":
                case "corr":
                case "add":
                case "unclear":
                    readLayer(child, context, ratio, grace, collector);
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * Reads a tuplet container: the durations of its events are scaled by numbase / num.
     *
     * @param tuplet the tuplet element
     * @param context the layer context
     * @param ratio the ratio of enclosing tuplets
     * @param grace the grace type of an enclosing graceGrp, or null
     * @param collector the events of an enclosing tuplet (filled), or null
     */
    private void readTuplet(Element tuplet, MeiLayerContext context, Fraction ratio, String grace, List<EventData> collector) {
        int num = intValue(xml.attr(tuplet, "num"), 3);
        int numbase = intValue(xml.attr(tuplet, "numbase"), 2);
        if (num <= 0 || numbase <= 0) {
            num = 1;
            numbase = 1;
        }
        List<EventData> inner = new ArrayList<>();
        readLayer(tuplet, context, ratio.multiply(numbase, num), grace, inner);
        List<EventData> timed = new ArrayList<>();
        for (EventData event : inner) {
            if (!event.isGrace()) {
                timed.add(event);
            }
            if (event.getTupletActual() == 0) {
                event.setTuplet(num, numbase);
            }
        }
        if (!timed.isEmpty()) {
            timed.get(0).setTupletStart(true);
            timed.get(timed.size() - 1).setTupletStop(true);
        }
        addAll(collector, inner);
    }


    /**
     * Adds an event to the voice, registers its ID and adds it to the collector.
     *
     * @param context the layer context
     * @param event the event
     * @param collector the collector, or null
     * @param element the element of the event
     */
    private void addEvent(MeiLayerContext context, EventData event, List<EventData> collector, Element element) {
        context.getVoice().getEvents().add(event);
        if (collector != null) {
            collector.add(event);
        }
        register(element, event, null, context);
    }


    /**
     * Registers the xml:id of an element.
     *
     * @param element the element
     * @param event the event
     * @param note the note (null for events)
     * @param context the layer context
     */
    private void register(Element element, EventData event, NoteData note, MeiLayerContext context) {
        String id = idOf(element);
        if (id != null) {
            ids.put(id, new MeiEventRef(event, note, context.getVoice(), context.getStaffDef().getPartIndex(),
                    context.getMeasureIndex(), context.getStaffDef().getStaffInPart()));
        }
    }


    /**
     * Creates the event of a single note.
     *
     * @param element the note element
     * @param context the layer context
     * @param ratio the tuplet ratio
     * @param grace the grace type of an enclosing graceGrp, or null
     * @return the event
     */
    private EventData noteEvent(Element element, MeiLayerContext context, Fraction ratio, String grace) {
        EventData event = new EventData();
        applyTiming(element, event, context, ratio, grace);
        NoteData note = readNote(element, context, event);
        event.getNotes().add(note);
        applyEventAttributes(element, event, context);
        return event;
    }


    /**
     * Creates the event of a chord.
     *
     * @param element the chord element
     * @param context the layer context
     * @param ratio the tuplet ratio
     * @param grace the grace type of an enclosing graceGrp, or null
     * @return the event
     */
    private EventData chordEvent(Element element, MeiLayerContext context, Fraction ratio, String grace) {
        EventData event = new EventData();
        applyTiming(element, event, context, ratio, grace);
        for (Element noteElement : xml.children(element, "note")) {
            event.getNotes().add(readNote(noteElement, context, event));
            applyArticulations(noteElement, event);
            readLyrics(noteElement, event);
        }
        applyEventAttributes(element, event, context);
        return event;
    }


    /**
     * Creates the event of a rest or space.
     *
     * @param element the rest or space element
     * @param context the layer context
     * @param ratio the tuplet ratio
     * @param invisible true for a space
     * @return the event
     */
    private EventData restEvent(Element element, MeiLayerContext context, Fraction ratio, boolean invisible) {
        EventData event = new EventData();
        applyTiming(element, event, context, ratio, null);
        event.setInvisible(invisible);
        applyFermataAttribute(element, event);
        return event;
    }


    /**
     * Sets duration, note type, dots, tuplet ratio and grace of an event.
     *
     * @param element the note, chord or rest element
     * @param event the event
     * @param context the layer context
     * @param ratio the tuplet ratio
     * @param grace the grace type of an enclosing graceGrp, or null
     */
    private void applyTiming(Element element, EventData event, MeiLayerContext context, Fraction ratio, String grace) {
        String dur = xml.attr(element, "dur");
        Element parent = (Element) element.getParentNode();
        if (dur == null && "chord".equals(xml.name(parent))) {
            dur = xml.attr(parent, "dur");
        }
        if (dur == null) {
            dur = context.getLastDur();
        }
        if (dur == null) {
            dur = "4";
        }
        context.setLastDur(dur);
        int dots = intValue(xml.attr(element, "dots"), 0);
        Fraction base = converter.durationOf(dur);
        if (base == null) {
            base = Fraction.of(1, 4);
        }
        event.setType(converter.typeOf(dur));
        event.setDots(dots);
        event.setDuration(converter.dotted(base, dots).multiply(ratio));
        String graceType = firstNonNull(xml.attr(element, "grace"), grace);
        if (graceType != null) {
            event.setGrace(true);
            event.setGraceSlash("unacc".equals(graceType) || "1slash".equals(xml.attr(element, "stem.mod")));
        }
    }


    /**
     * Applies the attributes and children shared by notes and chords: articulations, stem,
     * cross-staff, lyrics, fermata and the ID.
     *
     * @param element the note or chord element
     * @param event the event
     * @param context the layer context
     */
    private void applyEventAttributes(Element element, EventData event, MeiLayerContext context) {
        applyArticulations(element, event);
        String stem = xml.attr(element, "stem.dir");
        if ("up".equals(stem) || "down".equals(stem)) {
            event.setStem(stem);
        }
        String staffNumber = xml.attr(element, "staff");
        if (staffNumber != null) {
            MeiStaffDef target = staffDefs.get(staffNumber.trim().split("\\s+")[0]);
            if (target != null && target.getPartIndex() == context.getStaffDef().getPartIndex()
                    && target.getStaffInPart() != context.getStaffDef().getStaffInPart()) {
                event.setStaff(target.getStaffInPart());
            }
        }
        readLyrics(element, event);
        applyFermataAttribute(element, event);
    }


    /**
     * Applies the articulations of an element (attribute and artic children).
     *
     * @param element the element
     * @param event the event
     */
    private void applyArticulations(Element element, EventData event) {
        converter.applyArticulations(xml.attr(element, "artic"), event);
        for (Element artic : xml.children(element, "artic")) {
            converter.applyArticulations(xml.attr(artic, "artic"), event);
        }
    }


    /**
     * Applies a fermata attribute (above or below) of a note, chord or rest.
     *
     * @param element the element
     * @param event the event
     */
    private void applyFermataAttribute(Element element, EventData event) {
        String fermata = xml.attr(element, "fermata");
        if (fermata != null) {
            event.getFermatas().add(builder.createFermata(null, "below".equals(fermata)));
        }
    }


    /**
     * Reads a note: pitch with gestural and written accidentals, tie attribute, notehead and ID.
     *
     * @param element the note element
     * @param context the layer context
     * @param event the event of the note
     * @return the note data
     */
    private NoteData readNote(Element element, MeiLayerContext context, EventData event) {
        String step = xml.attr(element, "pname");
        if (step == null) {
            step = "c";
        }
        step = step.toUpperCase();
        int octave = intValue(xml.attr(element, "oct"), 4);
        String written = xml.attr(element, "accid");
        String gestural = xml.attr(element, "accid.ges");
        for (Element accid : xml.children(element, "accid")) {
            written = firstNonNull(xml.attr(accid, "accid"), written);
            gestural = firstNonNull(xml.attr(accid, "accid.ges"), gestural);
        }
        String pitchKey = step + octave;
        Integer alter = converter.alterOf(gestural);
        if (written != null && converter.alterOf(written) != null) {
            context.getAccidentals().put(pitchKey, converter.alterOf(written));
            if (alter == null) {
                alter = converter.alterOf(written);
            }
        }
        if (alter == null) {
            alter = context.getAccidentals().get(pitchKey);
        }
        if (alter == null) {
            int fifths = 0;
            if (context.getStaffDef().getKeyFifths() != null) {
                fifths = context.getStaffDef().getKeyFifths();
            }
            alter = PitchSpeller.getInstance().keyAlter(step, fifths);
        }
        NoteData note = new NoteData(step, alter, octave);
        note.setAccidental(converter.accidentalName(written));
        ClefImpl clef = context.getStaffDef().getClef();
        if (clef != null && "percussion".equals(clef.getSign())) {
            note.setUnpitched(true);
        }
        String tie = xml.attr(element, "tie");
        if (tie != null) {
            note.setTieStart(tie.contains("i") || tie.contains("m"));
            note.setTieStop(tie.contains("t") || tie.contains("m"));
        }
        String head = xml.attr(element, "head.shape");
        if (head != null) {
            switch (head) {
                case "x":
                case "+":
                    note.setNotehead("x");
                    break;
                case "diamond":
                    note.setNotehead("diamond");
                    break;
                case "slash":
                    note.setNotehead("slash");
                    break;
                default:
                    break;
            }
        }
        register(element, event, note, context);
        return note;
    }


    /**
     * Reads the verses (lyrics) of a note or chord.
     *
     * @param element the note or chord element
     * @param event the event
     */
    private void readLyrics(Element element, EventData event) {
        for (Element verse : xml.children(element, "verse")) {
            final int number = intValue(xml.attr(verse, N), 1);
            List<Element> syllables = xml.descendants(verse, "syl");
            if (syllables.isEmpty()) {
                continue;
            }
            StringBuilder text = new StringBuilder();
            for (Element syl : syllables) {
                if (text.length() > 0) {
                    text.append('‿');
                }
                text.append(xml.text(syl));
            }
            Element first = syllables.get(0);
            Element last = syllables.get(syllables.size() - 1);
            String start = xml.attr(first, "wordpos");
            String end = xml.attr(last, "wordpos");
            String syllabic = "single";
            if ("i".equals(start)) {
                syllabic = "begin";
            } else if ("m".equals(start) || "m".equals(end)) {
                syllabic = "middle";
            } else if ("t".equals(end)) {
                syllabic = "end";
            }
            event.getLyrics().add(builder.createLyric(number, text.toString(), syllabic, "u".equals(xml.attr(last, "con"))));
        }
    }


    /**
     * Copies the events of the same voice of the previous measure (measure repeat).
     *
     * @param context the layer context
     */
    private void copyPreviousMeasure(MeiLayerContext context) {
        int index = context.getMeasureIndex();
        if (index == 0) {
            return;
        }
        PartData part = score.getParts().get(context.getStaffDef().getPartIndex());
        MeasureData previous = part.getMeasures().get(index - 1);
        for (VoiceData voice : previous.getVoices()) {
            if (voice.getStaff() != context.getVoice().getStaff() || voice.getVoice() != context.getVoice().getVoice()) {
                continue;
            }
            for (EventData source : voice.getEvents()) {
                context.getVoice().getEvents().add(copyEvent(source));
            }
        }
    }


    /**
     * Copies the timing and pitches of an event (without ties, slurs, lyrics and directions).
     *
     * @param source the event
     * @return the copy
     */
    private EventData copyEvent(EventData source) {
        EventData copy = new EventData();
        copy.setDuration(source.getDuration());
        copy.setType(source.getType());
        copy.setDots(source.getDots());
        copy.setTuplet(source.getTupletActual(), source.getTupletNormal());
        copy.setTupletStart(source.isTupletStart());
        copy.setTupletStop(source.isTupletStop());
        copy.setGrace(source.isGrace());
        copy.setGraceSlash(source.isGraceSlash());
        copy.setMeasureRest(source.isMeasureRest());
        copy.setInvisible(source.isInvisible());
        copy.setStaff(source.getStaff());
        for (NoteData note : source.getNotes()) {
            NoteData noteCopy = new NoteData(note.getStep(), note.getAlter(), note.getOctave());
            noteCopy.setUnpitched(note.isUnpitched());
            noteCopy.setNotehead(note.getNotehead());
            copy.getNotes().add(noteCopy);
        }
        return copy;
    }


    /**
     * Applies the tuplet spans: the durations of the events from the start to the end are scaled.
     */
    private void applyTupletSpans() {
        for (MeiControlEvent controlEvent : controlEvents) {
            Element element = controlEvent.getElement();
            if (!"tupletSpan".equals(xml.name(element))) {
                continue;
            }
            MeiEventRef start = ref(xml.attr(element, "startid"));
            MeiEventRef end = ref(xml.attr(element, "endid"));
            int num = intValue(xml.attr(element, "num"), 0);
            int numbase = intValue(xml.attr(element, "numbase"), 0);
            if (start == null || end == null || start.getVoice() != end.getVoice() || num <= 0 || numbase <= 0) {
                continue;
            }
            List<EventData> events = start.getVoice().getEvents();
            int from = indexOfIdentical(events, start.getEvent());
            int to = indexOfIdentical(events, end.getEvent());
            if (from < 0 || to < from) {
                continue;
            }
            for (int i = from; i <= to; i++) {
                EventData event = events.get(i);
                if (event.getDuration() != null && !event.isGrace()) {
                    event.setDuration(event.getDuration().multiply(numbase, num));
                }
                event.setTuplet(num, numbase);
            }
            events.get(from).setTupletStart(true);
            events.get(to).setTupletStop(true);
        }
    }


    /**
     * Calculates the position of every event from the start of its measure.
     */
    private void calculatePositions() {
        for (PartData part : score.getParts()) {
            for (MeasureData measure : part.getMeasures()) {
                for (VoiceData voice : measure.getVoices()) {
                    Fraction cursor = Fraction.ZERO;
                    for (EventData event : voice.getEvents()) {
                        positions.put(event, cursor);
                        if (!event.isGrace() && event.getDuration() != null) {
                            cursor = cursor.add(event.getDuration());
                        }
                    }
                }
            }
        }
    }


    /**
     * Applies one control event.
     *
     * @param controlEvent the control event
     */
    private void applyControlEvent(MeiControlEvent controlEvent) {
        Element element = controlEvent.getElement();
        MeiEventRef start = ref(xml.attr(element, "startid"));
        MeiEventRef end = ref(xml.attr(element, "endid"));
        if (start == null) {
            start = eventAt(element, controlEvent.getMeasureIndex(), "tstamp", controlEvent.getBeatUnit());
        }
        switch (xml.name(element)) {
            case "tie":
                applyTie(start, end);
                break;
            case "slur":
            case "phrase":
                applySlur(element, controlEvent, start, end);
                break;
            case "dynam":
                addDirection(element, controlEvent, start, dynamics(element));
                break;
            case "dir":
                String dirText = xml.text(element);
                if (!dirText.isEmpty()) {
                    addDirection(element, controlEvent, start, builder.createWords(dirText, placement(element), ITALIC, null));
                }
                break;
            case "tempo":
                addDirection(element, controlEvent, start, tempo(element));
                break;
            case "reh":
                addDirection(element, controlEvent, start, builder.createRehearsal(xml.text(element)));
                break;
            case "repeatMark":
                DirectionImpl repeat = repeatMark(element);
                if (repeat != null) {
                    addDirection(element, controlEvent, start, repeat);
                }
                break;
            case "hairpin":
                applyHairpin(element, controlEvent, start, end);
                break;
            case "octave":
                applyOctave(element, controlEvent, start, end);
                break;
            case "pedal":
                applyPedal(element, controlEvent, start);
                break;
            case "fermata":
                if (start != null) {
                    start.getEvent().getFermatas().add(builder.createFermata(fermataShape(xml.attr(element, "shape")),
                            "below".equals(xml.attr(element, "place")) || "inv".equals(xml.attr(element, "form"))));
                }
                break;
            case "trill":
                if (start != null) {
                    start.getEvent().ornaments().setTrillMark(EMPTY);
                }
                break;
            case "mordent":
                if (start != null) {
                    if ("upper".equals(xml.attr(element, "form"))) {
                        start.getEvent().ornaments().setInvertedMordent(EMPTY);
                    } else {
                        start.getEvent().ornaments().setMordent(EMPTY);
                    }
                }
                break;
            case "turn":
                applyTurn(element, start);
                break;
            case "breath":
                if (start != null) {
                    start.getEvent().articulations().setBreathMark(EMPTY);
                }
                break;
            case "caesura":
                if (start != null) {
                    start.getEvent().articulations().setCaesura(EMPTY);
                }
                break;
            case "harm":
                applyHarmony(element, controlEvent, start);
                break;
            case "fing":
                if (start != null) {
                    if (start.getNote() != null) {
                        start.getNote().technical().setFingering(xml.text(element));
                    } else {
                        start.getEvent().technical().setFingering(xml.text(element));
                    }
                }
                break;
            case "gliss":
                if (start != null && end != null && start.getNote() != null && end.getNote() != null) {
                    start.getNote().setGlissandoStart(true);
                    end.getNote().setGlissandoStop(true);
                }
                break;
            default:
                break;
        }
    }


    /**
     * Applies a tie between two notes (or all notes of two chords).
     *
     * @param start the start reference
     * @param end the end reference
     */
    private void applyTie(MeiEventRef start, MeiEventRef end) {
        if (start == null) {
            return;
        }
        if (start.getNote() != null) {
            start.getNote().setTieStart(true);
        } else {
            for (NoteData note : start.getEvent().getNotes()) {
                note.setTieStart(true);
            }
        }
        if (end == null) {
            return;
        }
        if (end.getNote() != null) {
            end.getNote().setTieStop(true);
        } else {
            for (NoteData note : end.getEvent().getNotes()) {
                note.setTieStop(true);
            }
        }
    }


    /**
     * Applies a slur (or phrase mark).
     *
     * @param element the slur element
     * @param controlEvent the control event
     * @param start the start reference
     * @param end the end reference
     */
    private void applySlur(Element element, MeiControlEvent controlEvent, MeiEventRef start, MeiEventRef end) {
        MeiEventRef last = end;
        if (last == null) {
            last = eventAt2(element, controlEvent);
        }
        if (start == null || last == null) {
            return;
        }
        final int number = nextNumber(slurNumbers, start.getPartIndex());
        start.getEvent().getSlurStarts().add(number);
        last.getEvent().getSlurStops().add(number);
    }


    /**
     * Applies a hairpin (crescendo or diminuendo wedge).
     *
     * @param element the hairpin element
     * @param controlEvent the control event
     * @param start the start reference
     * @param end the end reference
     */
    private void applyHairpin(Element element, MeiControlEvent controlEvent, MeiEventRef start, MeiEventRef end) {
        int partIndex = partIndexOf(element, start);
        if (partIndex < 0) {
            return;
        }
        final int number = nextNumber(wedgeNumbers, partIndex);
        String type = "crescendo";
        if ("dim".equals(xml.attr(element, "form")) || "decres".equals(xml.attr(element, "form"))) {
            type = "diminuendo";
        }
        addDirection(element, controlEvent, start, builder.createWedge(type, number));
        addEndDirection(element, controlEvent, end, builder.createWedge(ScoreBuilder.STOP, number));
    }


    /**
     * Applies an octave line (8va, 8vb, 15ma and so on).
     *
     * @param element the octave element
     * @param controlEvent the control event
     * @param start the start reference
     * @param end the end reference
     */
    private void applyOctave(Element element, MeiControlEvent controlEvent, MeiEventRef start, MeiEventRef end) {
        int size = intValue(xml.attr(element, "dis"), 8);
        String type = "down";
        if ("below".equals(xml.attr(element, "dis.place"))) {
            type = "up";
        }
        addDirection(element, controlEvent, start, builder.createOctaveShift(type, size));
        addEndDirection(element, controlEvent, end, builder.createOctaveShift(ScoreBuilder.STOP, size));
    }


    /**
     * Applies a pedal mark.
     *
     * @param element the pedal element
     * @param controlEvent the control event
     * @param start the start reference
     */
    private void applyPedal(Element element, MeiControlEvent controlEvent, MeiEventRef start) {
        String dir = xml.attr(element, "dir");
        if ("up".equals(dir)) {
            addDirection(element, controlEvent, start, builder.createPedal(ScoreBuilder.STOP));
        } else {
            addDirection(element, controlEvent, start, builder.createPedal(ScoreBuilder.START));
        }
    }


    /**
     * Applies a turn ornament.
     *
     * @param element the turn element
     * @param start the start reference
     */
    private void applyTurn(Element element, MeiEventRef start) {
        if (start == null) {
            return;
        }
        boolean delayed = "true".equals(xml.attr(element, "delayed"));
        boolean inverted = "lower".equals(xml.attr(element, "form"));
        if (delayed && inverted) {
            start.getEvent().ornaments().setDelayedInvertedTurn(EMPTY);
        } else if (delayed) {
            start.getEvent().ornaments().setDelayedTurn(EMPTY);
        } else if (inverted) {
            start.getEvent().ornaments().setInvertedTurn(EMPTY);
        } else {
            start.getEvent().ornaments().setTurn(EMPTY);
        }
    }


    /**
     * Applies a chord symbol.
     *
     * @param element the harm element
     * @param controlEvent the control event
     * @param start the start reference
     */
    private void applyHarmony(Element element, MeiControlEvent controlEvent, MeiEventRef start) {
        HarmonyImpl harmony = ChordSymbolParser.getInstance().parse(xml.text(element));
        if (harmony == null) {
            return;
        }
        if (start != null) {
            start.getEvent().getHarmonies().add(harmony);
            return;
        }
        int partIndex = partIndexOf(element, null);
        if (partIndex >= 0) {
            score.getParts().get(partIndex).measure(controlEvent.getMeasureIndex()).getHarmonies().add(harmony);
        }
    }


    /**
     * Creates the direction of a dynam element: a dynamics marking or italic text.
     *
     * @param element the dynam element
     * @return the direction
     */
    private DirectionImpl dynamics(Element element) {
        String text = xml.text(element);
        String marking = firstNonNull(xml.attr(element, "label"), text).trim();
        DirectionImpl direction;
        if (DYNAMIC_MARKING.matcher(marking).matches()) {
            direction = builder.createDynamics(marking);
        } else {
            direction = builder.createWords(text, ScoreBuilder.BELOW, ITALIC, null);
        }
        if (xml.attr(element, "place") != null) {
            direction.setPlacement(placement(element));
        }
        return direction;
    }


    /**
     * Creates the direction of a tempo element: text, metronome mark and sound tempo.
     *
     * @param element the tempo element
     * @return the direction
     */
    private DirectionImpl tempo(Element element) {
        String text = PRIVATE_USE.matcher(xml.text(element)).replaceAll("");
        text = TEMPO_EQUATION.matcher(text).replaceAll("").trim();
        double bpm = doubleValue(xml.attr(element, "mm"));
        String unit = "quarter";
        int dots = 0;
        if (text.isEmpty()) {
            text = null;
        }
        if (bpm > 0 && xml.attr(element, "mm.unit") != null) {
            String type = converter.typeOf(xml.attr(element, "mm.unit"));
            if (type != null) {
                unit = type;
            }
            dots = intValue(xml.attr(element, "mm.dots"), 0);
        } else if (bpm <= 0) {
            bpm = doubleValue(xml.attr(element, "midi.bpm"));
            if (bpm > 0 && text != null) {
                // midi.bpm is the playback tempo only: the text is shown without a metronome mark
                DirectionImpl direction = builder.createTempo(0, unit, 0, text);
                SoundImpl sound = new SoundImpl();
                sound.setTempo(formatTempo(bpm));
                direction.setSound(sound);
                return direction;
            }
        }
        return builder.createTempo(bpm, unit, dots, text);
    }


    /**
     * Formats a tempo for the sound tempo: without decimals if it is a whole number.
     *
     * @param bpm the tempo in quarter notes per minute
     * @return the text
     */
    private String formatTempo(double bpm) {
        return BigDecimal.valueOf(bpm).stripTrailingZeros().toPlainString();
    }


    /**
     * Creates the direction of a repeat mark (segno, coda, fine, da capo, dal segno).
     *
     * @param element the repeatMark element
     * @return the direction, or null
     */
    private DirectionImpl repeatMark(Element element) {
        String func = xml.attr(element, "func");
        String text = xml.text(element);
        if (func == null) {
            if (text.isEmpty()) {
                return null;
            }
            return builder.createWords(text, ScoreBuilder.ABOVE, ITALIC, null);
        }
        SoundImpl sound = new SoundImpl();
        DirectionImpl direction;
        switch (func) {
            case "segno":
                return builder.createSegno();
            case "coda":
                return builder.createCoda();
            case "fine":
                direction = builder.createWords(firstNonEmpty(text, "Fine"), ScoreBuilder.ABOVE, ITALIC, null);
                sound.setFine("yes");
                break;
            case "daCapo":
            case "dacapo":
                direction = builder.createWords(firstNonEmpty(text, "D.C."), ScoreBuilder.ABOVE, ITALIC, null);
                sound.setDacapo("yes");
                break;
            case "dalSegno":
            case "dalsegno":
                direction = builder.createWords(firstNonEmpty(text, "D.S."), ScoreBuilder.ABOVE, ITALIC, null);
                sound.setDalsegno("segno");
                break;
            default:
                direction = builder.createWords(firstNonEmpty(text, "To Coda"), ScoreBuilder.ABOVE, ITALIC, null);
                sound.setTocoda("coda");
                break;
        }
        direction.setSound(sound);
        return direction;
    }


    /**
     * Adds a direction at the start of a control event: at the referenced event or at its time stamp.
     *
     * @param element the control event element
     * @param controlEvent the control event
     * @param start the start reference (may be null)
     * @param direction the direction
     */
    private void addDirection(Element element, MeiControlEvent controlEvent, MeiEventRef start, DirectionImpl direction) {
        if (xml.attr(element, "startid") != null && start != null) {
            start.getEvent().getDirections().add(direction);
            return;
        }
        MeiStaffDef staffDef = staffDefOf(element, start);
        if (staffDef == null) {
            return;
        }
        Fraction position = timestamp(xml.attr(element, "tstamp"), controlEvent.getBeatUnit());
        score.getParts().get(staffDef.getPartIndex()).measure(controlEvent.getMeasureIndex())
                .addDirection(direction, position, staffDef.getStaffInPart());
    }


    /**
     * Adds a direction at the end of a spanner: at the end event or at the tstamp2 position.
     *
     * @param element the control event element
     * @param controlEvent the control event
     * @param end the end reference (may be null)
     * @param direction the direction
     */
    private void addEndDirection(Element element, MeiControlEvent controlEvent, MeiEventRef end, DirectionImpl direction) {
        if (end != null) {
            end.getEvent().getDirections().add(direction);
            return;
        }
        String tstamp2 = xml.attr(element, "tstamp2");
        MeiStaffDef staffDef = staffDefOf(element, null);
        if (tstamp2 == null || staffDef == null) {
            return;
        }
        int measures = 0;
        String beat = tstamp2;
        int plus = tstamp2.indexOf("m+");
        if (plus >= 0) {
            measures = intValue(tstamp2.substring(0, plus), 0);
            beat = tstamp2.substring(plus + 2);
        }
        int index = Math.min(controlEvent.getMeasureIndex() + measures, Math.max(0, beatUnits.size() - 1));
        Fraction position = timestamp(beat, beatUnits.get(index));
        score.getParts().get(staffDef.getPartIndex()).measure(index).addDirection(direction, position, staffDef.getStaffInPart());
    }


    /**
     * Finds the event at the tstamp of a control event.
     *
     * @param element the control event element
     * @param index the measure index
     * @param attribute the time stamp attribute (tstamp)
     * @param beatUnit the length of one beat
     * @return the event reference, or null
     */
    private MeiEventRef eventAt(Element element, int index, String attribute, Fraction beatUnit) {
        String tstamp = xml.attr(element, attribute);
        MeiStaffDef staffDef = staffDefOf(element, null);
        if (tstamp == null || staffDef == null) {
            return null;
        }
        return findEvent(staffDef, index, timestamp(tstamp, beatUnit));
    }


    /**
     * Finds the event at the tstamp2 (end) of a control event.
     *
     * @param element the control event element
     * @param controlEvent the control event
     * @return the event reference, or null
     */
    private MeiEventRef eventAt2(Element element, MeiControlEvent controlEvent) {
        String tstamp2 = xml.attr(element, "tstamp2");
        MeiStaffDef staffDef = staffDefOf(element, null);
        if (tstamp2 == null || staffDef == null) {
            return null;
        }
        int measures = 0;
        String beat = tstamp2;
        int plus = tstamp2.indexOf("m+");
        if (plus >= 0) {
            measures = intValue(tstamp2.substring(0, plus), 0);
            beat = tstamp2.substring(plus + 2);
        }
        int index = Math.min(controlEvent.getMeasureIndex() + measures, Math.max(0, beatUnits.size() - 1));
        return findEvent(staffDef, index, timestamp(beat, beatUnits.get(index)));
    }


    /**
     * Finds the last event of a staff that starts at or before a position.
     *
     * @param staffDef the staff
     * @param index the measure index
     * @param position the position in the measure
     * @return the event reference, or null
     */
    private MeiEventRef findEvent(MeiStaffDef staffDef, int index, Fraction position) {
        PartData part = score.getParts().get(staffDef.getPartIndex());
        if (index >= part.getMeasures().size()) {
            return null;
        }
        MeasureData measure = part.getMeasures().get(index);
        EventData best = null;
        VoiceData bestVoice = null;
        Fraction bestPosition = null;
        for (VoiceData voice : measure.getVoices()) {
            if (voice.getStaff() != staffDef.getStaffInPart()) {
                continue;
            }
            for (EventData event : voice.getEvents()) {
                Fraction eventPosition = positions.get(event);
                if (eventPosition == null || event.isGrace() || eventPosition.compareTo(position) > 0) {
                    continue;
                }
                if (bestPosition == null || eventPosition.compareTo(bestPosition) > 0) {
                    best = event;
                    bestVoice = voice;
                    bestPosition = eventPosition;
                }
            }
        }
        if (best == null) {
            return null;
        }
        return new MeiEventRef(best, null, bestVoice, staffDef.getPartIndex(), index, staffDef.getStaffInPart());
    }


    /**
     * Converts an MEI time stamp (1-based beat, may be fractional) into a position.
     *
     * @param tstamp the time stamp
     * @param beatUnit the length of one beat
     * @return the position from the measure start
     */
    private Fraction timestamp(String tstamp, Fraction beatUnit) {
        if (tstamp == null) {
            return Fraction.ZERO;
        }
        try {
            BigDecimal value = new BigDecimal(tstamp.trim()).subtract(BigDecimal.ONE);
            if (value.signum() <= 0) {
                return Fraction.ZERO;
            }
            BigDecimal scaled = value.stripTrailingZeros();
            int scale = Math.max(0, scaled.scale());
            long denominator = BigDecimal.TEN.pow(scale).longValueExact();
            long numerator = scaled.movePointRight(scale).longValueExact();
            return Fraction.of(numerator, denominator).multiply(beatUnit);
        } catch (NumberFormatException | ArithmeticException e) {
            return Fraction.ZERO;
        }
    }


    /**
     * Gets the staff of a control event: from the start reference or the staff attribute.
     *
     * @param element the control event element
     * @param start the start reference (may be null)
     * @return the staff definition, or null
     */
    private MeiStaffDef staffDefOf(Element element, MeiEventRef start) {
        String staff = xml.attr(element, "staff");
        if (staff != null) {
            MeiStaffDef staffDef = staffDefs.get(staff.trim().split("\\s+")[0]);
            if (staffDef != null) {
                return staffDef;
            }
        }
        if (start != null) {
            for (MeiStaffDef staffDef : staffDefs.values()) {
                if (staffDef.getPartIndex() == start.getPartIndex() && staffDef.getStaffInPart() == start.getStaff()) {
                    return staffDef;
                }
            }
        }
        if (staffDefs.isEmpty()) {
            return null;
        }
        return staffDefs.values().iterator().next();
    }


    /**
     * Gets the part index of a control event.
     *
     * @param element the control event element
     * @param start the start reference (may be null)
     * @return the part index, or -1
     */
    private int partIndexOf(Element element, MeiEventRef start) {
        if (start != null) {
            return start.getPartIndex();
        }
        MeiStaffDef staffDef = staffDefOf(element, null);
        if (staffDef == null) {
            return -1;
        }
        return staffDef.getPartIndex();
    }


    /**
     * Gets the next spanner number of a part (1 to 6, cycling).
     *
     * @param counters the counters by part
     * @param partIndex the part index
     * @return the number
     */
    private int nextNumber(Map<Integer, int[]> counters, int partIndex) {
        int[] counter = counters.computeIfAbsent(partIndex, k -> new int[1]);
        counter[0] = counter[0] % MAX_NUMBER + 1;
        return counter[0];
    }


    /**
     * Gets the MusicXML fermata shape of an MEI shape.
     *
     * @param shape the MEI shape (curved, square, angular)
     * @return the MusicXML shape, or null for normal
     */
    private String fermataShape(String shape) {
        if ("square".equals(shape)) {
            return "square";
        }
        if ("angular".equals(shape)) {
            return "angled";
        }
        return null;
    }


    /**
     * Gets the placement of a control event.
     *
     * @param element the element
     * @return above, below or null
     */
    private String placement(Element element) {
        String place = xml.attr(element, "place");
        if ("above".equals(place) || "below".equals(place)) {
            return place;
        }
        return null;
    }


    /**
     * Resolves an ID reference (with or without leading #).
     *
     * @param reference the reference
     * @return the event reference, or null
     */
    private MeiEventRef ref(String reference) {
        if (reference == null) {
            return null;
        }
        String id = reference.trim();
        int hash = id.lastIndexOf('#');
        if (hash >= 0) {
            id = id.substring(hash + 1);
        }
        return ids.get(id);
    }


    /**
     * Finds the index of an object in a list (compared by identity).
     *
     * @param events the list
     * @param event the object
     * @return the index, or -1
     */
    private int indexOfIdentical(List<EventData> events, EventData event) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) == event) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Adds all events to a collector.
     *
     * @param collector the collector (may be null)
     * @param events the events
     */
    private void addAll(List<EventData> collector, List<EventData> events) {
        if (collector != null) {
            collector.addAll(events);
        }
    }


    /**
     * Gets the label of a staffDef or staffGrp (attribute or label child).
     *
     * @param element the element
     * @return the label, or null
     */
    private String labelOf(Element element) {
        Element label = xml.child(element, "label");
        if (label != null && !xml.text(label).isEmpty()) {
            return xml.text(label);
        }
        return xml.attr(element, "label");
    }


    /**
     * Gets the label abbreviation of a staffDef or staffGrp.
     *
     * @param element the element
     * @return the abbreviation, or null
     */
    private String labelAbbrOf(Element element) {
        Element label = xml.child(element, "labelAbbr");
        if (label != null && !xml.text(label).isEmpty()) {
            return xml.text(label);
        }
        return xml.attr(element, "label.abbr");
    }


    /**
     * Gets the MIDI program of the instrDef child of a group.
     *
     * @param element the element
     * @return the program (1-based), or null
     */
    private Integer programOf(Element element) {
        Element instrDef = xml.child(element, "instrDef");
        if (instrDef == null) {
            return null;
        }
        int program = intValue(xml.attr(instrDef, "midi.instrnum"), -1);
        if (program < 0 || program > 127) {
            return null;
        }
        return program + 1;
    }


    /**
     * Gets the xml:id of an element.
     *
     * @param element the element
     * @return the ID, or null
     */
    private String idOf(Element element) {
        String id = element.getAttributeNS(XML_NAMESPACE, "id");
        if (id == null || id.isEmpty()) {
            id = element.getAttribute("xml:id");
        }
        if (id == null || id.isEmpty()) {
            return null;
        }
        return id;
    }


    /**
     * Parses an integer.
     *
     * @param text the text (may be null)
     * @param defaultValue the default for invalid text
     * @return the value
     */
    private int intValue(String text, int defaultValue) {
        if (text == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * Parses a decimal number.
     *
     * @param text the text (may be null)
     * @return the value, or 0 if invalid
     */
    private double doubleValue(String text) {
        if (text == null) {
            return 0;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    /**
     * Returns the first non-null value.
     *
     * @param first the first value
     * @param second the second value
     * @return the first non-null value, or null
     */
    private String firstNonNull(String first, String second) {
        if (first != null) {
            return first;
        }
        return second;
    }


    /**
     * Returns the text if it is not empty, otherwise the default.
     *
     * @param text the text
     * @param defaultText the default
     * @return the text or the default
     */
    private String firstNonEmpty(String text, String defaultText) {
        if (text == null || text.isEmpty()) {
            return defaultText;
        }
        return text;
    }
}
