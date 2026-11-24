/*
 * FinaleMarkConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.format.finale.impl.dto.FinaleNotationData;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.WordsImpl;
import com.github.toolarium.music.model.notation.impl.ArticulationsImpl;
import com.github.toolarium.music.model.notation.impl.FermataImpl;
import com.github.toolarium.music.model.notation.impl.NotationsImpl;
import com.github.toolarium.music.model.notation.impl.OrnamentsImpl;
import com.github.toolarium.music.model.notation.impl.PlayingTechniqueImpl;
import com.github.toolarium.music.model.notation.impl.SlurImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import java.util.ArrayList;
import java.util.List;


/**
 * Converts the marks of a Finale entry (articulations, fermatas, the trill mark, numbers and slurs) to the notations of a note.
 *
 * @author patrick
 */
public final class FinaleMarkConverter {
    /** Prefix of the mark of a slur that starts on an entry, followed by the slur ID. */
    public static final String SLUR_START = "slur-start:";

    /** Prefix of the mark of a slur that ends on an entry, followed by the slur ID. */
    public static final String SLUR_STOP = "slur-stop:";

    /** Prefix of the mark of a number above an entry (fingering or beat count), followed by the number. */
    public static final String FINGERING = "fingering:";

    private static final String FERMATA_INVERTED = "fermata-inverted";
    private static final String NUMBER_FONT_SIZE = "8";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final FinaleMarkConverter INSTANCE = new FinaleMarkConverter();
    }


    /**
     * Constructor for FinaleMarkConverter.
     */
    private FinaleMarkConverter() {
        // NOP
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static FinaleMarkConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Applies the marks of an entry to its (first) note: articulations, fermatas, the trill mark, numbers (as fingering) and slurs.
     *
     * @param note the note (updated)
     * @param marks the marks, or null
     * @param notation the notation with the slur numbers in use
     */
    public void applyMarks(NoteImpl note, List<String> marks, FinaleNotationData notation) {
        if (marks == null || marks.isEmpty()) {
            return;
        }
        NotationsImpl notations = note.getNotations();
        if (notations == null) {
            notations = new NotationsImpl();
        }
        ArticulationsImpl articulations = notations.getArticulations();
        if (articulations == null) {
            articulations = new ArticulationsImpl();
        }
        List<SlurImpl> slurs = new ArrayList<>();
        if (notations.getSlurs() != null) {
            slurs.addAll(notations.getSlurs());
        }
        List<FermataImpl> fermatas = new ArrayList<>();
        boolean articulation = false;
        // the slurs that end on the entry free their number before the slurs that start on it take one
        for (String mark : marks) {
            if (mark.startsWith(SLUR_STOP)) {
                Integer number = notation.stopSlur(mark.substring(SLUR_STOP.length()));
                if (number != null) {
                    slurs.add(createSlur("stop", number));
                }
            }
        }
        for (String mark : marks) {
            if (mark.startsWith(SLUR_START)) {
                slurs.add(createSlur("start", notation.startSlur(mark.substring(SLUR_START.length()))));
            } else if (mark.startsWith(SLUR_STOP)) {
                continue;
            } else if (mark.startsWith(FINGERING)) {
                applyFingering(notations, mark.substring(FINGERING.length()));
            } else if ("fermata".equals(mark) || FERMATA_INVERTED.equals(mark)) {
                fermatas.add(ScoreBuilder.getInstance().createFermata(null, FERMATA_INVERTED.equals(mark)));
            } else if ("trill-mark".equals(mark)) {
                OrnamentsImpl ornaments = new OrnamentsImpl();
                ornaments.setTrillMark("");
                notations.setOrnaments(ornaments);
            } else {
                articulation |= applyArticulation(articulations, mark);
            }
        }
        if (articulation) {
            notations.setArticulations(articulations);
        }
        if (!slurs.isEmpty()) {
            notations.setSlurs(slurs);
        }
        if (!fermatas.isEmpty()) {
            notations.setFermatas(fermatas);
        }
        note.setNotations(notations);
    }


    /**
     * Moves the numbers of a rest (a beat count above a rest) to a text direction: most applications do not show the
     * fingering of a rest.
     *
     * @param note the note (updated: the fingering of a rest is removed)
     * @param staff the staff number, or null for a part with one staff
     * @return the text direction, or null if the note is no rest with numbers
     */
    public DirectionImpl restNumbers(NoteImpl note, Integer staff) {
        NotationsImpl notations = note.getNotations();
        if (note.getRest() == null || notations == null || notations.getTechnical() == null || notations.getTechnical().getFingering() == null) {
            return null;
        }
        DirectionImpl direction = ScoreBuilder.getInstance().createWords(notations.getTechnical().getFingering(), "above", null, null);
        WordsImpl words = direction.getDirectionTypes().get(0).getWords().get(0);
        words.setFontSize(NUMBER_FONT_SIZE);
        direction.setVoice(note.getVoice());
        direction.setStaff(staff);
        notations.getTechnical().setFingering(null);
        if (new PlayingTechniqueImpl().equals(notations.getTechnical())) {
            notations.setTechnical(null);
        }
        if (new NotationsImpl().equals(notations)) {
            note.setNotations(null);
        }
        return direction;
    }


    /**
     * Adds a number to the fingering of a note: several numbers of one note are separated by a space.
     *
     * @param notations the notations (updated)
     * @param number the number
     */
    private void applyFingering(NotationsImpl notations, String number) {
        PlayingTechniqueImpl technical = notations.getTechnical();
        if (technical == null) {
            technical = new PlayingTechniqueImpl();
            notations.setTechnical(technical);
        }
        if (technical.getFingering() == null) {
            technical.setFingering(number);
        } else {
            technical.setFingering(technical.getFingering() + " " + number);
        }
    }


    /**
     * Creates a slur.
     *
     * @param type start or stop
     * @param number the slur number
     * @return the slur
     */
    private SlurImpl createSlur(String type, int number) {
        SlurImpl slur = new SlurImpl();
        slur.setType(type);
        slur.setNumber(number);
        return slur;
    }


    /**
     * Sets an articulation.
     *
     * @param articulations the articulations (updated)
     * @param name the MusicXML articulation name
     * @return true if the articulation is known
     */
    private boolean applyArticulation(ArticulationsImpl articulations, String name) {
        switch (name) {
            case "staccato":
                articulations.setStaccato("");
                return true;
            case "accent":
                articulations.setAccent("");
                return true;
            case "tenuto":
                articulations.setTenuto("");
                return true;
            case "strong-accent":
                articulations.setStrongAccent("");
                return true;
            case "detached-legato":
                articulations.setDetachedLegato("");
                return true;
            case "breath-mark":
                articulations.setBreathMark("");
                return true;
            case "caesura":
                articulations.setCaesura("");
                return true;
            default:
                return false;
        }
    }
}
