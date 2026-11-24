# Music Model Reference

This document describes the Java model interfaces that represent musical scores.
All types are defined as interfaces (`I` prefix) in `com.github.toolarium.music.model`
and organized into domain-specific subpackages. Implementations live in `impl/`
subpackages with `Impl` suffix.

For the full architecture, see [ARCHITECTURE.md](ARCHITECTURE.md).


## Element hierarchy

```
IScore
|-- version : String
|-- IWork (workNumber, workTitle)
|-- movementNumber : String
|-- movementTitle : String
|-- IIdentification (creators, rights, encoding, source)
|-- IDefaults (IScaling, IPageLayout, ISystemLayout, IStaffLayout)
|-- List<ICredit> (page, creditType, creditWords)
|-- IPartList
|   |-- List<IPartGroup> (type, number, groupName, groupSymbol, groupBarline)
|   +-- List<IScorePart> (id, partName, scoreInstruments, midiInstruments)
+-- List<IPart>
    +-- List<IMeasure> (number, width)
        |-- List<IAttributes>
        |   |-- divisions : Integer
        |   |-- List<IKey> (fifths, mode)
        |   |-- List<ITime> (beats, beatType)
        |   +-- List<IClef> (sign, line, clefOctaveChange)
        |-- List<INote>
        |   |-- IPitch (step, alter, octave)
        |   |-- IRest / IUnpitched
        |   |-- duration, voice, type, stem, accidental
        |   |-- List<ITie>, List<IBeam>, List<ILyric>
        |   +-- INotations
        |       |-- List<ITied>, List<ISlur>, List<ITuplet>
        |       |-- IArticulations, IDynamics, IOrnaments, IPlayingTechnique
        |       +-- List<IFermata>, List<IGlissando>
        |-- List<IDirection>
        |   |-- List<IDirectionType> (IWords, IWedge, IMetronome, IPedal, IOctaveShift)
        |   +-- ISound (tempo, dynamics, dacapo, segno, coda, fine)
        |-- List<IBarline> (barStyle, IRepeat, IEnding, segno, coda)
        |-- List<IHarmony> (IRoot, kind, IBass, List<IDegree>, IFrame)
        |-- List<IForward>, List<IBackup> (multi-voice navigation)
        +-- List<IPrint> (page/system layout overrides)
```


## Package: model/score

Score-level metadata and the root document type.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IScore` | Root score element | version, work, movementTitle, identification, defaults, credits, partList, parts |
| `IWork` | Work title and number | workNumber, workTitle |
| `IIdentification` | Creator and encoding metadata | creators, rights, encoding, source |
| `ICreator` | A creator entry (composer, arranger, etc.) | type, value |
| `IEncoding` | Software and date of encoding | encodingDate, software, encoder, encodingDescription |
| `IDefaults` | Default layout and scaling | scaling |
| `IScaling` | Millimeters-to-tenths conversion factor | millimeters, tenths |
| `ICredit` | Page credit text (title, composer, etc.) | page, creditType, creditWords |
| `ICreditWords` | Formatted text in a credit | value, defaultX, defaultY, fontSize, justify, valign |

```java
IScore score = MusicFileFactory.getInstance().read(Path.of("song.gp5"));

String title = score.getMovementTitle();
IWork work = score.getWork();
if (work != null) {
    String workTitle = work.getWorkTitle();
}

IIdentification id = score.getIdentification();
if (id != null) {
    for (ICreator creator : id.getCreators()) {
        // creator.getType()  = "composer", "lyricist", ...
        // creator.getValue() = "Johann Sebastian Bach"
    }
}
```


## Package: model/part

Instrument and part definitions.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IPartList` | List of all parts and groups | partGroups, scoreParts |
| `IScorePart` | A single part/instrument definition | id, partName, partAbbreviation, scoreInstruments, midiDevices, midiInstruments |
| `IPartGroup` | Grouping of parts (bracket, brace) | type, number, groupName, groupSymbol, groupBarline |
| `IScoreInstrument` | Instrument definition | id, instrumentName, instrumentAbbreviation, instrumentSound |
| `IMidiDevice` | MIDI device assignment | id, port, value |
| `IMidiInstrument` | MIDI playback configuration | id, midiChannel, midiProgram, volume, pan |

