/*
 * CapBinaryReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl;

import com.github.toolarium.music.io.converter.PitchSpeller;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Reads a legacy binary capella file (.cap, signature {@code cap3-v:}) and converts it into a
 * CapXML document, so that the same score parser handles .cap and .capx files.
 *
 * <p>The file has a header (metadata, layout with staff layouts and brackets, gallery and the
 * background objects) followed by systems ('S'). A system has staves ('V') with time signature and
 * layout index; a staff has voices ('C') with note objects: rests, chords, clefs, keys, meters and
 * barlines. A note pitch is a diatonic step relative to the tonic of the key signature (the tonic
 * of a treble clef lies between C5 and B5 in capella terms), and the alteration is relative to the
 * key signature.</p>
 *
 * @author patrick
 */
final class CapBinaryReader {
    private static final byte[] SIGNATURE = "cap3-v:".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] END = {0, 0, 0, 0};
    private static final String NAMESPACE = "http://www.capella.de/CapXML/2.0";
    private static final int SIGNATURE_LENGTH = 8;
    private static final int OBJ_REST = 0;
    private static final int OBJ_CHORD = 1;
    private static final int OBJ_CLEF = 2;
    private static final int OBJ_KEY = 3;
    private static final int OBJ_METER = 4;
    private static final int OBJ_EXPLICIT_BARLINE = 5;
    private static final int OBJ_IMPLICIT_BARLINE = 6;
    private static final int OBJ_PAGE_BACKGROUND = 7;
    private static final int KEY_OFFSET = 7;
    private static final int TONIC_MIDI = 72;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int STEPS_PER_OCTAVE = 7;
    private static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11};
    private static final String[] BARLINES = {"single", "double", "end", "repEnd", "repStart", "repEndStart", "dashed"};
    private static final String[] ARTICULATIONS = {null, "staccato", "tenuto", "staccTenuto", "staccatissimo", "normalAccent", "strongAccent",
        "weakBeat", "strongBeat"};
    private static final String TRUE = "true";
    private static final String PITCH_STEPS = "CDEFGAB";

    private final CapBinaryInput in;
    private final PitchSpeller speller = PitchSpeller.getInstance();
    private final List<String> layoutNames = new ArrayList<>();
    private final List<String> layoutClefs = new ArrayList<>();
    private final List<Element> backgroundObjects = new ArrayList<>();
    private Document document;
    private int clefForm;
    private int clefOctave;
    private int fifths;


    /**
     * Constructor for CapBinaryReader.
     *
     * @param data the file content
     */
    CapBinaryReader(byte[] data) {
        this.in = new CapBinaryInput(data);
    }


    /**
     * Checks if the content is a legacy capella file.
     *
     * @param data the file content
     * @return true if the content starts with the capella signature
     */
    static boolean isCapFile(byte[] data) {
        return new CapBinaryInput(data).matches(SIGNATURE);
    }


    /**
     * Reads the file and converts it into a CapXML document.
     *
     * @return the score element of the CapXML document
     * @throws IOException if the file is invalid
     */
    Element read() throws IOException {
        if (!in.matches(SIGNATURE)) {
            throw new IOException("Invalid capella file: no cap3 signature.");
        }
        in.skip(SIGNATURE_LENGTH);
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IOException("Cannot create the CapXML document: " + e.getMessage(), e);
        }
        Element score = element(document, "score");
        Element info = element(score, "info");
        String author = in.readString();
        in.readString();
        String comment = in.readString();
        text(info, "author", author);
        text(info, "comment", comment);
        text(info, "encodingSoftware", "capella (cap3)");
        in.readUnsigned();
        in.readUnsigned();
        in.readByte();
        readLayout(element(score, "layout"));

        in.skip(4);
        in.skipExtra();
        readDrawObjects(null);
        int galleryNames = in.readUnsigned();
        for (int i = 0; i < galleryNames; i++) {
            in.readString();
        }
        Element background = document.createElementNS(NAMESPACE, "chord");
        readChord(background);
        Element backgroundDraw = child(background, "drawObjects");
        if (backgroundDraw != null) {
            for (Element drawObj : children(backgroundDraw)) {
                backgroundObjects.add(drawObj);
            }
        }
        in.skip(4);
        in.readFont();
        for (int i = 0; i < 5; i++) {
            in.readUnsigned();
        }
        Element systems = element(score, "systems");
        int systemCount = in.readUnsigned();
        for (int i = 0; i < systemCount; i++) {
            readSystem(element(systems, "system"));
        }
        if (!in.matches(END)) {
            throw new IOException("Invalid capella file: unexpected data at position " + in.getPosition() + ".");
        }
        return score;
    }


    /**
     * Reads the layout: line distances, instrument name font, staff layouts and brackets.
     *
     * @param layout the layout element (filled)
     * @throws IOException if the file is invalid
     */
    private void readLayout(Element layout) throws IOException {
        for (int i = 0; i < 4; i++) {
            in.readInt();
        }
        in.skip(4);
        in.readInt();
        in.readFont();
        Element staves = element(layout, "staves");
        int count = in.readUnsigned();
        for (int i = 0; i < count; i++) {
            readStaffLayout(element(staves, "staffLayout"), i);
        }
        int brackets = in.readUnsigned();
        Element bracketList = element(layout, "brackets");
        for (int i = 0; i < brackets; i++) {
            Element bracket = element(bracketList, "bracket");
            bracket.setAttribute("from", String.valueOf(in.readInt()));
            bracket.setAttribute("to", String.valueOf(in.readInt()));
            bracket.setAttribute("curly", String.valueOf(in.readByte() != 0));
        }
    }


    /**
     * Reads a staff layout: clef, sound, transposition and names.
     *
     * @param staffLayout the staffLayout element (filled)
     * @param index the index of the staff layout
     * @throws IOException if the file is invalid
     */
    private void readStaffLayout(Element staffLayout, int index) throws IOException {
        in.readByte();
        int lines = in.readByte();
        if (lines != 1 && lines != 2) {
            in.skip(11);
        }
        in.readByte();
        in.readInt();
        in.readInt();
        in.readInt();
        in.readByte();
        in.readByte();
        final String clef = clefName(in.readByte());
        int percussion = in.readByte();
        if ((percussion & 0x02) != 0) {
            in.readByte();
            in.skip(in.readByte());
        }
        if ((percussion & 0x04) != 0) {
            in.readByte();
            in.skip(in.readByte());
        }
        final int sound = in.readInt();
        final int volume = in.readInt();
        final int transpose = in.readInt();
        final String description = in.readString();
        final String name = in.readString();
        final String abbreviation = in.readString();
        in.readString();
        in.readString();

        String layoutName = "layout" + index;
        layoutNames.add(layoutName);
        String defaultClef = clef;
        if ((percussion & 0x01) != 0) {
            defaultClef = "percussion";
        }
        layoutClefs.add(defaultClef);
        staffLayout.setAttribute("description", layoutName);
        Element notation = element(staffLayout, "notation");
        notation.setAttribute("defaultClef", defaultClef);
        Element instrument = element(staffLayout, "instrument");
        String instrumentName = name;
        if (instrumentName.trim().isEmpty() && !description.trim().isEmpty() && !description.startsWith("unnamed")) {
            instrumentName = description;
        }
        if (!instrumentName.trim().isEmpty()) {
            instrument.setAttribute("name", instrumentName.trim());
        }
        if (!abbreviation.trim().isEmpty()) {
            instrument.setAttribute("abbrev", abbreviation.trim());
        }
        if (transpose != 0) {
            instrument.setAttribute("transpose", String.valueOf(transpose));
        }
        Element soundElement = element(staffLayout, "sound");
        soundElement.setAttribute("instr", String.valueOf(sound));
        soundElement.setAttribute("volume", String.valueOf(volume));
    }


    /**
     * Reads a system with its staves.
     *
     * @param system the system element (filled)
     * @throws IOException if the file is invalid
     */
    private void readSystem(Element system) throws IOException {
        expect('S', "system");
        in.readInt();
        in.readByte();
        in.readByte();
        in.readByte();
        system.setAttribute("tempo", String.valueOf(in.readUnsigned()));
        in.skipColor();
        in.skipExtra();
        in.readByte();
        Element staves = element(system, "staves");
        int count = in.readUnsigned();
        for (int i = 0; i < count; i++) {
            readStaff(element(staves, "staff"));
        }
    }


    /**
     * Reads a staff with its voices.
     *
     * @param staff the staff element (filled)
     * @throws IOException if the file is invalid
     */
    private void readStaff(Element staff) throws IOException {
        expect('V', "staff");
        final int numerator = in.readByte();
        final int denominator = in.readByte();
        final int layout = in.readByte();
        in.readInt();
        in.readInt();
        in.skipColor();
        in.skipExtra();
        if (layout >= layoutNames.size()) {
            throw new IOException("Invalid capella file: staff layout " + layout + " does not exist.");
        }
        staff.setAttribute("layout", layoutNames.get(layout));
        staff.setAttribute("defaultTime", timeName(numerator, denominator));
        Element voices = element(staff, "voices");
        int count = in.readUnsigned();
        for (int i = 0; i < count; i++) {
            clefOf(layoutClefs.get(layout));
            readVoice(element(element(voices, "voice"), "noteObjects"));
        }
    }


    /**
     * Reads a voice with its note objects.
     *
     * @param noteObjects the noteObjects element (filled)
     * @throws IOException if the file is invalid
     */
    private void readVoice(Element noteObjects) throws IOException {
        expect('C', "voice");
        in.readByte();
        in.readByte();
        in.readFont();
        in.readByte();
        in.skipExtra();
        fifths = 0;
        int count = in.readUnsigned();
        for (int i = 0; i < count; i++) {
            int type = in.readByte();
            in.skipExtra();
            if (type != OBJ_REST && type != OBJ_CHORD && type != OBJ_PAGE_BACKGROUND) {
                in.skipColor();
            }
            switch (type) {
                case OBJ_REST:
                    readRest(noteObjects);
                    break;
                case OBJ_CHORD:
                    Element chord = element(noteObjects, "chord");
                    readChord(chord);
                    if (!backgroundObjects.isEmpty()) {
                        Element drawObjects = child(chord, "drawObjects");
                        if (drawObjects == null) {
                            drawObjects = document.createElementNS(NAMESPACE, "drawObjects");
                            chord.insertBefore(drawObjects, chord.getFirstChild());
                        }
                        for (Element drawObj : backgroundObjects) {
                            drawObjects.appendChild(drawObj);
                        }
                        backgroundObjects.clear();
                    }
                    break;
                case OBJ_CLEF:
                    String clef = clefName(in.readByte());
                    clefOf(clef);
                    element(noteObjects, "clefSign").setAttribute("clef", clef);
                    break;
                case OBJ_KEY:
                    fifths = (in.readByte() & 0x7F) - KEY_OFFSET;
                    if (Math.abs(fifths) > KEY_OFFSET) {
                        throw new IOException("Invalid capella file: invalid key signature " + fifths + " at position " + in.getPosition() + ".");
                    }
                    element(noteObjects, "keySign").setAttribute("fifths", String.valueOf(fifths));
                    break;
                case OBJ_METER:
                    int numerator = in.readByte();
                    int denominator = in.readByte();
                    element(noteObjects, "timeSign").setAttribute("time", timeName(numerator, denominator));
                    break;
                case OBJ_EXPLICIT_BARLINE:
                case OBJ_IMPLICIT_BARLINE:
                    int barline = in.readByte() & 0x0F;
                    Element barlineElement = element(noteObjects, "barline");
                    if (barline < BARLINES.length) {
                        barlineElement.setAttribute("type", BARLINES[barline]);
                    }
                    break;
                case OBJ_PAGE_BACKGROUND:
                    readDrawObjects(null);
                    break;
                default:
                    throw new IOException("Invalid capella file: unknown note object type " + type + " at position " + in.getPosition() + ".");
            }
        }
    }


    /**
     * Reads a rest; a multi-measure rest becomes a whole rest for each measure.
     *
     * @param noteObjects the noteObjects element (filled)
     * @throws IOException if the file is invalid
     */
    private void readRest(Element noteObjects) throws IOException {
        Element rest = element(noteObjects, "rest");
        boolean invisible = readDuration(rest);
        int flags = in.readByte();
        int measures = 0;
        if ((flags & 0x01) != 0) {
            measures = in.readUnsigned();
        }
        if ((flags & 0x04) != 0) {
            in.readInt();
        }
        if (invisible) {
            element(rest, "display").setAttribute("invisible", TRUE);
        }
        if (measures > 0) {
            child(rest, "duration").setAttribute("base", "1/1");
            for (int i = 1; i < measures; i++) {
                noteObjects.appendChild(rest.cloneNode(true));
            }
        }
    }


    /**
     * Reads a chord: duration, beam, stem, articulation, ties, lyrics and notes.
     *
     * @param chord the chord element (filled)
     * @throws IOException if the file is invalid
     */
    private void readChord(Element chord) throws IOException {
        readDuration(chord);
        int flags = in.readByte();
        if ((flags & 0x01) != 0) {
            in.readByte();
        }
        if ((flags & 0x02) != 0) {
            in.readChar();
        }
        if ((flags & 0x04) != 0) {
            int stem = in.readChar();
            in.readChar();
            if (stem == 1) {
                element(chord, "stem").setAttribute("dir", "up");
            } else if (stem == -1) {
                element(chord, "stem").setAttribute("dir", "down");
            }
        }
        if ((flags & 0x08) != 0) {
            in.readByte();
            int articulation = in.readByte();
            if (articulation > 0 && articulation < ARTICULATIONS.length) {
                element(chord, "articulation").setAttribute("type", ARTICULATIONS[articulation]);
            }
        }
        boolean tieStart = false;
        boolean tieEnd = false;
        if ((flags & 0x10) != 0) {
            int ties = in.readByte();
            tieEnd = (ties & 0x01) != 0;
            tieStart = (ties & 0x02) != 0;
        }
        if ((flags & 0x20) != 0) {
            in.readChar();
            in.readChar();
        }
        if ((flags & 0x40) != 0) {
            readLyrics(chord);
        }
        Element heads = element(chord, "heads");
        int notes = in.readUnsigned();
        for (int i = 0; i < notes; i++) {
            readNote(element(heads, "head"), tieStart, tieEnd);
        }
    }


    /**
     * Reads the duration of a rest or chord: dots, grace, invisibility, note value, tuplet and
     * drawing objects.
     *
     * @param object the rest or chord element (filled)
     * @return true if the object is invisible
     * @throws IOException if the file is invalid
     */
    private boolean readDuration(Element object) throws IOException {
        int flags = in.readByte();
        if ((flags & 0x40) != 0) {
            in.skipColor();
        }
        int value = in.readByte();
        Element duration = element(object, "duration");
        duration.setAttribute("base", baseName(value & 0x0F));
        if ((flags & 0x03) != 0) {
            duration.setAttribute("dots", String.valueOf(flags & 0x03));
        }
        if ((flags & 0x04) != 0) {
            duration.setAttribute("noDuration", TRUE);
        }
        if ((value & 0x10) != 0) {
            in.readInt();
        }
        if ((value & 0x20) != 0) {
            int tuplet = in.readByte();
            Element tupletElement = element(duration, "tuplet");
            tupletElement.setAttribute("count", String.valueOf(tuplet & 0x0F));
            tupletElement.setAttribute("tripartite", String.valueOf((tuplet & 0x10) != 0));
            tupletElement.setAttribute("prolong", String.valueOf((tuplet & 0x20) != 0));
        }
        if ((value & 0x40) != 0) {
            readDrawObjects(object);
        }
        return (flags & 0x20) != 0;
    }


    /**
     * Reads the lyrics of a chord.
     *
     * @param chord the chord element (filled)
     * @throws IOException if the file is invalid
     */
    private void readLyrics(Element chord) throws IOException {
        Element lyric = element(chord, "lyric");
        int verses = in.readUnsigned();
        for (int i = 0; i < verses; i++) {
            if (in.readByte() == 0) {
                continue;
            }
            int flags = in.readByte();
            if ((flags & 0x08) != 0) {
                in.readString();
            }
            String text = "";
            if ((flags & 0x10) != 0) {
                text = in.readString();
            }
            Element verse = element(lyric, "verse");
            verse.setAttribute("i", String.valueOf(i));
            verse.setAttribute("hyphen", String.valueOf((flags & 0x04) != 0));
            verse.setAttribute("extender", String.valueOf((flags & 0x02) != 0));
            verse.setTextContent(text);
        }
    }


    /**
     * Reads a note: the diatonic step relative to the tonic of the key, the alteration relative to
     * the key and the explicit accidental flag.
     *
     * @param head the head element (filled)
     * @param tieStart true if the chord has a tie to the next chord
     * @param tieEnd true if the chord has a tie from the previous chord
     * @throws IOException if the file is invalid
     */
    private void readNote(Element head, boolean tieStart, boolean tieEnd) throws IOException {
        int value = in.readChar();
        boolean explicit = false;
        if (((value & 0x80) != 0) != ((value & 0x40) != 0)) {
            explicit = true;
            value = (byte) (value ^ 0x80);
        }
        int flags = in.readByte();
        int relative = ((flags >> 3) & 0x07) - 2;
        if ((flags & 0x40) != 0) {
            explicit = true;
        }

        int tonicPitchClass = Math.floorMod(STEPS_PER_OCTAVE * fifths, SEMITONES_PER_OCTAVE);
        int tonicStep = Math.floorMod(4 * fifths, STEPS_PER_OCTAVE);
        int tonicMidi = TONIC_MIDI + tonicPitchClass;
        int tonicOctave = Math.floorDiv(tonicMidi - MAJOR_SCALE[tonicStep] - speller.keyAlter(stepName(tonicStep), fifths), SEMITONES_PER_OCTAVE) - 1;
        int diatonic = tonicOctave * STEPS_PER_OCTAVE + tonicStep + value + clefShift();
        String step = stepName(Math.floorMod(diatonic, STEPS_PER_OCTAVE));
        int octave = Math.floorDiv(diatonic, STEPS_PER_OCTAVE);
        int alter = speller.keyAlter(step, fifths) + relative;

        head.setAttribute("pitch", step + (octave + 1));
        if (alter != 0 || explicit) {
            Element alterElement = element(head, "alter");
            alterElement.setAttribute("step", String.valueOf(alter));
            if (explicit) {
                alterElement.setAttribute("display", "force");
            }
        }
        if (tieStart || tieEnd) {
            Element tie = element(head, "tie");
            if (tieStart) {
                tie.setAttribute("begin", TRUE);
            }
            if (tieEnd) {
                tie.setAttribute("end", TRUE);
            }
        }
        int shape = flags & 0x07;
        if (shape == 1 || shape == 2) {
            head.setAttribute("shape", "cross");
        } else if (shape == 3) {
            head.setAttribute("shape", "diamond");
        }
    }


    /**
     * Gets the diatonic shift of the current clef: the clef family (G, C or F) and its octave line.
     *
     * @return the shift in diatonic steps
     */
    private int clefShift() {
        int shift = 0;
        if (clefForm == 1) {
            shift = -STEPS_PER_OCTAVE;
        } else if (clefForm == 2) {
            shift = -2 * STEPS_PER_OCTAVE;
        }
        return shift + clefOctave * STEPS_PER_OCTAVE;
    }


    /**
     * Sets the current clef from a CapXML clef name.
     *
     * @param clef the clef name, for example G2, F4- or percussion
     */
    private void clefOf(String clef) {
        clefForm = 0;
        clefOctave = 0;
        if (clef == null || clef.isEmpty()) {
            return;
        }
        if (clef.charAt(0) == 'C') {
            clefForm = 1;
        } else if (clef.charAt(0) == 'F') {
            clefForm = 2;
        }
        if (clef.endsWith("+")) {
            clefOctave = 1;
        } else if (clef.endsWith("-")) {
            clefOctave = -1;
        }
    }


    /**
     * Gets the CapXML clef name of a binary clef: form (bits 0 to 2: G, C, F, percussion), line
     * (bits 3 to 5: 0 is the fifth line) and octave (bits 6 and 7: 0 up, 1 none, 2 down).
     *
     * @param value the clef value
     * @return the clef name, for example G2, F4 or G2-
     */
    private String clefName(int value) {
        int form = value & 0x07;
        int line = 5 - ((value >> 3) & 0x07);
        int octave = (value >> 6) & 0x03;
        if (form > 2) {
            return "percussion";
        }
        String name = String.valueOf("GCF".charAt(form)) + Math.max(1, Math.min(5, line));
        if (octave == 0) {
            name += "+";
        } else if (octave == 2) {
            name += "-";
        }
        return name;
    }


    /**
     * Gets the CapXML time signature of a binary meter.
     *
     * @param numerator the numerator
     * @param denominator the denominator value (bits 0 to 6: log2 of the denominator plus 1, bit 7: alla breve)
     * @return the time signature, for example 4/4 or allaBreve
     */
    private String timeName(int numerator, int denominator) {
        if ((denominator & 0x80) != 0) {
            return "allaBreve";
        }
        int log2 = (denominator & 0x7F) - 1;
        if (log2 < 0 || log2 > 7 || numerator <= 0) {
            return "infinite";
        }
        return numerator + "/" + (1 << log2);
    }


    /**
     * Gets the CapXML duration base of a binary note value (0 whole to 8 256th, 9 breve).
     *
     * @param value the note value
     * @return the base, for example 1/4
     */
    private String baseName(int value) {
        if (value == 9) {
            return "2/1";
        }
        return "1/" + (1 << Math.min(8, value));
    }


    /**
     * Gets the step name of a diatonic index.
     *
     * @param index the index (0 is C)
     * @return the step
     */
    private String stepName(int index) {
        return String.valueOf(PITCH_STEPS.charAt(Math.floorMod(index, STEPS_PER_OCTAVE)));
    }


    /**
     * Reads an array of drawing objects. Slurs, wedges, voltas, trills and texts are added to the
     * drawObjects element of the owner; the other objects are skipped.
     *
     * @param owner the chord or rest element, or null to skip all objects
     * @throws IOException if the file is invalid
     */
    private void readDrawObjects(Element owner) throws IOException {
        int count = in.readUnsigned();
        Element drawObjects = null;
        if (owner != null && count > 0) {
            drawObjects = element(owner, "drawObjects");
        }
        for (int i = 0; i < count; i++) {
            Element shape = readDrawObject();
            if (drawObjects != null && shape != null) {
                Element drawObj = element(drawObjects, "drawObj");
                drawObj.appendChild(shape);
                Element basic = element(drawObj, "basic");
                basic.setAttribute("noteRange", shape.getAttribute("noteRange"));
                shape.removeAttribute("noteRange");
            }
        }
    }


    /**
     * Reads one drawing object.
     *
     * @return the CapXML element of a supported object, or null
     * @throws IOException if the file is invalid
     */
    private Element readDrawObject() throws IOException {
        int type = in.readByte();
        switch (type) {
            case 0:
                readBasic();
                readPoint();
                readDrawObjects(null);
                return null;
            case 1:
                readBasic();
                readPoint();
                in.readByte();
                readDrawObjects(null);
                return null;
            case 2:
                readBasic();
                in.skip(in.readUnsigned());
                return null;
            case 3:
                return readSimpleText();
            case 4:
                return readRichText();
            case 5:
                readLine();
                in.readInt();
                if (in.readByte() != 0) {
                    in.skipColor();
                }
                return null;
            case 6:
                readLine();
                return null;
            case 7:
                readBasic();
                int points = in.readUnsigned();
                for (int i = 0; i < points; i++) {
                    readPoint();
                }
                in.readByte();
                in.readByte();
                in.skipColor();
                return null;
            case 8:
                readLine();
                in.readByte();
                in.readByte();
                return null;
            case 9:
                return readSlur();
            case 10:
                readBasic();
                in.readInt();
                in.readInt();
                in.readInt();
                in.skipColor();
                int lines = in.readByte();
                if (lines != 1 && lines != 2) {
                    in.skip(11);
                }
                return null;
            case 11:
                return readWedge();
            case 12:
                return readVolta();
            case 13:
                readLine();
                in.readByte();
                in.readByte();
                return null;
            case 14:
                readBasic();
                in.skipColor();
                in.readWord();
                in.readDWord();
                return null;
            case 15:
                final Element trill = shape("trill", readBasic());
                in.readInt();
                in.readInt();
                in.readInt();
                in.skipColor();
                in.readByte();
                return trill;
            default:
                throw new IOException("Invalid capella file: unknown drawing object type " + type + " at position " + in.getPosition() + ".");
        }
    }


    /**
     * Reads the header of a drawing object.
     *
     * @return the number of notes the object spans
     * @throws IOException if the file is invalid
     */
    private int readBasic() throws IOException {
        in.readByte();
        in.readByte();
        in.readByte();
        in.readByte();
        in.readInt();
        return in.readWord() & 0xFF;
    }


    /**
     * Reads a point (x and y).
     *
     * @return the y coordinate
     * @throws IOException if the file is invalid
     */
    private int readPoint() throws IOException {
        in.readInt();
        return in.readInt();
    }


    /**
     * Reads a line object (header, two points, color and width).
     *
     * @return the number of notes the object spans
     * @throws IOException if the file is invalid
     */
    private int readLine() throws IOException {
        final int range = readBasic();
        readPoint();
        readPoint();
        in.skipColor();
        in.readByte();
        return range;
    }


    /**
     * Reads a slur.
     *
     * @return the slur element
     * @throws IOException if the file is invalid
     */
    private Element readSlur() throws IOException {
        final Element slur = shape("slur", readBasic());
        for (int i = 0; i < 4; i++) {
            readPoint();
        }
        in.skipColor();
        in.skip(4);
        return slur;
    }


    /**
     * Reads a wedge (hairpin).
     *
     * @return the wedge element
     * @throws IOException if the file is invalid
     */
    private Element readWedge() throws IOException {
        Element wedge = shape("wedge", readLine());
        wedge.setAttribute("decresc", String.valueOf((in.readByte() & 0x80) != 0));
        return wedge;
    }


    /**
     * Reads a volta (repeat ending).
     *
     * @return the volta element
     * @throws IOException if the file is invalid
     */
    private Element readVolta() throws IOException {
        final Element volta = shape("volta", readBasic());
        in.readInt();
        in.readInt();
        in.readInt();
        in.skipColor();
        in.readByte();
        int numbers = in.readByte();
        int first = numbers & 0x0F;
        int last = (numbers >> 4) & 0x0F;
        if (first <= 0) {
            first = 1;
        }
        volta.setAttribute("firstNumber", String.valueOf(first));
        volta.setAttribute("lastNumber", String.valueOf(Math.max(first, last)));
        return volta;
    }


    /**
     * Reads a simple text object.
     *
     * @return the text element
     * @throws IOException if the file is invalid
     */
    private Element readSimpleText() throws IOException {
        Element text = shape("text", readBasic());
        in.readInt();
        int y = in.readInt();
        in.readByte();
        int height = in.readFont();
        String content = in.readString().trim();
        return textElement(text, content, height, y);
    }


    /**
     * Reads a rich text object (RTF).
     *
     * @return the text element
     * @throws IOException if the file is invalid
     */
    private Element readRichText() throws IOException {
        final Element text = shape("text", readBasic());
        in.readInt();
        int y = in.readInt();
        in.readInt();
        in.readInt();
        String rtf = new String(in.readBytes(in.readUnsigned()), StandardCharsets.ISO_8859_1);
        return textElement(text, CapRtfConverter.getInstance().toText(rtf), CapRtfConverter.getInstance().fontSize(rtf), y);
    }


    /**
     * Fills a CapXML text element.
     *
     * @param text the text element
     * @param content the text content
     * @param height the font height in points
     * @param y the vertical position
     * @return the text element, or null for empty text
     */
    private Element textElement(Element text, String content, int height, int y) {
        if (content.isEmpty()) {
            return null;
        }
        text.setAttribute("y", String.valueOf(y));
        element(text, "font").setAttribute("height", String.valueOf(height));
        element(text, "content").setTextContent(content);
        return text;
    }


    /**
     * Creates a drawing object element with its note range.
     *
     * @param name the element name
     * @param range the number of notes the object spans
     * @return the element
     */
    private Element shape(String name, int range) {
        Element element = document.createElementNS(NAMESPACE, name);
        element.setAttribute("noteRange", String.valueOf(range));
        return element;
    }


    /**
     * Checks a one character signature.
     *
     * @param signature the expected character
     * @param description the description for the error message
     * @throws IOException if the signature does not match
     */
    private void expect(char signature, String description) throws IOException {
        int position = in.getPosition();
        if (in.readByte() != signature) {
            throw new IOException("Invalid capella file: no " + description + " signature at position " + position + ".");
        }
    }


    /**
     * Creates a child element.
     *
     * @param parent the parent (document or element)
     * @param name the element name
     * @return the element
     */
    private Element element(Node parent, String name) {
        Element element = document.createElementNS(NAMESPACE, name);
        parent.appendChild(element);
        return element;
    }


    /**
     * Creates a child element with text, if the text is not empty.
     *
     * @param parent the parent
     * @param name the element name
     * @param value the text
     */
    private void text(Element parent, String name, String value) {
        if (value != null && !value.trim().isEmpty()) {
            element(parent, name).setTextContent(value.trim());
        }
    }


    /**
     * Gets the first child element with a name.
     *
     * @param parent the parent
     * @param name the element name
     * @return the child, or null
     */
    private Element child(Element parent, String name) {
        for (Element element : children(parent)) {
            if (name.equals(element.getLocalName())) {
                return element;
            }
        }
        return null;
    }


    /**
     * Gets the child elements.
     *
     * @param parent the parent
     * @return the children
     */
    private List<Element> children(Element parent) {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) {
                result.add((Element) node);
            }
        }
        return result;
    }
}
