# MIDI test samples

Source: [jazz-soft/test-midi-files](https://github.com/jazz-soft/test-midi-files) repository on the
`main` branch, downloaded on 2026-11-14.

License: MIT. These files are used only as test input and are not part of the toolarium library itself.

The 11 files in this folder are synthetic test cases covering different MIDI features.
They are used by the round-trip and read-all tests:

| File | Purpose |
|------|---------|
| `test-2-tracks-type-0.mid` | SMF Type 0 (single track) with two tracks merged |
| `test-2-tracks-type-1.mid` | SMF Type 1 (multi track) |
| `test-c-major-scale.mid` | Simple C major scale |
| `test-all-gm-sounds.mid` | All General MIDI instrument sounds |
| `test-note-on-velocity.mid` | Note-on velocity variations |
| `test-control-40-damper.mid` | Damper pedal control change |
| `test-karaoke-kar.mid` | Karaoke format with lyrics |
| `test-multichannel-chords-1.mid` | Multi-channel chord events |
| `test-tempo-events-track-smf1.mid` | Tempo change events in SMF1 |
| `test-running-status-sysex.mid` | Running status interrupted by SysEx |
| `test-smpte-offset.mid` | SMPTE time offset |

## Edge cases (`edge-cases/`)

The `edge-cases/` subfolder contains 5 files that are intentionally invalid, corrupt, or use
unsupported formats. They are excluded from the round-trip scan and exercised by dedicated
tests in `MidiEdgeCaseTest` that verify the reader handles them gracefully:

| File | Purpose |
|------|---------|
| `test-not-a-midi-file.mid` | Not a MIDI file (plain text with .mid extension) |
| `test-empty.mid` | Valid MThd header and one track with only the end of track event (read as a score without parts) |
| `test-corrupt-file-extra-byte.mid` | Valid MIDI with one extra byte appended |
| `test-corrupt-file-missing-byte.mid` | Valid MIDI with one byte removed |
| `test-2-tracks-type-2.mid` | SMF Type 2 (independent tracks, rarely supported) |
