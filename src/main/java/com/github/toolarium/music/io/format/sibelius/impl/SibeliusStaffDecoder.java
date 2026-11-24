/*
 * SibeliusStaffDecoder.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.MeasureData;
import com.github.toolarium.music.io.dto.NoteData;
import com.github.toolarium.music.io.dto.PartData;
import com.github.toolarium.music.io.format.sibelius.impl.dto.SibeliusEvent;
import com.github.toolarium.music.io.format.sibelius.impl.dto.SibeliusObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Decodes the notes, rests, slurs, hairpins and texts of a Sibelius staff.
 *
 * <p>A note or rest object (type 0x29) holds the duration code at offset 57 (6 sixteenth, 7 eighth, 8 quarter,
 * 9 half, 10 whole, plus 0x10 for a dot), the fermata (offset 59, 0x20), the articulations (offset 60: 0x02 staccato,
 * 0x10 tenuto, 0x20 accent) and at offset 64 the number of pitches. Each pitch starts with 0x4B and a flag byte
 * (0x80: tied to the next note, followed by 63 bytes of tie data), then the diatonic pitch (35 = C4) and the MIDI pitch
 * times two in concert pitch, and ends with 0xFF. Slurs and hairpins hold their length at offset 57.</p>
 *
 * @author patrick
 */
public final class SibeliusStaffDecoder {
    /** Positions per quarter note. */
    public static final int QUARTER = 256;

    private static final int WHOLE = 4 * QUARTER;
    private static final int DURATION_OFFSET = 57;
    private static final int FERMATA_OFFSET = 59;
    private static final int ARTICULATION_OFFSET = 60;
    private static final int PITCH_COUNT_OFFSET = 64;
    private static final int LENGTH_OFFSET = 57;
    private static final int FERMATA = 0x20;
    private static final int STACCATO = 0x02;
    private static final int TENUTO = 0x10;
    private static final int ACCENT = 0x20;
    private static final int PITCH_START = 0x4B;
    private static final int TIE = 0x80;
    private static final int TIE_DATA = 63;
    private static final int PITCH_END = 0xFF;
    private static final int MAX_PITCH_BYTES = 32;
    private static final int DURATION_MASK = 0x0F;
    private static final int DOT_SHIFT = 4;
    private static final int DOT_MASK = 0x03;
    private static final int WHOLE_CODE = 10;
    private static final int STEPS_PER_OCTAVE = 7;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int[] STEP_SEMITONES = {0, 2, 4, 5, 7, 9, 11};
    private static final String STEPS = "CDEFGAB";
    private static final String EMPTY = "";
    private static final int MAX_NUMBER = 6;
    private static final int MAX_TUPLET = 16;
    private static final int POSITION_TOLERANCE = 2;
    private static final double TUPLET_TOLERANCE = 0.02;
    private static final Pattern DYNAMICS = Pattern.compile("(p{1,5}|f{1,5}|mp|mf|sf|sfz|sffz|sfp|fp|fz|rf|rfz|sfpp|pf|n)");


    /**
     * Private holder class for the singleton instance.
     */
    private static final class HOLDER {
        static final SibeliusStaffDecoder INSTANCE = new SibeliusStaffDecoder();
    }


    /**
     * Constructor.
     */
    private SibeliusStaffDecoder() {
        // NOP
    }


    /**
     * Gets the singleton instance.
     *
     * @return the instance
     */
    public static SibeliusStaffDecoder getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Reads the notes, rests, slurs, hairpins and texts of a staff into the measures of a part.
     *
     * @param stream the object stream
     * @param staff the staff (1 based)
     * @param part the part (the measures must exist and the transposition must be set)
     * @param barStarts the start of each bar from the start of the score in 1/256 quarter notes (one more than bars)
     */
    public void read(SibeliusObjectStream stream, int staff, PartData part, long[] barStarts) {
        List<SibeliusObject> objects = stream.getObjects(staff, -1);
        List<SibeliusObject> tuplets = new ArrayList<>();
        for (SibeliusObject object : objects) {
            if (object.getType() == SibeliusObjectStream.TUPLET) {
                tuplets.add(object);
            }
        }

        List<SibeliusEvent> events = new ArrayList<>();
        for (int voice = 1; voice <= 4; voice++) {
            readVoice(stream, objects, voice, part, barStarts, tuplets, events);
        }
        for (int bar = 1; bar < barStarts.length; bar++) {
            MeasureData measure = part.measure(bar - 1);
            if (measure.voice(1, 1).getEvents().isEmpty()) {
                EventData rest = new EventData();
                rest.setMeasureRest(true);
                rest.setDuration(Fraction.of(barStarts[bar] - barStarts[bar - 1], WHOLE));
                rest.setType("whole");
                measure.voice(1, 1).getEvents().add(rest);
            }
        }
        int[] numbers = new int[MAX_NUMBER + 1];
        List<SibeliusObject> slurs = new ArrayList<>();
        for (SibeliusObject object : objects) {
            if (object.getBar() < 1) {
                continue;
            }
            MeasureData measure = part.measure(object.getBar() - 1);
            Fraction position = Fraction.of(object.getPosition(), WHOLE);
            if (object.getType() == SibeliusObjectStream.SLUR) {
                slurs.add(object);
            } else if (object.getType() == SibeliusObjectStream.CRESCENDO || object.getType() == SibeliusObjectStream.DIMINUENDO) {
                addHairpin(stream, object, part, barStarts, numbers);
            } else if (object.getType() == SibeliusObjectStream.STAFF_TEXT) {
                addText(stream, object, measure, position);
            }
        }
        addSlurs(stream, slurs, events, barStarts);
    }


