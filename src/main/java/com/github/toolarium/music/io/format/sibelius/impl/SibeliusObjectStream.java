/*
 * SibeliusObjectStream.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl;

import com.github.toolarium.music.io.format.sibelius.impl.dto.SibeliusObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * The bar content stream of a decompressed Sibelius payload. The music is stored as a flat stream of objects. Bar
 * markers separate the bars: first the bars of the system staff (tempo, rehearsal marks, time signatures, system texts),
 * then the bars of each staff in score order. The system staff has one marker less than the number of bars, its first
 * bar starts before the first marker. The header objects of a staff (clef, initial key signature and instrument) stand
 * before its first marker, at the end of the previous staff.
 *
 * <p>Every object starts with its type, the position in the bar (big endian, 1/256 quarter notes) at offset 1 and the
 * voice at offset 19, followed by the byte 0x48, eight zero bytes and 0xFF.</p>
 *
 * @author patrick
 */
public class SibeliusObjectStream {
    /** Type of a note, chord or rest. */
    public static final int NOTE_REST = 0x29;

    /** Type of a bar rest. */
    public static final int BAR_REST = 0x28;

    /** Type of a slur. */
    public static final int SLUR = 0x14;

    /** Type of a crescendo hairpin. */
    public static final int CRESCENDO = 0x0E;

    /** Type of a diminuendo hairpin. */
    public static final int DIMINUENDO = 0x0F;

    /** Type of a tuplet. */
    public static final int TUPLET = 0x18;

    /** Type of a system text (tempo, title page texts, headers). */
    public static final int SYSTEM_TEXT = 0x21;

    /** Type of a staff text (expressions, techniques). */
    public static final int STAFF_TEXT = 0x22;

    /** Type of a rehearsal mark. */
    public static final int REHEARSAL = 0x26;

    /** Type of an instrument (change). */
    public static final int INSTRUMENT = 0x27;

    /** Type of a clef. */
    public static final int CLEF = 0x07;

    /** Type of a special barline. */
    public static final int BARLINE = 0x08;

    /** Type of a time signature. */
    public static final int TIME = 0x09;

    /** Type of the initial key signature of a staff. */
    public static final int INITIAL_KEY = 0x0A;

    /** The position of objects at the end of a bar (staff header objects, barlines). */
    public static final int END_OF_BAR = 0x7F00;

    private static final byte[] BAR_MARKER = {0x10, 0, 0, 0, 0, 0, 0, 0, 0, 0x05, 0, 0, 0, 0, 0, 0, 0, 0, 0x01, 0, 0, 0, 0, 0};
    private static final byte[] KEY_SIGNATURE = {0x03, 0x20, 0x00, 0x01};
    private static final int HEADER_SEARCH = 8000;
    private static final int VOICE_OFFSET = 19;
    private static final int SIGNATURE_OFFSET = 20;
    private static final int SIGNATURE = 0x48;
    private static final int ZERO_BYTES = 8;
    private static final int SIGNATURE_END = 29;
    private static final int ALL_VOICES = 15;
    private static final int MAX_VOICE = 4;
    private static final int KEY_FIFTHS_OFFSET = 57;
    private static final int KEY_SIGNATURE_OFFSET = 58;
    private static final int STRING_START = 56;
    private static final int MAX_STRING_LENGTH = 4000;
    private static final int BYTE_MASK = 0xFF;
    private static final int BYTE_BITS = 8;
    private static final int PRINTABLE = 0x20;
    private static final int UNICODE_NONCHARACTER = 0xFFFE;
    private final byte[] data;
    private final List<SibeliusObject> objects;
    private int staves;
    private int bars;


    /**
     * Constructor for SibeliusObjectStream.
     *
     * @param data the decompressed payload
     */
    public SibeliusObjectStream(byte[] data) {
        this.data = data;
        this.objects = new ArrayList<>();
    }


