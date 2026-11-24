# MuseScore .mscz / .mscx File Format

MuseScore is a product of the MuseScore community and Muse Group; "MuseScore" is a trademark of
its owner.

> **Disclaimer:** This document describes the format as observed in score files and as needed for
> interoperability. It is not an official specification. The toolarium implementation was written
> from format observation only; no MuseScore source code was used.


## Overview

| Extension | Content |
|-----------|---------|
| `.mscz` | ZIP container with `META-INF/container.xml`, the score (`.mscx`), thumbnails, style, audio and view settings |
| `.mscx` | The score as XML (root element `museScore`) |
| `.msc` | MuseScore 0.9 score file name inside old containers (same XML) |

The XML format changed with every major version. The `version` attribute of the root element
identifies it:

| `museScore version` | Saved by | Main characteristics |
|---------------------|----------|----------------------|
| `1.11` to `1.18` | MuseScore 0.9 and 1.x | No `Score` element; score lists (`siglist`, `tempolist`, `keylist`, `cleflist`); pitch as `Note` attributes in 1.11 |
| `2.06` | MuseScore 2.x | Elements of a measure listed directly; voices and positions via `track` and `tick` |
| `3.01`, `3.02` | MuseScore 3.x | `voice` elements; spanners with relative `location`; tuplets referenced by ID |
| `4.00` to `4.70` | MuseScore 4.x | Like 3.x; tuplets enclosed by `Tuplet` and `endTuplet`; `concertKey`/`actualKey`; `Ornament` elements; `eid` element IDs |


## Container (.mscz)

```
META-INF/container.xml        <rootfiles><rootfile full-path="score.mscx"/></rootfiles>
score.mscx                    the score
score_style.mss               style (4.x)
Thumbnails/thumbnail.png
audiosettings.json, viewsettings.json, automation.json   (4.x)
Pictures/...                  images
```

The root file is named in `container.xml`; old files may contain a `.msc` file.


## Score Structure

```
museScore version="4.40"
  programVersion, programRevision
  Score                       (version 1: the children are directly below museScore)
    Division                  ticks per quarter note (usually 480)
    Style, metaTag name="workTitle|composer|lyricist|arranger|copyright|..."
    Part [id]
      Staff [id]              StaffType group="pitched|percussion|tablature", defaultClef
      trackName
      Instrument
        longName, shortName, trackName
        transposeDiatonic, transposeChromatic   (written to sounding, for example -1/-2 for B flat)
        clef, Drum pitch=".." (drum set), Channel/program value=".."
    Staff id="1"              content of staff 1
      VBox                    frame with title texts (Text/style title, subtitle, composer, ...)
      Measure [len="3/8"]     len: irregular measure (pickup)
        startRepeat, endRepeat (number of plays), Jump, Marker
        voice                 up to 4 voices (version 3 and later)
          Clef, KeySig, TimeSig, Tempo, Dynamic, StaffText, RehearsalMark, Harmony, ...
          Spanner type="HairPin|Volta|Ottava|Pedal|..."
          Tuplet ... endTuplet
          Chord / Rest
          location            moves the position (fractions of a whole note)
          BarLine
```

Staves without an `id` in the part definition (version 1 and 4.7) are numbered in score order.


## Time

Positions inside a voice are implicit: every chord and rest advances the position by its
duration; `location/fractions` moves the position (for gaps in secondary voices). Durations are
`durationType` (`breve`, `whole`, `half`, `quarter`, `eighth`, `16th` ... `256th`, `measure`) plus
`dots`. A `measure` rest fills the measure. Version 1 files use `ticks` or `ticklen` (ticks at
`Division` per quarter). Version 1 and 2 files set positions explicitly with `tick` (absolute
ticks) and voices with `track` (staff index times 4 plus voice).

**Tuplets.** Version 4: a `Tuplet` element (`normalNotes`, `actualNotes`, `baseNote`) applies to the
following chords and rests up to `endTuplet`. Versions 1 to 3: `Tuplet id=".."` is defined in the
voice and referenced by the chords (`<Tuplet>1</Tuplet>`). The played length is the written length
times normal / actual.

**Local time signatures.** A `TimeSig` with `stretchN`/`stretchD` is local to its staff: the
measure length follows the time signature of the other staves (without stretch), and the notes of
the local staff are scaled by `stretchD / stretchN` (for example 18/16 over 3/4 with stretch 3/2 in
the Goldberg variations). MuseScore's own MusicXML export writes these notes unscaled.

