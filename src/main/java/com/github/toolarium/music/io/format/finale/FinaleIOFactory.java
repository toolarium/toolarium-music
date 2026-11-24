/*
 * FinaleIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.finale;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.format.finale.impl.FinaleReaderImpl;


/**
 * Singleton factory for creating Finale score reader instances.
 *
 * @author patrick
 */
public final class FinaleIOFactory {

    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final FinaleIOFactory INSTANCE = new FinaleIOFactory();
    }


    /**
     * Constructor.
     */
    private FinaleIOFactory() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static FinaleIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new Finale reader instance with the default options.
     *
     * @return a new reader
     * @see FinaleReaderOptions
     */
    public IMusicReader getReader() {
        return new FinaleReaderImpl(new FinaleReaderOptions());
    }


    /**
     * Gets a new Finale reader instance.
     *
     * @param options the options: tempo steps of gradual tempo changes, fonts of the page texts, page and system breaks
     * @return a new reader
     */
    public IMusicReader getReader(FinaleReaderOptions options) {
        return new FinaleReaderImpl(options);
    }
}
