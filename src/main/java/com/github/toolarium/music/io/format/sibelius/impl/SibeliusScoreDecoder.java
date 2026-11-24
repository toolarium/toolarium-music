/*
 * SibeliusScoreDecoder.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl;

import com.github.toolarium.music.io.converter.InstrumentSoundConverter;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.MeasureData;
import com.github.toolarium.music.io.dto.PartData;
import com.github.toolarium.music.io.dto.ScoreData;
import com.github.toolarium.music.io.format.sibelius.impl.dto.SibeliusObject;
import com.github.toolarium.music.io.format.sibelius.impl.dto.SibeliusTextRun;
import com.github.toolarium.music.model.layout.impl.PageLayoutImpl;
import com.github.toolarium.music.model.layout.impl.PageMarginsImpl;
import com.github.toolarium.music.model.part.impl.ScoreInstrumentImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.CreditImpl;
import com.github.toolarium.music.model.score.impl.CreditWordsImpl;
import com.github.toolarium.music.model.score.impl.DefaultsImpl;
import com.github.toolarium.music.model.score.impl.ScalingImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Decodes the music of a Sibelius payload (see {@link SibeliusObjectStream}) into a score: the instruments with their
 * transposition, clefs, key and time signatures, barlines, notes, slurs, hairpins, texts, tempo marks, rehearsal marks
 * and the texts of the title page.
 *
 * @author patrick
 */
public final class SibeliusScoreDecoder {
    private static final int WHOLE = 4 * SibeliusStaffDecoder.QUARTER;
    private static final int TIME_BEATS_OFFSET = 57;
    private static final int TIME_BEAT_TYPE_OFFSET = 58;
    private static final int THIRTY_SECONDS = 32;
    private static final int DEFAULT_BEATS = 4;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int FIFTHS_PER_SEMITONE = 7;
    private static final int MAX_FIFTHS = 7;
    private static final int DRUM_CHANNEL = 10;
    private static final int MAX_CHANNEL = 16;
    private static final String LIGHT_LIGHT = "light-light";
    private static final String PAGE_ALIGNED = "page_aligned.";
    private static final String MUSIC_FONT = "Opus";
    private static final String ARRANGER_PREFIX = "arr.";
    private static final String CENTER = "center";
    private static final String TOP = "top";
    private static final int PAGE_LEFT = 163;
    private static final int PAGE_CENTER = 620;
    private static final int PAGE_RIGHT = 1160;
    private static final int TITLE_Y = 1659;
    private static final int LEFT_Y = 1645;
    private static final int SUBTITLE_Y = 1590;
    private static final int BOTTOM_Y = 75;
    private static final int HEADER_Y = 1678;
    private static final int PAGE_NUMBER_LEFT = 165;
    private static final int PAGE_NUMBER_RIGHT = 1165;
    private static final Pattern PAGE_NUMBER = Pattern.compile("\\d{1,3}");
    private static final double STAFF_HEIGHT_MILLIMETERS = 6.77;
    private static final double STAFF_HEIGHT_TENTHS = 40.0;
    private static final double PAGE_HEIGHT = 1753.0;
    private static final double PAGE_WIDTH = 1240.0;
    private static final double PAGE_LEFT_MARGIN = 165.0;
    private static final double PAGE_MARGIN = 75.0;
    private static final Pattern BPM = Pattern.compile("=\\s*(\\d+(?:\\.\\d+)?)");
    private static final String[][] PROGRAMS = {
        {"piccolo", "73"}, {"flute", "74"}, {"oboe", "69"}, {"clarinet", "72"}, {"bassoon", "71"}, {"saxophone", "66"},
        {"trumpet", "57"}, {"trompete", "57"}, {"cornet", "57"}, {"flugelhorn", "57"}, {"horn", "61"}, {"trombone", "58"},
        {"posaune", "58"}, {"euphonium", "59"}, {"tuba", "59"}, {"violin", "41"}, {"viola", "42"}, {"cello", "43"},
        {"contrabass", "44"}, {"double bass", "44"}, {"guitar", "25"}, {"bass", "33"}, {"organ", "20"}, {"piano", "1"},
        {"voice", "53"}, {"soprano", "53"}, {"alto", "54"}, {"tenor", "53"}, {"choir", "53"}};


