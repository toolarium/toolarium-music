/*
 * DoricoNotationConverterTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.toolarium.music.model.direction.impl.DirectionTypeImpl;
import com.github.toolarium.music.model.notation.impl.ArticulationsImpl;
import com.github.toolarium.music.model.notation.impl.DynamicsImpl;
import com.github.toolarium.music.model.notation.impl.OrnamentsImpl;
import com.github.toolarium.music.model.note.impl.LyricImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;


/**
 * Tests for the {@link DoricoNotationConverter}.
 *
 * @author patrick
 */
public class DoricoNotationConverterTest {
    private static final String EMPTY = "";
    private static final String INTENSITY = "intensity";
    private static final String DIRECTION = "direction";
    private static final String PROPERTY_TABLE = "propertyTable";
    private static final String CONTENT = "content";
    private static final String DEFINITION = "definition";
    private static final String NOTE_EVENT = "NoteEventDefinition";
    private static final String LYRIC_ELEMENT = "LyricElementDefinition";
    private static final String TEXT_EVENT = "TextEventDefinition";
    private static final String FRAGMENT = "fragment";
    private static final String FRAGMENTS = "fragments";
    private static final String TEXT = "text";
    private static final String ONE = "1";
    private static final DoricoNotationConverter CONVERTER = DoricoNotationConverter.getInstance();


    /**
     * Tests the immediate, force and combined dynamics encodings.
     */
    @Test
    void testDynamics() {
        assertEquals("pp", CONVERTER.getImmediateDynamic(node(DEFINITION, leaf(INTENSITY, "steamVariantList:list: {int: 1}, {}, {bool: false}, {int: 2}"))));
        assertEquals("f", CONVERTER.getImmediateDynamic(node(DEFINITION, leaf(INTENSITY, "steamVariantList:list: {int: 0}, {}, {bool: false}, {int: 1}"))));
        assertEquals("fff", CONVERTER.getImmediateDynamic(node(DEFINITION, leaf(INTENSITY, "steamVariantList:list: {int: 0}, {}, {bool: false}, {int: 3}"))));
        assertEquals("mf", CONVERTER.getImmediateDynamic(node(DEFINITION, leaf(INTENSITY, "steamVariantList:list: {int: 0}, {}, {bool: true}, {}"))));
        assertEquals("mp", CONVERTER.getImmediateDynamic(node(DEFINITION, leaf(INTENSITY, "steamVariantList:list: {int: 1}, {}, {bool: true}, {}"))));
        assertNull(CONVERTER.getImmediateDynamic(node(DEFINITION, leaf(INTENSITY, "garbage"))));

        assertEquals("sfz", CONVERTER.getForceDynamic(node(DEFINITION, leaf("type", "kForceDynamicSfz"))));
        assertNull(CONVERTER.getForceDynamic(node(DEFINITION, leaf("type", "kOther"))));
        assertEquals("fp", CONVERTER.getCombinedDynamic(node(DEFINITION, leaf(DIRECTION, "kDown"), leaf("startIntensity", ONE), leaf("endIntensity", ONE))));
        assertEquals("pf", CONVERTER.getCombinedDynamic(node(DEFINITION, leaf(DIRECTION, "kUp"), leaf("startIntensity", ONE), leaf("endIntensity", ONE))));

        DynamicsImpl pp = CONVERTER.createDynamics("pp");
        assertEquals(EMPTY, pp.getPp());
        assertNull(pp.getPiano());
        assertEquals("sffz", CONVERTER.createDynamics("sffz").getOtherDynamics());
    }


    /**
     * Tests the articulation properties of a note.
     */
    @Test
    void testArticulations() {
        DoricoTreeNode note = node(NOTE_EVENT, properties(
                property("kArticulationDuration", "string: \"kStaccato\""),
                property("kArticulationForce", "string: \"kAccent\""),
                property("kNoteSpelling", "string: \"C\"")));
        ArticulationsImpl articulations = CONVERTER.createArticulations(note);
        assertNotNull(articulations);
        assertEquals(EMPTY, articulations.getStaccato());
        assertEquals(EMPTY, articulations.getAccent());
        assertNull(articulations.getTenuto());

        assertNull(CONVERTER.createArticulations(node(NOTE_EVENT, properties(property("kNoteSpelling", "string: \"C\"")))));
        assertEquals("kStaccato", CONVERTER.getProperty(note, "kArticulationDuration"));
        assertNull(CONVERTER.getProperty(note, "kMissing"));
    }