```java
IPartList partList = score.getPartList();
for (IScorePart scorePart : partList.getScoreParts()) {
    String partName = scorePart.getPartName();   // "Piano", "Violin I"
    String partId   = scorePart.getId();          // "P1", "P2"

    for (IMidiInstrument midi : scorePart.getMidiInstruments()) {
        Integer program = midi.getMidiProgram();  // General MIDI program number
        Integer channel = midi.getMidiChannel();
    }
}
```


## Package: model/measure

Measure structure, attributes, and navigation.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IMeasure` | A single measure in a part | number, width, implicit, musicData (content in document order), and typed views: attributes, notes, forwards, backups, directions, barlines, harmonies, prints, sounds |
| `IPart` | A part containing measures | id, measures |
| `IAttributes` | Key, time, clef changes | divisions, keys, times, staves, clefs, transposes, staffDetails |
| `IKey` | Key signature | fifths, mode, cancel |
| `ITime` | Time signature | symbol, beats, beatType |
| `IClef` | Clef definition | number, sign, line, clefOctaveChange |
| `ITranspose` | Transposition for reading instruments | diatonic, chromatic, octaveChange |
| `IStaffDetails` | Staff line configuration | number, staffLines, staffType |
| `IForward` | Move forward in time (multi-voice) | duration, voice, staff |
| `IBackup` | Move backward in time (multi-voice) | duration |
| `IPrint` | Page and system break control | newSystem, newPage, blankPage, pageNumber |

### Measure content order

As in MusicXML, the content of a measure is an ordered sequence: `IMeasure.getMusicData()` returns the
notes, backups, forwards, attributes, directions, harmonies, barlines, prints and sounds in document
order. The order defines the time position of each element: a note advances the time by its duration
(chord notes do not), a backup moves back and a forward moves on; a direction or a clef change applies
at the current time. A measure with two voices therefore contains the notes of voice 1, a backup over
the measure length and the notes of voice 2.

The typed getters (`getNotes()`, `getBackups()`, ...) return read-only lists of one element type in
document order (empty if the measure has none). `MeasureImpl.setMusicData()` and `addMusicData()` set
the content in order; a typed setter replaces the elements of its type at the position of the first one
(attributes and print elements without a previous one are inserted at the start, all others at the end).

```java
for (IPart part : score.getParts()) {
    for (IMeasure measure : part.getMeasures()) {
        for (IAttributes attr : measure.getAttributes()) {
            Integer divisions = attr.getDivisions();

            for (IKey key : attr.getKeys()) {
                Integer fifths = key.getFifths();  // 0=C, 1=G, -1=F, ...
                String  mode   = key.getMode();     // "major", "minor"
            }
            for (ITime time : attr.getTimes()) {
                String beats    = time.getBeats();    // "4"
                String beatType = time.getBeatType(); // "4"
            }
            for (IClef clef : attr.getClefs()) {
                String  sign = clef.getSign();  // "G", "F", "C"
                Integer line = clef.getLine();  // 2 for treble, 4 for bass
            }
        }
    }
}
```


## Package: model/note

Note, pitch, rest, and related types.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `INote` | A note or rest event | pitch, rest, unpitched, chord, duration, ties, voice, type, dots, accidental, timeModification, stem, notehead, staff, beams, notations, lyrics |
| `IPitch` | A pitched note (step + octave + alteration) | step (C-B), alter (-2 to +2), octave (0-8) |
| `IRest` | A rest (optionally positioned) | displayStep, displayOctave, measure |
| `IUnpitched` | An unpitched note (percussion) | displayStep, displayOctave |
| `ITie` | Tie start or stop | type |
| `ITimeModification` | Tuplet ratio | actualNotes, normalNotes, normalType |
| `IBeam` | Beam grouping | number, value (begin, continue, end) |
| `ILyric` | Lyrics attached to a note | number, name, syllabic, text, elision, extend |

```java
for (INote note : measure.getNotes()) {
    if (note.getRest() != null) {
        continue; // rest
    }

    IPitch pitch = note.getPitch();
    if (pitch != null) {
        String  step   = pitch.getStep();   // "C", "D", "E", ...
        Integer octave = pitch.getOctave(); // 4 = middle C octave
        Double  alter  = pitch.getAlter();  // 1.0 = sharp, -1.0 = flat
    }

    Integer duration = note.getDuration(); // in divisions
    String  type     = note.getType();     // "quarter", "eighth", "half", ...
    Integer voice    = note.getVoice();    // voice number for multi-voice
    boolean isChord  = note.getChord() != null; // true for stacked chord notes

    for (ILyric lyric : note.getLyrics()) {
        String syllabic = lyric.getSyllabic(); // "single", "begin", "middle", "end"
        String text     = lyric.getText();
    }
}
```


## Package: model/notation

Articulations, dynamics, ornaments, and other note-level markings.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `INotations` | Container for all note notations | tieds, slurs, tuplets, fermatas, articulations, dynamics, ornaments, playingTechnique, glissandos |
| `ITied` | Visual tie mark | type |
| `ISlur` | Slur line | type, number, lineType, placement |
| `ITuplet` | Tuplet bracket | type, number, bracket, showNumber |
| `IFermata` | Fermata symbol | type, value |
| `IArticulations` | Articulation marks | accent, staccato, tenuto, detachedLegato, staccatissimo, breathMark, caesura, etc. |
| `IDynamics` | Dynamic level markings | piano (p), pp, ppp, forte (f), ff, fff, mp, mf, sf, sfz, fp, etc. |
| `IOrnaments` | Ornament marks | trillMark, turn, mordent, invertedMordent, shake, wavyLine, etc. |
| `IPlayingTechnique` | Performance technique marks | upBow, downBow, harmonic, fingering, pluck, fret, string, hammerOn, pullOff, etc. |
| `IGlissando` | Glissando line | type, number, lineType, value |

```java
INotations notations = note.getNotations();
if (notations != null) {
    for (ISlur slur : notations.getSlurs()) {
        String type = slur.getType(); // "start", "stop"
    }

    IArticulations art = notations.getArticulations();
    if (art != null) {
        String staccato = art.getStaccato(); // non-null = staccato present
    }

    IDynamics dyn = notations.getDynamics();
    if (dyn != null) {
        String marking = dyn.getDynamicMarking(); // "f", "pp", "mf", ...
    }
}
```


## Package: model/direction

Measure-level directions: tempo, dynamics, pedal, rehearsal marks.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IDirection` | A direction attached to a measure | placement, directionTypes, offset, voice, staff, sound |
| `IDirectionType` | The content of a direction | rehearsal, segno, coda, words, wedge, pedal, metronome, octaveShift, dynamics |
| `IWedge` | Crescendo/diminuendo hairpin | type (crescendo, diminuendo, stop), number, spread |
| `IMetronome` | Tempo marking | beatUnit, beatUnitDot, perMinute, parentheses |
| `IPedal` | Sustain pedal marking | type (start, stop, change), line, sign |
| `IOctaveShift` | 8va/15ma marking | type (up, down, stop), size (8, 15) |
| `IWords` | Text expression | value, defaultX, defaultY, fontSize, fontWeight, fontStyle |
| `ISound` | Playback parameters and navigation | tempo, dynamics, dacapo, segno, dalsegno, coda, tocoda, fine |

