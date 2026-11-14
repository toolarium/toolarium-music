# Architecture: toolarium-music

Java music model library that provides a unified in-memory representation of musical scores
and supports reading and writing multiple file formats.

## Supported Formats

| Format | Extension | Read | Write | Description |
|--------|-----------|------|-------|-------------|
| MusicXML | `.musicxml`, `.xml` | Yes | Yes | Standard MusicXML 4.0 XML |
| MXL | `.mxl` | Yes | Yes | Compressed MusicXML (ZIP with META-INF/container.xml) |
| Finale | `.musx`, `.mus` | Yes | No | Finale container with metadata extraction |
| MIDI | `.mid`, `.midi` | Yes | Yes | Standard MIDI File (Type 0 and Type 1) via JDK `javax.sound.midi` |

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
Implementation classes use Jackson XML annotations for serialization.

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
+-- musicxml/impl/
|   +-- MusicXMLReaderImpl         (Jackson XML deserialization)
|   +-- MusicXMLWriterImpl         (Jackson XML serialization)
|   +-- MusicXMLIOFactory          (singleton: getReader(), getWriter())
+-- mxl/impl/
|   +-- MXLReaderImpl              (unzip, parse container.xml, delegate to MusicXMLReaderImpl)
|   +-- MXLWriterImpl              (create ZIP with META-INF/container.xml + .musicxml)
|   +-- MXLIOFactory               (singleton: getReader(), getWriter())
+-- midi/
|   +-- MidiIOFactory              (singleton: getReader(), getWriter())
|   +-- impl/
|       +-- MidiReaderImpl         (javax.sound.midi deserialization, Type 0/1 support)
|       +-- MidiWriterImpl         (javax.sound.midi serialization, produces Type 1)
|       +-- MidiNoteConverter      (MIDI note number to/from IPitch)
|       +-- MidiDurationConverter  (tick durations to/from NoteTypeValue with dot support)
+-- finale/
    +-- IFinaleContainer           (interface: container contents)
    +-- IFinaleContainerReader     (interface: read .musx/.mus)
    +-- IFinaleMetadata            (interface: extracted metadata)
    +-- FinaleContainerFactory     (singleton: getReader())
    +-- impl/
        +-- FinaleContainer, FinaleContainerReaderImpl, FinaleMetadata
```

### Layer 3: Facade (Main Entry Point)

```
com.github.toolarium.music
+-- MusicFileFactory               (singleton: read(), write() with auto-detection)
+-- MusicFileFormat                 (enum: MUSIC_XML, MXL, FINALE, MIDI)
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
    +-- musicxml/impl/ (MusicXMLIOFactory, MusicXMLReaderImpl, MusicXMLWriterImpl)
    +-- mxl/impl/ (MXLIOFactory, MXLReaderImpl, MXLWriterImpl)
    +-- midi/ (MidiIOFactory, MidiReaderImpl, MidiWriterImpl, converters)
    +-- finale/ (FinaleContainerFactory, IFinaleContainer, ...)
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
8. **Finale format support**: Reverse-engineered documentation in `docs/finale-musx-format.md`. The Finale reader extracts container metadata but does not convert to `IScore` (future work).
