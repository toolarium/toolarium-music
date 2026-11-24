/*
 * FinaleReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.converter.EncodingConverter;
import com.github.toolarium.music.io.converter.InstrumentSoundConverter;
import com.github.toolarium.music.io.converter.PitchSpeller;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.SpelledPitch;
import com.github.toolarium.music.io.format.finale.FinaleReaderOptions;
import com.github.toolarium.music.io.format.finale.impl.dto.FinaleExpression;
import com.github.toolarium.music.io.format.finale.impl.dto.FinaleFrame;
import com.github.toolarium.music.io.format.finale.impl.dto.FinaleHairpin;
import com.github.toolarium.music.io.format.finale.impl.dto.FinaleNotationData;
import com.github.toolarium.music.io.format.finale.impl.dto.FinaleOctaveShift;
import com.github.toolarium.music.io.format.finale.impl.dto.FinaleTuplet;
import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.barline.impl.EndingImpl;
import com.github.toolarium.music.model.barline.impl.RepeatImpl;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.BackupImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.measure.impl.ForwardImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.PrintImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.measure.impl.TransposeImpl;
import com.github.toolarium.music.model.notation.impl.NotationsImpl;
import com.github.toolarium.music.model.notation.impl.TiedImpl;
import com.github.toolarium.music.model.note.impl.GraceImpl;
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
import com.github.toolarium.music.model.score.impl.CreditWordsImpl;
import com.github.toolarium.music.model.score.impl.EncodingImpl;
import com.github.toolarium.music.model.score.impl.IdentificationImpl;
import com.github.toolarium.music.model.score.impl.MiscellaneousFieldImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import com.github.toolarium.music.model.score.impl.WorkImpl;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Reads Finale .musx and .mus files and converts them to an {@link IScore} model.
 *
 * <p>For .musx files (Finale 2014+), the conversion pipeline is: ZIP container to
 * extract score.dat to XOR decrypt to gzip decompress to parse EnigmaXML to map
 * to IScore model.</p>
 *
 * <p>For .mus files (Finale 1.0 through 2012), the legacy binary Enigma format
 * is parsed directly from the tagged data store.</p>
 *
 * @author patrick
 */
public class FinaleReaderImpl implements IMusicReader {
    private static final Logger LOG = LoggerFactory.getLogger(FinaleReaderImpl.class);
    private static final int BUFFER_SIZE = 8192;

    /** Prefix of the reduced (cue sized) notes in the note alterations. */
    private static final String CUE = "cue:";

    /** Separator of the entry number and the note ID of a note. */
    private static final String ID_SEPARATOR = ":";

    /** The normal note size in percent. */
    private static final int FULL_SIZE = 100;
    private static final long MAX_ENTRY_SIZE = 100L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 200L * 1024 * 1024;
    private static final int MAX_ENTRY_COUNT = 500;
    private static final String NS = "http://www.makemusic.com/2012/finale";
    private static final String ENIGMA_HEADER = "ENIGMA BINARY FI";
    private static final int ENIGMA_HEADER_LEN = 16;
    private static final int DIVISIONS = 1024;
    private static final int WHOLE_EDU = 4096;
    private static final int COMPOUND_BEAT_UNITS = 3;
    private static final int VOICES_PER_STAFF = 4;
    private static final int MAX_ENTRIES_PER_FRAME = 10000;
    private static final int STEPS_PER_OCTAVE = 7;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int MAX_DIVISION_SCALE = 840;
    private static final int LAYERS = 4;
    private static final String ATTR_CMPER = "cmper";
    private static final String ATTR_PART = "part";
    private static final String END_POINT = "endPt";
    private static final int OCTAVE = 12;
    private static final int OCTAVE_SIZE = 8;
    private static final int TWO_OCTAVES_SIZE = 15;
    private static final int CLEF_PERCUSSION = 4;
    private static final int CLEF_PERCUSSION_2 = 12;
    private static final String TIE_START = "start";
    private static final String TIE_STOP = "stop";
    private static final int SPECIAL_STAFF = 32767;
    private static final String YES = "yes";
    private final FinaleReaderOptions options;


    /**
     * Constructor for FinaleReaderImpl with the default options.
     */
    public FinaleReaderImpl() {
        this(new FinaleReaderOptions());
    }


    /**
     * Constructor for FinaleReaderImpl.
     *
     * @param options the options: tempo steps of gradual tempo changes, fonts of the page texts, page and system breaks
     */
    public FinaleReaderImpl(FinaleReaderOptions options) {
        if (options == null) {
            this.options = new FinaleReaderOptions();
        } else {
            this.options = options;
        }
    }


