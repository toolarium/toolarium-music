/*
 * IMusicReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


/**
 * Interface for reading music files.
 *
 * @author patrick
 */
public interface IMusicReader {

    /**
     * Read a music file from a path.
     *
     * @param path the path to the music file
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    IScore read(Path path) throws IOException;


    /**
     * Read a music file from an input stream.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    IScore read(InputStream inputStream) throws IOException;
}
