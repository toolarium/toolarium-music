/*
 * LilyPondIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.lilypond;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.io.format.lilypond.impl.LilyPondReaderImpl;
import com.github.toolarium.music.io.format.lilypond.impl.LilyPondWriterImpl;


/**
 * Factory for the LilyPond (.ly) reader and writer.
 *
 * @author patrick
 */
public final class LilyPondIOFactory {


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final LilyPondIOFactory INSTANCE = new LilyPondIOFactory();
    }


    /**
     * Constructor.
     */
    private LilyPondIOFactory() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static LilyPondIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a reader for LilyPond files.
     *
     * @return a new reader
     */
    public IMusicReader getReader() {
        return new LilyPondReaderImpl();
    }


    /**
     * Gets a new writer for LilyPond (.ly) files.
     *
     * @return a new writer
     */
    public IMusicWriter getWriter() {
        return new LilyPondWriterImpl();
    }
}
