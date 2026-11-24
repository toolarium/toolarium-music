# MEI 5.0 schema

The official RELAX NG schema of MEI 5.0 for common music notation (the `mei-CMN` customization). It describes
which elements and attributes a valid MEI file may contain, so the MEI written by toolarium-music is checked
against the same rules.

Source: <https://music-encoding.org/schema/5.0/mei-CMN.rng>, unchanged copy.
Published by the Music Encoding Initiative, licensed under the Educational Community License version 2.0
(<https://opensource.org/license/ECL-2.0/>).

| File | Content |
|------|---------|
| `mei-CMN.rng` | The MEI 5.0 schema for common music notation: header, score definitions, sections, measures, staves, layers, notes and control events. |

RELAX NG is not supported by `javax.xml.validation`; the tests validate with Jing (`org.relaxng:jing`).
