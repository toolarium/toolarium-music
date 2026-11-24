/*
 * MeiIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.mei;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.io.format.mei.impl.MeiReaderImpl;
import com.github.toolarium.music.io.format.mei.impl.MeiWriterImpl;


/**
 * Singleton factory for creating MEI (.mei) reader and writer instances.
 *
 * @author patrick
 */
public final class MeiIOFactory {


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MeiIOFactory INSTANCE = new MeiIOFactory();
    }


    /**
     * Constructor.
     */
    private MeiIOFactory() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MeiIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new reader for MEI (.mei) files.
     *
     * @return a new reader
     */
    public IMusicReader getReader() {
        return new MeiReaderImpl();
    }


    /**
     * Gets a new writer for MEI (.mei) files.
     *
     * @return a new writer
     */
    public IMusicWriter getWriter() {
        return new MeiWriterImpl();
    }
}
