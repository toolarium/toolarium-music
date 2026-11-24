/*
 * EnigmaXmlDecryptor.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Decrypts and decompresses Finale .musx score.dat files to produce EnigmaXML.
 *
 * <p>The score.dat inside a .musx ZIP archive is encrypted with a stream cipher
 * (LCG-based XOR keystream) and then gzip-compressed. This class reverses
 * both transformations to yield the raw EnigmaXML content.</p>
 *
 * <p>The cipher parameters were originally documented by the denigma project
 * (MIT license, credits Deguerre for discovery).</p>
 *
 * @author patrick
 * @see <a href="https://github.com/openmusx/denigma">
 *     denigma project</a>
 */
public final class EnigmaXmlDecryptor {
    private static final Logger LOG = LoggerFactory.getLogger(EnigmaXmlDecryptor.class);

    private static final long INITIAL_STATE = 0x28006D45L;
    private static final long MULTIPLIER = 0x41C64E6DL;
    private static final long INCREMENT = 0x3039L;
    private static final int RESET_EVERY = 0x20000;
    private static final long MASK_32 = 0xFFFFFFFFL;
    private static final int BUFFER_SIZE = 8192;
    private static final long MAX_DECOMPRESSED_SIZE = 64L * 1024 * 1024;
    private static final int GZIP_MAGIC_0 = 0x1F;
    private static final int GZIP_MAGIC_1 = 0x8B;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final EnigmaXmlDecryptor INSTANCE = new EnigmaXmlDecryptor();
    }


    /**
     * Constructor.
     */
    private EnigmaXmlDecryptor() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static EnigmaXmlDecryptor getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Decrypts and decompresses a score.dat byte array to produce EnigmaXML.
     *
     * @param encryptedData the raw score.dat bytes from the .musx ZIP archive
     * @return the decompressed EnigmaXML content as a byte array
     * @throws IOException if decryption or decompression fails
     */
    public byte[] decrypt(byte[] encryptedData) throws IOException {
        if (encryptedData == null || encryptedData.length == 0) {
            throw new IOException("score.dat is empty or null.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Decrypting score.dat: {} bytes.", encryptedData.length);
        }

        byte[] decrypted = xorDecrypt(encryptedData);

        if (decrypted.length < 2
                || (decrypted[0] & 0xFF) != GZIP_MAGIC_0
                || (decrypted[1] & 0xFF) != GZIP_MAGIC_1) {
            throw new IOException("Decrypted score.dat does not start with gzip magic bytes.");
        }

        byte[] xml = decompress(decrypted);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Decompressed EnigmaXML: {} bytes (ratio: {}).",
                    xml.length, String.format("%.1fx", (double) xml.length / encryptedData.length));
        }

        return xml;
    }


    /**
     * Applies the LCG-based XOR stream cipher to decrypt or encrypt the data.
     * The cipher is symmetric: applying it twice yields the original data.
     *
     * @param data the input data
     * @return the XOR-decrypted data
     */
    byte[] xorDecrypt(byte[] data) {
        byte[] out = new byte[data.length];
        long state = INITIAL_STATE;

        for (int i = 0; i < data.length; i++) {
            if (i % RESET_EVERY == 0) {
                state = INITIAL_STATE;
            }
            state = ((state * MULTIPLIER) + INCREMENT) & MASK_32;
            int upper = (int) ((state >> 16) & 0xFFFF);
            int keystreamByte = (upper + (upper / 255)) & 0xFF;
            out[i] = (byte) ((data[i] & 0xFF) ^ keystreamByte);
        }

        return out;
    }


    /**
     * Decompresses a gzip-compressed byte array with a size limit.
     *
     * @param compressed the gzip-compressed data
     * @return the decompressed data
     * @throws IOException if decompression fails or the output exceeds the size limit
     */
    private byte[] decompress(byte[] compressed) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            long total = 0;
            int len;

            while ((len = gis.read(buffer)) != -1) {
                total += len;
                if (total > MAX_DECOMPRESSED_SIZE) {
                    throw new IOException("Decompressed EnigmaXML exceeds maximum size ("
                            + MAX_DECOMPRESSED_SIZE + " bytes).");
                }
                baos.write(buffer, 0, len);
            }

            return baos.toByteArray();
        }
    }
}
