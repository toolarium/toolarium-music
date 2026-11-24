/*
 * ZipContentReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;


/**
 * Reads the entries of a ZIP container held in memory using its central directory. Unlike
 * {@link java.util.zip.ZipInputStream} it accepts containers whose local entry headers have wrong
 * sizes (as written by some notation programs).
 *
 * @author patrick
 */
public final class ZipContentReader {
    private static final int END_SIGNATURE = 0x06054B50;
    private static final int CENTRAL_SIGNATURE = 0x02014B50;
    private static final int LOCAL_SIGNATURE = 0x04034B50;
    private static final int END_SIZE = 22;
    private static final int MAX_COMMENT = 0xFFFF;
    private static final int CENTRAL_SIZE = 46;
    private static final int LOCAL_SIZE = 30;
    private static final int STORED = 0;
    private static final int DEFLATED = 8;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_INITIAL_CAPACITY = 1024 * 1024;
    private static final int BYTE_MASK = 0xFF;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final ZipContentReader INSTANCE = new ZipContentReader();
    }


    /**
     * Constructor.
     */
    private ZipContentReader() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static ZipContentReader getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Lists the entry names of a ZIP container.
     *
     * @param zip the container
     * @return the entry names with their central directory offsets
     * @throws IOException if the container is invalid
     */
    public Map<String, Integer> list(byte[] zip) throws IOException {
        int end = findEnd(zip);
        int count = readShort(zip, end + 10);
        int offset = readInt(zip, end + 16);
        Map<String, Integer> entries = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            if (offset < 0 || offset + CENTRAL_SIZE > zip.length || readInt(zip, offset) != CENTRAL_SIGNATURE) {
                throw new IOException("Invalid ZIP container: broken central directory.");
            }
            int nameLength = readShort(zip, offset + 28);
            int extraLength = readShort(zip, offset + 30);
            int commentLength = readShort(zip, offset + 32);
            if (offset + CENTRAL_SIZE + nameLength > zip.length) {
                throw new IOException("Invalid ZIP container: broken central directory.");
            }
            String name = new String(zip, offset + CENTRAL_SIZE, nameLength, StandardCharsets.UTF_8);
            entries.put(name, offset);
            offset += CENTRAL_SIZE + nameLength + extraLength + commentLength;
        }
        return entries;
    }


    /**
     * Reads an entry of a ZIP container.
     *
     * @param zip the container
     * @param name the entry name
     * @param maxSize the maximum uncompressed size
     * @return the content, or null if the entry does not exist
     * @throws IOException if the container or entry is invalid or too large
     */
    public byte[] read(byte[] zip, String name, int maxSize) throws IOException {
        Integer central = list(zip).get(name);
        if (central == null) {
            return null;
        }
        int method = readShort(zip, central + 10);
        int compressedSize = readInt(zip, central + 20);
        int size = readInt(zip, central + 24);
        int local = readInt(zip, central + 42);
        if (local < 0 || local + LOCAL_SIZE > zip.length || readInt(zip, local) != LOCAL_SIGNATURE) {
            throw new IOException("Invalid ZIP container: broken entry " + name + ".");
        }
        int start = local + LOCAL_SIZE + readShort(zip, local + 26) + readShort(zip, local + 28);
        if (compressedSize < 0 || (long) start + compressedSize > zip.length || size > maxSize || compressedSize > maxSize) {
            throw new IOException("Invalid ZIP container: entry " + name + " is broken or too large.");
        }
        if (method == STORED) {
            byte[] result = new byte[compressedSize];
            System.arraycopy(zip, start, result, 0, compressedSize);
            return result;
        }
        if (method != DEFLATED) {
            throw new IOException("Unsupported ZIP compression method " + method + " for entry " + name + ".");
        }
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(zip, start, compressedSize);
            // the size in the header is only a hint: a damaged header must not reserve a large buffer
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(0, Math.min(size, MAX_INITIAL_CAPACITY)));
            byte[] buffer = new byte[BUFFER_SIZE];
            while (!inflater.finished()) {
                int read = inflater.inflate(buffer);
                if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                out.write(buffer, 0, read);
                if (out.size() > maxSize) {
                    throw new IOException("ZIP entry " + name + " is too large.");
                }
            }
            return out.toByteArray();
        } catch (DataFormatException e) {
            throw new IOException("Invalid ZIP container: entry " + name + " cannot be decompressed.", e);
        } finally {
            inflater.end();
        }
    }


    /**
     * Finds the end of central directory record.
     *
     * @param zip the container
     * @return the offset of the record
     * @throws IOException if the record is missing
     */
    private int findEnd(byte[] zip) throws IOException {
        int min = Math.max(0, zip.length - END_SIZE - MAX_COMMENT);
        for (int i = zip.length - END_SIZE; i >= min; i--) {
            if (readInt(zip, i) == END_SIGNATURE) {
                return i;
            }
        }
        throw new IOException("Invalid ZIP container: no central directory.");
    }


    /**
     * Reads a little-endian 16 bit unsigned integer.
     *
     * @param data the data
     * @param index the index
     * @return the value, or -1 if out of range
     */
    private int readShort(byte[] data, int index) {
        if (index < 0 || index + 1 >= data.length) {
            return -1;
        }
        return (data[index] & BYTE_MASK) | ((data[index + 1] & BYTE_MASK) << 8);
    }


    /**
     * Reads a little-endian 32 bit integer.
     *
     * @param data the data
     * @param index the index
     * @return the value, or -1 if out of range
     */
    private int readInt(byte[] data, int index) {
        if (index < 0 || index + 3 >= data.length) {
            return -1;
        }
        return (data[index] & BYTE_MASK) | ((data[index + 1] & BYTE_MASK) << 8)
                | ((data[index + 2] & BYTE_MASK) << 16) | ((data[index + 3] & BYTE_MASK) << 24);
    }
}
