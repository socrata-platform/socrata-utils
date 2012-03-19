package com.socrata.util.deepcast;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Performs a "deep cast" of an object.  In order to use this safely, it is necessary for the programmer to
 * be aware of the implications of type erasure.  For example, if we have a map which is actually a
 * <code>Map&lt;String, Double&gt;</code>, it is possible to use <code>DeepCast</code> to convert it to
 * a <code>Map&lt;String, Number&gt;</code>.  This is a valid thing to do, as long as you're only reading
 * from the object.  If you <code>put</code> an <code>Integer</code> into the cast map, the compiler will
 * not complain and no error will be thrown <em>until that <code>Integer</code> is accessed by way of
 * the original uncast map</em>.  So be careful.
 */
public abstract class DeepCast<T> {
    private final Class<?> erasure;

    protected DeepCast(Class<?> erasure) {
        this.erasure = erasure;
    }

    public T cast(Object o) {
        if(o == null) return null; // ick, but what're you going to do? -- we're only allowing top-level nulls though.
        return cast(o, this);
    }

    public <E extends Exception> T castThrowing(Object o, Class<E> exType) throws E {
        try {
            return cast(o, this);
        } catch(ClassCastException e) {
            E ex;
            try {
                Constructor<E> ctor = exType.getConstructor(String.class);
                ex = ctor.newInstance(e.getMessage());
            } catch (Exception e2) {
                throw e; // re-throw original exception...
            }
            ex.setStackTrace(e.getStackTrace());
            throw ex;
        }
    }

    public boolean canCast(Object o) {
        try {
            cast(o);
            return true;
        } catch(ClassCastException e) {
            return false;
        }
    }

    // wish I could say ErasureOf[T] cursor...
    protected abstract void checkSubParts(Object cursor, DeepCast<?> topLevel);

    @SuppressWarnings("unchecked")
    public T cast(Object o, DeepCast<?> topLevel) {
        if(o != null) { // null is evil -- EVIL -- but it can also be cast to anything.
            if(!erasure.isInstance(o)) throw new ClassCastException("Unable to cast to " + topLevel + "; got an unexpected " + o.getClass());
            checkSubParts(o, topLevel);
        }
        return (T) o;
    }

    public static <T> DeepCast<T> terminus(final Class<T> elementType) {
        return new DeepCast<T>(elementType) {
            protected void checkSubParts(Object cursorRaw, DeepCast<?> topLevel) {}
            public String toString() { return elementType.getSimpleName(); }
        };
    }

    public abstract String toString();

    public static final DeepCast<Object> object = terminus(Object.class);
    public static final DeepCast<String> string = terminus(String.class);
    public static final DeepCast<Number> number = terminus(Number.class);
    public static final DeepCast<Integer> integer = terminus(Integer.class);

    // compositites common enough to be usefully pre-defined
    public static final DeepCast<List<String>> listOfString = listOf(string);
    public static final DeepCast<List<Object>> listOfObject = listOf(object);
    public static final DeepCast<Map<String, String>> mapOfString = mapOf(string, string);
    public static final DeepCast<Map<String, Object>> mapOfObject = mapOf(string, object);
    public static final DeepCast<List<Map<String, Object>>> listOfMapOfObject = listOf(mapOfObject);
    public static final DeepCast<Set<Integer>> setOfInteger = setOf(integer);

    public static <T> DeepCast<List<T>> listOf(final DeepCast<T> elementType) {
        return new DeepCast<List<T>>(List.class) {
            protected void checkSubParts(Object cursorRaw, DeepCast<?> topLevel) {
                List<?> cursor = (List<?>) cursorRaw;
                for(Object x : cursor) {
                    elementType.cast(x, topLevel);
                }
            }
            public String toString() { return "List<" + elementType + ">"; }
        };
    }

    public static <K,V> DeepCast<Map<K, V>> mapOf(final DeepCast<K> keyType, final DeepCast<V> valueType) {
        return new DeepCast<Map<K, V>>(Map.class) {
            protected void checkSubParts(Object cursorRaw, DeepCast<?> topLevel) {
                Map<?, ?> cursor = (Map<?,?>) cursorRaw;
                for(Map.Entry<?,?> ent : cursor.entrySet()) {
                    keyType.cast(ent.getKey(), topLevel);
                    valueType.cast(ent.getValue(), topLevel);
                }
            }
            public String toString() { return "Map<" + keyType + ", " + valueType + ">"; }
        };
    }

    public static <T> DeepCast<Set<T>> setOf(final DeepCast<T> elementType) {
        return new DeepCast<Set<T>>(Set.class) {
            protected void checkSubParts(Object cursorRaw, DeepCast<?> topLevel) {
                Set<?> cursor = (Set<?>) cursorRaw;
                for(Object x : cursor) {
                    elementType.cast(x, topLevel);
                }
            }
            public String toString() { return "Set<" + elementType + ">"; }
        };
    }
}
