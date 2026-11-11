# Music Model Reference

This document describes the Java model interfaces that represent musical scores.
All types are defined as interfaces (`I` prefix) in `com.github.toolarium.music.model`
and organized into domain-specific subpackages. Implementations live in `impl/`
subpackages with `Impl` suffix.

For the full architecture, see [ARCHITECTURE.md](ARCHITECTURE.md).


## Score Structure

The model uses a single `IScore` interface as the root. A score contains parts,
each part contains measures, and each measure contains notes, directions, barlines, etc.

```
IScore
  +-- version: String (e.g., "4.0")
  +-- work: IWork
  +-- movementNumber: String
  +-- movementTitle: String
  +-- identification: IIdentification
  +-- defaults: IDefaults
  +-- credits: List<ICredit>
  +-- partList: IPartList
  +-- parts: List<IPart>
        +-- id: String
        +-- measures: List<IMeasure>
```


## Package: model/score

Score-level metadata and the root document type.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IScore` | Root score element | version, work, movementTitle, identification, defaults, credits, partList, parts |
| `IWork` | Work title and number | workNumber, workTitle |
| `IIdentification` | Creator and encoding metadata | creators, rights, encoding, source |
| `ICreator` | A creator entry (composer, arranger, etc.) | type, value |
| `IEncoding` | Software and date of encoding | encodingDate, software, encoder, encodingDescription |
| `IDefaults` | Default layout and scaling | scaling |
| `IScaling` | Millimeters-to-tenths conversion factor | millimeters, tenths |
| `ICredit` | Page credit text (title, composer, etc.) | page, creditType, creditWords |
| `ICreditWords` | Formatted text in a credit | value, defaultX, defaultY, fontSize, justify, valign |


## Package: model/part

Instrument and part definitions.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IPartList` | List of all parts and groups | partGroups, scoreParts |
| `IScorePart` | A single part/instrument definition | id, partName, partAbbreviation, scoreInstruments, midiDevices, midiInstruments |
| `IPartGroup` | Grouping of parts (bracket, brace) | type, number, groupName, groupSymbol, groupBarline |
| `IScoreInstrument` | Instrument definition | id, instrumentName, instrumentAbbreviation, instrumentSound |
| `IMidiDevice` | MIDI device assignment | id, port, value |
| `IMidiInstrument` | MIDI playback configuration | id, midiChannel, midiProgram, volume, pan |


## Package: model/measure

Measure structure, attributes, and navigation.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IMeasure` | A single measure in a part | number, width, implicit, attributes, notes, forwards, backups, directions, barlines, harmonies, prints, sounds |
| `IPart` | A part containing measures | id, measures |
| `IAttributes` | Key, time, clef changes | divisions, keys, times, staves, clefs, transposes, staffDetails |
| `IKey` | Key signature | fifths, mode, cancel |
| `ITime` | Time signature | symbol, beats, beatType |
| `IClef` | Clef definition | number, sign, line, clefOctaveChange |
| `ITranspose` | Transposition for reading instruments | diatonic, chromatic, octaveChange |
| `IStaffDetails` | Staff line configuration | number, staffLines, staffType |
| `IForward` | Move forward in time (multi-voice) | duration, voice, staff |
| `IBackup` | Move backward in time (multi-voice) | duration |
| `IPrint` | Page and system break control | newSystem, newPage, blankPage, pageNumber |


## Package: model/note

Note, pitch, rest, and related types.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `INote` | A note or rest event | pitch, rest, unpitched, chord, duration, ties, voice, type, dots, accidental, timeModification, stem, notehead, staff, beams, notations, lyrics |
| `IPitch` | A pitched note (step + octave + alteration) | step (C-B), alter (-2 to +2), octave (0-8) |
| `IRest` | A rest (optionally positioned) | displayStep, displayOctave, measure |
| `IUnpitched` | An unpitched note (percussion) | displayStep, displayOctave |
| `ITie` | Tie start or stop | type |
| `ITimeModification` | Tuplet ratio | actualNotes, normalNotes, normalType |
| `IBeam` | Beam grouping | number, value (begin, continue, end) |
| `ILyric` | Lyrics attached to a note | number, name, syllabic, text, elision, extend |


## Package: model/notation

Articulations, dynamics, ornaments, and other note-level markings.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `INotations` | Container for all note notations | tieds, slurs, tuplets, fermatas, articulations, dynamics, ornaments, playingTechnique, glissandos |
| `ITied` | Visual tie mark | type |
| `ISlur` | Slur line | type, number, lineType, placement |
| `ITuplet` | Tuplet bracket | type, number, bracket, showNumber |
| `IFermata` | Fermata symbol | type, value |
| `IArticulations` | Articulation marks | accent, staccato, tenuto, detachedLegato, staccatissimo, breathMark, caesura, etc. |
| `IDynamics` | Dynamic level markings | piano (p), pp, ppp, forte (f), ff, fff, mp, mf, sf, sfz, fp, etc. |
| `IOrnaments` | Ornament marks | trillMark, turn, mordent, invertedMordent, shake, wavyLine, etc. |
| `IPlayingTechnique` | Performance technique marks | upBow, downBow, harmonic, fingering, pluck, fret, string, hammerOn, pullOff, etc. |
| `IGlissando` | Glissando line | type, number, lineType, value |


## Package: model/direction

Measure-level directions: tempo, dynamics, pedal, rehearsal marks.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IDirection` | A direction attached to a measure | placement, directionTypes, offset, voice, staff, sound |
| `IDirectionType` | The content of a direction | rehearsal, segno, coda, words, wedge, pedal, metronome, octaveShift, dynamics |
| `IWedge` | Crescendo/diminuendo hairpin | type (crescendo, diminuendo, stop), number, spread |
| `IMetronome` | Tempo marking | beatUnit, beatUnitDot, perMinute, parentheses |
| `IPedal` | Sustain pedal marking | type (start, stop, change), line, sign |
| `IOctaveShift` | 8va/15ma marking | type (up, down, stop), size (8, 15) |
| `IWords` | Text expression | value, defaultX, defaultY, fontSize, fontWeight, fontStyle |
| `ISound` | Playback parameters and navigation | tempo, dynamics, dacapo, segno, dalsegno, coda, tocoda, fine |


