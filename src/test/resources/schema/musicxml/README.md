# MusicXML 4.0 schema

The official W3C XML schema (XSD) of MusicXML 4.0. It describes which elements and attributes a valid MusicXML
file may contain, in which order and with which values. Applications such as Dorico check imported files against
it, so the MusicXML written by toolarium-music is checked against the same rules.

Source: <https://github.com/w3c/musicxml/tree/v4.0/schema> (tag `v4.0`), unchanged copies.
Copyright 2004-2021 the Contributors to the MusicXML Specification, published by the W3C Music Notation
Community Group under the W3C Community Final Specification Agreement.

| File | Content |
|------|---------|
| `musicxml.xsd` | The MusicXML schema: scores (`score-partwise`, `score-timewise`), parts, measures, notes, directions, notations and all other elements. |
| `container.xsd` | The schema of `META-INF/container.xml` inside a compressed MusicXML file (`.mxl`), which names the root score file. |
| `xml.xsd` | Standard W3C schema of the `xml:` attributes (for example `xml:lang`), imported by `musicxml.xsd`. |
| `xlink.xsd` | Standard schema of the XLink attributes (for example `xlink:href`), imported by `musicxml.xsd`. |

`musicxml.xsd` imports `xml.xsd` and `xlink.xsd` from `http://www.musicxml.org/xsd/`. To validate without network
access, resolve these locations to the files in this folder (for example with an `LSResourceResolver`).