    /**
     * Reads the notes and rests of a voice.
     *
     * @param stream the object stream
     * @param objects the objects of the staff
     * @param voice the voice
     * @param part the part
     * @param barStarts the start of each bar
     * @param tuplets the tuplet objects of the staff
     * @param events the events of the staff (updated)
     */
    private void readVoice(SibeliusObjectStream stream, List<SibeliusObject> objects, int voice, PartData part, long[] barStarts,
                           List<SibeliusObject> tuplets, List<SibeliusEvent> events) {
        List<SibeliusObject> notes = new ArrayList<>();
        for (SibeliusObject object : objects) {
            if (object.getType() == SibeliusObjectStream.NOTE_REST && object.getVoice() == voice && object.getBar() > 0) {
                notes.add(object);
            }
        }
        notes.sort(Comparator.comparingInt(SibeliusObject::getBar).thenComparingInt(SibeliusObject::getPosition));

        Set<Integer> tied = new HashSet<>();
        int bar = 0;
        Fraction cursor = Fraction.ZERO;
        for (int i = 0; i < notes.size(); i++) {
            SibeliusObject object = notes.get(i);
            if (object.getBar() != bar) {
                bar = object.getBar();
                cursor = Fraction.ZERO;
            }
            MeasureData measure = part.measure(bar - 1);

            // the positions of tuplet notes are rounded, so a gap smaller than the rounding is no gap
            Fraction gapLength = Fraction.of(object.getPosition(), WHOLE).subtract(cursor);
            if (gapLength.compareTo(Fraction.of(POSITION_TOLERANCE, WHOLE)) > 0) {
                EventData gap = new EventData();
                gap.setDuration(gapLength);
                gap.setInvisible(true);
                measure.voice(1, voice).getEvents().add(gap);
            }

            int code = stream.u8(object.getOffset() + DURATION_OFFSET);
            Fraction nominal = nominalDuration(code);
            EventData event = new EventData();
            event.setDuration(nominal);
            SibeliusObject tuplet = findTuplet(stream, tuplets, object);
            if (tuplet != null) {
                applyTuplet(stream, tuplet, notes, i, event);
            }
            readPitches(stream, object, part, event, tied);
            readArticulations(stream, object, event);
            measure.voice(1, voice).getEvents().add(event);
            events.add(new SibeliusEvent(voice, barStarts[bar - 1] + object.getPosition(), event));
            cursor = cursor.max(Fraction.of(object.getPosition(), WHOLE)).add(event.getDuration());
        }
    }


    /**
     * Gets the duration of a duration code.
     *
     * @param code the duration code
     * @return the duration in whole notes
     */
    private Fraction nominalDuration(int code) {
        int value = code & DURATION_MASK;
        int dots = (code >> DOT_SHIFT) & DOT_MASK;
        Fraction base;
        if (value >= WHOLE_CODE) {
            base = Fraction.of(1L << (value - WHOLE_CODE), 1);
        } else {
            base = Fraction.of(1, 1L << (WHOLE_CODE - value));
        }
        Fraction result = base;
        Fraction dot = base;
        for (int d = 0; d < dots; d++) {
            dot = dot.multiply(1, 2);
            result = result.add(dot);
        }
        return result;
    }


    /**
     * Finds the tuplet a note belongs to.
     *
     * @param stream the object stream
     * @param tuplets the tuplets of the staff
     * @param note the note
     * @return the tuplet, or null
     */
    private SibeliusObject findTuplet(SibeliusObjectStream stream, List<SibeliusObject> tuplets, SibeliusObject note) {
        for (SibeliusObject tuplet : tuplets) {
            if (tuplet.getBar() == note.getBar() && tuplet.getVoice() == note.getVoice() && note.getPosition() >= tuplet.getPosition()
                    && note.getPosition() <= tuplet.getPosition() + stream.u32(tuplet.getOffset() + LENGTH_OFFSET)) {
                return tuplet;
            }
        }
        return null;
    }


