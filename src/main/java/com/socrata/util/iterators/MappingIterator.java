package com.socrata.util.iterators;

import java.util.Iterator;

public abstract class MappingIterator<T, U> implements Iterator<U> {
    private final Iterator<T> underlying;
    
    public MappingIterator(Iterator<T> underlying) {
        this.underlying = underlying;
    }

    public boolean hasNext() { return underlying.hasNext(); }

    public U next() { return transform(underlying.next()); }

    public void remove() { underlying.remove(); }

    public abstract U transform(T input);
}
