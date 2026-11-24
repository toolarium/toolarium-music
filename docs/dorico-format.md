# Dorico .dorico File Format: Reverse-Engineered Documentation

Documented from analysis of Dorico project files and the Dorico application binary,
cross-checked against community findings for Dorico 6.
Dorico is a product of Steinberg Media Technologies GmbH (Yamaha). "Dorico" and "Steinberg"
are trademarks of their respective owners.

> **Disclaimer:** This document is a best-effort reconstruction for interoperability purposes.
> It is not an official specification and may contain inaccuracies or omissions.
> No guarantee of correctness or completeness is made. Steinberg has stated that it has no
> plans to publish details of the internal file format and that the format changes with almost
> every release. The analysis below was done by inspecting project files and the strings
> embedded in the executable; no code was decompiled.

> **Confidence markers:** statements marked **(verified)** were confirmed by a byte-exact
> round trip over all sample files. Statements marked **(inferred)** are derived from naming
> or a small number of observations and should be re-checked against more samples.


## Overview

| Property | Value |
|----------|-------|
| Extension | `.dorico` |
| Container | ZIP (deflate), OASIS/ODF-style `META-INF/container.xml` |
| Media type | `application/vnd.steinberg.dorico-project` |
| Main payload | `score.dtn` (score) and `scorelibrary.dtn` (library entities) |
| Payload encoding | Generic **binary tree** ("Steam tree") or, alternatively, the same tree as XML |
| Encryption | **None** |
| Compression inside `.dtn` | **None** (only the ZIP deflate of the container) |
| Internal code name | `Steam` (C++ namespace `Steinberg::Steam`, IO structs in `Steinberg::Steam::IO`) |

Key properties of the format:

1. A `.dorico` file is a **plain ZIP archive**. Renaming it to `.zip` and extracting it works.
2. `score.dtn` is **not encrypted**, despite reports on the user forum. It is a compact binary
   serialisation of a named tree: string table of element names, string table of values, and a
   flat array of nodes. **(verified)**
3. The tree has the **same shape as an XML document**: every node has a name, a text value, and
   optionally ordered children. Steinberg ships its factory libraries (for example
   `scorelibrary.xml`, `instruments.xml` in the installation folder) in the XML rendering of
   exactly this tree.
4. Dorico stores the **semantic music model**, not the engraved result. Bars, rests, beams,
   ties across barlines, stem directions and page layout are calculated at load time and are
   only stored when the user overrides the automatic result.


## Container Format

### ZIP entries (Dorico 4.x and later)

| Entry | Type | Description |
|-------|------|-------------|
| `META-INF/container.xml` | XML | Points to `score.dtn` as the root file |
| `score.dtn` | binary tree | The score: players, flows, events, layouts, options, playback data |
| `scorelibrary.dtn` | binary tree | Library entities used by the project (styles, instruments, clefs, noteheads, ...) |
| `supplementary_data/workspace/workspace.xml` | XML | Window/tab state (mode, zoom, scroll position) |
| `supplementary_data/preview/preview.pdf` | PDF | Preview of the first page |
| `supplementary_data/preview/preview.png` | PNG | Thumbnail of the first page |
| `supplementary_data/vstaudioengine/enginedata` | chunked binary | VST audio engine (mixer, routing, plug-in states), see below |
| `supplementary_data/virtualinstrument/drumPads.xml` | XML | Drum pad mapping (usually an empty XML document) |
| `supplementary_data/graphics/graphicManifest.xml` | XML | Manifest of embedded graphics (usually an empty XML document) |
| `supplementary_data/graphics/*` | images | Imported graphics referenced by graphic frames |
| `supplementary_data/scorelibraryresources/` | folder | Resources referenced by library entities (often empty) |
| `supplementary_data/guidedtutorial/*` | XML, GIF, PNG | Only in the bundled guided-tutorial projects |

Additional entries reported for Dorico 6 projects:

| Entry | Type | Description |
|-------|------|-------------|
| `supplementary_data/scoreinfo/scoreinfo.xml` | XML | Plain project info (title, composer, created and last saved Dorico version) |
| proofing filters | XML | Saved proofing/filter settings |

Notes:

- There is **no `mimetype` entry** (unlike ODF, MXL and Finale `.musx`).
- Directory entries (`META-INF/`, `supplementary_data/...`) are stored explicitly.
- Files that went through macOS Finder may contain `__MACOSX/` and `.DS_Store` entries. A
  reader must ignore them.

### container.xml

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
	<rootfiles>
		<rootfile full-path="score.dtn" media-type="application/vnd.steinberg.dorico-project"/>
	</rootfiles>
</container>
```

The namespace is the OASIS OpenDocument container namespace, the same one used by MXL. A
generic MXL-style container parser can locate the root file.

### Format detection

| Check | Result |
|-------|--------|
| First bytes | `50 4B 03 04` (ZIP local file header) |
| `META-INF/container.xml` | rootfile `media-type="application/vnd.steinberg.dorico-project"` |
| `score.dtn` first 8 bytes | `04 00 00 00 01 00 00 00` (binary encoding) or `<?xml` (XML encoding) |
| First name in the name table | `kScore` (for `scorelibrary.dtn`: `kScoreLibrary`) |


## .dtn Binary Tree Encoding

All observed `.dtn` files (Dorico 4.0 saved files and Dorico 6.0 to 6.2 files reported by the
community) use the same layout.

### Layout

```
offset  type         field
------  -----------  ---------------------------------------------------------------
0       u32 LE       magic1 = 4
4       u32 LE       magic2 = 1
8       u32 LE       nameCount
12      char[]       nameCount NUL-terminated UTF-8 strings (element names)
...     u32 LE       valueCount
...     char[]       valueCount NUL-terminated UTF-8 strings (values); value[0] = ""
...     u32 LE       nodeCount
...     Node[]       nodeCount node records; node 0 is the root
```

Each node record:

```
u8      flags            bit 1 (0x02): record has a child list
                         bit 0 (0x01): node is an array (list) node
varint  nameIndex        index into the name table
varint  valueIndex       index into the value table (0 = empty string)
if (flags & 0x02):
  varint  childCount
  varint  childNodeIndex[childCount]
