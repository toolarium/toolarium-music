/*
 * CapellaReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.ScoreData;
import com.github.toolarium.music.io.stream.StreamContentReader;
import com.github.toolarium.music.io.xml.XmlDocumentReader;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Implementation of {@link IMusicReader} for capella files: .capx (a ZIP container with the
 * CapXML document score.xml, or a plain CapXML document) and legacy binary .cap files (converted
 * into a CapXML document by {@link CapBinaryReader}).
 *
 * @author patrick
 */
public class CapellaReaderImpl implements IMusicReader {
    private static final Logger LOG = LoggerFactory.getLogger(CapellaReaderImpl.class);
    private static final int MAX_SIZE = 64 * 1024 * 1024;
    private static final String SCORE_ENTRY = "score.xml";
    private final XmlDocumentReader xml = XmlDocumentReader.getInstance();


    /**
     * Constructor for CapellaReaderImpl.
     */
    public CapellaReaderImpl() {
        // default constructor
    }


    /**
     * Reads a capella file from a path.
     *
     * @param path the path to the capella file
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading capella from path: {}", path);
        }

        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


    /**
     * Reads a capella file from an input stream.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        byte[] content = StreamContentReader.getInstance().readAll(inputStream, MAX_SIZE, "capella file");
        Element root;
        if (CapBinaryReader.isCapFile(content)) {
            try {
                root = new CapBinaryReader(content).read();
            } catch (RuntimeException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to read capella: {}", e.getMessage(), e);
                }
                throw new IOException("Invalid capella file: " + e.getMessage(), e);
            }
        } else {
            byte[] scoreXml = content;
            if (content.length > 1 && content[0] == 'P' && content[1] == 'K') {
                scoreXml = extractScore(content);
            }
            Document document = xml.parse(scoreXml, "capella");
            root = document.getDocumentElement();
            if (root == null || !"score".equals(xml.name(root))) {
                throw new IOException("Invalid capella file: the root element is not score.");
            }
        }
        try {
            ScoreData score = new CapxScoreParser().parse(root);
            if (score.getParts().isEmpty()) {
                throw new IOException("Invalid capella file: no staves found.");
            }
            return ScoreBuilder.getInstance().build(score);
        } catch (RuntimeException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read capella: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid capella file: " + e.getMessage(), e);
        }
    }


    /**
     * Extracts the CapXML document from the ZIP container.
     *
     * @param content the container
     * @return the score.xml document
     * @throws IOException if the container has no score document
     */
    private byte[] extractScore(byte[] content) throws IOException {
        byte[] fallback = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (SCORE_ENTRY.equalsIgnoreCase(name) || name.toLowerCase().endsWith("/" + SCORE_ENTRY)) {
                    return StreamContentReader.getInstance().readAll(zip, MAX_SIZE, "capella score");
                }
                if (fallback == null && name.toLowerCase().endsWith(".xml")) {
                    fallback = StreamContentReader.getInstance().readAll(zip, MAX_SIZE, "capella score");
                }
            }
        } catch (IllegalArgumentException e) {
            // a damaged entry (for example a broken file name) is reported by the ZIP stream as illegal argument
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid ZIP archive: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid ZIP archive: " + e.getMessage(), e);
        }
        if (fallback == null) {
            throw new IOException("Invalid capella file: no score.xml in the container.");
        }
        return fallback;
    }
}
