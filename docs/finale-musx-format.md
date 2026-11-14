# Finale .musx/.mus File Format: Reverse-Engineered Documentation

Documented from analysis of Finale 27's file structure and publicly observable behavior.
Finale is a product of MakeMusic; "Finale", "Enigma", and related names are trademarks of MakeMusic.

> **Disclaimer:** This document is a best-effort reconstruction for interoperability purposes.
> It is not an official specification and may contain inaccuracies or omissions.
> No guarantee of correctness or completeness is made.


## Overview

Finale uses a proprietary format called **Enigma** (internally referenced as "enigma version" in file headers).
The format comes in two variants:

| Extension | Format | Since |
|-----------|--------|-------|
| `.mus` | Legacy binary (OLE2-style container) | Finale 1.0 |
| `.musx` | Modern variant (introduced Finale 2014+, version 25+) | Finale 2014 |

The `.musx` format is not documented publicly. The structures below describe the
**in-memory data model** that maps to the file format's data structures.


## File Header

The file header has the following structure:

```
Header
  type            : String       -- Application type code ("FIN", "FJR", "FNP", etc.)
  copyright       : String       -- Finale's copyright notice
  status          : byte[4]      -- Status flags (bit field)
  created         : FileVersionInfo
  modified        : FileVersionInfo
  printed         : FileVersionInfo
  title           : String       -- Max 64 chars
  composer        : String       -- Max 32 chars
  userCopyright   : String       -- Max 64 chars
  description     : String
  encoding        : String       -- Text encoding info
```

**Status flags** (byte 0):
- Bit 0 (0x01): `TEXT_ENCODING_MAC` (Mac text encoding)
- Bit 1 (0x02): `CLEAN_2K1` (cleaned for 2001 format)
- Bit 2 (0x04): `TEXT_ENCODING_KNOWN` (encoding is explicitly set)

**FileVersionInfo** (embedded in Header for created/modified/printed):
```
FileVersionInfo
  year            : short
  month           : short
  day             : short
  modifiedBy      : String
  enigmaVersion   : int          -- Enigma engine version
  enigmaFinVersion: FinVersion
  application     : String
  platform        : String       -- "Windows" or "Mac"
  appVersion      : int
  appFinVersion   : FinVersion
  fileVersion     : int
  fileFinVersion  : FinVersion
```

**FinVersion** (version encoding):
```
FinVersion
  majorVersion    : int
  minorVersion    : int
  maintVersion    : int
  devStatus       : short        -- 0=DEV, 1=ALPHA, 2=BETA, 3=RC, 4=RELEASE
  buildNumber     : int
```

**Application types** (from `Application` enum):
| ID | Name | Header Code |
|----|------|-------------|
| 1 | Finale | FIN |
| 4 | Allegro | FJR |
| 6 | Finale Music Viewer | FMV |
| 7 | PrintMusic | NTR |
| 8 | Finale NotePad | FNP |
| 11 | QuizMaker | FQM |
| 12 | QuizTaker | FQT |
| 14 | Finale SongWriter | FSW |
| 16 | Finale Reader | FIN |


## Version Encoding

Versions are encoded as a single 32-bit integer:
```
Bits 31-24: Major version (8 bits)
Bits 23-20: Minor version (4 bits)
Bits 19-16: Maintenance version (4 bits)
Bit  15:    Debug status
Bits 14-8:  Dev status (7 bits)
Bits 7-0:   Build number (8 bits)
```

Known versions: 25.5, 26.0 through 26.3, 27.0 through 27.4, 28.0.


## Data Tag System (Enigma Tags)

The format is organized as a tagged data store. Each data record is identified by an **Extag** (extended tag),
which combines an **Etag** (base tag type) with a 2-character code.

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

Each record is addressed by a **cmper** (component number, short) and optionally an **inci** (sub-index, short).

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

#### Extended Other Records (XOT_*)
| Tag | Code | Description |
|-----|------|-------------|
| XOT_CHANNEL_PLAY_DATA | 'C','P' | Channel playback data |
| XOT_CLEF_ELEMENT | 'C','E' | Clef definitions |
| XOT_FILE_DESC | 'F','d' | File descriptors |
| XOT_HUMAN_PLAYBACK_OPTIONS | 'h','p' | Human Playback options (3 inci) |
| XOT_KEY_FORMAT | 'K','F' | Key signature format |
| XOT_KEY_MAP | 'K','M' | Key mapping |
| XOT_KEYSIG_ATTRIB | 'K','A' | Key signature attributes |
| XOT_PART_DEF | 'p','D' | Part definitions |
| XOT_PLAY_INST_2006 | 'P','L' | Playback instruments (4 inci) |
| XOT_SETTINGS_01..99 | '0'-'9','0'-'9' | Various settings blocks |

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
| ED_BEAM_*_MODS | various | Beam modifications (above/below) |
| ED_CROSS_STAFF | 'C','D' | Cross-staff note data |
| ED_DOT_MODS | 'd','o' | Dot position modifications |
| ED_LYRIC_ENTRY_INFO | 'L','E' | Lyric entry information |
| ED_NOTEHEAD_MODS_2002 | 'c','n' | Notehead modifications (2 inci) |
| ED_PERC_NOTE_CODE | 'n','C' | Percussion note codes |
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

