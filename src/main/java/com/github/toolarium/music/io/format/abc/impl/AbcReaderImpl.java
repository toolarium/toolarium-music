/*
 * AbcReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.converter.ScoreBuilder;
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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link IMusicReader} for ABC notation (.abc) files (ABC standard 2.1).
 * A file can contain several tunes; the first tune is read.
 *
 * @author patrick
 */
public class AbcReaderImpl implements IMusicReader {
    private static final Logger LOG = LoggerFactory.getLogger(AbcReaderImpl.class);
    private static final int MAX_SIZE = 16 * 1024 * 1024;


    /**
     * Constructor for AbcReaderImpl.
     */
    public AbcReaderImpl() {
        // default constructor
    }


    /**
     * Reads an ABC file from a path.
     *
     * @param path the path to the ABC file
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading ABC from path: {}", path);
        }

        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


    /**
     * Reads an ABC file from an input stream.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream must not be null.");
        }
        String text = decode(StreamContentReader.getInstance().readAll(inputStream, MAX_SIZE, "ABC file"));
        List<String> tune = firstTune(text);
        if (tune.isEmpty()) {
            throw new IOException("Invalid ABC file: no tune found.");
        }
        try {
            return ScoreBuilder.getInstance().build(new AbcTuneParser().parse(tune));
        } catch (RuntimeException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read ABC: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid ABC file: " + e.getMessage(), e);
        }
    }


    /**
     * Extracts the lines of the first tune. Fields and directives of the file header (before the
     * first X: line) are kept as defaults. Without an X: line the whole file is one tune.
     *
     * @param text the file content
     * @return the lines of the first tune
     */
    private List<String> firstTune(String text) {
        String[] lines = text.split("\r\n|\r|\n", -1);
        List<String> header = new ArrayList<>();
        List<String> tune = new ArrayList<>();
        boolean inTune = false;
        boolean hasX = false;
        for (String line : lines) {
            if (line.startsWith("X:")) {
                hasX = true;
                break;
            }
        }
        if (!hasX) {
            for (String line : lines) {
                tune.add(line);
            }
            return trimEmpty(tune);
        }
        for (String line : lines) {
            if (!inTune) {
                if (line.startsWith("X:")) {
                    inTune = true;
                    tune.addAll(header);
                    tune.add(line);
                } else if (line.startsWith("%%") || (line.length() > 1 && line.charAt(1) == ':' && Character.isLetter(line.charAt(0)))) {
                    header.add(line);
                }
                continue;
            }
            if (line.trim().isEmpty() || line.startsWith("X:")) {
                break;
            }
            tune.add(line);
        }
        return tune;
    }


    /**
     * Removes leading and trailing empty lines.
     *
     * @param lines the lines
     * @return the trimmed lines
     */
    private List<String> trimEmpty(List<String> lines) {
        int start = 0;
        int end = lines.size();
        while (start < end && lines.get(start).trim().isEmpty()) {
            start++;
        }
        while (end > start && lines.get(end - 1).trim().isEmpty()) {
            end--;
        }
        return new ArrayList<>(lines.subList(start, end));
    }


    /**
     * Decodes the file content as UTF-8, or as ISO-8859-1 if it is not valid UTF-8.
     *
     * @param content the content
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
