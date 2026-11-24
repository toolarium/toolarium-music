/*
 * SibeliusTextConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl;

import com.github.toolarium.music.io.format.sibelius.impl.dto.SibeliusTextRun;
import java.util.ArrayList;
import java.util.List;


/**
 * Converts formatted Sibelius texts. Formatting commands stand between two backslashes: {@code \B\} and {@code \b\}
 * switch bold on and off, {@code \I\} and {@code \i\} italic, {@code \fName\} selects a font ({@code \f_\} the font of
 * the style), {@code \sNN\} the size, {@code \n\} is a line break and {@code \$NAME\} a placeholder.
 *
 * @author patrick
 */
public final class SibeliusTextConverter {
    private static final char COMMAND = '\\';
    private static final String STYLE_FONT = "_";
    private static final double SIZE_UNITS_PER_POINT = 155.0 / 24.0;


    /**
     * Private holder class for the singleton instance.
     */
    private static final class HOLDER {
        static final SibeliusTextConverter INSTANCE = new SibeliusTextConverter();
    }


    /**
     * Constructor.
     */
    private SibeliusTextConverter() {
        // NOP
    }


    /**
     * Gets the singleton instance.
     *
     * @return the instance
     */
    public static SibeliusTextConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Splits a formatted text into parts with the same formatting.
     *
     * @param formatted the formatted text
     * @return the text runs (without empty runs)
     */
    public List<SibeliusTextRun> getRuns(String formatted) {
        List<SibeliusTextRun> result = new ArrayList<>();
        if (formatted == null) {
            return result;
        }
        String font = null;
        Integer size = null;
        boolean bold = false;
        boolean italic = false;
        StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < formatted.length()) {
            char c = formatted.charAt(i);
            int end = -1;
            if (c == COMMAND) {
                end = formatted.indexOf(COMMAND, i + 1);
            }
            if (end < 0) {
                text.append(c);
                i++;
                continue;
            }

            String command = formatted.substring(i + 1, end);
            i = end + 1;
            if ("n".equals(command)) {
                text.append('\n');
                continue;
            }
            String newFont = font;
            Integer newSize = size;
            boolean newBold = bold;
            boolean newItalic = italic;
            if (command.startsWith("f") && command.length() > 1) {
                newFont = command.substring(1);
                if (STYLE_FONT.equals(newFont)) {
                    newFont = null;
                }
            } else if (command.startsWith("s") && command.length() > 1) {
                newSize = parseSize(command.substring(1), size);
            } else if ("B".equals(command)) {
                newBold = true;
            } else if ("b".equals(command)) {
                newBold = false;
            } else if ("I".equals(command)) {
                newItalic = true;
            } else if ("i".equals(command)) {
                newItalic = false;
            }
            boolean changed = !equalsNullable(font, newFont) || !equalsNullable(size, newSize) || bold != newBold || italic != newItalic;
            if (changed && text.length() > 0) {
                result.add(new SibeliusTextRun(text.toString(), font, size, bold, italic));
                text.setLength(0);
            }
            font = newFont;
            size = newSize;
            bold = newBold;
            italic = newItalic;
        }
        if (text.length() > 0) {
            result.add(new SibeliusTextRun(text.toString(), font, size, bold, italic));
        }
        return result;
    }


    /**
     * Gets the plain text of a formatted text.
     *
     * @param formatted the formatted text
     * @return the plain text (trimmed)
     */
    public String getPlainText(String formatted) {
        StringBuilder result = new StringBuilder();
        for (SibeliusTextRun run : getRuns(formatted)) {
            result.append(run.getText());
        }
        return result.toString().trim();
    }


    /**
     * Parses a size command.
     *
     * @param value the value of the size command
     * @param current the current size
     * @return the size in points
     */
    private Integer parseSize(String value, Integer current) {
        try {
            return (int) Math.round(Integer.parseInt(value) / SIZE_UNITS_PER_POINT);
        } catch (NumberFormatException e) {
            return current;
        }
    }


    /**
     * Compares two values which can be null.
     *
     * @param a the first value
     * @param b the second value
     * @return true if both are null or equal
     */
    private boolean equalsNullable(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