    /**
     * Private holder class for the singleton instance.
     */
    private static final class HOLDER {
        static final SibeliusScoreDecoder INSTANCE = new SibeliusScoreDecoder();
    }


    /**
     * Constructor.
     */
    private SibeliusScoreDecoder() {
        // NOP
    }


    /**
     * Gets the singleton instance.
     *
     * @return the instance
     */
    public static SibeliusScoreDecoder getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Decodes the music of a payload.
     *
     * @param payload the decompressed payload
     * @return the score, or null if the payload has no bar content stream
     */
    public IScore decode(byte[] payload) {
        SibeliusObjectStream stream = new SibeliusObjectStream(payload);
        if (!stream.parse()) {
            return null;
        }

        ScoreData score = new ScoreData();
        score.setSoftware("Sibelius");
        int bars = stream.getBars();
        int[][] times = readTimes(stream, bars);
        long[] barStarts = new long[bars + 1];
        for (int b = 0; b < bars; b++) {
            barStarts[b + 1] = barStarts[b] + (long) times[b][0] * WHOLE / times[b][1];
        }

        List<Integer> barlines = new ArrayList<>();
        for (SibeliusObject object : stream.getObjects(0, SibeliusObjectStream.BARLINE)) {
            if (object.getPosition() == SibeliusObjectStream.END_OF_BAR && object.getBar() >= 1 && object.getBar() < bars
                    && stream.getStrings(object).isEmpty()) {
                barlines.add(object.getBar());
            }
        }

        List<Integer> programs = new ArrayList<>();
        for (int staff = 1; staff <= stream.getStaves(); staff++) {
            PartData part = readPart(stream, staff, bars, times, barlines, programs);
            SibeliusStaffDecoder.getInstance().read(stream, staff, part, barStarts);
            score.getParts().add(part);
        }

        List<CreditImpl> credits = new ArrayList<>();
        readSystemTexts(stream, score, credits);

        ScoreImpl result = ScoreBuilder.getInstance().build(score);
        if (!credits.isEmpty()) {
            result.setDefaults(createDefaults());
            result.setCredits(credits);
        }
        applyInstrumentSounds(result, score, programs);
        return result;
    }


    /**
     * Reads the time signature of each bar.
     *
     * @param stream the object stream
     * @param bars the number of bars
     * @return the beats and the beat type of each bar
     */
    private int[][] readTimes(SibeliusObjectStream stream, int bars) {
        int[][] changes = new int[bars][];
        for (SibeliusObject object : stream.getObjects(0, SibeliusObjectStream.TIME)) {
            int beats = stream.u8(object.getOffset() + TIME_BEATS_OFFSET);
            int unit = stream.u8(object.getOffset() + TIME_BEAT_TYPE_OFFSET);
            if (object.getBar() >= 1 && object.getBar() <= bars && beats > 0 && unit > 0 && THIRTY_SECONDS % unit == 0) {
                changes[object.getBar() - 1] = new int[] {beats, THIRTY_SECONDS / unit};
            }
        }
        int[][] result = new int[bars][];
        int[] current = {DEFAULT_BEATS, DEFAULT_BEATS};
        for (int b = 0; b < bars; b++) {
            if (changes[b] != null) {
                current = changes[b];
            }
            result[b] = current;
        }
        return result;
    }


