/*
 * MusicXMLReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.musicxml.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link IMusicReader} using Jackson XML (XmlMapper).
 *
 * @author patrick
 */
public class MusicXMLReaderImpl implements IMusicReader {

    private static final Logger LOG = LoggerFactory.getLogger(MusicXMLReaderImpl.class);
    private static final XmlMapper XML_MAPPER;

    static {
        XML_MAPPER = new XmlMapper();
        XML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        XML_MAPPER.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }


    /**
     * Constructor for MusicXMLReaderImpl.
     */
    public MusicXMLReaderImpl() {
        // default constructor
    }


    /**
     * Read a MusicXML file from a path.
     *
     * @param path the path to the MusicXML file
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading score from path: {}", path);
        }

        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


    /**
     * Read a MusicXML from an input stream.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        try {
            byte[] cleanXml = stripDoctype(inputStream);
            ScoreImpl score = XML_MAPPER.readValue(cleanXml, ScoreImpl.class);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully read score, version: {}", score.getVersion());
            }

            return score;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read score: {}", e.getMessage(), e);
            }

            throw new IOException("Failed to read MusicXML score", e);
        }
    }


    /**
     * Strip the DOCTYPE declaration from the input stream content.
     * MusicXML files typically contain a DOCTYPE declaration that Jackson XML cannot handle.
     * Handles both simple DOCTYPE and those with internal DTD subsets (bracket blocks).
     *
     * @param inputStream the input stream to read from
     * @return the content bytes with the DOCTYPE declaration removed
     * @throws IOException in case of read errors
     */
    private byte[] stripDoctype(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes, StandardCharsets.UTF_8);

        int doctypeStart = content.indexOf("<!DOCTYPE");
        if (doctypeStart < 0) {
            return bytes;
        }

        int bracketDepth = 0;
        int doctypeEnd = -1;
        for (int i = doctypeStart; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '[') {
                bracketDepth++;
            } else if (c == ']') {
                bracketDepth--;
            } else if (c == '>' && bracketDepth == 0) {
                doctypeEnd = i + 1;
                break;
            }
        }

        if (doctypeEnd < 0) {
            return bytes;
        }

        String cleaned = content.substring(0, doctypeStart) + content.substring(doctypeEnd);
        return cleaned.getBytes(StandardCharsets.UTF_8);
    }
}
