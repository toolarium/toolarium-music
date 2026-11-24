/*
 * SibeliusNoteExtractor.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius.impl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Extracts note records from a decompressed Sibelius payload.
 *
 * <p>Note records are identified by the byte pattern {@code 01 4B 00} followed by
 * two pitch bytes: the diatonic pitch (7 per octave, 35 = middle C) and the MIDI
 * pitch doubled (120 = middle C). The duration type code is stored 7 bytes before
 * the pattern start.</p>
 *
 * @author patrick
 */
final class SibeliusNoteExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(SibeliusNoteExtractor.class);
    private static final byte[] NOTE_MARKER = {0x01, 0x4B, 0x00};
    private static final int DURATION_OFFSET = -7;
    private static final int DIATONIC_OFFSET = 3;
    private static final int MIDI2_OFFSET = 4;
    private static final int DIATONIC_MIDDLE_C = 35;
    private static final int MIDI_MIDDLE_C_DOUBLED = 120;
    private static final int LOOKBACK_RANGE = 900;
    private static final String[] STEP_NAMES = {"C", "D", "E", "F", "G", "A", "B"};

    private static final byte[] EXPRESSION_STYLE = toUtf16BE("text.staff.expression");
    private static final byte[] LYRIC_LINE = toUtf16BE("line.staff.lyric");
    private static final byte[] OPUS_TEXT = toUtf16BE("Opus Text");


    /**
     * Constructor.
     */
    SibeliusNoteExtractor() {
        // package-private
    }


    /**
     * Extracts all note records from the decompressed payload.
     *
     * @param payload the decompressed payload bytes
     * @return the list of extracted notes, ordered by their position in the file
     */
    List<SibNote> extractNotes(byte[] payload) {
        List<SibNote> notes = new ArrayList<>();
        int pos = 0;
        int previousNoteEnd = 0;

        while (pos < payload.length - 5) {
            int idx = findPattern(payload, NOTE_MARKER, pos);
            if (idx == -1 || idx + MIDI2_OFFSET >= payload.length) {
                // no further note, or a note marker at the end of a truncated file
                break;
            }

            int diatonic = payload[idx + DIATONIC_OFFSET] & 0xFF;
            int midi2 = payload[idx + MIDI2_OFFSET] & 0xFF;

            // Validate: diatonic pitch should be reasonable (14-70 covers most instruments)
            // and midi2 should be approximately diatonic * some factor
            if (diatonic >= 7 && diatonic <= 77 && midi2 > 0 && midi2 < 255) {
                int midiPitch = midi2 / 2;
                int durOffset = idx + DURATION_OFFSET;
                int durCode = 0;
                if (durOffset >= 0) {
                    durCode = payload[durOffset] & 0xFF;
                }

                // Additional validation: the duration code should be in range 4-11
                if (durCode >= 4 && durCode <= 11) {
                    String step = STEP_NAMES[diatonic % 7];
                    int octave = (diatonic / 7) - 1;
                    int duration = durationCodeToTicks(durCode);
                    String type = durationCodeToType(durCode);

                    // Scan backwards for dynamics and lyrics attached to this note
                    int searchStart = previousNoteEnd;
                    String dynamic = findDynamic(payload, searchStart, idx);
                    String lyricText = findLyric(payload, searchStart, idx);

                    SibNote note = new SibNote(idx, step, octave, midiPitch, diatonic,
                            duration, type, dynamic, lyricText);
                    notes.add(note);
                }
            }

            previousNoteEnd = idx + 32;
            pos = idx + 1;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Extracted {} notes from payload.", notes.size());
        }

        return notes;
    }


    /**
     * Searches for a byte pattern in the data starting from the given offset.
     *
     * @param data the data to search
     * @param pattern the pattern to find
     * @param fromIndex the starting index
     * @return the index of the pattern, or -1 if not found
     */
    private int findPattern(byte[] data, byte[] pattern, int fromIndex) {
        int limit = data.length - pattern.length;
        for (int i = fromIndex; i <= limit; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Scans the region before a note for a dynamic marking.
     *
     * <p>Dynamics are stored as ExpressionTextItem objects with the style
     * {@code text.staff.expression} and formatted text using the Opus Text font
     * where f=forte, p=piano, m=mezzo, etc.</p>
     *
     * @param payload the decompressed payload
     * @param searchStart the start of the search region
     * @param notePos the position of the note marker
     * @return the dynamic marking (e.g., "f", "p", "mf"), or null if none found
     */
    private String findDynamic(byte[] payload, int searchStart, int notePos) {
        int lookStart = Math.max(searchStart, notePos - LOOKBACK_RANGE);
        int exprPos = findPatternBackward(payload, EXPRESSION_STYLE, lookStart, notePos);
        if (exprPos < 0) {
            return null;
        }

        // The formatted text is before the expression style marker.
        // It contains "\i\\fOpus Text\f" followed by the dynamic character.
        // Search backwards from the expression marker for the Opus Text reference.
        int opusPos = findPatternBackward(payload, OPUS_TEXT, lookStart, exprPos);
        if (opusPos < 0) {
            return null;
        }

        // The dynamic text is the visible characters in the formatted string.
        // After "Opus Text\f", the remaining text before style marker is the dynamic.
        // Extract visible chars between the two \f markers.
        // For now, scan the region for known dynamic characters in the formatted string.
        int scanStart = opusPos + OPUS_TEXT.length;
        int scanEnd = Math.min(exprPos, scanStart + 40);
        StringBuilder dynamicText = new StringBuilder();
        for (int i = scanStart; i < scanEnd - 1; i += 2) {
            int hi = payload[i] & 0xFF;
            int lo = payload[i + 1] & 0xFF;
            if (hi == 0 && lo == 0x5C) {
                // Backslash = formatting command, skip until next backslash
                i += 2;
                while (i < scanEnd - 1) {
                    int h2 = payload[i] & 0xFF;
                    int l2 = payload[i + 1] & 0xFF;
                    if (h2 == 0 && l2 == 0x5C) {
                        break;
                    }
                    i += 2;
                }
                continue;
            }
            if (hi == 0 && lo >= 0x20 && lo < 0x7F && lo != 0x5C) {
                char c = (char) lo;
                if (c == 'f' || c == 'p' || c == 'm' || c == 's' || c == 'z'
                        || c == 'r' || c == 'n') {
                    dynamicText.append(c);
                }
            }
        }

        String result = dynamicText.toString().trim();
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }


    /**
     * Scans the region before a note for a lyric text.
     *
     * <p>Lyrics are stored as Lyric objects with the style {@code line.staff.lyric}
     * and the text as a length-prefixed UTF-16BE string.</p>
     *
     * @param payload the decompressed payload
     * @param searchStart the start of the search region
     * @param notePos the position of the note marker
     * @return the lyric text, or null if none found
     */
    private String findLyric(byte[] payload, int searchStart, int notePos) {
        int lookStart = Math.max(searchStart, notePos - LOOKBACK_RANGE);
        int lyricPos = findPatternBackward(payload, LYRIC_LINE, lookStart, notePos);
        if (lyricPos < 0) {
            return null;
        }

        // The lyric text is a length-prefixed UTF-16BE string near the lyric marker.
        // Scan forward from the lyric marker position for a short string.
        int scanStart = lyricPos + LYRIC_LINE.length;
        int scanEnd = Math.min(notePos, scanStart + 200);

        for (int i = scanStart; i < scanEnd - 4; i++) {
            int len = ((payload[i] & 0xFF) << 8) | (payload[i + 1] & 0xFF);
            if (len >= 2 && len <= 200 && len % 2 == 0 && i + 2 + len <= scanEnd) {
                int hi = payload[i + 2] & 0xFF;
                int lo = payload[i + 3] & 0xFF;
                // Must start with a printable ASCII char (not a control/style identifier)
                if (hi == 0 && lo >= 0x20 && lo <= 0x7E) {
                    try {
                        String text = new String(payload, i + 2, len, java.nio.charset.StandardCharsets.UTF_16BE);
                        // Filter: lyric text should be short and not look like a style name
                        if (text.length() >= 1 && text.length() <= 50
                                && !text.contains(".") && !text.contains("\\")) {
                            return text;
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        return null;
    }


    /**
     * Searches backwards for a byte pattern between two positions.
     *
     * @param data the data to search
     * @param pattern the pattern to find
     * @param start the earliest position to search
     * @param end the latest position to search
     * @return the last occurrence of the pattern in the range, or -1
     */
    private int findPatternBackward(byte[] data, byte[] pattern, int start, int end) {
        for (int i = end - pattern.length; i >= start; i--) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Converts a Java string to UTF-16BE bytes for pattern matching.
     *
     * @param s the string
     * @return the UTF-16BE encoded bytes
     */
    private static byte[] toUtf16BE(String s) {
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_16BE);
    }


    /**
     * Converts a Sibelius duration type code to tick duration in MusicXML divisions.
     *
     * <p>Duration codes confirmed by binary diffing of minimal test .sib files:
     * code 8 = quarter note (verified), code 9 = half note (verified).
     * Output is in MusicXML divisions where 256 = one quarter note.</p>
     *
     * @param code the duration type code
     * @return the duration in MusicXML divisions (256 = quarter note)
     */
    private int durationCodeToTicks(int code) {
        if (code == 3) {
            return 8;     // 128th note
        } else if (code == 4) {
            return 16;    // 64th note
        } else if (code == 5) {
            return 32;    // 32nd note
        } else if (code == 6) {
            return 64;    // 16th note
        } else if (code == 7) {
            return 128;   // eighth note
        } else if (code == 8) {
            return 256;   // quarter note
        } else if (code == 9) {
            return 512;   // half note
        } else if (code == 10) {
            return 1024;  // whole note
        } else if (code == 11) {
            return 2048;  // breve
        }
        return 256; // default to quarter
    }


    /**
     * Converts a Sibelius duration type code to a MusicXML note type name.
     *
     * <p>Duration codes confirmed by binary diffing of minimal test .sib files:
     * code 8 = quarter note (verified), code 9 = half note (verified).</p>
     *
     * @param code the duration type code
     * @return the MusicXML type name
     */
    private String durationCodeToType(int code) {
        if (code == 3) {
            return "128th";
        } else if (code == 4) {
            return "64th";
        } else if (code == 5) {
            return "32nd";
        } else if (code == 6) {
            return "16th";
        } else if (code == 7) {
            return "eighth";
        } else if (code == 8) {
            return "quarter";
        } else if (code == 9) {
            return "half";
        } else if (code == 10) {
            return "whole";
        } else if (code == 11) {
            return "breve";
        }
        return "quarter";
    }


    /**
     * Represents a note extracted from the Sibelius payload.
     *
     * @author patrick
     */
    static final class SibNote {
        private final int fileOffset;
        private final String step;
        private final int octave;
        private final int midiPitch;
        private final int diatonicPitch;
        private final int duration;
        private final String type;
        private final String dynamic;
        private final String lyricText;

        /**
         * Constructor.
         *
         * @param fileOffset the byte offset in the payload
         * @param step the note step (C, D, E, F, G, A, B)
         * @param octave the octave number (4 = middle C octave)
         * @param midiPitch the MIDI pitch (60 = middle C)
         * @param diatonicPitch the diatonic pitch (35 = middle C)
         * @param duration the duration in ticks (256 = quarter note)
         * @param type the MusicXML note type name
         * @param dynamic the dynamic marking (e.g., "f", "p"), or null
         * @param lyricText the lyric text, or null
         */
        SibNote(int fileOffset, String step, int octave, int midiPitch,
                int diatonicPitch, int duration, String type,
                String dynamic, String lyricText) {
            this.fileOffset = fileOffset;
            this.step = step;
            this.octave = octave;
            this.midiPitch = midiPitch;
            this.diatonicPitch = diatonicPitch;
            this.duration = duration;
            this.type = type;
            this.dynamic = dynamic;
            this.lyricText = lyricText;
        }

        /**
         * Gets the byte offset in the payload.
         *
         * @return the file offset
         */
        int getFileOffset() {
            return fileOffset;
        }

        /**
         * Gets the note step name.
         *
         * @return the step (C, D, E, F, G, A, B)
         */
        String getStep() {
            return step;
        }

        /**
         * Gets the octave number.
         *
         * @return the octave (4 = middle C octave)
         */
        int getOctave() {
            return octave;
        }

        /**
         * Gets the MIDI pitch.
         *
         * @return the MIDI pitch (60 = middle C)
         */
        int getMidiPitch() {
            return midiPitch;
        }

        /**
         * Gets the diatonic pitch.
         *
         * @return the diatonic pitch (35 = middle C)
         */
        int getDiatonicPitch() {
            return diatonicPitch;
        }

        /**
         * Gets the duration in ticks.
         *
         * @return the duration (256 = quarter note)
         */
        int getDuration() {
            return duration;
        }

        /**
         * Gets the MusicXML note type name.
         *
         * @return the type ("quarter", "half", "whole", etc.)
         */
        String getType() {
            return type;
        }

        /**
         * Gets the dynamic marking attached to this note.
         *
         * @return the dynamic (e.g., "f", "p", "mf"), or null
         */
        String getDynamic() {
            return dynamic;
        }

        /**
         * Gets the lyric text attached to this note.
         *
         * @return the lyric text, or null
         */
        String getLyricText() {
            return lyricText;
        }
    }
}