    /**
     * Reads a Finale .musx file from the given path.
     *
     * @param path the path to the .musx file
     * @return the parsed score
     * @throws IOException if an I/O error occurs
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading Finale score from path: {}", path);
        }

        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


    /**
     * Reads a Finale .musx or .mus file from an input stream. The format is
     * auto-detected from the stream header: .mus files start with "ENIGMA BINARY FI",
     * while .musx files start with a ZIP signature.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException if an I/O error occurs
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading Finale score from input stream.");
        }

        BufferedInputStream buffered = new BufferedInputStream(inputStream);
        buffered.mark(ENIGMA_HEADER_LEN);
        byte[] header = new byte[ENIGMA_HEADER_LEN];
        int headerRead = buffered.read(header);
        buffered.reset();

        if (headerRead >= ENIGMA_HEADER_LEN) {
            String headerStr = new String(header, StandardCharsets.US_ASCII);
            if (headerStr.startsWith(ENIGMA_HEADER)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Detected .mus (legacy binary) format.");
                }
                byte[] allBytes = readAllBytes(buffered);
                Document musDoc;
                try {
                    musDoc = MusEnigmaConverter.getInstance().convert(allBytes);
                } catch (IOException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Converting the .mus records failed, reading the notes only: {}", e.getMessage(), e);
                    }
                    return MusBinaryParser.getInstance().parse(allBytes);
                }
                return convertToScore(musDoc);
            }
        }

        byte[] scoreData = extractMusxScoreData(buffered);
        byte[] enigmaXml = EnigmaXmlDecryptor.getInstance().decrypt(scoreData);
        String xmlString = new String(enigmaXml, StandardCharsets.UTF_8);
        Document doc = parseXml(xmlString.getBytes(StandardCharsets.UTF_8));
        return convertToScore(doc);
    }


    /**
     * Extracts the score.dat bytes from a .musx ZIP archive.
     *
     * @param inputStream the input stream positioned at the start of the .musx ZIP data
     * @return the raw score.dat bytes
     * @throws IOException if extraction fails
     */
    private byte[] extractMusxScoreData(InputStream inputStream) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        long totalSize = 0;

        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            int entryCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw new IOException("MUSX archive exceeds maximum entry count.");
                }

                byte[] data = readBounded(zis);
                totalSize += data.length;
                if (totalSize > MAX_TOTAL_SIZE) {
                    throw new IOException("MUSX archive exceeds maximum total decompressed size.");
                }

                entries.put(entry.getName(), data);
                zis.closeEntry();
            }
        } catch (IllegalArgumentException e) {
            // a damaged entry (for example a broken file name) is reported by the ZIP stream as illegal argument
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid ZIP archive: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid ZIP archive: " + e.getMessage(), e);
        }

        byte[] scoreData = entries.get("score.dat");
        if (scoreData == null || scoreData.length == 0) {
            throw new IOException("No score.dat found in the .musx archive.");
        }

        return scoreData;
    }


    /**
     * Parses an XML byte array into a DOM Document with secure parser settings.
     *
     * @param xmlBytes the XML content
     * @return the parsed Document
     * @throws IOException if parsing fails
     */
    private Document parseXml(byte[] xmlBytes) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new java.io.ByteArrayInputStream(xmlBytes));
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to parse EnigmaXML: {}", e.getMessage(), e);
            }
            throw new IOException("Failed to parse EnigmaXML", e);
        }
    }


    /**
     * Converts an EnigmaXML DOM document into an IScore model.
     *
     * @param doc the EnigmaXML DOM document
     * @return the converted score
     * @throws IOException if conversion fails
     */
    private IScore convertToScore(Document doc) throws IOException {
        Element root = doc.getDocumentElement();

        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");

        List<CreditImpl> credits = FinaleNotationReader.getInstance().readCredits(root, options.isFonts());
        buildIdentification(root, score, credits);
        buildMovementTitle(root, score);
        if (!credits.isEmpty()) {
            score.setCredits(credits);
            score.setDefaults(FinaleNotationReader.getInstance().readDefaults(root));
        }

        Map<Integer, StaffInfo> staffInfoMap = parseStaffSpecs(root);
        Map<Integer, MeasureInfo> measureInfoMap = parseMeasureSpecs(root);
        Map<Integer, Element> entryMap = parseEntries(root);
        Map<Integer, FinaleFrame> frameMap = parseFrameSpecs(root);
        Map<String, Map<Integer, int[]>> gfholdMap = parseGfholds(root);
        Map<Integer, FinaleTuplet> tuplets = parseTupletDefs(root);
        Set<String> enharmonics = parseEnharmonics(root);
        List<FinaleExpression> expressions = FinaleNotationReader.getInstance().readExpressions(root, options.isTempoSteps());
        Map<Integer, List<String>> entryMarks = FinaleNotationReader.getInstance().readEntryMarks(root);
        Map<Integer, int[]> playbackRoutes = FinaleNotationReader.getInstance().readPlaybackRoutes(root);
        List<PartDefInfo> partDefs = parsePartDefs(root);
        Map<Integer, GroupInfo> groups = parseMultiStaffGroups(root);

        List<Integer> partStaves = determinePartStaves(staffInfoMap, groups, parseScoreStaves(root));
        int pickup = pickupTime(partStaves, groups, measureInfoMap, frameMap, gfholdMap);
        List<FinaleOctaveShift> octaveShifts = parseOctaveShifts(root);
        List<FinaleHairpin> hairpins = FinaleNotationReader.getInstance().readHairpins(root);
        FinaleNotationData notation = new FinaleNotationData(tuplets, octaveShifts, hairpins, enharmonics, entryMarks);
        if (options.isLayoutBreaks()) {
            notation.setBreaks(FinaleNotationReader.getInstance().readBreaks(root));
        } else {
            notation.setBreaks(new HashMap<>());
        }

        PartListImpl partList = new PartListImpl();
        List<ScorePartImpl> scoreParts = new ArrayList<>();
        List<PartImpl> parts = new ArrayList<>();

        for (int idx = 0; idx < partStaves.size(); idx++) {
            int staffCmper = partStaves.get(idx);
            StaffInfo staffInfo = staffInfoMap.get(staffCmper);
            String partId = "P" + (idx + 1);

            ScorePartImpl scorePart = new ScorePartImpl();
            scorePart.setId(partId);
            String partName = resolveStaffName(root, staffInfo);
            if (partName == null || partName.isEmpty()) {
                // Finale names a staff without a name MusicXML Part
                partName = "MusicXML Part";
            }
            scorePart.setPartName(partName);
            int[] route = playbackRoutes.get(staffCmper);
            if (route != null) {
                ScoreInstrumentImpl instrument = new ScoreInstrumentImpl();
                instrument.setId(partId + "-I1");
                instrument.setInstrumentName(partName);
                int chromatic = 0;
                if (staffInfo != null) {
                    chromatic = createTranspose(staffInfo).getChromatic();
                }
                instrument.setInstrumentSound(InstrumentSoundConverter.getInstance().getSound(route[0], chromatic));
                scorePart.setScoreInstruments(Collections.singletonList(instrument));
                MidiInstrumentImpl midi = new MidiInstrumentImpl();
                midi.setId(partId + "-I1");
                midi.setMidiChannel(route[1]);
                midi.setMidiProgram(route[0]);
                scorePart.setMidiInstruments(Collections.singletonList(midi));
            }
            scoreParts.add(scorePart);

            List<Integer> groupedStaves = new ArrayList<>();
            groupedStaves.add(staffCmper);
            GroupInfo group = findGroupForStaff(staffCmper, groups);
            if (group != null) {
                if (group.getStaffNum1() == staffCmper && staffInfoMap.containsKey(group.getStaffNum2())) {
                    groupedStaves.add(group.getStaffNum2());
                }
            }

            // the expressions of the staves of the part; expressions of the whole score (tempo, rehearsal marks)
            // belong to the first part, applications show them for the whole system
            List<FinaleExpression> partExpressions = new ArrayList<>();
            for (FinaleExpression expression : expressions) {
                boolean scoreWide = expression.getStaff() < 0 && idx == 0;
                if (groupedStaves.contains(expression.getStaff()) || scoreWide) {
                    partExpressions.add(expression);
                }
            }
            PartImpl part = buildPart(partId, groupedStaves, measureInfoMap, entryMap,
                    frameMap, gfholdMap, staffInfoMap, pickup, notation, partExpressions);
            parts.add(part);
        }

        partList.setScoreParts(scoreParts);
        score.setPartList(partList);
        score.setParts(parts);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Converted Finale score: {} parts, {} measures.",
                    parts.size(), measureInfoMap.size());
        }

        return score;
    }


    /**
     * Builds the identification section from EnigmaXML fileInfo elements.
     *
     * @param root the EnigmaXML root element
     * @param score the score to populate
     * @param credits the texts of the first page: the copyright text on the page is the rights
     */
    private void buildIdentification(Element root, ScoreImpl score, List<CreditImpl> credits) {
        IdentificationImpl identification = new IdentificationImpl();

        EncodingImpl encoding = new EncodingImpl();
        List<String> software = new ArrayList<>();
        software.add(EncodingConverter.getInstance().getSoftware());
        encoding.setSoftware(software);
        // the creation date of the Finale file
        Element created = getChildElement(getChildElement(getChildElement(root, "header"), "headerData"), "created");
        if (created != null && getIntChild(created, "year", 0) > 0) {
            String date = String.format("%04d-%02d-%02d", getIntChild(created, "year", 0), getIntChild(created, "month", 1), getIntChild(created, "day", 1));
            encoding.setEncodingDate(date);
            List<MiscellaneousFieldImpl> miscellaneous = new ArrayList<>();
            miscellaneous.add(new MiscellaneousFieldImpl("creationDate", date));
            identification.setMiscellaneous(miscellaneous);
        }
        identification.setEncoding(encoding);

        List<CreatorImpl> creators = new ArrayList<>();
        for (Element fi : FinaleElementIndex.getInstance().getElements(root, "fileInfo")) {
            String type = fi.getAttribute("type");
            String text = fi.getTextContent();
            if (text == null || text.trim().isEmpty()) {
                continue;
            }
            text = text.trim();

            if ("composer".equals(type) || "arranger".equals(type) || "lyricist".equals(type)) {
                CreatorImpl creator = new CreatorImpl();
                creator.setType(type);
                creator.setValue(text);
                creators.add(creator);
            } else if ("copyright".equals(type)) {
                identification.setRights(text);
            }
        }
        for (CreditImpl credit : credits) {
            if ("rights".equals(credit.getCreditType()) && credit.getCreditWordsList() != null) {
                StringBuilder rights = new StringBuilder();
                for (CreditWordsImpl words : credit.getCreditWordsList()) {
                    rights.append(words.getValue());
                }
                identification.setRights(rights.toString());
            }
        }

        if (!creators.isEmpty()) {
            identification.setCreators(creators);
        }

        score.setIdentification(identification);
    }


    /**
     * Extracts the work and movement title from EnigmaXML fileInfo.
     *
     * @param root the EnigmaXML root element
     * @param score the score to populate
     */
    private void buildMovementTitle(Element root, ScoreImpl score) {
        for (Element fi : FinaleElementIndex.getInstance().getElements(root, "fileInfo")) {
            if ("title".equals(fi.getAttribute("type"))) {
                String text = fi.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    WorkImpl work = new WorkImpl();
                    work.setWorkTitle(text.trim());
                    score.setWork(work);
                    score.setMovementTitle(text.trim());
                }
                break;
            }
        }
    }


    /**
     * Parses staffSpec elements into a map by cmper.
     *
     * @param root the EnigmaXML root element
     * @return map of staff cmper to StaffInfo
     */
    private Map<Integer, StaffInfo> parseStaffSpecs(Element root) {
        Map<Integer, StaffInfo> result = new TreeMap<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "staffSpec")) {
            int cmper = Integer.parseInt(el.getAttribute(ATTR_CMPER));
            if (cmper == SPECIAL_STAFF) {
                continue;
            }

            StaffInfo info = new StaffInfo();
            info.setCmper(cmper);
            info.setDefaultClef(getIntChild(el, "defaultClef", 0));
            info.setFullNameRef(getIntChild(el, "fullName", -1));
            info.setTransInterval(0);
            Element trans = getChildElement(el, "transposition");
            if (trans != null) {
                Element keySig = getChildElement(trans, "keysig");
                if (keySig != null) {
                    info.setTransInterval(getIntChild(keySig, "interval", 0));
                    info.setTransKeyAdjust(getIntChild(keySig, "adjust", 0));
                }
            }

            result.put(cmper, info);
        }
        return result;
    }


    /**
     * Parses measSpec elements into a map by measure number (cmper).
     *
     * @param root the EnigmaXML root element
     * @return map of measure number to MeasureInfo
     */
    private Map<Integer, MeasureInfo> parseMeasureSpecs(Element root) {
        Map<Integer, MeasureInfo> result = new TreeMap<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "measSpec")) {
            String partAttr = el.getAttribute(ATTR_PART);
            if (partAttr != null && !partAttr.isEmpty()) {
                continue;
            }

            int cmper = Integer.parseInt(el.getAttribute(ATTR_CMPER));
            if (result.containsKey(cmper)) {
                continue;
            }

            MeasureInfo info = new MeasureInfo();
            info.setNumber(cmper);
            int key = getIntChild(getChildElement(el, "keySig"), "key", 0);
            info.setKeyFifths(FinalePitchConverter.getInstance().fifthsOf(key));
            info.setMinor(FinalePitchConverter.getInstance().isMinor(key));
            info.setBeats(getIntChild(el, "beats", 4));
            info.setDivbeat(getIntChild(el, "divbeat", DIVISIONS));
            info.setDisplayBeats(getIntChild(el, "dispBeats", info.getBeats()));
            info.setDisplayDivbeat(getIntChild(el, "dispDivbeat", info.getDivbeat()));
            info.setDisplayTime(hasChild(el, "useDisplayTimesig"));
            info.setBarline(getTextChild(el, "barline"));
            info.setWidth(getIntChild(el, "width", 0));
            info.setForwardRepeat(hasChild(el, "forRepBar") || hasChild(el, "forwardRepeat"));
            info.setBackwardRepeat(hasChild(el, "bacRepBar") || hasChild(el, "backwardRepeat"));
            info.setEnding(hasChild(el, "barEnding"));

            result.put(cmper, info);
        }

        // the passes of an ending (repeatPassList with one act per pass)
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "repeatPassList")) {
            String partAttr = el.getAttribute(ATTR_PART);
            MeasureInfo info = result.get(Integer.parseInt(el.getAttribute(ATTR_CMPER)));
            if ((partAttr != null && !partAttr.isEmpty()) || info == null || !info.isEnding()) {
                continue;
            }
            List<String> passes = new ArrayList<>();
            for (Element act : getChildElements(el, "act")) {
                passes.add(act.getTextContent().trim());
            }
            if (!passes.isEmpty()) {
                info.setEndingNumber(String.join(", ", passes));
            }
        }
        return result;
    }


    /**
     * Parses entry elements into a map by entry number.
     *
     * @param root the EnigmaXML root element
     * @return map of entry number to entry Element
     */
    private Map<Integer, Element> parseEntries(Element root) {
        Map<Integer, Element> result = new LinkedHashMap<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "entry")) {
            int entnum = Integer.parseInt(el.getAttribute("entnum"));
            result.put(entnum, el);
        }
        return result;
    }


    /**
     * Parses frameSpec elements into a map by frame cmper. A frame can consist of several records
     * (attribute inci) that are merged: the first and last entry of the frame and its start time.
     *
     * @param root the EnigmaXML root element
     * @return map of frame cmper to frame
     */
    private Map<Integer, FinaleFrame> parseFrameSpecs(Element root) {
        Map<Integer, FinaleFrame> result = new LinkedHashMap<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "frameSpec")) {
            String partAttr = el.getAttribute(ATTR_PART);
            if (partAttr != null && !partAttr.isEmpty()) {
                continue;
            }
            int cmper = Integer.parseInt(el.getAttribute(ATTR_CMPER));
            FinaleFrame frame = new FinaleFrame(getIntChild(el, "startEntry", 0), getIntChild(el, "endEntry", 0), getIntChild(el, "startTime", 0));
            FinaleFrame previous = result.get(cmper);
            if (previous != null) {
                frame = previous.merge(frame);
            }
            result.put(cmper, frame);
        }
        return result;
    }


    /**
     * Parses gfhold elements: the frames of the four layers of every staff and measure.
     *
     * @param root the EnigmaXML root element
     * @return map of staff cmper to (measure cmper to the frames of the layers, 0 for none)
     */
    private Map<String, Map<Integer, int[]>> parseGfholds(Element root) {
        Map<String, Map<Integer, int[]>> result = new LinkedHashMap<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "gfhold")) {
            String partAttr = el.getAttribute(ATTR_PART);
            if (partAttr != null && !partAttr.isEmpty()) {
                continue;
            }

            int[] frames = new int[LAYERS];
            boolean any = false;
            for (int layer = 0; layer < LAYERS; layer++) {
                frames[layer] = getIntChild(el, "frame" + (layer + 1), 0);
                any |= frames[layer] > 0;
            }
            if (any) {
                result.computeIfAbsent(el.getAttribute("cmper1"), k -> new TreeMap<>()).put(Integer.parseInt(el.getAttribute("cmper2")), frames);
            }
        }
        return result;
    }


    /**
     * Parses partDef elements.
     *
     * @param root the EnigmaXML root element
     * @return list of PartDefInfo
     */
    private List<PartDefInfo> parsePartDefs(Element root) {
        List<PartDefInfo> result = new ArrayList<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "partDef")) {
            PartDefInfo info = new PartDefInfo();
            info.setCmper(Integer.parseInt(el.getAttribute(ATTR_CMPER)));
            info.setPartOrder(getIntChild(el, "partOrder", 0));
            info.setDefaultNameStaff(getIntChild(el, "defaultNameStaff", -1));
            info.setDefaultNameGroup(getIntChild(el, "defaultNameGroup", -1));
            result.add(info);
        }
        return result;
    }


    /**
     * Parses multiStaffInstGroup elements.
     *
     * @param root the EnigmaXML root element
     * @return map of group cmper to GroupInfo
     */
    private Map<Integer, GroupInfo> parseMultiStaffGroups(Element root) {
        Map<Integer, GroupInfo> result = new LinkedHashMap<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "multiStaffInstGroup")) {
            String partAttr = el.getAttribute(ATTR_PART);
            if (partAttr != null && !partAttr.isEmpty()) {
                continue;
            }

            int cmper = Integer.parseInt(el.getAttribute(ATTR_CMPER));
            GroupInfo info = new GroupInfo();
            info.setStaffNum1(getIntChild(el, "staffNum1", 0));
            info.setStaffNum2(getIntChild(el, "staffNum2", 0));
            result.put(cmper, info);
        }
        return result;
    }


    /**
     * Determines the staves that start a part, in score order: the staves of the score's staff list
     * (staves that are in no staff list, for example leftover or playback staves, are not part of the
     * score), without the second staff of a multi-staff group.
     *
     * @param staffInfoMap the staff specifications
     * @param groups the multi-staff groups
     * @param scoreStaves the staves of the score in score order (empty to take all staves)
     * @return the staves that start a part
     */
    private List<Integer> determinePartStaves(Map<Integer, StaffInfo> staffInfoMap,
                                              Map<Integer, GroupInfo> groups,
                                              List<Integer> scoreStaves) {
        java.util.Set<Integer> secondary = new java.util.LinkedHashSet<>();
        for (GroupInfo g : groups.values()) {
            secondary.add(g.getStaffNum2());
        }

        List<Integer> candidates = new ArrayList<>();
        for (int cmper : scoreStaves) {
            if (staffInfoMap.containsKey(cmper)) {
                candidates.add(cmper);
            }
        }
        if (candidates.isEmpty()) {
            candidates.addAll(staffInfoMap.keySet());
        }

        List<Integer> result = new ArrayList<>();
        for (int cmper : candidates) {
            if (!secondary.contains(cmper)) {
                result.add(cmper);
            }
        }
        return result;
    }


    /**
     * Parses the staves of the score in score order from the staff lists (instUsed): the list of the
     * score (cmper 0), or the union of all lists if it has none.
     *
     * @param root the EnigmaXML root element
     * @return the staves in score order
     */
    private List<Integer> parseScoreStaves(Element root) {
        TreeMap<Integer, TreeMap<Integer, Integer>> lists = new TreeMap<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "instUsed")) {
            String partAttr = el.getAttribute(ATTR_PART);
            if (partAttr != null && !partAttr.isEmpty()) {
                continue;
            }
            int staff = getIntChild(el, "inst", -1);
            if (staff <= 0 || staff == SPECIAL_STAFF) {
                continue;
            }
            lists.computeIfAbsent(Integer.parseInt(el.getAttribute(ATTR_CMPER)), k -> new TreeMap<>())
                 .put(Integer.parseInt(el.getAttribute("inci")), staff);
        }

        java.util.Set<Integer> result = new java.util.LinkedHashSet<>();
        if (lists.containsKey(0)) {
            result.addAll(lists.get(0).values());
        } else {
            for (TreeMap<Integer, Integer> list : lists.values()) {
                result.addAll(list.values());
            }
        }
        return new ArrayList<>(result);
    }


    /**
     * Finds the multi-staff group that contains the given staff as its first member.
     *
     * @param staffCmper the staff cmper
     * @param groups the group definitions
     * @return the group, or null if not found
     */
    private GroupInfo findGroupForStaff(int staffCmper, Map<Integer, GroupInfo> groups) {
        for (GroupInfo g : groups.values()) {
            if (g.getStaffNum1() == staffCmper) {
                return g;
            }
        }
        return null;
    }


    /**
     * Resolves the gfhold map for a given staff. A staff without gfholds has no music: the gfholds of
     * the special staff 32767 belong to another staff and are not used.
     *
     * @param staffCmper the staff component number
     * @param gfholdMap the complete gfhold map keyed by cmper1 string
     * @return the measure-to-frame map for this staff, or null
     */
    private Map<Integer, int[]> resolveGfholds(int staffCmper, Map<String, Map<Integer, int[]>> gfholdMap) {
        return gfholdMap.get(String.valueOf(staffCmper));
    }


    /**
     * Resolves the staff name from text references.
     *
     * @param root the EnigmaXML root element
     * @param staffInfo the staff info
     * @return the staff name, or null
     */
    private String resolveStaffName(Element root, StaffInfo staffInfo) {
        if (staffInfo == null || staffInfo.getFullNameRef() < 0) {
            return null;
        }

        // the full name is a text block whose text is the block text with the text block's text ID
        int textId = -1;
        for (Element textBlock : FinaleElementIndex.getInstance().getElements(root, "textBlock")) {
            if (String.valueOf(staffInfo.getFullNameRef()).equals(textBlock.getAttribute(ATTR_CMPER))) {
                textId = getIntChild(textBlock, "textID", -1);
                break;
            }
        }
        if (textId < 0) {
            return null;
        }

        for (Element bt : FinaleElementIndex.getInstance().getElements(root, "blockText")) {
            String numAttr = bt.getAttribute("number");
            if (numAttr != null && !numAttr.isEmpty()) {
                int num = Integer.parseInt(numAttr);
                if (num == textId) {
                    String text = bt.getTextContent();
                    if (text != null) {
                        return FinaleTextConverter.getInstance().getPlainText(text);
                    }
                }
            }
        }
        return null;
    }


    /**
     * Builds a part with all its measures from the EnigmaXML data.
     *
     * @param partId the MusicXML part ID
     * @param staves the staff cmpers belonging to this part
     * @param measureInfoMap the measure specifications
     * @param entryMap the entry elements by entry number
     * @param frameMap the frames by frame cmper
     * @param gfholdMap the frames of the layers by staff and measure
     * @param staffInfoMap the staff info map
     * @param pickup the length of the missing beginning of a pick-up first measure in EDU, or 0
     * @param notation the tuplets, octave lines, hairpins, note alterations and entry marks
     * @param expressions the expressions of the part
     * @return the constructed part
     */
    private PartImpl buildPart(String partId, List<Integer> staves,
                               Map<Integer, MeasureInfo> measureInfoMap,
                               Map<Integer, Element> entryMap,
                               Map<Integer, FinaleFrame> frameMap,
                               Map<String, Map<Integer, int[]>> gfholdMap,
                               Map<Integer, StaffInfo> staffInfoMap,
                               int pickup,
                               FinaleNotationData notation,
                               List<FinaleExpression> expressions) {
        PartImpl part = new PartImpl();
        part.setId(partId);
        int scale = divisionScale(notation.getTuplets());

        boolean multiStaff = staves.size() > 1;
        StaffInfo partStaff = staffInfoMap.get(staves.get(0));

        List<MeasureImpl> measures = new ArrayList<>();
        Set<String> openTies = new HashSet<>();
        int prevKeyFifths = Integer.MIN_VALUE;
        int prevBeats = -1;
        int prevDivbeat = -1;

        List<Integer> measureNumbers = new ArrayList<>(measureInfoMap.keySet());
        Collections.sort(measureNumbers);

        for (int measNum : measureNumbers) {
            MeasureInfo measInfo = measureInfoMap.get(measNum);
            MeasureImpl measure = new MeasureImpl();
            measure.setNumber(String.valueOf(measNum));
            int skipped = 0;
            if (measNum == measureNumbers.get(0) && pickup > 0) {
                skipped = pickup;
                measure.setNumber("0");
                measure.setImplicit(YES);
            }
            // a pick-up measure is either a full measure whose music starts later, or a measure with a shorter
            // real time signature than the displayed one; staves without music get the displayed length minus the start
            int realLength = measInfo.getBeats() * measInfo.getDivbeat();
            int displayLength = measInfo.getDisplayBeats() * measInfo.getDisplayDivbeat();
            int measureDuration = realLength * scale;
            int restDuration = measureDuration;
            if (skipped > 0) {
                if (realLength >= displayLength) {
                    measureDuration = (realLength - skipped) * scale;
                }
                restDuration = Math.max(0, displayLength - skipped) * scale;
            }

            List<AttributesImpl> attrList = new ArrayList<>();
            boolean needAttributes = (measNum == measureNumbers.get(0))
                    || measInfo.getKeyFifths() != prevKeyFifths
                    || measInfo.getBeats() != prevBeats
                    || measInfo.getDivbeat() != prevDivbeat;

            if (needAttributes) {
                AttributesImpl attr = new AttributesImpl();

                if (measNum == measureNumbers.get(0)) {
                    attr.setDivisions(DIVISIONS * scale);
                    if (multiStaff) {
                        attr.setStaves(staves.size());
                    }

                    attr.setClefs(createClefs(staves, staffInfoMap));
                }

                if (measNum == measureNumbers.get(0) && partStaff != null && partStaff.getTransInterval() != 0) {
                    attr.setTransposes(Collections.singletonList(createTranspose(partStaff)));
                }

                if (measInfo.getKeyFifths() != prevKeyFifths) {
                    KeyImpl key = new KeyImpl();
                    int keyAdjust = 0;
                    if (partStaff != null) {
                        keyAdjust = partStaff.getTransKeyAdjust();
                    }
                    key.setFifths(FinalePitchConverter.getInstance().transposeKey(measInfo.getKeyFifths(), keyAdjust));
                    if (measInfo.isMinor()) {
                        key.setMode("minor");
                    }
                    attr.setKeys(Collections.singletonList(key));
                }

                if (measInfo.getBeats() != prevBeats || measInfo.getDivbeat() != prevDivbeat) {
                    if (measInfo.isDisplayTime()) {
                        attr.setTimes(Collections.singletonList(createTime(measInfo.getDisplayBeats(), measInfo.getDisplayDivbeat())));
                    } else {
                        attr.setTimes(Collections.singletonList(createTime(measInfo.getBeats(), measInfo.getDivbeat())));
                    }
                }

                attrList.add(attr);
                prevKeyFifths = measInfo.getKeyFifths();
                prevBeats = measInfo.getBeats();
                prevDivbeat = measInfo.getDivbeat();
            }

            List<IMusicDataElement> content = new ArrayList<>();
            if (notation.getBreaks().containsKey(measNum)) {
                content.add(createPrint(notation.getBreaks().get(measNum)));
            }
            content.addAll(attrList);
            content.addAll(measureDirections(notation, expressions, staves, measNum, skipped, scale));
            int previousVoiceDuration = -1;
            for (int si = 0; si < staves.size(); si++) {
                int staffCmper = staves.get(si);
                Map<Integer, int[]> staffGfholds = resolveGfholds(staffCmper, gfholdMap);
                Integer staffNumber = null;
                if (multiStaff) {
                    staffNumber = si + 1;
                }

                List<List<NoteImpl>> voices = new ArrayList<>();
                List<Integer> startTimes = new ArrayList<>();
                int[] layerFrames = null;
                if (staffGfholds != null) {
                    layerFrames = staffGfholds.get(measNum);
                }
                for (int layer = 0; layerFrames != null && layer < LAYERS; layer++) {
                    FinaleFrame frame = frameMap.get(layerFrames[layer]);
                    if (layerFrames[layer] <= 0 || frame == null || frame.getStartEntry() <= 0) {
                        continue;
                    }
                    int voice = si * VOICES_PER_STAFF + layer + 1;
                    List<NoteImpl> notes = convertFrameEntries(frame, entryMap, measInfo, staffInfoMap.get(staffCmper), staffNumber, voice, notation, scale);
                    if (!notes.isEmpty()) {
                        applyMeasureRest(notes, measureDuration, scale);
                        applyOctaveShifts(notes, notation.getOctaveShifts(), staffCmper, measNum, frame.getStartTime(), scale);
                        if (isPercussion(staffInfoMap.get(staffCmper))) {
                            toUnpitched(notes);
                        }
                        voices.add(notes);
                        startTimes.add(Math.max(0, frame.getStartTime() - skipped) * scale);
                    }
                }
                if (voices.isEmpty()) {
                    NoteImpl wholeRest = createWholeRest(restDuration, staffNumber);
                    wholeRest.setVoice(si * VOICES_PER_STAFF + 1);
                    voices.add(Collections.singletonList(wholeRest));
                    startTimes.add(0);
                }

                previousVoiceDuration = addVoices(content, voices, startTimes, staffNumber, openTies, previousVoiceDuration);
            }

            content.addAll(convertBarline(measInfo));
            measure.setMusicData(content);
            measures.add(measure);
        }

        part.setMeasures(measures);
        return part;
    }


    /**
     * Creates a whole-measure rest note.
     *
     * @param duration the rest duration in divisions
     * @param staff the staff number, or null for single-staff parts
     * @return the rest note
     */
    private NoteImpl createWholeRest(int duration, Integer staff) {
        NoteImpl note = createWholeRestNote(duration, staff);
        note.getRest().setMeasure(YES);
        return note;
    }


    /**
     * Turns a layer that consists of one whole rest into a measure rest: Finale writes a whole rest
     * for a full measure in any time signature.
     *
     * @param notes the notes of the layer (updated)
     * @param measureDuration the length of the measure in divisions
     * @param scale the divisions per EDU
     */
    private void applyMeasureRest(List<NoteImpl> notes, int measureDuration, int scale) {
        if (notes.size() != 1) {
            return;
        }
        NoteImpl note = notes.get(0);
        if (note.getRest() != null && note.getDuration() != null && note.getDuration() == WHOLE_EDU * scale && note.getTimeModification() == null) {
            note.setDuration(measureDuration);
            note.setDots(null);
            note.getRest().setMeasure(YES);
        }
    }


    /**
     * Creates the print element of a page or system break.
     *
     * @param type page for a page break, else a system break
     * @return the print element
     */
    private PrintImpl createPrint(String type) {
        PrintImpl print = new PrintImpl();
        if ("page".equals(type)) {
            print.setNewPage(YES);
        } else {
            print.setNewSystem(YES);
        }
        return print;
    }


    /**
     * Creates a whole rest note.
     *
     * @param duration the rest duration in divisions
     * @param staff the staff number, or null for single-staff parts
     * @return the rest note
     */
    private NoteImpl createWholeRestNote(int duration, Integer staff) {
        NoteImpl note = new NoteImpl();
        note.setRest(new RestImpl());
        note.setDuration(duration);
        note.setType("whole");
        note.setVoice(1);
        if (staff != null) {
            note.setStaff(staff);
        }
        return note;
    }


    /**
     * Converts the barline and repeat information from a measure specification
     * into MusicXML barline elements.
     *
     * @param measInfo the measure specification data
     * @return list of barline elements (may be empty)
     */
    private List<BarlineImpl> convertBarline(MeasureInfo measInfo) {
        List<BarlineImpl> barlines = new ArrayList<>();
        String endingNumber = null;
        if (measInfo.isEnding()) {
            endingNumber = measInfo.getEndingNumber();
            if (endingNumber == null) {
                endingNumber = "1";
            }
        }

        if (measInfo.isForwardRepeat() || endingNumber != null) {
            BarlineImpl barline = new BarlineImpl();
            barline.setLocation("left");
            if (measInfo.isForwardRepeat()) {
                barline.setBarStyle("heavy-light");
                RepeatImpl repeat = new RepeatImpl();
                repeat.setDirection("forward");
                barline.setRepeat(repeat);
            }
            if (endingNumber != null) {
                barline.setEnding(createEnding(endingNumber, "start"));
            }
            barlines.add(barline);
        }

        String barStyle = null;
        if (measInfo.getBarline() != null) {
            barStyle = mapFinaleBarlineToMusicXml(measInfo.getBarline());
        }
        if (measInfo.isBackwardRepeat()) {
            barStyle = "light-heavy";
        }
        if (barStyle != null || measInfo.isBackwardRepeat() || endingNumber != null) {
            BarlineImpl barline = new BarlineImpl();
            barline.setLocation("right");
            barline.setBarStyle(barStyle);
            if (measInfo.isBackwardRepeat()) {
                RepeatImpl repeat = new RepeatImpl();
                repeat.setDirection("backward");
                barline.setRepeat(repeat);
            }
            if (endingNumber != null) {
                // an ending ends with the backward repeat; the last ending ends without a hook
                if (measInfo.isBackwardRepeat()) {
                    barline.setEnding(createEnding(endingNumber, TIE_STOP));
                } else {
                    barline.setEnding(createEnding(endingNumber, "discontinue"));
                }
            }
            barlines.add(barline);
        }
        return barlines;
    }


    /**
     * Creates an ending (volta).
     *
     * @param number the passes
     * @param type start, stop or discontinue
     * @return the ending
     */
    private EndingImpl createEnding(String number, String type) {
        EndingImpl ending = new EndingImpl();
        ending.setNumber(number);
        ending.setType(type);
        return ending;
    }


    /**
     * Maps a Finale barline type string to a MusicXML bar-style string.
     *
     * @param finaleBarline the Finale barline value
     * @return the MusicXML bar-style, or null if no explicit barline is needed
     */
    private String mapFinaleBarlineToMusicXml(String finaleBarline) {
        if ("double".equals(finaleBarline)) {
            return "light-light";
        } else if ("final".equals(finaleBarline)) {
            return "light-heavy";
        } else if ("dashed".equals(finaleBarline)) {
            return "dashed";
        } else if ("heavy".equals(finaleBarline)) {
            return "heavy";
        } else if ("tick".equals(finaleBarline)) {
            return "tick";
        } else if ("none".equals(finaleBarline)) {
            return "none";
        }
        return null;
    }


    /**
     * Converts the entries of one layer of a frame to MusicXML notes: the linked list of entries is
     * followed from the first to the last entry of the layer.
     *
     * @param frame the frame of the layer
     * @param entryMap the entry elements by entry number
     * @param measInfo the measure (key signature)
     * @param staffInfo the staff (transposition), or null
     * @param staff the staff number, or null for single-staff parts
     * @param voice the voice number
     * @param notation the tuplets, note alterations and entry marks
     * @param scale the divisions per EDU
     * @return list of notes
     */
    private List<NoteImpl> convertFrameEntries(FinaleFrame frame, Map<Integer, Element> entryMap, MeasureInfo measInfo, StaffInfo staffInfo,
                                                Integer staff, int voice, FinaleNotationData notation, int scale) {
        Map<Integer, FinaleTuplet> tuplets = notation.getTuplets();
        Set<String> enharmonics = notation.getNoteAlterations();
        Map<Integer, List<String>> entryMarks = notation.getEntryMarks();
        List<NoteImpl> result = new ArrayList<>();
        int endEntry = frame.getEndEntry();
        int entnum = frame.getStartEntry();
        int safety = 0;
        FinaleTuplet tuplet = null;
        long tupletRemaining = 0;
        while (entnum > 0 && safety < MAX_ENTRIES_PER_FRAME) {
            safety++;
            Element entry = entryMap.get(entnum);
            if (entry == null) {
                break;
            }

            int dura = getIntAttrOrChild(entry, "dura", DIVISIONS);
            boolean isRest = hasChild(entry, "isRest");
            boolean isNote = hasChild(entry, "isNote");
            boolean isGraceNote = hasChild(entry, "graceNote");

            List<Element> noteElements = getChildElements(entry, "note");
            int numNotes = noteElements.size();

            // a tuplet starts at the entry with its definition and lasts for its nominal length
            if (!isGraceNote && hasChild(entry, "tupletStart") && tuplets.containsKey(entnum)) {
                tuplet = tuplets.get(entnum);
                tupletRemaining = tuplet.getSymbolicLength();
            }
            int duration = dura * scale;
            TimeModificationImpl timeModification = null;
            if (tuplet != null && !isGraceNote) {
                duration = (int) ((long) dura * scale * tuplet.getReferenceLength() / tuplet.getSymbolicLength());
                timeModification = createTimeModification(tuplet);
                tupletRemaining -= dura;
                if (tupletRemaining <= 0) {
                    tuplet = null;
                }
            }

            // a rest has no isNote flag; its note element only gives the vertical position of the rest
            if (isRest || !isNote) {
                NoteImpl restNote = new NoteImpl();
                restNote.setRest(new RestImpl());
                restNote.setDuration(duration);
                restNote.setTimeModification(timeModification);
                restNote.setType(duraToType(dura));
                restNote.setVoice(voice);
                if (staff != null) {
                    restNote.setStaff(staff);
                }
                addDots(restNote, dura);
                FinaleMarkConverter.getInstance().applyMarks(restNote, entryMarks.get(entnum), notation);
                result.add(restNote);
            } else {
                for (int ni = 0; ni < numNotes; ni++) {
                    Element noteEl = noteElements.get(ni);
                    NoteImpl note = new NoteImpl();

                    if (ni > 0) {
                        note.setChord("");
                    }

                    if (isGraceNote) {
                        GraceImpl grace = new GraceImpl();
                        grace.setSlash(YES);
                        note.setGrace(grace);
                        note.setDuration(0);
                    }

                    int harmLev = getIntChild(noteEl, "harmLev", 0);
                    int harmAlt = getIntChild(noteEl, "harmAlt", 0);

                    PitchImpl pitch = convertPitch(harmLev, harmAlt, measInfo, staffInfo, enharmonics.contains(entnum + ID_SEPARATOR + noteEl.getAttribute("id")));
                    note.setPitch(pitch);
                    if (!isGraceNote) {
                        note.setDuration(duration);
                        note.setTimeModification(timeModification);
                    }
                    note.setType(duraToType(dura));
                    if (enharmonics.contains(CUE + entnum) || enharmonics.contains(CUE + entnum + ID_SEPARATOR + noteEl.getAttribute("id"))) {
                        note.setTypeSize("cue");
                    }
                    note.setVoice(voice);
                    if (staff != null) {
                        note.setStaff(staff);
                    }
                    if (!isGraceNote) {
                        addDots(note, dura);
                    }

                    boolean hasTieStart = hasChild(noteEl, "tieStart");
                    boolean hasTieEnd = hasChild(noteEl, "tieEnd");
                    if (hasTieStart || hasTieEnd) {
                        List<TieImpl> ties = new ArrayList<>();
                        List<TiedImpl> tieds = new ArrayList<>();

                        if (hasTieStart) {
                            TieImpl tieStart = new TieImpl();
                            tieStart.setType("start");
                            ties.add(tieStart);
                            TiedImpl tiedStart = new TiedImpl();
                            tiedStart.setType("start");
                            tieds.add(tiedStart);
                        }
                        if (hasTieEnd) {
                            TieImpl tieEnd = new TieImpl();
                            tieEnd.setType(TIE_STOP);
                            ties.add(tieEnd);
                            TiedImpl tiedEnd = new TiedImpl();
                            tiedEnd.setType(TIE_STOP);
                            tieds.add(tiedEnd);
                        }

                        note.setTies(ties);
                        NotationsImpl notations = new NotationsImpl();
                        notations.setTieds(tieds);
                        note.setNotations(notations);
                    }

                    if (ni == 0) {
                        FinaleMarkConverter.getInstance().applyMarks(note, entryMarks.get(entnum), notation);
                    }
                    result.add(note);
                }
            }

            if (entnum == endEntry) {
                break;
            }
            int next = 0;
            String nextStr = entry.getAttribute("next");
            if (nextStr != null && !nextStr.isEmpty()) {
                next = Integer.parseInt(nextStr);
            }
            entnum = next;
        }

        return result;
    }


    /**
     * Creates the clefs of the staves of a part (numbered if the part has more than one staff).
     *
     * @param staves the staves of the part
     * @param staffInfoMap the staff specifications
     * @return the clefs
     */
    private List<ClefImpl> createClefs(List<Integer> staves, Map<Integer, StaffInfo> staffInfoMap) {
        List<ClefImpl> clefs = new ArrayList<>();
        for (int si = 0; si < staves.size(); si++) {
            StaffInfo staffInfo = staffInfoMap.get(staves.get(si));
            int clefId = 0;
            if (staffInfo != null) {
                clefId = staffInfo.getDefaultClef();
            }
            ClefImpl clef = FinaleClefConverter.getInstance().getClef(clefId);
            if (staves.size() > 1) {
                clef.setNumber(si + 1);
            }
            clefs.add(clef);
        }
        return clefs;
    }


    /**
     * Parses the octave lines (smart shapes octaveUp, octaveDown, twoOctaveUp and twoOctaveDown).
     *
     * @param root the EnigmaXML root element
     * @return the octave lines
     */
    private List<FinaleOctaveShift> parseOctaveShifts(Element root) {
        List<FinaleOctaveShift> result = new ArrayList<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "smartShape")) {
            String partAttr = el.getAttribute(ATTR_PART);
            if (partAttr != null && !partAttr.isEmpty()) {
                continue;
            }
            int semitones;
            switch (String.valueOf(getTextChild(el, "shapeType"))) {
                case "octaveUp":
                    semitones = OCTAVE;
                    break;
                case "octaveDown":
                    semitones = -OCTAVE;
                    break;
                case "twoOctaveUp":
                    semitones = 2 * OCTAVE;
                    break;
                case "twoOctaveDown":
                    semitones = -2 * OCTAVE;
                    break;
                default:
                    continue;
            }
            Element start = getChildElement(getChildElement(el, "startTermSeg"), END_POINT);
            Element end = getChildElement(getChildElement(el, "endTermSeg"), END_POINT);
            if (start == null || end == null) {
                continue;
            }
            result.add(new FinaleOctaveShift(getIntChild(start, "inst", 0), getIntChild(start, "meas", 0), getIntChild(start, "edu", 0),
                                             getIntChild(end, "meas", 0), getIntChild(end, "edu", Integer.MAX_VALUE), semitones));
        }
        return result;
    }


    /**
     * Applies the octave lines to the notes of a layer: the notes under a line get their sounding pitch.
     *
     * @param notes the notes of the layer (updated)
     * @param octaveShifts the octave lines
     * @param staff the staff
     * @param measure the measure
     * @param startTime the start time of the layer in EDU
     * @param scale the divisions per EDU
     */
    private void applyOctaveShifts(List<NoteImpl> notes, List<FinaleOctaveShift> octaveShifts, int staff, int measure, int startTime, int scale) {
        List<FinaleOctaveShift> lines = new ArrayList<>();
        for (FinaleOctaveShift line : octaveShifts) {
            if (line.getStaff() == staff && line.getStartMeasure() <= measure && line.getEndMeasure() >= measure) {
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            return;
        }

        FinalePitchConverter converter = FinalePitchConverter.getInstance();
        long position = (long) startTime * scale;
        long chordPosition = position;
        for (NoteImpl note : notes) {
            if (note.getChord() == null) {
                chordPosition = position;
                if (note.getDuration() != null) {
                    position += note.getDuration();
                }
            }
            if (note.getPitch() == null) {
                continue;
            }
            for (FinaleOctaveShift line : lines) {
                if (line.contains(measure, chordPosition / scale)) {
                    PitchImpl pitch = note.getPitch();
                    int alter = 0;
                    if (pitch.getAlter() != null) {
                        alter = (int) Math.round(pitch.getAlter());
                    }
                    int octaves = line.getSemitones() / OCTAVE;
                    SpelledPitch shifted = converter.transpose(new SpelledPitch(pitch.getStep(), alter, pitch.getOctave()), octaves * STEPS_PER_OCTAVE, line.getSemitones());
                    pitch.setOctave(shifted.getOctave());
                }
            }
        }
    }


    /**
     * Creates the octave line directions of a measure: the start and the stop of every line of the
     * staves of the part, with their offset from the beginning of the measure.
     *
     * @param octaveShifts the octave lines
     * @param staves the staves of the part
     * @param measure the measure
     * @param skipped the missing beginning of a pick-up measure in EDU
     * @param scale the divisions per EDU
     * @return the directions
     */
    private List<DirectionImpl> octaveShiftDirections(List<FinaleOctaveShift> octaveShifts, List<Integer> staves, int measure, int skipped, int scale) {
        ScoreBuilder builder = ScoreBuilder.getInstance();
        List<DirectionImpl> result = new ArrayList<>();
        for (FinaleOctaveShift line : octaveShifts) {
            int staffIndex = staves.indexOf(line.getStaff());
            if (staffIndex < 0) {
                continue;
            }
            int size = OCTAVE_SIZE;
            if (Math.abs(line.getSemitones()) > OCTAVE) {
                size = TWO_OCTAVES_SIZE;
            }
            // MusicXML shifts the display: an 8va line (sounding higher) is a shift down
            String type = "up";
            if (line.getSemitones() > 0) {
                type = "down";
            }
            if (line.getStartMeasure() == measure) {
                result.add(positioned(builder.createOctaveShift(type, size), line.getStartEdu() - skipped, staffIndex, staves.size(), scale));
            }
            if (line.getEndMeasure() == measure && line.getEndEdu() != Integer.MAX_VALUE) {
                result.add(positioned(builder.createOctaveShift("stop", size), line.getEndEdu() - skipped, staffIndex, staves.size(), scale));
            }
        }
        return result;
    }


    /**
     * Creates the directions of a measure: octave lines, hairpins and expressions.
     *
     * @param notation the notation with the octave lines and the hairpins
     * @param expressions the expressions of the part
     * @param staves the staves of the part
     * @param measure the measure
     * @param skipped the time skipped at the start of the measure (pickup) in EDU
     * @param scale the division scale
     * @return the directions
     */
    private List<IMusicDataElement> measureDirections(FinaleNotationData notation, List<FinaleExpression> expressions, List<Integer> staves,
                                                      int measure, int skipped, int scale) {
        List<IMusicDataElement> result = new ArrayList<>();
        result.addAll(octaveShiftDirections(notation.getOctaveShifts(), staves, measure, skipped, scale));
        result.addAll(hairpinDirections(notation.getHairpins(), staves, measure, skipped, scale));
        for (FinaleExpression expression : expressions) {
            if (expression.getMeasure() != measure) {
                continue;
            }
            int offset = Math.max(0, expression.getEduOffset() - skipped) * scale;
            if (expression.getText() == null && expression.getSoundTempo() > 0) {
                // a tempo step of a gradual tempo change: only played back
                SoundImpl sound = new SoundImpl();
                sound.setTempo(String.valueOf(expression.getSoundTempo()));
                result.add(sound);
                if (offset > 0) {
                    sound.setOffset(offset);
                }
                continue;
            }
            DirectionImpl direction = positioned(expressionDirection(expression), expression.getEduOffset() - skipped,
                                                 Math.max(0, staves.indexOf(expression.getStaff())), staves.size(), scale);
            if (direction.getSound() != null && offset > 0) {
                // the offset of the direction only moves its appearance, the offset of the sound moves the playback
                direction.getSound().setOffset(offset);
            }
            result.add(direction);
        }
        return result;
    }


    /**
     * Adds the voices of a staff to the content of a measure, each after a backup to the start of the measure
     * (except the first voice of the measure) and a forward to its start time.
     *
     * @param content the content of the measure (updated)
     * @param voices the notes of the voices
     * @param startTimes the start times of the voices in divisions
     * @param staffNumber the staff number, or null for a part with one staff
     * @param openTies the open ties (updated)
     * @param previousVoiceDuration the duration of the voice added before, or -1 if none
     * @return the duration of the last voice added
     */
    private int addVoices(List<IMusicDataElement> content, List<List<NoteImpl>> voices, List<Integer> startTimes, Integer staffNumber,
                          Set<String> openTies, int previousVoiceDuration) {
        int previous = previousVoiceDuration;
        for (int v = 0; v < voices.size(); v++) {
            if (previous > 0) {
                BackupImpl backup = new BackupImpl();
                backup.setDuration(previous);
                content.add(backup);
            }
            // a layer that starts later than the beginning of the measure
            int startTime = startTimes.get(v);
            if (startTime > 0) {
                ForwardImpl forward = new ForwardImpl();
                forward.setDuration(startTime);
                forward.setVoice(voices.get(v).get(0).getVoice());
                if (staffNumber != null) {
                    forward.setStaff(staffNumber);
                }
                content.add(forward);
            }
            closeTies(voices.get(v), openTies);
            for (NoteImpl note : voices.get(v)) {
                DirectionImpl numbers = FinaleMarkConverter.getInstance().restNumbers(note, staffNumber);
                if (numbers != null) {
                    content.add(numbers);
                }
                content.add(note);
            }
            previous = startTime + voiceDuration(voices.get(v));
        }
        return previous;
    }


    /**
     * Creates the wedge directions of the hairpins that start or end in a measure.
     *
     * @param hairpins the hairpins
     * @param staves the staves of the part
     * @param measure the measure
     * @param skipped the time skipped at the start of the measure (pickup) in EDU
     * @param scale the division scale
     * @return the directions
     */
    private List<DirectionImpl> hairpinDirections(List<FinaleHairpin> hairpins, List<Integer> staves, int measure, int skipped, int scale) {
        ScoreBuilder builder = ScoreBuilder.getInstance();
        List<DirectionImpl> result = new ArrayList<>();
        for (FinaleHairpin hairpin : hairpins) {
            int staffIndex = staves.indexOf(hairpin.getStaff());
            if (staffIndex < 0) {
                continue;
            }
            if (hairpin.getStartMeasure() == measure) {
                String type = "diminuendo";
                if (hairpin.isCrescendo()) {
                    type = "crescendo";
                }
                DirectionImpl start = builder.createWedge(type, hairpin.getNumber());
                result.add(positioned(start, Math.max(0, hairpin.getStartEdu() - skipped), staffIndex, staves.size(), scale));
            }
            if (hairpin.getEndMeasure() == measure) {
                DirectionImpl stop = builder.createWedge("stop", hairpin.getNumber());
                result.add(positioned(stop, Math.max(0, hairpin.getEndEdu() - skipped), staffIndex, staves.size(), scale));
            }
        }
        return result;
    }


    /**
     * Creates the direction of an expression: a tempo mark with its metronome value, or a dynamic.
     *
     * @param expression the expression
     * @return the direction
     */
    private DirectionImpl expressionDirection(FinaleExpression expression) {
        ScoreBuilder builder = ScoreBuilder.getInstance();
        if (expression.isRehearsal()) {
            return builder.createRehearsal(expression.getText());
        }
        if (expression.getBeatsPerMinute() <= 0 && expression.getDynamic() == null) {
            DirectionImpl words = builder.createWords(expression.getText(), "above", null, null);
            if (expression.getSoundTempo() > 0) {
                SoundImpl sound = new SoundImpl();
                sound.setTempo(String.valueOf(expression.getSoundTempo()));
                words.setSound(sound);
            }
            return words;
        }
        if (expression.getBeatsPerMinute() <= 0) {
            return builder.createDynamics(expression.getDynamic());
        }

        // the tempo text is the text before the metronome mark (a note glyph, an equals sign and the value)
        String text = expression.getText();
        if (text != null && text.contains("=")) {
            text = text.substring(0, text.indexOf('=')).trim();
            int space = text.lastIndexOf(' ');
            if (space >= 0) {
                text = text.substring(0, space).trim();
            } else {
                text = null;
            }
        }

        int unit = expression.getBeatUnitEdu();
        int dots = 0;
        if (unit % COMPOUND_BEAT_UNITS == 0 && Integer.bitCount(unit / COMPOUND_BEAT_UNITS) == 1) {
            unit = unit / COMPOUND_BEAT_UNITS * 2;
            dots = 1;
        }
        return builder.createTempo(expression.getBeatsPerMinute(), duraToType(unit), dots, text);
    }


    /**
     * Sets the offset and the staff of a direction.
     *
     * @param direction the direction
     * @param edu the position from the beginning of the measure in EDU
     * @param staffIndex the index of the staff in the part
     * @param staffCount the number of staves of the part
     * @param scale the divisions per EDU
     * @return the direction
     */
    private DirectionImpl positioned(DirectionImpl direction, int edu, int staffIndex, int staffCount, int scale) {
        if (edu > 0) {
            direction.setOffset(edu * scale);
        }
        if (staffCount > 1) {
            direction.setStaff(staffIndex + 1);
        }
        return direction;
    }


    /**
     * Gets the length of the missing beginning of a pick-up first measure: the start time of the frame of the
     * special staff 32767 in the first measure, or else the earliest start time of the
     * frames with entries in the first measure, if all of them start later than the beginning of the measure.
     *
     * @param staves the staves of the parts
     * @param groups the multi-staff groups
     * @param measureInfoMap the measure specifications
     * @param frameMap the frames by frame cmper
     * @param gfholdMap the frames of the layers by staff and measure
     * @return the length of the missing beginning in EDU, or 0 if the first measure is complete
     */
    private int pickupTime(List<Integer> staves, Map<Integer, GroupInfo> groups, Map<Integer, MeasureInfo> measureInfoMap,
                           Map<Integer, FinaleFrame> frameMap, Map<String, Map<Integer, int[]>> gfholdMap) {
        if (measureInfoMap.isEmpty()) {
            return 0;
        }
        int first = Collections.min(measureInfoMap.keySet());

        // the special staff 32767 defines the pick-up of the score with a frame that starts later
        Map<Integer, int[]> specialGfholds = resolveGfholds(SPECIAL_STAFF, gfholdMap);
        if (specialGfholds != null && specialGfholds.get(first) != null) {
            for (int frameNumber : specialGfholds.get(first)) {
                FinaleFrame frame = frameMap.get(frameNumber);
                if (frameNumber > 0 && frame != null && frame.getStartTime() > 0) {
                    return frame.getStartTime();
                }
            }
        }

        List<Integer> allStaves = new ArrayList<>(staves);
        for (GroupInfo group : groups.values()) {
            if (staves.contains(group.getStaffNum1())) {
                allStaves.add(group.getStaffNum2());
            }
        }

        int result = Integer.MAX_VALUE;
        for (int staff : allStaves) {
            Map<Integer, int[]> staffGfholds = resolveGfholds(staff, gfholdMap);
            if (staffGfholds == null || staffGfholds.get(first) == null) {
                continue;
            }
            for (int frameNumber : staffGfholds.get(first)) {
                FinaleFrame frame = frameMap.get(frameNumber);
                if (frameNumber > 0 && frame != null && frame.getStartEntry() > 0) {
                    result = Math.min(result, frame.getStartTime());
                }
            }
        }
        if (result == Integer.MAX_VALUE) {
            return 0;
        }
        return result;
    }


    /**
     * Parses the note alterations: the enharmonically respelled notes (noteAlter with the enharmonic flag) as
     * entry number and note ID, and the reduced notes (noteAlter or entrySize below 100 percent) with the
     * prefix cue: as entry number and note ID, or entry number only for a whole entry.
     *
     * @param root the EnigmaXML root element
     * @return the altered notes
     */
    private Set<String> parseEnharmonics(Element root) {
        Set<String> result = new HashSet<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "noteAlter")) {
            String partAttr = el.getAttribute(ATTR_PART);
            if ((partAttr == null || partAttr.isEmpty()) && hasChild(el, "enharmonic")) {
                result.add(el.getAttribute("entnum") + ID_SEPARATOR + getIntChild(el, "noteID", 0));
            }
            if ((partAttr == null || partAttr.isEmpty()) && isReduced(el)) {
                result.add(CUE + el.getAttribute("entnum") + ID_SEPARATOR + getIntChild(el, "noteID", 0));
            }
        }

        // a reduced entry (all notes of a chord)
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "entrySize")) {
            String partAttr = el.getAttribute(ATTR_PART);
            if ((partAttr == null || partAttr.isEmpty()) && isReduced(el)) {
                result.add(CUE + el.getAttribute("entnum"));
            }
        }
        return result;
    }


    /**
     * Tells whether a size record (noteAlter or entrySize) reduces the size below the normal size.
     *
     * @param el the record
     * @return true for a reduced size
     */
    private boolean isReduced(Element el) {
        int percent = getIntChild(el, "percent", FULL_SIZE);
        return percent > 0 && percent < FULL_SIZE;
    }


    /**
     * Parses the tuplet definitions (tupletDef) by the entry that starts the tuplet.
     *
     * @param root the EnigmaXML root element
     * @return the tuplet definitions by entry number
     */
    private Map<Integer, FinaleTuplet> parseTupletDefs(Element root) {
        Map<Integer, FinaleTuplet> result = new LinkedHashMap<>();
        for (Element el : FinaleElementIndex.getInstance().getElements(root, "tupletDef")) {
            String partAttr = el.getAttribute(ATTR_PART);
            if (partAttr != null && !partAttr.isEmpty()) {
                continue;
            }
            FinaleTuplet tuplet = new FinaleTuplet(getIntChild(el, "symbolicNum", 0), getIntChild(el, "symbolicDur", 0),
                                                   getIntChild(el, "refNum", 0), getIntChild(el, "refDur", 0));
            if (tuplet.isValid()) {
                result.putIfAbsent(Integer.parseInt(el.getAttribute("entnum")), tuplet);
            }
        }
        return result;
    }


    /**
     * Calculates the divisions per EDU that give every tuplet note a whole number of divisions: the least
     * common multiple of the reduced tuplet denominators (limited, larger values are rounded).
     *
     * @param tuplets the tuplet definitions
     * @return the divisions per EDU
     */
    private int divisionScale(Map<Integer, FinaleTuplet> tuplets) {
        long scale = 1;
        for (FinaleTuplet tuplet : tuplets.values()) {
            long symbolic = tuplet.getSymbolicLength();
            long denominator = symbolic / gcd(symbolic, tuplet.getReferenceLength());
            long next = scale / gcd(scale, denominator) * denominator;
            if (next <= MAX_DIVISION_SCALE) {
                scale = next;
            }
        }
        return (int) scale;
    }


    /**
     * Calculates the greatest common divisor.
     *
     * @param a the first value
     * @param b the second value
     * @return the greatest common divisor
     */
    private long gcd(long a, long b) {
        long x = Math.abs(a);
        long y = Math.abs(b);
        while (y != 0) {
            long t = x % y;
            x = y;
            y = t;
        }
        if (x == 0) {
            return 1;
        }
        return x;
    }


    /**
     * Creates the time modification of a tuplet note: the number of notes and the number of notes of
     * the same type in the time of the tuplet.
     *
     * @param tuplet the tuplet
     * @return the time modification, or null if the reference time is not a whole number of notes
     */
    private TimeModificationImpl createTimeModification(FinaleTuplet tuplet) {
        if (tuplet.getReferenceLength() % tuplet.getSymbolicDuration() != 0) {
            return null;
        }
        TimeModificationImpl result = new TimeModificationImpl();
        result.setActualNotes(tuplet.getSymbolicNumber());
        result.setNormalNotes((int) (tuplet.getReferenceLength() / tuplet.getSymbolicDuration()));
        return result;
    }


    /**
     * Checks if a staff is a percussion staff (percussion clef).
     *
     * @param staffInfo the staff, or null
     * @return true for a percussion staff
     */
    private boolean isPercussion(StaffInfo staffInfo) {
        return staffInfo != null && (staffInfo.getDefaultClef() == CLEF_PERCUSSION || staffInfo.getDefaultClef() == CLEF_PERCUSSION_2);
    }


    /**
     * Turns the pitched notes of a percussion staff into unpitched notes at the same staff position.
     *
     * @param notes the notes (updated)
     */
    private void toUnpitched(List<NoteImpl> notes) {
        for (NoteImpl note : notes) {
            PitchImpl pitch = note.getPitch();
            if (pitch == null) {
                continue;
            }
            UnpitchedImpl unpitched = new UnpitchedImpl();
            unpitched.setDisplayStep(pitch.getStep());
            unpitched.setDisplayOctave(pitch.getOctave());
            note.setPitch(null);
            note.setUnpitched(unpitched);
        }
    }


    /**
     * Keeps a tie stop only if a tie to the same pitch is open in the same staff and voice: Finale can
     * end a tie in another layer than it starts, MusicXML ties connect notes of one voice.
     *
     * @param notes the notes of a voice in time order (updated)
     * @param openTies the open ties of the part by staff, voice and pitch (updated)
     */
    private void closeTies(List<NoteImpl> notes, Set<String> openTies) {
        for (NoteImpl note : notes) {
            if (note.getPitch() == null || note.getTies() == null) {
                continue;
            }
            int alter = 0;
            if (note.getPitch().getAlter() != null) {
                alter = (int) Math.round(note.getPitch().getAlter());
            }
            String key = note.getStaff() + ID_SEPARATOR + note.getVoice() + ID_SEPARATOR
                    + PitchSpeller.getInstance().toMidi(note.getPitch().getStep(), alter, note.getPitch().getOctave());
            boolean stop = false;
            boolean start = false;
            for (TieImpl tie : note.getTies()) {
                stop |= TIE_STOP.equals(tie.getType());
                start |= TIE_START.equals(tie.getType());
            }
            if (stop && !openTies.remove(key)) {
                removeTieStop(note);
            }
            if (start) {
                openTies.add(key);
            }
        }
    }


    /**
     * Removes the tie stop of a note.
     *
     * @param note the note (updated)
     */
    private void removeTieStop(NoteImpl note) {
        List<TieImpl> ties = new ArrayList<>();
        for (TieImpl tie : note.getTies()) {
            if (!TIE_STOP.equals(tie.getType())) {
                ties.add(tie);
            }
        }
        if (ties.isEmpty()) {
            note.setTies(null);
        } else {
            note.setTies(ties);
        }

        if (note.getNotations() != null && note.getNotations().getTieds() != null) {
            List<TiedImpl> tieds = new ArrayList<>();
            for (TiedImpl tied : note.getNotations().getTieds()) {
                if (!TIE_STOP.equals(tied.getType())) {
                    tieds.add(tied);
                }
            }
            if (tieds.isEmpty()) {
                note.getNotations().setTieds(null);
            } else {
                note.getNotations().setTieds(tieds);
            }
        }
    }


    /**
     * Gets the time a voice takes: the duration of all notes that are not chord notes.
     *
     * @param notes the notes of the voice
     * @return the duration in divisions
     */
    private int voiceDuration(List<NoteImpl> notes) {
        int result = 0;
        for (NoteImpl note : notes) {
            if (note.getChord() == null && note.getDuration() != null) {
                result += note.getDuration();
            }
        }
        return result;
    }


    /**
     * Creates the MusicXML transposition of a transposing staff: the interval from the written to the sounding pitch.
     *
     * @param staffInfo the staff
     * @return the transposition
     */
    private TransposeImpl createTranspose(StaffInfo staffInfo) {
        int diatonic = staffInfo.getTransInterval();
        int chromatic = FinalePitchConverter.getInstance().chromaticInterval(diatonic, staffInfo.getTransKeyAdjust());
        int octaves = Math.floorDiv(diatonic, STEPS_PER_OCTAVE);
        TransposeImpl transpose = new TransposeImpl();
        transpose.setDiatonic(-(diatonic - octaves * STEPS_PER_OCTAVE));
        transpose.setChromatic(-(chromatic - octaves * SEMITONES_PER_OCTAVE));
        if (octaves != 0) {
            transpose.setOctaveChange(-octaves);
        }
        return transpose;
    }


    /**
     * Creates a time signature from the Finale beats and beat duration: a dotted beat (for example
     * two beats of 1536 EDU) is a compound meter (6/8).
     *
     * @param beats the number of beats
     * @param divbeat the beat duration in EDU
     * @return the time signature
     */
    private TimeImpl createTime(int beats, int divbeat) {
        int count = beats;
        int unit = divbeat;
        if (divbeat % COMPOUND_BEAT_UNITS == 0 && Integer.bitCount(divbeat / COMPOUND_BEAT_UNITS) == 1 && divbeat / COMPOUND_BEAT_UNITS * 2 <= WHOLE_EDU) {
            count = beats * COMPOUND_BEAT_UNITS;
            unit = divbeat / COMPOUND_BEAT_UNITS * 2;
        }
        TimeImpl time = new TimeImpl();
        time.setBeats(String.valueOf(count));
        time.setBeatType(String.valueOf(eduToBeatType(unit)));
        return time;
    }


    /**
     * Converts Finale harmLev/harmAlt to a MusicXML PitchImpl: the concert pitch relative to the tonic
     * of the key, transposed to the written pitch of a transposing staff.
     *
     * @param harmLev the diatonic displacement from the tonic in the octave of middle C
     * @param harmAlt the chromatic alteration relative to the key signature
     * @param measInfo the measure (key signature)
     * @param staffInfo the staff (transposition), or null
     * @param enharmonic true if the note is respelled enharmonically
     * @return the pitch
     */
    private PitchImpl convertPitch(int harmLev, int harmAlt, MeasureInfo measInfo, StaffInfo staffInfo, boolean enharmonic) {
        FinalePitchConverter converter = FinalePitchConverter.getInstance();
        SpelledPitch spelled = converter.concertPitch(harmLev, harmAlt, measInfo.getKeyFifths(), measInfo.isMinor());
        if (staffInfo != null && staffInfo.getTransInterval() != 0) {
            int diatonic = staffInfo.getTransInterval();
            spelled = converter.transpose(spelled, diatonic, converter.chromaticInterval(diatonic, staffInfo.getTransKeyAdjust()));
        }
        if (enharmonic) {
            spelled = converter.respell(spelled);
        }

        PitchImpl pitch = new PitchImpl();
        pitch.setStep(spelled.getStep());
        pitch.setOctave(spelled.getOctave());
        if (spelled.getAlter() != 0) {
            pitch.setAlter((double) spelled.getAlter());
        }
        return pitch;
    }


    /**
     * Converts a Finale EDU divbeat value to a MusicXML beat-type denominator.
     * The divbeat is the duration of one beat in EDU.
     *
     * @param divbeat the beat duration in EDU
     * @return the MusicXML beat-type value (1, 2, 4, 8, 16, etc.)
     */
    private int eduToBeatType(int divbeat) {
        if (divbeat >= 4096) {
            return 1;
        } else if (divbeat >= 2048) {
            return 2;
        } else if (divbeat >= 1024) {
            return 4;
        } else if (divbeat >= 512) {
            return 8;
        } else if (divbeat >= 256) {
            return 16;
        } else if (divbeat >= 128) {
            return 32;
        }
        return 4;
    }


    /**
     * Converts a Finale EDU duration to a MusicXML note type string.
     *
     * @param dura the duration in EDU (1024 = quarter note)
     * @return the MusicXML type string
     */
    private String duraToType(int dura) {
        int baseDura = stripDots(dura);

        if (baseDura >= 8192) {
            return "breve";
        } else if (baseDura >= 4096) {
            return "whole";
        } else if (baseDura >= 2048) {
            return "half";
        } else if (baseDura >= 1024) {
            return "quarter";
        } else if (baseDura >= 512) {
            return "eighth";
        } else if (baseDura >= 256) {
            return "16th";
        } else if (baseDura >= 128) {
            return "32nd";
        } else if (baseDura >= 64) {
            return "64th";
        } else {
            return "128th";
        }
    }


    /**
     * Strips dot extensions from a duration to find the base duration.
     *
     * @param dura the duration in EDU
     * @return the base (un-dotted) duration
     */
    private int stripDots(int dura) {
        int[] bases = {8192, 4096, 2048, 1024, 512, 256, 128, 64, 32};
        for (int base : bases) {
            if (dura == base) {
                return base;
            }
            int dotted = base + base / 2;
            if (dura == dotted) {
                return base;
            }
            int doubleDotted = base + base / 2 + base / 4;
            if (dura == doubleDotted) {
                return base;
            }
        }
        return dura;
    }


    /**
     * Adds dot elements to a note if the duration is dotted.
     *
     * @param note the note to modify
     * @param dura the duration in EDU
     */
    private void addDots(NoteImpl note, int dura) {
        int[] bases = {8192, 4096, 2048, 1024, 512, 256, 128, 64, 32};
        for (int base : bases) {
            int dotted = base + base / 2;
            if (dura == dotted) {
                List<String> dots = new ArrayList<>();
                dots.add("");
                note.setDots(dots);
                return;
            }
            int doubleDotted = base + base / 2 + base / 4;
            if (dura == doubleDotted) {
                List<String> dots = new ArrayList<>();
                dots.add("");
                dots.add("");
                note.setDots(dots);
                return;
            }
        }
    }


    /**
     * Gets the text content of a named child element.
     *
     * @param parent the parent element
     * @param childName the child element name
     * @return the text content, or null if not found
     */
    private String getTextChild(Element parent, String childName) {
        Element child = getChildElement(parent, childName);
        if (child == null) {
            return null;
        }
        return child.getTextContent();
    }


    /**
     * Gets the integer value of a named child element.
     *
     * @param parent the parent element
     * @param childName the child element name
     * @param defaultValue the default value if not found
     * @return the integer value
     */
    private int getIntChild(Element parent, String childName, int defaultValue) {
        String text = getTextChild(parent, childName);
        if (text != null && !text.isEmpty()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return defaultValue;
    }


    /**
     * Gets the integer value from either an attribute or child element.
     *
     * @param el the element
     * @param name the attribute/child name
     * @param defaultValue the default value
     * @return the integer value
     */
    private int getIntAttrOrChild(Element el, String name, int defaultValue) {
        int val = getIntChild(el, name, Integer.MIN_VALUE);
        if (val != Integer.MIN_VALUE) {
            return val;
        }
        return defaultValue;
    }


    /**
     * Gets the first direct child element with the given local name (in the Finale namespace).
     *
     * @param parent the parent element
     * @param childName the child element name
     * @return the child element, or null
     */
    private Element getChildElement(Element parent, String childName) {
        if (parent == null) {
            return null;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && childName.equals(child.getLocalName()) && NS.equals(child.getNamespaceURI())) {
                return (Element) child;
            }
        }
        return null;
    }


    /**
     * Gets the direct child elements with the given local name.
     *
     * @param el the element
     * @param childName the child element name
     * @return the child elements in document order
     */
    private List<Element> getChildElements(Element el, String childName) {
        List<Element> result = new ArrayList<>();
        for (Node child = el.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && childName.equals(child.getLocalName())) {
                result.add((Element) child);
            }
        }
        return result;
    }


    /**
     * Checks if the element has a child element with the given name.
     *
     * @param el the element
     * @param childName the child element name
     * @return true if the child exists
     */
    private boolean hasChild(Element el, String childName) {
        return getChildElement(el, childName) != null;
    }


    /**
     * Reads a bounded amount of data from an input stream.
     *
     * @param inputStream the input stream
     * @return the data as a byte array
     * @throws IOException if reading fails or size limit is exceeded
     */
    private byte[] readBounded(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            total += len;
            if (total > MAX_ENTRY_SIZE) {
                throw new IOException("ZIP entry exceeds maximum size.");
            }
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }


    /**
     * Reads all bytes from an input stream with a size limit.
     *
     * @param inputStream the input stream
     * @return all bytes from the stream
     * @throws IOException if reading fails or the size limit is exceeded
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            total += len;
            if (total > MAX_TOTAL_SIZE) {
                throw new IOException("Input exceeds maximum allowed size.");
            }
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }


    /**
     * Holds parsed staff specification data.
     *
     * @author patrick
     */
    private static final class StaffInfo {
        private int cmper;
        private int defaultClef;
        private int fullNameRef;
        private int transInterval;
        private int transKeyAdjust;


        /**
         * Gets the component number.
         *
         * @return the component number
         */
        int getCmper() {
            return cmper;
        }


        /**
         * Sets the component number.
         *
         * @param cmper the component number
         */
        void setCmper(int cmper) {
            this.cmper = cmper;
        }


        /**
         * Gets the default clef identifier.
         *
         * @return the default clef identifier
         */
        int getDefaultClef() {
            return defaultClef;
        }


        /**
         * Sets the default clef identifier.
         *
         * @param defaultClef the default clef identifier
         */
        void setDefaultClef(int defaultClef) {
            this.defaultClef = defaultClef;
        }


        /**
         * Gets the full name reference.
         *
         * @return the full name reference
         */
        int getFullNameRef() {
            return fullNameRef;
        }


        /**
         * Sets the full name reference.
         *
         * @param fullNameRef the full name reference
         */
        void setFullNameRef(int fullNameRef) {
            this.fullNameRef = fullNameRef;
        }


        /**
         * Gets the transposition interval.
         *
         * @return the transposition interval
         */
        int getTransInterval() {
            return transInterval;
        }


        /**
         * Sets the transposition interval.
         *
         * @param transInterval the transposition interval
         */
        void setTransInterval(int transInterval) {
            this.transInterval = transInterval;
        }


        /**
         * Gets the key adjustment of the transposition in fifths.
         *
         * @return the key adjustment
         */
        int getTransKeyAdjust() {
            return transKeyAdjust;
        }


        /**
         * Sets the key adjustment of the transposition in fifths.
         *
         * @param transKeyAdjust the key adjustment
         */
        void setTransKeyAdjust(int transKeyAdjust) {
            this.transKeyAdjust = transKeyAdjust;
        }
    }


    /**
     * Holds parsed measure specification data.
     *
     * @author patrick
     */
    private static final class MeasureInfo {
        private int number;
        private int keyFifths;
        private boolean minor;
        private int beats;
        private int divbeat;
        private int displayBeats;
        private int displayDivbeat;
        private boolean displayTime;
        private String barline;
        private int width;
        private boolean forwardRepeat;
        private boolean backwardRepeat;
        private boolean ending;
        private String endingNumber;


        /**
         * Gets the measure number.
         *
         * @return the measure number
         */
        int getNumber() {
            return number;
        }


        /**
         * Sets the measure number.
         *
         * @param number the measure number
         */
        void setNumber(int number) {
            this.number = number;
        }


        /**
         * Gets the key signature as number of fifths.
         *
         * @return the number of fifths
         */
        int getKeyFifths() {
            return keyFifths;
        }


        /**
         * Sets the key signature as number of fifths.
         *
         * @param keyFifths the number of fifths
         */
        void setKeyFifths(int keyFifths) {
            this.keyFifths = keyFifths;
        }


        /**
         * Checks if the key is a minor key.
         *
         * @return true for a minor key
         */
        boolean isMinor() {
            return minor;
        }


        /**
         * Sets if the key is a minor key.
         *
         * @param minor true for a minor key
         */
        void setMinor(boolean minor) {
            this.minor = minor;
        }


        /**
         * Gets the number of beats per measure.
         *
         * @return the number of beats
         */
        int getBeats() {
            return beats;
        }


        /**
         * Sets the number of beats per measure.
         *
         * @param beats the number of beats
         */
        void setBeats(int beats) {
            this.beats = beats;
        }


        /**
         * Gets the beat duration in EDU.
         *
         * @return the beat duration
         */
        int getDivbeat() {
            return divbeat;
        }


        /**
         * Sets the beat duration in EDU.
         *
         * @param divbeat the beat duration
         */
        void setDivbeat(int divbeat) {
            this.divbeat = divbeat;
        }


        /**
         * Gets the number of beats of the displayed time signature.
         *
         * @return the number of beats
         */
        int getDisplayBeats() {
            return displayBeats;
        }


        /**
         * Sets the number of beats of the displayed time signature.
         *
         * @param displayBeats the number of beats
         */
        void setDisplayBeats(int displayBeats) {
            this.displayBeats = displayBeats;
        }


        /**
         * Gets the beat duration of the displayed time signature in EDU.
         *
         * @return the beat duration
         */
        int getDisplayDivbeat() {
            return displayDivbeat;
        }


        /**
         * Sets the beat duration of the displayed time signature in EDU.
         *
         * @param displayDivbeat the beat duration
         */
        void setDisplayDivbeat(int displayDivbeat) {
            this.displayDivbeat = displayDivbeat;
        }


        /**
         * Checks if the displayed time signature is shown instead of the real one.
         *
         * @return true to show the displayed time signature
         */
        boolean isDisplayTime() {
            return displayTime;
        }


        /**
         * Sets if the displayed time signature is shown instead of the real one.
         *
         * @param displayTime true to show the displayed time signature
         */
        void setDisplayTime(boolean displayTime) {
            this.displayTime = displayTime;
        }


        /**
         * Gets the barline type.
         *
         * @return the barline type
         */
        String getBarline() {
            return barline;
        }


        /**
         * Sets the barline type.
         *
         * @param barline the barline type
         */
        void setBarline(String barline) {
            this.barline = barline;
        }


        /**
         * Gets the measure width.
         *
         * @return the measure width
         */
        int getWidth() {
            return width;
        }


        /**
         * Sets the measure width.
         *
         * @param width the measure width
         */
        void setWidth(int width) {
            this.width = width;
        }


        /**
         * Checks if this measure has a forward repeat.
         *
         * @return true if there is a forward repeat
         */
        boolean isForwardRepeat() {
            return forwardRepeat;
        }


        /**
         * Sets whether this measure has a forward repeat.
         *
         * @param forwardRepeat true if there is a forward repeat
         */
        void setForwardRepeat(boolean forwardRepeat) {
            this.forwardRepeat = forwardRepeat;
        }


        /**
         * Checks if this measure has a backward repeat.
         *
         * @return true if there is a backward repeat
         */
        boolean isBackwardRepeat() {
            return backwardRepeat;
        }


        /**
         * Sets whether this measure has a backward repeat.
         *
         * @param backwardRepeat true if there is a backward repeat
         */
        void setBackwardRepeat(boolean backwardRepeat) {
            this.backwardRepeat = backwardRepeat;
        }


        /**
         * Checks if the measure is an ending (volta).
         *
         * @return true for an ending
         */
        boolean isEnding() {
            return ending;
        }


        /**
         * Sets if the measure is an ending (volta).
         *
         * @param ending true for an ending
         */
        void setEnding(boolean ending) {
            this.ending = ending;
        }


        /**
         * Gets the passes of the ending, for example 1 or 1, 2.
         *
         * @return the passes, or null
         */
        String getEndingNumber() {
            return endingNumber;
        }


        /**
         * Sets the passes of the ending.
         *
         * @param endingNumber the passes
         */
        void setEndingNumber(String endingNumber) {
            this.endingNumber = endingNumber;
        }
    }


    /**
     * Holds parsed part definition data.
     *
     * @author patrick
     */
    private static final class PartDefInfo {
        private int cmper;
        private int partOrder;
        private int defaultNameStaff;
        private int defaultNameGroup;


        /**
         * Gets the component number.
         *
         * @return the component number
         */
        int getCmper() {
            return cmper;
        }


        /**
         * Sets the component number.
         *
         * @param cmper the component number
         */
        void setCmper(int cmper) {
            this.cmper = cmper;
        }


        /**
         * Gets the part order.
         *
         * @return the part order
         */
        int getPartOrder() {
            return partOrder;
        }


        /**
         * Sets the part order.
         *
         * @param partOrder the part order
         */
        void setPartOrder(int partOrder) {
            this.partOrder = partOrder;
        }


        /**
         * Gets the default name staff reference.
         *
         * @return the default name staff reference
         */
        int getDefaultNameStaff() {
            return defaultNameStaff;
        }


        /**
         * Sets the default name staff reference.
         *
         * @param defaultNameStaff the default name staff reference
         */
        void setDefaultNameStaff(int defaultNameStaff) {
            this.defaultNameStaff = defaultNameStaff;
        }


        /**
         * Gets the default name group reference.
         *
         * @return the default name group reference
         */
        int getDefaultNameGroup() {
            return defaultNameGroup;
        }


        /**
         * Sets the default name group reference.
         *
         * @param defaultNameGroup the default name group reference
         */
        void setDefaultNameGroup(int defaultNameGroup) {
            this.defaultNameGroup = defaultNameGroup;
        }
    }


    /**
     * Holds parsed multi-staff group data.
     *
     * @author patrick
     */
    private static final class GroupInfo {
        private int staffNum1;
        private int staffNum2;


        /**
         * Gets the first staff number.
         *
         * @return the first staff number
         */
        int getStaffNum1() {
            return staffNum1;
        }


        /**
         * Sets the first staff number.
         *
         * @param staffNum1 the first staff number
         */
        void setStaffNum1(int staffNum1) {
            this.staffNum1 = staffNum1;
        }


        /**
         * Gets the second staff number.
         *
         * @return the second staff number
         */
        int getStaffNum2() {
            return staffNum2;
        }


        /**
         * Sets the second staff number.
         *
         * @param staffNum2 the second staff number
         */
        void setStaffNum2(int staffNum2) {
            this.staffNum2 = staffNum2;
        }
    }
}
