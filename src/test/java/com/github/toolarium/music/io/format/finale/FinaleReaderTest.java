/*
 * FinaleReaderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for the Finale (.musx and .mus) reader.
 *
 * @author patrick
 */
public class FinaleReaderTest {
    private static final Logger LOG = LoggerFactory.getLogger(FinaleReaderTest.class);
    private static final String EXT_MUSX = ".musx";
    private static final String EXT_MUS = ".mus";
    private static final String NO_MUSX_SAMPLES = "No .musx sample files found under samples directory.";
    private static final String NO_MUS_SAMPLES = "No .mus sample files found under samples directory.";
    private static final String UNREADABLE = "unreadable: ";


    /**
     * Tests that FinaleIOFactory returns a non-null reader.
     */
    @Test
    void testFactoryReturnsReader() {
        IMusicReader reader = FinaleIOFactory.getInstance().getReader();
        assertNotNull(reader);
    }


    /**
     * Tests reading a .musx sample file and verifies basic structure.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadMusxSample() throws IOException {
        Path path = TestResourceUtil.getInstance().findFirstFileByExtension(EXT_MUSX);
        assertNotNull(path, NO_MUSX_SAMPLES);

        IScore score = MusicFileFactory.getInstance().read(path);

        assertNotNull(score);
        assertNotNull(score.getPartList());
        assertTrue(score.getParts().size() > 0, "Score should have at least one part.");

        if (LOG.isDebugEnabled()) {
            LOG.debug("MUSX sample: {} parts, title: {}",
                    score.getParts().size(), score.getMovementTitle());
        }

        for (IPart part : score.getParts()) {
            assertTrue(part.getMeasures().size() > 0,
                    "Each part should have at least one measure.");
        }
    }


    /**
     * Tests reading a .mus sample file and verifies basic structure.
     * Some .mus files may use unsupported compression; the test tries
     * files until it finds one that can be parsed.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadMusSample() throws IOException {
        IScore score = readFirstParsableMus();

        assertNotNull(score);
        assertNotNull(score.getPartList());
        assertTrue(score.getParts().size() > 0, "Score should have at least one part.");

        if (LOG.isDebugEnabled()) {
            LOG.debug("MUS sample: {} parts, title: {}",
                    score.getParts().size(), score.getMovementTitle());
        }

        for (IPart part : score.getParts()) {
            assertTrue(part.getMeasures().size() > 0,
                    "Each part should have at least one measure.");
        }
    }


    /**
     * Tests converting a Finale .musx file to MXL by reading and writing through the factory.
     *
     * @throws IOException if conversion fails
     */
    @Test
    void testFinaleMusxToMxlConversion() throws IOException {
        Path inputPath = TestResourceUtil.getInstance().findFirstFileByExtension(EXT_MUSX);
        assertNotNull(inputPath, NO_MUSX_SAMPLES);

        IScore score = MusicFileFactory.getInstance().read(inputPath);
        assertNotNull(score);

        Path tempFile = Files.createTempFile("finale-converted-", ".mxl");
        try {
            MusicFileFactory.getInstance().write(score, tempFile, MusicFileFormat.MXL);
            assertTrue(Files.size(tempFile) > 0, "MXL output should not be empty.");

            IScore reloaded = MusicFileFactory.getInstance().read(tempFile, MusicFileFormat.MXL);
            assertNotNull(reloaded);
            assertEquals(score.getParts().size(), reloaded.getParts().size(),
                    "Round-tripped score should have the same number of parts.");

            if (LOG.isDebugEnabled()) {
                LOG.debug("Finale MUSX to MXL conversion: {} bytes output.", Files.size(tempFile));
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }


    /**
     * Tests reading a Finale .musx file via InputStream.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadMusxFromInputStream() throws IOException {
        Path path = TestResourceUtil.getInstance().findFirstFileByExtension(EXT_MUSX);
        assertNotNull(path, NO_MUSX_SAMPLES);

        byte[] data = Files.readAllBytes(path);

        IScore score = MusicFileFactory.getInstance().read(
                new ByteArrayInputStream(data), MusicFileFormat.FINALE);
        assertNotNull(score);
        assertTrue(score.getParts().size() > 0);
    }


    /**
     * Tests reading a Finale .mus file via InputStream.
     * Some .mus files may use unsupported compression; the test tries
     * files until it finds one that can be parsed.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadMusFromInputStream() throws IOException {
        List<Path> musFiles = TestResourceUtil.getInstance().findFilesByExtension(EXT_MUS);
        TestResourceUtil.getInstance().assumeSamples(musFiles.size() > 0, NO_MUS_SAMPLES);

        for (Path path : musFiles) {
            try {
                byte[] data = Files.readAllBytes(path);
                IScore score = MusicFileFactory.getInstance().read(
                        new ByteArrayInputStream(data), MusicFileFormat.FINALE);
                assertNotNull(score);
                assertTrue(score.getParts().size() > 0);
                return;
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping {}: {}", path.getFileName(), e.getMessage(), e);
                }
            }
        }

        fail("No .mus sample could be read via InputStream.");
    }


    /**
     * Tests that notes in a .musx score have valid pitches or rests.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testMusxNotesHavePitchOrRest() throws IOException {
        Path path = TestResourceUtil.getInstance().findFirstFileByExtension(EXT_MUSX);
        assertNotNull(path, NO_MUSX_SAMPLES);

        IScore score = MusicFileFactory.getInstance().read(path);

        for (IPart part : score.getParts()) {
            for (IMeasure measure : part.getMeasures()) {
                if (measure.getNotes() != null) {
                    for (INote note : measure.getNotes()) {
                        boolean hasPitch = note.getPitch() != null;
                        boolean hasRest = note.getRest() != null;
                        assertTrue(hasPitch || hasRest,
                                "Every note must have a pitch or be a rest.");
                        assertNotNull(note.getType(),
                                "Every note must have a type.");
                        assertTrue(note.getDuration() > 0,
                                "Every note must have a positive duration.");
                    }
                }
            }
        }
    }


    /**
     * Tests converting a Finale .musx file to MusicXML format.
     *
     * @throws IOException if conversion fails
     */
    @Test
    void testFinaleMusxToMusicXmlConversion() throws IOException {
        Path inputPath = TestResourceUtil.getInstance().findFirstFileByExtension(EXT_MUSX);
        assertNotNull(inputPath, NO_MUSX_SAMPLES);

        IScore score = MusicFileFactory.getInstance().read(inputPath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MusicFileFactory.getInstance().write(score, baos, MusicFileFormat.MUSIC_XML);
        String xml = baos.toString("UTF-8");

        assertTrue(xml.contains("<score-partwise"), "Output should be MusicXML.");
        assertTrue(xml.contains("<part-list>"), "Output should contain part-list.");
        assertTrue(xml.contains("<note"), "Output should contain notes.");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Finale MUSX to MusicXML: {} characters.", xml.length());
        }
    }


    /**
     * Tests reading all available .musx sample files to ensure none throw exceptions.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadAllMusxSamples() throws IOException {
        List<Path> musxFiles = TestResourceUtil.getInstance().findFilesByExtension(EXT_MUSX);
        assertTrue(musxFiles.size() > 0, NO_MUSX_SAMPLES);

        List<String> failures = TestResourceUtil.getInstance().processFiles(musxFiles, path -> {
            IScore score = MusicFileFactory.getInstance().read(path);
            assertNotNull(score, "Score from " + path.getFileName() + " should not be null.");
            assertNotNull(score.getPartList(), "Part list from " + path.getFileName() + " should not be null.");
            assertTrue(score.getParts().size() > 0, "Score from " + path.getFileName() + " should have parts.");
            return null;
        });

        assertTrue(failures.isEmpty(), failures.size() + " of " + musxFiles.size() + " .musx files failed:"
                + System.lineSeparator() + String.join(System.lineSeparator(), failures));
        LOG.info("Successfully read {} .musx files.", musxFiles.size());
    }


    /**
     * Tests reading all available .mus sample files.
     * Some files may use unsupported compression (DCL) or other edge cases;
     * the test verifies that at least 50% of files are read successfully.
     *
     * @throws IOException if an I/O error occurs during file discovery
     */
    @Test
    void testReadAllMusSamples() throws IOException {
        List<Path> musFiles = TestResourceUtil.getInstance().findFilesByExtension(EXT_MUS);
        TestResourceUtil.getInstance().assumeSamples(musFiles.size() > 0, NO_MUS_SAMPLES);

        // a file which can't be read (IOException) is tolerated, any other failure fails the test
        List<String> failures = TestResourceUtil.getInstance().processFiles(musFiles, path -> {
            try {
                IScore score = MusicFileFactory.getInstance().read(path);
                assertNotNull(score, "Score from " + path.getFileName() + " should not be null.");
                assertNotNull(score.getPartList(), "Part list from " + path.getFileName() + " should not be null.");
                assertTrue(score.getParts().size() > 0, "Score from " + path.getFileName() + " should have parts.");
                return null;
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to read {}: {}", path.getFileName(), e.getMessage(), e);
                }
                return UNREADABLE + path;
            }
        });

        List<String> errors = new ArrayList<>();
        int failCount = 0;
        for (String failure : failures) {
            if (failure.startsWith(UNREADABLE)) {
                failCount++;
            } else {
                errors.add(failure);
            }
        }
        assertTrue(errors.isEmpty(), errors.size() + " .mus files failed:" + System.lineSeparator()
                + String.join(System.lineSeparator(), errors));

        int total = musFiles.size();
        int successCount = total - failCount;
        LOG.info("Read {} of {} .mus files successfully ({} failed).", successCount, total, failCount);
        assertTrue(successCount > 0, "At least one .mus file should be read successfully.");
        assertTrue(successCount * 2 >= total,
                "At least 50% of .mus files should be read successfully, but only "
                + successCount + " of " + total + " succeeded.");
    }


    /**
     * Tries to read .mus sample files until one succeeds.
     * Some files may use unsupported compression (DCL) or other edge cases.
     *
     * @return a successfully parsed score
     * @throws IOException if no .mus file could be found or all files fail
     */
    private IScore readFirstParsableMus() throws IOException {
        List<Path> musFiles = TestResourceUtil.getInstance().findFilesByExtension(EXT_MUS);
        TestResourceUtil.getInstance().assumeSamples(musFiles.size() > 0, NO_MUS_SAMPLES);

        for (Path path : musFiles) {
            try {
                return MusicFileFactory.getInstance().read(path);
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping {}: {}", path.getFileName(), e.getMessage(), e);
                }
            }
        }

        fail("No .mus sample could be parsed successfully.");
        return null;
    }
}
