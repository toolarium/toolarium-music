/*
 * CapBinaryInput.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Reads the primitives of legacy capella files (.cap): bytes, variable length unsigned and signed
 * integers, length prefixed strings, colors and fonts.
 *
 * <p>An unsigned value is one byte; 254 is followed by a 16 bit value and 255 by a 32 bit value. A
 * signed value is one byte; -128 is followed by a 16 bit value and 127 by a 32 bit value. Multi-byte
 * values are little-endian.</p>
 *
 * @author patrick
 */
final class CapBinaryInput {
    private static final int BYTE_MASK = 0xFF;
    private static final int UNSIGNED_16 = 254;
    private static final int UNSIGNED_32 = 255;
    private static final int SIGNED_16 = -128;
    private static final int SIGNED_32 = 127;
    private static final int RGB_COLOR = 0xFF;
    private static final int LOGFONT_SIZE = 28;
    private static final int MAX_LENGTH = 16 * 1024 * 1024;
    private static final int FONT_HEIGHT_SCALE = 1000;
    private final byte[] data;
    private final List<Integer> fontHeights = new ArrayList<>();
    private int position;


    /**
     * Constructor for CapBinaryInput.
     *
     * @param data the file content
     */
    CapBinaryInput(byte[] data) {
        this.data = data;
    }


    /**
     * Gets the current position.
     *
     * @return the position
     */
    int getPosition() {
        return position;
    }


    /**
     * Reads an unsigned byte.
     *
     * @return the value (0 to 255)
     * @throws IOException at the end of the data
     */
    int readByte() throws IOException {
        ensure(1);
        return data[position++] & BYTE_MASK;
    }


    /**
     * Reads a signed byte.
     *
     * @return the value (-128 to 127)
     * @throws IOException at the end of the data
     */
    int readChar() throws IOException {
        ensure(1);
        return data[position++];
    }


    /**
     * Reads a little-endian 16 bit unsigned value.
     *
     * @return the value
     * @throws IOException at the end of the data
     */
    int readWord() throws IOException {
        ensure(2);
        int value = (data[position] & BYTE_MASK) | ((data[position + 1] & BYTE_MASK) << 8);
        position += 2;
        return value;
    }


    /**
     * Reads a little-endian 32 bit value.
     *
     * @return the value
     * @throws IOException at the end of the data
     */
    int readDWord() throws IOException {
        int low = readWord();
        int high = readWord();
        return low | (high << 16);
    }


    /**
     * Reads a variable length unsigned value.
     *
     * @return the value
     * @throws IOException at the end of the data
     */
    int readUnsigned() throws IOException {
        int value = readByte();
        if (value == UNSIGNED_16) {
            return readWord();
        }
        if (value == UNSIGNED_32) {
            return readDWord();
        }
        return value;
    }


    /**
     * Reads a variable length signed value.
     *
     * @return the value
     * @throws IOException at the end of the data
     */
    int readInt() throws IOException {
        int value = readChar();
        if (value == SIGNED_16) {
            return (short) readWord();
        }
        if (value == SIGNED_32) {
            return readDWord();
        }
        return value;
    }


    /**
     * Reads a length prefixed string (ISO-8859-1).
     *
     * @return the string
     * @throws IOException at the end of the data or for an invalid length
     */
    String readString() throws IOException {
        return new String(readBytes(readUnsigned()), StandardCharsets.ISO_8859_1);
    }


    /**
     * Reads bytes.
     *
     * @param length the number of bytes
     * @return the bytes
     * @throws IOException at the end of the data or for an invalid length
     */
    byte[] readBytes(int length) throws IOException {
        if (length < 0 || length > MAX_LENGTH) {
            throw new IOException("Invalid length " + length + " at position " + position + ".");
        }
        ensure(length);
        byte[] result = new byte[length];
        System.arraycopy(data, position, result, 0, length);
        position += length;
        return result;
    }


    /**
     * Skips bytes.
     *
     * @param count the number of bytes
     * @throws IOException if the data ends before
     */
    void skip(int count) throws IOException {
        if (count < 0) {
            throw new IOException("Invalid skip " + count + " at position " + position + ".");
        }
        ensure(count);
        position += count;
    }


    /**
     * Skips an extension block: a byte count followed by the bytes.
     *
     * @throws IOException at the end of the data
     */
    void skipExtra() throws IOException {
        skip(readByte());
    }


    /**
     * Skips a color: an index into the standard colors, or 255 followed by red, green and blue.
     *
     * @throws IOException at the end of the data
     */
    void skipColor() throws IOException {
        if (readByte() == RGB_COLOR) {
            skip(3);
        }
    }


    /**
     * Reads a font: the index of a font read before (1-based), or 0 followed by a new font (Windows
     * LOGFONT values, color and face name).
     *
     * @return the font height in points (0 if unknown)
     * @throws IOException at the end of the data or for an invalid font index
     */
    int readFont() throws IOException {
        int index = readUnsigned();
        if (index > 0) {
            if (index > fontHeights.size()) {
                throw new IOException("Invalid font index " + index + " at position " + position + ".");
            }
            return fontHeights.get(index - 1);
        }
        final int height = Math.abs(readDWord()) / FONT_HEIGHT_SCALE;
        skip(LOGFONT_SIZE - 4);
        skipColor();
        readString();
        fontHeights.add(height);
        return height;
    }


    /**
     * Checks if the data has a signature at the current position.
     *
     * @param signature the expected bytes
     * @return true if the bytes match
     */
    boolean matches(byte[] signature) {
        if (position + signature.length > data.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[position + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }


    /**
     * Checks that enough bytes are available.
     *
     * @param count the number of bytes needed
     * @throws IOException if the data ends before
     */
    private void ensure(int count) throws IOException {
        if (position + count > data.length) {
            throw new IOException("Unexpected end of capella file at position " + position + ".");
        }
    }
}
