# MEI test samples

Source: [musescore/MuseScore](https://github.com/musescore/MuseScore) repository on the `master`
branch, folder `src/importexport/mei/tests/data`, downloaded on 2026-11-16. The 64 files cover one
MEI feature each (accidentals, articulations, beams, clefs, dynamics, endings, fermatas, grace
notes, hairpins, lyrics, ornaments, slurs, ties, tuplets, transposition and so on).

License: GPLv3. These files are used only as test input and are not part of the toolarium
library itself.

## Reference exports (`reference/`)

`reference/<name>.mscx` is the MuseScore import of `<name>.mei` as stored in the MuseScore test
suite. `MeiReaderTest` compares the sounding notes of the reader with these references. Five
references differ from the MEI content and are excluded:

- `color-01`, `lyric-03`, `tie-01`, `trill-01`: MuseScore 5 stores a tie on its end note, which the
  MuseScore reader reads as a tie start.
- `ftrem-01`: MuseScore gives both notes of a two-note tremolo the full value.
