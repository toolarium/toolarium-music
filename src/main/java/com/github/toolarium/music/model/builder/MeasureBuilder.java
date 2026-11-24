/*
 * MeasureBuilder.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.builder;

import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import com.github.toolarium.music.model.note.impl.PitchImpl;
import com.github.toolarium.music.model.note.impl.RestImpl;
import java.util.ArrayList;
import java.util.List;


/**
 * Fluent builder for a single measure.
 *
 * <p>Obtained via {@link PartBuilder#measure(int)}. Call attribute methods
 * ({@link #key(int, String)}, {@link #time(String, String)}, {@link #clef(String, int)})
 * to set the measure attributes, then add notes and rests. Call {@link #end()} to
 * register the measure with the parent {@link PartBuilder}.</p>
 *
 * <p>Durations are computed automatically from the note type using {@link #DIVISIONS} ticks
 * per quarter note. Augmentation dots extend the duration by the standard formula
 * (one dot: base + base/2; two dots: base + base/2 + base/4).</p>
 *
 * <p>For multi-voice content or chord notes, use {@link #beginNote(String, int, String)} and
 * {@link #beginRest(String)}, which return a {@link NoteBuilder} with
 * {@link NoteBuilder#voice(int)} and {@link NoteBuilder#chord()} methods.</p>
 *
 * @author patrick
 */
public class MeasureBuilder {

    /**
     * Number of internal ticks per quarter note used by this builder.
     * With 8 ticks per quarter note: whole = 32, half = 16, quarter = 8,
     * eighth = 4, 16th = 2, 32nd = 1.
     * Notes shorter than 32nd are represented as 1 tick (minimum).
     */
    public static final int DIVISIONS = 8;

    private final PartBuilder partBuilder;
    private final MeasureImpl measure;
    private final AttributesImpl attributes;
    private boolean attributesFlushed;


    /**
     * Constructor. Called by {@link PartBuilder}.
     *
     * @param partBuilder  the parent part builder
     * @param number       the measure number string
     * @param setDivisions {@code true} to include a {@code <divisions>} element (first measure of each part only)
     */
    MeasureBuilder(PartBuilder partBuilder, String number, boolean setDivisions) {
        this.partBuilder = partBuilder;
        this.measure = new MeasureImpl();
        this.measure.setNumber(number);
        this.attributes = new AttributesImpl();
        this.attributesFlushed = false;
        if (setDivisions) {
            this.attributes.setDivisions(DIVISIONS);
        }
    }


    /**
     * Sets the key signature. Use {@code fifths = 0} for C major or A minor; positive values for sharps
     * (1 = G, 2 = D, 3 = A, 4 = E, 5 = B, 6 = F#, 7 = C#); negative values for flats
     * (-1 = F, -2 = Bb, -3 = Eb, -4 = Ab, -5 = Db, -6 = Gb, -7 = Cb).
     *
     * @param fifths the number of sharps (positive) or flats (negative)
     * @param mode   the mode: {@code "major"} or {@code "minor"}
     * @return this builder
     */
    public MeasureBuilder key(int fifths, String mode) {
        KeyImpl key = new KeyImpl();
        key.setFifths(fifths);
        key.setMode(mode);
        attributes.getKeys().add(key);
        return this;
    }


    /**
     * Sets the time signature.
     *
     * @param beats    the number of beats per measure (for example {@code "4"})
     * @param beatType the beat unit denominator (for example {@code "4"} for quarter notes,
     *                 {@code "8"} for eighth notes)
     * @return this builder
     */
    public MeasureBuilder time(String beats, String beatType) {
        TimeImpl time = new TimeImpl();
        time.setBeats(beats);
        time.setBeatType(beatType);
        attributes.getTimes().add(time);
        return this;
    }


    /**
     * Adds a clef to the measure attributes.
     * Common clefs: treble = {@code ("G", 2)}, bass = {@code ("F", 4)},
     * alto = {@code ("C", 3)}, tenor = {@code ("C", 4)}.
     *
     * @param sign the clef sign: {@code "G"}, {@code "F"} or {@code "C"}
     * @param line the staff line the clef sits on
     * @return this builder
     */
    public MeasureBuilder clef(String sign, int line) {
        ClefImpl clef = new ClefImpl();
        clef.setSign(sign);
        clef.setLine(line);
        attributes.getClefs().add(clef);
        return this;
    }


