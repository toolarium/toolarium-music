/*
 * MusicXMLDocumentOrderTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musicxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.format.musicxml.impl.MusicXMLIOFactory;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.measure.impl.MeasureContentMapping;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Tests that the MusicXML reader and writer keep the measure content in document order: the order of
 * notes, backups, forwards, attributes and directions defines the time position of each element.
 *
 * @author patrick
 */
public class MusicXMLDocumentOrderTest {
    private static final String NOTE = "note";
    private static final String BACKUP = "backup";
    private static final String MEASURE = "measure";
    private static final String MUSICXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>\n"
            + "<part id=\"P1\"><measure number=\"1\" width=\"200\">\n"
            + "<print/>\n"
            + "<attributes><divisions>1</divisions><time><beats>2</beats><beat-type>4</beat-type></time></attributes>\n"
            + "<direction placement=\"below\"><direction-type><dynamics><p/></dynamics></direction-type></direction>\n"
            + "<note default-x=\"10\"><pitch><step>C</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
            + "<direction><direction-type><words>dolce</words></direction-type></direction>\n"
            + "<harmony><root><root-step>G</root-step></root><kind>major</kind></harmony>\n"
            + "<note default-x=\"40\"><pitch><step>D</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type></note>\n"
            + "<backup><duration>2</duration></backup>\n"
            + "<figured-bass><figure><figure-number>6</figure-number></figure></figured-bass>\n"
            + "<note><pitch><step>E</step><octave>4</octave></pitch><duration>1</duration><voice>2</voice><type>quarter</type></note>\n"
            + "<attributes><clef><sign>F</sign><line>4</line></clef></attributes>\n"
            + "<note><pitch><step>F</step><octave>4</octave></pitch><duration>1</duration><voice>2</voice><type>quarter</type></note>\n"
            + "<barline location=\"right\"><bar-style>light-heavy</bar-style></barline>\n"
            + "</measure></part></score-partwise>\n";


