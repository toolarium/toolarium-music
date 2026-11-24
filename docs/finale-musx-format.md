# Finale .musx/.mus File Format: Reverse-Engineered Documentation

Documented from analysis of Finale 27's file structure and publicly observable behavior.
Finale is a product of MakeMusic; "Finale", "Enigma", and related names are trademarks of MakeMusic.

> **Disclaimer:** This document is a best-effort reconstruction for interoperability purposes.
> It is not an official specification and may contain inaccuracies or omissions.
> No guarantee of correctness or completeness is made.


## Overview

Finale uses a proprietary format called **Enigma** (Environment for Notation Intuitive Graphic
Music Algorithms). The format comes in two variants:

| Extension | Format | Since |
|-----------|--------|-------|
| `.mus` | Legacy binary (OLE2-style container) | Finale 1.0 |
| `.musx` | Modern variant (ZIP container with encrypted XML payload) | Finale 2014 (v25+) |

Finale was discontinued by MakeMusic on 2024-08-26.


## .musx Container Format

The `.musx` file is a **ZIP archive** with MIME type `application/vnd.makemusic.notation`.

### Standard ZIP entries (present in all .musx files)

| Entry | Type | Typical Size | Description |
|-------|------|-------------|-------------|
| `mimetype` | text | 34 bytes | Always `application/vnd.makemusic.notation` (uncompressed, first entry) |
| `META-INF/container.xml` | XML | ~252 bytes | Points to `score.dat` as the root file |
| `NotationMetadata.xml` | XML | ~343 bytes | Key signature, tempo, duration, creator |
| `score.dat` | binary | 70K-180K+ | XOR-encrypted gzip of EnigmaXML (see below) |

### Optional entries

| Entry | Type | Description |
|-------|------|-------------|
| `presets/*.preset` | XML (Apple plist) | AudioUnit instrument presets |

### container.xml format

```xml
<?xml version="1.0" encoding="UTF-8"?>
<container version="26" xmlns="http://www.makemusic.com/2012/container">
  <rootfiles>
    <rootfile full-path="score.dat"
              media-type="application/vnd.makemusic.notation.dat.1"/>
  </rootfiles>
</container>
```

### NotationMetadata.xml format

```xml
<?xml version="1.0" encoding="UTF-8"?>
<metadata version="26.1"
          xmlns="http://www.makemusic.com/2012/NotationMetadata">
  <fileInfo>
    <keySignature>G</keySignature>
    <initialTempo>120</initialTempo>
    <scoreDuration>64.0</scoreDuration>
    <creatorString>Finale 2012.2 for Macintosh</creatorString>
  </fileInfo>
</metadata>
```


## score.dat Decryption

The `score.dat` file uses a **symmetric XOR keystream cipher** based on a BSD-style
Linear Congruential Generator (LCG). The same function encrypts and decrypts (XOR is
its own inverse). The decrypted output is a **gzip archive** that decompresses to
**EnigmaXML**, a UTF-8 XML document containing all score data.

### Decryption pipeline

```
score.dat  -->  XOR keystream decrypt  -->  gzip data  -->  gunzip  -->  EnigmaXML
```

### Algorithm

```
SEED       = 0x28006D45
MULTIPLIER = 0x41C64E6D
INCREMENT  = 0x3039
RESET_EVERY = 0x20000 bytes (131072)

state = SEED
for each byte at position i:
    if i > 0 and (i mod RESET_EVERY) == 0:
        state = SEED
    state = (state * MULTIPLIER + INCREMENT) mod 2^32
    upper = (state >> 16) & 0xFFFF
    c = upper + floor(upper / 255)
    output[i] = input[i] XOR (c & 0xFF)
```

The LCG advances **before** each byte is used (the initial seed value is not used
directly for byte 0). The state resets to the initial seed every 131072 bytes.

### Verification

When correctly decrypted, the output begins with the gzip magic bytes `1F 8B 08`.
After gunzip decompression, the result is an XML document beginning with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<finale version="26.1" xmlns="http://www.makemusic.com/2012/finale">
```

### Data accessibility

| Data | Source | Available |
|------|--------|-----------|
| File format detection | ZIP mimetype | Yes |
| Container version | container.xml | Yes |
| Key signature, tempo, duration | NotationMetadata.xml | Yes |
| Creator application string | NotationMetadata.xml | Yes |
| AudioUnit presets | presets/*.preset | Yes |
| Notes, rests, pitches, durations | score.dat (EnigmaXML) | **Yes** |
| Lyrics, expressions, dynamics | score.dat (EnigmaXML) | **Yes** |
| Articulations, slurs, ties | score.dat (EnigmaXML) | **Yes** |
| Staff/instrument definitions | score.dat (EnigmaXML) | **Yes** |
| Page layout, system breaks | score.dat (EnigmaXML) | **Yes** |
| Repeats, endings, barlines | score.dat (EnigmaXML) | **Yes** |


## EnigmaXML Structure

The decrypted XML follows the namespace `http://www.makemusic.com/2012/finale` with
a `<finale>` root element. The top-level structure:

