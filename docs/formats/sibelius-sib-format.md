# Sibelius .sib File Format: Reverse-Engineered Documentation

Sibelius is a product of Avid Technology (formerly Sibelius Software);
"Sibelius" and related names are trademarks of Avid Technology.

> **Disclaimer:** This document is a best-effort reconstruction for interoperability purposes.
> It is not an official specification and may contain inaccuracies or omissions.
> No guarantee of correctness or completeness is made.


## Overview

Sibelius uses a proprietary binary container format for its score files.
The format has been in use since Sibelius 1.0 (originally on Acorn RISC OS, 1993)
and has evolved through many versions while maintaining backward compatibility.

| Extension | Description | Since |
|-----------|-------------|-------|
| `.sib` | Standard Sibelius score file | Sibelius 1.0 (1993) |
| `.si7` | Acorn RISC OS variant (Sibelius 6-7) | Sibelius 6 (Acorn) |
| `.sco` | Sibelius Scorch web viewer file | Sibelius 3+ |
| `.ideas` | Sibelius Ideas library database | Sibelius 5+ |
| `.lib` | Sibelius library file | Sibelius 2+ |

The `.sib` format is **not** based on ZIP, XML, OLE2, or any other public standard.
It uses a custom container with encrypted and compressed payloads.

**PRONOM identifier:** fmt/696

**MIME type (unofficial):** `application/x-sibelius-score`


## File Identification

### Magic Bytes

All Sibelius files begin with a 10-byte signature:

```
Offset  Hex                          ASCII
0x00    0F 53 49 42 45 4C 49 55 53 00   .SIBELIUS.
```

- Byte 0: `0x0F` (length prefix marker, 15 decimal)
- Bytes 1-8: ASCII `SIBELIUS`
- Byte 9: `0x00` (null terminator)

Sibelius Scorch files use a different magic: `0F 43 43 53 43 4F 52 43 48` = `.CCSCORCH`

### Version Field

Bytes 10-11 (immediately after the magic) contain a **big-endian u16** version identifier.
Combined with bytes 12-13, this forms a 32-bit container version number.

The full version is a 32-bit big-endian integer at offset 0x0A:

```
Container Version = (bytes[0x0A] << 24) | (bytes[0x0B] << 16) | (bytes[0x0C] << 8) | bytes[0x0D]
```


## Container Version Table

| Version (hex) | Version (u16 high) | Sibelius Version | Notes |
|---------------|-------------------|------------------|-------|
| `0x00020001` | `0x0002` | 1.x | Earliest known |
| `0x00080001` | `0x0008` | 2.x | |
| `0x000A0001` | `0x000A` | 3.x | |
| `0x001B0015` | `0x001B` | 4.x | |
| `0x001C0001` | `0x001C` | 4.x (variant) | Mapped to 0x1B0015 |
| `0x00280001` | `0x0028` | 4.x (late) | |
| `0x002D0001` | `0x002D` | 5.x | Used in .ideas files |
| `0x002D0003` | `0x002D` | 5.0 | |
| `0x00360000` | `0x0036` | 6.x | Sibelius 6 |
| `0x00390001` | `0x0039` | 7.x | Sibelius 7 |
| `0x003D000E` | `0x003D` | 7.5.x | |
| `0x00440001` | `0x0044` | 2024.3.1+ | Recent versions |
| `0x00450005` | `0x0045` | 2025.x | Latest known |

Over 40 distinct version signatures have been documented by PRONOM (fmt/1978-1989).


## Container Structure

### Block/Chunk Format

A Sibelius file is organized as a **tree of chunks** (also called blocks).
Each chunk begins with the same 10-byte magic signature followed by structured header fields.

#### Chunk Header (28+ bytes)

```
Offset  Size    Type        Description
0x00    10      bytes       Magic: 0x0F + "SIBELIUS" + 0x00
0x0A    4       u32 BE      Container version
0x0E    4       u32 BE      Timestamp (only if version > 6)
0x12    4       u32 BE      Mode flags (see below)
0x16    4       u32 BE      Compress info (only if compressed)
0x1A    4       u32 BE      Child count (only if version > 0x210000)
```

**Note on field sizes:** Header fields after the version use 32-bit big-endian integers.
In files with version <= 6, the timestamp field is absent, shifting subsequent offsets.
In practice, the header layout varies by version.

For Sibelius 6 files (version 0x0036), the observed header structure after the 12-byte
magic+version is:

