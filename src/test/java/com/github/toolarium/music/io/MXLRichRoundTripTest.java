/*
 * MXLRichRoundTripTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.mxl.impl.MXLIOFactory;
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
 * Round-trip tests for MXL files that verify rich content preservation
 * (parts, measures, notes, metadata) and exercise the MusicFileFactory facade.
 *
 * @author patrick
 */
public class MXLRichRoundTripTest {
    private static final Logger LOG = LoggerFactory.getLogger(MXLRichRoundTripTest.class);
    private static final String MXL_EXT = ".mxl";
    private static final String SKIP_MSG = "No .mxl sample files found, skipping test.";


    /**
     * Tests that the part count is preserved after an MXL write and read-back cycle
     * for every discovered .mxl sample file.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testMxlRoundTripPreservesPartCount() throws IOException {
        List<Path> mxlFiles = TestResourceUtil.getInstance().findFilesByExtension(MXL_EXT);
        assertFalse(mxlFiles.isEmpty(), SKIP_MSG);

        IMusicReader reader = MXLIOFactory.getInstance().getReader();
        IMusicWriter writer = MXLIOFactory.getInstance().getWriter();
        int passed = 0;
        List<String> failures = new ArrayList<>();

        for (Path mxlPath : mxlFiles) {
            try {
                IScore original = reader.read(mxlPath);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                writer.write(original, baos);

                IScore readBack = reader.read(new ByteArrayInputStream(baos.toByteArray()));
                assertNotNull(readBack, "Read-back score should not be null: " + mxlPath);

                int originalParts = 0;
                if (original.getParts() != null) {
                    originalParts = original.getParts().size();
                }
                int readBackParts = 0;
                if (readBack.getParts() != null) {
                    readBackParts = readBack.getParts().size();
                }

                assertEquals(originalParts, readBackParts, "Part count mismatch for: " + mxlPath.getFileName());

                LOG.info("Part count preserved for '{}': {} parts.", mxlPath.getFileName(), originalParts);
                passed++;
            } catch (Exception e) {
                String msg = mxlPath.getFileName() + ": " + e.getMessage();
                failures.add(msg);
                LOG.info("FAILED '{}': {}", mxlPath.getFileName(), e.getMessage());
            }
        }

        LOG.info("Part count round-trip: {} passed, {} failed out of {} .mxl files.", passed, failures.size(), mxlFiles.size());
        assertTrue(failures.isEmpty(), "Failed for " + failures.size() + " file(s): " + String.join("; ", failures));
    }


    /**
     * Tests that the measure count of the first part is preserved after
     * an MXL write and read-back cycle.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testMxlRoundTripPreservesMeasureCount() throws IOException {
        List<Path> mxlFiles = TestResourceUtil.getInstance().findFilesByExtension(MXL_EXT);
        assertFalse(mxlFiles.isEmpty(), SKIP_MSG);

        IMusicReader reader = MXLIOFactory.getInstance().getReader();

        IScore original = reader.read(mxlFiles.get(0));
        assertNotNull(original);
        assertNotNull(original.getParts());
        assertTrue(original.getParts().size() > 0, "Score should have at least one part");

        IPart firstPart = original.getParts().get(0);
        int originalMeasures = 0;
        if (firstPart.getMeasures() != null) {
            originalMeasures = firstPart.getMeasures().size();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IMusicWriter writer = MXLIOFactory.getInstance().getWriter();
        writer.write(original, baos);

        IScore readBack = reader.read(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(readBack.getParts());
        assertTrue(readBack.getParts().size() > 0);

        IPart readBackFirstPart = readBack.getParts().get(0);
        int readBackMeasures = 0;
        if (readBackFirstPart.getMeasures() != null) {
            readBackMeasures = readBackFirstPart.getMeasures().size();
        }

        assertEquals(originalMeasures, readBackMeasures, "Measure count mismatch for first part in: " + mxlFiles.get(0).getFileName());
        LOG.info("Measure count preserved for '{}': {} measures.", mxlFiles.get(0).getFileName(), originalMeasures);
    }


    /**
     * Tests that pitched note content in the first part is preserved after
     * an MXL round-trip, allowing up to 10% tolerance for serialization differences.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testMxlRoundTripPreservesNoteContent() throws IOException {
        List<Path> mxlFiles = TestResourceUtil.getInstance().findFilesByExtension(MXL_EXT);
        assertFalse(mxlFiles.isEmpty(), SKIP_MSG);

        IMusicReader reader = MXLIOFactory.getInstance().getReader();
        IMusicWriter writer = MXLIOFactory.getInstance().getWriter();

        IScore original = reader.read(mxlFiles.get(0));
        int originalNotes = countPitchedNotes(original);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(original, baos);

        IScore readBack = reader.read(new ByteArrayInputStream(baos.toByteArray()));
        int readBackNotes = countPitchedNotes(readBack);

        if (originalNotes > 0) {
            double ratio = (double) readBackNotes / originalNotes;
            assertTrue(ratio >= 0.9 && ratio <= 1.1, "Note count diverged beyond 10%: original=" + originalNotes + ", readBack=" + readBackNotes);
        }

        LOG.info("Note count for '{}': original={}, readBack={}.", mxlFiles.get(0).getFileName(), originalNotes, readBackNotes);
    }


    /**
     * Tests that score metadata (version, movement title, identification)
     * survives an MXL round-trip.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testMxlRoundTripPreservesMetadata() throws IOException {
        List<Path> mxlFiles = TestResourceUtil.getInstance().findFilesByExtension(MXL_EXT);
        assertFalse(mxlFiles.isEmpty(), SKIP_MSG);

        IMusicReader reader = MXLIOFactory.getInstance().getReader();
        IMusicWriter writer = MXLIOFactory.getInstance().getWriter();

        IScore original = reader.read(mxlFiles.get(0));
        assertNotNull(original);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(original, baos);

        IScore readBack = reader.read(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(readBack);

        assertEquals(original.getVersion(), readBack.getVersion(), "Version mismatch");
        assertEquals(original.getMovementTitle(), readBack.getMovementTitle(), "Movement title mismatch");

        if (original.getIdentification() != null) {
            assertNotNull(readBack.getIdentification(), "Identification should survive round-trip");
        }

        LOG.info("Metadata preserved for '{}': version='{}', title='{}'.", mxlFiles.get(0).getFileName(), readBack.getVersion(), readBack.getMovementTitle());
    }


    /**
     * Tests reading all discovered .mxl files through the MusicFileFactory facade.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testFacadeReadMxlFiles() throws IOException {
        List<Path> mxlFiles = TestResourceUtil.getInstance().findFilesByExtension(MXL_EXT);
        assertFalse(mxlFiles.isEmpty(), SKIP_MSG);

        int passed = 0;
        List<String> failures = new ArrayList<>();

        for (Path mxlPath : mxlFiles) {
            try {
                IScore score = MusicFileFactory.getInstance().read(mxlPath);
                assertNotNull(score, "Facade should return non-null score for: " + mxlPath);
                assertNotNull(score.getParts(), "Parts should not be null for: " + mxlPath);
                assertTrue(score.getParts().size() > 0, "Score should have parts: " + mxlPath);
                passed++;
            } catch (Exception e) {
                String msg = mxlPath.getFileName() + ": " + e.getMessage();
                failures.add(msg);
                LOG.info("FAILED facade read '{}': {}", mxlPath.getFileName(), e.getMessage());
            }
        }

        LOG.info("Facade read: {} passed, {} failed out of {} .mxl files.", passed, failures.size(), mxlFiles.size());
        assertTrue(failures.isEmpty(), "Failed for " + failures.size() + " file(s): " + String.join("; ", failures));
    }


    /**
     * Tests writing an MXL file through the facade and reading it back.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testFacadeWriteAndReadBackMxl() throws IOException {
        List<Path> mxlFiles = TestResourceUtil.getInstance().findFilesByExtension(MXL_EXT);
        assertFalse(mxlFiles.isEmpty(), SKIP_MSG);

        IScore original = MusicFileFactory.getInstance().read(mxlFiles.get(0));
        assertNotNull(original);

        Path tempFile = Files.createTempFile("round-trip-", MXL_EXT);
        try {
            MusicFileFactory.getInstance().write(original, tempFile);

            assertTrue(Files.size(tempFile) > 0, "Written .mxl file should not be empty");

            IScore readBack = MusicFileFactory.getInstance().read(tempFile);
            assertNotNull(readBack);
            assertEquals(original.getVersion(), readBack.getVersion(), "Version mismatch via facade");
            assertEquals(original.getMovementTitle(), readBack.getMovementTitle(), "Title mismatch via facade");

            LOG.info("Facade round-trip successful for '{}': version='{}', title='{}'.", mxlFiles.get(0).getFileName(), readBack.getVersion(), readBack.getMovementTitle());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }


    /**
     * Counts the total number of pitched notes across all parts and measures of a score.
     *
     * @param score the score to count notes in
     * @return the total number of pitched notes
     */
    private int countPitchedNotes(IScore score) {
        int count = 0;
        if (score.getParts() == null) {
            return count;
        }

        for (IPart part : score.getParts()) {
            if (part.getMeasures() == null) {
                continue;
            }
            for (IMeasure measure : part.getMeasures()) {
                if (measure.getNotes() == null) {
                    continue;
                }
                for (INote note : measure.getNotes()) {
                    if (note.getPitch() != null) {
                        count++;
                    }
                }
            }
        }

        return count;
    }
}
