/*
 * ZipContentReaderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;


/**
 * Tests for the ZIP and stream content readers.
 *
 * @author patrick
 */
public class ZipContentReaderTest {

    /**
     * Tests listing and reading stored and deflated entries.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadEntries() throws IOException {
        byte[] text = "hello score".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("Content/score.gpif"));
            zip.write(text);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("other.txt"));
            zip.write(new byte[] {1, 2, 3});
            zip.closeEntry();
        }
        byte[] content = out.toByteArray();
        ZipContentReader reader = ZipContentReader.getInstance();
        assertEquals(2, reader.list(content).size());
        assertArrayEquals(text, reader.read(content, "Content/score.gpif", 1024));
        assertNull(reader.read(content, "missing", 1024));
        assertThrows(IOException.class, () -> reader.read(content, "Content/score.gpif", 4));
        assertThrows(IOException.class, () -> reader.list(new byte[] {1, 2, 3}));
    }


    /**
     * Tests the size limit of the stream content reader.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testStreamLimit() throws IOException {
        StreamContentReader reader = StreamContentReader.getInstance();
        assertEquals(3, reader.readAll(new ByteArrayInputStream(new byte[] {1, 2, 3}), 10, "test").length);
        assertThrows(IOException.class, () -> reader.readAll(new ByteArrayInputStream(new byte[20000]), 10, "test"));
        assertThrows(IOException.class, () -> reader.readAll(null, 10, "test"));
    }
}
