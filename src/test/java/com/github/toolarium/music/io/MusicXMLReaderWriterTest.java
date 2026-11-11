/*
 * MusicXMLReaderWriterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.io.musicxml.impl.MusicXMLIOFactory;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for the MusicXML reader and writer.
 *
 * @author patrick
 */
public class MusicXMLReaderWriterTest {
    private static final Logger LOG = LoggerFactory.getLogger(MusicXMLReaderWriterTest.class);
    private static final Path SAMPLES_DIR = Paths.get("src/test/resources/samples");

    /**
     * Tests reading a MusicXML file and verifying basic structure.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadMusicXML() throws IOException {
        Path sampleFile = SAMPLES_DIR.resolve("sample-score-partwise.musicxml");
        if (!Files.exists(sampleFile)) {
            LOG.info("Sample MusicXML file not found, skipping test.");
            return;
        }

        IMusicReader reader = MusicXMLIOFactory.getInstance().getReader();
        IScore score = reader.read(sampleFile);

        assertNotNull(score);
        assertNotNull(score.getVersion());
        assertEquals("4.0", score.getVersion());
        assertNotNull(score.getMovementTitle());
        assertNotNull(score.getIdentification());
        assertNotNull(score.getPartList());
        assertNotNull(score.getParts());
        assertTrue(score.getParts().size() > 0);

        LOG.info("Read MusicXML: title='{}', parts={}, version={}",
                score.getMovementTitle(), score.getParts().size(), score.getVersion());
    }


    /**
     * Tests reading identification metadata (composer, arranger, encoding).
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadIdentification() throws IOException {
        Path sampleFile = SAMPLES_DIR.resolve("sample-score-partwise.musicxml");
        if (!Files.exists(sampleFile)) {
            LOG.info("Sample MusicXML file not found, skipping test.");
            return;
        }

        IMusicReader reader = MusicXMLIOFactory.getInstance().getReader();
        IScore score = reader.read(sampleFile);

        assertNotNull(score.getIdentification());
        assertNotNull(score.getIdentification().getCreators());
        assertTrue(score.getIdentification().getCreators().size() > 0);
        assertNotNull(score.getIdentification().getEncoding());

        LOG.info("IdentificationImpl: creators={}, encoding.software={}",
                score.getIdentification().getCreators().size(),
                score.getIdentification().getEncoding().getSoftware());
    }


    /**
     * Tests reading part list and score parts.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadPartList() throws IOException {
        Path sampleFile = SAMPLES_DIR.resolve("sample-score-partwise.musicxml");
        if (!Files.exists(sampleFile)) {
            LOG.info("Sample MusicXML file not found, skipping test.");
            return;
        }

        IMusicReader reader = MusicXMLIOFactory.getInstance().getReader();
        IScore score = reader.read(sampleFile);

        assertNotNull(score.getPartList());
        assertNotNull(score.getPartList().getScoreParts());
        assertTrue(score.getPartList().getScoreParts().size() > 0);

        String partName = score.getPartList().getScoreParts().get(0).getPartName();
        assertNotNull(partName);

        LOG.info("PartImpl list: {} parts, first part name='{}'",
                score.getPartList().getScoreParts().size(), partName);
    }


    /**
     * Tests writing a ScoreImpl to XML and reading it back.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteAndReadBack() throws IOException {
        Path sampleFile = SAMPLES_DIR.resolve("sample-score-partwise.musicxml");
        if (!Files.exists(sampleFile)) {
            LOG.info("Sample MusicXML file not found, skipping test.");
            return;
        }

        IMusicReader reader = MusicXMLIOFactory.getInstance().getReader();
        IMusicWriter writer = MusicXMLIOFactory.getInstance().getWriter();

        IScore original = reader.read(sampleFile);

        // Write to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(original, baos);
        byte[] xmlBytes = baos.toByteArray();
        assertTrue(xmlBytes.length > 0);

        String xmlContent = new String(xmlBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(xmlContent.contains("score-partwise"));
        assertTrue(xmlContent.contains("<?xml"));

        LOG.info("Written XML: {} bytes", xmlBytes.length);

        // Read back
        IScore readBack = reader.read(new ByteArrayInputStream(xmlBytes));
        assertNotNull(readBack);
        assertEquals(original.getVersion(), readBack.getVersion());
        assertEquals(original.getMovementTitle(), readBack.getMovementTitle());

        LOG.info("Round-trip successful: title='{}', version='{}'",
                readBack.getMovementTitle(), readBack.getVersion());
    }


    /**
     * Tests reading a programmatically constructed minimal score.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteMinimalScore() throws IOException {
        IMusicWriter writer = MusicXMLIOFactory.getInstance().getWriter();

        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");
        score.setMovementTitle("Test Score");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(score, baos);

        String xml = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(xml.contains("score-partwise"));
        assertTrue(xml.contains("Test Score"));

        // Read it back
        IMusicReader reader = MusicXMLIOFactory.getInstance().getReader();
        IScore readBack = reader.read(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals("4.0", readBack.getVersion());
        assertEquals("Test Score", readBack.getMovementTitle());
    }


    /**
     * Tests reading all .musicxml files from the extracted MXL samples.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadAllMusicXMLFiles() throws IOException {
        if (!Files.exists(SAMPLES_DIR)) {
            LOG.info("Samples directory not found, skipping test.");
            return;
        }

        IMusicReader reader = MusicXMLIOFactory.getInstance().getReader();
        int count = 0;

        try (Stream<Path> paths = Files.walk(SAMPLES_DIR)) {
            for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".musicxml"))::iterator) {
                IScore score = reader.read(path);
                assertNotNull(score);
                count++;
            }
        }

        LOG.info("Successfully read {} .musicxml files.", count);
    }
}