```
Offset  Size    Description
0x0C    2       u16 BE: Flags / sub-version
0x0E    2       u16 BE: Block count estimate (approx file_size / 512)
0x10    2       u16 BE: Checksum / hash
0x12    2       u16 BE: Reserved (0x0000)
0x14    2       u16 BE: Chunk type (0x0030 = Container, 0x003A = Data)
0x16    2       u16 BE: Reserved (0x0000)
0x18    2       u16 BE: Child count
```

Total fixed header: **26 bytes** for the observed format.

#### Chunk Types

| Type | Hex | Description |
|------|-----|-------------|
| Container | `0x0030` | Directory chunk containing child references |
| Data | `0x003A` | Leaf chunk containing encrypted/compressed payload |

#### Child Reference Table

Container chunks include a child reference table immediately after the header.
Each entry is **8 bytes**:

```
Offset  Size    Description
0x00    2       u16 BE: Child tag/type identifier
0x02    2       u16 BE: Child offset high word
0x04    2       u16 BE: Child offset low word
0x06    2       u16 BE: Reserved (0x0000)
```

The child offset is a 32-bit value assembled as: `(high_word << 16) | low_word`

The last entry may lack the trailing 2-byte reserved field (the next chunk's magic
starts immediately).

#### Tag Types (Chunk Tags)

| Tag | Description | Context |
|-----|-------------|---------|
| 0 | Unused/null entry | Placeholder in root |
| 1 | Main score data (sib) | Root or Version_Dir |
| 2 | Library data (lib) | Library files |
| 3 | Secondary score data (sib3) | Alternate format |
| 4 | Idea directory entry | Ideas database |
| 5 | Idea text metadata | Within Idea_Dir |
| 6 | Idea score snippet (sib) | Within Idea_Dir |
| 7 | Idea thumbnail (PNG) | Within Idea_Dir |
| 8 | Scorch PDF directory | Scorch files |
| 9 | Scorch PDF DRM/restrictions | Scorch files |
| 10 | Scorch PDF embedded data | Scorch files |
| 11 | Versions directory | Score version history |
| 12 | Version directory entry | Within Versions_Dir |
| 13 | Version description | Within Version_Dir |


## File Layout Examples

### Simple .sib File (most scores)

```
[Root Container]  (type=0x0030, children=2)
  +-- [tag=1] Main Data Block  (type=0x003A, encrypted+compressed payload)
  +-- [tag=0] Null/empty entry
```

The vast majority of .sib files contain just two top-level entries: the main encrypted
score data (tag 1) at offset 0x2A, and a null entry.

### .sib File with Version History (e.g., Piano Solo.sib)

```
[Root Container]  (type=0x0030, children=2)
  +-- [tag=1]  Main Data Block  (type=0x003A)
  +-- [tag=11] Versions Directory  (type=0x0030, children=1)
      +-- [tag=12] Version Directory  (type=0x0030, children=2)
          +-- [tag=13] Version Description  (type=0x003A)
          +-- [tag=1]  Version Score Data   (type=0x003A)
```

### .ideas File (Ideas Library Database)

```
[Root Container]  (type=0x0030, children=N)
  +-- [tag=4] Idea Entry 0  (type=0x0030, children=3)
  |   +-- [tag=5] Idea Text   (type=0x003A, metadata)
  |   +-- [tag=6] Idea Score  (type=0x003A, score snippet)
  |   +-- [tag=7] Idea Image  (type=0x003A, PNG thumbnail)
  +-- [tag=4] Idea Entry 1  (type=0x0030, children=3)
  |   +-- ...
  +-- ...
```


## Encryption

### Overview

The data payload within Data chunks (type 0x003A) is encrypted using a
**stream cipher** based on a pseudo-random number generator (PRNG).
The cipher was first publicly described by cw2k in the SCORA project.

The encryption is applied **byte-by-byte**: each plaintext byte is XORed with the
low byte of the next PRNG output value.

### Cipher Seed and Data Start

The seed and the start of the encrypted data are relative to the **end of the chunk header**
(after magic, version, timestamp, mode and compress info: 0x1A bytes into the chunk for all
versions with a timestamp), not to the file start: **(verified)**

| File | Seed | Encrypted data |
|------|------|----------------|
| SIBELIUS | header end + 0x17 (4-byte big-endian) | seed + 4 + 0x23 |
| CCSCORCH | header end + 0x0D (4-byte big-endian) | seed + 4 + 0x11 |

For versions after 0x210000 a 4-byte item count (not encrypted) follows at the data start, and
the encrypted data starts after it. For a Sibelius 6 score chunk at file offset 0x2A the seed is
at 0x5B and the data at 0x86.

