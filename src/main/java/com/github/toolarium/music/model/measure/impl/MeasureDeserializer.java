/*
 * MeasureDeserializer.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.github.toolarium.music.model.common.IMusicDataElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Reads a measure with its content in document order: the order of notes, backups, forwards,
 * attributes and directions defines the time position of each element in MusicXML. Elements that
 * the model does not support (for example figured bass, grouping or link) are skipped.
 *
 * @author patrick
 */
public class MeasureDeserializer extends StdDeserializer<MeasureImpl> {
    private static final long serialVersionUID = 1L;


    /**
     * Constructor for MeasureDeserializer.
     */
    public MeasureDeserializer() {
        super(MeasureImpl.class);
    }


    /**
     * Reads the measure.
     *
     * @param parser the parser positioned at the start of the measure
     * @param context the deserialization context
     * @return the measure
     * @throws IOException if reading fails
     */
    @Override
    public MeasureImpl deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        MeasureImpl measure = new MeasureImpl();
        List<IMusicDataElement> content = new ArrayList<>();
        MeasureContentMapping mapping = MeasureContentMapping.getInstance();

        JsonToken token = parser.currentToken();
        if (token == JsonToken.START_OBJECT) {
            token = parser.nextToken();
        }
        while (token == JsonToken.FIELD_NAME) {
            String name = parser.currentName();
            token = parser.nextToken();
            if ("number".equals(name)) {
                measure.setNumber(parser.getValueAsString());
            } else if ("width".equals(name)) {
                measure.setWidth(parser.getValueAsString());
            } else if ("implicit".equals(name)) {
                measure.setImplicit(parser.getValueAsString());
            } else {
                Class<? extends IMusicDataElement> type = mapping.getElementClass(name);
                if (type == null) {
                    parser.skipChildren();
                } else {
                    content.add(readElement(parser, context, type));
                }
            }
            token = parser.nextToken();
        }

        measure.setMusicData(content);
        return measure;
    }


    /**
     * Reads one content element. An empty element (for example {@code <print/>}) becomes an
     * element without values.
     *
     * @param parser the parser positioned at the value of the element
     * @param context the deserialization context
     * @param type the model class
     * @return the element
     * @throws IOException if reading fails
     */
    private IMusicDataElement readElement(JsonParser parser, DeserializationContext context, Class<? extends IMusicDataElement> type) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.START_OBJECT) {
            IMusicDataElement element = context.readValue(parser, type);
            if (element != null) {
                return element;
            }
        } else {
            // an empty element or an element with only text: the model elements have no text content
            parser.skipChildren();
        }

        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IOException("Cannot create measure element " + type.getSimpleName() + ".", e);
        }
    }
}