    /**
     * Parses the bar markers and the objects of the payload.
     *
     * @return true if the stream was found and its structure is consistent
     */
    public boolean parse() {
        List<Integer> markers = findMarkers();
        if (markers.isEmpty()) {
            return false;
        }

        List<Integer> offsets = new ArrayList<>();
        int start = Math.max(0, markers.get(0) - HEADER_SEARCH);
        for (int x = start; x + SIGNATURE_END < data.length; x++) {
            if (isObject(x)) {
                offsets.add(x);
            }
        }

        int clefs = 0;
        for (Integer offset : offsets) {
            if (u8(offset) == CLEF && u16(offset + 1) == END_OF_BAR && offset > markers.get(0)) {
                clefs++;
            }
        }
        if (clefs == 0 || (markers.size() + 1) % (clefs + 1) != 0) {
            return false;
        }
        staves = clefs;
        bars = (markers.size() + 1) / (clefs + 1);

        int region = -1;
        for (Integer offset : offsets) {
            while (region + 1 < markers.size() && markers.get(region + 1) < offset) {
                region++;
            }
            int type = u8(offset);
            int staff;
            int bar;
            if (region <= bars - 2) {
                staff = 0;
                bar = region + 2;
            } else {
                staff = (region - (bars - 1)) / bars + 1;
                bar = (region - (bars - 1)) % bars + 1;
            }
            if (isHeader(offset, type)) {
                staff++;
                bar = 0;
            }
            if (staff <= staves) {
                objects.add(new SibeliusObject(offset, type, staff, bar, u16(offset + 1), u8(offset + VOICE_OFFSET)));
            }
        }
        return true;
    }


    /**
     * Gets the number of staves.
     *
     * @return the number of staves (without the system staff)
     */
    public int getStaves() {
        return staves;
    }


    /**
     * Gets the number of bars.
     *
     * @return the number of bars
     */
    public int getBars() {
        return bars;
    }


