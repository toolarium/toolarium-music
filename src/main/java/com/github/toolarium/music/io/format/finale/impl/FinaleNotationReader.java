/*
 * FinaleNotationReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl;

import com.github.toolarium.music.io.format.finale.impl.dto.FinaleExpression;
import com.github.toolarium.music.io.format.finale.impl.dto.FinaleHairpin;
import com.github.toolarium.music.model.layout.impl.PageLayoutImpl;
import com.github.toolarium.music.model.layout.impl.PageMarginsImpl;
import com.github.toolarium.music.model.score.impl.CreditImpl;
import com.github.toolarium.music.model.score.impl.CreditWordsImpl;
import com.github.toolarium.music.model.score.impl.DefaultsImpl;
import com.github.toolarium.music.model.score.impl.ScalingImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Reads the notation records of an EnigmaXML document that are not part of the notes: the text
 * expressions (dynamics, tempo marks and texts) assigned to measures.
 *
 * <p>An expression assignment (measExprAssign) refers to a text expression definition (textExprDef),
 * which refers to its text (expression) and its marking category (markingsCategory). Dynamics are
 * glyphs of the music font (Maestro and compatible fonts); a tempo mark plays back a tempo
 * (playType time) whose value is the metronome value and whose auxiliary data is the beat unit.</p>
 *
 * @author patrick
 */
public final class FinaleNotationReader {
    private static final String NS = "http://www.makemusic.com/2012/finale";
    private static final String ATTR_CMPER = "cmper";
    private static final String ATTR_PART = "part";
    private static final String PLAY_TIME = "time";
    private static final int QUARTER = 1024;
    private static final Pattern METRONOME = Pattern.compile("(?:^|\\s)([qhewx][.k]?)\\s*=\\s*(?:c\\.?\\s*)?([0-9]+)");
    private static final Map<String, Integer> BEAT_UNITS = new HashMap<>();
    private static final Map<String, String> DYNAMICS = new HashMap<>();
    private static final Map<String, String> ARTICULATIONS = new HashMap<>();
    private static final int MAX_WEDGES = 6;
    private static final int LETTERS = 26;
    private static final String REHEARSAL_LETTER = "^rehearsal(";
    private static final Pattern A_TEMPO = Pattern.compile("^(a tempo|tempo i|tempo primo)(\\s|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SLOWER = Pattern.compile("(^|\\s)(rit|rall|allarg)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FASTER = Pattern.compile("(^|\\s)(accel|string)", Pattern.CASE_INSENSITIVE);
    private static final int DEFAULT_TEMPO = 120;
    private static final int GRADUAL_PERCENT = 25;
    private static final int GRADUAL_MEASURES = 2;
    private static final String TEXT_FONT = "text:";
    private static final char FIRST_DIGIT = '0';
    private static final char LAST_DIGIT = '9';
    private static final int COPYRIGHT_SIGN = 169;
    private static final Pattern PAGE_NUMBER = Pattern.compile("\\^page\\(\\d*\\)");
    private static final Pattern PERFORMANCE_TIME = Pattern.compile("\\^perftime\\(\\d*\\)");
    private static final Pattern FONT_COMMAND = Pattern.compile("\\^(fontid|size|nfx)\\((\\d+)\\)");
    private static final Pattern TEXT_COMMAND = Pattern.compile("\\^[A-Za-z]+\\([^)]*\\)");
    private static final int RUN_FAMILY = 0;
    private static final int RUN_SIZE = 1;
    private static final int RUN_EFFECTS = 2;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int EVPU_PER_POINT = 4;
    private static final double LINE_SPACING = 1.2;
    private static final int BOLD = 1;
    private static final int ITALIC = 2;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int PAGE_HEIGHT = 0;
    private static final int PAGE_WIDTH = 1;
    private static final int PAGE_TOP = 2;
    private static final int PAGE_LEFT = 3;
    private static final int PAGE_BOTTOM = 4;
    private static final int PAGE_RIGHT = 5;
    private static final int PAGE_PERCENT = 6;
    private static final int PERCENT = 100;
    private static final int DEFAULT_PAGE_HEIGHT = 3366;
    private static final int DEFAULT_PAGE_WIDTH = 2380;
    private static final int DEFAULT_MARGIN = 144;
    private static final int STAFF_HEIGHT_EVPU = 96;
    private static final int STAFF_HEIGHT_TENTHS = 40;
    private static final double STAFF_HEIGHT_MILLIMETERS = 96 * 25.4 / 288;
    private static final String[][] CREDIT_TYPES = {
        {"title", "title"}, {"subtitle", "subtitle"}, {"composer", "composer"}, {"arranger", "arranger"},
        {"lyricist", "lyricist"}, {"copyright", "rights"}, {"cprsym", "rights"}};

    static {
        // articulation characters of the music font and the marks they stand for
        ARTICULATIONS.put("46", "staccato");
        ARTICULATIONS.put("62", "accent");
        ARTICULATIONS.put("45", "tenuto");
        ARTICULATIONS.put("94", "strong-accent");
        ARTICULATIONS.put("248", "detached-legato");
        ARTICULATIONS.put("249", "accent,staccato");
        ARTICULATIONS.put("138", "accent,tenuto");
        ARTICULATIONS.put("172", "staccato,strong-accent");
        ARTICULATIONS.put("44", "breath-mark");
        ARTICULATIONS.put("34", "caesura");
        ARTICULATIONS.put("85", "fermata");
        ARTICULATIONS.put("117", "fermata-inverted");
        ARTICULATIONS.put("217", "trill-mark");

        // note glyphs of the music text fonts: q quarter, h half, e eighth, w whole, x sixteenth, a dot or k for dotted
        BEAT_UNITS.put("w", 4 * QUARTER);
        BEAT_UNITS.put("h", 2 * QUARTER);
        BEAT_UNITS.put("q", QUARTER);
        BEAT_UNITS.put("e", QUARTER / 2);
        BEAT_UNITS.put("x", QUARTER / 4);
        BEAT_UNITS.put("h.", 3 * QUARTER);
        BEAT_UNITS.put("q.", QUARTER * 3 / 2);
        BEAT_UNITS.put("e.", QUARTER * 3 / 4);
        BEAT_UNITS.put("hk", 3 * QUARTER);
        BEAT_UNITS.put("qk", QUARTER * 3 / 2);
        BEAT_UNITS.put("ek", QUARTER * 3 / 4);

        DYNAMICS.put("p", "p");
        DYNAMICS.put("P", "mp");
        DYNAMICS.put("F", "mf");
        DYNAMICS.put("f", "f");
        DYNAMICS.put(String.valueOf((char) 185), "pp");
        DYNAMICS.put(String.valueOf((char) 184), "ppp");
        DYNAMICS.put(String.valueOf((char) 175), "pppp");
        DYNAMICS.put(String.valueOf((char) 196), "ff");
        DYNAMICS.put(String.valueOf((char) 236), "fff");
        DYNAMICS.put(String.valueOf((char) 235), "ffff");
        DYNAMICS.put("S", "sf");
        DYNAMICS.put("Z", "fz");
        DYNAMICS.put(String.valueOf((char) 167), "sfz");
        DYNAMICS.put(String.valueOf((char) 141), "sffz");
        DYNAMICS.put(String.valueOf((char) 234), "fp");
    }


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final FinaleNotationReader INSTANCE = new FinaleNotationReader();
    }


