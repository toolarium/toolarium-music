/*
 * MusicFileFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music;

import com.github.toolarium.music.io.musicxml.impl.MusicXMLIOFactory;
import com.github.toolarium.music.io.mxl.impl.MXLIOFactory;
import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Singleton factory for reading and writing music files.
 * Provides a unified API that auto-detects the file format by extension
 * or accepts an explicit {@link MusicFileFormat}.
 *
 * @author patrick
 */
public final class MusicFileFactory {
    private static final Logger LOG = LoggerFactory.getLogger(MusicFileFactory.class);


    /**
     * Singleton holder.
     */
    private static final class HOLDER {
        static final MusicFileFactory INSTANCE = new MusicFileFactory();
    }


    /**
     * Private constructor.
     */
    private MusicFileFactory() {
        // private
    }


    /**
     * Gets the singleton instance of the factory.
     *
     * @return the factory instance
     */
    public static MusicFileFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Reads a score from any supported format.
     * Auto-detects the format by file extension (.musicxml, .xml, .mxl).
     *
     * @param path the path to the file
     * @return the parsed score
     * @throws IOException in case of an I/O error
     */
    public IScore read(Path path) throws IOException {
        MusicFileFormat format = MusicFileFormat.fromFileName(path.getFileName().toString());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading score from path: {}, detected format: {}", path, format);
        }

        try {
            if (MusicFileFormat.MXL == format) {
                return MXLIOFactory.getInstance().getReader().read(path);
            }
            return MusicXMLIOFactory.getInstance().getReader().read(path);
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read score from path: {}", path, e);
            }
            throw e;
        }
    }


    /**
     * Reads a score from an input stream using the specified format.
     *
     * @param inputStream the input stream to read from
     * @param format the format of the input data
     * @return the parsed score
     * @throws IOException in case of an I/O error
     */
    public IScore read(InputStream inputStream, MusicFileFormat format) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading score from input stream, format: {}", format);
        }

        try {
            if (MusicFileFormat.MXL == format) {
                return MXLIOFactory.getInstance().getReader().read(inputStream);
            }
            return MusicXMLIOFactory.getInstance().getReader().read(inputStream);
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read score: {}", e.getMessage(), e);
            }
            throw e;
        }
    }


    /**
     * Writes a score to a file. Auto-detects the format by file extension.
     *
     * @param score the score to write
     * @param path the path to write to
     * @throws IOException in case of an I/O error
     */
    public void write(IScore score, Path path) throws IOException {
        MusicFileFormat format = MusicFileFormat.fromFileName(path.getFileName().toString());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing score to path: {}, detected format: {}", path, format);
        }

        try {
            if (MusicFileFormat.MXL == format) {
                MXLIOFactory.getInstance().getWriter().write(score, path);
            } else {
                MusicXMLIOFactory.getInstance().getWriter().write(score, path);
            }
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to write score to path: {}", path, e);
            }
            throw e;
        }
    }


    /**
     * Writes a score to an output stream in the specified format.
     *
     * @param score the score to write
     * @param outputStream the output stream to write to
     * @param format the output format
     * @throws IOException in case of an I/O error
     */
    public void write(IScore score, OutputStream outputStream, MusicFileFormat format)
            throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing score to output stream, format: {}", format);
        }

        try {
            if (MusicFileFormat.MXL == format) {
                MXLIOFactory.getInstance().getWriter().write(score, outputStream);
            } else {
                MusicXMLIOFactory.getInstance().getWriter().write(score, outputStream);
            }
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to write score: {}", e.getMessage(), e);
            }
            throw e;
        }
    }
}