```

`varint` is unsigned **LEB128** (7 bits per byte, least significant group first, high bit set
on every byte except the last). **(verified)**

### Flag values

| flags | XML rendering | Meaning | Count in a typical score |
|-------|---------------|---------|--------------------------|
| `0` | `<name>value</name>` | Leaf (or an empty object) | ~43 000 |
| `1` | `<name array="true"/>` | **Empty** array (no child list stored) | ~2 200 |
| `2` | `<name>...children...</name>` | Object with children | ~6 500 |
| `3` | `<name array="true">...</name>` | Array with children | ~1 500 |

Only the two low bits carry meaning: bit `0x01` marks an array and bit `0x02` marks a stored
child list. Files saved by Steinberg's own tutorials use exactly the four values above, but
projects from other installations set the **upper six bits to a constant per file** (observed
`0x08`, `0x1c`, `0x20`, `0x28`, `0x74`, `0xa0`, `0xbc`, `0xc0`, `0xcc`, `0xfc`, for example `0xa2`
instead of `2`). The constant differs between `score.dtn` and `scorelibrary.dtn` of the same
project and looks like uninitialised padding. A reader must mask the flag byte with `0x03`.
**(verified: 12 files of 6 public projects, fileVersion 1.1392 to 1.1452, all decode to the last
byte with this rule)** Container nodes (flags 2 and 3) always have `valueIndex = 0`. Array elements are child nodes whose name is the element type (for example
`NoteEventDefinition`, `scorePlayer`, `entry`), or the generic name `array`. **(verified)**

### Writer rules (byte-exact reproduction)

Re-encoding a decoded tree with the following rules reproduces every sample file byte for byte
(16 of 16 files). **(verified)**

1. **Node numbering** is **pre-order depth-first**: the root is 0, then each child subtree in
   order. Child indices therefore always point forward.
2. **Name table** order is order of first occurrence in pre-order traversal.
3. **Value table** is pre-seeded with `""` at index 0; the remaining values are in order of
   first occurrence in pre-order traversal. Each distinct value is stored once.
4. Header constants `4` and `1` are written unchanged.

Every node has exactly one parent and there are no orphan nodes, so the structure is a strict
tree (no shared sub-objects). **(verified)**

### Decoder (pseudo code)

```
readTree(bytes):
    p = 0
    magic1 = u32(p); magic2 = u32(p)            // 4, 1
    names  = readStringTable(p)                  // u32 count + NUL-terminated strings
    values = readStringTable(p)
    nodeCount = u32(p)
    nodes = []
    repeat nodeCount:
        flags = u8(p)
        name  = names[varint(p)]
        value = values[varint(p)]
        children = []
        if flags & 2:
            repeat varint(p): children.add(varint(p))
        nodes.add(Node(name, value, isArray = flags & 1, children))
    assert p == bytes.length
    return nodes[0]
```

### XML encoding

Dorico can read the same tree from XML. The executable contains `XMLFormatStreamReader` and
`XMLFormatStreamWriter` classes and a hidden developer preference `kSaveScoresInXMLFormat`.
Factory library files in the installation folder use this encoding, and the community reports
that example projects installed with Dorico 6.1.5 contain `score.xml`/`scorelibrary.xml`
instead of `.dtn` files.

Mapping:

| Binary | XML |
|--------|-----|
| node name | element name |
| node value | element text |
| flags bit 0 | attribute `array="true"` (the only attribute used) |
| empty leaf | `<parentEntityID/>` |

A reader should accept both encodings: check whether the payload starts with `<?xml`.

### Value syntax

Values are always strings. Their interpretation depends on the element:

| Form | Example | Used for |
|------|---------|----------|
| integer | `76`, `-1` | IDs, pitches, counts |
| rational | `3/2`, `27/4`, `0` | positions and durations (in quarter notes) |
| decimal | `100.000000` | scale factors, sizes |
| boolean | `true`, `false` | flags |
| enum | `kKeySigMajor`, `kVoiceStream` | enumerations (always `k` prefix) |
| entity ID | `instrument.keyboard.piano`, `clef.treble` | references to library entities |
| text | `Inputting notes` | titles, names |
| timestamp | `1617288769` | Unix time in seconds (`createdDate`, `lastSavedDate`) |
| colour | `#39a8ec` | colours |
| typed variant | `string: "E"`, `int: 4`, `double: 0.2`, `bool: true`, `null: ` | property values |
| typed list | `list: {int: 0}, {int: 0}` | property values |
| variant list | `steamVariantList:list: {int: -1}, {int: -1}, {bool: false}` | compound values (tuples) |

The typed variant syntax is `<type>: <value>`. Lists nest with braces:
`steamVariantList:list: {list: {int: 8}, {int: 0}}`. Strings are double-quoted.


## Versioning

The root contains `<fileVersion>`, and `info` contains created and last-saved application
versions.

| Saved by Dorico | `fileVersion` | Source |
|-----------------|---------------|--------|
| 4.0.0 | 1.1059 / 1.1070 | sample projects |
| 4.0.5 | 1.1096 | sample projects |
| 6.0.22 | 1.1392 | community |
| 6.1.0 | 1.1416 | community |
| 6.1.5 (bundled examples, XML) | 1.1412 | community |
| 6.1.10 | 1.1417 | community |
| 6.2.0 | 1.1444 | community |
| 6.2.20 | 1.1450 | community |
| 6.2.30 | 1.1452 | community |

Observations:

- The binary container format (header `4, 1`, encoding rules) has not changed between Dorico 4
  and Dorico 6.
- Schema changes between versions are **additive**: new options and fields appear, existing
  music-bearing paths stay the same. Older Dorico versions drop data they do not understand.
- `fileVersion` should be treated as informational. A robust reader validates structure and
  ignores unknown elements.


## score.dtn: Top-Level Structure

```
kScore
├── fileVersion                     "1.1096"
├── instrumentScoreOrderEntityID    e.g. instrumentscoreorder.orchestral
├── layoutSortOrder                 kByScoreOrder
├── idGenerators                    next free ID per object kind
│     instrument, player, playerGroups, layout, flow, event, eventStream,
│     blockInstance, block, stave, frameChain, page, textFlow, eventGroup, doodle, doodleGroup
├── info                            project metadata (see below)
├── scoreOptions                    engravingOptions, noteInputOptions, playbackOptions,
│                                   printingOptions, defaultNotationOptionsForFlow,
│                                   defaultFrameChainOptions, defaultLayoutOptionsMap, viewOptions
├── library                         embedded copy of the library (same schema as kScoreLibrary)
├── scorePlayers / array            players and their instruments
├── scorePlayerGroups / array       player groups
├── flows / array                   the music (one flow per movement/piece)
├── textFlows / array               text frames content
├── layouts / array                 full score and part layouts (page setup, frame chains)
├── playModeData                    playback engine state, instrument playback data, mixer
├── playModeDataExtra / array
└── printingModeData
```

### info (project metadata)

