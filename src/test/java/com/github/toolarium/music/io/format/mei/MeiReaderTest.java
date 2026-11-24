/*
 * MeiReaderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei;

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
import com.github.toolarium.music.model.direction.IDirection;
import com.github.toolarium.music.model.direction.IDirectionType;
import com.github.toolarium.music.model.measure.IAttributes;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;


/**
 * Tests for the MEI (.mei) reader.
 *
 * @author patrick
 */
public class MeiReaderTest {
    private static final Path MEI_DIR = TestResourceUtil.getInstance().getSamplesDir().resolve("mei");
    private static final Path REFERENCE_DIR = MEI_DIR.resolve("reference");
    private static final String MEI = ".mei";

    /**
     * Samples where the MuseScore reference differs from the MEI content: MuseScore 5 stores a tie
     * on its end note (the MuseScore reader reads it as a tie start), and MuseScore gives both notes
     * of a two-note tremolo the full value.
     */
    private static final Set<String> REFERENCE_DIFFERENCES = new HashSet<>(Arrays.asList(
            "color-01.mei", "ftrem-01.mei", "lyric-03.mei", "tie-01.mei", "trill-01.mei"));


    /**
     * Tests that the factory returns a reader and that the format is detected by extension.
     */
    @Test
    void testFactoryAndFormatDetection() {
        assertNotNull(MeiIOFactory.getInstance().getReader());
        assertEquals(MusicFileFormat.MEI, MusicFileFormat.fromFileName("Score.MEI"));
    }


    /**
     * Tests that every MEI sample can be read.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadAllSamples() throws IOException {
        List<Path> files = TestResourceUtil.getInstance().findFilesByExtension(MEI);
        assertTrue(files.size() >= 60, "samples: " + files.size());
        List<String> failures = TestResourceUtil.getInstance().processFiles(files, file -> {
            IScore score = MusicFileFactory.getInstance().read(file);
            assertFalse(score.getParts().isEmpty(), file.toString());
            assertFalse(score.getParts().get(0).getMeasures().isEmpty(), file.toString());
            return null;
        });
        assertTrue(failures.isEmpty(), String.join(System.lineSeparator(), failures));
    }


    /**
     * Compares the sounding notes of every sample with the MuseScore reference (MEI import by MuseScore).
     *
     * @throws Exception if reading fails
     */
    @Test
    void testCompareWithMuseScoreReference() throws Exception {
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        List<Path> compareFiles = new ArrayList<>();
        for (Path file : TestResourceUtil.getInstance().findFilesByExtension(MEI)) {
            String name = file.getFileName().toString();
            Path reference = REFERENCE_DIR.resolve(name.replace(MEI, ".mscx"));
            if (!Files.exists(reference) || REFERENCE_DIFFERENCES.contains(name)) {
                continue;
            }
            compareFiles.add(file);
        }
        List<String> failures = TestResourceUtil.getInstance().processFiles(compareFiles, file -> {
            String name = file.getFileName().toString();
            Path reference = REFERENCE_DIR.resolve(name.replace(MEI, ".mscx"));
            IScore expected = MusicFileFactory.getInstance().read(reference, MusicFileFormat.MUSESCORE_XML);
            return comparator.compare(name, comparator.readConverted(expected), comparator.readConverted(MusicFileFactory.getInstance().read(file)));
        });
        assertTrue(compareFiles.size() >= 55, "compared: " + compareFiles.size());
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }


    /**
     * Tests an inline MEI document: header, parts from staff groups, key and meter, gestural
     * accidentals, chords, tuplets, lyrics, endings, repeats and control events.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadInlineDocument() throws IOException {
        IScore score = read(document());
        assertEquals("Inline", score.getWork().getWorkTitle());
        assertEquals("J. Doe", score.getIdentification().getCreators().get(0).getValue());
        assertEquals(2, score.getParts().size());
        assertEquals("Piano", score.getPartList().getScoreParts().get(1).getPartName());

        IPart violin = score.getParts().get(0);
        IAttributes attributes = violin.getMeasures().get(0).getAttributes().get(0);
        assertEquals(2, attributes.getKeys().get(0).getFifths());
        assertEquals("3", attributes.getTimes().get(0).getBeats());
        List<? extends INote> first = violin.getMeasures().get(0).getNotes();
        // f in D major sounds as f sharp (accid.ges), c is given with a written natural
        assertEquals(1.0, first.get(0).getPitch().getAlter());
        assertEquals("natural", first.get(1).getAccidental());
        assertEquals("", first.get(3).getChord());
        assertEquals("la", first.get(0).getLyrics().get(0).getText());
        assertEquals("start", first.get(0).getNotations().getSlurs().get(0).getType());
        assertTrue(hasDynamics(violin.getMeasures().get(0)));

        List<? extends INote> second = violin.getMeasures().get(1).getNotes();
        assertEquals(3, second.get(0).getTimeModification().getActualNotes());
        assertEquals("start", second.get(0).getNotations().getTuplets().get(0).getType());
        assertEquals("1", ending(violin.getMeasures().get(1)));
        assertEquals("backward", violin.getMeasures().get(1).getBarlines().get(1).getRepeat().getDirection());

        IPart piano = score.getParts().get(1);
        IAttributes pianoAttributes = piano.getMeasures().get(0).getAttributes().get(0);
        assertEquals(2, pianoAttributes.getStaves());
        assertEquals("F", pianoAttributes.getClefs().get(1).getSign());
        assertEquals("yes", piano.getMeasures().get(0).getNotes().get(0).getRest().getMeasure());
    }


    /**
     * Tests that a document without the mei root element is rejected.
     */
    @Test
    void testRejectInvalidInput() {
        assertThrows(IOException.class, () -> read("<score/>"));
        assertThrows(IOException.class, () -> read("not xml"));
    }


