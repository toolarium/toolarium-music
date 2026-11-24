/*
 * FinaleReaderOptions.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale;

import java.util.Objects;


/**
 * The options of the Finale reader. By default the fonts of the page texts are taken over, gradual tempo
 * changes (rit., rall., accel.) are text only and the page and system breaks are left to the application.
 *
 * @author patrick
 */
public class FinaleReaderOptions {
    private boolean tempoSteps;
    private boolean fonts;
    private boolean layoutBreaks;


    /**
     * Constructor for FinaleReaderOptions with the default options.
     */
    public FinaleReaderOptions() {
        this.tempoSteps = false;
        this.fonts = true;
        this.layoutBreaks = false;
    }


    /**
     * Checks whether gradual tempo changes (rit., rall., accel.) are played back with a tempo change on every
     * quarter, which some applications show as tempo marks.
     *
     * @return true to play back gradual tempo changes, false for text only (default)
     */
    public boolean isTempoSteps() {
        return tempoSteps;
    }


    /**
     * Sets whether gradual tempo changes (rit., rall., accel.) are played back with a tempo change on every quarter.
     *
     * @param tempoSteps true to play back gradual tempo changes, false for text only
     * @return the options
     */
    public FinaleReaderOptions setTempoSteps(boolean tempoSteps) {
        this.tempoSteps = tempoSteps;
        return this;
    }


    /**
     * Checks whether the fonts of the page texts (font, size, bold and italic) are taken over.
     *
     * @return true to take over the fonts (default), false for the fonts of the application
     */
    public boolean isFonts() {
        return fonts;
    }


    /**
     * Sets whether the fonts of the page texts (font, size, bold and italic) are taken over.
     *
     * @param fonts true to take over the fonts, false for the fonts of the application
     * @return the options
     */
    public FinaleReaderOptions setFonts(boolean fonts) {
        this.fonts = fonts;
        return this;
    }


    /**
     * Checks whether the page and system breaks of the Finale layout are taken over.
     *
     * @return true to take over the breaks, false to leave the layout to the application (default)
     */
    public boolean isLayoutBreaks() {
        return layoutBreaks;
    }


    /**
     * Sets whether the page and system breaks of the Finale layout are taken over.
     *
     * @param layoutBreaks true to take over the breaks, false to leave the layout to the application
     * @return the options
     */
    public FinaleReaderOptions setLayoutBreaks(boolean layoutBreaks) {
        this.layoutBreaks = layoutBreaks;
        return this;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(tempoSteps, fonts, layoutBreaks);
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
        FinaleReaderOptions other = (FinaleReaderOptions) obj;
        return tempoSteps == other.tempoSteps && fonts == other.fonts && layoutBreaks == other.layoutBreaks;
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "FinaleReaderOptions [tempoSteps=" + tempoSteps + ", fonts=" + fonts + ", layoutBreaks=" + layoutBreaks + "]";
    }
}
