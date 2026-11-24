/*
 * MuseScoreFiles.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl.dto;

import java.util.Arrays;


/**
 * The files of a MuseScore container (.mscz) that are read: the score file (.mscx) and the style file
 * (score_style.mss, MuseScore 4) with the page style.
 *
 * @author patrick
 */
public class MuseScoreFiles {
    private final byte[] score;
    private final byte[] style;


    /**
     * Constructor for MuseScoreFiles.
     *
     * @param score the content of the score file
     * @param style the content of the style file, or null if the container has none
     */
    public MuseScoreFiles(byte[] score, byte[] style) {
        this.score = score;
        this.style = style;
    }


    /**
     * Gets the content of the score file.
     *
     * @return the content of the score file
     */
    public byte[] getScore() {
        return score;
    }


    /**
     * Gets the content of the style file.
     *
     * @return the content of the style file, or null if the container has none
     */
    public byte[] getStyle() {
        return style;
    }


    /**
     * Returns a hash code over all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(score) + Arrays.hashCode(style);
    }


    /**
     * Compares all fields with another object.
     *
     * @param obj the other object
     * @return true if the other object is a MuseScoreFiles with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MuseScoreFiles other = (MuseScoreFiles) obj;
        return Arrays.equals(score, other.score) && Arrays.equals(style, other.style);
    }


    /**
     * Creates a text representation with all fields (the sizes of the files).
     *
     * @return the text
     */
    @Override
    public String toString() {
        return "MuseScoreFiles ["
                + "score=" + length(score) + " bytes"
                + ", style=" + length(style) + " bytes"
                + "]";
    }


    /**
     * Gets the length of a content.
     *
     * @param content the content, or null
     * @return the length, 0 for null
     */
    private static int length(byte[] content) {
        if (content == null) {
            return 0;
        }
        return content.length;
    }
}
