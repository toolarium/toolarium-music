/*
 * DoricoIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.dorico;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.format.dorico.impl.DoricoReaderImpl;


/**
 * Singleton factory for creating Dorico (.dorico) reader instances.
 *
 * @author patrick
 */
public final class DoricoIOFactory {


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final DoricoIOFactory INSTANCE = new DoricoIOFactory();
    }


    /**
     * Constructor.
     */
    private DoricoIOFactory() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static DoricoIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new reader for Dorico (.dorico) project files.
     *
     * @return a new reader
     */
    public IMusicReader getReader() {
        return new DoricoReaderImpl();
    }
}