#### Preferences (PF_*)
| Tag | Code | Description |
|-----|------|-------------|
| PF_GENERAL_PREFS | 'g','p' | General preferences |
| PF_CHORD_PREFS | 'c','p' | Chord preferences |
| PF_DEFAULT_FONT_PREFS | 'd','f' | Default font preferences |
| PF_LAYER_PREFS | 'L','A' | Layer preferences |
| PF_*_PREFS_26_2 | 'O',x | All v26.2+ preferences (barline, beam, clef, font, grace note, key, lyric, etc.) |


## Core Data Model

### Note (individual pitch within an entry)

```
Note
  exnote     : long       -- Extended note pointer
  tcd        : short      -- Tone Center Displacement (pitch encoding)
  alteration : short      -- Chromatic alteration (sharp/flat)
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

**Pitch encoding**: `tcd` (Tone Center Displacement) encodes the pitch as a diatonic displacement
from a reference point. `alteration` adds chromatic modification (sharps/flats).
`noteID` of 31 (REST_ID) indicates a rest entry.


### Entry (a note/rest event, may contain up to 12 notes for chords)

```
Entry
  exentry   : long       -- Extended entry pointer
  entnum    : int        -- Entry number
  dura      : short      -- Duration value (encoded)
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


### MeasureSpec (measure-level specification)

```
MeasureSpec
  measpace         : short   -- Measure spacing
  key              : short   -- Key signature (circle of fifths encoding)
  beats            : short   -- Time signature numerator
  divBeat          : short   -- Time signature denominator
  auxflag          : short   -- Auxiliary flags
  meflag           : short   -- Measure flags
  dispBeats        : short   -- Display time sig numerator
  dispDivBeat      : short   -- Display time sig denominator
  newflag          : short   -- Additional flags
  customBarShape   : short   -- Custom barline shape ID
  customLeftBarShape: short  -- Custom left barline shape ID
  frontSpaceExtra  : short   -- Extra front spacing
  backSpaceExtra   : short   -- Extra back spacing
  keyless          : boolean -- Atonal/keyless
  hideKeySigShowAccis : boolean
```

**Barline encoding** (meflag bits 7-4):
| Value | Barline Style |
|-------|---------------|
| 0x00 | No barline |
| 0x10 | Normal |
| 0x20 | Double (light-light) |
| 0x30 | Dashed |
| 0x40 | Heavy (solid) |
| 0x50 | Final (light-heavy) |
| 0x60 | Tick/partial |
| 0xE0 | Custom shape |

**Measure flags** (meflag):
| Mask | Name | Description |
|------|------|-------------|
| 0x8000 | LINE_BREAK | System break at this measure |
| 0x4000 | DYNAM_BIT | Has dynamics |
| 0x2000 | BREAK_REST | Break multi-measure rest |
| 0x1000 | NO_MEAS_NUM | Hide measure number |
| 0x0800 | DELTA_KEY | Key change at this measure |
| 0x0400 | DELTA_TIME | Time change at this measure |
| 0x0100 | MEASURE_TEXT | Has measure-attached text |
| 0x0008 | FOR_REP_BAR | Forward repeat bar |
| 0x0004 | BAC_REP_BAR | Backward repeat bar |
| 0x0002 | BAR_ENDING | Has ending (volta) |
| 0x0001 | REPEATS | Has repeats |

**New flags** (newflag):
| Mask | Name | Description |
|------|------|-------------|
| 0x0800 | PAGE_BREAK | Page break |
| 0x0200 | DISPLAY_ALT_DEN_TSIG | Display alternate denominator |
| 0x0100 | DISPLAY_ALT_NUM_TSIG | Display alternate numerator |
| 0x0004 | USE_DISPLAY_TIMESIG | Use display time signature |
| 0x0002 | ABBRV_TIME | Abbreviated time (common/cut) |


### StaffSpec (staff/instrument specification)

```
StaffSpec
  flag             : short   -- Staff flags
  clefs            : short   -- Clef data
  topLines         : short   -- Lines above middle
  botLines         : short   -- Lines below middle
  transposition    : short   -- Transposition value
  instflag         : short   -- Instrument flags
  lineSpace        : int     -- Line spacing
  mfont            : short   -- Music font ID
  sizeEfx          : short   -- Size effects
  fullName         : short   -- Full name text ID
  abbrvName        : short   -- Abbreviated name text ID
  instUuid         : byte[16] -- Instrument UUID
  staffID          : short   -- Staff identifier
  (many more layout/display fields)
```


## File Type Constants

```
BINARY     = 1    -- .mus binary format
ETF        = 5    -- Enigma Transportable File (text-based)
TEMPLATE   = 8    -- Template file
BINARY_OLD = 11   -- Old binary format
ETF_OLD    = 12   -- Old ETF format
MUSICXML   = 32   -- MusicXML export
```


## Key Insights for Implementation

1. **The format is record-based**: each data type has a 2-char tag code. Records are indexed by cmper (component number) and optionally inci (sub-index).

2. **Music data is organized as**: Staff -> Measure -> Layer -> Entry -> Note (up to 12 per chord)

3. **Durations are encoded as EDU** (Enigma Duration Units): 1024 EDU per quarter note.

4. **Pitch uses TCD** (Tone Center Displacement): a diatonic offset system where the reference point is the bottom line of the current clef. Chromatic alteration is stored separately.

5. **Bit flags are pervasive**: most data types use densely packed bit fields for boolean properties.

6. **The format has evolved over 30+ years**: many tag names include version suffixes (_2004, _2005, _2010, _2011, _2012, _2014, _26_2) indicating when features were added.

7. **No public specification**: the binary container format is undocumented. A pure reader would need to reverse-engineer the binary layout independently.
