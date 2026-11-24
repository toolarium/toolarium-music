/*
 * LilyPondTokenizer.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl;

import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Splits a LilyPond source into tokens: comments are removed, strings, commands, Scheme expressions, words,
 * numbers and symbols become tokens with their position in the source.
 *
 * @author patrick
 */
public final class LilyPondTokenizer {
    private static final int MAX_TOKENS = 5_000_000;
    private static final String TWO_CHARACTER_SYMBOLS = "<< >> -- __";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final LilyPondTokenizer INSTANCE = new LilyPondTokenizer();
    }


    /**
     * Constructor.
     */
    private LilyPondTokenizer() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static LilyPondTokenizer getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Splits a source into tokens.
     *
     * @param source the LilyPond source
     * @return the tokens in source order
     * @throws IOException if a string or a comment is not closed, or the source has too many tokens
     */
    public List<LilyPondToken> tokenize(String source) throws IOException {
        List<LilyPondToken> result = new ArrayList<>();
        int length = source.length();
        int i = 0;
        while (i < length) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '%') {
                i = skipComment(source, i);
            } else if (c == '"') {
                i = readString(source, i, result);
            } else if (c == '\\') {
                i = readCommand(source, i, result);
            } else if (c == '#' || c == '$') {
                i = readScheme(source, i, result);
            } else if (Character.isLetter(c)) {
                int end = i;
                while (end < length && Character.isLetter(source.charAt(end))) {
                    end++;
                }
                result.add(new LilyPondToken(LilyPondToken.WORD, source.substring(i, end), i, end));
                i = end;
            } else if (Character.isDigit(c)) {
                int end = i;
                while (end < length && Character.isDigit(source.charAt(end))) {
                    end++;
                }
                result.add(new LilyPondToken(LilyPondToken.NUMBER, source.substring(i, end), i, end));
                i = end;
            } else {
                String pair = null;
                if (i + 1 < length) {
                    pair = source.substring(i, i + 2);
                }
                if (pair != null && TWO_CHARACTER_SYMBOLS.contains(pair) && pair.indexOf(' ') < 0) {
                    result.add(new LilyPondToken(LilyPondToken.SYMBOL, pair, i, i + 2));
                    i += 2;
                } else {
                    result.add(new LilyPondToken(LilyPondToken.SYMBOL, String.valueOf(c), i, i + 1));
                    i++;
                }
            }
            if (result.size() > MAX_TOKENS) {
                throw new IOException("LilyPond source has too many tokens.");
            }
        }
        return result;
    }


    /**
     * Skips a line comment (% up to the end of the line) or a block comment (%{ up to %}).
     *
     * @param source the source
     * @param start the offset of the %
     * @return the offset after the comment
     * @throws IOException if a block comment is not closed
     */
    private int skipComment(String source, int start) throws IOException {
        if (start + 1 < source.length() && source.charAt(start + 1) == '{') {
            int end = source.indexOf("%}", start + 2);
            if (end < 0) {
                throw new IOException("LilyPond block comment is not closed.");
            }
            return end + 2;
        }
        int end = source.indexOf('\n', start);
        if (end < 0) {
            return source.length();
        }
        return end + 1;
    }


    /**
     * Reads a string in double quotes; \" and \\ are escapes.
     *
     * @param source the source
     * @param start the offset of the opening quote
     * @param result the tokens (filled)
     * @return the offset after the closing quote
     * @throws IOException if the string is not closed
     */
    private int readString(String source, int start, List<LilyPondToken> result) throws IOException {
        StringBuilder text = new StringBuilder();
        int i = start + 1;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\\' && i + 1 < source.length()) {
                text.append(source.charAt(i + 1));
                i += 2;
            } else if (c == '"') {
                result.add(new LilyPondToken(LilyPondToken.STRING, text.toString(), start, i + 1));
                return i + 1;
            } else {
                text.append(c);
                i++;
            }
        }
        throw new IOException("LilyPond string is not closed.");
    }


    /**
     * Reads a command: a backslash with a name (\new, \relative), or a backslash with one sign (\\, \&lt;, \!).
     *
     * @param source the source
     * @param start the offset of the backslash
     * @param result the tokens (filled)
     * @return the offset after the command
     */
    private int readCommand(String source, int start, List<LilyPondToken> result) {
        int i = start + 1;
        if (i >= source.length()) {
            result.add(new LilyPondToken(LilyPondToken.SYMBOL, "\\", start, i));
            return i;
        }
        if (Character.isLetter(source.charAt(i))) {
            // a name may contain letters, and a dash or an underscore followed by a letter (\voiceOne, \slurDown)
            while (i < source.length() && (Character.isLetter(source.charAt(i))
                    || (source.charAt(i) == '-' || source.charAt(i) == '_') && i + 1 < source.length() && Character.isLetter(source.charAt(i + 1)))) {
                i++;
            }
        } else {
            i++;
        }
        result.add(new LilyPondToken(LilyPondToken.COMMAND, source.substring(start, i), start, i));
        return i;
    }


    /**
     * Reads a Scheme expression after # (or $): a list in parentheses, a quoted datum, a string or an atom.
     *
     * @param source the source
     * @param start the offset of the #
     * @param result the tokens (filled)
     * @return the offset after the expression
     * @throws IOException if a list or a string is not closed
     */
    private int readScheme(String source, int start, List<LilyPondToken> result) throws IOException {
        int i = start + 1;
        while (i < source.length() && (source.charAt(i) == '\'' || source.charAt(i) == '`' || source.charAt(i) == ',')) {
            i++;
        }
        if (i < source.length() && source.charAt(i) == '(') {
            int depth = 0;
            while (i < source.length()) {
                char c = source.charAt(i);
                if (c == '"') {
                    i = skipSchemeString(source, i);
                    continue;
                }
                if (c == ';') {
                    int lineEnd = source.indexOf('\n', i);
                    if (lineEnd < 0) {
                        lineEnd = source.length();
                    }
                    i = lineEnd;
                    continue;
                }
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth == 0) {
                        i++;
                        break;
                    }
                }
                i++;
            }
            if (depth != 0) {
                throw new IOException("LilyPond Scheme expression is not closed.");
            }
        } else if (i < source.length() && source.charAt(i) == '"') {
            i = skipSchemeString(source, i);
        } else {
            while (i < source.length() && !Character.isWhitespace(source.charAt(i)) && "{}()<>\"".indexOf(source.charAt(i)) < 0) {
                i++;
            }
        }
        result.add(new LilyPondToken(LilyPondToken.SCHEME, source.substring(start, i), start, i));
        return i;
    }


    /**
     * Skips a string of a Scheme expression.
     *
     * @param source the source
     * @param start the offset of the opening quote
     * @return the offset after the closing quote
     * @throws IOException if the string is not closed
     */
    private int skipSchemeString(String source, int start) throws IOException {
        int i = start + 1;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '"') {
                return i + 1;
            } else {
                i++;
            }
        }
        throw new IOException("LilyPond string is not closed.");
    }
}
