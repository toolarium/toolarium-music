/*
 * MeasureSerializer.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.measure.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.github.toolarium.music.model.common.IMusicDataElement;
import java.io.IOException;


/**
 * Writes a measure with its content in document order: the order of notes, backups, forwards,
 * attributes and directions defines the time position of each element in MusicXML.
 *
 * @author patrick
 */
public class MeasureSerializer extends StdSerializer<MeasureImpl> {
    private static final long serialVersionUID = 1L;


    /**
     * Constructor for MeasureSerializer.
     */
    public MeasureSerializer() {
        super(MeasureImpl.class);
    }


    /**
     * Writes the measure: the attributes number, width and implicit, then the content in document order.
     *
     * @param measure the measure
     * @param generator the generator
     * @param provider the serializer provider
     * @throws IOException if writing fails
     */
    @Override
    public void serialize(MeasureImpl measure, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        writeAttribute(generator, "number", measure.getNumber());
        writeAttribute(generator, "width", measure.getWidth());
        writeAttribute(generator, "implicit", measure.getImplicit());

        MeasureContentMapping mapping = MeasureContentMapping.getInstance();
        for (IMusicDataElement element : measure.getMusicData()) {
            String name = mapping.getElementName(element);
            if (name != null) {
                generator.writeFieldName(name);
                provider.defaultSerializeValue(element, generator);
            }
        }
        generator.writeEndObject();
    }


    /**
     * Writes an XML attribute if the value is set.
     *
     * @param generator the generator
     * @param name the attribute name
     * @param value the value, or null
     * @throws IOException if writing fails
     */
    private void writeAttribute(JsonGenerator generator, String name, String value) throws IOException {
        if (value == null) {
            return;
        }
        if (generator instanceof ToXmlGenerator) {
            ((ToXmlGenerator) generator).setNextIsAttribute(true);
        }
        generator.writeStringField(name, value);
        if (generator instanceof ToXmlGenerator) {
            ((ToXmlGenerator) generator).setNextIsAttribute(false);
        }
    }
}
