# Guitar Pro File Formats (.gp3, .gp4, .gp5, .gpx, .gp)

Guitar Pro is a product of Arobas Music; "Guitar Pro" is a trademark of its owner.

> **Disclaimer:** This document describes the formats as observed in score files and as needed for
> interoperability. It is not an official specification. The formats are widely documented by open
> projects (alphaTab, PyGuitarPro, TuxGuitar); toolarium contains none of their source code. The
> sample files used for testing come from alphaTab (MPL-2.0).


## Overview

| Extension | Version | Container | Score |
|-----------|---------|-----------|-------|
| `.gp3` | Guitar Pro 3 | none | binary |
| `.gp4` | Guitar Pro 4 | none | binary |
| `.gp5` | Guitar Pro 5 | none | binary |
| `.gpx` | Guitar Pro 6 | BCFZ (compressed) or BCFS file system | `score.gpif` (XML) |
| `.gp` | Guitar Pro 7 and 8 | ZIP | `Content/score.gpif` (XML) |

The reader detects the format from the content, not from the extension: `PK` (ZIP), `BCFZ` or
`BCFS` (GPX), otherwise the binary formats with their version string.

A Guitar Pro score has **tracks** (instruments, usually one staff each) and **master bars**
(measure headers shared by all tracks: time, key, repeats, endings, markers). Each track has one
**bar** per master bar, a bar has voices, a voice has **beats** (rhythmic events) and a beat has
**notes**, each on a string with a fret (or a drum articulation).


## Binary formats (GP3, GP4, GP5)

### Primitives

All integers are little-endian. Strings appear in three forms:

| Name | Encoding |
|------|----------|
| byte size string (n) | length byte, then a field of n bytes |
| int byte size string | int (field size + 1), length byte, then the field |
| int size string | int length, then the characters |

Strings are Windows-1252.

### File layout

```
version              byte size string (30), for example "FICHIER GUITAR PRO v5.10"
information          title, subtitle, artist, album, words, [music: GP5 only], copyright, tab,
                     instructions (int byte size strings), notices (int count + strings)
triplet feel         bool (GP3 and GP4 only)
lyrics               GP4 and GP5: track int (1-based), 5 lines of (start measure int, int size string)
master effect        GP5.10: 19 bytes
page setup           GP5: 30 bytes, 10 int byte size strings
tempo name           GP5: int byte size string
tempo                int (quarter notes per minute)
hide tempo           GP5.10: bool
key                  int (fifths); GP4 and GP5: octave byte
MIDI channels        64 x (program int, volume, balance, chorus, reverb, phaser, tremolo bytes, 2 bytes)
directions, reverb   GP5: 38 bytes (19 shorts) + int
measure count        int
track count          int
measure headers      one per measure
tracks               one per track (then GP5: 2 bytes for 5.00, 1 byte for 5.10)
measures             for each measure, for each track: one bar
```

### Measure header

```
[GP5, not the first measure: 1 byte]
flags                byte: 0x01 numerator, 0x02 denominator, 0x04 repeat start, 0x08 repeat end,
                     0x10 alternate ending, 0x20 marker, 0x40 key, 0x80 double bar
numerator            signed byte (0x01)
denominator          signed byte (0x02)
repeat count         signed byte (0x08)
alternate ending     GP3/GP4: byte, ending number (0x10)
marker               int byte size string + color (4 bytes) (0x20)
key                  signed byte fifths, signed byte (1 = minor) (0x40)
alternate endings    GP5: byte, bit mask (bit 0 = ending 1) (0x10)
GP5                  beaming 4 bytes (if 0x01 or 0x02), 1 byte (without 0x10), triplet feel byte
```

Time and key are only stored where they change; a measure takes them from the previous one.

### Track

```
[GP5: 1 byte before the first track, and before every track in version 5.00]
flags                byte: 0x01 drums
name                 byte size string (40)
string count         int
tuning               7 ints (MIDI pitch, highest string first)
port, channel, effect channel, fret count, capo     ints (channel is the index into the MIDI channels, 1-based)
color                4 bytes
GP5                  flags short, auto accentuation byte, bank byte, RSE (humanize byte, 3 ints,
                     12 bytes, instrument int, unknown int, sound bank int, effect: short + 1 byte in
                     5.00, int in 5.10); GP5.10: equalizer 4 bytes, effect and category strings
```

### Bar, beat and note

A bar has one voice (GP3, GP4) or two voices (GP5, followed by a line break byte). A voice is a
beat count int followed by the beats.

```
beat flags           byte: 0x01 dotted, 0x02 chord, 0x04 text, 0x08 beat effects, 0x10 mix table,
                     0x20 tuplet, 0x40 status (empty or rest)
status               byte (0x40): 0x00 empty beat, 0x02 rest
duration             signed byte: -2 whole, -1 half, 0 quarter, 1 eighth ... 4 64th
tuplet               int (0x20): 3 in 2, 5/6/7 in 4, 9 to 13 in 8, 2 in 3, 4 in 6, 8 in 12
chord                (0x02) chord diagram with its name
text                 (0x04) int + byte length string
beat effects         (0x08) slap/tap/pop, tremolo bar, stroke, pick stroke
mix table            (0x10) instrument, volume, pan, tempo and their transition durations
string flags         byte: bit 6 = first (highest) string ... bit 0 = seventh string
notes                one per flagged string
GP5                  display flags short; 0x0800: 1 more byte
```