```xml
<finale version="26.1" xmlns="http://www.makemusic.com/2012/finale">
  <mappings>  ... </mappings>     <!-- Version migration rules -->
  <header>    ... </header>       <!-- File header and version info -->
  <options>   ... </options>      <!-- Global document options -->
  <others>    ... </others>       <!-- "Other" records: staves, measures, shapes, etc. -->
  <details>   ... </details>      <!-- "Detail" records: chords, groups, baselines -->
  <entries>   ... </entries>      <!-- Music entries: notes, rests, durations, pitches -->
  <texts>     ... </texts>        <!-- Text content: titles, lyrics, expressions -->
</finale>
```

### `<mappings>`: Version migration

Contains `<mapGroup>` elements that describe how to migrate data between format versions.
Each group has `minInclusive`/`maxExclusive` version bounds and a list of `<move>` or
`<rename>` operations. These allow older files to be upgraded to the current schema.

### `<header>`: File header

```xml
<header>
  <headerData>
    <wordOrder>lo-endian</wordOrder>
    <textEncoding>Mac</textEncoding>
    <created>
      <year>2011</year> <month>9</month> <day>26</day>
      <enigmaVersion><major>17</major><devStatus>beta</devStatus></enigmaVersion>
      <application>FIN</application>
      <platform>MAC</platform>
      <appVersion><major>17</major><devStatus>rc</devStatus><build>2</build></appVersion>
    </created>
    <modified>
      <year>2019</year> <month>4</month> <day>3</day>
      <enigmaVersion><major>26</major><minor>1</minor></enigmaVersion>
      <application>FIN</application>
      <platform>WIN</platform>
    </modified>
  </headerData>
</header>
```

**Application codes**: FIN (Finale), FJR (Allegro), FMV (Music Viewer), NTR (PrintMusic),
FNP (NotePad), FQM (QuizMaker), FQT (QuizTaker), FSW (SongWriter).

### `<options>`: Global document options

Contains subelements for every category of document-wide settings:

| Element | Description |
|---------|-------------|
| `<accidentalOptions>` | Accidental spacing and positioning |
| `<barlineOptions>` | Barline widths, dash patterns, draw rules |
| `<beamOptions>` | Beam slopes, separations, stub lengths |
| `<chordOptions>` | Chord symbol display and playback |
| `<clefOptions>` | Clef definitions (18 clefs indexed 0-17), cautionary clef settings |
| `<fontOptions>` | Default fonts for all categories (music, clef, time, chord, lyric, etc.) |
| `<graceNoteOptions>` | Grace note scaling, playback duration, slash settings |
| `<keySignatureOptions>` | Key signature cancellation and display rules |
| `<lyricOptions>` | Lyric positioning, word extension, hyphen settings |
| `<noteOptions>` | Note spacing, stem lengths, cross-staff rules |
| `<repeatOptions>` | Repeat barline and ending defaults |
| `<smartShapeOptions>` | Slur, hairpin, ottava defaults |
| `<stemOptions>` | Stem connection, direction, length rules |
| `<tieOptions>` | Tie placement and curvature |
| `<timeSignatureOptions>` | Time signature display modes |
| `<tupletOptions>` | Tuplet bracket and number display |

Each font entry has `<fontID>`, `<fontSize>`, and optionally `<efx>` with `<bold/>` / `<italic/>`.

### `<others>`: Record-based data

Contains records identified by `cmper` (component number) and optionally `inci` (sub-index).

#### Measure specifications (`<measSpec>`)

```xml
<measSpec cmper="1">
  <width>168</width>
  <keySig><key>1</key></keySig>   <!-- Circle of fifths: 1 = G major -->
  <beats>4</beats>                 <!-- Time sig numerator -->
  <divbeat>1024</divbeat>          <!-- Time sig denominator in EDU -->
  <dispBeats>4</dispBeats>         <!-- Display numerator -->
  <dispDivbeat>1024</dispDivbeat>  <!-- Display denominator -->
  <posMode>timesigPlusPos</posMode>
  <barline>normal</barline>        <!-- normal, double, final, dashed, heavy, tick -->
  <leftBarline>default</leftBarline>
</measSpec>
<measSpec cmper="1" part="1" shared="true"/>  <!-- Part sharing -->
```

**Barline values**: `none`, `normal`, `double`, `dashed`, `heavy`, `final`, `tick`, `custom`.

**Additional measure elements**: `<forwardRepeat/>`, `<backwardRepeat/>`, `<lineBreak/>`,
`<pageBreak/>`, `<deltaKey/>`, `<deltaTime/>`, `<hideKeySig/>`, `<breakRest/>`.

#### Staff specifications (`<staffSpec>`)