```java
for (IDirection direction : measure.getDirections()) {
    ISound sound = direction.getSound();
    if (sound != null) {
        String tempo    = sound.getTempo();    // BPM as string, e.g., "120"
        String dynamics = sound.getDynamics(); // MIDI velocity
    }

    for (IDirectionType dt : direction.getDirectionTypes()) {
        IMetronome metro = dt.getMetronome();
        if (metro != null) {
            String beatUnit = metro.getBeatUnit();  // "quarter", "half"
            String bpm      = metro.getPerMinute(); // "120"
        }

        IWedge wedge = dt.getWedge();
        if (wedge != null) {
            String type = wedge.getType(); // "crescendo", "diminuendo", "stop"
        }

        IWords words = dt.getWords();
        if (words != null) {
            String text = words.getText(); // "dolce", "rit.", etc.
        }
    }
}
```


## Package: model/barline

Barline styles, repeats, and endings.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IBarline` | Barline at measure boundary | location, barStyle, repeat, ending, fermata, segno, coda |
| `IRepeat` | Repeat barline | direction (forward, backward), times, winged |
| `IEnding` | Volta bracket (1st/2nd ending) | number, type (start, stop, discontinue), value |

```java
for (IBarline barline : measure.getBarlines()) {
    String style = barline.getBarStyle(); // "light-heavy", "heavy-light", etc.

    IRepeat repeat = barline.getRepeat();
    if (repeat != null) {
        String dir = repeat.getDirection(); // "forward" or "backward"
    }

    IEnding ending = barline.getEnding();
    if (ending != null) {
        String number = ending.getNumber(); // "1", "2", etc.
        String type   = ending.getType();   // "start", "stop", "discontinue"
    }
}
```


## Package: model/harmony

Chord symbols and analysis.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IHarmony` | Chord symbol | type, root, function, kind, inversion, bass, degrees, frame, staff |
| `IRoot` | Chord root pitch | rootStep, rootAlter |
| `IBass` | Bass note (for slash chords) | bassStep, bassAlter |
| `IDegree` | Chord degree alteration | degreeValue, degreeAlter, degreeType |
| `IFrame` | Guitar chord diagram | frameStrings, frameFrets |

