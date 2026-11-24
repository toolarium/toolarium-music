/*
 * MeasureImplTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.measure.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Tests the ordered content of a measure: document order, typed views and the typed setters.
 *
 * @author patrick
 */
public class MeasureImplTest {

    /**
     * Tests that the content keeps the document order and the typed getters return the elements of one type.
     */
    @Test
    void testDocumentOrder() {
        NoteImpl first = note(1);
        BackupImpl backup = backup(4);
        NoteImpl second = note(2);
        DirectionImpl direction = new DirectionImpl();

        MeasureImpl measure = new MeasureImpl();
        measure.setMusicData(Arrays.asList(first, direction, backup, null, second));

        assertEquals(Arrays.asList(first, direction, backup, second), measure.getMusicData());
        assertEquals(Arrays.asList(first, second), measure.getNotes());
        assertEquals(Collections.singletonList(backup), measure.getBackups());
        assertEquals(Collections.singletonList(direction), measure.getDirections());
        assertTrue(measure.getForwards().isEmpty());
        assertTrue(measure.getAttributes().isEmpty());
    }


    /**
     * Tests that the content and the typed lists cannot be changed through the getters.
     */
    @Test
    void testReadOnlyViews() {
        MeasureImpl measure = new MeasureImpl();
        measure.addMusicData(note(1));
        assertThrows(UnsupportedOperationException.class, () -> measure.getNotes().add(note(2)));
        assertThrows(UnsupportedOperationException.class, () -> measure.getMusicData().clear());
    }


    /**
     * Tests that a typed setter replaces the elements of its type at the position of the first one.
     */
    @Test
    void testSetterReplacesInPlace() {
        NoteImpl first = note(1);
        BackupImpl backup = backup(4);
        NoteImpl second = note(2);
        MeasureImpl measure = new MeasureImpl();
        measure.setMusicData(Arrays.asList(first, backup, second));

        BackupImpl other = backup(2);
        measure.setBackups(Collections.singletonList(other));
        assertEquals(Arrays.asList(first, other, second), measure.getMusicData());

        measure.setBackups(null);
        assertEquals(Arrays.asList(first, second), measure.getMusicData());
    }


    /**
     * Tests the position of new elements set by a typed setter: attributes at the start after print
     * elements, all other elements at the end.
     */
    @Test
    void testSetterPosition() {
        NoteImpl note = note(1);
        PrintImpl print = new PrintImpl();
        MeasureImpl measure = new MeasureImpl();
        measure.setNotes(Collections.singletonList(note));
        measure.setPrints(Collections.singletonList(print));

        AttributesImpl attributes = new AttributesImpl();
        attributes.setDivisions(1);
        measure.setAttributes(Collections.singletonList(attributes));
        BarlineImpl barline = new BarlineImpl();
        measure.setBarlines(Collections.singletonList(barline));

        List<IMusicDataElement> expected = Arrays.asList(print, attributes, note, barline);
        assertEquals(expected, measure.getMusicData());
    }


    /**
     * Tests that equality depends on the order of the content.
     */
    @Test
    void testEqualityDependsOnOrder() {
        NoteImpl first = note(1);
        NoteImpl second = note(2);
        MeasureImpl measure = new MeasureImpl();
        measure.setMusicData(Arrays.asList(first, second));
        MeasureImpl same = new MeasureImpl();
        same.setMusicData(Arrays.asList(first, second));
        MeasureImpl reversed = new MeasureImpl();
        reversed.setMusicData(Arrays.asList(second, first));

        assertEquals(measure, same);
        assertEquals(measure.hashCode(), same.hashCode());
        assertNotEquals(measure, reversed);
        assertTrue(measure.toString().contains("musicData"));
    }


    /**
     * Creates a note with a duration.
     *
     * @param duration the duration
     * @return the note
     */
    private NoteImpl note(int duration) {
        NoteImpl note = new NoteImpl();
        note.setDuration(duration);
        return note;
    }


    /**
     * Creates a backup.
     *
     * @param duration the duration
     * @return the backup
     */
    private BackupImpl backup(int duration) {
        BackupImpl backup = new BackupImpl();
        backup.setDuration(duration);
        return backup;
    }
}
