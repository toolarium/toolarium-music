/*
 * ICreditWords.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score;


/**
 * Represents the MusicXML credit-words element.
 *
 * @author patrick
 */
public interface ICreditWords {

    /**
     * Gets the value.
     *
     * @return the value
     */
    String getValue();


    /**
     * Gets the default-x attribute.
     *
     * @return the default-x attribute
     */
    String getDefaultX();


    /**
     * Gets the default-y attribute.
     *
     * @return the default-y attribute
     */
    String getDefaultY();


    /**
     * Gets the font-size attribute.
     *
     * @return the font-size attribute
     */
    String getFontSize();


    /**
     * Gets the justify attribute.
     *
     * @return the justify attribute
     */
    String getJustify();


    /**
     * Gets the valign attribute.
     *
     * @return the valign attribute
     */
    String getValign();
}
