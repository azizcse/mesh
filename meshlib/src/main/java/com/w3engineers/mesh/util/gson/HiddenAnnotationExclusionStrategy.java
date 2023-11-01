package com.w3engineers.mesh.util.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 *Excludes any field (or class) that is tagged with an "@Hidden"
 */
public class HiddenAnnotationExclusionStrategy implements ExclusionStrategy
{
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }

    public boolean shouldSkipField(FieldAttributes f) {
        return f.getAnnotation(Exclude.class) != null;
    }
}
