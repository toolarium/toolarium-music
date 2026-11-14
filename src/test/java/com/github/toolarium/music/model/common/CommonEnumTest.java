/*
 * CommonEnumTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Tests the common enums in the model/common package.
 * Verifies getValue() mappings, valueOf() round-trips,
 * and completeness for all music model enums.
 *
 * @author patrick
 */
public class CommonEnumTest {

    /**
     * Tests all NoteTypeValue constants and their XML values.
     */
    @Test
    void testNoteTypeValueConstants() {
        assertEquals(14, NoteTypeValue.values().length);

        assertEquals("maxima", NoteTypeValue.MAXIMA.getValue());
        assertEquals("long", NoteTypeValue.LONG.getValue());
        assertEquals("breve", NoteTypeValue.BREVE.getValue());
        assertEquals("whole", NoteTypeValue.WHOLE.getValue());
        assertEquals("half", NoteTypeValue.HALF.getValue());
        assertEquals("quarter", NoteTypeValue.QUARTER.getValue());
        assertEquals("eighth", NoteTypeValue.EIGHTH.getValue());
        assertEquals("16th", NoteTypeValue.SIXTEENTH.getValue());
        assertEquals("32nd", NoteTypeValue.THIRTY_SECOND.getValue());
        assertEquals("64th", NoteTypeValue.SIXTY_FOURTH.getValue());
        assertEquals("128th", NoteTypeValue.ONE_HUNDRED_TWENTY_EIGHTH.getValue());
        assertEquals("256th", NoteTypeValue.TWO_HUNDRED_FIFTY_SIXTH.getValue());
        assertEquals("512th", NoteTypeValue.FIVE_HUNDRED_TWELFTH.getValue());
        assertEquals("1024th", NoteTypeValue.ONE_THOUSAND_TWENTY_FOURTH.getValue());

        for (NoteTypeValue v : NoteTypeValue.values()) {
            assertEquals(v, NoteTypeValue.valueOf(v.name()));
            assertNotNull(v.getValue());
        }
    }


    /**
     * Tests all BarStyleValue constants and their XML values.
     */
    @Test
    void testBarStyleValueConstants() {
        assertEquals(11, BarStyleValue.values().length);

        assertEquals("regular", BarStyleValue.REGULAR.getValue());
        assertEquals("dotted", BarStyleValue.DOTTED.getValue());
        assertEquals("dashed", BarStyleValue.DASHED.getValue());
        assertEquals("heavy", BarStyleValue.HEAVY.getValue());
        assertEquals("light-light", BarStyleValue.LIGHT_LIGHT.getValue());
        assertEquals("light-heavy", BarStyleValue.LIGHT_HEAVY.getValue());
        assertEquals("heavy-light", BarStyleValue.HEAVY_LIGHT.getValue());
        assertEquals("heavy-heavy", BarStyleValue.HEAVY_HEAVY.getValue());
        assertEquals("tick", BarStyleValue.TICK.getValue());
        assertEquals("short", BarStyleValue.SHORT.getValue());
        assertEquals("none", BarStyleValue.NONE.getValue());

        for (BarStyleValue v : BarStyleValue.values()) {
            assertEquals(v, BarStyleValue.valueOf(v.name()));
            assertNotNull(v.getValue());
        }
    }


    /**
     * Tests all ClefSign constants and their XML values.
     */
    @Test
    void testClefSignConstants() {
        assertEquals(7, ClefSign.values().length);

        assertEquals("G", ClefSign.G.getValue());
        assertEquals("F", ClefSign.F.getValue());
        assertEquals("C", ClefSign.C.getValue());
        assertEquals("percussion", ClefSign.PERCUSSION.getValue());
        assertEquals("TAB", ClefSign.TAB.getValue());
        assertEquals("jianpu", ClefSign.JIANPU.getValue());
        assertEquals("none", ClefSign.NONE.getValue());

        for (ClefSign v : ClefSign.values()) {
            assertEquals(v, ClefSign.valueOf(v.name()));
            assertNotNull(v.getValue());
        }
    }


    /**
     * Tests StemValue constants and their XML values.
     */
    @Test
    void testStemValueConstants() {
        assertEquals(4, StemValue.values().length);

        assertEquals("up", StemValue.UP.getValue());
        assertEquals("down", StemValue.DOWN.getValue());
        assertEquals("double", StemValue.DOUBLE.getValue());
        assertEquals("none", StemValue.NONE.getValue());

        for (StemValue v : StemValue.values()) {
            assertEquals(v, StemValue.valueOf(v.name()));
        }
    }


