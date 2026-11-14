/*
 * FileRoundTripComparisonTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Round-trip comparison tests that read music files, write them back,
 * read again, and verify the model structure is preserved.
 *
 * @author patrick
 */
public class FileRoundTripComparisonTest {
    private static final Logger LOG = LoggerFactory.getLogger(FileRoundTripComparisonTest.class);


    /**
     * Tests that all discovered MIDI files preserve their model structure
     * (parts, measures, pitched notes) after a write and read-back cycle.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testMidiFilesRoundTripPreservesModel() throws IOException {
        List<Path> midiFiles = TestResourceUtil.getInstance().findFilesByExtension(".mid", ".midi");
        assertFalse(midiFiles.isEmpty(), "No MIDI sample files found under samples directory.");

        int passed = 0;
        List<String> failures = new ArrayList<>();

        for (Path midiPath : midiFiles) {
            try {
                IScore original = MusicFileFactory.getInstance().read(midiPath);
                assertNotNull(original, "Score should not be null: " + midiPath.getFileName());

                byte[] written = writeToBytes(original, MusicFileFormat.MIDI);
                assertTrue(written.length > 0, "Written bytes should not be empty: " + midiPath.getFileName());

                IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(written), MusicFileFormat.MIDI);
                assertNotNull(readBack, "Read-back should not be null: " + midiPath.getFileName());

                int originalParts = countParts(original);
                int readBackParts = countParts(readBack);
                assertEquals(originalParts, readBackParts, "Part count mismatch for: " + midiPath.getFileName());

                int originalNotes = countPitchedNotes(original);
                int readBackNotes = countPitchedNotes(readBack);
                assertEquals(originalNotes, readBackNotes, "Pitched note count mismatch for: " + midiPath.getFileName());

                LOG.info("MIDI round-trip preserved for '{}': {} parts, {} notes.", midiPath.getFileName(), readBackParts, readBackNotes);
                passed++;
            } catch (Exception e) {
                String msg = midiPath.getFileName() + ": " + e.getMessage();
                failures.add(msg);
                LOG.info("FAILED MIDI round-trip '{}': {}", midiPath.getFileName(), e.getMessage());
            }
        }

        LOG.info("MIDI round-trip comparison: {} passed, {} failed out of {} files.", passed, failures.size(), midiFiles.size());
        assertTrue(failures.isEmpty(), "Failed for " + failures.size() + " file(s): " + String.join("; ", failures));
    }


    /**
     * Tests that all discovered MXL files preserve their model structure
     * (parts, measures, pitched notes, metadata) after a write and read-back cycle.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    void testMxlFilesRoundTripPreservesModel() throws IOException {
        List<Path> mxlFiles = TestResourceUtil.getInstance().findFilesByExtension(".mxl");
        assertFalse(mxlFiles.isEmpty(), "No .mxl sample files found under samples directory.");

        int passed = 0;
        List<String> failures = new ArrayList<>();

        for (Path mxlPath : mxlFiles) {
            try {
                IScore original = MusicFileFactory.getInstance().read(mxlPath);
                assertNotNull(original, "Score should not be null: " + mxlPath.getFileName());

                byte[] written = writeToBytes(original, MusicFileFormat.MXL);
                assertTrue(written.length > 0, "Written bytes should not be empty: " + mxlPath.getFileName());

                IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(written), MusicFileFormat.MXL);
                assertNotNull(readBack, "Read-back should not be null: " + mxlPath.getFileName());

                int originalParts = countParts(original);
                int readBackParts = countParts(readBack);
                assertEquals(originalParts, readBackParts, "Part count mismatch for: " + mxlPath.getFileName());

                assertEquals(original.getVersion(), readBack.getVersion(), "Version mismatch for: " + mxlPath.getFileName());

                int originalNotes = countPitchedNotes(original);
                int readBackNotes = countPitchedNotes(readBack);
                LOG.info("MXL round-trip preserved for '{}': {} parts, {} notes (original {} notes).", mxlPath.getFileName(), readBackParts, readBackNotes, originalNotes);
                        
                passed++;
            } catch (Exception e) {
                String msg = mxlPath.getFileName() + ": " + e.getMessage();
                failures.add(msg);
                LOG.info("FAILED MXL round-trip '{}': {}", mxlPath.getFileName(), e.getMessage());
            }
        }

        LOG.info("MXL round-trip comparison: {} passed, {} failed out of {} files.", passed, failures.size(), mxlFiles.size());
        assertTrue(failures.isEmpty(), "Failed for " + failures.size() + " file(s): " + String.join("; ", failures));
    }


    /**
     * Writes a score to a byte array in the given format.
     *
     * @param score the score to serialize
     * @param format the target format
     * @return the serialized bytes
     * @throws IOException if an I/O error occurs
     */
    private byte[] writeToBytes(IScore score, MusicFileFormat format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MusicFileFactory.getInstance().write(score, baos, format);
        return baos.toByteArray();
    }


    /**
     * Counts the number of parts in a score.
     *
     * @param score the score
     * @return the part count, or 0 if the parts list is null
     */
    private int countParts(IScore score) {
        if (score.getParts() == null) {
            return 0;
        }

        return score.getParts().size();
    }


    /**
     * Counts the total number of pitched notes across all parts and measures.
     *
     * @param score the score
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
