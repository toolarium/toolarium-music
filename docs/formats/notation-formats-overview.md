# Music Notation Software and File Formats: Landscape Overview 2026

Survey of notation tools that use their own file format, their development status, popularity,
and how feasible it would be to support them in toolarium-music.

> **Status:** Research snapshot as of November 2026. Product status changes over time; verify
> against the vendor before relying on any single statement.
>
> **Popularity:** No reliable public market share data exists for notation software. Market
> research reports only give a total market size. The popularity ratings below are qualitative,
> based on community size, update activity, and how often the format circulates.
>
> All product names are trademarks of their respective owners.


## Formats Already Supported

| Format | Extension | Read | Write | Notes |
|--------|-----------|------|-------|-------|
| ABC notation | `.abc` | ✓ | ✓ | ABC 2.1, first tune of a file; writes one tune with all voices, see [abc-format.md](abc-format.md) |
| capella | `.capx`, `.cap` | ✓ |  | `.capx`: ZIP with CapXML; `.cap`: legacy binary format (capella 2000 to 2008), decoded, see [capella-format.md](capella-format.md) |
| Dorico | `.dorico` | ✓ |  | ZIP with a binary tree, all flows with notes and notation (tuplets, grace notes, dynamics, articulations, lyrics, repeats). No pickup bars yet. See [dorico-format.md](dorico-format.md) |
| Finale | `.musx`, `.mus` | ✓ |  | ZIP container with encrypted EnigmaXML, see [finale-musx-format.md](finale-musx-format.md) |
| Guitar Pro | `.gp`, `.gpx`, `.gp5`, `.gp4`, `.gp3` | ✓ |  | All versions from Guitar Pro 3 to 8: binary GP3 to GP5, GP6 BCFZ container and GP7/8 ZIP with GPIF XML. Tracks, tuning, string and fret, drums, voices, tuplets, ties, hammer-ons, slides, harmonics, dynamics, chord names, lyrics, markers, tempo, repeats and endings. See [guitarpro-format.md](guitarpro-format.md) |
| LilyPond | `.ly`, `.ily` | ✓ | ✓ | Own parser, first score of a file; Scheme is not evaluated. Writes LilyPond 2.24 source that LilyPond 2.26 compiles, see [lilypond-format.md](lilypond-format.md) |
| MEI | `.mei` | ✓ | ✓ | Read MEI 3, 4 and 5, write MEI 5 (common music notation with control events), see [mei-format.md](mei-format.md) |
| MIDI | `.mid`, `.midi` | ✓ | ✓ | Standard MIDI File, see [midi-format.md](midi-format.md) |
| MuseScore | `.mscz`, `.mscx` | ✓ | ✓ | ZIP with XML. Reads all file versions (1.x to 4.x), writes the MuseScore 4 format. See [musescore-format.md](musescore-format.md) |
| MusicXML | `.musicxml`, `.xml` | ✓ | ✓ | Open interchange standard (W3C community group), see [musicxml-format.md](musicxml-format.md) |
| MXL | `.mxl` | ✓ | ✓ | Compressed MusicXML (ZIP with `META-INF/container.xml`), see [musicxml-format.md](musicxml-format.md) |
| Sibelius | `.sib`, `.sco`, `.si7` | ✓ |  | Encrypted binary container with zlib compression. Decrypts, decompresses, extracts metadata, instruments, notes with pitches and durations, ties, slurs, tuplets, articulations, fermatas, dynamics, hairpins, texts, tempo, and lyrics. See [sibelius-sib-format.md](sibelius-sib-format.md) |


## Tier 1: Major, Actively Developed Tools

| Tool | Vendor | Native format | Container / encoding | Status | Popularity |
|------|--------|---------------|----------------------|--------|------------|
| **MuseScore Studio** | Muse Group (open source, GPL) | `.mscz`, `.mscx` | ZIP containing an XML `.mscx`. Not formally specified, but fully defined by the open source code | Very active (4.x line) | Largest user base by far. musescore.com hosts millions of scores; dominant in hobby and education |
| **Sibelius** | Avid | `.sib` | Proprietary encrypted binary with zlib compression. Container, cipher and note encoding fully decoded (see [sibelius-sib-format.md](sibelius-sib-format.md)) | Active, year based versioning (2026.x) | Long time professional and education leader; still very large, especially in UK, US and education |
| **Dorico** | Steinberg (Yamaha) | `.dorico` | ZIP containing `score.dtn`, an unencrypted binary tree (or the same tree as XML). Not officially documented, but fully decodable, see [dorico-format.md](dorico-format.md) | Very active (Dorico 6) | Fastest growing; widely considered the most advanced tool in active development. Main destination of Finale users after 2024 |
| **Finale** | MakeMusic | `.musx` (current), `.mus` (legacy binary) | ZIP container | **Discontinued** August 2024. No new licenses, existing installations keep working | Huge legacy archive (publishers, US band and choir). Now primarily a migration format |
| **Guitar Pro** | Arobas Music | `.gp` (GP7/8), `.gpx` (GP6), `.gp3`/`.gp4`/`.gp5` (older) | `.gp` is a ZIP containing `Content/score.gpif` (XML). `.gpx` is a custom compressed container. `.gp3` to `.gp5` are binary and fully reverse engineered | Active (GP8) | Dominant for guitar tablature; enormous number of files in circulation (Ultimate Guitar, Songsterr) |


