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
    MUSIC_XML("musicxml", "MusicXML"),

    /** Compressed MXL (.mxl) format (ZIP container). */
    MXL("mxl", "MusicXML"),

    /** Finale (.musx, .mus) format. */
    FINALE("musx", "Finale"),

    /** Standard MIDI (.mid, .midi) format. */
    MIDI("mid", "MIDI"),

    /** Compressed MuseScore (.mscz) format (ZIP container). */
    MUSESCORE("mscz", "MuseScore"),

    /** Uncompressed MuseScore (.mscx) format. */
    MUSESCORE_XML("mscx", "MuseScore"),

    /** Dorico project (.dorico) format (ZIP container, read only). */
    DORICO("dorico", "Dorico"),

    /** Sibelius (.sib, .sco) format (encrypted binary container, read only). */
    SIBELIUS("sib", "Sibelius"),

    /** Guitar Pro (.gp, .gpx, .gp5, .gp4, .gp3) format (read only). */
    GUITAR_PRO("gp", "Guitar Pro"),

    /** capella (.capx, .cap) format (read only). */
    CAPELLA("capx", "Capella"),

    /** Music Encoding Initiative (.mei) format. */
    MEI("mei", "MEI"),

    /** ABC notation (.abc) format. */
    ABC("abc", "ABC"),

    /** LilyPond (.ly) format. */
    LILYPOND("ly", "LilyPond");

    private final String extension;
    private final String displayName;


    /**
     * Constructor.
     *
     * @param extension the file extension
     * @param displayName the name of the format, for example Sibelius
     */
    MusicFileFormat(String extension, String displayName) {
        this.extension = extension;
        this.displayName = displayName;
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
     * Gets the name of the format.
     *
     * @return the name, for example Sibelius
     */
    public String getDisplayName() {
        return displayName;
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
        if (lower.endsWith(".mscz")) {
            return MUSESCORE;
        }
        if (lower.endsWith(".mscx")) {
            return MUSESCORE_XML;
        }
        if (lower.endsWith(".dorico")) {
            return DORICO;
        }
        if (lower.endsWith(".sib") || lower.endsWith(".sco") || lower.endsWith(".si7")) {
            return SIBELIUS;
        }
        if (lower.endsWith(".gp") || lower.endsWith(".gpx") || lower.endsWith(".gp5") || lower.endsWith(".gp4") || lower.endsWith(".gp3")) {
            return GUITAR_PRO;
        }
        if (lower.endsWith(".capx") || lower.endsWith(".cap")) {
            return CAPELLA;
        }
        if (lower.endsWith(".mei")) {
            return MEI;
        }
        if (lower.endsWith(".abc")) {
            return ABC;
        }
        if (lower.endsWith(".ly") || lower.endsWith(".ily")) {
            return LILYPOND;
        }
        throw new IllegalArgumentException("Cannot detect music file format from file name: " + fileName);
    }
}