    /**
     * Adds a pitched note in voice 1. The duration is computed from the note type.
     *
     * @param step   the pitch step: {@code "C"}, {@code "D"}, {@code "E"}, {@code "F"},
     *               {@code "G"}, {@code "A"} or {@code "B"}
     * @param octave the octave number (4 = middle C octave)
     * @param type   the note type: {@code "whole"}, {@code "half"}, {@code "quarter"},
     *               {@code "eighth"}, {@code "16th"}, {@code "32nd"}, etc.
     * @return this builder
     */
    public MeasureBuilder note(String step, int octave, String type) {
        return note(step, octave, null, type, 0);
    }


    /**
     * Adds a pitched note with an accidental alteration in voice 1.
     *
     * @param step   the pitch step
     * @param octave the octave number
     * @param alter  the alteration in semitones ({@code 1.0} = sharp, {@code -1.0} = flat,
     *               {@code null} = natural)
     * @param type   the note type
     * @return this builder
     */
    public MeasureBuilder note(String step, int octave, Double alter, String type) {
        return note(step, octave, alter, type, 0);
    }


    /**
     * Adds a dotted pitched note in voice 1.
     *
     * @param step   the pitch step
     * @param octave the octave number
     * @param type   the note type
     * @param dots   the number of augmentation dots (1 = dotted, 2 = double-dotted)
     * @return this builder
     */
    public MeasureBuilder note(String step, int octave, String type, int dots) {
        return note(step, octave, null, type, dots);
    }


    /**
     * Adds a pitched note with an accidental alteration and augmentation dots in voice 1.
     *
     * @param step   the pitch step
     * @param octave the octave number
     * @param alter  the alteration in semitones, or {@code null} for natural
     * @param type   the note type
     * @param dots   the number of augmentation dots (0 = plain, 1 = dotted, 2 = double-dotted)
     * @return this builder
     */
    public MeasureBuilder note(String step, int octave, Double alter, String type, int dots) {
        PitchImpl pitch = new PitchImpl();
        pitch.setStep(step);
        pitch.setOctave(octave);
        if (alter != null) {
            pitch.setAlter(alter);
        }
        NoteImpl note = new NoteImpl();
        note.setPitch(pitch);
        note.setType(type);
        note.setDuration(computeDuration(type, dots));
        note.setVoice(1);
        applyDots(note, dots);
        addNoteElement(note);
        return this;
    }


    /**
     * Adds a rest in voice 1. The duration is computed from the note type.
     *
     * @param type the note type: {@code "whole"}, {@code "half"}, {@code "quarter"},
     *             {@code "eighth"}, etc.
     * @return this builder
     */
    public MeasureBuilder rest(String type) {
        return rest(type, 0);
    }


    /**
     * Adds a dotted rest in voice 1.
     *
     * @param type the note type
     * @param dots the number of augmentation dots (0 = plain, 1 = dotted, 2 = double-dotted)
     * @return this builder
     */
    public MeasureBuilder rest(String type, int dots) {
        NoteImpl note = new NoteImpl();
        note.setRest(new RestImpl());
        note.setType(type);
        note.setDuration(computeDuration(type, dots));
        note.setVoice(1);
        applyDots(note, dots);
        addNoteElement(note);
        return this;
    }


    /**
     * Opens a {@link NoteBuilder} for a pitched note, allowing voice, chord and other properties
     * to be set before {@link NoteBuilder#end()} adds the note and returns to this builder.
     *
     * @param step   the pitch step
     * @param octave the octave number
     * @param type   the note type
     * @return a new {@link NoteBuilder} whose {@link NoteBuilder#end()} returns this builder
     */
    public NoteBuilder beginNote(String step, int octave, String type) {
        PitchImpl pitch = new PitchImpl();
        pitch.setStep(step);
        pitch.setOctave(octave);
        NoteImpl note = new NoteImpl();
        note.setPitch(pitch);
        note.setType(type);
        note.setDuration(computeDuration(type, 0));
        note.setVoice(1);
        return new NoteBuilder(this, note);
    }


