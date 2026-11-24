# capella .capx / .cap File Format

capella is a product of capella-software AG; "capella" is a trademark of its owner.

> **Disclaimer:** This document describes the formats as observed in score files and as needed for
> interoperability. It is not an official specification. The binary layout was reconstructed from
> sample files and verified against the import results of MuseScore (the only other open
> implementation, GPLv3). toolarium contains no MuseScore source code.


## Overview

| Extension | Content | Saved by |
|-----------|---------|----------|
| `.capx` | ZIP container with `score.xml` (CapXML 1.0 or 2.0), or a plain CapXML document | capella 7 and later |
| `.cap` | Binary file with signature `cap3-v:` | capella 2000 to capella 2008 (and later versions on request) |

Both formats hold the same music model: the score is stored **system by system** (a system is one
line of music). Each system has staves, each staff up to four voices, and each voice a sequence of
note objects. Measures are not stored: barlines are placed automatically by the time signature,
and an explicit barline object can end a measure earlier.

toolarium converts a `.cap` file into an in-memory CapXML document, so both formats share one score
parser (`CapxScoreParser`).


## CapXML (.capx)

```
score                              (namespace http://www.capella.de/CapXML/1.0 or 2.0)
+-- info                           title, author, comment, encodingSoftware
+-- layout
|   +-- staves/staffLayout         description (the key of a staff layout)
|   |   +-- notation               defaultClef
|   |   +-- instrument             name, abbrev (attributes or child elements), transpose
|   |   +-- sound                  instr (General MIDI program, 0-based), volume
|   +-- brackets/bracket           from, to (staff layout indices), curly (piano brace)
+-- systems/system                 tempo (quarter notes per minute)
    +-- staves/staff               layout (staff layout description), defaultTime
        +-- voices/voice/noteObjects
            +-- clefSign           clef: treble, bass, alto, tenor, soprano, mezzoSoprano, baritone,
            |                      percussion, or G2, F4, C3 with + (8va) or - (8vb)
            +-- keySign            fifths
            +-- timeSign           time: 4/4, C, allaBreve, infinite
            +-- chord / rest
            |   +-- duration       base (1/4, 1/8, 2/1 ...), dots, noDuration (grace note)
            |   |   +-- tuplet     count, tripartite, prolong
            |   +-- display        invisible (rests)
            |   +-- stem           dir
            |   +-- articulation   type: staccato, tenuto, staccTenuto, staccatissimo,
            |   |                  normalAccent, strongAccent, weakBeat, strongBeat
            |   +-- lyric/verse    i (verse index), hyphen, extender, text
            |   +-- drawObjects/drawObj
            |   |   +-- basic      noteRange (number of following notes a spanner covers)
            |   |   +-- slur, wedge (decresc), volta (firstNumber, lastNumber), trill,
            |   |       text (x, y, font height, content), bracket (tuplet bracket)
            |   +-- heads/head     pitch (C5 = middle C), shape
            |       +-- alter      step (absolute alteration), display (force)
            |       +-- tie        begin, end
            +-- barline            type: single, double, end, repEnd, repStart, repEndStart, dashed
```

### Pitch

A head pitch is a note name with an octave where **octave 5 is the octave of middle C** (`C5` is
MIDI 60). The `alter` element gives the absolute alteration in semitones (it is not relative to the
key signature).

### Tuplets

A tuplet of `count` notes is played in the time of the largest power of two below `count`
(3 in 2, 5 in 4, 9 in 8). A `tripartite` tuplet uses the largest multiple of three times a power of
two below `count` instead (4 in 3, 8 in 6, 14 in 12). A `prolong` tuplet uses the next value
above `count` (2 in 3). A tuplet group lasts the first note value times the normal notes; notes
without tuplet information belong to an open group until it is complete.

### Measures

A voice fills a measure until the length of the current time signature is reached; then a new
measure starts. A `barline` object ends a measure earlier (for example a pickup) or only sets the
style of the measure that just ended. The end of a system also ends a measure. A time signature is
valid for all staves.


## Binary format (.cap)

### Primitives

