/*
 * StreamContentReader.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.io.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Reads the complete content of an input stream with a size limit.
 *
 * @author patrick
 */
public final class StreamContentReader {
    private static final int BUFFER_SIZE = 8192;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final StreamContentReader INSTANCE = new StreamContentReader();
    }


    /**
     * Constructor.
     */
    private StreamContentReader() {
        // singleton
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static StreamContentReader getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Reads the whole stream.
     *
     * @param inputStream the input stream
     * @param maxSize the maximum number of bytes
     * @param description the content description for error messages, for example MEI file
     * @return the content
     * @throws IOException if reading fails or the content is larger than the maximum
     */
    public byte[] readAll(InputStream inputStream, int maxSize, String description) throws IOException {
        if (inputStream == null) {
            throw new IOException("Input stream must not be null.");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer)) > 0) {
            out.write(buffer, 0, read);
            if (out.size() > maxSize) {
                throw new IOException(description + " too large.");
            }
        }
        return out.toByteArray();
    }
}
