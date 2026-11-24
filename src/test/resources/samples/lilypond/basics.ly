\version "2.24.0"

\header {
  title = "Basics"
  composer = "toolarium"
}

\score {
  \new Staff {
    \tempo 4 = 96
    \time 4/4
    \key d \major
    \clef treble
    d'4 e'4 fis'4 g'4 |
    a'8 b'8 cis''8 d''8 e''2 |
    d''4. cis''8 b'4 r4 |
    \clef alto
    \key bes \major
    bes2 c'4 d'4 |
    es'4 f'8 g'8 a'2 |
    \clef bass
    \time 3/4
    \key c \major
    c4 e,4 g,4 |
    c,2. |
    fisis4 geses4 r4 |
    c1*3/4 \bar "|."
  }
}
