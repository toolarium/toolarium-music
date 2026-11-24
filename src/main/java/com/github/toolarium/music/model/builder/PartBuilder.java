/*
 * PartBuilder.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.builder;

import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import java.util.ArrayList;
import java.util.List;


/**
 * Fluent builder for a single part within a score.
 *
 * <p>Obtained via {@link ScoreBuilder#part(String)} or {@link ScoreBuilder#part(String, String)}.
 * Call {@link #measure(int)} to open a {@link MeasureBuilder} for each measure.
 * Call {@link #end()} to register the completed part with the parent {@link ScoreBuilder}.</p>
 *
 * @author patrick
 */
public class PartBuilder {

    private final ScoreBuilder scoreBuilder;
    private final ScorePartImpl scorePart;
    private final PartImpl part;
    private final List<MeasureImpl> measures;


    /**
     * Constructor. Called by {@link ScoreBuilder}.
     *
     * @param scoreBuilder the parent score builder
     * @param name         the part name shown on the score
     * @param id           the MusicXML part id
     */
    PartBuilder(ScoreBuilder scoreBuilder, String name, String id) {
        this.scoreBuilder = scoreBuilder;
        this.scorePart = new ScorePartImpl();
        this.scorePart.setId(id);
        this.scorePart.setPartName(name);
        this.part = new PartImpl();
        this.part.setId(id);
        this.measures = new ArrayList<>();
        this.part.setMeasures(this.measures);
    }


    /**
     * Sets the abbreviated name shown on continuation lines of the part.
     *
     * @param abbreviation the abbreviation (for example {@code "Vln."})
     * @return this builder
     */
    public PartBuilder abbreviation(String abbreviation) {
        scorePart.setPartAbbreviation(abbreviation);
        return this;
    }


    /**
     * Opens a new measure builder for the given measure number.
     * The first measure of this part automatically receives a {@code <divisions>} element
     * set to {@link MeasureBuilder#DIVISIONS} ticks per quarter note.
     *
     * @param number the measure number (1-based)
     * @return a new {@link MeasureBuilder} whose {@link MeasureBuilder#end()} returns this builder
     */
    public MeasureBuilder measure(int number) {
        boolean isFirst = measures.isEmpty();
        return new MeasureBuilder(this, String.valueOf(number), isFirst);
    }


    /**
     * Closes this part builder and registers the part with the parent score builder.
     *
     * @return the parent {@link ScoreBuilder}
     */
    public ScoreBuilder end() {
        scoreBuilder.addPart(scorePart, part);
        return scoreBuilder;
    }


    /**
     * Registers a completed measure. Called by {@link MeasureBuilder#end()}.
     *
     * @param measure the assembled measure
     */
    void addMeasure(MeasureImpl measure) {
        measures.add(measure);
    }
}
