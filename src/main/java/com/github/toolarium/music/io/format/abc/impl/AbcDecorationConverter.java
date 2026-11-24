/*
 * AbcDecorationConverter.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc.impl;

import com.github.toolarium.music.io.converter.ScoreBuilder;
import com.github.toolarium.music.io.dto.EventData;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import java.util.HashSet;
import java.util.Set;


/**
 * Applies ABC decorations ({@code !trill!}, {@code .}, {@code ~}, {@code !mf!} and so on) to an event.
 *
 * @author patrick
 */
final class AbcDecorationConverter {
    private static final String EMPTY = "";
    private static final Set<String> DYNAMICS = createDynamics();


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final AbcDecorationConverter INSTANCE = new AbcDecorationConverter();
    }


    /**
     * Constructor.
     */
    private AbcDecorationConverter() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    static AbcDecorationConverter getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the long name of a one-character decoration.
     *
     * @param symbol the symbol
     * @return the decoration name, or null if the character is not a decoration
     */
    String nameOf(char symbol) {
        switch (symbol) {
            case '.':
                return "staccato";
            case '~':
                return "roll";
            case 'H':
                return "fermata";
            case 'L':
                return "accent";
            case 'M':
                return "lowermordent";
            case 'O':
                return "coda";
            case 'P':
                return "uppermordent";
            case 'S':
                return "segno";
            case 'T':
                return "trill";
            case 'u':
                return "upbow";
            case 'v':
                return "downbow";
            default:
                return null;
        }
    }


    /**
     * Applies a decoration to an event.
     *
     * @param name the decoration name without delimiters
     * @param event the event
     * @param wedgeNumber the number of the current wedge
     * @return true if the decoration was recognized
     */
    boolean apply(String name, EventData event, int wedgeNumber) {
        ScoreBuilder builder = ScoreBuilder.getInstance();
        if (DYNAMICS.contains(name)) {
            event.getDirections().add(builder.createDynamics(name));
            return true;
        }
        if (applyArticulation(name, event) || applyOrnament(name, event) || applyTechnical(name, event)) {
            return true;
        }
        switch (name) {
            case "fermata":
                event.getFermatas().add(builder.createFermata(null, false));
                return true;
            case "invertedfermata":
                event.getFermatas().add(builder.createFermata(null, true));
                return true;
            case "segno":
                event.getDirections().add(builder.createSegno());
                return true;
            case "coda":
                event.getDirections().add(builder.createCoda());
                return true;
            case "D.C.":
            case "dacapo":
                event.getDirections().add(jump("D.C.", "dacapo"));
                return true;
            case "D.S.":
                event.getDirections().add(jump("D.S.", "dalsegno"));
                return true;
            case "fine":
                event.getDirections().add(jump("Fine", "fine"));
                return true;
            case "dacoda":
                event.getDirections().add(jump("To Coda", "tocoda"));
                return true;
            case "crescendo(":
            case "<(":
                event.getDirections().add(builder.createWedge("crescendo", wedgeNumber));
                return true;
            case "diminuendo(":
            case ">(":
                event.getDirections().add(builder.createWedge("diminuendo", wedgeNumber));
                return true;
            case "crescendo)":
            case "<)":
            case "diminuendo)":
            case ">)":
                event.getDirections().add(builder.createWedge(ScoreBuilder.STOP, wedgeNumber));
                return true;
            case "ped":
                event.getDirections().add(builder.createPedal(ScoreBuilder.START));
                return true;
            case "ped-up":
                event.getDirections().add(builder.createPedal(ScoreBuilder.STOP));
                return true;
            case "8va(":
                event.getDirections().add(builder.createOctaveShift("down", 8));
                return true;
            case "8vb(":
                event.getDirections().add(builder.createOctaveShift("up", 8));
                return true;
            case "8va)":
            case "8vb)":
                event.getDirections().add(builder.createOctaveShift(ScoreBuilder.STOP, 8));
                return true;
            default:
                return false;
        }
    }


    /**
     * Applies an articulation decoration.
     *
     * @param name the decoration name
     * @param event the event
     * @return true if the decoration is an articulation
     */
    private boolean applyArticulation(String name, EventData event) {
        switch (name) {
            case "staccato":
                event.articulations().setStaccato(EMPTY);
                return true;
            case "tenuto":
                event.articulations().setTenuto(EMPTY);
                return true;
            case "accent":
            case ">":
            case "emphasis":
                event.articulations().setAccent(EMPTY);
                return true;
            case "marcato":
            case "^":
                event.articulations().setStrongAccent(EMPTY);
                return true;
            case "wedge":
            case "staccatissimo":
                event.articulations().setStaccatissimo(EMPTY);
                return true;
            case "breath":
                event.articulations().setBreathMark(EMPTY);
                return true;
            case "caesura":
                event.articulations().setCaesura(EMPTY);
                return true;
            default:
                return false;
        }
    }


    /**
     * Applies an ornament decoration.
     *
     * @param name the decoration name
     * @param event the event
     * @return true if the decoration is an ornament
     */
    private boolean applyOrnament(String name, EventData event) {
        switch (name) {
            case "trill":
                event.ornaments().setTrillMark(EMPTY);
                return true;
            case "trill(":
                event.ornaments().setWavyLine(ScoreBuilder.START);
                return true;
            case "trill)":
                event.ornaments().setWavyLine(ScoreBuilder.STOP);
                return true;
            case "lowermordent":
            case "mordent":
                event.ornaments().setMordent(EMPTY);
                return true;
            case "uppermordent":
            case "pralltriller":
                event.ornaments().setInvertedMordent(EMPTY);
                return true;
            case "roll":
            case "turn":
                event.ornaments().setTurn(EMPTY);
                return true;
            case "invertedturn":
            case "turnx":
                event.ornaments().setInvertedTurn(EMPTY);
                return true;
            default:
                return false;
        }
    }


    /**
     * Applies a technical decoration (bowing, fingering and so on).
     *
     * @param name the decoration name
     * @param event the event
     * @return true if the decoration is technical
     */
    private boolean applyTechnical(String name, EventData event) {
        switch (name) {
            case "upbow":
                event.technical().setUpBow(EMPTY);
                return true;
            case "downbow":
                event.technical().setDownBow(EMPTY);
                return true;
            case "open":
                event.technical().setOpenString(EMPTY);
                return true;
            case "thumb":
                event.technical().setThumbPosition(EMPTY);
                return true;
            case "snap":
                event.technical().setSnapPizzicato(EMPTY);
                return true;
            case "plus":
            case "+":
                event.technical().setStopped(EMPTY);
                return true;
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
                event.technical().setFingering(name);
                return true;
            default:
                return false;
        }
    }


    /**
     * Creates a jump direction (D.C., D.S., Fine, To Coda) with its playback sound.
     *
     * @param text the text
     * @param sound the sound attribute (dacapo, dalsegno, fine or tocoda)
     * @return the direction
     */
    private DirectionImpl jump(String text, String sound) {
        DirectionImpl direction = ScoreBuilder.getInstance().createWords(text, ScoreBuilder.ABOVE, "italic", null);
        SoundImpl soundImpl = new SoundImpl();
        switch (sound) {
            case "dacapo":
                soundImpl.setDacapo("yes");
                break;
            case "dalsegno":
                soundImpl.setDalsegno("segno");
                break;
            case "fine":
                soundImpl.setFine("yes");
                break;
            default:
                soundImpl.setTocoda("coda");
                break;
        }
        direction.setSound(soundImpl);
        return direction;
    }


    /**
     * Creates the dynamics decoration names.
     *
     * @return the names
     */
    private static Set<String> createDynamics() {
        Set<String> result = new HashSet<>();
        for (String name : new String[] {"pppp", "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff", "ffff", "sfz", "sf", "sfp", "fp", "rfz", "rf"}) {
            result.add(name);
        }
        return result;
    }
}