    /**
     * Constructor for FinaleNotationReader.
     */
    private FinaleNotationReader() {
        // NOP
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static FinaleNotationReader getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Reads the expressions assigned to measures.
     *
     * @param root the EnigmaXML root element
     * @param tempoSteps true to add the tempo steps of gradual tempo changes (rit., rall., accel.)
     * @return the expressions
     */
    public List<FinaleExpression> readExpressions(Element root, boolean tempoSteps) {
        // the text of the n-th expression definition is the n-th expression text (the text numbers can have gaps)
        List<String> texts = new ArrayList<>();
        for (Element expression : elements(root, "expression")) {
            texts.add(expression.getTextContent());
        }
        Map<String, Element> definitions = new HashMap<>();
        for (Element definition : elements(root, "textExprDef")) {
            if (isScore(definition)) {
                definitions.put(definition.getAttribute(ATTR_CMPER), definition);
            }
        }

        List<FinaleExpression> result = new ArrayList<>();
        List<int[]> gradualChanges = new ArrayList<>();
        int rehearsalMarks = 0;
        int tempo = 0;
        for (Element assign : elements(root, "measExprAssign")) {
            if (!isScore(assign) || hasChild(assign, "hidden")) {
                continue;
            }
            String id = text(assign, "textExprID");
            Element definition = definitions.get(id);
            if (definition == null) {
                continue;
            }
            int measure = Integer.parseInt(assign.getAttribute(ATTR_CMPER));
            int staff = intValue(assign, "staffAssign", -1);
            int offset = intValue(assign, "horzEduOff", 0);

            String raw = "";
            int index = Integer.parseInt(id) - 1;
            if (index >= 0 && index < texts.size()) {
                raw = texts.get(index);
            }
            String plain = plainText(raw);
            Matcher metronome = METRONOME.matcher(plain);
            if (raw.contains(REHEARSAL_LETTER)) {
                // an automatic rehearsal mark: the letters follow the order of the marks in the score
                result.add(new FinaleExpression(measure, staff, offset, rehearsalLetter(rehearsalMarks), true, 0));
                rehearsalMarks++;
            } else if (PLAY_TIME.equals(text(definition, "playType")) && intValue(definition, "value", 0) > 0) {
                result.add(new FinaleExpression(measure, staff, offset, null, intValue(definition, "value", 0), intValue(definition, "auxdata1", QUARTER), plain));
                tempo = intValue(definition, "value", 0) * intValue(definition, "auxdata1", QUARTER) / QUARTER;
            } else if (DYNAMICS.containsKey(plain)) {
                result.add(new FinaleExpression(measure, staff, offset, DYNAMICS.get(plain), 0, 0, null));
            } else if (metronome.find() && BEAT_UNITS.containsKey(metronome.group(1))) {
                // a tempo mark as text only: the note glyph of the music font, an equals sign and the value
                result.add(new FinaleExpression(measure, staff, offset, null, Integer.parseInt(metronome.group(2)), BEAT_UNITS.get(metronome.group(1)), plain));
                tempo = Integer.parseInt(metronome.group(2)) * BEAT_UNITS.get(metronome.group(1)) / QUARTER;
            } else if (!plain.isEmpty()) {
                // a tempo returns to the last tempo mark
                int soundTempo = 0;
                if (A_TEMPO.matcher(plain).find()) {
                    soundTempo = tempo;
                }
                result.add(new FinaleExpression(measure, staff, offset, plain, false, soundTempo));
                if (SLOWER.matcher(plain).find()) {
                    gradualChanges.add(new int[] {result.size() - 1, -1, tempo});
                } else if (FASTER.matcher(plain).find()) {
                    gradualChanges.add(new int[] {result.size() - 1, 1, tempo});
                }
            }
        }
        if (tempoSteps) {
            result.addAll(tempoSteps(root, result, gradualChanges));
        }
        return result;
    }


    /**
     * Creates the tempo steps of the gradual tempo changes (rit., rall., accel.): MusicXML has no gradual tempo, so the
     * tempo changes on every quarter from the mark to the next tempo change (a tempo mark, a tempo or the next gradual
     * change), at most for two measures. The tempo ends at the next tempo mark if it lies in the direction of the change,
     * else it changes by a quarter of the tempo.
     *
     * @param root the EnigmaXML root element
     * @param expressions the expressions
     * @param changes the gradual changes: the index of the expression, -1 for slower or 1 for faster, and the tempo before
     * @return the tempo steps (expressions without text with a sound tempo)
     */
    private List<FinaleExpression> tempoSteps(Element root, List<FinaleExpression> expressions, List<int[]> changes) {
        List<FinaleExpression> result = new ArrayList<>();
        if (changes.isEmpty()) {
            return result;
        }
        Map<Integer, Integer> lengths = measureLengths(root);
        for (int[] change : changes) {
            FinaleExpression start = expressions.get(change[0]);
            int tempo = change[2];
            if (tempo <= 0) {
                tempo = DEFAULT_TEMPO;
            }
            int target = tempo * (PERCENT + change[1] * GRADUAL_PERCENT) / PERCENT;
            int endMeasure = start.getMeasure() + GRADUAL_MEASURES;
            int endOffset = 0;
            FinaleExpression end = nextTempoChange(expressions, start);
            if (end != null && end.getMeasure() < endMeasure) {
                endMeasure = end.getMeasure();
                endOffset = end.getEduOffset();
                int next = quarterTempo(end);
                if (next > 0 && (next - tempo) * change[1] > 0) {
                    target = next;
                }
            }

            List<int[]> beats = new ArrayList<>();
            int measure = start.getMeasure();
            int offset = start.getEduOffset();
            while (measure < endMeasure || (measure == endMeasure && offset < endOffset)) {
                if (offset >= lengths.getOrDefault(measure, 4 * QUARTER)) {
                    measure++;
                    offset = 0;
                } else {
                    beats.add(new int[] {measure, offset});
                    offset += QUARTER;
                }
            }
            for (int i = 0; i < beats.size(); i++) {
                int value = tempo + (target - tempo) * (i + 1) / beats.size();
                result.add(new FinaleExpression(beats.get(i)[0], start.getStaff(), beats.get(i)[1], null, false, value));
            }
        }
        return result;
    }


    /**
     * Gets the next expression after an expression that changes the tempo: a tempo mark, a tempo or a gradual change.
     *
     * @param expressions the expressions
     * @param start the expression
     * @return the next expression that changes the tempo, or null if there is none
     */
    private FinaleExpression nextTempoChange(List<FinaleExpression> expressions, FinaleExpression start) {
        FinaleExpression result = null;
        for (FinaleExpression expression : expressions) {
            if (expression != start && isTempoChange(expression) && isBefore(start, expression) && (result == null || isBefore(expression, result))) {
                result = expression;
            }
        }
        return result;
    }


    /**
     * Checks whether an expression changes the tempo: a tempo mark, a tempo or a gradual change.
     *
     * @param expression the expression
     * @return true if the expression changes the tempo
     */
    private boolean isTempoChange(FinaleExpression expression) {
        if (expression.getBeatsPerMinute() > 0 || expression.getSoundTempo() > 0) {
            return true;
        }
        String text = expression.getText();
        return text != null && (SLOWER.matcher(text).find() || FASTER.matcher(text).find());
    }


    /**
     * Checks whether an expression comes before another one.
     *
     * @param first the first expression
     * @param second the second expression
     * @return true if the first expression is earlier than the second one
     */
    private boolean isBefore(FinaleExpression first, FinaleExpression second) {
        return first.getMeasure() < second.getMeasure() || (first.getMeasure() == second.getMeasure() && first.getEduOffset() < second.getEduOffset());
    }


    /**
     * Gets the tempo an expression sets in quarters per minute.
     *
     * @param expression the expression
     * @return the tempo, or 0 if the expression sets no tempo
     */
    private int quarterTempo(FinaleExpression expression) {
        if (expression.getBeatsPerMinute() > 0) {
            return expression.getBeatsPerMinute() * expression.getBeatUnitEdu() / QUARTER;
        }
        return expression.getSoundTempo();
    }


    /**
     * Reads the lengths of the measures.
     *
     * @param root the EnigmaXML root element
     * @return the length in EDU by measure
     */
    private Map<Integer, Integer> measureLengths(Element root) {
        Map<Integer, Integer> result = new HashMap<>();
        for (Element spec : elements(root, "measSpec")) {
            int length = intValue(spec, "beats", 4) * intValue(spec, "divbeat", QUARTER);
            if (isScore(spec) && length > 0) {
                result.putIfAbsent(Integer.parseInt(spec.getAttribute(ATTR_CMPER)), length);
            }
        }
        return result;
    }


    /**
     * Gets the mark of a number of a text font (a fingering or a beat count above a note).
     *
     * @param character the character of the articulation definition: its code, prefixed with text: for a text font
     * @return the mark fingering:number, or null if the character is no digit of a text font
     */
    private String number(String character) {
        if (character == null || !character.startsWith(TEXT_FONT)) {
            return null;
        }
        try {
            char digit = (char) Integer.parseInt(character.substring(TEXT_FONT.length()));
            if (digit >= FIRST_DIGIT && digit <= LAST_DIGIT) {
                return FinaleMarkConverter.FINGERING + digit;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }


    /**
     * Gets the letters of a rehearsal mark: A to Z, then AA, BB and so on.
     *
     * @param index the index of the rehearsal mark (0 for the first)
     * @return the letters
     */
    private String rehearsalLetter(int index) {
        char letter = (char) ('A' + index % LETTERS);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i <= index / LETTERS; i++) {
            result.append(letter);
        }
        return result.toString();
    }


    /**
     * Reads the marks of the entries: articulations, fermatas and trills (articulation assignments with a
     * music font character), and the start and stop of slurs (entry based smart shapes). The marks of an
     * entry are articulation names (staccato, accent, ...), fermata, trill-mark, and slur-start:id or
     * slur-stop:id with the ID of the slur (its smart shape number).
     *
     * @param root the EnigmaXML root element
     * @return the marks by entry number
     */
    public Map<Integer, List<String>> readEntryMarks(Element root) {
        Map<String, String> characters = new HashMap<>();
        for (Element definition : elements(root, "articDef")) {
            if (isScore(definition) && hasChild(definition, "fontMain")) {
                // a character of a text font
                characters.put(definition.getAttribute(ATTR_CMPER), TEXT_FONT + text(definition, "charMain"));
            } else if (isScore(definition)) {
                characters.put(definition.getAttribute(ATTR_CMPER), text(definition, "charMain"));
            }
        }

        Map<Integer, List<String>> result = new HashMap<>();
        for (Element assign : elements(root, "articAssign")) {
            String definition = text(assign, "articDef");
            if (!isScore(assign) || definition == null || hasChild(assign, "hidden")) {
                continue;
            }
            String marks = ARTICULATIONS.get(characters.get(definition));
            if (marks == null) {
                marks = number(characters.get(definition));
            }
            if (marks != null) {
                List<String> entryMarks = result.computeIfAbsent(Integer.parseInt(assign.getAttribute("entnum")), k -> new ArrayList<>());
                for (String mark : marks.split(",")) {
                    entryMarks.add(mark);
                }
            }
        }

        for (Element shape : elements(root, "smartShape")) {
            // a smart shape without type has the default type: a slur down
            String type = text(shape, "shapeType");
            if (type == null) {
                type = "slurDown";
            }
            if (!isScore(shape) || !type.startsWith("slur") || !hasChild(shape, "entryBased")) {
                continue;
            }
            Integer start = entryNumber(shape, "startTermSeg");
            Integer end = entryNumber(shape, "endTermSeg");
            if (start == null || end == null) {
                continue;
            }
            String id = shape.getAttribute(ATTR_CMPER);
            result.computeIfAbsent(start, k -> new ArrayList<>()).add(FinaleMarkConverter.SLUR_START + id);
            result.computeIfAbsent(end, k -> new ArrayList<>()).add(FinaleMarkConverter.SLUR_STOP + id);
        }
        return result;
    }


    /**
     * Reads the texts of the pages (title, subtitle, composer, arranger, copyright, page numbers and free texts).
     * The text inserts of the file information (^title(), ^composer(), ...), the part name of the score and the
     * performance time and the page number are replaced by their values. A text with font changes (^fontid, ^size
     * and ^nfx) has a credit words element for each font. Free texts with the same alignment on a page are joined
     * into one credit from top to bottom, separated by empty lines for the space between them, because most
     * applications place free texts by their alignment only.
     *
     * @param root the EnigmaXML root element
     * @param useFonts true to take over the fonts of the texts, false for the fonts of the application
     * @return the credits
     */
    public List<CreditImpl> readCredits(Element root, boolean useFonts) {
        Map<String, String> fileInfo = new HashMap<>();
        for (Element info : elements(root, "fileInfo")) {
            fileInfo.put(info.getAttribute("type"), info.getTextContent().trim());
        }
        Map<String, String> blockTexts = new HashMap<>();
        for (Element text : elements(root, "blockText")) {
            blockTexts.put(text.getAttribute("number"), text.getTextContent());
        }
        Map<String, String> textIds = new HashMap<>();
        for (Element block : elements(root, "textBlock")) {
            textIds.put(block.getAttribute(ATTR_CMPER), text(block, "textID"));
        }
        for (Element part : elements(root, "partDef")) {
            String name = blockTexts.get(textIds.get(text(part, "nameID")));
            if ("0".equals(part.getAttribute(ATTR_CMPER)) && name != null) {
                fileInfo.put("partname", plainText(name).trim());
            }
        }
        fileInfo.put("perftime", performanceTime(root));
        Map<String, String> fonts = new HashMap<>();
        if (useFonts) {
            for (Element font : elements(root, "fontName")) {
                fonts.put(font.getAttribute(ATTR_CMPER), text(font, "name"));
            }
        }

        int pages = pageCount(root);
        List<CreditImpl> result = new ArrayList<>();
        for (Element assign : elements(root, "pageTextAssign")) {
            String raw = blockTexts.get(textIds.get(text(assign, "block")));
            if (!isScore(assign) || hasChild(assign, "hidden") || raw == null) {
                continue;
            }
            // a text on the pages from its start page to its end page (0 for the last page)
            int last = intValue(assign, "endPage", 0);
            if (last <= 0 || last > pages) {
                last = pages;
            }
            for (int number = Math.max(1, intValue(assign, "startPage", 1)); number <= last; number++) {
                CreditImpl credit = pageCredit(root, assign, raw, number, fileInfo, fonts, useFonts);
                if (credit != null) {
                    result.add(credit);
                }
            }
        }
        return joinFreeTexts(result, readPage(root, 1)[PAGE_PERCENT]);
    }


    /**
     * Joins the free texts (credits without a type) with the same page, alignment and vertical alignment into one
     * credit: the texts follow each other from top to bottom, separated by empty lines for the space between them.
     *
     * @param credits the credits
     * @param percent the page percent
     * @return the credits with the joined free texts
     */
    private List<CreditImpl> joinFreeTexts(List<CreditImpl> credits, int percent) {
        Map<String, List<CreditImpl>> groups = new LinkedHashMap<>();
        List<CreditImpl> result = new ArrayList<>();
        for (CreditImpl credit : credits) {
            CreditWordsImpl words = credit.getCreditWords();
            if (credit.getCreditType() == null && words.getDefaultY() != null) {
                String key = credit.getPage() + "/" + words.getJustify() + "/" + words.getValign();
                if (!groups.containsKey(key)) {
                    result.add(credit);
                }
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(credit);
            } else {
                result.add(credit);
            }
        }
        for (List<CreditImpl> group : groups.values()) {
            if (group.size() < 2) {
                continue;
            }
            group.sort((a, b) -> Integer.compare(creditY(b), creditY(a)));
            CreditImpl first = group.get(0);
            List<CreditWordsImpl> runs = new ArrayList<>(first.getCreditWordsList());
            int previousY = creditY(first);
            for (CreditImpl next : group.subList(1, group.size())) {
                CreditWordsImpl last = runs.get(runs.size() - 1);
                int size = DEFAULT_FONT_SIZE;
                if (last.getFontSize() != null) {
                    size = Integer.parseInt(last.getFontSize());
                }
                // the line height in tenths and the space below the previous text
                double lineHeight = size * EVPU_PER_POINT * LINE_SPACING * STAFF_HEIGHT_TENTHS / STAFF_HEIGHT_EVPU * PERCENT / percent;
                double space = previousY - creditY(next) - lines(runs) * lineHeight;
                StringBuilder separator = new StringBuilder("\n");
                for (long i = Math.round(Math.max(0, space) / lineHeight); i > 0; i--) {
                    separator.append('\n');
                }
                last.setValue(last.getValue() + separator);
                for (CreditWordsImpl words : next.getCreditWordsList()) {
                    CreditWordsImpl run = new CreditWordsImpl();
                    run.setValue(words.getValue());
                    run.setFontFamily(words.getFontFamily());
                    run.setFontSize(words.getFontSize());
                    run.setFontWeight(words.getFontWeight());
                    run.setFontStyle(words.getFontStyle());
                    runs.add(run);
                }
                previousY = creditY(next);
                result.remove(next);
            }
            first.setCreditWordsList(runs);
        }
        return result;
    }


    /**
     * Gets the vertical position of a credit.
     *
     * @param credit the credit
     * @return the default-y of its first credit words in tenths
     */
    private int creditY(CreditImpl credit) {
        return Integer.parseInt(credit.getCreditWords().getDefaultY());
    }


    /**
     * Counts the lines of credit words.
     *
     * @param runs the credit words
     * @return the number of lines
     */
    private int lines(List<CreditWordsImpl> runs) {
        int result = 1;
        for (CreditWordsImpl run : runs) {
            result += run.getValue().length() - run.getValue().replace("\n", "").length();
        }
        return result;
    }


    /**
     * Creates the credit of a page text on a page. On a right page (odd page number) a text with an independent
     * right page position (indRpPos) uses its right page alignment and displacement.
     *
     * @param root the EnigmaXML root element
     * @param assign the page text assignment
     * @param raw the text with its Finale commands
     * @param number the page number
     * @param fileInfo the values of the text inserts
     * @param fonts the font names by font ID
     * @param useFonts true to take over the fonts of the text
     * @return the credit, or null if the text is empty
     */
    private CreditImpl pageCredit(Element root, Element assign, String raw, int number, Map<String, String> fileInfo, Map<String, String> fonts,
                                  boolean useFonts) {
        List<CreditWordsImpl> runs = creditRuns(PAGE_NUMBER.matcher(raw).replaceAll(String.valueOf(number)), fileInfo, fonts, useFonts);
        if (runs.isEmpty()) {
            return null;
        }
        CreditWordsImpl words = runs.get(0);
        String justify = text(assign, "hposLp");
        int xdisp = intValue(assign, "xdisp", 0);
        int ydisp = intValue(assign, "ydisp", 0);
        if (number % 2 == 1 && hasChild(assign, "indRpPos")) {
            justify = text(assign, "hposRp");
            xdisp = intValue(assign, "rightPgXdisp", 0);
            ydisp = intValue(assign, "rightPgYdisp", 0);
        }
        if (justify == null) {
            justify = "left";
        }
        words.setJustify(justify);

        // the position relative to the page margins; y counts upwards from the bottom of the page in MusicXML
        int[] page = readPage(root, number);
        int x = page[PAGE_LEFT] + xdisp;
        if ("center".equals(justify)) {
            x = page[PAGE_WIDTH] / 2 + xdisp;
        } else if ("right".equals(justify)) {
            x = page[PAGE_WIDTH] - page[PAGE_RIGHT] + xdisp;
        }
        int y;
        if ("bottom".equals(text(assign, "vpos"))) {
            words.setValign("bottom");
            y = page[PAGE_BOTTOM] + ydisp;
        } else {
            words.setValign("top");
            y = page[PAGE_HEIGHT] - page[PAGE_TOP] + ydisp;
        }
        words.setDefaultX(tenths(x, page[PAGE_PERCENT]));
        words.setDefaultY(tenths(y, page[PAGE_PERCENT]));
        CreditImpl credit = new CreditImpl();
        credit.setPage(number);
        if (number == 1) {
            credit.setCreditType(creditType(raw));
        } else if (PAGE_NUMBER.matcher(raw).find()) {
            credit.setCreditType("page number");
        }
        credit.setCreditWordsList(runs);
        return credit;
    }


    /**
     * Counts the pages of the score.
     *
     * @param root the EnigmaXML root element
     * @return the number of pages, at least 1
     */
    private int pageCount(Element root) {
        int result = 1;
        for (Element spec : elements(root, "pageSpec")) {
            if (isScore(spec)) {
                result = Math.max(result, Integer.parseInt(spec.getAttribute(ATTR_CMPER)));
            }
        }
        return result;
    }


    /**
     * Reads the page and system breaks of the score: the first measure of each system, and of each page.
     *
     * @param root the EnigmaXML root element
     * @return page or system by the first measure of a system (without the first measure of the score)
     */
    public Map<Integer, String> readBreaks(Element root) {
        Map<Integer, Integer> systems = new HashMap<>();
        for (Element system : elements(root, "staffSystemSpec")) {
            if (isScore(system)) {
                systems.put(Integer.parseInt(system.getAttribute(ATTR_CMPER)), intValue(system, "startMeas", 0));
            }
        }
        Map<Integer, String> result = new HashMap<>();
        for (Integer measure : systems.values()) {
            if (measure > 1) {
                result.put(measure, "system");
            }
        }
        for (Element spec : elements(root, "pageSpec")) {
            Integer measure = systems.get(intValue(spec, "firstSystem", 0));
            if (isScore(spec) && measure != null && measure > 1) {
                result.put(measure, "page");
            }
        }
        return result;
    }


    /**
     * Reads the page layout of the first page: the scaling (a staff space of 24 EVPU is 10 tenths, reduced by
     * the page percent) and the page size and margins in tenths.
     *
     * @param root the EnigmaXML root element
     * @return the defaults
     */
    public DefaultsImpl readDefaults(Element root) {
        int[] page = readPage(root, 1);
        ScalingImpl scaling = new ScalingImpl();
        scaling.setMillimeters(round(STAFF_HEIGHT_MILLIMETERS * page[PAGE_PERCENT] / PERCENT));
        scaling.setTenths((double) STAFF_HEIGHT_TENTHS);

        PageMarginsImpl margins = new PageMarginsImpl();
        margins.setType("both");
        margins.setLeftMargin(Double.valueOf(tenths(page[PAGE_LEFT], page[PAGE_PERCENT])));
        margins.setRightMargin(Double.valueOf(tenths(page[PAGE_RIGHT], page[PAGE_PERCENT])));
        margins.setTopMargin(Double.valueOf(tenths(page[PAGE_TOP], page[PAGE_PERCENT])));
        margins.setBottomMargin(Double.valueOf(tenths(page[PAGE_BOTTOM], page[PAGE_PERCENT])));
        PageLayoutImpl layout = new PageLayoutImpl();
        layout.setPageHeight(Double.valueOf(tenths(page[PAGE_HEIGHT], page[PAGE_PERCENT])));
        layout.setPageWidth(Double.valueOf(tenths(page[PAGE_WIDTH], page[PAGE_PERCENT])));
        List<PageMarginsImpl> marginList = new ArrayList<>();
        marginList.add(margins);
        layout.setPageMargins(marginList);

        DefaultsImpl defaults = new DefaultsImpl();
        defaults.setScaling(scaling);
        defaults.setPageLayout(layout);
        return defaults;
    }


    /**
     * Reads the size and the margins of a page (pageSpec) in EVPU (288 per inch). Finale stores the
     * top and right margins as negative values; they are returned as distances.
     *
     * @param root the EnigmaXML root element
     * @param number the page number
     * @return height, width, top, left, bottom and right margin, and the page percent
     */
    private int[] readPage(Element root, int number) {
        int[] page = {DEFAULT_PAGE_HEIGHT, DEFAULT_PAGE_WIDTH, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, PERCENT};
        for (Element spec : elements(root, "pageSpec")) {
            if (isScore(spec) && String.valueOf(number).equals(spec.getAttribute(ATTR_CMPER))) {
                page[PAGE_HEIGHT] = intValue(spec, "height", DEFAULT_PAGE_HEIGHT);
                page[PAGE_WIDTH] = intValue(spec, "width", DEFAULT_PAGE_WIDTH);
                page[PAGE_TOP] = Math.abs(intValue(spec, "margTop", DEFAULT_MARGIN));
                page[PAGE_LEFT] = Math.abs(intValue(spec, "margLeft", DEFAULT_MARGIN));
                page[PAGE_BOTTOM] = Math.abs(intValue(spec, "margBottom", DEFAULT_MARGIN));
                page[PAGE_RIGHT] = Math.abs(intValue(spec, "margRight", DEFAULT_MARGIN));
                int percent = intValue(spec, "percent", PERCENT);
                if (percent > 0) {
                    page[PAGE_PERCENT] = percent;
                }
                break;
            }
        }
        return page;
    }


    /**
     * Converts a page distance in EVPU to tenths.
     *
     * @param evpu the distance in EVPU
     * @param percent the page percent
     * @return the distance in tenths
     */
    private String tenths(int evpu, int percent) {
        double tenths = (double) evpu * STAFF_HEIGHT_TENTHS / STAFF_HEIGHT_EVPU * PERCENT / percent;
        return String.valueOf(Math.round(tenths));
    }


    /**
     * Rounds a value to two decimals.
     *
     * @param value the value
     * @return the rounded value
     */
    private double round(double value) {
        return Math.round(value * PERCENT) / (double) PERCENT;
    }


    /**
     * Gets the credit type of a page text from its first file information insert.
     *
     * @param raw the text with its Finale commands
     * @return the credit type, or null for a free text
     */
    private String creditType(String raw) {
        for (String[] insert : CREDIT_TYPES) {
            if (raw.contains("^" + insert[0] + "(")) {
                return insert[1];
            }
        }
        return null;
    }


    /**
     * Replaces the inserts of a page text (file information, part name, performance time and copyright sign) and
     * splits it into credit words with one font each (^fontid, ^size and ^nfx: 1 bold, 2 italic). The other Finale
     * commands are removed, spaces are collapsed and empty lines are left out.
     *
     * @param raw the text with its Finale commands
     * @param fileInfo the values of the text inserts
     * @param fonts the font names by font ID
     * @param useFonts true to set the fonts of the credit words, false for one credit words without font
     * @return the credit words, empty if the text is empty
     */
    private List<CreditWordsImpl> creditRuns(String raw, Map<String, String> fileInfo, Map<String, String> fonts, boolean useFonts) {
        String text = raw.replace("^cprsym()", String.valueOf((char) COPYRIGHT_SIGN));
        text = PERFORMANCE_TIME.matcher(text).replaceAll(Matcher.quoteReplacement(fileInfo.getOrDefault("perftime", "")));
        for (Map.Entry<String, String> info : fileInfo.entrySet()) {
            text = text.replace("^" + info.getKey() + "()", info.getValue());
        }

        // the runs of text with the same font: family, size and effects
        List<String[]> fontRuns = new ArrayList<>();
        List<StringBuilder> texts = new ArrayList<>();
        String[] font = {null, null, "0"};
        int position = 0;
        Matcher command = FONT_COMMAND.matcher(text);
        while (command.find()) {
            addRun(fontRuns, texts, text.substring(position, command.start()), font, useFonts);
            font = font.clone();
            if ("fontid".equals(command.group(1))) {
                font[RUN_FAMILY] = fonts.get(command.group(2));
            } else if ("size".equals(command.group(1))) {
                font[RUN_SIZE] = command.group(2);
            } else {
                font[RUN_EFFECTS] = command.group(2);
            }
            position = command.end();
        }
        addRun(fontRuns, texts, text.substring(position), font, useFonts);

        List<CreditWordsImpl> result = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            String value = texts.get(i).toString().replaceAll("[ \\t]+", " ").replaceAll(" ?\\n ?", "\n").replaceAll("\n{2,}", "\n");
            if (i == 0) {
                value = value.replaceAll("^\\s+", "");
            }
            if (i == texts.size() - 1) {
                value = value.replaceAll("\\s+$", "");
            }
            if (value.isEmpty()) {
                continue;
            }
            CreditWordsImpl words = new CreditWordsImpl();
            words.setValue(value);
            if (useFonts) {
                applyFont(words, fontRuns.get(i));
            }
            result.add(words);
        }
        return result;
    }


    /**
     * Adds a part of a text to the runs of text with the same font.
     *
     * @param fontRuns the fonts of the runs (updated)
     * @param texts the texts of the runs (updated)
     * @param text the part of the text with its Finale commands
     * @param font the font of the part: family, size and effects
     * @param useFonts true to keep the runs of different fonts apart
     */
    private void addRun(List<String[]> fontRuns, List<StringBuilder> texts, String text, String[] font, boolean useFonts) {
        String plain = TEXT_COMMAND.matcher(text).replaceAll("");
        if (plain.isEmpty()) {
            return;
        }
        if (!texts.isEmpty() && (!useFonts || Arrays.equals(fontRuns.get(fontRuns.size() - 1), font))) {
            texts.get(texts.size() - 1).append(plain);
        } else {
            fontRuns.add(font);
            texts.add(new StringBuilder(plain));
        }
    }


    /**
     * Sets the font of credit words.
     *
     * @param words the credit words (updated)
     * @param font the font: family, size and effects (^nfx: 1 bold, 2 italic)
     */
    private void applyFont(CreditWordsImpl words, String[] font) {
        words.setFontFamily(font[RUN_FAMILY]);
        words.setFontSize(font[RUN_SIZE]);
        int effects = Integer.parseInt(font[RUN_EFFECTS]);
        if ((effects & BOLD) != 0) {
            words.setFontWeight("bold");
        }
        if ((effects & ITALIC) != 0) {
            words.setFontStyle("italic");
        }
    }


    /**
     * Gets the performance time of the score as Finale shows it: the length of all measures played at the first
     * tempo mark (or 120 quarters per minute), with the seconds rounded up, for example 5'03".
     *
     * @param root the EnigmaXML root element
     * @return the performance time
     */
    private String performanceTime(Element root) {
        int tempo = DEFAULT_TEMPO;
        List<FinaleExpression> expressions = readExpressions(root, false);
        FinaleExpression first = null;
        for (FinaleExpression expression : expressions) {
            if (expression.getBeatsPerMinute() > 0 && (first == null || isBefore(expression, first))) {
                first = expression;
            }
        }
        if (first != null) {
            tempo = quarterTempo(first);
        }
        long length = 0;
        for (int measureLength : measureLengths(root).values()) {
            length += measureLength;
        }
        long seconds = (length * SECONDS_PER_MINUTE + (long) QUARTER * tempo - 1) / ((long) QUARTER * tempo);
        return seconds / SECONDS_PER_MINUTE + "'" + String.format("%02d", seconds % SECONDS_PER_MINUTE) + "\"";
    }


    /**
     * Reads the hairpins: the measure based crescendo and decrescendo smart shapes.
     *
     * @param root the EnigmaXML root element
     * @return the hairpins
     */
    public List<FinaleHairpin> readHairpins(Element root) {
        List<FinaleHairpin> result = new ArrayList<>();
        for (Element shape : elements(root, "smartShape")) {
            String type = text(shape, "shapeType");
            if (!isScore(shape) || hasChild(shape, "hidden") || !("cresc".equals(type) || "decresc".equals(type))) {
                continue;
            }
            Element start = endPoint(shape, "startTermSeg");
            Element end = endPoint(shape, "endTermSeg");
            if (start == null || end == null) {
                continue;
            }
            int number = result.size() % MAX_WEDGES + 1;
            result.add(new FinaleHairpin(intValue(start, "inst", 0), intValue(start, "meas", 0), intValue(start, "edu", 0),
                                         intValue(end, "meas", 0), intValue(end, "edu", 0), "cresc".equals(type), number));
        }
        return result;
    }


    /**
     * Reads the playback sound of the staves (playbackRoute): the General MIDI program and the channel.
     *
     * @param root the EnigmaXML root element
     * @return by staff the MIDI program (1 to 128) and channel (1 to 16)
     */
    public Map<Integer, int[]> readPlaybackRoutes(Element root) {
        Map<Integer, int[]> result = new HashMap<>();
        for (Element route : elements(root, "playbackRoute")) {
            if (!isScore(route) || text(route, "patch") == null) {
                continue;
            }
            // patch and channel are counted from 0
            int program = intValue(route, "patch", 0) + 1;
            int channel = intValue(route, "virtChannel", 0) + 1;
            result.put(Integer.parseInt(route.getAttribute(ATTR_CMPER)), new int[] {program, channel});
        }
        return result;
    }


    /**
     * Gets the entry number of an end of an entry based smart shape.
     *
     * @param shape the smart shape
     * @param segment the segment name (startTermSeg or endTermSeg)
     * @return the entry number, or null
     */
    private Integer entryNumber(Element shape, String segment) {
        Element point = endPoint(shape, segment);
        if (point == null) {
            return null;
        }
        int entry = intValue(point, "entryNum", 0);
        if (entry <= 0) {
            return null;
        }
        return entry;
    }


    /**
     * Gets the end point of a smart shape segment.
     *
     * @param shape the smart shape
     * @param segment the segment name (startTermSeg or endTermSeg)
     * @return the end point (endPt), or null
     */
    private Element endPoint(Element shape, String segment) {
        Element seg = child(shape, segment);
        if (seg == null) {
            return null;
        }
        return child(seg, "endPt");
    }


    /**
     * Gets a direct child element.
     *
     * @param parent the parent
     * @param name the local name
     * @return the child, or null
     */
    private Element child(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getLocalName())) {
                return (Element) node;
            }
        }
        return null;
    }


    /**
     * Removes the Finale text commands (font, size, effects) of a text.
     *
     * @param text the text, or null
     * @return the plain text, or an empty string
     */
    private String plainText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\^[A-Za-z]+\\([^)]*\\)", "").trim();
    }


    /**
     * Checks if a record belongs to the score (not to a linked part).
     *
     * @param element the record
     * @return true for a score record
     */
    private boolean isScore(Element element) {
        String part = element.getAttribute(ATTR_PART);
        return part == null || part.isEmpty();
    }


    /**
     * Gets all elements with a local name in the Finale namespace.
     *
     * @param root the root element
     * @param name the local name
     * @return the elements
     */
    private List<Element> elements(Element root, String name) {
        return FinaleElementIndex.getInstance().getElements(root, name);
    }


    /**
     * Checks if an element has a direct child element.
     *
     * @param parent the parent
     * @param name the local name of the child
     * @return true if the child exists
     */
    private boolean hasChild(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getLocalName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Gets the text of a direct child element.
     *
     * @param parent the parent
     * @param name the local name of the child
     * @return the trimmed text, or null
     */
    private String text(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getLocalName())) {
                return node.getTextContent().trim();
            }
        }
        return null;
    }


    /**
     * Gets the integer value of a direct child element.
     *
     * @param parent the parent
     * @param name the local name of the child
     * @param defaultValue the value if the child is missing or invalid
     * @return the value
     */
    private int intValue(Element parent, String name, int defaultValue) {
        String value = text(parent, name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