```xml
<staffSpec cmper="1">
  <staffLines>5</staffLines>
  <lineSpace>24</lineSpace>
  <instUuid>3a3622cc-fc9a-42e0-a743-6b200dd8ce88</instUuid>
  <defaultClef>0</defaultClef>    <!-- 0=treble, 3=bass, see clef table -->
  <transposition>
    <noKeyOpt/>
    <keysig><interval>0</interval><adjust>0</adjust></keysig>
  </transposition>
  <fullName>2</fullName>           <!-- Text block reference -->
  <useAutoNum/>
</staffSpec>
```

#### Frame specifications (`<frameSpec>`)

Frames connect entries to staves and measures. Each frame describes a range of entries:

```xml
<frameSpec cmper="2" inci="0">
  <startEntry>5</startEntry>
  <endEntry>8</endEntry>
</frameSpec>
```

The `cmper` corresponds to a measure number. `inci` distinguishes layers within the
same measure.

#### GF hold (`<gfhold>`)

Links frames to staff-measure pairs (in `<details>` section):

```xml
<gfhold cmper1="32767" cmper2="1">
  <clefID>0</clefID>
  <clefPercent>75</clefPercent>
  <frame1>1</frame1>              <!-- Reference to frameSpec cmper -->
</gfhold>
```

Here `cmper1` is the staff number (32767 for the default staff list) and `cmper2` is the
measure number.

#### Articulation definitions (`<articDef>`)

```xml
<articDef cmper="1">
  <charMain>46</charMain>         <!-- Character code in music font -->
  <sizeMain>24</sizeMain>
  <autoHorz/> <autoVert/>
  <autoVertMode>autoNoteStem</autoVertMode>
  <insideSlur/>
  <playArtic/>
  <durTopNotePercent>40</durTopNotePercent>
</articDef>
```

#### Text expression definitions (`<textExprDef>`)

```xml
<textExprDef cmper="1">
  <categoryID>1</categoryID>
  <descStr>Pianissimo</descStr>
  <value>28</value>
  <playType>velocity</playType>
  <isHidden/>
</textExprDef>
```

#### Other record types in `<others>`

| Element | Description |
|---------|-------------|
| `<instDef>` | Instrument definitions (MIDI channel, patch, volume) |
| `<fontInfo>` | Font name/ID mappings |
| `<smartShape>` | Smart shapes (slurs, hairpins, ottava, glissando) |
| `<chordSuffix>` | Chord suffix definitions |
| `<endingRepeat>` | Volta bracket definitions |
| `<backwardRepeat>` | Backward repeat bar definitions |
| `<textRepeat>` | Text repeats (D.C., D.S., Coda, Fine) |
| `<tempo>` | Tempo marking definitions |
| `<shapeDef>` / `<shapeData>` | Custom shape definitions |
| `<staffSystemSpec>` | System layout (top, left, right, bottom, measure range) |
| `<pageSpec>` | Page dimensions and margins |
| `<multiStaffInstGroup>` | Multi-staff instrument groups |
| `<partDef>` | Part extraction definitions |
| `<percNoteInfo>` | Percussion note mappings |
| `<fretGroup>` / `<fretboard>` | Fretboard diagram definitions |

### `<details>`: Measure-staff detail records

Addressed by `cmper1` (staff), `cmper2` (measure), and optionally `inci`:

| Element | Description |
|---------|-------------|
| `<gfhold>` | Frame holder: connects staff+measure to an entry frame |
| `<groupSpec>` | Bracket/brace groups (start staff, end staff, type) |
| `<chord>` | Chord symbol (root, suffix, alt bass, position) |
| `<baselinesChords>` | Chord baseline position |
| `<baselinesExprAboveStaff>` | Expression above-staff baseline |
| `<baselinesExprBelowStaff>` | Expression below-staff baseline |
| `<baselinesFingerboards>` | Fretboard baseline position |
| `<baselinesLyricsVerse>` | Verse lyric baselines (per lyric number) |
| `<baselinesLyricsChorus>` | Chorus lyric baselines |
| `<baselinesLyricsSection>` | Section lyric baselines |
| `<midiExpr>` | MIDI expression data |

### `<entries>`: Music entry pool

Entries form a **linked list** via `prev`/`next` attributes. Each entry represents a
note event or rest at a point in time:

```xml
<entry entnum="5" prev="4" next="6">
  <dura>1024</dura>               <!-- Duration in EDU (1024 = quarter note) -->
  <numNotes>1</numNotes>
  <isValid/>
  <isNote/>                        <!-- Present for notes, absent for rests -->
  <sorted/>
  <note id="1">
    <harmLev>6</harmLev>          <!-- Harmonic level (TCD pitch) -->
    <harmAlt>0</harmAlt>          <!-- Chromatic alteration -->
    <isValid/>
  </note>
</entry>
```

#### Entry boolean flags (presence = true)

