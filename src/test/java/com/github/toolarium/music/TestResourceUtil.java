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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Singleton utility for discovering test resource files.
 * Centralizes the test resources path constant and provides
 * helper methods for finding files by extension and for processing many sample files in parallel.
 * The samples in the protected folder are only used by the integration tests (system property
 * {@value #INTEGRATION_TEST_PROPERTY} set to true by the Gradle task integrationTest).
 *
 * @author patrick
 */
public final class TestResourceUtil {
    /** The path to the test resources directory. */
    public static final String RESOURCES_PATH = "src/test/resources";

    /** The subdirectory within test resources that contains sample files. */
    public static final String SAMPLES_SUBDIR = "samples";

    /** The subdirectory within the samples with the protected samples, which only the integration tests use. */
    public static final String PROTECTED_SUBDIR = "_protected_";

    /** The system property which is true when the integration tests run. */
    public static final String INTEGRATION_TEST_PROPERTY = "toolarium.music.integrationTest";

    /** The system property with the number of threads for the sample files (default 1.5 threads per processor). */
    public static final String THREADS_PROPERTY = "toolarium.music.testThreads";

    private static final Logger LOG = LoggerFactory.getLogger(TestResourceUtil.class);
    private static final int THREADS_PER_TWO_PROCESSORS = 3;


    /**
     * Processes one sample file.
     *
     * @author patrick
     */
    @FunctionalInterface
    public interface SampleFileProcessor {
        /**
         * Processes a sample file.
         *
         * @param file the file
         * @return the failure message, or null if the file is fine
         * @throws Exception if processing the file fails, which counts as a failure of the file
         */
        String process(Path file) throws Exception;
    }


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
                    .filter(this::isIncluded)
                    .filter(p -> matchesExtension(p, extensions))
                    .findFirst()
                    .orElse(null);
        }
    }


    /**
     * Checks whether the integration tests run, which also use the protected samples.
     *
     * @return true for the integration tests
     */
    public boolean isIntegrationTest() {
        return Boolean.getBoolean(INTEGRATION_TEST_PROPERTY);
    }


    /**
     * Gets the number of threads for the sample files: the system property {@value #THREADS_PROPERTY}, otherwise
     * 1.5 threads per processor (reading a sample also waits for the disk and the memory, so a few threads more
     * than processors are faster).
     *
     * @return the number of threads, at least 1
     */
    public int getThreads() {
        String value = System.getProperty(THREADS_PROPERTY);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Math.max(1, Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invalid number of test threads '{}', the default is used.", value, e);
                }
                LOG.warn("Invalid number of test threads '{}', the default is used.", value);
            }
        }
        return Math.max(1, Runtime.getRuntime().availableProcessors() * THREADS_PER_TWO_PROCESSORS / 2);
    }


    /**
     * Checks that samples were found: a test without samples is skipped (for example in the unit tests, when the
     * samples of a type are all in the protected folder).
     *
     * @param found true if samples were found
     * @param message the message if no samples were found
     */
    public void assumeSamples(boolean found, String message) {
        Assumptions.assumeTrue(found, message);
    }


    /**
     * Processes sample files in parallel, one task per file on a thread pool (see {@link #getThreads()}).
     *
     * @param files the files
     * @param processor the processor
     * @return the failure messages in the order of the files, empty if all files are fine
     * @throws IllegalStateException if the processing is interrupted or a task fails unexpectedly
     */
    public List<String> processFiles(List<Path> files, SampleFileProcessor processor) {
        ExecutorService executor = Executors.newFixedThreadPool(getThreads());
        try {
            List<Future<String>> results = new ArrayList<>();
            for (Path file : files) {
                results.add(executor.submit(() -> process(file, processor)));
            }

            List<String> failures = new ArrayList<>();
            for (Future<String> result : results) {
                String failure = result.get();
                if (failure != null) {
                    failures.add(failure);
                }
            }
            return failures;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processing the sample files was interrupted.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Processing the sample files failed: " + e.getCause(), e.getCause());
        } finally {
            executor.shutdownNow();
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
                    .filter(this::isIncluded)
                    .filter(p -> matchesExtension(p, extensions))
                    .collect(Collectors.toList());
        }
    }


    /**
     * Processes one sample file: an exception or an error of the processor becomes the failure message of the file.
     *
     * @param file the file
     * @param processor the processor
     * @return the failure message, or null if the file is fine
     */
    private String process(Path file, SampleFileProcessor processor) {
        try {
            return processor.process(file);
        } catch (Exception | AssertionError e) {
            return file + ": " + e.getMessage();
        }
    }


    /**
     * Checks whether a sample file is used: the protected samples only by the integration tests.
     *
     * @param path the file path
     * @return true if the file is used
     */
    private boolean isIncluded(Path path) {
        if (isIntegrationTest()) {
            return true;
        }
        for (Path part : path) {
            if (PROTECTED_SUBDIR.equals(part.toString())) {
                return false;
            }
        }
        return true;
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
