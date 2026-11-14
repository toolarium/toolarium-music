/*
 * MusicXMLReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.musicxml.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLInputFactory;
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
    private static final long MAX_INPUT_SIZE = 100L * 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;
    private static final byte[] DOCTYPE_MARKER = {0x3C, 0x21, 0x44, 0x4F, 0x43, 0x54, 0x59, 0x50, 0x45};

    static {
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XML_MAPPER = new XmlMapper(new XmlFactory(xif));
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
     * Performs a byte-level pre-check to avoid String conversion when no DOCTYPE is present.
     *
     * @param inputStream the input stream to read from
     * @return the content bytes with the DOCTYPE declaration removed
     * @throws IOException in case of read errors
     */
    private byte[] stripDoctype(InputStream inputStream) throws IOException {
        byte[] bytes = readBounded(inputStream);

        if (!containsBytes(bytes, DOCTYPE_MARKER)) {
            return bytes;
        }

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


    /**
     * Reads all bytes from the input stream with a size limit.
     *
     * @param inputStream the input stream to read from
     * @return the content as a byte array
     * @throws IOException if an I/O error occurs or the input exceeds the size limit
     */
    private byte[] readBounded(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            total += len;
            if (total > MAX_INPUT_SIZE) {
                throw new IOException("MusicXML input exceeds maximum allowed size (" + MAX_INPUT_SIZE + " bytes).");
            }
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }


    /**
     * Checks if a byte array contains the given byte pattern.
     *
     * @param data the data to search in
     * @param pattern the pattern to search for
     * @return true if the pattern is found
     */
    private boolean containsBytes(byte[] data, byte[] pattern) {
        for (int i = 0; i <= data.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }
}
