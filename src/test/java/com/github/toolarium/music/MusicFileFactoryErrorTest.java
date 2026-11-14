/*
 * MusicFileFactoryErrorTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;


/**
 * Tests error paths in {@link MusicFileFactory}: null parameters,
 * unsupported formats, and unknown file extensions.
 *
 * @author patrick
 */
public class MusicFileFactoryErrorTest {


    /**
     * Tests that reading from a null path throws IOException.
     */
    @Test
    void testReadNullPath() {
        assertThrows(IOException.class, () -> {
            MusicFileFactory.getInstance().read((Path) null);
        });
    }


    /**
     * Tests that reading from a null input stream throws IOException.
     */
    @Test
    void testReadNullInputStream() {
        assertThrows(IOException.class, () -> {
            MusicFileFactory.getInstance().read((InputStream) null, MusicFileFormat.MUSIC_XML);
        });
    }


    /**
     * Tests that reading with a null format throws IOException.
     */
    @Test
    void testReadNullFormat() {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        assertThrows(IOException.class, () -> {
            MusicFileFactory.getInstance().read(bais, null);
        });
    }


    /**
     * Tests that writing a null score throws IOException.
     */
    @Test
    void testWriteNullScore() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertThrows(IOException.class, () -> {
            MusicFileFactory.getInstance().write(null, baos, MusicFileFormat.MUSIC_XML);
        });
    }


    /**
     * Tests that writing to a null output stream throws IOException.
     */
    @Test
    void testWriteNullOutputStream() {
        ScoreImpl score = new ScoreImpl();
        assertThrows(IOException.class, () -> {
            MusicFileFactory.getInstance().write(score, (OutputStream) null, MusicFileFormat.MUSIC_XML);
        });
    }


    /**
     * Tests that writing to a null path throws IOException.
     */
    @Test
    void testWriteNullPath() {
        ScoreImpl score = new ScoreImpl();
        assertThrows(IOException.class, () -> {
            MusicFileFactory.getInstance().write(score, (Path) null);
        });
    }


    /**
     * Tests that writing with an unsupported format (FINALE) throws IOException.
     */
    @Test
    void testWriteUnsupportedFormat() {
        ScoreImpl score = new ScoreImpl();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assertThrows(IOException.class, () -> {
            MusicFileFactory.getInstance().write(score, baos, MusicFileFormat.FINALE);
        });
    }


    /**
     * Tests that reading with an unsupported format (FINALE) throws IOException.
     */
    @Test
    void testReadUnsupportedFormat() {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        assertThrows(IOException.class, () -> {
            MusicFileFactory.getInstance().read(bais, MusicFileFormat.FINALE);
        });
    }


    /**
     * Tests that reading a file with an unknown extension throws IOException.
     *
     * @throws IOException if temp file creation fails
     */
    @Test
    void testReadUnknownExtension() throws IOException {
        Path tempFile = Files.createTempFile("test-unknown-", ".xyz");
        try {
            assertThrows(IOException.class, () -> {
                MusicFileFactory.getInstance().read(tempFile);
            });
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }


    /**
     * Tests that writing a file with an unknown extension throws IOException.
     *
     * @throws IOException if temp file creation fails
     */
    @Test
    void testWriteUnknownExtension() throws IOException {
        Path tempFile = Files.createTempFile("test-unknown-", ".xyz");
        try {
            ScoreImpl score = new ScoreImpl();
            assertThrows(IOException.class, () -> {
                MusicFileFactory.getInstance().write(score, tempFile);
            });
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
