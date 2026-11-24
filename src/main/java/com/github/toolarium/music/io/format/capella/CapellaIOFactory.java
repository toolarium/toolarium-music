/*
 * CapellaIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.capella;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.format.capella.impl.CapellaReaderImpl;


/**
 * Singleton factory for creating capella (.capx, .cap) reader instances.
 *
 * @author patrick
 */
public final class CapellaIOFactory {


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final CapellaIOFactory INSTANCE = new CapellaIOFactory();
    }


    /**
     * Constructor.
     */
    private CapellaIOFactory() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static CapellaIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new reader for capella (.capx) files.
     *
     * @return a new reader
     */
    public IMusicReader getReader() {
        return new CapellaReaderImpl();
    }
}
