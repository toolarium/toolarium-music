/*
 * CapRtfConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Converts the rich text (RTF) of capella text objects into plain text and finds its font size.
 *
 * @author patrick
 */
final class CapRtfConverter {
    private static final Pattern FONT_SIZE = Pattern.compile("\\\\fs(\\d+)");
    private static final String[] SKIPPED_GROUPS = {"\\fonttbl", "\\colortbl", "\\stylesheet", "\\info", "\\*"};
    private static final int HEX_RADIX = 16;
    private static final int HALF_POINTS = 2;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final CapRtfConverter INSTANCE = new CapRtfConverter();
    }


    /**
     * Constructor.
     */
    private CapRtfConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    static CapRtfConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Converts RTF into plain text: control words and the font, color and style tables are removed,
     * paragraph marks become spaces. Text that is not RTF is returned trimmed.
     *
     * @param rtf the RTF text
     * @return the plain text
     */
    String toText(String rtf) {
        if (rtf == null) {
            return "";
        }
        if (!rtf.trim().startsWith("{\\rtf")) {
            return rtf.trim();
        }
        StringBuilder text = new StringBuilder();
        int skipDepth = -1;
        int depth = 0;
        int i = 0;
        while (i < rtf.length()) {
            char c = rtf.charAt(i);
            if (c == '{') {
                depth++;
                if (skipDepth < 0 && startsSkippedGroup(rtf, i + 1)) {
                    skipDepth = depth;
                }
                i++;
                continue;
            }
            if (c == '}') {
                if (depth == skipDepth) {
                    skipDepth = -1;
                }
                depth--;
                i++;
                continue;
            }
            if (c == '\\') {
                i = readControl(rtf, i, text, skipDepth < 0);
                continue;
            }
            if (skipDepth < 0 && c != '\r' && c != '\n') {
                text.append(c);
            }
            i++;
        }
        return text.toString().replaceAll("\\s+", " ").trim();
    }


    /**
     * Gets the first font size of RTF text.
     *
     * @param rtf the RTF text
     * @return the font size in points, or 0 if the text has none
     */
    int fontSize(String rtf) {
        if (rtf == null) {
            return 0;
        }
        Matcher matcher = FONT_SIZE.matcher(rtf);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) / HALF_POINTS;
        }
        return 0;
    }


    /**
     * Reads a control word, control symbol or escaped character.
     *
     * @param rtf the RTF text
     * @param start the index of the backslash
     * @param text the plain text (filled)
     * @param visible true if the text is not in a skipped group
     * @return the index after the control
     */
    private int readControl(String rtf, int start, StringBuilder text, boolean visible) {
        int i = start + 1;
        if (i >= rtf.length()) {
            return i;
        }
        char c = rtf.charAt(i);
        if (c == '\'' && i + 2 < rtf.length()) {
            if (visible) {
                text.append((char) Integer.parseInt(rtf.substring(i + 1, i + 3), HEX_RADIX));
            }
            return i + 3;
        }
        if (!Character.isLetter(c)) {
            if (visible && (c == '\\' || c == '{' || c == '}')) {
                text.append(c);
            }
            return i + 1;
        }
        int end = i;
        while (end < rtf.length() && Character.isLetter(rtf.charAt(end))) {
            end++;
        }
        String word = rtf.substring(i, end);
        if (end < rtf.length() && (rtf.charAt(end) == '-' || Character.isDigit(rtf.charAt(end)))) {
            end++;
            while (end < rtf.length() && Character.isDigit(rtf.charAt(end))) {
                end++;
            }
        }
        if (end < rtf.length() && rtf.charAt(end) == ' ') {
            end++;
        }
        if (visible && ("par".equals(word) || "line".equals(word) || "tab".equals(word))) {
            text.append(' ');
        }
        return end;
    }


    /**
     * Checks if a group starts with a control word of a skipped group.
     *
     * @param rtf the RTF text
     * @param index the index after the opening brace
     * @return true for a font, color, style or info table or an ignorable destination
     */
    private boolean startsSkippedGroup(String rtf, int index) {
        for (String group : SKIPPED_GROUPS) {
            if (rtf.startsWith(group, index)) {
                return true;
            }
        }
        return false;
    }
}
