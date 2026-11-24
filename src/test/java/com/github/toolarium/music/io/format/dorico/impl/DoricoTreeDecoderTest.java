/*
 * DoricoTreeDecoderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;


/**
 * Tests for the {@link DoricoTreeDecoder}.
 *
 * @author patrick
 */
public class DoricoTreeDecoderTest {
    private static final String INFO = "info";

    /**
     * Tests decoding a binary tree with leaves, objects, filled and empty arrays.
     *
     * @throws IOException if decoding fails
     */
    @Test
    void testDecodeBinary() throws IOException {
        DoricoTreeNode root = DoricoTreeDecoder.getInstance().decode(encode(sampleTree()));
        assertSampleTree(root);
    }


    /**
     * Tests decoding the XML encoding of the same tree.
     *
     * @throws IOException if decoding fails
     */
    @Test
    void testDecodeXml() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<kScore>\n"
                + "  <fileVersion>1.1096</fileVersion>\n"
                + "  <info>\n"
                + "    <title>Test</title>\n"
                + "    <composer/>\n"
                + "  </info>\n"
                + "  <flows>\n"
                + "    <array array=\"true\">\n"
                + "      <flow><flowID>0</flowID></flow>\n"
                + "      <flow><flowID>1</flowID></flow>\n"
                + "    </array>\n"
                + "  </flows>\n"
                + "  <textFlows><array array=\"true\"/></textFlows>\n"
                + "</kScore>\n";
        DoricoTreeNode root = DoricoTreeDecoder.getInstance().decode(xml.getBytes(StandardCharsets.UTF_8));
        assertSampleTree(root);
    }


    /**
     * Tests the path helpers of the node.
     *
     * @throws IOException if decoding fails
     */
    @Test
    void testPathHelpers() throws IOException {
        DoricoTreeNode root = DoricoTreeDecoder.getInstance().decode(encode(sampleTree()));
        assertEquals("Test", root.getPathValue(INFO, "title"));
        assertNull(root.getPathValue(INFO, "missing"));
        assertNull(root.getPath("missing", "title"));
        assertEquals(2, root.getChild("flows").getArrayItems().size());
        assertTrue(root.getChild("textFlows").getArrayItems().isEmpty());
        assertTrue(root.getChild(INFO).getArrayItems().isEmpty());
    }


    /**
     * Tests that empty data and an unknown header are rejected.
     */
    @Test
    void testRejectInvalidHeader() {
        assertThrows(IOException.class, () -> DoricoTreeDecoder.getInstance().decode(new byte[0]));
        assertThrows(IOException.class, () -> DoricoTreeDecoder.getInstance().decode(null));
        byte[] data = encode(sampleTree());
        data[0] = 5;
        assertThrows(IOException.class, () -> DoricoTreeDecoder.getInstance().decode(data));
    }


    /**
     * Tests that every truncation of a valid file is rejected with an IOException.
     */
    @Test
    void testRejectTruncatedData() {
        byte[] data = encode(sampleTree());
        for (int length = 1; length < data.length; length++) {
            byte[] truncated = Arrays.copyOf(data, length);
            assertThrows(IOException.class, () -> DoricoTreeDecoder.getInstance().decode(truncated),
                    "Truncated data of length " + length + " must be rejected.");
        }
    }


    /**
     * Tests that backward child references (which could form cycles) are rejected.
     */
    @Test
    void testRejectBackwardChildReference() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeInt(out, 4);
        writeInt(out, 1);
        writeInt(out, 1);
        writeCString(out, "kScore");
        writeInt(out, 1);
        writeCString(out, "");
        writeInt(out, 2);
        // node 0 with child 1, node 1 with child 0
        out.write(2);
        out.write(0);
        out.write(0);
        out.write(1);
        out.write(1);
        out.write(2);
        out.write(0);
        out.write(0);
        out.write(1);
        out.write(0);
        assertThrows(IOException.class, () -> DoricoTreeDecoder.getInstance().decode(out.toByteArray()));
    }


    /**
     * Tests that a node with two parents is rejected.
     */
    @Test
    void testRejectSharedChild() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeInt(out, 4);
        writeInt(out, 1);
        writeInt(out, 1);
        writeCString(out, "kScore");
        writeInt(out, 1);
        writeCString(out, "");
        writeInt(out, 2);
        out.write(2);
        out.write(0);
        out.write(0);
        out.write(2);
        out.write(1);
        out.write(1);
        out.write(0);
        out.write(0);
        out.write(0);
        assertThrows(IOException.class, () -> DoricoTreeDecoder.getInstance().decode(out.toByteArray()));
    }


    /**
     * Tests that an XML tree with a DOCTYPE is rejected (XXE protection).
     */
    @Test
    void testRejectXmlDoctype() {
        String xml = "<?xml version=\"1.0\"?><!DOCTYPE kScore [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<kScore><title>&x;</title></kScore>";
        assertThrows(IOException.class, () -> DoricoTreeDecoder.getInstance().decode(xml.getBytes(StandardCharsets.UTF_8)));
    }


    /**
     * Tests that a varint longer than five bytes is rejected.
     */
    @Test
    void testRejectOverlongVarint() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeInt(out, 4);
        writeInt(out, 1);
        writeInt(out, 1);
        writeCString(out, "kScore");
        writeInt(out, 1);
        writeCString(out, "");
        writeInt(out, 1);
        out.write(0);
        for (int i = 0; i < 6; i++) {
            out.write(0x80);
        }
        out.write(0);
        out.write(0);
        assertThrows(IOException.class, () -> DoricoTreeDecoder.getInstance().decode(out.toByteArray()));
    }


    /**
     * Builds the sample tree used by several tests.
     *
     * @return the root node
     */
    private DoricoTreeNode sampleTree() {
        DoricoTreeNode info = node(INFO, node("title", "Test"), node("composer", ""));
        DoricoTreeNode flow0 = node("flow", node("flowID", "0"));
        DoricoTreeNode flow1 = node("flow", node("flowID", "1"));
        DoricoTreeNode flows = node("flows", new DoricoTreeNode("array", "", true, Arrays.asList(flow0, flow1)));
        DoricoTreeNode textFlows = node("textFlows", new DoricoTreeNode("array", "", true, Collections.emptyList()));
        return node("kScore", node("fileVersion", "1.1096"), info, flows, textFlows);
    }


    /**
     * Verifies the decoded sample tree.
     *
     * @param root the decoded root
     */
    private void assertSampleTree(DoricoTreeNode root) {
        assertNotNull(root);
        assertEquals("kScore", root.getName());
        assertEquals(4, root.getChildren().size());
        assertEquals("1.1096", root.getPathValue("fileVersion"));
        assertEquals("Test", root.getPathValue(INFO, "title"));
        assertEquals("", root.getPathValue(INFO, "composer"));

        DoricoTreeNode array = root.getPath("flows", "array");
        assertTrue(array.isArray());
        assertEquals(2, array.getChildren().size());
        assertEquals("1", array.getChildren().get(1).getPathValue("flowID"));

        DoricoTreeNode emptyArray = root.getPath("textFlows", "array");
        assertTrue(emptyArray.isArray());
        assertTrue(emptyArray.getChildren().isEmpty());
        assertFalse(root.getChild(INFO).isArray());
    }


    /**
     * Creates a leaf node.
     *
     * @param name the name
     * @param value the value
     * @return the node
     */
    private DoricoTreeNode node(String name, String value) {
        return new DoricoTreeNode(name, value, false, null);
    }


    /**
     * Creates an object node.
     *
     * @param name the name
     * @param children the children
     * @return the node
     */
    private DoricoTreeNode node(String name, DoricoTreeNode... children) {
        return new DoricoTreeNode(name, "", false, Arrays.asList(children));
    }


    /**
     * Encodes a tree in the binary .dtn format (pre-order numbering, first-occurrence string tables).
     *
     * @param root the root node
     * @return the encoded bytes
     */
    private byte[] encode(DoricoTreeNode root) {
        List<DoricoTreeNode> order = new ArrayList<>();
        preOrder(root, order);

        Map<String, Integer> names = new LinkedHashMap<>();
        Map<String, Integer> values = new LinkedHashMap<>();
        values.put("", 0);
        for (DoricoTreeNode n : order) {
            names.putIfAbsent(n.getName(), names.size());
        }
        for (DoricoTreeNode n : order) {
            values.putIfAbsent(n.getValue(), values.size());
        }

        Map<DoricoTreeNode, Integer> ids = new IdentityHashMap<>();
        for (int i = 0; i < order.size(); i++) {
            ids.put(order.get(i), i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeInt(out, 4);
        writeInt(out, 1);
        writeInt(out, names.size());
        for (String name : names.keySet()) {
            writeCString(out, name);
        }
        writeInt(out, values.size());
        for (String value : values.keySet()) {
            writeCString(out, value);
        }
        writeInt(out, order.size());
        for (DoricoTreeNode n : order) {
            int flags = 0;
            if (n.isArray()) {
                flags |= 1;
            }
            if (!n.getChildren().isEmpty()) {
                flags |= 2;
            }
            out.write(flags);
            writeVarint(out, names.get(n.getName()));
            writeVarint(out, values.get(n.getValue()));
            if (!n.getChildren().isEmpty()) {
                writeVarint(out, n.getChildren().size());
                for (DoricoTreeNode child : n.getChildren()) {
                    writeVarint(out, ids.get(child));
                }
            }
        }
        return out.toByteArray();
    }


    /**
     * Collects nodes in pre-order.
     *
     * @param node the node
     * @param order the result list
     */
    private void preOrder(DoricoTreeNode node, List<DoricoTreeNode> order) {
        order.add(node);
        for (DoricoTreeNode child : node.getChildren()) {
            preOrder(child, order);
        }
    }


    /**
     * Writes a little-endian 32-bit integer.
     *
     * @param out the output
     * @param value the value
     */
    private void writeInt(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }


    /**
     * Writes a NUL-terminated UTF-8 string.
     *
     * @param out the output
     * @param value the value
     */
    private void writeCString(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
        out.write(0);
    }


    /**
     * Writes an unsigned LEB128 varint.
     *
     * @param out the output
     * @param value the value
     */
    private void writeVarint(ByteArrayOutputStream out, int value) {
        int v = value;
        while (v >= 0x80) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v);
    }
}
