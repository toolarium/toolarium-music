/*
 * NoteBuilder.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.builder;

import com.github.toolarium.music.model.note.impl.NoteImpl;
import java.util.ArrayList;
import java.util.List;


/**
 * Fluent builder for a single note or rest within a measure.
 *
 * <p>Obtained via {@link MeasureBuilder#beginNote(String, int, String)} or
 * {@link MeasureBuilder#beginRest(String)}. Set optional properties such as voice, chord
 * or dots, then call {@link #end()} to add the note to the measure and return to the
 * {@link MeasureBuilder}.</p>
 *
 * @author patrick
 */
public class NoteBuilder {

    private final MeasureBuilder measureBuilder;
    private final NoteImpl note;


    /**
     * Constructor. Called by {@link MeasureBuilder}.
     *
     * @param measureBuilder the parent measure builder
     * @param note           the note being assembled
     */
    NoteBuilder(MeasureBuilder measureBuilder, NoteImpl note) {
        this.measureBuilder = measureBuilder;
        this.note = note;
    }


    /**
     * Sets the alteration in semitones for a pitched note.
     * Has no effect on rests.
     *
     * @param alter the alteration: {@code 1.0} = sharp, {@code -1.0} = flat,
     *              {@code null} = natural
     * @return this builder
     */
    public NoteBuilder alter(Double alter) {
        if (note.getPitch() != null) {
            note.getPitch().setAlter(alter);
        }
        return this;
    }


    /**
     * Sets the number of augmentation dots and recalculates the duration accordingly.
     *
     * @param dots the number of augmentation dots (1 = dotted, 2 = double-dotted)
     * @return this builder
     */
    public NoteBuilder dots(int dots) {
        note.setDuration(MeasureBuilder.computeDuration(note.getType(), dots));
        List<String> dotList = new ArrayList<>();
        for (int i = 0; i < dots; i++) {
            dotList.add("");
        }
        note.setDots(dotList);
        return this;
    }


    /**
     * Sets the voice number for multi-voice measures.
     * Voice 1 is the default; use 2 and higher for additional voices in the same measure.
     *
     * @param voice the voice number (1-based)
     * @return this builder
     */
    public NoteBuilder voice(int voice) {
        note.setVoice(voice);
        return this;
    }


    /**
     * Marks this note as a chord note, sounded together with the preceding note.
     * In MusicXML, a {@code <chord>} element on a note means it shares the time position
     * of the note immediately before it.
     *
     * @return this builder
     */
    public NoteBuilder chord() {
        note.setChord("chord");
        return this;
    }


    /**
     * Closes this note builder, adds the note to the measure and returns to the parent measure builder.
     *
     * @return the parent {@link MeasureBuilder}
     */
    public MeasureBuilder end() {
        measureBuilder.addNoteElement(note);
        return measureBuilder;
    }
}
