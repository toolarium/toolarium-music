/*
 * IGrace.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.note;


/**
 * Defines the Grace interface. A grace note has no duration of its own and is
 * played before (or after) the principal note it decorates.
 *
 * @author patrick
 */
public interface IGrace {

    /**
     * Gets the slash attribute. A value of "yes" marks an acciaccatura (slashed
     * grace note), any other value or null an appoggiatura.
     *
     * @return the slash value
     */
    String getSlash();


    /**
     * Gets the steal-time-previous attribute: the percentage of the duration
     * stolen from the previous note.
     *
     * @return the steal-time-previous value
     */
    String getStealTimePrevious();


    /**
     * Gets the steal-time-following attribute: the percentage of the duration
     * stolen from the following note.
     *
     * @return the steal-time-following value
     */
    String getStealTimeFollowing();


    /**
     * Gets the make-time attribute: the time in divisions the grace note takes
     * when it does not steal time from its neighbours.
     *
     * @return the make-time value
     */
    String getMakeTime();
}
