/*
 * MuseScoreDirectionWriter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore.impl;

import com.github.toolarium.music.io.converter.DynamicsConverter;
import com.github.toolarium.music.model.direction.IDirection;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.direction.IMetronome;
import com.github.toolarium.music.model.direction.IWords;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Creates the MuseScore elements of directions: dynamics, texts, rehearsal marks and tempo marks.
 *
 * @author patrick
 */
public final class MuseScoreDirectionWriter {
    private static final String SUBTYPE = "subtype";
    private static final String TEXT = "text";
    private static final double SECONDS_PER_MINUTE = 60.0;
    private static final double DOTTED = 1.5;
    private static final Map<String, String> METRONOME_SYMBOLS = createMetronomeSymbols();
    private static final Map<String, Double> BEAT_UNIT_QUARTERS = createBeatUnitQuarters();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MuseScoreDirectionWriter INSTANCE = new MuseScoreDirectionWriter();
    }


    /**
     * Constructor.
     */
    private MuseScoreDirectionWriter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MuseScoreDirectionWriter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Creates the MuseScore element of a direction type (dynamics, tempo, text, rehearsal mark).
     *
     * @param type the direction type
     * @return the element, or null if the type has no MuseScore element
     */
    MuseScoreElement getDirectionElement(IDirectionType type) {
        if (type.getDynamics() != null) {
            String marking = DynamicsConverter.getInstance().getMarking(type.getDynamics());
            if (marking == null) {
                return null;
            }
            return out -> {
                out.open("Dynamic");
                if (MuseScoreSymbolConverter.getInstance().isKnownDynamic(marking)) {
                    out.leaf(SUBTYPE, marking);
                } else {
                    out.leaf(SUBTYPE, "other-dynamics");
                    out.leaf(TEXT, marking);
                }
                out.close();
            };
        }
        if (type.getRehearsal() != null) {
            return out -> {
                out.open("RehearsalMark");
                out.leaf(TEXT, type.getRehearsal());
                out.close();
            };
        }
        if (type.getWords() != null && !type.getWords().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (IWords words : type.getWords()) {
                if (words.getValue() != null) {
                    sb.append(words.getValue());
                }
            }
            String text = sb.toString().trim();
            if (text.isEmpty()) {
                return null;
            }
            // a number (for example a beat count above a rest) looks like the fingerings of the notes beside it
            boolean number = text.matches("[0-9]+");
            return out -> {
                out.open("StaffText");
                if (number) {
                    out.leaf("style", "fingering");
                }
                out.leaf(TEXT, text);
                out.close();
            };
        }
        return null;
    }


    /**
     * Checks whether a direction is a tempo mark (a metronome mark or a sound tempo).
     *
     * @param direction the direction
     * @return true for a tempo mark
     */
    boolean isTempo(IDirection direction) {
        if (direction.getSound() != null && direction.getSound().getTempo() != null) {
            return true;
        }
        for (IDirectionType type : nonNull(direction.getDirectionTypes())) {
            if (type.getMetronome() != null) {
                return true;
            }
        }
        return false;
    }


    /**
     * Creates the tempo element of a direction: the words and the metronome mark (with the note symbol of
     * MuseScore, so that MuseScore recognizes the tempo) become the text, the tempo is stored in quarter notes
     * per second.
     *
     * @param direction the direction
     * @return the element
     */
    MuseScoreElement getTempoElement(IDirection direction) {
        double bpm = 0;
        if (direction.getSound() != null && direction.getSound().getTempo() != null) {
            bpm = parseDouble(direction.getSound().getTempo());
        }
        MuseScoreXmlOutput escaper = new MuseScoreXmlOutput();
        StringBuilder text = new StringBuilder();
        for (IDirectionType type : nonNull(direction.getDirectionTypes())) {
            for (IWords words : nonNull(type.getWords())) {
                if (words.getValue() != null) {
                    text.append(escaper.escape(words.getValue().trim()));
                }
            }
            IMetronome metronome = type.getMetronome();
            if (metronome != null && metronome.getPerMinute() != null) {
                String symbol = METRONOME_SYMBOLS.get(metronome.getBeatUnit());
                if (symbol == null) {
                    symbol = METRONOME_SYMBOLS.get("quarter");
                }
                if (text.length() > 0) {
                    text.append(' ');
                }
                text.append("<sym>").append(symbol).append("</sym>");
                double quarters = BEAT_UNIT_QUARTERS.getOrDefault(metronome.getBeatUnit(), 1.0);
                if (metronome.getBeatUnitDot() != null) {
                    text.append("<sym>space</sym><sym>metAugmentationDot</sym>");
                    quarters = quarters * DOTTED;
                }
                text.append(" = ").append(escaper.escape(metronome.getPerMinute()));
                if (bpm <= 0) {
                    bpm = parseDouble(metronome.getPerMinute()) * quarters;
                }
            }
        }
        if (bpm <= 0) {
            bpm = SECONDS_PER_MINUTE * 2;
        }
        double quartersPerSecond = bpm / SECONDS_PER_MINUTE;
        String tempoText = text.toString().trim();
        return out -> {
            out.open("Tempo");
            out.leaf("tempo", String.valueOf(quartersPerSecond));
            out.leaf("followText", "1");
            if (!tempoText.isEmpty()) {
                out.openInline(TEXT);
                out.raw(tempoText);
                out.closeInline(TEXT);
            }
            out.close();
        };
    }


    /**
     * Creates the MuseScore symbols of the metronome beat units.
     *
     * @return the symbol per beat unit
     */
    private static Map<String, String> createMetronomeSymbols() {
        Map<String, String> result = new HashMap<>();
        result.put("whole", "metNoteWhole");
        result.put("half", "metNoteHalfUp");
        result.put("quarter", "metNoteQuarterUp");
        result.put("eighth", "metNote8thUp");
        result.put("16th", "metNote16thUp");
        return result;
    }


    /**
     * Creates the length of the metronome beat units in quarter notes.
     *
     * @return the length per beat unit
     */
    private static Map<String, Double> createBeatUnitQuarters() {
        Map<String, Double> result = new HashMap<>();
        result.put("whole", 4.0);
        result.put("half", 2.0);
        result.put("quarter", 1.0);
        result.put("eighth", 0.5);
        result.put("16th", 0.25);
        return result;
    }


    /**
     * Parses a decimal value.
     *
     * @param value the text (may be null)
     * @return the value, or 0
     */
    private double parseDouble(String value) {
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
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
