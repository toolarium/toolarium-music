# Architecture: toolarium-music

Java music model library that provides a unified in-memory representation of musical scores
and supports reading and writing multiple file formats.

## Supported Formats

| Format | Extension | Read | Write | Description |
|--------|-----------|------|-------|-------------|
| MusicXML | `.musicxml`, `.xml` | Yes | Yes | Standard MusicXML 4.0 XML |
| MXL | `.mxl` | Yes | Yes | Compressed MusicXML (ZIP with META-INF/container.xml) |
| Finale | `.musx`, `.mus` | Yes | No | Finale .musx with full score extraction (decrypts EnigmaXML) |
| MIDI | `.mid`, `.midi` | Yes | Yes | Standard MIDI File (Type 0 and Type 1) via JDK `javax.sound.midi` |
| MuseScore | `.mscz`, `.mscx` | Yes | Yes | MuseScore 1.x to 4.x (ZIP with XML); writes the MuseScore 4 format |
| Dorico | `.dorico` | Yes | No | ZIP with a binary tree (all flows) |
| Sibelius | `.sib`, `.sco`, `.si7` | Yes | No | Encrypted, compressed binary container |
| Guitar Pro | `.gp`, `.gpx`, `.gp5`, `.gp4`, `.gp3` | Yes | No | GP7/8 ZIP and GP6 BCFZ container with GPIF XML; GP3 to GP5 binary |
| capella | `.capx`, `.cap` | Yes | No | ZIP with CapXML (`score.xml`); legacy binary `.cap` converted into CapXML |
| MEI | `.mei` | Yes | Yes | Music Encoding Initiative XML (read MEI 3, 4 and 5, write MEI 5) |
| ABC | `.abc` | Yes | Yes | ABC notation 2.1 (text) |
| LilyPond | `.ly`, `.ily` | Yes | Yes | LilyPond text (own parser; writes LilyPond 2.24 source) |

The model is format-agnostic: regardless of input format, data is loaded into the same
`IScore` model. Format differences are handled in the I/O layer.


## Element Hierarchy

```
IScore
+-- version
+-- IWork (workTitle, workNumber)
+-- movementNumber, movementTitle
+-- IIdentification (creators, rights, encoding, source)
+-- IDefaults (IScaling, IPageLayout, ISystemLayout, IStaffLayout)
+-- List<ICredit> (creditWords)
+-- IPartList
|   +-- List<IScorePart> (id, partName, scoreInstruments, midiInstruments)
|   +-- List<IPartGroup> (groupName, groupSymbol, groupBarline)
+-- List<IPart>
    +-- List<IMeasure> (number, width)
        +-- List<IAttributes> (divisions, keys, times, clefs, transposes)
        +-- List<INote>
        |   +-- IPitch (step, alter, octave) / IRest / IUnpitched
        |   +-- duration, voice, type, stem, accidental
        |   +-- INotations (tied, slur, tuplet, dynamics, articulations,
        |   |              ornaments, playingTechnique, fermata, glissando)
        |   +-- List<ILyric> (syllabic, text)
        +-- List<IDirection> (IDirectionType: words, wedge, metronome, pedal, octaveShift)
        +-- List<IBarline> (barStyle, repeat, ending)
        +-- List<IHarmony> (root, kind, bass, degree, frame)
        +-- List<IForward>, List<IBackup> (multi-voice navigation)
        +-- List<IPrint> (page/system layout overrides)
        +-- List<ISound> (tempo, dynamics, dacapo, segno, coda, fine)
```


## Architecture Layers

### Layer 1: Model

The model classes live in `com.github.toolarium.music.model` and its subpackages.
Each subpackage has interfaces at the root and implementations in an `impl/` subpackage.
Implementation classes use Jackson XML annotations for serialization. The measure content is kept in
document order (`IMeasure.getMusicData()`), because in MusicXML the order of notes, backups, forwards,
attributes and directions defines the time position of each element; `MeasureSerializer` and
`MeasureDeserializer` read and write it in that order, and the typed getters (`getNotes()`, ...) are
read-only views (see `docs/MODEL.md`).

