/*
 * MuseScorePageTextReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import com.github.toolarium.music.model.layout.impl.PageLayoutImpl;
import com.github.toolarium.music.model.layout.impl.PageMarginsImpl;
import com.github.toolarium.music.model.score.impl.CreditImpl;
import com.github.toolarium.music.model.score.impl.CreditWordsImpl;
import com.github.toolarium.music.model.score.impl.DefaultsImpl;
import com.github.toolarium.music.model.score.impl.ScalingImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Reads the page texts of a MuseScore score as credits: the texts of the title frame become the credits of the
 * first page, the header and footer of the page style (with their macros, for example $p for the page number)
 * become the credits of every page. MuseScore stores no page count, so the pages are estimated from the number of
 * staves and bars. The page size and margins of the page style become the defaults of the score, and the positions
 * of the credits follow them.
 *
 * @author patrick
 */
public final class MuseScorePageTextReader {
    private static final int BARS_PER_SYSTEM = 4;
    private static final int STAVES_PER_PAGE = 10;
    private static final int TITLE_OFFSET = 19;
    private static final int CREDIT_LINE = 72;
    private static final double TENTHS_PER_STAFF = 40;
    private static final double SPATIUMS_PER_STAFF = 4;
    private static final double TENTHS_PER_SPATIUM = 10;
    private static final double MILLIMETERS_PER_INCH = 25.4;
    private static final double MAX_PAGE_INCHES = 100;
    private static final double MIN_SPATIUM = 0.1;
    private static final double MAX_SPATIUM = 10;
    private static final int WIDTH = 0;
    private static final int HEIGHT = 1;
    private static final int LEFT_MARGIN = 2;
    private static final int RIGHT_MARGIN = 3;
    private static final int TOP_MARGIN = 4;
    private static final int BOTTOM_MARGIN = 5;
    private static final int FONT_FIELDS = 4;
    private static final int FONT_SIZE = 0;
    private static final int FONT_FACE = 1;
    private static final int FONT_WEIGHT = 2;
    private static final int FONT_STYLE = 3;
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String CENTER = "center";
    private static final String HEADER = "Header";
    private static final String FOOTER = "Footer";
    private static final String TRUE = "1";
    private static final String PAGE_NUMBER = "page number";
    private static final String RIGHTS = "rights";
    private static final Pattern MACRO = Pattern.compile("\\$(:([^:]*):|.)");
    private static final Map<String, String> DEFAULT_STYLE = createDefaultStyle();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MuseScorePageTextReader INSTANCE = new MuseScorePageTextReader();
    }


    /**
     * Constructor.
     */
    private MuseScorePageTextReader() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MuseScorePageTextReader getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Reads the credits of a score: the title frame of the first page, the header and footer of every page.
     *
     * @param firstStaff the content of the first staff (its first frame is the title frame), or null
     * @param style the Style element (of the score or of the style file), or null for the default style
     * @param metaTags the meta tags (copyright, part name, ...)
     * @param staves the number of staves
     * @param bars the number of bars
     * @return the credits, empty if the score has no page texts
     */
    List<CreditImpl> readCredits(Element firstStaff, Element style, Map<String, String> metaTags, int staves, int bars) {
        Map<String, String> values = readStyle(style);
        int[] page = pageGeometry(values);
        List<CreditImpl> result = new ArrayList<>();
        readTitleFrame(firstStaff, metaTags, page, result);
        int barsPerPage = BARS_PER_SYSTEM * Math.max(1, STAVES_PER_PAGE / Math.max(1, staves));
        int pages = Math.max(1, (bars + barsPerPage - 1) / barsPerPage);
        for (int number = 1; number <= pages; number++) {
            readPageArea(values, HEADER, new int[] {number, pages}, metaTags, page, result);
            readPageArea(values, FOOTER, new int[] {number, pages}, metaTags, page, result);
        }
        return result;
    }


    /**
     * Reads the page size and margins of the page style as defaults of the score (in tenths: a staff is 40 tenths).
     *
     * @param style the Style element (of the score or of the style file), or null for the default style
     * @return the defaults
     */
    DefaultsImpl readDefaults(Element style) {
        Map<String, String> values = readStyle(style);
        final int[] page = pageGeometry(values);
        ScalingImpl scaling = new ScalingImpl();
        scaling.setMillimeters(spatium(values) * SPATIUMS_PER_STAFF);
        scaling.setTenths(TENTHS_PER_STAFF);
        PageMarginsImpl margins = new PageMarginsImpl();
        margins.setType("both");
        margins.setLeftMargin((double) page[LEFT_MARGIN]);
        margins.setRightMargin((double) page[RIGHT_MARGIN]);
        margins.setTopMargin((double) page[TOP_MARGIN]);
        margins.setBottomMargin((double) page[BOTTOM_MARGIN]);
        PageLayoutImpl layout = new PageLayoutImpl();
        layout.setPageWidth((double) page[WIDTH]);
        layout.setPageHeight((double) page[HEIGHT]);
        layout.setPageMargins(Collections.singletonList(margins));
        DefaultsImpl defaults = new DefaultsImpl();
        defaults.setScaling(scaling);
        defaults.setPageLayout(layout);
        return defaults;
    }


    /**
     * Gets the page size and margins (of the odd pages) in tenths: width, height, left, right, top and bottom.
     *
     * @param style the style values
     * @return the page geometry
     */
    private int[] pageGeometry(Map<String, String> style) {
        double tenthsPerInch = MILLIMETERS_PER_INCH * TENTHS_PER_SPATIUM / spatium(style);
        double width = number(style, "pageWidth", MAX_PAGE_INCHES);
        double height = number(style, "pageHeight", MAX_PAGE_INCHES);
        double left = Math.min(number(style, "pageOddLeftMargin", MAX_PAGE_INCHES), width / 2);
        double right = Math.max(0, Math.min(width - left - number(style, "pagePrintableWidth", MAX_PAGE_INCHES), width / 2));
        double top = Math.min(number(style, "pageOddTopMargin", MAX_PAGE_INCHES), height / 2);
        double bottom = Math.min(number(style, "pageOddBottomMargin", MAX_PAGE_INCHES), height / 2);
        return new int[] {(int) Math.round(width * tenthsPerInch), (int) Math.round(height * tenthsPerInch),
            (int) Math.round(left * tenthsPerInch), (int) Math.round(right * tenthsPerInch),
            (int) Math.round(top * tenthsPerInch), (int) Math.round(bottom * tenthsPerInch)};
    }


    /**
     * Gets the staff space of the style in millimeters.
     *
     * @param style the style values
     * @return the staff space
     */
    private double spatium(Map<String, String> style) {
        double spatium = number(style, "spatium", MAX_SPATIUM);
        if (spatium < MIN_SPATIUM) {
            spatium = Double.parseDouble(DEFAULT_STYLE.get("spatium"));
        }
        return spatium;
    }


    /**
     * Gets a number of the style: a value that is no number or outside 0 to the maximum gets MuseScore's default.
     *
     * @param style the style values
     * @param name the name
     * @param max the largest valid value
     * @return the number
     */
    private double number(Map<String, String> style, String name, double max) {
        double result;
        try {
            result = Double.parseDouble(style.get(name).trim());
        } catch (RuntimeException e) {
            result = -1;
        }
        if (Double.isNaN(result) || result < 0 || result > max) {
            result = Double.parseDouble(DEFAULT_STYLE.get(name));
        }
        return result;
    }


    /**
     * Reads the texts of the title frame (the first frame before the first measure) as credits of the first page.
     *
     * @param firstStaff the content of the first staff, or null
     * @param metaTags the meta tags
     * @param page the page geometry
     * @param result the credits (filled)
     */
    private void readTitleFrame(Element firstStaff, Map<String, String> metaTags, int[] page, List<CreditImpl> result) {
        if (firstStaff == null) {
            return;
        }
        for (Node node = firstStaff.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element) node;
            if ("Measure".equals(element.getTagName())) {
                return;
            }
            if ("VBox".equals(element.getTagName())) {
                for (Element text : children(element, "Text")) {
                    addFrameText(text, metaTags, page, result);
                }
                return;
            }
        }
    }


    /**
     * Adds a text of the title frame as credit: its style (title, subtitle, composer, poet) gives the type and the
     * position, its markup the font.
     *
     * @param text the Text element
     * @param metaTags the meta tags
     * @param page the page geometry
     * @param result the credits (filled)
     */
    private void addFrameText(Element text, Map<String, String> metaTags, int[] page, List<CreditImpl> result) {
        Element content = child(text, "text");
        if (content == null) {
            return;
        }
        List<CreditWordsImpl> runs = runs(content);
        if (runs.isEmpty()) {
            return;
        }
        StringBuilder all = new StringBuilder();
        for (CreditWordsImpl run : runs) {
            all.append(run.getValue());
        }
        String value = all.toString().trim();
        String style = textOf(child(text, "style")).toLowerCase();
        if (style.isEmpty()) {
            style = textOf(child(text, "subtype")).toLowerCase();
        }

        String type = null;
        String justify = CENTER;
        int x = page[WIDTH] / 2;
        int y = page[HEIGHT] - page[TOP_MARGIN] - TITLE_OFFSET;
        int top = y;
        if ("title".equals(style)) {
            type = "title";
        } else if ("subtitle".equals(style)) {
            type = "subtitle";
            y = top - CREDIT_LINE;
        } else if ("composer".equals(style)) {
            type = "composer";
            justify = RIGHT;
            x = page[WIDTH] - page[RIGHT_MARGIN];
            y = top - CREDIT_LINE;
        } else if ("poet".equals(style) || "lyricist".equals(style)) {
            // MuseScore writes a left text of the first page as poet: only the text of the lyricist is one
            if (value.equals(metaTags.get("lyricist"))) {
                type = "lyricist";
            }
            justify = LEFT;
            x = page[LEFT_MARGIN];
        } else {
            y = top - 2 * CREDIT_LINE;
        }

        CreditWordsImpl first = runs.get(0);
        first.setDefaultX(String.valueOf(x));
        first.setDefaultY(String.valueOf(y));
        first.setJustify(justify);
        first.setValign("top");
        CreditImpl credit = createCredit(1, type, first);
        credit.setCreditWordsList(runs);
        result.add(credit);
    }


    /**
     * Reads the header or the footer of a page: the left, center and right text, with the macros expanded.
     *
     * @param style the style values
     * @param area the area: Header or Footer
     * @param pageNumbers the page (1-based) and the number of pages
     * @param metaTags the meta tags
     * @param geometry the page geometry
     * @param result the credits (filled)
     */
    private void readPageArea(Map<String, String> style, String area, int[] pageNumbers, Map<String, String> metaTags, int[] geometry, List<CreditImpl> result) {
        int page = pageNumbers[0];
        final int pages = pageNumbers[1];
        String prefix = area.toLowerCase();
        if (!TRUE.equals(style.get("show" + area)) || (page == 1 && !TRUE.equals(style.get(prefix + "FirstPage")))) {
            return;
        }
        String parity = "odd";
        if (page % 2 == 0 && TRUE.equals(style.get(prefix + "OddEven"))) {
            parity = "even";
        }
        int y = geometry[HEIGHT] - geometry[TOP_MARGIN];
        String valign = "top";
        if (FOOTER.equals(area)) {
            y = geometry[BOTTOM_MARGIN];
            valign = "bottom";
        }
        String[][] slots = {{"L", LEFT, String.valueOf(geometry[LEFT_MARGIN])}, {"C", CENTER, String.valueOf(geometry[WIDTH] / 2)},
            {"R", RIGHT, String.valueOf(geometry[WIDTH] - geometry[RIGHT_MARGIN])}};
        for (String[] slot : slots) {
            String template = style.get(parity + area + slot[0]);
            if (template == null || template.trim().isEmpty()) {
                continue;
            }
            String[] expanded = expand(template, page, pages, metaTags);
            String value = expanded[0].trim();
            if (value.isEmpty()) {
                continue;
            }
            CreditWordsImpl words = createWords(value, Integer.parseInt(slot[2]), y, slot[1], valign);
            words.setFontFamily(style.get(prefix + "FontFace"));
            words.setFontSize(style.get(prefix + "FontSize"));
            result.add(createCredit(page, expanded[1], words));
        }
    }


    /**
     * Expands the macros of a header or footer text for a page.
     *
     * @param template the text with macros
     * @param page the page (1-based)
     * @param pages the number of pages
     * @param metaTags the meta tags
     * @return the text and the credit type (page number, rights or null)
     */
    private String[] expand(String template, int page, int pages, Map<String, String> metaTags) {
        StringBuilder text = new StringBuilder();
        String type = null;
        Matcher matcher = MACRO.matcher(template);
        int last = 0;
        while (matcher.find()) {
            text.append(template, last, matcher.start());
            last = matcher.end();
            String value = null;
            switch (matcher.group(1).charAt(0)) {
                case 'p':
                    if (page > 1) {
                        value = String.valueOf(page);
                        type = PAGE_NUMBER;
                    }
                    break;
                case 'N':
                    if (pages > 1) {
                        value = String.valueOf(page);
                        type = PAGE_NUMBER;
                    }
                    break;
                case 'P':
                    value = String.valueOf(page);
                    type = PAGE_NUMBER;
                    break;
                case 'n':
                    value = String.valueOf(pages);
                    break;
                case 'C':
                    if (page == 1) {
                        value = metaTags.get("copyright");
                        type = RIGHTS;
                    }
                    break;
                case 'c':
                    value = metaTags.get("copyright");
                    type = RIGHTS;
                    break;
                case 'i':
                    if (page > 1) {
                        value = metaTags.get("partName");
                    }
                    break;
                case 'I':
                    value = metaTags.get("partName");
                    break;
                case ':':
                    value = metaTags.get(matcher.group(2));
                    break;
                case '$':
                    value = "$";
                    break;
                default:
                    break;
            }
            if (value != null) {
                text.append(value);
            }
        }
        text.append(template.substring(last));
        return new String[] {text.toString(), type};
    }


    /**
     * Reads the values of the page style; a value that is not in the style keeps MuseScore's default.
     *
     * @param style the Style element, or null
     * @return the values by name
     */
    private Map<String, String> readStyle(Element style) {
        Map<String, String> result = new HashMap<>(DEFAULT_STYLE);
        if (style != null) {
            for (Node node = style.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element) {
                    result.put(((Element) node).getTagName(), node.getTextContent());
                }
            }
        }
        return result;
    }


    /**
     * Splits a text markup into runs of the same font (size, face, bold, italic): the line breaks (br) are kept.
     * The first run starts without white space, the last run ends without it.
     *
     * @param content the text element with its markup
     * @return the runs, empty if the text is empty
     */
    private List<CreditWordsImpl> runs(Element content) {
        List<CreditWordsImpl> result = new ArrayList<>();
        String[] font = new String[FONT_FIELDS];
        StringBuilder text = new StringBuilder();
        collectRuns(content, font, text, result);
        finishRun(font, text, result);
        while (!result.isEmpty() && result.get(0).getValue().trim().isEmpty()) {
            result.remove(0);
        }
        while (!result.isEmpty() && result.get(result.size() - 1).getValue().trim().isEmpty()) {
            result.remove(result.size() - 1);
        }
        if (!result.isEmpty()) {
            CreditWordsImpl first = result.get(0);
            first.setValue(first.getValue().replaceAll("^\\s+", ""));
            CreditWordsImpl last = result.get(result.size() - 1);
            last.setValue(last.getValue().replaceAll("\\s+$", ""));
        }
        return result;
    }


    /**
     * Collects the runs of a node and its children.
     *
     * @param node the node
     * @param font the current font: size, face, weight and style (updated)
     * @param text the text of the current run (updated)
     * @param result the runs (filled)
     */
    private void collectRuns(Node node, String[] font, StringBuilder text, List<CreditWordsImpl> result) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(child.getNodeValue().replace('\u00A0', ' '));
            } else if (child instanceof Element) {
                Element element = (Element) child;
                String tag = element.getTagName();
                if ("br".equals(tag)) {
                    text.append('\n');
                } else if ("font".equals(tag)) {
                    setFont(font, FONT_SIZE, element.getAttribute("size"), text, result);
                    setFont(font, FONT_FACE, element.getAttribute("face"), text, result);
                } else if ("b".equals(tag) || "i".equals(tag)) {
                    int field = FONT_WEIGHT;
                    String value = "bold";
                    if ("i".equals(tag)) {
                        field = FONT_STYLE;
                        value = "italic";
                    }
                    String before = font[field];
                    setFont(font, field, value, text, result);
                    collectRuns(element, font, text, result);
                    if (before == null) {
                        finishRun(font, text, result);
                    }
                    font[field] = before;
                } else {
                    collectRuns(element, font, text, result);
                }
            }
        }
    }


    /**
     * Changes a field of the current font: a run with text in the old font ends.
     *
     * @param font the current font (updated)
     * @param field the field
     * @param value the new value (empty keeps the field)
     * @param text the text of the current run (updated)
     * @param result the runs (filled)
     */
    private void setFont(String[] font, int field, String value, StringBuilder text, List<CreditWordsImpl> result) {
        if (value == null || value.isEmpty() || value.equals(font[field])) {
            return;
        }
        finishRun(font, text, result);
        font[field] = value;
    }


    /**
     * Ends the current run: its text becomes credit words with the current font.
     *
     * @param font the current font
     * @param text the text of the run (cleared)
     * @param result the runs (filled)
     */
    private void finishRun(String[] font, StringBuilder text, List<CreditWordsImpl> result) {
        if (text.length() == 0) {
            return;
        }
        CreditWordsImpl words = new CreditWordsImpl();
        words.setValue(text.toString());
        words.setFontSize(font[FONT_SIZE]);
        words.setFontFamily(font[FONT_FACE]);
        words.setFontWeight(font[FONT_WEIGHT]);
        words.setFontStyle(font[FONT_STYLE]);
        result.add(words);
        text.setLength(0);
    }


    /**
     * Creates the words of a credit.
     *
     * @param value the text
     * @param x the horizontal position in tenths
     * @param y the vertical position in tenths
     * @param justify the justification
     * @param valign the vertical alignment
     * @return the credit words
     */
    private CreditWordsImpl createWords(String value, int x, int y, String justify, String valign) {
        CreditWordsImpl words = new CreditWordsImpl();
        words.setValue(value);
        words.setDefaultX(String.valueOf(x));
        words.setDefaultY(String.valueOf(y));
        words.setJustify(justify);
        words.setValign(valign);
        return words;
    }


    /**
     * Creates a credit.
     *
     * @param page the page (1-based)
     * @param type the credit type, or null
     * @param words the words
     * @return the credit
     */
    private CreditImpl createCredit(int page, String type, CreditWordsImpl words) {
        CreditImpl credit = new CreditImpl();
        credit.setPage(page);
        if (type != null) {
            credit.setCreditType(type);
        }
        credit.setCreditWords(words);
        return credit;
    }


    /**
     * Gets the first child element with a name.
     *
     * @param parent the parent
     * @param name the name
     * @return the child, or null
     */
    private Element child(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && name.equals(((Element) node).getTagName())) {
                return (Element) node;
            }
        }
        return null;
    }


    /**
     * Gets the child elements with a name.
     *
     * @param parent the parent
     * @param name the name
     * @return the children
     */
    private List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && name.equals(((Element) node).getTagName())) {
                result.add((Element) node);
            }
        }
        return result;
    }


    /**
     * Gets the text of an element.
     *
     * @param element the element, or null
     * @return the trimmed text, empty if there is none
     */
    private String textOf(Element element) {
        if (element == null) {
            return "";
        }
        return element.getTextContent().trim();
    }


    /**
     * Creates MuseScore's default page style: the page number in the header of the pages after the first page,
     * the copyright in the footer of the first page.
     *
     * @return the default values by name
     */
    private static Map<String, String> createDefaultStyle() {
        Map<String, String> result = new HashMap<>();
        result.put("showHeader", TRUE);
        result.put("headerFirstPage", "0");
        result.put("headerOddEven", TRUE);
        result.put("evenHeaderL", "$p");
        result.put("oddHeaderR", "$p");
        result.put("showFooter", TRUE);
        result.put("footerFirstPage", TRUE);
        result.put("footerOddEven", TRUE);
        result.put("evenFooterC", "$C");
        result.put("oddFooterC", "$C");
        result.put("pageWidth", "8.27");
        result.put("pageHeight", "11.69");
        result.put("pagePrintableWidth", "7.0889");
        result.put("pageOddLeftMargin", "0.590551");
        result.put("pageOddTopMargin", "0.590551");
        result.put("pageOddBottomMargin", "0.590551");
        result.put("spatium", "1.75");
        return result;
    }
}
