/*
 * DoricoTreeDecoder.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Decodes Dorico tree files (score.dtn, scorelibrary.dtn) into a {@link DoricoTreeNode} tree.
 *
 * <p>Two encodings are supported. The binary encoding consists of a header (two little-endian
 * 32-bit values 4 and 1), a name table, a value table and a flat node array where every node
 * record holds a flag byte, a name index, a value index and, if flag bit 1 is set, a child count
 * and child node indexes (all LEB128 varints). The XML encoding renders the same tree as XML,
 * with the attribute {@code array="true"} for array nodes.</p>
 *
 * @author patrick
 */
public final class DoricoTreeDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(DoricoTreeDecoder.class);
    private static final int MAGIC_1 = 4;
    private static final int MAGIC_2 = 1;
    private static final int FLAG_ARRAY = 0x01;
    private static final int FLAG_CHILDREN = 0x02;
    private static final int MIN_NODE_RECORD_SIZE = 3;
    private static final int MAX_VARINT_BYTES = 5;
    private static final int MAX_XML_DEPTH = 1024;
    private static final String ATTR_ARRAY = "array";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final DoricoTreeDecoder INSTANCE = new DoricoTreeDecoder();
    }


    /**
     * Constructor.
     */
    private DoricoTreeDecoder() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static DoricoTreeDecoder getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Decodes a Dorico tree file, detecting the binary or the XML encoding.
     *
     * @param data the file content
     * @return the root node
     * @throws IOException if the data is not a valid Dorico tree
     */
    public DoricoTreeNode decode(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("Dorico tree data is empty.");
        }

        if (isXml(data)) {
            return decodeXml(data);
        }
        return decodeBinary(data);
    }


    /**
     * Checks whether the data starts with an XML declaration or element (after an optional
     * UTF-8 byte order mark and whitespace).
     *
     * @param data the data
     * @return true if the data looks like XML
     */
    private boolean isXml(byte[] data) {
        int i = 0;
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            i = 3;
        }
        while (i < data.length && (data[i] == ' ' || data[i] == '\t' || data[i] == '\r' || data[i] == '\n')) {
            i++;
        }
        return i < data.length && data[i] == '<';
    }


    /**
     * Decodes the binary tree encoding.
     *
     * @param data the file content
     * @return the root node
     * @throws IOException if the data is malformed
     */
    private DoricoTreeNode decodeBinary(byte[] data) throws IOException {
        Cursor cursor = new Cursor(data);
        int magic1 = cursor.readInt32();
        int magic2 = cursor.readInt32();
        if (magic1 != MAGIC_1 || magic2 != MAGIC_2) {
            throw new IOException("Unsupported Dorico tree header (" + magic1 + ", " + magic2 + ").");
        }

        String[] names = readStringTable(cursor);
        String[] values = readStringTable(cursor);

        int nodeCount = cursor.readInt32();
        if (nodeCount <= 0 || nodeCount > cursor.remaining() / MIN_NODE_RECORD_SIZE) {
            throw new IOException("Invalid Dorico tree node count: " + nodeCount + ".");
        }

        byte[] flags = new byte[nodeCount];
        int[] nameIndex = new int[nodeCount];
        int[] valueIndex = new int[nodeCount];
        int[][] childIndex = new int[nodeCount][];
        boolean[] hasParent = new boolean[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            flags[i] = (byte) cursor.readByte();
            nameIndex[i] = checkIndex(cursor.readVarint(), names.length, "name");
            valueIndex[i] = checkIndex(cursor.readVarint(), values.length, "value");

            if ((flags[i] & FLAG_CHILDREN) != 0) {
                int count = cursor.readVarint();
                if (count > cursor.remaining()) {
                    throw new IOException("Invalid Dorico tree child count: " + count + ".");
                }

                int[] children = new int[count];
                for (int c = 0; c < count; c++) {
                    int child = cursor.readVarint();
                    // children are numbered in pre-order, so they always follow their parent
                    if (child <= i || child >= nodeCount) {
                        throw new IOException("Invalid Dorico tree child reference " + child + " in node " + i + ".");
                    }
                    if (hasParent[child]) {
                        throw new IOException("Dorico tree node " + child + " has more than one parent.");
                    }
                    hasParent[child] = true;
                    children[c] = child;
                }
                childIndex[i] = children;
            }
        }

        if (cursor.remaining() > 0 && LOG.isDebugEnabled()) {
            LOG.debug("Ignoring {} trailing bytes after the Dorico tree.", cursor.remaining());
        }

        // build bottom-up: every child index is larger than its parent index
        DoricoTreeNode[] nodes = new DoricoTreeNode[nodeCount];
        for (int i = nodeCount - 1; i >= 0; i--) {
            List<DoricoTreeNode> children = new ArrayList<>();
            if (childIndex[i] != null) {
                for (int child : childIndex[i]) {
                    children.add(nodes[child]);
                    nodes[child] = null;
                }
            }
            nodes[i] = new DoricoTreeNode(names[nameIndex[i]], values[valueIndex[i]], (flags[i] & FLAG_ARRAY) != 0, children);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Decoded binary Dorico tree: {} names, {} values, {} nodes.", names.length, values.length, nodeCount);
        }
        return nodes[0];
    }


    /**
     * Reads a string table: a 32-bit count followed by NUL-terminated UTF-8 strings.
     *
     * @param cursor the cursor
     * @return the strings
     * @throws IOException if the table is malformed
     */
    private String[] readStringTable(Cursor cursor) throws IOException {
        int count = cursor.readInt32();
        if (count < 0 || count > cursor.remaining()) {
            throw new IOException("Invalid Dorico tree string table size: " + count + ".");
        }

        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = cursor.readCString();
        }
        return result;
    }


    /**
     * Validates a table index.
     *
     * @param index the index
     * @param size the table size
     * @param table the table name for the error message
     * @return the index
     * @throws IOException if the index is out of range
     */
    private int checkIndex(int index, int size, String table) throws IOException {
        if (index >= size) {
            throw new IOException("Invalid Dorico tree " + table + " index: " + index + ".");
        }
        return index;
    }


    /**
     * Decodes the XML tree encoding.
     *
     * @param data the file content
     * @return the root node
     * @throws IOException if the XML is malformed
     */
    private DoricoTreeNode decodeXml(byte[] data) throws IOException {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new ByteArrayInputStream(data));
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to parse Dorico XML tree: {}", e.getMessage(), e);
            }
            throw new IOException("Failed to parse Dorico XML tree.", e);
        }

        return convertElement(document.getDocumentElement(), 0);
    }


    /**
     * Converts a DOM element into a tree node.
     *
     * @param element the element
     * @param depth the current depth
     * @return the node
     * @throws IOException if the tree is too deep
     */
    private DoricoTreeNode convertElement(Element element, int depth) throws IOException {
        if (depth > MAX_XML_DEPTH) {
            throw new IOException("Dorico XML tree exceeds maximum depth (" + MAX_XML_DEPTH + ").");
        }

        List<DoricoTreeNode> children = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                children.add(convertElement((Element) node, depth + 1));
            } else if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(node.getNodeValue());
            }
        }

        String value = "";
        if (children.isEmpty()) {
            value = text.toString();
        }
        boolean array = "true".equals(element.getAttribute(ATTR_ARRAY));
        return new DoricoTreeNode(element.getTagName(), value, array, children);
    }


    /**
     * Bounds-checked little-endian reader over a byte array.
     *
     * @author patrick
     */
    private static final class Cursor {
        private final byte[] data;
        private int position;


        /**
         * Constructor for Cursor.
         *
         * @param data the data
         */
        Cursor(byte[] data) {
            this.data = data;
            this.position = 0;
        }


        /**
         * Gets the number of unread bytes.
         *
         * @return the remaining byte count
         */
        int remaining() {
            return data.length - position;
        }


        /**
         * Reads an unsigned byte.
         *
         * @return the byte value (0 to 255)
         * @throws IOException if the end of data is reached
         */
        int readByte() throws IOException {
            if (position >= data.length) {
                throw new IOException("Unexpected end of Dorico tree data.");
            }
            return data[position++] & 0xFF;
        }


        /**
         * Reads a little-endian 32-bit integer.
         *
         * @return the value
         * @throws IOException if the end of data is reached
         */
        int readInt32() throws IOException {
            int b0 = readByte();
            int b1 = readByte();
            int b2 = readByte();
            int b3 = readByte();
            return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
        }


        /**
         * Reads an unsigned LEB128 varint that must fit into a non-negative int.
         *
         * @return the value
         * @throws IOException if the varint is malformed or too large
         */
        int readVarint() throws IOException {
            long result = 0;
            for (int i = 0; i < MAX_VARINT_BYTES; i++) {
                int b = readByte();
                result |= (long) (b & 0x7F) << (7 * i);
                if ((b & 0x80) == 0) {
                    if (result > Integer.MAX_VALUE) {
                        throw new IOException("Dorico tree varint out of range.");
                    }
                    return (int) result;
                }
            }
            throw new IOException("Dorico tree varint too long.");
        }


        /**
         * Reads a NUL-terminated UTF-8 string.
         *
         * @return the string
         * @throws IOException if no terminator is found
         */
        String readCString() throws IOException {
            int start = position;
            while (position < data.length && data[position] != 0) {
                position++;
            }
            if (position >= data.length) {
                throw new IOException("Unterminated string in Dorico tree data.");
            }
            String result = new String(data, start, position - start, StandardCharsets.UTF_8);
            position++;
            return result;
        }
    }
}
