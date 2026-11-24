/*
 * FinaleClefConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale.impl;

import com.github.toolarium.music.model.measure.impl.ClefImpl;


/**
 * Converts the clefs of Finale (clef IDs) into MusicXML clefs.
 *
 * @author patrick
 */
public final class FinaleClefConverter {
    private static final String SIGN_C = "C";
    private static final String SIGN_F = "F";
    private static final String SIGN_G = "G";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final FinaleClefConverter INSTANCE = new FinaleClefConverter();
    }


    /**
     * Constructor.
     */
    private FinaleClefConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static FinaleClefConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Converts a Finale clef ID to a MusicXML ClefImpl.
     *
     * @param finaleClefId the Finale clef identifier
     * @return the MusicXML clef
     */
    public ClefImpl getClef(int finaleClefId) {
        ClefImpl clef = new ClefImpl();
        if (finaleClefId == 1) {
            clef.setSign(SIGN_C);
            clef.setLine(3);
        } else if (finaleClefId == 2) {
            clef.setSign(SIGN_C);
            clef.setLine(4);
        } else if (finaleClefId == 3) {
            clef.setSign(SIGN_F);
            clef.setLine(4);
        } else if (finaleClefId == 4 || finaleClefId == 12) {
            clef.setSign("percussion");
        } else if (finaleClefId == 5) {
            clef.setSign(SIGN_G);
            clef.setLine(2);
            clef.setClefOctaveChange(-1);
        } else if (finaleClefId == 6) {
            clef.setSign(SIGN_F);
            clef.setLine(4);
            clef.setClefOctaveChange(-1);
        } else if (finaleClefId == 7) {
            clef.setSign(SIGN_F);
            clef.setLine(3);
        } else if (finaleClefId == 8) {
            clef.setSign(SIGN_G);
            clef.setLine(1);
        } else if (finaleClefId == 9) {
            clef.setSign(SIGN_C);
            clef.setLine(5);
        } else if (finaleClefId == 10) {
            clef.setSign(SIGN_C);
            clef.setLine(2);
        } else if (finaleClefId == 11) {
            clef.setSign(SIGN_C);
            clef.setLine(1);
        } else if (finaleClefId == 13) {
            clef.setSign(SIGN_G);
            clef.setLine(2);
            clef.setClefOctaveChange(1);
        } else if (finaleClefId == 14) {
            clef.setSign(SIGN_F);
            clef.setLine(4);
            clef.setClefOctaveChange(1);
        } else if (finaleClefId == 16 || finaleClefId == 17) {
            clef.setSign("TAB");
            clef.setLine(5);
        } else {
            clef.setSign(SIGN_G);
            clef.setLine(2);
        }
        return clef;
    }
}