| Subpackage | Interfaces |
|------------|------------|
| `model/score/` | `IScore`, `IWork`, `IIdentification`, `IEncoding`, `ICreator`, `ICredit`, `ICreditWords`, `IDefaults`, `IScaling` |
| `model/part/` | `IPartList`, `IScorePart`, `IPartGroup`, `IScoreInstrument`, `IMidiDevice`, `IMidiInstrument` |
| `model/measure/` | `IMeasure`, `IPart`, `IAttributes`, `IKey`, `ITime`, `IClef`, `ITranspose`, `IStaffDetails`, `IPrint`, `IBackup`, `IForward` |
| `model/note/` | `INote`, `IPitch`, `IRest`, `IUnpitched`, `ITie`, `IBeam`, `ILyric`, `ITimeModification` |
| `model/notation/` | `INotations`, `ITied`, `ISlur`, `ITuplet`, `IArticulations`, `IDynamics`, `IOrnaments`, `IPlayingTechnique`, `IFermata`, `IGlissando` |
| `model/direction/` | `IDirection`, `IDirectionType`, `ISound`, `IWords`, `IMetronome`, `IWedge`, `IPedal`, `IOctaveShift` |
| `model/barline/` | `IBarline`, `IRepeat`, `IEnding` |
| `model/harmony/` | `IHarmony`, `IRoot`, `IBass`, `IDegree`, `IFrame` |
| `model/layout/` | `IPageLayout`, `IPageMargins`, `ISystemLayout`, `ISystemMargins`, `IStaffLayout` |
| `model/common/` | `IMusicDataElement` (marker), enums (`NoteTypeValue`, `ClefSign`, `BarStyleValue`, etc.) |

### Layer 2: Reader/Writer (I/O)

```
com.github.toolarium.music.io
+-- IMusicReader                   (interface: read music files)
+-- IMusicWriter                   (interface: write music files)
+-- dto/                           (format independent data passed from a reader to the ScoreBuilder)
|   +-- ScoreData, PartData, MeasureData, VoiceData, EventData, NoteData, DirectionData, ClefChange
|   +-- Fraction                   (immutable rational number for positions and durations)
|   +-- TypeAndDots, SpelledPitch
+-- converter/                     (singletons)
|   +-- ScoreBuilder               (builds IScore from the dto: divisions, gap rests, attributes,
|   |                               ties, slurs, tuplets, directions, barlines and endings)
|   +-- NoteValueConverter, PitchSpeller, ChordSymbolParser, DynamicsConverter
+-- xml/XmlDocumentReader          (secure, namespace independent XML parsing and navigation)
+-- stream/                        (StreamContentReader, ZipContentReader)
+-- format/
    +-- musicxml/impl/             (MusicXMLIOFactory, MusicXMLReaderImpl, MusicXMLWriterImpl)
    +-- mxl/impl/                  (MXLIOFactory, MXLReaderImpl, MXLWriterImpl)
    +-- midi/                      (MidiIOFactory; impl/ reader, writer, note and duration converters)
    +-- finale/                    (FinaleIOFactory, FinaleContainerFactory, container interfaces; impl/)
    +-- musescore/                 (MuseScoreIOFactory; impl/ readers, writers, converters)
    +-- dorico/                    (DoricoIOFactory; impl/ tree decoder, reader, converters)
    +-- sibelius/                  (SibeliusIOFactory; impl/ cipher, container, note extractor, reader)
    +-- guitarpro/                 (GuitarProIOFactory)
    |   +-- impl/                  (GuitarProReaderImpl, GpBinaryReader, GpxContainerReader, GpifReader,
    |       |                       GuitarProScoreAssembler, GuitarProConverter)
    |       +-- dto/               (GpSong, GpTrack, GpMasterBar, GpBar, GpBeat, GpNote, ...)
    +-- capella/                   (CapellaIOFactory; impl/ CapellaReaderImpl, CapxScoreParser, CapBinaryReader, CapBinaryInput,
    |                               CapRtfConverter, CapellaConverter, dto/)
    +-- mei/                       (MeiIOFactory; impl/ MeiReaderImpl, MeiBodyParser, MeiConverter, dto/)
    +-- abc/                       (AbcIOFactory; impl/ AbcReaderImpl, AbcTuneParser, AbcFieldParser,
                                    AbcDecorationConverter, dto/)
```

