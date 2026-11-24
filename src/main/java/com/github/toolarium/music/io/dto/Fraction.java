/*
 * Fraction.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.dto;

import java.util.Objects;


/**
 * Immutable rational number used for musical time positions and durations, shared by the
 * format readers and writers. The fraction is always kept reduced with a positive denominator.
 *
 * <p>The unit depends on the caller: MuseScore uses fractions of a whole note (1/4 is a quarter
 * note), Dorico uses quarter notes (1/2 is an eighth note). {@link #toDivisions(long)} and
 * {@link #fromDivisions(long, long)} assume fractions of a whole note.</p>
 *
 * @author patrick
 */
public final class Fraction implements Comparable<Fraction> {

    /** The fraction zero. */
    public static final Fraction ZERO = new Fraction(0, 1);

    private final long numerator;
    private final long denominator;


    /**
     * Constructor.
     *
     * @param numerator the numerator
     * @param denominator the denominator, must not be zero
     * @throws ArithmeticException if the denominator is zero
     */
    private Fraction(long numerator, long denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("Fraction denominator must not be zero.");
        }

        long n = numerator;
        long d = denominator;
        if (d < 0) {
            n = -n;
            d = -d;
        }

        long g = gcd(Math.abs(n), d);
        if (g > 1) {
            n = n / g;
            d = d / g;
        }

        this.numerator = n;
        this.denominator = d;
    }


    /**
     * Creates a new fraction.
     *
     * @param numerator the numerator
     * @param denominator the denominator, must not be zero
     * @return the reduced fraction
     * @throws ArithmeticException if the denominator is zero
     */
    public static Fraction of(long numerator, long denominator) {
        return new Fraction(numerator, denominator);
    }


    /**
     * Parses a fraction from its textual form "n/d" or "n".
     *
     * @param text the text to parse
     * @return the parsed fraction, or null if the text is null, empty or invalid
     */
    public static Fraction parse(String text) {
        if (text == null) {
            return null;
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            int slash = trimmed.indexOf('/');
            if (slash < 0) {
                return new Fraction(Long.parseLong(trimmed), 1);
            }

            long d = Long.parseLong(trimmed.substring(slash + 1).trim());
            if (d == 0) {
                return null;
            }

            return new Fraction(Long.parseLong(trimmed.substring(0, slash).trim()), d);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Gets the numerator.
     *
     * @return the numerator
     */
    public long getNumerator() {
        return numerator;
    }


    /**
     * Gets the denominator.
     *
     * @return the denominator (always positive)
     */
    public long getDenominator() {
        return denominator;
    }


    /**
     * Adds another fraction.
     *
     * @param other the fraction to add
     * @return the sum
     */
    public Fraction add(Fraction other) {
        return new Fraction(Math.addExact(Math.multiplyExact(numerator, other.denominator), Math.multiplyExact(other.numerator, denominator)),
                            Math.multiplyExact(denominator, other.denominator));
    }


    /**
     * Subtracts another fraction.
     *
     * @param other the fraction to subtract
     * @return the difference
     */
    public Fraction subtract(Fraction other) {
        return add(other.negate());
    }


    /**
     * Multiplies by another fraction.
     *
     * @param other the factor
     * @return the product
     */
    public Fraction multiply(Fraction other) {
        return new Fraction(Math.multiplyExact(numerator, other.numerator), Math.multiplyExact(denominator, other.denominator));
    }


    /**
     * Multiplies by the ratio n/d.
     *
     * @param n the numerator of the factor
     * @param d the denominator of the factor
     * @return the product
     */
    public Fraction multiply(long n, long d) {
        return multiply(new Fraction(n, d));
    }


    /**
     * Negates this fraction.
     *
     * @return the negated fraction
     */
    public Fraction negate() {
        return new Fraction(-numerator, denominator);
    }


    /**
     * Checks whether this fraction is zero.
     *
     * @return true if zero
     */
    public boolean isZero() {
        return numerator == 0;
    }


    /**
     * Checks whether this fraction is greater than zero.
     *
     * @return true if positive
     */
    public boolean isPositive() {
        return numerator > 0;
    }


    /**
     * Checks whether this fraction is less than zero.
     *
     * @return true if negative
     */
    public boolean isNegative() {
        return numerator < 0;
    }


    /**
     * Returns the larger of this and another fraction.
     *
     * @param other the other fraction
     * @return the larger fraction
     */
    public Fraction max(Fraction other) {
        if (compareTo(other) >= 0) {
            return this;
        }
        return other;
    }


    /**
     * Returns the smaller of this and another fraction.
     *
     * @param other the other fraction
     * @return the smaller fraction
     */
    public Fraction min(Fraction other) {
        if (compareTo(other) <= 0) {
            return this;
        }
        return other;
    }


    /**
     * Converts this fraction of a whole note into a number of MusicXML divisions.
     *
     * @param divisions the divisions per quarter note
     * @return the rounded number of divisions
     */
    public long toDivisions(long divisions) {
        return Math.round((double) numerator * 4 * divisions / denominator);
    }


    /**
     * Creates a fraction of a whole note from a number of MusicXML divisions.
     *
     * @param value the value in divisions
     * @param divisions the divisions per quarter note
     * @return the fraction of a whole note, or zero if divisions is not positive
     */
    public static Fraction fromDivisions(long value, long divisions) {
        if (divisions <= 0) {
            return ZERO;
        }
        return new Fraction(value, 4 * divisions);
    }


    /**
     * Compares two fractions.
     *
     * @param other the other fraction
     * @return a negative, zero or positive value
     */
    @Override
    public int compareTo(Fraction other) {
        return Long.compare(Math.multiplyExact(numerator, other.denominator), Math.multiplyExact(other.numerator, denominator));
    }


    /**
     * Computes the greatest common divisor.
     *
     * @param a the first value (non-negative)
     * @param b the second value (non-negative)
     * @return the greatest common divisor
     */
    static long gcd(long a, long b) {
        long x = a;
        long y = b;
        while (y != 0) {
            long t = x % y;
            x = y;
            y = t;
        }
        return x;
    }


    /**
     * Computes the least common multiple.
     *
     * @param a the first value (positive)
     * @param b the second value (positive)
     * @return the least common multiple
     */
    static long lcm(long a, long b) {
        return Math.multiplyExact(a / gcd(a, b), b);
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
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
        Fraction other = (Fraction) obj;
        return numerator == other.numerator && denominator == other.denominator;
    }


    /**
     * Returns the textual form "n/d" as used by MuseScore.
     *
     * @return the textual form
     */
    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }
}
