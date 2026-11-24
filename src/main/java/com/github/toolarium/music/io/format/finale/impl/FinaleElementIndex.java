/*
 * FinaleElementIndex.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Finds the elements of an EnigmaXML document by their local name. The document is walked once and the elements
 * are kept on its root element, so that the many lookups of a conversion don't walk the whole document again.
 *
 * @author patrick
 */
public final class FinaleElementIndex {
    private static final String NS = "http://www.makemusic.com/2012/finale";
    private static final String INDEX_KEY = FinaleElementIndex.class.getName();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final FinaleElementIndex INSTANCE = new FinaleElementIndex();
    }


    /**
     * Constructor.
     */
    private FinaleElementIndex() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static FinaleElementIndex getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets all elements below the root element with a local name in the Finale namespace, in document order (the
     * same elements as root.getElementsByTagNameNS). The document must not change after the first lookup.
     *
     * @param root the root element
     * @param name the local name
     * @return the elements, never null
     */
    public List<Element> getElements(Element root, String name) {
        List<Element> result = getIndex(root).get(name);
        if (result == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(result);
    }


    /**
     * Gets the index of a root element, which is created on the first lookup.
     *
     * @param root the root element
     * @return the elements by local name
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<Element>> getIndex(Element root) {
        Object index = root.getUserData(INDEX_KEY);
        if (index instanceof Map) {
            return (Map<String, List<Element>>) index;
        }
        Map<String, List<Element>> result = createIndex(root);
        root.setUserData(INDEX_KEY, result, null);
        return result;
    }


    /**
     * Walks the elements below the root element once, in document order.
     *
     * @param root the root element
     * @return the elements by local name
     */
    private Map<String, List<Element>> createIndex(Element root) {
        Map<String, List<Element>> result = new HashMap<>();
        Node node = root.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && NS.equals(node.getNamespaceURI())) {
                result.computeIfAbsent(node.getLocalName(), k -> new ArrayList<>()).add((Element) node);
            }
            node = next(root, node);
        }
        return result;
    }


    /**
     * Gets the next node in document order below the root element.
     *
     * @param root the root element
     * @param node the current node
     * @return the next node, or null at the end
     */
    private Node next(Element root, Node node) {
        if (node.getFirstChild() != null) {
            return node.getFirstChild();
        }
        Node current = node;
        while (current != null && current != root) {
            if (current.getNextSibling() != null) {
                return current.getNextSibling();
            }
            current = current.getParentNode();
        }
        return null;
    }
}