    /**
     * Tests that reading keeps every note and the document order of the measure content; unsupported
     * elements are skipped.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testReadKeepsDocumentOrder() throws IOException {
        IMeasure measure = readFirstMeasure(MUSICXML);
        assertEquals("1", measure.getNumber());
        assertEquals("200", measure.getWidth());
        assertEquals(4, measure.getNotes().size());
        assertEquals("[print, attributes, direction, note, direction, harmony, note, backup, note, attributes, note, barline]",
                names(measure.getMusicData()).toString());
        assertEquals("D", measure.getNotes().get(1).getPitch().getStep());
        assertEquals("F", measure.getNotes().get(3).getPitch().getStep());
    }


    /**
     * Tests that writing keeps the document order, and that the written file reads back unchanged.
     *
     * @throws Exception if reading, writing or parsing fails
     */
    @Test
    void testWriteKeepsDocumentOrder() throws Exception {
        IScore score = MusicXMLIOFactory.getInstance().getReader().read(new ByteArrayInputStream(MUSICXML.getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MusicXMLIOFactory.getInstance().getWriter().write(score, out);

        Document document = parse(new ByteArrayInputStream(out.toByteArray()));
        Element measure = (Element) document.getElementsByTagName(MEASURE).item(0);
        assertEquals("1", measure.getAttribute("number"));
        assertEquals("[print, attributes, direction, note, direction, harmony, note, backup, note, attributes, note, barline]",
                childNames(measure).toString());

        IScore reread = MusicXMLIOFactory.getInstance().getReader().read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(score.getParts().get(0).getMeasures().get(0), reread.getParts().get(0).getMeasures().get(0));
    }


    /**
     * Tests that a two-voice measure built from another format is written as notes of voice 1, a backup,
     * then the notes of voice 2.
     *
     * @throws IOException if reading fails
     */
    @Test
    void testBuiltVoicesAreSeparatedByBackups() throws IOException {
        String abc = "X:1\nT:Voices\nM:2/4\nL:1/4\nK:C\nV:1\nc d|\nV:2\nE F|\n";
        IScore score = MusicFileFactory.getInstance().read(new ByteArrayInputStream(abc.getBytes(StandardCharsets.UTF_8)),
                MusicFileFormat.ABC);
        for (IPart part : score.getParts()) {
            List<String> names = names(part.getMeasures().get(0).getMusicData());
            int backup = names.indexOf(BACKUP);
            if (backup >= 0) {
                assertTrue(names.subList(0, backup).contains(NOTE), names.toString());
                assertTrue(names.subList(backup, names.size()).contains(NOTE), names.toString());
            }
        }
    }


    /**
     * Tests every MusicXML and MXL sample: the reader keeps all notes of every measure (directions
     * between notes are common in files exported by notation programs).
     *
     * @throws Exception if reading fails
     */
    @Test
    void testSamplesKeepAllNotes() throws Exception {
        List<Path> files = new ArrayList<>();
        files.addAll(TestResourceUtil.getInstance().findFilesByExtension(".musicxml"));
        files.addAll(TestResourceUtil.getInstance().findFilesByExtension(".mxl"));
        assertTrue(files.size() > 0, "No MusicXML samples found.");

        List<String> failures = TestResourceUtil.getInstance().processFiles(files, file -> {
            int expected = countNotes(file);
            if (expected < 0) {
                return null;
            }
            IScore score = MusicFileFactory.getInstance().read(file);
            int actual = 0;
            for (IPart part : score.getParts()) {
                for (IMeasure measure : part.getMeasures()) {
                    if (measure.getNotes() != null) {
                        actual += measure.getNotes().size();
                    }
                }
            }
            if (actual != expected) {
                return file + ": " + expected + " notes in the file, " + actual + " read";
            }
            return null;
        });
        assertTrue(failures.isEmpty(), failures.size() + " of " + files.size() + " files lose notes:" + System.lineSeparator()
                + String.join(System.lineSeparator(), failures));
    }


    /**
     * Reads the first measure of a MusicXML text.
     *
     * @param musicXml the MusicXML text
     * @return the first measure
     * @throws IOException if reading fails
     */
    private IMeasure readFirstMeasure(String musicXml) throws IOException {
        IScore score = MusicXMLIOFactory.getInstance().getReader().read(new ByteArrayInputStream(musicXml.getBytes(StandardCharsets.UTF_8)));
        return score.getParts().get(0).getMeasures().get(0);
    }


    /**
     * Gets the MusicXML element names of measure content.
     *
     * @param content the content
     * @return the element names
     */
    private List<String> names(List<? extends IMusicDataElement> content) {
        List<String> result = new ArrayList<>();
        for (IMusicDataElement element : content) {
            result.add(MeasureContentMapping.getInstance().getElementName(element));
        }
        return result;
    }


    /**
     * Gets the names of the child elements.
     *
     * @param element the element
     * @return the child element names
     */
    private List<String> childNames(Element element) {
        List<String> result = new ArrayList<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                result.add(children.item(i).getNodeName());
            }
        }
        return result;
    }


    /**
     * Counts the note elements of a partwise MusicXML or MXL file with the DOM parser.
     *
     * @param file the file
     * @return the number of notes, or -1 if the file is not a partwise score
     * @throws Exception if reading fails
     */
    private int countNotes(Path file) throws Exception {
        Document document;
        if (file.toString().toLowerCase().endsWith(".mxl")) {
            document = null;
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(file))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (!name.startsWith("META-INF") && (name.endsWith(".xml") || name.endsWith(".musicxml"))) {
                        document = parse(new ByteArrayInputStream(zip.readAllBytes()));
                        break;
                    }
                }
            }
        } else {
            try (InputStream in = Files.newInputStream(file)) {
                document = parse(in);
            }
        }
        if (document == null || !"score-partwise".equals(document.getDocumentElement().getNodeName())) {
            return -1;
        }

        int count = 0;
        NodeList measures = document.getElementsByTagName(MEASURE);
        for (int i = 0; i < measures.getLength(); i++) {
            for (Node child = measures.item(i).getFirstChild(); child != null; child = child.getNextSibling()) {
                if (NOTE.equals(child.getNodeName())) {
                    count++;
                }
            }
        }
        return count;
    }


    /**
     * Parses XML without loading the DTD.
     *
     * @param in the input
     * @return the document
     * @throws Exception if parsing fails
     */
    private Document parse(InputStream in) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(in);
    }
}
