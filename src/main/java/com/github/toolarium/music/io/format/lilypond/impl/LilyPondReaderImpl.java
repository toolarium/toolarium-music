/*
 * LilyPondReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondScore;
import com.github.toolarium.music.io.stream.StreamContentReader;
import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Reads LilyPond (.ly) files: the source is split into tokens, parsed into staves and voices, divided into
 * measures and built into the score model.
 *
 * @author patrick
 */
public class LilyPondReaderImpl implements IMusicReader {
    private static final Logger LOG = LoggerFactory.getLogger(LilyPondReaderImpl.class);
    private static final int MAX_SIZE = 16 * 1024 * 1024;


    /**
     * Constructor for LilyPondReaderImpl.
     */
    public LilyPondReaderImpl() {
        // default constructor
    }


    /**
     * Reads a LilyPond file.
     *
     * @param path the path to the file
     * @return the score
     * @throws IOException if the file cannot be read or is no valid LilyPond file
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading LilyPond from path: {}", path);
        }
        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


    /**
     * Reads a LilyPond source from a stream.
     *
     * @param inputStream the input stream
     * @return the score
     * @throws IOException if the stream cannot be read or is no valid LilyPond source
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream must not be null.");
        }
        String source = decode(StreamContentReader.getInstance().readAll(inputStream, MAX_SIZE, "LilyPond file"));
        try {
            LilyPondScore score = new LilyPondParser(source, LilyPondTokenizer.getInstance().tokenize(source)).parse();
            return ScoreBuilder.getInstance().build(LilyPondMeasureBuilder.getInstance().build(score));
        } catch (RuntimeException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read LilyPond: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid LilyPond file: " + e.getMessage(), e);
        }
    }


    /**
     * Decodes the source: UTF-8 (with or without byte order mark), otherwise ISO-8859-1.
     *
     * @param content the bytes
     * @return the text
     */
    private String decode(byte[] content) {
        int offset = 0;
        if (content.length >= 3 && (content[0] & 0xFF) == 0xEF && (content[1] & 0xFF) == 0xBB && (content[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content, offset, content.length - offset)).toString();
        } catch (CharacterCodingException e) {
            return new String(content, StandardCharsets.ISO_8859_1);
        }
    }
}
