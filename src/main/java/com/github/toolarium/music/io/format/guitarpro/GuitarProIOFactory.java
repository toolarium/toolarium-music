/*
 * GuitarProIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.guitarpro;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.format.guitarpro.impl.GuitarProReaderImpl;


/**
 * Singleton factory for creating Guitar Pro (.gp, .gpx, .gp5, .gp4, .gp3) reader instances.
 *
 * @author patrick
 */
public final class GuitarProIOFactory {


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final GuitarProIOFactory INSTANCE = new GuitarProIOFactory();
    }


    /**
     * Constructor.
     */
    private GuitarProIOFactory() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static GuitarProIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new reader for Guitar Pro (.gp, .gpx, .gp5, .gp4, .gp3) files.
     *
     * @return a new reader
     */
    public IMusicReader getReader() {
        return new GuitarProReaderImpl();
    }
}
