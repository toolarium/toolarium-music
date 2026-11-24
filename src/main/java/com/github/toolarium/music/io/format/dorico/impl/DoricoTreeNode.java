/*
 * DoricoTreeNode.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Immutable node of a decoded Dorico tree (.dtn file). Every node has a name, a text value,
 * an array flag and an ordered list of children, which mirrors the XML rendering of the tree.
 *
 * @author patrick
 */
public final class DoricoTreeNode {
    private final String name;
    private final String value;
    private final boolean array;
    private final List<DoricoTreeNode> children;


    /**
     * Constructor for DoricoTreeNode.
     *
     * @param name the node name
     * @param value the node value, null is treated as an empty string
     * @param array true if the node is an array (list) node
     * @param children the child nodes, null is treated as no children
     */
    public DoricoTreeNode(String name, String value, boolean array, List<DoricoTreeNode> children) {
        this.name = name;
        if (value == null) {
            this.value = "";
        } else {
            this.value = value;
        }
        this.array = array;
        if (children == null || children.isEmpty()) {
            this.children = Collections.emptyList();
        } else {
            this.children = Collections.unmodifiableList(new ArrayList<>(children));
        }
    }


    /**
     * Gets the node name.
     *
     * @return the node name
     */
    public String getName() {
        return name;
    }


    /**
     * Gets the node value.
     *
     * @return the node value, never null (empty for container nodes)
     */
    public String getValue() {
        return value;
    }


    /**
     * Checks whether the node is an array (list) node.
     *
     * @return true if the node is an array node
     */
    public boolean isArray() {
        return array;
    }


    /**
     * Gets all child nodes.
     *
     * @return the unmodifiable list of children
     */
    public List<DoricoTreeNode> getChildren() {
        return children;
    }


    /**
     * Gets the first child with the given name.
     *
     * @param childName the child name
     * @return the first matching child, or null if there is none
     */
    public DoricoTreeNode getChild(String childName) {
        for (DoricoTreeNode child : children) {
            if (child.getName().equals(childName)) {
                return child;
            }
        }
        return null;
    }


    /**
     * Gets all children with the given name.
     *
     * @param childName the child name
     * @return the list of matching children (may be empty)
     */
    public List<DoricoTreeNode> getChildrenByName(String childName) {
        List<DoricoTreeNode> result = new ArrayList<>();
        for (DoricoTreeNode child : children) {
            if (child.getName().equals(childName)) {
                result.add(child);
            }
        }
        return result;
    }


    /**
     * Follows a path of child names starting at this node.
     *
     * @param path the child names, one per level
     * @return the node at the end of the path, or null if any step is missing
     */
    public DoricoTreeNode getPath(String... path) {
        DoricoTreeNode current = this;
        for (String step : path) {
            current = current.getChild(step);
            if (current == null) {
                return null;
            }
        }
        return current;
    }


    /**
     * Gets the value at the end of a path of child names.
     *
     * @param path the child names, one per level
     * @return the value, or null if any step is missing
     */
    public String getPathValue(String... path) {
        DoricoTreeNode node = getPath(path);
        if (node == null) {
            return null;
        }
        return node.getValue();
    }


    /**
     * Gets the items of a Dorico array container. Dorico wraps lists either directly
     * (the node itself is the array) or in a child called "array" or "entities".
     *
     * @return the list items, or an empty list if the node holds no array
     */
    public List<DoricoTreeNode> getArrayItems() {
        if (array) {
            return children;
        }
        for (DoricoTreeNode child : children) {
            if (child.isArray()) {
                return child.getChildren();
            }
        }
        return Collections.emptyList();
    }


    /**
     * Returns a short description of the node (name, value and child count).
     *
     * @return the description
     */
    @Override
    public String toString() {
        return "DoricoTreeNode [name=" + name + ", value=" + value + ", array=" + array
                + ", children=" + children.size() + "]";
    }
}
