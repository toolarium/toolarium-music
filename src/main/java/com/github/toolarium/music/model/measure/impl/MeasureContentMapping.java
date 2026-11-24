/*
 * MeasureContentMapping.java
 *
 * Copyright by toolarium, all rights reserved.
 */
package com.github.toolarium.music.model.measure.impl;

import com.github.toolarium.music.model.barline.impl.BarlineImpl;
import com.github.toolarium.music.model.common.IMusicDataElement;
import com.github.toolarium.music.model.direction.impl.DirectionImpl;
import com.github.toolarium.music.model.direction.impl.SoundImpl;
import com.github.toolarium.music.model.harmony.impl.HarmonyImpl;
import com.github.toolarium.music.model.note.impl.NoteImpl;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Maps the MusicXML element names of the measure content to their model classes.
 *
 * @author patrick
 */
public final class MeasureContentMapping {
    private final Map<String, Class<? extends IMusicDataElement>> classes;


    /**
     * Private class, the only instance of the singelton which will be created by accessing the holder class.
     *
     * @author patrick
     */
    private static final class HOLDER {
        static final MeasureContentMapping INSTANCE = new MeasureContentMapping();
    }


    /**
     * Constructor for MeasureContentMapping.
     */
    private MeasureContentMapping() {
        Map<String, Class<? extends IMusicDataElement>> map = new LinkedHashMap<>();
        map.put("note", NoteImpl.class);
        map.put("backup", BackupImpl.class);
        map.put("forward", ForwardImpl.class);
        map.put("attributes", AttributesImpl.class);
        map.put("direction", DirectionImpl.class);
        map.put("harmony", HarmonyImpl.class);
        map.put("barline", BarlineImpl.class);
        map.put("print", PrintImpl.class);
        map.put("sound", SoundImpl.class);
        classes = Collections.unmodifiableMap(map);
    }


    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static MeasureContentMapping getInstance() {
        return HOLDER.INSTANCE;
    }


    /**
     * Gets the model class of a MusicXML measure content element.
     *
     * @param elementName the element name
     * @return the class, or null if the element is not supported
     */
    public Class<? extends IMusicDataElement> getElementClass(String elementName) {
        return classes.get(elementName);
    }


    /**
     * Gets the MusicXML element name of a measure content element.
     *
     * @param element the element
     * @return the element name, or null if the element type is not supported
     */
    public String getElementName(IMusicDataElement element) {
        if (element == null) {
            return null;
        }
        for (Map.Entry<String, Class<? extends IMusicDataElement>> entry : classes.entrySet()) {
            if (entry.getValue().isInstance(element)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
