# Architecture: toolarium-music

Java music model library that provides a unified in-memory representation of musical scores
and supports reading and writing multiple file formats.

For the complete list of model interfaces, field names, element hierarchy and code examples
see [MODEL.md](MODEL.md). For quick-start API usage see the [README](../README.md).


## Supported Formats

| Format | Extension | Read | Write | Container / encoding |
|--------|-----------|------|-------|----------------------|
| [ABC](formats/abc-format.md) | `.abc` | ✓ | ✓ | ABC notation 2.1 plain text |
| [capella](formats/capella-format.md) | `.capx`, `.cap` | ✓ |  | ZIP with CapXML (`score.xml`); legacy binary `.cap` converted to CapXML |
| [Dorico](formats/dorico-format.md) | `.dorico` | ✓ |  | ZIP with a proprietary binary tree (all flows) |
| [Finale](formats/finale-musx-format.md) | `.musx`, `.mus` | ✓ |  | ZIP; decrypts EnigmaXML from `score.dat` |
| [Guitar Pro](formats/guitarpro-format.md) | `.gp`, `.gpx`, `.gp5`, `.gp4`, `.gp3` | ✓ |  | GP7/8 ZIP; GP6 BCFZ with GPIF XML; GP3-GP5 binary |
| [LilyPond](formats/lilypond-format.md) | `.ly`, `.ily` | ✓ | ✓ | LilyPond 2.24 plain text |
| [MEI](formats/mei-format.md) | `.mei` | ✓ | ✓ | XML (read MEI 3, 4 and 5; write MEI 5) |
| [MIDI](formats/midi-format.md) | `.mid`, `.midi` | ✓ | ✓ | Standard MIDI File (Type 0 and 1) via JDK `javax.sound.midi` |
| [MuseScore](formats/musescore-format.md) | `.mscz`, `.mscx` | ✓ | ✓ | ZIP with XML (1.x to 4.x); writes MuseScore 4 |
| [MusicXML](formats/musicxml-format.md) | `.musicxml`, `.xml` | ✓ | ✓ | Plain XML (MusicXML 4.0) |
| [MXL](formats/musicxml-format.md) | `.mxl` | ✓ | ✓ | ZIP with `META-INF/container.xml` |
| [Sibelius](formats/sibelius-sib-format.md) | `.sib`, `.sco`, `.si7` | ✓ |  | Encrypted, zlib-compressed binary container |

The model is format-agnostic: regardless of input format, data is loaded into the same
`IScore` model. Format differences are handled entirely in the I/O layer.


## Architecture Layers

### Layer 1: Model

The model classes live in `com.github.toolarium.music.model` and its subpackages.
Each subpackage has interfaces at the root and implementations in an `impl/` subpackage.
Implementation classes use Jackson XML annotations for serialization.

The measure content is kept in document order (`IMeasure.getMusicData()`), because in MusicXML
the order of notes, backups, forwards, attributes and directions defines the time position of
each element. `MeasureSerializer` and `MeasureDeserializer` read and write it in that order;
the typed getters (`getNotes()`, `getBackups()`, ...) are read-only views.

For the full list of subpackages, interfaces and field names see [MODEL.md](MODEL.md).

### Layer 2: Reader/Writer (I/O)

```
com.github.toolarium.music.io
|-- IMusicReader                   (interface: read a music file into IScore)
|-- IMusicWriter                   (interface: write an IScore to a file)
|-- dto/                           (format-independent DTOs passed from a reader to ScoreBuilder)
|   |-- ScoreData, PartData, MeasureData, VoiceData, EventData, NoteData, DirectionData, ClefChange
|   |-- Fraction                   (immutable rational number for positions and durations)
|   +-- TypeAndDots, SpelledPitch
|-- converter/                     (singletons)
|   |-- ScoreBuilder               (builds IScore from DTOs: divisions, gap rests, attributes,
|   |                               ties, slurs, tuplets, directions, barlines and endings)
|   +-- NoteValueConverter, PitchSpeller, ChordSymbolParser, DynamicsConverter
|-- xml/XmlDocumentReader          (secure, namespace-independent XML parsing and navigation)
|-- stream/                        (StreamContentReader, ZipContentReader)
+-- format/
    |-- musicxml/impl/             (MusicXMLIOFactory, MusicXMLReaderImpl, MusicXMLWriterImpl)
    |-- mxl/impl/                  (MXLIOFactory, MXLReaderImpl, MXLWriterImpl)
    |-- midi/                      (MidiIOFactory; impl/ reader, writer, note and duration converters)
    |-- finale/                    (FinaleIOFactory, FinaleContainerFactory; impl/)
    |-- musescore/                 (MuseScoreIOFactory; impl/ readers, writers, converters)
    |-- dorico/                    (DoricoIOFactory; impl/ tree decoder, reader, converters)
    |-- sibelius/                  (SibeliusIOFactory; impl/ cipher, container, note extractor, reader)
    |-- guitarpro/                 (GuitarProIOFactory; impl/ binary/container/GPIF readers, assembler, dto/)
    |-- capella/                   (CapellaIOFactory; impl/ CapellaReaderImpl, CapxScoreParser,
    |                               CapBinaryReader, CapRtfConverter, dto/)
    |-- mei/                       (MeiIOFactory; impl/ MeiReaderImpl, MeiBodyParser, MeiConverter,
    |                               MeiWriterImpl, dto/)
    |-- abc/                       (AbcIOFactory; impl/ AbcReaderImpl, AbcTuneParser, AbcFieldParser,
    |                               AbcWriterImpl, dto/)
    +-- lilypond/                  (LilyPondIOFactory; impl/ LilyPondReaderImpl, LilyPondTokenizer,
                                    LilyPondParser, LilyPondWriterImpl, dto/)
```

