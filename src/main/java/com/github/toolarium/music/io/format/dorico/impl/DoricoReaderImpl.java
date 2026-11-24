/*
 * DoricoReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.converter.InstrumentSoundConverter;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.BarInfo;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.BarlinePlan;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.ChordInfo;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.ClefInfo;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.EventRef;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.FlowContext;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.GraceGroup;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.KeyInfo;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.NoteMarks;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.NoteSpec;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.PartLayout;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.PartSource;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.TimeInfo;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.TimedPiece;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.TupletInfo;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.VoiceLayout;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderTypes.VoiceSource;
import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.barline.impl.EndingImpl;
import com.github.toolarium.music.model.barline.impl.RepeatImpl;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.DirectionTypeImpl;
import com.github.toolarium.music.model.direction.impl.MetronomeImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.direction.impl.WedgeImpl;
import com.github.toolarium.music.model.direction.impl.WordsImpl;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.BackupImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.measure.impl.TransposeImpl;
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
import com.github.toolarium.music.model.note.impl.RestImpl;
import com.github.toolarium.music.model.note.impl.TieImpl;
import com.github.toolarium.music.model.note.impl.TimeModificationImpl;
import com.github.toolarium.music.model.part.impl.MidiInstrumentImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.part.impl.ScoreInstrumentImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Reads Dorico project files (.dorico) and converts them to an {@link IScore} model.
 *
 * <p>The pipeline is: ZIP container, root file from META-INF/container.xml (score.dtn),
 * tree decoding with {@link DoricoTreeDecoder}, then mapping of the tree to the score model.</p>
 *
 * <p>Scope: all flows (appended as consecutive bars), project metadata, players and instruments as
 * parts, staves, clefs and clef changes, instrument transposition, key and time signatures, the bar
 * grid, notes with spelling, chords, voices, rests, ties (also across barlines), slurs, tuplets,
 * grace notes, articulations, ornaments, fermatas, lyrics, dynamics, hairpins, staff text, tempo
 * marks, repeat barlines, repeat endings and repeat markers. Pitches are converted from concert
 * pitch to written pitch for transposing instruments.</p>
 *
 * @author patrick
 */
public class DoricoReaderImpl implements IMusicReader {
    static final String LOCATION_RIGHT = "right";
    static final int MAX_SLUR_NUMBER = 6;
    private static final Logger LOG = LoggerFactory.getLogger(DoricoReaderImpl.class);
    private static final int BUFFER_SIZE = 8192;
    private static final long MAX_ENTRY_SIZE = 64L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 200L * 1024 * 1024;
    private static final int MAX_ENTRY_COUNT = 2000;
    private static final int MAX_MEASURES = 100000;
    private static final long MAX_DIVISIONS = 1L << 20;
    private static final String CONTAINER_ENTRY = "META-INF/container.xml";
    private static final String DEFAULT_ROOT_FILE = "score.dtn";
    private static final String DEFAULT_ROOT_FILE_XML = "score.xml";
    private static final String MACOS_PREFIX = "__MACOSX/";
    private static final String ROOT_NODE = "kScore";
    private static final String STREAM_GLOBAL = "kGlobalTimebaseStream";
    private static final String EVENTS = "events";
    private static final String POSITION = "position";
    private static final String REHEARSAL_MARK = "RehearsalMarkEventDefinition";
    private static final int DRUM_CHANNEL = 10;
    private static final int MAX_CHANNEL = 16;
    private static final String GRADUAL_TEMPO = "GradualTempoChangeEventDefinition";
    private static final int LETTERS = 26;
    private static final String DURATION = "duration";
    private static final String ELEMENT_ID = "elementID";
    private static final String PLAYER_ID = "playerID";
    private static final String SECONDARY_POSITION = "secondaryPosition";
    private static final String BAR_STYLE_FINAL = "light-heavy";
    private static final String PLACEMENT_ABOVE = "above";
    private static final String PLACEMENT_BELOW = "below";
    private static final String FONT_ITALIC = "italic";
    private static final int TUPLET_GRID = 64;
    private static final String CLEF_TREBLE = "clef.treble";
    private static final String NOTE_C = "C";
    private static final String NOTE_F = "F";
    private static final String NOTE_G = "G";
    private static final Fraction QUARTERS_PER_WHOLE = Fraction.of(4, 1);
    private static final int VOICES_PER_STAFF = 4;
    private static final int COMPOUND_BEATS_PER_GROUP = 3;
    private static final int[] COMPOUND_BEAT_GROUPS = {4, 2, 1};
    private static final int COMPOUND_MIN_BEAT_TYPE = 8;
    private static final int MAX_MIDI_PITCH = 127;
    private static final int MAX_FIFTHS = 7;
    private static final int FIFTHS_CYCLE = 12;
    private static final int QUARTER_NOTE_VALUE = 8;
    private static final double MICROSECONDS_PER_MINUTE = 60000000.0;
    private static final String TYPE_START = "start";
    private static final String TYPE_STOP = "stop";
    private static final String START_PROPERTY_OFFSET = "0:0:1:0";
    private static final Pattern SPELLING_PATTERN = Pattern.compile("\"([A-G])\"");
    private static final Pattern VARIANT_INT_PATTERN = Pattern.compile("\\{int: (-?\\d+)\\}");
    private static final Map<String, Integer> NOTE_NAME_FIFTHS = createNoteNameFifths();
    private static final Map<String, Integer> ACCIDENTAL_FIFTHS = createAccidentalFifths();
    private static final Map<String, ClefInfo> CLEFS = createClefs();


    /**
     * Constructor for DoricoReaderImpl.
     */
    public DoricoReaderImpl() {
        // default constructor
    }


    /**
     * Reads a Dorico project from the given path.
     *
     * @param path the path to the .dorico file
     * @return the parsed score
     * @throws IOException if an I/O error occurs or the file is not a valid Dorico project
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading Dorico project from path: {}", path);
        }

        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


    /**
     * Reads a Dorico project from an input stream.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException if an I/O error occurs or the data is not a valid Dorico project
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream must not be null.");
        }

        byte[] treeData = extractScoreTree(inputStream);
        DoricoTreeNode root = DoricoTreeDecoder.getInstance().decode(treeData);
        if (!ROOT_NODE.equals(root.getName())) {
            throw new IOException("Not a Dorico score: unexpected root node '" + root.getName() + "'.");
        }

        try {
            return convertToScore(root);
        } catch (ArithmeticException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Arithmetic overflow while converting Dorico score: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid Dorico score: rhythmic values out of range.", e);
        }
    }


    /**
     * Extracts the score tree file from the .dorico ZIP container.
     *
     * @param inputStream the input stream of the .dorico file
     * @return the raw score tree bytes
     * @throws IOException if extraction fails or no score tree is found
     */
    private byte[] extractScoreTree(InputStream inputStream) throws IOException {
        Map<String, byte[]> candidates = new HashMap<>();
        byte[] containerXml = null;
        long totalSize = 0;
        int entryCount = 0;

        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw new IOException("Dorico archive exceeds maximum entry count.");
                }

                String name = entry.getName();
                if (entry.isDirectory() || name.startsWith(MACOS_PREFIX)) {
                    zis.closeEntry();
                    continue;
                }

                // only the container and top-level tree files are kept; everything else is skipped
                boolean keep = CONTAINER_ENTRY.equals(name) || (name.indexOf('/') < 0
                        && (name.endsWith(".dtn") || name.endsWith(".xml")));
                ByteArrayOutputStream output = null;
                if (keep) {
                    output = new ByteArrayOutputStream();
                }
                totalSize += readBounded(zis, output);
                if (totalSize > MAX_TOTAL_SIZE) {
                    throw new IOException("Dorico archive exceeds maximum total decompressed size.");
                }

