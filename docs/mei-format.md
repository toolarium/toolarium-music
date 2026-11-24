# MEI File Format (.mei)

MEI (Music Encoding Initiative) is an open XML format for music notation, maintained by the
Music Encoding Initiative community. The specification is published at
[music-encoding.org](https://music-encoding.org).

> **Note:** This document describes the part of MEI that toolarium reads (common music notation of
> MEI 3, 4 and 5, including MEI basic) and how it is mapped to the model. It is not a replacement for
> the MEI guidelines.


## Document structure

```
mei                                   meiversion
+-- meiHead
|   +-- fileDesc/titleStmt            title (type subtitle), composer, lyricist, arranger,
|   |                                 respStmt/persName with role
|   +-- fileDesc/pubStmt              availability (rights), date isodate (encoding date)
|   +-- encodingDesc/appInfo          application/name
+-- music/body/mdiv/score
    +-- scoreDef                      meter.count, meter.unit, meter.sym, keysig or key.sig, key.mode
    |   +-- pgHead                    rend type title, subtitle, composer (fallback for the header)
    |   +-- staffGrp                  symbol (brace, bracket), label, labelAbbr, instrDef
    |       +-- staffDef              n, lines, label, labelAbbr, clef.shape, clef.line, clef.dis,
    |                                 clef.dis.place, keysig, trans.semi, trans.diat, instrDef
    +-- section / ending (n, label) / expansion / app (lem, rdg) / choice
        +-- scoreDef, staffDef        changes of meter, key and clef
        +-- measure                   n, left, right (rptstart, rptend, rptboth, dbl, end, dashed,
            |                         dotted, invis), metcon="false" (incomplete measure)
            +-- staff n / layer n     events
            +-- control events        tie, slur, phrase, dynam, dir, tempo, hairpin, octave, pedal,
                                      fermata, trill, mordent, turn, breath, caesura, harm, reh,
                                      fing, gliss, repeatMark, tupletSpan
```

### Layer events

| Element | Meaning |
|---------|---------|
| `note` | pname, oct, dur, dots, grace (acc, unacc), accid (written) and accid.ges (sounding), child `accid`, tie (i, m, t), stem.dir, staff (cross-staff), head.shape, artic, fermata, child `verse/syl` (wordpos i, m, t; con u for an extension) |
| `chord` | dur, dots, grace, artic, notes |
| `rest`, `space` | dur, dots (`space` is an invisible rest) |
| `mRest`, `mSpace`, `multiRest` | measure rest; `multiRest num` spans several measures |
| `mRpt`, `mRpt2` | repeats the previous measure |
| `clef` | clef change inside the layer |
| `beam`, `bTrem`, `graceGrp`, `ligature` | containers; their events are read in order |
| `tuplet` | num in the time of numbase; nested tuplets multiply |
| `fTrem` | two-note tremolo: each note has the full value in the encoding and half of it in time |

### Durations

`dur` is the note value (1 whole, 2 half, 4 quarter ... 1024, breve, long, maxima). A note without
`dur` takes the duration of the previous event of the layer.

### Pitch

The sounding pitch of a note is its `pname` and `oct` with the alteration from, in this order:
`accid.ges`, `accid`, an earlier written accidental of the same pitch in the measure (and staff),
and the key signature of the staff. `trans.semi` and `trans.diat` give the transposition (sounding
minus written) of a staff.

### Control events

Control events refer to events by `startid` and `endid` (`#` + `xml:id`) or by time stamps:
`tstamp` is the 1-based beat in the measure (the beat unit is `meter.unit`), `tstamp2` is
`Xm+Y` (X measures later, beat Y). Control events are applied after the whole document is read, so
that they can refer to later measures.


## Mapping to the toolarium-music Model

| MEI | Model |
|-----|-------|
| `staffDef` | part; a braced `staffGrp` of unlabeled staves (or a labeled group whose staves have no labels) is one part with several staves |
| `layer` | voice (layer n of staff s is voice (s - 1) × 4 + n) |
| `scoreDef` and `staffDef` changes | time, key and clef changes from the next measure |
| `measure` left and right | repeats and bar styles; `ending` gives the ending number of its measures |
| `metcon="false"` | measure with the length of its content (pickup) |
| `tie`, `slur`, `phrase` | tie and slur notations |
| `dynam` | dynamics direction (markings such as `mf`, `sfz`) or italic words |
| `dir`, `reh`, `tempo` | words, rehearsal mark, metronome with sound tempo (`mm`, `mm.unit` or `midi.bpm`) |
| `hairpin`, `octave`, `pedal` | wedge, octave shift and pedal directions at the start and end |
| `fermata`, `trill`, `mordent`, `turn`, `breath`, `caesura` | fermatas, ornaments, breath mark, caesura |
| `artic` | articulations and bowings (acc, stacc, ten, stacciss, marc, spicc, upbow, dnbow, harm, snap, stop, open) |
| `harm` | chord symbol |
| `fing` | fingering |
| `gliss` | glissando |
| `repeatMark` | segno, coda, fine, D.C., D.S. (with playback sound) |
| `verse` | lyrics |

Not converted: page layout and breaks, beams, figured bass, chord tablature frames, arpeggios,
editorial markup beyond the chosen reading (`lem`, `corr`, `reg`).


## Writing MEI

toolarium writes MEI 5.0 (common music notation). Every written file is checked against the official
MEI 5.0 schema (`mei-CMN.rng`) in the tests and read back with the same sounding notes.

| Model | MEI |
|-------|-----|
| title, subtitle | `titleStmt/title`, `title type="subordinate"` |
| composer, arranger, lyricist, other creators | `composer`, `arranger`, `lyricist`, `respStmt/persName role` |
| rights | `pubStmt/availability/useRestrict` |
| part with one staff | `staffDef` with `label`, `labelAbbr`, clef, `keySig`, `trans.diat`/`trans.semi`, `instrDef midi.instrnum` |
| part with several staves | `staffGrp symbol="brace"` with `label` and one `staffDef` per staff |
| time signature | `meter.count`/`meter.unit` on `scoreDef` (a change: a `scoreDef` before the measure) |
| key signature (per staff, also for transposing parts) | `keySig sig mode` in the `staffDef` (a change: `scoreDef/staffGrp/staffDef`) |
| clef change | `clef` in the layer before the next note |
| voice | `layer` (a gap is filled with `space`) |
| note, chord, rest, measure rest, invisible rest | `note`, `chord`, `rest`, `mRest`, `space`/`mSpace` |
| written and sounding accidental | `accid`, `accid.ges` |
| tuplet (also without tuplet marks) | `tuplet num numbase` |
| beam | `beam` (a beam that crosses a tuplet is left out) |
| grace note (with slash) | `grace="acc"` (`grace="unacc" stem.mod="1slash"`) |
| tie | `tie="i"`, `"m"`, `"t"` |
| articulations, fermata | `artic`, `fermata` (portato: staccato and tenuto) |
| cue note, notehead, invisible note | `cue`, `head.shape`, `visible="false"` |
| lyrics | `verse/syl` with `wordpos` and `con` |
| barlines, repeats, endings | `measure left/right` (`end`, `dbl`, `rptstart`, `rptend`, ...), `ending n label` |
| pickup measure | `metcon="false"` |
| slur | `slur startid endid` |
| dynamics, hairpin, tempo, words, rehearsal mark | `dynam`, `hairpin tstamp2`, `tempo mm mm.unit midi.bpm`, `dir`, `reh` (placed by `tstamp`) |
| chord symbol | `harm` with the chord text (for example `Cm7/G`) |
| fingering, ornaments, breath mark, caesura | `fing`, `trill`, `mordent`, `turn`, `breath`, `caesura` |
| segno, coda, pedal, octave line | `repeatMark func`, `pedal dir`, `octave dis dis.place` |
| page size, margins, staff size | `scoreDef page.width`, `page.height`, `page.*mar` (in `vu`), `vu.height` |
| title page texts, headers and footers (credits) | `pgHead`/`pgFoot` with `func="first"`, `"all"` or alternating `"alt1"` (even pages) and `"alt2"` (odd pages); a text block is a `rend` with `type`, `halign`, `valign` and font (`fontfam`, `fontsize`, `fontweight`, `fontstyle`), lines separated by `lb`, the page number as `num label="page"` |
| number of pages | `fileDesc/extent unit="page" quantity` |

When reading, the text blocks of a header or footer are placed by their alignment within the page
margins and stacked; without `extent` the number of pages is estimated from the measures.

Not written: system and page breaks, exact positions of the page texts, stem and beam details
beyond the primary beam, glissandos, tremolos and multi-measure rests (written as single
measure rests). MIDI notes below C0 are written in octave 0 (MEI has no lower octave).
