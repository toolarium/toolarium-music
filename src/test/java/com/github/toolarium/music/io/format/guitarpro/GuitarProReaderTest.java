/*
 * GuitarProReaderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.SoundingNoteComparator;
import com.github.toolarium.music.model.barline.IBarline;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.notation.IPlayingTechnique;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;


/**
 * Tests for the Guitar Pro reader (GP3, GP4, GP5, GPX and GP).
 *
 * @author patrick
 */
public class GuitarProReaderTest {
    private static final Path GP_DIR = TestResourceUtil.getInstance().getSamplesDir().resolve("guitarpro");

    /**
     * Songs with the same name but a different content in two versions of the test files.
     */
    private static final Set<String> DIFFERENT_SONGS = new HashSet<>(Arrays.asList(
            "gp5/ottavia", "gp4/harmonic-types", "gp4/score-info", "gp6/beaming-mode", "gp6/slash", "gp7/slash"));


    /**
     * Tests that the factory returns a reader and that the formats are detected by extension.
     */
    @Test
    void testFactoryAndFormatDetection() {
        assertNotNull(GuitarProIOFactory.getInstance().getReader());
        for (String name : new String[] {"a.gp", "a.gpx", "a.gp5", "a.GP4", "a.gp3"}) {
            assertEquals(MusicFileFormat.GUITAR_PRO, MusicFileFormat.fromFileName(name));
        }
    }


    /**
     * Tests that every Guitar Pro sample (GP3 to GP8) can be read.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadAllSamples() throws IOException {
        List<Path> files = TestResourceUtil.getInstance().findFilesByExtension(".gp", ".gpx", ".gp5", ".gp4", ".gp3");
        assertTrue(files.size() >= 220, "samples: " + files.size());
        List<String> failures = TestResourceUtil.getInstance().processFiles(files, file -> {
            IScore score = MusicFileFactory.getInstance().read(file);
            assertFalse(score.getParts().isEmpty(), file.toString());
            assertFalse(score.getParts().get(0).getMeasures().isEmpty(), file.toString());
            return null;
        });
        assertTrue(failures.isEmpty(), String.join(System.lineSeparator(), failures));
    }


    /**
     * Compares the sounding notes of the same test song saved by different Guitar Pro versions:
     * the binary reader (GP3 to GP5) and the GPIF reader (GP6 to GP8) must agree.
     *
     * @throws Exception if reading fails
     */
    @Test
    void testCompareVersions() throws Exception {
        String[][] pairs = {{"gp3", ".gp3", "gp4", ".gp4"}, {"gp4", ".gp4", "gp5", ".gp5"}, {"gp5", ".gp5", "gp7", ".gp"},
            {"gp6", ".gpx", "gp7", ".gp"}, {"gp7", ".gp", "gp8", ".gp"}};
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        List<Path> compareFiles = new ArrayList<>();
        Map<Path, Path> others = new HashMap<>();
        Map<Path, String> songs = new HashMap<>();
        for (String[] pair : pairs) {
            List<Path> files = new ArrayList<>();
            try (Stream<Path> list = Files.list(GP_DIR.resolve(pair[0]))) {
                list.filter(p -> p.toString().endsWith(pair[1])).sorted().forEach(files::add);
            }
            for (Path file : files) {
                String song = file.getFileName().toString().replace(pair[1], "");
                Path other = GP_DIR.resolve(pair[2]).resolve(song + pair[3]);
                if (!Files.exists(other) || DIFFERENT_SONGS.contains(pair[0] + "/" + song)) {
                    continue;
                }
                compareFiles.add(file);
                others.put(file, other);
                songs.put(file, pair[0] + "/" + song);
            }
        }
        List<String> failures = TestResourceUtil.getInstance().processFiles(compareFiles,
            file -> comparator.compare(songs.get(file), comparator.readConverted(MusicFileFactory.getInstance().read(others.get(file))),
                                       comparator.readConverted(MusicFileFactory.getInstance().read(file))));
        assertTrue(compareFiles.size() >= 100, "compared: " + compareFiles.size());
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }


