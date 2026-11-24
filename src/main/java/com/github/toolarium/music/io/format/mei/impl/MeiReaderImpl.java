/*
 * MeiReaderImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.ScoreData;
import com.github.toolarium.music.io.stream.StreamContentReader;
import com.github.toolarium.music.io.xml.XmlDocumentReader;
import com.github.toolarium.music.model.score.IScore;
import com.github.toolarium.music.model.score.impl.ScoreImpl;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Implementation of {@link IMusicReader} for MEI (Music Encoding Initiative, .mei) files.
 * Supports the common music notation subset of MEI 3, 4 and 5 (including MEI basic).
 *
 * @author patrick
 */
public class MeiReaderImpl implements IMusicReader {
    private static final Logger LOG = LoggerFactory.getLogger(MeiReaderImpl.class);
    private static final int MAX_SIZE = 64 * 1024 * 1024;
    private static final int DATE_LENGTH = 10;
    private final XmlDocumentReader xml = XmlDocumentReader.getInstance();


    /**
     * Constructor for MeiReaderImpl.
     */
    public MeiReaderImpl() {
        // default constructor
    }


    /**
     * Reads an MEI file from a path.
     *
     * @param path the path to the MEI file
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(Path path) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading MEI from path: {}", path);
        }

        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


    /**
     * Reads an MEI file from an input stream.
     *
     * @param inputStream the input stream to read from
     * @return the parsed score
     * @throws IOException in case of read errors
     */
    @Override
    public IScore read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream must not be null.");
        }
        Document document = xml.parse(StreamContentReader.getInstance().readAll(inputStream, MAX_SIZE, "MEI file"), "MEI");
        Element root = document.getDocumentElement();
        if (root == null || !"mei".equals(xml.name(root))) {
            throw new IOException("Invalid MEI file: the root element is not mei.");
        }
        Element music = xml.child(root, "music");
        if (music == null) {
            throw new IOException("Invalid MEI file: no music element.");
        }
        try {
            ScoreData score = new ScoreData();
            readHeader(xml.child(root, "meiHead"), score);
            String version = xml.attr(root, "meiversion");
            if (version == null) {
                score.setSoftware("MEI");
            } else {
                score.setSoftware("MEI " + version);
            }
            new MeiBodyParser(score).parse(music);
            if (score.getTitle() == null) {
                score.setTitle(pageHeadText(music, "title"));
            }
            if (score.getSubtitle() == null) {
                score.setSubtitle(pageHeadText(music, "subtitle"));
            }
            if (score.getCreators().isEmpty()) {
                score.addCreator("composer", pageHeadText(music, "composer"));
            }
            if (score.getParts().isEmpty()) {
                throw new IOException("Invalid MEI file: no staff definitions found.");
            }
            ScoreImpl built = ScoreBuilder.getInstance().build(score);
            int measures = 0;
            if (!built.getParts().isEmpty()) {
                measures = built.getParts().get(0).getMeasures().size();
            }
            MeiPageTextReader.getInstance().apply(root, music, measures, built);
            return built;
        } catch (RuntimeException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to read MEI: {}", e.getMessage(), e);
            }
            throw new IOException("Invalid MEI file: " + e.getMessage(), e);
        }
    }


    /**
     * Reads the metadata of the MEI header: titles, creators, rights, date and application.
     *
     * @param head the meiHead element (may be null)
     * @param score the score data
     */
    private void readHeader(Element head, ScoreData score) {
        Element titleStmt = xml.path(head, "fileDesc/titleStmt");
        if (titleStmt == null) {
            Element work = xml.path(head, "workList/work");
            if (work == null) {
                work = xml.path(head, "workDesc/work");
            }
            titleStmt = work;
        }
        if (titleStmt != null) {
            for (Element title : xml.children(titleStmt, "title")) {
                String type = xml.attr(title, "type");
                String text = xml.text(title);
                if (text.isEmpty()) {
                    continue;
                }
                if ("subtitle".equals(type) || "sub".equals(type) || "subordinate".equals(type)) {
                    if (score.getSubtitle() == null) {
                        score.setSubtitle(text);
                    }
                } else if (score.getTitle() == null) {
                    score.setTitle(text);
                }
            }
            for (String role : new String[] {"composer", "lyricist", "arranger", "author", "librettist"}) {
                for (Element creator : xml.children(titleStmt, role)) {
                    score.addCreator(creatorType(role), xml.text(creator));
                }
            }
            for (Element respStmt : xml.children(titleStmt, "respStmt")) {
                for (Element person : xml.elements(respStmt)) {
                    String role = xml.attr(person, "role");
                    if (role != null && !xml.text(person).isEmpty()) {
                        score.addCreator(creatorType(role), xml.text(person));
                    }
                }
            }
        }
        Element pubStmt = xml.path(head, "fileDesc/pubStmt");
        if (pubStmt != null) {
            Element availability = xml.child(pubStmt, "availability");
            if (availability != null && !xml.text(availability).isEmpty()) {
                score.setRights(xml.text(availability));
            }
            Element date = xml.child(pubStmt, "date");
            String isodate = xml.attr(date, "isodate");
            if (isodate != null && isodate.length() >= DATE_LENGTH) {
                score.setEncodingDate(isodate.substring(0, DATE_LENGTH));
            }
        }
        List<Element> applications = xml.descendants(xml.child(head, "encodingDesc"), "application");
        if (!applications.isEmpty()) {
            String name = xml.childText(applications.get(0), "name");
            if (name != null) {
                score.setSource(name);
            }
        }
    }


    /**
     * Gets the MusicXML creator type of an MEI role.
     *
     * @param role the MEI role or element name
     * @return the creator type
     */
    private String creatorType(String role) {
        switch (role.toLowerCase()) {
            case "author":
            case "librettist":
            case "lyricist":
            case "poet":
                return "lyricist";
            case "arranger":
                return "arranger";
            default:
                return role.toLowerCase();
        }
    }


    /**
     * Gets a text of the page head of the first score definition (rend type title, subtitle or composer).
     *
     * @param music the music element
     * @param type the rend type
     * @return the text, or null
     */
    private String pageHeadText(Element music, String type) {
        for (Element pageHead : xml.descendants(music, "pgHead")) {
            for (Element rend : xml.descendants(pageHead, "rend")) {
                if (type.equals(xml.attr(rend, "type")) && !xml.text(rend).isEmpty()) {
                    return xml.text(rend);
                }
            }
        }
        return null;
    }
}
