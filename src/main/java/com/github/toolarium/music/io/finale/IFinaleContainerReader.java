/*
 * IFinaleContainerReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.io.finale;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


/**
 * Defines the FinaleContainerReader interface.
 *
 * @author patrick
 */
public interface IFinaleContainerReader {

    /**
     * Reads a Finale container from the given file path.
     *
     * @param path the path to the .musx or .mus file
     * @return the parsed Finale container
     * @throws IOException if an I/O error occurs or the format is unknown
     */
    IFinaleContainer read(Path path) throws IOException;

    /**
     * Reads a Finale container from the given input stream.
     *
     * @param inputStream the input stream to read from
     * @return the parsed Finale container
     * @throws IOException if an I/O error occurs or the format is unknown
     */
    IFinaleContainer read(InputStream inputStream) throws IOException;
}
