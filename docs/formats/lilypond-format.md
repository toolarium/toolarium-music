# LilyPond (.ly)

LilyPond is an open text format and engraving program for music notation. The language is described
in the [LilyPond Notation Reference](https://lilypond.org/doc/v2.24/Documentation/notation/index).

> **Note:** This document describes the part of the LilyPond language that toolarium reads and how
> it is mapped to the model. toolarium has its own parser; it does not run LilyPond and does not
> evaluate Scheme code.


## File structure

A LilyPond file is read as UTF-8 (with or without byte order mark), or as ISO-8859-1 if it is not
valid UTF-8. The first `\score` block is read; without a `\score`, the first music expression at the
top level is read. Further scores and `\book` blocks are skipped.

| Element | Meaning | Mapping |
|---------|---------|---------|
| `\header { ... }` | `title`, `subtitle`, `composer`, `arranger`, `poet`, `copyright` (strings or `\markup`) | work title, subtitle, creators, rights |
| `name = ...` | variable | expanded where `\name` is used |
| `\language "..."` | note names: `nederlands` (default), `english`, `deutsch` | pitch spelling |
| `\version`, `\include` | | skipped |
| `\paper`, `\layout`, `\midi` | layout and playback settings | skipped |


## Contexts

| Context | Mapping |
|---------|---------|
| `\new Staff`, `RhythmicStaff`, `TabStaff`, `DrumStaff` | a part with one staff (a staff of the enclosing piano staff) |
| `\new PianoStaff`, `\new GrandStaff` | one part with several staves |
| `\new StaffGroup`, `\new ChoirStaff` | grouping only; each staff is a part |
| `\new Voice [= "name"]` | a voice of the staff (up to 4 voices per staff) |
| `<< ... \\ ... >>` | simultaneous voices |
| `\new ChordNames`, `\chords`, `\chordmode` | chord symbols (`c1`, `g4:7`, `a2:m7`, `c1/e`) |
| `\addlyrics`, `\new Lyrics \lyricsto "voice"` | lyrics of a voice, one verse per block |
| `\with { instrumentName = ... }`, `\set Staff.instrumentName` | part name and abbreviation |

Music outside any staff becomes one part.


## Music

| Notation | Meaning |
|----------|---------|
| `c d e f g a b`, `cis`, `bes`, `fisis`, `ees`, `es` | note names (Dutch); English (`cs`, `bf`, `c-sharp`) and German (`h`, `b`) with `\language` |
| `'`, `,` | octave up, octave down (`c` is the octave below middle C) |
| `\relative [pitch]`, `\fixed pitch`, `\absolute` | octave entry mode |
| `\transpose from to` | transposition |
| `4`, `8.`, `2..`, `\breve`, `1*3/4` | duration with dots and scaling; a note without duration repeats the last one |
| `r`, `R`, `s` | rest, full measure rest, invisible rest |
| `<c e g>4`, `q` | chord, repeated chord |
| `\tuplet 3/2 { ... }`, `\times 2/3 { ... }` | tuplet |
| `\grace`, `\acciaccatura`, `\appoggiatura`, `\slashedGrace` | grace notes (with slash for acciaccatura) |
| `\repeat volta n { } \alternative { { } { } }` | repeat barlines and endings |
| `\repeat unfold n`, `percent`, `tremolo` | written out |
| `\time n/d`, `\key pitch \mode`, `\clef name` | time, key (all church modes), clef (with octave: `treble_8`, `bass^15`) |
| `\partial d` | pickup measure (implicit first measure) |
| `\tempo "text" 4 = 120` | tempo with metronome mark |
| `\bar "\|."`, `"\|\|"`, `".\|:"`, `":\|."` | barline styles and repeat barlines |
| `\mark \default`, `\mark "A"` | rehearsal mark |
| `\override`, `\once`, `\tweak`, `\revert`, `\unset`, Scheme (`#...`) | skipped |

### Marks after a note

| Notation | Mapping |
|----------|---------|
| `~` | tie |
| `(` `)`, `\(` `\)` | slur, phrasing slur |
| `-.`, `->`, `--`, `-^`, `-!`, `-_` | staccato, accent, tenuto, marcato, staccatissimo, portato |
| `\staccato`, `\accent`, `\tenuto`, `\marcato`, `\fermata`, `\trill`, `\mordent`, `\prall`, `\turn` | articulations, fermata and ornaments |
| `\p`, `\mf`, `\ff`, `\sfz`, `\fp`, ... | dynamics |
| `\<`, `\>`, `\!` | crescendo and diminuendo hairpins |
| `^"text"`, `_"text"`, `-\markup { ... }` | words above or below |
| `-1`, `[`, `]`, `:32` | fingering, beams and tremolo: skipped |


## Measures

LilyPond has no measures; the voices are divided into measures by the time signatures (4/4 if there
is none) and the pickup of `\partial`. A note over a barline is split into tied notes, a length
without a single note value (for example `1*5/4`) is split into tied notes, and a gap in a voice is
filled with an invisible rest.


## Lyrics

The syllables are assigned to the notes of the voice in order. A note gets no syllable if it is a
rest, a grace note, the continuation of a tie or inside a slur. `--` joins syllables of a word
(begin, middle, end), `__` is an extender line and `_` skips a note.


## Limits

- Only the first score of a file is read.
- Scheme code, music functions defined in the file and `\include` files are not evaluated.
- Fingerings, beams, tremolos, layout (`\paper`, `\layout`, overrides) and MIDI settings are not read.


## Writing LilyPond

toolarium writes LilyPond source for version 2.24 and later (Dutch note names, absolute octaves). The
written files compile with LilyPond 2.26 and read back with the same sounding notes.

| Model | LilyPond |
|-------|----------|
| title, subtitle, composer, arranger, lyricist, rights | `\header` with `title`, `subtitle`, `composer`, `arranger`, `poet`, `copyright` (several lines as `\markup \center-column`) |
| part with one staff | `\new Staff \with { instrumentName shortInstrumentName midiInstrument }` |
| part with several staves | `\new PianoStaff` with one `\new Staff` per staff |
| voice | `\new Voice = "staffNvoiceM"` with `\voiceOne` to `\voiceFour`; a gap is filled with spacer rests (`s`) |
| transposing part | `\transposition` (for example `bes` for a B flat trumpet); the notes are written pitch |
| clef, key, time signature | `\clef`, `\key ... \major` (all church modes), `\time` (4/4 and 2/2 with `\numericTimeSignature` unless the source has the C symbol) |
| pickup | `\partial` |
| measure shorter or longer than its meter | a hidden time signature of its length (`\once \omit Staff.TimeSignature \time`) |
| note, chord, rest, measure rest, invisible rest | `c'4`, `<c' e' g'>4`, `r4`, `R1`, `s4` |
| tuplet (also without tuplet marks) | `\tuplet 3/2 { ... }` |
| grace notes (with slash) | `\grace { ... }` (`\acciaccatura { ... }`) |
| tie (all or some notes of a chord) | `~` after the note or chord, `<c'~ e'>` for single chord notes |
| slur, nested slur | `( )`, `\( \)` |
| beam | `[ ]` for the primary beam |
| articulations, fermata, ornaments, fingering | `->`, `-^`, `-.`, `--`, `-_`, `-!`, `\fermata`, `\trill`, `\mordent`, `\prall`, `\turn`, `\reverseturn`, `-1` |
| dynamics, hairpins | `\p`, `\mf`, `\sfz` (other texts as `\dynamic` markup), `\<`, `\>`, `\!` |
| words | `^"text"`, `_"text"` (italic or bold as markup) |
| tempo | `\tempo "Allegro" 4 = 120`; a playback tempo without metronome mark with `\once \set Score.tempoHideNote = ##t` |
| rehearsal mark, segno, coda | `\mark "A"`, `\segnoMark`, `\codaMark` |
| pedal, octave line | `\sustainOn`, `\sustainOff`, `\ottava` |
| barline | `\bar "|."` and the other bar types |
| repeat with endings | `\repeat volta n { ... } \alternative { { ... } { ... } }` |
| chord symbols | `\new ChordNames \chordmode { c1 g1:7 a1:m7/e }` |
| lyrics | `\new Lyrics \lyricsto "voice" { "Sing" -- "ing" __ _ }`, one per verse |

The reader reads back what the writer writes, including `\transposition`, `midiInstrument`, ties of
single chord notes, more than four voices per staff and hidden tempo marks.

Not written: page layout and the title page texts beyond the header fields, stem directions and
note positions, and ties that LilyPond cannot express: a tie between two voices or between
enharmonic notes (for example C sharp to D flat).