Each format has its own package below `io/format/` with a factory at the package root and the
implementation in `impl/`. Reader state objects are DTOs in `impl/dto/` (own classes with
accessors, `equals`, `hashCode` and `toString`).

The readers for Guitar Pro, capella, MEI, ABC and LilyPond share one pipeline: the format-specific
parser fills the `io/dto` objects (parts, measures with time/key/clef/barlines, voices with events
and notes), and `ScoreBuilder` converts them into the `IScore` model. The builder calculates
divisions, fills voices with invisible rests, writes attributes only where they change, and places
directions at their time offsets.

### Layer 3: Facade

```
com.github.toolarium.music
|-- MusicFileFactory   (singleton: read() / write() with auto-detection by file extension)
+-- MusicFileFormat    (enum: MUSIC_XML, MXL, FINALE, MIDI, MUSESCORE, MUSESCORE_XML,
                        DORICO, SIBELIUS, GUITAR_PRO, CAPELLA, MEI, ABC, LILYPOND)
```

`MusicFileFactory` is the main entry point. It detects the format from the file extension and
delegates to the appropriate reader or writer. Each factory follows the HOLDER singleton pattern:

```java
public final class MusicFileFactory {

    private static final class HOLDER {
        static final MusicFileFactory INSTANCE = new MusicFileFactory();
    }

    private MusicFileFactory() { }

    public static MusicFileFactory getInstance() {
        return HOLDER.INSTANCE;
    }

    public IScore read(Path path) throws IOException { ... }

    public void write(IScore score, Path path) throws IOException { ... }
}
```


## Dependencies

| Dependency | Scope | Purpose |
|------------|-------|---------|
| `com.fasterxml.jackson.dataformat:jackson-dataformat-xml` | implementation | XML serialization/deserialization for the model |
| `com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations` | implementation | Jakarta XML Bind annotation support for Jackson |
| `org.slf4j:slf4j-api` | implementation | Logging API |
| `info.picocli:picocli` | cli only | Command-line argument parsing (not in the library POM) |


## MXL Container Structure

```
myfile.mxl  (ZIP)
|-- META-INF/
|   +-- container.xml       (required: points to the root .musicxml file)
+-- score.musicxml
+-- [optional resources]    (images, media)
```

`container.xml` format:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="score.musicxml"
              media-type="application/vnd.recordare.musicxml+xml"/>
  </rootfiles>
</container>
```


## Design Decisions

1. **Format-agnostic model**: a single `IScore` interface represents all scores regardless of input
   format. No partwise/timewise distinction in the model.
2. **Jackson XML over JAXB**: lightweight, widely adopted, no heavy runtime like Glassfish or MOXy.
3. **Hand-coded model over XSD code generation**: full control over class design, allows enforcing
   project conventions (interfaces, `Impl` suffix, `hashCode`/`equals`/`toString`).
4. **MXL as first-class citizen**: the facade handles both plain and compressed formats transparently.
5. **Interface-first design**: all public API is defined through `I`-prefixed interfaces.
   Implementations live in `impl/` subpackages with `Impl` suffix.
6. **Singleton factory pattern**: each format package has a factory with HOLDER pattern
   (`getInstance()`, `get*()` methods). HOLDER class is placed before the private constructor.
7. **No `{@inheritDoc}`**: every method has proper, self-contained Javadoc.
8. **Ordered measure content**: a measure keeps its content in one ordered list rather than one list
   per element type. Separate typed lists cannot express where a backup or direction sits between
   notes: Jackson replaced a list whenever another element interrupted it (notes were lost on
   reading), and the writer emitted all notes before the backups (voices were misplaced on writing).
9. **Finale format**: fully reverse-engineered; see
   [formats/finale-musx-format.md](formats/finale-musx-format.md). The reader decrypts `score.dat`
   (LCG-XOR cipher), decompresses the gzip envelope, parses the resulting EnigmaXML, and builds a
   complete `IScore` model.
10. **Further formats**: candidate formats and their priority are evaluated in
    [formats/notation-formats-overview.md](formats/notation-formats-overview.md).