                if (CONTAINER_ENTRY.equals(name)) {
                    containerXml = output.toByteArray();
                } else if (keep) {
                    candidates.put(name, output.toByteArray());
                }
                zis.closeEntry();
            }
        } catch (IllegalArgumentException e) {
            // a damaged entry (for example a broken file name) is reported by the ZIP stream as illegal argument
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid ZIP archive: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid ZIP archive: " + e.getMessage(), e);
        }

        if (entryCount == 0) {
            throw new IOException("Not a Dorico project: no ZIP entries found.");
        }

        String rootFile = null;
        if (containerXml != null) {
            rootFile = parseRootFile(containerXml);
        }

        byte[] result = null;
        if (rootFile != null) {
            result = candidates.get(rootFile);
        }
        if (result == null) {
            result = candidates.get(DEFAULT_ROOT_FILE);
        }
        if (result == null) {
            result = candidates.get(DEFAULT_ROOT_FILE_XML);
        }
        if (result == null || result.length == 0) {
            throw new IOException("No score.dtn found in the Dorico project.");
        }
        return result;
    }


    /**
     * Reads the root file path from META-INF/container.xml.
     *
     * @param containerXml the container.xml content
     * @return the root file path, or null if it cannot be determined
     */
    private String parseRootFile(byte[] containerXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(containerXml));
            NodeList rootFiles = document.getElementsByTagNameNS("*", "rootfile");
            if (rootFiles.getLength() > 0) {
                String path = ((Element) rootFiles.item(0)).getAttribute("full-path");
                if (path != null && !path.isEmpty()) {
                    return path;
                }
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not parse Dorico container.xml, using default root file: {}", e.getMessage(), e);
            }
        }
        return null;
    }


    /**
     * Converts the decoded Dorico tree into an IScore model.
     *
     * @param root the kScore root node
     * @return the converted score
     * @throws IOException if the conversion fails
     */
    private IScore convertToScore(DoricoTreeNode root) throws IOException {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");

        DoricoTreeNode info = root.getChild("info");
        List<DoricoTreeNode> flows = getArrayItems(root.getChild("flows"));
        DoricoTreeNode firstFlow = null;
        if (!flows.isEmpty()) {
            firstFlow = flows.get(0);
        }

        DoricoScoreInfoConverter.getInstance().buildIdentification(info, firstFlow, score);

        Map<String, DoricoTreeNode> instrumentEntities = indexEntities(root.getPath("library", "instruments"));
        List<PartSource> partSources = collectParts(root, flows, instrumentEntities);

        // the flows (movements) follow each other in one score, the bar numbers continue
        List<FlowContext> contexts = new ArrayList<>();
        int totalBars = 0;
        for (DoricoTreeNode flow : flows) {
            FlowContext context = analyseFlow(flow);
            totalBars += context.getBars().size();
            if (totalBars > MAX_MEASURES) {
                throw new IOException("Dorico project exceeds maximum bar count (" + MAX_MEASURES + ").");
            }
            contexts.add(context);
        }
        if (contexts.isEmpty()) {
            contexts.add(new FlowContext());
        }

        PartListImpl partList = new PartListImpl();
        List<ScorePartImpl> scoreParts = new ArrayList<>();
        List<PartImpl> parts = new ArrayList<>();
        for (int i = 0; i < partSources.size(); i++) {
            PartSource source = partSources.get(i);
            String partId = "P" + (i + 1);

            ScorePartImpl scorePart = new ScorePartImpl();
            scorePart.setId(partId);
            scorePart.setPartName(source.getName());
            if (source.getAbbreviation() != null && !source.getAbbreviation().isEmpty()) {
                scorePart.setPartAbbreviation(source.getAbbreviation());
            }
            addInstrument(scorePart, source, i);
            scoreParts.add(scorePart);
            parts.add(buildPart(partId, source, contexts, i == 0));
        }

        partList.setScoreParts(scoreParts);
        score.setPartList(partList);
        score.setParts(parts);
        DoricoScoreInfoConverter.getInstance().addPageTexts(score, parts.size(), totalBars);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Converted Dorico score: {} parts, {} flows, {} bars.", parts.size(), flows.size(), totalBars);
        }
        return score;
    }


    /**
     * Indexes library entities of a collection by their entity ID.
     *
     * @param collection the library collection node (may be null)
     * @return the entities by entity ID
     */
    private Map<String, DoricoTreeNode> indexEntities(DoricoTreeNode collection) {
        Map<String, DoricoTreeNode> result = new HashMap<>();
        for (DoricoTreeNode entity : getArrayItems(collection)) {
            String entityId = entity.getPathValue("entityID");
            if (entityId != null && !entityId.isEmpty()) {
                result.put(entityId, entity);
            }
        }
        return result;
    }


    /**
     * Collects one part per instrument of every player that takes part in at least one flow.
     *
     * @param root the kScore root node
     * @param flows the flows (may be empty, then all players are converted)
     * @param instrumentEntities the library instrument entities by entity ID
     * @return the part sources in score order
     */
    private List<PartSource> collectParts(DoricoTreeNode root, List<DoricoTreeNode> flows, Map<String, DoricoTreeNode> instrumentEntities) {
        List<String> flowPlayerIds = new ArrayList<>();
        for (DoricoTreeNode flow : flows) {
            for (DoricoTreeNode flowPlayer : getArrayItems(flow.getChild("flowPlayers"))) {
                flowPlayerIds.add(flowPlayer.getPathValue(PLAYER_ID));
            }
        }

        List<PartSource> result = new ArrayList<>();
        for (DoricoTreeNode player : playersInScoreOrder(root)) {
            String playerId = player.getPathValue(PLAYER_ID);
            if (!flows.isEmpty() && !flowPlayerIds.contains(playerId)) {
                continue;
            }

            List<DoricoTreeNode> instruments = getArrayItems(player.getChild("instruments"));
            DoricoNotationConverter names = DoricoNotationConverter.getInstance();
            String playerName = names.getPlayerName(player.getPathValue("displayName"));
            String playerShortName = names.getPlayerName(player.getPathValue("displayShortName"));
            if (playerShortName != null && playerShortName.matches("\\d+")) {
                // the short name was only the key and the number of the player
                playerShortName = null;
            }
            for (int i = 0; i < instruments.size(); i++) {
                DoricoTreeNode instrument = instruments.get(i);
                String name = playerName;
                String abbreviation = playerShortName;
                if (i > 0 || name == null) {
                    name = names.getName(instrument.getPathValue("nameData", "singularFullName"));
                    abbreviation = names.getName(instrument.getPathValue("nameData", "singularShortName"));
                }
                if (name == null) {
                    name = "Player " + playerId;
                }

                DoricoTreeNode entity = instrumentEntities.get(instrument.getPathValue("entityID"));
                result.add(new PartSource(name, abbreviation, instrument, entity, playerId));
            }
        }
        return result;
    }


    /**
     * Gets the players in the order of the full score layout (the order Dorico shows and exports
     * them in). Players that are not part of the full score layout follow in their stored order.
     *
     * @param root the kScore root node
     * @return the players in score order
     */
    private List<DoricoTreeNode> playersInScoreOrder(DoricoTreeNode root) {
        List<DoricoTreeNode> players = getArrayItems(root.getChild("scorePlayers"));
        List<String> order = new ArrayList<>();
        for (DoricoTreeNode layout : getArrayItems(root.getChild("layouts"))) {
            if ("kFullScoreLayout".equals(layout.getPathValue("layoutType"))) {
                for (DoricoTreeNode layoutPlayer : getArrayItems(layout.getChild("layoutPlayers"))) {
                    order.add(layoutPlayer.getPathValue(PLAYER_ID));
                }
                break;
            }
        }
        List<DoricoTreeNode> result = new ArrayList<>();
        for (String playerId : order) {
            for (DoricoTreeNode player : players) {
                if (playerId != null && playerId.equals(player.getPathValue(PLAYER_ID)) && !result.contains(player)) {
                    result.add(player);
                }
            }
        }
        for (DoricoTreeNode player : players) {
            if (!result.contains(player)) {
                result.add(player);
            }
        }
        return result;
    }


    /**
     * Analyses a flow: collects all events, key and time signatures and builds the bar grid.
     *
     * @param flow the flow node
     * @return the flow context
     * @throws IOException if the flow is too large to convert
     */
    private FlowContext analyseFlow(DoricoTreeNode flow) throws IOException {
        Map<String, DoricoTreeNode> blocks = new HashMap<>();
        for (DoricoTreeNode block : getArrayItems(flow.getChild("blocks"))) {
            blocks.put(block.getPathValue("blockID"), block);
        }

        Map<String, DoricoTreeNode> blockInstances = new HashMap<>();
        for (DoricoTreeNode blockInstance : getArrayItems(flow.getChild("blockInstances"))) {
            blockInstances.put(blockInstance.getPathValue("blockInstanceID"), blockInstance);
        }

        FlowContext context = new FlowContext();
        String title = nonEmpty(flow.getPathValue("flowInfo", "title"));
        if (title == null) {
            title = nonEmpty(flow.getPathValue("flowName"));
        }
        context.setTitle(title);

        Map<String, DoricoTreeNode> barDivisionElements = new HashMap<>();
        Map<String, DoricoTreeNode> tonalityElements = new HashMap<>();
        for (DoricoTreeNode table : getArrayItems(flow.getChild("elementTables"))) {
            for (DoricoTreeNode element : getArrayItems(table)) {
                String elementId = element.getPathValue(ELEMENT_ID);
                if ("BarDivisionElementDefinition".equals(element.getName())) {
                    barDivisionElements.put(elementId, element);
                } else if ("TonalityDivisionElementDefinition".equals(element.getName())) {
                    tonalityElements.put(elementId, element);
                } else if ("DynamicGroupElementDefinition".equals(element.getName())) {
                    context.getDynamicGroups().put(elementId, element);
                } else if ("LyricElementDefinition".equals(element.getName())) {
                    context.getLyricElements().put(elementId, element);
                }
            }
        }

        TreeMap<Fraction, TimeInfo> timeChanges = new TreeMap<>();
        TreeMap<Fraction, KeyInfo> keyChanges = new TreeMap<>();
        TreeMap<Fraction, DoricoTreeNode> tempoChanges = new TreeMap<>();
        Map<Fraction, Fraction> irregularBars = new HashMap<>();
        Map<String, List<EventRef>> streamEvents = new HashMap<>();
        Fraction end = Fraction.ZERO;
        long divisions = 1;

        for (DoricoTreeNode stream : getArrayItems(flow.getChild("eventStreams"))) {
            boolean global = STREAM_GLOBAL.equals(stream.getPathValue("eventStreamType"));
            List<EventRef> events = new ArrayList<>();
            streamEvents.put(stream.getPathValue("streamID"), events);
            for (String blockInstanceId : splitIds(stream.getPathValue("blockInstanceIDs"))) {
                DoricoTreeNode blockInstance = blockInstances.get(blockInstanceId);
                if (blockInstance == null) {
                    continue;
                }

                Fraction offset = parseFraction(blockInstance.getPathValue(POSITION));
                DoricoTreeNode block = blocks.get(blockInstance.getPathValue("blockID"));
                if (block == null) {
                    continue;
                }

                for (DoricoTreeNode event : getArrayItems(block.getChild(EVENTS))) {
                    Fraction position = offset.add(parseFraction(event.getPathValue(POSITION)));
                    Fraction duration = parseFraction(event.getPathValue(DURATION));
                    end = end.max(position.add(duration));
                    divisions = lcm(divisions, position.getDenominator());
                    divisions = lcm(divisions, duration.getDenominator());
                    events.add(new EventRef(event, position, duration, parseSignedFraction(event.getPathValue(SECONDARY_POSITION))));

                    if (!global) {
                        continue;
                    }
                    if ("ImmediateTempoChangeEventDefinition".equals(event.getName())) {
                        tempoChanges.put(position, event);
                    } else if ("BarDivisionEventDefinition".equals(event.getName())) {
                        DoricoTreeNode element = barDivisionElements.get(event.getPathValue("barDivisionElementID"));
                        TimeInfo time = parseTime(element);
                        if (time != null) {
                            timeChanges.put(position, time);
                        }
                        Fraction irregularLength = parseFraction(getPathValue(element, "barDivisionData", "irregularBarLength"));
                        if (irregularLength.compareTo(Fraction.ZERO) > 0) {
                            irregularBars.put(position, irregularLength);
                        }
                        String barline = nonEmpty(getPathValue(element, "barDivisionData", "barlineChangeIfAny", "type"));
                        if (barline != null) {
                            context.getBarlineTypes().put(position, barline);
                        }
                    } else if ("TonalityDivisionEventDefinition".equals(event.getName())) {
                        KeyInfo key = parseKey(tonalityElements.get(event.getPathValue("tonalityDivisionElementID")));
                        if (key != null) {
                            keyChanges.put(position, key);
                        }
                    } else if ("PauseEventDefinition".equals(event.getName()) || "RepeatEndingLineEventDefinition".equals(event.getName())
                            || "RepeatMarkerEventDefinition".equals(event.getName()) || REHEARSAL_MARK.equals(event.getName())
                            || GRADUAL_TEMPO.equals(event.getName())) {
                        context.getGlobalEvents().add(events.get(events.size() - 1));
                    }
                }
            }
        }

        Fraction minimumLength = parseFraction(flow.getPathValue("minimumNotatedLength", "length"));
        end = end.max(minimumLength);

        List<BarInfo> bars = buildBars(end, timeChanges, keyChanges, irregularBars);
        for (BarInfo bar : bars) {
            divisions = lcm(divisions, bar.getLength().getDenominator());
        }
        if (divisions > MAX_DIVISIONS) {
            throw new IOException("Dorico flow uses a too fine rhythmic grid (" + divisions + " divisions per quarter note).");
        }

        Map<String, DoricoTreeNode> flowPlayers = new HashMap<>();
        for (DoricoTreeNode flowPlayer : getArrayItems(flow.getChild("flowPlayers"))) {
            flowPlayers.put(flowPlayer.getPathValue(PLAYER_ID), flowPlayer);
        }

        for (List<EventRef> events : streamEvents.values()) {
            events.sort(null);
        }
        context.getGlobalEvents().sort(null);

        context.setBars(bars);
        context.setDivisions((int) divisions);
        context.setStreamEvents(streamEvents);
        context.setTempoChanges(tempoChanges);
        context.setKeyChanges(keyChanges);
        context.setFlowPlayers(flowPlayers);
        return context;
    }


    /**
     * Builds the bar grid of a flow from its time signature changes.
     *
     * @param end the end of the flow in quarter notes
     * @param timeChanges the time signatures by position
     * @param keyChanges the key signatures by position
     * @param irregularBars the length of the irregular bar (pick-up bar) that starts at a bar division, by position
     * @return the bars
     * @throws IOException if the flow has too many bars
     */
    private List<BarInfo> buildBars(Fraction end, TreeMap<Fraction, TimeInfo> timeChanges,
                                    TreeMap<Fraction, KeyInfo> keyChanges, Map<Fraction, Fraction> irregularBars) throws IOException {
        List<BarInfo> bars = new ArrayList<>();
        TimeInfo currentTime = new TimeInfo("4", 4, Fraction.of(4, 1));
        Fraction start = Fraction.ZERO;

        do {
            TimeInfo timeChange = null;
            Map.Entry<Fraction, TimeInfo> timeEntry = timeChanges.floorEntry(start);
            if (timeEntry != null && timeEntry.getValue() != currentTime) {
                // a bar division that repeats the current time signature (for example to place a repeat barline) is no change
                if (bars.isEmpty() || !timeEntry.getValue().isSameAs(currentTime)) {
                    timeChange = timeEntry.getValue();
                }
                currentTime = timeEntry.getValue();
            }

            Fraction length = currentTime.getBarLength();
            Fraction next = timeChanges.higherKey(start);
            if (next != null && next.compareTo(start.add(length)) < 0) {
                // a time signature change in the middle of a bar shortens the bar (irregular bar)
                length = next.subtract(start);
                TimeInfo nextTime = timeChanges.get(next);
                if (timeChange != null && nextTime.getBarLength().equals(length)) {
                    // a displayed meter (for example 9/8 over 3/4 with triplets) followed by the real meter:
                    // the bar gets the time signature that matches its length
                    timeChange = nextTime;
                    currentTime = nextTime;
                }
            }

            // an irregular bar length on the bar division shortens the bar that starts there: a pick-up bar at the
            // start of the flow, or after a repeat barline in the middle of a bar
            Fraction irregularLength = irregularBars.get(start);
            boolean pickup = false;
            if (irregularLength != null && irregularLength.compareTo(length) < 0) {
                length = irregularLength;
                pickup = true;
            }

            KeyInfo keyChange = null;
            Fraction barEnd = start.add(length);
            for (Map.Entry<Fraction, KeyInfo> keyEntry : keyChanges.subMap(start, true, barEnd, false).entrySet()) {
                keyChange = keyEntry.getValue();
            }

            bars.add(new BarInfo(start, length, currentTime, timeChange, keyChange, pickup));
            if (bars.size() > MAX_MEASURES) {
                throw new IOException("Dorico flow exceeds maximum bar count (" + MAX_MEASURES + ").");
            }
            start = barEnd;
        } while (start.compareTo(end) < 0);

        return bars;
    }


    /**
     * Builds a part with the bars of all flows. Every flow starts with complete attributes and ends
     * with a final barline; the bar numbers continue across flows.
     *
     * @param partId the part ID
     * @param source the part source
     * @param contexts the flow contexts in score order
     * @param firstPart true for the first part, which receives the global directions (tempo, repeat markers, flow titles)
     * @return the part
     */
    private PartImpl buildPart(String partId, PartSource source, List<FlowContext> contexts, boolean firstPart) {
        PartImpl part = new PartImpl();
        part.setId(partId);

        int staves = parseInt(source.getInstrument().getPathValue("numStaves"), 1);
        if (staves < 1) {
            staves = 1;
        }
        boolean multiStaff = staves > 1;
        int writtenChromatic = 0;
        int writtenDiatonic = 0;
        if (source.getEntity() != null) {
            writtenChromatic = parseInt(source.getEntity().getPathValue("pitchedInstrumentData", "transposedChromaticTransposition"), 0);
            writtenDiatonic = parseInt(source.getEntity().getPathValue("pitchedInstrumentData", "transposedDiatonicTransposition"), 0);
        }

        List<MeasureImpl> measures = new ArrayList<>();
        for (FlowContext context : contexts) {
            int divisions = context.getDivisions();
            PartLayout layout = resolveLayout(source, context, staves, writtenChromatic, writtenDiatonic);
            if (firstPart) {
                // the directions of the whole score (rehearsal marks, repeat markers) belong to the first part
                addGlobalDirections(layout, context, contexts.size() > 1);
                addRehearsalMarks(layout, context);
            }
            BarlinePlan barlines = buildBarlines(context);

            List<BarInfo> bars = context.getBars();
            for (int b = 0; b < bars.size(); b++) {
                BarInfo bar = bars.get(b);
                MeasureImpl measure = new MeasureImpl();
                measure.setNumber(String.valueOf(measures.size() + 1));
                if (bar.isPickup()) {
                    measure.setImplicit("yes");
                }

                final List<AttributesImpl> attributesList = buildAttributes(source, layout, bar, b == 0, divisions, writtenChromatic, writtenDiatonic);
                List<DirectionImpl> directions = new ArrayList<>();
                if (firstPart) {
                    directions.addAll(buildTempoDirections(bar, context, divisions));
                }
                Fraction barEnd = bar.getStart().add(bar.getLength());
                Fraction directionsEnd = barEnd;
                boolean lastBar = b == bars.size() - 1;
                if (lastBar) {
                    // directions at the very end of the flow (for example the end of a hairpin) belong to the last bar
                    directionsEnd = barEnd.add(Fraction.of(1, 1));
                }
                for (Map.Entry<Fraction, List<DirectionImpl>> entry : layout.getDirections().subMap(bar.getStart(), true, directionsEnd, false).entrySet()) {
                    int offset = toDivisions(entry.getKey().subtract(bar.getStart()), divisions);
                    for (DirectionImpl direction : entry.getValue()) {
                        if (offset > 0) {
                            direction.setOffset(offset);
                        }
                        directions.add(direction);
                    }
                }
                BarlineImpl right = barlines.getRight(barEnd);
                if (right == null && lastBar) {
                    right = createBarline(LOCATION_RIGHT);
                }
                if (right != null && lastBar && right.getBarStyle() == null) {
                    right.setBarStyle(BAR_STYLE_FINAL);
                }

                // the content in document order: the directions carry their offset from the start of the bar
                List<IMusicDataElement> content = new ArrayList<>();
                content.add(barlines.getLeft(bar.getStart()));
                content.addAll(attributesList);
                content.addAll(directions);
                content.addAll(buildMeasureContent(bar, layout, divisions, multiStaff));
                content.add(right);
                measure.setMusicData(content);
                measures.add(measure);
            }
        }

        part.setMeasures(measures);
        return part;
    }


    /**
     * Builds the attributes of a bar: the complete attributes at the start of a flow, key, time and
     * clef changes otherwise. Clef changes in the middle of a bar are added as separate attributes,
     * since the measure model does not keep the order of its content.
     *
     * @param source the part source
     * @param layout the part layout of the flow
     * @param bar the bar
     * @param flowStart true for the first bar of a flow
     * @param divisions the divisions per quarter note
     * @param writtenChromatic the semitones the written pitch lies above the sounding pitch
     * @param writtenDiatonic the diatonic steps the written pitch lies above the sounding pitch
     * @return the attributes (may be empty)
     */
    private List<AttributesImpl> buildAttributes(PartSource source, PartLayout layout, BarInfo bar, boolean flowStart,
                                                 int divisions, int writtenChromatic, int writtenDiatonic) {
        int staves = layout.getStaves();

        AttributesImpl attributes = new AttributesImpl();
        boolean used = false;
        if (flowStart) {
            attributes.setDivisions(divisions);
            if (staves > 1) {
                attributes.setStaves(staves);
            }
            List<ClefImpl> clefs = buildClefs(source.getInstrument(), staves);
            for (ClefImpl clef : layout.getClefs(bar.getStart())) {
                // clefs at the start of the flow replace the default clefs of the instrument
                int index = 0;
                if (clef.getNumber() != null) {
                    index = clef.getNumber() - 1;
                }
                if (index >= 0 && index < clefs.size()) {
                    clefs.set(index, clef);
                }
            }
            attributes.setClefs(clefs);
            TransposeImpl transpose = buildTranspose(source.getEntity());
            if (transpose != null) {
                attributes.setTransposes(Collections.singletonList(transpose));
            }
            used = true;
        } else if (!layout.getClefs(bar.getStart()).isEmpty()) {
            attributes.setClefs(layout.getClefs(bar.getStart()));
            used = true;
        }

        KeyInfo key = bar.getKeyChange();
        if (flowStart && key == null) {
            key = new KeyInfo(0, "major");
        }
        if (key != null) {
            KeyImpl keyImpl = new KeyImpl();
            keyImpl.setFifths(writtenFifths(key, writtenChromatic, writtenDiatonic));
            keyImpl.setMode(key.getMode());
            attributes.setKeys(Collections.singletonList(keyImpl));
            used = true;
        }
        if (bar.getTimeChange() != null) {
            TimeImpl time = new TimeImpl();
            time.setBeats(bar.getTimeChange().getBeats());
            time.setBeatType(String.valueOf(bar.getTimeChange().getBeatType()));
            attributes.setTimes(Collections.singletonList(time));
            used = true;
        }
        List<AttributesImpl> result = new ArrayList<>();
        if (used) {
            result.add(attributes);
        }

        Fraction barEnd = bar.getStart().add(bar.getLength());
        for (Map.Entry<Fraction, List<ClefImpl>> entry : layout.getClefChanges().subMap(bar.getStart(), false, barEnd, false).entrySet()) {
            AttributesImpl clefChange = new AttributesImpl();
            clefChange.setClefs(entry.getValue());
            result.add(clefChange);
        }
        return result;
    }


    /**
     * Adds the global directions of a flow to the first part: repeat markers and, in a project
     * with several flows, the flow title at the start of the flow.
     *
     * @param layout the part layout receiving the directions
     * @param context the flow context
     * @param multipleFlows true if the project has more than one flow
     */
    private void addGlobalDirections(PartLayout layout, FlowContext context, boolean multipleFlows) {
        DoricoNotationConverter converter = DoricoNotationConverter.getInstance();
        if (multipleFlows && context.getTitle() != null) {
            WordsImpl words = converter.createWords(context.getTitle(), null);
            words.setFontWeight("bold");
            layout.addDirection(Fraction.ZERO, createDirection(PLACEMENT_ABOVE, null, wordsType(Collections.singletonList(words))));
        }

        for (EventRef event : context.getGlobalEvents()) {
            if (!"RepeatMarkerEventDefinition".equals(event.getNode().getName())
                    || "true".equals(converter.getProperty(event.getNode(), "kRepeatMarkerHide"))) {
                continue;
            }
            DirectionTypeImpl type = converter.createRepeatMarker(event.getNode().getPathValue("sectionType"),
                    converter.getProperty(event.getNode(), "kRepeatMarkerLabelOverride"));
            if (type != null) {
                layout.addDirection(event.getPosition(), createDirection(PLACEMENT_ABOVE, null, type));
            }
        }
    }


    /**
     * Adds the rehearsal marks of a flow to a part. Dorico numbers the rehearsal marks automatically: the letters
     * follow the order of the marks (A to Z, then AA, BB and so on).
     *
     * @param layout the part layout receiving the directions
     * @param context the flow context
     */
    private void addRehearsalMarks(PartLayout layout, FlowContext context) {
        List<Fraction> positions = new ArrayList<>();
        for (EventRef event : context.getGlobalEvents()) {
            if (REHEARSAL_MARK.equals(event.getNode().getName())) {
                positions.add(event.getPosition());
            }
        }
        Collections.sort(positions);
        for (int i = 0; i < positions.size(); i++) {
            char letter = (char) ('A' + i % LETTERS);
            StringBuilder text = new StringBuilder();
            for (int n = 0; n <= i / LETTERS; n++) {
                text.append(letter);
            }
            DirectionTypeImpl type = new DirectionTypeImpl();
            type.setRehearsal(text.toString());
            layout.addDirection(positions.get(i), createDirection(PLACEMENT_ABOVE, null, type));
        }
    }


    /**
     * Plans the barlines of a flow: repeat and double barlines from the bar divisions, repeat
     * endings and fermatas attached to barlines.
     *
     * @param context the flow context
     * @return the barline plan
     */
    private BarlinePlan buildBarlines(FlowContext context) {
        DoricoNotationConverter converter = DoricoNotationConverter.getInstance();
        BarlinePlan plan = new BarlinePlan();
        for (Map.Entry<Fraction, String> entry : context.getBarlineTypes().entrySet()) {
            String type = entry.getValue();
            if ("kEndRepeat".equals(type) || "kEndAndStartRepeat".equals(type)) {
                BarlineImpl right = plan.right(entry.getKey());
                right.setBarStyle(BAR_STYLE_FINAL);
                right.setRepeat(createRepeat("backward"));
            }
            if ("kStartRepeat".equals(type) || "kEndAndStartRepeat".equals(type)) {
                BarlineImpl left = plan.left(entry.getKey());
                left.setBarStyle("heavy-light");
                left.setRepeat(createRepeat("forward"));
            }
            if ("kDouble".equals(type)) {
                plan.right(entry.getKey()).setBarStyle("light-light");
            }
        }

        for (EventRef event : context.getGlobalEvents()) {
            String name = event.getNode().getName();
            if ("RepeatEndingLineEventDefinition".equals(name)) {
                List<Fraction> segments = readEndingSegments(event);
                Fraction start = event.getPosition();
                for (int i = 0; i < segments.size(); i++) {
                    Fraction end = start.add(segments.get(i));
                    String number = String.valueOf(i + 1);
                    plan.left(start).setEnding(createEnding(number, TYPE_START));
                    String stopType = TYPE_STOP;
                    if (i == segments.size() - 1 && segments.size() > 1) {
                        // the last ending is usually open
                        stopType = "discontinue";
                    }
                    plan.right(end).setEnding(createEnding(number, stopType));
                    start = end;
                }
            } else if ("PauseEventDefinition".equals(name) && converter.getProperty(event.getNode(), "kPauseAttachToBarline") != null) {
                plan.right(event.getPosition()).setFermata(createFermata());
            }
        }
        return plan;
    }


    /**
     * Reads the segment lengths of a repeat ending line (one per ending).
     *
     * @param event the RepeatEndingLineEventDefinition event
     * @return the segment lengths in quarter notes
     */
    private List<Fraction> readEndingSegments(EventRef event) {
        TreeMap<Integer, Fraction> segments = new TreeMap<>();
        for (DoricoTreeNode entry : DoricoNotationConverter.getInstance().getProperties(event.getNode(), "kRepeatEndingLineSegmentDuration")) {
            // the property offset "2:<segment>:0:0" selects the segment
            String offset = DoricoNotationConverter.getInstance().plainValue(entry.getPathValue("context", "propertyOffset"));
            String[] parts = String.valueOf(offset).split(":");
            int index = 0;
            if (parts.length > 1) {
                index = parseInt(parts[1], 0);
            }
            Fraction length = parseFraction(DoricoNotationConverter.getInstance().plainValue(entry.getPathValue("value")));
            if (length.isPositive()) {
                segments.put(index, length);
            }
        }

        List<Fraction> result = new ArrayList<>(segments.values());
        if (result.isEmpty() && event.getDuration().isPositive()) {
            result.add(event.getDuration());
        }
        return result;
    }


    /**
     * Resolves the staves and voices of a part from the flow player data and prepares the chords of every voice.
     *
     * @param source the part source
     * @param context the flow context
     * @param staves the number of staves of the instrument
     * @param writtenChromatic the semitones the written pitch lies above the sounding pitch
     * @param writtenDiatonic the diatonic steps the written pitch lies above the sounding pitch
     * @return the part layout
     */
    private PartLayout resolveLayout(PartSource source, FlowContext context, int staves, int writtenChromatic, int writtenDiatonic) {
        PartLayout layout = new PartLayout(staves);
        DoricoTreeNode flowPlayer = context.getFlowPlayers().get(source.getPlayerId());
        if (flowPlayer == null) {
            return layout;
        }

        String instrumentId = source.getInstrument().getPathValue("instrumentID");
        DoricoTreeNode flowInstrument = null;
        for (DoricoTreeNode candidate : getArrayItems(flowPlayer.getChild("flowPlayerInstruments"))) {
            if (instrumentId != null && instrumentId.equals(candidate.getPathValue("instrumentID"))) {
                flowInstrument = candidate;
                break;
            }
        }
        if (flowInstrument == null) {
            return layout;
        }

        Map<String, Integer> staveIdToStaff = new HashMap<>();
        for (DoricoTreeNode info : getArrayItems(flowInstrument.getChild("staveStreamInfos"))) {
            if (isOssia(info)) {
                continue;
            }
            int staff = parseInt(info.getPathValue("nthStaveInInstrument"), 0) + 1;
            if (staff >= 1 && staff <= staves) {
                staveIdToStaff.put(info.getPathValue("staveID"), staff);
                readStaveEvents(layout, context, getEvents(context, info.getPathValue("staveStreamID")), staff);
            }
        }

        int[] voicesPerStaff = new int[staves + 1];
        for (DoricoTreeNode info : getArrayItems(flowInstrument.getChild("voiceStreamInfos"))) {
            if (isOssia(info)) {
                continue;
            }
            Integer staff = staveIdToStaff.get(info.getPathValue("defaultLogicalStaveID"));
            if (staff == null) {
                staff = 1;
            }
            int index = voicesPerStaff[staff];
            voicesPerStaff[staff]++;

            VoiceLayout voice = new VoiceLayout(staff, (staff - 1) * VOICES_PER_STAFF + index + 1, index == 0);
            VoiceSource voiceSource = new VoiceSource(staveIdToStaff, writtenChromatic, writtenDiatonic);
            fillVoice(voice, voiceSource, getEvents(context, info.getPathValue("voiceStreamID")), context, layout);
            layout.addVoice(voice);
        }
        return layout;
    }


    /**
     * Gets the events of a stream.
     *
     * @param context the flow context
     * @param streamId the stream ID
     * @return the events sorted by position (may be empty)
     */
    private List<EventRef> getEvents(FlowContext context, String streamId) {
        List<EventRef> events = context.getStreamEvents().get(streamId);
        if (events == null) {
            return Collections.emptyList();
        }
        return events;
    }


    /**
     * Reads the events of an instrument stave stream: clef changes and dynamics.
     *
     * @param layout the part layout
     * @param context the flow context
     * @param events the stave stream events
     * @param staff the staff number (1-based)
     */
    private void readStaveEvents(PartLayout layout, FlowContext context, List<EventRef> events, int staff) {
        Integer staffNumber = null;
        if (layout.getStaves() > 1) {
            staffNumber = staff;
        }

        for (EventRef event : events) {
            String name = event.getNode().getName();
            if ("ClefEventDefinition".equals(name)) {
                String clefId = nonEmpty(event.getNode().getPathValue("entityIDs", "idForTransposingLayouts"));
                if (clefId == null) {
                    clefId = nonEmpty(event.getNode().getPathValue("entityIDs", "idForConcertLayouts"));
                }
                if (clefId != null) {
                    layout.addClef(event.getPosition(), createClef(clefId, staffNumber));
                }
            } else if ("DynamicGroupEventDefinition".equals(name)) {
                DoricoTreeNode group = context.getDynamicGroups().get(event.getNode().getPathValue("dynamicGroupID"));
                if (group != null) {
                    addDynamics(layout, event.getPosition(), group, staffNumber);
                }
            }
        }
    }


    /**
     * Adds the directions of a dynamic group: immediate dynamics (pp, mf), force dynamics (sfz),
     * combined dynamics (fp), hairpins and written gradual changes (cresc., dim.).
     *
     * @param layout the part layout
     * @param position the position of the group
     * @param group the DynamicGroupElementDefinition node
     * @param staffNumber the staff number, or null for single-staff parts
     */
    private void addDynamics(PartLayout layout, Fraction position, DoricoTreeNode group, Integer staffNumber) {
        DoricoNotationConverter converter = DoricoNotationConverter.getInstance();
        for (DoricoTreeNode definition : getArrayItems(group.getChild("subElements"))) {
            Fraction start = position.add(parseFraction(definition.getPathValue(POSITION)));
            String marking = null;
            if ("ImmediateChangeDynamicDefinition".equals(definition.getName())) {
                marking = converter.getImmediateDynamic(definition);
            } else if ("ForceDynamicDefinition".equals(definition.getName())) {
                marking = converter.getForceDynamic(definition);
            } else if ("CombinedDynamicDefinition".equals(definition.getName())) {
                marking = converter.getCombinedDynamic(definition);
            } else if ("GradualChangeDynamicDefinition".equals(definition.getName())) {
                addGradualDynamic(layout, start, definition, staffNumber);
                continue;
            }

            List<DirectionTypeImpl> types = new ArrayList<>();
            if (marking != null && converter.getProperty(definition, "kDynamicHideIntensityMarking") == null) {
                DirectionTypeImpl type = new DirectionTypeImpl();
                type.setDynamics(converter.createDynamics(marking));
                types.add(type);
            }
            String textAfter = nonEmpty(converter.getProperty(definition, "kDynamicCustomTextAfter"));
            if (textAfter != null) {
                types.add(wordsType(Collections.singletonList(converter.createWords(textAfter, FONT_ITALIC))));
            }
            if (!types.isEmpty()) {
                layout.addDirection(start, createDirection(PLACEMENT_BELOW, staffNumber, types.toArray(new DirectionTypeImpl[0])));
            }
        }
    }


    /**
     * Adds a gradual dynamic: a hairpin (wedge start and stop) or the words cresc. or dim.
     *
     * @param layout the part layout
     * @param start the start position
     * @param definition the GradualChangeDynamicDefinition node
     * @param staffNumber the staff number, or null for single-staff parts
     */
    private void addGradualDynamic(PartLayout layout, Fraction start, DoricoTreeNode definition, Integer staffNumber) {
        DoricoNotationConverter converter = DoricoNotationConverter.getInstance();
        boolean crescendo = !"kGradualDynamicDown".equals(definition.getPathValue("direction"));
        String appearance = converter.getProperty(definition, "kDynamicGradualChangeDynamicAppearance");
        if ("kText".equals(appearance) || "kTextWithLine".equals(appearance)) {
            String text = "dim.";
            if (crescendo) {
                text = "cresc.";
            }
            layout.addDirection(start, createDirection(PLACEMENT_BELOW, staffNumber,
                    wordsType(Collections.singletonList(converter.createWords(text, FONT_ITALIC)))));
            return;
        }

        Fraction end = start.add(parseFraction(definition.getPathValue(DURATION)));
        int number = layout.allocateWedgeNumber(start, end);
        WedgeImpl wedge = new WedgeImpl();
        if (crescendo) {
            wedge.setType("crescendo");
        } else {
            wedge.setType("diminuendo");
        }
        wedge.setNumber(number);
        DirectionTypeImpl startType = new DirectionTypeImpl();
        startType.setWedge(wedge);
        layout.addDirection(start, createDirection(PLACEMENT_BELOW, staffNumber, startType));

        WedgeImpl stop = new WedgeImpl();
        stop.setType(TYPE_STOP);
        stop.setNumber(number);
        DirectionTypeImpl stopType = new DirectionTypeImpl();
        stopType.setWedge(stop);
        layout.addDirection(end, createDirection(PLACEMENT_BELOW, staffNumber, stopType));
    }


    /**
     * Checks whether a stave or voice stream info belongs to an ossia stave.
     *
     * @param info the stream info node
     * @return true for ossia staves
     */
    private boolean isOssia(DoricoTreeNode info) {
        String ossia = info.getPathValue("ossia");
        return ossia != null && !ossia.isEmpty() && !"kNone".equals(ossia);
    }


    /**
     * Fills a voice from its stream events: notes at the same position form a chord, grace notes,
     * articulations, ornaments, lyrics and fermatas are attached to their chord, slurs to the first
     * and last chord they cover, tuplets are kept as time spans and staff text becomes a direction.
     *
     * @param voice the voice to fill
     * @param source the pitch conversion data of the part
     * @param events the events of the voice stream, sorted by position
     * @param context the flow context
     * @param layout the part layout receiving the text directions
     */
    private void fillVoice(VoiceLayout voice, VoiceSource source, List<EventRef> events, FlowContext context, PartLayout layout) {
        DoricoNotationConverter converter = DoricoNotationConverter.getInstance();
        TreeMap<Fraction, ChordInfo> chords = new TreeMap<>();
        TreeMap<Fraction, TreeMap<Fraction, GraceGroup>> graces = new TreeMap<>();
        List<EventRef> slurs = new ArrayList<>();
        List<EventRef> marks = new ArrayList<>();
        for (EventRef event : events) {
            DoricoTreeNode node = event.getNode();
            String name = node.getName();
            if ("NoteEventDefinition".equals(name)) {
                NoteSpec note = createNoteSpec(node, event.getPosition(), source, context);
                if (note == null || !event.getDuration().isPositive()) {
                    continue;
                }
                ChordInfo chord = chords.get(event.getPosition());
                if (chord == null) {
                    chord = new ChordInfo(event.getPosition(), event.getPosition().add(event.getDuration()));
                    chords.put(event.getPosition(), chord);
                }
                note.setEnd(event.getPosition().add(event.getDuration()));
                note.setCue("kCue".equals(converter.getProperty(node, "kEventScale")));
                chord.addNote(note);
                if (chord.getArticulations() == null) {
                    chord.setArticulations(converter.createArticulations(node));
                }
                if (chord.getFingering() == null) {
                    chord.setFingering(fingering(converter.getProperty(node, "kNoteFingering")));
                }
            } else if ("GraceNoteEventDefinition".equals(name)) {
                NoteSpec note = createNoteSpec(node, event.getPosition(), source, context);
                Fraction notated = parseFraction(node.getPathValue("notatedDuration"));
                if (note == null || !notated.isPositive()) {
                    continue;
                }
                TreeMap<Fraction, GraceGroup> groups = graces.computeIfAbsent(event.getPosition(), k -> new TreeMap<>());
                Fraction order = parseSignedFraction(node.getPathValue(SECONDARY_POSITION));
                GraceGroup group = groups.get(order);
                if (group == null) {
                    group = new GraceGroup(notated, "true".equals(node.getPathValue("slashed")));
                    groups.put(order, group);
                }
                group.addNote(note);
            } else if ("SlurEventDefinition".equals(name)) {
                slurs.add(event);
            } else if ("TupletEventDefinition".equals(name)) {
                TupletInfo tuplet = createTuplet(event);
                if (tuplet != null) {
                    voice.addTuplet(tuplet);
                }
            } else if ("TextEventDefinition".equals(name)) {
                addText(layout, event, voice);
            } else {
                marks.add(event);
            }
        }

        splitLongerNotes(chords);

        // notes in one voice must not overlap: a chord ends at the latest where the next one starts
        List<ChordInfo> result = new ArrayList<>(chords.values());
        for (int i = 0; i + 1 < result.size(); i++) {
            ChordInfo chord = result.get(i);
            Fraction nextStart = result.get(i + 1).getStart();
            if (chord.getEnd().compareTo(nextStart) > 0) {
                chord.setEnd(nextStart);
            }
        }

        // grace notes belong to the chord at their position (or the next chord)
        for (Map.Entry<Fraction, TreeMap<Fraction, GraceGroup>> entry : graces.entrySet()) {
            Map.Entry<Fraction, ChordInfo> target = chords.ceilingEntry(entry.getKey());
            if (target != null) {
                target.getValue().getGraces().addAll(entry.getValue().values());
            }
        }

        attachMarks(chords, marks, context);
        for (EventRef pause : context.getGlobalEvents()) {
            if ("PauseEventDefinition".equals(pause.getNode().getName())
                    && converter.getProperty(pause.getNode(), "kPauseAttachToBarline") == null) {
                ChordInfo chord = chords.get(pause.getPosition());
                if (chord != null) {
                    chord.setFermata(true);
                }
            }
        }

        assignSlurs(result, slurs);
        voice.setChords(result);
    }


    /**
     * Attaches lyrics, ornaments and trills to the chord at their position.
     *
     * @param chords the chords by position
     * @param marks the lyric, ornament and trill events of the voice
     * @param context the flow context
     */
    private void attachMarks(TreeMap<Fraction, ChordInfo> chords, List<EventRef> marks, FlowContext context) {
        DoricoNotationConverter converter = DoricoNotationConverter.getInstance();
        for (EventRef event : marks) {
            ChordInfo chord = chords.get(event.getPosition());
            if (chord == null) {
                continue;
            }
            String name = event.getNode().getName();
            if ("LyricEventDefinition".equals(name)) {
                DoricoTreeNode element = context.getLyricElements().get(event.getNode().getPathValue("lyricElementID"));
                if (element != null) {
                    LyricImpl lyric = converter.createLyric(element);
                    if (lyric != null) {
                        chord.getLyrics().add(lyric);
                    }
                }
            } else if ("OrnamentEventDefinition".equals(name)) {
                OrnamentsImpl ornament = converter.createOrnament(event.getNode().getPathValue("entityID"));
                if (ornament != null && chord.getOrnaments() == null) {
                    chord.setOrnaments(ornament);
                }
            } else if ("TrillEventDefinition".equals(name) && chord.getOrnaments() == null) {
                chord.setOrnaments(converter.createTrill());
            }
        }
    }


    /**
     * Converts the pitch of a note or grace note event into a written note.
     *
     * @param node the note or grace note event
     * @param position the absolute position of the event
     * @param source the pitch conversion data of the part
     * @param context the flow context
     * @return the note, or null if the pitch is invalid
     */
    private NoteSpec createNoteSpec(DoricoTreeNode node, Fraction position, VoiceSource source, FlowContext context) {
        int pitch = parseInt(node.getPathValue("pitch"), -1);
        if (pitch < 0 || pitch > MAX_MIDI_PITCH) {
            return null;
        }

        boolean preferFlats = false;
        Map.Entry<Fraction, KeyInfo> key = context.getKeyChanges().floorEntry(position);
        if (key != null && writtenFifths(key.getValue(), source.getWrittenChromatic(), source.getWrittenDiatonic()) < 0) {
            preferFlats = true;
        }
        String letter = DoricoPitchConverter.getInstance().shiftLetter(readSpelling(node), source.getWrittenDiatonic());
        Integer staff = source.getStaveIdToStaff().get(node.getPathValue("logicalStaveIDOverride"));
        return new NoteSpec(DoricoPitchConverter.getInstance().toPitch(pitch + source.getWrittenChromatic(), letter, preferFlats), staff);
    }


    /**
     * Reads a tuplet event.
     *
     * @param event the TupletEventDefinition event
     * @return the tuplet, or null if the definition is invalid
     */
    private TupletInfo createTuplet(EventRef event) {
        List<Integer> definition = parseVariantInts(event.getNode().getPathValue("tupletDefinition"));
        if (definition.size() < 2 || definition.get(0) <= 0 || definition.get(1) <= 0 || !event.getDuration().isPositive()) {
            return null;
        }

        String normalType = null;
        if (definition.size() >= 3) {
            normalType = DoricoDurationConverter.getInstance().noteValueToType(definition.get(2));
        }
        DoricoNotationConverter converter = DoricoNotationConverter.getInstance();
        boolean showNumber = !"kNone".equals(converter.getProperty(event.getNode(), "kTupletTextContent"));
        boolean showBracket = !"kHidden".equals(converter.getProperty(event.getNode(), "kTupletBracketVisibility"));
        return new TupletInfo(event.getPosition(), event.getPosition().add(event.getDuration()),
                definition.get(0), definition.get(1), normalType, showNumber, showBracket);
    }


    /**
     * Adds a staff text event as a words direction.
     *
     * @param layout the part layout
     * @param event the TextEventDefinition event
     * @param voice the voice of the event
     */
    private void addText(PartLayout layout, EventRef event, VoiceLayout voice) {
        String text = DoricoNotationConverter.getInstance().getText(event.getNode());
        if (text == null) {
            return;
        }
        String placement = PLACEMENT_ABOVE;
        if ("kBelow".equals(event.getNode().getPathValue("aboveOrBelow"))) {
            placement = PLACEMENT_BELOW;
        }
        Integer staffNumber = null;
        if (layout.getStaves() > 1) {
            staffNumber = voice.getStaff();
        }
        layout.addDirection(event.getPosition(), createDirection(placement, staffNumber,
                wordsType(Collections.singletonList(DoricoNotationConverter.getInstance().createWords(text, FONT_ITALIC)))));
    }


    /**
     * Reads the spelled letter name of a note (property kNoteSpelling at the start of the note).
     *
     * @param note the note event node
     * @return the letter name, or null if the note has no spelling
     */
    private String readSpelling(DoricoTreeNode note) {
        String result = null;
        for (DoricoTreeNode entry : getArrayItems(note.getPath("propertyTable", "entries"))) {
            if (!"kNoteSpelling".equals(entry.getPathValue("propertyType"))) {
                continue;
            }

            String value = entry.getPathValue("value");
            if (value == null) {
                continue;
            }
            Matcher matcher = SPELLING_PATTERN.matcher(value);
            if (!matcher.find()) {
                continue;
            }

            String offset = entry.getPathValue("context", "propertyOffset");
            if (offset == null || offset.contains(START_PROPERTY_OFFSET)) {
                return matcher.group(1);
            }
            if (result == null) {
                result = matcher.group(1);
            }
        }
        return result;
    }


    /**
     * Attaches slurs to the first and the last chord they cover. Slur numbers are reused once a slur has ended.
     *
     * @param chords the chords of the voice
     * @param slurs the slur events of the voice
     */
    private void assignSlurs(List<ChordInfo> chords, List<EventRef> slurs) {
        Fraction[] slotEnds = new Fraction[MAX_SLUR_NUMBER];
        for (EventRef slur : slurs) {
            Fraction start = slur.getPosition();
            Fraction end = start.add(slur.getDuration());

            ChordInfo first = null;
            ChordInfo last = null;
            for (ChordInfo chord : chords) {
                // the slur end is inclusive: it ends on the chord that starts at the end position
                if (chord.getStart().compareTo(start) >= 0 && chord.getStart().compareTo(end) <= 0) {
                    if (first == null) {
                        first = chord;
                    }
                    last = chord;
                }
            }
            if (first == null || first == last) {
                continue;
            }

            int number = 1;
            for (int i = 0; i < slotEnds.length; i++) {
                if (slotEnds[i] == null || slotEnds[i].compareTo(first.getStart()) <= 0) {
                    number = i + 1;
                    slotEnds[i] = last.getStart();
                    break;
                }
            }
            first.addSlurStart(number);
            last.addSlurStop(number);
        }
    }


    /**
     * Builds the notes and rests of all voices of a part in document order, the voices separated by backups.
     *
     * @param bar the bar
     * @param layout the part layout
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @return the content of the voices
     */
    private List<IMusicDataElement> buildMeasureContent(BarInfo bar, PartLayout layout, int divisions, boolean multiStaff) {
        List<IMusicDataElement> notes = new ArrayList<>();
        int barDuration = toDivisions(bar.getLength(), divisions);

        for (int staff = 1; staff <= layout.getStaves(); staff++) {
            List<List<NoteImpl>> voiceContents = new ArrayList<>();
            for (VoiceLayout voice : layout.getVoices(staff)) {
                List<NoteImpl> content = buildVoiceBar(voice, bar, divisions, multiStaff);
                if (!content.isEmpty()) {
                    voiceContents.add(content);
                }
            }
            if (voiceContents.isEmpty()) {
                voiceContents.add(Collections.singletonList(createBarRest(barDuration, staff, (staff - 1) * VOICES_PER_STAFF + 1, multiStaff)));
            }

            for (List<NoteImpl> content : voiceContents) {
                if (!notes.isEmpty()) {
                    BackupImpl backup = new BackupImpl();
                    backup.setDuration(barDuration);
                    notes.add(backup);
                }
                notes.addAll(content);
            }
        }
        return notes;
    }


    /**
     * Builds the notes and rests of one voice in one bar. Notes crossing the bar boundaries or
     * lengths that need several note values are split and tied. Gaps are filled with rests;
     * a primary voice without notes gets a whole-bar rest, a secondary voice without notes is omitted.
     *
     * @param voice the voice
     * @param bar the bar
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @return the notes and rests of the voice in this bar (may be empty)
     */
    private List<NoteImpl> buildVoiceBar(VoiceLayout voice, BarInfo bar, int divisions, boolean multiStaff) {
        List<NoteImpl> result = new ArrayList<>();
        Fraction barStart = bar.getStart();
        Fraction barEnd = barStart.add(bar.getLength());
        Fraction cursor = barStart;

        for (ChordInfo chord : voice.getChords()) {
            if (chord.getEnd().compareTo(barStart) <= 0) {
                continue;
            }
            if (chord.getStart().compareTo(barEnd) >= 0) {
                break;
            }

            Fraction start = chord.getStart().max(cursor);
            Fraction end = chord.getEnd();
            if (end.compareTo(barEnd) > 0) {
                end = barEnd;
            }
            if (end.compareTo(start) <= 0) {
                continue;
            }

            if (start.compareTo(cursor) > 0) {
                addRests(result, voice, bar, cursor, start, divisions, multiStaff);
            }
            if (start.equals(chord.getStart())) {
                addGraceNotes(result, chord, voice, multiStaff);
            }

            for (TimedPiece piece : splitSpan(voice, bar, start, end, true, divisions)) {
                boolean first = piece.getStart().equals(chord.getStart());
                boolean last = piece.getEnd().equals(chord.getEnd());
                for (int n = 0; n < chord.getNotes().size(); n++) {
                    NoteSpec spec = chord.getNotes().get(n);
                    NoteImpl note = createNote(piece, voice, divisions, multiStaff);
                    note.setPitch(spec.getPitch());
                    if (spec.isCue()) {
                        note.setTypeSize("cue");
                    }
                    if (multiStaff && spec.getStaff() != null) {
                        note.setStaff(spec.getStaff());
                    }
                    if (n > 0) {
                        note.setChord("");
                    }

                    NoteMarks marks = new NoteMarks();
                    marks.setTieStop(!first || spec.isTiedFromPrevious());
                    marks.setTieStart(!last || spec.isTiedToNext());
                    if (n == 0) {
                        marks.setTuplet(piece);
                        if (first) {
                            marks.setSlurStarts(chord.getSlurStarts());
                            marks.setChord(chord);
                        }
                        if (last) {
                            marks.setSlurStops(chord.getSlurStops());
                        }
                    }
                    addNotations(note, marks);
                    if (n == 0 && first && !chord.getLyrics().isEmpty()) {
                        note.setLyrics(chord.getLyrics());
                    }
                    result.add(note);
                }
            }
            cursor = end;
        }

        if (result.isEmpty()) {
            if (voice.isPrimary()) {
                result.add(createBarRest(toDivisions(bar.getLength(), divisions), voice.getStaff(), voice.getVoiceNumber(), multiStaff));
            }
            return result;
        }

        if (cursor.compareTo(barEnd) < 0) {
            addRests(result, voice, bar, cursor, barEnd, divisions, multiStaff);
        }
        return result;
    }


    /**
     * Splits a time span of a voice within a bar into notatable pieces. Parts of the span inside a
     * tuplet are notated with the written values of the tuplet; the other parts follow the meter.
     *
     * @param voice the voice
     * @param bar the bar
     * @param from the start of the span (absolute)
     * @param to the end of the span (absolute, not after the bar end)
     * @param note true for a note (dotted values allowed), false for a rest
     * @param divisions the divisions per quarter note
     * @return the pieces in time order
     */
    private List<TimedPiece> splitSpan(VoiceLayout voice, BarInfo bar, Fraction from, Fraction to, boolean note, int divisions) {
        DoricoDurationConverter converter = DoricoDurationConverter.getInstance();
        List<TimedPiece> result = new ArrayList<>();
        Fraction segmentStart = from;
        while (segmentStart.compareTo(to) < 0) {
            TupletInfo tuplet = voice.findTuplet(segmentStart);
            Fraction segmentEnd = to;
            if (tuplet != null) {
                segmentEnd = segmentEnd.min(tuplet.getEnd());
            } else {
                Fraction nextTuplet = voice.findNextTupletStart(segmentStart);
                if (nextTuplet != null) {
                    segmentEnd = segmentEnd.min(nextTuplet);
                }
            }

            if (tuplet == null) {
                List<DoricoDurationConverter.Piece> pieces = splitNote(bar, segmentStart, segmentEnd, note, divisions);
                Fraction position = segmentStart;
                for (DoricoDurationConverter.Piece piece : pieces) {
                    result.add(new TimedPiece(piece, position, position.add(piece.getLength()), null));
                    position = position.add(piece.getLength());
                }
            } else {
                // inside a tuplet the written values are the played lengths times actual / normal
                Fraction ratio = Fraction.of(tuplet.getActual(), tuplet.getNormal());
                Fraction inverse = Fraction.of(tuplet.getNormal(), tuplet.getActual());
                Fraction written = segmentStart.subtract(tuplet.getStart()).multiply(ratio);
                Fraction writtenLength = segmentEnd.subtract(segmentStart).multiply(ratio);
                for (DoricoDurationConverter.Piece piece : converter.split(written, writtenLength, note, TUPLET_GRID)) {
                    Fraction start = tuplet.getStart().add(written.multiply(inverse));
                    written = written.add(piece.getLength());
                    Fraction end = tuplet.getStart().add(written.multiply(inverse));
                    result.add(new TimedPiece(piece, start, end, tuplet));
                }
            }
            segmentStart = segmentEnd;
        }
        return result;
    }


    /**
     * Adds the grace notes of a chord (before the chord).
     *
     * @param result the list receiving the grace notes
     * @param chord the chord
     * @param voice the voice
     * @param multiStaff true if the part has more than one staff
     */
    private void addGraceNotes(List<NoteImpl> result, ChordInfo chord, VoiceLayout voice, boolean multiStaff) {
        for (GraceGroup group : chord.getGraces()) {
            DoricoDurationConverter.Piece piece = DoricoDurationConverter.getInstance().split(Fraction.ZERO, group.getNotatedDuration(), true, TUPLET_GRID).get(0);
            for (int n = 0; n < group.getNotes().size(); n++) {
                NoteSpec spec = group.getNotes().get(n);
                NoteImpl note = new NoteImpl();
                GraceImpl grace = new GraceImpl();
                if (group.isSlashed()) {
                    grace.setSlash("yes");
                }
                note.setGrace(grace);
                note.setPitch(spec.getPitch());
                note.setType(piece.getType());
                if (piece.getDots() > 0) {
                    note.setDots(createDots(piece.getDots()));
                }
                note.setVoice(voice.getVoiceNumber());
                if (multiStaff) {
                    Integer staff = spec.getStaff();
                    if (staff == null) {
                        staff = voice.getStaff();
                    }
                    note.setStaff(staff);
                }
                if (n > 0) {
                    note.setChord("");
                }
                result.add(note);
            }
        }
    }


    /**
     * Splits a note or rest within a bar into notatable pieces. In compound meters (6/8, 9/8,
     * 12/8) the values follow the dotted beat, as Dorico notates them: a part before the next beat
     * is split off, whole beats become dotted values (a dotted half for two beats of a note, a
     * dotted quarter rest per beat) and the rest of a beat is aligned to the beat.
     *
     * @param bar the bar
     * @param start the start of the note or rest part (absolute)
     * @param end the end of the note or rest part (absolute, not after the bar end)
     * @param note true for a note (dotted values allowed), false for a rest
     * @param divisions the divisions per quarter note
     * @return the pieces in time order
     */
    private List<DoricoDurationConverter.Piece> splitNote(BarInfo bar, Fraction start, Fraction end, boolean note, int divisions) {
        DoricoDurationConverter converter = DoricoDurationConverter.getInstance();
        Fraction offset = start.subtract(bar.getStart());
        Fraction remaining = end.subtract(start);
        Fraction beat = compoundBeat(bar.getTime());
        if (beat == null && note) {
            return splitSimpleNote(bar, offset, remaining, divisions);
        }
        if (beat == null) {
            return converter.split(offset, remaining, note, divisions);
        }

        List<DoricoDurationConverter.Piece> result = new ArrayList<>();
        Fraction beatsBefore = offset.multiply(beat.getDenominator(), beat.getNumerator());
        Fraction beatStart = beat.multiply(Math.floorDiv(beatsBefore.getNumerator(), beatsBefore.getDenominator()), 1);
        Fraction offsetInBeat = offset.subtract(beatStart);
        Fraction position = offset;
        if (offsetInBeat.isPositive()) {
            Fraction first = beatStart.add(beat).subtract(offset).min(remaining);
            result.addAll(converter.split(offsetInBeat, first, note, divisions));
            position = position.add(first);
            remaining = remaining.subtract(first);
        }

        while (remaining.compareTo(beat) >= 0) {
            Fraction span = beat;
            for (int beats : COMPOUND_BEAT_GROUPS) {
                Fraction candidate = beat.multiply(beats, 1);
                boolean aligned = note || position.multiply(candidate.getDenominator(), candidate.getNumerator()).getDenominator() == 1;
                if (candidate.compareTo(remaining) <= 0 && aligned && (note || beats <= 2)) {
                    span = candidate;
                    break;
                }
            }
            result.addAll(converter.split(Fraction.ZERO, span, true, divisions));
            position = position.add(span);
            remaining = remaining.subtract(span);
        }

        if (remaining.isPositive()) {
            result.addAll(converter.split(Fraction.ZERO, remaining, note, divisions));
        }
        return result;
    }


    /**
     * Splits a note in a simple meter (4/4, 3/4, 2/2, ...) the way it is usually notated: a note that starts on a
     * beat takes the longest undotted or dotted value; a note that starts off the beat keeps one value if it ends
     * within the beat or if it lasts one beat without crossing the middle of the bar, else it is split at the next
     * beat. The pieces are tied.
     *
     * @param bar the bar
     * @param offset the start of the note relative to the bar start
     * @param length the length of the note
     * @param divisions the divisions per quarter note
     * @return the pieces in time order
     */
    private List<DoricoDurationConverter.Piece> splitSimpleNote(BarInfo bar, Fraction offset, Fraction length, int divisions) {
        DoricoDurationConverter converter = DoricoDurationConverter.getInstance();
        Fraction beat = Fraction.of(4, 1);
        if (bar.getTime() != null && bar.getTime().getBeatType() > 0) {
            beat = Fraction.of(4, bar.getTime().getBeatType());
        }
        Fraction beats = bar.getLength().multiply(beat.getDenominator(), beat.getNumerator());
        Fraction middle = null;
        if (beats.getDenominator() == 1 && beats.getNumerator() % 2 == 0) {
            middle = bar.getLength().multiply(1, 2);
        }

        List<DoricoDurationConverter.Piece> result = new ArrayList<>();
        Fraction position = offset;
        Fraction remaining = length;
        while (remaining.isPositive()) {
            Fraction beatsBefore = position.multiply(beat.getDenominator(), beat.getNumerator());
            Fraction beatStart = beat.multiply(Math.floorDiv(beatsBefore.getNumerator(), beatsBefore.getDenominator()), 1);
            Fraction toBeat = beatStart.add(beat).subtract(position);
            Fraction piece;
            if (position.equals(beatStart)) {
                piece = converter.longestSingle(remaining, divisions);
            } else if (remaining.compareTo(toBeat) <= 0) {
                piece = remaining;
            } else if (remaining.equals(beat) && !crosses(position, position.add(remaining), middle)) {
                piece = remaining;
            } else {
                piece = toBeat;
            }
            if (piece == null) {
                result.addAll(converter.split(position, remaining, true, divisions));
                break;
            }
            DoricoDurationConverter.Piece single = converter.single(piece, divisions);
            if (single != null) {
                result.add(single);
            } else {
                result.addAll(converter.split(position, piece, true, divisions));
            }
            position = position.add(piece);
            remaining = remaining.subtract(piece);
        }
        return result;
    }


    /**
     * Checks whether a span crosses a position.
     *
     * @param from the start of the span
     * @param to the end of the span
     * @param at the position, or null
     * @return true if the position lies inside the span
     */
    private boolean crosses(Fraction from, Fraction to, Fraction at) {
        return at != null && from.compareTo(at) < 0 && to.compareTo(at) > 0;
    }


    /**
     * Gets the beat length of a compound meter (dotted beat), for example a dotted quarter in 6/8.
     *
     * @param time the time signature (may be null)
     * @return the beat length in quarter notes, or null for simple or additive meters
     */
    private Fraction compoundBeat(TimeInfo time) {
        if (time == null || time.getBeatType() < COMPOUND_MIN_BEAT_TYPE) {
            return null;
        }
        int numerator = parseInt(time.getBeats(), 0);
        if (numerator <= COMPOUND_BEATS_PER_GROUP || numerator % COMPOUND_BEATS_PER_GROUP != 0) {
            return null;
        }
        return Fraction.of(COMPOUND_BEATS_PER_GROUP * 4L, time.getBeatType());
    }


    /**
     * Adds rests for a gap in a voice.
     *
     * @param result the list receiving the rests
     * @param voice the voice
     * @param bar the bar
     * @param from the start of the gap (absolute)
     * @param to the end of the gap (absolute)
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     */
    private void addRests(List<NoteImpl> result, VoiceLayout voice, BarInfo bar, Fraction from, Fraction to, int divisions, boolean multiStaff) {
        for (TimedPiece piece : splitSpan(voice, bar, from, to, false, divisions)) {
            NoteImpl rest = createNote(piece, voice, divisions, multiStaff);
            rest.setRest(new RestImpl());
            NoteMarks marks = new NoteMarks();
            marks.setTuplet(piece);
            addNotations(rest, marks);
            result.add(rest);
        }
    }


    /**
     * Creates a note or rest skeleton with duration, type, dots, time modification, voice and staff.
     * The duration is taken from the piece boundaries, so that the pieces of a bar add up exactly.
     *
     * @param piece the notatable piece with its position
     * @param voice the voice
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @return the note
     */
    private NoteImpl createNote(TimedPiece piece, VoiceLayout voice, int divisions, boolean multiStaff) {
        NoteImpl note = new NoteImpl();
        note.setDuration(toDivisions(piece.getEnd(), divisions) - toDivisions(piece.getStart(), divisions));
        note.setType(piece.getPiece().getType());
        if (piece.getPiece().getDots() > 0) {
            note.setDots(createDots(piece.getPiece().getDots()));
        }
        TupletInfo tuplet = piece.getTuplet();
        if (tuplet != null) {
            TimeModificationImpl timeModification = new TimeModificationImpl();
            timeModification.setActualNotes(tuplet.getActual());
            timeModification.setNormalNotes(tuplet.getNormal());
            if (tuplet.getNormalType() != null && !tuplet.getNormalType().equals(piece.getPiece().getType())) {
                timeModification.setNormalType(tuplet.getNormalType());
            }
            note.setTimeModification(timeModification);
        }
        note.setVoice(voice.getVoiceNumber());
        if (multiStaff) {
            note.setStaff(voice.getStaff());
        }
        return note;
    }


    /**
     * Creates the dots of a note.
     *
     * @param count the number of dots
     * @return the dots
     */
    private List<String> createDots(int count) {
        List<String> dots = new ArrayList<>();
        for (int d = 0; d < count; d++) {
            dots.add("");
        }
        return dots;
    }


    /**
     * Adds the notations of a note: ties, slurs, tuplet brackets and, for the first piece of a
     * chord, articulations, ornaments and a fermata.
     *
     * @param note the note
     * @param marks the marks of the note
     */
    private void addNotations(NoteImpl note, NoteMarks marks) {
        NotationsImpl notations = new NotationsImpl();
        boolean used = false;
        if (marks.isTieStart() || marks.isTieStop()) {
            List<TieImpl> ties = new ArrayList<>();
            List<TiedImpl> tieds = new ArrayList<>();
            if (marks.isTieStop()) {
                ties.add(createTie(TYPE_STOP));
                tieds.add(createTied(TYPE_STOP));
            }
            if (marks.isTieStart()) {
                ties.add(createTie(TYPE_START));
                tieds.add(createTied(TYPE_START));
            }
            note.setTies(ties);
            notations.setTieds(tieds);
            used = true;
        }

        if (!marks.getSlurStarts().isEmpty() || !marks.getSlurStops().isEmpty()) {
            List<SlurImpl> slurs = new ArrayList<>();
            for (Integer number : marks.getSlurStops()) {
                slurs.add(createSlur(TYPE_STOP, number));
            }
            for (Integer number : marks.getSlurStarts()) {
                slurs.add(createSlur(TYPE_START, number));
            }
            notations.setSlurs(slurs);
            used = true;
        }

        List<TupletImpl> tuplets = createTupletNotations(marks.getTuplet());
        if (!tuplets.isEmpty()) {
            notations.setTuplets(tuplets);
            used = true;
        }

        ChordInfo chord = marks.getChord();
        if (chord != null) {
            if (chord.getArticulations() != null) {
                notations.setArticulations(chord.getArticulations());
                used = true;
            }
            if (chord.getFingering() != null) {
                PlayingTechniqueImpl technical = new PlayingTechniqueImpl();
                technical.setFingering(chord.getFingering());
                notations.setTechnical(technical);
                used = true;
            }
            if (chord.getOrnaments() != null) {
                notations.setOrnaments(chord.getOrnaments());
                used = true;
            }
            if (chord.isFermata()) {
                notations.setFermatas(Collections.singletonList(createFermata()));
                used = true;
            }
        }

        if (used) {
            note.setNotations(notations);
        }
    }


    /**
     * Creates the tuplet start and stop notations of a piece that starts or ends a tuplet.
     *
     * @param piece the piece (may be null)
     * @return the tuplet notations (may be empty)
     */
    private List<TupletImpl> createTupletNotations(TimedPiece piece) {
        if (piece == null || piece.getTuplet() == null) {
            return Collections.emptyList();
        }
        TupletInfo tuplet = piece.getTuplet();
        List<TupletImpl> result = new ArrayList<>();
        if (piece.getStart().equals(tuplet.getStart())) {
            TupletImpl start = new TupletImpl();
            start.setType(TYPE_START);
            start.setNumber(1);
            if (tuplet.isShowBracket()) {
                start.setBracket("yes");
            } else {
                start.setBracket("no");
            }
            if (!tuplet.isShowNumber()) {
                start.setShowNumber("none");
            }
            result.add(start);
        }
        if (piece.getEnd().equals(tuplet.getEnd())) {
            TupletImpl stop = new TupletImpl();
            stop.setType(TYPE_STOP);
            stop.setNumber(1);
            result.add(stop);
        }
        return result;
    }


    /**
     * Creates a tie (sound).
     *
     * @param type start or stop
     * @return the tie
     */
    private TieImpl createTie(String type) {
        TieImpl tie = new TieImpl();
        tie.setType(type);
        return tie;
    }


    /**
     * Creates a tied notation (visual).
     *
     * @param type start or stop
     * @return the tied notation
     */
    private TiedImpl createTied(String type) {
        TiedImpl tied = new TiedImpl();
        tied.setType(type);
        return tied;
    }


    /**
     * Creates a slur notation.
     *
     * @param type start or stop
     * @param number the slur number
     * @return the slur
     */
    private SlurImpl createSlur(String type, int number) {
        SlurImpl slur = new SlurImpl();
        slur.setType(type);
        slur.setNumber(number);
        return slur;
    }


    /**
     * Creates a barline.
     *
     * @param location left or right
     * @return the barline
     */
    private BarlineImpl createBarline(String location) {
        BarlineImpl barline = new BarlineImpl();
        barline.setLocation(location);
        return barline;
    }


    /**
     * Creates a repeat sign of a barline.
     *
     * @param direction forward or backward
     * @return the repeat
     */
    private RepeatImpl createRepeat(String direction) {
        RepeatImpl repeat = new RepeatImpl();
        repeat.setDirection(direction);
        return repeat;
    }


    /**
     * Creates a repeat ending.
     *
     * @param number the ending number
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
     * Creates an upright fermata.
     *
     * @return the fermata
     */
    private FermataImpl createFermata() {
        FermataImpl fermata = new FermataImpl();
        fermata.setType("upright");
        return fermata;
    }


    /**
     * Creates a direction.
     *
     * @param placement above or below
     * @param staff the staff number, or null for single-staff parts
     * @param types the direction types
     * @return the direction
     */
    private DirectionImpl createDirection(String placement, Integer staff, DirectionTypeImpl... types) {
        DirectionImpl direction = new DirectionImpl();
        direction.setPlacement(placement);
        direction.setDirectionTypes(new ArrayList<>(Arrays.asList(types)));
        if (staff != null) {
            direction.setStaff(staff);
        }
        return direction;
    }


    /**
     * Creates a words direction type.
     *
     * @param words the words
     * @return the direction type
     */
    private DirectionTypeImpl wordsType(List<WordsImpl> words) {
        DirectionTypeImpl type = new DirectionTypeImpl();
        type.setWords(words);
        return type;
    }


    /**
     * Creates the MusicXML clef of a Dorico clef entity.
     *
     * @param clefId the clef entity ID, for example clef.bass
     * @param staffNumber the staff number, or null for single-staff parts
     * @return the clef (a treble clef if the ID is unknown)
     */
    private ClefImpl createClef(String clefId, Integer staffNumber) {
        ClefInfo clefInfo = CLEFS.get(clefId);
        if (clefInfo == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unknown Dorico clef '{}', using treble clef.", clefId);
            }
            clefInfo = CLEFS.get(CLEF_TREBLE);
        }

        ClefImpl clef = new ClefImpl();
        clef.setSign(clefInfo.getSign());
        if (clefInfo.getLine() > 0) {
            clef.setLine(clefInfo.getLine());
        }
        if (clefInfo.getOctaveChange() != 0) {
            clef.setClefOctaveChange(clefInfo.getOctaveChange());
        }
        if (staffNumber != null) {
            clef.setNumber(staffNumber);
        }
        return clef;
    }


    /**
     * Creates a whole-bar rest.
     *
     * @param duration the rest duration in divisions
     * @param staff the staff number (1-based)
     * @param voice the voice number
     * @param multiStaff true if the part has more than one staff
     * @return the rest note
     */
    private NoteImpl createBarRest(int duration, int staff, int voice, boolean multiStaff) {
        RestImpl rest = new RestImpl();
        rest.setMeasure("yes");

        NoteImpl note = new NoteImpl();
        note.setRest(rest);
        note.setDuration(duration);
        note.setVoice(voice);
        // a bar rest is drawn as a whole rest in every meter
        note.setType("whole");
        if (multiStaff) {
            note.setStaff(staff);
        }
        return note;
    }


    /**
     * Splits the notes of a chord that are longer than the shortest note of the chord: the chord ends with its
     * shortest note and the longer notes continue as tied notes in a chord at that end (Dorico stores tied notes
     * as one long note, also when only some notes of a chord are tied).
     *
     * @param chords the chords of a voice by start (updated)
     */
    private void splitLongerNotes(TreeMap<Fraction, ChordInfo> chords) {
        Fraction position = null;
        if (!chords.isEmpty()) {
            position = chords.firstKey();
        }
        while (position != null) {
            ChordInfo chord = chords.get(position);
            Fraction end = null;
            for (NoteSpec note : chord.getNotes()) {
                if (note.getEnd() != null && (end == null || note.getEnd().compareTo(end) < 0)) {
                    end = note.getEnd();
                }
            }
            if (end != null && end.compareTo(chord.getStart()) > 0) {
                chord.setEnd(end);
                for (NoteSpec note : chord.getNotes()) {
                    if (note.getEnd() == null || note.getEnd().compareTo(end) <= 0) {
                        continue;
                    }
                    NoteSpec rest = new NoteSpec(note.getPitch(), note.getStaff());
                    rest.setEnd(note.getEnd());
                    rest.setCue(note.isCue());
                    rest.setTiedFromPrevious(true);
                    rest.setTiedToNext(note.isTiedToNext());
                    note.setTiedToNext(true);
                    note.setEnd(end);
                    ChordInfo next = chords.get(end);
                    if (next == null) {
                        next = new ChordInfo(end, rest.getEnd());
                        chords.put(end, next);
                    }
                    next.addNote(rest);
                }
            }
            position = chords.higherKey(position);
        }
    }


    /**
     * Adds the instrument of a part: the standard MusicXML sound of the Dorico instrument and the General MIDI
     * program of the sound on its own channel (the drum channel 10 is skipped).
     *
     * @param scorePart the score part (updated)
     * @param source the part source
     * @param index the index of the part
     */
    private void addInstrument(ScorePartImpl scorePart, PartSource source, int index) {
        String sound = null;
        if (source.getEntity() != null) {
            sound = nonEmpty(source.getEntity().getPathValue("musicXMLSoundID"));
        }
        if (sound == null) {
            return;
        }
        String instrumentId = scorePart.getId() + "-I1";
        ScoreInstrumentImpl instrument = new ScoreInstrumentImpl();
        instrument.setId(instrumentId);
        instrument.setInstrumentName(scorePart.getPartName());
        instrument.setInstrumentSound(sound);
        scorePart.setScoreInstruments(Collections.singletonList(instrument));
        int program = InstrumentSoundConverter.getInstance().getProgram(sound);
        if (program > 0) {
            int channel = index + 1;
            if (channel >= DRUM_CHANNEL) {
                channel++;
            }
            MidiInstrumentImpl midi = new MidiInstrumentImpl();
            midi.setId(instrumentId);
            midi.setMidiChannel(Math.min(channel, MAX_CHANNEL));
            midi.setMidiProgram(program);
            scorePart.setMidiInstruments(Collections.singletonList(midi));
        }
    }


    /**
     * Gets the fingering of a note from its kNoteFingering property (a list of finger numbers). Dorico counts the
     * fingers from 0, so the stored value 0 is the finger 1.
     *
     * @param value the property value, or null
     * @return the finger numbers separated by spaces, or null if there are none
     */
    private String fingering(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (Integer finger : parseVariantInts(value)) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(finger + 1);
        }
        if (result.length() == 0) {
            return null;
        }
        return result.toString();
    }


    /**
     * Gets the last absolute tempo before a position, as length of a quarter note.
     *
     * @param context the flow context
     * @param position the position
     * @return the length of a quarter note in microseconds, or 0 if there is no absolute tempo before
     */
    private long previousTempo(FlowContext context, Fraction position) {
        long result = 0;
        for (Map.Entry<Fraction, DoricoTreeNode> entry : context.getTempoChanges().headMap(position, false).entrySet()) {
            DoricoTreeNode data = entry.getValue().getChild("data");
            long tempoValue = 0;
            if (data != null) {
                tempoValue = parseLong(data.getPathValue("absoluteTempo", "tempoValue"));
            }
            if (tempoValue > 0) {
                List<Integer> beatUnit = parseVariantInts(data.getPathValue("absoluteTempo", "beatUnits"));
                Fraction beatLength = Fraction.of(1, 1);
                if (beatUnit.size() >= 2) {
                    beatLength = DoricoDurationConverter.getInstance().noteValueToLength(beatUnit.get(0), beatUnit.get(1));
                }
                result = tempoValue * beatLength.getDenominator() / beatLength.getNumerator();
            }
        }
        return result;
    }


    /**
     * Builds the tempo directions that start in a bar.
     *
     * @param bar the bar
     * @param context the flow context
     * @param divisions the divisions per quarter note
     * @return the directions (may be empty)
     */
    private List<DirectionImpl> buildTempoDirections(BarInfo bar, FlowContext context, int divisions) {
        List<DirectionImpl> result = new ArrayList<>();
        Fraction barEnd = bar.getStart().add(bar.getLength());
        for (Map.Entry<Fraction, DoricoTreeNode> entry : context.getTempoChanges().subMap(bar.getStart(), true, barEnd, false).entrySet()) {
            DoricoTreeNode data = entry.getValue().getChild("data");
            if (data == null) {
                continue;
            }

            List<DirectionTypeImpl> directionTypes = new ArrayList<>();
            String text = nonEmpty(data.getPathValue("text"));
            if (text != null) {
                WordsImpl words = new WordsImpl();
                words.setValue(text);
                words.setFontWeight("bold");
                DirectionTypeImpl wordsType = new DirectionTypeImpl();
                wordsType.setWords(Collections.singletonList(words));
                directionTypes.add(wordsType);
            }

            SoundImpl sound = null;
            long tempoValue = parseLong(data.getPathValue("absoluteTempo", "tempoValue"));
            if (tempoValue <= 0 && "kReset".equals(data.getPathValue("type"))) {
                // a tempo (tempo primo): back to the last absolute tempo
                long previous = previousTempo(context, entry.getKey());
                if (previous > 0) {
                    sound = new SoundImpl();
                    sound.setTempo(String.valueOf(Math.round(MICROSECONDS_PER_MINUTE / previous)));
                }
            }
            if (tempoValue > 0) {
                List<Integer> beatUnit = parseVariantInts(data.getPathValue("absoluteTempo", "beatUnits"));
                int noteValue = QUARTER_NOTE_VALUE;
                int dots = 0;
                if (beatUnit.size() >= 2) {
                    noteValue = beatUnit.get(0);
                    dots = beatUnit.get(1);
                }

                // the tempo value is the length of one beat unit in microseconds
                double perMinute = MICROSECONDS_PER_MINUTE / tempoValue;
                MetronomeImpl metronome = new MetronomeImpl();
                metronome.setBeatUnit(DoricoDurationConverter.getInstance().noteValueToType(noteValue));
                if (dots > 0) {
                    metronome.setBeatUnitDot("");
                }
                metronome.setPerMinute(String.valueOf(Math.round(perMinute)));
                DirectionTypeImpl metronomeType = new DirectionTypeImpl();
                metronomeType.setMetronome(metronome);
                directionTypes.add(metronomeType);

                Fraction beatLength = DoricoDurationConverter.getInstance().noteValueToLength(noteValue, dots);
                double quarterPerMinute = perMinute * beatLength.getNumerator() / beatLength.getDenominator();
                sound = new SoundImpl();
                sound.setTempo(String.valueOf(Math.round(quarterPerMinute)));
            }

            if (directionTypes.isEmpty()) {
                continue;
            }

            DirectionImpl direction = new DirectionImpl();
            direction.setPlacement(PLACEMENT_ABOVE);
            direction.setDirectionTypes(directionTypes);
            int offset = toDivisions(entry.getKey().subtract(bar.getStart()), divisions);
            if (offset > 0) {
                direction.setOffset(offset);
            }
            if (sound != null) {
                // the offset of the sound moves the playback, the offset of the direction only its appearance
                if (offset > 0) {
                    sound.setOffset(offset);
                }
                direction.setSound(sound);
            }
            result.add(direction);
        }
        for (EventRef event : context.getGlobalEvents()) {
            if (!GRADUAL_TEMPO.equals(event.getNode().getName()) || event.getPosition().compareTo(bar.getStart()) < 0
                    || event.getPosition().compareTo(barEnd) >= 0) {
                continue;
            }
            String text = nonEmpty(event.getNode().getPathValue("data", "text"));
            if (text == null) {
                continue;
            }
            WordsImpl words = new WordsImpl();
            words.setValue(text);
            words.setFontStyle("italic");
            DirectionTypeImpl wordsType = new DirectionTypeImpl();
            wordsType.setWords(Collections.singletonList(words));
            DirectionImpl direction = new DirectionImpl();
            direction.setPlacement(PLACEMENT_ABOVE);
            direction.setDirectionTypes(Collections.singletonList(wordsType));
            int offset = toDivisions(event.getPosition().subtract(bar.getStart()), divisions);
            if (offset > 0) {
                direction.setOffset(offset);
            }
            result.add(direction);
        }
        return result;
    }


    /**
     * Calculates the written key signature of a part from the concert key signature.
     *
     * @param key the concert key signature
     * @param writtenChromatic the semitones the written pitch lies above the sounding pitch
     * @param writtenDiatonic the diatonic steps the written pitch lies above the sounding pitch
     * @return the written key signature in fifths
     */
    private int writtenFifths(KeyInfo key, int writtenChromatic, int writtenDiatonic) {
        if ("none".equals(key.getMode())) {
            return key.getFifths();
        }
        int fifths = key.getFifths() + DoricoPitchConverter.getInstance().intervalToFifths(writtenChromatic, writtenDiatonic);
        while (fifths > MAX_FIFTHS) {
            fifths -= FIFTHS_CYCLE;
        }
        while (fifths < -MAX_FIFTHS) {
            fifths += FIFTHS_CYCLE;
        }
        return fifths;
    }


    /**
     * Extracts the integers of a Dorico variant list value such as
     * "steamVariantList:list: {list: {int: 8}, {int: 0}}".
     *
     * @param value the value (may be null)
     * @return the integers in order of appearance
     */
    private List<Integer> parseVariantInts(String value) {
        List<Integer> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        Matcher matcher = VARIANT_INT_PATTERN.matcher(value);
        while (matcher.find()) {
            result.add(parseInt(matcher.group(1), 0));
        }
        return result;
    }


    /**
     * Parses a long value.
     *
     * @param value the value (may be null)
     * @return the parsed value, or 0 if parsing fails
     */
    private long parseLong(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    /**
     * Builds the initial clefs of an instrument.
     *
     * @param instrument the score instrument node
     * @param staves the number of staves
     * @return the clefs, one per staff
     */
    private List<ClefImpl> buildClefs(DoricoTreeNode instrument, int staves) {
        List<String> clefIds = new ArrayList<>();
        for (DoricoTreeNode clef : getArrayItems(instrument.getChild("clefs"))) {
            clefIds.add(clef.getPathValue("idForTransposingLayouts"));
        }

        List<ClefImpl> clefs = new ArrayList<>();
        for (int s = 0; s < staves; s++) {
            String clefId = CLEF_TREBLE;
            if (s < clefIds.size() && clefIds.get(s) != null) {
                clefId = clefIds.get(s);
            }
            Integer staffNumber = null;
            if (staves > 1) {
                staffNumber = s + 1;
            }
            clefs.add(createClef(clefId, staffNumber));
        }
        return clefs;
    }


    /**
     * Builds the MusicXML transposition from the library instrument entity. Dorico stores how
     * far the written pitch lies above the sounding pitch; MusicXML stores the interval from
     * written to sounding pitch, so the signs are inverted.
     *
     * @param entity the library instrument entity (may be null)
     * @return the transposition, or null for non-transposing instruments
     */
    private TransposeImpl buildTranspose(DoricoTreeNode entity) {
        if (entity == null) {
            return null;
        }

        int chromatic = -parseInt(entity.getPathValue("pitchedInstrumentData", "transposedChromaticTransposition"), 0);
        int diatonic = -parseInt(entity.getPathValue("pitchedInstrumentData", "transposedDiatonicTransposition"), 0);
        if (chromatic == 0 && diatonic == 0) {
            return null;
        }

        int octaveChange = chromatic / 12;
        TransposeImpl transpose = new TransposeImpl();
        transpose.setChromatic(chromatic - octaveChange * 12);
        transpose.setDiatonic(diatonic - octaveChange * 7);
        if (octaveChange != 0) {
            transpose.setOctaveChange(octaveChange);
        }
        return transpose;
    }


    /**
     * Parses a time signature from a bar division element.
     *
     * @param element the bar division element (may be null)
     * @return the time signature, or null if the element holds none
     */
    private TimeInfo parseTime(DoricoTreeNode element) {
        if (element == null) {
            return null;
        }

        List<DoricoTreeNode> divisions = getArrayItems(element.getPath("barDivisionData", "timeSignature", "timeSignaturesAndDivisions"));
        if (divisions.isEmpty()) {
            return null;
        }

        DoricoTreeNode timeSignature = divisions.get(0).getChild("timeSignature");
        if (timeSignature == null) {
            return null;
        }

        String numerator = nonEmpty(timeSignature.getPathValue("numerator"));
        int denominator = parseInt(timeSignature.getPathValue("denominator"), 0);
        if (numerator == null || denominator <= 0) {
            return null;
        }

        // additive numerators such as "2+3" are summed for the bar length
        long beats = 0;
        for (String part : numerator.split("\\+")) {
            int value = parseInt(part.trim(), 0);
            if (value <= 0) {
                return null;
            }
            beats += value;
        }

        Fraction barLength = Fraction.of(beats, denominator).multiply(QUARTERS_PER_WHOLE);
        return new TimeInfo(numerator, denominator, barLength);
    }


    /**
     * Parses a key signature from a tonality division element.
     *
     * @param element the tonality division element (may be null)
     * @return the key signature, or null if it cannot be determined
     */
    private KeyInfo parseKey(DoricoTreeNode element) {
        if (element == null) {
            return null;
        }

        DoricoTreeNode keySignature = element.getPath("tonalityDivisionData", "keySignature");
        if (keySignature == null) {
            return null;
        }

        String tonalityType = keySignature.getPathValue("root", "tonalityType");
        String noteName = keySignature.getPathValue("root", "root", "noteName");
        String accidentalId = keySignature.getPathValue("root", "root", "accidentalID");

        Integer tonicFifths = null;
        if (noteName != null && NOTE_NAME_FIFTHS.containsKey(noteName)) {
            tonicFifths = NOTE_NAME_FIFTHS.get(noteName) + accidentalFifths(accidentalId);
        }

        if ("kKeySigMajor".equals(tonalityType) && tonicFifths != null && Math.abs(tonicFifths) <= 7) {
            return new KeyInfo(tonicFifths, "major");
        }
        if ("kKeySigMinor".equals(tonalityType) && tonicFifths != null && Math.abs(tonicFifths - 3) <= 7) {
            return new KeyInfo(tonicFifths - 3, "minor");
        }
        if ("kKeySigAtonal".equals(tonalityType)) {
            return new KeyInfo(0, "none");
        }

        // modal or custom key: count the accidentals of the key signature
        int fifths = 0;
        for (DoricoTreeNode accidental : getArrayItems(keySignature.getChild("accidentals"))) {
            fifths += Integer.signum(accidentalFifths(accidental.getPathValue("noteNameAndAccidental", "accidentalID")));
        }
        if (Math.abs(fifths) > 7) {
            return null;
        }
        return new KeyInfo(fifths, null);
    }


    /**
     * Converts an accidental ID into a shift on the circle of fifths.
     *
     * @param accidentalId the accidental ID (may be null)
     * @return the shift (+7 per sharp, -7 per flat)
     */
    private int accidentalFifths(String accidentalId) {
        if (accidentalId == null) {
            return 0;
        }
        for (Map.Entry<String, Integer> entry : ACCIDENTAL_FIFTHS.entrySet()) {
            if (accidentalId.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0;
    }


    /**
     * Gets the items of a Dorico array container.
     *
     * @param node the container node (may be null)
     * @return the items, or an empty list
     */
    private List<DoricoTreeNode> getArrayItems(DoricoTreeNode node) {
        if (node == null) {
            return Collections.emptyList();
        }
        return node.getArrayItems();
    }


    /**
     * Splits a Dorico ID list value ("1000" or "1000,1001").
     *
     * @param value the value (may be null)
     * @return the IDs
     */
    private List<String> splitIds(String value) {
        List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        for (String id : value.split("[,\\s]+")) {
            if (!id.isEmpty()) {
                result.add(id);
            }
        }
        return result;
    }


    /**
     * Parses a rational Dorico position or duration.
     *
     * @param value the value (may be null)
     * @return the fraction, zero if the value is missing or invalid
     */
    private Fraction parseFraction(String value) {
        Fraction result = Fraction.parse(value);
        if (result == null || result.isNegative()) {
            return Fraction.ZERO;
        }
        return result;
    }


    /**
     * Parses a rational Dorico value that may be negative (for example the secondary position of grace notes).
     *
     * @param value the value (may be null)
     * @return the fraction, zero if the value is missing or invalid
     */
    private Fraction parseSignedFraction(String value) {
        Fraction result = Fraction.parse(value);
        if (result == null) {
            return Fraction.ZERO;
        }
        return result;
    }


    /**
     * Gets the value at a path below a node that may be null.
     *
     * @param node the node (may be null)
     * @param names the child names
     * @return the value, or null
     */
    private String getPathValue(DoricoTreeNode node, String... names) {
        if (node == null) {
            return null;
        }
        return node.getPathValue(names);
    }


    /**
     * Parses an integer value.
     *
     * @param value the value (may be null)
     * @param defaultValue the value to use if parsing fails
     * @return the parsed integer
     */
    private int parseInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * Returns the trimmed value or null if it is empty.
     *
     * @param value the value
     * @return the trimmed value or null
     */
    private String nonEmpty(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }


    /**
     * Converts a length in quarter notes into MusicXML divisions.
     *
     * @param quarters the length in quarter notes
     * @param divisions the divisions per quarter note
     * @return the length in divisions
     */
    private int toDivisions(Fraction quarters, int divisions) {
        return (int) (quarters.getNumerator() * divisions / quarters.getDenominator());
    }


    /**
     * Calculates the least common multiple, used for the divisions per quarter note.
     *
     * @param a the first value
     * @param b the second value
     * @return the least common multiple (capped above the allowed maximum)
     */
    private long lcm(long a, long b) {
        if (a <= 0 || b <= 0) {
            return Math.max(a, b);
        }
        long x = a;
        long y = b;
        while (y != 0) {
            long t = x % y;
            x = y;
            y = t;
        }
        long result = (a / x) * b;
        if (result > MAX_DIVISIONS || result < 0) {
            return MAX_DIVISIONS + 1;
        }
        return result;
    }


    /**
     * Reads a ZIP entry with a size limit.
     *
     * @param inputStream the entry stream
     * @param output the stream receiving the data, or null to only consume the entry
     * @return the number of bytes read
     * @throws IOException if reading fails or the size limit is exceeded
     */
    private long readBounded(InputStream inputStream, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            total += len;
            if (total > MAX_ENTRY_SIZE) {
                throw new IOException("Dorico archive entry exceeds maximum size.");
            }
            if (output != null) {
                output.write(buffer, 0, len);
            }
        }
        return total;
    }


    /**
     * Creates the circle of fifths position of the natural note names.
     *
     * @return the map
     */
    private static Map<String, Integer> createNoteNameFifths() {
        Map<String, Integer> map = new HashMap<>();
        map.put(NOTE_F, -1);
        map.put(NOTE_C, 0);
        map.put(NOTE_G, 1);
        map.put("D", 2);
        map.put("A", 3);
        map.put("E", 4);
        map.put("B", 5);
        return map;
    }


    /**
     * Creates the circle of fifths shift of the Dorico accidental ID suffixes.
     *
     * @return the map (longest suffixes first)
     */
    private static Map<String, Integer> createAccidentalFifths() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(".triple-sharp", 21);
        map.put(".triple-flat", -21);
        map.put(".double-sharp", 14);
        map.put(".double-flat", -14);
        map.put(".sharp", 7);
        map.put(".flat", -7);
        map.put(".natural", 0);
        return map;
    }


    /**
     * Creates the mapping from Dorico clef entity IDs to MusicXML clefs.
     *
     * @return the map
     */
    private static Map<String, ClefInfo> createClefs() {
        Map<String, ClefInfo> map = new HashMap<>();
        map.put(CLEF_TREBLE, new ClefInfo(NOTE_G, 2, 0));
        map.put("clef.treble8va", new ClefInfo(NOTE_G, 2, 1));
        map.put("clef.treble8ba", new ClefInfo(NOTE_G, 2, -1));
        map.put("clef.treble15ma", new ClefInfo(NOTE_G, 2, 2));
        map.put("clef.treble15ba", new ClefInfo(NOTE_G, 2, -2));
        map.put("clef.frenchviolin", new ClefInfo(NOTE_G, 1, 0));
        map.put("clef.bass", new ClefInfo(NOTE_F, 4, 0));
        map.put("clef.bass8va", new ClefInfo(NOTE_F, 4, 1));
        map.put("clef.bass8ba", new ClefInfo(NOTE_F, 4, -1));
        map.put("clef.bass15ma", new ClefInfo(NOTE_F, 4, 2));
        map.put("clef.bass15ba", new ClefInfo(NOTE_F, 4, -2));
        map.put("clef.baritone-bass", new ClefInfo(NOTE_F, 3, 0));
        map.put("clef.sub-bass", new ClefInfo(NOTE_F, 5, 0));
        map.put("clef.alto", new ClefInfo(NOTE_C, 3, 0));
        map.put("clef.alto8ba", new ClefInfo(NOTE_C, 3, -1));
        map.put("clef.tenor", new ClefInfo(NOTE_C, 4, 0));
        map.put("clef.tenor8ba", new ClefInfo(NOTE_C, 4, -1));
        map.put("clef.soprano", new ClefInfo(NOTE_C, 1, 0));
        map.put("clef.mezzo-soprano", new ClefInfo(NOTE_C, 2, 0));
        map.put("clef.baritone", new ClefInfo(NOTE_C, 5, 0));
        map.put("clef.percussion", new ClefInfo("percussion", 0, 0));
        map.put("clef.percussion2", new ClefInfo("percussion", 0, 0));
        map.put("clef.percussionindian", new ClefInfo("percussion", 0, 0));
        map.put("clef.tab", new ClefInfo("TAB", 0, 0));
        map.put("clef.tab.4", new ClefInfo("TAB", 0, 0));
        map.put("clef.invisible", new ClefInfo("none", 0, 0));
        return map;
    }
}