    /**
     * Tests the conversion of property values.
     */
    @Test
    void testPlainValue() {
        assertEquals("kStaccato", CONVERTER.plainValue("string: \"kStaccato\""));
        assertEquals("2", CONVERTER.plainValue("int: 2"));
        assertEquals(EMPTY, CONVERTER.plainValue("null: "));
        assertEquals("raw", CONVERTER.plainValue("raw"));
        assertNull(CONVERTER.plainValue(null));
    }


    /**
     * Tests the ornament entities.
     */
    @Test
    void testOrnaments() {
        assertEquals(EMPTY, CONVERTER.createOrnament("ornament.trill").getTrillMark());
        OrnamentsImpl shortTrill = CONVERTER.createOrnament("ornament.shortTrill");
        assertEquals(EMPTY, shortTrill.getInvertedMordent());
        assertNull(shortTrill.getTrillMark());
        assertEquals(EMPTY, CONVERTER.createOrnament("ornament.mordent").getMordent());
        assertEquals(EMPTY, CONVERTER.createOrnament("ornament.turn").getTurn());
        assertNull(CONVERTER.createOrnament("ornament.unknown"));
    }


    /**
     * Tests the conversion of lyric elements.
     */
    @Test
    void testLyrics() {
        LyricImpl lyric = CONVERTER.createLyric(node(LYRIC_ELEMENT, leaf(TEXT, "know"),
                leaf("syllableType", "kStart"), leaf("lineType", "kVerse"), leaf("lineNumber", ONE)));
        assertEquals("know", lyric.getText());
        assertEquals("begin", lyric.getSyllabic());
        assertEquals("2", lyric.getNumber());
        assertNull(lyric.getName());

        LyricImpl chorus = CONVERTER.createLyric(node(LYRIC_ELEMENT, leaf(TEXT, "Thou"),
                leaf("syllableType", "kWholeWord"), leaf("lineType", "kChorus"), leaf("lineNumber", "0")));
        assertEquals("single", chorus.getSyllabic());
        assertEquals("1", chorus.getNumber());
        assertEquals("chorus", chorus.getName());

        assertNull(CONVERTER.createLyric(node(LYRIC_ELEMENT, leaf(TEXT, EMPTY))));
    }


    /**
     * Tests the text of text events (fragments without SMuFL symbols) and repeat markers.
     */
    @Test
    void testTextAndRepeatMarkers() {
        DoricoTreeNode text = node(TEXT_EVENT, node(CONTENT, leaf(CONTENT, EMPTY),
                array(FRAGMENTS, node(FRAGMENT, leaf(CONTENT, "con ")), node(FRAGMENT, leaf(CONTENT, "sordina")))));
        assertEquals("con sordina", CONVERTER.getText(text));
        assertNull(CONVERTER.getText(node(TEXT_EVENT, node(CONTENT,
                array(FRAGMENTS, node(FRAGMENT, leaf(CONTENT, " ")))))));

        DirectionTypeImpl fine = CONVERTER.createRepeatMarker("kFine", null);
        assertEquals("Fine", fine.getWords().get(0).getValue());
        assertEquals("Menuetto da capo", CONVERTER.createRepeatMarker("kFine", "Menuetto da capo").getWords().get(0).getValue());
        assertEquals(EMPTY, CONVERTER.createRepeatMarker("kSegno", null).getSegno());
        assertNull(CONVERTER.createRepeatMarker("kUnknown", null));
    }


    /**
     * Creates a leaf node.
     *
     * @param name the name
     * @param value the value
     * @return the node
     */
    private DoricoTreeNode leaf(String name, String value) {
        return new DoricoTreeNode(name, value, false, Collections.emptyList());
    }


    /**
     * Creates an object node.
     *
     * @param name the name
     * @param children the children
     * @return the node
     */
    private DoricoTreeNode node(String name, DoricoTreeNode... children) {
        return new DoricoTreeNode(name, EMPTY, false, new ArrayList<>(Arrays.asList(children)));
    }


    /**
     * Creates an array node.
     *
     * @param name the name
     * @param children the items
     * @return the node
     */
    private DoricoTreeNode array(String name, DoricoTreeNode... children) {
        return new DoricoTreeNode(name, EMPTY, true, new ArrayList<>(Arrays.asList(children)));
    }


    /**
     * Creates a property table.
     *
     * @param entries the entries
     * @return the property table node
     */
    private DoricoTreeNode properties(DoricoTreeNode... entries) {
        return node(PROPERTY_TABLE, array("entries", entries));
    }


    /**
     * Creates a property table entry.
     *
     * @param type the property type
     * @param value the value
     * @return the entry node
     */
    private DoricoTreeNode property(String type, String value) {
        return node("entry", leaf("propertyType", type), leaf("value", value));
    }
}