| Flag | Description |
|------|-------------|
| `<isValid/>` | Entry is valid |
| `<isNote/>` | Is a note (absent = rest) |
| `<sorted/>` | Entry is sorted |
| `<graceNote/>` | Grace note |
| `<floatRest/>` | Floating rest |
| `<freezeStem/>` | Frozen stem direction |
| `<upstem/>` | Stem direction up |
| `<crossStaff/>` | Cross-staff entry |
| `<voice2/>` | Voice 2 |
| `<tupletStart/>` | Tuplet start marker |

#### Entry data elements

| Element | Type | Description |
|---------|------|-------------|
| `<dura>` | int | Duration in EDU |
| `<numNotes>` | int | Number of notes in chord (0 for rests) |
| `<note id="N">` | element | Note within the entry (1-based ID, up to 12) |

#### Note elements

| Element | Type | Description |
|---------|------|-------------|
| `<harmLev>` | int | Harmonic level (TCD diatonic displacement) |
| `<harmAlt>` | int | Chromatic alteration (positive = sharp) |
| `<isValid/>` | flag | Note is valid |
| `<tieStart/>` | flag | Tie starts here |
| `<tieEnd/>` | flag | Tie ends here |
| `<showAccidental/>` | flag | Show accidental |
| `<parenAccidental/>` | flag | Parenthesized accidental |

### `<texts>`: Text content

Contains all text data referenced by other records:

```xml
<!-- File information -->
<fileInfo type="title">Auld Lang Syne</fileInfo>
<fileInfo type="composer">Traditional, Scottish</fileInfo>
<fileInfo type="copyright">&#169;</fileInfo>

<!-- Text blocks (referenced by textExprDef, staffSpec, etc.) -->
<blockText number="14">^fontid(9)^size(24)^nfx(0)^title()</blockText>

<!-- Smart shape text -->
<smartShapeText number="1">^fontid(1)^size(12)^nfx(2)Glissando</smartShapeText>

<!-- Expression text -->
<expression number="1">^fontMus(Maestro,0)^size(24)^nfx(0)p</expression>

<!-- Lyrics -->
<verseLyric number="1">syllable text here</verseLyric>
```

**Text formatting commands** use caret notation:
- `^fontid(N)`: switch to font by ID
- `^fontTxt(Name,ID)`: switch to named text font
- `^fontMus(Name,ID)`: switch to named music font
- `^size(N)`: font size
- `^nfx(N)`: font effects (0=plain, 1=bold, 2=italic, 3=bold+italic)
- `^title()`, `^composer()`, `^copyright()`: insert file info fields
- `^page(N)`: page number
- `^partname()`: part name insert
- `^baseline(N)`: vertical baseline adjustment
- `^rehearsal()`: auto-numbered rehearsal mark


## Data Model

The relationship between elements follows this hierarchy:

```
Score
  +-- staffSpec[cmper]        : staff definition (clef, lines, transposition, UUID)
  +-- measSpec[cmper]         : measure definition (key, time, barline, width)
  +-- gfhold[staff, measure]  : links a staff+measure to an entry frame
  +-- frameSpec[cmper, inci]  : identifies a range of entries (startEntry..endEntry)
  +-- entry[entnum]           : linked list of note/rest events
       +-- note[id]           : individual pitch within a chord (up to 12)
```

### Traversal

To read the music for staff S, measure M:
1. Find `<gfhold cmper1="S" cmper2="M">` to get the frame reference
2. Look up `<frameSpec cmper="M">` to get `startEntry` and `endEntry`
3. Walk the linked list from `startEntry` through `next` pointers until `endEntry`
4. Each entry gives duration, and each note within gives pitch


## Duration System (EDU)

Durations use **EDU** (Enigma Duration Units): **1024 EDU per quarter note**.

| EDU | Note Type | EDU | Note Type |
|-----|-----------|-----|-----------|
| 4 | 1024th | 512 | Eighth |
| 8 | 512th | 1024 | Quarter |
| 16 | 256th | 2048 | Half |
| 32 | 128th | 4096 | Whole |
| 64 | 64th | 8192 | Breve |
| 128 | 32nd | 16384 | Long |
| 256 | 16th | | |

Dotted durations: base EDU * 1.5 (single dot), * 1.75 (double dot), etc.

**Beam counts** by EDU: 512=1 beam, 256=2, 128=3, 64=4, 32=5, 16=6, 8=7, 4=8.

**Duration tolerance (SLACK)**: 48 EDU. Durations within 48 EDU of a standard value
may be rounded to that value.


## Pitch Encoding (TCD)

Pitches use **TCD** (Tone Center Displacement): a diatonic displacement from a reference
point, with separate chromatic alteration.

- **Reference point**: `harmLev = 0` is **C4** (middle C, MIDI 60)
- `harmLev` (in EnigmaXML) / `tcd` (in binary model): diatonic steps from middle C
- `harmAlt` / `alteration`: chromatic alteration **relative to the key signature**
  (positive = raise, negative = lower). A note that belongs to the key signature
  naturally has `harmAlt = 0`; accidentals outside the key carry a non-zero value.
- Step mapping: `step = harmLev mod 7` (C=0, D=1, E=2, F=3, G=4, A=5, B=6).
  For negative `harmLev`, the modulus wraps: -1 = B below middle C.
