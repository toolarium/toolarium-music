/*
 * SibeliusIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.sibelius;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.format.sibelius.impl.SibeliusReaderImpl;


/**
 * Singleton factory for creating Sibelius score reader instances.
 *
 * @author patrick
 */
public final class SibeliusIOFactory {

    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final SibeliusIOFactory INSTANCE = new SibeliusIOFactory();
    }


    /**
     * Constructor.
     */
    private SibeliusIOFactory() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static SibeliusIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new Sibelius reader instance.
     *
     * @return a new reader
     */
    public IMusicReader getReader() {
        return new SibeliusReaderImpl();
    }
}