    /**
     * Reads the instrument, the clef, the key signatures and the measures of a staff.
     *
     * @param stream the object stream
     * @param staff the staff
     * @param bars the number of bars
     * @param times the time signature of each bar
     * @param barlines the bars with a double barline
     * @param programs the MIDI program of each part (updated)
     * @return the part
     */
    private PartData readPart(SibeliusObjectStream stream, int staff, int bars, int[][] times, List<Integer> barlines, List<Integer> programs) {
        String name = null;
        String instrument = null;
        String clef = "clef.treble";
        for (SibeliusObject object : stream.getObjects(staff, -1)) {
            if (object.getBar() != 0) {
                continue;
            }
            List<String> strings = stream.getStrings(object);
            if (object.getType() == SibeliusObjectStream.INSTRUMENT && !strings.isEmpty()) {
                name = SibeliusTextConverter.getInstance().getPlainText(strings.get(0));
                for (String text : strings) {
                    if (text.startsWith("instrument.")) {
                        instrument = text;
                    }
                }
            } else if (object.getType() == SibeliusObjectStream.CLEF && !strings.isEmpty()) {
                clef = strings.get(0);
            }
        }

        PartData part = new PartData(name);
        int[] transpose = readTranspose(stream, instrument);
        part.setTranspose(transpose[1], transpose[0]);
        int program = findProgram(instrument, name);
        programs.add(program);
        if (program > 0) {
            part.setMidiProgram(program);
            int channel = staff;
            if (channel >= DRUM_CHANNEL) {
                channel++;
            }
            part.setMidiChannel(Math.min(channel, MAX_CHANNEL));
        }
        part.getDefaultClefs().put(1, createClef(clef));

        int transposeFifths = Math.floorMod(transpose[1] * FIFTHS_PER_SEMITONE, SEMITONES_PER_OCTAVE);
        if (transposeFifths > SEMITONES_PER_OCTAVE / 2) {
            transposeFifths -= SEMITONES_PER_OCTAVE;
        }
        for (int b = 0; b < bars; b++) {
            MeasureData measure = part.measure(b);
            if (b == 0 || times[b] != times[b - 1]) {
                measure.setTime(times[b][0], times[b][1], null);
            }
            if (barlines.contains(b + 1)) {
                measure.setRightBarStyle(LIGHT_LIGHT);
            }
        }
        for (SibeliusObject object : stream.getObjects(staff, -1)) {
            if (!stream.isKeySignature(object)) {
                continue;
            }
            int fifths = writtenKey(stream.getKeyFifths(object), transposeFifths);
            if (object.getBar() == 0) {
                part.measure(0).setKey(fifths, null);
            } else if (object.getBar() < bars) {
                part.measure(object.getBar()).setKey(fifths, null);
                part.measure(object.getBar() - 1).setRightBarStyle(LIGHT_LIGHT);
            }
        }
        return part;
    }


    /**
     * Calculates the written key of a transposing instrument.
     *
     * @param concert the key in concert pitch
     * @param transposeFifths the transposition in fifths
     * @return the written key
     */
    private int writtenKey(int concert, int transposeFifths) {
        int fifths = concert - transposeFifths;
        if (fifths > MAX_FIFTHS) {
            fifths -= SEMITONES_PER_OCTAVE;
        } else if (fifths < -MAX_FIFTHS) {
            fifths += SEMITONES_PER_OCTAVE;
        }
        return fifths;
    }


    /**
     * Reads the transposition of an instrument definition: after the staff type, the clefs and the glissando type
     * follow the diatonic and the chromatic (times two) transposition as signed 16 bit values.
     *
     * @param stream the object stream
     * @param instrument the instrument ID, or null
     * @return the diatonic and the chromatic transposition (from written to sounding pitch)
     */
    private int[] readTranspose(SibeliusObjectStream stream, String instrument) {
        int[] result = {0, 0};
        if (instrument == null) {
            return result;
        }
        int offset = stream.indexOf(instrument);
        int staffType = stream.indexOf("stafftype.");
        if (offset < 0 || staffType < 0) {
            return result;
        }
        int p = offset;
        boolean afterStaffType = false;
        for (int i = 0; i < 16 && p > 0; i++) {
            List<String> strings = stream.readStrings(p, p + 400, 1);
            int end = stream.getStringEnd(p);
            if (strings.isEmpty() || end < 0) {
                break;
            }
            String text = strings.get(0);
            if (text.startsWith("stafftype.")) {
                afterStaffType = true;
            } else if (afterStaffType && text.matches("[A-Z]{4,}")) {
                result[0] = stream.s16(end);
                result[1] = stream.s16(end + 2) / 2;
                return result;
            }
            p = end;
        }
        return result;
    }


    /**
     * Finds the General MIDI program of an instrument.
     *
     * @param instrument the instrument ID, or null
     * @param name the instrument name, or null
     * @return the program (1 based), or 0 if unknown
     */
    private int findProgram(String instrument, String name) {
        String text = "";
        if (instrument != null && !instrument.contains(".user.")) {
            text = instrument.toLowerCase(Locale.ROOT);
        }
        if (name != null) {
            text = text + " " + name.toLowerCase(Locale.ROOT);
        }
        for (String[] entry : PROGRAMS) {
            if (text.contains(entry[0])) {
                return Integer.parseInt(entry[1]);
            }
        }
        return 0;
    }


