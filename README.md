[![License](https://img.shields.io/github/license/toolarium/toolarium-music)](https://github.com/toolarium/toolarium-music/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.toolarium/toolarium-music/0.8.0)](https://search.maven.org/artifact/com.github.toolarium/toolarium-music/0.8.0/jar)
[![javadoc](https://javadoc.io/badge2/com.github.toolarium/toolarium-music/javadoc.svg)](https://javadoc.io/doc/com.github.toolarium/toolarium-music)

# toolarium-music

Java music model library that provides a unified in-memory representation of musical scores
and supports reading and writing multiple file formats:

| Format | Extension | Read | Write |
|--------|-----------|------|-------|
| MusicXML | `.musicxml`, `.xml` | Yes | Yes |
| Compressed MXL | `.mxl` | Yes | Yes |
| Finale | `.musx`, `.mus` | Yes (container + metadata) | No |
| MIDI | `.mid`, `.midi` | Yes | Yes |

The model is format-agnostic. Regardless of the input format, data is loaded into the same
`IScore` model. The library handles format detection and conversion transparently.


## Features

- Complete Java model for musical scores (notes, measures, parts, dynamics, harmony, layout)
- Read and write MusicXML 4.0 and compressed MXL archives
- Read and write Standard MIDI Files (Type 0 and Type 1) using JDK `javax.sound.midi`
- Read Finale `.musx` (ZIP container) and `.mus` (Enigma binary) files
- Interface-first API: all public types are interfaces, implementations are pluggable
- Jackson XML serialization (lightweight, no JAXB/Glassfish/MOXy dependency)
- Single `MusicFileFactory` entry point with auto-detection by file extension


## Quick Start

### Reading a score (auto-detects format)

```java
IScore score = MusicFileFactory.getInstance().read(Path.of("score.musicxml"));

String title = score.getMovementTitle();
IPartList partList = score.getPartList();
List<? extends IPart> parts = score.getParts();
```

### Writing a score (auto-detects format)

```java
// Auto-detects by extension
MusicFileFactory.getInstance().write(score, Path.of("output.musicxml"));
MusicFileFactory.getInstance().write(score, Path.of("output.mxl"));

// Or specify format explicitly
MusicFileFactory.getInstance().write(score, outputStream, MusicFileFormat.MUSIC_XML);
MusicFileFactory.getInstance().write(score, outputStream, MusicFileFormat.MXL);
```

### Using format-specific readers directly

```java
// MusicXML
IMusicReader reader = MusicXMLIOFactory.getInstance().getReader();
IScore score = reader.read(Path.of("score.musicxml"));

IMusicWriter writer = MusicXMLIOFactory.getInstance().getWriter();
writer.write(score, Path.of("output.musicxml"));
```

### Reading and writing MIDI files

```java
// Via the facade (auto-detects by extension)
IScore score = MusicFileFactory.getInstance().read(Path.of("song.mid"));
MusicFileFactory.getInstance().write(score, Path.of("output.mid"));

// Or use the MIDI factory directly
IMusicReader reader = MidiIOFactory.getInstance().getReader();
IScore score = reader.read(Path.of("song.mid"));

IMusicWriter writer = MidiIOFactory.getInstance().getWriter();
writer.write(score, Path.of("output.mid"));
```

### Reading a Finale file

```java
IFinaleContainerReader reader = FinaleContainerFactory.getInstance().getReader();
IFinaleContainer container = reader.read(Path.of("score.musx"));

IFinaleMetadata metadata = container.getMetadata();
String key = metadata.getKeySignature();
Double tempo = metadata.getInitialTempo();
```


## Model

The library provides a complete Java model for musical scores. Every model type is defined
as an interface (prefixed with `I`). Implementations live in `impl/` subpackages and
use Jackson XML annotations for serialization.

### Package overview

| Package | Interfaces | Description |
|---------|------------|-------------|
| `model/score` | IScore, IWork, IIdentification, IEncoding, ICreator, ICredit, ICreditWords, IDefaults, IScaling | Top-level score structure, metadata, credits |
| `model/part` | IPartList, IScorePart, IPartGroup, IScoreInstrument, IMidiDevice, IMidiInstrument | Part definitions, instruments, MIDI mappings |
| `model/measure` | IMeasure, IPart, IAttributes, IKey, ITime, IClef, ITranspose, IStaffDetails, IPrint, IBackup, IForward | Measures, musical attributes, navigation |
| `model/note` | INote, IPitch, IRest, IUnpitched, ITie, IBeam, ILyric, ITimeModification | Notes, pitches, rests, lyrics |
| `model/notation` | INotations, ITied, ISlur, ITuplet, IFermata, IArticulations, IDynamics, IOrnaments, IPlayingTechnique, IGlissando | Performance and notation markings |
| `model/direction` | IDirection, IDirectionType, ISound, IWords, IMetronome, IWedge, IPedal, IOctaveShift | Expressive directions, tempo, dynamics |
| `model/barline` | IBarline, IRepeat, IEnding | Barlines, repeats, volta brackets |
| `model/harmony` | IHarmony, IRoot, IBass, IDegree, IFrame | Chord symbols, guitar frames |
| `model/layout` | IPageLayout, IPageMargins, ISystemLayout, ISystemMargins, IStaffLayout | Page and system layout |
| `model/common` | IMusicDataElement + 20 enums | Shared marker interface and enums |

### Element hierarchy

```
IScore
|-- version : String
|-- IWork (workTitle, workNumber)
|-- movementNumber : String
|-- movementTitle : String
|-- IIdentification (creators, rights, encoding, source)
|-- IDefaults (IScaling, IPageLayout, ISystemLayout, IStaffLayout)
|-- List<ICredit> (creditWords)
|-- IPartList
|   |-- List<IPartGroup> (groupName, groupSymbol, groupBarline)
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

### Score structure (model/score)

The top-level container for a musical score.

| Interface | Fields | Description |
|-----------|--------|-------------|
| `IScore` | version, work, movementNumber, movementTitle, identification, defaults, credits, partList, parts | The root score object |
| `IWork` | workNumber, workTitle | Composition identification |
| `IIdentification` | creators, rights, encoding, source | Creator and rights metadata |
| `ICreator` | type, value | A single creator (type: "composer", "lyricist", etc.) |
| `IEncoding` | software, encodingDate, supports | Software and encoding details |
| `IDefaults` | scaling, pageLayout, systemLayout, staffLayout | Default layout settings |
| `IScaling` | millimeters, tenths | Millimeter-to-tenths ratio |
| `ICredit` | page, creditWords | Page credits (title, composer, etc.) |
| `ICreditWords` | text, fontSize, fontWeight, justify, defaultX, defaultY, valign | Formatted credit text |

```java
// Traversing score metadata
IScore score = MusicFileFactory.getInstance().read(path);

String title = score.getMovementTitle();
IWork work = score.getWork();
if (work != null) {
    String workTitle = work.getWorkTitle();
}

IIdentification id = score.getIdentification();
if (id != null) {
    for (ICreator creator : id.getCreators()) {
        // creator.getType() = "composer", "lyricist", ...
        // creator.getValue() = "Johann Sebastian Bach"
    }
}
```

### Part definitions (model/part)

Defines the instruments in a score.

| Interface | Fields | Description |
|-----------|--------|-------------|
| `IPartList` | partGroups, scoreParts | Ordered list of parts and groups |
| `IScorePart` | id, partName, partAbbreviation, scoreInstruments, midiDevices, midiInstruments | A single instrument part |
| `IPartGroup` | type, number, groupName, groupSymbol, groupBarline | Groups parts (e.g., orchestral sections) |
| `IScoreInstrument` | id, instrumentName, instrumentAbbreviation, instrumentSound | Instrument definition |
| `IMidiInstrument` | id, midiChannel, midiProgram, midiBank, midiName, volume, pan | MIDI playback mapping |
| `IMidiDevice` | port, id | MIDI device port assignment |

```java
// Iterating parts and instruments
IPartList partList = score.getPartList();
for (IScorePart scorePart : partList.getScoreParts()) {
    String partName = scorePart.getPartName();      // "Piano", "Violin I"
    String partId = scorePart.getId();               // "P1", "P2"

    for (IMidiInstrument midi : scorePart.getMidiInstruments()) {
        Integer program = midi.getMidiProgram();      // General MIDI program number
        Integer channel = midi.getMidiChannel();
    }
}
```

### Measures and attributes (model/measure)

The core musical content containers.

| Interface | Fields | Description |
|-----------|--------|-------------|
| `IMeasure` | number, width, implicit, attributes, notes, directions, barlines, harmonies, forwards, backups, prints, sounds | A single measure with all its content |
| `IPart` | id, measures | Container of measures for one part |
| `IAttributes` | divisions, keys, times, staves, clefs, transposes, staffDetails, instruments | Musical attributes (key, time, clef, etc.) |
| `IKey` | fifths, mode, cancel | Key signature (fifths: -7 to 7; mode: "major", "minor", etc.) |
| `ITime` | beats, beatType, symbol | Time signature (beats/beatType, e.g., "4"/"4") |
| `IClef` | sign, line, number, clefOctaveChange | Clef (sign: G/F/C/percussion/TAB) |
| `ITranspose` | diatonic, chromatic, octaveChange, doubling | Transposition for transposing instruments |
| `IStaffDetails` | staffLines, staffTuning | Staff configuration |
| `IPrint` | newSystem, newPage | System and page break hints |
| `IBackup` | duration | Navigate backward in time (multi-voice) |
| `IForward` | duration | Navigate forward in time (multi-voice) |

```java
// Walking through measures
for (IPart part : score.getParts()) {
    for (IMeasure measure : part.getMeasures()) {
        String measureNumber = measure.getNumber();  // "1", "2", ...

        // Read attributes (key, time, clef) from first measure
        for (IAttributes attr : measure.getAttributes()) {
            Integer divisions = attr.getDivisions();

            for (IKey key : attr.getKeys()) {
                Integer fifths = key.getFifths();    // 0=C, 1=G, -1=F, ...
                String mode = key.getMode();          // "major", "minor"
            }

            for (ITime time : attr.getTimes()) {
                String beats = time.getBeats();       // "4"
                String beatType = time.getBeatType();  // "4"
            }

            for (IClef clef : attr.getClefs()) {
                String sign = clef.getSign();         // "G", "F", "C"
                Integer line = clef.getLine();        // 2 for treble, 4 for bass
            }
        }
    }
}
```

### Notes and pitches (model/note)

The fundamental musical events.

| Interface | Fields | Description |
|-----------|--------|-------------|
| `INote` | pitch, rest, unpitched, chord, duration, ties, voice, type, dots, accidental, timeModification, stem, notehead, staff, beams, notations, lyrics, defaultX, defaultY, printObject | A single note, rest, or chord member |
| `IPitch` | step, alter, octave | Pitch: step (A-G), alter (-2.0 to 2.0), octave (0-9) |
| `IRest` | displayStep, displayOctave | A rest (optional display position) |
| `IUnpitched` | displayStep, displayOctave | Unpitched note (percussion) |
| `ITie` | type | Tie start/stop |
| `IBeam` | value, number | Beam grouping |
| `ILyric` | number, syllabic, text, extend, elision, placement | Lyrics attached to a note |
| `ITimeModification` | actualNotes, normalNotes, normalType | Tuplet timing (e.g., 3 in the time of 2) |

```java
// Reading notes from a measure
for (INote note : measure.getNotes()) {
    if (note.getRest() != null) {
        // This is a rest
        continue;
    }

    IPitch pitch = note.getPitch();
    if (pitch != null) {
        String step = pitch.getStep();         // "C", "D", "E", ...
        Integer octave = pitch.getOctave();    // 4 = middle C octave
        Double alter = pitch.getAlter();       // 1.0 = sharp, -1.0 = flat
    }

    Integer duration = note.getDuration();      // In divisions
    String type = note.getType();               // "quarter", "eighth", "half", ...
    String stem = note.getStem();               // "up", "down"
    Integer voice = note.getVoice();            // Voice number for multi-voice

    // Chord detection: non-null chord means this note is part of a chord
    boolean isChord = note.getChord() != null;

    // Lyrics
    for (ILyric lyric : note.getLyrics()) {
        String syllabic = lyric.getSyllabic();  // "single", "begin", "middle", "end"
        String text = lyric.getText();
    }
}
```

### Notations (model/notation)

Performance markings and articulations attached to notes.

| Interface | Fields | Description |
|-----------|--------|-------------|
| `INotations` | tieds, slurs, tuplets, fermatas, articulations, dynamics, ornaments, technical, glissandos | Container for all notation markings |
| `ITied` | type, number, lineType, placement | Tied note connection |
| `ISlur` | type, number, lineType, placement | Slur (legato arc) |
| `ITuplet` | type, number, bracket, showNumber, showType | Tuplet bracket |
| `IFermata` | type | Fermata (hold) |
| `IArticulations` | staccato, accent, tenuto, staccatissimo, marcato, detachedLegato, strongAccent | Articulation marks |
| `IDynamics` | dynamicMarking | Dynamic level (pp, p, mp, mf, f, ff, etc.) |
| `IOrnaments` | trillMark, mordent, invertedMordent, turn, tremolo | Ornamental figures |
| `IPlayingTechnique` | upBow, downBow, harmonic, openString, fingering, pluck, string, fret | Instrument-specific techniques |
| `IGlissando` | type, number, lineType, text | Glissando/portamento |

```java
// Checking notations on a note
INotations notations = note.getNotations();
if (notations != null) {
    for (ISlur slur : notations.getSlurs()) {
        String type = slur.getType();       // "start", "stop"
    }

    IArticulations art = notations.getArticulations();
    if (art != null) {
        String staccato = art.getStaccato();  // non-null means staccato present
    }

    IDynamics dyn = notations.getDynamics();
    if (dyn != null) {
        String marking = dyn.getDynamicMarking();  // "f", "pp", "mf", ...
    }
}
```

### Directions (model/direction)

Expressive and tempo markings that apply to a point in time, not to a specific note.

| Interface | Fields | Description |
|-----------|--------|-------------|
| `IDirection` | placement, directionTypes, offset, voice, staff, sound | Direction container |
| `IDirectionType` | words, wedge, metronome, pedal, octaveShift, rehearsal, segno, coda, bracket, dynamics | The specific direction content |
| `ISound` | tempo, dynamics, dacapo, segno, dalsegno, coda, tocoda, fine, forwardRepeat | Playback instructions |
| `IWords` | text, fontSize, fontWeight, fontStyle, placement, defaultX, defaultY | Text directions (e.g., "dolce", "rit.") |
| `IMetronome` | beatUnit, perMinute, beatUnitDot | Metronome marking (e.g., quarter = 120) |
| `IWedge` | type, niente, number, spread | Crescendo/diminuendo hairpins |
| `IPedal` | type, line | Sustain pedal markings |
| `IOctaveShift` | type, number, size | 8va/8vb/15ma octave transposition |

```java
// Reading tempo and direction markings
for (IDirection direction : measure.getDirections()) {
    ISound sound = direction.getSound();
    if (sound != null) {
        String tempo = sound.getTempo();          // BPM as string, e.g., "120"
        String dynamics = sound.getDynamics();     // MIDI velocity
    }

    for (IDirectionType dt : direction.getDirectionTypes()) {
        IMetronome metro = dt.getMetronome();
        if (metro != null) {
            String beatUnit = metro.getBeatUnit();    // "quarter", "half"
            String bpm = metro.getPerMinute();        // "120"
        }

        IWedge wedge = dt.getWedge();
        if (wedge != null) {
            String type = wedge.getType();            // "crescendo", "diminuendo", "stop"
        }

        IWords words = dt.getWords();
        if (words != null) {
            String text = words.getText();            // "dolce", "rit.", etc.
        }
    }
}
```

### Barlines and repeats (model/barline)

Barline styles, repeat signs, and volta brackets.

| Interface | Fields | Description |
|-----------|--------|-------------|
| `IBarline` | location, barStyle, repeat, ending, fermata, segno, coda | Barline with optional repeat/ending |
| `IRepeat` | direction, winged | Repeat sign (direction: "forward", "backward") |
| `IEnding` | type, number, endingNumber, printObject | Volta bracket (1st/2nd ending) |

```java
// Detecting repeat barlines
for (IBarline barline : measure.getBarlines()) {
    String style = barline.getBarStyle();   // "light-heavy", "heavy-light", etc.

    IRepeat repeat = barline.getRepeat();
    if (repeat != null) {
        String dir = repeat.getDirection();  // "forward" or "backward"
    }

    IEnding ending = barline.getEnding();
    if (ending != null) {
        String number = ending.getNumber();  // "1", "2", etc.
        String type = ending.getType();      // "start", "stop", "discontinue"
    }
}
```

### Harmony (model/harmony)

Chord symbols and guitar chord diagrams.

| Interface | Fields | Description |
|-----------|--------|-------------|
| `IHarmony` | type, printFrame, root, function, kind, inversion, bass, degrees, frame, staff | Chord symbol |
| `IRoot` | rootStep, rootAlter | Root note of chord (step + alteration) |
| `IBass` | bassStep, bassAlter | Bass note for slash chords |
| `IDegree` | degreeValue, degreeAlter, degreeType | Chord degree modification (add, alter, subtract) |
| `IFrame` | frameStrings, frameFrets, fretList | Guitar chord diagram |

```java
// Reading chord symbols
for (IHarmony harmony : measure.getHarmonies()) {
    IRoot root = harmony.getRoot();
    String step = root.getRootStep();        // "C", "D", etc.
    Double alter = root.getRootAlter();      // 1.0 = sharp
    String kind = harmony.getKind();          // "major", "minor", "dominant", etc.

    IBass bass = harmony.getBass();
    if (bass != null) {
        String bassNote = bass.getBassStep(); // Slash chord: C/E
    }
}
```

### Layout (model/layout)

Page and system layout settings (typically in IDefaults or IPrint overrides).

| Interface | Fields | Description |
|-----------|--------|-------------|
| `IPageLayout` | pageHeight, pageWidth, pageMargins | Page dimensions |
| `IPageMargins` | marginValue, marginType | Page margins |
| `ISystemLayout` | systemDistance, topSystemDistance, systemMargins | System spacing |
| `ISystemMargins` | leftMargin, rightMargin | System margins |
| `IStaffLayout` | staffDistance, staffSize | Staff spacing and size |

### Common types (model/common)

Shared enums used across the model.

| Enum | Values | Usage |
|------|--------|-------|
| `NoteTypeValue` | WHOLE, HALF, QUARTER, EIGHTH, SIXTEENTH, ... | Note duration type |
| `ClefSign` | G, F, C, PERCUSSION, TAB | Clef sign |
| `BarStyleValue` | REGULAR, LIGHT_LIGHT, LIGHT_HEAVY, HEAVY_LIGHT, HEAVY_HEAVY, DOTTED, DASHED, TICK, SHORT, NONE | Barline style |
| `ModeValue` | MAJOR, MINOR, DORIAN, PHRYGIAN, LYDIAN, MIXOLYDIAN, AEOLIAN, LOCRIAN | Key mode |
| `StemValue` | UP, DOWN, DOUBLE, NONE | Stem direction |
| `AccidentalValue` | SHARP, NATURAL, FLAT, DOUBLE_SHARP, FLAT_FLAT, QUARTER_FLAT, QUARTER_SHARP, ... | Accidental types |
| `BeamValue` | BEGIN, CONTINUE, END, FORWARD_HOOK, BACKWARD_HOOK | Beam state |
| `StartStop` | START, STOP | Start/stop events |
| `StartStopContinue` | START, STOP, CONTINUE | Extended start/stop |
| `AboveBelow` | ABOVE, BELOW | Placement |
| `BackwardForward` | BACKWARD, FORWARD | Repeat direction |
| `UpDown` | UP, DOWN | Vertical direction |
| `WedgeType` | CRESCENDO, DIMINUENDO, STOP | Wedge (hairpin) type |
| `SyllabicValue` | SINGLE, BEGIN, END, MIDDLE | Lyric syllable position |
| `YesNo` | YES, NO | Boolean attribute |
| `GroupSymbolValue` | NONE, BRACE, BRACKET, LINE | Part group symbol |
| `GroupBarlineValue` | YES, NO | Part group barline |
| `LineType` | SOLID, DASHED, DOTTED, WAVY | Line rendering |
| `NoteheadValue` | SLASH, TRIANGLE, DIAMOND, SQUARE, CROSS, X, CIRCLE_X, ... | Notehead shape |
| `PlacementValue` | ABOVE, BELOW | Element placement |

### Package structure

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
    |-- musicxml/impl/  (MusicXMLIOFactory, MusicXMLReaderImpl, MusicXMLWriterImpl)
    |-- mxl/impl/       (MXLIOFactory, MXLReaderImpl, MXLWriterImpl)
    |-- midi/            (MidiIOFactory, MidiReaderImpl, MidiWriterImpl, converters)
    +-- finale/          (FinaleContainerFactory, IFinaleContainer, ...)
```


## Building

```bash
# Set up the build environment
source cb --setenv   # Linux/Git Bash
cb --setenv          # Windows

# Build
cb
```

Requires Java 11+.


## Adding the library

**Gradle**
```gradle
implementation 'com.github.toolarium:toolarium-music:0.8.0'
```

**Maven**
```xml
<dependency>
    <groupId>com.github.toolarium</groupId>
    <artifactId>toolarium-music</artifactId>
    <version>0.8.0</version>
</dependency>
```

The library brings `jackson-dataformat-xml` and `slf4j-api` as transitive dependencies. You must provide an SLF4J binding (e.g., Logback) at runtime.


## Documentation

- [Architecture](ARCHITECTURE.md): design decisions, package structure, implementation phases
- [Model Reference](docs/MODEL.md): complete list of all model interfaces and their fields
- [Finale Format](docs/finale-musx-format.md): reverse-engineered Finale .musx/.mus format documentation


## Built With

* [cb](https://github.com/toolarium/common-build) - The toolarium common build


## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/toolarium/toolarium-music/tags).


## License

This project is licensed under the MPL-2.0 License, see the [LICENSE](LICENSE) file for details.

| Action | Allowed | Required | Not Allowed |
|---|---|---|---|
| Commercial use | Yes | | |
| Private use | Yes | | |
| Modify source | Yes | | |
| Distribute | Yes | | |
| Mix with proprietary code | Yes | | |
| Share modified MPL files | | Yes | |
| Keep license notices | | Yes | |
| Remove copyright notices | | | Yes |
| Hold authors liable | | | Yes |
| Use authors' trademarks | | | Yes |
