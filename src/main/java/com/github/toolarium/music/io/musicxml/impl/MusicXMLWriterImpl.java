/*
 * MusicXMLWriterImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.musicxml.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link IMusicWriter} using Jackson XML (XmlMapper).
 *
 * @author patrick
 */
public class MusicXMLWriterImpl implements IMusicWriter {

    private static final Logger LOG = LoggerFactory.getLogger(MusicXMLWriterImpl.class);
    private static final XmlMapper XML_MAPPER;

    static {
        XML_MAPPER = new XmlMapper();
        XML_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
        XML_MAPPER.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        XML_MAPPER.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    }


    /**
     * Constructor for MusicXMLWriterImpl.
     */
    public MusicXMLWriterImpl() {
        // default constructor
    }


    /**
     * Write a score to a file.
     *
     * @param score the score to write
     * @param path the path to write to
     * @throws IOException in case of write errors
     */
    @Override
    public void write(IScore score, Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing score to path: {}", path);
        }

        try (OutputStream os = Files.newOutputStream(path)) {
            write(score, os);
        }
    }


    /**
     * Write a score to an output stream.
     *
     * @param score the score to write
     * @param outputStream the output stream to write to
     * @throws IOException in case of write errors
     */
    @Override
    public void write(IScore score, OutputStream outputStream) throws IOException {
        try {
            XML_MAPPER.writeValue(outputStream, score);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully wrote score.");
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to write score: {}", e.getMessage(), e);
            }

            throw new IOException("Failed to write MusicXML score", e);
        }
    }
}