| Element | Example | MusicXML equivalent |
|---------|---------|---------------------|
| `title` | `Inputting notes` | `work/work-title` |
| `subtitle` | | `credit` |
| `dedication` | | `credit` |
| `composer` | | `identification/creator[@type=composer]` |
| `arranger` | | `identification/creator[@type=arranger]` |
| `lyricist` | | `identification/creator[@type=lyricist]` |
| `artist`, `copyist`, `publisher`, `editor` | | `identification/creator` |
| `copyright` | | `identification/rights` |
| `workNumber` | | `work/work-number` |
| `composerDates`, `compositionYear`, `otherInfo` | | `credit` / `miscellaneous` |
| `createdDate`, `lastSavedDate` | `1617288769` | `encoding/encoding-date` (Unix seconds) |
| `createdVersion`, `lastSavedVersion` | `4.0.0`, `4.0.5` | `encoding/software` |
| `originalSourcePath`, `filePath` | | (absolute path on the author's machine) |
| `generatePreviews` | `true` | |

Each flow has its own `flowInfo` with the same fields (title, composer, ...).

**MusicXML import of page texts.** Dorico's MusicXML import fills the project information from the credits of
the first page (title, subtitle, composer, rights) and does not show any other page text. A free text of the first
page (for example "Partitur / Aufführungsdauer" at the top left of Disney Love Medley) is lost on import, also
with the credit type `part name` or `lyricist`. **(verified with Dorico 5)**


## Players and Instruments

```
scorePlayers/array/scorePlayer
├── playerID                      0
├── soloPlayer                    true (solo) / false (section player)
├── baseName / displayName        Piano
├── baseShortName / displayShortName   Pno
├── isSoloist
└── instruments (array) / instrument
      ├── entityID                instrument.keyboard.piano      (library entity)
      ├── baseInstrumentEntityID
      ├── instrumentID            0                               (referenced by flows)
      ├── numStaves               2
      ├── transpositionVisibility kNever
      ├── instrumentColour        #39a8ec
      ├── standardRange / advancedRange   A0-C8
      ├── nameData                singular/plural, full/short names, language, gender
      ├── formattedNameData       same names as rich text paragraphs
      ├── transposition
      │     intervalForTransposingScores   steamVariantList:list: {int: 2}, {string: "kIntervalMajorOrPerfect"}
      ├── staveDefinition         numStaveLines, barlineSpan, bracketSpan, useBrace, isVocalStave
      └── clefs (array) / clef / idForTransposingLayouts   clef.treble, clef.bass
```

**Player names.** The `displayName` of a player with a transposing instrument contains the key that Dorico
adds itself, for example `Posaune (B{@flat@})` or `Bb-Trompete (B{@flat@}) 1`, and the short name is often
only that key (`(B{@flat@}) 1`). The reader removes the key, as the other notation applications choose the
instrument (and its sound) by the name: with the key, a trombone was played with a trumpet sound.
**(verified on Disney Love Medley)**

- A **player** holds one or more **instruments** (instrument changes, doubling).
- Transposing instruments carry an interval: for a B flat trumpet `{int: 2}, kIntervalMajorOrPerfect`
  means a major second. The exact transposition is in the library instrument entity (embedded in
  `kScore/library/instruments` and in `scorelibrary.dtn`), see below.
- Note pitches are stored at **sounding (concert) pitch**; the transposition is applied only for
  display. **(verified: the stored pitches of the B flat trumpet sample match the notes printed in
  its concert-pitch preview)**

Transposition data of a library instrument entity (`InstrumentEntityDefinition/pitchedInstrumentData`):

| Field | B flat trumpet | Horn in F | Alto sax | Guitar | Piccolo | Meaning |
|-------|----------------|-----------|----------|--------|---------|---------|
| `transposedChromaticTransposition` | 2 | 7 | 9 | 12 | -12 | semitones the written pitch lies **above** the sounding pitch |
| `transposedDiatonicTransposition` | 1 | 4 | 5 | 7 | -7 | diatonic steps the written pitch lies above the sounding pitch |
| `concertOctaveTransposition` | 0 | 0 | 0 | 1 | -1 | octave shift used in concert-pitch layouts |

MusicXML `<transpose>` describes written to sounding, so both values change sign (trumpet:
`chromatic -2`, `diatonic -1`). The written key signature moves by `7 * chromatic - 12 * diatonic`
fifths (trumpet: +2, concert C major is written D major).
- `scorePlayerGroups` group players (`playerIDs`, `name`, `groupID`).


## Flows and the Event Model

A **flow** is a self-contained span of music (movement, song, exercise). It plays the role of a
MusicXML score body. Dorico has no stored measures: everything is positioned on a continuous
time line.

```
flows/array/flow
├── flowID, flowName, playerAcquisitionPolicy
├── minimumNotatedLength          length in quarter notes (24), behaviour kNotateUpToLength
├── flowPlayers/array/flowPlayer  maps players to event streams
├── elementTables/array           shared "elements" referenced by events
├── notationOptions               per-flow notation rules (beaming, accidentals, rests, ...)
├── blocks/array/block            containers of events
├── blockInstances/array          placement of blocks on streams
├── eventStreams/array            streams (global, stave, voice, player, automation)
├── flowInfo                      per-flow title/composer
├── flowVideoData, automationStreamTable, condensedStreams, editingLocks
```

### Streams, blocks and block instances

```
eventStream (streamID, eventStreamType, blockInstanceIDs)
     │  1..n
     ▼
blockInstance (blockInstanceID, blockID, streamID, position)
     │  1
     ▼
block (blockID, parentEventStreamType, events[])
```

- An **event stream** is a time line of a given type.
- A **block** holds events. A **block instance** places a block on a stream at a `position`.
  Event positions are relative to the block instance position. In all observed files there is
  exactly one block instance per stream at position 0.
- The indirection allows the same block to appear more than once (repeated material).
  **(inferred)**

Stream types (`eventStreamType`, `parentEventStreamType`):

| Enum | Content |
|------|---------|
| `kGlobalTimebaseStream` | One per flow. Time signatures, key signatures, tempo, rehearsal marks, repeats, system text, breaks |
| `kInstrumentStaveStream` | One per stave of an instrument. Clefs, octave lines, stave-attached items |
| `kVoiceStream` | One per voice. Notes, rests, grace notes, tuplets, slurs, lyrics |
| `kPlayerStream` | One per player in the flow. Dynamics, playing techniques, player-level items |
| `kAutomationStream` | Playback automation |

### Mapping streams to players, instruments, staves and voices

```
flowPlayer
├── playerID                     → scorePlayer.playerID
├── eventStreamID                → the kPlayerStream for this player
└── flowPlayerInstruments/array/flowPlayerInstrument
      ├── instrumentID           → scorePlayer.instruments[].instrumentID
      ├── staveStreamInfos/array/staveStreamInfo
      │     staveStreamID (→ kInstrumentStaveStream), nthStaveInInstrument, staveID,
      │     ossia (kNone), divisiIndex, extraStaveIndex
      └── voiceStreamInfos/array/voiceStreamInfo
            voiceStreamID (→ kVoiceStream), defaultLogicalStaveID (→ staveID),
            voiceDirection (kPreferStemsUp/...), slashType (kWithoutSlashes/...), ossia
```

To find the notes of a stave: for each `voiceStreamInfo` with `defaultLogicalStaveID == staveID`,
take its `voiceStreamID`, find the `eventStream`, follow `blockInstanceIDs` to the block, and
read the block's `events`. A note can be moved to another stave with
`logicalStaveIDOverride` (cross-staff notation; `-1` = none).

### Common event fields

Every event definition starts with these fields:

| Field | Type | Meaning |
|-------|------|---------|
| `eventID` | int | Unique within the project (`idGenerators/event` holds the next ID) |
| `position` | rational | Start, in **quarter notes** from the start of the block instance |
| `secondaryPosition` | rational | Ordering of zero-duration items at the same position (grace notes, clef before/after grace notes) **(inferred)** |
| `duration` | rational | Length in quarter notes (0 for point events) |
| `secondaryEndOffset` | rational | Secondary offset of the end point |
| `propertyTable/entries` | array | Overridden properties (see below) |

Rational positions: `0`, `1/2` (eighth), `3/2` (dotted quarter), `27/4`. There is no
divisions/ticks grid: positions are exact fractions, so triplets and other tuplets are exact
(for example `1/3`).

### Event types

The executable defines 53 event types. Events found in sample files are marked with a check.

| Event | Typical stream | Specific fields | Seen |
|-------|----------------|-----------------|------|
| `NoteEventDefinition` | voice | `logicalStaveIDOverride`, `pitch` | ✓ |
| `RestEventDefinition` | voice | `logicalStaveIDOverride` (explicit rest only) | |
| `BarRestEventDefinition` | voice | | |
| `GraceNoteEventDefinition` | voice | `pitch`, `notatedDuration`, `slashed`, `logicalStaveIDOverride` | |
| `TupletEventDefinition` | voice | `tupletDefinition` | |
| `SlurEventDefinition` | voice | `slurElementID` | ✓ |
| `LyricEventDefinition` | voice | `lyricElementID` | |
| `ArpeggioEventDefinition` | voice | `arpeggioType` | |
| `GlissandoLineEventDefinition` | voice | `fromNoteEventAndBlockInstanceIDs`, `toNoteEventAndBlockInstanceIDs` | |
| `GuitarBendEventDefinition` | voice | | |
| `TrillEventDefinition` | voice | | |
| `OrnamentEventDefinition` | voice | `entityID` | |
| `BarDivisionEventDefinition` | global | `barDivisionElementID` (time signature) | ✓ |
| `TonalityDivisionEventDefinition` | global | `tonalityDivisionElementID` (key signature) | ✓ |
| `ImmediateTempoChangeEventDefinition` | global | `data` (tempo) | ✓ |
| `GradualTempoChangeEventDefinition` | global | `data` (rit., accel.) | |
| `RehearsalMarkEventDefinition` | global | | |
| `RepeatMarkerEventDefinition` | global | `jumpTypeAndQualification`, `sectionType` | |
| `RepeatEndingLineEventDefinition` | global | | |
| `MarkerEventDefinition` | global | | |
| `BreakEventDefinition` | global | `breakType` (frame/system break) | |
| `PauseEventDefinition` | global | `pauseData` (fermata, caesura, breath mark) | |
| `RhythmicFeelEventDefinition` | global | `feelEntityID` (swing) | |
| `ClefEventDefinition` | stave | `entityIDs`, `forceAfterGraceNotes` | |
| `OttavaLineEventDefinition` | stave | `entityID` | |
| `StaveCountChangeEventDefinition` | stave | `staveCount` | |
| `DynamicGroupEventDefinition` | player | `dynamicGroupID` | |
| `PlayingTechniqueEventDefinition` | player | `entityID` | |
| `PedalLineEventDefinition` | player | `pedalLineType` | |
| `HarpPedalEventDefinition` | player | `pedalSettings` (`cPedal` ... `bPedal`) | |
| `TextEventDefinition` | player/global | `aboveOrBelow`, `isSystemText`, text paragraphs | |
| `ChordSymbolEventDefinition` | global | `chordSymbolData` | |
| `ChordSymbolInInstrumentStaveStreamEventDefinition` | stave | `chordSymbolData` | |
| `ChordSymbolVisibilityEventDefinition` | player | | |
| `FiguredBassEventDefinition` | player | `figuredBassEventData` | |
| `RomanNumeralEventDefinition` | player | | |
| `InstrumentChangeEventDefinition` | player | | |
| `CueEventDefinition` | stave | | |
| `DivisiEventDefinition` | stave | `divisiSections` | |
| `CommentEventDefinition` | any | `author`, `authorInitials`, `commentText`, `timestamp`, `inReplyTo` | |
| `HorizontalLineEventDefinition` | any | `lineStyleEntityID` | |
| `VerticalLineEventDefinition` | any | | |
| `MultiRestEventDefinition`, `SplitMultirestEventDefinition` | global | | |
| `GenerativeBarRepeatEventDefinition` | voice | `numberOfBarsToRepeat`, `barGroupingMultiple` | |
| `GenerativeSlashEventDefinition` | voice | | |
| `NoteSpacingChangeEventDefinition` | global | | |
| `BracketAndBarlineChangeEventDefinition` | global | | |
| `CondensingChangeEventDefinition`, `CondensationLabelEventDefinition` | player | | |
| `PercussionLegendEventDefinition` | stave | | |
| `AutomationEventDefinition` | automation | `automationDataPoint` | |
| `MIDITriggerEventDefinition` | player | | |

Specific field names come from the serialisation key catalogue embedded in the executable.
Their assignment to event types is based on naming and should be confirmed with samples.
**(inferred, except for rows marked as seen)**

### Element tables

Events that share data, or that need a stable identity independent of their position, refer to
an **element** in `flow/elementTables`:

| Table | Element | Referenced by |
|-------|---------|---------------|
| `BarDivisionElementTableDefinition` | `BarDivisionElementDefinition` | `barDivisionElementID` |
| `TonalityDivisionElementTableDefinition` | `TonalityDivisionElementDefinition` | `tonalityDivisionElementID` |
| `SlurElementTableDefinition` | `SlurElementDefinition` | `slurElementID` |
| `LyricElementTableDefinition` | `LyricElementDefinition` | `lyricElementID` |
| `DynamicGroupElementTableDefinition` | `DynamicGroupElementDefinition` | `dynamicGroupID` |

Each element has `elementID`, `referencingEventIDs` (back reference to the events), a
`propertyTable` and `subElements` (for example the individual dynamics of a dynamic group).


## Music Content

### Notes

```xml
<NoteEventDefinition>
  <eventID>2</eventID>
  <position>0</position>
  <secondaryPosition>0</secondaryPosition>
  <duration>3/2</duration>
  <secondaryEndOffset>0</secondaryEndOffset>
  <propertyTable>
    <entries array="true">
      <entry>
        <propertyType>kNoteSpelling</propertyType>
        <value>string: "E"</value>
        <context>
          <layoutIDWithFrameChainID>steamVariantList:list: {int: -1}, {int: -1}, {bool: false}</layoutIDWithFrameChainID>
          <blockInstanceID>-1</blockInstanceID>
          <propertyOffset>string: "0:0:1:0"</propertyOffset>
        </context>
      </entry>
    </entries>
  </propertyTable>
  <logicalStaveIDOverride>-1</logicalStaveIDOverride>
  <pitch>76</pitch>
</NoteEventDefinition>
```

**Pitch.** `pitch` is the **MIDI note number at sounding pitch** (60 = middle C). **(verified
for non-transposing instruments)**

**Spelling.** The `kNoteSpelling` property holds only the **letter name** (`"C"`, `"F"`, ...).
Octave and accidental are implied by the MIDI pitch:

```
alter  = pitch - naturalPitch(letter, octave)   // octave chosen so that |alter| <= 2
```

Examples from the samples: pitch 73 with spelling `"C"` is C sharp 5, the same pitch 73 with
spelling `"D"` is D flat 5, pitch 66 with `"G"` is G flat 4, pitch 68 with `"A"` is A flat 4.
All 35 distinct pitch/spelling pairs in the samples resolve to an alteration within one
semitone. **(verified on the samples)** In
microtonal tonality systems (`temperamentDefinitionID` other than 12-EDO) the accidental is
given by an accidental ID instead (`kNoteAccidentalOverride`). **(inferred)**

**Chords.** A chord is several `NoteEventDefinition` events in the same voice with the same
`position` and `duration`. **(verified on the piano sample)**

**Durations.** `duration` is the **played** length in quarter notes. Dorico decides at
render time how to notate it (single note, dotted note, or tied notes across beats and
barlines) based on `notationOptions/noteGroupingOptions`. A note of `duration` 5 in 4/4 is
rendered as a whole note tied to a quarter note. A reader that targets a measure-based model
(MusicXML/`IScore`) must **split notes at barlines** and insert ties itself.

**Ties.** A tie chain with the same pitch is usually a **single note event** with the full
duration. Notation of the split is controlled by `kNoteSplitDurations` and
`kNoteForcedSplitPositions`. `kNoteTiedToEvent` links a note to a separate following event (ties
between different events, for example enharmonic ties), and `kNoteEndHasLVTie` adds a laissez
vibrer tie. **(inferred)**

**Rests.** Rests are **implicit**: gaps in a voice are filled with rests automatically, and
empty bars get bar rests. `RestEventDefinition` exists only for explicit rests (for example
with a forced position via `kRestForcePositionAndDuration`). A reader must generate rests from
gaps.

**Grace notes.** `GraceNoteEventDefinition` lives in the voice stream. Its `position` is the
position of the main note and its `duration` is 0. The negative `secondaryPosition` orders the
grace notes before the main note (for example `-1/2` then `-1/4` for two grace notes); grace
notes with the same `secondaryPosition` form a chord. `notatedDuration` is the written value in
quarter notes (`1/4` = sixteenth, `1/2` = eighth), `slashed` marks an acciaccatura. Pitch and
`kNoteSpelling` work as for normal notes. **(verified on K331 and Sousa)**

**Dorico's own split.** Most notes carry `kNoteSplitDurations` (for example
`list: {string: "1/2"}, {string: "1/4"}`): the note value pieces Dorico used when it last
notated the note. It is a cached layout result and is not needed to read the music.
**(observed)**

