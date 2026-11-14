/*
 * FinaleContainerFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.finale;

import com.github.toolarium.music.io.finale.impl.FinaleContainerReaderImpl;


/**
 * Singleton factory for creating Finale container readers.
 *
 * @author patrick
 */
public final class FinaleContainerFactory {

    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final FinaleContainerFactory INSTANCE = new FinaleContainerFactory();
    }


    /**
     * Constructor.
     */
    private FinaleContainerFactory() {
        // private
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static FinaleContainerFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new Finale container reader.
     *
     * @return a new reader instance
     */
    public IFinaleContainerReader getReader() {
        return new FinaleContainerReaderImpl();
    }
}
