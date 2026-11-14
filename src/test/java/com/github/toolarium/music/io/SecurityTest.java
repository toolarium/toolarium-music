/*
 * SecurityTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.toolarium.music.io.midi.MidiIOFactory;
import com.github.toolarium.music.io.musicxml.impl.MusicXMLIOFactory;
import com.github.toolarium.music.io.mxl.impl.MXLIOFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for security hardening: XXE prevention, ZIP bomb protection,
 * null input handling, and invalid data rejection.
 *
 * @author patrick
 */
public class SecurityTest {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityTest.class);


    /**
     * Tests that the MusicXML reader rejects input containing an XXE DOCTYPE declaration.
     */
    @Test
    void testXxeRejectionMusicXML() {
        String xxePayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<score-partwise version=\"4.0\">"
                + "<movement-title>&xxe;</movement-title>"
                + "</score-partwise>";
        byte[] bytes = xxePayload.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

        assertThrows(Exception.class, () -> {
            MusicXMLIOFactory.getInstance().getReader().read(bais);
        });

        LOG.info("XXE rejection for MusicXML confirmed.");
    }


    /**
     * Tests that the MXL reader rejects a container.xml containing an XXE DOCTYPE declaration.
     *
     * @throws IOException if an I/O error occurs building the test archive
     */
    @Test
    void testXxeRejectionMxl() throws IOException {
        String xxeContainer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<container version=\"1.0\">"
                + "<rootfiles><rootfile full-path=\"&xxe;\"/></rootfiles>"
                + "</container>";
        byte[] mxlBytes = buildMxlArchive("META-INF/container.xml", xxeContainer);
        ByteArrayInputStream bais = new ByteArrayInputStream(mxlBytes);

        assertThrows(Exception.class, () -> {
            MXLIOFactory.getInstance().getReader().read(bais);
        });

        LOG.info("XXE rejection for MXL container.xml confirmed.");
    }


    /**
     * Tests that the MXL reader rejects a ZIP archive with an oversized entry
     * that exceeds the maximum allowed decompressed size.
     *
     * @throws IOException if an I/O error occurs building the test archive
     */
    @Test
    void testZipBombRejectionMxl() throws IOException {
        ByteArrayOutputStream archiveOut = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(archiveOut)) {
            String containerXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
                    + "<rootfiles><rootfile full-path=\"score.musicxml\""
                    + " media-type=\"application/vnd.recordare.musicxml+xml\"/></rootfiles>"
                    + "</container>";
            ZipEntry containerEntry = new ZipEntry("META-INF/container.xml");
            zos.putNextEntry(containerEntry);
            zos.write(containerXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            ZipEntry bombEntry = new ZipEntry("score.musicxml");
            bombEntry.setSize(200_000_000L);
            zos.putNextEntry(bombEntry);
            byte[] padding = new byte[1024];
            for (int i = 0; i < 1024; i++) {
                zos.write(padding);
            }
            zos.closeEntry();
        }

        byte[] mxlBytes = archiveOut.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(mxlBytes);

        assertThrows(IOException.class, () -> {
            MXLIOFactory.getInstance().getReader().read(bais);
        });

        LOG.info("ZIP bomb rejection for MXL confirmed.");
    }


    /**
     * Tests that the MusicXML reader handles a null input stream gracefully.
     */
    @Test
    void testMusicXMLReaderNullInputStream() {
        assertThrows(Exception.class, () -> {
            MusicXMLIOFactory.getInstance().getReader().read((InputStream) null);
        });

        LOG.info("MusicXML null input stream rejection confirmed.");
    }


    /**
     * Tests that the MXL reader handles a null input stream gracefully.
     */
    @Test
    void testMxlReaderNullInputStream() {
        assertThrows(Exception.class, () -> {
            MXLIOFactory.getInstance().getReader().read((InputStream) null);
        });

        LOG.info("MXL null input stream rejection confirmed.");
    }


    /**
     * Tests that the MIDI reader rejects garbage data that is not a valid MIDI file.
     */
    @Test
    void testMidiReaderInvalidData() {
        byte[] garbage = "not a midi file".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream bais = new ByteArrayInputStream(garbage);

        assertThrows(IOException.class, () -> {
            MidiIOFactory.getInstance().getReader().read(bais);
        });

        LOG.info("MIDI invalid data rejection confirmed.");
    }


    /**
     * Tests that the MIDI reader rejects an empty input stream.
     */
    @Test
    void testMidiReaderEmptyStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);

        assertThrows(IOException.class, () -> {
            MidiIOFactory.getInstance().getReader().read(bais);
        });

        LOG.info("MIDI empty stream rejection confirmed.");
    }


    /**
     * Builds a minimal MXL (ZIP) archive containing a single entry.
     *
     * @param entryName the ZIP entry name
     * @param content the entry content as a string
     * @return the archive bytes
     * @throws IOException if an I/O error occurs
     */
    private byte[] buildMxlArchive(String entryName, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
