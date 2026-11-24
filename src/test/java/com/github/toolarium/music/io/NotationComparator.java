/*
 * NotationComparator.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io;

import com.github.toolarium.music.io.format.musicxml.impl.MusicXMLIOFactory;
import com.github.toolarium.music.model.score.IScore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Compares the notation of two MusicXML documents beyond the sounding notes: part names, clef changes,
 * dynamics, tempo, articulations and slurs. Measures are matched by their position in the part, the
 * elements are compared per measure as multisets (their order and position within the measure are not
 * compared).
 *
 * @author patrick
 */
public final class NotationComparator {
    private static final String MEASURE = "measure";
    private static final String NOTE = "note";
    private static final int MAX_REPORTED_DIFFERENCES = 5;


    /**
     * The compared aspects.
     *
     * @author patrick
     */
    public enum Aspect {
        /** The part names. */
        PART_NAMES,

        /** The clef changes. */
        CLEFS,

        /** The dynamics. */
        DYNAMICS,

        /** The tempo marks. */
        TEMPO,

        /** The articulations. */
        ARTICULATIONS,
        
        /** The slurs. */
        SLURS
    }


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final NotationComparator INSTANCE = new NotationComparator();
    }


    /**
     * Constructor for NotationComparator.
     */
    private NotationComparator() {
        // NOP
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static NotationComparator getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Compares a score with a MusicXML or MXL reference.
     *
     * @param reference the reference file
     * @param score the score
     * @return the differences per aspect (an aspect without differences is not contained)
     * @throws Exception if reading fails
     */
    public Map<Aspect, String> compare(Path reference, IScore score) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MusicXMLIOFactory.getInstance().getWriter().write(score, out);
        Document actual = parse(new ByteArrayInputStream(out.toByteArray()));
        Document expected;
        try (InputStream in = open(reference)) {
            expected = parse(in);
        }
        return compare(expected, actual);
    }


    /**
     * Compares two MusicXML documents.
     *
     * @param expected the expected document
     * @param actual the actual document
     * @return the differences per aspect (an aspect without differences is not contained)
     */
    public Map<Aspect, String> compare(Document expected, Document actual) {
        Map<Aspect, String> result = new EnumMap<>(Aspect.class);
        List<String> expectedNames = partNames(expected);
        List<String> actualNames = partNames(actual);
        if (!expectedNames.equals(actualNames)) {
            result.put(Aspect.PART_NAMES, "expected " + expectedNames + ", converted " + actualNames);
        }

        List<Element> expectedParts = parts(expected);
        List<Element> actualParts = parts(actual);
        for (Aspect aspect : new Aspect[] {Aspect.CLEFS, Aspect.DYNAMICS, Aspect.TEMPO, Aspect.ARTICULATIONS, Aspect.SLURS}) {
            List<String> differences = new ArrayList<>();
            for (int p = 0; p < Math.min(expectedParts.size(), actualParts.size()); p++) {
                List<String> missing = elements(expectedParts.get(p), aspect);
                List<String> extra = elements(actualParts.get(p), aspect);
                List<String> common = new ArrayList<>(missing);
                for (String value : common) {
                    if (extra.remove(value)) {
                        missing.remove(value);
                    }
                }
                if (!missing.isEmpty() || !extra.isEmpty()) {
                    differences.add("part " + (p + 1) + ": " + missing.size() + " missing " + head(missing) + ", " + extra.size() + " not in the reference " + head(extra));
                }
            }
            if (!differences.isEmpty()) {
                result.put(aspect, String.join("; ", differences));
            }
        }
        return result;
    }


    /**
     * Gets the part names.
     *
     * @param document the document
     * @return the part names in part order
     */
    private List<String> partNames(Document document) {
        List<String> result = new ArrayList<>();
        NodeList scoreParts = document.getElementsByTagName("score-part");
        for (int i = 0; i < scoreParts.getLength(); i++) {
            Element name = first((Element) scoreParts.item(i), "part-name");
            String text = "";
            if (name != null) {
                text = name.getTextContent().replaceAll("\\s+", " ").trim();
            }
            result.add(text);
        }
        return result;
    }


    /**
     * Gets the elements of an aspect in a part, each as measure position and value.
     *
     * @param part the part
     * @param aspect the aspect
     * @return the elements
     */
    private List<String> elements(Element part, Aspect aspect) {
        List<String> result = new ArrayList<>();
        Map<String, String> currentClefs = new HashMap<>();
        int index = 0;
        for (Element measure : children(part, MEASURE)) {
            index++;
            for (Element child : children(measure, null)) {
                switch (aspect) {
                    case CLEFS:
                        addClefs(result, index, child, currentClefs);
                        break;
                    case DYNAMICS:
                        if ("direction".equals(child.getTagName()) || NOTE.equals(child.getTagName())) {
                            for (Element dynamics : descendants(child, "dynamics")) {
                                for (Element type : children(dynamics, null)) {
                                    result.add(index + ":" + type.getTagName());
                                }
                            }
                        }
                        break;
                    case TEMPO:
                        addTempo(result, index, child);
                        break;
                    case ARTICULATIONS:
                        if (NOTE.equals(child.getTagName())) {
                            for (Element articulations : descendants(child, "articulations")) {
                                for (Element type : children(articulations, null)) {
                                    result.add(index + ":" + type.getTagName());
                                }
                            }
                        }
                        break;
                    case SLURS:
                        if (NOTE.equals(child.getTagName())) {
                            for (Element slur : descendants(child, "slur")) {
                                if ("start".equals(slur.getAttribute("type"))) {
                                    result.add(index + ":slur");
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        Collections.sort(result);
        return result;
    }


    /**
     * Adds the clefs of an attributes element that change the clef of their staff.
     *
     * @param result the elements (filled)
     * @param index the measure position
     * @param child the measure content element
     * @param currentClefs the current clef by staff (updated)
     */
    private void addClefs(List<String> result, int index, Element child, Map<String, String> currentClefs) {
        if (!"attributes".equals(child.getTagName())) {
            return;
        }
        for (Element clef : children(child, "clef")) {
            String staff = clef.getAttribute("number");
            if (staff.isEmpty()) {
                staff = "1";
            }
            String value = text(clef, "sign") + text(clef, "line") + text(clef, "clef-octave-change");
            if (!value.equals(currentClefs.get(staff))) {
                currentClefs.put(staff, value);
                result.add(index + ":" + staff + ":" + value);
            }
        }
    }


    /**
     * Adds the tempo of a direction: the metronome mark (beat unit and beats per minute), or the
     * playback tempo of its sound element.
     *
     * @param result the elements (filled)
     * @param index the measure position
     * @param child the measure content element
     */
    private void addTempo(List<String> result, int index, Element child) {
        if (!"direction".equals(child.getTagName())) {
            return;
        }
        for (Element metronome : descendants(child, "metronome")) {
            String beatUnit = text(metronome, "beat-unit");
            if (first(metronome, "beat-unit-dot") != null) {
                beatUnit += ".";
            }
            result.add(index + ":" + beatUnit + "=" + text(metronome, "per-minute"));
        }
    }


    /**
     * Gets the parts of a partwise document.
     *
     * @param document the document
     * @return the parts
     */
    private List<Element> parts(Document document) {
        List<Element> result = new ArrayList<>();
        NodeList parts = document.getElementsByTagName("part");
        for (int i = 0; i < parts.getLength(); i++) {
            Element part = (Element) parts.item(i);
            if ("score-partwise".equals(((Element) part.getParentNode()).getTagName())) {
                result.add(part);
            }
        }
        return result;
    }


    /**
     * Gets the child elements, all or with a name.
     *
     * @param parent the parent
     * @param name the name, or null for all
     * @return the child elements
     */
    private List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && (name == null || name.equals(node.getNodeName()))) {
                result.add((Element) node);
            }
        }
        return result;
    }


    /**
     * Gets the descendant elements with a name.
     *
     * @param parent the parent
     * @param name the name
     * @return the elements
     */
    private List<Element> descendants(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getElementsByTagName(name);
        for (int i = 0; i < nodes.getLength(); i++) {
            result.add((Element) nodes.item(i));
        }
        return result;
    }


    /**
     * Gets the first child element with a name.
     *
     * @param parent the parent
     * @param name the name
     * @return the element, or null
     */
    private Element first(Element parent, String name) {
        List<Element> result = children(parent, name);
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }


    /**
     * Gets the text of a child element.
     *
     * @param parent the parent
     * @param name the child name
     * @return the trimmed text, or an empty string
     */
    private String text(Element parent, String name) {
        Element element = first(parent, name);
        if (element == null) {
            return "";
        }
        return element.getTextContent().trim();
    }


    /**
     * Gets the first values of a list.
     *
     * @param values the values
     * @return the first values as text
     */
    private String head(List<String> values) {
        return values.subList(0, Math.min(values.size(), MAX_REPORTED_DIFFERENCES)).toString();
    }


    /**
     * Opens a MusicXML file or the score of an MXL archive.
     *
     * @param file the file
     * @return the MusicXML content
     * @throws IOException if reading fails
     */
    private InputStream open(Path file) throws IOException {
        if (!file.toString().toLowerCase().endsWith(".mxl")) {
            return Files.newInputStream(file);
        }
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.startsWith("META-INF") && (name.endsWith(".xml") || name.endsWith(".musicxml"))) {
                    return new ByteArrayInputStream(zip.readAllBytes());
                }
            }
        }
        throw new IOException("No score found in " + file);
    }


    /**
     * Parses XML without loading the DTD.
     *
     * @param in the input
     * @return the document
     * @throws IOException if parsing fails
     */
    private Document parse(InputStream in) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(in);
        } catch (Exception e) {
            throw new IOException("Cannot parse MusicXML: " + e.getMessage(), e);
        }
    }


    /**
     * Counts the files per aspect (helper for reports).
     *
     * @param results the comparison results
     * @return the number of files with differences per aspect
     */
    public Map<Aspect, Integer> countByAspect(List<Map<Aspect, String>> results) {
        Map<Aspect, Integer> counts = new TreeMap<>();
        for (Map<Aspect, String> result : results) {
            for (Aspect aspect : result.keySet()) {
                counts.merge(aspect, 1, Integer::sum);
            }
        }
        return counts;
    }
}