```
note flags           byte: 0x01 duration (GP3/GP4: 2 bytes; GP5: duration percent double),
                     0x02 heavy accent, 0x04 ghost, 0x08 effects, 0x10 dynamics, 0x20 type and fret,
                     0x40 accent, 0x80 fingering
type                 byte (0x20): 1 normal, 2 tie (continues the previous note on the string), 3 dead
dynamics             signed byte (0x10): 1 ppp ... 6 f ... 8 fff
fret                 signed byte (0x20); for drums the General MIDI drum note
fingering            left and right hand signed bytes (0x80)
GP5                  1 more flags byte
note effects         (0x08) flags byte (GP4/GP5: 2 flags bytes): bend, grace note, tremolo
                     picking, slide, harmonic, trill, let ring, hammer-on/pull-off, vibrato,
                     palm mute, staccato
```

The sounding pitch of a note is the tuning of its string plus the fret.


## GPX container (Guitar Pro 6)

**BCFZ** is a compressed file system: `BCFZ`, the uncompressed length (int), then a stream of bits
(most significant bit first). A `1` bit starts a back reference: a 4 bit word size, then offset
and length with that many bits (least significant bit first); the bytes are copied from that
offset back in the output. A `0` bit starts literals: a 2 bit count (least significant bit first)
followed by that many bytes of 8 bits. The stream may end before the declared length.

**BCFS** is the file system itself: `BCFS`, then sectors of 4096 bytes (the first sector is
skipped). A sector starting with the int `2` is a file entry: name at offset 4 (127 bytes, zero
terminated), size at offset 0x8C and a list of data sector numbers (ints) from offset 0x94 up to a
zero. The file content is the concatenation of these sectors, cut to the size.


## GPIF (Guitar Pro 6, 7 and 8)

`score.gpif` is XML. Elements are stored in flat lists and reference each other by ID:

```
GPIF
+-- GPVersion, Score            Title, SubTitle, Artist, Album, Words, Music, WordsAndMusic,
|                               Copyright, Tabber, Instructions
+-- MasterTrack/Automations     Tempo: Bar, Position (ratio of the bar), Value "bpm unit"
|                               (unit 1 eighth, 2 quarter, 3 dotted quarter, 4 half, 5 dotted half), Text
+-- Tracks/Track                Name, ShortName, InstrumentSet (Type drumKit, Elements/Articulations
|                               with OutputMidiNumber), Sounds/Sound/MIDI/Program (GP7+),
|                               GeneralMidi (GP6), MidiConnection/PrimaryChannel, Transpose
|                               (Chromatic, Octave), Staves/Staff/Properties (Tuning Pitches from
|                               the lowest string, CapoFret, DiagramCollection with chord names),
|                               Lyrics/Line (Text, Offset)
+-- MasterBars/MasterBar        Key (AccidentalCount, Mode), Time, Repeat (start, end, count),
|                               AlternateEndings, Section (Letter, Text), DoubleBar, Anacrusis,
|                               Fermatas, Directions (Jump, Target), Bars (one ID per track and staff)
+-- Bars/Bar                    Clef, Ottavia, Voices (4 IDs, -1 for none)
+-- Voices/Voice                Beats (IDs)
+-- Beats/Beat                  Rhythm ref, Notes (IDs), GraceNotes, Dynamic, Chord, FreeText,
|                               Lyrics/Line, Properties (PickStroke, Slapped, Popped ...)
+-- Notes/Note                  Tie (origin, destination), Vibrato, LetRing, Trill, Accent (bits:
|                               1 staccato, 4 heavy accent, 8 accent, 16 tenuto), LeftFingering,
|                               InstrumentArticulation (drums), Properties: String (0 = lowest),
|                               Fret, Midi, ConcertPitch, TransposedPitch (Step, Accidental,
|                               Octave), Tone and Octave (GP6), Muted, PalmMuted, HopoOrigin,
|                               Slide (Flags), HarmonicType, Bended
+-- Rhythms/Rhythm              NoteValue (Whole ... 256th), AugmentationDot count,
                                PrimaryTuplet (num, den), SecondaryTuplet
```

GPIF octaves are one higher than scientific pitch notation (octave 4 starts at MIDI 48).
`TransposedPitch` is the displayed pitch; it can include a capo and is only used for the spelling
when it matches the sounding pitch and the transposition of the track.


## Mapping to the toolarium-music Model

| Guitar Pro | Model |
|------------|-------|
| track | part with name, abbreviation, MIDI program, channel, volume and pan; drum tracks are percussion parts |
| guitar and bass | written an octave higher than they sound (transpose -12); GP7+ uses the Transpose of the track |
| master bar | time and key signature, repeats (count as times), alternate endings, double bar, marker as rehearsal mark, jumps (Segno, Coda, D.C., D.S., Fine) |
| tempo | metronome direction with sound tempo |
| beat | note, chord or rest with type, dots and time modification; empty beats are invisible rests |
| note | pitch spelled in the key (or the GPIF spelling), string and fret as technical notations |
| tie | tie from the previous note on the same string |
| hammer-on origin | hammer-on (higher fret) or pull-off (lower fret) to the next note on the string |
| slide | glissando to the next note on the string |
| dead note, harmonic, accent, staccato, trill, fingering | x notehead, harmonic, accent or strong accent, staccato, trill mark, fingering |
| grace note | grace note with slash before the beat |
| dynamics | dynamics direction where the dynamic changes |
| chord, text | chord symbol, words |
| drum note | unpitched note at the General MIDI drum position (x notehead for cymbals) |
| lyrics | lyrics of the lyrics track, syllables split at hyphens |

Not converted: bends, whammy bar, vibrato, palm mute, let ring, tremolo picking, strokes, mix table
changes other than tempo, RSE sound settings, layout and chord diagrams.
