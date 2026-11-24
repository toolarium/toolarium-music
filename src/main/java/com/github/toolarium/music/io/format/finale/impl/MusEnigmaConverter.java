/*
 * MusEnigmaConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl;

import com.github.toolarium.music.io.format.finale.impl.dto.MusRecord;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Converts the records of a Finale 2007 to 2012 .mus file into the EnigmaXML structure of a .musx file, so that
 * the .musx reader reads both formats the same way.
 *
 * <p>The others and details pools hold records of the form [tag][key][part][size][0][data][4 bytes]; several
 * records (incis) of one key are stored one after the other in the data of one record. The entries pool holds rows
 * of 38 bytes and the text pool the texts with their commands as control characters. Only the records the reader
 * uses are converted; the records of the linked parts are left out.</p>
 *
 * @author patrick
 */
public final class MusEnigmaConverter {
    private static final Logger LOG = LoggerFactory.getLogger(MusEnigmaConverter.class);
    private static final String NS = "http://www.makemusic.com/2012/finale";
    private static final int OTHERS_HEADER = 10;
    private static final int DETAILS_HEADER = 12;
    private static final int TRAILER = 4;
    private static final int MAX_RECORD_SIZE = 8000;
    private static final int ENTRY_ROW = 38;
    private static final int ENTRY_SHORTS = 19;
    private static final int SLOTS_FIRST_ROW = 2;
    private static final int SLOTS_NEXT_ROW = 5;
    private static final int SLOT_SIZE = 3;
    private static final int YEAR_BASE = 1900;
    private static final int CREATED_BEFORE_APPLICATION = 10;
    private static final int HEADER_SIZE = 512;
    private static final int HEX = 16;
    private static final int DIGITS = 4;
    private static final int LONG_DIGITS = 8;
    private static final int BYTE_MASK = 0xFF;
    private static final int COMMAND_CHAR = 0xC2;
    private static final int COMMAND_FIRST = 0x80;
    private static final int COMMAND_LAST = 0xBF;
    private static final int MAX_DIGIT = 0x10;

    // record tags of the others pool
    private static final int TAG_ARTIC_DEF = 121;
    private static final int TAG_FONT_NAME = 144;
    private static final int TAG_FRAME_SPEC = 146;
    private static final int TAG_INST_USED = 159;
    private static final int TAG_MEAS_SPEC = 176;
    private static final int TAG_MEAS_EXPR_ASSIGN = 177;
    private static final int TAG_TEXT_BLOCK = 183;
    private static final int TAG_PLAYBACK_ROUTE = 185;
    private static final int TAG_PAGE_SPEC = 187;
    private static final int TAG_PAGE_TEXT_ASSIGN = 194;
    private static final int TAG_REPEAT_PASS_LIST = 206;
    private static final int TAG_SMART_SHAPE = 217;
    private static final int TAG_STAFF_SYSTEM_SPEC = 223;
    private static final int TAG_STAFF_SPEC = 231;
    private static final int TAG_TEXT_EXPR_DEF = 241;
    private static final int TAG_PART_DEF = 282;

    // record tags of the details pool
    private static final int TAG_ENTRY_SIZE = 1008;
    private static final int TAG_ARTIC_ASSIGN = 1009;
    private static final int TAG_GFHOLD = 1044;
    private static final int TAG_NOTE_ALTER = 1058;
    private static final int TAG_TUPLET_DEF = 1072;

    // the number of 16 bit values of one inci of the records that hold several
    private static final int FRAME_ITEM = 6;
    private static final int EXPR_ASSIGN_ITEM = 12;
    private static final int PAGE_TEXT_ITEM = 12;
    private static final int INST_USED_ITEM = 12;
    private static final int ARTIC_ASSIGN_ITEM = 10;
    private static final int NOTE_ALTER_ITEM = 10;

    // flags
    private static final int FRAME_START_TIME = 0x0800;
    private static final int MEAS_ENDING = 0x0002;
    private static final int MEAS_BACKWARD_REPEAT = 0x0004;
    private static final int MEAS_FORWARD_REPEAT = 0x0008;
    private static final int MEAS_DISPLAY_TIME = 0x0004;
    private static final int BARLINE_SHIFT = 4;
    private static final int BARLINE_MASK = 0x7;
    private static final int TRANSPOSE_SHIFT = 6;
    private static final int TRANSPOSE_ADJUST_BITS = 6;
    private static final int TRANSPOSE_INTERVAL_BITS = 8;
    private static final int PLAY_TYPE_MASK = 0x7;
    private static final int HPOS_LEFT_SHIFT = 2;
    private static final int HPOS_RIGHT_SHIFT = 4;
    private static final int VPOS_SHIFT = 8;
    private static final int POS_MASK = 0x3;
    private static final int IND_RIGHT_PAGE = 0x0800;
    private static final int ENHARMONIC = 0x0004;
    private static final int ENTRY_VALID = 0x8000;
    private static final int ENTRY_NOTE = 0x4000;
    private static final int ENTRY_FLOAT_REST = 0x0100;
    private static final int ENTRY_GRACE = 0x0080;
    private static final int ENTRY_TUPLET_START = 0x0008;
    private static final int NOTE_VALID = 0x8000;
    private static final int NOTE_TIE_START = 0x4000;
    private static final int NOTE_TIE_END = 0x2000;
    private static final int NOTE_ID_MASK = 0x1F;
    private static final int ALTER_MASK = 0x7;
    private static final int ALTER_SIGN = 0x8;
    private static final int LEVEL_SHIFT = 4;
    private static final int CHAR_OFFSET = 12;

