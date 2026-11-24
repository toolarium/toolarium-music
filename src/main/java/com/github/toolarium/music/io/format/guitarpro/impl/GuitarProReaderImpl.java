/*
 * GuitarProReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.format.guitarpro.impl.dto.GpSong;
import com.github.toolarium.music.io.stream.StreamContentReader;
import com.github.toolarium.music.io.stream.ZipContentReader;
import com.github.toolarium.music.io.xml.XmlDocumentReader;
import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Implementation of {@link IMusicReader} for Guitar Pro files. The format is detected from the
 * content: GP7 and GP8 (.gp, a ZIP container with Content/score.gpif), GP6 (.gpx, a BCFZ or BCFS
 * container with score.gpif) and the binary formats GP3, GP4 and GP5.
 *
 * @author patrick
 */
public class GuitarProReaderImpl implements IMusicReader {
    private static final Logger LOG = LoggerFactory.getLogger(GuitarProReaderImpl.class);
    private static final int MAX_SIZE = 128 * 1024 * 1024;
    private static final String GPIF = "score.gpif";
    private final XmlDocumentReader xml = XmlDocumentReader.getInstance();


    /**
     * Constructor for GuitarProReaderImpl.
     */
    public GuitarProReaderImpl() {
        // default constructor
    }


    /**
     * Reads a Guitar Pro file from a path.
     *
     * @param path the path to the Guitar Pro file
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading Guitar Pro from path: {}", path);
        }

        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


    /**
     * Reads a Guitar Pro file from an input stream.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        byte[] content = StreamContentReader.getInstance().readAll(inputStream, MAX_SIZE, "Guitar Pro file");
        try {
            GpSong song;
            if (content.length > 1 && content[0] == 'P' && content[1] == 'K') {
                song = readGpif(extractGpif(content), "7");
            } else if (GpxContainerReader.getInstance().isContainer(content)) {
                Map<String, byte[]> files = GpxContainerReader.getInstance().read(content);
                byte[] gpif = files.get(GPIF);
                if (gpif == null) {
                    throw new IOException("Invalid Guitar Pro 6 file: no score.gpif in the container.");
                }
                song = readGpif(gpif, "6");
            } else {
                song = new GpBinaryReader(content).read();
            }
            if (song.getTracks().isEmpty()) {
                throw new IOException("Invalid Guitar Pro file: no tracks found.");
            }
            return ScoreBuilder.getInstance().build(new GuitarProScoreAssembler().assemble(song));
        } catch (RuntimeException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read Guitar Pro: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid Guitar Pro file: " + e.getMessage(), e);
        }
    }


    /**
     * Parses and reads a GPIF document.
     *
     * @param gpif the document
     * @param defaultVersion the version if the document has none
     * @return the song
     * @throws IOException if the document is invalid
     */
    private GpSong readGpif(byte[] gpif, String defaultVersion) throws IOException {
        Document document = xml.parse(gpif, "Guitar Pro");
        Element root = document.getDocumentElement();
        if (root == null || !"GPIF".equals(xml.name(root))) {
            throw new IOException("Invalid Guitar Pro file: the root element is not GPIF.");
        }
        return new GpifReader().read(root, defaultVersion);
    }


    /**
     * Extracts score.gpif from the ZIP container of a GP7 or GP8 file.
     *
     * @param content the container
     * @return the GPIF document
     * @throws IOException if the container has no GPIF document
     */
    private byte[] extractGpif(byte[] content) throws IOException {
        ZipContentReader zip = ZipContentReader.getInstance();
        for (String name : zip.list(content).keySet()) {
            if (name.toLowerCase().endsWith(GPIF)) {
                return zip.read(content, name, MAX_SIZE);
            }
        }
        throw new IOException("Invalid Guitar Pro file: no Content/score.gpif in the container.");
    }
}
