/*
 * MuseScorePageTextWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import com.github.toolarium.music.model.score.ICreator;
import com.github.toolarium.music.model.score.ICredit;
import com.github.toolarium.music.model.score.ICreditWords;
import com.github.toolarium.music.model.score.IScore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Writes the texts of the pages of a MuseScore 4 score: the title frame with the texts of the first page and the
 * header and footer of the following pages (MuseScore shows them from its page style, not from frames).
 *
 * @author patrick
 */
public final class MuseScorePageTextWriter {
    private static final String TEXT = "text";
    private static final String PAGE_NUMBER = "page number";
    private static final String PAGE_NUMBER_MACRO = "$p";
    private static final String COPYRIGHT_FIRST_PAGE_MACRO = "$C";
    private static final String PART_NAME_NOT_FIRST_PAGE_MACRO = "$i";
    private static final String MACRO = "$";
    private static final String MACRO_ESCAPED = "$$";
    private static final String[] FOOTER_SLOTS = {"C", "L", "R"};
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String BOTTOM = "bottom";
    private static final String HEADER = "Header";
    private static final String FOOTER = "Footer";
    private static final String[] SLOTS = {"L", "C", "R"};
    private static final int PAGE_PARITY = 2;
    private static final int HALF_PAGE_HEIGHT = 877;
    private static final double MIN_FRAME_HEIGHT = 10.0;
    private static final double FRAME_MARGIN = 10.0;
    private static final double TENTHS_PER_SPACE = 10.0;
    private static final double TENTHS_PER_POINT = 2.0;
    private static final double LINE_SPACING = 1.2;
    private static final double DEFAULT_FONT_SIZE = 10.0;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MuseScorePageTextWriter INSTANCE = new MuseScorePageTextWriter();
    }


    /**
     * Constructor.
     */
    private MuseScorePageTextWriter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MuseScorePageTextWriter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Writes the header and footer of the pages after the first page as page style. The texts of the second
     * and third page are taken (left and right pages); the copyright stays in the footer of the first page.
     * MuseScore shows the footer of the first page with the same texts as the other pages, so the footer text of
     * the other pages is shown by the part name macro (all pages except the first) and returned as part name.
     *
     * @param out the output
     * @param score the score
     * @return the part name (the footer text of the pages after the first page), or null
     */
    String writeStyle(MuseScoreXmlOutput out, IScore score) {
        Map<String, String> header = new LinkedHashMap<>();
        Map<String, String> footer = new LinkedHashMap<>();
        String[] headerFont = new String[2];
        String[] footerFont = new String[2];
        boolean rights = false;
        for (ICredit credit : nonNull(score.getCredits())) {
            ICreditWords words = credit.getCreditWords();
            if (words == null || credit.getPage() == null) {
                continue;
            }
            if (credit.getPage() == 1) {
                rights = rights || "rights".equals(credit.getCreditType());
                continue;
            }
            String parity = "odd";
            if (credit.getPage() % PAGE_PARITY == 0) {
                parity = "even";
            }
            String slot = slot(words.getJustify());
            if (PAGE_NUMBER.equals(credit.getCreditType())) {
                header.putIfAbsent(parity + HEADER + slot, PAGE_NUMBER_MACRO);
            } else if (isBottom(words)) {
                putBoth(footer, FOOTER, slot, plainText(credit));
                setFont(footerFont, words);
            } else {
                putBoth(header, HEADER, slot, plainText(credit));
                setFont(headerFont, words);
            }
        }
        if (header.isEmpty() && footer.isEmpty()) {
            return null;
        }
        String partName = null;
        if (rights) {
            // the copyright only on the first page: MuseScore shows the footer of the first page with the texts of
            // the other pages, so one footer text (the center one first) is shown by the part name macro (not on the
            // first page); MuseScore has only one part name, further footer texts are shown on the first page too
            for (String slot : FOOTER_SLOTS) {
                String text = footer.get("odd" + FOOTER + slot);
                if (text != null) {
                    partName = text.replace(MACRO_ESCAPED, MACRO);
                    footer.put("odd" + FOOTER + slot, PART_NAME_NOT_FIRST_PAGE_MACRO);
                    footer.put("even" + FOOTER + slot, PART_NAME_NOT_FIRST_PAGE_MACRO);
                    break;
                }
            }
            String center = COPYRIGHT_FIRST_PAGE_MACRO;
            if (PART_NAME_NOT_FIRST_PAGE_MACRO.equals(footer.get("oddFooterC"))) {
                center = center + "\n" + PART_NAME_NOT_FIRST_PAGE_MACRO;
            }
            footer.put("oddFooterC", center);
            footer.put("evenFooterC", center);
        }

        out.open("Style");
        out.leaf("showHeader", "1");
        out.leaf("headerFirstPage", "0");
        out.leaf("headerOddEven", "1");
        writeSlots(out, header, HEADER);
        out.leaf("showFooter", "1");
        out.leaf("footerFirstPage", String.valueOf(boolValue(rights)));
        out.leaf("footerOddEven", "1");
        writeSlots(out, footer, FOOTER);
        writeFont(out, "header", headerFont);
        writeFont(out, "footer", footerFont);
        out.close();
        return partName;
    }


    /**
     * Writes the title frame with the texts of the first page, or with the title and the composer if the score has
     * no texts for the first page.
     *
     * @param out the output
     * @param score the score
     */
    void writeTitleFrame(MuseScoreXmlOutput out, IScore score) {
        List<String[]> texts = new ArrayList<>();
        for (ICredit credit : nonNull(score.getCredits())) {
            String style = frameStyle(credit);
            if (style != null) {
                texts.add(new String[] {style, credit.getCreditWords().getJustify(), richText(out, credit)});
            }
        }
        if (texts.isEmpty()) {
            String title = score.getMovementTitle();
            if (title == null && score.getWork() != null) {
                title = score.getWork().getWorkTitle();
            }
            if (title != null) {
                texts.add(new String[] {"title", null, out.escape(title)});
            }
            if (score.getIdentification() != null) {
                for (ICreator creator : nonNull(score.getIdentification().getCreators())) {
                    if ("composer".equals(creator.getType()) && creator.getValue() != null) {
                        texts.add(new String[] {"composer", null, out.escape(creator.getValue())});
                    }
                }
            }
        }
        if (texts.isEmpty()) {
            return;
        }
        out.open("VBox");
        out.leaf("height", String.valueOf(frameHeight(score)));
        for (String[] text : texts) {
            out.open("Text");
            out.leaf("style", text[0]);
            if ("poet".equals(text[0]) && LEFT.equals(text[1])) {
                out.leaf("align", "left,top");
            }
            out.openInline(TEXT);
            out.raw(text[2]);
            out.closeInline(TEXT);
            out.close();
        }
        out.close();
    }


    /**
     * Gets the MuseScore text style of a text of the first page.
     *
     * @param credit the credit
     * @return the style, or null if the text is not shown in the title frame
     */
    private String frameStyle(ICredit credit) {
        ICreditWords words = credit.getCreditWords();
        if (words == null || credit.getPage() == null || credit.getPage() != 1) {
            return null;
        }
        String type = credit.getCreditType();
        if ("rights".equals(type) || PAGE_NUMBER.equals(type)) {
            return null;
        }
        if ("title".equals(type) || "subtitle".equals(type) || "composer".equals(type)) {
            return type;
        }
        if ("arranger".equals(type)) {
            return "composer";
        }
        if ("lyricist".equals(type) || LEFT.equals(words.getJustify())) {
            return "poet";
        }
        if (RIGHT.equals(words.getJustify())) {
            return "composer";
        }
        if (isBottom(words)) {
            return null;
        }
        return "subtitle";
    }


    /**
     * Calculates the height of the title frame in staff spaces from the texts of the first page: from the top of
     * the highest text to the bottom of the lowest text, with a margin to the first system.
     *
     * @param score the score
     * @return the height in staff spaces
     */
    private double frameHeight(IScore score) {
        double top = Double.NaN;
        double bottom = Double.NaN;
        for (ICredit credit : nonNull(score.getCredits())) {
            if (frameStyle(credit) == null) {
                continue;
            }
            double y = parse(credit.getCreditWords().getDefaultY(), Double.NaN);
            if (Double.isNaN(y)) {
                continue;
            }
            double height = 0;
            for (ICreditWords words : runs(credit)) {
                int lines = 0;
                if (words.getValue() != null) {
                    lines = words.getValue().split("\n", -1).length - 1;
                }
                if (height == 0) {
                    lines++;
                }
                height += lines * parse(words.getFontSize(), DEFAULT_FONT_SIZE) * LINE_SPACING * TENTHS_PER_POINT;
            }
            if (Double.isNaN(top) || y > top) {
                top = y;
            }
            if (Double.isNaN(bottom) || y - height < bottom) {
                bottom = y - height;
            }
        }
        if (Double.isNaN(top)) {
            return MIN_FRAME_HEIGHT;
        }
        return Math.max(MIN_FRAME_HEIGHT, Math.round((top - bottom) / TENTHS_PER_SPACE + FRAME_MARGIN));
    }


    /**
     * Parses a number.
     *
     * @param text the text (may be null)
     * @param defaultValue the value if the text is not a number
     * @return the number
     */
    private double parse(String text, double defaultValue) {
        if (text == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * Gets the words of a credit.
     *
     * @param credit the credit
     * @return the words
     */
    private List<? extends ICreditWords> runs(ICredit credit) {
        List<? extends ICreditWords> runs = credit.getCreditWordsList();
        if (runs == null) {
            runs = Collections.singletonList(credit.getCreditWords());
        }
        return runs;
    }


    /**
     * Converts the words of a credit into MuseScore rich text: font size, font face, bold, italic and line breaks.
     *
     * @param out the output (for escaping)
     * @param credit the credit
     * @return the rich text
     */
    private String richText(MuseScoreXmlOutput out, ICredit credit) {
        StringBuilder sb = new StringBuilder();
        for (ICreditWords words : runs(credit)) {
            if (words.getValue() == null) {
                continue;
            }
            if (words.getFontSize() != null) {
                sb.append("<font size=\"").append(out.escape(words.getFontSize())).append("\"/>");
            }
            if (words.getFontFamily() != null) {
                sb.append("<font face=\"").append(out.escape(words.getFontFamily())).append("\"/>");
            }
            boolean bold = "bold".equals(words.getFontWeight());
            boolean italic = "italic".equals(words.getFontStyle());
            if (bold) {
                sb.append("<b>");
            }
            if (italic) {
                sb.append("<i>");
            }
            // line breaks stay newlines: MuseScore reads them in this format version, but drops <br/> in frames
            sb.append(out.escape(words.getValue()).replace("\r", ""));
            if (italic) {
                sb.append("</i>");
            }
            if (bold) {
                sb.append("</b>");
            }
        }
        return sb.toString();
    }


    /**
     * Gets the plain text of a credit (all its words).
     *
     * @param credit the credit
     * @return the text
     */
    private String plainText(ICredit credit) {
        StringBuilder sb = new StringBuilder();
        for (ICreditWords words : runs(credit)) {
            if (words.getValue() != null) {
                sb.append(words.getValue());
            }
        }
        return sb.toString().replace("\r", "").trim();
    }


    /**
     * Puts a text into the slot of the left and the right pages, if the slot is still empty. MuseScore reads a $ in
     * the header and the footer as start of a macro, so a $ of the text is written as $$.
     *
     * @param slots the slots (updated)
     * @param area the area: Header or Footer
     * @param slot the slot: L, C or R
     * @param text the text
     */
    private void putBoth(Map<String, String> slots, String area, String slot, String text) {
        if (text.isEmpty()) {
            return;
        }
        String escaped = text.replace(MACRO, MACRO_ESCAPED);
        slots.putIfAbsent("odd" + area + slot, escaped);
        slots.putIfAbsent("even" + area + slot, escaped);
    }


    /**
     * Writes the six slots of the header or the footer.
     *
     * @param out the output
     * @param slots the slots
     * @param area the area: Header or Footer
     */
    private void writeSlots(MuseScoreXmlOutput out, Map<String, String> slots, String area) {
        for (String parity : new String[] {"even", "odd"}) {
            for (String slot : SLOTS) {
                String name = parity + area + slot;
                String value = slots.get(name);
                if (value == null) {
                    value = "";
                }
                out.leaf(name, value);
            }
        }
    }


    /**
     * Writes the font of the header or the footer, if one is known.
     *
     * @param out the output
     * @param prefix the prefix: header or footer
     * @param font the font face and size
     */
    private void writeFont(MuseScoreXmlOutput out, String prefix, String[] font) {
        if (font[0] != null) {
            out.leaf(prefix + "FontFace", font[0]);
        }
        if (font[1] != null) {
            out.leaf(prefix + "FontSize", font[1]);
        }
    }


    /**
     * Takes the font of the first text of the header or the footer.
     *
     * @param font the font face and size (updated)
     * @param words the words
     */
    private void setFont(String[] font, ICreditWords words) {
        if (font[0] == null && font[1] == null) {
            font[0] = words.getFontFamily();
            font[1] = words.getFontSize();
        }
    }


    /**
     * Gets the slot of a justification.
     *
     * @param justify the justification (may be null)
     * @return L, C or R
     */
    private String slot(String justify) {
        if (LEFT.equals(justify)) {
            return "L";
        }
        if (RIGHT.equals(justify)) {
            return "R";
        }
        return "C";
    }


    /**
     * Checks whether a text is at the bottom of the page.
     *
     * @param words the words
     * @return true at the bottom
     */
    private boolean isBottom(ICreditWords words) {
        if (BOTTOM.equals(words.getValign())) {
            return true;
        }
        if (words.getValign() == null && words.getDefaultY() != null) {
            try {
                return Double.parseDouble(words.getDefaultY()) < HALF_PAGE_HEIGHT;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }


    /**
     * Converts a flag into the MuseScore style value.
     *
     * @param value the flag
     * @return 1 or 0
     */
    private int boolValue(boolean value) {
        if (value) {
            return 1;
        }
        return 0;
    }


    /**
     * Returns the list, or an empty list for null.
     *
     * @param <T> the element type
     * @param list the list
     * @return the list
     */
    private static <T> List<T> nonNull(List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }
}
