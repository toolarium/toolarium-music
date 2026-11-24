# Sibelius test samples

## sibmei test files

Source: [music-encoding/sibmei](https://github.com/music-encoding/sibmei) repository,
`test/sibmeiTestSibs/` directory on the `develop` branch, downloaded on 2026-11-14.

License: GPLv3. These files are used only as test input and are not part of the toolarium
library itself.

Each file is a small, feature-specific Sibelius score created for the sibmei MEI export plugin
test suite. The 23 files cover: accidentals, arpeggios, bar numbers, bar rests, beams, clefs,
color, fermatas, grace notes, hairpins, headers, lines, lyrics, meter, note styles, repeats,
slurs and ties, staves, symbols, text, transposing instruments, tuplet beams, and voltas.

## Minimal test files

Created for binary diffing to reverse-engineer the Sibelius payload format.
Each `.sib` file was created in Sibelius 6 by importing the corresponding `.mxl` source.

| File | Content | Purpose |
|------|---------|---------|
| `single-c4-quarter` | C4 quarter note | Baseline |
| `single-d4-quarter` | D4 quarter note | Pitch field mapping |
| `single-c4-half` | C4 half note | Duration field mapping |
| `single-c4-quarter-rest` | Rest then C4 quarter | Position field mapping |
| `two-notes-c4-e4` | C4 + E4 quarter notes | Multiple note detection |
| `two-c4-untied` | Two C4 quarters, no tie | Tie baseline |
| `two-c4-tied` | Two C4 quarters, tied | Tie encoding |
| `c4-no-dynamic` | C4 whole note, no dynamic | Dynamic baseline |
| `c4-with-forte` | C4 whole note with forte | Dynamic encoding |
| `c4-no-lyric` | C4 whole note, no lyric | Lyric baseline |
| `c4-with-lyric` | C4 whole note with "la" | Lyric encoding |