**Root chunk by version.** Sibelius 1 to 4 files (version high word up to 0x001B) store the
encrypted score chunk directly at the root (mode 0x3A). Newer files have a directory chunk
(mode 0x30) at the root whose child table lists the score chunk (type 1). This was verified on 1289 local samples.

**Unlocked files.** Converter tools (for example SCORA) write the score chunk with mode 0x30
(neither encrypted nor compressed) and the decompressed payload directly after the header and
item count. Such a chunk is still referenced as type 1 by the root directory.

**Simple cipher.** Some Sibelius 1 and 2 files (versions 0x0000000A, 0x0001000D, 0x00080008) use
a simpler cipher: a single MSVC LCG seeded with the seed, `key = key * 214013 + 2531011`,
keystream byte `(key >> 16) & 0x7FFF`. The seed and data offsets are the same. Files of the same
version also use the standard cipher, so the version does not tell which cipher applies; the
reader tries the standard cipher first and the simple cipher if the result is no valid zlib
stream. **(verified)**

### PRNG Algorithm

The PRNG uses two Linear Congruential Generators (LCGs) combined with a mode selector.

**Key derivation from seed:**

```
PRNG_Mode = 2  (for .sib files; 3 for .sco files)
Key = CryptSeed
EKey1 = (CryptSeed & 0xFFFFFFF8) ^ 0x1F30D27B
EKey2 = (CryptSeed & 0xFFFFFFF8) ^ 0x3A4C80B2
EKeyMode = (CryptSeed & 7) << 4
```

**PRNG step function (mode 2, used for .sib files):**

```java
int[] PostXor = { 27811, 12742, 37606, 5005 };

EKeyMode = (EKeyMode * 13821) & 0x7FFF;       // Mode multiplier for .sib
int EKeyMode_tmp = (EKeyMode >> 8) & 3;        // Select which LCG(s) to use
int EKey1_tmp = 0;

if (EKeyMode_tmp != 2) {
    EKey1 = EKey1 * 214013 + 2531011;          // MSVCR90 LCG
    EKey1_tmp = EKey1;
}
if (EKeyMode_tmp != 1) {
    EKey2 = EKey2 * 1103515245 + 12345;        // glibc LCG
    EKey1_tmp ^= EKey2;
}

return PostXor[EKeyMode_tmp] ^ ((EKey1_tmp >> 16) & 0x7FFF);
```

**For Scorch files (mode 3),** the only difference is the mode multiplier:

```
EKeyMode = (EKeyMode * 47485) & 0xFFFF;       // 0xB97D
```

**Decryption:**

```
plaintext_byte = encrypted_byte ^ (PRNG() & 0xFF)
```

### Mode Flags

The chunk header contains a mode/flags field that indicates the processing layers:

| Bit | Mask | Meaning |
|-----|------|---------|
| 1 | 0x02 | Encrypted (stream cipher) |
| 3 | 0x08 | Compressed (zlib/deflate) |

Both flags are typically set for score data chunks.


## Compression

After decryption, the payload is **zlib-compressed** (deflate algorithm).
Standard zlib decompression (Java `Inflater`, Python `zlib.decompress`) can be used
on the decrypted byte stream.

The decompressed data contains the actual score object tree.


## Decompressed Payload Structure

### Overview

The decompressed payload contains a serialized tree of **named fields** that represent
the musical score data. The data chunk header (after decryption and decompression)
contains an `items` count indicating the number of top-level score objects.

The payload uses a combination of:

- Fixed-size numeric fields (integers and floats)
- Length-prefixed UTF-16BE strings
- 7-bit packed variable-length integers (for sizes/counts in some contexts)
- Type identifier bytes (for embedded objects like images)

### Byte Ordering

The payload uses **mixed endianness**:

- **Container headers** (magic, version, chunk fields): big-endian
- **Payload numeric values**: stored as 32-bit values in a `[u16 high, u16 low]` format
  where each 16-bit word is big-endian, but IEEE 754 floats are stored as **little-endian**
  32-bit values

Examples from decompressed Choir.sib:
- `9A 99 19 3F` = float 0.6 (little-endian IEEE 754)
- `00 00 80 3F` = float 1.0 (little-endian IEEE 754)
- `00 00 80 3E` = float 0.25 (little-endian IEEE 754)

### String Encoding

Strings are stored as length-prefixed UTF-16BE:

```
u16 BE: padding (0x0000) or flags
u16 BE: byte length of string data
bytes:  UTF-16BE encoded character data
```

The byte length is always even (2 bytes per character). String character count
equals byte_length / 2.