**Note-level properties seen in the samples:** `kNoteAccidentalVisibility` (`kShow`, `kHide`,
`kParenthesise`), `kNoteForcedVoiceDirection` (stem direction), `kNoteSingleStemTremolo`,
`kNoteTiedToEvent` (explicit tie to another event, `{eventID}, {blockInstanceID}`),
`kNoteEndHasLVTie` (laissez vibrer), `kEventScale` `kCue`, `kNoteheadSetOverride`.

**Fingering.** `kNoteFingering` holds the numbers of a note's fingering as a list, counted from 0: the stored
value `0` is shown as 1, `4` as 5. **(verified with a test project: the fingerings 1 to 5 of an imported
MusicXML file are stored as 0 to 4)**

### Tuplets

```
TupletEventDefinition       in the voice stream
  position / duration       span of the tuplet in quarter notes
  tupletDefinition          steamVariantList:list: {int: 3}, {int: 2}, {list: {int: 7}, {int: 0}}, {bool: true}
  propertyTable             kTupletTextContent "kNone" hides the number,
                            kTupletBracketVisibility "kHidden" hides the bracket
```

The tuple is (actual notes, normal notes, note value with dots, ...).
`{int: 3}, {int: 2}, {list: {int: 7}, {int: 0}}` is "3 in the time of 2 eighths" with a span of
1 quarter. `{int: 2}, {int: 3}, {list: {int: 7}, {int: 0}}` with a span of `3/2` is a duplet in
9/8 (Clair de lune). The notes inside keep exact rational positions such as `1/3`; the written
value of a note inside is its played length times actual / normal. **(verified on Jesu joy and
Clair de lune)**

