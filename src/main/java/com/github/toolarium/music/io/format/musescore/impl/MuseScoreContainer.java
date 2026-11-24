/*
 * MuseScoreContainer.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import com.github.toolarium.music.io.format.musescore.impl.dto.MuseScoreFiles;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Reads and writes the MuseScore container (.mscz): a ZIP archive with
 * {@code META-INF/container.xml} that names the score file (.mscx).
 *
 * @author patrick
 */
public final class MuseScoreContainer {
    private static final Logger LOG = LoggerFactory.getLogger(MuseScoreContainer.class);
    private static final int BUFFER_SIZE = 8192;
    private static final long MAX_ENTRY_SIZE = 64L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 200L * 1024 * 1024;
    private static final int MAX_ENTRY_COUNT = 2000;
    private static final String CONTAINER_ENTRY = "META-INF/container.xml";
    private static final String STYLE_ENTRY = "score_style.mss";
    private static final String MSCX_EXTENSION = ".mscx";
    private static final String MSC_EXTENSION = ".msc";
    private static final Pattern ROOT_FILE = Pattern.compile("<rootfile\\s+full-path=\"([^\"]+)\"");


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MuseScoreContainer INSTANCE = new MuseScoreContainer();
    }


    /**
     * Constructor.
     */
    private MuseScoreContainer() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MuseScoreContainer getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Reads the score file (.mscx content) from a MuseScore container.
     *
     * @param inputStream the .mscz content
     * @return the content of the score file
     * @throws IOException if the archive is invalid or contains no score
     */
    public byte[] readScore(InputStream inputStream) throws IOException {
        return readFiles(inputStream).getScore();
    }


    /**
     * Reads the score file (.mscx content) and the style file (score_style.mss, MuseScore 4, with the page style)
     * from a MuseScore container in one pass.
     *
     * @param inputStream the .mscz content
     * @return the files: the score and the style (null if the container has none)
     * @throws IOException if the archive is invalid or contains no score
     */
    public MuseScoreFiles readFiles(InputStream inputStream) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        String firstScore = null;
        long total = 0;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                count++;
                if (count > MAX_ENTRY_COUNT) {
                    throw new IOException("MuseScore archive contains too many entries.");
                }
                String name = entry.getName();
                boolean wanted = !entry.isDirectory() && (CONTAINER_ENTRY.equals(name) || STYLE_ENTRY.equals(name) || isScoreFile(name));
                ByteArrayOutputStream content = null;
                if (wanted) {
                    content = new ByteArrayOutputStream();
                }
                total += readBounded(zip, content);
                if (total > MAX_TOTAL_SIZE) {
                    throw new IOException("MuseScore archive exceeds maximum total size.");
                }
                if (wanted) {
                    entries.put(name, content.toByteArray());
                    if (firstScore == null && isScoreFile(name) && !name.contains("/")) {
                        firstScore = name;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            // a damaged entry (for example a broken file name) is reported by the ZIP stream as illegal argument
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid ZIP archive: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid ZIP archive: " + e.getMessage(), e);
        }

        String rootFile = null;
        byte[] container = entries.get(CONTAINER_ENTRY);
        if (container != null) {
            Matcher matcher = ROOT_FILE.matcher(new String(container, StandardCharsets.UTF_8));
            while (matcher.find() && rootFile == null) {
                if (isScoreFile(matcher.group(1))) {
                    rootFile = matcher.group(1);
                }
            }
        }
        if (rootFile == null || !entries.containsKey(rootFile)) {
            rootFile = firstScore;
        }
        if (rootFile == null) {
            for (String name : entries.keySet()) {
                if (isScoreFile(name)) {
                    rootFile = name;
                }
            }
        }
        if (rootFile == null) {
            throw new IOException("MuseScore archive contains no score file (.mscx).");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading MuseScore score file {} from archive.", rootFile);
        }
        return new MuseScoreFiles(entries.get(rootFile), entries.get(STYLE_ENTRY));
    }


    /**
     * Checks whether an archive entry is a score file (.mscx, or .msc of MuseScore 0.9).
     *
     * @param name the entry name
     * @return true for a score file
     */
    private boolean isScoreFile(String name) {
        return name.endsWith(MSCX_EXTENSION) || name.endsWith(MSC_EXTENSION);
    }


    /**
     * Writes a MuseScore container with one score file.
     *
     * @param scoreName the name of the score file, for example score.mscx
     * @param mscx the content of the score file
     * @param outputStream the target (not closed)
     * @throws IOException if writing fails
     */
    public void writeScore(String scoreName, byte[] mscx, OutputStream outputStream) throws IOException {
        String container = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<container>\n"
                + "  <rootfiles>\n"
                + "    <rootfile full-path=\"" + scoreName + "\"/>\n"
                + "    </rootfiles>\n"
                + "  </container>\n";
        ZipOutputStream zip = new ZipOutputStream(new NonClosingOutputStream(outputStream));
        zip.putNextEntry(new ZipEntry(CONTAINER_ENTRY));
        zip.write(container.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        zip.putNextEntry(new ZipEntry(scoreName));
        zip.write(mscx);
        zip.closeEntry();
        zip.finish();
        zip.close();
    }


    /**
     * Reads a stream with a size limit.
     *
     * @param inputStream the stream
     * @param output the target, or null to skip the content
     * @return the number of bytes read
     * @throws IOException if reading fails or the limit is exceeded
     */
    public long readBounded(InputStream inputStream, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            total += len;
            if (total > MAX_ENTRY_SIZE) {
                throw new IOException("MuseScore file exceeds maximum size.");
            }
            if (output != null) {
                output.write(buffer, 0, len);
            }
        }
        return total;
    }


    /**
     * Reads a whole stream with a size limit.
     *
     * @param inputStream the stream
     * @return the content
     * @throws IOException if reading fails or the limit is exceeded
     */
    public byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        readBounded(inputStream, output);
        return output.toByteArray();
    }


    /**
     * Wraps a stream so that closing the ZIP stream does not close the caller's stream.
     *
     * @author patrick
     */
    private static final class NonClosingOutputStream extends OutputStream {
        private final OutputStream delegate;


        /**
         * Constructor for NonClosingOutputStream.
         *
         * @param delegate the wrapped stream
         */
        NonClosingOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }


        /**
         * Writes a byte.
         *
         * @param b the byte
         * @throws IOException if writing fails
         */
        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }


        /**
         * Writes bytes.
         *
         * @param b the bytes
         * @param off the offset
         * @param len the length
         * @throws IOException if writing fails
         */
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }


        /**
         * Flushes the wrapped stream.
         *
         * @throws IOException if flushing fails
         */
        @Override
        public void flush() throws IOException {
            delegate.flush();
        }


        /**
         * Flushes but does not close the wrapped stream.
         *
         * @throws IOException if flushing fails
         */
        @Override
        public void close() throws IOException {
            delegate.flush();
        }
    }


    /**
     * Creates an input stream for content.
     *
     * @param content the content
     * @return the stream
     */
    public InputStream stream(byte[] content) {
        return new ByteArrayInputStream(content);
    }
}