Verified examples from decompressed Choir.sib payload:
- `00 00 00 08` + `00 56 00 2E 00 53 00 2E` = "V.S." (8 bytes, 4 chars)
- `00 00 00 26` + UTF-16BE data = "Enclos'd in cinders" (38 bytes, 19 chars)
- `00 00 00 1A` + UTF-16BE data = "Thomas Chevis" (26 bytes, 13 chars)

### Score-Level Fields (Observed)

The payload begins with score-level properties as a sequence of fixed-size fields.
Observed from the Choir.sib example (4 SATB staves, 6 bars):

| Offset | Value | Likely Meaning |
|--------|-------|----------------|
| 0x00 | 0 | Reserved |
| 0x04 | 64 | Score width (units TBD) |
| 0x08 | 3000 | Score height (units TBD) |
| 0x14 | 528 | Staff/page layout parameter |
| 0x34 | 2 | Number of staves or voices |
| 0x44 | 205 | Bar count or layout parameter |
| 0x48 | 1024 | Bar duration (1024 = whole note in 1/256th quarters) |
| 0x54 | 6 | Number of bars |

After the fixed-size score header, the payload contains:
- Score metadata strings (title, composer, lyricist, copyright)
- Part/instrument names
- Sound device configuration (MIDI devices, sound sets)
- Line type definitions (hairpins, slurs, glissandi)
- Articulation definitions
- Text style definitions (font names, styles)
- The actual musical content (staves, bars, notes)

### Known String Field Types in Payload

From analysis of decompressed .sib files, these field categories appear:

| Category | Example Values |
|----------|---------------|
| Metadata | Title, Composer, Lyricist, Copyright |
| Part names | "Full Score", "SOPRANO\\nALTO\\nTENOR\\nBASS" |
| Sound devices | "Kontakt Player", "Microsoft GS Wavetable SW Synth" |
| Sound sets | "General MIDI" |
| Line identifiers | "line.staff.hairpin.crescendo", "line.staff.slur.up" |
| Articulations | "detache", "+legato", "Pause" |
| Playback params | "Duration", "1:110:1:100" |
| Text styles | "Instrument names", "text.instrumentname" |
| Font names | "Times New Roman" |

### Payload Layout

The decompressed payload is organized in five major regions:

```
[Score header]        ~0x000 to ~0x400     Metadata, fixed fields, LE floats
[Style definitions]   ~0x400 to ~3%        Text styles, line types, symbols, articulations
[Staff definitions]   ~3% to ~7%           Staff types, instrument sound IDs, clefs
[Musical content]     ~7% to ~75%          Bar records, note data, tempo keywords
[Type dictionary]     ~96% to ~97%         Consecutive type name strings (paired)
[Bucket data]         ~97% to end          Bucket containers, document GUID
```

All Sibelius 6 files use exactly **48 top-level items** in the payload header.
The object data region is approximately 78% zero bytes, indicating sparse field
encoding with many unused/default fields.

### Staff Definitions

The staff definition region contains one entry per available staff type.
Each entry includes several consecutive named strings:

| String prefix | Description | Example |
|---------------|-------------|---------|
| `stafftype.pitched.` | Pitched staff type | `stafftype.pitched.5.singers` |
| `stafftype.tab.` | Tablature staff type | `stafftype.tab.6.guitar.standard` |
| `stafftype.percussion.` | Percussion staff type | `stafftype.percussion.5.drum.set` |
| `instrument.` | Instrument sound ID | `instrument.singers.soprano` |
| `clef.` | Default clef assignment | `clef.treble`, `clef.bass`, `clef.treble.down.8` |
| `voice.` | Voice type for playback | `voice.male.tenor.en` |

The full library contains over 300 staff type definitions covering pitched instruments,
tablature tunings (guitar, lute, banjo, ukulele, etc.), and percussion setups.

### Clef Identifiers

| Sibelius ID | MusicXML mapping |
|-------------|-----------------|
| `clef.treble` | G clef, line 2 |
| `clef.bass` | F clef, line 4 |
| `clef.treble.down.8` | G clef, line 2, octave change -1 |
| `clef.treble.up.8` | G clef, line 2, octave change +1 |
| `clef.alto` | C clef, line 3 |
| `clef.tenor` | C clef, line 4 |
| `clef.percussion` | percussion clef |
| `clef.tab` | TAB clef |

### Bar Records

Musical content is stored using fixed-size **30-byte bar records** with a `0x22`
(34 decimal) marker byte at the start. Records are contiguous with a stride of 30
bytes. A typical score contains `(number_of_staves + 1) * number_of_bars` records
(the +1 is for the system staff).

Most fields in bar records default to zero. Observed non-zero fields include values
at offsets 7-8 (possibly duration or position) and 20-24 (possibly note references
or layout data).