```java
for (IHarmony harmony : measure.getHarmonies()) {
    IRoot  root = harmony.getRoot();
    String step  = root.getRootStep();  // "C", "D", etc.
    Double alter = root.getRootAlter(); // 1.0 = sharp
    String kind  = harmony.getKind();   // "major", "minor", "dominant", etc.

    IBass bass = harmony.getBass();
    if (bass != null) {
        String bassNote = bass.getBassStep(); // slash chord: C/E
    }
}
```


## Package: model/layout

Page and system layout.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IPageLayout` | Page dimensions | pageHeight, pageWidth, pageMargins |
| `IPageMargins` | Page margin values | type, leftMargin, rightMargin, topMargin, bottomMargin |
| `ISystemLayout` | System spacing | systemMargins, systemDistance, topSystemDistance |
| `ISystemMargins` | System margin values | leftMargin, rightMargin |
| `IStaffLayout` | Staff spacing | number, staffDistance |


## Package: model/common

Shared enums and marker interfaces.

| Type | Values |
|------|--------|
| `IMusicDataElement` | Marker interface for elements inside a measure (Note, Direction, Attributes, Barline, Harmony, Forward, Backup, Print, Sound); `IMeasure.getMusicData()` holds them in document order |
| `NoteTypeValue` | maxima, long, breve, whole, half, quarter, eighth, 16th, 32nd, 64th, 128th, 256th, 512th, 1024th |
| `ClefSign` | G, F, C, percussion, TAB, jianpu, none |
| `StemValue` | up, down, double, none |
| `AccidentalValue` | sharp, natural, flat, double-sharp, double-flat, etc. |
| `BeamValue` | begin, continue, end, forward hook, backward hook |
| `BarStyleValue` | regular, dotted, dashed, heavy, light-light, light-heavy, heavy-light, heavy-heavy, tick, short, none |
| `ModeValue` | major, minor, dorian, phrygian, lydian, mixolydian, aeolian, ionian, locrian, none |
| `WedgeType` | crescendo, diminuendo, stop, continue |
| `NoteheadValue` | slash, triangle, diamond, x, circle-x, normal, none, do, re, mi, fa, so, la, ti, etc. |
| `SyllabicValue` | single, begin, end, middle |
| `StartStop` | start, stop |
| `StartStopContinue` | start, stop, continue |
| `AboveBelow` | above, below |
| `UpDown` | up, down |
| `YesNo` | yes, no |
| `BackwardForward` | backward, forward |
| `LineType` | solid, dashed, dotted, wavy |
| `GroupSymbolValue` | none, brace, line, bracket, square |
| `GroupBarlineValue` | yes, no, Mensurstrich |
| `PlacementValue` | above, below |


## Package structure

```
com.github.toolarium.music
|-- MusicFileFactory               <- singleton facade (read/write with auto-detection)
|-- MusicFileFormat                <- supported file formats enum
|-- model/
|   |-- score/
|   |   |-- IScore, IWork, IIdentification, ICreator, ICredit, ...
|   |   +-- impl/  (ScoreImpl, WorkImpl, IdentificationImpl, ...)
|   |-- part/
|   |   |-- IPartList, IScorePart, IPartGroup, IScoreInstrument, ...
|   |   +-- impl/  (PartListImpl, ScorePartImpl, ...)
|   |-- measure/
|   |   |-- IMeasure, IPart, IAttributes, IKey, ITime, IClef, ...
|   |   +-- impl/  (MeasureImpl, PartImpl, AttributesImpl, ...)
|   |-- note/
|   |   |-- INote, IPitch, IRest, ITie, IBeam, ILyric, ...
|   |   +-- impl/  (NoteImpl, PitchImpl, RestImpl, ...)
|   |-- notation/
|   |   |-- INotations, ISlur, ITuplet, IArticulations, IDynamics, ...
|   |   +-- impl/  (NotationsImpl, SlurImpl, TupletImpl, ...)
|   |-- direction/
|   |   |-- IDirection, IDirectionType, ISound, IWords, IMetronome, ...
|   |   +-- impl/  (DirectionImpl, SoundImpl, MetronomeImpl, ...)
|   |-- barline/
|   |   |-- IBarline, IRepeat, IEnding
|   |   +-- impl/  (BarlineImpl, RepeatImpl, EndingImpl)
|   |-- harmony/
|   |   |-- IHarmony, IRoot, IBass, IDegree, IFrame
|   |   +-- impl/  (HarmonyImpl, RootImpl, BassImpl, ...)
|   |-- layout/
|   |   |-- IPageLayout, ISystemLayout, IStaffLayout, ...
|   |   +-- impl/  (PageLayoutImpl, SystemLayoutImpl, ...)
|   +-- common/
|       |-- IMusicDataElement       <- marker interface for measure content
|       +-- enums (NoteTypeValue, ClefSign, BarStyleValue, ...)
+-- io/
    |-- IMusicReader, IMusicWriter
    |-- dto/              (ScoreData, PartData, MeasureData, VoiceData, EventData, NoteData, Fraction, ...)
    |-- converter/        (ScoreBuilder, NoteValueConverter, PitchSpeller, ChordSymbolParser, DynamicsConverter)
    |-- stream/           (StreamContentReader, ZipContentReader)
    |-- xml/              (XmlDocumentReader)
    +-- format/
        |-- abc/          (AbcIOFactory; impl/ AbcReaderImpl, AbcTuneParser, AbcWriterImpl, dto/)
        |-- capella/      (CapellaIOFactory; impl/ CapellaReaderImpl, CapxScoreParser, CapBinaryReader, dto/)
        |-- dorico/       (DoricoIOFactory; impl/ DoricoReaderImpl, DoricoTreeDecoder, converters)
        |-- finale/       (FinaleIOFactory, FinaleContainerFactory; impl/ FinaleReaderImpl, EnigmaXmlDecryptor, ...)
        |-- guitarpro/    (GuitarProIOFactory; impl/ GpBinaryReader, GpxContainerReader, GpifReader, dto/)
        |-- lilypond/     (LilyPondIOFactory; impl/ LilyPondReaderImpl, LilyPondTokenizer, LilyPondParser, LilyPondWriterImpl, dto/)
        |-- mei/          (MeiIOFactory; impl/ MeiReaderImpl, MeiBodyParser, MeiWriterImpl, dto/)
        |-- midi/         (MidiIOFactory; impl/ MidiReaderImpl, MidiWriterImpl, converters)
        |-- musescore/    (MuseScoreIOFactory; impl/ MuseScoreReader, MuseScoreWriter, converters)
        |-- musicxml/impl/ (MusicXMLIOFactory, MusicXMLReaderImpl, MusicXMLWriterImpl)
        |-- mxl/impl/     (MXLIOFactory, MXLReaderImpl, MXLWriterImpl)
        +-- sibelius/     (SibeliusIOFactory; impl/ SibeliusCipher, SibeliusContainerReader, SibeliusReaderImpl)
```
