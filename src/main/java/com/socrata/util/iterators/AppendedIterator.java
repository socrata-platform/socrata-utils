package com.socrata.util.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.ArrayList;

public class AppendedIterator<T> implements Iterator<T> {
    private final Iterator<T>[] underlying;
    private int current;

    public AppendedIterator(Iterator<T>[] underlying) {
        this.underlying = underlying;
        this.current = 0;
    }

    public boolean hasNext() {
        while(current < underlying.length && !underlying[current].hasNext()) current += 1;
        return current < underlying.length;
    }

    public void remove() { throw new UnsupportedOperationException(); }

    public T next() {
        if(!hasNext()) throw new NoSuchElementException();
        return underlying[current].next();
    }
}
