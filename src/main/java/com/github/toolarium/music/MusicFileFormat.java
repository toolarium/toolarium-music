/*
 * MusicFileFormat.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music;


/**
 * Defines the supported music file formats.
 *
 * @author patrick
 */
public enum MusicFileFormat {

    /** Plain MusicXML (.musicxml) format. */
    MUSIC_XML("musicxml"),

    /** Compressed MXL (.mxl) format (ZIP container). */
    MXL("mxl"),

    /** Finale (.musx, .mus) format. */
    FINALE("musx"),

    /** Standard MIDI (.mid, .midi) format. */
    MIDI("mid");

    private final String extension;


    /**
     * Constructor.
     *
     * @param extension the file extension
     */
    MusicFileFormat(String extension) {
        this.extension = extension;
    }


    /**
     * Gets the file extension associated with this format.
     *
     * @return the file extension (without leading dot)
     */
    public String getExtension() {
        return extension;
    }


    /**
     * Detects the format from a file name or path based on its extension.
     *
     * @param fileName the file name or path string
     * @return the detected format
     * @throws IllegalArgumentException if the extension is not recognized
     */
    public static MusicFileFormat fromFileName(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".mxl")) {
            return MXL;
        }
        if (lower.endsWith(".musicxml") || lower.endsWith(".xml")) {
            return MUSIC_XML;
        }
        if (lower.endsWith(".musx") || lower.endsWith(".mus")) {
            return FINALE;
        }
        if (lower.endsWith(".mid") || lower.endsWith(".midi")) {
            return MIDI;
        }
        throw new IllegalArgumentException("Cannot detect music file format from file name: " + fileName);
    }
}