**Measure repeats.** `MeasureRepeat` (4.x) or `RepeatMeasure` (older) repeats the previous measure.


## Pitch

`Note/pitch` is the **concert** MIDI pitch; `tpc` is the concert spelling and `tpc2` the written
spelling as tonal pitch class (line of fifths: 14 = C, 15 = G, 13 = F, 6 = Cb, 22 = C#). The written
pitch of a transposing instrument is the concert pitch minus `transposeChromatic`. Notes under an
octave line (`Ottava`) keep their written position; they sound an octave (two, three) higher or
lower. Cross-staff chords have `staffMove` (version 2: `move`).

**Key signatures.** 4.x: `concertKey` and, for transposing instruments, `actualKey` (written key).
Older versions: `accidental` (key of the staff). Version 1 files store keys in the `keylist` of the
staff.


## Spanners (versions 3 and 4)

```
<Spanner type="Slur|Tie|HairPin|Volta|Ottava|Pedal|Trill|TextLine">
  <Slur/>                          definition (start only)
  <next><location>                 at the start: distance to the end
    <voices>-1</voices>            other voice (ties may change the voice)
    <measures>1</measures>
    <fractions>-3/4</fractions>    relative position change within the measure
  </location></next>
</Spanner>
<Spanner type="..."><prev><location>...</location></prev></Spanner>   at the end
```

Slurs and ties are written inside the chord and the note; hairpins, voltas, octave lines and pedal
lines in the voice. The end position is the start measure plus `measures`, at the start position
plus `fractions`. Versions 1 and 2 use IDs instead: `Tie id`/`endSpanner id` in notes, `Slur id`
definitions with `<Slur type="start|stop" id=".."/>` in the chords, `HairPin id` with `endSpanner`.


## Notation Elements

| Element | Content |
|---------|---------|
| `Articulation/subtype` | `articStaccatoAbove`, `articAccentBelow`, `articTenutoAbove`, `articMarcatoAbove`, ... (older: `staccato`, `sforzato`, `tenuto`, `fermata`, ...) |
| `Ornament/subtype` (4.x) | `ornamentTrill`, `ornamentMordent`, `ornamentShortTrill`, `ornamentTurn`, ... |
| `Fermata/subtype` (4.x, before the chord) | `fermataAbove`, `fermataBelow`, ... |
| `Dynamic/subtype` | `pp`, `mf`, `sfz`, ... |
| `Tempo` | `tempo` in quarter notes per second (1.6 = 96 BPM), `text` with `sym` elements |
| `Lyrics` | `no` (0-based verse), `syllabic`, `text` |
| `Harmony` | `root` and `base` as tonal pitch class, `name` (extension) |
| `acciaccatura`, `appoggiatura`, `grace4`, `grace16`, `grace32`, `grace8after` ... | grace chord (no duration) |
| `Breath`, `Arpeggio`, `Tremolo`, `ChordLine`, `Glissando` | further chord elements |
| `BarLine/subtype` | `double`, `end`, `dashed`, ... |
| `Jump`, `Marker` | D.C./D.S. texts, segno, coda, fine |

Texts contain formatting elements (`b`, `i`, `font`) and symbols (`<sym>metNoteQuarterUp</sym>`).


## Mapping to the toolarium-music Model

| MuseScore | `IScore` / MusicXML |
|-----------|---------------------|
| `Part` with staves | part with `staves` |
| voices of a staff | voices `staff * 4 + voice + 1`; gaps become invisible rests |
| concert pitch, `tpc2` | written pitch and `transpose` |
| local time signature | stretched durations with `time-modification` |
| ottava | sounding pitch plus `octave-shift` direction |
| hairpin, ottava, pedal | `wedge`, `octave-shift`, `pedal` directions |
| volta | `ending` on barlines |
| percussion staff | `unpitched` with the display position |

The writer produces MuseScore 4 files (`version="4.40"`): concert pitches with `tpc`/`tpc2`, voices
on the home staff of their notes (cross-staff chords with `staffMove`), slurs, ties, hairpins and
voltas as spanners with relative locations, complete tuplets, local time signatures for stretched
staves, and the same measure length in all staves. MuseScore 4 opens the written files of all
samples and reads the same notes as from the originals.

**Limits:** layout (positions, spacing, breaks), styles, beams, playback settings and drum set
definitions are not converted; the position of chord symbols and clef changes inside a measure is
not kept by the measure model.
