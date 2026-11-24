/*
 * SibeliusContainerReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl;

import com.github.toolarium.music.io.stream.StreamContentReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Reads and decodes the Sibelius container format used by .sib, .sco, and .si7 files.
 *
 * <p>The container format is a tree of chunks, each starting with a magic signature and a
 * header (version, timestamp, mode flags, compress info). A directory chunk lists its children
 * as pairs of chunk type and file offset; the chunk of type 1 holds the score. The score chunk
 * is encrypted with a stream cipher and zlib-compressed. Sibelius 1 to 4 files store the score
 * chunk directly at the root, newer versions wrap it in a directory chunk.</p>
 *
 * <p>The cipher seed follows the chunk header after a fixed gap (0x17 bytes for Sibelius,
 * 0x0D bytes for Scorch files); the encrypted data starts a second fixed gap after the seed
 * (0x23 bytes for Sibelius, 0x11 bytes for Scorch files), followed by the item count for
 * versions after 0x210000.</p>
 *
 * @author patrick
 */
final class SibeliusContainerReader {
    private static final Logger LOG = LoggerFactory.getLogger(SibeliusContainerReader.class);
    private static final String MAGIC_SIBELIUS = "SIBELIUS";
    private static final String MAGIC_SCORCH = "CCSCORCH";
    private static final int MAGIC_MARKER = 0x0F;
    private static final int MAGIC_LEN = 10;
    private static final int MODE_ENCRYPTED = 0x02;
    private static final int MODE_COMPRESSED = 0x08;
    private static final int MIN_VERSION = 5;
    private static final int VERSION_WITH_TIMESTAMP = 6;
    private static final int VERSION_WITH_ITEMS = 0x210000;
    private static final int CHUNK_TYPE_SCORE = 1;
    private static final Set<Integer> DIRECTORY_TYPES = new HashSet<>(Arrays.asList(4, 8, 11, 12));
    private static final int MIN_CHILD_OFFSET = 27;
    private static final int MAX_CHILDREN = 1024;
    private static final int MAX_DEPTH = 8;
    private static final int MAX_FILE_SIZE = 200 * 1024 * 1024;
    private static final long MAX_DECOMPRESSED_SIZE = 200L * 1024 * 1024;
    private static final int SIB_SEED_SKIP = 0x17;
    private static final int SIB_DATA_SKIP = 0x23;
    private static final int SCO_SEED_SKIP = 0x0D;
    private static final int SCO_DATA_SKIP = 0x11;


    /**
     * Constructor.
     */
    SibeliusContainerReader() {
        // package-private
    }


    /**
     * Reads a Sibelius container from the given input stream, finds the score chunk, decrypts
     * and decompresses it, and returns the raw decompressed payload.
     *
     * @param inputStream the input stream positioned at the start of the file
     * @return the decompressed score payload bytes
     * @throws IOException if the file is invalid, unsupported, or an I/O error occurs
     */
    byte[] readAndDecompress(InputStream inputStream) throws IOException {
        byte[] data = StreamContentReader.getInstance().readAll(inputStream, MAX_FILE_SIZE, "Sibelius file");
        boolean isScorch = isScorch(data, 0);
        byte[] payload = readChunk(data, 0, isScorch, 0, false);
        if (payload == null) {
            throw new IOException("The Sibelius file contains no score data.");
        }
        return payload;
    }


