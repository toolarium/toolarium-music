/*
 * ScoreBuilder.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.builder;

import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.CreatorImpl;
import com.github.toolarium.music.model.score.impl.IdentificationImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import com.github.toolarium.music.model.score.impl.WorkImpl;


/**
 * Fluent builder for {@link IScore}.
 *
 * <p>Use {@link #builder()} to start, then chain methods to set metadata and add parts.
 * Each call to {@link #part(String)} or {@link #part(String, String)} opens a {@link PartBuilder};
 * call {@link PartBuilder#end()} to return here, then call {@link #build()} to produce the score.</p>
 *
 * <pre>{@code
 * IScore score = ScoreBuilder.builder()
 *     .title("Ode to Joy")
 *     .composer("Beethoven")
 *     .part("Violin")
 *         .measure(1)
 *             .time("4", "4").key(0, "major").clef("G", 2)
 *             .note("E", 4, "quarter")
 *             .note("D", 4, "quarter")
 *             .rest("half")
 *         .end()
 *     .end()
 *     .build();
 * }</pre>
 *
 * @author patrick
 */
public class ScoreBuilder {

    private final ScoreImpl score;
    private final PartListImpl partList;


    /**
     * Private constructor; use {@link #builder()} to create an instance.
     */
    private ScoreBuilder() {
        this.score = new ScoreImpl();
        this.score.setVersion("4.0");
        this.partList = new PartListImpl();
        this.score.setPartList(this.partList);
    }


    /**
     * Creates a new {@link ScoreBuilder}.
     *
     * @return a new builder instance
     */
    public static ScoreBuilder builder() {
        return new ScoreBuilder();
    }


    /**
     * Sets the movement title shown above the score.
     *
     * @param title the title text
     * @return this builder
     */
    public ScoreBuilder title(String title) {
        score.setMovementTitle(title);
        return this;
    }


    /**
     * Sets the movement number.
     *
     * @param number the movement number
     * @return this builder
     */
    public ScoreBuilder movementNumber(String number) {
        score.setMovementNumber(number);
        return this;
    }


    /**
     * Sets the work title (optional; used for the larger work of which this score is a movement).
     *
     * @param workTitle the work title
     * @return this builder
     */
    public ScoreBuilder workTitle(String workTitle) {
        WorkImpl work = score.getWork();
        if (work == null) {
            work = new WorkImpl();
            score.setWork(work);
        }
        work.setWorkTitle(workTitle);
        return this;
    }


    /**
     * Adds a composer credit to the score identification.
     *
     * @param name the composer name
     * @return this builder
     */
    public ScoreBuilder composer(String name) {
        return addCreator("composer", name);
    }


    /**
     * Adds a lyricist credit to the score identification.
     *
     * @param name the lyricist name
     * @return this builder
     */
    public ScoreBuilder lyricist(String name) {
        return addCreator("lyricist", name);
    }


    /**
     * Adds an arranger credit to the score identification.
     *
     * @param name the arranger name
     * @return this builder
     */
    public ScoreBuilder arranger(String name) {
        return addCreator("arranger", name);
    }


    /**
     * Opens a new part builder. The part id is auto-assigned as {@code "P1"}, {@code "P2"}, etc.
     *
     * @param name the part name shown on the score (for example {@code "Piano"})
     * @return a new {@link PartBuilder} whose {@link PartBuilder#end()} returns this builder
     */
    public PartBuilder part(String name) {
        String id = "P" + (partList.getScoreParts().size() + 1);
        return new PartBuilder(this, name, id);
    }


    /**
     * Opens a new part builder with an explicit part id.
     *
     * @param name the part name shown on the score (for example {@code "Piano"})
     * @param id   the MusicXML part id (for example {@code "P1"})
     * @return a new {@link PartBuilder} whose {@link PartBuilder#end()} returns this builder
     */
    public PartBuilder part(String name, String id) {
        return new PartBuilder(this, name, id);
    }


    /**
     * Builds the score.
     *
     * @return the assembled {@link IScore}
     */
    public IScore build() {
        return score;
    }


    /**
     * Adds a creator (composer, lyricist, arranger) to the score identification.
     *
     * @param type the creator role
     * @param name the person's name
     * @return this builder
     */
    private ScoreBuilder addCreator(String type, String name) {
        IdentificationImpl id = score.getIdentification();
        if (id == null) {
            id = new IdentificationImpl();
            score.setIdentification(id);
        }
        CreatorImpl creator = new CreatorImpl();
        creator.setType(type);
        creator.setValue(name);
        id.getCreators().add(creator);
        return this;
    }


    /**
     * Registers a completed part. Called by {@link PartBuilder#end()}.
     *
     * @param scorePart the score-part definition for the part list
     * @param part      the part with measures
     */
    void addPart(ScorePartImpl scorePart, PartImpl part) {
        partList.getScoreParts().add(scorePart);
        score.getParts().add(part);
    }
}
