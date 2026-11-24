# LilyPond test samples

Created for this project; no third-party content. Test input for the planned LilyPond reader; no test reads
these files so far.

| File | Content |
|------|---------|
| `basics.ly` | Header, tempo, time signatures 4/4 and 3/4, keys D major, B flat major and C major, clefs treble, alto and bass, absolute pitches with octave marks, durations with dots and a multiplier (`c1*3/4`), rests, sharps, flats and double accidentals in the note names, bar checks and the final barline |
| `relative.ly` | Relative pitches (`\relative`) with octave marks, A minor, dotted rhythms |
| `tuplets-chords.ly` | Triplets and a quintuplet (`\tuplet`), chord names (`\chordmode` with `:7` and `:m`), two voices on one staff (`<< { } \\ { } >>`) |
| `lyrics.ly` | Lyrics (`\addlyrics`) with hyphens (`--`) and an extender (`__`) |
