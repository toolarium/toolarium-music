/*
 * AbcIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.abc;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.io.format.abc.impl.AbcReaderImpl;
import com.github.toolarium.music.io.format.abc.impl.AbcWriterImpl;


/**
 * Singleton factory for creating ABC notation (.abc) reader and writer instances.
 *
 * @author patrick
 */
public final class AbcIOFactory {


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final AbcIOFactory INSTANCE = new AbcIOFactory();
    }


    /**
     * Constructor.
     */
    private AbcIOFactory() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static AbcIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new reader for ABC notation (.abc) files.
     *
     * @return a new reader
     */
    public IMusicReader getReader() {
        return new AbcReaderImpl();
    }


    /**
     * Gets a new writer for ABC notation (.abc) files.
     *
     * @return a new writer
     */
    public IMusicWriter getWriter() {
        return new AbcWriterImpl();
    }
}
