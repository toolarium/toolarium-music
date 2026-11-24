# toolarium-music

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [ 0.9.0 ] - 2026-11-24
### Added
- `ToolariumMusic` command-line entry point: reads one or more files or directories and converts them to a target format in parallel, using all available CPU cores. Accepts `--format` (`-f`, default: `mxl`) and `--output` (`-o`, default: current directory) options, plus `--help` and `--version`.
- `ScoreBuilder` fluent builder API (`com.github.toolarium.music.model.builder`): creates an `IScore` programmatically without reading a file. Nested `PartBuilder`, `MeasureBuilder` and `NoteBuilder` cover metadata (title, composer, work title), key and time signatures, clefs, pitched notes with optional accidentals and augmentation dots, rests, and multi-voice or chord notes.


- MuseScore `.mscz` and `.mscx` reading (versions 1.x to 4.x) and writing (MuseScore 4 format), including the title page, the header and footer of the pages, the instruments, fingerings and cue-size notes.
- MuseScore format documentation in `docs/formats/musescore-format.md`.
- Full Finale `.musx` and `.mus` score reading with conversion to `IScore`.
- Finale format documentation in `docs/formats/finale-musx-format.md`.
- Dorico `.dorico` project reading with notes and notation of all flows, including pick-up bars; parts are in the order of the full score.
- Dorico format documentation in `docs/formats/dorico-format.md`.
- Sibelius `.sib`, `.sco`, and `.si7` reading for all Sibelius versions (1 to 2025) and Scorch files: metadata, instruments, notes with pitches and durations, ties, slurs, tuplets, articulations, fermatas, dynamics, hairpins, texts, tempo, and lyrics.
- Sibelius format documentation in `docs/formats/sibelius-sib-format.md`.
- Guitar Pro reading: `.gp3`, `.gp4`, `.gp5`, `.gpx` (Guitar Pro 6) and `.gp` (Guitar Pro 7 and 8).
- capella reading: `.capx` (CapXML) and the legacy binary `.cap` format.
- MEI `.mei` reading (MEI 3, 4 and 5) and writing (MEI 5, valid against the MEI 5.0 schema).
- ABC notation `.abc` reading and writing (ABC 2.1).
- LilyPond `.ly` reading (first score of a file) and writing (LilyPond 2.24 and later).
- `MusicFileFormat` values `GUITAR_PRO`, `CAPELLA`, `MEI`, `ABC` and `LILYPOND` with detection by file extension.
- Format documentation for every supported format in `docs/`, linked from the README.
- Gradle task `integrationTest` (`cb integrationTest`): runs the tests on all samples, including the local protected samples; the regular build tests only the tracked samples.

### Changed
- `IMeasure` keeps the measure content in document order (`getMusicData()`); the typed getters such as `getNotes()` return read-only lists of one element type.
- All format packages moved to `io/format/<format>` (for example `io.format.midi.MidiIOFactory`); code that uses `MusicFileFactory` is not affected. Test classes follow the same structure.
- Reading a damaged or invalid file is reported with an `IOException` in every format; a damaged MIDI file can no longer exhaust the memory.


## [ 0.8.0 ] - 2026-11-11
### Added
- Complete Java model for musical scores (IScore, IPart, IMeasure, INote, IPitch, etc.).
- MusicXML 4.0 reader and writer (plain `.musicxml` format).
- MXL reader and writer (compressed `.mxl` ZIP archives).
- Finale `.musx` and `.mus` container reader with metadata extraction.
- MIDI reader and writer (`.mid`, `.midi` files) using JDK `javax.sound.midi`.
- `MusicFileFactory` singleton facade with auto-detection by file extension.
- `MusicFileFormat` enum supporting MUSIC_XML, MXL, FINALE, and MIDI formats.
- `IMusicReader` and `IMusicWriter` interfaces for format-specific I/O.
- Model organized by domain: score, part, measure, note, notation, direction, barline, harmony, layout, common.
