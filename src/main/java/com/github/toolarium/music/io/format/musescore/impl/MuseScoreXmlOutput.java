/*
 * MuseScoreXmlOutput.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import java.util.ArrayList;
import java.util.List;


/**
 * A small indenting XML writer in the style of MuseScore files.
 *
 * @author patrick
 */
final class MuseScoreXmlOutput {
    private final StringBuilder sb = new StringBuilder();
    private final List<String> stack = new ArrayList<>();


    /**
     * Appends raw text.
     *
     * @param text the text
     */
    void raw(String text) {
        sb.append(text);
    }


    /**
     * Opens an element.
     *
     * @param name the element name
     * @param attributes alternating attribute names and values
     */
    void open(String name, String... attributes) {
        indent();
        sb.append('<').append(name);
        appendAttributes(attributes);
        sb.append(">\n");
        stack.add(name);
    }


    /**
     * Opens an element whose text follows on the same line.
     *
     * @param name the element name
     * @param attributes alternating attribute names and values
     */
    void openInline(String name, String... attributes) {
        indent();
        sb.append('<').append(name);
        appendAttributes(attributes);
        sb.append('>');
    }


    /**
     * Closes an element opened with {@link #openInline(String, String...)}.
     *
     * @param name the element name
     */
    void closeInline(String name) {
        sb.append("</").append(name).append(">\n");
    }


    /**
     * Appends escaped text.
     *
     * @param text the text
     */
    void text(String text) {
        sb.append(escape(text));
    }


    /**
     * Closes the innermost open element.
     */
    void close() {
        String name = stack.remove(stack.size() - 1);
        indent();
        sb.append("</").append(name).append(">\n");
    }


    /**
     * Writes an element with text content.
     *
     * @param name the element name
     * @param text the text
     */
    void leaf(String name, String text) {
        indent();
        sb.append('<').append(name).append('>').append(escape(text)).append("</").append(name).append(">\n");
    }


    /**
     * Writes an empty element.
     *
     * @param name the element name
     * @param attributes alternating attribute names and values
     */
    void empty(String name, String... attributes) {
        indent();
        sb.append('<').append(name);
        appendAttributes(attributes);
        sb.append("/>\n");
    }


    /**
     * Appends attributes.
     *
     * @param attributes alternating attribute names and values
     */
    private void appendAttributes(String... attributes) {
        for (int i = 0; i + 1 < attributes.length; i += 2) {
            sb.append(' ').append(attributes[i]).append("=\"").append(escape(attributes[i + 1])).append('"');
        }
    }


    /**
     * Appends the indentation of the current depth.
     */
    private void indent() {
        for (int i = 0; i < stack.size(); i++) {
            sb.append("  ");
        }
    }


    /**
     * Escapes XML special characters.
     *
     * @param text the text (may be null)
     * @return the escaped text
     */
    String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }


    /**
     * Returns the document.
     *
     * @return the XML text
     */
    @Override
    public String toString() {
        return sb.toString();
    }
}
