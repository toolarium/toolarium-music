/*
 * MeiWriterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.MusicFileFactory;
import com.github.toolarium.music.MusicFileFormat;
import com.github.toolarium.music.TestResourceUtil;
import com.github.toolarium.music.io.SoundingNoteComparator;
import com.github.toolarium.music.model.measure.IMeasure;
import com.github.toolarium.music.model.measure.IPart;
import com.github.toolarium.music.model.note.INote;
import com.github.toolarium.music.model.score.ICredit;
import com.github.toolarium.music.model.score.IScore;
import com.thaiopensource.util.PropertyMap;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.IncorrectSchemaException;
import com.thaiopensource.validate.Schema;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.Validator;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;


/**
 * Tests for the MEI (.mei) writer: every written file is valid against the MEI 5.0 schema and reads back with the
 * same sounding notes.
 *
 * @author patrick
 */
public class MeiWriterTest {
    private static final Path SCHEMA = TestResourceUtil.getInstance().getResourcesDir().resolve("schema").resolve("mei").resolve("mei-CMN.rng");
    private static final Path DISNEY = TestResourceUtil.getInstance().getSamplesDir().resolve(TestResourceUtil.PROTECTED_SUBDIR).resolve("quintet")
            .resolve("filmmusic").resolve("Disney Love Medley").resolve("Disney Love Medley.musx");
    private static final int MAX_ERRORS = 5;
    private static Schema meiSchema;


    /**
     * Tests that the factory returns a writer.
     */
    @Test
    void testFactory() {
        assertNotNull(MeiIOFactory.getInstance().getWriter());
    }


