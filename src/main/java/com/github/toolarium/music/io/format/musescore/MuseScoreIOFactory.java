/*
 * MuseScoreIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.musescore;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.io.format.musescore.impl.MscxReaderImpl;
import com.github.toolarium.music.io.format.musescore.impl.MscxWriterImpl;
import com.github.toolarium.music.io.format.musescore.impl.MsczReaderImpl;
import com.github.toolarium.music.io.format.musescore.impl.MsczWriterImpl;


/**
 * Singleton factory for creating MuseScore reader and writer instances.
 * Supports the compressed (.mscz) and the uncompressed (.mscx) MuseScore formats.
 *
 * @author patrick
 */
public final class MuseScoreIOFactory {


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MuseScoreIOFactory INSTANCE = new MuseScoreIOFactory();
    }


    /**
     * Constructor.
     */
    private MuseScoreIOFactory() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MuseScoreIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new reader for compressed MuseScore (.mscz) files.
     *
     * @return a new reader
     */
    public IMusicReader getReader() {
        return new MsczReaderImpl();
    }


    /**
     * Gets a new writer for compressed MuseScore (.mscz) files.
     *
     * @return a new writer
     */
    public IMusicWriter getWriter() {
        return new MsczWriterImpl();
    }


    /**
     * Gets a new reader for uncompressed MuseScore (.mscx) files.
     *
     * @return a new reader
     */
    public IMusicReader getMscxReader() {
        return new MscxReaderImpl();
    }


    /**
     * Gets a new writer for uncompressed MuseScore (.mscx) files.
     *
     * @return a new writer
     */
    public IMusicWriter getMscxWriter() {
        return new MscxWriterImpl();
    }
}
