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
     * @throws IOException in case of an I/O error or unsupported format
     */
    public IScore read(Path path) throws IOException {
        if (path == null) {
            throw new IOException("Path must not be null.");
        }

        MusicFileFormat format;
        try {
            format = MusicFileFormat.fromFileName(path.getFileName().toString());
        } catch (IllegalArgumentException e) {
            throw new IOException("Cannot detect music file format: " + path.getFileName(), e);
        }

        return read(path, format);
    }


    /**
     * Reads a score from a file using the specified format.
     *
     * @param path the path to the file
     * @param format the format of the input data
     * @return the parsed score
     * @throws IOException in case of an I/O error or unsupported format
     */
    public IScore read(Path path, MusicFileFormat format) throws IOException {
        if (path == null) {
            throw new IOException("Path must not be null.");
        }
        if (format == null) {
            throw new IOException("Format must not be null.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading score from path: {}, format: {}", path, format);
        }

        try {
            if (MusicFileFormat.MXL == format) {
                return MXLIOFactory.getInstance().getReader().read(path);
            } else if (MusicFileFormat.MUSIC_XML == format) {
                return MusicXMLIOFactory.getInstance().getReader().read(path);
            } else {
                throw new IOException("Reading format " + format + " as IScore is not supported.");
            }
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
     * @throws IOException in case of an I/O error or unsupported format
     */
    public IScore read(InputStream inputStream, MusicFileFormat format) throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream must not be null.");
        }
        if (format == null) {
            throw new IOException("Format must not be null.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading score from input stream, format: {}", format);
        }

        try {
            if (MusicFileFormat.MXL == format) {
                return MXLIOFactory.getInstance().getReader().read(inputStream);
            } else if (MusicFileFormat.MUSIC_XML == format) {
                return MusicXMLIOFactory.getInstance().getReader().read(inputStream);
            } else {
                throw new IOException("Reading format " + format + " as IScore is not supported.");
            }
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
     * @throws IOException in case of an I/O error or unsupported format
     */
    public void write(IScore score, Path path) throws IOException {
        if (score == null) {
            throw new IOException("Score must not be null.");
        }
        if (path == null) {
            throw new IOException("Path must not be null.");
        }

        MusicFileFormat format;
        try {
            format = MusicFileFormat.fromFileName(path.getFileName().toString());
        } catch (IllegalArgumentException e) {
            throw new IOException("Cannot detect music file format: " + path.getFileName(), e);
        }

        write(score, path, format);
    }


    /**
     * Writes a score to a file in the specified format.
     *
     * @param score the score to write
     * @param path the path to write to
     * @param format the output format
     * @throws IOException in case of an I/O error or unsupported format
     */
    public void write(IScore score, Path path, MusicFileFormat format) throws IOException {
        if (score == null) {
            throw new IOException("Score must not be null.");
        }
        if (path == null) {
            throw new IOException("Path must not be null.");
        }
        if (format == null) {
            throw new IOException("Format must not be null.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing score to path: {}, format: {}", path, format);
        }

        try {
            if (MusicFileFormat.MXL == format) {
                MXLIOFactory.getInstance().getWriter().write(score, path);
            } else if (MusicFileFormat.MUSIC_XML == format) {
                MusicXMLIOFactory.getInstance().getWriter().write(score, path);
            } else {
                throw new IOException("Writing format " + format + " is not supported.");
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
     * @throws IOException in case of an I/O error or unsupported format
     */
    public void write(IScore score, OutputStream outputStream, MusicFileFormat format)
            throws IOException {
        if (score == null) {
            throw new IOException("Score must not be null.");
        }
        if (outputStream == null) {
            throw new IOException("Output stream must not be null.");
        }
        if (format == null) {
            throw new IOException("Format must not be null.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing score to output stream, format: {}", format);
        }

        try {
            if (MusicFileFormat.MXL == format) {
                MXLIOFactory.getInstance().getWriter().write(score, outputStream);
            } else if (MusicFileFormat.MUSIC_XML == format) {
                MusicXMLIOFactory.getInstance().getWriter().write(score, outputStream);
            } else {
                throw new IOException("Writing format " + format + " is not supported.");
            }
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to write score: {}", e.getMessage(), e);
            }
            throw e;
        }
    }
}