    /**
     * Gets the objects in the order of the stream.
     *
     * @return the objects
     */
    public List<SibeliusObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }


    /**
     * Gets the objects of a staff (0 for the system staff) and type.
     *
     * @param staff the staff
     * @param type the type, or -1 for all types
     * @return the objects
     */
    public List<SibeliusObject> getObjects(int staff, int type) {
        List<SibeliusObject> result = new ArrayList<>();
        for (SibeliusObject object : objects) {
            if (object.getStaff() == staff && (type < 0 || object.getType() == type)) {
                result.add(object);
            }
        }
        return result;
    }


    /**
     * Checks whether an object is a key signature.
     *
     * @param object the object
     * @return true for a key signature
     */
    public boolean isKeySignature(SibeliusObject object) {
        int offset = object.getOffset() + KEY_SIGNATURE_OFFSET;
        return offset + KEY_SIGNATURE.length <= data.length
                && Arrays.equals(Arrays.copyOfRange(data, offset, offset + KEY_SIGNATURE.length), KEY_SIGNATURE);
    }


    /**
     * Gets the key of a key signature object in concert pitch.
     *
     * @param object the key signature object
     * @return the number of sharps (positive) or flats (negative)
     */
    public int getKeyFifths(SibeliusObject object) {
        return s8(object.getOffset() + KEY_FIFTHS_OFFSET);
    }


    /**
     * Reads the strings of an object: the text and the style of a text object, or the name, the style and the
     * instrument of an instrument object.
     *
     * @param object the object
     * @return the strings
     */
    public List<String> getStrings(SibeliusObject object) {
        int end = data.length;
        int index = objects.indexOf(object);
        if (index >= 0 && index + 1 < objects.size()) {
            end = Math.max(object.getOffset(), objects.get(index + 1).getOffset());
        }
        return readStrings(object.getOffset() + STRING_START, end, Integer.MAX_VALUE);
    }


    /**
     * Reads the length prefixed (32 bit big endian byte count) UTF-16BE strings of a range.
     *
     * @param start the start offset
     * @param end the end offset
     * @param max the maximum number of strings
     * @return the strings
     */
    public List<String> readStrings(int start, int end, int max) {
        List<String> result = new ArrayList<>();
        int p = start;
        while (p + 4 < end && result.size() < max) {
            int length = stringLength(p, end);
            if (length > 0) {
                result.add(new String(data, p + 4, length, StandardCharsets.UTF_16BE));
                p += 4 + length;
            } else {
                p++;
            }
        }
        return result;
    }


    /**
     * Gets the offset after the first string that starts at or after the given offset.
     *
     * @param start the start offset
     * @return the offset after the string, or -1 if there is none
     */
    public int getStringEnd(int start) {
        for (int p = start; p + 4 < data.length; p++) {
            int length = stringLength(p, data.length);
            if (length > 0) {
                return p + 4 + length;
            }
        }
        return -1;
    }


    /**
     * Finds a UTF-16BE string in the payload.
     *
     * @param text the text
     * @return the offset of the first occurrence, or -1
     */
    public int indexOf(String text) {
        byte[] pattern = text.getBytes(StandardCharsets.UTF_16BE);
        for (int i = 0; i + pattern.length <= data.length; i++) {
            int j = 0;
            while (j < pattern.length && data[i + j] == pattern[j]) {
                j++;
            }
            if (j == pattern.length) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Reads an unsigned byte.
     *
     * @param offset the offset
     * @return the value
     */
    public int u8(int offset) {
        if (offset < 0 || offset >= data.length) {
            return 0;
        }
        return data[offset] & BYTE_MASK;
    }


    /**
     * Reads a signed byte.
     *
     * @param offset the offset
     * @return the value
     */
    public int s8(int offset) {
        if (offset < 0 || offset >= data.length) {
            return 0;
        }
        return data[offset];
    }


    /**
     * Reads an unsigned big endian 16 bit value.
     *
     * @param offset the offset
     * @return the value
     */
    public int u16(int offset) {
        return (u8(offset) << BYTE_BITS) | u8(offset + 1);
    }


    /**
     * Reads a signed big endian 16 bit value.
     *
     * @param offset the offset
     * @return the value
     */
    public int s16(int offset) {
        return (short) u16(offset);
    }


    /**
     * Reads a big endian 32 bit value.
     *
     * @param offset the offset
     * @return the value
     */
    public int u32(int offset) {
        return (u16(offset) << (2 * BYTE_BITS)) | u16(offset + 2);
    }


    /**
     * Gets the length of a string at an offset.
     *
     * @param p the offset of the length
     * @param end the end of the range
     * @return the length in bytes, or 0 if there is no valid string
     */
    private int stringLength(int p, int end) {
        int length = u32(p);
        if (length < 2 || length > MAX_STRING_LENGTH || length % 2 != 0 || p + 4 + length > end) {
            return 0;
        }
        boolean visible = false;
        for (int i = p + 4; i < p + 4 + length; i += 2) {
            int c = u16(i);
            if (c < PRINTABLE || c >= UNICODE_NONCHARACTER || Character.isSurrogate((char) c)) {
                return 0;
            }
            visible |= !Character.isSpaceChar(c);
        }
        if (!visible) {
            return 0;
        }
        return length;
    }


    /**
     * Finds the bar markers.
     *
     * @return the offsets of the bar markers
     */
    private List<Integer> findMarkers() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i + BAR_MARKER.length <= data.length; i++) {
            int j = 0;
            while (j < BAR_MARKER.length && data[i + j] == BAR_MARKER[j]) {
                j++;
            }
            if (j == BAR_MARKER.length) {
                result.add(i);
            }
        }
        return result;
    }


    /**
     * Checks whether an object starts at an offset.
     *
     * @param x the offset
     * @return true if the object signature matches
     */
    private boolean isObject(int x) {
        int voice = u8(x + VOICE_OFFSET);
        if (u8(x + SIGNATURE_OFFSET) != SIGNATURE || u8(x + SIGNATURE_END) != BYTE_MASK) {
            return false;
        }
        if (voice != ALL_VOICES && (voice < 1 || voice > MAX_VOICE)) {
            return false;
        }
        for (int i = 0; i < ZERO_BYTES; i++) {
            if (data[x + SIGNATURE_OFFSET + 1 + i] != 0) {
                return false;
            }
        }
        return true;
    }


    /**
     * Checks whether an object belongs to the header of the next staff.
     *
     * @param offset the offset
     * @param type the type
     * @return true for a clef, an initial key signature or an instrument at the end of a bar
     */
    private boolean isHeader(int offset, int type) {
        return (type == CLEF || type == INITIAL_KEY || type == INSTRUMENT) && u16(offset + 1) == END_OF_BAR;
    }
}
