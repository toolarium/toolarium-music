/*
 * SibeliusTextRun.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl.dto;

import java.util.Objects;


/**
 * A part of a formatted Sibelius text with the same formatting: the text, the font, the size in points and the
 * bold and italic state.
 *
 * @author patrick
 */
public class SibeliusTextRun {
    private final String text;
    private final String font;
    private final Integer size;
    private final boolean bold;
    private final boolean italic;


    /**
     * Constructor for SibeliusTextRun.
     *
     * @param text the text
     * @param font the font family, or null for the style font
     * @param size the size in points, or null for the style size
     * @param bold true for bold text
     * @param italic true for italic text
     */
    public SibeliusTextRun(String text, String font, Integer size, boolean bold, boolean italic) {
        this.text = text;
        this.font = font;
        this.size = size;
        this.bold = bold;
        this.italic = italic;
    }


    /**
     * Gets the text.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }


    /**
     * Gets the font family.
     *
     * @return the font family, or null for the style font
     */
    public String getFont() {
        return font;
    }


    /**
     * Gets the size in points.
     *
     * @return the size, or null for the style size
     */
    public Integer getSize() {
        return size;
    }


    /**
     * Checks whether the text is bold.
     *
     * @return true for bold text
     */
    public boolean isBold() {
        return bold;
    }


    /**
     * Checks whether the text is italic.
     *
     * @return true for italic text
     */
    public boolean isItalic() {
        return italic;
    }


    /**
     * Calculates the hash code.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(text, font, size, bold, italic);
    }


    /**
     * Compares this text run with another object.
     *
     * @param obj the other object
     * @return true if both are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SibeliusTextRun other = (SibeliusTextRun) obj;
        return Objects.equals(text, other.text) && Objects.equals(font, other.font) && Objects.equals(size, other.size)
                && bold == other.bold && italic == other.italic;
    }


    /**
     * Gets the string representation.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SibeliusTextRun [text=" + text + ", font=" + font + ", size=" + size + ", bold=" + bold + ", italic=" + italic + "]";
    }
}
