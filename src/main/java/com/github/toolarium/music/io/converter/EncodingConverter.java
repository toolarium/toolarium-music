/*
 * EncodingConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.converter;

import com.github.toolarium.music.Version;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.EncodingImpl;
import com.github.toolarium.music.model.score.impl.IdentificationImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.util.ArrayList;
import java.util.List;


/**
 * Adds the information about this library to the encoding of a converted score: the software with the version
 * (for example {@code toolarium-music x.y.z}) and an encoding description naming the source format.
 *
 * @author patrick
 */
public final class EncodingConverter {
    /** The name of the library. */
    public static final String LIBRARY_NAME = Version.PROJECT_NAME;


    /**
     * Private holder class for the singleton instance.
     */
    private static final class HOLDER {
        static final EncodingConverter INSTANCE = new EncodingConverter();
    }


    /**
     * Constructor.
     */
    private EncodingConverter() {
        // NOP
    }


    /**
     * Gets the singleton instance.
     *
     * @return the instance
     */
    public static EncodingConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the version of the library.
     *
     * @return the version, for example 0.9.0
     */
    public String getVersion() {
        return Version.VERSION;
    }


    /**
     * Gets the software entry of the library: the name and the version.
     *
     * @return the software entry, for example toolarium-music 0.9.0
     */
    public String getSoftware() {
        return LIBRARY_NAME + " " + getVersion();
    }


    /**
     * Gets the encoding description of a conversion.
     *
     * @param source the name of the source format, or null
     * @return the description, for example Converted by toolarium-music 0.9.0 from Sibelius
     */
    public String getDescription(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "Converted by " + getSoftware();
        }
        return "Converted by " + getSoftware() + " from " + source.trim();
    }


    /**
     * Marks a score as converted by this library: replaces the software entries of the library by the entry with
     * the version and sets the encoding description. Other software entries (for example the program which wrote
     * the source file) are kept. A score of another implementation than ScoreImpl is left unchanged.
     *
     * @param score the score
     * @param source the name of the source format, or null
     */
    public void apply(IScore score, String source) {
        if (!(score instanceof ScoreImpl)) {
            return;
        }
        ScoreImpl scoreImpl = (ScoreImpl) score;
        IdentificationImpl identification = scoreImpl.getIdentification();
        if (identification == null) {
            identification = new IdentificationImpl();
            scoreImpl.setIdentification(identification);
        }
        EncodingImpl encoding = identification.getEncoding();
        if (encoding == null) {
            encoding = new EncodingImpl();
            identification.setEncoding(encoding);
        }

        List<String> software = new ArrayList<>();
        software.add(getSoftware());
        if (encoding.getSoftware() != null) {
            for (String entry : encoding.getSoftware()) {
                if (entry != null && !entry.startsWith(LIBRARY_NAME)) {
                    software.add(entry);
                }
            }
        }
        encoding.setSoftware(software);
        encoding.setEncodingDescription(getDescription(source));
    }
}
