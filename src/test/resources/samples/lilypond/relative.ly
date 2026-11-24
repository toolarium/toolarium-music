\version "2.24.0"

\header {
  title = "Relative"
  composer = "toolarium"
}

\score {
  \new Staff {
    \tempo 4 = 120
    \time 4/4
    \key a \minor
    \clef treble
    \relative c'' {
      a4 c8. e16 a4 e8 c8 |
      b4 d8. f16 b,4 gis8 b8 |
      c8 e a c b gis e d |
      c,4 e8 a, c'2 |
      a,1 |
    }
  }
}
