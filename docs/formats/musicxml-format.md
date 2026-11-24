# MusicXML and MXL File Formats (.musicxml, .xml, .mxl)

MusicXML is an open standard for music notation, maintained by the W3C Music Notation Community
Group. The specification is published at [w3.org/2021/06/musicxml40](https://www.w3.org/2021/06/musicxml40/).

> **Note:** MusicXML is the reference model of toolarium: the classes of the `model` package
> correspond to the MusicXML 4.0 elements (see [MODEL.md](MODEL.md)). This document describes the
> files and how they are read and written.


## Formats

| Extension | Content |
|-----------|---------|
| `.musicxml`, `.xml` | Uncompressed MusicXML, root element `score-partwise` |
| `.mxl` | Compressed MusicXML: a ZIP container |

### MXL container

```
mimetype                       application/vnd.recordare.musicxml (written first, uncompressed)
META-INF/container.xml         <container><rootfiles><rootfile full-path="score.musicxml"/></rootfiles></container>
score.musicxml                 the score (any name, given by the root file)
```

The reader takes the first `rootfile` of `META-INF/container.xml`; without a container it uses the
first `.musicxml` or `.xml` entry. The writer creates the mimetype entry, the container and the
score entry.


## Score structure

toolarium reads and writes the partwise form (`score-partwise`): parts contain measures. The
timewise form (`score-timewise`) is not supported.

```
score-partwise                 version
+-- work, movement-number, movement-title
+-- identification             creator (type), rights, encoding (software, encoding-date), source
+-- defaults, credit           layout and credits
+-- part-list                  part-group, score-part (part-name, score-instrument, midi-instrument)
+-- part (id)
    +-- measure (number, implicit)
        +-- attributes         divisions, key, time, staves, clef, transpose, staff-details
        +-- note               pitch / rest / unpitched, duration, voice, type, dot, accidental,
        |                      time-modification, stem, notehead, staff, beam, notations, lyric
        +-- backup, forward    move the position for the next voice
        +-- direction          direction-type (words, dynamics, wedge, metronome, pedal,
        |                      octave-shift, rehearsal, segno, coda), offset, staff, sound
        +-- harmony            chord symbols
        +-- barline            bar-style, repeat, ending
        +-- print, sound
```

### Durations

`divisions` in the attributes gives the number of duration units per quarter note; every `duration`
and `backup`, `forward` and `offset` value is a multiple of it. toolarium readers calculate the
divisions of a part as the least common multiple of all note lengths (up to 16 777 216).

### Pitch

`pitch` is the sounding pitch for non-transposing instruments; for transposing instruments the
written pitch, with `transpose` (chromatic, diatonic, octave-change) giving sounding minus written.


## Reading and writing

The model classes carry Jackson XML annotations; reading and writing are Jackson XML
deserialization and serialization. Unset elements are not written. The DOCTYPE of MusicXML files is
not resolved (no network access, no external entities).

Known limitation: the model keeps notes, backups, directions and the other measure elements in
separate lists, so the original order of the elements inside a measure is not kept when a file is
written again.
