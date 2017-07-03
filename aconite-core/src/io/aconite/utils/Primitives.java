package io.aconite.utils;

import java.util.HashMap;
import java.util.Map;

public class Primitives {
    private static final Map<Class, Class> WRAPPED_CLASSES;

    static {
        WRAPPED_CLASSES = new HashMap<>();
        WRAPPED_CLASSES.put(boolean.class, Boolean.class);
        WRAPPED_CLASSES.put(byte.class, Byte.class);
        WRAPPED_CLASSES.put(short.class, Short.class);
        WRAPPED_CLASSES.put(char.class, Character.class);
        WRAPPED_CLASSES.put(int.class, Integer.class);
        WRAPPED_CLASSES.put(long.class, Long.class);
        WRAPPED_CLASSES.put(float.class, Float.class);
        WRAPPED_CLASSES.put(double.class, Double.class);
    }

    @SuppressWarnings("unchecked")
    public static Class wrap(Class cls) {
        return cls.isPrimitive() ? WRAPPED_CLASSES.get(cls) : cls;
    }
}
