[![License](https://img.shields.io/github/license/toolarium/toolarium-music)](https://github.com/toolarium/toolarium-music/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.toolarium/toolarium-music/0.9.0)](https://search.maven.org/artifact/com.github.toolarium/toolarium-music/0.9.0/jar)
[![javadoc](https://javadoc.io/badge2/com.github.toolarium/toolarium-music/javadoc.svg)](https://javadoc.io/doc/com.github.toolarium/toolarium-music)

# toolarium-music

Java music model library that provides a unified in-memory representation of musical scores
and supports reading and writing multiple file formats:

| Format | Type | Extension | Read | Write |
|--------|------|-----------|------|-------|
| [ABC notation](https://abcnotation.com/wiki/abc:standard:v2.1) | Open standard | `.abc` | ✓ | ✓ |
| [Capella](https://www.capella-software.com) | Commercial | `.capx`, `.cap` | ✓ |  |
| [Compressed MXL](https://github.com/w3c/musicxml) | Open standard | `.mxl` | ✓ | ✓ |
| [Dorico](https://www.steinberg.net/dorico/) | Commercial | `.dorico` | ✓ |  |
| [Finale](https://www.finalemusic.com) | Commercial | `.musx`, `.mus` | ✓ |  |
| [Guitar Pro](https://www.guitar-pro.com) | Commercial | `.gp`, `.gpx`, `.gp5`, `.gp4`, `.gp3` | ✓ |  |
| [LilyPond](https://gitlab.com/lilypond/lilypond) | Open source | `.ly`, `.ily` | ✓ | ✓ |
| [MEI](https://github.com/music-encoding/music-encoding) | Open standard | `.mei` | ✓ | ✓ |
| [MIDI](https://midi.org/standard-midi-files-specification) | Open standard | `.mid`, `.midi` | ✓ | ✓ |
| [MuseScore](https://github.com/musescore/MuseScore) | Open source | `.mscz`, `.mscx` | ✓ | ✓ |
| [MusicXML](https://github.com/w3c/musicxml) | Open standard | `.musicxml`, `.xml` | ✓ | ✓ |
| [Sibelius](https://www.avid.com/sibelius) | Commercial | `.sib`, `.sco`, `.si7` | ✓ |  |

The model is format-agnostic. Regardless of the input format, data is loaded into the same
`IScore` model. The library handles format detection and conversion transparently.


## Command-Line Tool

Download the self-contained executable JAR (classifier `cli`) from
[Maven Central](https://search.maven.org/artifact/com.github.toolarium/toolarium-music/0.9.0/jar).
No installation required; Java 11 or later is the only dependency.

```bash
java -jar toolarium-music-0.9.0-cli.jar [options] FILE|DIR...
```

| Option | Short | Default | Description |
|--------|-------|---------|-------------|
| `--format` | `-f` | `mxl` | Output format |
| `--output` | `-o` | current directory | Output directory |
| `--help` | `-h` | | Show help and exit |
| `--version` | `-V` | | Print version and exit |

Supported output formats: `mxl`, `musicxml`, `mid`, `mscz`, `mscx`, `mei`, `abc`, `ly`.

### Examples

```bash
# Convert a Sibelius file to MXL (default output format)
java -jar toolarium-music-0.9.0-cli.jar song.sib

# Convert all files in a directory to MIDI, write to /tmp/output
java -jar toolarium-music-0.9.0-cli.jar -f mid -o /tmp/output scores/

# Convert multiple files to MusicXML
java -jar toolarium-music-0.9.0-cli.jar -f musicxml piece.gp5 score.musx arrangement.dorico
```

Files inside a directory are found recursively. All input files are processed in parallel using
all available CPU cores. The exit code is 0 on full success and 1 if any conversion failed.


## Library

Add the library to your project:

**Gradle**
```gradle
implementation 'com.github.toolarium:toolarium-music:0.9.0'
```

**Maven**
```xml
<dependency>
    <groupId>com.github.toolarium</groupId>
    <artifactId>toolarium-music</artifactId>
    <version>0.9.0</version>
</dependency>
```

The library brings `jackson-dataformat-xml` and `slf4j-api` as transitive dependencies.
You must provide an SLF4J binding (e.g., Logback) at runtime.

### Building a score

`ScoreBuilder` creates an `IScore` programmatically without reading a file.
The fluent API mirrors the score hierarchy: score, part, measure, note.

```java
IScore score = ScoreBuilder.builder()
    .title("Ode to Joy")
    .composer("Beethoven")
    .part("Violin")
        .measure(1)
            .time("4", "4").key(0, "major").clef("G", 2)
            .note("E", 4, "quarter")
            .note("E", 4, "quarter")
            .note("F", 4, "quarter")
            .note("G", 4, "quarter")
        .end()
    .end()
    .build();

MusicFileFactory.getInstance().write(score, Path.of("output.mxl"));
```

Accidentals, dotted notes, rests and multi-voice content are supported:

```java
.note("F", 4, 1.0, "eighth")        // F-sharp eighth
.note("G", 4, "eighth", 1)          // dotted G eighth
.rest("quarter")                     // quarter rest
.beginNote("C", 4, "quarter").voice(2).end()          // voice 2
.beginNote("E", 4, "quarter").voice(2).chord().end()  // chord note
```

Durations are computed automatically from the note type (8 ticks per quarter note).
The completed score can be passed directly to `MusicFileFactory.getInstance().write()`.


### Reading a score

The format is detected by the file extension; every format is read into the same `IScore` model.

```java
IScore score = MusicFileFactory.getInstance().read(Path.of("song.gp5"));

String title = score.getMovementTitle();
List<? extends IScorePart> instruments = score.getPartList().getScoreParts();
List<? extends IPart> parts = score.getParts();
```

### Writing and converting a score

MusicXML, MXL, MIDI, MuseScore, MEI, ABC and LilyPond can be written.
Reading one format and writing another converts it.

```java
IScore score = MusicFileFactory.getInstance().read(Path.of("score.sib"));
MusicFileFactory.getInstance().write(score, Path.of("score.musicxml"));
MusicFileFactory.getInstance().write(score, Path.of("score.mscz"));
```

### Using streams

With streams the format must be given explicitly.

```java
IScore score = MusicFileFactory.getInstance().read(inputStream, MusicFileFormat.CAPELLA);
MusicFileFactory.getInstance().write(score, outputStream, MusicFileFormat.MXL);
```

### Using a format factory directly

Each format has a singleton factory in `com.github.toolarium.music.io.format.<format>`:

```java
IMusicReader reader = MidiIOFactory.getInstance().getReader();
IScore score = reader.read(Path.of("song.mid"));

IMusicWriter writer = MidiIOFactory.getInstance().getWriter();
writer.write(score, Path.of("copy.mid"));
```

| Format | `MusicFileFormat` | Factory | Reader | Writer |
|--------|-------------------|---------|--------|--------|
| ABC notation | `ABC` | `format.abc.AbcIOFactory` | `getReader()` | `getWriter()` |
| capella | `CAPELLA` | `format.capella.CapellaIOFactory` | `getReader()` | |
| Dorico | `DORICO` | `format.dorico.DoricoIOFactory` | `getReader()` | |
| Finale | `FINALE` | `format.finale.FinaleIOFactory` | `getReader()` | |
| Guitar Pro | `GUITAR_PRO` | `format.guitarpro.GuitarProIOFactory` | `getReader()` | |
| LilyPond | `LILYPOND` | `format.lilypond.LilyPondIOFactory` | `getReader()` | `getWriter()` |
| MEI | `MEI` | `format.mei.MeiIOFactory` | `getReader()` | `getWriter()` |
| MIDI | `MIDI` | `format.midi.MidiIOFactory` | `getReader()` | `getWriter()` |
| MuseScore | `MUSESCORE`, `MUSESCORE_XML` | `format.musescore.MuseScoreIOFactory` | `getReader()`, `getMscxReader()` | `getWriter()`, `getMscxWriter()` |
| MusicXML | `MUSIC_XML` | `format.musicxml.impl.MusicXMLIOFactory` | `getReader()` | `getWriter()` |
| MXL | `MXL` | `format.mxl.impl.MXLIOFactory` | `getReader()` | `getWriter()` |
| Sibelius | `SIBELIUS` | `format.sibelius.SibeliusIOFactory` | `getReader()` | |

What each reader converts and its known limitations are described in the format documentation
(see [Documentation](#documentation)).

### Accessing a Finale container

The Finale container reader gives access to the metadata of a `.musx` file without converting the score.

```java
IFinaleContainer container = FinaleContainerFactory.getInstance().getReader().read(Path.of("score.musx"));
IFinaleMetadata metadata = container.getMetadata();
String key = metadata.getKeySignature();
Double tempo = metadata.getInitialTempo();
```

### Errors and thread safety

A file that cannot be read (damaged, invalid, too large or of an unsupported format) is reported with an
`IOException`. The readers limit the size of the input and of packed content, so a damaged file cannot
exhaust the memory.

`MusicFileFactory`, the format factories, the readers and the writers keep no state between calls and can
be used by several threads at the same time. The model classes (`ScoreImpl`, `MeasureImpl`, ...) are
mutable and not synchronized: a score can be read by several threads, but must not be changed while
another thread uses it.


## Model

The library provides a complete Java model for musical scores organized into domain packages
(`score`, `part`, `measure`, `note`, `notation`, `direction`, `barline`, `harmony`, `layout`, `common`).
Every type is an interface (prefixed with `I`); implementations live in `impl/` subpackages.

See [docs/MODEL.md](docs/MODEL.md) for the full reference with field lists, element hierarchy,
code examples, and package structure.


## Building

```bash
cb
```

Requires Java 11+. The build runs unit tests on the sample files in `src/test/resources/samples`.
Tests that read many samples process them in parallel; the thread count defaults to 1.5 per processor
and can be overridden:

```bash
cb -PtestThreads=16
```

### Integration tests

The integration tests run on all samples including the local protected ones in
`src/test/resources/samples/_protected_` (copyrighted real-world scores, not part of the repository):

```bash
cb integrationTest
```

A test whose sample type only exists in the protected folder is skipped if no sample is found.


## Documentation

- [Architecture](docs/ARCHITECTURE.md): design decisions, package structure, implementation phases
- [Model Reference](docs/MODEL.md): complete list of all model interfaces, element hierarchy, code examples and package structure
- [Notation Formats Overview](docs/formats/notation-formats-overview.md): survey of notation tools and file formats, their status, popularity and support priority

### File Formats

- [ABC Format](docs/formats/abc-format.md): supported ABC notation `.abc` syntax, its mapping and the ABC writer
- [Capella Format](docs/formats/capella-format.md): capella `.capx` (CapXML) and reverse-engineered legacy binary `.cap` format documentation
- [Dorico Format](docs/formats/dorico-format.md): reverse-engineered Dorico `.dorico` format documentation
- [Finale Format](docs/formats/finale-musx-format.md): reverse-engineered Finale `.musx`/`.mus` format documentation
- [Guitar Pro Format](docs/formats/guitarpro-format.md): Guitar Pro `.gp3`/`.gp4`/`.gp5` binary, `.gpx` container and `.gp` (GPIF) format documentation
- [LilyPond Format](docs/formats/lilypond-format.md): supported LilyPond `.ly` syntax, its mapping and the LilyPond writer
- [MEI Format](docs/formats/mei-format.md): supported MEI `.mei` elements, their mapping and the MEI writer
- [MIDI Format](docs/formats/midi-format.md): Standard MIDI File `.mid`/`.midi` reading and writing
- [MuseScore Format](docs/formats/musescore-format.md): MuseScore `.mscz`/`.mscx` format documentation (versions 1.x to 4.x)
- [MusicXML and MXL Format](docs/formats/musicxml-format.md): MusicXML `.musicxml`/`.xml` and compressed `.mxl` files
- [Sibelius Format](docs/formats/sibelius-sib-format.md): reverse-engineered Sibelius `.sib`/`.sco` format documentation


## Built With

* [cb](https://github.com/toolarium/common-build): the toolarium common build


## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the
[tags on this repository](https://github.com/toolarium/toolarium-music/tags).


## License

This project is licensed under the MPL-2.0 License, see the [LICENSE](LICENSE) file for details.

| Action | Allowed | Required | Not Allowed |
|---|---|---|---|
| Commercial use | ✓ | | |
| Private use | ✓ | | |
| Modify source | ✓ | | |
| Distribute | ✓ | | |
| Mix with proprietary code | ✓ | | |
| Share modified MPL files | | ✓ | |
| Keep license notices | | ✓ | |
| Remove copyright notices | | | ✓ |
| Hold authors liable | | | ✓ |
| Use authors' trademarks | | | ✓ |