### Tempo Keywords

The payload contains a block of **TempoKeyword** entries, each pairing a tempo
name with a BPM value:

```
"Adagio"          + TempoKeyword + float 0x42700000 (60.0 BPM)
"Allegretto"      + TempoKeyword + float 0x42500000 (52.0 BPM)
"Allegro"         + TempoKeyword + float 0x42C80000 (100.0 BPM)
```

**DeltaTempoKeyword** entries provide relative tempo changes (rit., accel.).

### Document GUID

The payload ends with a document GUID as a UTF-16BE string in standard UUID format
(e.g., `0D304C44-F843-41B9-BBA9-72EFE91E4950`). This uniquely identifies the score
document.

### Bar Content Index

After the 30-byte bar records, the payload contains a **bar content index**:
a sequential list of entries that maps numbered objects to their positions
within the bar. Each entry is 7 or 8 bytes:

```
u8:  entry index (1-based, sequential)
u32: position offset (increasing values within each bar)
u8:  flags (0x00 or 0x01, possibly indicating a tie or continuation)
```

Observed from the Choir.sib example (first staff, first bar, 15 entries):

```
Entry  1: position=0x064 (100)   flags=0x00
Entry  2: position=0x0CA (202)   flags=0x00
Entry  3: position=0x131 (305)   flags=0x00
...
Entry 15: position=0x5DC (1500)  flags=0x01
```

After the content index, a **reset marker** (`01 00 2A 00 00 00 00`) appears,
followed by the next staff's or bar's data.

### Note Object Data

Note objects are stored as fixed-size records deep in the payload (around 80-90%
through the file). Each note record has the following confirmed structure,
verified by binary diffing of minimal test scores created in Sibelius 6:

```
Offset  Bytes  Description
-32     1      Padding (0x00)
-15     4      Marker: FF FF FF FF
-11     1      Duration type code (see table below)
-10     6      Zeros
 -4     1      Flags: 0x01
 -3     1      Constant: 0x4B (75)
 -2     1      Constant: 0x00
 -1     0      (pitch fields start here)
  0     1      Diatonic pitch (7 per octave, 35 = middle C)
 +1     1      MIDI pitch * 2 (120 = middle C, i.e., 60 * 2)
 +2     1      Constant: 0x00
 +3     1      Duration type code (repeated): 0x08
 +4     28     Zeros and flags (mostly 0x00)
```

Offsets are relative to the diatonic pitch byte. The record is preceded by a
16-byte header with `FF FF FF FF` marker and followed by ~32 bytes of mostly zeros.

**Duration type codes** (at offset -11 relative to pitch):

| Code | Duration |
|------|----------|
| `0x05` | 32nd note |
| `0x06` | 16th note |
| `0x07` | Eighth note |
| `0x08` | Quarter note |
| `0x09` | Half note |
| `0x0A` | Whole note |

**Pitch encoding:**

| Field | Formula | Example (C4) | Example (D4) | Example (E4) |
|-------|---------|-------------|-------------|-------------|
| Diatonic pitch | 7 * octave + step_index (C=0) | 35 (0x23) | 36 (0x24) | 37 (0x25) |
| MIDI pitch * 2 | standard_midi_pitch * 2 | 120 (0x78) | 124 (0x7C) | 128 (0x80) |

The MIDI pitch is stored doubled (multiplied by 2), likely to allow half-step
precision for microtonal notation.

**Note record spacing:**

In a two-note score (C4 + E4), the note records are **207 bytes apart**, suggesting
a fixed note record size of approximately 207 bytes with variable padding.

**Detection pattern:**

Note records can be located by searching for the byte sequence
`01 4B 00` followed by two pitch bytes where the second byte equals
the first byte's MIDI equivalent doubled.

### Lyrics and Text Content

Lyrics and title text appear in the post-bar data region, stored as
length-prefixed UTF-16BE strings with uppercase markup:

- Title text is stored with `header_notp1` prefix markers
- Lyric text (e.g., "Enclos'd in cinders") appears inline
- Creator text (e.g., "WILLIAM SHAKESPEARE") may be uppercased

### Beat Grouping Data

Time signature behavior is defined by beat grouping strings:

```
"2,2,2"                 Three groups of 2 (6/8)
"4,4,4"                 Three groups of 4 (12/8)
"8,8,8"                 Three groups of 8
"4,4,4,4,4,4"           Six groups of 4
```

These determine how beams are grouped within each bar.

### Open Questions

The following aspects of the payload structure remain unmapped:

