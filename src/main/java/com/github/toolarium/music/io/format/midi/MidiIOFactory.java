/*
 * MidiIOFactory.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.format.midi;

import com.github.toolarium.music.io.IMusicReader;
import com.github.toolarium.music.io.IMusicWriter;
import com.github.toolarium.music.io.format.midi.impl.MidiReaderImpl;
import com.github.toolarium.music.io.format.midi.impl.MidiWriterImpl;


/**
 * Singleton factory for creating MIDI reader and writer instances.
 *
 * @author patrick
 */
public final class MidiIOFactory {


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MidiIOFactory INSTANCE = new MidiIOFactory();
    }


    /**
     * Constructor.
     */
    private MidiIOFactory() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MidiIOFactory getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets a new MIDI reader instance.
     *
     * @return a new reader
     */
    public IMusicReader getReader() {
        return new MidiReaderImpl();
    }


    /**
     * Gets a new MIDI writer instance that writes all information (high quality).
     *
     * @return a new writer
     * @see MidiWriterOptions
     */
    public IMusicWriter getWriter() {
        return new MidiWriterImpl(MidiWriterOptions.highQuality());
    }


    /**
     * Gets a new MIDI writer instance.
     *
     * @param options the options: which information the MIDI file keeps, for example
     *        {@link MidiWriterOptions#highQuality()} or {@link MidiWriterOptions#obfuscated()}
     * @return a new writer
     */
    public IMusicWriter getWriter(MidiWriterOptions options) {
        return new MidiWriterImpl(options);
    }
}
