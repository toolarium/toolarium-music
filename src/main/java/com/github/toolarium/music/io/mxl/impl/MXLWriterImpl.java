/*
 * MXLWriterImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.mxl.impl;

import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.io.musicxml.impl.MusicXMLWriterImpl;
import com.github.toolarium.music.model.score.IScore;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link IMusicWriter} for writing compressed MusicXML (.mxl) files.
 *
 * @author patrick
 */
public class MXLWriterImpl implements IMusicWriter {

    private static final Logger LOG = LoggerFactory.getLogger(MXLWriterImpl.class);
    private static final String ROOT_FILE_NAME = "score.musicxml";
    private static final String MIMETYPE_CONTENT = "application/vnd.recordare.musicxml+xml";
    private final IMusicWriter musicXmlWriter = new MusicXMLWriterImpl();


    /**
     * Constructor for MXLWriterImpl.
     */
    public MXLWriterImpl() {
        // default constructor
    }


    /**
     * Write a score to an MXL file at the given path.
     *
     * @param score the score to write
     * @param path the path to write to
     * @throws IOException in case of write errors
     */
    @Override
    public void write(IScore score, Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing score to MXL path: {}", path);
        }

        try (OutputStream os = Files.newOutputStream(path)) {
            write(score, os);
        }
    }


    /**
     * Write a score to an MXL output stream.
     * Streams the MusicXML content directly into the ZIP entry to avoid double-buffering.
     *
     * @param score the score to write
     * @param outputStream the output stream to write to
     * @throws IOException in case of write errors
     */
    @Override
    public void write(IScore score, OutputStream outputStream) throws IOException {
        String containerXml = buildContainerXml(ROOT_FILE_NAME);
        byte[] containerBytes = containerXml.getBytes(StandardCharsets.UTF_8);
        byte[] mimetypeBytes = MIMETYPE_CONTENT.getBytes(StandardCharsets.US_ASCII);

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            // mimetype entry (stored, not compressed)
            ZipEntry mimetypeEntry = new ZipEntry("mimetype");
            mimetypeEntry.setMethod(ZipEntry.STORED);
            mimetypeEntry.setSize(mimetypeBytes.length);
            mimetypeEntry.setCompressedSize(mimetypeBytes.length);
            CRC32 crc = new CRC32();
            crc.update(mimetypeBytes);
            mimetypeEntry.setCrc(crc.getValue());
            zos.putNextEntry(mimetypeEntry);
            zos.write(mimetypeBytes);
            zos.closeEntry();

            // META-INF/container.xml
            zos.putNextEntry(new ZipEntry("META-INF/container.xml"));
            zos.write(containerBytes);
            zos.closeEntry();

            // score.musicxml (streamed directly into the ZIP entry)
            zos.putNextEntry(new ZipEntry(ROOT_FILE_NAME));
            musicXmlWriter.write(score, zos);
            zos.closeEntry();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Successfully wrote score to MXL archive.");
        }
    }


    /**
     * Build the container.xml content referencing the root MusicXML file.
     *
     * @param rootFileName the name of the root MusicXML file
     * @return the container.xml content as a string
     */
    private String buildContainerXml(String rootFileName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
             + "<container>\n"
             + "  <rootfiles>\n"
             + "    <rootfile full-path=\"" + rootFileName
             + "\" media-type=\"application/vnd.recordare.musicxml+xml\"/>\n"
             + "  </rootfiles>\n"
             + "</container>\n";
    }
}
