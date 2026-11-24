/*
 * MeiPageTextReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl;

import com.github.toolarium.music.io.xml.XmlDocumentReader;
import com.github.toolarium.music.model.layout.impl.PageLayoutImpl;
import com.github.toolarium.music.model.layout.impl.PageMarginsImpl;
import com.github.toolarium.music.model.score.impl.CreditImpl;
import com.github.toolarium.music.model.score.impl.CreditWordsImpl;
import com.github.toolarium.music.model.score.impl.DefaultsImpl;
import com.github.toolarium.music.model.score.impl.ScalingImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Reads the page headers and footers of an MEI score (pgHead and pgFoot of the first score definition) as credits:
 * func="first" is the first page, func="all" every page (the following pages if there is a first page header),
 * alt1 and alt2 the even and odd following pages, func="last" the last page. The number of pages comes from the
 * extent of the file description (unit page), otherwise it is estimated from the measures. A text block (rend) is
 * placed by its horizontal and vertical alignment within the page margins; the blocks of a column are stacked.
 *
 * @author patrick
 */
public final class MeiPageTextReader {
    /** The default page in tenths: width, height, left, right, top and bottom margin. */
    static final double[] DEFAULT_PAGE = {1240, 1753, 75, 75, 75, 75};

    private static final int WIDTH = 0;
    private static final int HEIGHT = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;
    private static final int TOP = 4;
    private static final int BOTTOM = 5;
    private static final double TENTHS_PER_STAFF = 40;
    private static final double DEFAULT_STAFF_MILLIMETERS = 7;
    private static final double MILLIMETERS_PER_POINT = 25.4 / 72;
    private static final double LINE_SPACING = 1.2;
    private static final double DEFAULT_FONT_SIZE = 12;
    private static final int VU_PER_STAFF = 8;
    private static final int MEASURES_PER_PAGE = 24;
    private static final String REND = "rend";
    private static final String CENTER = "center";
    private static final String RIGHT_ALIGN = "right";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MeiPageTextReader INSTANCE = new MeiPageTextReader();
    }


    /**
     * Constructor.
     */
    private MeiPageTextReader() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MeiPageTextReader getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Reads the page texts and the page layout into a score: the credits are replaced if the score has page texts.
     *
     * @param root the mei element
     * @param music the music element
     * @param measures the number of measures (for the estimated page count)
     * @param score the score
     */
    void apply(Element root, Element music, int measures, ScoreImpl score) {
        XmlDocumentReader xml = XmlDocumentReader.getInstance();
        List<Element> scoreDefs = xml.descendants(music, "scoreDef");
        if (scoreDefs.isEmpty()) {
            return;
        }
        Element scoreDef = scoreDefs.get(0);
        double staffMillimeters = staffMillimeters(xml.attr(scoreDef, "vu.height"));
        double[] page = geometry(scoreDef, staffMillimeters);
        if (xml.attr(scoreDef, "page.height") != null || xml.attr(scoreDef, "vu.height") != null) {
            score.setDefaults(defaults(page, staffMillimeters));
        }
        List<Element> heads = xml.children(scoreDef, "pgHead");
        List<Element> foots = xml.children(scoreDef, "pgFoot");
        if (heads.isEmpty() && foots.isEmpty()) {
            return;
        }
        int pages = pageCount(root, measures);
        double tenthsPerPoint = MILLIMETERS_PER_POINT * TENTHS_PER_STAFF / staffMillimeters;
        List<CreditImpl> credits = new ArrayList<>();
        readArea(heads, true, page, pages, tenthsPerPoint, credits);
        readArea(foots, false, page, pages, tenthsPerPoint, credits);
        if (!credits.isEmpty()) {
            credits.sort((a, b) -> Integer.compare(a.getPage(), b.getPage()));
            score.setCredits(credits);
        }
    }


    /**
     * Reads the page headers or the page footers.
     *
     * @param areas the pgHead or pgFoot elements
     * @param head true for page headers
     * @param page the page geometry
     * @param pages the number of pages
     * @param tenthsPerPoint the tenths of a point
     * @param credits the credits (filled)
     */
    private void readArea(List<Element> areas, boolean head, double[] page, int pages, double tenthsPerPoint, List<CreditImpl> credits) {
        boolean hasFirst = false;
        for (Element area : areas) {
            hasFirst = hasFirst || "first".equals(XmlDocumentReader.getInstance().attr(area, "func"));
        }
        for (Element area : areas) {
            for (int number : pages(XmlDocumentReader.getInstance().attr(area, "func"), pages, hasFirst)) {
                readBlocks(area, head, number, page, tenthsPerPoint, credits);
            }
        }
    }


    /**
     * Gets the pages of a page function.
     *
     * @param func the MEI page function, or null for all pages
     * @param pages the number of pages
     * @param hasFirst true if a header or footer of the first page exists
     * @return the page numbers
     */
    private List<Integer> pages(String func, int pages, boolean hasFirst) {
        List<Integer> result = new ArrayList<>();
        if ("first".equals(func)) {
            result.add(1);
            return result;
        }
        if ("last".equals(func)) {
            result.add(pages);
            return result;
        }
        int first = 1;
        if (hasFirst) {
            first = 2;
        }
        for (int number = first; number <= pages; number++) {
            boolean even = number % 2 == 0;
            if ("alt1".equals(func) && !even || "alt2".equals(func) && even) {
                continue;
            }
            result.add(number);
        }
        return result;
    }


    /**
     * Reads the text blocks of a page header or footer for a page: each rend child is a credit, placed by its
     * alignment; the blocks of a column are stacked from the top (header) or from the bottom (footer).
     *
     * @param area the pgHead or pgFoot element
     * @param head true for a page header
     * @param number the page number
     * @param page the page geometry
     * @param tenthsPerPoint the tenths of a point
     * @param credits the credits (filled)
     */
    private void readBlocks(Element area, boolean head, int number, double[] page, double tenthsPerPoint, List<CreditImpl> credits) {
        XmlDocumentReader xml = XmlDocumentReader.getInstance();
        List<Element> blocks = new ArrayList<>(xml.children(area, REND));
        if (!head) {
            Collections.reverse(blocks);
        }
        double[] offsets = new double[3];
        for (Element block : blocks) {
            List<CreditWordsImpl> words = words(block, number);
            if (words.isEmpty()) {
                continue;
            }
            String halign = xml.attr(block, "halign");
            int column = 0;
            double x = page[LEFT];
            if (CENTER.equals(halign)) {
                column = 1;
                x = page[WIDTH] / 2;
            } else if (RIGHT_ALIGN.equals(halign)) {
                column = 2;
                x = page[WIDTH] - page[RIGHT];
            }
            double blockHeight = height(words, tenthsPerPoint);
            double y;
            String valign;
            if (head) {
                y = page[HEIGHT] - page[TOP] - offsets[column];
                valign = "top";
            } else {
                y = page[BOTTOM] + offsets[column];
                valign = "bottom";
            }
            offsets[column] += blockHeight;
            CreditWordsImpl first = words.get(0);
            first.setDefaultX(format(x));
            first.setDefaultY(format(y));
            first.setJustify(justify(halign));
            first.setValign(valign);
            CreditImpl credit = new CreditImpl();
            credit.setPage(number);
            String type = xml.attr(block, "type");
            if (type != null) {
                credit.setCreditType(type.replace('-', ' '));
            }
            if (words.size() == 1) {
                credit.setCreditWords(first);
            } else {
                credit.setCreditWordsList(words);
            }
            credits.add(credit);
        }
    }


    /**
     * Reads the words of a text block: one per nested rend with its font, or the block itself.
     *
     * @param block the rend element of the block
     * @param number the page number (for num label="page")
     * @return the words
     */
    private List<CreditWordsImpl> words(Element block, int number) {
        List<CreditWordsImpl> result = new ArrayList<>();
        List<Element> parts = XmlDocumentReader.getInstance().children(block, REND);
        if (parts.isEmpty()) {
            parts = Collections.singletonList(block);
        }
        for (Element part : parts) {
            StringBuilder text = new StringBuilder();
            appendText(part, number, text);
            if (text.toString().trim().isEmpty()) {
                continue;
            }
            CreditWordsImpl words = new CreditWordsImpl();
            words.setValue(text.toString());
            applyFont(part, words);
            result.add(words);
        }
        return result;
    }


    /**
     * Appends the text of an element: lb is a line break, num label="page" the page number.
     *
     * @param element the element
     * @param number the page number
     * @param text the text (appended to)
     */
    private void appendText(Element element, int number, StringBuilder text) {
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(node.getNodeValue().replaceAll("\\s*\\n\\s*", " "));
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                String name = XmlDocumentReader.getInstance().name(child);
                if ("lb".equals(name)) {
                    text.append('\n');
                } else if ("num".equals(name) && MeiPageTextWriter.PAGE_LABEL.equals(XmlDocumentReader.getInstance().attr(child, "label"))) {
                    text.append(number);
                } else {
                    appendText(child, number, text);
                }
            }
        }
    }


    /**
     * Applies the font of a rend to words.
     *
     * @param rend the rend element
     * @param words the words
     */
    private void applyFont(Element rend, CreditWordsImpl words) {
        XmlDocumentReader xml = XmlDocumentReader.getInstance();
        words.setFontFamily(xml.attr(rend, "fontfam"));
        String size = xml.attr(rend, "fontsize");
        if (size != null && size.endsWith("pt")) {
            double value = number(size.substring(0, size.length() - 2), -1);
            if (value > 0) {
                words.setFontSize(format(value));
            }
        }
        if ("bold".equals(xml.attr(rend, "fontweight"))) {
            words.setFontWeight("bold");
        }
        if ("italic".equals(xml.attr(rend, "fontstyle"))) {
            words.setFontStyle("italic");
        }
    }


    /**
     * Gets the height of a text block: its lines with their font sizes.
     *
     * @param words the words of the block
     * @param tenthsPerPoint the tenths of a point
     * @return the height in tenths
     */
    private double height(List<CreditWordsImpl> words, double tenthsPerPoint) {
        double result = 0;
        for (CreditWordsImpl part : words) {
            double size = number(part.getFontSize(), DEFAULT_FONT_SIZE);
            int lines = part.getValue().split("\n", -1).length;
            result += lines * size * LINE_SPACING * tenthsPerPoint;
        }
        return result;
    }


    /**
     * Gets the number of pages: the extent of the file description (unit page), otherwise estimated.
     *
     * @param root the mei element
     * @param measures the number of measures
     * @return the number of pages
     */
    private int pageCount(Element root, int measures) {
        XmlDocumentReader xml = XmlDocumentReader.getInstance();
        Element extent = xml.path(xml.child(root, "meiHead"), "fileDesc/extent");
        if (extent != null && "page".equals(xml.attr(extent, "unit"))) {
            int quantity = (int) number(xml.attr(extent, "quantity"), -1);
            if (quantity >= 1 && quantity <= 10_000) {
                return quantity;
            }
        }
        return Math.max(1, (measures + MEASURES_PER_PAGE - 1) / MEASURES_PER_PAGE);
    }


    /**
     * Gets the page geometry of the score definition in tenths.
     *
     * @param scoreDef the scoreDef element
     * @param staffMillimeters the staff height in millimeters
     * @return width, height, left, right, top and bottom margin
     */
    private double[] geometry(Element scoreDef, double staffMillimeters) {
        XmlDocumentReader xml = XmlDocumentReader.getInstance();
        String[] names = {"page.width", "page.height", "page.leftmar", "page.rightmar", "page.topmar", "page.botmar"};
        double[] result = DEFAULT_PAGE.clone();
        for (int i = 0; i < names.length; i++) {
            double value = tenths(xml.attr(scoreDef, names[i]), staffMillimeters);
            if (value >= 0) {
                result[i] = value;
            }
        }
        return result;
    }


    /**
     * Creates the defaults of the score: scaling and page layout.
     *
     * @param page the page geometry
     * @param staffMillimeters the staff height in millimeters
     * @return the defaults
     */
    private DefaultsImpl defaults(double[] page, double staffMillimeters) {
        ScalingImpl scaling = new ScalingImpl();
        scaling.setMillimeters(staffMillimeters);
        scaling.setTenths(TENTHS_PER_STAFF);
        PageMarginsImpl margins = new PageMarginsImpl();
        margins.setType("both");
        margins.setLeftMargin(page[LEFT]);
        margins.setRightMargin(page[RIGHT]);
        margins.setTopMargin(page[TOP]);
        margins.setBottomMargin(page[BOTTOM]);
        PageLayoutImpl layout = new PageLayoutImpl();
        layout.setPageWidth(page[WIDTH]);
        layout.setPageHeight(page[HEIGHT]);
        layout.setPageMargins(Collections.singletonList(margins));
        DefaultsImpl defaults = new DefaultsImpl();
        defaults.setScaling(scaling);
        defaults.setPageLayout(layout);
        return defaults;
    }


    /**
     * Gets the staff height in millimeters from the size of a virtual unit (an eighth of the staff).
     *
     * @param vuHeight the vu.height attribute, or null
     * @return the staff height
     */
    private double staffMillimeters(String vuHeight) {
        if (vuHeight != null && vuHeight.endsWith("mm")) {
            double value = number(vuHeight.substring(0, vuHeight.length() - 2), -1);
            if (value > 0 && value < 10) {
                return value * VU_PER_STAFF;
            }
        }
        return DEFAULT_STAFF_MILLIMETERS;
    }


    /**
     * Converts an MEI measurement into tenths.
     *
     * @param value the measurement (vu, mm, cm, in or pt), or null
     * @param staffMillimeters the staff height in millimeters
     * @return the tenths, or -1 if invalid
     */
    private double tenths(String value, double staffMillimeters) {
        if (value == null) {
            return -1;
        }
        String text = value.trim();
        double millimetersPerTenth = staffMillimeters / TENTHS_PER_STAFF;
        double factor = MeiPageTextWriter.TENTHS_PER_VU;
        String number = text;
        if (text.endsWith("vu")) {
            number = text.substring(0, text.length() - 2);
        } else if (text.endsWith("mm")) {
            number = text.substring(0, text.length() - 2);
            factor = 1 / millimetersPerTenth;
        } else if (text.endsWith("cm")) {
            number = text.substring(0, text.length() - 2);
            factor = 10 / millimetersPerTenth;
        } else if (text.endsWith("in")) {
            number = text.substring(0, text.length() - 2);
            factor = 25.4 / millimetersPerTenth;
        } else if (text.endsWith("pt")) {
            number = text.substring(0, text.length() - 2);
            factor = MILLIMETERS_PER_POINT / millimetersPerTenth;
        }
        double parsed = number(number, -1);
        if (parsed < 0) {
            return -1;
        }
        return parsed * factor;
    }


    /**
     * Gets the MusicXML justification of an MEI horizontal alignment.
     *
     * @param halign the alignment, or null
     * @return left, center or right
     */
    private String justify(String halign) {
        if (CENTER.equals(halign) || RIGHT_ALIGN.equals(halign)) {
            return halign;
        }
        return "left";
    }


    /**
     * Formats a number with at most two decimals.
     *
     * @param value the value
     * @return the text
     */
    private String format(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
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
            double value = Double.parseDouble(text.trim());
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