### Note value enumeration

Note values in variant lists are encoded as `{list: {int: value}, {int: dots}}`.

| int | Value | Evidence |
|-----|-------|----------|
| 7 | eighth (quaver) | tuplet definition of eighth triplets |
| 8 | quarter (crotchet) | tempo beat unit, open time signature beat length |

The remaining values are presumably sequential (6 = sixteenth, 9 = half, 10 = whole, 11 =
breve). **(inferred)** Named enums used elsewhere: `kMaxima`, `kBreve`, `kSemibreve`, `kMinim`,
`kCrotchet`, `kQuaver`, `kSemiQuaver`, `kDemiSemiQuaver`, `kHemiDemiSemiQuaver`.

### Time signatures (bar divisions)

A `BarDivisionEventDefinition` at a position in the global stream refers to a
`BarDivisionElementDefinition`:

```
barDivisionData
├── suppressInitialBarline, timeSignatureCommon, ...
└── timeSignature/timeSignaturesAndDivisions/array/timeSignatureAndDivision
      ├── behaviour                   kNewBar
      └── timeSignature
            ├── numerator             4
            ├── denominator           4
            └── beatGroupingAndOptions/beatGrouping
                  ├── beatUnit            kCrotchet
                  └── beatLengthsInUnits  steamVariantList:list: {int: 1}, {int: 1}, {int: 1}, {int: 1}
```

**Bars are not stored.** Bar boundaries follow from the sequence of bar divisions:
bar length in quarter notes = `numerator * 4 / denominator`, starting at each
`BarDivisionEventDefinition` position. A pick-up bar is stored as `barDivisionData/irregularBarLength`
(in quarter notes, for example `1/2` for an eighth-note upbeat) on the bar division where it starts:
the bar starting there has that length, and the regular bars follow it. This is used at the start
of a flow and after a repeat barline in the middle of a bar (the bar before the repeat barline is
cut short by the bar division). Dorico's MusicXML export writes the pick-up bar as an implicit
measure numbered `X0`. **(verified)** Without any bar division the flow is unmetered (open meter).

**Display meters and per-stave divisions.** A time signature with `behaviour` `kNoDivision`
does not start a bar of its own, and `kTimeSignatureHide` hides it. Bar and tonality divisions
can also live in an **instrument stave stream**; they override the global one for that stave
only. Jesus bleibet meine Freude uses both: the global stream has a visible 9/8 at 0 and a hidden
3/4 at quarter 3, every stave except Violin I has its own 3/4 at 0, and Violin I shows 9/8 while
its notes are hidden 3:2 triplets. The bars are 3 quarter notes long. **(verified against the
preview)**

The length of a flow is the larger of the end of its last event and
`minimumNotatedLength/length` (in quarter notes), rounded up to a full bar. **(verified: the
trumpet sample ends at quarter 34 and its preview shows 9 bars of 4/4, the last one filled with a
rest)**

