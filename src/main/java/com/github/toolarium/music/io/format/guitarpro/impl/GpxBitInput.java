/*
 * GpxBitInput.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl;

import java.io.IOException;


/**
 * Reads bits from a byte array, most significant bit first.
 *
 * @author patrick
 */
final class GpxBitInput {
    private static final int BITS_PER_BYTE = 8;
    private final byte[] data;
    private int bytePosition;
    private int bitPosition;


    /**
     * Constructor for GpxBitInput.
     *
     * @param data the data
     * @param start the index of the first byte
     */
    GpxBitInput(byte[] data, int start) {
        this.data = data;
        this.bytePosition = start;
    }


    /**
     * Checks if more bits are available.
     *
     * @return true if the end is not reached
     */
    boolean hasMore() {
        return bytePosition < data.length;
    }


    /**
     * Checks if a number of bits is available.
     *
     * @param count the number of bits
     * @return true if the bits can be read
     */
    boolean hasBits(int count) {
        long available = (long) (data.length - bytePosition) * BITS_PER_BYTE - bitPosition;
        return available >= count;
    }


    /**
     * Reads one bit.
     *
     * @return the bit
     * @throws IOException at the end of the data
     */
    int readBit() throws IOException {
        if (bytePosition >= data.length) {
            throw new IOException("Invalid Guitar Pro 6 container: unexpected end of compressed data.");
        }
        int bit = (data[bytePosition] >> (BITS_PER_BYTE - 1 - bitPosition)) & 1;
        bitPosition++;
        if (bitPosition == BITS_PER_BYTE) {
            bitPosition = 0;
            bytePosition++;
        }
        return bit;
    }


    /**
     * Reads bits, most significant first.
     *
     * @param count the number of bits
     * @return the value
     * @throws IOException at the end of the data
     */
    int readBits(int count) throws IOException {
        int value = 0;
        for (int i = count - 1; i >= 0; i--) {
            value |= readBit() << i;
        }
        return value;
    }


    /**
     * Reads bits, least significant first.
     *
     * @param count the number of bits
     * @return the value
     * @throws IOException at the end of the data
     */
    int readBitsReversed(int count) throws IOException {
        int value = 0;
        for (int i = 0; i < count; i++) {
            value |= readBit() << i;
        }
        return value;
    }
}