    /**
     * Tests the metadata and tracks of the score information sample (GP5 and GP7).
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadScoreInfo() throws IOException {
        for (String file : new String[] {"gp5/score-info.gp5", "gp7/score-info.gp"}) {
            IScore score = MusicFileFactory.getInstance().read(GP_DIR.resolve(file));
            assertEquals("Title", score.getWork().getWorkTitle());
            assertEquals("Music", score.getIdentification().getCreators().get(0).getValue());
            assertEquals("Words", score.getIdentification().getCreators().get(1).getValue());
            assertEquals("Copyright", score.getIdentification().getRights());
            assertEquals(2, score.getParts().size());
            assertEquals("Track 1", score.getPartList().getScoreParts().get(0).getPartName());
        }
    }


    /**
     * Tests the guitar techniques of the effects sample: string and fret, hammer-on and pull-off,
     * slides, harmonics and dead notes, and the octave transposition of the guitar.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadEffects() throws IOException {
        IScore score = MusicFileFactory.getInstance().read(GP_DIR.resolve("gp5/effects.gp5"));
        IPart part = score.getParts().get(0);
        assertEquals(-1, part.getMeasures().get(0).getAttributes().get(0).getTransposes().get(0).getOctaveChange());
        int strings = 0;
        int hammers = 0;
        int slides = 0;
        int harmonics = 0;
        int dead = 0;
        for (IMeasure measure : part.getMeasures()) {
            for (INote note : measure.getNotes()) {
                if (note.getNotations() == null) {
                    continue;
                }
                IPlayingTechnique technical = note.getNotations().getTechnical();
                if (technical != null && technical.getString() != null && technical.getFret() != null) {
                    strings++;
                }
                if (technical != null && (technical.getHammerOn() != null || technical.getPullOff() != null)) {
                    hammers++;
                }
                if (technical != null && technical.getHarmonic() != null) {
                    harmonics++;
                }
                if (note.getNotations().getGlissandos() != null) {
                    slides++;
                }
                if ("x".equals(note.getNotehead())) {
                    dead++;
                }
            }
        }
        assertTrue(strings > 50, "string and fret: " + strings);
        assertTrue(hammers > 0, "hammer-on and pull-off: " + hammers);
        assertTrue(slides > 0, "slides: " + slides);
        assertTrue(harmonics > 0, "harmonics: " + harmonics);
        assertTrue(dead > 0, "dead notes: " + dead);
    }


    /**
     * Tests a drum track: percussion clef and unpitched notes.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadDrums() throws IOException {
        IPart part = MusicFileFactory.getInstance().read(GP_DIR.resolve("gp7/drum-tabs.gp")).getParts().get(0);
        assertEquals("percussion", part.getMeasures().get(0).getAttributes().get(0).getClefs().get(0).getSign());
        INote note = null;
        for (INote candidate : part.getMeasures().get(0).getNotes()) {
            if (candidate.getRest() == null) {
                note = candidate;
                break;
            }
        }
        assertNotNull(note);
        assertNotNull(note.getUnpitched());
    }


    /**
     * Tests repeats and alternate endings.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadRepeatsAndEndings() throws IOException {
        for (String file : new String[] {"gp5/repeat-close-alternate-endings.gp5", "gp7/repeat-close-alternate-endings.gp"}) {
            IPart part = MusicFileFactory.getInstance().read(GP_DIR.resolve(file)).getParts().get(0);
            assertEquals("forward", barline(part.getMeasures().get(0), "left").getRepeat().getDirection());
            assertEquals("1, 3", barline(part.getMeasures().get(1), "left").getEnding().getNumber());
            assertEquals("2", barline(part.getMeasures().get(2), "left").getEnding().getNumber());
        }
    }


    /**
     * Tests that invalid input is rejected.
     */
    @Test
    void testRejectInvalidInput() {
        assertThrows(IOException.class, () -> MusicFileFactory.getInstance().read(new ByteArrayInputStream(new byte[] {1, 2, 3}), MusicFileFormat.GUITAR_PRO));
        byte[] header = "\u0018FICHIER GUITAR PRO v9.99".getBytes(StandardCharsets.ISO_8859_1);
        assertThrows(IOException.class, () -> MusicFileFactory.getInstance().read(new ByteArrayInputStream(header), MusicFileFormat.GUITAR_PRO));
        assertThrows(IOException.class, () -> MusicFileFactory.getInstance().read(new ByteArrayInputStream("BCFZ\u0010\u0000\u0000\u0000".getBytes(StandardCharsets.ISO_8859_1)), MusicFileFormat.GUITAR_PRO));
    }


    /**
     * Gets the barline of a measure at a location.
     *
     * @param measure the measure
     * @param location left or right
     * @return the barline
     */
    private IBarline barline(IMeasure measure, String location) {
        for (IBarline barline : measure.getBarlines()) {
            if (location.equals(barline.getLocation())) {
                return barline;
            }
        }
        return null;
    }
}