    // element and attribute names used several times
    private static final String CMPER = "cmper";
    private static final String INCI = "inci";
    private static final String ENTNUM = "entnum";
    private static final String NUMBER = "number";
    private static final String END_PT = "endPt";

    private static final String[] BARLINES = {"none", "normal", "double", "heavy", "dashed", "final", "custom", "tick"};
    private static final String[] PLAY_TYPES = {null, "time", "amplitude", "transpose", "dump"};
    private static final String[] HPOS = {"left", "right", "center", "left"};
    private static final String[] VPOS = {null, "bottom", "center", null};
    private static final String[] SHAPE_TYPES = {"slurDown", "slurUp", "decresc", "cresc", "octaveDown", "octaveUp", "dashLineUp",
        "dashLineDown", "dashSlurDown", "dashSlurUp", "dashLine", "solidLine", "solidLineDown", "solidLineUp", "trill", "slurAuto",
        "dashSlurAuto", "trillExt", "solidLineDownBoth", "solidLineUpBoth", "twoOctaveDown", "twoOctaveUp", "dashLineDownBoth",
        "dashLineUpBoth", "glissando", "tabSlide", "bendHat", "bendCurve", "smartLine", "solidLineUpLeft", "solidLineDownLeft",
        "dashLineUpLeft", "dashLineDownLeft", "solidLineUpDown", "solidLineDownUp", "dashLineUpDown", "dashLineDownUp", "hyphen",
        "lyricExt", "wordExt"};
    private static final String[] FILE_INFO = {null, "title", "composer", "copyright", "description", "lyricist", "arranger", "subtitle"};
    private static final Map<Integer, String> COMMANDS = new HashMap<>();
    private static final Map<Integer, Integer> COMMAND_DIGITS = new HashMap<>();