- Octave: `octave = 4 + (harmLev - step) / 7`
- MusicXML `alter` = key-signature accidental for the step + `harmAlt`
- Chromatic values: C=0, D=2, E=4, F=5, G=7, A=9, B=11

### Pitch conversion tables

```
stepToXml    = [C, D, E, F, G, A, B]
midiTcd      = [0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6]
midiAlter    = [0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0]
stepToFinale = {A=5, B=6, C=0, D=1, E=2, F=3, G=4}
stepToChromatic = {A=9, B=11, C=0, D=2, E=4, F=5, G=7}
```


## Key Signature Encoding

Stored as circle-of-fifths value in `<measSpec>` / `<keySig>` / `<key>`:

| Value | Key (major) | Value | Key (major) |
|-------|-------------|-------|-------------|
| -7 | Cb | 1 | G |
| -6 | Gb | 2 | D |
| -5 | Db | 3 | A |
| -4 | Ab | 4 | E |
| -3 | Eb | 5 | B |
| -2 | Bb | 6 | F# |
| -1 | F | 7 | C# |
| 0 | C | | |

**Tonic tables** (for resolving major/minor):
```
majorSharpTonics = [C, G, D, A, E, B, F]
minorSharpTonics = [A, E, B, F, C, G, D]
majorFlatTonics  = [C, F, Bb, Eb, Ab, Db, Gb]
minorFlatTonics  = [A, D, G, C, F, Bb, Eb]
```


## Clef System

18 clefs indexed 0-17:

| Index | Sign | Line | Oct | Description |
|-------|------|------|-----|-------------|
| 0 | G | 2 | 0 | Treble |
| 1 | C | 3 | 0 | Alto |
| 2 | C | 4 | 0 | Tenor |
| 3 | F | 4 | 0 | Bass |
| 4 | perc | 0 | 0 | Percussion |
| 5 | G | 2 | -1 | Treble 8vb |
| 6 | F | 4 | -1 | Bass 8vb |
| 7 | F | 3 | 0 | Baritone F |
| 8 | G | 1 | 0 | French violin |
| 9 | C | 5 | 0 | Baritone C |
| 10 | C | 2 | 0 | Mezzo-soprano |
| 11 | C | 1 | 0 | Soprano |
| 12 | perc | 0 | 0 | Percussion (alt) |
| 13 | G | 2 | 1 | Treble 8va |
| 14 | F | 4 | 1 | Bass 8va |
| 15 | none | 0 | 0 | Blank/hidden |
| 16 | TAB | 0 | 0 | Tablature |
| 17 | TAB | 0 | 0 | Tablature (alt) |

The `<defaultClef>` element in `<staffSpec>` references this index.


## Smart Shape Types

| Code | Type | Code | Type |
|------|------|------|------|
| 0 | Slur (down) | 14 | Trill |
| 1 | Slur (up) | 15 | Slur (auto) |
| 2 | Decrescendo | 25 | Glissando |
| 3 | Crescendo | 38 | Lyric hyphen |
| 4 | Octave down | 39 | Word extension |
| 5 | Octave up | | |


## Accidental Encoding

```
alterToAccidental = [triple-flat, flat-flat, flat, natural, sharp, double-sharp, triple-sharp]
```

Array index = alteration value + 3 (so alteration -3 = triple-flat, 0 = natural, +2 = double-sharp).


## Harmony / Chord Data

```
defaultKindText = {augmented=aug, diminished=dim, dominant=7, major=(empty),
                   minor=m, major-seventh=maj7, ...}
stepToFretboard = {A=0, B=2, C=3, D=5, E=7, F=8, G=10}
```


## File Type Constants

| Value | Name | Description |
|-------|------|-------------|
| 1 | BINARY | .mus binary format |
| 5 | ETF | Enigma Transportable File (text-based) |
| 8 | TEMPLATE | Template file |
| 11 | BINARY_OLD | Old binary format |
| 12 | ETF_OLD | Old ETF format |
| 32 | MUSICXML | MusicXML export |


## Binary Model (in-memory representation)

The sections below document the in-memory data structures used by the Java plugin bridge.
These map to the EnigmaXML elements above but use compact binary field layouts.

### Note (individual pitch within an entry)

```
Note
  exnote     : long       -- Extended note pointer
  tcd        : short      -- Tone Center Displacement (= harmLev in XML)
  alteration : short      -- Chromatic alteration (= harmAlt in XML)
  noteID     : short      -- Note ID within entry (1-based, 31 = REST)
  flag       : int        -- Bit flags (32 bits)
```