    /**
     * Tests BeamValue constants and their XML values.
     */
    @Test
    void testBeamValueConstants() {
        assertEquals(5, BeamValue.values().length);

        assertEquals("begin", BeamValue.BEGIN.getValue());
        assertEquals("continue", BeamValue.CONTINUE.getValue());
        assertEquals("end", BeamValue.END.getValue());
        assertEquals("forward hook", BeamValue.FORWARD_HOOK.getValue());
        assertEquals("backward hook", BeamValue.BACKWARD_HOOK.getValue());

        for (BeamValue v : BeamValue.values()) {
            assertEquals(v, BeamValue.valueOf(v.name()));
        }
    }


    /**
     * Tests the two-constant enums: StartStop, AboveBelow, UpDown,
     * BackwardForward, PlacementValue, YesNo.
     */
    @Test
    void testTwoConstantEnums() {
        assertEquals(2, StartStop.values().length);
        assertEquals("start", StartStop.START.getValue());
        assertEquals("stop", StartStop.STOP.getValue());

        assertEquals(2, AboveBelow.values().length);
        assertEquals("above", AboveBelow.ABOVE.getValue());
        assertEquals("below", AboveBelow.BELOW.getValue());

        assertEquals(2, UpDown.values().length);
        assertEquals("up", UpDown.UP.getValue());
        assertEquals("down", UpDown.DOWN.getValue());

        assertEquals(2, BackwardForward.values().length);
        assertEquals("backward", BackwardForward.BACKWARD.getValue());
        assertEquals("forward", BackwardForward.FORWARD.getValue());

        assertEquals(2, PlacementValue.values().length);
        assertEquals("above", PlacementValue.ABOVE.getValue());
        assertEquals("below", PlacementValue.BELOW.getValue());

        assertEquals(2, YesNo.values().length);
        assertEquals("yes", YesNo.YES.getValue());
        assertEquals("no", YesNo.NO.getValue());
    }


    /**
     * Tests StartStopContinue constants and their XML values.
     */
    @Test
    void testStartStopContinueConstants() {
        assertEquals(3, StartStopContinue.values().length);

        assertEquals("start", StartStopContinue.START.getValue());
        assertEquals("stop", StartStopContinue.STOP.getValue());
        assertEquals("continue", StartStopContinue.CONTINUE.getValue());

        for (StartStopContinue v : StartStopContinue.values()) {
            assertEquals(v, StartStopContinue.valueOf(v.name()));
        }
    }


    /**
     * Tests WedgeType constants and their XML values.
     */
    @Test
    void testWedgeTypeConstants() {
        assertEquals(4, WedgeType.values().length);

        assertEquals("crescendo", WedgeType.CRESCENDO.getValue());
        assertEquals("diminuendo", WedgeType.DIMINUENDO.getValue());
        assertEquals("stop", WedgeType.STOP.getValue());
        assertEquals("continue", WedgeType.CONTINUE.getValue());

        for (WedgeType v : WedgeType.values()) {
            assertEquals(v, WedgeType.valueOf(v.name()));
        }
    }


    /**
     * Tests LineType constants and their XML values.
     */
    @Test
    void testLineTypeConstants() {
        assertEquals(4, LineType.values().length);

        assertEquals("solid", LineType.SOLID.getValue());
        assertEquals("dashed", LineType.DASHED.getValue());
        assertEquals("dotted", LineType.DOTTED.getValue());
        assertEquals("wavy", LineType.WAVY.getValue());

        for (LineType v : LineType.values()) {
            assertEquals(v, LineType.valueOf(v.name()));
        }
    }


    /**
     * Tests SyllabicValue constants and their XML values.
     */
    @Test
    void testSyllabicValueConstants() {
        assertEquals(4, SyllabicValue.values().length);

        assertEquals("single", SyllabicValue.SINGLE.getValue());
        assertEquals("begin", SyllabicValue.BEGIN.getValue());
        assertEquals("end", SyllabicValue.END.getValue());
        assertEquals("middle", SyllabicValue.MIDDLE.getValue());

        for (SyllabicValue v : SyllabicValue.values()) {
            assertEquals(v, SyllabicValue.valueOf(v.name()));
        }
    }


    /**
     * Tests GroupBarlineValue constants and their XML values.
     */
    @Test
    void testGroupBarlineValueConstants() {
        assertEquals(3, GroupBarlineValue.values().length);

        assertEquals("yes", GroupBarlineValue.YES.getValue());
        assertEquals("no", GroupBarlineValue.NO.getValue());
        assertEquals("Mensurstrich", GroupBarlineValue.MENSURSTRICH.getValue());

        for (GroupBarlineValue v : GroupBarlineValue.values()) {
            assertEquals(v, GroupBarlineValue.valueOf(v.name()));
        }
    }


