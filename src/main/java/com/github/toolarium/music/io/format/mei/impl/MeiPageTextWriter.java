/*
 * MeiPageTextWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl;

import com.github.toolarium.music.model.layout.IPageLayout;
import com.github.toolarium.music.model.layout.IPageMargins;
import com.github.toolarium.music.model.score.ICredit;
import com.github.toolarium.music.model.score.ICreditWords;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.CreditWordsImpl;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Writes the page texts of a score (its credits) as MEI page headers and footers: the texts of the first page with
 * func="first", the texts of the following pages with func="all", or alternating (alt1 for the even pages, alt2 for
 * the odd pages) if they differ. A page number becomes num label="page". The page size and margins are written in
 * virtual units (half a staff space, 5 tenths) on the score definition.
 *
 * @author patrick
 */
public final class MeiPageTextWriter {
    /** The credit type of a page number. */
    static final String PAGE_NUMBER = "page number";

    /** The label of a page number element. */
    static final String PAGE_LABEL = "page";

    /** Tenths per virtual unit (a virtual unit is half a staff space, a staff space is 10 tenths). */
    static final int TENTHS_PER_VU = 5;

    private static final String PG_HEAD = "pgHead";
    private static final String PG_FOOT = "pgFoot";
    private static final String FUNC = "func";
    private static final String REND = "rend";
    private static final String TOP = "top";
    private static final String BOTTOM = "bottom";
    private static final String VU = "vu";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MeiPageTextWriter INSTANCE = new MeiPageTextWriter();
    }


    /**
     * Constructor.
     */
    private MeiPageTextWriter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MeiPageTextWriter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the number of pages: the highest page of a credit.
     *
     * @param score the score
     * @return the number of pages, or 0 without page texts
     */
    int pageCount(IScore score) {
        int result = 0;
        for (ICredit credit : credits(score)) {
            result = Math.max(result, page(credit));
        }
        return result;
    }


    /**
     * Gets the page attributes of the score definition: the size of a virtual unit, the page size and margins.
     *
     * @param score the score
     * @return the attribute names and values
     */
    List<String> pageAttributes(IScore score) {
        List<String> result = new ArrayList<>();
        if (score.getDefaults() == null) {
            return result;
        }
        if (score.getDefaults().getScaling() != null && score.getDefaults().getScaling().getMillimeters() != null
                && score.getDefaults().getScaling().getTenths() != null && score.getDefaults().getScaling().getTenths() > 0) {
            double vuMillimeters = score.getDefaults().getScaling().getMillimeters() / score.getDefaults().getScaling().getTenths() * TENTHS_PER_VU;
            result.add("vu.height");
            result.add(number(vuMillimeters, 4) + "mm");
        }
        IPageLayout layout = score.getDefaults().getPageLayout();
        if (layout == null || layout.getPageHeight() == null || layout.getPageWidth() == null) {
            return result;
        }
        result.add("page.height");
        result.add(vu(layout.getPageHeight()));
        result.add("page.width");
        result.add(vu(layout.getPageWidth()));
        if (layout.getPageMargins() != null && !layout.getPageMargins().isEmpty()) {
            IPageMargins margins = layout.getPageMargins().get(0);
            addMargin(result, "page.topmar", margins.getTopMargin());
            addMargin(result, "page.botmar", margins.getBottomMargin());
            addMargin(result, "page.leftmar", margins.getLeftMargin());
            addMargin(result, "page.rightmar", margins.getRightMargin());
        }
        return result;
    }


    /**
     * Writes the page headers and footers (inside the score definition, before the staff groups).
     *
     * @param out the output
     * @param score the score
     */
    void write(MeiXmlOutput out, IScore score) {
        Map<Integer, List<ICredit>> pages = new TreeMap<>();
        for (ICredit credit : credits(score)) {
            pages.computeIfAbsent(page(credit), key -> new ArrayList<>()).add(credit);
        }
        if (pages.isEmpty()) {
            return;
        }
        double height = pageHeight(score);
        Map<String, List<ICredit>> functions = new LinkedHashMap<>();
        functions.put("first", pages.get(1));
        List<ICredit> even = pages.get(2);
        List<ICredit> odd = pages.get(3);
        if (even != null && odd != null && !signature(even).equals(signature(odd))) {
            functions.put("alt1", even);
            functions.put("alt2", odd);
        } else if (even != null) {
            functions.put("all", even);
        } else if (odd != null) {
            functions.put("all", odd);
        }
        // the schema expects all page headers before the page footers
        for (boolean head : new boolean[] {true, false}) {
            for (Map.Entry<String, List<ICredit>> entry : functions.entrySet()) {
                writePage(out, entry.getValue(), entry.getKey(), height, head);
            }
        }
    }


    /**
     * Writes the texts of a page in the upper half as page header or the texts in the lower half as page footer.
     *
     * @param out the output
     * @param credits the credits of the page, or null
     * @param func the MEI page function
     * @param height the page height in tenths
     * @param header true for the page header, false for the page footer
     */
    private void writePage(MeiXmlOutput out, List<ICredit> credits, String func, double height, boolean header) {
        if (credits == null) {
            return;
        }
        List<ICredit> head = new ArrayList<>();
        List<ICredit> foot = new ArrayList<>();
        for (ICredit credit : sortedFromTop(credits)) {
            ICreditWords first = words(credit).get(0);
            boolean bottom = BOTTOM.equals(first.getValign());
            double y = number(first.getDefaultY(), height);
            if (bottom || first.getValign() == null && y < height / 2) {
                foot.add(credit);
            } else {
                head.add(credit);
            }
        }
        if (header) {
            writeArea(out, PG_HEAD, func, head);
        } else {
            writeArea(out, PG_FOOT, func, foot);
        }
    }


    /**
     * Writes a page header or footer.
     *
     * @param out the output
     * @param name pgHead or pgFoot
     * @param func the MEI page function
     * @param credits the credits from top to bottom
     */
    private void writeArea(MeiXmlOutput out, String name, String func, List<ICredit> credits) {
        if (credits.isEmpty()) {
            return;
        }
        out.open(name, FUNC, func);
        for (ICredit credit : credits) {
            List<? extends ICreditWords> words = words(credit);
            ICreditWords first = words.get(0);
            List<String> attributes = new ArrayList<>();
            add(attributes, "type", creditType(credit));
            add(attributes, "halign", halign(first.getJustify()));
            add(attributes, "valign", valign(first.getValign(), PG_FOOT.equals(name)));
            out.openInline(REND, attributes.toArray(new String[0]));
            for (ICreditWords part : words) {
                writeWords(out, credit, part);
            }
            out.closeInline(REND);
        }
        out.close();
    }


    /**
     * Writes the words of a credit: a rend with the font, the lines separated by lb, a page number as num.
     *
     * @param out the output
     * @param credit the credit
     * @param words the words
     */
    private void writeWords(MeiXmlOutput out, ICredit credit, ICreditWords words) {
        if (words.getValue() == null) {
            return;
        }
        List<String> attributes = new ArrayList<>();
        add(attributes, "fontfam", trimToNull(words.getFontFamily()));
        add(attributes, "fontsize", fontSize(words.getFontSize()));
        if ("bold".equals(words.getFontWeight())) {
            add(attributes, "fontweight", "bold");
        }
        if ("italic".equals(words.getFontStyle())) {
            add(attributes, "fontstyle", "italic");
        }
        out.raw("<" + REND + attributes(out, attributes) + ">");
        if (PAGE_NUMBER.equals(credit.getCreditType()) && words.getValue().trim().matches("[0-9]+")) {
            out.raw("<num label=\"" + PAGE_LABEL + "\">");
            out.text(words.getValue().trim());
            out.raw("</num>");
        } else {
            String[] lines = words.getValue().split("\r?\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    out.raw("<lb/>");
                }
                out.text(lines[i]);
            }
        }
        out.raw("</" + REND + ">");
    }


    /**
     * Formats attributes for raw output.
     *
     * @param out the output (for the escaping)
     * @param attributes the attribute names and values
     * @return the attributes with a leading space each
     */
    private String attributes(MeiXmlOutput out, List<String> attributes) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i + 1 < attributes.size(); i += 2) {
            result.append(' ').append(attributes.get(i)).append("=\"").append(out.escape(attributes.get(i + 1))).append('"');
        }
        return result.toString();
    }


    /**
     * Gets a signature of the texts of a page without the page number value (to compare even and odd pages).
     *
     * @param credits the credits
     * @return the signature
     */
    private String signature(List<ICredit> credits) {
        StringBuilder result = new StringBuilder();
        for (ICredit credit : sortedFromTop(credits)) {
            for (ICreditWords words : words(credit)) {
                String value = words.getValue();
                if (PAGE_NUMBER.equals(credit.getCreditType())) {
                    value = "#";
                }
                result.append(value).append('|').append(words.getJustify()).append('|').append(words.getDefaultX()).append('|')
                        .append(words.getDefaultY()).append(';');
            }
        }
        return result.toString();
    }


    /**
     * Sorts credits from the top of the page to the bottom.
     *
     * @param credits the credits
     * @return the sorted credits
     */
    private List<ICredit> sortedFromTop(List<ICredit> credits) {
        List<ICredit> result = new ArrayList<>(credits);
        result.sort((a, b) -> Double.compare(number(words(b).get(0).getDefaultY(), 0), number(words(a).get(0).getDefaultY(), 0)));
        return result;
    }


    /**
     * Gets the credits of a score that have words.
     *
     * @param score the score
     * @return the credits
     */
    private List<ICredit> credits(IScore score) {
        List<ICredit> result = new ArrayList<>();
        if (score.getCredits() == null) {
            return result;
        }
        for (ICredit credit : score.getCredits()) {
            boolean hasText = false;
            for (ICreditWords words : words(credit)) {
                hasText = hasText || words.getValue() != null && !words.getValue().trim().isEmpty();
            }
            if (hasText && words(credit).get(0).getDefaultY() != null) {
                result.add(credit);
            }
        }
        return result;
    }


    /**
     * Gets the words of a credit.
     *
     * @param credit the credit
     * @return the words (at least one element, possibly empty)
     */
    private List<? extends ICreditWords> words(ICredit credit) {
        if (credit.getCreditWordsList() != null && !credit.getCreditWordsList().isEmpty()) {
            return credit.getCreditWordsList();
        }
        List<ICreditWords> result = new ArrayList<>();
        if (credit.getCreditWords() != null) {
            result.add(credit.getCreditWords());
        } else {
            result.add(new CreditWordsImpl());
        }
        return result;
    }


    /**
     * Gets the page of a credit.
     *
     * @param credit the credit
     * @return the page (1 if unknown)
     */
    private int page(ICredit credit) {
        if (credit.getPage() == null || credit.getPage() < 1) {
            return 1;
        }
        return credit.getPage();
    }


    /**
     * Gets the credit type as MEI token.
     *
     * @param credit the credit
     * @return the type (for example title or page-number), or null
     */
    private String creditType(ICredit credit) {
        String type = trimToNull(credit.getCreditType());
        if (type == null) {
            return null;
        }
        return type.replaceAll("[^A-Za-z0-9._\\-]+", "-");
    }


    /**
     * Gets the MEI horizontal alignment.
     *
     * @param justify the MusicXML justification, or null
     * @return left, center or right
     */
    private String halign(String justify) {
        if ("right".equals(justify) || "center".equals(justify)) {
            return justify;
        }
        return "left";
    }


    /**
     * Gets the MEI vertical alignment.
     *
     * @param valign the MusicXML vertical alignment, or null
     * @param footer true for a page footer
     * @return top, middle or bottom
     */
    private String valign(String valign, boolean footer) {
        if (TOP.equals(valign) || BOTTOM.equals(valign) || "middle".equals(valign)) {
            return valign;
        }
        if (footer) {
            return BOTTOM;
        }
        return TOP;
    }


    /**
     * Gets the MEI font size of a MusicXML font size (in points).
     *
     * @param fontSize the font size, or null
     * @return the font size with unit, or null
     */
    private String fontSize(String fontSize) {
        double size = number(fontSize, -1);
        if (size <= 0) {
            return null;
        }
        return number(size, 2) + "pt";
    }


    /**
     * Gets the page height in tenths.
     *
     * @param score the score
     * @return the page height, or the height of the default page
     */
    private double pageHeight(IScore score) {
        if (score.getDefaults() != null && score.getDefaults().getPageLayout() != null && score.getDefaults().getPageLayout().getPageHeight() != null) {
            return score.getDefaults().getPageLayout().getPageHeight();
        }
        return MeiPageTextReader.DEFAULT_PAGE[1];
    }


    /**
     * Adds a margin in virtual units.
     *
     * @param result the attributes
     * @param name the attribute name
     * @param margin the margin in tenths, or null
     */
    private void addMargin(List<String> result, String name, Double margin) {
        if (margin != null && margin >= 0) {
            result.add(name);
            result.add(vu(margin));
        }
    }


    /**
     * Formats tenths as virtual units.
     *
     * @param tenths the tenths
     * @return the value with unit
     */
    private String vu(double tenths) {
        return number(Math.max(0, tenths) / TENTHS_PER_VU, 2) + VU;
    }


    /**
     * Formats a number without trailing zeros.
     *
     * @param value the value
     * @param scale the decimals
     * @return the text
     */
    private String number(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }


    /**
     * Parses a number.
     *
     * @param text the text, or null
     * @param defaultValue the value if the text is no number
     * @return the number
     */
    private double number(String text, double defaultValue) {
        if (text == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * Trims a text.
     *
     * @param value the text, or null
     * @return the trimmed text, or null if empty
     */
    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }


    /**
     * Adds an attribute if its value is set.
     *
     * @param attributes the attributes
     * @param name the name
     * @param value the value, or null
     */
    private void add(List<String> attributes, String name, String value) {
        if (value != null) {
            attributes.add(name);
            attributes.add(value);
        }
    }
}
