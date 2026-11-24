/*
 * MusicFileFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music;

import com.github.toolarium.music.io.converter.EncodingConverter;
import com.github.toolarium.music.io.format.abc.AbcIOFactory;
import com.github.toolarium.music.io.format.capella.CapellaIOFactory;
import com.github.toolarium.music.io.format.dorico.DoricoIOFactory;
import com.github.toolarium.music.io.format.finale.FinaleIOFactory;
import com.github.toolarium.music.io.format.guitarpro.GuitarProIOFactory;
import com.github.toolarium.music.io.format.lilypond.LilyPondIOFactory;
import com.github.toolarium.music.io.format.mei.MeiIOFactory;
import com.github.toolarium.music.io.format.midi.MidiIOFactory;
import com.github.toolarium.music.io.format.musescore.MuseScoreIOFactory;
import com.github.toolarium.music.io.format.musicxml.impl.MusicXMLIOFactory;
import com.github.toolarium.music.io.format.mxl.impl.MXLIOFactory;
import com.github.toolarium.music.io.format.sibelius.SibeliusIOFactory;
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
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MusicFileFactory INSTANCE = new MusicFileFactory();
    }


    /**
     * Constructor.
     */
    private MusicFileFactory() {
        // private
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MusicFileFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Reads a score from any supported format.
     * Auto-detects the format by file extension (.musicxml, .xml, .mxl, .musx, .mid, .mscz, .mscx, .dorico, .sib, .sco,
     * .gp, .gpx, .gp5, .gp4, .gp3, .capx, .cap, .mei, .abc).
     *
     * @param path the path to the file
     * @return the parsed score
     * @throws IOException in case of an I/O error, an unsupported format or invalid content
     */
    public IScore read(Path path) throws IOException {
        if (path == null) {
            throw new IOException("Path must not be null.");
        }

        Path fileName = path.getFileName();
        if (fileName == null) {
            throw new IOException("Cannot detect music file format: path has no file name component.");
        }

        MusicFileFormat format;
        try {
            format = MusicFileFormat.fromFileName(fileName.toString());
        } catch (IllegalArgumentException e) {
            throw new IOException("Cannot detect music file format: " + fileName, e);
        }

        return read(path, format);
    }


    /**
     * Reads a score from a file using the specified format.
     *
     * @param path the path to the file
     * @param format the format of the input data
     * @return the parsed score
     * @throws IOException in case of an I/O error, an unsupported format or invalid content
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
                return converted(MXLIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.MUSIC_XML == format) {
                return converted(MusicXMLIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.MIDI == format) {
                return converted(MidiIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.FINALE == format) {
                return converted(FinaleIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.MUSESCORE == format) {
                return converted(MuseScoreIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.MUSESCORE_XML == format) {
                return converted(MuseScoreIOFactory.getInstance().getMscxReader().read(path), format);
            } else if (MusicFileFormat.DORICO == format) {
                return converted(DoricoIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.SIBELIUS == format) {
                return converted(SibeliusIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.ABC == format) {
                return converted(AbcIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.LILYPOND == format) {
                return converted(LilyPondIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.MEI == format) {
                return converted(MeiIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.CAPELLA == format) {
                return converted(CapellaIOFactory.getInstance().getReader().read(path), format);
            } else if (MusicFileFormat.GUITAR_PRO == format) {
                return converted(GuitarProIOFactory.getInstance().getReader().read(path), format);
            } else {
                throw new IOException("Reading format " + format + " as IScore is not supported.");
            }
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read score from path: {}", path, e);
            }
            throw e;
        } catch (RuntimeException | StackOverflowError e) {
            // a broken file can fail the reader in an unexpected way: the caller gets the documented IOException
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read score from path: {}", path, e);
            }
            throw new IOException("Invalid " + format + " file: " + e, e);
        }
    }


    /**
     * Reads a score from an input stream using the specified format.
     *
     * @param inputStream the input stream to read from
     * @param format the format of the input data
     * @return the parsed score
     * @throws IOException in case of an I/O error, an unsupported format or invalid content
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
                return converted(MXLIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.MUSIC_XML == format) {
                return converted(MusicXMLIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.MIDI == format) {
                return converted(MidiIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.FINALE == format) {
                return converted(FinaleIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.MUSESCORE == format) {
                return converted(MuseScoreIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.MUSESCORE_XML == format) {
                return converted(MuseScoreIOFactory.getInstance().getMscxReader().read(inputStream), format);
            } else if (MusicFileFormat.DORICO == format) {
                return converted(DoricoIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.SIBELIUS == format) {
                return converted(SibeliusIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.ABC == format) {
                return converted(AbcIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.LILYPOND == format) {
                return converted(LilyPondIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.MEI == format) {
                return converted(MeiIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.CAPELLA == format) {
                return converted(CapellaIOFactory.getInstance().getReader().read(inputStream), format);
            } else if (MusicFileFormat.GUITAR_PRO == format) {
                return converted(GuitarProIOFactory.getInstance().getReader().read(inputStream), format);
            } else {
                throw new IOException("Reading format " + format + " as IScore is not supported.");
            }
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read score: {}", e.getMessage(), e);
            }
            throw e;
        } catch (RuntimeException | StackOverflowError e) {
            // a broken file can fail the reader in an unexpected way: the caller gets the documented IOException
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read score: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid " + format + " data: " + e, e);
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

        Path fileName = path.getFileName();
        if (fileName == null) {
            throw new IOException("Cannot detect music file format: path has no file name component.");
        }

        MusicFileFormat format;
        try {
            format = MusicFileFormat.fromFileName(fileName.toString());
        } catch (IllegalArgumentException e) {
            throw new IOException("Cannot detect music file format: " + fileName, e);
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
            } else if (MusicFileFormat.MIDI == format) {
                MidiIOFactory.getInstance().getWriter().write(score, path);
            } else if (MusicFileFormat.MUSESCORE == format) {
                MuseScoreIOFactory.getInstance().getWriter().write(score, path);
            } else if (MusicFileFormat.MUSESCORE_XML == format) {
                MuseScoreIOFactory.getInstance().getMscxWriter().write(score, path);
            } else if (MusicFileFormat.MEI == format) {
                MeiIOFactory.getInstance().getWriter().write(score, path);
            } else if (MusicFileFormat.LILYPOND == format) {
                LilyPondIOFactory.getInstance().getWriter().write(score, path);
            } else if (MusicFileFormat.ABC == format) {
                AbcIOFactory.getInstance().getWriter().write(score, path);
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
            } else if (MusicFileFormat.MIDI == format) {
                MidiIOFactory.getInstance().getWriter().write(score, outputStream);
            } else if (MusicFileFormat.MUSESCORE == format) {
                MuseScoreIOFactory.getInstance().getWriter().write(score, outputStream);
            } else if (MusicFileFormat.MUSESCORE_XML == format) {
                MuseScoreIOFactory.getInstance().getMscxWriter().write(score, outputStream);
            } else if (MusicFileFormat.MEI == format) {
                MeiIOFactory.getInstance().getWriter().write(score, outputStream);
            } else if (MusicFileFormat.LILYPOND == format) {
                LilyPondIOFactory.getInstance().getWriter().write(score, outputStream);
            } else if (MusicFileFormat.ABC == format) {
                AbcIOFactory.getInstance().getWriter().write(score, outputStream);
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

    /**
     * Marks a score read from another format than MusicXML as converted by this library.
     *
     * @param score the score
     * @param format the format of the source file
     * @return the score
     */
    private IScore converted(IScore score, MusicFileFormat format) {
        if (score != null && MusicFileFormat.MUSIC_XML != format && MusicFileFormat.MXL != format) {
            EncodingConverter.getInstance().apply(score, format.getDisplayName());
        }
        return score;
    }
}