Each file format has its own package below `io/format/` with a factory at the package root and the
implementation in `impl/`. State objects of a reader are DTOs in `impl/dto/` (own classes with
accessors, `equals`, `hashCode` and `toString`).

The readers of Guitar Pro, capella, MEI and ABC share one pipeline: the format specific parser
collects the music into the DTOs of `io/dto` (parts, measures with time, key, clefs and barlines,
voices with events and notes), and `ScoreBuilder` converts them into the `IScore` model. The
builder calculates the divisions of each part, fills voices with invisible rests, writes attributes
only where they change and places directions at their offsets.

### Layer 3: Facade (Main Entry Point)

```
com.github.toolarium.music
+-- MusicFileFactory               (singleton: read(), write() with auto-detection)
+-- MusicFileFormat                 (enum: MUSIC_XML, MXL, FINALE, MIDI, MUSESCORE, MUSESCORE_XML,
                                     DORICO, SIBELIUS, GUITAR_PRO, CAPELLA, MEI, ABC, LILYPOND)
```

`MusicFileFactory` is the main entry point. It auto-detects the format by file extension
and delegates to the appropriate reader/writer.

Each factory follows the HOLDER pattern:

```java
public final class MusicFileFactory {

    private static final class HOLDER {
        static final MusicFileFactory INSTANCE = new MusicFileFactory();
    }

    private MusicFileFactory() {
        // private
    }

    public static MusicFileFactory getInstance() {
        return HOLDER.INSTANCE;
    }

    public IScore read(Path path) throws IOException {
        // auto-detect format, delegate to reader
    }

    public void write(IScore score, Path path) throws IOException {
        // auto-detect format, delegate to writer
    }
}
```


## Package Structure

```
com.github.toolarium.music
+-- MusicFileFactory                <- singleton facade (read/write with auto-detection)
+-- MusicFileFormat                 <- supported file formats enum
+-- model/
|   +-- score/
|   |   +-- IScore, IWork, IIdentification, ICreator, ICredit, ...
|   |   +-- impl/ (ScoreImpl, WorkImpl, IdentificationImpl, ...)
|   +-- part/
|   |   +-- IPartList, IScorePart, IPartGroup, IScoreInstrument, ...
|   |   +-- impl/ (PartListImpl, ScorePartImpl, ...)
|   +-- measure/
|   |   +-- IMeasure, IPart, IAttributes, IKey, ITime, IClef, ...
|   |   +-- impl/ (MeasureImpl, PartImpl, AttributesImpl, ...)
|   +-- note/
|   |   +-- INote, IPitch, IRest, ITie, IBeam, ILyric, ...
|   |   +-- impl/ (NoteImpl, PitchImpl, RestImpl, ...)
|   +-- notation/
|   |   +-- INotations, ISlur, ITuplet, IArticulations, IDynamics, IPlayingTechnique, ...
|   |   +-- impl/ (NotationsImpl, SlurImpl, PlayingTechniqueImpl, ...)
|   +-- direction/
|   |   +-- IDirection, IDirectionType, ISound, IWords, IMetronome, ...
|   |   +-- impl/ (DirectionImpl, SoundImpl, MetronomeImpl, ...)
|   +-- barline/
|   |   +-- IBarline, IRepeat, IEnding
|   |   +-- impl/ (BarlineImpl, RepeatImpl, EndingImpl)
|   +-- harmony/
|   |   +-- IHarmony, IRoot, IBass, IDegree, IFrame
|   |   +-- impl/ (HarmonyImpl, RootImpl, BassImpl, ...)
|   +-- layout/
|   |   +-- IPageLayout, ISystemLayout, IStaffLayout, ...
|   |   +-- impl/ (PageLayoutImpl, SystemLayoutImpl, ...)
|   +-- common/
|       +-- IMusicDataElement       <- marker interface for measure content
|       +-- enums (NoteTypeValue, ClefSign, BarStyleValue, ...)
+-- io/
    +-- IMusicReader, IMusicWriter
    +-- dto/ (ScoreData, PartData, MeasureData, VoiceData, EventData, NoteData, Fraction, ...)
    +-- converter/ (ScoreBuilder, NoteValueConverter, PitchSpeller, ChordSymbolParser, DynamicsConverter)
    +-- xml/ (XmlDocumentReader)
    +-- stream/ (StreamContentReader, ZipContentReader)
    +-- format/
        +-- musicxml/, mxl/, midi/, finale/, musescore/, dorico/, sibelius/
        +-- guitarpro/, capella/, mei/, abc/
```