- **NoteRest field layout:** The exact byte positions of pitch, duration, voice,
  stem direction, and articulation flags within a NoteRest object are not yet
  fully mapped. The structure uses positional encoding without inline field names.
- **Version differences:** The payload grammar varies across Sibelius versions.
  The libsib project documented that the grammar follows Sibelius 8.3.0 readers,
  with extensions for fields added through Sibelius 2025.3.

### Type Dictionary

Near the end of the payload (approximately 96% through), there is a **type dictionary**
region containing consecutive length-prefixed UTF-16BE type name strings. Each type
appears twice (reader name and writer name). Known object types (from Sibelius 6):

| Type Name | Description |
|-----------|-------------|
| NoteRest | Notes, chords, and rests |
| Stave | Individual staff |
| StaveGroup | Multi-staff instrument group |
| System | System (line of music) |
| SystemStave | System staff (tempo, rehearsal marks) |
| KeySignature | Key signature |
| TimeSignature | Time signature |
| Slur | Slur line |
| Hairpin | Crescendo/diminuendo hairpin |
| Line, GlissLine, PedalLine, TrillLine, RepeatLine | Various line types |
| Lyric | Lyric text |
| TextItem, SystemTextItem | Text objects |
| ExpressionTextItem | Expression text |
| TechniqueTextItem | Technique text |
| TempoTextItem | Tempo text |
| Tuplet | Tuplet group |
| Tie | Tie between notes |
| Symbol, SystemSymbol | Symbol objects |
| GuitarFrame | Guitar chord diagram |
| Graphic | Embedded graphic |
| Highlight | Highlight marking |
| InstrumentChange | Instrument change |
| SpecialBarline, InvisibleBarline, SystemicBarLine | Barline types |
| RehearsalMark | Rehearsal mark |
| RhythmDot | Augmentation dot |
| Selection | Selection state |
| StaveLines | Staff line configuration |
| StaveTypeChange | Staff type change |
| HitPointBucket, EndingLineBucket, PedalLineBucket | Bucket containers |


### 7-Bit Packed Integers

Variable-length integers (used for sizes in some contexts) are encoded using a 7-bit
packing scheme where each byte contributes 7 bits of data. The least significant bit
of each byte indicates whether more bytes follow:

```
Bit 0 = 1: More bytes follow (7 high bits contain data)
Bit 0 = 0: Last byte (7 high bits contain data)
```

### Data Types in Payload

| Type Byte | Data Type | Description |
|-----------|-----------|-------------|
| Various | int32 | 32-bit integer (mixed endianness) |
| Various | float32 | IEEE 754 single-precision float (little-endian) |
| Various | string | Length-prefixed UTF-16BE text |
| `0x5C` | image | Embedded ARGB bitmap data |

### Embedded Images

Images (thumbnails, graphic objects) are stored inline with a specific structure:

```
0x5C (image marker byte)
u32 BE: pixel count (width * height)
bytes:  ARGB pixel data (pixel_count * 4 bytes)
u32 BE: width in pixels
u32 BE: checksum[0]
u32 BE: checksum[1]
u32 BE: checksum[2]
u32 BE: checksum[3]
float:  X pixels per mm (typically 3.78 = 96 DPI)
float:  Y pixels per mm (typically 3.78 = 96 DPI)
7bit:   additional data size
bytes:  additional data
```

Image pixels are stored bottom-to-top (like BMP), requiring vertical flip for display.


## Score Object Model

The decompressed payload serializes Sibelius's internal object hierarchy.
This model is documented through the ManuScript plugin API reference (Sibelius 6).

### Object Hierarchy

```
Score
  +-- SystemStaff (system-level objects: tempo, rehearsal marks, system text)
  |   +-- Bar[]
  |       +-- BarObject[] (TimeSignature, KeySignature, RehearsalMark, SystemTextItem...)
  +-- Staff[] (one per instrument/voice)
      +-- Bar[]
          +-- BarObject[]
              +-- NoteRest (notes, chords, rests)
              |   +-- Note[] (individual noteheads within a NoteRest)
              +-- Clef
              +-- KeySignature
              +-- TimeSignature
              +-- Text / TextItem
              +-- Line (hairpins, slurs, pedal lines, trills, etc.)
              +-- Tuplet
              +-- BarRest
              +-- LyricItem
              +-- GuitarFrame (chord diagrams)
              +-- SymbolItem
              +-- Barline
```

### Key Object Properties

#### Score

