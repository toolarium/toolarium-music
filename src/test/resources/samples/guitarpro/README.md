# Guitar Pro test samples

Source: [CoderLine/alphaTab](https://github.com/CoderLine/alphaTab) repository on the `develop`
branch, folder `packages/alphatab/test-data/guitarpro3` to `guitarpro8`, downloaded on
2026-11-16. The folders hold the same test songs (notes, tuplets, repeats, effects and so on) saved
by different Guitar Pro versions:

| Folder | Format | Files |
|--------|--------|-------|
| `gp3/` | Guitar Pro 3 (`.gp3`, binary) | 18 |
| `gp4/` | Guitar Pro 4 (`.gp4`, binary) | 23 |
| `gp5/` | Guitar Pro 5 (`.gp5`, binary) | 49 |
| `gp6/` | Guitar Pro 6 (`.gpx`, BCFZ/BCFS container with GPIF) | 36 |
| `gp7/` | Guitar Pro 7 (`.gp`, ZIP container with GPIF) | 50 |
| `gp8/` | Guitar Pro 8 (`.gp`, ZIP container with GPIF) | 49 |

The audio track sample (`canon-audio-track.gp`, 1 MB), the screenshots and the binary stylesheet
were not taken.

License: MPL-2.0 (alphaTab). The files are unmodified and used only as test input; they are not
part of the toolarium library itself.

`GuitarProReaderTest` reads every file and compares the sounding notes of the same song across
versions (GP3 with GP4, GP4 with GP5, GP5 with GP7, GP6 with GP7, GP7 with GP8). A few songs have a
different content in two versions and are excluded from that comparison: `ottavia` (GP5),
`harmonic-types` and `score-info` (GP4), `beaming-mode` and `slash` (GP6) and `slash` (GP7).
