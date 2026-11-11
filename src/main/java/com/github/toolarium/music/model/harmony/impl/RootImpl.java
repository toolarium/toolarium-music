/*
 * RootImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.harmony.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.harmony.IRoot;
import java.util.Objects;

/**
 * Implements the {@link IRoot} interface.
 *
 * @author patrick
 */
public class RootImpl implements IRoot {

    @JacksonXmlProperty(localName = "root-step")
    private String rootStep;

    @JacksonXmlProperty(localName = "root-alter")
    private Double rootAlter;


    /**
     * Gets the root step.
     *
     * @return the root step
     */
    @Override
    public String getRootStep() {
        return rootStep;
    }


    /**
     * Sets the root step.
     *
     * @param rootStep the root step
     */
    public void setRootStep(String rootStep) {
        this.rootStep = rootStep;
    }


    /**
     * Gets the root alter.
     *
     * @return the root alter
     */
    @Override
    public Double getRootAlter() {
        return rootAlter;
    }


    /**
     * Sets the root alter.
     *
     * @param rootAlter the root alter
     */
    public void setRootAlter(Double rootAlter) {
        this.rootAlter = rootAlter;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(rootStep, rootAlter);
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
        RootImpl other = (RootImpl) obj;
        return Objects.equals(rootStep, other.rootStep)
                && Objects.equals(rootAlter, other.rootAlter);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "RootImpl [rootStep=" + rootStep + ", rootAlter=" + rootAlter + "]";
    }
}
