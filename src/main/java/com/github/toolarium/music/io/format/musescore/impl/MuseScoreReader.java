/*
 * MuseScoreReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import com.github.toolarium.music.io.converter.DynamicsConverter;
import com.github.toolarium.music.io.converter.EncodingConverter;
import com.github.toolarium.music.io.converter.InstrumentSoundConverter;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.Event;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.KeyData;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.MeasureData;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.MeasureFrame;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.NoteData;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.PartDirections;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.PartInfo;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.PositionedDirection;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.ReadContext;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.SpannerData;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.StaffData;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.TimeSignature;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.TupletState;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.VoiceState;
import com.github.toolarium.music.io.format.musescore.impl.MuseScoreReaderTypes.VoltaData;
import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.barline.impl.EndingImpl;
import com.github.toolarium.music.model.barline.impl.RepeatImpl;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.DirectionTypeImpl;
import com.github.toolarium.music.model.direction.impl.MetronomeImpl;
import com.github.toolarium.music.model.direction.impl.OctaveShiftImpl;
import com.github.toolarium.music.model.direction.impl.PedalImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.direction.impl.WedgeImpl;
import com.github.toolarium.music.model.direction.impl.WordsImpl;
import com.github.toolarium.music.model.harmony.impl.BassImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.harmony.impl.RootImpl;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.BackupImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.measure.impl.TransposeImpl;
import com.github.toolarium.music.model.notation.impl.ArticulationsImpl;
import com.github.toolarium.music.model.notation.impl.FermataImpl;
import com.github.toolarium.music.model.notation.impl.NotationsImpl;
import com.github.toolarium.music.model.notation.impl.OrnamentsImpl;
import com.github.toolarium.music.model.notation.impl.PlayingTechniqueImpl;
import com.github.toolarium.music.model.notation.impl.SlurImpl;
import com.github.toolarium.music.model.notation.impl.TiedImpl;
import com.github.toolarium.music.model.notation.impl.TupletImpl;
import com.github.toolarium.music.model.note.impl.GraceImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import com.github.toolarium.music.model.note.impl.PitchImpl;
import com.github.toolarium.music.model.note.impl.RestImpl;
import com.github.toolarium.music.model.note.impl.TieImpl;
import com.github.toolarium.music.model.note.impl.TimeModificationImpl;
import com.github.toolarium.music.model.note.impl.UnpitchedImpl;
import com.github.toolarium.music.model.part.impl.MidiInstrumentImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.part.impl.ScoreInstrumentImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.CreatorImpl;
import com.github.toolarium.music.model.score.impl.CreditImpl;
import com.github.toolarium.music.model.score.impl.EncodingImpl;
import com.github.toolarium.music.model.score.impl.IdentificationImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import com.github.toolarium.music.model.score.impl.WorkImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Converts a MuseScore score document (.mscx content) of any version from 1.x to 4.x into an
 * {@link IScore}.
 *
 * <p>MuseScore stores one sequence of elements per staff and measure. Files of version 3 and 4
 * group the elements of each voice in {@code voice} elements and position them relative to the
 * previous element ({@code location} moves the position). Files of version 1 and 2 list all
 * elements of a measure directly and set positions and voices with {@code tick} and
 * {@code track}. Both forms are read into the same time line: positions are fractions of a whole
 * note, pitches are concert MIDI pitches spelled by tonal pitch classes.</p>
 *
 * @author patrick
 */