    /**
     * Applies a tuplet to a note: the notes of the tuplet are scaled to the length of the tuplet.
     *
     * @param stream the object stream
     * @param tuplet the tuplet
     * @param notes the notes of the voice
     * @param index the index of the note
     * @param event the event of the note
     */
    private void applyTuplet(SibeliusObjectStream stream, SibeliusObject tuplet, List<SibeliusObject> notes, int index, EventData event) {
        // the length of a tuplet reaches to the start of its last note
        long end = tuplet.getPosition() + stream.u32(tuplet.getOffset() + LENGTH_OFFSET);
        SibeliusObject note = notes.get(index);
        int first = -1;
        int last = -1;
        for (int i = 0; i < notes.size(); i++) {
            SibeliusObject other = notes.get(i);
            if (other.getBar() == note.getBar() && other.getPosition() >= tuplet.getPosition() && other.getPosition() <= end) {
                if (first < 0) {
                    first = i;
                }
                last = i;
            }
        }
        if (first < 0 || last <= first) {
            return;
        }
        Fraction nominal = Fraction.ZERO;
        for (int i = first; i < last; i++) {
            nominal = nominal.add(nominalDuration(stream.u8(notes.get(i).getOffset() + DURATION_OFFSET)));
        }
        long span = notes.get(last).getPosition() - notes.get(first).getPosition();
        double ratio = nominal.getNumerator() * (double) WHOLE / nominal.getDenominator() / span;
        int[] tupletRatio = findRatio(ratio);
        if (tupletRatio == null) {
            return;
        }
        event.setDuration(event.getDuration().multiply(tupletRatio[1], tupletRatio[0]));
        event.setTuplet(tupletRatio[0], tupletRatio[1]);
        event.setTupletStart(index == first);
        event.setTupletStop(index == last);
    }


    /**
     * Finds the tuplet ratio (actual notes in the time of normal notes) closest to a measured ratio.
     *
     * @param ratio the ratio of the nominal to the actual duration
     * @return the actual and the normal number of notes, or null if the ratio is no tuplet
     */
    private int[] findRatio(double ratio) {
        int[] best = null;
        double error = TUPLET_TOLERANCE;
        for (int actual = 2; actual <= MAX_TUPLET; actual++) {
            for (int normal = 1; normal <= MAX_TUPLET; normal++) {
                double difference = Math.abs((double) actual / normal - ratio);
                if (actual != normal && difference < error) {
                    error = difference;
                    best = new int[] {actual, normal};
                }
            }
        }
        return best;
    }


    /**
     * Reads the pitches of a note or chord in written pitch.
     *
     * @param stream the object stream
     * @param object the note object
     * @param part the part with the transposition
     * @param event the event (updated)
     * @param tied the written MIDI pitches tied from the previous event of the voice (updated)
     */
    private void readPitches(SibeliusObjectStream stream, SibeliusObject object, PartData part, EventData event, Set<Integer> tied) {
        int count = stream.u8(object.getOffset() + PITCH_COUNT_OFFSET);
        int p = object.getOffset() + PITCH_COUNT_OFFSET + 1;
        Set<Integer> next = new HashSet<>();
        for (int n = 0; n < count; n++) {
            if (stream.u8(p) != PITCH_START) {
                break;
            }
            int flag = stream.u8(p + 1);
            p += 2;
            if ((flag & TIE) != 0) {
                p += TIE_DATA;
            }
            int diatonic = stream.u8(p) - part.getTransposeDiatonic();
            int midi = stream.u8(p + 1) / 2 - part.getTransposeChromatic();
            int step = Math.floorMod(diatonic, STEPS_PER_OCTAVE);
            int octave = Math.floorDiv(diatonic, STEPS_PER_OCTAVE) - 1;
            int alter = midi - ((octave + 1) * SEMITONES_PER_OCTAVE + STEP_SEMITONES[step]);
            NoteData note = new NoteData(String.valueOf(STEPS.charAt(step)), alter, octave);
            note.setTieStop(tied.contains(midi));
            if ((flag & TIE) != 0) {
                note.setTieStart(true);
                next.add(midi);
            }
            event.getNotes().add(note);

            int end = p + 2;
            while (end < p + MAX_PITCH_BYTES && stream.u8(end) != PITCH_END) {
                end++;
            }
            p = end + 1;
        }
        tied.clear();
        tied.addAll(next);
    }


