/*
 * ScoreBuilder.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.converter;

import com.github.toolarium.music.io.dto.ClefChange;
import com.github.toolarium.music.io.dto.DirectionData;
import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.io.dto.Fraction;
import com.github.toolarium.music.io.dto.MeasureData;
import com.github.toolarium.music.io.dto.NoteData;
import com.github.toolarium.music.io.dto.PartData;
import com.github.toolarium.music.io.dto.ScoreData;
import com.github.toolarium.music.io.dto.TypeAndDots;
import com.github.toolarium.music.io.dto.VoiceData;
import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.barline.impl.EndingImpl;
import com.github.toolarium.music.model.barline.impl.RepeatImpl;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.DirectionTypeImpl;
import com.github.toolarium.music.model.direction.impl.MetronomeImpl;
import com.github.toolarium.music.model.direction.impl.OctaveShiftImpl;
import com.github.toolarium.music.model.direction.impl.PedalImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.direction.impl.WedgeImpl;
import com.github.toolarium.music.model.direction.impl.WordsImpl;
import com.github.toolarium.music.model.measure.impl.AttributesImpl;
import com.github.toolarium.music.model.measure.impl.BackupImpl;
import com.github.toolarium.music.model.measure.impl.ClefImpl;
import com.github.toolarium.music.model.measure.impl.KeyImpl;
import com.github.toolarium.music.model.measure.impl.MeasureImpl;
import com.github.toolarium.music.model.measure.impl.PartImpl;
import com.github.toolarium.music.model.measure.impl.StaffDetailsImpl;
import com.github.toolarium.music.model.measure.impl.TimeImpl;
import com.github.toolarium.music.model.measure.impl.TransposeImpl;
import com.github.toolarium.music.model.notation.impl.FermataImpl;
import com.github.toolarium.music.model.notation.impl.GlissandoImpl;
import com.github.toolarium.music.model.notation.impl.NotationsImpl;
import com.github.toolarium.music.model.notation.impl.PlayingTechniqueImpl;
import com.github.toolarium.music.model.notation.impl.SlurImpl;
import com.github.toolarium.music.model.notation.impl.TiedImpl;
import com.github.toolarium.music.model.notation.impl.TupletImpl;
import com.github.toolarium.music.model.note.impl.GraceImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import com.github.toolarium.music.model.note.impl.PitchImpl;
import com.github.toolarium.music.model.note.impl.RestImpl;
import com.github.toolarium.music.model.note.impl.TieImpl;
import com.github.toolarium.music.model.note.impl.TimeModificationImpl;
import com.github.toolarium.music.model.note.impl.UnpitchedImpl;
import com.github.toolarium.music.model.part.impl.MidiInstrumentImpl;
import com.github.toolarium.music.model.part.impl.PartListImpl;
import com.github.toolarium.music.model.part.impl.ScoreInstrumentImpl;
import com.github.toolarium.music.model.part.impl.ScorePartImpl;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.CreatorImpl;
import com.github.toolarium.music.model.score.impl.CreditImpl;
import com.github.toolarium.music.model.score.impl.CreditWordsImpl;
import com.github.toolarium.music.model.score.impl.EncodingImpl;
import com.github.toolarium.music.model.score.impl.IdentificationImpl;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import com.github.toolarium.music.model.score.impl.WorkImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Builds the {@link IScore} model from the format independent data collected by a reader
 * ({@link ScoreData}, {@link PartData}, {@link MeasureData}, {@link VoiceData}, {@link EventData}).
 *
 * <p>The builder calculates the divisions of each part, fills gaps in voices with invisible
 * rests, writes the attributes where time, key or clef change, creates the notations of ties,
 * slurs and tuplets and places directions and barlines. Parts with fewer measures than others
 * are filled with measure rests.</p>
 *
 * @author patrick
 */
public final class ScoreBuilder {
    /** Placement above the staff. */
    public static final String ABOVE = "above";

    /** Placement below the staff. */
    public static final String BELOW = "below";

    /** Start of a spanner. */
    public static final String START = "start";

    /** End of a spanner. */
    public static final String STOP = "stop";