## Dependencies

| Dependency | Scope | Purpose |
|------------|-------|---------|
| `com.fasterxml.jackson.dataformat:jackson-dataformat-xml` | implementation | Jackson XML serialization/deserialization |
| `com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations` | implementation | Jakarta XML Bind annotation support for Jackson |
| `org.slf4j:slf4j-api` | implementation | Logging |


## MXL Container Structure

```
myfile.mxl (ZIP archive)
+-- META-INF/
|   +-- container.xml       (required: points to the root .musicxml document)
+-- score.musicxml          (the main MusicXML file)
+-- [optional resources]    (images, media)
```

**container.xml** format:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="score.musicxml"
              media-type="application/vnd.recordare.musicxml+xml"/>
  </rootfiles>
</container>
```


## Target API Usage

```java
// Read any format (auto-detect by extension)
IScore score = MusicFileFactory.getInstance().read(Path.of("score.mxl"));

// Access data
String title = score.getMovementTitle();
List<? extends IScorePart> parts = score.getPartList().getScoreParts();
List<? extends IMeasure> measures = score.getParts().get(0).getMeasures();

// Write back
MusicFileFactory.getInstance().write(score, Path.of("output.musicxml"));
MusicFileFactory.getInstance().write(score, Path.of("output.mxl"));

// Or specify format explicitly
MusicFileFactory.getInstance().write(score, outputStream, MusicFileFormat.MUSIC_XML);
```


## Design Decisions

1. **Format-agnostic model**: A single `IScore` interface represents all scores regardless of input format. No partwise/timewise distinction in the model.
2. **Jackson XML over JAXB**: Lightweight, widely adopted, no heavy runtime like Glassfish or MOXy.
3. **Hand-coded model over XSD code generation**: Full control over class design, allows enforcing project conventions (interfaces, `Impl` suffix, `hashCode`/`equals`/`toString`).
4. **MXL as first-class citizen**: The facade handles both plain and compressed formats transparently.
5. **Interface-first design**: All public API is defined through `I`-prefixed interfaces. Implementations live in `impl/` subpackages with `Impl` suffix.
6. **Singleton factory pattern**: Each `impl/` package has a factory with HOLDER pattern (`getInstance()`, `get*()` methods). HOLDER class is placed before the private constructor.
7. **No `{@inheritDoc}`**: Every method has proper, self-contained Javadoc.
8. **Finale format support**: Fully reverse-engineered; see `docs/finale-musx-format.md`. The Finale reader decrypts score.dat (LCG-XOR cipher), decompresses the gzip envelope, parses the resulting EnigmaXML, and builds a complete `IScore` model with notes, pitches, durations, keys, time signatures, clefs, and multi-staff support.
9. **Ordered measure content**: A measure keeps its content in one list in document order instead of one list per element type. Separate lists cannot express where a backup or a direction sits between notes: Jackson replaced a list whenever another element interrupted it (notes were lost on reading), and the writer emitted all notes before the backups (voices were misplaced on writing).
10. **Further formats**: Candidate formats and their priority (MuseScore, Guitar Pro, capella, MEI, ABC) are evaluated in `docs/notation-formats-overview.md`.