    /**
     * Reads the fermata and the articulations of a note.
     *
     * @param stream the object stream
     * @param object the note object
     * @param event the event (updated)
     */
    private void readArticulations(SibeliusObjectStream stream, SibeliusObject object, EventData event) {
        if ((stream.u8(object.getOffset() + FERMATA_OFFSET) & FERMATA) != 0) {
            event.getFermatas().add(ScoreBuilder.getInstance().createFermata(null, false));
        }
        if (event.isRest()) {
            return;
        }
        int flags = stream.u8(object.getOffset() + ARTICULATION_OFFSET);
        if ((flags & ACCENT) != 0) {
            event.articulations().setAccent(EMPTY);
        }
        if ((flags & STACCATO) != 0) {
            event.articulations().setStaccato(EMPTY);
        }
        if ((flags & TENUTO) != 0) {
            event.articulations().setTenuto(EMPTY);
        }
    }


    /**
     * Adds a hairpin: a wedge at its start and a wedge stop at its end.
     *
     * @param stream the object stream
     * @param object the hairpin object
     * @param part the part
     * @param barStarts the start of each bar
     * @param numbers the end of the wedge (or slur) using each number (updated)
     */
    private void addHairpin(SibeliusObjectStream stream, SibeliusObject object, PartData part, long[] barStarts, int[] numbers) {
        long start = barStarts[object.getBar() - 1] + object.getPosition();
        long end = start + stream.u32(object.getOffset() + LENGTH_OFFSET);
        int number = allocate(numbers, start, end);
        String type = "crescendo";
        if (object.getType() == SibeliusObjectStream.DIMINUENDO) {
            type = "diminuendo";
        }
        ScoreBuilder builder = ScoreBuilder.getInstance();
        part.measure(object.getBar() - 1).addDirection(builder.createWedge(type, number), Fraction.of(object.getPosition(), WHOLE), 1);

        // a hairpin to the end of a bar stops at the end of that bar
        int bar = object.getBar();
        while (bar < barStarts.length - 1 && barStarts[bar] < end) {
            bar++;
        }
        part.measure(bar - 1).addDirection(builder.createWedge(ScoreBuilder.STOP, number), Fraction.of(end - barStarts[bar - 1], WHOLE), 1);
    }


    /**
     * Adds a staff text: a dynamic, an expression or a technique.
     *
     * @param stream the object stream
     * @param object the text object
     * @param measure the measure
     * @param position the position in the measure
     */
    private void addText(SibeliusObjectStream stream, SibeliusObject object, MeasureData measure, Fraction position) {
        List<String> strings = stream.getStrings(object);
        if (strings.isEmpty()) {
            return;
        }
        String text = SibeliusTextConverter.getInstance().getPlainText(strings.get(0));
        if (text.isEmpty()) {
            return;
        }
        String style = EMPTY;
        if (strings.size() > 1) {
            style = strings.get(1);
        }
        ScoreBuilder builder = ScoreBuilder.getInstance();
        if (style.contains("expression")) {
            if (DYNAMICS.matcher(text).matches()) {
                measure.addDirection(builder.createDynamics(text), position, 1);
            } else {
                measure.addDirection(builder.createWords(text, ScoreBuilder.BELOW, "italic", null), position, 1);
            }
        } else {
            measure.addDirection(builder.createWords(text, ScoreBuilder.ABOVE, null, null), position, 1);
        }
    }


    /**
     * Adds the slurs to the first and the last note under each slur.
     *
     * @param stream the object stream
     * @param slurs the slur objects
     * @param events the events of the staff
     * @param barStarts the start of each bar
     */
    private void addSlurs(SibeliusObjectStream stream, List<SibeliusObject> slurs, List<SibeliusEvent> events, long[] barStarts) {
        events.sort(Comparator.comparingLong(SibeliusEvent::getPosition));
        int[] numbers = new int[MAX_NUMBER + 1];
        for (SibeliusObject slur : slurs) {
            long start = barStarts[slur.getBar() - 1] + slur.getPosition();
            long end = start + stream.u32(slur.getOffset() + LENGTH_OFFSET);
            SibeliusEvent first = null;
            SibeliusEvent last = null;
            for (SibeliusEvent event : events) {
                if (event.getVoice() != slur.getVoice() || event.getEvent().isRest() || event.getPosition() > end) {
                    continue;
                }
                if (first == null && event.getPosition() >= start) {
                    first = event;
                }
                if (first != null) {
                    last = event;
                }
            }
            if (first == null || last == first) {
                continue;
            }
            int number = allocate(numbers, start, end);
            first.getEvent().getSlurStarts().add(number);
            last.getEvent().getSlurStops().add(number);
        }
    }


    /**
     * Allocates the lowest number (1 to 6) which is free at a start position.
     *
     * @param numbers the end position using each number (updated)
     * @param start the start position
     * @param end the end position
     * @return the number
     */
    private int allocate(int[] numbers, long start, long end) {
        for (int n = 1; n <= MAX_NUMBER; n++) {
            if (numbers[n] <= start) {
                numbers[n] = (int) end;
                return n;
            }
        }
        numbers[1] = (int) end;
        return 1;
    }
}