### Key signatures (tonality divisions)

```
tonalityDivisionData
├── temperamentDefinitionID   temperament definition.12-edo
├── accidentalSystemID        accidental system.12et.western
├── tonalitySystemID          tonalitysystem.equaltemperament
├── keySignature
│     ├── showCautionaryNaturals
│     ├── root
│     │     ├── tonalityType   kKeySigMajor / kKeySigMinor / ...
│     │     └── root/noteName  A,  root/accidentalID accidental.12et.western.natural
│     └── accidentals (array)  explicit accidentals (custom or derived)
│           accidental/noteNameAndAccidental/noteName F, accidentalID ...
└── spellingRule/mode         kUseGlobalAlgorithm
```

The tonic plus `tonalityType` defines the key: A + `kKeySigMajor` is A major (3 sharps; the
fifths value for MusicXML is derived from the tonic). Key signatures are stored at
**concert pitch**; transposing instruments display a transposed key. **(verified)**

### Tempo

```
ImmediateTempoChangeEventDefinition/data
├── type            kAbsolute
├── presetID        tempopreset.grazioso   (library entity, amount 108)
├── text            Grazioso
├── flags, modifierFlags
└── absoluteTempo
      ├── tempoValue   555555                                   (microseconds per beat)
      └── beatUnits    steamVariantList:list: {list: {int: 8}, {int: 0}}   (quarter, no dots)
```

`tempoValue` is **microseconds per beat**, like MIDI: 60 000 000 / 555 555 = 108 BPM, matching
the preset `amount` of 108 in the library. **(verified)**

### Slurs

`SlurEventDefinition` has `position` and `duration` in the voice and a `slurElementID`. The span
is **inclusive**: the slur starts on the note at `position` and ends on the note that **starts** at
`position + duration` (for example a slur at 0 with duration 6 ends on the note at 6).
**(verified against the previews)** Shape overrides live in the property table (`kSlur*`).

### Dynamics

A `DynamicGroupEventDefinition` lives in the **instrument stave stream** (not in a voice). It
refers via `dynamicGroupID` to a `DynamicGroupElementDefinition` (element table
`DynamicGroupElementTableDefinition` of the flow) whose `subElements` hold the individual marks,
each with a `position` relative to the group event:

| Definition | Content |
|------------|---------|
| `ImmediateChangeDynamicDefinition` | `intensity` list: family (0 forte, 1 piano), reserved, mezzo flag, letter count. pp = `{int: 1}, {}, {bool: false}, {int: 2}`, mf = `{int: 0}, {}, {bool: true}, {}`, f = `{int: 0}, {}, {bool: false}, {int: 1}` |
| `GradualChangeDynamicDefinition` | `type` `kCrescOrDim`, `direction` (`kGradualDynamicUp` = crescendo, `kGradualDynamicDown` = diminuendo), `duration` in quarter notes |
| `ForceDynamicDefinition` | `type` (`kForceDynamicSfz`, ...), `intensityLevel` |
| `CombinedDynamicDefinition` | for example fp: `direction` `kDown`, `startIntensity`, `endIntensity` |

The intensity encoding matches the printed music of all samples: Clair de lune pp, p, ppp, f;
K331 p and f; Washington Post pp, p, mf, f, ff, fff. **(verified)** mp as
`{int: 1}, {}, {bool: true}, {}` is **(inferred)** by symmetry with mf.

Properties of the sub elements: `kDynamicGradualChangeDynamicAppearance` `kText` (written
"cresc."/"dim.") or `kTextWithLine` (text with a dashed line), absent for a hairpin;
`kDynamicCustomTextAfter` (for example "simile", "et animé"); `kDynamicHideIntensityMarking`;
`kDynamicParenthesiseFlag`.

### Articulations

Articulations are **note properties**, not separate events:

| Property | Values |
|----------|--------|
| `kArticulationDuration` | `kStaccato`, `kTenuto`, `kStaccatissimo`, `kStaccatoTenuto`, ... |
| `kArticulationForce` | `kAccent`, `kMarcato`, ... |
| `kArticulationStress` | `kStress`, `kUnstress` |

`kStaccato`, `kTenuto`, `kStaccatissimo` (duration) and `kAccent` (force) occur in the public
samples (778 and 335 notes). **(verified)** The other values exist as enum strings in the
executable; their assignment to a property is **(inferred)**.

### Clefs

`ClefEventDefinition` lives in the **instrument stave stream**; the stave follows from
`staveStreamInfos/staveStreamInfo` (`staveStreamID` to `nthStaveInInstrument`). The clef is
`entityIDs/idForTransposingLayouts` (for example `clef.treble`, `clef.bass`); a separate
`idForConcertLayouts` can exist. A clef at position 0 repeats the initial clef. **(verified)**

### Lyrics

`LyricEventDefinition` lives in the voice stream at the position of its note and refers via
`lyricElementID` to a `LyricElementDefinition` in the flow element tables:

```
LyricElementDefinition
├── text           Thou
├── syllableType   kWholeWord / kStart / kMiddle / kEnd
├── lineType       kVerse (kChorus, kTranslation presumably)
├── lineNumber     0-based verse number
└── richText       formatted copy of the text
```

Extender lines are not stored; Dorico draws them when a syllable is followed by notes without
lyrics. **(verified on Purcell and Jesu joy)**

### Ornaments, trills and fermatas

| Event | Stream | Content |
|-------|--------|---------|
| `OrnamentEventDefinition` | voice | `entityID` such as `ornament.trill`, `ornament.shortTrill`, `ornament.mordent`, `ornament.turn`, `ornament.tremblement`, baroque `ornament.precomp...` |
| `TrillEventDefinition` | voice | trill with a span (`duration`); `kTrillHasWigglyLine`, `kTrillIntervalAppearance` |
| `PauseEventDefinition` | global | `pauseData/pauseType` `kFermata`, `fermataDuration` `kNormal`; `kPauseAttachToBarline`, `kPauseVerticalPlacement` |
| `ArpeggioEventDefinition` | voice | arpeggio on the chord at its position |
| `PlayingTechniqueEventDefinition` | voice | playing technique (pizz., ...) |
| `TextEventDefinition` | voice | staff text: `content/fragments/fragment/content` concatenated (may contain SMuFL private use characters), `aboveOrBelow`, `isSystemText` |
| `OttavaLineEventDefinition` | stave | octave line |

**(observed)**

### Repeats, endings and repeat markers

Repeat barlines are part of the bar division: a `BarDivisionElementDefinition` can carry
`barDivisionData/barlineChangeIfAny/type` with `kStartRepeat`, `kEndRepeat`,
`kEndAndStartRepeat` or `kDouble`. Such a division may have no time signature change at all; it
only places a barline at its position. **(verified on K331)**

`RepeatEndingLineEventDefinition` (global) spans the endings: `kRepeatEndingLineNumSegments`
(for example 2) and one `kRepeatEndingLineSegmentDuration` per segment, distinguished by the
property offset `2:<segment>:0:0` (for example 3 and 3 quarter notes for 1st and 2nd ending).
`RepeatMarkerEventDefinition` (global) has `sectionType` such as `kFine`, plus
`kRepeatMarkerLabelOverride` (for example "Menuetto da capo") and `kRepeatMarkerHide`.
**(observed)**

