/*
 * MeiScoreWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl;

import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterControl;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterMeasure;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterPlan;
import com.github.toolarium.music.io.format.mei.impl.dto.MeiWriterStaff;
import com.github.toolarium.music.model.score.ICreator;
import com.github.toolarium.music.model.score.ICredit;
import com.github.toolarium.music.model.score.ICreditWords;
import com.github.toolarium.music.model.score.IScore;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Writes a score as MEI 5 (common music notation): the header with title, creators and rights, the score
 * definition with the staff groups of the parts, and the measures with their staves, layers and control events.
 *
 * @author patrick
 */
public final class MeiScoreWriter {
    private static final String MEI_NAMESPACE = "http://www.music-encoding.org/ns/mei";
    private static final String MEI_VERSION = "5.0";
    private static final String SCHEMA = "https://music-encoding.org/schema/5.0/mei-CMN.rng";
    private static final String STAFF_GRP = "staffGrp";
    private static final String STAFF_DEF = "staffDef";
    private static final String SCORE_DEF = "scoreDef";
    private static final String LINES = "lines";
    private static final String TITLE = "title";
    private static final String APPLICATION = "toolarium-music";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MeiScoreWriter INSTANCE = new MeiScoreWriter();
    }


    /**
     * Constructor.
     */
    private MeiScoreWriter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MeiScoreWriter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Writes a score as MEI.
     *
     * @param score the score
     * @return the MEI document (UTF-8)
     */
    public byte[] write(IScore score) {
        final MeiWriterPlan plan = MeiMeasurePlanner.getInstance().plan(score);
        MeiXmlOutput out = new MeiXmlOutput();
        out.raw("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.raw("<?xml-model href=\"" + SCHEMA + "\" type=\"application/xml\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>\n");
        out.open("mei", "xmlns", MEI_NAMESPACE, "meiversion", MEI_VERSION);
        writeHead(out, score);
        out.open("music");
        out.open("body");
        out.open("mdiv");
        out.open("score");
        writeScoreDef(out, score, plan);
        out.open("section");
        writeMeasures(out, plan);
        out.close();
        out.close();
        out.close();
        out.close();
        out.close();
        out.close();
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }


    /**
     * Writes the header: title, subtitle, creators, rights and the application.
     *
     * @param out the output
     * @param score the score
     */
    private void writeHead(MeiXmlOutput out, IScore score) {
        out.open("meiHead");
        out.open("fileDesc");
        out.open("titleStmt");
        out.leaf(TITLE, nonNull(title(score)));
        String subtitle = credit(score, "subtitle");
        if (subtitle != null) {
            out.openInline(TITLE, "type", "subordinate");
            out.text(subtitle);
            out.closeInline(TITLE);
        }
        List<String[]> persons = new ArrayList<>();
        if (score.getIdentification() != null && score.getIdentification().getCreators() != null) {
            for (ICreator creator : score.getIdentification().getCreators()) {
                if (creator.getValue() == null || creator.getValue().trim().isEmpty()) {
                    continue;
                }
                String role = creator.getType();
                if (role == null) {
                    role = "composer";
                }
                if ("composer".equals(role) || "arranger".equals(role) || "lyricist".equals(role)) {
                    out.leaf(role, creator.getValue().trim());
                } else {
                    persons.add(new String[] {role, creator.getValue().trim()});
                }
            }
        }
        if (!persons.isEmpty()) {
            out.open("respStmt");
            for (String[] person : persons) {
                out.openInline("persName", "role", person[0]);
                out.text(person[1]);
                out.closeInline("persName");
            }
            out.close();
        }
        out.close();
        int pages = MeiPageTextWriter.getInstance().pageCount(score);
        if (pages > 0) {
            out.openInline("extent", "unit", "page", "quantity", String.valueOf(pages));
            out.text(pages + " pages");
            out.closeInline("extent");
        }
        out.open("pubStmt");
        if (score.getIdentification() != null && score.getIdentification().getRights() != null
                && !score.getIdentification().getRights().trim().isEmpty()) {
            out.open("availability");
            out.leaf("useRestrict", score.getIdentification().getRights().trim());
            out.close();
        }
        out.close();
        out.close();
        out.open("encodingDesc");
        out.open("appInfo");
        out.open("application");
        out.leaf("name", APPLICATION);
        out.close();
        out.close();
        out.close();
        out.close();
    }


    /**
     * Writes the initial score definition: meter, page layout, page texts and the staff groups of the parts (a part with several staves is a
     * braced group with the name of the part).
     *
     * @param out the output
     * @param score the score (for the page layout and the page texts)
     * @param plan the plan
     */
    private void writeScoreDef(MeiXmlOutput out, IScore score, MeiWriterPlan plan) {
        List<String> attributes = new ArrayList<>();
        if (!plan.getMeasures().isEmpty()) {
            MeiWriterMeasure first = plan.getMeasures().get(0);
            attributes.add("meter.count");
            attributes.add(String.valueOf(first.getMeterCount()));
            attributes.add("meter.unit");
            attributes.add(String.valueOf(first.getMeterUnit()));
        }
        attributes.addAll(MeiPageTextWriter.getInstance().pageAttributes(score));
        out.open(SCORE_DEF, attributes.toArray(new String[0]));
        MeiPageTextWriter.getInstance().write(out, score);
        out.open(STAFF_GRP, "symbol", "bracket", "bar.thru", "false");
        Map<Integer, String> keys = keysOfFirstMeasure(plan);
        Map<Integer, String> modes = modesOfFirstMeasure(plan);
        int i = 0;
        List<MeiWriterStaff> staves = plan.getStaves();
        while (i < staves.size()) {
            int part = staves.get(i).getPartIndex();
            int end = i;
            while (end + 1 < staves.size() && staves.get(end + 1).getPartIndex() == part) {
                end++;
            }
            if (end > i) {
                out.open(STAFF_GRP, "symbol", "brace", "bar.thru", "true");
                writeLabels(out, staves.get(i));
                for (int s = i; s <= end; s++) {
                    writeStaffDef(out, staves.get(s), false, keys.get(staves.get(s).getNumber()), modes.get(staves.get(s).getNumber()), s == i);
                }
                out.close();
            } else {
                writeStaffDef(out, staves.get(i), true, keys.get(staves.get(i).getNumber()), modes.get(staves.get(i).getNumber()), true);
            }
            i = end + 1;
        }
        out.close();
        out.close();
    }


    /**
     * Writes a staff definition.
     *
     * @param out the output
     * @param staff the staff
     * @param labels true to write the name of the part
     * @param keySig the key signature, or null
     * @param mode the mode, or null
     * @param instrument true to write the MIDI instrument
     */
    private void writeStaffDef(MeiXmlOutput out, MeiWriterStaff staff, boolean labels, String keySig, String mode, boolean instrument) {
        List<String> attributes = new ArrayList<>();
        attributes.add("n");
        attributes.add(String.valueOf(staff.getNumber()));
        attributes.add(LINES);
        attributes.add(String.valueOf(staff.getLines()));
        attributes.addAll(MeiNotationConverter.getInstance().clefAttributes(staff.getClef(), "clef."));
        if (staff.getTransposeSemitones() != null) {
            attributes.add("trans.diat");
            attributes.add(String.valueOf(staff.getTransposeDiatonic()));
            attributes.add("trans.semi");
            attributes.add(String.valueOf(staff.getTransposeSemitones()));
        }
        out.open(STAFF_DEF, attributes.toArray(new String[0]));
        if (labels) {
            writeLabels(out, staff);
        }
        writeKeySig(out, keySig, mode);
        if (instrument && staff.getProgram() != null) {
            out.empty("instrDef", "midi.instrnum", String.valueOf(staff.getProgram() - 1));
        }
        out.close();
    }


    /**
     * Writes the name and abbreviation of a part.
     *
     * @param out the output
     * @param staff the first staff of the part
     */
    private void writeLabels(MeiXmlOutput out, MeiWriterStaff staff) {
        if (staff.getLabel() != null) {
            out.leaf("label", staff.getLabel());
        }
        if (staff.getAbbreviation() != null) {
            out.leaf("labelAbbr", staff.getAbbreviation());
        }
    }


    /**
     * Writes a key signature element.
     *
     * @param out the output
     * @param keySig the key signature, or null
     * @param mode the mode, or null
     */
    private void writeKeySig(MeiXmlOutput out, String keySig, String mode) {
        if (keySig == null) {
            return;
        }
        if (isMeiMode(mode)) {
            out.empty("keySig", "sig", keySig, "mode", mode);
        } else {
            out.empty("keySig", "sig", keySig);
        }
    }


    /**
     * Writes the measures with the score definition changes and the endings.
     *
     * @param out the output
     * @param plan the plan
     */
    private void writeMeasures(MeiXmlOutput out, MeiWriterPlan plan) {
        boolean inEnding = false;
        for (int m = 0; m < plan.getMeasures().size(); m++) {
            MeiWriterMeasure measure = plan.getMeasures().get(m);
            if (measure.getEndingStart() != null) {
                if (inEnding) {
                    out.close();
                }
                String number = measure.getEndingStart().trim().replaceAll("[^A-Za-z0-9._\\-]+", "-");
                out.open("ending", "n", number, "label", measure.getEndingStart().trim());
                inEnding = true;
            }
            if (m > 0) {
                writeScoreDefChange(out, plan, measure);
            }
            writeMeasure(out, plan, measure);
            if (measure.isEndingStop() && inEnding) {
                out.close();
                inEnding = false;
            }
        }
        if (inEnding) {
            out.close();
        }
    }


    /**
     * Writes the meter and key changes before a measure.
     *
     * @param out the output
     * @param plan the plan
     * @param measure the measure
     */
    private void writeScoreDefChange(MeiXmlOutput out, MeiWriterPlan plan, MeiWriterMeasure measure) {
        if (!measure.isMeterChange() && measure.getKeySigs().isEmpty()) {
            return;
        }
        if (measure.isMeterChange()) {
            out.open(SCORE_DEF, "meter.count", String.valueOf(measure.getMeterCount()), "meter.unit", String.valueOf(measure.getMeterUnit()));
        } else {
            out.open(SCORE_DEF);
        }
        if (!measure.getKeySigs().isEmpty()) {
            out.open(STAFF_GRP);
            for (Map.Entry<Integer, String> entry : measure.getKeySigs().entrySet()) {
                MeiWriterStaff staff = plan.getStaves().get(entry.getKey() - 1);
                out.open(STAFF_DEF, "n", String.valueOf(entry.getKey()), LINES, String.valueOf(staff.getLines()));
                writeKeySig(out, entry.getValue(), measure.getKeyModes().get(entry.getKey()));
                out.close();
            }
            out.close();
        }
        out.close();
    }


    /**
     * Writes a measure: its staves with the layers and its control events.
     *
     * @param out the output
     * @param plan the plan
     * @param measure the measure
     */
    private void writeMeasure(MeiXmlOutput out, MeiWriterPlan plan, MeiWriterMeasure measure) {
        List<String> attributes = new ArrayList<>();
        attributes.add("n");
        attributes.add(measure.getNumber());
        if (measure.getLeft() != null) {
            attributes.add("left");
            attributes.add(measure.getLeft());
        }
        if (measure.getRight() != null) {
            attributes.add("right");
            attributes.add(measure.getRight());
        }
        if (measure.isImplicit()) {
            attributes.add("metcon");
            attributes.add("false");
        }
        out.open("measure", attributes.toArray(new String[0]));
        for (MeiWriterStaff staff : plan.getStaves()) {
            out.open("staff", "n", String.valueOf(staff.getNumber()));
            MeiLayerWriter.getInstance().writeLayers(out, measure.getStaves().get(staff.getNumber()), measure.getClefsAtEnd().get(staff.getNumber()));
            out.close();
        }
        for (MeiWriterControl control : measure.getControls()) {
            List<String> controlAttributes = new ArrayList<>();
            for (Map.Entry<String, String> entry : control.getAttributes().entrySet()) {
                controlAttributes.add(entry.getKey());
                controlAttributes.add(entry.getValue());
            }
            String[] array = controlAttributes.toArray(new String[0]);
            if (control.getText() == null) {
                out.empty(control.getName(), array);
            } else {
                out.openInline(control.getName(), array);
                out.text(control.getText());
                out.closeInline(control.getName());
            }
        }
        out.close();
    }


    /**
     * Gets the initial key signatures by staff number.
     *
     * @param plan the plan
     * @return the key signatures
     */
    private Map<Integer, String> keysOfFirstMeasure(MeiWriterPlan plan) {
        if (plan.getMeasures().isEmpty()) {
            return new HashMap<>();
        }
        return plan.getMeasures().get(0).getKeySigs();
    }


    /**
     * Gets the initial key modes by staff number.
     *
     * @param plan the plan
     * @return the modes
     */
    private Map<Integer, String> modesOfFirstMeasure(MeiWriterPlan plan) {
        if (plan.getMeasures().isEmpty()) {
            return new HashMap<>();
        }
        return plan.getMeasures().get(0).getKeyModes();
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
        return credit(score, TITLE);
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
     * Checks whether a MusicXML mode is an MEI mode.
     *
     * @param mode the mode, or null
     * @return true for major, minor and the church modes
     */
    private boolean isMeiMode(String mode) {
        return "major".equals(mode) || "minor".equals(mode) || "dorian".equals(mode) || "phrygian".equals(mode) || "lydian".equals(mode)
                || "mixolydian".equals(mode) || "aeolian".equals(mode) || "locrian".equals(mode);
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
