/*
 * SibeliusCipher.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl;


/**
 * Stream cipher for decrypting Sibelius .sib and Scorch .sco file payloads.
 *
 * <p>The cipher uses two combined Linear Congruential Generators (LCGs) with a
 * mode selector that determines which generators contribute to each output byte.
 * Some files of Sibelius 1 and 2 use a simple cipher instead: a single LCG seeded
 * with the cipher seed. The algorithm was first publicly documented in the SCORA
 * project (cw2k).</p>
 *
 * @author patrick
 */
final class SibeliusCipher {
    private static final int MAGIC_XOR_1 = 0x1F30D27B;
    private static final int MAGIC_XOR_2 = 0x3A4C80B2;
    private static final int LCG1_MULT = 214013;
    private static final int LCG1_INC = 2531011;
    private static final int LCG2_MULT = 1103515245;
    private static final int LCG2_INC = 12345;
    private static final int SIB_MODE_MULT = 13821;
    private static final int SIB_MODE_MASK = 0x7FFF;
    private static final int SCO_MODE_MULT = 47485;
    private static final int SCO_MODE_MASK = 0xFFFF;
    private static final int[] POST_XOR = {27811, 12742, 37606, 5005};

    private int encryptionKey1;
    private int encryptionKey2;
    private int encryptionKeyMode;
    private int simpleKey;
    private final boolean isScorch;
    private final boolean simple;


    /**
     * Constructor.
     *
     * @param seed the 32-bit cipher seed from the file header
     * @param isScorch true for Scorch (.sco) files, false for Sibelius (.sib) files
     */
    SibeliusCipher(int seed, boolean isScorch) {
        this(seed, isScorch, false);
    }


    /**
     * Constructor.
     *
     * @param seed the 32-bit cipher seed from the file header
     * @param isScorch true for Scorch (.sco) files, false for Sibelius (.sib) files
     * @param simple true for the simple cipher (a single LCG) used by some Sibelius 1 and 2 files
     */
    SibeliusCipher(int seed, boolean isScorch, boolean simple) {
        this.isScorch = isScorch;
        this.simple = simple;
        this.simpleKey = seed;
        this.encryptionKey1 = (seed & 0xFFFFFFF8) ^ MAGIC_XOR_1;
        this.encryptionKey2 = (seed & 0xFFFFFFF8) ^ MAGIC_XOR_2;
        this.encryptionKeyMode = (seed & 7) << 4;
    }


    /**
     * Generates the next pseudo-random value from the keystream.
     *
     * @return the next 15-bit PRNG value
     */
    int nextValue() {
        if (simple) {
            simpleKey = simpleKey * LCG1_MULT + LCG1_INC;
            return (simpleKey >> 16) & 0x7FFF;
        }

        if (isScorch) {
            encryptionKeyMode = (encryptionKeyMode * SCO_MODE_MULT) & SCO_MODE_MASK;
        } else {
            encryptionKeyMode = (encryptionKeyMode * SIB_MODE_MULT) & SIB_MODE_MASK;
        }

        int modeSelect = (encryptionKeyMode >> 8) & 3;
        int combined = 0;

        if (modeSelect != 2) {
            encryptionKey1 = encryptionKey1 * LCG1_MULT + LCG1_INC;
            combined = encryptionKey1;
        }

        if (modeSelect != 1) {
            encryptionKey2 = encryptionKey2 * LCG2_MULT + LCG2_INC;
            combined ^= encryptionKey2;
        }

        return POST_XOR[modeSelect] ^ ((combined >> 16) & 0x7FFF);
    }


    /**
     * Decrypts a single byte using the keystream.
     *
     * @param encrypted the encrypted byte
     * @return the decrypted byte
     */
    byte decrypt(byte encrypted) {
        return (byte) (encrypted ^ nextValue());
    }
}
