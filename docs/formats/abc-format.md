# ABC Notation (.abc)

ABC is an open text format for music notation, originally created by Chris Walshaw. The standard
(version 2.1) is published at [abcnotation.com](https://abcnotation.com/wiki/abc:standard:v2.1).

> **Note:** This document describes the part of ABC 2.1 that toolarium reads and how it is mapped to
> the model. It is not a replacement for the standard.


## File structure

An ABC file holds one or more tunes. A tune starts with `X:` and ends at an empty line. Fields and
`%%` directives before the first `X:` are defaults for all tunes. toolarium reads the **first
tune**; a file without `X:` is read as one tune. The text is read as UTF-8, or as ISO-8859-1 if it
is not valid UTF-8.

### Header fields

| Field | Meaning | Mapping |
|-------|---------|---------|
| `T:` | title (the first), subtitles (the others); in the body a section title | work title, subtitle credit, words |
| `C:` | composer | composer |
| `Z:` | transcriber | creator `transcriber` |
| `S:`, `A:` | source, area | source |
| `M:` | meter: `4/4`, `C`, `C|`, `2+3/8`, `none` | time signature |
| `L:` | unit note length | (default 1/16 for meters below 3/4, otherwise 1/8) |
| `Q:` | tempo: `1/4=120`, `120`, `"Allegro" 3/8=60` | metronome direction with sound tempo |
| `K:` | key and end of the header: `G`, `F#m`, `Ddor`, `Bb mix`, `none`, `HP`, `Hp`, explicit accidentals (`exp ^f _b`), clef (`clef=bass`, `bass`, `alto`, `tenor`, `treble-8`, `perc`), `transpose=` | key with mode, clef, transposition |
| `V:` | voice: ID, `name=`, `sname=`, `clef=`, `transpose=` | part or staff |
| `P:` | part label in the body | rehearsal mark |
| `w:` | lyrics of the last music line of the voice | lyrics |
| `%%score`, `%%staves` | voice grouping: `(A B)` one staff, `{RH LH}` one part with two staves | parts and staves |
| `%%MIDI program n`, `%%MIDI channel n` | General MIDI program and channel | MIDI instrument |

Inline fields (`[K:..]`, `[M:..]`, `[L:..]`, `[Q:..]`, `[V:..]`, `[P:..]`) change the value inside a
line.

### Music

| Notation | Meaning |
|----------|---------|
| `C D E F G A B c d ... b` | notes; `C` is middle C, lower case is one octave higher, `'` up and `,` down |
| `^ ^^ _ __ =` | sharp, double sharp, flat, double flat, natural (valid to the end of the measure) |
| `A2 A/2 A3/2 A/ A//` | length as a multiple of the unit note length |
| `z x` | rest, invisible rest |
| `Z4 X4` | multi-measure rest (one measure rest per measure) |
| `[CEG]2` | chord; the length of the first note times the length after the bracket |
| `A>B A<B A>>B` | broken rhythm (3:1, 1:3, 7:1 and so on) |
| `(3abc (5:4:5abcde` | tuplet p:q:r (p notes in the time of q, for r notes); default q depends on p and the meter |
| `A-A` | tie (also over the bar line; a tied note keeps its accidental) |
| `( )` | slur |
| `{g}A {/g}A` | grace notes (appoggiatura, acciaccatura) |
| `"Am7"` | chord symbol |
| `"^text" "_text" "<text" ">text" "@text"` | annotation above or below |
| `. ~ H L M O P S T u v` | staccato, roll (turn), fermata, accent, lower mordent, coda, upper mordent, segno, trill, up-bow, down-bow |
| `!decoration!` `+decoration+` | long decorations: dynamics (`!p!` to `!fff!`, `!sfz!`), articulations, ornaments, fermatas, `!segno!`, `!coda!`, `!D.C.!`, `!D.S.!`, `!fine!`, hairpins (`!<(! !<)! !crescendo(!`), pedal, `!8va(!`, fingering `!1!` to `!5!` |
| `| || |] [| .|` | bar lines (regular, double, final, heavy-light, dashed) |
| `|: :| :: :|:` | repeats |
| `[1 |1 :|2 [1,3 [1-2` | endings (an ending after `:|` ends at the next bar line) |
| `&` | voice overlay (a second voice in the same measure) |
| `\` at the line end | line continuation |

### Lyrics

`w:` aligns syllables with the notes of the last music line of the current voice: a space
separates words, `-` syllables, `_` extends the previous syllable over the next note, `*` skips a
note, `|` advances to the next measure, `~` joins words under one note and `\-` is a literal hyphen.
Each further `w:` line is the next verse.


## Mapping to the toolarium-music Model

| ABC | Model |
|-----|-------|
| voice | part, or a staff of a part (`%%score` grouping); overlays are further voices of the staff |
| measure | measure; partial measures at the start, at the end and next to repeats keep their written length (anacrusis), other measures are filled to the time signature with invisible rests |
| note pitch | pitch with alteration from the accidental, earlier accidentals in the measure, a tie or the key |
| key mode | key with the MusicXML mode (major, minor, dorian, mixolydian, phrygian, lydian, locrian) |
| tuplet | time modification with tuplet bracket start and stop |

Not converted: line breaks and layout directives, `W:` words after the tune, further tunes of the
file, microtonal accidentals (approximated by the plain accidental), user defined decorations (`U:`).


## Writing ABC

toolarium writes one ABC 2.1 tune with all parts and voices. The written tune reads back with the
same sounding notes.

| Model | ABC |
|-------|-----|
| title, subtitle | `T:` (one field per subtitle line) |
| composer, arranger, lyricist | `C:`, `C:arr. name`, `C:words: name` |
| rights | `N:` (read back as rights if it starts with © or "Copyright") |
| other creators | `Z:` |
| unit note length | `L:1/8` |
| parts and staves | `%%score`: the voices of a staff in parentheses, the staves of a part in braces |
| voice | `V:S1V1` (staff 1, voice 1) with `name`, `sname`, `clef` and `transpose` (semitones); `%%MIDI program` before it |
| key (per staff, also for transposing parts), meter, clef change | `[K:D]`, `[M:3/4]` (`C`, `C|`), `[V:id clef=bass]` |
| measure in the middle that does not fill its meter | a meter of its length, then the meter again |
| note, chord, rest, measure rest, invisible rest | `^f2`, `[CEG]2`, `z2`, `Z`, `x2`, `X` |
| accidentals | written when they differ from the key, an earlier accidental in the measure or the start of a tie |
| tuplet (also without tuplet marks) | `(p:q:r` |
| grace notes (with slash) | `{g}`, `{/g}` |
| tie (all or some notes of a chord) | `A2-`, `[C-EG]2` |
| slur | `( )` |
| beam | notes of a beam without space between them |
| articulations, fermata, ornaments, fingering | `!accent!`, `!marcato!`, `!staccato!`, `!tenuto!`, `!wedge!`, `!breath!`, `!fermata!`, `!trill!`, `!lowermordent!`, `!uppermordent!`, `!turn!`, `!1!` to `!5!` |
| dynamics, hairpins | `!p!` to `!fff!`, `!sfz!`, `!<(!` `!<)!`, `!>(!` `!>)!` |
| words | `"^text"`, `"_text"` |
| tempo | `[Q:"Allegro" 1/4=120]` |
| rehearsal mark, segno, coda | `[P:A]`, `!segno!`, `!coda!` |
| pedal, octave line | `!ped!`, `!ped-up!`, `!8va(!` `!8va)!`, `!8vb(!` `!8vb)!` |
| bar lines, repeats, endings | `|`, `||`, `|]`, `[|`, `.|`, `|:`, `:|`, `::`, `[1`, `[2` |
| chord symbols | `"Am7"` |
| lyrics | one `w:` line per verse after every music line (four measures): `-` between syllables, `_` extension, `*` skip |

Not written: the title page and page texts beyond the header fields, fonts and positions, a playback
tempo without metronome mark (`Q:` always shows the mark), text fingerings, grace notes at the end of
a measure, nested tuplets, and ties that ABC cannot express: a tie between two voices or between
enharmonic notes (for example C sharp to D flat).