    static {
        // text commands: the control character after the caret, the command and the number of argument digits
        registerCommand(0x84, "nfx", DIGITS);
        registerCommand(0x85, "fontid", DIGITS);
        registerCommand(0x86, "size", DIGITS);
        registerCommand(0x8A, "composer", 0);
        registerCommand(0x8B, "copyright", 0);
        registerCommand(0x92, "flat", 0);
        registerCommand(0x95, "page", LONG_DIGITS);
        registerCommand(0x99, "title", 0);
        registerCommand(0x9B, "perftime", DIGITS);
        registerCommand(0x9C, "cprsym", 0);
        registerCommand(0xA0, "partname", 0);
        registerCommand(0xA2, "arranger", 0);
        registerCommand(0xA3, "subtitle", 0);
        registerCommand(0xA4, "fontTxt", DIGITS);
        registerCommand(0xA5, "fontMus", DIGITS);
        registerCommand(0xA6, "fontNum", DIGITS);
        registerCommand(0xA7, "rehearsal", 0);
    }


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MusEnigmaConverter INSTANCE = new MusEnigmaConverter();
    }


    /**
     * Constructor for MusEnigmaConverter.
     */
    private MusEnigmaConverter() {
        // NOP
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MusEnigmaConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Converts a .mus file into an EnigmaXML document.
     *
     * @param data the raw .mus file bytes
     * @return the EnigmaXML document
     * @throws IOException if the file is not a supported .mus file
     */
    public Document convert(byte[] data) throws IOException {
        Map<Integer, byte[]> pools = MusBinaryParser.getInstance().readPools(data);
        byte[] othersPool = pools.get(0);
        byte[] detailsPool = pools.get(1);
        byte[] entriesPool = pools.get(2);
        if (othersPool == null || detailsPool == null || entriesPool == null) {
            throw new IOException("Required pools not found in .mus file.");
        }
        List<MusRecord> others = readRecords(othersPool, OTHERS_HEADER);
        List<MusRecord> details = readRecords(detailsPool, DETAILS_HEADER);
        if (others.stream().noneMatch(r -> r.getTag() == TAG_MEAS_SPEC)) {
            throw new IOException("No measure records found in .mus file.");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Converting .mus records: {} others, {} details.", others.size(), details.size());
        }

        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            doc = factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IOException("Cannot create the EnigmaXML document.", e);
        }
        Element root = doc.createElementNS(NS, "finale");
        doc.appendChild(root);
        addHeader(root, data);
        Map<Integer, String> fonts = new HashMap<>();
        Element othersElement = element(root, "others");
        for (MusRecord record : others) {
            if (record.getPart() == 0) {
                addOther(othersElement, record, fonts);
            }
        }
        Element detailsElement = element(root, "details");
        for (MusRecord record : details) {
            if (record.getPart() == 0) {
                addDetail(detailsElement, record);
            }
        }
        addEntries(element(root, "entries"), entriesPool);
        byte[] textPool = pools.get(3);
        if (textPool != null) {
            addTexts(element(root, "texts"), textPool, fonts);
        }
        return doc;
    }


    /**
     * Registers a text command.
     *
     * @param code the control character after the caret
     * @param name the command
     * @param digits the number of argument digits, 0 for a command without argument
     */
    private static void registerCommand(int code, String name, int digits) {
        COMMANDS.put(code, name);
        COMMAND_DIGITS.put(code, digits);
    }


    /**
     * Reads the records of the others or details pool. Free space between records is skipped: a record is accepted
     * when the next record follows directly after it (or after the field mask of a linked part record).
     *
     * @param pool the pool data
     * @param headerSize the size of the record header (10 for others, 12 for details)
     * @return the records
     */
    private List<MusRecord> readRecords(byte[] pool, int headerSize) {
        List<MusRecord> result = new ArrayList<>();
        int pos = 0;
        while (pos + headerSize <= pool.length) {
            int size = recordSize(pool, pos, headerSize);
            if (size >= 0) {
                int next = pos + headerSize + size + TRAILER;
                int part = word(pool, pos + headerSize - 6);
                if (next < pool.length && recordSize(pool, next, headerSize) < 0 && part > 0) {
                    // a linked part record can be followed by the mask of its part specific fields
                    next += size;
                }
                if (next >= pool.length || recordSize(pool, next, headerSize) >= 0) {
                    short[] values = new short[size / 2];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = (short) word(pool, pos + headerSize + 2 * i);
                    }
                    int cmper2 = 0;
                    if (headerSize == DETAILS_HEADER) {
                        cmper2 = word(pool, pos + 4);
                    }
                    result.add(new MusRecord(word(pool, pos), word(pool, pos + 2), cmper2, part, values));
                    pos = next;
                    continue;
                }
            }
            pos += 2;
        }
        return result;
    }


    /**
     * Gets the data size of a record header, if the header is valid.
     *
     * @param pool the pool data
     * @param pos the position of the header
     * @param headerSize the size of the record header
     * @return the data size, or -1 if there is no valid header at the position
     */
    private int recordSize(byte[] pool, int pos, int headerSize) {
        if (pos + headerSize > pool.length) {
            return -1;
        }
        int tag = word(pool, pos);
        int size = word(pool, pos + headerSize - 4) & 0xFFFF;
        if (tag <= 0 || size > MAX_RECORD_SIZE || size % 2 != 0 || pos + headerSize + size + TRAILER > pool.length) {
            return -1;
        }
        return size;
    }


    /**
     * Reads a signed little-endian 16 bit value.
     *
     * @param data the data
     * @param pos the position
     * @return the value
     */
    private int word(byte[] data, int pos) {
        return (short) ((data[pos] & BYTE_MASK) | ((data[pos + 1] & BYTE_MASK) << 8));
    }


    /**
     * Adds the header with the creation date: year since 1900, month and day, 10 bytes before the application
     * name (FIN) of the file header.
     *
     * @param root the root element
     * @param data the raw .mus file bytes
     */
    private void addHeader(Element root, byte[] data) {
        int limit = Math.min(data.length, HEADER_SIZE) - DIGITS;
        for (int i = CREATED_BEFORE_APPLICATION; i < limit; i++) {
            if (data[i] == 'F' && data[i + 1] == 'I' && data[i + 2] == 'N' && data[i + 3] == 0) {
                int pos = i - CREATED_BEFORE_APPLICATION;
                Element created = element(element(element(root, "header"), "headerData"), "created");
                value(created, "year", YEAR_BASE + (data[pos] & BYTE_MASK));
                value(created, "month", data[pos + 1] & BYTE_MASK);
                value(created, "day", data[pos + 2] & BYTE_MASK);
                return;
            }
        }
    }


    /**
     * Adds an others record.
     *
     * @param others the others element
     * @param r the record
     * @param fonts the font names by font ID (updated)
     */
    private void addOther(Element others, MusRecord r, Map<Integer, String> fonts) {
        switch (r.getTag()) {
            case TAG_MEAS_SPEC:
                addMeasSpec(others, r);
                break;
            case TAG_STAFF_SPEC:
                addStaffSpec(others, r);
                break;
            case TAG_FRAME_SPEC:
                addFrameSpec(others, r);
                break;
            case TAG_SMART_SHAPE:
                addSmartShape(others, r);
                break;
            case TAG_MEAS_EXPR_ASSIGN:
                addMeasExprAssign(others, r);
                break;
            case TAG_PAGE_TEXT_ASSIGN:
                addPageTextAssign(others, r);
                break;
            case TAG_FONT_NAME:
                fonts.put(r.getCmper1(), fontName(r));
                text(keyed(others, "fontName", r), "name", fontName(r));
                break;
            default:
                addSimpleOther(others, r);
                break;
        }
    }


    /**
     * Adds an others record with a simple layout.
     *
     * @param others the others element
     * @param r the record
     */
    private void addSimpleOther(Element others, MusRecord r) {
        switch (r.getTag()) {
            case TAG_PART_DEF:
                Element part = keyed(others, "partDef", r);
                value(part, "nameID", r.get(0));
                value(part, "partOrder", r.get(1));
                break;
            case TAG_INST_USED:
                for (int i = 0; i < r.size() / INST_USED_ITEM; i++) {
                    Element used = keyed(others, "instUsed", r);
                    used.setAttribute(INCI, String.valueOf(i));
                    value(used, "inst", r.item(i, INST_USED_ITEM).get(0));
                }
                break;
            case TAG_ARTIC_DEF:
                Element artic = keyed(others, "articDef", r);
                value(artic, "charMain", r.get(1));
                optional(artic, "fontMain", r.get(2));
                value(artic, "charAlt", r.get(7));
                optional(artic, "fontAlt", r.get(8));
                break;
            case TAG_TEXT_EXPR_DEF:
                Element def = keyed(others, "textExprDef", r);
                value(def, "textIDKey", r.get(0));
                value(def, "value", r.get(2));
                value(def, "auxdata1", r.get(3));
                text(def, "playType", name(PLAY_TYPES, r.get(5) & PLAY_TYPE_MASK));
                break;
            case TAG_PAGE_SPEC:
                Element page = keyed(others, "pageSpec", r);
                value(page, "height", r.get(0));
                value(page, "width", r.get(2));
                value(page, "firstSystem", r.get(4));
                value(page, "margTop", r.get(6));
                value(page, "margLeft", r.get(7));
                value(page, "margBottom", r.get(8));
                value(page, "margRight", r.get(9));
                value(page, "percent", r.get(10));
                break;
            case TAG_STAFF_SYSTEM_SPEC:
                Element system = keyed(others, "staffSystemSpec", r);
                value(system, "startMeas", r.get(4));
                value(system, "endMeas", r.get(6));
                break;
            case TAG_TEXT_BLOCK:
                value(keyed(others, "textBlock", r), "textID", r.get(0));
                break;
            case TAG_PLAYBACK_ROUTE:
                Element route = keyed(others, "playbackRoute", r);
                value(route, "virtChannel", r.get(0));
                value(route, "patch", r.get(1));
                break;
            case TAG_REPEAT_PASS_LIST:
                Element passes = keyed(others, "repeatPassList", r);
                for (int i = 0; i < r.size() && r.get(i) > 0; i++) {
                    value(passes, "act", r.get(i));
                }
                break;
            default:
                break;
        }
    }


    /**
     * Adds a measure specification: width, key, time signature, barline, repeats and ending.
     *
     * @param others the others element
     * @param r the record
     */
    private void addMeasSpec(Element others, MusRecord r) {
        Element meas = keyed(others, "measSpec", r);
        value(meas, "width", r.get(0));
        value(element(meas, "keySig"), "key", r.get(1));
        value(meas, "beats", r.get(2));
        value(meas, "divbeat", r.get(3) & 0xFFFF);
        value(meas, "dispBeats", r.get(6));
        value(meas, "dispDivbeat", r.get(7) & 0xFFFF);
        flag(meas, "useDisplayTimesig", (r.get(8) & MEAS_DISPLAY_TIME) != 0);
        text(meas, "barline", name(BARLINES, (r.get(5) >> BARLINE_SHIFT) & BARLINE_MASK));
        flag(meas, "forRepBar", (r.get(5) & MEAS_FORWARD_REPEAT) != 0);
        flag(meas, "bacRepBar", (r.get(5) & MEAS_BACKWARD_REPEAT) != 0);
        flag(meas, "barEnding", (r.get(5) & MEAS_ENDING) != 0);
    }


    /**
     * Adds a staff specification: default clef, names and transposition (key adjustment in the lower 6 bits, the
     * diatonic interval above them).
     *
     * @param others the others element
     * @param r the record
     */
    private void addStaffSpec(Element others, MusRecord r) {
        Element staff = keyed(others, "staffSpec", r);
        value(staff, "defaultClef", r.get(6) & BYTE_MASK);
        optional(staff, "fullName", r.get(15));
        optional(staff, "abbrvName", r.get(16));
        int interval = signed((r.get(10) >> TRANSPOSE_SHIFT) & ((1 << TRANSPOSE_INTERVAL_BITS) - 1), TRANSPOSE_INTERVAL_BITS);
        int adjust = signed(r.get(10) & ((1 << TRANSPOSE_ADJUST_BITS) - 1), TRANSPOSE_ADJUST_BITS);
        if (interval != 0 || adjust != 0) {
            Element keySig = element(element(staff, "transposition"), "keysig");
            value(keySig, "interval", interval);
            value(keySig, "adjust", adjust);
        }
    }


    /**
     * Adds the frames of a frame record: start and end entry, or the start time of a frame that starts later.
     *
     * @param others the others element
     * @param r the record
     */
    private void addFrameSpec(Element others, MusRecord r) {
        int items = r.size() / FRAME_ITEM;
        for (int i = 0; i < items; i++) {
            MusRecord item = r.item(i, FRAME_ITEM);
            Element frame = keyed(others, "frameSpec", r);
            frame.setAttribute(INCI, String.valueOf(i));
            if (items > 1 && i == 0 && (item.get(5) & FRAME_START_TIME) != 0) {
                value(frame, "startTime", item.getInt(0));
            } else {
                value(frame, "startEntry", item.getInt(0));
                value(frame, "endEntry", item.getInt(2));
            }
        }
    }


    /**
     * Adds a smart shape: type and the two end points (staff, measure and the entry or the position in EDU).
     *
     * @param others the others element
     * @param r the record
     */
    private void addSmartShape(Element others, MusRecord r) {
        Element shape = keyed(others, "smartShape", r);
        text(shape, "shapeType", name(SHAPE_TYPES, r.get(0)));
        boolean entryBased = (r.get(1) & 1) != 0;
        flag(shape, "entryBased", entryBased);
        endPoint(element(shape, "startTermSeg"), r, 3, entryBased);
        endPoint(element(shape, "endTermSeg"), r, 18, entryBased);
    }


    /**
     * Adds the end point of a smart shape.
     *
     * @param segment the term segment element
     * @param r the record
     * @param index the index of the staff of the end point
     * @param entryBased true for a shape attached to entries
     */
    private void endPoint(Element segment, MusRecord r, int index, boolean entryBased) {
        Element point = element(segment, END_PT);
        value(point, "inst", r.get(index));
        value(point, "meas", r.get(index + 1));
        if (entryBased) {
            value(point, "entryNum", r.getInt(index + 2));
        } else {
            value(point, "edu", r.getInt(index + 2));
        }
    }


    /**
     * Adds the expressions assigned to a measure.
     *
     * @param others the others element
     * @param r the record
     */
    private void addMeasExprAssign(Element others, MusRecord r) {
        for (int i = 0; i < r.size() / EXPR_ASSIGN_ITEM; i++) {
            MusRecord item = r.item(i, EXPR_ASSIGN_ITEM);
            Element assign = keyed(others, "measExprAssign", r);
            assign.setAttribute(INCI, String.valueOf(i));
            value(assign, "textExprID", item.get(0));
            value(assign, "horzEduOff", item.get(1));
            value(assign, "horzEvpuOff", item.get(2));
            value(assign, "vertOff", item.get(3));
            value(assign, "staffAssign", item.get(4));
        }
    }


    /**
     * Adds the texts assigned to pages: position, pages and alignment (bits 2-3 left page, bits 4-5 right page:
     * 0 left, 1 right, 2 center; bits 8-9 vertical: 1 bottom, 2 center).
     *
     * @param others the others element
     * @param r the record
     */
    private void addPageTextAssign(Element others, MusRecord r) {
        for (int i = 0; i < r.size() / PAGE_TEXT_ITEM; i++) {
            MusRecord item = r.item(i, PAGE_TEXT_ITEM);
            if (item.get(0) == 0) {
                continue;
            }
            Element assign = keyed(others, "pageTextAssign", r);
            assign.setAttribute(INCI, String.valueOf(i));
            value(assign, "block", item.get(0));
            value(assign, "xdisp", item.get(1));
            value(assign, "ydisp", item.get(2));
            value(assign, "startPage", item.get(3));
            value(assign, "endPage", item.get(4));
            int flags = item.get(5);
            text(assign, "hposLp", HPOS[(flags >> HPOS_LEFT_SHIFT) & POS_MASK]);
            text(assign, "hposRp", HPOS[(flags >> HPOS_RIGHT_SHIFT) & POS_MASK]);
            text(assign, "vpos", VPOS[(flags >> VPOS_SHIFT) & POS_MASK]);
            flag(assign, "indRpPos", (flags & IND_RIGHT_PAGE) != 0);
            value(assign, "rightPgXdisp", item.get(6));
            value(assign, "rightPgYdisp", item.get(7));
        }
    }


    /**
     * Gets the font name of a font record: a null terminated string from byte 12.
     *
     * @param r the record
     * @return the font name
     */
    private String fontName(MusRecord r) {
        byte[] bytes = r.getBytes();
        int end = CHAR_OFFSET;
        while (end < bytes.length && bytes[end] != 0) {
            end++;
        }
        if (end <= CHAR_OFFSET) {
            return "";
        }
        return new String(bytes, CHAR_OFFSET, end - CHAR_OFFSET, Charset.forName("windows-1252"));
    }


    /**
     * Adds a details record: frames of a staff and measure, articulations, note alterations, entry size and tuplets.
     *
     * @param details the details element
     * @param r the record
     */
    private void addDetail(Element details, MusRecord r) {
        switch (r.getTag()) {
            case TAG_GFHOLD:
                Element hold = element(details, "gfhold");
                hold.setAttribute("cmper1", String.valueOf(r.getCmper1()));
                hold.setAttribute("cmper2", String.valueOf(r.getCmper2()));
                value(hold, "clefID", r.get(0));
                for (int layer = 1; layer <= DIGITS; layer++) {
                    optional(hold, "frame" + layer, r.get(2 + layer));
                }
                break;
            case TAG_ARTIC_ASSIGN:
                for (int i = 0; i < r.size() / ARTIC_ASSIGN_ITEM; i++) {
                    MusRecord item = r.item(i, ARTIC_ASSIGN_ITEM);
                    Element assign = entryDetail(details, "articAssign", r, i);
                    value(assign, "articDef", item.get(0));
                    value(assign, "horzOff", item.get(1));
                    value(assign, "vertOff", item.get(3));
                }
                break;
            case TAG_NOTE_ALTER:
                for (int i = 0; i < r.size() / NOTE_ALTER_ITEM; i++) {
                    MusRecord item = r.item(i, NOTE_ALTER_ITEM);
                    Element alter = entryDetail(details, "noteAlter", r, i);
                    value(alter, "noteID", item.get(0));
                    value(alter, "percent", item.get(1));
                    flag(alter, "enharmonic", (item.get(4) & ENHARMONIC) != 0);
                }
                break;
            case TAG_ENTRY_SIZE:
                value(entryDetail(details, "entrySize", r, -1), "percent", r.get(1));
                break;
            case TAG_TUPLET_DEF:
                Element tuplet = entryDetail(details, "tupletDef", r, 0);
                value(tuplet, "symbolicNum", r.get(0));
                value(tuplet, "symbolicDur", r.get(1));
                value(tuplet, "refNum", r.get(2));
                value(tuplet, "refDur", r.get(3));
                break;
            default:
                break;
        }
    }


    /**
     * Adds an entry detail element.
     *
     * @param details the details element
     * @param name the element name
     * @param r the record
     * @param inci the inci, or -1 for none
     * @return the element
     */
    private Element entryDetail(Element details, String name, MusRecord r, int inci) {
        Element result = element(details, name);
        result.setAttribute(ENTNUM, String.valueOf(r.getEntryNumber()));
        if (inci >= 0) {
            result.setAttribute(INCI, String.valueOf(inci));
        }
        return result;
    }


    /**
     * Adds the entries: rows of 38 bytes, the first row of an entry with the entry data and two note slots,
     * further rows with five more note slots.
     *
     * @param entries the entries element
     * @param pool the entries pool
     */
    private void addEntries(Element entries, byte[] pool) {
        Map<Integer, List<int[]>> notes = new TreeMap<>();
        Map<Integer, Element> elements = new TreeMap<>();
        for (int pos = 0; pos + ENTRY_ROW <= pool.length; pos += ENTRY_ROW) {
            int[] s = new int[ENTRY_SHORTS];
            for (int i = 0; i < ENTRY_SHORTS; i++) {
                s[i] = word(pool, pos + 2 * i);
            }
            int entnum = (s[0] & 0xFFFF) | (s[1] << HEX);
            if (entnum == 0) {
                continue;
            }
            int firstSlot = 3;
            int slots = SLOTS_NEXT_ROW;
            if (s[2] == 0) {
                elements.put(entnum, entry(entries, entnum, s));
                firstSlot = 13;
                slots = SLOTS_FIRST_ROW;
            }
            List<int[]> list = notes.computeIfAbsent(entnum, k -> new ArrayList<>());
            for (int i = 0; i < slots; i++) {
                int at = firstSlot + SLOT_SIZE * i;
                list.add(new int[] {s[at], s[at + 1], s[at + 2]});
            }
        }
        for (Map.Entry<Integer, Element> e : elements.entrySet()) {
            int count = Integer.parseInt(e.getValue().getAttribute("numNotesCount"));
            e.getValue().removeAttribute("numNotesCount");
            List<int[]> slots = notes.get(e.getKey());
            for (int i = 0; i < count && i < slots.size(); i++) {
                addNote(e.getValue(), slots.get(i));
            }
        }
    }


    /**
     * Creates an entry element from its first row.
     *
     * @param entries the entries element
     * @param entnum the entry number
     * @param s the values of the first row
     * @return the entry element (with the number of notes as temporary attribute)
     */
    private Element entry(Element entries, int entnum, int[] s) {
        Element entry = element(entries, "entry");
        entry.setAttribute(ENTNUM, String.valueOf(entnum));
        entry.setAttribute("prev", String.valueOf((s[3] & 0xFFFF) | (s[4] << HEX)));
        entry.setAttribute("next", String.valueOf((s[5] & 0xFFFF) | (s[6] << HEX)));
        int flags = s[10];
        value(entry, "dura", s[7] & 0xFFFF);
        value(entry, "numNotes", s[12]);
        flag(entry, "isValid", (flags & ENTRY_VALID) != 0);
        flag(entry, "isNote", (flags & ENTRY_NOTE) != 0);
        flag(entry, "graceNote", (flags & ENTRY_GRACE) != 0);
        flag(entry, "floatRest", (flags & ENTRY_FLOAT_REST) != 0);
        flag(entry, "tupletStart", (flags & ENTRY_TUPLET_START) != 0);
        entry.setAttribute("numNotesCount", String.valueOf(Math.max(0, s[12])));
        return entry;
    }


    /**
     * Adds a note: pitch (diatonic level in the upper 12 bits, alteration in the lower 4 bits), note ID and ties.
     *
     * @param entry the entry element
     * @param slot the note slot: pitch, low flags, high flags
     */
    private void addNote(Element entry, int[] slot) {
        Element note = element(entry, "note");
        note.setAttribute("id", String.valueOf(slot[2] & NOTE_ID_MASK));
        int alter = slot[0] & ALTER_MASK;
        if ((slot[0] & ALTER_SIGN) != 0) {
            alter = -alter;
        }
        value(note, "harmLev", slot[0] >> LEVEL_SHIFT);
        value(note, "harmAlt", alter);
        flag(note, "isValid", (slot[2] & NOTE_VALID) != 0);
        flag(note, "tieStart", (slot[2] & NOTE_TIE_START) != 0);
        flag(note, "tieEnd", (slot[2] & NOTE_TIE_END) != 0);
    }


    /**
     * Adds the texts: "^block(n)...^end", "^expression(n)...^end" and "^fileInfo(n)...^end".
     *
     * @param texts the texts element
     * @param pool the text pool
     * @param fonts the font names by font ID
     */
    private void addTexts(Element texts, byte[] pool, Map<Integer, String> fonts) {
        Map<Integer, String> expressions = new TreeMap<>();
        Map<Integer, String> blocks = new TreeMap<>();
        int pos = 0;
        while (pos < pool.length) {
            int start = indexOf(pool, '^', pos);
            if (start < 0) {
                break;
            }
            int open = indexOf(pool, '(', start);
            int close = indexOf(pool, ')', start);
            int end = indexOf(pool, "^end", start + 1);
            if (open < 0 || close < 0 || end < 0 || close > end) {
                break;
            }
            String type = new String(pool, start + 1, open - start - 1, StandardCharsets.US_ASCII);
            int number = parseNumber(new String(pool, open + 1, close - open - 1, StandardCharsets.US_ASCII));
            String content = decodeText(pool, close + 1, end, fonts);
            if ("block".equals(type)) {
                blocks.put(number, content);
            } else if ("expression".equals(type)) {
                expressions.put(number, content);
            } else if ("fileInfo".equals(type) && number > 0 && number < FILE_INFO.length) {
                Element info = element(texts, "fileInfo");
                info.setAttribute("type", FILE_INFO[number]);
                info.setTextContent(content);
            }
            pos = end + DIGITS;
        }
        for (Map.Entry<Integer, String> block : blocks.entrySet()) {
            Element element = element(texts, "blockText");
            element.setAttribute(NUMBER, String.valueOf(block.getKey()));
            element.setTextContent(block.getValue());
        }
        for (Map.Entry<Integer, String> expression : expressions.entrySet()) {
            Element element = element(texts, "expression");
            element.setAttribute(NUMBER, String.valueOf(expression.getKey()));
            element.setTextContent(expression.getValue());
        }
    }


    /**
     * Decodes the content of a text: the characters (UTF-8, or Windows-1252 for older files) and the commands
     * (a caret, a control character and the argument as hexadecimal digits, each stored as digit + 1).
     *
     * @param pool the text pool
     * @param from the start of the content
     * @param to the end of the content
     * @param fonts the font names by font ID
     * @return the text with the commands in EnigmaXML notation (^name(argument))
     */
    private String decodeText(byte[] pool, int from, int to, Map<Integer, String> fonts) {
        StringBuilder result = new StringBuilder();
        ByteArrayOutputStream characters = new ByteArrayOutputStream();
        int pos = from;
        while (pos < to) {
            int b = pool[pos] & BYTE_MASK;
            int code = 0;
            int length = 2;
            if (b == '^' && pos + 2 < to && (pool[pos + 1] & BYTE_MASK) == COMMAND_CHAR) {
                // Finale 2012: the control character in UTF-8
                code = pool[pos + 2] & BYTE_MASK;
                length = 3;
            } else if (b == '^' && pos + 1 < to) {
                // older versions: the control character as one byte
                code = pool[pos + 1] & BYTE_MASK;
            }
            if (code < COMMAND_FIRST || code > COMMAND_LAST) {
                characters.write(b);
                pos++;
                continue;
            }
            result.append(characters(characters.toByteArray()));
            characters.reset();
            pos += length;
            String name = COMMANDS.get(code);
            int digits = COMMAND_DIGITS.getOrDefault(code, 0);
            if (name == null && pos < to && pool[pos] == 1) {
                // an unknown command with an argument
                digits = DIGITS;
            }
            long argument = 0;
            for (int i = 0; i < digits && pos < to; i++) {
                argument = argument * HEX + Math.max(0, (pool[pos] & BYTE_MASK) - 1);
                pos++;
            }
            if (name != null) {
                result.append(command(name, digits, argument, fonts));
            }
        }
        result.append(characters(characters.toByteArray()));
        // Finale stores line breaks as carriage return and line feed
        return result.toString().replace("\r\n", "\n").replace('\r', '\n');
    }


    /**
     * Formats a text command in EnigmaXML notation.
     *
     * @param name the command
     * @param digits the number of argument digits (0 for no argument)
     * @param argument the argument
     * @param fonts the font names by font ID
     * @return the command
     */
    private String command(String name, int digits, long argument, Map<Integer, String> fonts) {
        if (digits == 0) {
            return "^" + name + "()";
        }
        if (name.startsWith("font") && !"fontid".equals(name)) {
            return "^" + name + "(" + fonts.getOrDefault((int) argument, "Font" + argument) + ",0)";
        }
        return "^" + name + "(" + argument + ")";
    }


    /**
     * Decodes characters: UTF-8 if valid, else Windows-1252.
     *
     * @param bytes the bytes
     * @return the characters
     */
    private String characters(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }


    /**
     * Finds a byte.
     *
     * @param data the data
     * @param b the byte
     * @param from the start position
     * @return the position, or -1 if not found
     */
    private int indexOf(byte[] data, char b, int from) {
        for (int i = from; i < data.length; i++) {
            if (data[i] == b) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Finds a text.
     *
     * @param data the data
     * @param text the text (ASCII)
     * @param from the start position
     * @return the position, or -1 if not found
     */
    private int indexOf(byte[] data, String text, int from) {
        for (int i = from; i + text.length() <= data.length; i++) {
            boolean found = true;
            for (int j = 0; j < text.length() && found; j++) {
                found = data[i + j] == text.charAt(j);
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Parses a number.
     *
     * @param text the text
     * @return the number, or 0 if it is no number
     */
    private int parseNumber(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    /**
     * Sign-extends a bit field.
     *
     * @param value the value of the bit field
     * @param bits the number of bits
     * @return the signed value
     */
    private int signed(int value, int bits) {
        if ((value & (1 << (bits - 1))) != 0) {
            return value - (1 << bits);
        }
        return value;
    }


    /**
     * Gets a name from a table.
     *
     * @param names the names by value
     * @param index the value
     * @return the name, or null if the value is out of the table
     */
    private String name(String[] names, int index) {
        if (index < 0 || index >= names.length) {
            return null;
        }
        return names[index];
    }


    /**
     * Adds an element for a record with a cmper attribute.
     *
     * @param parent the parent element
     * @param name the element name
     * @param r the record
     * @return the element
     */
    private Element keyed(Element parent, String name, MusRecord r) {
        Element result = element(parent, name);
        result.setAttribute(CMPER, String.valueOf(r.getCmper1()));
        return result;
    }


    /**
     * Adds an element.
     *
     * @param parent the parent element
     * @param name the element name
     * @return the element
     */
    private Element element(Element parent, String name) {
        Element result = parent.getOwnerDocument().createElementNS(NS, name);
        parent.appendChild(result);
        return result;
    }


    /**
     * Adds an element with a number.
     *
     * @param parent the parent element
     * @param name the element name
     * @param value the number
     */
    private void value(Element parent, String name, long value) {
        element(parent, name).setTextContent(String.valueOf(value));
    }


    /**
     * Adds an element with a number if the number is not 0.
     *
     * @param parent the parent element
     * @param name the element name
     * @param value the number
     */
    private void optional(Element parent, String name, int value) {
        if (value != 0) {
            value(parent, name, value);
        }
    }


    /**
     * Adds an element with a text, if there is a text.
     *
     * @param parent the parent element
     * @param name the element name
     * @param value the text, or null
     */
    private void text(Element parent, String name, String value) {
        if (value != null) {
            element(parent, name).setTextContent(value);
        }
    }


    /**
     * Adds an empty element (a set flag).
     *
     * @param parent the parent element
     * @param name the element name
     * @param set true to add the element
     */
    private void flag(Element parent, String name, boolean set) {
        if (set) {
            element(parent, name);
        }
    }
}
