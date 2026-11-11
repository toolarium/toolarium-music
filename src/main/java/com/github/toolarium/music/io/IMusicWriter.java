/*
 * IMusicWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;


/**
 * Interface for writing music files.
 *
 * @author patrick
 */
public interface IMusicWriter {

    /**
     * Write a score to a file.
     *
     * @param score the score to write
     * @param path the path to write to
     * @throws IOException in case of write errors
     */
    void write(IScore score, Path path) throws IOException;


    /**
     * Write a score to an output stream.
     *
     * @param score the score to write
     * @param outputStream the output stream to write to
     * @throws IOException in case of write errors
     */
    void write(IScore score, OutputStream outputStream) throws IOException;
}
