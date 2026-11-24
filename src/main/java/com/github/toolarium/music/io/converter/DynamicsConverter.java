/*
 * DynamicsConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.converter;

import com.github.toolarium.music.model.notation.IDynamics;
import com.github.toolarium.music.model.notation.impl.DynamicsImpl;


/**
 * Converts between dynamic markings (pp, mf, sfz, ...) and the MusicXML dynamics model. Shared by
 * the format readers and writers.
 *
 * @author patrick
 */
public final class DynamicsConverter {
    private static final String EMPTY = "";
    private static final String PIANO = "p";
    private static final String FORTE = "f";


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final DynamicsConverter INSTANCE = new DynamicsConverter();
    }


    /**
     * Constructor.
     */
    private DynamicsConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static DynamicsConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Creates the MusicXML dynamics for a marking such as pp, mf or sfz. Markings without a
     * MusicXML element become other-dynamics.
     *
     * @param marking the marking
     * @return the dynamics
     */
    public DynamicsImpl createDynamics(String marking) {
        DynamicsImpl dynamics = new DynamicsImpl();
        switch (marking) {
            case PIANO:
                dynamics.setPiano(EMPTY);
                break;
            case "pp":
                dynamics.setPp(EMPTY);
                break;
            case "ppp":
                dynamics.setPpp(EMPTY);
                break;
            case "pppp":
                dynamics.setPppp(EMPTY);
                break;
            case FORTE:
                dynamics.setForte(EMPTY);
                break;
            case "ff":
                dynamics.setFf(EMPTY);
                break;
            case "fff":
                dynamics.setFff(EMPTY);
                break;
            case "ffff":
                dynamics.setFfff(EMPTY);
                break;
            case "mp":
                dynamics.setMp(EMPTY);
                break;
            case "mf":
                dynamics.setMf(EMPTY);
                break;
            case "sf":
                dynamics.setSf(EMPTY);
                break;
            case "sfp":
                dynamics.setSfp(EMPTY);
                break;
            case "sfz":
                dynamics.setSfz(EMPTY);
                break;
            case "fp":
                dynamics.setFp(EMPTY);
                break;
            case "rf":
                dynamics.setRf(EMPTY);
                break;
            case "rfz":
                dynamics.setRfz(EMPTY);
                break;
            default:
                dynamics.setOtherDynamics(marking);
                break;
        }
        return dynamics;
    }


    /**
     * Gets the marking of MusicXML dynamics (the first marking that is set).
     *
     * @param dynamics the dynamics (may be null)
     * @return the marking, for example mf, or null if no marking is set
     */
    public String getMarking(IDynamics dynamics) {
        if (dynamics == null) {
            return null;
        }
        String[][] markings = {
            {PIANO, dynamics.getPiano()}, {"pp", dynamics.getPp()}, {"ppp", dynamics.getPpp()}, {"pppp", dynamics.getPppp()},
            {FORTE, dynamics.getForte()}, {"ff", dynamics.getFf()}, {"fff", dynamics.getFff()}, {"ffff", dynamics.getFfff()},
            {"mp", dynamics.getMp()}, {"mf", dynamics.getMf()}, {"sf", dynamics.getSf()}, {"sfp", dynamics.getSfp()},
            {"sfz", dynamics.getSfz()}, {"fp", dynamics.getFp()}, {"rf", dynamics.getRf()}, {"rfz", dynamics.getRfz()}};
        for (String[] marking : markings) {
            if (marking[1] != null) {
                return marking[0];
            }
        }
        String other = dynamics.getOtherDynamics();
        if (other != null && !other.trim().isEmpty()) {
            return other.trim();
        }
        return null;
    }
}