    /**
     * Writes the samples of the text and XML formats as MEI: every file is schema valid and reads back with the same
     * sounding notes.
     *
     * @throws IOException if the samples cannot be found
     */
    @Test
    void testWriteSamplesValidAndReadBack() throws IOException {
        List<Path> files = TestResourceUtil.getInstance().findFilesByExtension(".musicxml", ".mxl", ".mei", ".abc", ".ly", ".capx", ".mscz");
        assertTrue(files.size() >= 10, "samples: " + files.size());
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        List<String> failures = TestResourceUtil.getInstance().processFiles(files, file -> {
            IScore original = MusicFileFactory.getInstance().read(file);
            byte[] mei = write(original);
            String errors = validate(mei);
            if (errors != null) {
                return file.getFileName() + ": " + errors;
            }
            IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(mei), MusicFileFormat.MEI);
            assertEquals(original.getParts().size(), readBack.getParts().size(), file.toString());
            return comparator.compare(file.getFileName().toString(), comparator.readConverted(original), comparator.readConverted(readBack));
        });
        assertTrue(failures.isEmpty(), String.join(System.lineSeparator(), failures));
    }


    /**
     * Tests the content of a written score: header, staff definitions, tuplets, chord symbols and lyrics.
     *
     * @throws IOException if reading or writing fails
     */
    @Test
    void testWriteContent() throws IOException {
        Path lilypond = TestResourceUtil.getInstance().getSamplesDir().resolve("lilypond");
        String tuplets = new String(write(MusicFileFactory.getInstance().read(lilypond.resolve("tuplets-chords.ly"))), StandardCharsets.UTF_8);
        assertTrue(tuplets.contains("<mei xmlns=\"http://www.music-encoding.org/ns/mei\" meiversion=\"5.0\">"));
        assertTrue(tuplets.contains("<title>Tuplets and Chords</title>"));
        assertTrue(tuplets.contains("<composer>toolarium</composer>"));
        assertTrue(tuplets.contains("<tuplet num=\"3\" numbase=\"2\">"));
        assertTrue(tuplets.contains("<tuplet num=\"5\" numbase=\"4\">"));
        assertTrue(tuplets.contains(">G7</harm>"));
        assertTrue(tuplets.contains("<layer n=\"2\">"));

        String lyrics = new String(write(MusicFileFactory.getInstance().read(lilypond.resolve("lyrics.ly"))), StandardCharsets.UTF_8);
        assertTrue(lyrics.contains("<syl wordpos=\"i\" con=\"d\">Sing</syl>"));
        assertTrue(lyrics.contains("<syl wordpos=\"t\">ing</syl>"));
    }


    /**
     * Writes the Disney Love Medley (Finale source, protected sample of the integration tests) as MEI: the file is
     * schema valid and reads back with the same parts, measures, notes, transpositions and page texts.
     *
     * @throws IOException if reading or writing fails
     */
    @Test
    void testWriteDisneyLoveMedley() throws IOException {
        TestResourceUtil.getInstance().assumeSamples(TestResourceUtil.getInstance().isIntegrationTest() && Files.exists(DISNEY),
                "Disney Love Medley is a protected sample of the integration tests.");
        IScore original = MusicFileFactory.getInstance().read(DISNEY);
        byte[] mei = write(original);
        String errors = validate(mei);
        assertEquals(null, errors);
        String text = new String(mei, StandardCharsets.UTF_8);
        assertTrue(text.contains("<title>Disney Love Medley</title>"));
        assertTrue(text.contains("<label>Horn in F</label>"));
        assertTrue(text.contains("trans.diat=\"-4\" trans.semi=\"-7\""));

        IScore readBack = MusicFileFactory.getInstance().read(new ByteArrayInputStream(mei), MusicFileFormat.MEI);
        assertEquals(original.getParts().size(), readBack.getParts().size());
        assertEquals(original.getParts().get(0).getMeasures().size(), readBack.getParts().get(0).getMeasures().size());
        assertEquals(notes(original), notes(readBack));
        SoundingNoteComparator comparator = SoundingNoteComparator.getInstance();
        assertEquals(null, comparator.compare("Disney Love Medley", comparator.readConverted(original), comparator.readConverted(readBack)));

        // the page texts: title page, running header with page number and footer on all 11 pages
        assertEquals(original.getCredits().size(), readBack.getCredits().size());
        assertTrue(text.contains("<pgHead func=\"first\">"));
        assertTrue(text.contains("<num label=\"page\">"));
        int footers = 0;
        for (ICredit credit : readBack.getCredits()) {
            if (credit.getCreditWords() != null && "- PM -".equals(credit.getCreditWords().getValue())) {
                footers++;
            }
        }
        assertEquals(10, footers);
    }


    /**
     * Writes a score as MEI.
     *
     * @param score the score
     * @return the MEI document
     * @throws IOException if writing fails
     */
    private byte[] write(IScore score) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MusicFileFactory.getInstance().write(score, out, MusicFileFormat.MEI);
        return out.toByteArray();
    }


    /**
     * Validates an MEI document against the MEI 5.0 schema.
     *
     * @param mei the document
     * @return the first errors, or null if the document is valid
     * @throws IOException if the schema cannot be read
     */
    private String validate(byte[] mei) throws IOException {
        List<String> errors = new ArrayList<>();
        PropertyMapBuilder properties = new PropertyMapBuilder();
        properties.put(ValidateProperty.ERROR_HANDLER, new ErrorHandler() {
            /**
             * Ignores a warning.
             *
             * @param exception the warning
             */
            @Override
            public void warning(SAXParseException exception) {
                // warnings are not errors
            }

            /**
             * Collects an error.
             *
             * @param exception the error
             */
            @Override
            public void error(SAXParseException exception) {
                if (errors.size() < MAX_ERRORS) {
                    errors.add(exception.getLineNumber() + ": " + exception.getMessage());
                }
            }

            /**
             * Collects a fatal error.
             *
             * @param exception the error
             */
            @Override
            public void fatalError(SAXParseException exception) {
                error(exception);
            }
        });
        try {
            Validator validator = schema().createValidator(properties.toPropertyMap());
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setContentHandler(validator.getContentHandler());
            if (validator.getDTDHandler() != null) {
                reader.setDTDHandler(validator.getDTDHandler());
            }
            reader.parse(new InputSource(new ByteArrayInputStream(mei)));
            if (errors.isEmpty()) {
                return null;
            }
        } catch (SAXException | ParserConfigurationException e) {
            errors.add(e.getMessage());
        }
        return String.join("; ", errors);
    }


    /**
     * Gets the MEI 5.0 schema: it is read once and shared by all validations (a schema is thread safe, a validator
     * is created per document).
     *
     * @return the schema
     * @throws IOException if the schema cannot be read
     */
    private static synchronized Schema schema() throws IOException {
        if (meiSchema == null) {
            try {
                meiSchema = new AutoSchemaReader().createSchema(ValidationDriver.fileInputSource(SCHEMA.toFile()), PropertyMap.EMPTY);
            } catch (SAXException | IncorrectSchemaException e) {
                throw new IOException("The MEI schema cannot be loaded: " + SCHEMA, e);
            }
        }
        return meiSchema;
    }


    /**
     * Counts the notes (without rests) of a score.
     *
     * @param score the score
     * @return the number of notes
     */
    private int notes(IScore score) {
        int result = 0;
        for (IPart part : score.getParts()) {
            for (IMeasure measure : part.getMeasures()) {
                for (INote note : measure.getNotes()) {
                    if (note.getRest() == null) {
                        result++;
                    }
                }
            }
        }
        return result;
    }
}
