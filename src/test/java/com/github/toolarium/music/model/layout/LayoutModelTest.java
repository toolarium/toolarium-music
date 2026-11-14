/*
 * LayoutModelTest.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.toolarium.music.model.layout.impl.PageLayoutImpl;
import com.github.toolarium.music.model.layout.impl.PageMarginsImpl;
import com.github.toolarium.music.model.layout.impl.StaffLayoutImpl;
import com.github.toolarium.music.model.layout.impl.SystemLayoutImpl;
import com.github.toolarium.music.model.layout.impl.SystemMarginsImpl;
import org.junit.jupiter.api.Test;


/**
 * Tests the equals, hashCode, and toString contracts for layout model classes.
 *
 * @author patrick
 */
public class LayoutModelTest {

    /**
     * Tests the equals/hashCode/toString contract for PageLayoutImpl with default values.
     */
    @Test
    void testPageLayoutImplDefaultContract() {
        PageLayoutImpl a = new PageLayoutImpl();
        PageLayoutImpl b = new PageLayoutImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());
    }


    /**
     * Tests PageLayoutImpl equality with populated fields.
     */
    @Test
    void testPageLayoutImplFieldEquality() {
        PageLayoutImpl a = new PageLayoutImpl();
        a.setPageHeight(1584.0);
        a.setPageWidth(1224.0);

        PageLayoutImpl b = new PageLayoutImpl();
        assertFalse(a.equals(b));

        b.setPageHeight(1584.0);
        b.setPageWidth(1224.0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        b.setPageWidth(800.0);
        assertFalse(a.equals(b));
    }


    /**
     * Tests PageLayoutImpl equality when the margins list contains entries.
     */
    @Test
    void testPageLayoutImplWithMargins() {
        PageMarginsImpl margin = new PageMarginsImpl();
        margin.setType("both");
        margin.setLeftMargin(70.0);
        margin.setRightMargin(70.0);
        margin.setTopMargin(88.0);
        margin.setBottomMargin(88.0);

        PageLayoutImpl a = new PageLayoutImpl();
        a.setPageHeight(1584.0);
        a.setPageWidth(1224.0);
        a.getPageMargins().add(margin);

        PageMarginsImpl marginCopy = new PageMarginsImpl();
        marginCopy.setType("both");
        marginCopy.setLeftMargin(70.0);
        marginCopy.setRightMargin(70.0);
        marginCopy.setTopMargin(88.0);
        marginCopy.setBottomMargin(88.0);

        PageLayoutImpl b = new PageLayoutImpl();
        b.setPageHeight(1584.0);
        b.setPageWidth(1224.0);
        b.getPageMargins().add(marginCopy);

        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }


    /**
     * Tests the equals/hashCode/toString contract for PageMarginsImpl with default values.
     */
    @Test
    void testPageMarginsImplDefaultContract() {
        PageMarginsImpl a = new PageMarginsImpl();
        PageMarginsImpl b = new PageMarginsImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());
    }


    /**
     * Tests PageMarginsImpl equality with populated fields.
     */
    @Test
    void testPageMarginsImplFieldEquality() {
        PageMarginsImpl a = new PageMarginsImpl();
        a.setType("both");
        a.setLeftMargin(70.0);
        a.setRightMargin(70.0);
        a.setTopMargin(88.0);
        a.setBottomMargin(88.0);

        PageMarginsImpl b = new PageMarginsImpl();
        assertFalse(a.equals(b));

        b.setType("both");
        b.setLeftMargin(70.0);
        b.setRightMargin(70.0);
        b.setTopMargin(88.0);
        b.setBottomMargin(88.0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        b.setTopMargin(100.0);
        assertFalse(a.equals(b));
    }


    /**
     * Tests the equals/hashCode/toString contract for SystemLayoutImpl with default values.
     */
    @Test
    void testSystemLayoutImplDefaultContract() {
        SystemLayoutImpl a = new SystemLayoutImpl();
        SystemLayoutImpl b = new SystemLayoutImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());
    }


    /**
     * Tests SystemLayoutImpl equality with populated fields.
     */
    @Test
    void testSystemLayoutImplFieldEquality() {
        SystemMarginsImpl margins = new SystemMarginsImpl();
        margins.setLeftMargin(20.0);
        margins.setRightMargin(20.0);

        SystemLayoutImpl a = new SystemLayoutImpl();
        a.setSystemMargins(margins);
        a.setSystemDistance(120.0);
        a.setTopSystemDistance(70.0);

        SystemLayoutImpl b = new SystemLayoutImpl();
        assertFalse(a.equals(b));

        SystemMarginsImpl marginsCopy = new SystemMarginsImpl();
        marginsCopy.setLeftMargin(20.0);
        marginsCopy.setRightMargin(20.0);

        b.setSystemMargins(marginsCopy);
        b.setSystemDistance(120.0);
        b.setTopSystemDistance(70.0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        b.setSystemDistance(200.0);
        assertFalse(a.equals(b));
    }


    /**
     * Tests the equals/hashCode/toString contract for SystemMarginsImpl with default values.
     */
    @Test
    void testSystemMarginsImplDefaultContract() {
        SystemMarginsImpl a = new SystemMarginsImpl();
        SystemMarginsImpl b = new SystemMarginsImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());
    }


    /**
     * Tests SystemMarginsImpl equality with populated fields.
     */
    @Test
    void testSystemMarginsImplFieldEquality() {
        SystemMarginsImpl a = new SystemMarginsImpl();
        a.setLeftMargin(20.0);
        a.setRightMargin(30.0);

        SystemMarginsImpl b = new SystemMarginsImpl();
        assertFalse(a.equals(b));

        b.setLeftMargin(20.0);
        b.setRightMargin(30.0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        b.setRightMargin(50.0);
        assertFalse(a.equals(b));
    }


    /**
     * Tests the equals/hashCode/toString contract for StaffLayoutImpl with default values.
     */
    @Test
    void testStaffLayoutImplDefaultContract() {
        StaffLayoutImpl a = new StaffLayoutImpl();
        StaffLayoutImpl b = new StaffLayoutImpl();

        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(null));
        assertNotNull(a.toString());
    }


    /**
     * Tests StaffLayoutImpl equality with populated fields.
     */
    @Test
    void testStaffLayoutImplFieldEquality() {
        StaffLayoutImpl a = new StaffLayoutImpl();
        a.setNumber(1);
        a.setStaffDistance(65.0);

        StaffLayoutImpl b = new StaffLayoutImpl();
        assertFalse(a.equals(b));

        b.setNumber(1);
        b.setStaffDistance(65.0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());

        b.setNumber(2);
        assertFalse(a.equals(b));
    }
}
