/*
 * MusRecord.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl.dto;

import java.util.Arrays;
import java.util.Objects;


/**
 * A record of the others or details pool of a Finale .mus file: the tag, the key (cmper, or cmper1 and cmper2),
 * the linked part (0 for the score) and the data as little-endian 16 bit values.
 *
 * @author patrick
 */
public class MusRecord {
    private static final int WORD_MASK = 0xFFFF;
    private static final int WORD_BITS = 16;
    private final int tag;
    private final int cmper1;
    private final int cmper2;
    private final int part;
    private final short[] values;


    /**
     * Constructor for MusRecord.
     *
     * @param tag the record tag
     * @param cmper1 the first key (the cmper of an others record)
     * @param cmper2 the second key (0 for an others record)
     * @param part the linked part, 0 for the score
     * @param values the data as 16 bit values
     */
    public MusRecord(int tag, int cmper1, int cmper2, int part, short[] values) {
        this.tag = tag;
        this.cmper1 = cmper1;
        this.cmper2 = cmper2;
        this.part = part;
        this.values = values.clone();
    }


    /**
     * Gets the record tag.
     *
     * @return the tag
     */
    public int getTag() {
        return tag;
    }


    /**
     * Gets the first key: the cmper of an others record, the staff of a gfhold or the high word of an entry number.
     *
     * @return the first key
     */
    public int getCmper1() {
        return cmper1;
    }


    /**
     * Gets the second key: the measure of a gfhold or the low word of an entry number.
     *
     * @return the second key
     */
    public int getCmper2() {
        return cmper2;
    }


    /**
     * Gets the entry number of an entry detail record.
     *
     * @return the entry number
     */
    public int getEntryNumber() {
        return (cmper1 << WORD_BITS) | (cmper2 & WORD_MASK);
    }


    /**
     * Gets the linked part.
     *
     * @return the linked part, 0 for the score
     */
    public int getPart() {
        return part;
    }


    /**
     * Gets the number of 16 bit values.
     *
     * @return the number of values
     */
    public int size() {
        return values.length;
    }


    /**
     * Gets a signed 16 bit value.
     *
     * @param index the index of the value
     * @return the value, or 0 if the record is shorter
     */
    public int get(int index) {
        if (index < 0 || index >= values.length) {
            return 0;
        }
        return values[index];
    }


    /**
     * Gets a 32 bit value from two 16 bit values (low word first).
     *
     * @param index the index of the low word
     * @return the value
     */
    public int getInt(int index) {
        return (get(index) & WORD_MASK) | (get(index + 1) << WORD_BITS);
    }


    /**
     * Gets a part of the data, for a record that holds several items of the same size (incis).
     *
     * @param index the index of the item
     * @param itemSize the number of 16 bit values per item
     * @return the item as a record with the same tag and keys
     */
    public MusRecord item(int index, int itemSize) {
        short[] item = new short[itemSize];
        for (int i = 0; i < itemSize; i++) {
            item[i] = (short) get(index * itemSize + i);
        }
        return new MusRecord(tag, cmper1, cmper2, part, item);
    }


    /**
     * Gets the data as bytes.
     *
     * @return the data in little-endian byte order
     */
    public byte[] getBytes() {
        byte[] result = new byte[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            result[2 * i] = (byte) values[i];
            result[2 * i + 1] = (byte) (values[i] >> (WORD_BITS / 2));
        }
        return result;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(tag, cmper1, cmper2, part) * 31 + Arrays.hashCode(values);
    }


    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param obj the reference object to compare
     * @return true if this object is the same as the obj argument, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MusRecord other = (MusRecord) obj;
        return tag == other.tag && cmper1 == other.cmper1 && cmper2 == other.cmper2 && part == other.part && Arrays.equals(values, other.values);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "MusRecord [tag=" + tag + ", cmper1=" + cmper1 + ", cmper2=" + cmper2 + ", part=" + part + ", values=" + Arrays.toString(values) + "]";
    }
}