    /**
     * Opens a {@link NoteBuilder} for a rest, allowing voice and other properties to be set
     * before {@link NoteBuilder#end()} adds the rest and returns to this builder.
     *
     * @param type the note type
     * @return a new {@link NoteBuilder} whose {@link NoteBuilder#end()} returns this builder
     */
    public NoteBuilder beginRest(String type) {
        NoteImpl note = new NoteImpl();
        note.setRest(new RestImpl());
        note.setType(type);
        note.setDuration(computeDuration(type, 0));
        note.setVoice(1);
        return new NoteBuilder(this, note);
    }


    /**
     * Closes this measure builder and registers the measure with the parent part builder.
     *
     * @return the parent {@link PartBuilder}
     */
    public PartBuilder end() {
        flushAttributes();
        partBuilder.addMeasure(measure);
        return partBuilder;
    }


    /**
     * Adds an element to the measure, flushing pending attributes before the first element.
     * Called by the convenience methods and by {@link NoteBuilder#end()}.
     *
     * @param element the note or rest element to add
     */
    void addNoteElement(IMusicDataElement element) {
        flushAttributes();
        measure.addMusicData(element);
    }


    /**
     * Writes the pending attributes element to the measure if it has not been written yet
     * and if it contains any content (divisions, key, time or clef).
     */
    private void flushAttributes() {
        if (attributesFlushed) {
            return;
        }
        boolean hasContent = attributes.getDivisions() != null
                || !attributes.getKeys().isEmpty()
                || !attributes.getTimes().isEmpty()
                || !attributes.getClefs().isEmpty();
        if (hasContent) {
            measure.addMusicData(attributes);
            attributesFlushed = true;
        }
    }


    /**
     * Adds augmentation dot markers to a note.
     *
     * @param note the note to modify
     * @param dots the number of dots
     */
    private static void applyDots(NoteImpl note, int dots) {
        if (dots > 0) {
            List<String> dotList = new ArrayList<>();
            for (int i = 0; i < dots; i++) {
                dotList.add("");
            }
            note.setDots(dotList);
        }
    }


    /**
     * Computes the duration in ticks for a note type and dot count using {@link #DIVISIONS} ticks per quarter note.
     *
     * @param type the note type string
     * @param dots the number of augmentation dots
     * @return the duration in ticks (minimum 1)
     */
    static int computeDuration(String type, int dots) {
        int base = baseDuration(type);
        int duration = base;
        int add = base;
        for (int i = 0; i < dots; i++) {
            add = add / 2;
            duration = duration + add;
        }
        return duration;
    }


    /**
     * Returns the base duration in ticks for a note type with {@link #DIVISIONS} ticks per quarter note.
     * Notes shorter than 32nd return 1 (the minimum representable tick).
     *
     * @param type the note type string
     * @return the base duration in ticks
     */
    private static int baseDuration(String type) {
        if ("32nd".equals(type)) {
            return 1;
        }
        if ("16th".equals(type)) {
            return DIVISIONS / 4;
        }
        if ("eighth".equals(type)) {
            return DIVISIONS / 2;
        }
        if ("half".equals(type)) {
            return DIVISIONS * 2;
        }
        if ("whole".equals(type)) {
            return DIVISIONS * 4;
        }
        if ("breve".equals(type)) {
            return DIVISIONS * 8;
        }
        if ("long".equals(type)) {
            return DIVISIONS * 16;
        }
        if ("maxima".equals(type)) {
            return DIVISIONS * 32;
        }
        if ("64th".equals(type)) {
            return 1;
        }
        if ("128th".equals(type)) {
            return 1;
        }
        if ("256th".equals(type)) {
            return 1;
        }
        if ("512th".equals(type)) {
            return 1;
        }
        if ("1024th".equals(type)) {
            return 1;
        }
        // default: quarter
        return DIVISIONS;
    }
}