    /**
     * Reads a chunk: a score chunk returns its decoded payload, a directory chunk returns the
     * payload of its score child (searched depth-first, direct children first). A score chunk
     * without compression and encryption (for example a file unlocked by a converter tool)
     * contains the payload as it is.
     *
     * @param data the file content
     * @param offset the offset of the chunk
     * @param isScorch true for Scorch files
     * @param depth the nesting depth
     * @param scoreChunk true if the chunk is referenced as score chunk (type 1) by its directory
     * @return the payload, or null if the chunk contains no score data
     * @throws IOException if the chunk is invalid
     */
    private byte[] readChunk(byte[] data, int offset, boolean isScorch, int depth, boolean scoreChunk) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("Sibelius chunk tree is nested too deeply.");
        }
        isScorch(data, offset);
        int position = offset + MAGIC_LEN;
        int version = readInt(data, position);
        position += 4;
        if (version < MIN_VERSION) {
            throw new IOException("Sibelius file format version too old (pre-Sibelius 2). Version: 0x" + Integer.toHexString(version));
        }
        if (version > VERSION_WITH_TIMESTAMP) {
            position += 4;
        }

        int mode = readInt(data, position);
        position += 4;
        boolean isCompressed = (mode & MODE_COMPRESSED) != 0;
        boolean isEncrypted = (mode & MODE_ENCRYPTED) != 0;
        if (isCompressed) {
            position += 4;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Chunk at 0x{}: version 0x{} (Sibelius {}), mode 0x{}", Integer.toHexString(offset), Integer.toHexString(version),
                      mapVersionToSibelius(version >> 16), Integer.toHexString(mode));
        }

        SibeliusCipher cipher = null;
        int seed = 0;
        if (isEncrypted) {
            int seedPosition = position + SIB_SEED_SKIP;
            int dataSkip = SIB_DATA_SKIP;
            if (isScorch) {
                seedPosition = position + SCO_SEED_SKIP;
                dataSkip = SCO_DATA_SKIP;
            }
            seed = readInt(data, seedPosition);
            cipher = new SibeliusCipher(seed, isScorch);
            position = seedPosition + 4 + dataSkip;
        }

        int items = 0;
        if (version > VERSION_WITH_ITEMS) {
            items = readInt(data, position);
            position += 4;
        }

        if (isCompressed) {
            try {
                return decryptAndDecompress(data, position, cipher);
            } catch (IOException e) {
                if (!isEncrypted) {
                    throw e;
                }

                // some Sibelius 1 and 2 files use the simple cipher
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Decoding with the standard cipher failed, trying the simple cipher: {}", e.getMessage(), e);
                }
                return decryptAndDecompress(data, position, new SibeliusCipher(seed, isScorch, true));
            }
        }
        if (isEncrypted || scoreChunk) {
            return decrypt(data, position, cipher);
        }
        return readChildren(data, position, items, isScorch, depth);
    }


    /**
     * Reads the children of a directory chunk and returns the payload of the score child.
     *
     * @param data the file content
     * @param position the position of the child table
     * @param items the item count of the directory chunk
     * @param isScorch true for Scorch files
     * @param depth the nesting depth of the directory chunk
     * @return the payload, or null if no child contains score data
     * @throws IOException if a child chunk is invalid
     */
    private byte[] readChildren(byte[] data, int position, int items, boolean isScorch, int depth) throws IOException {
        List<int[]> children = new ArrayList<>();
        int entry = position;
        while (children.size() < MAX_CHILDREN && entry + 8 <= data.length) {
            int type = readInt(data, entry);
            int childOffset = readInt(data, entry + 4);
            if (childOffset < MIN_CHILD_OFFSET || childOffset >= data.length) {
                break;
            }
            children.add(new int[] {type, childOffset});
            entry += 8;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Directory chunk with {} items and {} children", items, children.size());
        }

        for (int[] child : children) {
            if (child[0] == CHUNK_TYPE_SCORE) {
                byte[] payload = readChunk(data, child[1], isScorch, depth + 1, true);
                if (payload != null) {
                    return payload;
                }
            }
        }
        // no score at this level: search the directory children (for example the version history)
        for (int[] child : children) {
            if (DIRECTORY_TYPES.contains(child[0]) && isScorchOrSibelius(data, child[1])) {
                byte[] payload = readChunk(data, child[1], isScorch, depth + 1, false);
                if (payload != null) {
                    return payload;
                }
            }
        }
        return null;
    }


    /**
     * Checks the magic of a chunk.
     *
     * @param data the file content
     * @param offset the chunk offset
     * @return true for a Scorch chunk, false for a Sibelius chunk
     * @throws IOException if the magic is invalid
     */
    private boolean isScorch(byte[] data, int offset) throws IOException {
        if (offset < 0 || offset + MAGIC_LEN > data.length) {
            throw new IOException("Invalid Sibelius file: truncated chunk header.");
        }
        int marker = data[offset] & 0xFF;
        if (marker != MAGIC_MARKER) {
            throw new IOException("Invalid Sibelius file: expected 0x0F marker, got 0x" + Integer.toHexString(marker));
        }

        String magic = new String(data, offset + 1, 8, StandardCharsets.US_ASCII);
        if (MAGIC_SIBELIUS.equals(magic)) {
            return false;
        }
        if (MAGIC_SCORCH.equals(magic)) {
            return true;
        }
        throw new IOException("Invalid file: expected SIBELIUS or CCSCORCH magic, got: " + magic);
    }


    /**
     * Checks if a chunk magic starts at the given offset.
     *
     * @param data the file content
     * @param offset the offset
     * @return true if a Sibelius or Scorch chunk starts there
     */
    private boolean isScorchOrSibelius(byte[] data, int offset) {
        try {
            isScorch(data, offset);
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    /**
     * Reads a big-endian 32-bit integer.
     *
     * @param data the file content
     * @param position the position
     * @return the value
     * @throws IOException if the position is outside the data
     */
    private int readInt(byte[] data, int position) throws IOException {
        if (position < 0 || position + 4 > data.length) {
            throw new IOException("Invalid Sibelius file: unexpected end of data.");
        }
        return ((data[position] & 0xFF) << 24) | ((data[position + 1] & 0xFF) << 16) | ((data[position + 2] & 0xFF) << 8) | (data[position + 3] & 0xFF);
    }


    /**
     * Decrypts (if the cipher is non-null) and decompresses the data from the given position.
     *
     * @param data the file content
     * @param position the start of the compressed data
     * @param cipher the cipher for decryption, or null if not encrypted
     * @return the decompressed bytes
     * @throws IOException if decompression fails or size limits are exceeded
     */
    private byte[] decryptAndDecompress(byte[] data, int position, SibeliusCipher cipher) throws IOException {
        if (position > data.length) {
            throw new IOException("Invalid Sibelius file: unexpected end of data.");
        }

        byte[] compressed = decrypt(data, position, cipher);
        Inflater inflater = new Inflater();
        ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
        try {
            inflater.setInput(compressed);
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                decompressed.write(buffer, 0, count);
                if (decompressed.size() > MAX_DECOMPRESSED_SIZE) {
                    throw new IOException("Decompressed data exceeds maximum allowed size (" + MAX_DECOMPRESSED_SIZE + " bytes).");
                }
            }
        } catch (DataFormatException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid compressed Sibelius data: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid compressed Sibelius data: " + e.getMessage(), e);
        } finally {
            inflater.end();
        }

        if (decompressed.size() == 0) {
            throw new IOException("The Sibelius score data is empty.");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Decompressed {} bytes", decompressed.size());
        }
        return decompressed.toByteArray();
    }


    /**
     * Copies the data from the given position, decrypting each byte if a cipher is given.
     *
     * @param data the file content
     * @param position the start of the data
     * @param cipher the cipher for decryption, or null if not encrypted
     * @return the (decrypted) bytes
     */
    private byte[] decrypt(byte[] data, int position, SibeliusCipher cipher) {
        byte[] result = new byte[Math.max(0, data.length - position)];
        for (int i = 0; i < result.length; i++) {
            byte b = data[position + i];
            if (cipher != null) {
                b = cipher.decrypt(b);
            }
            result[i] = b;
        }
        return result;
    }


    /**
     * Maps a version high word to a human-readable Sibelius version string.
     *
     * @param versionHigh the version high word (upper 16 bits of the container version)
     * @return the Sibelius version string
     */
    private String mapVersionToSibelius(int versionHigh) {
        if (versionHigh <= 0x0002) {
            return "1.x";
        } else if (versionHigh <= 0x0008) {
            return "2.x";
        } else if (versionHigh <= 0x000A) {
            return "3.x";
        } else if (versionHigh <= 0x001C) {
            return "4.x";
        } else if (versionHigh <= 0x002D) {
            return "5.x";
        } else if (versionHigh <= 0x0036) {
            return "6.x";
        } else if (versionHigh <= 0x0039) {
            return "7.x";
        } else if (versionHigh <= 0x003D) {
            return "7.5+";
        } else if (versionHigh <= 0x0044) {
            return "2024.x";
        } else {
            return "2025+";
        }
    }
}
