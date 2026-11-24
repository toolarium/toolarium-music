/*
 * MusBinaryParser.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl;

import com.github.toolarium.music.io.converter.EncodingConverter;
import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.barline.impl.RepeatImpl;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.BackupImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.notation.impl.NotationsImpl;
import com.github.toolarium.music.model.notation.impl.TiedImpl;
import com.github.toolarium.music.model.note.impl.GraceImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import com.github.toolarium.music.model.note.impl.PitchImpl;
import com.github.toolarium.music.model.note.impl.RestImpl;
import com.github.toolarium.music.model.note.impl.TieImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.CreatorImpl;
import com.github.toolarium.music.model.score.impl.EncodingImpl;
import com.github.toolarium.music.model.score.impl.IdentificationImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Parses Finale .mus (legacy binary Enigma) files and converts them to an {@link IScore}.
 *
 * <p>Supports the zlib-compressed era (Finale 2007 through 2012) and the uncompressed
 * era (Finale 3.x through 2000). The binary format uses a block chain container with
 * four pools: others (measure/staff specs), details (gfholds), entries (notes), and text.</p>
 *
 * @author patrick
 */
public final class MusBinaryParser {
    private static final Logger LOG = LoggerFactory.getLogger(MusBinaryParser.class);

    private static final int BLOCK_HEADER_SIZE = 6;
    private static final int BLOCK_TYPE_END = 0xFFFF;

    private static final int POOL_OTHERS_UNCOMPRESSED = 0x0001;
    private static final int POOL_DETAILS_UNCOMPRESSED = 0x0002;
    private static final int POOL_ENTRIES_UNCOMPRESSED = 0x0003;
    private static final int POOL_TEXT_UNCOMPRESSED = 0x0004;

    private static final int POOL_OTHERS_DCL = 0x000F;
    private static final int POOL_DETAILS_DCL = 0x0010;
    private static final int POOL_ENTRIES_DCL = 0x0011;
    private static final int POOL_TEXT_DCL = 0x0012;

    private static final int POOL_OTHERS_ZLIB = 0x001A;
    private static final int POOL_OTHERS_ZLIB_ALT1 = 0x0014;
    private static final int POOL_OTHERS_ZLIB_ALT2 = 0x0018;
    private static final int POOL_DETAILS_ZLIB = 0x001B;
    private static final int POOL_DETAILS_ZLIB_ALT1 = 0x0015;
    private static final int POOL_DETAILS_ZLIB_ALT2 = 0x0019;
    private static final int POOL_ENTRIES_ZLIB = 0x0016;
    private static final int POOL_TEXT_ZLIB = 0x0017;

    private static final int OTHER_ROW_SIZE = 16;
    private static final int DETAIL_ROW_SIZE = 16;
    private static final int ENTRY_ROW_SIZE = 38;
    private static final int NOTE_SLOT_SIZE = 6;
    private static final int MAX_NOTES_PER_ENTRY = 12;

    private static final int TAG_MEASURE_SPEC = ('M' << 8) | 'S';
    private static final int TAG_STAFF_SPEC = ('I' << 8) | 'S';
    private static final int TAG_MULTI_STAFF_GROUP = ('M' << 8) | 'G';
    private static final int TAG_GFHOLD = ('G' << 8) | 'F';
    private static final int TAG_FILE_INFO = ('f' << 8) | 'i';
    private static final int TAG_TEXT_BLOCK = ('b' << 8) | 'l';

    private static final int EF_VALID = 0x80000000;
    private static final int EF_NOTE = 0x40000000;
    private static final int EF_VOICE2 = 0x10000000;
    private static final int EF_GRACE_NOTE = 0x00800000;
    private static final int XEF_SLASH_GRACE = 0x0100;

    private static final int NF_VALID = 0x80000000;
    private static final int NF_TIE_START = 0x40000000;
    private static final int NF_TIE_END = 0x20000000;

    private static final int MEAS_SEC_BARLINE_MASK = 0x00F0;
    private static final int MEAS_SEC_BARLINE_SHIFT = 4;
    private static final int MEAS_SEC_FORWARD_REPEAT = 0x0008;
    private static final int MEAS_SEC_BACKWARD_REPEAT = 0x0004;

    private static final int DIVISIONS = 1024;
    private static final int SPECIAL_STAFF = 32767;
    private static final String SIGN_C = "C";
    private static final String SIGN_G = "G";
    private static final String[] STEP_NAMES = {SIGN_C, "D", "E", "F", SIGN_G, "A", "B"};

    private static final long MAX_POOL_SIZE = 64L * 1024 * 1024;
    private static final long MAX_DECOMPRESSED_SIZE = 128L * 1024 * 1024;


    /**
     * Private class, the only instance of the singleton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MusBinaryParser INSTANCE = new MusBinaryParser();
    }


    /**
     * Constructor.
     */
    private MusBinaryParser() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MusBinaryParser getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Parses a .mus binary file and returns the score model.
     *
     * @param data the raw .mus file bytes
     * @return the parsed score
     * @throws IOException if parsing fails
     */
    public IScore parse(byte[] data) throws IOException {
        if (data == null || data.length < BLOCK_HEADER_SIZE) {
            throw new IOException("Invalid .mus file: insufficient data.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Parsing .mus binary file: {} bytes.", data.length);
        }

        int chainStart = findBlockChainStart(data);
        ByteOrder byteOrder = detectByteOrder(data, chainStart);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Block chain starts at offset {}, byte order: {}.", chainStart, byteOrder);
        }

        Map<Integer, byte[]> pools = parseBlockChain(data, chainStart, byteOrder);

        byte[] othersPool = pools.get(0);
        byte[] detailsPool = pools.get(1);
        byte[] entriesPool = pools.get(2);
        byte[] textPool = pools.get(3);

        if (othersPool == null || entriesPool == null) {
            throw new IOException("Required pools not found in .mus file.");
        }

        if (LOG.isDebugEnabled()) {
            int detailsLen = 0;
            if (detailsPool != null) {
                detailsLen = detailsPool.length;
            }
            int textLen = 0;
            if (textPool != null) {
                textLen = textPool.length;
            }
            LOG.debug("Pools: others={} bytes, details={} bytes, entries={} bytes, text={} bytes.",
                    othersPool.length, detailsLen, entriesPool.length, textLen);
        }

        Map<Integer, MusMeasureSpec> measures = parseMeasureSpecs(othersPool, byteOrder);
        Map<Integer, MusStaffSpec> staves = parseStaffSpecs(othersPool, byteOrder);
        Map<Integer, MusEntry> entries = parseEntries(entriesPool, byteOrder);
        Map<String, Map<Integer, Integer>> gfholds = new TreeMap<>();
        if (detailsPool != null) {
            gfholds = parseGfholds(detailsPool, byteOrder);
        }

        String title = null;
        String composer = null;
        if (textPool != null) {
            Map<String, String> fileInfo = parseFileInfoTexts(textPool);
            title = fileInfo.get("title");
            composer = fileInfo.get("composer");
        }

