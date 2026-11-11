/*
 * MusicXMLIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.musicxml.impl;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.IMusicWriter;


/**
 * Singleton factory for creating MusicXML reader and writer instances.
 *
 * @author patrick
 */
public final class MusicXMLIOFactory {

    /**
     * Singleton holder.
     */
    private static final class HOLDER {
        static final MusicXMLIOFactory INSTANCE = new MusicXMLIOFactory();
    }

    /**
     * Private constructor.
     */
    private MusicXMLIOFactory() {
        // private
    }


    /**
     * Gets the singleton instance.
     *
     * @return the factory instance
     */
    public static MusicXMLIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new MusicXML reader.
     *
     * @return a new reader instance
     */
    public IMusicReader getReader() {
        return new MusicXMLReaderImpl();
    }


    /**
     * Gets a new MusicXML writer.
     *
     * @return a new writer instance
     */
    public IMusicWriter getWriter() {
        return new MusicXMLWriterImpl();
    }
}
