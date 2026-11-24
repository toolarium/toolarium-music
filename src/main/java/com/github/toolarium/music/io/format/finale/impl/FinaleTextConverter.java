/*
 * FinaleTextConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl;

import java.util.ArrayList;
import java.util.List;


/**
 * Converts the texts of Finale with their markup (^font, ^size and other commands) into plain text.
 *
 * @author patrick
 */
public final class FinaleTextConverter {

    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final FinaleTextConverter INSTANCE = new FinaleTextConverter();
    }


    /**
     * Constructor.
     */
    private FinaleTextConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static FinaleTextConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Strips Finale text markup (^font, ^size, etc.) to extract plain text.
     *
     * @param text the raw Finale text with markup
     * @return the plain text content
     */
    public String getPlainText(String text) {
        if (text == null) {
            return null;
        }

        // accidental commands become letters (B^flat() is Bb), all other commands (font, size, effects, inserts) are formatting
        // ^^ is a literal caret
        List<String> pieces = new ArrayList<>();
        for (String piece : text.split("\\^\\^", -1)) {
            String result = piece.replace("^flat()", "b").replace("^sharp()", "#").replace("^natural()", "");
            pieces.add(result.replaceAll("\\^[A-Za-z]+\\([^)]*\\)", ""));
        }
        return String.join("^", pieces).replaceAll("\\s+", " ").trim();
    }
}