    /**
     * Tests GroupSymbolValue constants and their XML values.
     */
    @Test
    void testGroupSymbolValueConstants() {
        assertEquals(5, GroupSymbolValue.values().length);

        assertEquals("none", GroupSymbolValue.NONE.getValue());
        assertEquals("brace", GroupSymbolValue.BRACE.getValue());
        assertEquals("line", GroupSymbolValue.LINE.getValue());
        assertEquals("bracket", GroupSymbolValue.BRACKET.getValue());
        assertEquals("square", GroupSymbolValue.SQUARE.getValue());

        for (GroupSymbolValue v : GroupSymbolValue.values()) {
            assertEquals(v, GroupSymbolValue.valueOf(v.name()));
        }
    }


    /**
     * Tests AccidentalValue has all expected constants with non-null values.
     */
    @Test
    void testAccidentalValueConstants() {
        assertEquals(13, AccidentalValue.values().length);

        assertEquals("sharp", AccidentalValue.SHARP.getValue());
        assertEquals("natural", AccidentalValue.NATURAL.getValue());
        assertEquals("flat", AccidentalValue.FLAT.getValue());
        assertEquals("double-sharp", AccidentalValue.DOUBLE_SHARP.getValue());

        for (AccidentalValue v : AccidentalValue.values()) {
            assertNotNull(v.getValue());
            assertEquals(v, AccidentalValue.valueOf(v.name()));
        }
    }


    /**
     * Tests ModeValue has all expected constants with non-null values.
     */
    @Test
    void testModeValueConstants() {
        assertEquals(10, ModeValue.values().length);

        assertEquals("major", ModeValue.MAJOR.getValue());
        assertEquals("minor", ModeValue.MINOR.getValue());
        assertEquals("dorian", ModeValue.DORIAN.getValue());

        for (ModeValue v : ModeValue.values()) {
            assertNotNull(v.getValue());
            assertEquals(v, ModeValue.valueOf(v.name()));
        }
    }


    /**
     * Tests NoteheadValue has all expected constants with non-null values.
     */
    @Test
    void testNoteheadValueConstants() {
        assertTrue(NoteheadValue.values().length >= 10);

        assertEquals("slash", NoteheadValue.SLASH.getValue());
        assertEquals("triangle", NoteheadValue.TRIANGLE.getValue());
        assertEquals("diamond", NoteheadValue.DIAMOND.getValue());
        assertEquals("normal", NoteheadValue.NORMAL.getValue());

        for (NoteheadValue v : NoteheadValue.values()) {
            assertNotNull(v.getValue());
            assertEquals(v, NoteheadValue.valueOf(v.name()));
        }
    }


    /**
     * Tests that every enum constant across all common enums has a non-null getValue() result.
     */
    @Test
    void testAllEnumValuesNonNull() {
        assertAllValuesNonNull(NoteTypeValue.values());
        assertAllValuesNonNull(BarStyleValue.values());
        assertAllValuesNonNull(ClefSign.values());
        assertAllValuesNonNull(StemValue.values());
        assertAllValuesNonNull(BeamValue.values());
        assertAllValuesNonNull(StartStop.values());
        assertAllValuesNonNull(StartStopContinue.values());
        assertAllValuesNonNull(AboveBelow.values());
        assertAllValuesNonNull(UpDown.values());
        assertAllValuesNonNull(BackwardForward.values());
        assertAllValuesNonNull(PlacementValue.values());
        assertAllValuesNonNull(YesNo.values());
        assertAllValuesNonNull(WedgeType.values());
        assertAllValuesNonNull(LineType.values());
        assertAllValuesNonNull(SyllabicValue.values());
        assertAllValuesNonNull(GroupBarlineValue.values());
        assertAllValuesNonNull(GroupSymbolValue.values());
        assertAllValuesNonNull(AccidentalValue.values());
        assertAllValuesNonNull(ModeValue.values());
        assertAllValuesNonNull(NoteheadValue.values());
    }


    /**
     * Helper that asserts every enum constant has a non-empty getValue() result.
     *
     * @param constants the array of enum constants to check
     * @param <E> the enum type
     * @throws AssertionError if any constant is missing getValue() or has a null or empty value
     */
    private <E extends Enum<E>> void assertAllValuesNonNull(E[] constants) {
        for (E constant : constants) {
            try {
                String val = (String) constant.getClass().getMethod("getValue").invoke(constant);
                assertNotNull(val, constant.name() + " should have a non-null value");
                assertTrue(val.length() > 0, constant.name() + " should have a non-empty value");
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Enum " + constant.getClass().getSimpleName()
                        + " is missing getValue(): " + e.getMessage());
            }
        }
    }
}