| Property | Type | Description |
|----------|------|-------------|
| StaffCount | int | Number of staves |
| BarCount | int | Number of bars (same for all staves) |
| Title | string | Score title (from Score Info) |
| Composer | string | Composer name |
| Arranger | string | Arranger name |
| Copyright | string | Copyright notice |
| Publisher | string | Publisher |
| ScoreWidth | float | Page width in mm |
| ScoreHeight | float | Page height in mm |
| StaffHeight | float | Staff height in mm |
| PageCount | int | Number of pages |
| TransposingScore | bool | Transposing score mode |
| InstrumentChanges | bool | Has instrument changes |
| DynamicParts | bool | Has dynamic parts |

#### Staff

| Property | Type | Description |
|----------|------|-------------|
| StaffNum | int | Staff number (1-based) |
| InstrumentName | string | Full instrument name |
| ShortInstrumentName | string | Abbreviated instrument name |
| BarCount | int | Number of bars in staff |
| IsPercussionStaff | bool | Percussion notation |
| NumStavesInSameInstrument | int | Staves per instrument |
| Channel | int | MIDI channel |
| Volume | int | MIDI volume (0-127) |

#### Bar

| Property | Type | Description |
|----------|------|-------------|
| BarNumber | int | Bar number (internal, 1-based) |
| Length | int | Bar duration in 1/256th quarter notes |
| TimeSignature | TimeSignature | Active time signature |
| KeySignature | KeySignature | Active key signature |
| BarObjectCount | int | Number of objects in bar |
| NoteCount | int | Number of NoteRests in bar |

#### NoteRest

| Property | Type | Description |
|----------|------|-------------|
| Position | int | Rhythmic position (1/256th quarter notes from bar start) |
| Duration | int | Duration (1/256th quarter notes) |
| NoteCount | int | Number of noteheads (0=rest, 1=note, 2+=chord) |
| Voice | int | Voice number (1-4) |
| GraceNote | bool | Is grace note |
| IsAcciaccatura | bool | Grace note with slash |
| IsAppoggiatura | bool | Grace note without slash |
| Beam | int | Beam grouping info |
| StemFlipped | bool | Stem direction overridden |
| SingleTremolos | int | Tremolo strokes on stem (-1 to 7) |
| Highest | Note | Highest note in chord |
| Lowest | Note | Lowest note in chord |
| Articulations | int | Articulation flags |

#### Note (within NoteRest)

| Property | Type | Description |
|----------|------|-------------|
| Pitch | int | MIDI pitch (60 = middle C) |
| DiatonicPitch | int | Diatonic pitch (35 = middle C, 7 per octave) |
| Name | string | Pitch name as string (e.g., "C#4") |
| Accidental | int | Accidental type (Sharp, Flat, Natural, etc.) |
| Tied | bool | Tied to next note |
| NoteStyle | int | Notehead style index |
| StringNum | int | Guitar string number (-1 if unset) |

#### Rhythmic Position Units

Positions and durations use **1/256th of a quarter note** as the base unit:

| Duration | Value | Description |
|----------|-------|-------------|
| 1024 | Whole note (semibreve) | |
| 512 | Half note (minim) | |
| 256 | Quarter note (crotchet) | |
| 128 | Eighth note (quaver) | |
| 64 | 16th note (semiquaver) | |
| 32 | 32nd note (demisemiquaver) | |
| 16 | 64th note | |
| 8 | 128th note | |

Dotted values add half the base: dotted quarter = 256 + 128 = 384.

#### TimeSignature

| Property | Type | Description |
|----------|------|-------------|
| Numerator | int | Top number |
| Denominator | int | Bottom number |
| Text | string | Display text (for irregular meters) |

#### KeySignature

| Property | Type | Description |
|----------|------|-------------|
| Sharps | int | Number of sharps (negative for flats) |
| Major | bool | Is major key |
| AsText | string | Key name as string |

#### Clef

| Property | Type | Description |
|----------|------|-------------|
| StyleId | string | Clef identifier |

#### Line

| Property | Type | Description |
|----------|------|-------------|
| Type | string | Line type (hairpin, slur, trill, etc.) |
| EndBarNumber | int | Bar where line ends |
| EndPosition | int | Position where line ends |
| Duration | int | Total duration |

#### Text / TextItem

| Property | Type | Description |
|----------|------|-------------|
| Text | string | The text content |
| StyleId | string | Text style name |
| StyleAsText | string | Human-readable style name |

