\version "2.24.0"

\header {
  title = "Lyrics"
  composer = "toolarium"
}

\score {
  \new Staff {
    \time 3/4
    \key g \major
    \clef treble
    g'4 a'4 b'4 |
    d''2 b'4 |
    c''4 b'4 a'4 |
    g'2. |
  }
  \addlyrics {
    Sing -- ing a tune, __ hap -- py and new
  }
}
