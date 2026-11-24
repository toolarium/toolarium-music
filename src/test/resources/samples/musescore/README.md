# MuseScore test samples

Source: [musescore/MuseScore](https://github.com/musescore/MuseScore) repository on the `main`
branch, downloaded on 2026-11-14. Files come from two directories:

- `demos/`: five demonstration scores (All Dudes, Amazing Grace, Reunion, Triumph, Goldberg Variations)
- `test/`: ten small feature-specific test files (accidentals, arpeggios, fractional beams,
  multi-measure rests, noteheads, piccolo, repeats, slurs)

License: GPLv3. These files are used only as test input and are not part of the toolarium
library itself.

## Reference exports (`reference/`)

`reference/<name>.mxl` is the MusicXML export of `<name>.mscz` by MuseScore 4.7
(`MuseScore4.exe -o <name>.mxl <name>.mscz`), created on 2026-11-15. `MuseScoreReaderWriterTest`
compares the reader with these exports. MuseScore 4 cannot open the files of version 1.11 to 1.13
(`accidental3`, `fractional_beams`, `mmrest`, `noteheads`, `piccolo`), so they have no reference.
The exports are derived from the GPLv3 samples above and are used the same way.