### Where events live

| Stream | Event types |
|--------|-------------|
| `kGlobalTimebaseStream` | bar divisions, tonality divisions, tempo, pauses (fermatas), repeat endings and markers, breaks, figured bass, note spacing changes |
| `kInstrumentStaveStream` | clefs, dynamics, octave lines, stave count changes, comments, harp pedals, per-stave bar or tonality divisions |
| `kVoiceStream` | notes, grace notes, rests, slurs, tuplets, lyrics, ornaments, trills, arpeggios, playing techniques, text |

**(verified on 6 public projects)**

Each has companion properties for placement (`...ForcedPlacement`, `...DY`, `...Hidden`,
`...TieChainPosition`).


## Property Tables

Most visual and notational details are stored as sparse **property overrides**. Default values
come from options and are not stored.

```
propertyTable/entries/array/entry
├── propertyType        kNoteSpelling
├── value               string: "E"
└── context
      ├── layoutIDWithFrameChainID   steamVariantList:list: {int: layoutID}, {int: frameChainID}, {bool: ...}
      ├── blockInstanceID            -1 (all instances)
      ├── propertyOffset             string: "0:0:1:0"
      └── assignedStaveType          (optional)
```

- `layoutID = -1` means the property applies to **all layouts**. Other values make it a
  layout-specific override (part layout vs. full score), which corresponds to the "Local / Global
  properties" switch in Dorico.
- `blockInstanceID = -1` means all instances of the block.
- `propertyOffset` locates the property inside a long note (for example a different spelling
  after a barline split). `0:0:1:0` is the start of the event. Its exact syntax is unknown.

### Property type catalogue

The executable lists **939** property types. Grouped by prefix:

| Prefix | Count | Examples |
|--------|-------|----------|
| `kNote` | 162 | `kNotePitch`, `kNoteSpelling`, `kNoteAccidentalOverride`, `kNoteAccidentalVisibility_v3`, `kNoteTiedToEvent`, `kNoteSplitDurations`, `kNoteVelocity`, `kNoteStringIndex`, `kNoteFingering`, `kNoteEndHasLVTie`, `kNoteheadSetOverride`, `kNoteSingleStemTremolo` |
| `kDynamic` | 79 | dynamic appearance and position |
| `kTempo` | 40 | tempo mark text and metronome mark |
| `kFiguredBass` | 36 | figured bass layout |
| `kHorizontalLine` | 29 | line text, caps, placement |
| `kSlur` | 28 | slur shape and direction |
| `kChord` | 28 | chord symbol appearance |
| `kTrill`, `kCue`, `kBar` | 27 each | trill intervals, cue settings, bar division settings |
| `kVerticalLine`, `kPedal`, `kPlaying`, `kText`, `kRepeat` | 21 to 26 | |
| `kEvent` | 19 | `kEventHide`, `kEventDx`, `kEventDy`, `kEventColour`, `kEventMuted`, `kEventScale`, `kEventPlayingTechnique` |
| `kTuplet` | 18 | `kTupletBracketVisibility`, `kTupletTextContent`, `kTupletForcedPlacement` |
| `kArticulation` | 17 | see above |
| `kTie` | 13 | `kTieStyle`, control point deltas |
| `kBeam` | 12 | `kBeamStart`, `kBeamEnd`, `kBeamNone`, `kBeamSplitSecondary` |
| `kGrace` | 7 | `kGraceNoteSlashed` |
| `kRest` | 5 | `kRestForcePositionAndDuration`, `kRestPositionOnStave` |
| `kClef` | 7 | `kClefEventEntityIDs`, `kClefHidden` |
| `kLyric` | 14 | `kLyricSyllableType`, `kLyricLineNumber`, `kLyricIsChorus` |

Beam overrides seen in samples: `kBeamStart` with `list: {int: 0}, {int: 0}`, `kBeamEnd` with
`null: `.


## Layouts

`layouts/array/layout` defines each full score and part layout:

- `layoutOptions`: page size and margins, staff spacing, note spacing, casting off, page
  numbers, staff labels, bar numbers, condensing, bracketing, and so on.
- `layoutPlayers`: which players appear in the layout, in the order they are shown. The order of
  `scorePlayers` is the order the players were added; the full score layout (`layoutType`
  `kFullScoreLayout`) gives the score order that Dorico shows and exports. **(verified)**
- `pageSequenceOverrides`: frame chains (music, text, graphic), page overrides, master page
  changes, page number changes, flow heading changes.

Engraved positions (system breaks, note positions) are not stored, only user overrides
(for example `BreakEventDefinition`, `kEventDx`/`kEventDy`, `staveAdjustments`). Layout data is
not needed to recover the music.


## scorelibrary.dtn

Same binary encoding, root `kScoreLibrary`. It holds the library **entities** the project uses
or has modified:

```
kScoreLibrary
├── fileVersion
├── temperaments, accidentalSystems, accidentalDefinitions, tonalitySystemDefinitions
├── ensembles, instruments, instrumentNames, instrumentFamilies, instrumentScoreOrders
├── clefDefinitions, ottavaLineDefinitions
├── noteheadSetDefinitions, noteheadDefinitions, restSetDefinitions, restDefinitions
├── flagSetDefinitions, flagDefinitions
├── graphicDefinitions, blobDefinitions, drawingDefinitions, textDefinitions, glyphDefinitions,
│   compositeDefinitions
├── tempoPresetDefinitions, rhythmicFeelDefinitions
├── penstyles, fontstyles, brushstyles, paragraphStyles, characterStyles
├── pagePairDefinitionSets, pageDimensionPresets
├── playingTechniques, ornamentDefinitions, multiSegmentLineDefinitions, ...
├── expressionMapDefinitions, drumKitNoteMapDefinitions
├── chordSymbolAppearance..., percussionKit..., frettedInstrumentData..., chordDiagram...
└── lineAnnotation..., lineBodyStyle..., lineStyle...
```

Each collection is `<name>/entities/array/<Type>` and each entity has `name`, `entityID`,
`parentEntityID`, `inheritanceMask` and type-specific fields. Entities inherit from a parent
entity and only override the fields set in `inheritanceMask`. The full factory library is
available in the installation folder as XML (`scorelibrary.xml`, `instruments.xml`,
`composites.xml`, `glyphs.xml`, `playingTechniqueDefinitions.xml`, ...), which is useful to
resolve entity IDs such as `instrument.keyboard.piano` or `clef.treble` without the project
library.

The `kScore/library` node in `score.dtn` contains an embedded library with the same schema.


## supplementary_data/vstaudioengine/enginedata

Chunked binary file with the playback configuration of the VST audio engine. All integers are
little-endian. **(verified on 4 samples)**

```
u32 len, char[len]   "MixerData"
u32 len, char[len]   "1.0 beta 3"            (format version string)
repeat until EOF:
  u32 len, char[len] chunk name
  u64 size
  u8[size]           chunk payload
```

