/*
 * GpBinaryInput.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl;

import java.io.IOException;
import java.nio.charset.Charset;


/**
 * Reads the little-endian primitives and length-prefixed strings of the binary Guitar Pro
 * formats (GP3 to GP5) from a byte array.
 *
 * @author patrick
 */
final class GpBinaryInput {
    private static final Charset CHARSET = Charset.forName("windows-1252");
    private static final int BYTE_MASK = 0xFF;
    private static final int MAX_STRING = 1024 * 1024;
    private final byte[] data;
    private int position;


    /**
     * Constructor for GpBinaryInput.
     *
     * @param data the file content
     */
    GpBinaryInput(byte[] data) {
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
     * Checks if more bytes are available.
     *
     * @return true if the end is not reached
     */
    boolean hasMore() {
        return position < data.length;
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
    int readSignedByte() throws IOException {
        ensure(1);
        return data[position++];
    }


    /**
     * Reads a boolean (one byte, not zero is true).
     *
     * @return the value
     * @throws IOException at the end of the data
     */
    boolean readBool() throws IOException {
        return readByte() != 0;
    }


    /**
     * Reads a signed 16 bit integer.
     *
     * @return the value
     * @throws IOException at the end of the data
     */
    int readShort() throws IOException {
        ensure(2);
        int value = (data[position] & BYTE_MASK) | (data[position + 1] << 8);
        position += 2;
        return (short) value;
    }


    /**
     * Reads a signed 32 bit integer.
     *
     * @return the value
     * @throws IOException at the end of the data
     */
    int readInt() throws IOException {
        ensure(4);
        int value = (data[position] & BYTE_MASK) | ((data[position + 1] & BYTE_MASK) << 8)
                | ((data[position + 2] & BYTE_MASK) << 16) | ((data[position + 3] & BYTE_MASK) << 24);
        position += 4;
        return value;
    }


    /**
     * Skips bytes.
     *
     * @param count the number of bytes
     * @throws IOException if the data ends before
     */
    void skip(int count) throws IOException {
        ensure(count);
        position += count;
    }


    /**
     * Reads a string with a byte length followed by a fixed size field: {@code length (1) + size}.
     *
     * @param size the size of the field
     * @return the string
     * @throws IOException at the end of the data
     */
    String readByteSizeString(int size) throws IOException {
        int length = readByte();
        ensure(size);
        String result = string(position, Math.min(length, size));
        position += size;
        return result;
    }


    /**
     * Reads a string with an integer size, a byte length and the characters: {@code size (4) + length (1) + size - 1}.
     *
     * @return the string
     * @throws IOException at the end of the data or for an invalid size
     */
    String readIntByteSizeString() throws IOException {
        int size = readInt() - 1;
        int length = readByte();
        if (size < 0 || size > MAX_STRING) {
            throw new IOException("Invalid Guitar Pro string size: " + size);
        }
        ensure(size);
        String result = string(position, Math.min(length, size));
        position += size;
        return result;
    }


    /**
     * Reads a string with an integer length followed by the characters.
     *
     * @return the string
     * @throws IOException at the end of the data or for an invalid length
     */
    String readIntSizeString() throws IOException {
        int length = readInt();
        if (length < 0 || length > MAX_STRING) {
            throw new IOException("Invalid Guitar Pro string length: " + length);
        }
        ensure(length);
        String result = string(position, length);
        position += length;
        return result;
    }


    /**
     * Reads a text with an unused integer followed by a byte length and the characters.
     *
     * @return the text
     * @throws IOException at the end of the data
     */
    String readIntUnusedString() throws IOException {
        skip(4);
        int length = readByte();
        ensure(length);
        String result = string(position, length);
        position += length;
        return result;
    }


    /**
     * Decodes characters.
     *
     * @param start the start index
     * @param length the number of bytes
     * @return the string
     */
    private String string(int start, int length) {
        return new String(data, start, length, CHARSET);
    }


    /**
     * Checks that enough bytes are available.
     *
     * @param count the number of bytes needed
     * @throws IOException if the data ends before
     */
    private void ensure(int count) throws IOException {
        if (count < 0 || position + count > data.length) {
            throw new IOException("Unexpected end of Guitar Pro file at position " + position + ".");
        }
    }
}