**Note flags** (32-bit `flag` field):
| Bit | Mask | Name | Description |
|-----|------|------|-------------|
| 31 | 0x80000000 | SET_BIT | Note is set/valid |
| 30 | 0x40000000 | TS_BIT | Tie start |
| 29 | 0x20000000 | TE_BIT | Tie end |
| 28 | 0x10000000 | CROSS_BIT | Cross-staff note |
| 27 | 0x08000000 | UP_SEC_BIT | Upstem second |
| 26 | 0x04000000 | DW_SEC_BIT | Downstem second |
| 25 | 0x02000000 | UP_SP_BIT | Up split |
| 24 | 0x01000000 | ACCI_BIT | Has accidental |
| 23 | 0x00800000 | PAREN_ACCI | Parenthesized accidental |
| 22 | 0x00400000 | NO_PLAYBACK | No playback |
| 21 | 0x00200000 | NO_SPACING | No spacing contribution |
| 4 | 0x00000010 | HIDE_NOTE | Hidden note |
| 3 | 0x00000008 | LEFT_TREMOLO | Left tremolo |
| 1 | 0x00000002 | FREEZE_ACCI | Frozen accidental position |
| 0 | 0x00000001 | RIGHT_TREMOLO | Right tremolo |


### Entry (a note/rest event, may contain up to 12 notes for chords)

```
Entry
  exentry   : long       -- Extended entry pointer
  entnum    : int        -- Entry number
  dura      : short      -- Duration value in EDU
  orgposi   : short      -- Original position in measure
  ef        : int        -- Entry flags (32 bits)
  xef       : short      -- Extended entry flags (16 bits)
  numNotes  : short      -- Number of notes (0-12, max TGFNN=12)
  notes     : Note[12]   -- Array of notes
  elldur    : int        -- Elapsed duration
  actdur    : int        -- Actual duration
  clef      : short      -- Clef at this entry
```

**Entry flags** (`ef`, 32-bit):
| Bit | Mask | Name | Description |
|-----|------|------|-------------|
| 31 | 0x80000000 | VALID | Entry is valid |
| 30 | 0x40000000 | NOTE_BIT | Is a note (vs rest) |
| 29 | 0x20000000 | V1_CNTLR_BIT | Voice 1 controller |
| 28 | 0x10000000 | V2_BIT | Voice 2 |
| 27 | 0x08000000 | CREATED_BY_HP | Created by Human Playback |
| 25 | 0x02000000 | PLAY_DISABLED_BY_HP | Playback disabled by HP |
| 24 | 0x01000000 | FLOAT_REST | Floating rest |
| 23 | 0x00800000 | GRACE_NOTE | Grace note |
| 22 | 0x00400000 | NOTE_DETAIL | Has note details |
| 21 | 0x00200000 | IMRK_DETAIL | Has articulation |
| 20 | 0x00100000 | LYRIC_DETAIL | Has lyrics |
| 19 | 0x00080000 | TUPL_START | Tuplet start |
| 18 | 0x00040000 | SPLIT_REST | Split rest |
| 17 | 0x00020000 | PRFM_DATA | Performance data |
| 16 | 0x00010000 | TREMOLO | Tremolo |
| 15 | 0x00008000 | IGNORE_BIT | Ignored entry |
| 14 | 0x00004000 | BMEXT_DETAIL | Beam extend detail |
| 13 | 0x00002000 | FLIP_TIE | Tie flipped |
| 12 | 0x00001000 | DOT_TIE_ALTS | Dot/tie alternates |
| 11 | 0x00000800 | BEAM_BIT | Beaming info |
| 10 | 0x00000400 | SEC_BEAM_BIT | Secondary beam |
| 8 | 0x00000100 | FREEZE_STEM | Frozen stem direction |
| 7 | 0x00000080 | STEM_DETAIL | Has stem detail |
| 6 | 0x00000040 | CROSS_STAFF | Cross-staff entry |
| 4 | 0x00000010 | REV_UPSTEM_BIT | Reverse upstem |
| 3 | 0x00000008 | REV_DWSTEM_BIT | Reverse downstem |
| 2 | 0x00000004 | DBL_STEM_BIT | Double stem |
| 1 | 0x00000002 | SPLIT_BIT | Split stem |
| 0 | 0x00000001 | UPSTEM_BIT | Upstem direction |

Voices: bits 29-28 encode 4 voice layers (V1=00, V2=01, V1ctrl=10, V2alt=11).

**Extended entry flags** (`xef`, 16-bit):
| Bit | Mask | Name | Description |
|-----|------|------|-------------|
| 14 | 0x4000 | GRACE_KEEP_WITH_PRIOR | Grace note keeps with prior |
| 12 | 0x1000 | FREEZE_BEAM | Frozen beam |
| 11 | 0x0800 | NO_SPACING | No spacing |
| 10 | 0x0400 | NO_PLAYBACK | No playback |
| 8 | 0x0100 | SLASH_GRACE | Slashed grace note |
| 7 | 0x0080 | ENTRY_IS_SORTED | Entry is sorted |
| 6 | 0x0040 | NO_LEGER | No ledger lines |
| 5 | 0x0020 | SMART_SHAPE_DETAIL | Has smart shape |
| 3 | 0x0008 | IN_TUPLET | Inside a tuplet |