        final Map<Integer, MusGroupInfo> groups = parseMultiStaffGroups(othersPool, byteOrder);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Parsed: {} measures, {} staves, {} entries, {} gfhold staves.",
                    measures.size(), staves.size(), entries.size(), gfholds.size());
        }

        if (staves.isEmpty() && !entries.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Staff/measure specs empty; falling back to entry-chain-based score building.");
            }
            return buildScoreFromEntryChains(entries, title, composer, textPool);
        }

        return buildScore(measures, staves, groups, entries, gfholds, title, composer);
    }


    /**
     * Reads the decompressed pools of a .mus file in little-endian byte order.
     *
     * @param data the raw .mus file bytes
     * @return map of pool index (0=others, 1=details, 2=entries, 3=text) to the pool data
     * @throws IOException if the file is too short, big-endian or cannot be decompressed
     */
    Map<Integer, byte[]> readPools(byte[] data) throws IOException {
        if (data == null || data.length < BLOCK_HEADER_SIZE) {
            throw new IOException("Invalid .mus file: insufficient data.");
        }
        int chainStart = findBlockChainStart(data);
        if (detectByteOrder(data, chainStart) != ByteOrder.LITTLE_ENDIAN) {
            throw new IOException("Big-endian .mus files are not supported.");
        }
        return parseBlockChain(data, chainStart, ByteOrder.LITTLE_ENDIAN);
    }


    /**
     * Finds the start of the block chain in the .mus file. The file begins with a
     * banner string terminated by a null byte, followed by possible padding.
     *
     * @param data the raw file bytes
     * @return the byte offset where the block chain begins
     */
    private int findBlockChainStart(byte[] data) {
        int pos = 0;
        // Skip the ENIGMA header text
        while (pos < data.length && data[pos] != 0) {
            pos++;
        }

        if (pos + BLOCK_HEADER_SIZE > data.length) {
            return pos;
        }

        // Scan the rest of the file for a validated block chain start.
        // A valid start has a known block type whose length field points
        // to another known block type (or end marker).
        int scanEnd = data.length - BLOCK_HEADER_SIZE;
        for (int i = pos; i < scanEnd; i++) {
            if (validateChainCandidate(data, i, ByteOrder.LITTLE_ENDIAN)
                    || validateChainCandidate(data, i, ByteOrder.BIG_ENDIAN)) {
                return i;
            }
        }

        return pos;
    }


    /**
     * Checks whether a candidate offset is a valid block chain start by verifying
     * that at least three consecutive blocks have known types and reasonable lengths,
     * or that two blocks are followed by the end marker.
     *
     * @param data the raw file bytes
     * @param offset the candidate offset
     * @param order the byte order to test
     * @return true if this looks like a real block chain start
     */
    private boolean validateChainCandidate(byte[] data, int offset, ByteOrder order) {
        int pos = offset;
        int validated = 0;
        int required = 3;

        while (validated < required && pos + BLOCK_HEADER_SIZE <= data.length) {
            int blockType = readU16(data, pos, order);
            if (blockType == BLOCK_TYPE_END) {
                return validated >= 2;
            }
            if (!isKnownBlockType(blockType)) {
                return false;
            }

            int blockLength = readU32(data, pos + 2, order);
            if (blockLength < BLOCK_HEADER_SIZE || blockLength > data.length) {
                return false;
            }

            pos += blockLength;
            validated++;
        }

        return validated >= required;
    }


    /**
     * Detects the byte order of the file by examining block headers.
     *
     * @param data the raw file bytes
     * @param chainStart the offset where the block chain begins
     * @return the detected byte order
     */
    private ByteOrder detectByteOrder(byte[] data, int chainStart) {
        if (chainStart + BLOCK_HEADER_SIZE > data.length) {
            return ByteOrder.LITTLE_ENDIAN;
        }

        int typeLE = readU16(data, chainStart, ByteOrder.LITTLE_ENDIAN);
        int typeBE = readU16(data, chainStart, ByteOrder.BIG_ENDIAN);

        if (isKnownBlockType(typeLE)) {
            int lenLE = readU32(data, chainStart + 2, ByteOrder.LITTLE_ENDIAN);
            if (lenLE >= BLOCK_HEADER_SIZE && lenLE < data.length) {
                return ByteOrder.LITTLE_ENDIAN;
            }
        }

        if (isKnownBlockType(typeBE)) {
            int lenBE = readU32(data, chainStart + 2, ByteOrder.BIG_ENDIAN);
            if (lenBE >= BLOCK_HEADER_SIZE && lenBE < data.length) {
                return ByteOrder.BIG_ENDIAN;
            }
        }

        return ByteOrder.LITTLE_ENDIAN;
    }


    /**
     * Checks if a block type value matches a known pool type.
     *
     * @param type the block type value
     * @return true if the type is a known pool type
     */
    private boolean isKnownBlockType(int type) {
        return type == POOL_OTHERS_UNCOMPRESSED || type == POOL_DETAILS_UNCOMPRESSED
                || type == POOL_ENTRIES_UNCOMPRESSED || type == POOL_TEXT_UNCOMPRESSED
                || type == POOL_OTHERS_DCL || type == POOL_DETAILS_DCL
                || type == POOL_ENTRIES_DCL || type == POOL_TEXT_DCL
                || type == POOL_OTHERS_ZLIB || type == POOL_OTHERS_ZLIB_ALT1
                || type == POOL_OTHERS_ZLIB_ALT2 || type == POOL_DETAILS_ZLIB
                || type == POOL_DETAILS_ZLIB_ALT1 || type == POOL_DETAILS_ZLIB_ALT2
                || type == POOL_ENTRIES_ZLIB || type == POOL_TEXT_ZLIB;
    }


    /**
     * Parses the block chain and extracts the four pools (others, details, entries, text).
     * Returns a map keyed by pool index: 0=others, 1=details, 2=entries, 3=text.
     *
     * @param data the raw file bytes
     * @param chainStart the offset where the block chain begins
     * @param byteOrder the file byte order
     * @return map of pool index to decompressed pool data
     * @throws IOException if parsing or decompression fails
     */
    private Map<Integer, byte[]> parseBlockChain(byte[] data, int chainStart, ByteOrder byteOrder) throws IOException {
        Map<Integer, byte[]> pools = new LinkedHashMap<>();
        int pos = chainStart;

        while (pos + BLOCK_HEADER_SIZE <= data.length) {
            int blockType = readU16(data, pos, byteOrder);
            if (blockType == BLOCK_TYPE_END) {
                break;
            }

            int blockLength = readU32(data, pos + 2, byteOrder);
            if (blockLength < BLOCK_HEADER_SIZE) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invalid block length {} at offset {}, stopping chain.", blockLength, pos);
                }
                break;
            }

            int dataLength = blockLength - BLOCK_HEADER_SIZE;
            int dataStart = pos + BLOCK_HEADER_SIZE;
            if (dataStart + dataLength > data.length) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Block at offset {} extends beyond file, truncating.", pos);
                }
                dataLength = data.length - dataStart;
            }

            byte[] blockData = new byte[dataLength];
            System.arraycopy(data, dataStart, blockData, 0, dataLength);

            int poolIndex = classifyPool(blockType);
            if (poolIndex >= 0) {
                if (isDclBlockType(blockType)) {
                    throw new IOException("DCL-compressed .mus file (block type 0x"
                            + Integer.toHexString(blockType) + "); only uncompressed and zlib formats are supported.");
                }
                boolean compressed = isCompressedBlockType(blockType);
                byte[] poolData = blockData;
                if (compressed && blockData.length > 0) {
                    poolData = decompressZlib(blockData, blockType);
                }
                if (poolData.length > 0) {
                    byte[] existing = pools.get(poolIndex);
                    if (existing != null) {
                        poolData = concatenate(existing, poolData);
                    }
                    pools.put(poolIndex, poolData);
                }
            }

            pos += blockLength;
        }

        return pools;
    }


    /**
     * Classifies a block type into a pool index.
     *
     * @param blockType the block type code
     * @return the pool index (0=others, 1=details, 2=entries, 3=text), or -1 if unknown
     */
    private int classifyPool(int blockType) {
        if (blockType == POOL_OTHERS_UNCOMPRESSED || blockType == POOL_OTHERS_DCL
                || blockType == POOL_OTHERS_ZLIB || blockType == POOL_OTHERS_ZLIB_ALT1
                || blockType == POOL_OTHERS_ZLIB_ALT2) {
            return 0;
        }
        if (blockType == POOL_DETAILS_UNCOMPRESSED || blockType == POOL_DETAILS_DCL
                || blockType == POOL_DETAILS_ZLIB || blockType == POOL_DETAILS_ZLIB_ALT1
                || blockType == POOL_DETAILS_ZLIB_ALT2) {
            return 1;
        }
        if (blockType == POOL_ENTRIES_UNCOMPRESSED || blockType == POOL_ENTRIES_DCL
                || blockType == POOL_ENTRIES_ZLIB) {
            return 2;
        }
        if (blockType == POOL_TEXT_UNCOMPRESSED || blockType == POOL_TEXT_DCL
                || blockType == POOL_TEXT_ZLIB) {
            return 3;
        }
        return -1;
    }


    /**
     * Checks if a block type indicates compressed data.
     *
     * @param blockType the block type code
     * @return true if the block data is zlib-compressed
     */
    private boolean isCompressedBlockType(int blockType) {
        return blockType != POOL_OTHERS_UNCOMPRESSED
                && blockType != POOL_DETAILS_UNCOMPRESSED
                && blockType != POOL_ENTRIES_UNCOMPRESSED
                && blockType != POOL_TEXT_UNCOMPRESSED;
    }


    /**
     * Checks if a block type indicates DCL (Data Compression Library) compression,
     * which is used in older Finale .mus files (pre-Finale 2000).
     *
     * @param blockType the block type code
     * @return true if the block uses DCL compression
     */
    private boolean isDclBlockType(int blockType) {
        return blockType == POOL_OTHERS_DCL
                || blockType == POOL_DETAILS_DCL
                || blockType == POOL_ENTRIES_DCL
                || blockType == POOL_TEXT_DCL;
    }


    /**
     * Decompresses a zlib-compressed pool block. The compressed data may be preceded
     * by a CRC-32 checksum and stored length.
     *
     * @param compressedData the compressed block data
     * @param blockType the block type for diagnostic messages
     * @return the decompressed data
     * @throws IOException if decompression fails
     */
    private byte[] decompressZlib(byte[] compressedData, int blockType) throws IOException {
        int offset = findZlibStart(compressedData);

        if (offset >= compressedData.length) {
            return new byte[0];
        }

        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressedData, offset, compressedData.length - offset);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            long total = 0;

            while (!inflater.finished()) {
                try {
                    int count = inflater.inflate(buffer);
                    if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                        break;
                    }
                    total += count;
                    if (total > MAX_DECOMPRESSED_SIZE) {
                        throw new IOException("Decompressed pool exceeds maximum size.");
                    }
                    baos.write(buffer, 0, count);
                } catch (DataFormatException e) {
                    if (baos.size() > 0) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Partial zlib decompression for block type 0x{}: {} bytes before error.",
                                    Integer.toHexString(blockType), baos.size(), e);
                        }
                        break;
                    }

                    if (offset == 8) {
                        return decompressZlibNoHeader(compressedData);
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Failed to decompress block type 0x{}: {}",
                                Integer.toHexString(blockType), e.getMessage(), e);
                    }
                    throw new IOException("Failed to decompress .mus pool data.", e);
                }
            }

            return baos.toByteArray();
        } finally {
            inflater.end();
        }
    }


    /**
     * Attempts zlib decompression without skipping the 8-byte header, as a fallback.
     *
     * @param compressedData the raw block data
     * @return the decompressed data
     * @throws IOException if decompression fails
     */
    private byte[] decompressZlibNoHeader(byte[] compressedData) throws IOException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressedData);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            long total = 0;

            while (!inflater.finished()) {
                try {
                    int count = inflater.inflate(buffer);
                    if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                        break;
                    }
                    total += count;
                    if (total > MAX_DECOMPRESSED_SIZE) {
                        throw new IOException("Decompressed pool exceeds maximum size.");
                    }
                    baos.write(buffer, 0, count);
                } catch (DataFormatException e) {
                    if (baos.size() > 0) {
                        break;
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Zlib fallback decompression also failed: {}", e.getMessage(), e);
                    }
                    throw new IOException("Failed to decompress .mus pool data.", e);
                }
            }

            return baos.toByteArray();
        } finally {
            inflater.end();
        }
    }


    /**
     * Finds the start of the zlib stream within block data by looking for the
     * zlib header magic bytes (0x78 followed by a valid check byte).
     * The sub-header before the zlib stream varies in size across .mus file versions.
     *
     * @param data the raw block data (without the 6-byte block header)
     * @return the offset of the zlib header, or 0 if not found within the first 16 bytes
     */
    private int findZlibStart(byte[] data) {
        int scanLimit = Math.min(data.length - 1, 16);
        for (int i = 0; i < scanLimit; i++) {
            if ((data[i] & 0xFF) == 0x78) {
                int check = (0x78 << 8) | (data[i + 1] & 0xFF);
                if (check % 31 == 0) {
                    return i;
                }
            }
        }

        return 0;
    }


    /**
     * Parses measure specification records from the others pool.
     *
     * @param othersPool the decompressed others pool data
     * @param byteOrder the file byte order
     * @return map of measure number (cmper) to measure spec
     */
    private Map<Integer, MusMeasureSpec> parseMeasureSpecs(byte[] othersPool, ByteOrder byteOrder) {
        Map<Integer, MusMeasureSpec> result = new TreeMap<>();
        int pos = 0;

        while (pos + OTHER_ROW_SIZE <= othersPool.length) {
            int cmper = readU16(othersPool, pos, byteOrder);
            int tag = readU16(othersPool, pos + 2, byteOrder);

            if (tag == TAG_MEASURE_SPEC && cmper > 0 && cmper < SPECIAL_STAFF) {
                if (!result.containsKey(cmper)) {
                    MusMeasureSpec spec = new MusMeasureSpec();
                    spec.setCmper(cmper);
                    spec.setWidth(readS16(othersPool, pos + 4, byteOrder));
                    spec.setKey(readS16(othersPool, pos + 6, byteOrder));
                    spec.setBeats(readU16(othersPool, pos + 8, byteOrder));
                    spec.setDivBeat(readU16(othersPool, pos + 10, byteOrder));
                    spec.setPrimaryFlags(readU16(othersPool, pos + 12, byteOrder));
                    spec.setSecondaryFlags(readU16(othersPool, pos + 14, byteOrder));
                    result.put(cmper, spec);
                }
            }

            pos += OTHER_ROW_SIZE;
        }

        return result;
    }


    /**
     * Parses staff specification records from the others pool.
     *
     * @param othersPool the decompressed others pool data
     * @param byteOrder the file byte order
     * @return map of staff number (cmper) to staff spec
     */
    private Map<Integer, MusStaffSpec> parseStaffSpecs(byte[] othersPool, ByteOrder byteOrder) {
        Map<Integer, MusStaffSpec> result = new TreeMap<>();
        int pos = 0;

        while (pos + OTHER_ROW_SIZE <= othersPool.length) {
            int cmper = readU16(othersPool, pos, byteOrder);
            int tag = readU16(othersPool, pos + 2, byteOrder);

            if (tag == TAG_STAFF_SPEC && cmper > 0 && cmper < SPECIAL_STAFF) {
                if (!result.containsKey(cmper)) {
                    MusStaffSpec spec = new MusStaffSpec();
                    spec.setCmper(cmper);
                    spec.setDefaultClef(readU16(othersPool, pos + 4, byteOrder));
                    spec.setTransInterval(readS16(othersPool, pos + 6, byteOrder));
                    spec.setNameRef(readU16(othersPool, pos + 8, byteOrder));
                    result.put(cmper, spec);
                }
            }

            pos += OTHER_ROW_SIZE;
        }

        return result;
    }


    /**
     * Parses multi-staff instrument group records from the others pool.
     *
     * @param othersPool the decompressed others pool data
     * @param byteOrder the file byte order
     * @return map of group cmper to group info
     */
    private Map<Integer, MusGroupInfo> parseMultiStaffGroups(byte[] othersPool, ByteOrder byteOrder) {
        Map<Integer, MusGroupInfo> result = new LinkedHashMap<>();
        int pos = 0;

        while (pos + OTHER_ROW_SIZE <= othersPool.length) {
            int cmper = readU16(othersPool, pos, byteOrder);
            int tag = readU16(othersPool, pos + 2, byteOrder);

            if (tag == TAG_MULTI_STAFF_GROUP && cmper > 0) {
                if (!result.containsKey(cmper)) {
                    MusGroupInfo group = new MusGroupInfo();
                    group.setStaffNum1(readU16(othersPool, pos + 4, byteOrder));
                    group.setStaffNum2(readU16(othersPool, pos + 6, byteOrder));
                    result.put(cmper, group);
                }
            }

            pos += OTHER_ROW_SIZE;
        }

        return result;
    }


    /**
     * Parses entry records from the entries pool. Each entry is 38 bytes and forms
     * a linked list via prev/next entry numbers.
     *
     * @param entriesPool the decompressed entries pool data
     * @param byteOrder the file byte order
     * @return map of entry number to parsed entry
     */
    private Map<Integer, MusEntry> parseEntries(byte[] entriesPool, ByteOrder byteOrder) {
        Map<Integer, MusEntry> result = new LinkedHashMap<>();
        int pos = 0;

        while (pos + ENTRY_ROW_SIZE <= entriesPool.length) {
            int entnum = readU32(entriesPool, pos, byteOrder);
            int incidence = readU16(entriesPool, pos + 4, byteOrder);

            if (entnum == 0) {
                pos += ENTRY_ROW_SIZE;
                continue;
            }

            if (incidence == 0) {
                MusEntry entry = new MusEntry();
                entry.setEntnum(entnum);
                entry.setPrev(readU32(entriesPool, pos + 6, byteOrder));
                entry.setNext(readU32(entriesPool, pos + 10, byteOrder));
                entry.setDura(readU16(entriesPool, pos + 14, byteOrder));
                entry.setFlags(readU32(entriesPool, pos + 18, byteOrder));
                entry.setExtendedFlags(readU16(entriesPool, pos + 22, byteOrder));
                int entryNoteCount = readU16(entriesPool, pos + 24, byteOrder);

                if (entryNoteCount > MAX_NOTES_PER_ENTRY) {
                    entryNoteCount = MAX_NOTES_PER_ENTRY;
                }
                entry.setNoteCount(entryNoteCount);

                List<MusNote> entryNotes = new ArrayList<>();
                int slotsInRow = Math.min(entryNoteCount, 2);
                for (int ni = 0; ni < slotsInRow; ni++) {
                    int slotOffset = pos + 26 + (ni * NOTE_SLOT_SIZE);
                    MusNote note = parseNoteSlot(entriesPool, slotOffset, byteOrder);
                    entryNotes.add(note);
                }
                entry.setNotes(entryNotes);

                result.put(entnum, entry);
            } else {
                MusEntry parent = result.get(entnum);
                if (parent != null && parent.getNotes().size() < parent.getNoteCount()) {
                    int slotsNeeded = parent.getNoteCount() - parent.getNotes().size();
                    int slotsInRow = Math.min(slotsNeeded, 5);
                    for (int ni = 0; ni < slotsInRow; ni++) {
                        int slotOffset = pos + 6 + (ni * NOTE_SLOT_SIZE);
                        if (slotOffset + NOTE_SLOT_SIZE <= pos + ENTRY_ROW_SIZE) {
                            MusNote note = parseNoteSlot(entriesPool, slotOffset, byteOrder);
                            parent.getNotes().add(note);
                        }
                    }
                }
            }

            pos += ENTRY_ROW_SIZE;
        }

        return result;
    }


    /**
     * Parses a single note slot (6 bytes) from an entry row.
     *
     * @param data the pool data
     * @param offset the offset of the note slot
     * @param byteOrder the file byte order
     * @return the parsed note
     */
    private MusNote parseNoteSlot(byte[] data, int offset, ByteOrder byteOrder) {
        MusNote note = new MusNote();
        int tcd = readU16(data, offset, byteOrder);
        note.setFlags(readU32(data, offset + 2, byteOrder));

        int rawLevel = tcd >> 4;
        if ((tcd & 0x8000) != 0) {
            rawLevel -= 4096;
        }
        note.setHarmLev(rawLevel);

        int altMag = tcd & 0x0007;
        if ((tcd & 0x0008) != 0) {
            altMag = -altMag;
        }
        note.setHarmAlt(altMag);

        note.setNoteId((note.getFlags() >> 16) & 0x001F);

        return note;
    }


    /**
     * Parses gfhold records from the details pool. Each gfhold links a staff+measure
     * combination to a start entry number.
     *
     * @param detailsPool the decompressed details pool data
     * @param byteOrder the file byte order
     * @return map of staff key to (measure number to start entry number)
     */
    private Map<String, Map<Integer, Integer>> parseGfholds(byte[] detailsPool, ByteOrder byteOrder) {
        Map<String, Map<Integer, Integer>> result = new TreeMap<>();
        int pos = 0;

        while (pos + DETAIL_ROW_SIZE <= detailsPool.length) {
            int cmper1 = readU16(detailsPool, pos, byteOrder);
            int cmper2 = readU16(detailsPool, pos + 2, byteOrder);
            int tag = readU16(detailsPool, pos + 4, byteOrder);

            if (tag == TAG_GFHOLD && cmper1 > 0 && cmper2 > 0) {
                int startEntry = readU16(detailsPool, pos + 10, byteOrder);
                if (startEntry > 0) {
                    String staffKey = String.valueOf(cmper1);
                    result.computeIfAbsent(staffKey, k -> new TreeMap<>()).putIfAbsent(cmper2, startEntry);
                }
            }

            pos += DETAIL_ROW_SIZE;
        }

        return result;
    }


    /**
     * Parses file info text records from the text pool. The text pool uses Finale
     * markup with {@code ^fileInfo(N)} tags where N identifies the field:
     * 1=title, 6=composer. Content between the tag and the next {@code ^end} is
     * stripped of font markup and returned.
     *
     * @param textPool the decompressed text pool data
     * @return map of info type (title, composer) to text content
     */
    private Map<String, String> parseFileInfoTexts(byte[] textPool) {
        Map<String, String> result = new LinkedHashMap<>();
        String poolText = new String(textPool, StandardCharsets.ISO_8859_1);

        String titleContent = extractFileInfoContent(poolText, "^fileInfo(1)");
        if (titleContent != null && !titleContent.isEmpty()) {
            result.put("title", titleContent);
        }

        String composerContent = extractFileInfoContent(poolText, "^fileInfo(6)");
        if (composerContent != null && !composerContent.isEmpty()) {
            result.put("composer", composerContent);
        }

        return result;
    }


    /**
     * Extracts content after a {@code ^fileInfo(N)} tag, stripping Finale markup.
     * The content extends until the next {@code ^end} marker or end of string.
     *
     * @param poolText the text pool as a string
     * @param marker the fileInfo marker to search for
     * @return the extracted plain text, or null if the marker is not found
     */
    private String extractFileInfoContent(String poolText, String marker) {
        int idx = poolText.indexOf(marker);
        if (idx < 0) {
            return null;
        }

        int contentStart = idx + marker.length();
        int endIdx = poolText.indexOf("^end", contentStart);
        if (endIdx < 0) {
            endIdx = poolText.length();
        }

        String raw = poolText.substring(contentStart, endIdx);
        return FinaleTextConverter.getInstance().getPlainText(raw);
    }


    /**
     * Builds a score from entry chains when the others pool cannot be parsed with
     * the traditional tag-based approach (binary .mus files use a different record
     * format). Each chain of linked entries becomes one part, and entries are divided
     * into measures based on accumulated duration.
     *
     * @param entries the parsed entry map
     * @param title the score title, or null
     * @param composer the composer name, or null
     * @param textPool the decompressed text pool for instrument names, or null
     * @return the constructed score
     */
    private IScore buildScoreFromEntryChains(Map<Integer, MusEntry> entries,
                                              String title, String composer, byte[] textPool) {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");

        IdentificationImpl identification = new IdentificationImpl();
        EncodingImpl encoding = new EncodingImpl();
        List<String> software = new ArrayList<>();
        software.add(EncodingConverter.getInstance().getSoftware());
        encoding.setSoftware(software);
        identification.setEncoding(encoding);

        if (composer != null && !composer.isEmpty()) {
            List<CreatorImpl> creators = new ArrayList<>();
            CreatorImpl creator = new CreatorImpl();
            creator.setType("composer");
            creator.setValue(composer);
            creators.add(creator);
            identification.setCreators(creators);
        }
        score.setIdentification(identification);

        if (title != null && !title.isEmpty()) {
            score.setMovementTitle(title);
        }

        java.util.Set<Integer> referencedNexts = new java.util.LinkedHashSet<>();
        for (MusEntry entry : entries.values()) {
            if (entry.getNext() > 0) {
                referencedNexts.add(entry.getNext());
            }
        }

        List<Integer> chainHeads = new ArrayList<>();
        for (MusEntry entry : entries.values()) {
            if (!referencedNexts.contains(entry.getEntnum())) {
                chainHeads.add(entry.getEntnum());
            }
        }

        if (chainHeads.isEmpty()) {
            chainHeads.add(entries.keySet().iterator().next());
        }

        List<String> instrumentNames = new ArrayList<>();
        if (textPool != null) {
            instrumentNames = parseTextBlockNames(textPool);
        }

        PartListImpl partList = new PartListImpl();
        List<ScorePartImpl> scoreParts = new ArrayList<>();
        List<PartImpl> parts = new ArrayList<>();

        for (int idx = 0; idx < chainHeads.size(); idx++) {
            String partId = "P" + (idx + 1);
            String partName = "Part " + (idx + 1);
            if (idx < instrumentNames.size() && !instrumentNames.get(idx).isEmpty()) {
                partName = instrumentNames.get(idx);
            }

            ScorePartImpl scorePart = new ScorePartImpl();
            scorePart.setId(partId);
            scorePart.setPartName(partName);
            scoreParts.add(scorePart);

            PartImpl part = buildPartFromChain(partId, chainHeads.get(idx), entries);
            parts.add(part);
        }

        partList.setScoreParts(scoreParts);
        score.setPartList(partList);
        score.setParts(parts);

        return score;
    }


    /**
     * Extracts instrument names from {@code ^block()} tags in the text pool.
     * These typically contain the part/instrument names used in the score.
     *
     * @param textPool the decompressed text pool data
     * @return list of instrument names in order of appearance
     */
    private List<String> parseTextBlockNames(byte[] textPool) {
        List<String> names = new ArrayList<>();
        String poolText = new String(textPool, StandardCharsets.ISO_8859_1);
        String marker = "^block()";
        int searchFrom = 0;

        while (searchFrom < poolText.length()) {
            int idx = poolText.indexOf(marker, searchFrom);
            if (idx < 0) {
                break;
            }

            int contentStart = idx + marker.length();
            int endIdx = poolText.indexOf("^end", contentStart);
            if (endIdx < 0) {
                endIdx = poolText.length();
            }

            String raw = poolText.substring(contentStart, endIdx);
            String name = FinaleTextConverter.getInstance().getPlainText(raw);
            if (name != null && !name.isEmpty()) {
                names.add(name);
            }

            searchFrom = endIdx + 1;
        }

        return names;
    }


    /**
     * Builds a single part by following an entry chain and dividing entries into
     * measures based on accumulated duration. Assumes 4/4 time signature with
     * divisions of 1024 per quarter note (4096 ticks per measure).
     *
     * @param partId the MusicXML part ID
     * @param chainHead the entry number of the first entry in the chain
     * @param entries the parsed entry map
     * @return the constructed part
     */
    private PartImpl buildPartFromChain(String partId, int chainHead, Map<Integer, MusEntry> entries) {
        PartImpl part = new PartImpl();
        part.setId(partId);

        int ticksPerMeasure = 4 * DIVISIONS;
        int measNum = 1;
        int accumulated = 0;

        List<MeasureImpl> measureList = new ArrayList<>();
        List<NoteImpl> currentNotes = new ArrayList<>();

        List<NoteImpl> allNotes = convertEntries(chainHead, entries, 0, null);

        for (NoteImpl note : allNotes) {
            int noteDura = note.getDuration();
            if (note.getGrace() != null) {
                noteDura = 0;
            }

            if (accumulated + noteDura > ticksPerMeasure && accumulated > 0) {
                MeasureImpl measure = new MeasureImpl();
                measure.setNumber(String.valueOf(measNum));
                if (measNum == 1) {
                    measure.setAttributes(Collections.singletonList(buildDefaultAttributes()));
                }
                measure.setNotes(currentNotes);
                measureList.add(measure);
                measNum++;
                currentNotes = new ArrayList<>();
                accumulated = 0;
            }

            currentNotes.add(note);
            accumulated += noteDura;
        }

        if (!currentNotes.isEmpty()) {
            MeasureImpl measure = new MeasureImpl();
            measure.setNumber(String.valueOf(measNum));
            if (measNum == 1) {
                measure.setAttributes(Collections.singletonList(buildDefaultAttributes()));
            }
            measure.setNotes(currentNotes);
            measureList.add(measure);
        }

        if (measureList.isEmpty()) {
            MeasureImpl emptyMeasure = new MeasureImpl();
            emptyMeasure.setNumber("1");
            emptyMeasure.setAttributes(Collections.singletonList(buildDefaultAttributes()));
            measureList.add(emptyMeasure);
        }

        part.setMeasures(measureList);
        return part;
    }


    /**
     * Builds a default attributes element with 4/4 time, treble clef, key of C major
     * and divisions of 1024. Used as a fallback when measure specs are not available.
     *
     * @return the default attributes
     */
    private AttributesImpl buildDefaultAttributes() {
        AttributesImpl attr = new AttributesImpl();
        attr.setDivisions(DIVISIONS);

        KeyImpl key = new KeyImpl();
        key.setFifths(0);
        attr.setKeys(Collections.singletonList(key));

        TimeImpl time = new TimeImpl();
        time.setBeats("4");
        time.setBeatType("4");
        attr.setTimes(Collections.singletonList(time));

        List<ClefImpl> clefs = new ArrayList<>();
        ClefImpl clef = new ClefImpl();
        clef.setSign(SIGN_G);
        clef.setLine(2);
        clefs.add(clef);
        attr.setClefs(clefs);

        return attr;
    }


    /**
     * Builds an IScore from the parsed binary data.
     *
     * @param measures the parsed measure specifications
     * @param staves the parsed staff specifications
     * @param groups the parsed multi-staff groups
     * @param entries the parsed note entries
     * @param gfholds the gfhold mappings (staff to measure to start entry)
     * @param title the score title, or null
     * @param composer the composer name, or null
     * @return the constructed score
     */
    private IScore buildScore(Map<Integer, MusMeasureSpec> measures,
                              Map<Integer, MusStaffSpec> staves,
                              Map<Integer, MusGroupInfo> groups,
                              Map<Integer, MusEntry> entries,
                              Map<String, Map<Integer, Integer>> gfholds,
                              String title, String composer) {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");

        IdentificationImpl identification = new IdentificationImpl();
        EncodingImpl encoding = new EncodingImpl();
        List<String> software = new ArrayList<>();
        software.add(EncodingConverter.getInstance().getSoftware());
        encoding.setSoftware(software);
        identification.setEncoding(encoding);

        if (composer != null && !composer.isEmpty()) {
            List<CreatorImpl> creators = new ArrayList<>();
            CreatorImpl creator = new CreatorImpl();
            creator.setType("composer");
            creator.setValue(composer);
            creators.add(creator);
            identification.setCreators(creators);
        }
        score.setIdentification(identification);

        if (title != null && !title.isEmpty()) {
            score.setMovementTitle(title);
        }

        java.util.Set<Integer> secondary = new java.util.LinkedHashSet<>();
        for (MusGroupInfo g : groups.values()) {
            secondary.add(g.getStaffNum2());
        }

        List<Integer> partStaves = new ArrayList<>();
        for (int cmper : staves.keySet()) {
            if (!secondary.contains(cmper)) {
                partStaves.add(cmper);
            }
        }

        PartListImpl partList = new PartListImpl();
        List<ScorePartImpl> scoreParts = new ArrayList<>();
        List<PartImpl> parts = new ArrayList<>();

        for (int idx = 0; idx < partStaves.size(); idx++) {
            int staffCmper = partStaves.get(idx);
            String partId = "P" + (idx + 1);

            ScorePartImpl scorePart = new ScorePartImpl();
            scorePart.setId(partId);
            scorePart.setPartName("Staff " + staffCmper);
            scoreParts.add(scorePart);

            List<Integer> groupedStaves = new ArrayList<>();
            groupedStaves.add(staffCmper);
            for (MusGroupInfo g : groups.values()) {
                if (g.getStaffNum1() == staffCmper && staves.containsKey(g.getStaffNum2())) {
                    groupedStaves.add(g.getStaffNum2());
                    break;
                }
            }

            PartImpl part = buildPart(partId, groupedStaves, measures, entries, gfholds, staves);
            parts.add(part);
        }

        partList.setScoreParts(scoreParts);
        score.setPartList(partList);
        score.setParts(parts);

        return score;
    }


    /**
     * Builds a part with all its measures from the parsed binary data.
     *
     * @param partId the MusicXML part ID
     * @param staffCmpers the staff cmpers belonging to this part
     * @param measures the measure specifications
     * @param entries the note entries
     * @param gfholds the gfhold mappings
     * @param staves the staff specifications
     * @return the constructed part
     */
    private PartImpl buildPart(String partId, List<Integer> staffCmpers,
                               Map<Integer, MusMeasureSpec> measures,
                               Map<Integer, MusEntry> entries,
                               Map<String, Map<Integer, Integer>> gfholds,
                               Map<Integer, MusStaffSpec> staves) {
        PartImpl part = new PartImpl();
        part.setId(partId);

        boolean multiStaff = staffCmpers.size() > 1;
        List<MeasureImpl> measureList = new ArrayList<>();
        int prevKey = Integer.MIN_VALUE;
        int prevBeats = -1;
        int prevDivBeat = -1;

        List<Integer> measureNumbers = new ArrayList<>(measures.keySet());
        Collections.sort(measureNumbers);

        for (int measNum : measureNumbers) {
            MusMeasureSpec meas = measures.get(measNum);
            MeasureImpl measure = new MeasureImpl();
            measure.setNumber(String.valueOf(measNum));

            boolean needAttributes = (measNum == measureNumbers.get(0))
                    || meas.getKey() != prevKey
                    || meas.getBeats() != prevBeats
                    || meas.getDivBeat() != prevDivBeat;

            if (needAttributes) {
                AttributesImpl attr = new AttributesImpl();

                if (measNum == measureNumbers.get(0)) {
                    attr.setDivisions(DIVISIONS);
                    if (multiStaff) {
                        attr.setStaves(staffCmpers.size());
                    }

                    List<ClefImpl> clefs = new ArrayList<>();
                    for (int si = 0; si < staffCmpers.size(); si++) {
                        MusStaffSpec staffSpec = staves.get(staffCmpers.get(si));
                        int clefId = 0;
                        if (staffSpec != null) {
                            clefId = staffSpec.getDefaultClef();
                        }
                        ClefImpl clef = FinaleClefConverter.getInstance().getClef(clefId);
                        if (multiStaff) {
                            clef.setNumber(si + 1);
                        }
                        clefs.add(clef);
                    }
                    attr.setClefs(clefs);
                }

                if (meas.getKey() != prevKey) {
                    KeyImpl key = new KeyImpl();
                    key.setFifths(meas.getKey());
                    attr.setKeys(Collections.singletonList(key));
                }

                if (meas.getBeats() != prevBeats || meas.getDivBeat() != prevDivBeat) {
                    TimeImpl time = new TimeImpl();
                    time.setBeats(String.valueOf(meas.getBeats()));
                    int beatType = eduToBeatType(meas.getDivBeat());
                    time.setBeatType(String.valueOf(beatType));
                    attr.setTimes(Collections.singletonList(time));
                }

                measure.setAttributes(Collections.singletonList(attr));
                prevKey = meas.getKey();
                prevBeats = meas.getBeats();
                prevDivBeat = meas.getDivBeat();
            }

            List<IMusicDataElement> content = new ArrayList<>();
            for (int si = 0; si < staffCmpers.size(); si++) {
                int staffCmper = staffCmpers.get(si);

                if (si > 0) {
                    BackupImpl backup = new BackupImpl();
                    backup.setDuration(meas.getBeats() * meas.getDivBeat());
                    content.add(backup);
                }

                List<NoteImpl> notes = new ArrayList<>();
                Map<Integer, Integer> staffGfholds = resolveGfholds(staffCmper, gfholds);
                Integer startEntry = null;
                if (staffGfholds != null) {
                    startEntry = staffGfholds.get(measNum);
                }

                if (startEntry != null && startEntry > 0) {
                    Integer staffNumber = null;
                    if (multiStaff) {
                        staffNumber = si + 1;
                    }
                    List<NoteImpl> entryNotes = convertEntries(startEntry, entries, meas.getKey(), staffNumber);
                    notes.addAll(entryNotes);
                }

                if (notes.isEmpty()) {
                    Integer restStaffNumber = null;
                    if (multiStaff) {
                        restStaffNumber = si + 1;
                    }
                    NoteImpl wholeRest = createWholeRest(meas.getBeats() * meas.getDivBeat(), restStaffNumber);
                    notes.add(wholeRest);
                }
                content.addAll(notes);
            }

            for (IMusicDataElement element : content) {
                measure.addMusicData(element);
            }
            for (BarlineImpl barline : convertBarline(meas)) {
                measure.addMusicData(barline);
            }
            measureList.add(measure);
        }

        part.setMeasures(measureList);
        return part;
    }


    /**
     * Resolves the gfhold map for a given staff. Tries the staff cmper as key first,
     * falls back to the default staff list (32767).
     *
     * @param staffCmper the staff number
     * @param gfholds the complete gfhold map
     * @return the measure-to-startEntry map, or null
     */
    private Map<Integer, Integer> resolveGfholds(int staffCmper,
                                                  Map<String, Map<Integer, Integer>> gfholds) {
        String staffKey = String.valueOf(staffCmper);
        Map<Integer, Integer> result = gfholds.get(staffKey);
        if (result != null && !result.isEmpty()) {
            return result;
        }
        return gfholds.get(String.valueOf(SPECIAL_STAFF));
    }


    /**
     * Converts a chain of binary entries starting from the given entry number into
     * MusicXML notes.
     *
     * @param startEntry the starting entry number
     * @param entries the parsed entry map
     * @param keyFifths the current key signature
     * @param staff the staff number, or null for single-staff parts
     * @return list of notes
     */
    private List<NoteImpl> convertEntries(int startEntry, Map<Integer, MusEntry> entries,
                                           int keyFifths, Integer staff) {
        List<NoteImpl> result = new ArrayList<>();
        int entnum = startEntry;
        int safety = 0;

        while (entnum > 0 && safety < 10000) {
            safety++;
            MusEntry entry = entries.get(entnum);
            if (entry == null) {
                break;
            }

            boolean isValid = (entry.getFlags() & EF_VALID) != 0;
            if (!isValid) {
                entnum = entry.getNext();
                continue;
            }

            boolean isNote = (entry.getFlags() & EF_NOTE) != 0;
            boolean isGraceNote = (entry.getFlags() & EF_GRACE_NOTE) != 0;
            boolean isVoice2 = (entry.getFlags() & EF_VOICE2) != 0;
            boolean slashGrace = (entry.getExtendedFlags() & XEF_SLASH_GRACE) != 0;
            int voice = 1;
            if (isVoice2) {
                voice = 2;
            }
            int dura = entry.getDura();

            if (!isNote || entry.getNotes().isEmpty()) {
                NoteImpl restNote = new NoteImpl();
                restNote.setRest(new RestImpl());
                restNote.setDuration(dura);
                restNote.setType(duraToType(dura));
                restNote.setVoice(voice);
                if (staff != null) {
                    restNote.setStaff(staff);
                }
                addDots(restNote, dura);
                result.add(restNote);
            } else {
                for (int ni = 0; ni < entry.getNotes().size(); ni++) {
                    MusNote musNote = entry.getNotes().get(ni);
                    if ((musNote.getFlags() & NF_VALID) == 0) {
                        continue;
                    }

                    NoteImpl note = new NoteImpl();

                    if (ni > 0) {
                        note.setChord("");
                    }

                    if (isGraceNote) {
                        GraceImpl grace = new GraceImpl();
                        if (slashGrace) {
                            grace.setSlash("yes");
                        }
                        note.setGrace(grace);
                        note.setDuration(0);
                    }

                    PitchImpl pitch = convertPitch(musNote.getHarmLev(), musNote.getHarmAlt(), keyFifths);
                    note.setPitch(pitch);
                    if (!isGraceNote) {
                        note.setDuration(dura);
                    }
                    note.setType(duraToType(dura));
                    note.setVoice(voice);
                    if (staff != null) {
                        note.setStaff(staff);
                    }
                    if (!isGraceNote) {
                        addDots(note, dura);
                    }

                    boolean hasTieStart = (musNote.getFlags() & NF_TIE_START) != 0;
                    boolean hasTieEnd = (musNote.getFlags() & NF_TIE_END) != 0;
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
                            tieEnd.setType("stop");
                            ties.add(tieEnd);
                            TiedImpl tiedEnd = new TiedImpl();
                            tiedEnd.setType("stop");
                            tieds.add(tiedEnd);
                        }

                        note.setTies(ties);
                        NotationsImpl notations = new NotationsImpl();
                        notations.setTieds(tieds);
                        note.setNotations(notations);
                    }

                    result.add(note);
                }
            }

            entnum = entry.getNext();
        }

        return result;
    }


    /**
     * Converts barline and repeat information from a binary measure spec into
     * MusicXML barline elements.
     *
     * @param meas the binary measure specification
     * @return list of barline elements (may be empty)
     */
    private List<BarlineImpl> convertBarline(MusMeasureSpec meas) {
        List<BarlineImpl> barlines = new ArrayList<>();

        boolean forwardRepeat = (meas.getSecondaryFlags() & MEAS_SEC_FORWARD_REPEAT) != 0;
        boolean backwardRepeat = (meas.getSecondaryFlags() & MEAS_SEC_BACKWARD_REPEAT) != 0;

        if (forwardRepeat) {
            BarlineImpl barline = new BarlineImpl();
            barline.setLocation("left");
            barline.setBarStyle("heavy-light");
            RepeatImpl repeat = new RepeatImpl();
            repeat.setDirection("forward");
            barline.setRepeat(repeat);
            barlines.add(barline);
        }

        if (backwardRepeat) {
            BarlineImpl barline = new BarlineImpl();
            barline.setLocation("right");
            barline.setBarStyle("light-heavy");
            RepeatImpl repeat = new RepeatImpl();
            repeat.setDirection("backward");
            barline.setRepeat(repeat);
            barlines.add(barline);
        }

        int barlineCode = (meas.getSecondaryFlags() & MEAS_SEC_BARLINE_MASK) >> MEAS_SEC_BARLINE_SHIFT;
        String barStyle = mapBarlineCode(barlineCode);
        if (barStyle != null) {
            BarlineImpl barline = new BarlineImpl();
            barline.setLocation("right");
            barline.setBarStyle(barStyle);
            barlines.add(barline);
        }

        return barlines;
    }


    /**
     * Maps a binary barline code to a MusicXML bar-style string.
     *
     * @param code the barline code from the secondary flags
     * @return the MusicXML bar-style, or null if no explicit barline is needed
     */
    private String mapBarlineCode(int code) {
        if (code == 1) {
            return "light-light";
        } else if (code == 2) {
            return "light-heavy";
        } else if (code == 3) {
            return "none";
        } else if (code == 4) {
            return "dashed";
        } else if (code == 5) {
            return "heavy";
        } else if (code == 6) {
            return "tick";
        }
        return null;
    }


    /**
     * Creates a whole-measure rest note.
     *
     * @param duration the rest duration in divisions
     * @param staff the staff number, or null for single-staff parts
     * @return the rest note
     */
    private NoteImpl createWholeRest(int duration, Integer staff) {
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
     * Converts harmLev/harmAlt to a MusicXML PitchImpl.
     *
     * @param harmLev the diatonic displacement from middle C
     * @param harmAlt the chromatic alteration from the key signature
     * @param keyFifths the key signature (number of fifths)
     * @return the pitch
     */
    private PitchImpl convertPitch(int harmLev, int harmAlt, int keyFifths) {
        int step = harmLev % 7;
        if (step < 0) {
            step += 7;
        }
        int octave = 4 + (harmLev - step) / 7;

        int[] keyAccidentals = computeKeyAccidentals(keyFifths);
        double alter = keyAccidentals[step] + harmAlt;

        PitchImpl pitch = new PitchImpl();
        pitch.setStep(STEP_NAMES[step]);
        pitch.setOctave(octave);
        if (alter != 0.0) {
            pitch.setAlter(alter);
        }
        return pitch;
    }


    /**
     * Computes the accidentals for each diatonic step from a key signature.
     *
     * @param fifths the number of fifths
     * @return array of 7 accidentals
     */
    private int[] computeKeyAccidentals(int fifths) {
        int[] acc = new int[7];
        int[] sharpOrder = {3, 0, 4, 1, 5, 2, 6};
        int[] flatOrder = {6, 2, 5, 1, 4, 0, 3};

        if (fifths > 0) {
            int count = Math.min(fifths, 7);
            for (int i = 0; i < count; i++) {
                acc[sharpOrder[i]] = 1;
            }
        } else if (fifths < 0) {
            int count = Math.min(-fifths, 7);
            for (int i = 0; i < count; i++) {
                acc[flatOrder[i]] = -1;
            }
        }
        return acc;
    }


    /**
     * Converts a Finale EDU divbeat value to a MusicXML beat-type denominator.
     *
     * @param divbeat the beat duration in EDU
     * @return the MusicXML beat-type value
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
     * @param dura the duration in EDU
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
        }
        return "128th";
    }


    /**
     * Strips dot extensions from a duration to find the base duration.
     *
     * @param dura the duration in EDU
     * @return the base duration
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
     * Reads an unsigned 16-bit integer from the data at the given offset.
     *
     * @param data the byte array
     * @param offset the byte offset
     * @param byteOrder the byte order
     * @return the unsigned 16-bit value
     */
    private int readU16(byte[] data, int offset, ByteOrder byteOrder) {
        if (offset + 2 > data.length) {
            return 0;
        }
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
        }
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }


    /**
     * Reads a signed 16-bit integer from the data at the given offset.
     *
     * @param data the byte array
     * @param offset the byte offset
     * @param byteOrder the byte order
     * @return the signed 16-bit value
     */
    private int readS16(byte[] data, int offset, ByteOrder byteOrder) {
        int val = readU16(data, offset, byteOrder);
        if (val >= 0x8000) {
            val -= 0x10000;
        }
        return val;
    }


    /**
     * Reads an unsigned 32-bit integer from the data at the given offset.
     *
     * @param data the byte array
     * @param offset the byte offset
     * @param byteOrder the byte order
     * @return the unsigned 32-bit value (as int; values above 2^31 wrap to negative)
     */
    private int readU32(byte[] data, int offset, ByteOrder byteOrder) {
        if (offset + 4 > data.length) {
            return 0;
        }
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            return (data[offset] & 0xFF)
                    | ((data[offset + 1] & 0xFF) << 8)
                    | ((data[offset + 2] & 0xFF) << 16)
                    | ((data[offset + 3] & 0xFF) << 24);
        }
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }


    /**
     * Concatenates two byte arrays.
     *
     * @param a the first array
     * @param b the second array
     * @return the concatenated array
     */
    private byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }


    /**
     * Holds parsed binary measure specification data.
     *
     * @author patrick
     */
    private static final class MusMeasureSpec {
        private int cmper;
        private int width;
        private int key;
        private int beats;
        private int divBeat;
        private int primaryFlags;
        private int secondaryFlags;


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
         * Gets the key signature as number of fifths.
         *
         * @return the key signature
         */
        int getKey() {
            return key;
        }


        /**
         * Sets the key signature as number of fifths.
         *
         * @param key the key signature
         */
        void setKey(int key) {
            this.key = key;
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
         * Gets the beat division (EDU per beat).
         *
         * @return the beat division
         */
        int getDivBeat() {
            return divBeat;
        }


        /**
         * Sets the beat division (EDU per beat).
         *
         * @param divBeat the beat division
         */
        void setDivBeat(int divBeat) {
            this.divBeat = divBeat;
        }


        /**
         * Gets the primary measure flags.
         *
         * @return the primary flags
         */
        int getPrimaryFlags() {
            return primaryFlags;
        }


        /**
         * Sets the primary measure flags.
         *
         * @param primaryFlags the primary flags
         */
        void setPrimaryFlags(int primaryFlags) {
            this.primaryFlags = primaryFlags;
        }


        /**
         * Gets the secondary measure flags.
         *
         * @return the secondary flags
         */
        int getSecondaryFlags() {
            return secondaryFlags;
        }


        /**
         * Sets the secondary measure flags.
         *
         * @param secondaryFlags the secondary flags
         */
        void setSecondaryFlags(int secondaryFlags) {
            this.secondaryFlags = secondaryFlags;
        }
    }


    /**
     * Holds parsed binary staff specification data.
     *
     * @author patrick
     */
    private static final class MusStaffSpec {
        private int cmper;
        private int defaultClef;
        private int transInterval;
        private int nameRef;


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
         * Gets the staff name reference.
         *
         * @return the staff name reference
         */
        int getNameRef() {
            return nameRef;
        }


        /**
         * Sets the staff name reference.
         *
         * @param nameRef the staff name reference
         */
        void setNameRef(int nameRef) {
            this.nameRef = nameRef;
        }
    }


    /**
     * Holds parsed multi-staff group data.
     *
     * @author patrick
     */
    private static final class MusGroupInfo {
        private int staffNum1;
        private int staffNum2;


        /**
         * Gets the first staff number in the group.
         *
         * @return the first staff number
         */
        int getStaffNum1() {
            return staffNum1;
        }


        /**
         * Sets the first staff number in the group.
         *
         * @param staffNum1 the first staff number
         */
        void setStaffNum1(int staffNum1) {
            this.staffNum1 = staffNum1;
        }


        /**
         * Gets the second staff number in the group.
         *
         * @return the second staff number
         */
        int getStaffNum2() {
            return staffNum2;
        }


        /**
         * Sets the second staff number in the group.
         *
         * @param staffNum2 the second staff number
         */
        void setStaffNum2(int staffNum2) {
            this.staffNum2 = staffNum2;
        }
    }


    /**
     * Holds a parsed binary entry (note/rest event).
     *
     * @author patrick
     */
    private static final class MusEntry {
        private int entnum;
        private int prev;
        private int next;
        private int dura;
        private int flags;
        private int extendedFlags;
        private int noteCount;
        private List<MusNote> notes;


        /**
         * Gets the entry number.
         *
         * @return the entry number
         */
        int getEntnum() {
            return entnum;
        }


        /**
         * Sets the entry number.
         *
         * @param entnum the entry number
         */
        void setEntnum(int entnum) {
            this.entnum = entnum;
        }


        /**
         * Gets the previous entry number in the linked list.
         *
         * @return the previous entry number
         */
        int getPrev() {
            return prev;
        }


        /**
         * Sets the previous entry number in the linked list.
         *
         * @param prev the previous entry number
         */
        void setPrev(int prev) {
            this.prev = prev;
        }


        /**
         * Gets the next entry number in the linked list.
         *
         * @return the next entry number
         */
        int getNext() {
            return next;
        }


        /**
         * Sets the next entry number in the linked list.
         *
         * @param next the next entry number
         */
        void setNext(int next) {
            this.next = next;
        }


        /**
         * Gets the duration in EDU.
         *
         * @return the duration
         */
        int getDura() {
            return dura;
        }


        /**
         * Sets the duration in EDU.
         *
         * @param dura the duration
         */
        void setDura(int dura) {
            this.dura = dura;
        }


        /**
         * Gets the entry flags.
         *
         * @return the entry flags
         */
        int getFlags() {
            return flags;
        }


        /**
         * Sets the entry flags.
         *
         * @param flags the entry flags
         */
        void setFlags(int flags) {
            this.flags = flags;
        }


        /**
         * Gets the extended entry flags.
         *
         * @return the extended flags
         */
        int getExtendedFlags() {
            return extendedFlags;
        }


        /**
         * Sets the extended entry flags.
         *
         * @param extendedFlags the extended flags
         */
        void setExtendedFlags(int extendedFlags) {
            this.extendedFlags = extendedFlags;
        }


        /**
         * Gets the number of notes in this entry.
         *
         * @return the note count
         */
        int getNoteCount() {
            return noteCount;
        }


        /**
         * Sets the number of notes in this entry.
         *
         * @param noteCount the note count
         */
        void setNoteCount(int noteCount) {
            this.noteCount = noteCount;
        }


        /**
         * Gets the list of notes in this entry.
         *
         * @return the notes
         */
        List<MusNote> getNotes() {
            return notes;
        }


        /**
         * Sets the list of notes in this entry.
         *
         * @param notes the notes
         */
        void setNotes(List<MusNote> notes) {
            this.notes = notes;
        }
    }


    /**
     * Holds a parsed binary note (individual pitch within an entry).
     *
     * @author patrick
     */
    private static final class MusNote {
        private int harmLev;
        private int harmAlt;
        private int flags;
        private int noteId;


        /**
         * Gets the harmonic level (diatonic displacement from middle C).
         *
         * @return the harmonic level
         */
        int getHarmLev() {
            return harmLev;
        }


        /**
         * Sets the harmonic level (diatonic displacement from middle C).
         *
         * @param harmLev the harmonic level
         */
        void setHarmLev(int harmLev) {
            this.harmLev = harmLev;
        }


        /**
         * Gets the harmonic alteration (chromatic offset from key signature).
         *
         * @return the harmonic alteration
         */
        int getHarmAlt() {
            return harmAlt;
        }


        /**
         * Sets the harmonic alteration (chromatic offset from key signature).
         *
         * @param harmAlt the harmonic alteration
         */
        void setHarmAlt(int harmAlt) {
            this.harmAlt = harmAlt;
        }


        /**
         * Gets the note flags.
         *
         * @return the note flags
         */
        int getFlags() {
            return flags;
        }


        /**
         * Sets the note flags.
         *
         * @param flags the note flags
         */
        void setFlags(int flags) {
            this.flags = flags;
        }


        /**
         * Gets the note identifier.
         *
         * @return the note identifier
         */
        int getNoteId() {
            return noteId;
        }


        /**
         * Sets the note identifier.
         *
         * @param noteId the note identifier
         */
        void setNoteId(int noteId) {
            this.noteId = noteId;
        }
    }
}
