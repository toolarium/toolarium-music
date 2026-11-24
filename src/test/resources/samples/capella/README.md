# capella test samples

Source: [musescore/MuseScore](https://github.com/musescore/MuseScore) repository on the `master`
branch, folder `src/importexport/capella/tests/data`, downloaded on 2026-11-16.

- `*.capx`: 18 CapXML files (ZIP container with `score.xml`)
- `*.cap`: 15 legacy binary capella files: 9 with a reference, `test1-extra-staff.cap`, and 5 corrupt
  variants that the reader must reject

License: GPLv3. These files are used only as test input and are not part of the toolarium
library itself.

## Reference exports (`reference/`)

`reference/<name>.capx.mscx` and `reference/<name>.cap.mscx` are the MuseScore imports of the samples
as stored in the MuseScore test suite (`<name>.capx-ref.mscx`, `<name>.cap-ref.mscx`). All 9 `.cap`
references match. `CapellaReaderTest` compares the sounding notes of the reader
with these references. Two references differ from the capella content and are excluded:

- `testScaleC4C5`: MuseScore ignores the default time (3/4) of a staff without time signature.
- `testSlurTie`: MuseScore 5 stores a tie on its end note, which the MuseScore reader reads as a tie start.
