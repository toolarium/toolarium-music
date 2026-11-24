/*
 * MsczWriterImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Writes compressed MuseScore (.mscz) files in the MuseScore 4 format.
 *
 * @author patrick
 */
public class MsczWriterImpl implements IMusicWriter {
    private static final String SCORE_FILE = "score.mscx";


    /**
     * Constructor for MsczWriterImpl.
     */
    public MsczWriterImpl() {
        // default constructor
    }


    /**
     * Writes a score as compressed MuseScore (.mscz) file to a path.
     *
     * @param score the score to write
     * @param path the target path
     * @throws IOException if writing fails
     */
    @Override
    public void write(IScore score, Path path) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            write(score, outputStream);
        }
    }


    /**
     * Writes a score as compressed MuseScore (.mscz) file to an output stream. The stream is not closed.
     *
     * @param score the score to write
     * @param outputStream the output stream
     * @throws IOException if writing fails
     */
    @Override
    public void write(IScore score, OutputStream outputStream) throws IOException {
        MuseScoreContainer.getInstance().writeScore(SCORE_FILE, MuseScoreWriter.getInstance().write(score), outputStream);
    }
}