| Type | Encoding |
|------|----------|
| byte | 1 byte |
| unsigned | 1 byte; `254` is followed by a 16 bit value, `255` by a 32 bit value |
| int | 1 signed byte; `-128` is followed by a 16 bit value, `127` by a 32 bit value |
| word, dword | 16 and 32 bit little-endian |
| string | unsigned length followed by ISO-8859-1 characters |
| color | 1 byte index into the standard colors; `255` is followed by red, green and blue |
| font | unsigned index of a font read before (1-based); `0` is followed by a new font: 5 dwords (height in 1/1000 point, width, escapement, orientation, weight), 8 bytes (italic, underline, strike-out, charset, precisions, quality, pitch and family), color and face name |
| extra | byte count followed by that many bytes (reserved for extensions, skipped) |

### File layout

```
"cap3-v:" + version letter            8 bytes
author, keywords, comment             strings
spacing relative, absolute            unsigned, unsigned (75, 16)
flags                                 byte
layout
    small and normal line distance    int, int (1/100 of a staff space: 120, 180)
    top and inter-system distance     int, int
    name alignment, vertical adjust   byte, byte
    flags, system separators          byte, byte
    unnamed counter                   int
    instrument name font              font
    staff layouts                     unsigned count, staff layout (see below)
    brackets                          unsigned count, (from int, to int, curly byte)
beam flattening                       4 bytes (49, 29, 148, 55)
extra
gallery                               drawing object array, unsigned count of names, strings
background chord                      chord (holds page objects such as title texts)
bar numbers                           4 bytes (show, frame, distance x, distance y), font
first page, page margins              5 unsigned (margins in 1/100 mm)
systems                               unsigned count, system (see below)
end                                   4 zero bytes (followed by one byte)
```

### Staff layout

```
barline mode, note lines              byte, byte (1 one line, 2 five lines, else 11 more bytes)
small                                 byte
top, bottom, group distance           int, int, int
barline from, to                      byte, byte
clef                                  byte: form bits 0-2 (0 G, 1 C, 2 F, 3 percussion),
                                            line bits 3-5 (0 = fifth line ... 4 = first line),
                                            octave bits 6-7 (0 up, 1 none, 2 down)
percussion flags                      byte: 1 percussion channel, 2 and 4 sound maps
                                      (each map: first note byte, count byte, count bytes)
sound, volume, transposition          int, int, int (sound = General MIDI program, 0-based)
description, name, abbreviation,
intermediate name, intermediate abbr. strings
```

### System, staff and voice

```
system   'S', additional bar count int, bar count reset byte, left indent byte, beam mode byte,
         tempo unsigned, color, extra, flags byte, staves (unsigned count)
staff    'V', time numerator byte, time denominator byte (log2 + 1; bit 7 alla breve),
         staff layout index byte, top and bottom distance int, color, extra, voices (unsigned count)
voice    'C', lyrics position byte, verse distance byte, lyrics font, stem direction byte, extra,
         note objects (unsigned count)
```

A note object starts with its type byte and an extra block. Clef, key, meter and barline objects
then have a color.

| Type | Object | Content |
|------|--------|---------|
| 0 | rest | duration, flags byte (1 multi-measure rest with unsigned count, 2 centered, 4 vertical shift int) |
| 1 | chord | duration, chord data, notes |
| 2 | clef | clef byte (as in the staff layout) |
| 3 | key | byte: fifths + 7 |
| 4 | meter | numerator byte, denominator byte (as in the staff) |
| 5, 6 | explicit and implicit barline | byte: type bits 0-3 (0 single, 1 double, 2 end, 3 repeat end, 4 repeat start, 5 repeat end and start, 6 dashed) |
| 7 | page background | drawing object array |

### Duration

```
flags     byte: dots bits 0-1, 4 grace note (no duration), 8 grace note after,
          16 small, 32 invisible, 64 color follows
value     byte: note value bits 0-3 (0 whole, 1 half, 2 quarter ... 8 256th, 9 breve),
          16 horizontal shift int follows, 32 tuplet byte follows, 64 drawing objects follow
tuplet    byte: count bits 0-3, 16 tripartite, 32 prolong
```

### Chord and notes