    /**
     * Creates a clef from a Sibelius clef style.
     *
     * @param style the clef style, for example clef.treble or clef.bass
     * @return the clef
     */
    private com.github.toolarium.music.model.measure.impl.ClefImpl createClef(String style) {
        ScoreBuilder builder = ScoreBuilder.getInstance();
        int octave = 0;
        if (style.contains(".down")) {
            octave = -1;
        } else if (style.contains(".up")) {
            octave = 1;
        }
        if (style.startsWith("clef.bass")) {
            return builder.createClef("F", 4, octave);
        }
        if (style.startsWith("clef.alto")) {
            return builder.createClef("C", 3, octave);
        }
        if (style.startsWith("clef.tenor")) {
            return builder.createClef("C", 4, octave);
        }
        if (style.startsWith("clef.percussion")) {
            return builder.createClef("percussion", null, 0);
        }
        return builder.createClef("G", 2, octave);
    }


    /**
     * Reads the system texts: tempo marks and rehearsal marks into the first part, the title page texts into the
     * identification and the credits.
     *
     * @param stream the object stream
     * @param score the score data
     * @param credits the credits (updated)
     */
    private void readSystemTexts(SibeliusObjectStream stream, ScoreData score, List<CreditImpl> credits) {
        if (score.getParts().isEmpty()) {
            return;
        }
        PartData first = score.getParts().get(0);
        ScoreBuilder builder = ScoreBuilder.getInstance();
        int rehearsal = 0;
        List<String[]> runningTexts = new ArrayList<>();
        int pages = 1;
        for (SibeliusObject object : stream.getObjects(0, -1)) {
            if (object.getBar() < 1) {
                continue;
            }
            MeasureData measure = first.measure(object.getBar() - 1);
            Fraction position = Fraction.of(object.getPosition(), WHOLE);
            if (object.getType() == SibeliusObjectStream.REHEARSAL) {
                measure.addDirection(builder.createRehearsal(String.valueOf((char) ('A' + rehearsal))), position, 1);
                rehearsal++;
            } else if (object.getType() == SibeliusObjectStream.SYSTEM_TEXT) {
                List<String> strings = stream.getStrings(object);
                if (strings.size() < 2) {
                    continue;
                }
                String style = strings.get(1);
                if (style.contains("tempo")) {
                    measure.addDirection(createTempo(strings.get(0)), position, 1);
                } else if (style.contains(PAGE_ALIGNED) && object.getBar() == 1) {
                    addTitlePageText(score, credits, strings.get(0), style.substring(style.indexOf(PAGE_ALIGNED) + PAGE_ALIGNED.length()));
                } else if (style.contains(PAGE_ALIGNED)) {
                    // the texts of the following pages: headers and footers, and page numbers imported as texts
                    String role = style.substring(style.indexOf(PAGE_ALIGNED) + PAGE_ALIGNED.length());
                    String text = SibeliusTextConverter.getInstance().getPlainText(strings.get(0));
                    if (PAGE_NUMBER.matcher(text).matches()) {
                        pages = Math.max(pages, Integer.parseInt(text));
                    } else if (!text.isEmpty() && (role.startsWith("header") || role.startsWith("footer")) && !containsText(runningTexts, strings.get(0))) {
                        runningTexts.add(new String[] {role, strings.get(0)});
                    }
                }
            }
        }
        addRunningTexts(credits, runningTexts, pages);
    }