public final class MuseScoreReader {
    static final int VOICES_PER_STAFF = 4;
    static final int DEFAULT_DIVISION = 480;
    static final long MAX_DIVISIONS = 1L << 20;
    static final int MAX_NUMBER = 6;
    static final Fraction QUARTERS_PER_WHOLE = Fraction.of(4, 1);
    static final String CLEF_TREBLE = "G";
    private static final Logger LOG = LoggerFactory.getLogger(MuseScoreReader.class);
    private static final int MAX_MEASURES = 100000;
    private static final int MIDI_MAX = 127;
    private static final double SECONDS_PER_MINUTE = 60.0;
    private static final Fraction WHOLE = Fraction.of(1, 1);
    private static final String ATTR_ID = "id";
    private static final String YES = "yes";
    private static final String TICK = "tick";
    private static final String CHORD = "Chord";
    private static final String METRONOME_TEXT = "\\s*[♩♪♫]\\.?\\s*=\\s*\\d+\\s*";
    private static final String REST = "Rest";
    private static final String NOTE = "Note";
    private static final String SUBTYPE = "subtype";
    private static final String TEXT = "text";
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String ABOVE = "above";
    private static final String BELOW = "below";
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String EMPTY = "";
    private static final String MEASURE = "measure";
    private static final String SPANNER = "Spanner";
    private static final String TYPE = "type";
    private static final String TUPLET = "Tuplet";
    private static final String[] LEGACY_CLEFS = {"G", "G8va", "G15ma", "G8vb", "F", "F8vb", "F15mb", "F3", "F5", "C1", "C2", "C3",
        "C4", "TAB", "PERC", "C5", "G1", "F8va", "F15ma", "PERC2", "TAB2"};
    private static final String[] GRACE_TYPES = {"acciaccatura", "appoggiatura", "grace4", "grace16", "grace32", "grace8after",
        "grace16after", "grace32after"};
    private static final Map<String, String> LEGACY_ARTICULATIONS = createLegacyArticulations();
    private static final Map<String, String> SYMBOLS = createSymbols();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MuseScoreReader INSTANCE = new MuseScoreReader();
    }


    /**
     * Constructor.
     */
    private MuseScoreReader() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MuseScoreReader getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Reads a MuseScore score document.
     *
     * @param mscx the content of the .mscx file
     * @return the score
     * @throws IOException if the document is not a valid MuseScore score
     */
    public IScore read(byte[] mscx) throws IOException {
        return read(mscx, null);
    }


    /**
     * Reads a MuseScore score document with its style file.
     *
     * @param mscx the content of the .mscx file
     * @param style the content of the style file (score_style.mss), or null if there is none
     * @return the score
     * @throws IOException if the document is not a valid MuseScore score
     */
    public IScore read(byte[] mscx, byte[] style) throws IOException {
        Document document = parse(mscx);
        Element root = document.getDocumentElement();
        if (root == null || !"museScore".equals(root.getTagName())) {
            throw new IOException("Not a MuseScore document (root element museScore expected).");
        }
        Element scoreElement = child(root, "Score");
        if (scoreElement == null && child(root, "Part") != null) {
            // version 1 files have no Score element: the parts and staves are children of the root
            scoreElement = root;
        }
        if (scoreElement == null) {
            throw new IOException("MuseScore document contains no Score element.");
        }

        // the page style is in the score (versions 1 to 3) or in the style file (version 4)
        Element styleElement = child(scoreElement, "Style");
        if (styleElement == null && style != null) {
            try {
                Element styleRoot = parse(style).getDocumentElement();
                if (styleRoot != null) {
                    styleElement = child(styleRoot, "Style");
                }
            } catch (IOException e) {
                // the style file only holds the layout: the score is read with the default page style
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The style file of the MuseScore document is invalid, the default page style is used: {}", e.getMessage(), e);
                }
                LOG.warn("The style file of the MuseScore document is invalid, the default page style is used: {}", e.getMessage());
            }
        }

        ReadContext context = new ReadContext(root.getAttribute("version"), intValue(child(scoreElement, "Division"), DEFAULT_DIVISION));
        try {
            return convert(scoreElement, styleElement, context);
        } catch (ArithmeticException | IllegalArgumentException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid MuseScore document: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid MuseScore document: " + e.getMessage(), e);
        } catch (StackOverflowError e) {
            // the texts are read recursively: a document with absurdly deep nesting is rejected like any invalid one
            if (LOG.isDebugEnabled()) {
                LOG.debug("MuseScore document is nested too deeply.", e);
            }
            throw new IOException("Invalid MuseScore document: nested too deeply.", e);
        }
    }


    /**
     * Converts the Score element.
     *
     * @param scoreElement the Score element
     * @param styleElement the Style element with the page style, or null
     * @param context the read context
     * @return the score
     * @throws IOException if the score is too large
     */
    private IScore convert(Element scoreElement, Element styleElement, ReadContext context) throws IOException {
        Map<String, String> metaTags = new HashMap<>();
        Set<String> metaTagNames = new HashSet<>();
        for (Element metaTag : children(scoreElement, "metaTag")) {
            metaTagNames.add(metaTag.getAttribute("name"));
            String value = metaTag.getTextContent();
            if (value != null && !value.trim().isEmpty()) {
                metaTags.put(metaTag.getAttribute("name"), value.trim());
            }
        }

        List<PartInfo> parts = new ArrayList<>();
        int[] staffCounter = new int[1];
        for (Element part : children(scoreElement, "Part")) {
            parts.add(readPart(part, staffCounter));
        }

        // the staff contents follow the part definitions as Staff elements of the Score
        Map<String, Element> staffContents = new LinkedHashMap<>();
        Map<String, String> frameTexts = new HashMap<>();
        for (Element staff : children(scoreElement, "Staff")) {
            staffContents.put(staff.getAttribute(ATTR_ID), staff);
            readFrameTexts(staff, frameTexts);
        }
        if (parts.isEmpty() || staffContents.isEmpty()) {
            throw new IOException("MuseScore document contains no parts or staves.");
        }

        readScoreLists(scoreElement, context);
        Element firstStaff = staffContents.values().iterator().next();
        List<MeasureFrame> frames = buildFrames(firstStaff, new ArrayList<>(staffContents.values()), context);
        context.setFrames(frames);

        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");
        buildIdentification(score, metaTags, metaTagNames.contains("lyricist"), frameTexts, context);

        PartListImpl partList = new PartListImpl();
        List<ScorePartImpl> scoreParts = new ArrayList<>();
        List<PartImpl> partImpls = new ArrayList<>();
        for (int p = 0; p < parts.size(); p++) {
            PartInfo part = parts.get(p);
            String partId = "P" + (p + 1);
            List<StaffData> staves = new ArrayList<>();
            for (int s = 0; s < part.getStaffIds().size(); s++) {
                Element content = staffContents.get(part.getStaffIds().get(s));
                if (content == null) {
                    continue;
                }
                staves.add(readStaff(content, part, s, context));
            }
            if (staves.isEmpty()) {
                continue;
            }
            scoreParts.add(buildScorePart(partId, part));
            partImpls.add(buildPart(partId, part, staves, context, p == 0));
        }
        partList.setScoreParts(scoreParts);
        score.setPartList(partList);
        score.setParts(partImpls);
        List<CreditImpl> credits = MuseScorePageTextReader.getInstance().readCredits(firstStaff, styleElement, metaTags, staffContents.size(), frames.size());
        if (!credits.isEmpty()) {
            score.setDefaults(MuseScorePageTextReader.getInstance().readDefaults(styleElement));
            score.setCredits(credits);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Converted MuseScore {} score: {} parts, {} measures.", context.getVersion(), partImpls.size(), frames.size());
        }
        return score;
    }


    /**
     * Reads the time signature and tempo lists of version 1 files.
     *
     * @param scoreElement the score element
     * @param context the read context
     */
    private void readScoreLists(Element scoreElement, ReadContext context) {
        for (Element sig : children(child(scoreElement, "siglist"), "sig")) {
            int nom = intValue(child(sig, "nom"), 0);
            int denom = intValue(child(sig, "denom"), 0);
            if (nom > 0 && denom > 0) {
                context.getSigList().put(intValue(sig.getAttribute(TICK), 0), new TimeSignature(nom, denom));
            }
        }
        for (Element tempo : children(child(scoreElement, "tempolist"), "tempo")) {
            double value = doubleValue(tempo);
            if (value > 0) {
                context.getTempoList().put(intValue(tempo.getAttribute(TICK), 0), value);
            }
        }
    }


    /**
     * Converts a position in whole notes into ticks.
     *
     * @param position the position
     * @param context the read context
     * @return the ticks
     */
    private int wholeToTicks(Fraction position, ReadContext context) {
        Fraction ticks = position.multiply((long) context.getDivision() * 4, 1);
        return (int) (ticks.getNumerator() / ticks.getDenominator());
    }


    /**
     * Reads a part definition. Staves without an ID (version 1 and 4.7 files) are numbered in
     * score order.
     *
     * @param part the Part element
     * @param staffCounter the number of staves defined so far (updated)
     * @return the part information
     */
    private PartInfo readPart(Element part, int[] staffCounter) {
        PartInfo info = new PartInfo();
        Element instrument = child(part, "Instrument");
        info.setName(cleanText(child(instrument, "longName")));
        if (info.getName() == null) {
            info.setName(cleanText(child(part, "trackName")));
        }
        if (info.getName() == null) {
            info.setName(cleanText(child(instrument, "trackName")));
        }
        info.setAbbreviation(cleanText(child(instrument, "shortName")));
        info.setTransposeChromatic(intValue(child(instrument, "transposeChromatic"), 0));
        info.setTransposeDiatonic(intValue(child(instrument, "transposeDiatonic"), 0));
        Element channel = child(instrument, "Channel");
        if (channel != null && child(channel, "program") != null) {
            info.setProgram(intValue(child(channel, "program").getAttribute("value"), -1));
        }
        // the sound of the instrument (version 3 and later), otherwise the sound of its MIDI program
        String sound = text(child(instrument, "instrumentId"));
        if (sound.isEmpty() && info.getProgram() >= 0) {
            sound = InstrumentSoundConverter.getInstance().getSound(info.getProgram() + 1, info.getTransposeChromatic());
        }
        if (sound != null && !sound.isEmpty()) {
            info.setSound(sound);
        }
        String instrumentClef = text(child(instrument, "clef"));
        for (Element staff : children(part, "Staff")) {
            staffCounter[0]++;
            String id = staff.getAttribute(ATTR_ID);
            if (id.isEmpty()) {
                id = String.valueOf(staffCounter[0]);
            }
            info.getStaffIds().add(id);
            TreeMap<Integer, Integer> keys = new TreeMap<>();
            for (Element key : children(child(staff, "keylist"), "key")) {
                keys.put(intValue(key.getAttribute(TICK), 0), intValue(key.getAttribute("idx"), 0));
            }
            info.getKeyLists().add(keys);
            TreeMap<Integer, String> clefs = new TreeMap<>();
            for (Element listedClef : children(child(staff, "cleflist"), "clef")) {
                int index = intValue(listedClef.getAttribute("idx"), 0);
                if (index >= 0 && index < LEGACY_CLEFS.length) {
                    clefs.put(intValue(listedClef.getAttribute(TICK), 0), LEGACY_CLEFS[index]);
                }
            }
            info.getClefLists().add(clefs);
            String clef = text(child(staff, "defaultClef"));
            if (clef.isEmpty() && clefs.containsKey(0)) {
                clef = clefs.get(0);
            }
            if (clef.isEmpty()) {
                clef = instrumentClef;
            }
            info.getDefaultClefs().add(clef);
            Element staffType = child(staff, "StaffType");
            boolean percussion = staffType != null && "percussion".equals(staffType.getAttribute("group"));
            info.getPercussion().add(percussion);
        }
        if (child(instrument, "Drum") != null && info.getPercussion().isEmpty()) {
            info.getPercussion().add(true);
        }
        return info;
    }


    /**
     * Reads the texts of the frames (title, subtitle, composer, lyricist) of a staff.
     *
     * @param staff the staff content element
     * @param texts the texts by lower case style name
     */
    private void readFrameTexts(Element staff, Map<String, String> texts) {
        for (Element frame : children(staff, "VBox")) {
            for (Element text : children(frame, "Text")) {
                String style = text(child(text, "style")).toLowerCase();
                if (style.isEmpty()) {
                    style = text(child(text, SUBTYPE)).toLowerCase();
                }
                String value = cleanText(child(text, TEXT));
                if (!style.isEmpty() && value != null && !texts.containsKey(style)) {
                    texts.put(style, value);
                }
            }
        }
    }


    /**
     * Builds the measure frames (start and length of every measure). The measure lengths follow
     * the global time signatures; local time signatures (with a stretch) of single staves are ignored.
     *
     * @param staff the content element of the first staff
     * @param staves the content elements of all staves
     * @param context the read context
     * @return the measure frames
     * @throws IOException if the score has too many measures
     */
    private List<MeasureFrame> buildFrames(Element staff, List<Element> staves, ReadContext context) throws IOException {
        List<List<Element>> staffMeasures = new ArrayList<>();
        for (Element content : staves) {
            staffMeasures.add(children(content, "Measure"));
        }
        List<MeasureFrame> frames = new ArrayList<>();
        Fraction start = Fraction.ZERO;
        TimeSignature time = new TimeSignature(4, 4);
        List<Element> measures = children(staff, "Measure");
        for (int m = 0; m < measures.size(); m++) {
            final Element measure = measures.get(m);
            TimeSignature change = null;
            for (List<Element> other : staffMeasures) {
                if (m >= other.size() || change != null) {
                    continue;
                }
                for (Element element : measureElements(other.get(m))) {
                    if ("TimeSig".equals(element.getTagName()) && readStretch(element).equals(WHOLE)) {
                        TimeSignature parsed = readTimeSignature(element);
                        if (parsed != null) {
                            change = parsed;
                        }
                    }
                }
            }
            if (change == null) {
                change = context.getSigList().get(wholeToTicks(start, context));
            }
            boolean timeChange = change != null && (frames.isEmpty() || !change.isSameAs(time));
            if (change != null) {
                time = change;
            }
            if (frames.isEmpty()) {
                timeChange = true;
            }

            Fraction length = time.getLength();
            String len = measure.getAttribute("len");
            if (!len.isEmpty()) {
                Fraction parsed = Fraction.parse(len);
                if (parsed != null && parsed.isPositive()) {
                    length = parsed;
                }
            }
            frames.add(new MeasureFrame(start, length, time, timeChange));
            if (frames.size() > MAX_MEASURES) {
                throw new IOException("MuseScore score exceeds maximum measure count (" + MAX_MEASURES + ").");
            }
            start = start.add(length);
        }
        return frames;
    }


    /**
     * Gets the elements of a measure that contain the music: the children of all voice elements
     * (version 3 and later) or the direct children (version 1 and 2).
     *
     * @param measure the Measure element
     * @return the elements
     */
    private List<Element> measureElements(Element measure) {
        List<Element> voices = children(measure, "voice");
        if (voices.isEmpty()) {
            return children(measure, null);
        }
        List<Element> result = new ArrayList<>();
        for (Element voice : voices) {
            result.addAll(children(voice, null));
        }
        return result;
    }


    /**
     * Gets the explicit tick of an element of a version 1 or 2 file.
     *
     * @param element the element
     * @param context the read context
     * @return the tick, or null if the element has none
     */
    private Integer positionOf(Element element, ReadContext context) {
        Element tick = child(element, TICK);
        if (tick == null || context.isVoiceBased()) {
            return null;
        }
        return intValue(tick, 0);
    }


    /**
     * Reads the content of one staff.
     *
     * @param staff the staff content element
     * @param part the part information
     * @param staffIndex the index of the staff within the part
     * @param context the read context
     * @return the staff data
     */
    private StaffData readStaff(Element staff, PartInfo part, int staffIndex, ReadContext context) {
        StaffData data = new StaffData(staffIndex, part.isPercussion(staffIndex));
        data.setTransposition(part.getTransposeChromatic(), part.getTransposeDiatonic());
        List<Element> measures = children(staff, "Measure");
        Fraction stretch = WHOLE;
        for (int m = 0; m < measures.size() && m < context.getFrames().size(); m++) {
            Element measure = measures.get(m);
            MeasureData measureData = new MeasureData();
            data.getMeasures().add(measureData);
            boolean timeSignature = false;
            for (Element element : measureElements(measure)) {
                if ("TimeSig".equals(element.getTagName()) && readTimeSignature(element) != null) {
                    // a local time signature has a stretch (nominal to actual length), a global one has none
                    stretch = readStretch(element);
                    timeSignature = true;
                }
            }
            MeasureFrame frame = context.getFrames().get(m);
            if (frame.isTimeChange() && !timeSignature) {
                // a staff without its own time signature follows the global change
                stretch = WHOLE;
            }
            measureData.setStretch(stretch);

            if (child(measure, "startRepeat") != null || "1".equals(measure.getAttribute("startRepeat"))) {
                measureData.setStartRepeat(true);
            }
            Element endRepeat = child(measure, "endRepeat");
            if (endRepeat != null) {
                measureData.setEndRepeat(Math.max(2, intValue(endRepeat, 2)));
            } else if (!measure.getAttribute("endRepeat").isEmpty()) {
                measureData.setEndRepeat(Math.max(2, intValue(measure.getAttribute("endRepeat"), 2)));
            }
            for (Element element : children(measure, null)) {
                readMeasureLevel(element, measureData, frame, staffIndex, context);
            }

            List<Element> voices = children(measure, "voice");
            if (voices.isEmpty()) {
                readVoice(children(measure, null), 0, false, data, measureData, frame, m, context);
            } else {
                for (int v = 0; v < voices.size() && v < VOICES_PER_STAFF; v++) {
                    readVoice(children(voices.get(v), null), v, true, data, measureData, frame, m, context);
                }
            }

            applyLegacyLists(part, staffIndex, measureData, frame, context);
            if (measureData.isMeasureRepeat() && m > 0) {
                copyPreviousMeasure(data, m, context);
            }
        }
        return data;
    }


    /**
     * Applies the key, clef and tempo lists of version 1 files to a measure.
     *
     * @param part the part information
     * @param staffIndex the index of the staff within the part
     * @param measureData the measure data
     * @param frame the measure frame
     * @param context the read context
     */
    private void applyLegacyLists(PartInfo part, int staffIndex, MeasureData measureData, MeasureFrame frame, ReadContext context) {
        int startTick = wholeToTicks(frame.getStart(), context);
        int endTick = wholeToTicks(frame.getStart().add(frame.getLength()), context);
        if (staffIndex < part.getKeyLists().size()) {
            Integer key = part.getKeyLists().get(staffIndex).get(startTick);
            if (key != null && measureData.getKey() == null) {
                measureData.setKey(new KeyData(key, null, false));
            }
        }
        if (staffIndex < part.getClefLists().size()) {
            for (Map.Entry<Integer, String> entry : part.getClefLists().get(staffIndex).subMap(startTick, true, endTick, false).entrySet()) {
                if (entry.getKey() > 0) {
                    measureData.getClefs().put(ticksToWhole(entry.getKey(), context), entry.getValue());
                }
            }
        }
        if (staffIndex == 0) {
            for (Map.Entry<Integer, Double> entry : context.getTempoList().subMap(startTick, true, endTick, false).entrySet()) {
                DirectionImpl direction = createTempoDirection(entry.getValue(), null, false);
                if (direction != null) {
                    measureData.addGlobalDirection(ticksToWhole(entry.getKey(), context), direction);
                }
            }
        }
    }


    /**
     * Reads the elements of a measure that do not belong to a voice: jumps and markers.
     *
     * @param element the element
     * @param measureData the measure data
     * @param frame the measure frame
     * @param staffIndex the index of the staff within the part
     * @param context the read context
     */
    private void readMeasureLevel(Element element, MeasureData measureData, MeasureFrame frame, int staffIndex, ReadContext context) {
        String tag = element.getTagName();
        if ("Jump".equals(tag)) {
            String text = cleanText(child(element, TEXT));
            if (text != null) {
                SoundImpl sound = new SoundImpl();
                String jumpTo = text(child(element, "jumpTo"));
                if (START.equals(jumpTo)) {
                    sound.setDacapo(YES);
                } else if (!jumpTo.isEmpty()) {
                    sound.setDalsegno(jumpTo);
                }
                DirectionImpl direction = createDirection(ABOVE, wordsType(text, null));
                direction.setSound(sound);
                measureData.addDirection(frame.getStart().add(frame.getLength()), direction, staffIndex);
            }
        } else if ("Marker".equals(tag)) {
            DirectionTypeImpl type = markerType(element);
            if (type != null) {
                measureData.addDirection(frame.getStart(), createDirection(ABOVE, type), staffIndex);
            }
        }
    }


    /**
     * Creates the direction type of a marker (segno, coda, fine, to coda).
     *
     * @param marker the Marker element
     * @return the direction type, or null for an unknown marker
     */
    private DirectionTypeImpl markerType(Element marker) {
        String label = text(child(marker, "label")).toLowerCase();
        if (label.isEmpty()) {
            label = text(child(marker, SUBTYPE)).toLowerCase();
        }
        DirectionTypeImpl type = new DirectionTypeImpl();
        if (label.startsWith("segno")) {
            type.setSegno(EMPTY);
            return type;
        }
        if (label.startsWith("coda") || label.startsWith("varcoda") || label.startsWith("codetta")) {
            type.setCoda(EMPTY);
            return type;
        }
        String text = cleanText(child(marker, TEXT));
        if (text == null && "fine".equals(label)) {
            text = "Fine";
        }
        if (text == null && label.startsWith("tocoda")) {
            text = "To Coda";
        }
        if (text == null) {
            return null;
        }
        type.setWords(Collections.singletonList(words(text, null)));
        return type;
    }


    /**
     * Reads the elements of one voice of a measure (or all elements of a version 1 or 2 measure).
     *
     * @param elements the elements in order
     * @param initialVoice the voice index (0 to 3)
     * @param voiceBased true for the voice elements of version 3 and later
     * @param staff the staff data
     * @param measureData the measure data
     * @param frame the measure frame
     * @param measureIndex the index of the measure
     * @param context the read context
     */
    private void readVoice(List<Element> elements, int initialVoice, boolean voiceBased, StaffData staff, MeasureData measureData,
                           MeasureFrame frame, int measureIndex, ReadContext context) {
        VoiceState state = new VoiceState(initialVoice, frame.getStart());
        state.setStretch(measureData.getStretch());
        for (Element element : elements) {
            String tag = element.getTagName();
            if (!voiceBased) {
                Fraction tick = tickOfPosition(element, context);
                if (tick != null) {
                    state.setPosition(tick);
                }
                Element track = child(element, "track");
                if (track != null) {
                    state.setVoice(Math.floorMod(intValue(track, 0), VOICES_PER_STAFF));
                }
            }

            switch (tag) {
                case TICK:
                    state.setPosition(ticksToWhole(intValue(element, 0), context));
                    break;
                case "location":
                    state.setPosition(state.getPosition().add(readFraction(child(element, "fractions")).multiply(state.getStretch())));
                    break;
                case CHORD:
                case REST:
                    readChordOrRest(element, state, staff, measureData, frame, measureIndex, context);
                    break;
                case TUPLET:
                    readTupletDefinition(element, state);
                    break;
                case "endTuplet":
                    state.popTuplet();
                    break;
                case "Clef":
                    readClef(element, state, staff, frame, measureData);
                    break;
                case "KeySig":
                    if (state.getPosition().equals(frame.getStart())) {
                        measureData.setKey(readKey(element, staff, context));
                    }
                    break;
                case SPANNER:
                    readSpanner(element, state.getPosition(), state, staff, measureIndex, context);
                    break;
                case "Fermata":
                    state.setPendingFermata(text(child(element, SUBTYPE)));
                    break;
                case "Breath":
                    staff.markBreath(state.getVoice(), state.getPosition());
                    break;
                case "BarLine":
                    readBarLine(element, measureData, state, frame);
                    break;
                case "MeasureRepeat":
                case "RepeatMeasure":
                    measureData.setMeasureRepeat(true);
                    break;
                case "Harmony":
                    readHarmony(element, measureData, staff.getStaffIndex());
                    break;
                case "endSpanner":
                    staff.endLegacySpanner(element.getAttribute(ATTR_ID), state.getPosition(), state.getVoice());
                    break;
                case "Slur":
                case "HairPin":
                case "Ottava":
                case "Pedal":
                case "Volta":
                case "Trill":
                    readLegacySpanner(element, state, staff);
                    break;
                default:
                    readDirection(element, state, staff, measureData);
                    break;
            }
        }
    }


    /**
     * Gets the explicit tick of an element of a version 1 or 2 file as position.
     *
     * @param element the element
     * @param context the read context
     * @return the absolute position, or null if the element has no tick
     */
    private Fraction tickOfPosition(Element element, ReadContext context) {
        Integer tick = positionOf(element, context);
        if (tick == null) {
            return null;
        }
        return ticksToWhole(tick, context);
    }


    /**
     * Converts ticks into a position in whole notes.
     *
     * @param ticks the ticks
     * @param context the read context
     * @return the position
     */
    private Fraction ticksToWhole(int ticks, ReadContext context) {
        return Fraction.of(ticks, (long) context.getDivision() * 4);
    }


    /**
     * Reads a chord or rest.
     *
     * @param element the Chord or Rest element
     * @param state the voice state
     * @param staff the staff data
     * @param measureData the measure data
     * @param frame the measure frame
     * @param measureIndex the index of the measure
     * @param context the read context
     */
    private void readChordOrRest(Element element, VoiceState state, StaffData staff, MeasureData measureData, MeasureFrame frame,
                                 int measureIndex, ReadContext context) {
        Fraction explicit = tickOfPosition(element, context);
        if (explicit != null) {
            state.setPosition(explicit);
        }
        Element track = child(element, "track");
        if (track != null) {
            state.setVoice(Math.floorMod(intValue(track, 0), VOICES_PER_STAFF));
        }

        String durationType = text(child(element, "durationType"));
        int dots = intValue(child(element, "dots"), 0);
        boolean measureRest = MEASURE.equals(durationType);
        Fraction base;
        if (measureRest) {
            base = frame.getLength();
        } else {
            base = MuseScoreDurationConverter.getInstance().toFraction(durationType, dots);
            if (base == null) {
                // version 1 files store the duration in ticks only
                int ticks = intValue(child(element, "ticks"), 0);
                if (ticks <= 0) {
                    ticks = intValue(child(element, "ticklen"), 0);
                }
                if (ticks <= 0) {
                    return;
                }
                base = ticksToWhole(ticks, context);
            }
        }

        TupletState tuplet = state.currentTuplet();
        Element tupletRef = child(element, TUPLET);
        if (tupletRef != null && child(tupletRef, "normalNotes") == null) {
            tuplet = state.getTuplet(text(tupletRef));
        }
        Fraction duration = base;
        if (tuplet != null && !measureRest) {
            duration = base.multiply(tuplet.getRatio());
        }
        if (!measureRest) {
            duration = duration.multiply(state.getStretch());
        }

        String grace = graceType(element);
        Event event = new Event(state.getPosition(), duration, durationType, dots, REST.equals(element.getTagName()), grace);
        event.setMeasureRest(measureRest);
        if (!measureRest && !WHOLE.equals(state.getStretch())) {
            event.setStretch(state.getStretch());
        }
        event.setVoice(state.getVoice());
        if (tuplet != null && grace == null && !measureRest) {
            event.setTuplet(tuplet);
            tuplet.addEvent(event);
        }
        if (state.getPendingFermata() != null && grace == null) {
            event.setFermata(state.getPendingFermata());
            state.setPendingFermata(null);
        }
        Element move = child(element, "staffMove");
        if (move == null) {
            // version 2 files
            move = child(element, "move");
        }
        event.setStaffMove(intValue(move, 0));
        event.setInvisible("0".equals(text(child(element, "visible"))));

        for (Element child : children(element, null)) {
            readChordChild(child, event, state, staff, measureIndex, context);
        }
        if ("1".equals(text(child(element, "small")))) {
            // a small chord: all its notes are small
            for (NoteData note : event.getNotes()) {
                note.setSmall(true);
            }
        }

        staff.addEvent(state.getVoice(), event);
        if (grace == null) {
            state.setPosition(state.getPosition().add(duration));
        }
    }


    /**
     * Reads a child element of a chord: notes, articulations, lyrics, slurs and spanners.
     *
     * @param child the child element
     * @param event the chord event
     * @param state the voice state
     * @param staff the staff data
     * @param measureIndex the index of the measure
     * @param context the read context
     */
    private void readChordChild(Element child, Event event, VoiceState state, StaffData staff, int measureIndex, ReadContext context) {
        switch (child.getTagName()) {
            case NOTE:
                NoteData note = readNote(child);
                if (note != null) {
                    event.getNotes().add(note);
                }
                break;
            case "Articulation":
            case "Ornament":
                String subtype = text(child(child, SUBTYPE));
                String converted = LEGACY_ARTICULATIONS.get(subtype);
                if (converted != null) {
                    subtype = converted;
                }
                if (subtype.startsWith("fermata")) {
                    event.setFermata(subtype);
                } else if (!subtype.isEmpty()) {
                    event.getArticulations().add(subtype);
                }
                break;
            case "Lyrics":
                LyricImpl lyric = readLyric(child);
                if (lyric != null) {
                    event.getLyrics().add(lyric);
                }
                break;
            case "Slur":
                // version 1 and 2: <Slur type="start" id="2"/> or number="2"
                String id = child.getAttribute(ATTR_ID);
                if (id.isEmpty()) {
                    id = child.getAttribute("number");
                }
                if (START.equals(child.getAttribute(TYPE))) {
                    staff.startLegacySlur(id, event);
                } else if (STOP.equals(child.getAttribute(TYPE))) {
                    staff.stopLegacySlur(id, event);
                }
                break;
            case SPANNER:
                readSpanner(child, event.getStart(), state, staff, measureIndex, context);
                break;
            default:
                break;
        }
    }


    /**
     * Reads a note.
     *
     * @param note the Note element
     * @return the note data, or null if the pitch is invalid
     */
    private NoteData readNote(Element note) {
        // version 1.1x files store pitch and spelling as attributes
        int pitch = intValue(child(note, "pitch"), intValue(note.getAttribute("pitch"), -1));
        if (pitch < 0 || pitch > MIDI_MAX) {
            return null;
        }
        int tpc = intValue(child(note, "tpc"), intValue(note.getAttribute("tpc"), Integer.MIN_VALUE));
        NoteData data = new NoteData(pitch, tpc, intValue(child(note, "tpc2"), Integer.MIN_VALUE));
        for (Element child : children(note, null)) {
            String tag = child.getTagName();
            if ("Tie".equals(tag)) {
                data.setTieStart(true);
            } else if (SPANNER.equals(tag) && "Tie".equals(child.getAttribute(TYPE)) && child(child, "next") != null) {
                data.setTieStart(true);
            } else if ("Accidental".equals(tag)) {
                String accidental = MuseScoreSymbolConverter.getInstance().accidentalToXml(text(child(child, SUBTYPE)));
                if (accidental != null) {
                    data.setAccidental(accidental);
                }
            } else if ("head".equals(tag)) {
                data.setNotehead(MuseScoreSymbolConverter.getInstance().noteheadToXml(text(child)));
            } else if ("Fingering".equals(tag)) {
                String finger = cleanText(child(child, TEXT));
                if (finger != null) {
                    data.addFingering(finger);
                }
            } else if ("small".equals(tag)) {
                data.setSmall("1".equals(text(child)));
            }
        }
        return data;
    }


    /**
     * Reads a lyric.
     *
     * @param lyrics the Lyrics element
     * @return the lyric, or null if it has no text
     */
    private LyricImpl readLyric(Element lyrics) {
        String text = cleanText(child(lyrics, TEXT));
        if (text == null) {
            return null;
        }
        LyricImpl lyric = new LyricImpl();
        lyric.setNumber(String.valueOf(intValue(child(lyrics, "no"), 0) + 1));
        String syllabic = text(child(lyrics, "syllabic"));
        if (syllabic.isEmpty()) {
            syllabic = "single";
        }
        lyric.setSyllabic(syllabic);
        lyric.setText(text);
        return lyric;
    }


    /**
     * Gets the grace note type of a chord.
     *
     * @param chord the Chord element
     * @return the grace type (for example acciaccatura), or null for a normal chord
     */
    private String graceType(Element chord) {
        for (String type : GRACE_TYPES) {
            if (child(chord, type) != null) {
                return type;
            }
        }
        String noteType = text(child(chord, "noteType"));
        if (!noteType.isEmpty()) {
            return noteType;
        }
        return null;
    }


    /**
     * Reads a tuplet definition: version 4 tuplets apply to the following elements up to
     * endTuplet, older versions are referenced by ID.
     *
     * @param element the Tuplet element
     * @param state the voice state
     */
    private void readTupletDefinition(Element element, VoiceState state) {
        int normal = intValue(child(element, "normalNotes"), 0);
        int actual = intValue(child(element, "actualNotes"), 0);
        if (normal <= 0 || actual <= 0) {
            return;
        }
        TupletState parent = state.currentTuplet();
        Element parentRef = child(element, TUPLET);
        if (parentRef != null) {
            parent = state.getTuplet(text(parentRef));
        }
        Fraction ratio = Fraction.of(normal, actual);
        if (parent != null) {
            ratio = ratio.multiply(parent.getRatio());
        }
        TupletState tuplet = new TupletState(actual, normal, text(child(element, "baseNote")), ratio);
        String id = element.getAttribute(ATTR_ID);
        if (id.isEmpty()) {
            state.pushTuplet(tuplet);
        } else {
            state.defineTuplet(id, tuplet);
        }
    }


    /**
     * Reads a clef change.
     *
     * @param element the Clef element
     * @param state the voice state
     * @param staff the staff data
     * @param frame the measure frame
     * @param measureData the measure data
     */
    private void readClef(Element element, VoiceState state, StaffData staff, MeasureFrame frame, MeasureData measureData) {
        String clefType = text(child(element, "transposingClefType"));
        if (clefType.isEmpty()) {
            clefType = text(child(element, "concertClefType"));
        }
        if (clefType.isEmpty()) {
            String subtype = text(child(element, SUBTYPE));
            int index = intValue(subtype, -1);
            if (index >= 0 && index < LEGACY_CLEFS.length) {
                clefType = LEGACY_CLEFS[index];
            } else if (!subtype.isEmpty()) {
                clefType = subtype;
            } else {
                clefType = CLEF_TREBLE;
            }
        }
        measureData.getClefs().put(state.getPosition(), clefType);
    }


    /**
     * Reads a key signature as written fifths.
     *
     * @param element the KeySig element
     * @param staff the staff data
     * @param context the read context
     * @return the key signature
     */
    private KeyData readKey(Element element, StaffData staff, ReadContext context) {
        Element actual = child(element, "actualKey");
        Element concert = child(element, "concertKey");
        Element accidental = child(element, "accidental");
        int fifths = 0;
        if (actual != null) {
            fifths = intValue(actual, 0);
        } else if (accidental != null) {
            fifths = intValue(accidental, 0);
        } else if (concert != null) {
            fifths = intValue(concert, 0);
        }
        String mode = text(child(element, "mode"));
        if (mode.isEmpty()) {
            mode = null;
        }
        return new KeyData(fifths, mode, concert != null && actual == null);
    }


    /**
     * Reads a barline inside a voice: a barline at the end of the measure becomes the right barline.
     *
     * @param element the BarLine element
     * @param measureData the measure data
     * @param state the voice state
     * @param frame the measure frame
     */
    private void readBarLine(Element element, MeasureData measureData, VoiceState state, MeasureFrame frame) {
        String style = MuseScoreSymbolConverter.getInstance().barlineToXml(text(child(element, SUBTYPE)));
        if (style != null && state.getPosition().compareTo(frame.getStart()) > 0) {
            measureData.setBarStyle(style);
        }
    }


    /**
     * Reads a chord symbol.
     *
     * @param element the Harmony element
     * @param measureData the measure data
     * @param staffIndex the index of the staff within the part
     */
    private void readHarmony(Element element, MeasureData measureData, int staffIndex) {
        int rootTpc = intValue(child(element, "root"), Integer.MIN_VALUE);
        MuseScorePitchConverter pitch = MuseScorePitchConverter.getInstance();
        if (!pitch.isValidTpc(rootTpc)) {
            return;
        }
        HarmonyImpl harmony = new HarmonyImpl();
        RootImpl root = new RootImpl();
        root.setRootStep(pitch.tpcToStep(rootTpc));
        if (pitch.tpcToAlter(rootTpc) != 0) {
            root.setRootAlter((double) pitch.tpcToAlter(rootTpc));
        }
        harmony.setRoot(root);
        String kind = MuseScoreSymbolConverter.getInstance().harmonyToKind(text(child(element, "name")));
        if (kind == null) {
            kind = "major";
        }
        harmony.setKind(kind);
        int bassTpc = intValue(child(element, "base"), Integer.MIN_VALUE);
        if (pitch.isValidTpc(bassTpc)) {
            BassImpl bass = new BassImpl();
            bass.setBassStep(pitch.tpcToStep(bassTpc));
            if (pitch.tpcToAlter(bassTpc) != 0) {
                bass.setBassAlter((double) pitch.tpcToAlter(bassTpc));
            }
            harmony.setBass(bass);
        }
        if (staffIndex > 0) {
            harmony.setStaff(staffIndex + 1);
        }
        measureData.getHarmonies().add(harmony);
    }


    /**
     * Reads a spanner of version 3 or later (slur, hairpin, ottava, pedal, volta, trill). Only the
     * start of a spanner (with a next location) is read; the end follows from the location.
     *
     * @param element the Spanner element
     * @param start the position of the spanner start
     * @param state the voice state
     * @param staff the staff data
     * @param measureIndex the index of the measure of the start
     * @param context the read context
     */
    private void readSpanner(Element element, Fraction start, VoiceState state, StaffData staff, int measureIndex, ReadContext context) {
        Element next = child(element, "next");
        if (next == null) {
            return;
        }
        Element location = child(next, "location");
        int measures = intValue(child(location, "measures"), 0);
        Fraction fractions = readFraction(child(location, "fractions"));
        int endMeasure = Math.min(measureIndex + measures, context.getFrames().size());
        Fraction end = context.measureStart(endMeasure).add(start.subtract(context.measureStart(measureIndex))).add(fractions);

        String type = element.getAttribute(TYPE);
        Element definition = child(element, type);
        staff.getSpanners().add(new SpannerData(type, definition, start, end, state.getVoice()));
    }


    /**
     * Reads a spanner definition of version 1 or 2 (ended by endSpanner with the same ID).
     *
     * @param element the spanner element (Slur, HairPin, Ottava, Pedal, Volta, Trill)
     * @param state the voice state
     * @param staff the staff data
     */
    private void readLegacySpanner(Element element, VoiceState state, StaffData staff) {
        String id = element.getAttribute(ATTR_ID);
        if ("Slur".equals(element.getTagName())) {
            // slurs of version 1 and 2 are attached to their chords
            return;
        }
        if (id.isEmpty()) {
            return;
        }
        staff.startLegacySpanner(id, new SpannerData(element.getTagName(), element, state.getPosition(), null, state.getVoice()));
    }


    /**
     * Reads a direction element of a voice: dynamics, tempo and text.
     *
     * @param element the element
     * @param state the voice state
     * @param staff the staff data
     * @param measureData the measure data
     */
    private void readDirection(Element element, VoiceState state, StaffData staff, MeasureData measureData) {
        String tag = element.getTagName();
        DirectionImpl direction = null;
        if ("Dynamic".equals(tag)) {
            String marking = text(child(element, SUBTYPE));
            if (marking.isEmpty() || "other-dynamics".equals(marking)) {
                marking = cleanText(child(element, TEXT));
            }
            if (marking != null && !marking.isEmpty()) {
                DirectionTypeImpl type = new DirectionTypeImpl();
                type.setDynamics(DynamicsConverter.getInstance().createDynamics(marking));
                direction = createDirection(BELOW, type);
            }
        } else if ("Tempo".equals(tag)) {
            direction = readTempo(element);
        } else if ("StaffText".equals(tag) || "SystemText".equals(tag) || "Expression".equals(tag) || "Text".equals(tag)
                || "InstrumentChange".equals(tag) || "PlayTechAnnotation".equals(tag)) {
            String text = cleanText(child(element, TEXT));
            if (text != null) {
                String style = null;
                if ("Expression".equals(tag)) {
                    style = "italic";
                }
                direction = createDirection(ABOVE, wordsType(text, style));
            }
        } else if ("RehearsalMark".equals(tag)) {
            String text = cleanText(child(element, TEXT));
            if (text != null) {
                DirectionTypeImpl type = new DirectionTypeImpl();
                type.setRehearsal(text);
                direction = createDirection(ABOVE, type);
            }
        }
        if (direction != null) {
            measureData.addDirection(state.getPosition(), direction, staff.getStaffIndex());
        }
    }


    /**
     * Reads a tempo mark: words, metronome and sound tempo. MuseScore stores the tempo in quarter
     * notes per second.
     *
     * @param element the Tempo element
     * @return the direction
     */
    private DirectionImpl readTempo(Element element) {
        Element text = child(element, TEXT);
        boolean bold = text != null && text.getElementsByTagName("b").getLength() > 0;
        return createTempoDirection(doubleValue(child(element, "tempo")), cleanText(text), bold);
    }


    /**
     * Creates a tempo direction: words, metronome and sound tempo. The metronome mark is shown only if the text
     * shows it (a note symbol), or if there is no text; a text without it (for example a tempo) only sets the tempo.
     *
     * @param quartersPerSecond the tempo in quarter notes per second
     * @param text the tempo text (may be null)
     * @param bold true if the text is bold
     * @return the direction, or null if there is neither a tempo nor a text
     */
    private DirectionImpl createTempoDirection(double quartersPerSecond, String text, boolean bold) {
        List<DirectionTypeImpl> types = new ArrayList<>();
        long bpm = Math.round(quartersPerSecond * SECONDS_PER_MINUTE);
        boolean metronomeShown = text == null;
        if (text != null) {
            String words = text.replaceAll(METRONOME_TEXT, " ").trim();
            metronomeShown = !words.equals(text.trim());
            if (!words.isEmpty()) {
                DirectionTypeImpl wordsType = wordsType(words, null);
                if (bold) {
                    wordsType.getWords().get(0).setFontWeight("bold");
                }
                types.add(wordsType);
            }
        }
        if (bpm > 0 && metronomeShown) {
            MetronomeImpl metronome = new MetronomeImpl();
            metronome.setBeatUnit("quarter");
            metronome.setPerMinute(String.valueOf(bpm));
            DirectionTypeImpl type = new DirectionTypeImpl();
            type.setMetronome(metronome);
            types.add(type);
        }
        if (types.isEmpty()) {
            return null;
        }
        DirectionImpl direction = createDirection(ABOVE, types.toArray(new DirectionTypeImpl[0]));
        if (bpm > 0) {
            SoundImpl sound = new SoundImpl();
            sound.setTempo(String.valueOf(bpm));
            direction.setSound(sound);
        }
        return direction;
    }


    /**
     * Copies the voices of the previous measure (measure repeat sign).
     *
     * @param staff the staff data
     * @param measureIndex the index of the measure with the repeat sign
     * @param context the read context
     */
    private void copyPreviousMeasure(StaffData staff, int measureIndex, ReadContext context) {
        Fraction previousStart = context.measureStart(measureIndex - 1);
        Fraction start = context.measureStart(measureIndex);
        Fraction shift = start.subtract(previousStart);
        for (int voice = 0; voice < VOICES_PER_STAFF; voice++) {
            List<Event> copies = new ArrayList<>();
            for (Event event : staff.getEvents(voice)) {
                if (event.getStart().compareTo(previousStart) >= 0 && event.getStart().compareTo(start) < 0) {
                    copies.add(event.copyShifted(shift));
                }
            }
            // the repeat sign itself is stored as a measure rest; replace it by the copied music
            staff.getEvents(voice).removeIf(e -> e.getStart().compareTo(start) >= 0 && e.getStart().compareTo(start.add(shift)) < 0);
            for (Event copy : copies) {
                staff.addEvent(voice, copy);
            }
        }
    }


    /**
     * Builds the identification, work and movement title.
     *
     * @param score the score
     * @param metaTags the meta tags
     * @param lyricistTag true if the score has a lyricist meta tag (then a text of the title frame is no lyricist)
     * @param frameTexts the texts of the title frame by style
     * @param context the read context
     */
    private void buildIdentification(ScoreImpl score, Map<String, String> metaTags, boolean lyricistTag, Map<String, String> frameTexts, ReadContext context) {
        String title = metaTags.get("workTitle");
        if (title == null) {
            title = frameTexts.get("title");
        }
        if (title != null) {
            WorkImpl work = new WorkImpl();
            work.setWorkTitle(title);
            String workNumber = metaTags.get("workNumber");
            if (workNumber != null) {
                work.setWorkNumber(workNumber);
            }
            score.setWork(work);
        }
        String movementTitle = metaTags.get("movementTitle");
        if (movementTitle == null) {
            movementTitle = title;
        }
        if (movementTitle != null) {
            score.setMovementTitle(movementTitle);
        }
        String movementNumber = metaTags.get("movementNumber");
        if (movementNumber != null) {
            score.setMovementNumber(movementNumber);
        }

        List<CreatorImpl> creators = new ArrayList<>();
        addCreator(creators, "composer", firstNonNull(metaTags.get("composer"), frameTexts.get("composer")));
        if (lyricistTag) {
            addCreator(creators, "lyricist", metaTags.get("lyricist"));
        } else {
            addCreator(creators, "lyricist", firstNonNull(frameTexts.get("lyricist"), frameTexts.get("poet")));
        }
        addCreator(creators, "arranger", metaTags.get("arranger"));
        addCreator(creators, "translator", metaTags.get("translator"));
        IdentificationImpl identification = new IdentificationImpl();
        if (!creators.isEmpty()) {
            identification.setCreators(creators);
        }
        if (metaTags.get("copyright") != null) {
            identification.setRights(metaTags.get("copyright"));
        }
        EncodingImpl encoding = new EncodingImpl();
        List<String> software = new ArrayList<>();
        software.add(EncodingConverter.getInstance().getSoftware());
        software.add("MuseScore file format " + context.getVersion());
        encoding.setSoftware(software);
        identification.setEncoding(encoding);
        score.setIdentification(identification);
    }


    /**
     * Adds a creator if the value is set.
     *
     * @param creators the creators
     * @param type the creator type
     * @param value the value (may be null)
     */
    private void addCreator(List<CreatorImpl> creators, String type, String value) {
        if (value == null) {
            return;
        }
        CreatorImpl creator = new CreatorImpl();
        creator.setType(type);
        creator.setValue(value);
        creators.add(creator);
    }


    /**
     * Builds the score part of a part.
     *
     * @param partId the part ID
     * @param part the part information
     * @return the score part
     */
    private ScorePartImpl buildScorePart(String partId, PartInfo part) {
        ScorePartImpl scorePart = new ScorePartImpl();
        scorePart.setId(partId);
        String name = part.getName();
        if (name == null) {
            name = partId;
        }
        scorePart.setPartName(name);
        if (part.getAbbreviation() != null) {
            scorePart.setPartAbbreviation(part.getAbbreviation());
        }
        boolean program = part.getProgram() >= 0 && part.getProgram() <= MIDI_MAX;
        if (program || part.getSound() != null) {
            ScoreInstrumentImpl instrument = new ScoreInstrumentImpl();
            instrument.setId(partId + "-I1");
            instrument.setInstrumentName(name);
            instrument.setInstrumentSound(part.getSound());
            scorePart.setScoreInstruments(Collections.singletonList(instrument));
        }
        if (program) {
            MidiInstrumentImpl midi = new MidiInstrumentImpl();
            midi.setId(partId + "-I1");
            midi.setMidiProgram(part.getProgram() + 1);
            scorePart.setMidiInstruments(Collections.singletonList(midi));
        }
        return scorePart;
    }


    /**
     * Builds a part with all measures.
     *
     * @param partId the part ID
     * @param part the part information
     * @param staves the staves of the part
     * @param context the read context
     * @param firstPart true for the first part
     * @return the part
     */
    private PartImpl buildPart(String partId, PartInfo part, List<StaffData> staves, ReadContext context, boolean firstPart) {
        int divisions = context.getDivisions(staves);
        boolean multiStaff = staves.size() > 1;
        for (StaffData staff : staves) {
            staff.finish(context);
        }

        PartImpl partImpl = new PartImpl();
        partImpl.setId(partId);
        List<MeasureImpl> measures = new ArrayList<>();
        Map<Integer, Integer> currentKeys = new HashMap<>();
        Map<Integer, String> currentClefs = new HashMap<>();
        PartDirections partDirections = buildSpannerDirections(staves);
        List<VoltaData> voltas = collectVoltas(staves);

        for (int m = 0; m < context.getFrames().size(); m++) {
            MeasureFrame frame = context.getFrames().get(m);
            MeasureImpl measure = new MeasureImpl();
            measure.setNumber(String.valueOf(m + 1));
            if (m == 0 && frame.getLength().compareTo(frame.getTime().getLength()) < 0) {
                measure.setImplicit(YES);
            }

            final List<AttributesImpl> attributes = buildAttributes(part, staves, m, frame, divisions, currentKeys, currentClefs, multiStaff);
            List<IMusicDataElement> notes = new ArrayList<>();
            int measureDuration = toDivisions(frame.getLength(), divisions);
            for (StaffData staff : staves) {
                for (int voice = 0; voice < VOICES_PER_STAFF; voice++) {
                    Fraction stretch = WHOLE;
                    if (staff.measure(m) != null) {
                        stretch = staff.measure(m).getStretch();
                    }
                    List<NoteImpl> voiceNotes = buildVoice(staff, voice, frame, stretch, divisions, multiStaff);
                    if (voiceNotes.isEmpty()) {
                        continue;
                    }
                    if (!notes.isEmpty()) {
                        BackupImpl backup = new BackupImpl();
                        backup.setDuration(measureDuration);
                        notes.add(backup);
                    }
                    notes.addAll(voiceNotes);
                }
            }

            List<DirectionImpl> directions = new ArrayList<>();
            List<HarmonyImpl> harmonies = new ArrayList<>();
            for (StaffData staff : staves) {
                MeasureData data = staff.measure(m);
                if (data == null) {
                    continue;
                }
                harmonies.addAll(data.getHarmonies());
                for (PositionedDirection positioned : data.getDirections()) {
                    if (positioned.isGlobal() && (!firstPart || staff.getStaffIndex() > 0)) {
                        continue;
                    }
                    directions.add(positionDirection(positioned, frame, divisions, multiStaff, staff.getStaffIndex()));
                }
            }
            for (PositionedDirection positioned : partDirections.inMeasure(frame, m == context.getFrames().size() - 1)) {
                directions.add(positionDirection(positioned, frame, divisions, multiStaff, positioned.getStaffIndex()));
            }
            List<BarlineImpl> barlines = buildBarlines(staves.get(0).measure(m), voltas, frame, m == context.getFrames().size() - 1);

            // the content in document order: the directions carry their offset from the start of the measure
            List<IMusicDataElement> content = new ArrayList<>();
            for (BarlineImpl barline : barlines) {
                if (LEFT.equals(barline.getLocation())) {
                    content.add(barline);
                }
            }
            content.addAll(attributes);
            content.addAll(directions);
            content.addAll(harmonies);
            content.addAll(notes);
            for (BarlineImpl barline : barlines) {
                if (!LEFT.equals(barline.getLocation())) {
                    content.add(barline);
                }
            }
            measure.setMusicData(content);
            measures.add(measure);
        }
        partImpl.setMeasures(measures);
        return partImpl;
    }


    /**
     * Builds the attributes of a measure: complete attributes in the first measure, changes later.
     *
     * @param part the part information
     * @param staves the staves of the part
     * @param measureIndex the index of the measure
     * @param frame the measure frame
     * @param divisions the divisions per quarter note
     * @param currentKeys the current key of every staff (updated)
     * @param currentClefs the current clef of every staff (updated)
     * @param multiStaff true if the part has more than one staff
     * @return the attributes (may be empty)
     */
    private List<AttributesImpl> buildAttributes(PartInfo part, List<StaffData> staves, int measureIndex, MeasureFrame frame, int divisions,
                                                 Map<Integer, Integer> currentKeys, Map<Integer, String> currentClefs, boolean multiStaff) {
        boolean first = measureIndex == 0;
        AttributesImpl attributes = new AttributesImpl();
        boolean used = false;
        if (first) {
            attributes.setDivisions(divisions);
            if (multiStaff) {
                attributes.setStaves(staves.size());
            }
            if (part.getTransposeChromatic() != 0 || part.getTransposeDiatonic() != 0) {
                TransposeImpl transpose = new TransposeImpl();
                int chromatic = part.getTransposeChromatic();
                int diatonic = part.getTransposeDiatonic();
                int octave = 0;
                while (chromatic <= -12) {
                    chromatic += 12;
                    diatonic += 7;
                    octave--;
                }
                while (chromatic >= 12) {
                    chromatic -= 12;
                    diatonic -= 7;
                    octave++;
                }
                transpose.setChromatic(chromatic);
                transpose.setDiatonic(diatonic);
                if (octave != 0) {
                    transpose.setOctaveChange(octave);
                }
                attributes.setTransposes(Collections.singletonList(transpose));
            }
            used = true;
        }

        MeasureData firstStaff = staves.get(0).measure(measureIndex);
        KeyData key = null;
        if (firstStaff != null) {
            key = firstStaff.getKey();
        }
        Integer currentKey = currentKeys.get(0);
        if (first && key == null) {
            key = new KeyData(0, null, false);
        }
        if (key != null && (currentKey == null || currentKey != key.getFifths())) {
            KeyImpl keyImpl = new KeyImpl();
            keyImpl.setFifths(key.getFifths());
            if (key.getMode() != null) {
                keyImpl.setMode(key.getMode());
            }
            attributes.setKeys(Collections.singletonList(keyImpl));
            currentKeys.put(0, key.getFifths());
            used = true;
        }

        if (frame.isTimeChange()) {
            TimeImpl time = new TimeImpl();
            time.setBeats(String.valueOf(frame.getTime().getBeats()));
            time.setBeatType(String.valueOf(frame.getTime().getBeatType()));
            attributes.setTimes(Collections.singletonList(time));
            used = true;
        }

        List<ClefImpl> clefs = new ArrayList<>();
        List<AttributesImpl> result = new ArrayList<>();
        List<AttributesImpl> midMeasure = new ArrayList<>();
        for (StaffData staff : staves) {
            MeasureData data = staff.measure(measureIndex);
            String clefType = null;
            if (data != null) {
                clefType = data.getClefs().get(frame.getStart());
            }
            if (first && clefType == null) {
                clefType = part.getDefaultClef(staff.getStaffIndex());
            }
            if (clefType != null && !clefType.equals(currentClefs.get(staff.getStaffIndex()))) {
                clefs.add(createClef(clefType, staff.getStaffIndex(), multiStaff));
                currentClefs.put(staff.getStaffIndex(), clefType);
            }
            if (data != null) {
                for (Map.Entry<Fraction, String> entry : data.getClefs().entrySet()) {
                    if (entry.getKey().compareTo(frame.getStart()) > 0 && !entry.getValue().equals(currentClefs.get(staff.getStaffIndex()))) {
                        AttributesImpl change = new AttributesImpl();
                        change.setClefs(Collections.singletonList(createClef(entry.getValue(), staff.getStaffIndex(), multiStaff)));
                        midMeasure.add(change);
                        currentClefs.put(staff.getStaffIndex(), entry.getValue());
                    }
                }
            }
        }
        if (!clefs.isEmpty()) {
            attributes.setClefs(clefs);
            used = true;
        }
        if (used) {
            result.add(attributes);
        }
        result.addAll(midMeasure);
        return result;
    }


    /**
     * Creates a MusicXML clef.
     *
     * @param clefType the MuseScore clef type
     * @param staffIndex the index of the staff within the part
     * @param multiStaff true if the part has more than one staff
     * @return the clef
     */
    private ClefImpl createClef(String clefType, int staffIndex, boolean multiStaff) {
        String[] xml = MuseScoreSymbolConverter.getInstance().clefToXml(clefType);
        if (xml == null) {
            xml = MuseScoreSymbolConverter.getInstance().clefToXml(CLEF_TREBLE);
        }
        ClefImpl clef = new ClefImpl();
        clef.setSign(xml[0]);
        if (xml.length > 1 && xml[1] != null) {
            clef.setLine(intValue(xml[1], 2));
        }
        if (xml.length > 2 && xml[2] != null && intValue(xml[2], 0) != 0) {
            clef.setClefOctaveChange(intValue(xml[2], 0));
        }
        if (multiStaff) {
            clef.setNumber(staffIndex + 1);
        }
        return clef;
    }


    /**
     * Builds the notes and rests of one voice in one measure. Gaps are filled with invisible rests.
     *
     * @param staff the staff data
     * @param voice the voice index
     * @param frame the measure frame
     * @param stretch the stretch of a local time signature (1 without)
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @return the notes (may be empty)
     */
    private List<NoteImpl> buildVoice(StaffData staff, int voice, MeasureFrame frame, Fraction stretch, int divisions, boolean multiStaff) {
        List<Event> events = staff.eventsIn(voice, frame);
        List<NoteImpl> result = new ArrayList<>();
        if (events.isEmpty()) {
            return result;
        }
        int voiceNumber = staff.getStaffIndex() * VOICES_PER_STAFF + voice + 1;
        Fraction cursor = frame.getStart();
        Fraction end = frame.getStart().add(frame.getLength());
        for (Event event : events) {
            if (event.getGrace() == null && event.getStart().compareTo(cursor) > 0) {
                result.addAll(gapRests(cursor, event.getStart(), voiceNumber, staff, stretch, divisions, multiStaff));
                cursor = event.getStart();
            }
            if (event.getGrace() == null && event.getStart().compareTo(cursor) < 0) {
                // overlapping elements (inconsistent file): skip
                continue;
            }
            result.addAll(buildEvent(event, voiceNumber, staff, divisions, multiStaff));
            if (event.getGrace() == null) {
                cursor = event.getStart().add(event.getDuration());
            }
        }
        if (cursor.compareTo(end) < 0) {
            result.addAll(gapRests(cursor, end, voiceNumber, staff, stretch, divisions, multiStaff));
        }
        return result;
    }


    /**
     * Creates invisible rests for a gap in a voice. In a staff with a local time signature the
     * rests have the nominal values of that time signature and its time modification.
     *
     * @param from the start of the gap
     * @param to the end of the gap
     * @param voiceNumber the MusicXML voice number
     * @param staff the staff data
     * @param stretch the stretch of a local time signature (1 without)
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @return the rests
     */
    private List<NoteImpl> gapRests(Fraction from, Fraction to, int voiceNumber, StaffData staff, Fraction stretch, int divisions, boolean multiStaff) {
        MuseScoreDurationConverter converter = MuseScoreDurationConverter.getInstance();
        List<NoteImpl> result = new ArrayList<>();
        Fraction nominalGap = to.subtract(from).multiply(stretch.getDenominator(), stretch.getNumerator());
        Fraction position = from;
        for (MuseScoreDurationConverter.TypeAndDots piece : converter.split(nominalGap)) {
            Fraction length = converter.toFraction(piece.getType(), piece.getDots()).multiply(stretch);
            NoteImpl rest = gapRest(position, position.add(length), voiceNumber, staff, divisions, multiStaff);
            rest.setType(piece.getType());
            if (piece.getDots() > 0) {
                rest.setDots(dots(piece.getDots()));
            }
            if (!WHOLE.equals(stretch)) {
                TimeModificationImpl modification = new TimeModificationImpl();
                modification.setActualNotes((int) stretch.getDenominator());
                modification.setNormalNotes((int) stretch.getNumerator());
                rest.setTimeModification(modification);
            }
            result.add(rest);
            position = position.add(length);
        }
        if (position.compareTo(to) < 0) {
            // a remainder without a note value: one rest keeps the measure complete
            result.add(gapRest(position, to, voiceNumber, staff, divisions, multiStaff));
        }
        return result;
    }


    /**
     * Creates one invisible rest.
     *
     * @param from the start
     * @param to the end
     * @param voiceNumber the MusicXML voice number
     * @param staff the staff data
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @return the rest
     */
    private NoteImpl gapRest(Fraction from, Fraction to, int voiceNumber, StaffData staff, int divisions, boolean multiStaff) {
        NoteImpl rest = new NoteImpl();
        rest.setRest(new RestImpl());
        rest.setDuration(toDivisions(to, divisions) - toDivisions(from, divisions));
        rest.setPrintObject("no");
        rest.setVoice(voiceNumber);
        if (multiStaff) {
            rest.setStaff(staff.getStaffIndex() + 1);
        }
        return rest;
    }


    /**
     * Builds the notes of one event (a chord, rest or grace chord).
     *
     * @param event the event
     * @param voiceNumber the MusicXML voice number
     * @param staff the staff data
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @return the notes
     */
    private List<NoteImpl> buildEvent(Event event, int voiceNumber, StaffData staff, int divisions, boolean multiStaff) {
        List<NoteImpl> result = new ArrayList<>();
        int duration = toDivisions(event.getStart().add(event.getDuration()), divisions) - toDivisions(event.getStart(), divisions);
        int noteStaff = staff.getStaffIndex() + 1 + event.getStaffMove();

        if (event.isRest() || event.getNotes().isEmpty()) {
            NoteImpl rest = new NoteImpl();
            RestImpl restImpl = new RestImpl();
            if (event.isMeasureRest()) {
                restImpl.setMeasure(YES);
            }
            rest.setRest(restImpl);
            rest.setDuration(duration);
            applyType(rest, event);
            rest.setVoice(voiceNumber);
            if (multiStaff) {
                rest.setStaff(staff.getStaffIndex() + 1);
            }
            if (event.isInvisible()) {
                rest.setPrintObject("no");
            }
            applyNotations(rest, event, null);
            result.add(rest);
            return result;
        }

        for (int n = 0; n < event.getNotes().size(); n++) {
            NoteData data = event.getNotes().get(n);
            NoteImpl note = new NoteImpl();
            if (event.getGrace() != null) {
                GraceImpl grace = new GraceImpl();
                if ("acciaccatura".equals(event.getGrace())) {
                    grace.setSlash(YES);
                }
                note.setGrace(grace);
            } else {
                note.setDuration(duration);
            }
            if (n > 0) {
                note.setChord(EMPTY);
            }
            applyPitch(note, data, staff);
            applyType(note, event);
            if (data.isSmall()) {
                note.setTypeSize("cue");
            }
            note.setVoice(voiceNumber);
            if (multiStaff) {
                note.setStaff(noteStaff);
            }
            if (data.getAccidental() != null) {
                note.setAccidental(data.getAccidental());
            }
            if (data.getNotehead() != null) {
                note.setNotehead(data.getNotehead());
            }
            if (data.isTieStart() || data.isTieStop()) {
                List<TieImpl> ties = new ArrayList<>();
                if (data.isTieStop()) {
                    ties.add(tie(STOP));
                }
                if (data.isTieStart()) {
                    ties.add(tie(START));
                }
                note.setTies(ties);
            }
            if (n == 0) {
                applyNotations(note, event, data);
                if (!event.getLyrics().isEmpty()) {
                    note.setLyrics(event.getLyrics());
                }
            } else {
                applyNotations(note, null, data);
            }
            result.add(note);
        }
        return result;
    }


    /**
     * Sets the written pitch of a note (or the display position of a percussion note).
     *
     * @param note the note
     * @param data the note data
     * @param staff the staff data
     */
    private void applyPitch(NoteImpl note, NoteData data, StaffData staff) {
        MuseScorePitchConverter converter = MuseScorePitchConverter.getInstance();
        int writtenMidi = data.getPitch() + data.getOctaveShift() - staff.getTransposeChromatic();
        int tpc = data.getTpc2();
        if (!converter.isValidTpc(tpc)) {
            tpc = data.getTpc();
            if (converter.isValidTpc(tpc) && (staff.getTransposeChromatic() != 0 || staff.getTransposeDiatonic() != 0)) {
                tpc = converter.transposeTpc(tpc, -staff.getTransposeDiatonic(), -staff.getTransposeChromatic());
            }
        }
        if (!converter.isValidTpc(tpc)) {
            tpc = converter.midiToTpc(writtenMidi);
        }
        String step = converter.tpcToStep(tpc);
        int alter = converter.tpcToAlter(tpc);
        int octave = converter.octaveOf(writtenMidi, step, alter);
        if (staff.isPercussion()) {
            UnpitchedImpl unpitched = new UnpitchedImpl();
            unpitched.setDisplayStep(step);
            unpitched.setDisplayOctave(octave);
            note.setUnpitched(unpitched);
            return;
        }
        PitchImpl pitch = new PitchImpl();
        pitch.setStep(step);
        if (alter != 0) {
            pitch.setAlter((double) alter);
        }
        pitch.setOctave(octave);
        note.setPitch(pitch);
    }


    /**
     * Sets the note type, dots and time modification.
     *
     * @param note the note
     * @param event the event
     */
    private void applyType(NoteImpl note, Event event) {
        String type = event.getType();
        if (MEASURE.equals(type) || type == null || type.isEmpty()) {
            if (!event.isMeasureRest()) {
                MuseScoreDurationConverter.TypeAndDots typeAndDots = MuseScoreDurationConverter.getInstance().fromFraction(event.getDuration());
                if (typeAndDots != null) {
                    note.setType(typeAndDots.getType());
                    if (typeAndDots.getDots() > 0) {
                        note.setDots(dots(typeAndDots.getDots()));
                    }
                }
            }
            return;
        }
        note.setType(MuseScoreDurationConverter.getInstance().normalizeType(type));
        if (event.getDots() > 0) {
            note.setDots(dots(event.getDots()));
        }
        TupletState tuplet = event.getTuplet();
        if (tuplet != null || event.getStretch() != null) {
            TimeModificationImpl modification = new TimeModificationImpl();
            Fraction ratio = WHOLE;
            if (tuplet != null) {
                ratio = tuplet.getRatio();
            }
            if (event.getStretch() != null) {
                // notes of a local time signature are shown at their nominal value
                ratio = ratio.multiply(event.getStretch());
            }
            modification.setActualNotes((int) ratio.getDenominator());
            modification.setNormalNotes((int) ratio.getNumerator());
            if (tuplet != null && !tuplet.getBaseNote().isEmpty() && !tuplet.getBaseNote().equals(type)) {
                modification.setNormalType(MuseScoreDurationConverter.getInstance().normalizeType(tuplet.getBaseNote()));
            }
            note.setTimeModification(modification);
        }
    }


    /**
     * Adds the notations of a note: ties, slurs, tuplets, articulations, ornaments and fermatas.
     *
     * @param note the note
     * @param event the event for the first note of a chord, null for further notes
     * @param data the note data (null for rests)
     */
    private void applyNotations(NoteImpl note, Event event, NoteData data) {
        NotationsImpl notations = new NotationsImpl();
        boolean used = false;
        if (data != null && (data.isTieStart() || data.isTieStop())) {
            List<TiedImpl> tieds = new ArrayList<>();
            if (data.isTieStop()) {
                tieds.add(tied(STOP));
            }
            if (data.isTieStart()) {
                tieds.add(tied(START));
            }
            notations.setTieds(tieds);
            used = true;
        }
        if (data != null && data.getFingering() != null) {
            PlayingTechniqueImpl technical = new PlayingTechniqueImpl();
            technical.setFingering(data.getFingering());
            notations.setTechnical(technical);
            used = true;
        }
        if (event != null) {
            used |= applyEventNotations(notations, event);
        }
        if (used) {
            note.setNotations(notations);
        }
    }


    /**
     * Adds the notations of an event to the first note.
     *
     * @param notations the notations
     * @param event the event
     * @return true if a notation was added
     */
    private boolean applyEventNotations(NotationsImpl notations, Event event) {
        boolean used = false;
        if (!event.getSlurStops().isEmpty() || !event.getSlurStarts().isEmpty()) {
            List<SlurImpl> slurs = new ArrayList<>();
            for (Integer number : event.getSlurStops()) {
                slurs.add(slur(STOP, number));
            }
            for (Integer number : event.getSlurStarts()) {
                slurs.add(slur(START, number));
            }
            notations.setSlurs(slurs);
            used = true;
        }
        TupletState tuplet = event.getTuplet();
        if (tuplet != null && (tuplet.isFirst(event) || tuplet.isLast(event))) {
            List<TupletImpl> tuplets = new ArrayList<>();
            if (tuplet.isFirst(event)) {
                TupletImpl start = new TupletImpl();
                start.setType(START);
                start.setNumber(1);
                tuplets.add(start);
            }
            if (tuplet.isLast(event)) {
                TupletImpl stop = new TupletImpl();
                stop.setType(STOP);
                stop.setNumber(1);
                tuplets.add(stop);
            }
            notations.setTuplets(tuplets);
            used = true;
        }
        if (event.getFermata() != null) {
            String[] fermata = MuseScoreSymbolConverter.getInstance().fermataToXml(event.getFermata());
            FermataImpl fermataImpl = new FermataImpl();
            if (fermata != null) {
                fermataImpl.setType(fermata[0]);
                if (fermata.length > 1 && fermata[1] != null && !fermata[1].isEmpty()) {
                    fermataImpl.setValue(fermata[1]);
                }
            } else {
                fermataImpl.setType("upright");
            }
            notations.setFermatas(Collections.singletonList(fermataImpl));
            used = true;
        }
        used |= applyArticulations(notations, event);
        return used;
    }


    /**
     * Adds the articulations, technical marks and ornaments of an event.
     *
     * @param notations the notations
     * @param event the event
     * @return true if a mark was added
     */
    private boolean applyArticulations(NotationsImpl notations, Event event) {
        ArticulationsImpl articulations = new ArticulationsImpl();
        PlayingTechniqueImpl technical = new PlayingTechniqueImpl();
        OrnamentsImpl ornaments = new OrnamentsImpl();
        boolean hasArticulation = false;
        boolean hasTechnical = false;
        boolean hasOrnament = false;
        List<String> subtypes = new ArrayList<>(event.getArticulations());
        if (event.isBreath()) {
            articulations.setBreathMark(EMPTY);
            hasArticulation = true;
        }
        for (String subtype : subtypes) {
            String[] marks = MuseScoreSymbolConverter.getInstance().articulationToXml(subtype);
            if (marks == null) {
                continue;
            }
            for (int i = 1; i < marks.length; i++) {
                if (MuseScoreSymbolConverter.GROUP_ORNAMENT.equals(marks[0])) {
                    hasOrnament |= setOrnament(ornaments, marks[i]);
                } else if (MuseScoreSymbolConverter.GROUP_TECHNICAL.equals(marks[0])) {
                    hasTechnical |= setTechnical(technical, marks[i]);
                } else {
                    hasArticulation |= setArticulation(articulations, marks[i]);
                }
            }
        }
        if (hasArticulation) {
            notations.setArticulations(articulations);
        }
        if (hasTechnical) {
            notations.setTechnical(technical);
        }
        if (hasOrnament) {
            notations.setOrnaments(ornaments);
        }
        return hasArticulation || hasTechnical || hasOrnament;
    }


    /**
     * Sets an articulation by its MusicXML name.
     *
     * @param articulations the articulations
     * @param name the MusicXML element name
     * @return true if the name is known
     */
    private boolean setArticulation(ArticulationsImpl articulations, String name) {
        switch (name) {
            case "staccato":
                articulations.setStaccato(EMPTY);
                return true;
            case "accent":
                articulations.setAccent(EMPTY);
                return true;
            case "tenuto":
                articulations.setTenuto(EMPTY);
                return true;
            case "strong-accent":
                articulations.setStrongAccent(EMPTY);
                return true;
            case "staccatissimo":
                articulations.setStaccatissimo(EMPTY);
                return true;
            case "spiccato":
                articulations.setSpiccato(EMPTY);
                return true;
            case "detached-legato":
                articulations.setDetachedLegato(EMPTY);
                return true;
            case "stress":
                articulations.setStress(EMPTY);
                return true;
            case "unstress":
                articulations.setUnstress(EMPTY);
                return true;
            default:
                return false;
        }
    }


    /**
     * Sets a technical mark by its MusicXML name.
     *
     * @param technical the technical marks
     * @param name the MusicXML element name
     * @return true if the name is known
     */
    private boolean setTechnical(PlayingTechniqueImpl technical, String name) {
        switch (name) {
            case "up-bow":
                technical.setUpBow(EMPTY);
                return true;
            case "down-bow":
                technical.setDownBow(EMPTY);
                return true;
            case "harmonic":
                technical.setHarmonic(EMPTY);
                return true;
            case "thumb-position":
                technical.setThumbPosition(EMPTY);
                return true;
            case "open-string":
                technical.setOpenString(EMPTY);
                return true;
            case "stopped":
                technical.setStopped(EMPTY);
                return true;
            case "snap-pizzicato":
                technical.setSnapPizzicato(EMPTY);
                return true;
            case "double-tongue":
                technical.setDoubleTongue(EMPTY);
                return true;
            case "triple-tongue":
                technical.setTripleTongue(EMPTY);
                return true;
            default:
                return false;
        }
    }


    /**
     * Sets an ornament by its MusicXML name.
     *
     * @param ornaments the ornaments
     * @param name the MusicXML element name
     * @return true if the name is known
     */
    private boolean setOrnament(OrnamentsImpl ornaments, String name) {
        switch (name) {
            case "trill-mark":
                ornaments.setTrillMark(EMPTY);
                return true;
            case "turn":
                ornaments.setTurn(EMPTY);
                return true;
            case "inverted-turn":
                ornaments.setInvertedTurn(EMPTY);
                return true;
            case "mordent":
                ornaments.setMordent(EMPTY);
                return true;
            case "inverted-mordent":
                ornaments.setInvertedMordent(EMPTY);
                return true;
            case "schleifer":
                ornaments.setSchleifer(EMPTY);
                return true;
            case "shake":
                ornaments.setShake(EMPTY);
                return true;
            default:
                return false;
        }
    }


    /**
     * Builds the directions of the spanners of a part: hairpins, octave lines, pedals and trills.
     * Slurs are attached to their chords in {@link StaffData#finish(ReadContext)}.
     *
     * @param staves the staves of the part
     * @return the positioned directions
     */
    private PartDirections buildSpannerDirections(List<StaffData> staves) {
        PartDirections result = new PartDirections();
        int[] numbers = new int[1];
        for (StaffData staff : staves) {
            for (SpannerData spanner : staff.getSpanners()) {
                if (spanner.getEnd() == null) {
                    continue;
                }
                String type = spanner.getType();
                if ("HairPin".equals(type)) {
                    int subtype = intValue(child(spanner.getDefinition(), SUBTYPE), 0);
                    int number = nextNumber(numbers);
                    WedgeImpl start = new WedgeImpl();
                    if (subtype == 1 || subtype == 3) {
                        start.setType("diminuendo");
                    } else {
                        start.setType("crescendo");
                    }
                    start.setNumber(number);
                    DirectionTypeImpl startType = new DirectionTypeImpl();
                    startType.setWedge(start);
                    result.add(spanner.getStart(), createDirection(BELOW, startType), staff.getStaffIndex());
                    WedgeImpl stop = new WedgeImpl();
                    stop.setType(STOP);
                    stop.setNumber(number);
                    DirectionTypeImpl stopType = new DirectionTypeImpl();
                    stopType.setWedge(stop);
                    if (spanner.getEnd().compareTo(spanner.getStart()) > 0) {
                        result.addEnd(spanner.getEnd(), createDirection(BELOW, stopType), staff.getStaffIndex());
                    } else {
                        // a hairpin without length ends where it starts (the end of a measure is only after its start)
                        result.add(spanner.getEnd(), createDirection(BELOW, stopType), staff.getStaffIndex());
                    }
                } else if ("Ottava".equals(type)) {
                    String[] ottava = MuseScoreSymbolConverter.getInstance().ottavaToXml(text(child(spanner.getDefinition(), SUBTYPE)));
                    if (ottava == null) {
                        continue;
                    }
                    OctaveShiftImpl start = new OctaveShiftImpl();
                    start.setType(ottava[0]);
                    start.setSize(intValue(ottava[1], 8));
                    DirectionTypeImpl startType = new DirectionTypeImpl();
                    startType.setOctaveShift(start);
                    result.add(spanner.getStart(), createDirection(ABOVE, startType), staff.getStaffIndex());
                    OctaveShiftImpl stop = new OctaveShiftImpl();
                    stop.setType(STOP);
                    stop.setSize(intValue(ottava[1], 8));
                    DirectionTypeImpl stopType = new DirectionTypeImpl();
                    stopType.setOctaveShift(stop);
                    result.add(spanner.getEnd(), createDirection(ABOVE, stopType), staff.getStaffIndex());
                } else if ("Pedal".equals(type)) {
                    PedalImpl start = new PedalImpl();
                    start.setType(START);
                    start.setLine(YES);
                    DirectionTypeImpl startType = new DirectionTypeImpl();
                    startType.setPedal(start);
                    result.add(spanner.getStart(), createDirection(BELOW, startType), staff.getStaffIndex());
                    PedalImpl stop = new PedalImpl();
                    stop.setType(STOP);
                    stop.setLine(YES);
                    DirectionTypeImpl stopType = new DirectionTypeImpl();
                    stopType.setPedal(stop);
                    result.add(spanner.getEnd(), createDirection(BELOW, stopType), staff.getStaffIndex());
                }
            }
        }
        return result;
    }


    /**
     * Gets the next spanner number (1 to 6, cycling).
     *
     * @param numbers the counter
     * @return the number
     */
    private int nextNumber(int[] numbers) {
        numbers[0] = numbers[0] % MAX_NUMBER + 1;
        return numbers[0];
    }


    /**
     * Collects the voltas (repeat endings) of the first staff.
     *
     * @param staves the staves of the part
     * @return the voltas
     */
    private List<VoltaData> collectVoltas(List<StaffData> staves) {
        List<VoltaData> result = new ArrayList<>();
        for (SpannerData spanner : staves.get(0).getSpanners()) {
            if (!"Volta".equals(spanner.getType()) || spanner.getEnd() == null) {
                continue;
            }
            Element definition = spanner.getDefinition();
            String endings = text(child(definition, "endings")).replace(" ", "");
            if (endings.isEmpty()) {
                endings = "1";
            }
            String text = cleanText(child(definition, "beginText"));
            if (text == null) {
                text = cleanText(child(definition, TEXT));
            }
            boolean closed = !"0".equals(text(child(definition, "endHookType"))) && !"0".equals(text(child(definition, SUBTYPE)));
            result.add(new VoltaData(spanner.getStart(), spanner.getEnd(), endings, text, closed));
        }
        return result;
    }


    /**
     * Builds the barlines of a measure: repeats, endings and special bar styles.
     *
     * @param data the measure data of the first staff (may be null)
     * @param voltas the voltas of the part
     * @param frame the measure frame
     * @param lastMeasure true for the last measure
     * @return the barlines (may be empty)
     */
    private List<BarlineImpl> buildBarlines(MeasureData data, List<VoltaData> voltas, MeasureFrame frame, boolean lastMeasure) {
        BarlineImpl left = null;
        BarlineImpl right = null;
        Fraction end = frame.getStart().add(frame.getLength());
        if (data != null && data.isStartRepeat()) {
            left = barline(LEFT);
            left.setBarStyle("heavy-light");
            RepeatImpl repeat = new RepeatImpl();
            repeat.setDirection("forward");
            left.setRepeat(repeat);
        }
        if (data != null && data.getEndRepeat() > 0) {
            right = barline(RIGHT);
            right.setBarStyle("light-heavy");
            RepeatImpl repeat = new RepeatImpl();
            repeat.setDirection("backward");
            if (data.getEndRepeat() > 2) {
                repeat.setTimes(data.getEndRepeat());
            }
            right.setRepeat(repeat);
        }
        for (VoltaData volta : voltas) {
            if (volta.getStart().equals(frame.getStart())) {
                if (left == null) {
                    left = barline(LEFT);
                }
                EndingImpl ending = new EndingImpl();
                ending.setNumber(volta.getEndings());
                ending.setType(START);
                if (volta.getText() != null) {
                    ending.setValue(volta.getText());
                }
                left.setEnding(ending);
            }
            if (volta.getEnd().equals(end)) {
                if (right == null) {
                    right = barline(RIGHT);
                }
                EndingImpl ending = new EndingImpl();
                ending.setNumber(volta.getEndings());
                if (volta.isClosed()) {
                    ending.setType(STOP);
                } else {
                    ending.setType("discontinue");
                }
                right.setEnding(ending);
            }
        }
        if (data != null && data.getBarStyle() != null && (right == null || right.getRepeat() == null)) {
            if (right == null) {
                right = barline(RIGHT);
            }
            right.setBarStyle(data.getBarStyle());
        }
        if (lastMeasure && right == null) {
            right = barline(RIGHT);
            right.setBarStyle("light-heavy");
        }
        List<BarlineImpl> result = new ArrayList<>();
        if (left != null) {
            result.add(left);
        }
        if (right != null) {
            result.add(right);
        }
        return result;
    }


    /**
     * Places a direction in a measure: offset from the measure start and staff.
     *
     * @param positioned the positioned direction
     * @param frame the measure frame
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @param staffIndex the index of the staff within the part
     * @return the direction
     */
    private DirectionImpl positionDirection(PositionedDirection positioned, MeasureFrame frame, int divisions, boolean multiStaff, int staffIndex) {
        DirectionImpl direction = positioned.getDirection();
        int offset = toDivisions(positioned.getPosition().subtract(frame.getStart()), divisions);
        if (offset > 0) {
            direction.setOffset(offset);
        }
        if (multiStaff) {
            direction.setStaff(staffIndex + 1);
        }
        return direction;
    }


    /**
     * Creates a barline.
     *
     * @param location left or right
     * @return the barline
     */
    private BarlineImpl barline(String location) {
        BarlineImpl barline = new BarlineImpl();
        barline.setLocation(location);
        return barline;
    }


    /**
     * Creates a direction.
     *
     * @param placement above or below
     * @param types the direction types
     * @return the direction
     */
    private DirectionImpl createDirection(String placement, DirectionTypeImpl... types) {
        DirectionImpl direction = new DirectionImpl();
        direction.setPlacement(placement);
        List<DirectionTypeImpl> list = new ArrayList<>();
        Collections.addAll(list, types);
        direction.setDirectionTypes(list);
        return direction;
    }


    /**
     * Creates a words direction type.
     *
     * @param text the text
     * @param fontStyle the font style, or null
     * @return the direction type
     */
    private DirectionTypeImpl wordsType(String text, String fontStyle) {
        DirectionTypeImpl type = new DirectionTypeImpl();
        List<WordsImpl> list = new ArrayList<>();
        list.add(words(text, fontStyle));
        type.setWords(list);
        return type;
    }


    /**
     * Creates words.
     *
     * @param text the text
     * @param fontStyle the font style, or null
     * @return the words
     */
    private WordsImpl words(String text, String fontStyle) {
        WordsImpl words = new WordsImpl();
        words.setValue(text);
        if (fontStyle != null) {
            words.setFontStyle(fontStyle);
        }
        return words;
    }


    /**
     * Creates a tie.
     *
     * @param type start or stop
     * @return the tie
     */
    private TieImpl tie(String type) {
        TieImpl tie = new TieImpl();
        tie.setType(type);
        return tie;
    }


    /**
     * Creates a tied notation.
     *
     * @param type start or stop
     * @return the tied notation
     */
    private TiedImpl tied(String type) {
        TiedImpl tied = new TiedImpl();
        tied.setType(type);
        return tied;
    }


    /**
     * Creates a slur notation.
     *
     * @param type start or stop
     * @param number the number
     * @return the slur
     */
    private SlurImpl slur(String type, int number) {
        SlurImpl slur = new SlurImpl();
        slur.setType(type);
        slur.setNumber(number);
        return slur;
    }


    /**
     * Creates the dots of a note.
     *
     * @param count the number of dots
     * @return the dots
     */
    private List<String> dots(int count) {
        List<String> dots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            dots.add(EMPTY);
        }
        return dots;
    }


    /**
     * Converts a position or length in whole notes into divisions.
     *
     * @param wholes the position in whole notes
     * @param divisions the divisions per quarter note
     * @return the divisions
     */
    private int toDivisions(Fraction wholes, int divisions) {
        Fraction quarters = wholes.multiply(QUARTERS_PER_WHOLE);
        return (int) Math.floorDiv(quarters.getNumerator() * divisions, quarters.getDenominator());
    }


    /**
     * Parses a MuseScore fraction element ("3/4" or "-1/4").
     *
     * @param element the element (may be null)
     * @return the fraction, zero if missing or invalid
     */
    private Fraction readFraction(Element element) {
        String text = text(element);
        if (text.isEmpty()) {
            return Fraction.ZERO;
        }
        Fraction result = Fraction.parse(text);
        if (result == null) {
            return Fraction.ZERO;
        }
        return result;
    }


    /**
     * Reads the stretch of a local time signature: the factor from nominal to actual durations.
     *
     * @param element the TimeSig element
     * @return the factor (1 for a global time signature)
     */
    private Fraction readStretch(Element element) {
        int stretchN = intValue(child(element, "stretchN"), 1);
        int stretchD = intValue(child(element, "stretchD"), 1);
        if (stretchN <= 0 || stretchD <= 0 || stretchN == stretchD) {
            return WHOLE;
        }
        return Fraction.of(stretchD, stretchN);
    }


    /**
     * Reads a time signature.
     *
     * @param element the TimeSig element
     * @return the time signature, or null if it is invalid
     */
    private TimeSignature readTimeSignature(Element element) {
        int beats = intValue(child(element, "sigN"), 0);
        int beatType = intValue(child(element, "sigD"), 0);
        if (beats <= 0 || beatType <= 0) {
            // version 1 files
            beats = intValue(child(element, "nom1"), 0);
            beatType = intValue(child(element, "den"), 0);
        }
        if (beats <= 0 || beatType <= 0) {
            return null;
        }
        return new TimeSignature(beats, beatType);
    }


    /**
     * Parses an XML document securely (no DTD, no external entities).
     *
     * @param content the document
     * @return the document
     * @throws IOException if the document cannot be parsed
     */
    private Document parse(byte[] content) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(new ByteArrayInputStream(content));
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to parse MuseScore document: {}", e.getMessage(), e);
            }
            throw new IOException("Failed to parse MuseScore document: " + e.getMessage(), e);
        }
    }


    /**
     * Gets the text of a MuseScore text element without formatting tags. Symbols are replaced by
     * their Unicode character where known.
     *
     * @param element the element (may be null)
     * @return the text, or null if it is empty
     */
    private String cleanText(Element element) {
        if (element == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        appendText(element, sb);
        String result = sb.toString().replace('\u00A0', ' ').replaceAll("[ \\t]+", " ").trim();
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }


    /**
     * Appends the text of a node and its children; symbol elements are mapped, line breaks (br) are kept.
     *
     * @param node the node
     * @param sb the text
     */
    private void appendText(Node node, StringBuilder sb) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(child.getNodeValue());
            } else if (child instanceof Element) {
                String tag = ((Element) child).getTagName();
                if ("sym".equals(tag)) {
                    String symbol = SYMBOLS.get(child.getTextContent().trim());
                    if (symbol != null) {
                        sb.append(symbol);
                    }
                } else if ("br".equals(tag)) {
                    sb.append('\n');
                } else if (!"font".equals(tag)) {
                    appendText(child, sb);
                }
            }
        }
    }


    /**
     * Gets the first child element with a name.
     *
     * @param parent the parent (may be null)
     * @param name the name
     * @return the element, or null
     */
    private Element child(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && name.equals(((Element) node).getTagName())) {
                return (Element) node;
            }
        }
        return null;
    }


    /**
     * Gets the child elements with a name.
     *
     * @param parent the parent (may be null)
     * @param name the name, or null for all child elements
     * @return the elements
     */
    private List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        if (parent == null) {
            return result;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && (name == null || name.equals(((Element) node).getTagName()))) {
                result.add((Element) node);
            }
        }
        return result;
    }


    /**
     * Gets the trimmed text of an element.
     *
     * @param element the element (may be null)
     * @return the text, or an empty string
     */
    private String text(Element element) {
        if (element == null) {
            return EMPTY;
        }
        return element.getTextContent().trim();
    }


    /**
     * Gets the integer value of an element.
     *
     * @param element the element (may be null)
     * @param defaultValue the value if the element is missing or invalid
     * @return the value
     */
    private int intValue(Element element, int defaultValue) {
        return intValue(text(element), defaultValue);
    }


    /**
     * Parses an integer.
     *
     * @param value the text
     * @param defaultValue the value if the text is invalid
     * @return the value
     */
    private int intValue(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }


    /**
     * Gets the decimal value of an element.
     *
     * @param element the element (may be null)
     * @return the value, or 0
     */
    private double doubleValue(Element element) {
        try {
            return Double.parseDouble(text(element));
        } catch (RuntimeException e) {
            return 0;
        }
    }


    /**
     * Gets the first value that is not null.
     *
     * @param values the values
     * @return the value, or null
     */
    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }


    /**
     * Creates the mapping of articulation names of version 1 and 2 files to current symbol names.
     *
     * @return the map
     */
    private static Map<String, String> createLegacyArticulations() {
        Map<String, String> map = new HashMap<>();
        map.put("staccato", "articStaccatoAbove");
        map.put("sforzato", "articAccentAbove");
        map.put("tenuto", "articTenutoAbove");
        map.put("marcato", "articMarcatoAbove");
        map.put("staccatissimo", "articStaccatissimoAbove");
        map.put("portato", "articTenutoStaccatoAbove");
        map.put("fermata", "fermataAbove");
        map.put("ufermata", "fermataAbove");
        map.put("dfermata", "fermataBelow");
        map.put("shortfermata", "fermataShortAbove");
        map.put("longfermata", "fermataLongAbove");
        map.put("trill", "ornamentTrill");
        map.put("turn", "ornamentTurn");
        map.put("reverseturn", "ornamentTurnInverted");
        map.put("mordent", "ornamentMordent");
        map.put("prall", "ornamentShortTrill");
        map.put("upbow", "stringsUpBow");
        map.put("downbow", "stringsDownBow");
        map.put("plusstop", "brassMuteClosed");
        map.put("ouvert", "brassMuteOpen");
        return map;
    }


    /**
     * Creates the mapping of SMuFL symbol names used in texts to Unicode characters.
     *
     * @return the map
     */
    private static Map<String, String> createSymbols() {
        Map<String, String> map = new HashMap<>();
        map.put("metNoteQuarterUp", "♩");
        map.put("metNote8thUp", "♪");
        map.put("metNoteHalfUp", "𝅗𝅥");
        map.put("metAugmentationDot", ".");
        map.put("accidentalFlat", "♭");
        map.put("accidentalSharp", "♯");
        map.put("accidentalNatural", "♮");
        map.put("segno", "𝄋");
        map.put("coda", "𝄌");
        return map;
    }
}
