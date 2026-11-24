/*
 * LilyPondScoreWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond.impl;

import com.github.toolarium.music.io.converter.NoteValueConverter;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.TypeAndDots;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondWriterMark;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondWriterMeasure;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondWriterPlan;
import com.github.toolarium.music.io.format.lilypond.impl.dto.LilyPondWriterStaff;
import com.github.toolarium.music.model.score.ICreator;
import com.github.toolarium.music.model.score.ICredit;
import com.github.toolarium.music.model.score.ICreditWords;
import com.github.toolarium.music.model.score.IScore;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Writes a score as LilyPond source (version 2.24, Dutch note names, absolute octaves): the header, the chord
 * names, one staff per staff of a part (a part with several staves is a piano staff) with one named voice per voice,
 * and the lyrics of the voices. Repeats with endings become \repeat volta with \alternative.
 *
 * @author patrick
 */
public final class LilyPondScoreWriter {
    private static final String VERSION = "2.24.0";
    private static final String INDENT = "  ";
    private static final String NEWLINE = "\n";
    private static final String VOLTA_COUNT = "@COUNT@";
    private static final String[] VOICE_COMMANDS = {"\\voiceOne", "\\voiceTwo", "\\voiceThree", "\\voiceFour"};


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final LilyPondScoreWriter INSTANCE = new LilyPondScoreWriter();
    }


    /**
     * Constructor.
     */
    private LilyPondScoreWriter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static LilyPondScoreWriter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Writes a score as LilyPond source.
     *
     * @param score the score
     * @return the source (UTF-8)
     */
    public byte[] write(IScore score) {
        final LilyPondWriterPlan plan = LilyPondWriterPlanner.getInstance().plan(score);
        StringBuilder out = new StringBuilder();
        out.append("\\version \"").append(VERSION).append('"').append(NEWLINE).append(NEWLINE);
        writeHeader(out, score);
        String[][] structure = repeatStructure(plan.getMeasures());
        out.append("\\score {").append(NEWLINE).append(INDENT).append("<<").append(NEWLINE);
        List<LilyPondWriterStaff> staves = plan.getStaves();
        int i = 0;
        while (i < staves.size()) {
            int part = staves.get(i).getPartIndex();
            int end = i;
            while (end + 1 < staves.size() && staves.get(end + 1).getPartIndex() == part) {
                end++;
            }
            if (plan.getPartsWithHarmonies().contains(part)) {
                writeChordNames(out, plan, part);
            }
            Map<String, Map<String, List<String>>> lyrics = new LinkedHashMap<>();
            if (end > i) {
                LilyPondWriterStaff first = staves.get(i);
                out.append(INDENT).append(INDENT).append("\\new PianoStaff").append(with(first, false)).append(" <<").append(NEWLINE);
                for (int s = i; s <= end; s++) {
                    writeStaff(out, plan, staves.get(s), structure, lyrics, INDENT + INDENT + INDENT, false);
                }
                out.append(INDENT).append(INDENT).append(">>").append(NEWLINE);
            } else {
                writeStaff(out, plan, staves.get(i), structure, lyrics, INDENT + INDENT, true);
            }
            writeLyrics(out, lyrics);
            i = end + 1;
        }
        out.append(INDENT).append(">>").append(NEWLINE);
        out.append(INDENT).append("\\layout { }").append(NEWLINE);
        out.append(INDENT).append("\\midi { }").append(NEWLINE);
        out.append('}').append(NEWLINE);
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }


    /**
     * Writes the header: title, subtitle, composer, arranger, poet and copyright.
     *
     * @param out the output
     * @param score the score
     */
    private void writeHeader(StringBuilder out, IScore score) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", title(score));
        fields.put("subtitle", credit(score, "subtitle"));
        if (score.getIdentification() != null && score.getIdentification().getCreators() != null) {
            for (ICreator creator : score.getIdentification().getCreators()) {
                String key = "composer";
                if ("arranger".equals(creator.getType())) {
                    key = "arranger";
                } else if ("lyricist".equals(creator.getType()) || "poet".equals(creator.getType())) {
                    key = "poet";
                }
                if (creator.getValue() != null && !creator.getValue().trim().isEmpty()) {
                    fields.putIfAbsent(key, creator.getValue().trim());
                }
            }
        }
        if (score.getIdentification() != null) {
            fields.put("copyright", score.getIdentification().getRights());
        }
        out.append("\\header {").append(NEWLINE);
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (field.getValue() == null || field.getValue().trim().isEmpty()) {
                continue;
            }
            out.append(INDENT).append(field.getKey()).append(" = ").append(headerText(field.getValue().trim())).append(NEWLINE);
        }
        out.append(INDENT).append("tagline = ##f").append(NEWLINE);
        out.append('}').append(NEWLINE).append(NEWLINE);
    }


    /**
     * Gets the text of a header field: a string, or a markup column for several lines.
     *
     * @param text the text
     * @return the LilyPond text
     */
    private String headerText(String text) {
        LilyPondNotationWriter writer = LilyPondNotationWriter.getInstance();
        String[] lines = text.split("\r?\n");
        if (lines.length == 1) {
            return writer.string(text);
        }
        StringBuilder result = new StringBuilder("\\markup \\center-column {");
        for (String line : lines) {
            result.append(' ').append(writer.string(line.trim()));
        }
        return result.append(" }").toString();
    }


    /**
     * Gets the with block of a staff or piano staff: instrument names and MIDI instrument.
     *
     * @param staff the staff (the first staff of a piano staff)
     * @param midi true to write the MIDI instrument
     * @return the with block, or an empty text
     */
    private String with(LilyPondWriterStaff staff, boolean midi) {
        LilyPondNotationWriter writer = LilyPondNotationWriter.getInstance();
        List<String> settings = new ArrayList<>();
        if (staff.getLabel() != null) {
            settings.add("instrumentName = " + writer.string(staff.getLabel()));
        }
        if (staff.getAbbreviation() != null) {
            settings.add("shortInstrumentName = " + writer.string(staff.getAbbreviation()));
        }
        String instrument = LilyPondInstrumentConverter.getInstance().getName(staff.getProgram());
        if (midi && instrument != null) {
            settings.add("midiInstrument = " + writer.string(instrument));
        }
        if (settings.isEmpty()) {
            return "";
        }
        return " \\with { " + String.join(" ", settings) + " }";
    }


    /**
     * Writes a staff with its voices.
     *
     * @param out the output
     * @param plan the plan
     * @param staff the staff
     * @param structure the repeat structure (tokens before and after every measure)
     * @param lyrics the lyrics of the voices by voice name and verse (filled)
     * @param indent the indentation
     * @param names true to write the instrument names (a staff of its own)
     */
    private void writeStaff(StringBuilder out, LilyPondWriterPlan plan, LilyPondWriterStaff staff, String[][] structure,
            Map<String, Map<String, List<String>>> lyrics, String indent, boolean names) {
        String with;
        if (names) {
            with = with(staff, true);
        } else {
            LilyPondWriterStaff copy = new LilyPondWriterStaff(staff.getNumber(), staff.getPartIndex(), staff.getStaffInPart());
            copy.setProgram(staff.getProgram());
            with = with(copy, true);
        }
        out.append(indent).append("\\new Staff = \"staff").append(staff.getNumber()).append('"').append(with).append(" <<").append(NEWLINE);
        int index = 0;
        for (int voice : staff.getVoices()) {
            String name = "staff" + staff.getNumber() + "voice" + voice;
            LilyPondVoiceWriter voiceWriter = new LilyPondVoiceWriter(plan, staff, voice, index == 0);
            out.append(indent).append(INDENT).append("\\new Voice = \"").append(name).append("\" {").append(NEWLINE);
            if (staff.getVoices().size() > 1 && index < VOICE_COMMANDS.length) {
                out.append(indent).append(INDENT).append(INDENT).append(VOICE_COMMANDS[index]).append(NEWLINE);
            }
            for (String line : voiceWriter.write(structure)) {
                out.append(indent).append(INDENT).append(INDENT).append(line).append(NEWLINE);
            }
            out.append(indent).append(INDENT).append('}').append(NEWLINE);
            if (!voiceWriter.getVerses().isEmpty()) {
                lyrics.put(name, voiceWriter.getVerses());
            }
            index++;
        }
        out.append(indent).append(">>").append(NEWLINE);
    }


    /**
     * Writes the lyrics of the voices of a part.
     *
     * @param out the output
     * @param lyrics the lyrics by voice name and verse
     */
    private void writeLyrics(StringBuilder out, Map<String, Map<String, List<String>>> lyrics) {
        for (Map.Entry<String, Map<String, List<String>>> voice : lyrics.entrySet()) {
            for (List<String> verse : voice.getValue().values()) {
                out.append(INDENT).append(INDENT).append("\\new Lyrics \\lyricsto \"").append(voice.getKey()).append("\" {").append(NEWLINE);
                StringBuilder line = new StringBuilder();
                for (String syllable : verse) {
                    if (line.length() > 100) {
                        out.append(INDENT).append(INDENT).append(INDENT).append(line.toString().trim()).append(NEWLINE);
                        line.setLength(0);
                    }
                    line.append(syllable).append(' ');
                }
                if (line.length() > 0) {
                    out.append(INDENT).append(INDENT).append(INDENT).append(line.toString().trim()).append(NEWLINE);
                }
                out.append(INDENT).append(INDENT).append('}').append(NEWLINE);
            }
        }
    }


    /**
     * Writes the chord names of a part.
     *
     * @param out the output
     * @param plan the plan
     * @param part the part index
     */
    private void writeChordNames(StringBuilder out, LilyPondWriterPlan plan, int part) {
        out.append(INDENT).append(INDENT).append("\\new ChordNames \\chordmode {").append(NEWLINE);
        for (LilyPondWriterMeasure measure : plan.getMeasures()) {
            List<String> tokens = new ArrayList<>();
            List<LilyPondWriterMark> chords = new ArrayList<>(measure.getHarmonies().getOrDefault(part, new ArrayList<>()));
            chords.sort((a, b) -> a.getPosition().compareTo(b.getPosition()));
            Fraction cursor = Fraction.ZERO;
            for (int c = 0; c <= chords.size(); c++) {
                Fraction next = measure.getLength();
                if (c < chords.size()) {
                    next = chords.get(c).getPosition().min(measure.getLength());
                }
                if (next.compareTo(cursor) > 0) {
                    if (c == 0) {
                        spacers(tokens, "s", next.subtract(cursor));
                    } else {
                        String[] chord = chords.get(c - 1).getText().split("\t", -1);
                        List<String> pieces = new ArrayList<>();
                        spacers(pieces, "", next.subtract(cursor));
                        tokens.add(chord[0] + pieces.get(0) + chord[1] + chord[2]);
                        for (int p = 1; p < pieces.size(); p++) {
                            tokens.add("s" + pieces.get(p));
                        }
                    }
                    cursor = next;
                }
            }
            out.append(INDENT).append(INDENT).append(INDENT).append(String.join(" ", tokens)).append(" |").append(NEWLINE);
        }
        out.append(INDENT).append(INDENT).append('}').append(NEWLINE);
    }


    /**
     * Adds tokens of note values that fill a length.
     *
     * @param tokens the tokens (added to)
     * @param prefix the prefix of every token (for example s)
     * @param length the length
     */
    static void spacers(List<String> tokens, String prefix, Fraction length) {
        List<TypeAndDots> values = NoteValueConverter.getInstance().split(length);
        if (values.isEmpty()) {
            tokens.add(prefix + LilyPondNotationWriter.getInstance().duration(length));
            return;
        }
        Fraction rest = length;
        for (TypeAndDots value : values) {
            tokens.add(prefix + LilyPondNotationWriter.getInstance().duration(value.getType(), value.getDots()));
            rest = rest.subtract(NoteValueConverter.getInstance().toFraction(value.getType(), value.getDots()));
        }
        if (rest.isPositive()) {
            tokens.add(prefix + LilyPondNotationWriter.getInstance().duration(rest));
        }
    }


    /**
     * Computes the repeat structure: \repeat volta before the repeated measures, \alternative around the endings.
     *
     * @param measures the measures
     * @return the tokens before ([0][m]) and after ([1][m]) every measure
     */
    private String[][] repeatStructure(List<LilyPondWriterMeasure> measures) {
        int n = measures.size();
        String[] before = new String[n];
        String[] after = new String[n];
        Arrays.fill(before, "");
        Arrays.fill(after, "");
        int open = -1;
        int lastClose = 0;
        int count = 2;
        int endings = 0;
        boolean inAlternative = false;
        boolean inEnding = false;
        for (int m = 0; m < n; m++) {
            LilyPondWriterMeasure measure = measures.get(m);
            if (measure.isRepeatStart() && open < 0 && !inAlternative) {
                open = m;
                before[m] = before[m] + "\\repeat volta " + VOLTA_COUNT + " { ";
                count = 2;
            }
            if (measure.getEndingStart() != null) {
                if (!inAlternative) {
                    if (open < 0) {
                        open = lastClose;
                        before[open] = "\\repeat volta " + VOLTA_COUNT + " { " + before[open];
                        count = 2;
                    }
                    before[m] = before[m] + "} \\alternative { { ";
                    inAlternative = true;
                    endings = 0;
                } else if (inEnding) {
                    before[m] = before[m] + "} { ";
                } else {
                    before[m] = before[m] + "{ ";
                }
                inEnding = true;
                endings += Math.max(1, measure.getEndingStart().split("[,\\s]+").length);
            }
            boolean nextEnding = m + 1 < n && measures.get(m + 1).getEndingStart() != null;
            if (inAlternative) {
                if (measure.isRepeatEnd()) {
                    count = Math.max(count, measure.getRepeatTimes());
                }
                if (measure.isEndingStop() || nextEnding) {
                    if (nextEnding) {
                        after[m] = after[m] + " }";
                        inEnding = false;
                    } else {
                        after[m] = after[m] + " } }";
                        before[open] = before[open].replace(VOLTA_COUNT, String.valueOf(Math.max(count, endings)));
                        inAlternative = false;
                        inEnding = false;
                        open = -1;
                        lastClose = m + 1;
                    }
                }
            } else if (measure.isRepeatEnd() && !nextEnding) {
                if (open < 0) {
                    open = lastClose;
                    before[open] = "\\repeat volta " + VOLTA_COUNT + " { " + before[open];
                }
                after[m] = after[m] + " }";
                before[open] = before[open].replace(VOLTA_COUNT, String.valueOf(Math.max(2, measure.getRepeatTimes())));
                open = -1;
                lastClose = m + 1;
            }
        }
        if (n > 0) {
            if (inAlternative) {
                after[n - 1] = after[n - 1] + " } }";
            } else if (open >= 0) {
                after[n - 1] = after[n - 1] + " }";
            }
            for (int m = 0; m < n; m++) {
                before[m] = before[m].replace(VOLTA_COUNT, String.valueOf(Math.max(2, Math.max(count, endings))));
            }
        }
        return new String[][] {before, after};
    }


    /**
     * Gets the title of a score: the work title or the movement title.
     *
     * @param score the score
     * @return the title, or null
     */
    private String title(IScore score) {
        if (score.getWork() != null && score.getWork().getWorkTitle() != null && !score.getWork().getWorkTitle().trim().isEmpty()) {
            return score.getWork().getWorkTitle().trim();
        }
        if (score.getMovementTitle() != null && !score.getMovementTitle().trim().isEmpty()) {
            return score.getMovementTitle().trim();
        }
        return credit(score, "title");
    }


    /**
     * Gets the text of a credit of a type.
     *
     * @param score the score
     * @param type the credit type (for example subtitle)
     * @return the text, or null
     */
    private String credit(IScore score, String type) {
        if (score.getCredits() == null) {
            return null;
        }
        for (ICredit credit : score.getCredits()) {
            if (!type.equals(credit.getCreditType())) {
                continue;
            }
            StringBuilder text = new StringBuilder();
            if (credit.getCreditWordsList() != null) {
                for (ICreditWords words : credit.getCreditWordsList()) {
                    if (words.getValue() != null) {
                        text.append(words.getValue());
                    }
                }
            } else if (credit.getCreditWords() != null && credit.getCreditWords().getValue() != null) {
                text.append(credit.getCreditWords().getValue());
            }
            if (text.toString().trim().length() > 0) {
                return text.toString().trim();
            }
        }
        return null;
    }
}
