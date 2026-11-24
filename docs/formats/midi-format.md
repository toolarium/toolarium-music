# Standard MIDI File Format (.mid, .midi)

The Standard MIDI File (SMF) format is specified by the MIDI Association. toolarium reads and writes
it with the JDK package `javax.sound.midi`.


## File structure

```
MThd   header: format (0, 1 or 2), number of tracks, resolution (ticks per quarter note)
MTrk   track: delta time + event, ...
```

| Format | Content | Support |
|--------|---------|---------|
| 0 | one track with all channels | read |
| 1 | several tracks played together (usually a tempo track and one track per instrument) | read and write |
| 2 | independent sequences | rejected |

Events used by toolarium:

| Event | Meaning |
|-------|---------|
| Note on (0x9n) / note off (0x8n, or note on with velocity 0) | note pitch, velocity, start and end |
| Program change (0xCn) | General MIDI instrument |
| Meta 0x03 | track name |
| Meta 0x51 | tempo in microseconds per quarter note |
| Meta 0x58 | time signature |
| Meta 0x59 | key signature (fifths, major or minor) |


## Reading

- A format 0 file is split into one part per channel; a format 1 file becomes one part per track
  that contains notes (named by the track name, or "Track n").
- The first time signature, key signature and tempo of the file apply to the whole score
  (default 4/4).
- Note durations are quantized to the nearest note value (with or without one dot); the MusicXML
  divisions are the MIDI resolution.
- Pitches are spelled from the MIDI note number; all parts use a treble clef.
- Notes are placed in measures by their start tick; a note that crosses a bar line is not split
  into tied notes. A measure without notes gets a whole rest.

MIDI files have no notation information: beams, articulations, voices, dynamics markings and
lyrics are not read.


## Writing

- One track per part (format 1) plus a tempo track with the tempo, time signature and key
  signature of the score (default tempo 120).
- The resolution is the divisions of the score.
- Each part gets a channel (from its MIDI instrument, otherwise in order, skipping the percussion
  channel 10) and a program change from its MIDI instrument.
- Notes are written with velocity 80; rests advance the time; chord notes start together.
