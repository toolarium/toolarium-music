/*
 * MscxReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Reads uncompressed MuseScore (.mscx) files of all versions (1.x to 4.x).
 *
 * @author patrick
 */
public class MscxReaderImpl implements IMusicReader {

    /**
     * Constructor for MscxReaderImpl.
     */
    public MscxReaderImpl() {
        // default constructor
    }


    /**
     * Reads an uncompressed MuseScore (.mscx) file from a path.
     *
     * @param path the path to the .mscx file
     * @return the parsed score
     * @throws IOException if the file cannot be read or is not a valid MuseScore file
     */
    @Override
    public IScore read(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return read(inputStream);
        }
    }


    /**
     * Reads an uncompressed MuseScore (.mscx) file from an input stream.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException if the content is not a valid MuseScore file
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        return MuseScoreReader.getInstance().read(MuseScoreContainer.getInstance().readAll(inputStream));
    }
}