### MeasureSpec (binary layout)

```
MeasureSpec
  measpace         : short   -- Measure spacing
  key              : short   -- Key signature (circle of fifths)
  beats            : short   -- Time signature numerator
  divBeat          : short   -- Time signature denominator in EDU
  auxflag          : short   -- Auxiliary flags
  meflag           : short   -- Measure flags (barline + state bits)
  dispBeats        : short   -- Display time sig numerator
  dispDivBeat      : short   -- Display time sig denominator
  newflag          : short   -- Additional flags
  customBarShape   : short   -- Custom barline shape ID
  customLeftBarShape: short  -- Custom left barline shape ID
  frontSpaceExtra  : short   -- Extra front spacing
  backSpaceExtra   : short   -- Extra back spacing
  keyless          : boolean -- Atonal/keyless
```


### Tgf (Time Group Frame)

The container for music data within a single staff, measure, and layer:

```
Tgf
  ptr        : long               -- Native pointer
  iuList     : short              -- IU list reference
  staffSlot  : short              -- Staff slot index
  inst       : short              -- Instrument index
  meas       : short              -- Measure number
  layer      : short              -- Layer (1-4)
  entries    : ArrayList<Entry>   -- List of note/rest events
  numEntries : int                -- Number of entries
  tpInfo     : ArrayList<TupletInfo> -- Tuplet info list
  numTuplets : int                -- Number of tuplets
```


## Data Tag System (Enigma Tags)

The binary format is organized as a tagged data store. Each data record is identified by an
**Extag** (extended tag), which combines an **Etag** (base tag type) with a 2-character code.

### Base Tag Types (Etag)

| Etag | Code | Description |
|------|------|-------------|
| OTHER | 'o','t' | Basic "other" record |
| SPECIAL_OTHER | 'S','O' | Special other record (variable size) |
| OTHER_STRING | 'o','s' | String-valued other record |
| OTHER_2INCI..44INCI | 'O',N | Other with N inci (sub-indices) |
| DETAIL | 'd','t' | Measure-staff detail record |
| SPECIAL_DETAIL | 'S','D' | Special detail record |
| DETAIL_2INCI..21INCI | 'D',N | Detail with N inci |
| ENTRY_DETAIL | 'e','d' | Note entry detail record |
| SPECIAL_ENTRY_DETAIL | 'S','d' | Special entry detail |
| ENTRY_DETAIL_2INCI..3INCI | 'E',N | Entry detail with N inci |
| TEXT | 't','x' | Text content record |
| PREFS | 'p','f' | Preferences record |
| GLOBALS | 'g','b' | Global settings record |

Each record is addressed by a **cmper** (component number) and optionally an **inci** (sub-index).

### Extended Tags (Extag): Complete Registry

#### Other Records (OT_*)
| Tag | Code | Description |
|-----|------|-------------|
| OT_ALLOTMENT | 'D','A' | Measure spacing allotment |
| OT_ARTICULATION_DEFINITION | 'I','X' | Articulation definitions (4 inci) |
| OT_BACKWARD_REPEAT | 'B','R' | Backward repeat (2 inci) |
| OT_BEAT_CHART_ELEMENT | 'B','C' | Beat chart spacing data |
| OT_CHORD_SUFFIX | 'I','V' | Chord suffix definitions |
| OT_ENDING_REPEAT | 'E','S' | Ending/volta brackets |
| OT_EXPRESSION_ASSIGN | 'D','Y' | Expression assignment to measures |
| OT_FONT_INFO | 'F','N' | Font information |
| OT_FREEZE_MEASURES | 'F','M' | Frozen measure data |
| OT_INST_DEFINITION | 'O','C' | Instrument definitions |
| OT_INST_DEFINITION_NAME | 'o','c' | Instrument name strings |
| OT_MEASURE_SPEC | 'M','S' | Measure specification (key, time, barline) |
| OT_MULTI_STAFF_INST_GROUP | 'M','G' | Multi-staff instrument groups |
| OT_PAGE_SPEC | 'P','S' | Page specification (layout) |
| OT_SHAPE | 'S','D' | Shape definitions |
| OT_SMART_SHAPE_2004 | 'S','x' | Smart shapes (slurs, hairpins, 8 inci) |
| OT_STAFF_SPEC | 'I','S' | Staff specification |
| OT_STAFF_STYLE_2010 | 'S','Y' | Staff styles |
| OT_STAFF_SYSTEM_SPEC | 'S','S' | Staff system specification |
| OT_TEMPO | 'A','C' | Tempo data |
| OT_TEXT_BLOCK | 'T','X' | Text block definitions |
| OT_TEXT_EXPRESSION | 'D','T' | Text expression definitions |
| OT_TEXT_REPEAT_2005 | 'R','U' | Text repeats (D.C., D.S., etc.) |

