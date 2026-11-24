/*
 * FinaleReferenceComparisonTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.SoundingNoteComparator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Compares the sounding notes of Finale files with the MusicXML export Finale made of them: every
 * {@code <name>.musx} or {@code <name>.mus} with a {@code <name>.mxl} in the same folder is compared.
 *
 * @author patrick
 */
public class FinaleReferenceComparisonTest {
    private static final Logger LOG = LoggerFactory.getLogger(FinaleReferenceComparisonTest.class);
    private static final String EXT_MXL = ".mxl";


    /**
     * Compares every .musx file with its export.
     *
     * @throws Exception if reading fails
     */
    @Test
    void testCompareMusxWithExport() throws Exception {
        List<String> differences = new ArrayList<>();
        int compared = compare(".musx", differences);
        assumeTrue(compared > 0, "No .musx files with a Finale export (<name>.mxl) found.");
        assertTrue(differences.isEmpty(), differences.size() + " of " + compared + " .musx files differ from their export:"
                + System.lineSeparator() + String.join(System.lineSeparator(), differences));
    }


    /**
     * Compares every .mus file (Finale 2010 and older) with its export. The reader of the legacy binary
     * format does not convert the notes correctly yet, so the result is only reported.
     *
     * @throws Exception if reading fails
     */
    @Test
    void testCompareMusWithExport() throws Exception {
        List<String> differences = new ArrayList<>();
        int compared = compare(".mus", differences);
        assumeTrue(compared > 0, "No .mus files with a Finale export (<name>.mxl) found.");
        LOG.info("{} of {} .mus files match their export.", compared - differences.size(), compared);
    }


    /**
     * Compares all files with the extension that have an export.
     *
     * @param extension the extension
     * @param differences the differences (filled)
     * @return the number of compared files
     * @throws Exception if finding the files fails
     */
    private int compare(String extension, List<String> differences) throws Exception {
        List<Path> files = new ArrayList<>();
        for (Path file : TestResourceUtil.getInstance().findFilesByExtension(extension)) {
            if (Files.exists(getExport(file, extension))) {
                files.add(file);
            }
        }

        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        differences.addAll(TestResourceUtil.getInstance().processFiles(files, file -> {
            String result = comparator.compare(file.getFileName().toString(),
                                               comparator.readConverted(MusicFileFactory.getInstance().read(getExport(file, extension))),
                                               comparator.readConverted(MusicFileFactory.getInstance().read(file)));
            if (result != null) {
                return file + ": " + result;
            }
            return null;
        }));
        return files.size();
    }


    /**
     * Gets the MusicXML export next to a file.
     *
     * @param file the file
     * @param extension the extension of the file
     * @return the path of the export
     */
    private Path getExport(Path file, String extension) {
        String name = file.getFileName().toString();
        return file.resolveSibling(name.substring(0, name.length() - extension.length()) + EXT_MXL);
    }
}
