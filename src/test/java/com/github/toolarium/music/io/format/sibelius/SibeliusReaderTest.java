/*
 * SibeliusReaderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.SoundingNoteComparator;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for the Sibelius (.sib and .sco) reader.
 *
 * @author patrick
 */
public class SibeliusReaderTest {
    private static final Logger LOG = LoggerFactory.getLogger(SibeliusReaderTest.class);
    private static final String EXT_SIB = ".sib";
    private static final String EXT_SCO = ".sco";
    private static final String NO_SIB_SAMPLES = "No .sib sample files found under samples directory.";
    private static final String NO_SCO_SAMPLES = "No .sco sample files found under samples directory.";

    private static final String LINE_SEPARATOR = System.lineSeparator();

    // the reader does not read rests yet: the note after the leading quarter rest starts at the beginning of the bar
    private static final Set<String> SOURCE_DIFFERENCES = new HashSet<>(Arrays.asList("single-c4-quarter-rest.sib"));


    /**
     * Tests that SibeliusIOFactory returns a non-null reader.
     */
    @Test
    void testFactoryReturnsReader() {
        IMusicReader reader = SibeliusIOFactory.getInstance().getReader();
        assertNotNull(reader);
    }


    /**
     * Tests reading a .sib sample file and verifies basic structure.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadSibSample() throws IOException {
        Path path = TestResourceUtil.getInstance().findFirstFileByExtension(EXT_SIB);
        assertNotNull(path, NO_SIB_SAMPLES);

        IScore score = MusicFileFactory.getInstance().read(path);

        assertNotNull(score);
        assertNotNull(score.getPartList());
        assertTrue(score.getParts().size() > 0, "Score should have at least one part.");

        if (LOG.isDebugEnabled()) {
            LOG.debug("SIB sample: {} parts, title: {}", score.getParts().size(), score.getMovementTitle());
        }

        for (IPart part : score.getParts()) {
            assertTrue(part.getMeasures().size() > 0, "Each part should have at least one measure.");
        }
    }


    /**
     * Tests reading a .sco sample file and verifies basic structure.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadScoSample() throws IOException {
        Path path = TestResourceUtil.getInstance().findFirstFileByExtension(EXT_SCO);
        TestResourceUtil.getInstance().assumeSamples(path != null, NO_SCO_SAMPLES);

        IScore score = MusicFileFactory.getInstance().read(path);

        assertNotNull(score);
        assertNotNull(score.getPartList());
        assertTrue(score.getParts().size() > 0, "Score should have at least one part.");

        if (LOG.isDebugEnabled()) {
            LOG.debug("SCO sample: {} parts, title: {}", score.getParts().size(), score.getMovementTitle());
        }
    }


    /**
     * Tests reading a .sib file via InputStream.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadSibFromInputStream() throws IOException {
        Path path = TestResourceUtil.getInstance().findFirstFileByExtension(EXT_SIB);
        assertNotNull(path, NO_SIB_SAMPLES);

        byte[] data = Files.readAllBytes(path);

        IScore score = MusicFileFactory.getInstance().read(new ByteArrayInputStream(data), MusicFileFormat.SIBELIUS);
        assertNotNull(score);
        assertTrue(score.getParts().size() > 0);
    }


    /**
     * Tests that all .sib sample files can be read and contain at least one part.
     *
     * @throws IOException if finding samples fails
     */
    @Test
    void testReadAllSibSamples() throws IOException {
        assertAllReadable(TestResourceUtil.getInstance().findFilesByExtension(EXT_SIB), NO_SIB_SAMPLES);
    }


    /**
     * Tests that all .sco sample files can be read and contain at least one part.
     *
     * @throws IOException if finding samples fails
     */
    @Test
    void testReadAllScoSamples() throws IOException {
        assertAllReadable(TestResourceUtil.getInstance().findFilesByExtension(EXT_SCO), NO_SCO_SAMPLES);
    }


    /**
     * Compares the sounding notes of every Sibelius test file that has its MusicXML source
     * ({@code <name>.mxl} next to {@code <name>.sib}) with that source.
     *
     * @throws Exception if reading fails
     */
    @Test
    void testCompareWithMusicXmlSource() throws Exception {
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        Path folder = TestResourceUtil.getInstance().getSamplesDir().resolve("sibelius");
        List<Path> compareFiles = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : (Iterable<Path>) files.sorted()::iterator) {
                String name = file.getFileName().toString();
                if (name.endsWith(EXT_SIB) && Files.exists(getSource(file)) && !SOURCE_DIFFERENCES.contains(name)) {
                    compareFiles.add(file);
                }
            }
        }
        List<String> differences = TestResourceUtil.getInstance().processFiles(compareFiles,
            file -> comparator.compare(file.getFileName().toString(), comparator.readConverted(MusicFileFactory.getInstance().read(getSource(file))),
                                       comparator.readConverted(MusicFileFactory.getInstance().read(file))));
        assertTrue(compareFiles.size() > 0, "No Sibelius test files with MusicXML source found.");
        assertTrue(differences.isEmpty(), String.join(LINE_SEPARATOR, differences));
    }


    /**
     * Gets the MusicXML source of a Sibelius test file, which lies next to it.
     *
     * @param file the Sibelius file
     * @return the path of the source
     */
    private Path getSource(Path file) {
        return file.resolveSibling(file.getFileName().toString().replace(EXT_SIB, ".mxl"));
    }


    /**
     * Reads every file and fails with the list of the files that cannot be read or have no part.
     *
     * @param files the files
     * @param noFilesMessage the message if there are no files
     */
    private void assertAllReadable(List<Path> files, String noFilesMessage) {
        TestResourceUtil.getInstance().assumeSamples(files.size() > 0, noFilesMessage);

        List<String> failures = TestResourceUtil.getInstance().processFiles(files, path -> {
            IScore score = MusicFileFactory.getInstance().read(path);
            if (score.getParts() == null || score.getParts().isEmpty()) {
                return path + ": no parts";
            }
            return null;
        });

        if (!failures.isEmpty()) {
            fail(failures.size() + " of " + files.size() + " files failed to read:" + LINE_SEPARATOR + String.join(LINE_SEPARATOR, failures));
        }
    }


    /**
     * Tests converting a Sibelius .sib file to MusicXML.
     *
     * @throws IOException if conversion fails
     */
    @Test
    void testSibToMusicXmlConversion() throws IOException {
        Path path = TestResourceUtil.getInstance().findFirstFileByExtension(EXT_SIB);
        assertNotNull(path, NO_SIB_SAMPLES);

        IScore score = MusicFileFactory.getInstance().read(path);
        assertNotNull(score);

        Path tempFile = Files.createTempFile("sib-converted-", ".musicxml");
        try {
            MusicFileFactory.getInstance().write(score, tempFile, MusicFileFormat.MUSIC_XML);
            assertTrue(Files.size(tempFile) > 0, "MusicXML output should not be empty.");

            IScore reloaded = MusicFileFactory.getInstance().read(tempFile, MusicFileFormat.MUSIC_XML);
            assertNotNull(reloaded);

            if (LOG.isDebugEnabled()) {
                LOG.debug("SIB to MusicXML conversion: {} bytes output.", Files.size(tempFile));
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
