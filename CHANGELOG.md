# toolarium-music

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 0.8.0 - 2026-11-11
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