    private static final String EMPTY = "";
    private static final String YES = "yes";
    private static final String NO = "no";
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final int DEFAULT_BEATS = 4;
    private static final int DEFAULT_BEAT_TYPE = 4;
    private static final int QUARTERS_PER_WHOLE = 4;
    private static final int VOICES_PER_STAFF = 4;
    private static final long MAX_DIVISIONS = 16_777_216L;
    private static final int FALLBACK_DIVISIONS = 10_080;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int STEPS_PER_OCTAVE = 7;
    private static final int TAB_CLEF_LINE = 5;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final ScoreBuilder INSTANCE = new ScoreBuilder();
    }


    /**
     * Constructor.
     */
    private ScoreBuilder() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static ScoreBuilder getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Builds the score.
     *
     * @param data the collected score data
     * @return the score
     */
    public ScoreImpl build(ScoreData data) {
        ScoreImpl score = new ScoreImpl();
        score.setVersion("4.0");
        buildIdentification(score, data);

        int measureCount = 0;
        for (PartData part : data.getParts()) {
            measureCount = Math.max(measureCount, part.getMeasures().size());
        }
        if (measureCount == 0) {
            measureCount = 1;
        }

        PartListImpl partList = new PartListImpl();
        List<ScorePartImpl> scoreParts = new ArrayList<>();
        List<PartImpl> parts = new ArrayList<>();
        for (int p = 0; p < data.getParts().size(); p++) {
            PartData part = data.getParts().get(p);
            String partId = "P" + (p + 1);
            scoreParts.add(buildScorePart(partId, part));
            parts.add(buildPart(partId, part, measureCount));
        }
        partList.setScoreParts(scoreParts);
        score.setPartList(partList);
        score.setParts(parts);
        return score;
    }


    /**
     * Creates a direction.
     *
     * @param placement above, below or null
     * @param types the direction types
     * @return the direction
     */
    public DirectionImpl createDirection(String placement, DirectionTypeImpl... types) {
        DirectionImpl direction = new DirectionImpl();
        if (placement != null) {
            direction.setPlacement(placement);
        }
        List<DirectionTypeImpl> list = new ArrayList<>();
        Collections.addAll(list, types);
        direction.setDirectionTypes(list);
        return direction;
    }


    /**
     * Creates a text direction.
     *
     * @param text the text
     * @param placement above, below or null
     * @param fontStyle the font style (italic) or null
     * @param fontWeight the font weight (bold) or null
     * @return the direction
     */
    public DirectionImpl createWords(String text, String placement, String fontStyle, String fontWeight) {
        WordsImpl words = new WordsImpl();
        words.setValue(text);
        if (fontStyle != null) {
            words.setFontStyle(fontStyle);
        }
        if (fontWeight != null) {
            words.setFontWeight(fontWeight);
        }
        DirectionTypeImpl type = new DirectionTypeImpl();
        List<WordsImpl> list = new ArrayList<>();
        list.add(words);
        type.setWords(list);
        return createDirection(placement, type);
    }


    /**
     * Creates a dynamics direction.
     *
     * @param marking the marking, for example mf
     * @return the direction
     */
    public DirectionImpl createDynamics(String marking) {
        DirectionTypeImpl type = new DirectionTypeImpl();
        type.setDynamics(DynamicsConverter.getInstance().createDynamics(marking));
        return createDirection(BELOW, type);
    }


    /**
     * Creates a tempo direction with a metronome mark and a sound tempo.
     *
     * @param bpm the beats per minute of the beat unit (0 for text only)
     * @param beatUnit the beat unit note type, for example quarter
     * @param beatUnitDots the number of dots of the beat unit
     * @param text the tempo text, or null
     * @return the direction
     */
    public DirectionImpl createTempo(double bpm, String beatUnit, int beatUnitDots, String text) {
        List<DirectionTypeImpl> types = new ArrayList<>();
        if (text != null && !text.trim().isEmpty()) {
            WordsImpl words = new WordsImpl();
            words.setValue(text.trim());
            words.setFontWeight("bold");
            DirectionTypeImpl wordsType = new DirectionTypeImpl();
            List<WordsImpl> list = new ArrayList<>();
            list.add(words);
            wordsType.setWords(list);
            types.add(wordsType);
        }
        DirectionImpl direction;
        if (bpm > 0) {
            MetronomeImpl metronome = new MetronomeImpl();
            metronome.setBeatUnit(beatUnit);
            if (beatUnitDots > 0) {
                metronome.setBeatUnitDot(EMPTY);
            }
            metronome.setPerMinute(formatNumber(bpm));
            DirectionTypeImpl type = new DirectionTypeImpl();
            type.setMetronome(metronome);
            types.add(type);
            direction = createDirection(ABOVE, types.toArray(new DirectionTypeImpl[0]));
            Fraction unit = NoteValueConverter.getInstance().toFraction(beatUnit, beatUnitDots);
            double quarters = bpm;
            if (unit != null) {
                quarters = bpm * unit.getNumerator() * QUARTERS_PER_WHOLE / unit.getDenominator();
            }
            SoundImpl sound = new SoundImpl();
            sound.setTempo(formatNumber(quarters));
            direction.setSound(sound);
        } else {
            direction = createDirection(ABOVE, types.toArray(new DirectionTypeImpl[0]));
        }
        return direction;
    }


    /**
     * Creates a wedge (hairpin) direction.
     *
     * @param type crescendo, diminuendo or stop
     * @param number the wedge number
     * @return the direction
     */
    public DirectionImpl createWedge(String type, int number) {
        WedgeImpl wedge = new WedgeImpl();
        wedge.setType(type);
        wedge.setNumber(number);
        DirectionTypeImpl directionType = new DirectionTypeImpl();
        directionType.setWedge(wedge);
        return createDirection(BELOW, directionType);
    }


    /**
     * Creates a rehearsal mark direction.
     *
     * @param text the rehearsal mark
     * @return the direction
     */
    public DirectionImpl createRehearsal(String text) {
        DirectionTypeImpl type = new DirectionTypeImpl();
        type.setRehearsal(text);
        return createDirection(ABOVE, type);
    }


    /**
     * Creates a segno direction.
     *
     * @return the direction
     */
    public DirectionImpl createSegno() {
        DirectionTypeImpl type = new DirectionTypeImpl();
        type.setSegno(EMPTY);
        return createDirection(ABOVE, type);
    }


    /**
     * Creates a coda direction.
     *
     * @return the direction
     */
    public DirectionImpl createCoda() {
        DirectionTypeImpl type = new DirectionTypeImpl();
        type.setCoda(EMPTY);
        return createDirection(ABOVE, type);
    }


    /**
     * Creates an octave shift direction.
     *
     * @param type up, down or stop (MusicXML: down for 8va, up for 8vb)
     * @param size the size (8, 15 or 22)
     * @return the direction
     */
    public DirectionImpl createOctaveShift(String type, int size) {
        OctaveShiftImpl shift = new OctaveShiftImpl();
        shift.setType(type);
        shift.setSize(size);
        DirectionTypeImpl directionType = new DirectionTypeImpl();
        directionType.setOctaveShift(shift);
        return createDirection(ABOVE, directionType);
    }


    /**
     * Creates a pedal direction.
     *
     * @param type start or stop
     * @return the direction
     */
    public DirectionImpl createPedal(String type) {
        PedalImpl pedal = new PedalImpl();
        pedal.setType(type);
        pedal.setLine(YES);
        DirectionTypeImpl directionType = new DirectionTypeImpl();
        directionType.setPedal(pedal);
        return createDirection(BELOW, directionType);
    }


    /**
     * Creates a clef.
     *
     * @param sign the sign (G, F, C, percussion or TAB)
     * @param line the line, or null for the default line of the sign
     * @param octaveChange the octave change (0 for none)
     * @return the clef
     */
    public ClefImpl createClef(String sign, Integer line, int octaveChange) {
        ClefImpl clef = new ClefImpl();
        clef.setSign(sign);
        if (line != null) {
            clef.setLine(line);
        }
        if (octaveChange != 0) {
            clef.setClefOctaveChange(octaveChange);
        }
        return clef;
    }


    /**
     * Creates a lyric.
     *
     * @param number the verse number (1-based)
     * @param text the syllable text
     * @param syllabic single, begin, middle or end (null for single)
     * @param extend true for a melisma extension line
     * @return the lyric
     */
    public LyricImpl createLyric(int number, String text, String syllabic, boolean extend) {
        LyricImpl lyric = new LyricImpl();
        lyric.setNumber(String.valueOf(number));
        if (syllabic == null) {
            lyric.setSyllabic("single");
        } else {
            lyric.setSyllabic(syllabic);
        }
        lyric.setText(text);
        if (extend) {
            lyric.setExtend(EMPTY);
        }
        return lyric;
    }


    /**
     * Creates a fermata.
     *
     * @param shape the shape (normal, angled, square), or null for normal
     * @param inverted true for a fermata below the note
     * @return the fermata
     */
    public FermataImpl createFermata(String shape, boolean inverted) {
        FermataImpl fermata = new FermataImpl();
        if (inverted) {
            fermata.setType("inverted");
        } else {
            fermata.setType("upright");
        }
        if (shape != null) {
            fermata.setValue(shape);
        }
        return fermata;
    }


    /**
     * Builds the identification, work and credits.
     *
     * @param score the score
     * @param data the score data
     */
    private void buildIdentification(ScoreImpl score, ScoreData data) {
        String title = trimToNull(data.getTitle());
        if (title != null) {
            WorkImpl work = new WorkImpl();
            work.setWorkTitle(title);
            score.setWork(work);
            score.setMovementTitle(title);
        }
        if (trimToNull(data.getMovementNumber()) != null) {
            score.setMovementNumber(data.getMovementNumber().trim());
        }

        List<CreditImpl> credits = new ArrayList<>();
        addCredit(credits, "title", title);
        addCredit(credits, "subtitle", trimToNull(data.getSubtitle()));

        IdentificationImpl identification = new IdentificationImpl();
        List<CreatorImpl> creators = new ArrayList<>();
        for (CreatorImpl creator : data.getCreators()) {
            creators.add(creator);
            addCredit(credits, creator.getType(), creator.getValue());
        }
        if (!creators.isEmpty()) {
            identification.setCreators(creators);
        }
        if (trimToNull(data.getRights()) != null) {
            identification.setRights(data.getRights().trim());
            addCredit(credits, "rights", data.getRights().trim());
        }
        if (trimToNull(data.getSource()) != null) {
            identification.setSource(data.getSource().trim());
        }
        EncodingImpl encoding = new EncodingImpl();
        List<String> software = new ArrayList<>();
        software.add(EncodingConverter.getInstance().getSoftware());
        if (trimToNull(data.getSoftware()) != null) {
            software.add(data.getSoftware().trim());
        }
        encoding.setSoftware(software);
        if (trimToNull(data.getEncodingDate()) != null) {
            encoding.setEncodingDate(data.getEncodingDate().trim());
        }
        identification.setEncoding(encoding);
        score.setIdentification(identification);
        if (!credits.isEmpty()) {
            score.setCredits(credits);
        }
    }


    /**
     * Adds a credit on the first page.
     *
     * @param credits the credits
     * @param type the credit type
     * @param text the text, ignored if null
     */
    private void addCredit(List<CreditImpl> credits, String type, String text) {
        if (text == null) {
            return;
        }
        CreditImpl credit = new CreditImpl();
        credit.setPage(1);
        credit.setCreditType(type);
        CreditWordsImpl words = new CreditWordsImpl();
        words.setValue(text);
        credit.setCreditWords(words);
        credits.add(credit);
    }


    /**
     * Builds the score part (part list entry) of a part.
     *
     * @param partId the part ID
     * @param part the part data
     * @return the score part
     */
    private ScorePartImpl buildScorePart(String partId, PartData part) {
        ScorePartImpl scorePart = new ScorePartImpl();
        scorePart.setId(partId);
        String name = trimToNull(part.getName());
        if (name == null) {
            name = partId;
        }
        scorePart.setPartName(name);
        if (trimToNull(part.getAbbreviation()) != null) {
            scorePart.setPartAbbreviation(part.getAbbreviation().trim());
        }
        if (part.getMidiProgram() != null || part.getMidiChannel() != null) {
            ScoreInstrumentImpl instrument = new ScoreInstrumentImpl();
            instrument.setId(partId + "-I1");
            instrument.setInstrumentName(name);
            scorePart.setScoreInstruments(Collections.singletonList(instrument));
            MidiInstrumentImpl midi = new MidiInstrumentImpl();
            midi.setId(partId + "-I1");
            midi.setMidiChannel(part.getMidiChannel());
            midi.setMidiBank(part.getMidiBank());
            midi.setMidiProgram(part.getMidiProgram());
            midi.setVolume(part.getVolume());
            midi.setPan(part.getPan());
            scorePart.setMidiInstruments(Collections.singletonList(midi));
        }
        return scorePart;
    }


    /**
     * Builds a part with all measures.
     *
     * @param partId the part ID
     * @param part the part data
     * @param measureCount the number of measures of the score
     * @return the part
     */
    private PartImpl buildPart(String partId, PartData part, int measureCount) {
        for (int m = 0; m < measureCount; m++) {
            part.measure(m);
        }
        int divisions = calculateDivisions(part);
        boolean multiStaff = part.getStaves() > 1;
        Map<Integer, Integer> defaultVoices = defaultVoices(part);

        PartImpl partImpl = new PartImpl();
        partImpl.setId(partId);
        List<MeasureImpl> measures = new ArrayList<>();
        int beats = DEFAULT_BEATS;
        int beatType = DEFAULT_BEAT_TYPE;
        String timeSymbol = null;
        Integer currentKey = null;
        String currentMode = null;
        Map<Integer, String> currentClefs = new HashMap<>();
        int[] slurNumbers = new int[1];

        for (int m = 0; m < measureCount; m++) {
            MeasureData data = part.getMeasures().get(m);
            boolean timeChange = m == 0;
            if (data.getBeats() != null && data.getBeatType() != null && data.getBeats() > 0 && data.getBeatType() > 0) {
                timeChange |= beats != data.getBeats() || beatType != data.getBeatType() || !equalsNullable(timeSymbol, data.getTimeSymbol());
                beats = data.getBeats();
                beatType = data.getBeatType();
                timeSymbol = data.getTimeSymbol();
            }
            Fraction nominal = Fraction.of(beats, beatType);
            Fraction length = measureLength(data, nominal, m);

            MeasureImpl measure = new MeasureImpl();
            measure.setNumber(String.valueOf(m + 1));
            if (data.isImplicit() || (m == 0 && length.compareTo(nominal) < 0)) {
                measure.setImplicit(YES);
            }

            AttributesImpl attributes = new AttributesImpl();
            boolean used = false;
            if (m == 0) {
                attributes.setDivisions(divisions);
                if (multiStaff) {
                    attributes.setStaves(part.getStaves());
                }
                applyTranspose(attributes, part);
                applyStaffDetails(attributes, part);
                used = true;
            }
            Integer keyFifths = data.getKeyFifths();
            if (m == 0 && keyFifths == null) {
                keyFifths = 0;
            }
            if (keyFifths != null && (currentKey == null || !currentKey.equals(keyFifths) || !equalsNullable(currentMode, data.getKeyMode()))) {
                KeyImpl key = new KeyImpl();
                key.setFifths(keyFifths);
                if (data.getKeyMode() != null) {
                    key.setMode(data.getKeyMode());
                }
                attributes.setKeys(Collections.singletonList(key));
                currentKey = keyFifths;
                currentMode = data.getKeyMode();
                used = true;
            }
            if (timeChange) {
                TimeImpl time = new TimeImpl();
                time.setBeats(String.valueOf(beats));
                time.setBeatType(String.valueOf(beatType));
                if (timeSymbol != null) {
                    time.setSymbol(timeSymbol);
                }
                attributes.setTimes(Collections.singletonList(time));
                used = true;
            }
            List<ClefChange> midMeasure = new ArrayList<>();
            List<ClefImpl> clefs = buildClefs(part, data, m, multiStaff, currentClefs, midMeasure);
            if (!clefs.isEmpty()) {
                attributes.setClefs(clefs);
                used = true;
            }

            // the measure content in document order: left barline, attributes, measure directions (with their offset
            // from the measure start), chord symbols of the measure, the voices separated by backups, right barline
            List<BarlineImpl> barlines = buildBarlines(part, m, measureCount);
            List<IMusicDataElement> content = new ArrayList<>();
            for (BarlineImpl barline : barlines) {
                if (LEFT.equals(barline.getLocation())) {
                    content.add(barline);
                }
            }
            if (used) {
                content.add(attributes);
            }
            for (DirectionData direction : data.getDirections()) {
                content.add(positionedDirection(direction, divisions, multiStaff));
            }
            content.addAll(data.getHarmonies());
            content.addAll(buildNotes(part, data, length, divisions, multiStaff, defaultVoices, midMeasure, slurNumbers));
            for (BarlineImpl barline : barlines) {
                if (!LEFT.equals(barline.getLocation())) {
                    content.add(barline);
                }
            }
            measure.setMusicData(content);
            measures.add(measure);
        }
        partImpl.setMeasures(measures);
        return partImpl;
    }


    /**
     * Calculates the length of a measure: the explicit length, the content of a pickup measure,
     * or the time signature (longer if the content is longer).
     *
     * @param data the measure data
     * @param nominal the length of the time signature
     * @param index the measure index
     * @return the length in whole notes
     */
    private Fraction measureLength(MeasureData data, Fraction nominal, int index) {
        if (data.getLength() != null && data.getLength().isPositive()) {
            return data.getLength();
        }
        Fraction content = data.getContentLength();
        if (!content.isPositive()) {
            return nominal;
        }
        if ((index == 0 || data.isImplicit()) && content.compareTo(nominal) < 0) {
            return content;
        }
        return nominal.max(content);
    }


    /**
     * Calculates the divisions per quarter note of a part: the least common multiple of the
     * denominators of all durations and positions.
     *
     * @param part the part data
     * @return the divisions
     */
    private int calculateDivisions(PartData part) {
        long result = 1;
        for (MeasureData measure : part.getMeasures()) {
            if (measure.getLength() != null) {
                result = lcm(result, quarterDenominator(measure.getLength()));
            }
            if (measure.getBeats() != null && measure.getBeatType() != null && measure.getBeatType() > 0) {
                result = lcm(result, quarterDenominator(Fraction.of(measure.getBeats(), measure.getBeatType())));
            }
            for (VoiceData voice : measure.getVoices()) {
                for (EventData event : voice.getEvents()) {
                    if (event.getDuration() != null && !event.isGrace()) {
                        result = lcm(result, quarterDenominator(event.getDuration()));
                    }
                }
            }
            for (DirectionData direction : measure.getDirections()) {
                if (direction.getPosition() != null) {
                    result = lcm(result, quarterDenominator(direction.getPosition()));
                }
            }
            if (result > MAX_DIVISIONS) {
                return FALLBACK_DIVISIONS;
            }
        }
        return (int) result;
    }


    /**
     * Gets the denominator of a length expressed in quarter notes.
     *
     * @param wholes the length in whole notes
     * @return the denominator
     */
    private long quarterDenominator(Fraction wholes) {
        return wholes.multiply(QUARTERS_PER_WHOLE, 1).getDenominator();
    }


    /**
     * Calculates the least common multiple.
     *
     * @param a the first value
     * @param b the second value
     * @return the least common multiple
     */
    private long lcm(long a, long b) {
        if (a == 0 || b == 0) {
            return Math.max(a, b);
        }
        long x = a;
        long y = b;
        while (y != 0) {
            long t = x % y;
            x = y;
            y = t;
        }
        return a / x * b;
    }


    /**
     * Finds the voice number used for measure rests of each staff: the lowest voice used in the staff.
     *
     * @param part the part data
     * @return the voice number by staff
     */
    private Map<Integer, Integer> defaultVoices(PartData part) {
        Map<Integer, Integer> result = new HashMap<>();
        for (MeasureData measure : part.getMeasures()) {
            for (VoiceData voice : measure.getVoices()) {
                Integer current = result.get(voice.getStaff());
                if (current == null || voice.getVoice() < current) {
                    result.put(voice.getStaff(), voice.getVoice());
                }
            }
        }
        for (int staff = 1; staff <= part.getStaves(); staff++) {
            if (!result.containsKey(staff)) {
                result.put(staff, (staff - 1) * VOICES_PER_STAFF + 1);
            }
        }
        return result;
    }


    /**
     * Adds the transposition of a part to the first attributes.
     *
     * @param attributes the attributes
     * @param part the part data
     */
    private void applyTranspose(AttributesImpl attributes, PartData part) {
        int chromatic = part.getTransposeChromatic();
        int diatonic = part.getTransposeDiatonic();
        if (chromatic == 0 && diatonic == 0) {
            return;
        }
        int octave = 0;
        while (chromatic <= -SEMITONES_PER_OCTAVE) {
            chromatic += SEMITONES_PER_OCTAVE;
            diatonic += STEPS_PER_OCTAVE;
            octave--;
        }
        while (chromatic >= SEMITONES_PER_OCTAVE) {
            chromatic -= SEMITONES_PER_OCTAVE;
            diatonic -= STEPS_PER_OCTAVE;
            octave++;
        }
        TransposeImpl transpose = new TransposeImpl();
        transpose.setChromatic(chromatic);
        transpose.setDiatonic(diatonic);
        if (octave != 0) {
            transpose.setOctaveChange(octave);
        }
        attributes.setTransposes(Collections.singletonList(transpose));
    }


    /**
     * Adds the staff details (number of lines) to the first attributes.
     *
     * @param attributes the attributes
     * @param part the part data
     */
    private void applyStaffDetails(AttributesImpl attributes, PartData part) {
        if (part.getStaffLines().isEmpty()) {
            return;
        }
        List<StaffDetailsImpl> details = new ArrayList<>();
        List<Integer> staves = new ArrayList<>(part.getStaffLines().keySet());
        Collections.sort(staves);
        for (Integer staff : staves) {
            StaffDetailsImpl detail = new StaffDetailsImpl();
            if (part.getStaves() > 1) {
                detail.setNumber(staff);
            }
            detail.setStaffLines(part.getStaffLines().get(staff));
            details.add(detail);
        }
        attributes.setStaffDetails(details);
    }


    /**
     * Builds the clefs at the start of a measure and the clef changes within it.
     *
     * @param part the part data
     * @param data the measure data
     * @param index the measure index
     * @param multiStaff true if the part has more than one staff
     * @param currentClefs the current clef of every staff (updated)
     * @param midMeasure the clef changes within the measure with their staff and position (filled)
     * @return the clefs at the measure start
     */
    private List<ClefImpl> buildClefs(PartData part, MeasureData data, int index, boolean multiStaff,
                                      Map<Integer, String> currentClefs, List<ClefChange> midMeasure) {
        List<ClefImpl> result = new ArrayList<>();
        for (int staff = 1; staff <= part.getStaves(); staff++) {
            ClefImpl start = null;
            List<ClefChange> later = new ArrayList<>();
            for (ClefChange change : data.getClefs()) {
                if (change.getStaff() != staff) {
                    continue;
                }
                if (change.getPosition() == null || !change.getPosition().isPositive()) {
                    start = change.getClef();
                } else {
                    later.add(change);
                }
            }
            if (index == 0 && start == null) {
                start = part.getDefaultClefs().get(staff);
                if (start == null) {
                    start = defaultClef(part, staff);
                }
            }
            if (start != null && !clefKey(start).equals(currentClefs.get(staff))) {
                result.add(numbered(start, staff, multiStaff));
                currentClefs.put(staff, clefKey(start));
            }
            later.sort(Comparator.comparing(ClefChange::getPosition));
            for (ClefChange change : later) {
                if (clefKey(change.getClef()).equals(currentClefs.get(staff))) {
                    continue;
                }
                midMeasure.add(new ClefChange(staff, change.getPosition(), numbered(change.getClef(), staff, multiStaff)));
                currentClefs.put(staff, clefKey(change.getClef()));
            }
        }
        return result;
    }


    /**
     * Creates the default clef of a staff: percussion for percussion parts, TAB for tablature
     * staves (six lines or fewer than five), treble otherwise.
     *
     * @param part the part data
     * @param staff the staff
     * @return the clef
     */
    private ClefImpl defaultClef(PartData part, int staff) {
        if (part.isPercussion()) {
            return createClef("percussion", null, 0);
        }
        return createClef("G", 2, 0);
    }


    /**
     * Copies a clef and sets its staff number.
     *
     * @param clef the clef
     * @param staff the staff
     * @param multiStaff true if the part has more than one staff
     * @return the clef copy
     */
    private ClefImpl numbered(ClefImpl clef, int staff, boolean multiStaff) {
        ClefImpl copy = createClef(clef.getSign(), clef.getLine(), 0);
        copy.setClefOctaveChange(clef.getClefOctaveChange());
        if ("TAB".equals(clef.getSign()) && clef.getLine() == null) {
            copy.setLine(TAB_CLEF_LINE);
        }
        if (multiStaff) {
            copy.setNumber(staff);
        }
        return copy;
    }


    /**
     * Gets a key that identifies a clef.
     *
     * @param clef the clef
     * @return the key
     */
    private String clefKey(ClefImpl clef) {
        return clef.getSign() + "/" + clef.getLine() + "/" + clef.getClefOctaveChange();
    }


    /**
     * Builds the voices of a measure in document order: each voice filled to the measure length, separated
     * by backups. The directions and chord symbols of an event and the clef changes within the measure are
     * placed directly before the notes they belong to.
     *
     * @param part the part data
     * @param data the measure data
     * @param length the measure length
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @param defaultVoices the voice for measure rests of each staff
     * @param clefChanges the clef changes within the measure
     * @param slurNumbers the slur number counter
     * @return the measure content of the voices
     */
    private List<IMusicDataElement> buildNotes(PartData part, MeasureData data, Fraction length, int divisions, boolean multiStaff,
                                               Map<Integer, Integer> defaultVoices, List<ClefChange> clefChanges, int[] slurNumbers) {
        List<VoiceData> voices = new ArrayList<>();
        for (VoiceData voice : data.getVoices()) {
            if (!voice.getEvents().isEmpty()) {
                voices.add(voice);
            }
        }
        for (int staff = 1; staff <= part.getStaves(); staff++) {
            boolean found = false;
            for (VoiceData voice : voices) {
                found |= voice.getStaff() == staff;
            }
            if (!found) {
                VoiceData rest = new VoiceData(staff, defaultVoices.get(staff));
                EventData event = new EventData();
                event.setMeasureRest(true);
                rest.getEvents().add(event);
                voices.add(rest);
            }
        }
        voices.sort(Comparator.comparingInt(VoiceData::getStaff).thenComparingInt(VoiceData::getVoice));

        List<IMusicDataElement> content = new ArrayList<>();
        List<ClefChange> pendingClefs = new ArrayList<>(clefChanges);
        pendingClefs.sort(Comparator.comparing(ClefChange::getPosition));
        int measureDuration = (int) length.toDivisions(divisions);
        for (VoiceData voice : voices) {
            if (!content.isEmpty()) {
                BackupImpl backup = new BackupImpl();
                backup.setDuration(measureDuration);
                content.add(backup);
            }
            Fraction cursor = Fraction.ZERO;
            for (EventData event : voice.getEvents()) {
                int staff = voice.getStaff();
                if (event.getStaff() != null) {
                    staff = event.getStaff();
                }
                addClefChanges(content, pendingClefs, voice.getStaff(), cursor);
                for (DirectionImpl direction : event.getDirections()) {
                    if (multiStaff) {
                        direction.setStaff(Math.max(1, staff));
                    }
                    content.add(direction);
                }
                content.addAll(event.getHarmonies());
                if (event.isGrace()) {
                    content.addAll(buildEvent(event, voice.getVoice(), staff, 0, multiStaff, part, slurNumbers));
                    continue;
                }
                Fraction duration = event.getDuration();
                if (event.isMeasureRest() || duration == null) {
                    duration = length.subtract(cursor);
                    if (!duration.isPositive()) {
                        continue;
                    }
                }
                int divisionDuration = (int) (cursor.add(duration).toDivisions(divisions) - cursor.toDivisions(divisions));
                List<NoteImpl> notes = buildEvent(event, voice.getVoice(), staff, divisionDuration, multiStaff, part, slurNumbers);
                if (event.isMeasureRest() && event.getDuration() == null && !notes.isEmpty()) {
                    notes.get(notes.size() - 1).setType(null);
                }
                content.addAll(notes);
                cursor = cursor.add(duration);
            }
            if (cursor.compareTo(length) < 0) {
                List<NoteImpl> rests = gapRests(cursor, length, voice.getVoice(), voice.getStaff(), divisions, multiStaff);
                Fraction position = cursor;
                for (NoteImpl rest : rests) {
                    addClefChanges(content, pendingClefs, voice.getStaff(), position);
                    content.add(rest);
                    position = position.add(Fraction.of(rest.getDuration(), 4L * divisions));
                }
            }
            addClefChanges(content, pendingClefs, voice.getStaff(), length);
        }
        return content;
    }


    /**
     * Adds the clef changes of a staff that take effect at or before the given position.
     *
     * @param content the measure content (filled)
     * @param pendingClefs the clef changes that are not yet added, sorted by position (updated)
     * @param staff the staff of the voice
     * @param position the position in the measure
     */
    private void addClefChanges(List<IMusicDataElement> content, List<ClefChange> pendingClefs, int staff, Fraction position) {
        Iterator<ClefChange> iterator = pendingClefs.iterator();
        while (iterator.hasNext()) {
            ClefChange change = iterator.next();
            if (change.getStaff() == staff && change.getPosition().compareTo(position) <= 0) {
                AttributesImpl attributes = new AttributesImpl();
                attributes.setClefs(Collections.singletonList(change.getClef()));
                content.add(attributes);
                iterator.remove();
            }
        }
    }


    /**
     * Creates a direction of the measure with its offset from the measure start.
     *
     * @param direction the direction with its position
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @return the direction
     */
    private DirectionImpl positionedDirection(DirectionData direction, int divisions, boolean multiStaff) {
        DirectionImpl impl = direction.getDirection();
        int offset = 0;
        if (direction.getPosition() != null) {
            offset = (int) direction.getPosition().toDivisions(divisions);
        }
        if (offset > 0) {
            impl.setOffset(offset);
        }
        if (multiStaff) {
            impl.setStaff(Math.max(1, direction.getStaff()));
        }
        return impl;
    }


    /**
     * Creates invisible rests for a gap at the end of a voice.
     *
     * @param from the start of the gap
     * @param to the end of the gap
     * @param voice the voice number
     * @param staff the staff
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @return the rests
     */
    private List<NoteImpl> gapRests(Fraction from, Fraction to, int voice, int staff, int divisions, boolean multiStaff) {
        List<NoteImpl> result = new ArrayList<>();
        Fraction position = from;
        for (TypeAndDots piece : NoteValueConverter.getInstance().split(to.subtract(from))) {
            Fraction end = position.add(NoteValueConverter.getInstance().toFraction(piece.getType(), piece.getDots()));
            NoteImpl rest = gapRest(position, end, voice, staff, divisions, multiStaff);
            rest.setType(piece.getType());
            if (piece.getDots() > 0) {
                rest.setDots(dots(piece.getDots()));
            }
            result.add(rest);
            position = end;
        }
        if (position.compareTo(to) < 0) {
            result.add(gapRest(position, to, voice, staff, divisions, multiStaff));
        }
        return result;
    }


    /**
     * Creates one invisible rest.
     *
     * @param from the start
     * @param to the end
     * @param voice the voice number
     * @param staff the staff
     * @param divisions the divisions per quarter note
     * @param multiStaff true if the part has more than one staff
     * @return the rest
     */
    private NoteImpl gapRest(Fraction from, Fraction to, int voice, int staff, int divisions, boolean multiStaff) {
        NoteImpl rest = new NoteImpl();
        rest.setRest(new RestImpl());
        rest.setDuration((int) (to.toDivisions(divisions) - from.toDivisions(divisions)));
        rest.setPrintObject(NO);
        rest.setVoice(voice);
        if (multiStaff) {
            rest.setStaff(staff);
        }
        return rest;
    }


    /**
     * Builds the notes of one event (a note, a chord or a rest).
     *
     * @param event the event
     * @param voice the voice number
     * @param staff the staff
     * @param duration the duration in divisions (0 for grace notes)
     * @param multiStaff true if the part has more than one staff
     * @param part the part data
     * @param slurNumbers the slur number counter
     * @return the notes
     */
    private List<NoteImpl> buildEvent(EventData event, int voice, int staff, int duration, boolean multiStaff, PartData part, int[] slurNumbers) {
        List<NoteImpl> result = new ArrayList<>();
        if (event.isRest()) {
            NoteImpl rest = new NoteImpl();
            RestImpl restImpl = new RestImpl();
            if (event.isMeasureRest()) {
                restImpl.setMeasure(YES);
            }
            rest.setRest(restImpl);
            rest.setDuration(duration);
            applyCommon(rest, event, voice, staff, multiStaff);
            applyNotations(rest, event, null, true);
            if (!event.getLyrics().isEmpty()) {
                rest.setLyrics(new ArrayList<>(event.getLyrics()));
            }
            result.add(rest);
            return result;
        }

        for (int n = 0; n < event.getNotes().size(); n++) {
            NoteData data = event.getNotes().get(n);
            NoteImpl note = new NoteImpl();
            if (event.isGrace()) {
                GraceImpl grace = new GraceImpl();
                if (event.isGraceSlash()) {
                    grace.setSlash(YES);
                }
                note.setGrace(grace);
            } else {
                note.setDuration(duration);
            }
            if (n > 0) {
                note.setChord(EMPTY);
            }
            if (data.isUnpitched() || (part.isPercussion() && data.getStep() != null)) {
                UnpitchedImpl unpitched = new UnpitchedImpl();
                unpitched.setDisplayStep(data.getStep());
                unpitched.setDisplayOctave(data.getOctave());
                note.setUnpitched(unpitched);
            } else {
                PitchImpl pitch = new PitchImpl();
                pitch.setStep(data.getStep());
                if (data.getAlter() != 0) {
                    pitch.setAlter((double) data.getAlter());
                }
                pitch.setOctave(data.getOctave());
                note.setPitch(pitch);
            }
            applyCommon(note, event, voice, staff, multiStaff);
            if (data.getAccidental() != null) {
                note.setAccidental(data.getAccidental());
            }
            if (data.getNotehead() != null) {
                note.setNotehead(data.getNotehead());
            }
            if (data.isTieStart() || data.isTieStop()) {
                List<TieImpl> ties = new ArrayList<>();
                if (data.isTieStop()) {
                    ties.add(tie(STOP));
                }
                if (data.isTieStart()) {
                    ties.add(tie(START));
                }
                note.setTies(ties);
            }
            applyNotations(note, event, data, n == 0);
            if (n == 0 && !event.getLyrics().isEmpty()) {
                note.setLyrics(new ArrayList<>(event.getLyrics()));
            }
            result.add(note);
        }
        return result;
    }


    /**
     * Sets the type, dots, time modification, voice, staff, stem and visibility of a note or rest.
     *
     * @param note the note
     * @param event the event
     * @param voice the voice number
     * @param staff the staff
     * @param multiStaff true if the part has more than one staff
     */
    private void applyCommon(NoteImpl note, EventData event, int voice, int staff, boolean multiStaff) {
        String type = event.getType();
        int dotCount = event.getDots();
        if (type == null && event.getDuration() != null && !event.isMeasureRest()) {
            Fraction nominal = event.getDuration();
            if (event.getTupletActual() > 0 && event.getTupletNormal() > 0) {
                nominal = nominal.multiply(event.getTupletActual(), event.getTupletNormal());
            }
            TypeAndDots typeAndDots = NoteValueConverter.getInstance().fromFraction(nominal);
            if (typeAndDots != null) {
                type = typeAndDots.getType();
                dotCount = typeAndDots.getDots();
            }
        }
        if (type != null) {
            note.setType(type);
        }
        if (dotCount > 0) {
            note.setDots(dots(dotCount));
        }
        if (event.getTupletActual() > 0 && event.getTupletNormal() > 0) {
            TimeModificationImpl modification = new TimeModificationImpl();
            modification.setActualNotes(event.getTupletActual());
            modification.setNormalNotes(event.getTupletNormal());
            if (event.getTupletNormalType() != null) {
                modification.setNormalType(event.getTupletNormalType());
            }
            note.setTimeModification(modification);
        }
        note.setVoice(voice);
        if (multiStaff) {
            note.setStaff(staff);
        }
        if (event.getStem() != null) {
            note.setStem(event.getStem());
        }
        if (event.isInvisible()) {
            note.setPrintObject(NO);
        }
    }


    /**
     * Adds the notations of a note: ties, glissandos and, for the first note of an event, tuplets,
     * slurs, articulations, ornaments, technical marks and fermatas.
     *
     * @param note the note
     * @param event the event
     * @param data the note data (null for rests)
     * @param first true for the first note of the event
     */
    private void applyNotations(NoteImpl note, EventData event, NoteData data, boolean first) {
        NotationsImpl notations = new NotationsImpl();
        boolean used = false;
        if (data != null && (data.isTieStart() || data.isTieStop())) {
            List<TiedImpl> tieds = new ArrayList<>();
            if (data.isTieStop()) {
                tieds.add(tied(STOP));
            }
            if (data.isTieStart()) {
                tieds.add(tied(START));
            }
            notations.setTieds(tieds);
            used = true;
        }
        if (data != null && (data.isGlissandoStart() || data.isGlissandoStop())) {
            List<GlissandoImpl> glissandos = new ArrayList<>();
            if (data.isGlissandoStop()) {
                glissandos.add(glissando(STOP));
            }
            if (data.isGlissandoStart()) {
                glissandos.add(glissando(START));
            }
            notations.setGlissandos(glissandos);
            used = true;
        }
        PlayingTechniqueImpl technical = null;
        if (data != null) {
            technical = data.getTechnical();
        }
        if (first) {
            technical = mergeTechnical(technical, event.getTechnical());
            used |= applyEventNotations(notations, event);
        }
        if (technical != null) {
            notations.setTechnical(technical);
            used = true;
        }
        if (used) {
            note.setNotations(notations);
        }
    }


    /**
     * Adds the notations of an event to its first note.
     *
     * @param notations the notations
     * @param event the event
     * @return true if a notation was added
     */
    private boolean applyEventNotations(NotationsImpl notations, EventData event) {
        boolean used = false;
        if (event.isTupletStart() || event.isTupletStop()) {
            List<TupletImpl> tuplets = new ArrayList<>();
            if (event.isTupletStart()) {
                TupletImpl tuplet = new TupletImpl();
                tuplet.setType(START);
                tuplet.setNumber(1);
                tuplets.add(tuplet);
            }
            if (event.isTupletStop()) {
                TupletImpl tuplet = new TupletImpl();
                tuplet.setType(STOP);
                tuplet.setNumber(1);
                tuplets.add(tuplet);
            }
            notations.setTuplets(tuplets);
            used = true;
        }
        if (!event.getSlurStarts().isEmpty() || !event.getSlurStops().isEmpty()) {
            List<SlurImpl> slurs = new ArrayList<>();
            for (Integer number : event.getSlurStops()) {
                slurs.add(slur(STOP, number));
            }
            for (Integer number : event.getSlurStarts()) {
                slurs.add(slur(START, number));
            }
            notations.setSlurs(slurs);
            used = true;
        }
        if (event.getArticulations() != null) {
            notations.setArticulations(event.getArticulations());
            used = true;
        }
        if (event.getOrnaments() != null) {
            notations.setOrnaments(event.getOrnaments());
            used = true;
        }
        if (!event.getFermatas().isEmpty()) {
            notations.setFermatas(new ArrayList<>(event.getFermatas()));
            used = true;
        }
        return used;
    }


    /**
     * Merges the technical notations of a note and of its event.
     *
     * @param note the technical notations of the note (may be null)
     * @param event the technical notations of the event (may be null)
     * @return the merged notations, or null if both are null
     */
    private PlayingTechniqueImpl mergeTechnical(PlayingTechniqueImpl note, PlayingTechniqueImpl event) {
        if (event == null) {
            return note;
        }
        if (note == null) {
            return event;
        }
        PlayingTechniqueImpl result = new PlayingTechniqueImpl();
        result.setUpBow(firstNonNull(note.getUpBow(), event.getUpBow()));
        result.setDownBow(firstNonNull(note.getDownBow(), event.getDownBow()));
        result.setHarmonic(firstNonNull(note.getHarmonic(), event.getHarmonic()));
        result.setOpenString(firstNonNull(note.getOpenString(), event.getOpenString()));
        result.setThumbPosition(firstNonNull(note.getThumbPosition(), event.getThumbPosition()));
        result.setFingering(firstNonNull(note.getFingering(), event.getFingering()));
        result.setPluck(firstNonNull(note.getPluck(), event.getPluck()));
        result.setDoubleTongue(firstNonNull(note.getDoubleTongue(), event.getDoubleTongue()));
        result.setTripleTongue(firstNonNull(note.getTripleTongue(), event.getTripleTongue()));
        result.setStopped(firstNonNull(note.getStopped(), event.getStopped()));
        result.setSnapPizzicato(firstNonNull(note.getSnapPizzicato(), event.getSnapPizzicato()));
        result.setFret(firstNonNull(note.getFret(), event.getFret()));
        result.setString(firstNonNull(note.getString(), event.getString()));
        result.setHammerOn(firstNonNull(note.getHammerOn(), event.getHammerOn()));
        result.setPullOff(firstNonNull(note.getPullOff(), event.getPullOff()));
        return result;
    }


    /**
     * Builds the barlines of a measure: repeats, endings and bar styles.
     *
     * @param part the part data
     * @param index the measure index
     * @param measureCount the number of measures
     * @return the barlines (may be empty)
     */
    private List<BarlineImpl> buildBarlines(PartData part, int index, int measureCount) {
        MeasureData data = part.getMeasures().get(index);
        BarlineImpl left = null;
        BarlineImpl right = null;
        if (data.isRepeatStart()) {
            left = barline(LEFT);
            left.setBarStyle("heavy-light");
            RepeatImpl repeat = new RepeatImpl();
            repeat.setDirection("forward");
            left.setRepeat(repeat);
        } else if (data.getLeftBarStyle() != null) {
            left = barline(LEFT);
            left.setBarStyle(data.getLeftBarStyle());
        }

        if (data.isRepeatEnd()) {
            right = barline(RIGHT);
            right.setBarStyle("light-heavy");
            RepeatImpl repeat = new RepeatImpl();
            repeat.setDirection("backward");
            if (data.getRepeatEndTimes() != null && data.getRepeatEndTimes() > 2) {
                repeat.setTimes(data.getRepeatEndTimes());
            }
            right.setRepeat(repeat);
        } else if (data.getRightBarStyle() != null) {
            right = barline(RIGHT);
            right.setBarStyle(data.getRightBarStyle());
        }

        String ending = data.getEndingNumber();
        if (ending != null) {
            String previous = null;
            if (index > 0) {
                previous = part.getMeasures().get(index - 1).getEndingNumber();
            }
            String next = null;
            if (index + 1 < measureCount) {
                next = part.getMeasures().get(index + 1).getEndingNumber();
            }
            if (!ending.equals(previous) || part.getMeasures().get(index).isRepeatStart()) {
                if (left == null) {
                    left = barline(LEFT);
                }
                left.setEnding(ending(ending, START));
            }
            boolean nextStarts = next != null && next.equals(ending) && part.getMeasures().get(index + 1).isRepeatStart();
            if (!ending.equals(next) || data.isRepeatEnd() || nextStarts) {
                if (right == null) {
                    right = barline(RIGHT);
                }
                if (data.isRepeatEnd()) {
                    right.setEnding(ending(ending, STOP));
                } else {
                    right.setEnding(ending(ending, "discontinue"));
                }
            }
        }

        List<BarlineImpl> result = new ArrayList<>();
        if (left != null) {
            result.add(left);
        }
        if (right != null) {
            result.add(right);
        }
        return result;
    }


    /**
     * Creates an ending.
     *
     * @param number the ending number
     * @param type start, stop or discontinue
     * @return the ending
     */
    private EndingImpl ending(String number, String type) {
        EndingImpl ending = new EndingImpl();
        ending.setNumber(number);
        ending.setType(type);
        if (START.equals(type)) {
            ending.setValue(number + ".");
        }
        return ending;
    }


    /**
     * Creates a barline.
     *
     * @param location left or right
     * @return the barline
     */
    private BarlineImpl barline(String location) {
        BarlineImpl barline = new BarlineImpl();
        barline.setLocation(location);
        return barline;
    }


    /**
     * Creates a tie.
     *
     * @param type start or stop
     * @return the tie
     */
    private TieImpl tie(String type) {
        TieImpl tie = new TieImpl();
        tie.setType(type);
        return tie;
    }


    /**
     * Creates a tied notation.
     *
     * @param type start or stop
     * @return the tied notation
     */
    private TiedImpl tied(String type) {
        TiedImpl tied = new TiedImpl();
        tied.setType(type);
        return tied;
    }


    /**
     * Creates a slur notation.
     *
     * @param type start or stop
     * @param number the slur number
     * @return the slur
     */
    private SlurImpl slur(String type, int number) {
        SlurImpl slur = new SlurImpl();
        slur.setType(type);
        slur.setNumber(number);
        return slur;
    }


    /**
     * Creates a glissando notation.
     *
     * @param type start or stop
     * @return the glissando
     */
    private GlissandoImpl glissando(String type) {
        GlissandoImpl glissando = new GlissandoImpl();
        glissando.setType(type);
        glissando.setNumber(1);
        glissando.setLineType("solid");
        return glissando;
    }


    /**
     * Creates the dots of a note.
     *
     * @param count the number of dots
     * @return the dots
     */
    private List<String> dots(int count) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(EMPTY);
        }
        return result;
    }


    /**
     * Formats a number without a fraction if it is integral.
     *
     * @param value the value
     * @return the text
     */
    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(Math.round(value * 100) / 100.0);
    }


    /**
     * Compares two nullable strings.
     *
     * @param a the first value
     * @param b the second value
     * @return true if both are null or equal
     */
    private boolean equalsNullable(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }


    /**
     * Returns the first non-null value.
     *
     * @param first the first value
     * @param second the second value
     * @return the first non-null value, or null
     */
    private String firstNonNull(String first, String second) {
        if (first != null) {
            return first;
        }
        return second;
    }


    /**
     * Trims a text and returns null for empty text.
     *
     * @param text the text
     * @return the trimmed text, or null
     */
    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