    /**
     * Checks whether a header or footer text was already found.
     *
     * @param runningTexts the role and the formatted text of each header and footer
     * @param formatted the formatted text
     * @return true if the text was already found
     */
    private boolean containsText(List<String[]> runningTexts, String formatted) {
        for (String[] text : runningTexts) {
            if (text[1].equals(formatted)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Adds the texts of the pages after the first page: a header and a footer on each page from page 2 and the
     * page numbers (on the left of left pages and on the right of right pages). The number of pages is the highest
     * page number found in the texts.
     *
     * @param credits the credits (updated)
     * @param runningTexts the role and the formatted text of each header and footer
     * @param pages the number of pages
     */
    private void addRunningTexts(List<CreditImpl> credits, List<String[]> runningTexts, int pages) {
        for (int page = 2; page <= pages; page++) {
            for (String[] text : runningTexts) {
                CreditImpl credit;
                if (text[0].startsWith("footer")) {
                    credit = createCredit(null, text[1], new int[] {PAGE_CENTER, BOTTOM_Y}, CENTER, "bottom");
                } else {
                    credit = createCredit(null, text[1], new int[] {PAGE_CENTER, HEADER_Y}, CENTER, TOP);
                }
                credit.setPage(page);
                credits.add(credit);
            }
            CreditImpl number;
            if (page % 2 == 0) {
                number = createCredit("page number", String.valueOf(page), new int[] {PAGE_NUMBER_LEFT, HEADER_Y}, "left", TOP);
            } else {
                number = createCredit("page number", String.valueOf(page), new int[] {PAGE_NUMBER_RIGHT, HEADER_Y}, "right", TOP);
            }
            number.setPage(page);
            credits.add(number);
        }
    }


    /**
     * Creates a tempo mark: the words before the metronome mark, the note value and the beats per minute.
     *
     * @param formatted the formatted text
     * @return the tempo direction
     */
    private com.github.toolarium.music.model.direction.impl.DirectionImpl createTempo(String formatted) {
        StringBuilder words = new StringBuilder();
        String glyph = null;
        for (SibeliusTextRun run : SibeliusTextConverter.getInstance().getRuns(formatted)) {
            if (run.getFont() != null && run.getFont().startsWith(MUSIC_FONT)) {
                if (glyph == null) {
                    glyph = run.getText().trim();
                }
            } else if (glyph == null) {
                words.append(run.getText());
            }
        }
        String beatUnit = "quarter";
        int dots = 0;
        if (glyph != null && !glyph.isEmpty()) {
            beatUnit = beatUnit(glyph.charAt(0));
            if (glyph.endsWith(".")) {
                dots = 1;
            }
        }
        double bpm = 0;
        Matcher matcher = BPM.matcher(SibeliusTextConverter.getInstance().getPlainText(formatted));
        if (matcher.find()) {
            bpm = Double.parseDouble(matcher.group(1));
        }
        return ScoreBuilder.getInstance().createTempo(bpm, beatUnit, dots, words.toString().trim());
    }


    /**
     * Gets the note value of a note glyph of the music text font.
     *
     * @param glyph the glyph
     * @return the note value
     */
    private String beatUnit(char glyph) {
        switch (glyph) {
            case 'w':
                return "whole";
            case 'h':
                return "half";
            case 'e':
                return "eighth";
            case 'x':
                return "16th";
            default:
                return "quarter";
        }
    }


    /**
     * Adds a text of the title page to the identification and the credits.
     *
     * @param score the score data
     * @param credits the credits (updated)
     * @param formatted the formatted text
     * @param role the role of the text: title, subtitle, composer, lyricist, copyright, header, footer or other
     */
    private void addTitlePageText(ScoreData score, List<CreditImpl> credits, String formatted, String role) {
        String text = SibeliusTextConverter.getInstance().getPlainText(formatted);
        if (text.isEmpty() || role.startsWith("header") || role.startsWith("footer")) {
            return;
        }
        String type = null;
        int[] position = {PAGE_LEFT, LEFT_Y};
        String justify = "left";
        String valign = TOP;
        if (role.startsWith("title")) {
            type = "title";
            position = new int[] {PAGE_CENTER, TITLE_Y};
            justify = CENTER;
            score.setTitle(text);
        } else if (role.startsWith("subtitle")) {
            type = "subtitle";
            position = new int[] {PAGE_CENTER, SUBTITLE_Y};
            justify = CENTER;
            score.setSubtitle(text);
        } else if (role.startsWith("composer")) {
            type = "composer";
            position = new int[] {PAGE_RIGHT, SUBTITLE_Y};
            justify = "right";
            addComposers(score, text);
        } else if (role.startsWith("lyricist") && !text.contains("\n")) {
            // the style of the text at the top left is also used for other information, for example the kind of score
            type = "lyricist";
            position = new int[] {PAGE_LEFT, SUBTITLE_Y};
            score.addCreator("lyricist", text);
        } else if (role.startsWith("copyright")) {
            type = "rights";
            position = new int[] {PAGE_CENTER, BOTTOM_Y};
            justify = CENTER;
            valign = "bottom";
            score.setRights(text);
        }
        credits.add(createCredit(type, formatted, position, justify, valign));
    }


    /**
     * Adds the composers and arrangers of a composer text: lines starting with arr. name an arranger.
     *
     * @param score the score data
     * @param text the composer text
     */
    private void addComposers(ScoreData score, String text) {
        StringBuilder composers = new StringBuilder();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith(ARRANGER_PREFIX)) {
                score.addCreator("arranger", trimmed.substring(ARRANGER_PREFIX.length()).trim());
            } else if (!trimmed.isEmpty()) {
                if (composers.length() > 0) {
                    composers.append('\n');
                }
                composers.append(trimmed);
            }
        }
        if (composers.length() > 0) {
            score.addCreator("composer", composers.toString());
        }
    }


    /**
     * Creates a credit of the first page with a credit words element for each formatted part of the text.
     *
     * @param type the credit type, or null
     * @param formatted the formatted text
     * @param position the default-x and default-y in tenths
     * @param justify the justification
     * @param valign the vertical alignment
     * @return the credit
     */
    private CreditImpl createCredit(String type, String formatted, int[] position, String justify, String valign) {
        List<CreditWordsImpl> words = new ArrayList<>();
        for (SibeliusTextRun run : SibeliusTextConverter.getInstance().getRuns(formatted)) {
            CreditWordsImpl word = new CreditWordsImpl();
            word.setValue(run.getText());
            if (words.isEmpty()) {
                word.setDefaultX(String.valueOf(position[0]));
                word.setDefaultY(String.valueOf(position[1]));
                word.setJustify(justify);
                word.setValign(valign);
            }
            if (run.getFont() != null) {
                word.setFontFamily(run.getFont());
            }
            if (run.getSize() != null) {
                word.setFontSize(String.valueOf(run.getSize()));
            }
            if (run.isBold()) {
                word.setFontWeight("bold");
            }
            if (run.isItalic()) {
                word.setFontStyle("italic");
            }
            words.add(word);
        }
        CreditImpl credit = new CreditImpl();
        credit.setPage(1);
        if (type != null) {
            credit.setCreditType(type);
        }
        credit.setCreditWordsList(words);
        return credit;
    }


    /**
     * Creates the page layout the positions of the title page texts refer to: an A4 page in tenths.
     *
     * @return the defaults
     */
    private DefaultsImpl createDefaults() {
        ScalingImpl scaling = new ScalingImpl();
        scaling.setMillimeters(STAFF_HEIGHT_MILLIMETERS);
        scaling.setTenths(STAFF_HEIGHT_TENTHS);
        PageMarginsImpl margins = new PageMarginsImpl();
        margins.setType("both");
        margins.setLeftMargin(PAGE_LEFT_MARGIN);
        margins.setRightMargin(PAGE_MARGIN);
        margins.setTopMargin(PAGE_MARGIN);
        margins.setBottomMargin(PAGE_MARGIN);
        PageLayoutImpl layout = new PageLayoutImpl();
        layout.setPageHeight(PAGE_HEIGHT);
        layout.setPageWidth(PAGE_WIDTH);
        List<PageMarginsImpl> marginList = new ArrayList<>();
        marginList.add(margins);
        layout.setPageMargins(marginList);
        DefaultsImpl defaults = new DefaultsImpl();
        defaults.setScaling(scaling);
        defaults.setPageLayout(layout);
        return defaults;
    }


    /**
     * Sets the instrument sound of each part from its MIDI program and transposition.
     *
     * @param score the score
     * @param data the score data
     * @param programs the MIDI program of each part
     */
    private void applyInstrumentSounds(ScoreImpl score, ScoreData data, List<Integer> programs) {
        if (score.getPartList() == null || score.getPartList().getScoreParts() == null) {
            return;
        }
        List<ScorePartImpl> scoreParts = score.getPartList().getScoreParts();
        for (int i = 0; i < scoreParts.size() && i < programs.size(); i++) {
            List<ScoreInstrumentImpl> instruments = scoreParts.get(i).getScoreInstruments();
            if (programs.get(i) > 0 && instruments != null && !instruments.isEmpty()) {
                String sound = InstrumentSoundConverter.getInstance().getSound(programs.get(i), data.getParts().get(i).getTransposeChromatic());
                instruments.get(0).setInstrumentSound(sound);
            }
        }
    }
}
