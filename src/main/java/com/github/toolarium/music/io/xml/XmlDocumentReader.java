/*
 * XmlDocumentReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Parses XML documents securely (no DTD, no external entities) and navigates elements by their
 * local name, independent of namespaces. Used by the readers of XML based formats.
 *
 * @author patrick
 */
public final class XmlDocumentReader {
    private static final Logger LOG = LoggerFactory.getLogger(XmlDocumentReader.class);


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final XmlDocumentReader INSTANCE = new XmlDocumentReader();
    }


    /**
     * Constructor.
     */
    private XmlDocumentReader() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static XmlDocumentReader getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Parses an XML document securely and namespace aware.
     *
     * @param content the document
     * @param description the document description for error messages, for example MEI
     * @return the document
     * @throws IOException if the document cannot be parsed
     */
    public Document parse(byte[] content, String description) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(new ByteArrayInputStream(content));
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to parse {} document: {}", description, e.getMessage(), e);
            }
            throw new IOException("Failed to parse " + description + " document: " + e.getMessage(), e);
        }
    }


    /**
     * Gets the local name of an element (without namespace prefix).
     *
     * @param element the element
     * @return the local name
     */
    public String name(Element element) {
        String name = element.getLocalName();
        if (name == null) {
            name = element.getNodeName();
            int colon = name.indexOf(':');
            if (colon >= 0) {
                name = name.substring(colon + 1);
            }
        }
        return name;
    }


    /**
     * Gets the child elements of an element.
     *
     * @param parent the parent (may be null)
     * @return the child elements
     */
    public List<Element> elements(Element parent) {
        List<Element> result = new ArrayList<>();
        if (parent == null) {
            return result;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) nodes.item(i));
            }
        }
        return result;
    }


    /**
     * Gets the child elements with a local name.
     *
     * @param parent the parent (may be null)
     * @param name the local name
     * @return the child elements
     */
    public List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        for (Element child : elements(parent)) {
            if (name.equals(name(child))) {
                result.add(child);
            }
        }
        return result;
    }


    /**
     * Gets the first child element with a local name.
     *
     * @param parent the parent (may be null)
     * @param name the local name
     * @return the child, or null
     */
    public Element child(Element parent, String name) {
        for (Element child : elements(parent)) {
            if (name.equals(name(child))) {
                return child;
            }
        }
        return null;
    }


    /**
     * Gets the element at a path of local names below a parent, for example {@code fileDesc/titleStmt}.
     *
     * @param parent the parent (may be null)
     * @param path the path separated by slashes
     * @return the element, or null
     */
    public Element path(Element parent, String path) {
        Element current = parent;
        for (String name : path.split("/")) {
            current = child(current, name);
            if (current == null) {
                return null;
            }
        }
        return current;
    }


    /**
     * Gets all descendant elements with a local name in document order.
     *
     * @param parent the parent (may be null)
     * @param name the local name
     * @return the descendants
     */
    public List<Element> descendants(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        for (Element child : elements(parent)) {
            if (name.equals(name(child))) {
                result.add(child);
            }
            result.addAll(descendants(child, name));
        }
        return result;
    }


    /**
     * Gets an attribute value.
     *
     * @param element the element (may be null)
     * @param name the attribute name
     * @return the trimmed value, or null if absent or empty
     */
    public String attr(Element element, String name) {
        if (element == null || !element.hasAttribute(name)) {
            return null;
        }
        String value = element.getAttribute(name).trim();
        if (value.isEmpty()) {
            return null;
        }
        return value;
    }


    /**
     * Gets the text of an element with normalized white space. Line break elements (lb, br) become spaces.
     *
     * @param element the element (may be null)
     * @return the text (empty if none)
     */
    public String text(Element element) {
        if (element == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendText(element, sb);
        return sb.toString().replaceAll("\\s+", " ").trim();
    }


    /**
     * Gets the text of a child element.
     *
     * @param parent the parent (may be null)
     * @param name the local name of the child
     * @return the text, or null if the child is missing or empty
     */
    public String childText(Element parent, String name) {
        String text = text(child(parent, name));
        if (text.isEmpty()) {
            return null;
        }
        return text;
    }


    /**
     * Appends the text of a node.
     *
     * @param node the node
     * @param sb the text
     */
    private void appendText(Node node, StringBuilder sb) {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(child.getNodeValue());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                String name = name((Element) child);
                if ("lb".equals(name) || "br".equals(name)) {
                    sb.append(' ');
                } else {
                    appendText(child, sb);
                }
            }
        }
    }
}
