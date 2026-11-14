/*
 * TestResourceUtil.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music;

import com.github.toolarium.common.util.FileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Singleton utility for discovering test resource files.
 * Centralizes the test resources path constant and provides
 * helper methods for finding files by extension.
 *
 * @author patrick
 */
public final class TestResourceUtil {
    /** The path to the test resources directory. */
    public static final String RESOURCES_PATH = "src/test/resources";

    /** The subdirectory within test resources that contains sample files. */
    public static final String SAMPLES_SUBDIR = "samples";

    private static final Logger LOG = LoggerFactory.getLogger(TestResourceUtil.class);


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final TestResourceUtil INSTANCE = new TestResourceUtil();
    }


    /**
     * Constructor.
     */
    private TestResourceUtil() {
        // NOP
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static TestResourceUtil getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Get the test resources directory as a {@link Path}.
     *
     * @return the resources directory path
     */
    public Path getResourcesDir() {
        return Paths.get(RESOURCES_PATH);
    }


    /**
     * Get the samples subdirectory as a {@link Path}.
     *
     * @return the samples directory path
     */
    public Path getSamplesDir() {
        return getResourcesDir().resolve(SAMPLES_SUBDIR);
    }


    /**
     * Check whether the test resources directory exists.
     *
     * @return true if the directory exists, false otherwise
     */
    public boolean resourcesDirExists() {
        return FileUtil.getInstance().existFile(RESOURCES_PATH);
    }


    /**
     * Find the first file in the samples directory matching any of the given extensions.
     *
     * @param extensions the file extensions to match (including the leading dot)
     * @return the first matching file, or null if none found
     * @throws IOException if an I/O error occurs during directory traversal
     */
    public Path findFirstFileByExtension(String... extensions) throws IOException {
        Path samplesDir = getSamplesDir();
        if (!Files.isDirectory(samplesDir)) {
            return null;
        }

        try (Stream<Path> walk = Files.walk(samplesDir)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> matchesExtension(p, extensions))
                    .findFirst()
                    .orElse(null);
        }
    }


    /**
     * Discover files under the samples directory matching any of the given extensions.
     * The extensions are matched case-insensitively and must include the leading dot (e.g. ".mid").
     *
     * @param extensions the file extensions to match
     * @return the list of matching file paths, or an empty list if the directory does not exist or no files match
     * @throws IOException if an I/O error occurs during directory traversal
     */
    public List<Path> findFilesByExtension(String... extensions) throws IOException {
        Path samplesDir = getSamplesDir();
        if (!Files.isDirectory(samplesDir)) {
            LOG.info("Samples directory '{}' not found.", samplesDir);
            return Collections.emptyList();
        }

        try (Stream<Path> walk = Files.walk(samplesDir)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> matchesExtension(p, extensions))
                    .collect(Collectors.toList());
        }
    }


    /**
     * Check whether a path matches any of the given extensions (case-insensitive).
     *
     * @param path the file path to check
     * @param extensions the extensions to match against
     * @return true if the file name ends with one of the extensions
     */
    private boolean matchesExtension(Path path, String... extensions) {
        String name = path.getFileName().toString().toLowerCase();
        for (String ext : extensions) {
            if (name.endsWith(ext.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}