## Package: model/barline

Barline styles, repeats, and endings.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IBarline` | Barline at measure boundary | location, barStyle, repeat, ending, fermata, segno, coda |
| `IRepeat` | Repeat barline | direction (forward, backward), times, winged |
| `IEnding` | Volta bracket (1st/2nd ending) | number, type (start, stop, discontinue), value |


## Package: model/harmony

Chord symbols and analysis.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IHarmony` | Chord symbol | type, root, function, kind, inversion, bass, degrees, frame, staff |
| `IRoot` | Chord root pitch | rootStep, rootAlter |
| `IBass` | Bass note (for slash chords) | bassStep, bassAlter |
| `IDegree` | Chord degree alteration | degreeValue, degreeAlter, degreeType |
| `IFrame` | Guitar chord diagram | frameStrings, frameFrets |


## Package: model/layout

Page and system layout.

| Interface | Description | Key Fields |
|-----------|-------------|------------|
| `IPageLayout` | Page dimensions | pageHeight, pageWidth, pageMargins |
| `IPageMargins` | Page margin values | type, leftMargin, rightMargin, topMargin, bottomMargin |
| `ISystemLayout` | System spacing | systemMargins, systemDistance, topSystemDistance |
| `ISystemMargins` | System margin values | leftMargin, rightMargin |
| `IStaffLayout` | Staff spacing | number, staffDistance |


## Package: model/common

Shared enums and marker interfaces.

| Type | Values |
|------|--------|
| `IMusicDataElement` | Marker interface for elements inside a measure (Note, Direction, Attributes, Barline, Harmony, Forward, Backup, Print, Sound) |
| `NoteTypeValue` | maxima, long, breve, whole, half, quarter, eighth, 16th, 32nd, 64th, 128th, 256th, 512th, 1024th |
| `ClefSign` | G, F, C, percussion, TAB, jianpu, none |
| `StemValue` | up, down, double, none |
| `AccidentalValue` | sharp, natural, flat, double-sharp, double-flat, etc. |
| `BeamValue` | begin, continue, end, forward hook, backward hook |
| `BarStyleValue` | regular, dotted, dashed, heavy, light-light, light-heavy, heavy-light, heavy-heavy, tick, short, none |
| `ModeValue` | major, minor, dorian, phrygian, lydian, mixolydian, aeolian, ionian, locrian, none |
| `WedgeType` | crescendo, diminuendo, stop, continue |
| `NoteheadValue` | slash, triangle, diamond, x, circle-x, normal, none, do, re, mi, fa, so, la, ti, etc. |
| `SyllabicValue` | single, begin, end, middle |
| `StartStop` | start, stop |
| `StartStopContinue` | start, stop, continue |
| `AboveBelow` | above, below |
| `UpDown` | up, down |
| `YesNo` | yes, no |
| `BackwardForward` | backward, forward |
| `LineType` | solid, dashed, dotted, wavy |
| `GroupSymbolValue` | none, brace, line, bracket, square |
| `GroupBarlineValue` | yes, no, Mensurstrich |
| `PlacementValue` | above, below |
