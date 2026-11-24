/*
 * AbcScoreWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl;

import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterPlan;
import com.github.toolarium.music.io.format.abc.impl.dto.AbcWriterStaff;
import com.github.toolarium.music.model.score.ICreator;
import com.github.toolarium.music.model.score.ICredit;
import com.github.toolarium.music.model.score.ICreditWords;
import com.github.toolarium.music.model.score.IScore;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Writes a score as ABC 2.1 tune: the header (title, subtitle, composer, arranger, rights, meter, unit note length),
 * the staff grouping (%%score), one voice definition per voice with name, clef and transposition, and the music of
 * every voice.
 *
 * @author patrick
 */
public final class AbcScoreWriter {
    private static final String NEWLINE = "\n";
    private static final String ARRANGER_PREFIX = "arr. ";
    private static final String LYRICIST_PREFIX = "words: ";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final AbcScoreWriter INSTANCE = new AbcScoreWriter();
    }


    /**
     * Constructor.
     */
    private AbcScoreWriter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static AbcScoreWriter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Writes a score as ABC tune.
     *
     * @param score the score
     * @return the ABC text (UTF-8)
     */
    public byte[] write(IScore score) {
        final AbcWriterPlan plan = AbcWriterPlanner.getInstance().plan(score);
        StringBuilder out = new StringBuilder();
        out.append("X:1").append(NEWLINE);
        writeHeader(out, score);
        if (!plan.getMeasures().isEmpty()) {
            out.append("M:").append(AbcVoiceWriter.meter(plan.getMeasures().get(0))).append(NEWLINE);
        }
        out.append("L:").append(AbcNotationWriter.UNIT.getNumerator()).append('/').append(AbcNotationWriter.UNIT.getDenominator()).append(NEWLINE);
        out.append("%%score ").append(scoreLayout(plan)).append(NEWLINE);
        for (AbcWriterStaff staff : plan.getStaves()) {
            boolean first = true;
            for (int voice : staff.getVoices()) {
                if (staff.getProgram() != null) {
                    out.append("%%MIDI program ").append(staff.getProgram() - 1).append(NEWLINE);
                }
                out.append("V:").append(voiceId(staff, voice)).append(voiceProperties(staff, first)).append(NEWLINE);
                first = false;
            }
        }
        out.append("K:C").append(NEWLINE);
        for (AbcWriterStaff staff : plan.getStaves()) {
            boolean primary = true;
            for (int voice : staff.getVoices()) {
                String id = voiceId(staff, voice);
                out.append("V:").append(id).append(NEWLINE);
                for (String line : new AbcVoiceWriter(plan, staff, voice, primary, id).write()) {
                    out.append(line).append(NEWLINE);
                }
                primary = false;
            }
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }


    /**
     * Writes the header fields: title and subtitle lines (T:), composers (C:, the arranger and lyricist with a
     * prefix), other creators (Z:) and the rights (N:).
     *
     * @param out the output
     * @param score the score
     */
    private void writeHeader(StringBuilder out, IScore score) {
        AbcNotationWriter writer = AbcNotationWriter.getInstance();
        String title = title(score);
        out.append("T:").append(writer.clean(nonNull(title))).append(NEWLINE);
        String subtitle = credit(score, "subtitle");
        if (subtitle != null) {
            for (String line : subtitle.split("\r?\n")) {
                if (!line.trim().isEmpty()) {
                    out.append("T:").append(writer.clean(line)).append(NEWLINE);
                }
            }
        }
        if (score.getIdentification() != null && score.getIdentification().getCreators() != null) {
            for (ICreator creator : score.getIdentification().getCreators()) {
                if (creator.getValue() == null || creator.getValue().trim().isEmpty()) {
                    continue;
                }
                for (String line : creator.getValue().split("\r?\n")) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    if ("arranger".equals(creator.getType())) {
                        out.append("C:").append(ARRANGER_PREFIX).append(writer.clean(line)).append(NEWLINE);
                    } else if ("lyricist".equals(creator.getType()) || "poet".equals(creator.getType())) {
                        out.append("C:").append(LYRICIST_PREFIX).append(writer.clean(line)).append(NEWLINE);
                    } else if ("composer".equals(creator.getType()) || creator.getType() == null) {
                        out.append("C:").append(writer.clean(line)).append(NEWLINE);
                    } else {
                        out.append("Z:").append(writer.clean(line)).append(NEWLINE);
                    }
                }
            }
        }
        if (score.getIdentification() != null && score.getIdentification().getRights() != null) {
            for (String line : score.getIdentification().getRights().split("\r?\n")) {
                if (!line.trim().isEmpty()) {
                    out.append("N:").append(writer.clean(line)).append(NEWLINE);
                }
            }
        }
    }


    /**
     * Gets the staff layout (%%score): the voices of a staff in parentheses, the staves of a part in braces.
     *
     * @param plan the plan
     * @return the layout
     */
    private String scoreLayout(AbcWriterPlan plan) {
        List<String> parts = new ArrayList<>();
        List<AbcWriterStaff> staves = plan.getStaves();
        int i = 0;
        while (i < staves.size()) {
            int part = staves.get(i).getPartIndex();
            List<String> group = new ArrayList<>();
            while (i < staves.size() && staves.get(i).getPartIndex() == part) {
                AbcWriterStaff staff = staves.get(i);
                List<String> ids = new ArrayList<>();
                for (int voice : staff.getVoices()) {
                    ids.add(voiceId(staff, voice));
                }
                if (ids.size() == 1) {
                    group.add(ids.get(0));
                } else {
                    group.add("(" + String.join(" ", ids) + ")");
                }
                i++;
            }
            if (group.size() == 1) {
                parts.add(group.get(0));
            } else {
                parts.add("{" + String.join(" ", group) + "}");
            }
        }
        return String.join(" ", parts);
    }


    /**
     * Gets the properties of a voice definition: name, short name, clef and transposition.
     *
     * @param staff the staff
     * @param first true for the first voice of the staff (with the names of a part's first staff)
     * @return the properties with a leading space
     */
    private String voiceProperties(AbcWriterStaff staff, boolean first) {
        AbcNotationWriter writer = AbcNotationWriter.getInstance();
        StringBuilder result = new StringBuilder();
        if (first && staff.getStaffInPart() == 1 && staff.getLabel() != null) {
            result.append(" name=\"").append(writer.clean(staff.getLabel())).append('"');
        }
        if (first && staff.getStaffInPart() == 1 && staff.getAbbreviation() != null) {
            result.append(" sname=\"").append(writer.clean(staff.getAbbreviation())).append('"');
        }
        result.append(" clef=").append(writer.clef(staff.getClef()));
        if (staff.getTransposeSemitones() != null && staff.getTransposeSemitones() != 0) {
            result.append(" transpose=").append(staff.getTransposeSemitones());
        }
        return result.toString();
    }


    /**
     * Gets the ABC ID of a voice.
     *
     * @param staff the staff
     * @param voice the voice number
     * @return the ID, for example S1V1
     */
    private String voiceId(AbcWriterStaff staff, int voice) {
        return "S" + staff.getNumber() + "V" + voice;
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


    /**
     * Gets a text or an empty text.
     *
     * @param text the text, or null
     * @return the text
     */
    private String nonNull(String text) {
        if (text == null) {
            return "";
        }
        return text;
    }
}