```
flags     byte: 1 beam mode byte, 2 notation staff byte, 4 stem direction and stem length,
          8 tremolo bars and articulation bytes, 16 tie byte (1 tie from the previous chord,
          2 tie to the next chord), 32 beam shift and slope, 64 lyrics
lyrics    unsigned verse count; per verse a byte (0 = empty), flags byte (1 left aligned,
          2 extender, 4 hyphen, 8 verse number string follows, 16 text string follows)
notes     unsigned count; per note a pitch byte and a flags byte
```

Articulation values: 1 staccato, 2 tenuto, 3 staccato with tenuto, 4 staccatissimo, 5 accent,
6 strong accent, 7 weak beat, 8 strong beat.

**Pitch.** The pitch byte is a signed diatonic step relative to the **tonic of the key signature**.
For a treble clef, step 0 is the tonic between C5 and B5 in MIDI terms (MIDI 72 + 7 × fifths modulo
12): C5 in C major, G5 in G major, A5 in A major, C♭6 in C♭ major. The C clef shifts the result one
octave down, the F clef two octaves; the octave bits of the clef shift it once more. If bits 7 and 6
of the pitch byte differ, the accidental is shown and bit 7 is inverted. The note flags hold the
notehead (bits 0-2), the alteration **relative to the key signature** (bits 3-5, value minus 2, so
`0x10` is the key alteration, `0x08` one semitone lower, `0x18` one semitone higher), the forced
accidental (bit 6) and silent (bit 7).

Example: `fifths = 1` (G major), pitch byte `-15`, flags `0x08`. Step -15 from G5 is F3; the key
makes it F♯3, the relative alteration -1 makes it F3 natural (MIDI 53).

### Drawing objects

An array starts with an unsigned count; each object starts with its type byte and a header:
anchor mode x, anchor mode y, distance y, flags (bytes), reference note (int) and a range word
(bits 0-7: number of following notes the object covers, bits 8-10 page range, bit 12 background).

| Type | Object | Body after the header |
|------|--------|-----------------------|
| 0 | group | point, drawing object array |
| 1 | transposable | point, byte, drawing object array |
| 2 | metafile | unsigned size, bytes |
| 3 | simple text | point, alignment byte, font, string |
| 4 | text | point, width int, ratio int, unsigned size, RTF text |
| 5 | rectangle or ellipse | line, radius int, filled byte (color if filled) |
| 6 | line | two points, color, width byte |
| 7 | polygon | unsigned count of points, filled byte, width byte, color |
| 8 | wavy line | line, wave length byte, adapt byte |
| 9 | slur | 4 points (Bezier curve), color, 4 bytes (end, middle, dot distance, dot width) |
| 10 | note lines | x0, x1, y ints, color, lines byte (11 more bytes unless 1 or 2) |
| 11 | wedge (hairpin) | line, byte (height bits 0-6, bit 7 diminuendo) |
| 12 | volta | x0, x1, y ints, color, flags byte, numbers byte (first bits 0-3, last bits 4-7) |
| 13 | bracket (tuplet) | line, orientation byte, number byte |
| 14 | guitar frame | color, word, dword (strings) |
| 15 | trill | x0, x1, y ints, color, trill sign byte |

A point is two ints (x, y).


## Mapping to the toolarium-music Model

| capella | Model |
|---------|-------|
| staff layout | part (name, abbreviation, MIDI program, transposition); staves joined by a curly bracket form one part with several staves |
| system tempo | metronome direction with sound tempo (when it changes) |
| clef, key, time | attributes of the measure where they appear (time signatures apply to all parts) |
| chord, rest | note and rest with type, dots and time modification; invisible rests keep `print-object="no"`; a whole rest at the start of a measure of another length is a measure rest; a multi-measure rest becomes one measure rest per measure |
| grace note (no duration) | grace note with slash |
| tie begin and end | tie and tied notations |
| slur, wedge (note range) | slur notations and wedge directions from the chord to the chord the range ends on |
| volta | ending of the measure |
| text | words direction; large texts (16 points and more) at the start of the score become title and subtitle |
| lyrics | lyrics with syllabic from the hyphen flags and extension |
| articulation, trill | articulations and trill mark |

Not converted: layout (page and system distances, beams, stem lengths), guitar frames, graphics
(lines, polygons, rectangles, metafiles), percussion sound maps.
