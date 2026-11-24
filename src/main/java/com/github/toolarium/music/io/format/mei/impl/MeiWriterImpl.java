/*
 * MeiWriterImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl;

import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Writes MEI (.mei) files (MEI 5, common music notation).
 *
 * @author patrick
 */
public class MeiWriterImpl implements IMusicWriter {
    private static final Logger LOG = LoggerFactory.getLogger(MeiWriterImpl.class);


    /**
     * Constructor for MeiWriterImpl.
     */
    public MeiWriterImpl() {
        // default constructor
    }


    /**
     * Writes a score as MEI file.
     *
     * @param score the score
     * @param path the path of the file
     * @throws IOException if the file cannot be written
     */
    @Override
    public void write(IScore score, Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing MEI to path: {}", path);
        }
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            write(score, outputStream);
        }
    }


    /**
     * Writes a score as MEI to a stream.
     *
     * @param score the score
     * @param outputStream the output stream
     * @throws IOException if the stream cannot be written
     */
    @Override
    public void write(IScore score, OutputStream outputStream) throws IOException {
        if (score == null) {
            throw new IOException("Score must not be null.");
        }
        outputStream.write(MeiScoreWriter.getInstance().write(score));
        outputStream.flush();
    }
}
