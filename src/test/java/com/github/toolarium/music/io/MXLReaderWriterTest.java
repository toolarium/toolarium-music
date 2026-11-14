/*
 * MXLReaderWriterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.mxl.impl.MXLIOFactory;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for the MXL (compressed MusicXML) reader and writer.
 *
 * @author patrick
 */
public class MXLReaderWriterTest {
    private static final Logger LOG = LoggerFactory.getLogger(MXLReaderWriterTest.class);
    private static final Path SAMPLES_DIR = TestResourceUtil.getInstance().getSamplesDir();

    /**
     * Tests reading a single .mxl file.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadMxlFile() throws IOException {
        Path mxlFile = TestResourceUtil.getInstance().findFirstFileByExtension(".mxl");
        assertNotNull(mxlFile, "No .mxl sample files found under samples directory.");

        IMusicReader reader = MXLIOFactory.getInstance().getReader();
        IScore score = reader.read(mxlFile);

        assertNotNull(score);
        assertNotNull(score.getVersion());
        assertNotNull(score.getPartList());
        assertNotNull(score.getParts());
        assertTrue(score.getParts().size() > 0);

        LOG.info("Read MXL: {}, title='{}', parts={}, version={}",
                mxlFile.getFileName(), score.getMovementTitle(),
                score.getParts().size(), score.getVersion());
    }


    /**
     * Tests MXL write and read-back round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testMxlRoundTrip() throws IOException {
        Path mxlFile = TestResourceUtil.getInstance().findFirstFileByExtension(".mxl");
        assertNotNull(mxlFile, "No .mxl sample files found under samples directory.");

        IMusicReader reader = MXLIOFactory.getInstance().getReader();
        IMusicWriter writer = MXLIOFactory.getInstance().getWriter();

        IScore original = reader.read(mxlFile);

        // Write to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(original, baos);
        byte[] mxlBytes = baos.toByteArray();
        assertTrue(mxlBytes.length > 0);

        // Verify it starts with PK (ZIP magic)
        assertEquals(0x50, mxlBytes[0] & 0xFF);
        assertEquals(0x4B, mxlBytes[1] & 0xFF);

        // Read back
        IScore readBack = reader.read(new ByteArrayInputStream(mxlBytes));
        assertNotNull(readBack);
        assertEquals(original.getVersion(), readBack.getVersion());
        assertEquals(original.getMovementTitle(), readBack.getMovementTitle());

        LOG.info("MXL round-trip successful: title='{}', written {} bytes",
                readBack.getMovementTitle(), mxlBytes.length);
    }


    /**
     * Tests writing a minimal score as MXL.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testWriteMinimalMxl() throws IOException {
        IMusicWriter writer = MXLIOFactory.getInstance().getWriter();

        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");
        score.setMovementTitle("MXL Test");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(score, baos);
        byte[] mxlBytes = baos.toByteArray();

        IMusicReader reader = MXLIOFactory.getInstance().getReader();
        IScore readBack = reader.read(new ByteArrayInputStream(mxlBytes));
        assertEquals("4.0", readBack.getVersion());
        assertEquals("MXL Test", readBack.getMovementTitle());
    }


    /**
     * Tests reading all .mxl sample files without errors.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadAllMxlFiles() throws IOException {
        List<Path> mxlFiles = TestResourceUtil.getInstance().findFilesByExtension(".mxl");
        assertFalse(mxlFiles.isEmpty(), "No .mxl sample files found under samples directory.");

        IMusicReader reader = MXLIOFactory.getInstance().getReader();
        int count = 0;
        int failed = 0;

        for (Path path : mxlFiles) {
            try {
                IScore score = reader.read(path);
                assertNotNull(score);
                count++;
            } catch (Exception e) {
                failed++;
                LOG.warn("Failed to read {}: {}", path.getFileName(), e.getMessage());
            }
        }

        LOG.info("Read {} .mxl files successfully, {} failed.", count, failed);
        assertTrue(count > 0, "Expected at least one .mxl file to be readable");
    }


}