| Chunk | Payload |
|-------|---------|
| `MixerDocument` | XML (`<mixer>` with channels, inserts, sends) |
| `AudioMapping` | XML |
| `EventMapping` | XML |
| `PluginStatesHeader` | u32 count of following plug-in states |
| `PluginState` | binary VST plug-in state (one per loaded plug-in) |

This file is irrelevant for notation import.


## supplementary_data/workspace/workspace.xml

Plain XML with window state: current mode (`kWriteMode`, `kEngraveMode`, `kPlayMode`,
`kPrintMode`, `kSetupMode`), tab layout, zoom level, scroll offsets, print and export dialog
state. Not relevant for notation import.


## Mapping to the toolarium-music Model

| Dorico | `IScore` / MusicXML |
|--------|---------------------|
| `info` | work/identification |
| `scorePlayer` + `instrument` | `score-part` (one part per instrument, or per player) |
| instrument `numStaves` | `staves` in attributes |
| `flow` | its bars, appended to the bars of the previous flows; the flow title becomes a words direction |
| `BarDivisionEvent` positions | measure boundaries (computed) |
| `TonalityDivisionEvent` | `key` (fifths from tonic and mode) |
| `ClefEvent` / instrument `clefs` | `clef` |
| `NoteEvent` pitch + `kNoteSpelling` | `pitch` (step, alter, octave) |
| `NoteEvent` duration | `duration`/`type`/`dot`, split at barlines with `tie` |
| voice streams | `voice` (number per stave) |
| gaps in a voice | generated `rest` |
| same position, same voice | `chord` |
| `TupletEvent` | `time-modification` + `tuplet` notations; written value = played length times actual / normal |
| `GraceNoteEvent` | `grace` notes before the main note (`slash` for `slashed`) |
| `SlurEvent` | `slur` start/stop on the first and last covered note |
| `DynamicGroupEvent` | `direction` with `dynamics`, `wedge` (hairpin) or words (cresc., dim.) |
| `ImmediateTempoChangeEvent`, `TextEvent` | `direction` (words, metronome, sound tempo) |
| `kArticulation*` properties | `articulations` |
| `OrnamentEvent`, `TrillEvent` | `ornaments` |
| `PauseEvent` | `fermata` on the note or the barline |
| `LyricEvent` | `lyric` (number, syllabic, text) |
| bar division `barlineChangeIfAny` | `barline` with `repeat` (or light-light for `kDouble`) |
| `RepeatEndingLineEvent` | `ending` start and stop (or discontinue for the last ending) |
| `RepeatMarkerEvent` | words (Fine, D.C., ...), `segno` or `coda` |

Steps for a reader:

1. Open the ZIP and read `META-INF/container.xml`.
2. Decode `score.dtn` (binary or XML) into a generic tree.
3. For each flow, build the bar grid from the global stream (`BarDivisionEventDefinition`).
4. For each player and instrument, resolve staves and voices via `flowPlayers`.
5. Collect the events of each voice stream (through block instances), sort by
   `position`/`secondaryPosition`, group notes into chords, fill gaps with rests, split at
   barlines and add ties.
6. Apply key signatures, clefs, tempo, dynamics and slurs from the other streams.

Notation rules used by the toolarium reader (Dorico decides them at render time, so they are not
in the file): note values are aligned to their own value within the bar; in compound meters
(6/8, 9/8, 12/8) notes are split at the dotted beat, whole beats become dotted values (dotted
quarter, dotted half, dotted whole) and rests of a whole beat are dotted; a bar that is cut short
by a following time signature of exactly its length gets that time signature (display meters).

Known limitations of the toolarium reader: a clef change in the middle of a bar is attached to
the bar without its position (the measure model does not keep the order of its content),
and time signatures shown on single staves only are not converted. Pick-up bars are written as
implicit measures (`implicit="yes"`).

Writing `.dorico` files is technically possible (the binary encoding is fully understood and
reproducible), but Dorico expects a complete and consistent model (options, library, layouts,
playback data). A practical writer would start from a template project saved by Dorico and
replace only the flows and players. Given the format churn, MusicXML remains the recommended
export path into Dorico.


## Key Insights for Implementation

1. **No encryption, no proprietary compression.** ZIP plus a simple tree serialisation.
   The whole `.dtn` decoder is about 40 lines of code.
2. **The tree is self-describing.** Element names are stored in the file, so a reader can be
   written against element names (like an XML reader) and is tolerant of added fields.
3. **Binary and XML are two encodings of one tree.** Supporting both costs very little and
   also covers the factory library XML files.
4. **Time is rational quarter notes, not ticks.** Use exact fractions (numerator/denominator)
   to avoid rounding errors with tuplets.
5. **The model is semantic.** Measures, rests, tie splits and beams have to be calculated by the
   importer; they are not in the file.
6. **Pitch is concert MIDI pitch plus a letter name.** Accidental and octave follow from these
   two values.
7. **Defaults are not stored.** Missing properties mean "use the option or library default".
8. **Expect churn.** `fileVersion` changes with almost every Dorico update. Readers should ignore
   unknown elements and avoid relying on node counts or orders.


## Legal Note

Steinberg does not publish the format, and its licence terms may restrict reverse engineering
of the software. The information here was obtained by observing data files and reading strings from
the binary, for the purpose of interoperability (compare Art. 6 of EU Directive 2009/24/EC).
Implementers should verify their own legal situation before shipping a Dorico reader or
writer.


## Sources

- Sample projects bundled with Dorico (8 projects, 16 `.dtn` files).
- Six public-domain projects from [Ancient Groove](https://www.ancientgroove.co.uk/dorico/index.html)
  (Bach, Debussy, Mozart, Purcell, Sousa; 12 `.dtn` files); they are the test samples in
  `src/test/resources/samples/dorico/`.
- Factory library XML files in the Dorico installation folder.
- Strings embedded in the Dorico executable (class names, serialisation keys, property
  types, enums).
- [ThePrismSystem/dorico-plugin-development: docs/file-format.md](https://github.com/ThePrismSystem/dorico-plugin-development)
  (independent analysis of Dorico 6.0 to 6.2 files; source of the Dorico 6 version table,
  dynamics and articulation details)
- [Delsin-Yu/DoricoUse](https://github.com/Delsin-Yu/DoricoUse) (CLI that reads `score.dtn`)
- [Steinberg forum: Dorico File Format, commitment to an unencrypted file format](https://forums.steinberg.net/t/dorico-file-format-commitment-to-an-unencrypted-file-format/736582)
- [Steinberg forum: New File Format?](https://forums.steinberg.net/t/new-file-format/772925)
- [Steinberg forum: Backwards compatibility for future versions?](https://forums.steinberg.net/t/backwards-compatibility-for-future-versions/115383)
- [Steinberg forum: score.dtn shows invalid format](https://forums.steinberg.net/t/file-corrupted-after-system-reboot-score-dtn-is-725kb-but-shows-invalid-format/1034024)
