/*
 * MXLReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.mxl.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.musicxml.impl.MusicXMLReaderImpl;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Implementation of {@link IMusicReader} for reading compressed MusicXML (.mxl) files.
 *
 * @author patrick
 */
public class MXLReaderImpl implements IMusicReader {

    private static final Logger LOG = LoggerFactory.getLogger(MXLReaderImpl.class);
    private static final long MAX_ENTRY_SIZE = 100L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 200L * 1024 * 1024;
    private static final int MAX_ENTRY_COUNT = 500;
    private final IMusicReader musicXmlReader = new MusicXMLReaderImpl();


    /**
     * Constructor for MXLReaderImpl.
     */
    public MXLReaderImpl() {
        // default constructor
    }


    /**
     * Read a score from an MXL file at the given path.
     *
     * @param path the path to the MXL file
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading score from MXL path: {}", path);
        }

        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


    /**
     * Read a score from an MXL input stream.
     * Optimized to stop buffering entries once the root file path is known from container.xml,
     * and to skip reading non-matching entries entirely.
     *
     * @param inputStream the input stream containing the MXL data
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        String rootPath = null;
        Map<String, byte[]> bufferedEntries = new LinkedHashMap<>();
        long totalSize = 0;
        int entryCount = 0;

        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw new IOException("MXL archive exceeds maximum entry count (" + MAX_ENTRY_COUNT + ").");
                }

                String name = entry.getName();

                if ("META-INF/container.xml".equals(name)) {
                    rootPath = parseContainerXml(readBounded(zis));
                    if (rootPath != null && bufferedEntries.containsKey(rootPath)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Found root MusicXML file in MXL archive: {}", rootPath);
                        }
                        return musicXmlReader.read(new ByteArrayInputStream(bufferedEntries.get(rootPath)));
                    }
                    zis.closeEntry();
                    continue;
                }

                if (rootPath != null && !name.equals(rootPath)) {
                    zis.closeEntry();
                    continue;
                }

                byte[] data = readBounded(zis);
                totalSize += data.length;
                if (totalSize > MAX_TOTAL_SIZE) {
                    throw new IOException("MXL archive exceeds maximum total decompressed size.");
                }

                if (rootPath != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found root MusicXML file in MXL archive: {}", rootPath);
                    }
                    return musicXmlReader.read(new ByteArrayInputStream(data));
                }

                bufferedEntries.put(name, data);
                zis.closeEntry();
            }
        }

        if (rootPath == null) {
            for (String name : bufferedEntries.keySet()) {
                if (name.endsWith(".musicxml") || (name.endsWith(".xml") && !"META-INF/container.xml".equals(name))) {
                    rootPath = name;
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No container.xml found, falling back to entry: {}", name);
                    }
                    break;
                }
            }
        }

        if (rootPath == null) {
            throw new IOException("No root MusicXML file found in MXL archive.");
        }

        byte[] musicXmlData = bufferedEntries.get(rootPath);
        if (musicXmlData == null) {
            throw new IOException("Root MusicXML file not found in MXL archive: " + rootPath);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Found root MusicXML file in MXL archive: {}", rootPath);
        }

        return musicXmlReader.read(new ByteArrayInputStream(musicXmlData));
    }


    /**
     * Parse the container.xml content to extract the root file path.
     *
     * @param containerData the raw bytes of container.xml
     * @return the full-path attribute of the rootfile element, or null if not found
     * @throws IOException in case of parse errors
     */
    private String parseContainerXml(byte[] containerData) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(containerData));
            NodeList rootFiles = document.getElementsByTagName("rootfile");

            if (rootFiles.getLength() > 0) {
                Element rootFile = (Element) rootFiles.item(0);
                return rootFile.getAttribute("full-path");
            }

            return null;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to parse container.xml: {}", e.getMessage(), e);
            }

            throw new IOException("Failed to parse container.xml in MXL archive", e);
        }
    }


    /**
     * Read all bytes from a ZipInputStream without closing it, enforcing a size limit.
     *
     * @param zis the zip input stream to read from
     * @return the bytes read from the current entry
     * @throws IOException in case of read errors or if the entry exceeds the size limit
     */
    private byte[] readBounded(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        long total = 0;
        int bytesRead;
        while ((bytesRead = zis.read(buffer)) != -1) {
            total += bytesRead;
            if (total > MAX_ENTRY_SIZE) {
                throw new IOException("ZIP entry exceeds maximum allowed size (" + MAX_ENTRY_SIZE + " bytes).");
            }
            baos.write(buffer, 0, bytesRead);
        }

        return baos.toByteArray();
    }
}
