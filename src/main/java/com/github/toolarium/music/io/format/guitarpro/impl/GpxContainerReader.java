/*
 * GpxContainerReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Reads the container of Guitar Pro 6 files (.gpx): a file system ("BCFS") that is usually
 * compressed with a bitwise LZ77 variant ("BCFZ"). The container holds score.gpif and other files.
 *
 * @author patrick
 */
final class GpxContainerReader {
    private static final String COMPRESSED = "BCFZ";
    private static final String FILE_SYSTEM = "BCFS";
    private static final int HEADER_SIZE = 4;
    private static final int SECTOR_SIZE = 0x1000;
    private static final int FILE_ENTRY = 2;
    private static final int NAME_OFFSET = 0x04;
    private static final int NAME_LENGTH = 127;
    private static final int SIZE_OFFSET = 0x8C;
    private static final int BLOCKS_OFFSET = 0x94;
    private static final int MAX_SIZE = 256 * 1024 * 1024;
    private static final int BYTE_MASK = 0xFF;
    private static final int BITS_PER_BYTE = 8;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final GpxContainerReader INSTANCE = new GpxContainerReader();
    }


    /**
     * Constructor.
     */
    private GpxContainerReader() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    static GpxContainerReader getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Checks if the content is a GPX container.
     *
     * @param content the file content
     * @return true for BCFZ or BCFS content
     */
    boolean isContainer(byte[] content) {
        String header = header(content, 0);
        return COMPRESSED.equals(header) || FILE_SYSTEM.equals(header);
    }


    /**
     * Reads the files of a GPX container.
     *
     * @param content the file content
     * @return the files by name
     * @throws IOException if the container is invalid
     */
    Map<String, byte[]> read(byte[] content) throws IOException {
        byte[] data = content;
        if (COMPRESSED.equals(header(data, 0))) {
            data = decompress(data);
        }
        if (!FILE_SYSTEM.equals(header(data, 0))) {
            throw new IOException("Invalid Guitar Pro 6 container: no file system found.");
        }
        return readFileSystem(data);
    }


    /**
     * Decompresses BCFZ content: a stream of bits where 1 starts a back reference (word size,
     * offset and length) and 0 starts up to three literal bytes.
     *
     * @param content the compressed content with header and expected length
     * @return the decompressed content
     * @throws IOException if the content is invalid
     */
    private byte[] decompress(byte[] content) throws IOException {
        if (content.length < HEADER_SIZE * 2) {
            throw new IOException("Invalid Guitar Pro 6 container: truncated header.");
        }
        int expected = readInt(content, HEADER_SIZE);
        if (expected < 0 || expected > MAX_SIZE) {
            throw new IOException("Invalid Guitar Pro 6 container: invalid size " + expected + ".");
        }
        GpxBitInput bits = new GpxBitInput(content, HEADER_SIZE * 2);
        ByteArrayOutputStream out = new ByteArrayOutputStream(expected);
        byte[] buffer = new byte[expected];
        int length = 0;
        while (length < expected && bits.hasMore()) {
            if (!bits.hasBits(1)) {
                break;
            }
            if (bits.readBits(1) == 1) {
                if (!bits.hasBits(4)) {
                    break;
                }
                int wordSize = bits.readBits(4);
                if (!bits.hasBits(wordSize * 2)) {
                    break;
                }
                int offset = bits.readBitsReversed(wordSize);
                int size = bits.readBitsReversed(wordSize);
                int source = length - offset;
                if (offset <= 0 || source < 0) {
                    throw new IOException("Invalid Guitar Pro 6 container: invalid back reference.");
                }
                int count = Math.min(offset, size);
                for (int i = 0; i < count && length < expected; i++) {
                    buffer[length] = buffer[source + i];
                    length++;
                }
            } else {
                if (!bits.hasBits(2)) {
                    break;
                }
                int size = bits.readBitsReversed(2);
                if (!bits.hasBits(size * BITS_PER_BYTE)) {
                    break;
                }
                for (int i = 0; i < size && length < expected; i++) {
                    buffer[length] = (byte) bits.readBits(BITS_PER_BYTE);
                    length++;
                }
            }
        }
        out.write(buffer, 0, length);
        return out.toByteArray();
    }


    /**
     * Reads the files of the BCFS file system: sectors of 4096 bytes; a file entry has a name, a
     * size and a list of data sector numbers.
     *
     * @param data the file system with header
     * @return the files by name
     */
    private Map<String, byte[]> readFileSystem(byte[] data) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        int base = HEADER_SIZE;
        int length = data.length - base;
        int offset = SECTOR_SIZE;
        while (offset + 3 < length) {
            if (readInt(data, base + offset) == FILE_ENTRY && offset + BLOCKS_OFFSET <= length) {
                String name = readName(data, base + offset + NAME_OFFSET);
                int size = readInt(data, base + offset + SIZE_OFFSET);
                ByteArrayOutputStream file = new ByteArrayOutputStream();
                int pointer = offset + BLOCKS_OFFSET;
                while (pointer + 3 < length) {
                    int sector = readInt(data, base + pointer);
                    if (sector <= 0) {
                        break;
                    }
                    int start = sector * SECTOR_SIZE;
                    if (start < 0 || start >= length) {
                        break;
                    }
                    file.write(data, base + start, Math.min(SECTOR_SIZE, length - start));
                    offset = start;
                    pointer += 4;
                }
                byte[] content = file.toByteArray();
                if (size >= 0 && size < content.length) {
                    byte[] trimmed = new byte[size];
                    System.arraycopy(content, 0, trimmed, 0, size);
                    content = trimmed;
                }
                files.put(name, content);
            }
            offset += SECTOR_SIZE;
        }
        return files;
    }


    /**
     * Reads a zero terminated file name.
     *
     * @param data the data
     * @param start the start index
     * @return the name
     */
    private String readName(byte[] data, int start) {
        int end = start;
        while (end < start + NAME_LENGTH && end < data.length && data[end] != 0) {
            end++;
        }
        return new String(data, start, end - start, StandardCharsets.UTF_8);
    }


    /**
     * Reads a little-endian 32 bit integer.
     *
     * @param data the data
     * @param index the index
     * @return the value, or -1 if the index is out of range
     */
    private int readInt(byte[] data, int index) {
        if (index < 0 || index + 3 >= data.length) {
            return -1;
        }
        return (data[index] & BYTE_MASK) | ((data[index + 1] & BYTE_MASK) << 8)
                | ((data[index + 2] & BYTE_MASK) << 16) | ((data[index + 3] & BYTE_MASK) << 24);
    }


    /**
     * Reads the four character header at an index.
     *
     * @param data the data
     * @param index the index
     * @return the header, or an empty string if the data is too short
     */
    private String header(byte[] data, int index) {
        if (data == null || data.length < index + HEADER_SIZE) {
            return "";
        }
        return new String(data, index, HEADER_SIZE, StandardCharsets.US_ASCII);
    }
}