#### Detail Records (DT_*)
| Tag | Code | Description |
|-----|------|-------------|
| DT_BASELINE_* | various | Baseline positions (chords, expressions, lyrics, fretboards) |
| DT_CHORD_2010 | 'C','h' | Chord symbols |
| DT_GFHOLD | 'G','F' | GF hold data |
| DT_GROUP_SPEC | 'N','G' | Group specifications (brackets) |
| DT_MEASURE_FLOAT_2014 | 'F','L' | Floating measure data |
| DT_MEASURE_TEXT | 'm','t' | Measure-attached text |
| DT_MIDI_EXPRESSION | 'M','E' | MIDI expression data |

#### Entry Detail Records (ED_*)
| Tag | Code | Description |
|-----|------|-------------|
| ED_ACCIDENTAL_MODS | 'A','h' | Accidental modifications (2 inci) |
| ED_ALTER | 'A','S' | Alterations |
| ED_ARTICULATION | 'I','M' | Articulation assignments |
| ED_CROSS_STAFF | 'C','D' | Cross-staff note data |
| ED_DOT_MODS | 'd','o' | Dot position modifications |
| ED_LYRIC_ENTRY_INFO | 'L','E' | Lyric entry information |
| ED_SMART_SHAPE_ENT_MARK | various | Entry-attached smart shape marks |
| ED_STEM_MODS | 'S','T' | Stem modifications |
| ED_TIE_MODS_START/END | 'T','s'/'T','e' | Tie modifications (3 inci) |
| ED_TUPLET | 'T','P' | Tuplet data (3 inci) |
| ED_VERSE/SECTION/CHORUS_LYRIC | various | Lyric assignments |

#### Text Records (TX_*)
| Tag | Code | Description |
|-----|------|-------------|
| TX_TEXT_BLOCK | 'b','l' | Text block content |
| TX_VERSE_LYRIC | 'v','e' | Verse lyric text |
| TX_SECTION_LYRIC | 's','e' | Section lyric text |
| TX_CHORUS_LYRIC | 'c','h' | Chorus lyric text |
| TX_SMART_SHAPE | 's','s' | Smart shape text |
| TX_TEXT_EXPRESSION | 'x','p' | Text expression content |
| TX_FILE_INFO | 'f','i' | File information text |


## Native Bridge (Pdk)

The Java plugin bridge (`Pdk`) communicates with the running Finale engine through JNI
native methods. This is how MakeMusic's own MusicXML export works (it requires Finale
running). Key native methods:

| Method | Purpose |
|--------|---------|
| `loadOther(Other, cmper, inci)` | Load an Other record |
| `loadDetail(Detail, cmper1, cmper2, inci)` | Load a Detail record |
| `loadEntryDetail(EntryDetail, entnum, inci)` | Load an Entry Detail |
| `loadRawText(RawText, cmper)` | Load text content |
| `loadPrefs(Prefs, cmper)` | Load preferences |
| `loadGlobals(Globals, cmper)` | Load global settings |
| `loadHeader(Header)` | Load file header |
| `loadTgf(Tgf, staff, meas, layer, iuList)` | Load music data for one frame |
| `getEntry(Entry, Tgf, index)` | Get entry by index |
| `getNote(Note, Entry, noteIdx)` | Get note from entry |
| `getTupletInfo(TupletInfo, Tgf, index)` | Get tuplet info |
| `openDocument(path)` | Open a Finale document |
| `countInstruments(iuList)` | Count instruments |
| `countItems(iuList)` | Count measures |

A standalone reader (without Finale) does not need the Pdk bridge. It can decrypt
`score.dat` directly and parse the resulting EnigmaXML.


## Key Insights for Implementation

1. **The format is fully decodable**: decrypt `score.dat` with the LCG-based XOR cipher,
   gunzip, and parse the resulting EnigmaXML.

2. **EnigmaXML is well-structured XML**: the format uses readable element names and
   attributes, making it straightforward to parse with standard XML libraries.

3. **Music data is organized as**: Staff -> Measure -> Frame (via gfhold) -> Entry -> Note.

4. **Durations are encoded as EDU** (Enigma Duration Units): 1024 EDU per quarter note.

5. **Pitch uses TCD** (Tone Center Displacement): `harmLev` is diatonic steps,
   `harmAlt` is chromatic alteration. Together they define the absolute pitch.

6. **Entries are a linked list**: `prev`/`next` attributes chain entries within each frame.

7. **Boolean flags use presence elements**: `<isValid/>` means true, absence means false.
   This is different from the binary model which uses bit fields.

8. **Text uses caret formatting**: `^fontid(N)^size(N)^nfx(N)text` encodes rich text
   with font, size, and style changes inline.

9. **The format has evolved over 30+ years**: many element names include version
   indicators. The `<mappings>` section handles forward migration.

10. **Community resources**: the `musxdom` C++ library (by Robert Patterson) and the
    `musx2mxl` Python tool both parse this format. The `finale-lua/lua-scripts` project
    documents the decryption algorithm. The EnigmaXML format documentation effort
    continues in the Project-Attacca repository.
