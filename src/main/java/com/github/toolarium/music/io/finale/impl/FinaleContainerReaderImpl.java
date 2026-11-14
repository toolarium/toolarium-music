/*
 * FinaleContainerReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.finale.impl;

import com.github.toolarium.music.io.finale.IFinaleContainer;
import com.github.toolarium.music.io.finale.IFinaleContainerReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Implementation of {@link IFinaleContainerReader}. Reads Finale .musx (ZIP container)
 * and .mus (raw Enigma binary) files.
 *
 * @author patrick
 */
public class FinaleContainerReaderImpl implements IFinaleContainerReader {
    private static final Logger LOG = LoggerFactory.getLogger(FinaleContainerReaderImpl.class);
    private static final int BUFFER_SIZE = 8192;
    private static final long MAX_ENTRY_SIZE = 100L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 200L * 1024 * 1024;
    private static final int MAX_ENTRY_COUNT = 500;
    private static final String ENIGMA_HEADER_PREFIX = "ENIGMA BINARY FI";
    private static final String DEFAULT_ROOT_FILE = "score.dat";

    /**
     * Default constructor.
     */
    public FinaleContainerReaderImpl() {
        // default
    }

    /**
     * Reads a Finale container from the given file path.
     *
     * @param path the path to the .musx or .mus file
     * @return the parsed Finale container
     * @throws IOException if an I/O error occurs or the format is unknown
     */
    @Override
    public IFinaleContainer read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading Finale file from path: {}", path);
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            return read(inputStream);
        }
    }

    /**
     * Reads a Finale container from the given file path.
     *
     * @param inputStream the input stream to read from
     * @return the parsed Finale container
     * @throws IOException if an I/O error occurs or the format is unknown
     */
    @Override
    public IFinaleContainer read(InputStream inputStream) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading Finale file from input stream.");
        }

        BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
        bufferedStream.mark(BUFFER_SIZE);

        byte[] header = new byte[2];
        int bytesRead = bufferedStream.read(header);
        if (bytesRead < 2) {
            throw new IOException("Unable to read file header: insufficient data.");
        }

        bufferedStream.reset();

        if (header[0] == 0x50 && header[1] == 0x4B) {
            return readMusx(bufferedStream);
        }

        byte[] enigmaCheck = new byte[ENIGMA_HEADER_PREFIX.length()];
        bufferedStream.mark(BUFFER_SIZE);
        int enigmaBytesRead = bufferedStream.read(enigmaCheck);
        bufferedStream.reset();

        if (enigmaBytesRead >= ENIGMA_HEADER_PREFIX.length()) {
            String headerString = new String(enigmaCheck, StandardCharsets.US_ASCII);
            if (ENIGMA_HEADER_PREFIX.equals(headerString)) {
                return readMus(bufferedStream);
            }
        }

        throw new IOException("Unknown Finale file format");
    }

    /**
     * Reads a .musx file (ZIP container format).
     *
     * @param inputStream the input stream positioned at the beginning of the ZIP data
     * @return the parsed Finale container
     * @throws IOException if an I/O error occurs during reading
     */
    private IFinaleContainer readMusx(InputStream inputStream) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Detected MUSX (ZIP) format.");
        }

        FinaleContainer container = new FinaleContainer();
        container.setMUSX(true);

        // Buffer all non-XML entries by name so we can match the root file
        // after container.xml reveals the root file path.
        java.util.Map<String, byte[]> bufferedEntries = new java.util.LinkedHashMap<>();

        long totalSize = 0;

        try (ZipInputStream zipStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            int entryCount = 0;
            while ((entry = zipStream.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw new IOException("MUSX archive exceeds maximum entry count (" + MAX_ENTRY_COUNT + ").");
                }

                String entryName = entry.getName();

                if ("mimetype".equals(entryName)) {
                    byte[] data = readBoundedEntry(zipStream);
                    totalSize += data.length;
                    if (totalSize > MAX_TOTAL_SIZE) {
                        throw new IOException("MUSX archive exceeds maximum total decompressed size.");
                    }
                    String mimetype = new String(data, StandardCharsets.UTF_8).trim();
                    container.setMimetype(mimetype);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Read mimetype: {}", mimetype);
                    }
                } else if ("META-INF/container.xml".equals(entryName)) {
                    byte[] data = readBoundedEntry(zipStream);
                    totalSize += data.length;
                    if (totalSize > MAX_TOTAL_SIZE) {
                        throw new IOException("MUSX archive exceeds maximum total decompressed size.");
                    }
                    parseContainerXml(new ByteArrayInputStream(data), container);
                } else if ("NotationMetadata.xml".equals(entryName)) {
                    byte[] data = readBoundedEntry(zipStream);
                    totalSize += data.length;
                    if (totalSize > MAX_TOTAL_SIZE) {
                        throw new IOException("MUSX archive exceeds maximum total decompressed size.");
                    }
                    parseNotationMetadataXml(new ByteArrayInputStream(data), container);
                } else {
                    byte[] data = readBoundedEntry(zipStream);
                    totalSize += data.length;
                    if (totalSize > MAX_TOTAL_SIZE) {
                        throw new IOException("MUSX archive exceeds maximum total decompressed size.");
                    }
                    bufferedEntries.put(entryName, data);
                }

                zipStream.closeEntry();
            }
        }

        // Resolve the root file from buffered entries
        String rootFilePath = container.getRootFilePath();
        if (rootFilePath == null) {
            rootFilePath = DEFAULT_ROOT_FILE;
        }

        byte[] scoreData = bufferedEntries.get(rootFilePath);
        if (scoreData != null) {
            container.setScoreData(scoreData);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Read score data from '{}': {} bytes.", rootFilePath, scoreData.length);
            }
        }

        return container;
    }

    /**
     * Reads a .mus file (raw Enigma binary format).
     *
     * @param inputStream the input stream positioned at the beginning of the Enigma data
     * @return the parsed Finale container
     * @throws IOException if an I/O error occurs during reading
     */
    private IFinaleContainer readMus(InputStream inputStream) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Detected MUS (Enigma binary) format.");
        }

        byte[] allBytes = readAllBytes(inputStream);

        FinaleContainer container = new FinaleContainer();
        container.setMUSX(false);
        container.setScoreData(allBytes);

        // Extract the Enigma header from the first line
        String enigmaHeader = extractEnigmaHeader(allBytes);
        container.setEnigmaHeader(enigmaHeader);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Read MUS file: {} bytes, header: {}", allBytes.length, enigmaHeader);
        }

        return container;
    }

    /**
     * Parses the META-INF/container.xml and populates the container with version,
     * root file path, and root media type.
     *
     * @param inputStream the input stream of the container.xml entry
     * @param container the container to populate
     * @throws IOException if an I/O or XML parsing error occurs
     */
    private void parseContainerXml(InputStream inputStream, FinaleContainer container) throws IOException {
        try {
            byte[] xmlBytes = readEntryAsBytes(inputStream);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));

            NodeList containerNodes = doc.getElementsByTagName("container");
            if (containerNodes.getLength() > 0) {
                Element containerElement = (Element) containerNodes.item(0);
                String version = containerElement.getAttribute("version");
                container.setContainerVersion(version);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Container version: {}", version);
                }
            }

            NodeList rootfileNodes = doc.getElementsByTagName("rootfile");
            if (rootfileNodes.getLength() > 0) {
                Element rootfileElement = (Element) rootfileNodes.item(0);
                String fullPath = rootfileElement.getAttribute("full-path");
                String mediaType = rootfileElement.getAttribute("media-type");
                container.setRootFilePath(fullPath);
                container.setRootMediaType(mediaType);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Root file path: {}, media type: {}", fullPath, mediaType);
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to parse container.xml: {}", e.getMessage(), e);
            }
            throw new IOException("Failed to parse container.xml", e);
        }
    }

    /**
     * Parses the NotationMetadata.xml and populates the container with metadata.
     *
     * @param inputStream the input stream of the NotationMetadata.xml entry
     * @param container the container to populate
     * @throws IOException if an I/O or XML parsing error occurs
     */
    private void parseNotationMetadataXml(InputStream inputStream, FinaleContainer container) throws IOException {
        try {
            byte[] xmlBytes = readEntryAsBytes(inputStream);
            // Sanitize invalid UTF-8 bytes (some Finale versions write non-UTF-8 characters
            // in metadata despite declaring UTF-8 encoding)
            xmlBytes = sanitizeUtf8(xmlBytes);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));

            FinaleMetadata metadata = new FinaleMetadata();

            String keySignature = getElementText(doc, "keySignature");
            if (keySignature != null) {
                metadata.setKeySignature(keySignature);
            }

            String initialTempo = getElementText(doc, "initialTempo");
            if (initialTempo != null) {
                try {
                    metadata.setInitialTempo(Double.valueOf(initialTempo));
                } catch (NumberFormatException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Invalid initialTempo value: {}", initialTempo, e);
                    }
                }
            }

            String scoreDuration = getElementText(doc, "scoreDuration");
            if (scoreDuration != null) {
                try {
                    metadata.setScoreDuration(Double.valueOf(scoreDuration));
                } catch (NumberFormatException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Invalid scoreDuration value: {}", scoreDuration, e);
                    }
                }
            }

            String creatorString = getElementText(doc, "creatorString");
            if (creatorString != null) {
                metadata.setCreatorString(creatorString);
            }

            container.setMetadata(metadata);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Parsed NotationMetadata: {}", metadata);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to parse NotationMetadata.xml: {}", e.getMessage(), e);
            }
            throw new IOException("Failed to parse NotationMetadata.xml", e);
        }
    }

    /**
     * Sanitizes XML byte data that may contain non-UTF-8 characters despite declaring UTF-8 encoding.
     * Some Finale versions write Mac Roman or Latin-1 characters in XML metadata.
     * This method replaces the UTF-8 encoding declaration with ISO-8859-1 to prevent parsing errors.
     *
     * @param data the raw XML byte data
     * @return the sanitized XML byte data
     */
    private byte[] sanitizeUtf8(byte[] data) {
        String content = new String(data, java.nio.charset.StandardCharsets.ISO_8859_1);
        content = content.replace("encoding=\"UTF-8\"", "encoding=\"ISO-8859-1\"");
        return content.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }


    /**
     * Extracts the text content of the first element with the given tag name.
     *
     * @param doc the XML document
     * @param tagName the tag name to search for
     * @return the text content, or null if the element was not found
     */
    private String getElementText(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    /**
     * Extracts the Enigma header string from the raw byte data.
     * The header is the text before the first null byte.
     *
     * @param data the raw file data
     * @return the Enigma header string
     */
    private String extractEnigmaHeader(byte[] data) {
        int end = 0;
        while (end < data.length && data[end] != 0) {
            end++;
        }
        return new String(data, 0, end, StandardCharsets.US_ASCII);
    }

    /**
     * Reads a ZIP entry as a UTF-8 string.
     *
     * @param zipStream the ZIP input stream positioned at the entry
     * @return the entry content as a string
     * @throws IOException if an I/O error occurs
     */
    private String readEntryAsString(ZipInputStream zipStream) throws IOException {
        byte[] bytes = readEntryAsBytes(zipStream);
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }

    /**
     * Reads a ZIP entry or input stream as a byte array.
     *
     * @param inputStream the input stream to read from
     * @return the content as a byte array
     * @throws IOException if an I/O error occurs
     */
    private byte[] readEntryAsBytes(InputStream inputStream) throws IOException {
        return readBoundedEntry(inputStream);
    }

    /**
     * Reads a ZIP entry with a size limit to protect against ZIP bombs.
     *
     * @param inputStream the input stream to read from
     * @return the content as a byte array
     * @throws IOException if an I/O error occurs or the entry exceeds the size limit
     */
    private byte[] readBoundedEntry(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            total += len;
            if (total > MAX_ENTRY_SIZE) {
                throw new IOException("ZIP entry exceeds maximum allowed size (" + MAX_ENTRY_SIZE + " bytes).");
            }
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    /**
     * Reads all bytes from the given input stream.
     *
     * @param inputStream the input stream to read from
     * @return all bytes from the stream
     * @throws IOException if an I/O error occurs
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            total += len;
            if (total > MAX_ENTRY_SIZE) {
                throw new IOException("Input exceeds maximum allowed size (" + MAX_ENTRY_SIZE + " bytes).");
            }
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }
}