## Tier 2: Active, Regional or Niche

| Tool | Format | Notes |
|------|--------|-------|
| **capella** (capella-software AG, Germany) | `.capx` (v7+), `.cap` (legacy binary) | `.capx` is a ZIP containing XML with a published schema. Version 10.0-07 released January 2026, followed by 10.0-08. Subscription model added in 2025. Strong in German speaking church music, choirs and schools |
| **Notion** (PreSonus, now Fender) | `.notion` | Proprietary. Still sold, development slowed, focus moved to Studio One integration |
| **Overture** (Sonic Scores) | `.ove` | Small but alive. MuseScore 3 could import older `.ove` files |
| **NoteWorthy Composer** | `.nwc` | Old Windows tool, still sold, loyal community (hymns, choirs). Reverse engineered (for example nwc2xml) |
| **Harmony Assistant / Melody Assistant** (Myriad, France) | `.myr`, `.mus` | Niche, still maintained |
| **TablEdit** | `.tef` | Folk, banjo, mandolin tablature. Still maintained; Guitar Pro can import it |
| **TuxGuitar** | `.tg` | Open source **Java** tablature editor (LGPL). Reads Guitar Pro and Power Tab files; useful as a reference implementation |
| **Band-in-a-Box** (PG Music) | `.sgu`, `.mgu` | Accompaniment and lead sheets rather than engraving. Large jazz and education user base |
| **StaffPad** (Muse Group) | proprietary | Handwriting input on tablets; exports MusicXML |


## Tier 3: Legacy or Discontinued

| Tool | Format | Status |
|------|--------|--------|
| **Encore** (Passport, Gvox, Passport, Sonic Scores since 2022) | `.enc` | Last release 5.0.x (around 2015). Encore 6 was announced for 2025, no evidence it shipped. Many archived files from the 1990s and 2000s |
| **Power Tab Editor** | `.ptb` (`.pt2` for the open source successor) | Legacy. Guitar Pro and TuxGuitar can import it |
| **SCORE** (Leland Smith) | `.pmx`, `.mus` | Historically important for publishers, no longer developed |
| **Mosaic** (MOTU), **Igor Engraver**, **Amadeus**, **Lime** | various | Discontinued or nearly so |
| **NIFF** | `.nif` | Failed 1990s interchange standard |


## Open and Text Based Formats

| Format | Type | Usage |
|--------|------|-------|
| **MEI** (`.mei`) | XML, open specification | Academia and digital musicology (Verovio). MuseScore imports it since 4.2 |
| **ABC notation** (`.abc`) | Plain text | Folk and traditional music; tens of thousands of tunes online. Simple to parse and write |
| **LilyPond** (`.ly`) | Text, effectively a programming language | High quality engraving, academia. Writing is easy, reading is hard |
| **Humdrum \*\*kern** (`.krn`) | Text | Music analysis and research corpora |
| **MuseData**, **Braille Music** (`.brf`) | Text | Niche and accessibility |


## Sources

- [Scoring Notes product guide](https://www.scoringnotes.com/productguide/)
- [Flat.io: best Finale alternatives 2026](https://blog.flat.io/best-finale-alternatives-2026/)
- [Songscription: notation software for MIDI/MusicXML export](https://www.songscription.ai/blog/best-notation-software-for-exporting-midi-musicxml)
- [Steinberg forum: scorewriter survey](https://forums.steinberg.net/t/discussion-and-survey-about-scorewriters-on-facebook/117308)
- [capella 10.0-07 release notes](https://www.capella-software.com/us/index.cfm/download/capella/version-history/capella-10-0-07)
- [capella 10.0-08 release notes](https://www.capella-software.com/index.cfm/us/download/capella/version-history/capella-10-0-08/)
- [capella product page](https://www.capella-software.com/us/index.cfm/products/capella)
- [Encore (software), Wikipedia](https://en.wikipedia.org/wiki/Encore_(software))
- [MuseScore forum post on Encore 6](https://musescore.org/en/node/372200)
- [Guitar Pro 8 import documentation](https://www.guitar-pro.com/docs/gp8/import-export/import)
- [Songscription: what is a Guitar Pro file](https://www.songscription.ai/blog/what-is-a-guitar-pro-file)
- [alphaTab format documentation](https://alphatab.net/docs/category/formats)