Text strings support inline formatting using backslash escape sequences:
`\B\` bold on, `\b\` bold off, `\I\` italic on, `\i\` italic off,
`\ffontname\` change font, `\spoints\` set font size.


## String Encoding

All text within the decompressed payload uses **UTF-16 Big-Endian** encoding.
This includes:

- Score metadata (title, composer, copyright)
- Text objects in the score
- Instrument names
- Style names and identifiers
- Scorch restriction fields

The SCORA project confirms this by using `"UTF-16BE"` for string operations
and by writing a BOM marker (`0xFE 0xFF`) when dumping text chunks.


## Scorch Restrictions (in .sco files)

Scorch files contain DRM-like restriction fields stored as UTF-16BE strings
in a dedicated chunk (tag 9). The chunk data starts with a BOM (`0xFE 0xFF`)
when dumped as text. Known restriction fields:

| Field | Description |
|-------|-------------|
| ViewAllPages | Controls whether all pages are visible or only a subset |
| PlayOptionType | Playback permission level |
| PrintMode | Print permission (enabled/disabled/limited) |
| ViewMode | Viewing mode restrictions |
| ViewPages | Specifies which page range is visible |
| SecurityScript | Embedded security/DRM script |

Additionally, a `GlobalSnap` byte at offset `0x6A` in the decompressed score
data controls save and print permissions (0 = restricted, 1 = unrestricted).


## Related File Formats

### Sibelius Scorch (.sco)

Uses `CCSCORCH` magic (hex `0F 43 43 53 43 4F 52 43 48`) instead of `SIBELIUS`.
The container structure is identical to `.sib`, with these differences:

**Cipher:** PRNG mode 3 (vs mode 2 for `.sib`). The mode multiplier changes from
13821 to 47485, and the key-mode mask is `0xFFFF` instead of `0x7FFF`.
The cipher seed offset also differs: 0x0D from file start (vs 0x17 for `.sib`).

**Embedded content:** Scorch files can wrap other content types, particularly PDF files.
The chunk tree may contain:

```
[Root Container]
  +-- [tag=8]  Scorch PDF Directory  (type=0x0030)
  |   +-- [tag=9]  DRM Restrictions  (type=0x003A, UTF-16BE text)
  |   +-- [tag=10] Embedded PDF Data (type=0x003A, raw PDF bytes)
  +-- [tag=1]  Score Data           (type=0x003A)
```

When a `.sco` file wraps a PDF, the score data chunk may be absent and the
embedded PDF is the primary payload.

**DRM restrictions:** Stored as UTF-16BE key-value strings in the tag 9 chunk.
These restrict what the Scorch web plugin allows (viewing, printing, playback).
Restriction fields can be neutralized by replacing the second byte of the field
name with `X` (e.g., `ViewAllPages` becomes `VXewAllPages`), which prevents the
Scorch plugin from recognizing the restriction.

**Scorch web plugin:** The `.sco` format was designed for the Sibelius Scorch browser
plugin (ActiveX/NPAPI). Scorch files were served with MIME type
`application/x-sibelius-score` and required a minimum Scorch plugin version
(typically 6150+). The plugin used an HTML `<object>` / `<embed>` tag with
`classid="clsid:A8F2B9BD-A6A0-486A-9744-18920D898429"`.

### Ideas Database (.ideas)

Uses the same container format with `SIBELIUS` magic. Contains multiple Idea entries,
each with text metadata (tag 5), a score snippet (tag 6), and a PNG thumbnail (tag 7).
The version field typically matches the creating Sibelius version.

### Library Files (.lib)

Standard Sibelius container format with tag 2 for library data.
Contains house styles, manuscript papers, and other reusable elements.


## Public Resources

- [SCORA (SCORch Away)](https://github.com/cw2k/SCORA): Java tool that decrypts and
  decompresses .sib and .sco files; first public description of the cipher algorithm
- [libsib](https://github.com/pfefferniels/libsib): Rust library for reading/writing
  .sib files; comprehensive format documentation and test coverage across versions
- [sib2mxl](https://github.com/pfefferniels/sib2mxl): Converts .sib to MusicXML 4.0,
  built on libsib
- [ManuScript Language Guide](https://resources.avid.com/SupportFiles/Sibelius/8.6/ManuScript_Language_Guide.pdf):
  Official Avid documentation of the plugin API, which mirrors the internal object model
- [Library of Congress FDD](https://www.loc.gov/preservation/digital/formats/fdd/fdd000609.shtml):
  Format description for Sibelius Music Notation Format
- [PRONOM fmt/696](https://www.nationalarchives.gov.uk/PRONOM/fmt/696):
  Technical registry entry with magic byte signatures
- [Tyler Thorsted's Sibelius Analysis](https://preservation.tylerthorsted.com/2024/04/12/sibelius/):
  Version identification research with hex signatures
- [DPC Sibelius Assessment](https://wiki.dpconline.org/images/6/62/Sibelius_Assessment_v1.15.pdf):
  Digital Preservation Coalition format assessment
