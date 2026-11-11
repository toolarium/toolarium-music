/*
 * MXLIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.mxl.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.IMusicWriter;


/**
 * Singleton factory for creating MXL reader and writer instances.
 *
 * @author patrick
 */
public final class MXLIOFactory {

    /**
     * Singleton holder.
     */
    private static final class HOLDER {
        static final MXLIOFactory INSTANCE = new MXLIOFactory();
    }

    /**
     * Private constructor.
     */
    private MXLIOFactory() {
        // private
    }


    /**
     * Gets the singleton instance.
     *
     * @return the factory instance
     */
    public static MXLIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new MXL reader for compressed MusicXML (.mxl) files.
     *
     * @return a new MXL reader instance
     */
    public IMusicReader getReader() {
        return new MXLReaderImpl();
    }


    /**
     * Gets a new MXL writer for compressed MusicXML (.mxl) files.
     *
     * @return a new MXL writer instance
     */
    public IMusicWriter getWriter() {
        return new MXLWriterImpl();
    }
}
