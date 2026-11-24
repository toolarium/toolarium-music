\version "2.24.0"

\header {
  title = "Tuplets and Chords"
  composer = "toolarium"
}

\score {
  <<
    \new ChordNames {
      \chordmode { c1 | g1:7 | a1:m | c1 }
    }
    \new Staff {
      \time 4/4
      \key c \major
      \clef treble
      \tuplet 3/2 { c'8 e'8 g'8 } \tuplet 3/2 { e'8 g'8 c''8 } \tuplet 5/4 { b'16 a'16 g'16 f'16 e'16 } d'4 |
      g'2 f'2 |
      << { e'4 g'4 a'4 c''4 } \\ { c'4 e'4 e'4 a'4 } >> |
      c''1 |
    }
  >>
}
