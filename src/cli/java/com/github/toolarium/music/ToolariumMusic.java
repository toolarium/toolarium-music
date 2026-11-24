/*
 * ToolariumMusic.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music;

import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command-line entry point for toolarium-music. Reads one or more music files or directories and converts them to a target format.
 *
 * <p>Usage examples:
 * <pre>
 *   toolarium-music song.sib
 *   toolarium-music -f mid -o /tmp/out song.mxl piece.musx
 *   toolarium-music -f musicxml scores/
 * </pre>
 *
 * @author patrick
 */
@Command(
    name = "toolarium-music",
    mixinStandardHelpOptions = true,
    versionProvider = ToolariumMusic.VersionProvider.class,
    description = "Convert music files between supported formats."
)
public class ToolariumMusic implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(ToolariumMusic.class);

    @Parameters(arity = "1..*", paramLabel = "FILE|DIR", description = "One or more input files or directories to convert.")
    private List<Path> inputs;

    @Option(names = {"-f", "--format"}, paramLabel = "FORMAT", defaultValue = "mxl",
            description = "Output format (default: mxl). Supported write formats: mxl, musicxml, mid, mscz, mscx, mei, abc, ly.")
    private String format;

    @Option(names = {"-o", "--output"}, paramLabel = "DIR", description = "Output directory. Defaults to the current working directory.")
    private Path outputDir;


    /**
     * Main entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ToolariumMusic()).execute(args);
        System.exit(exitCode); //CHECKSTYLE IGNORE THIS LINE
    }


    /**
     * Executes the conversion of all collected input files in parallel.
     *
     * @return 0 if all conversions succeeded, 1 if any failed, 2 on usage error
     * @throws Exception on unexpected errors
     */
    @Override
    public Integer call() throws Exception {
        MusicFileFormat outputFormat = resolveOutputFormat(format);
        if (outputFormat == null) {
            print(true, "Unknown or write-unsupported format: " + format + ". Supported write formats: mxl, musicxml, mid, mscz, mscx, mei, abc, ly.");
            return 2;
        }

        Path targetDir = resolveOutputDir();
        Files.createDirectories(targetDir);

        List<Path> files = collectFiles(inputs);
        if (files.isEmpty()) {
            print(true, "No supported music files found in the given inputs.");
            return 2;
        }

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<ConversionResult>> futures = new ArrayList<>();

        for (Path inputFile : files) {
            futures.add(executor.submit(() -> convert(inputFile, outputFormat, targetDir)));
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        return reportResults(futures);
    }


    /**
     * Writes a message to standard output or standard error.
     *
     * @param error true to write to standard error, false to write to standard output
     * @param message the message to print
     */
    private static void print(boolean error, String message) {
        if (error) {
            System.err.println(message); //CHECKSTYLE IGNORE THIS LINE
        } else {
            System.out.println(message); //CHECKSTYLE IGNORE THIS LINE
        }
    }


    /**
     * Resolves the output directory, defaulting to the current working directory.
     *
     * @return the output directory path
     */
    private Path resolveOutputDir() {
        if (outputDir != null) {
            return outputDir;
        }
        return Paths.get(".");
    }


    /**
     * Resolves the output format from an extension string such as {@code mxl} or {@code mid}.
     * Returns null if the extension is not recognized or the format does not support writing.
     *
     * @param ext the extension string (without leading dot)
     * @return the matching writable format, or null
     */
    private MusicFileFormat resolveOutputFormat(String ext) {
        try {
            MusicFileFormat fmt = MusicFileFormat.fromFileName("x." + ext.toLowerCase());
            if (isWritable(fmt)) {
                return fmt;
            }
            return null;
        } catch (IllegalArgumentException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unknown format extension: {}", ext, e);
            }
            return null;
        }
    }


    /**
     * Returns whether the given format supports writing.
     *
     * @param fmt the format to check
     * @return true if writing is supported
     */
    private boolean isWritable(MusicFileFormat fmt) {
        return fmt == MusicFileFormat.MXL
            || fmt == MusicFileFormat.MUSIC_XML
            || fmt == MusicFileFormat.MIDI
            || fmt == MusicFileFormat.MUSESCORE
            || fmt == MusicFileFormat.MUSESCORE_XML
            || fmt == MusicFileFormat.MEI
            || fmt == MusicFileFormat.LILYPOND
            || fmt == MusicFileFormat.ABC;
    }


    /**
     * Collects all readable music files from the given paths.
     * Directory paths are expanded recursively in sorted order.
     *
     * @param paths the input paths (files or directories)
     * @return the flat list of readable music files
     */
    private List<Path> collectFiles(List<Path> paths) {
        List<Path> result = new ArrayList<>();
        for (Path p : paths) {
            if (Files.isDirectory(p)) {
                try (Stream<Path> walk = Files.walk(p)) {
                    walk.filter(Files::isRegularFile)
                        .filter(this::isReadable)
                        .sorted()
                        .forEach(result::add);
                } catch (IOException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Cannot walk directory: {}", p, e);
                    }
                    print(true, "Cannot read directory: " + p + ": " + e.getMessage());
                }
            } else if (Files.isRegularFile(p)) {
                if (isReadable(p)) {
                    result.add(p);
                } else {
                    print(true, "Unsupported file format: " + p);
                }
            } else {
                print(true, "Not a file or directory: " + p);
            }
        }
        return result;
    }


    /**
     * Returns whether the given path has a recognized music format extension.
     *
     * @param path the path to check
     * @return true if the extension is recognized by {@link MusicFileFormat}
     */
    private boolean isReadable(Path path) {
        try {
            MusicFileFormat.fromFileName(path.getFileName().toString());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    /**
     * Converts a single input file to the target format and writes it to the target directory.
     *
     * @param inputFile the file to convert
     * @param outputFormat the target format
     * @param targetDir the output directory
     * @return the result of the conversion attempt
     */
    private ConversionResult convert(Path inputFile, MusicFileFormat outputFormat, Path targetDir) {
        Path outputFile = buildOutputPath(inputFile, outputFormat, targetDir);
        try {
            IScore score = MusicFileFactory.getInstance().read(inputFile);
            MusicFileFactory.getInstance().write(score, outputFile, outputFormat);
            return new ConversionResult(inputFile, outputFile, null);
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to convert: {} -> {}", inputFile, outputFile, e);
            }
            return new ConversionResult(inputFile, outputFile, e);
        }
    }


    /**
     * Builds the output file path by replacing the input file extension with the target format extension.
     *
     * @param inputFile the input file
     * @param outputFormat the target format
     * @param targetDir the output directory
     * @return the output file path
     */
    private Path buildOutputPath(Path inputFile, MusicFileFormat outputFormat, Path targetDir) {
        String fileName = inputFile.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName;
        if (dot > 0) {
            baseName = fileName.substring(0, dot);
        } else {
            baseName = fileName;
        }
        return targetDir.resolve(baseName + "." + outputFormat.getExtension());
    }


    /**
     * Collects all futures, prints per-file OK/FAIL lines, and returns the exit code.
     *
     * @param futures the list of conversion futures
     * @return 0 if all succeeded, 1 if any failed
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private int reportResults(List<Future<ConversionResult>> futures) throws InterruptedException {
        int success = 0;
        int failure = 0;
        for (Future<ConversionResult> future : futures) {
            try {
                ConversionResult result = future.get();
                if (result.getError() == null) {
                    print(false, "OK   " + result.getInput() + " -> " + result.getOutput());
                    success++;
                } else {
                    print(true, "FAIL " + result.getInput() + ": " + result.getError().getMessage());
                    failure++;
                }
            } catch (ExecutionException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unexpected error during conversion", e);
                }
                print(true, "FAIL (unexpected): " + e.getCause().getMessage());
                failure++;
            }
        }
        print(false, success + " converted, " + failure + " failed.");
        if (failure > 0) {
            return 1;
        }
        return 0;
    }


    /**
     * Holds the result of a single file conversion attempt.
     *
     * @author patrick
     */
    private static final class ConversionResult {

        private final Path input;
        private final Path output;
        private final IOException error;


        /**
         * Constructor.
         *
         * @param input the input file path
         * @param output the output file path
         * @param error the error that occurred, or null on success
         */
        ConversionResult(Path input, Path output, IOException error) {
            this.input = input;
            this.output = output;
            this.error = error;
        }


        /**
         * Gets the input file path.
         *
         * @return the input path
         */
        Path getInput() {
            return input;
        }


        /**
         * Gets the output file path.
         *
         * @return the output path
         */
        Path getOutput() {
            return output;
        }


        /**
         * Gets the error that occurred during conversion, or null if conversion succeeded.
         *
         * @return the error, or null
         */
        IOException getError() {
            return error;
        }
    }


    /**
     * Provides the version string for the {@code --version} flag by reading
     * the implementation version from the package manifest.
     *
     * @author patrick
     */
    static final class VersionProvider implements CommandLine.IVersionProvider {

        /**
         * Returns the version string array for picocli.
         *
         * @return an array containing the version line
         * @throws Exception never thrown
         */
        @Override
        public String[] getVersion() throws Exception {
            String version = ToolariumMusic.class.getPackage().getImplementationVersion();
            if (version == null) {
                version = "unknown";
            }
            return new String[]{"toolarium-music " + version};
        }
    }
}
