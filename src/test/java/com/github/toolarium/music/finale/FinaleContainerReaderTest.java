/*
 * FinaleContainerReaderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.finale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.finale.FinaleContainerFactory;
import com.github.toolarium.music.io.finale.IFinaleContainer;
import com.github.toolarium.music.io.finale.IFinaleContainerReader;
import com.github.toolarium.music.io.finale.IFinaleMetadata;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for the Finale container reader, covering both .musx (ZIP) and .mus (Enigma binary) formats.
 *
 * @author patrick
 */
public class FinaleContainerReaderTest {
    private static final Logger LOG = LoggerFactory.getLogger(FinaleContainerReaderTest.class);
    private static final Path SAMPLES_DIR = TestResourceUtil.getInstance().getSamplesDir();

    /**
     * Tests reading a .musx file (ZIP container format).
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadMusxFile() throws IOException {
        Path musxFile = TestResourceUtil.getInstance().findFirstFileByExtension(".musx");
        assertNotNull(musxFile, "No .musx sample files found under samples directory.");

        IFinaleContainerReader reader = FinaleContainerFactory.getInstance().getReader();
        IFinaleContainer container = reader.read(musxFile);

        assertNotNull(container);
        assertTrue(container.isMUSX());
        assertEquals("application/vnd.makemusic.notation", container.getMimetype());
        assertNotNull(container.getContainerVersion());
        assertNotNull(container.getRootFilePath());
        assertNotNull(container.getRootMediaType());
        assertNotNull(container.getScoreData());
        assertTrue(container.getScoreData().length > 0);
        assertNull(container.getEnigmaHeader());

        LOG.info("Read MUSX file: {}, score data: {} bytes, version: {}",
                musxFile.getFileName(), container.getScoreData().length, container.getContainerVersion());
    }


    /**
     * Tests that .musx metadata is correctly parsed from NotationMetadata.xml.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testMusxMetadata() throws IOException {
        Path musxFile = TestResourceUtil.getInstance().findFirstFileByExtension(".musx");
        assertNotNull(musxFile, "No .musx sample files found under samples directory.");

        IFinaleContainerReader reader = FinaleContainerFactory.getInstance().getReader();
        IFinaleContainer container = reader.read(musxFile);

        IFinaleMetadata metadata = container.getMetadata();
        assertNotNull(metadata);
        assertNotNull(metadata.getKeySignature());
        assertNotNull(metadata.getInitialTempo());
        assertNotNull(metadata.getScoreDuration());
        assertNotNull(metadata.getCreatorString());

        LOG.info("Metadata: key={}, tempo={}, duration={}, creator={}",
                metadata.getKeySignature(), metadata.getInitialTempo(),
                metadata.getScoreDuration(), metadata.getCreatorString());
    }


    /**
     * Tests reading a .mus file (raw Enigma binary format).
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadMusFile() throws IOException {
        Path musFile = TestResourceUtil.getInstance().findFirstFileByExtension(".mus");
        assertNotNull(musFile, "No .mus sample files found under samples directory.");

        IFinaleContainerReader reader = FinaleContainerFactory.getInstance().getReader();
        IFinaleContainer container = reader.read(musFile);

        assertNotNull(container);
        assertFalse(container.isMUSX());
        assertNotNull(container.getScoreData());
        assertTrue(container.getScoreData().length > 0);
        assertNotNull(container.getEnigmaHeader());
        assertTrue(container.getEnigmaHeader().startsWith("ENIGMA BINARY FILE"));
        assertNull(container.getMetadata());

        LOG.info("Read MUS file: {}, score data: {} bytes, header: {}",
                musFile.getFileName(), container.getScoreData().length, container.getEnigmaHeader());
    }


    /**
     * Tests that an unknown format throws an IOException.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testUnknownFormatThrows() throws IOException {
        byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};
        IFinaleContainerReader reader = FinaleContainerFactory.getInstance().getReader();
        assertThrows(IOException.class, () -> reader.read(new ByteArrayInputStream(garbage)));
    }


    /**
     * Tests reading all available .musx files to ensure none throw exceptions.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadAllMusxFiles() throws IOException {
        List<Path> musxFiles = TestResourceUtil.getInstance().findFilesByExtension(".musx");
        assertFalse(musxFiles.isEmpty(), "No .musx sample files found under samples directory.");

        IFinaleContainerReader reader = FinaleContainerFactory.getInstance().getReader();
        int count = 0;

        for (Path path : musxFiles) {
            IFinaleContainer container = reader.read(path);
            assertNotNull(container);
            assertTrue(container.isMUSX());
            assertNotNull(container.getScoreData());
            count++;
        }

        LOG.info("Successfully read {} .musx files.", count);
    }


    /**
     * Tests reading all available .mus files to ensure none throw exceptions.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testReadAllMusFiles() throws IOException {
        List<Path> musFiles = TestResourceUtil.getInstance().findFilesByExtension(".mus");
        assertFalse(musFiles.isEmpty(), "No .mus sample files found under samples directory.");

        IFinaleContainerReader reader = FinaleContainerFactory.getInstance().getReader();
        int count = 0;

        for (Path path : musFiles) {
            IFinaleContainer container = reader.read(path);
            assertNotNull(container);
            assertFalse(container.isMUSX());
            assertNotNull(container.getScoreData());
            count++;
        }

        LOG.info("Successfully read {} .mus files.", count);
    }


}
