/*
 * HarmonyImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.harmony.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.harmony.IHarmony;
import java.util.List;
import java.util.Objects;

/**
 * Implements the {@link IHarmony} interface.
 *
 * @author patrick
 */
public class HarmonyImpl implements IHarmony, IMusicDataElement {

    @JacksonXmlProperty(isAttribute = true, localName = "type")
    private String type;

    @JacksonXmlProperty(isAttribute = true, localName = "print-frame")
    private String printFrame;

    @JacksonXmlProperty(localName = "root")
    private RootImpl root;

    @JacksonXmlProperty(localName = "function")
    private String function;

    @JacksonXmlProperty(localName = "kind")
    private String kind;

    @JacksonXmlProperty(localName = "inversion")
    private Integer inversion;

    @JacksonXmlProperty(localName = "bass")
    private BassImpl bass;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "degree")
    private List<DegreeImpl> degrees;

    @JacksonXmlProperty(localName = "frame")
    private FrameImpl frame;

    @JacksonXmlProperty(localName = "staff")
    private Integer staff;


    /**
     * Gets the type.
     *
     * @return the type
     */
    @Override
    public String getType() {
        return type;
    }


    /**
     * Sets the type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }


    /**
     * Gets the print frame.
     *
     * @return the print frame
     */
    @Override
    public String getPrintFrame() {
        return printFrame;
    }


    /**
     * Sets the print frame.
     *
     * @param printFrame the print frame
     */
    public void setPrintFrame(String printFrame) {
        this.printFrame = printFrame;
    }


    /**
     * Gets the root.
     *
     * @return the root
     */
    @Override
    public RootImpl getRoot() {
        return root;
    }


    /**
     * Sets the root.
     *
     * @param root the root
     */
    public void setRoot(RootImpl root) {
        this.root = root;
    }


    /**
     * Gets the function.
     *
     * @return the function
     */
    @Override
    public String getFunction() {
        return function;
    }


    /**
     * Sets the function.
     *
     * @param function the function
     */
    public void setFunction(String function) {
        this.function = function;
    }


    /**
     * Gets the kind.
     *
     * @return the kind
     */
    @Override
    public String getKind() {
        return kind;
    }


    /**
     * Sets the kind.
     *
     * @param kind the kind
     */
    public void setKind(String kind) {
        this.kind = kind;
    }


    /**
     * Gets the inversion.
     *
     * @return the inversion
     */
    @Override
    public Integer getInversion() {
        return inversion;
    }


    /**
     * Sets the inversion.
     *
     * @param inversion the inversion
     */
    public void setInversion(Integer inversion) {
        this.inversion = inversion;
    }


    /**
     * Gets the bass.
     *
     * @return the bass
     */
    @Override
    public BassImpl getBass() {
        return bass;
    }


    /**
     * Sets the bass.
     *
     * @param bass the bass
     */
    public void setBass(BassImpl bass) {
        this.bass = bass;
    }


    /**
     * Gets the degrees.
     *
     * @return the degrees
     */
    @Override
    public List<DegreeImpl> getDegrees() {
        return degrees;
    }


    /**
     * Sets the degrees.
     *
     * @param degrees the degrees
     */
    public void setDegrees(List<DegreeImpl> degrees) {
        this.degrees = degrees;
    }


    /**
     * Gets the frame.
     *
     * @return the frame
     */
    @Override
    public FrameImpl getFrame() {
        return frame;
    }


    /**
     * Sets the frame.
     *
     * @param frame the frame
     */
    public void setFrame(FrameImpl frame) {
        this.frame = frame;
    }


    /**
     * Gets the staff.
     *
     * @return the staff
     */
    @Override
    public Integer getStaff() {
        return staff;
    }


    /**
     * Sets the staff.
     *
     * @param staff the staff
     */
    public void setStaff(Integer staff) {
        this.staff = staff;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, printFrame, root, function, kind, inversion, bass, degrees, frame, staff);
    }


    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param obj the reference object to compare
     * @return true if this object is the same as the obj argument, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        HarmonyImpl other = (HarmonyImpl) obj;
        return Objects.equals(type, other.type)
                && Objects.equals(printFrame, other.printFrame)
                && Objects.equals(root, other.root)
                && Objects.equals(function, other.function)
                && Objects.equals(kind, other.kind)
                && Objects.equals(inversion, other.inversion)
                && Objects.equals(bass, other.bass)
                && Objects.equals(degrees, other.degrees)
                && Objects.equals(frame, other.frame)
                && Objects.equals(staff, other.staff);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "HarmonyImpl [type=" + type + ", printFrame=" + printFrame + ", root=" + root + ", function=" + function
                + ", kind=" + kind + ", inversion=" + inversion + ", bass=" + bass + ", degrees=" + degrees
                + ", frame=" + frame + ", staff=" + staff + "]";
    }
}
