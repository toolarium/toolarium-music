/*
 * MusicFileFacadeTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for the MusicFileFactory (auto-detection of format by extension).
 *
 * @author patrick
 */
public class MusicFileFacadeTest {
    private static final Logger LOG = LoggerFactory.getLogger(MusicFileFacadeTest.class);
    private static final Path SAMPLES_DIR = Paths.get("src/test/resources/samples");

    /**
     * Tests reading a .musicxml file via the factory.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadMusicXmlViaFactory() throws IOException {
        Path sampleFile = SAMPLES_DIR.resolve("sample-score-partwise.musicxml");
        if (!Files.exists(sampleFile)) {
            LOG.info("Sample .musicxml file not found, skipping test.");
            return;
        }

        IScore score = MusicFileFactory.getInstance().read(sampleFile);

        assertNotNull(score);
        assertNotNull(score.getVersion());
        assertNotNull(score.getMovementTitle());

        LOG.info("Factory read .musicxml: title='{}', version='{}'",
                score.getMovementTitle(), score.getVersion());
    }


    /**
     * Tests reading a .mxl file via the factory (auto-detect by extension).
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadMxlViaFactory() throws IOException {
        Path mxlFile = findFirstMxl();
        if (mxlFile == null) {
            LOG.info("No .mxl sample files found, skipping test.");
            return;
        }

        IScore score = MusicFileFactory.getInstance().read(mxlFile);

        assertNotNull(score);
        assertNotNull(score.getPartList());
        assertTrue(score.getParts().size() > 0);

        LOG.info("Factory read .mxl: title='{}', parts={}",
                score.getMovementTitle(), score.getParts().size());
    }


    /**
     * Tests writing as .mxl and reading back via the factory.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteMxlAndReadBack() throws IOException {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");
        score.setMovementTitle("Facade Test");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MusicFileFactory.getInstance().write(score, baos, MusicFileFormat.MXL);
        byte[] mxlBytes = baos.toByteArray();
        assertTrue(mxlBytes.length > 0);

        IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(mxlBytes), MusicFileFormat.MXL);
        assertEquals("4.0", readBack.getVersion());
        assertEquals("Facade Test", readBack.getMovementTitle());
    }


    /**
     * Tests writing as .musicxml and reading back via the factory.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteMusicXmlAndReadBack() throws IOException {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");
        score.setMovementTitle("Plain XML Test");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MusicFileFactory.getInstance().write(score, baos, MusicFileFormat.MUSIC_XML);
        byte[] xmlBytes = baos.toByteArray();

        IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(xmlBytes), MusicFileFormat.MUSIC_XML);
        assertEquals("4.0", readBack.getVersion());
        assertEquals("Plain XML Test", readBack.getMovementTitle());
    }


    /**
     * Tests that the factory auto-detects format by file extension when writing.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testAutoDetectWriteFormat() throws IOException {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");
        score.setMovementTitle("Auto Detect");

        Path tempMxl = Files.createTempFile("test-", ".mxl");
        Path tempXml = Files.createTempFile("test-", ".musicxml");

        try {
            MusicFileFactory.getInstance().write(score, tempMxl);
            MusicFileFactory.getInstance().write(score, tempXml);

            // .mxl should be a ZIP (PK magic)
            byte[] mxlBytes = Files.readAllBytes(tempMxl);
            assertEquals(0x50, mxlBytes[0] & 0xFF);
            assertEquals(0x4B, mxlBytes[1] & 0xFF);

            // .musicxml should be XML (<?xml)
            byte[] xmlBytes = Files.readAllBytes(tempXml);
            String xmlStart = new String(xmlBytes, 0, Math.min(10, xmlBytes.length));
            assertTrue(xmlStart.contains("<?xml"));

            // Both should read back correctly
            IScore fromMxl = MusicFileFactory.getInstance().read(tempMxl);
            IScore fromXml = MusicFileFactory.getInstance().read(tempXml);
            assertEquals("Auto Detect", fromMxl.getMovementTitle());
            assertEquals("Auto Detect", fromXml.getMovementTitle());
        } finally {
            Files.deleteIfExists(tempMxl);
            Files.deleteIfExists(tempXml);
        }
    }


    /**
     * Finds the first .mxl file in the samples directory.
     *
     * @return the path, or null if not found
     * @throws IOException if an I/O error occurs
     */
    private Path findFirstMxl() throws IOException {
        if (!Files.exists(SAMPLES_DIR)) {
            return null;
        }

        try (java.util.stream.Stream<Path> paths = Files.walk(SAMPLES_DIR)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mxl"))
                    .findFirst()
                    .orElse(null);
        }
    }
}
