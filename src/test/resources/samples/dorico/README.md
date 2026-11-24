# Dorico test samples

Source: [Ancient Groove, Dorico project files](https://www.ancientgroove.co.uk/dorico/index.html),
downloaded on 2026-11-14. The `.dorico.zip` downloads were unpacked, and the K331 project was renamed
to `Mozart_Sonata_K331.dorico`. The file contents are unchanged.

Terms stated on the source page:

> These files may be freely downloaded and used for any purpose; though due acknowledgment of the
> original creator (usually specified in the Project Info data) is required in any modified versions.

The music itself is in the public domain (the site only accepts works by composers who died more
than 70 years ago). The project files are the work of their creators named below.

| File | Work | Creator (from the Project Info) |
|------|------|---------------------------------|
| `Bach_Inventions.dorico` | J. S. Bach: Two-Part Inventions, BWV 772 to 786 | Not named; published on Ancient Groove |
| `Bach_Jesu_joy.dorico` | J. S. Bach: Jesus bleibet meine Freude (from BWV 147) | "Produced by Ben Byram-Wigfield, 2025." |
| `Debussy_Clair_de_lune.dorico` | C. Debussy: Clair de lune | "Typeset by Ben Byram-Wigfield, 2025." |
| `Mozart_Sonata_K331.dorico` | W. A. Mozart: Piano Sonata in A major, K. 331 | Not named ("Made with Dorico"); published on Ancient Groove |
| `Purcell_Thou_knowest.dorico` | H. Purcell: Thou knowest, Lord | "Produced by Ben Byram-Wigfield, 2025." |
| `Sousa_Washington_Post.dorico` | J. P. Sousa: The Washington Post March | "Typeset by Ben Byram-Wigfield. Released into public domain." |

## Own test project

`fingering-test.dorico` was made for this project: a MusicXML file with five notes and the fingerings
1 to 5, imported into Dorico and saved. It shows that Dorico stores the fingerings counted from 0.

## Dorico exports for the comparison test

The folders `Bach_Inventions/` and `Bach_Jesu_joy/` contain MusicXML exports (`.mxl`) of the
projects, made with Dorico (File > Export > MusicXML, one file per flow and layout). The file names
follow Dorico's pattern `<project> - <layout> - <flow number> <flow title>.mxl`.
`DoricoReferenceComparisonTest` joins the `Full score` exports of all flows in flow order and compares
the sounding notes of every part with the reader output. The exports of single part layouts (for
example `01_Trumpet in C`) are not used by the test.