    /**
     * Reads an MEI document from a string.
     *
     * @param mei the document
     * @return the score
     * @throws IOException if reading fails
     */
    private IScore read(String mei) throws IOException {
        return MusicFileFactory.getInstance().read(new ByteArrayInputStream(mei.getBytes(StandardCharsets.UTF_8)), MusicFileFormat.MEI);
    }


    /**
     * Creates the inline test document.
     *
     * @return the MEI document
     */
    private String document() {
        return "<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.0\">"
                + "<meiHead><fileDesc><titleStmt><title>Inline</title><respStmt><persName role=\"composer\">J. Doe</persName></respStmt>"
                + "</titleStmt></fileDesc></meiHead>"
                + "<music><body><mdiv><score><scoreDef keysig=\"2s\" meter.count=\"3\" meter.unit=\"4\"><staffGrp>"
                + "<staffDef n=\"1\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"><label>Violin</label></staffDef>"
                + "<staffGrp symbol=\"brace\"><label>Piano</label>"
                + "<staffDef n=\"2\" lines=\"5\" clef.shape=\"G\" clef.line=\"2\"/><staffDef n=\"3\" lines=\"5\" clef.shape=\"F\" clef.line=\"4\"/>"
                + "</staffGrp></staffGrp></scoreDef>"
                + "<section><measure n=\"1\"><staff n=\"1\"><layer n=\"1\">"
                + "<note xml:id=\"n1\" dur=\"4\" pname=\"f\" oct=\"4\" accid.ges=\"s\"><verse n=\"1\"><syl>la</syl></verse></note>"
                + "<note xml:id=\"n2\" dur=\"4\" pname=\"c\" oct=\"5\"><accid accid=\"n\"/></note>"
                + "<chord dur=\"4\"><note pname=\"d\" oct=\"4\"/><note pname=\"a\" oct=\"4\"/></chord>"
                + "</layer></staff><staff n=\"2\"><layer n=\"1\"><mRest/></layer></staff><staff n=\"3\"><layer n=\"1\"><mRest/></layer></staff>"
                + "<slur startid=\"#n1\" endid=\"#n2\"/><dynam staff=\"1\" tstamp=\"1\">mf</dynam></measure>"
                + "<ending n=\"1\"><measure n=\"2\" right=\"rptend\"><staff n=\"1\"><layer n=\"1\">"
                + "<tuplet num=\"3\" numbase=\"2\"><note dur=\"8\" pname=\"e\" oct=\"4\"/><note dur=\"8\" pname=\"f\" oct=\"4\"/>"
                + "<note dur=\"8\" pname=\"g\" oct=\"4\"/></tuplet><note dur=\"2\" pname=\"a\" oct=\"4\"/>"
                + "</layer></staff><staff n=\"2\"><layer n=\"1\"><mRest/></layer></staff><staff n=\"3\"><layer n=\"1\"><mRest/></layer></staff>"
                + "</measure></ending></section></score></mdiv></body></music></mei>";
    }


    /**
     * Gets the ending number that starts in a measure.
     *
     * @param measure the measure
     * @return the ending number, or null
     */
    private String ending(IMeasure measure) {
        if (measure.getBarlines() == null) {
            return null;
        }
        for (IBarline barline : measure.getBarlines()) {
            if (barline.getEnding() != null && "start".equals(barline.getEnding().getType())) {
                return barline.getEnding().getNumber();
            }
        }
        return null;
    }


    /**
     * Checks if a measure has a dynamics direction.
     *
     * @param measure the measure
     * @return true if a dynamics direction exists
     */
    private boolean hasDynamics(IMeasure measure) {
        if (measure.getDirections() == null) {
            return false;
        }
        for (IDirection direction : measure.getDirections()) {
            for (IDirectionType type : direction.getDirectionTypes()) {
                if (type.getDynamics() != null) {
                    return true;
                }
            }
        }
        return false;
    }
}
