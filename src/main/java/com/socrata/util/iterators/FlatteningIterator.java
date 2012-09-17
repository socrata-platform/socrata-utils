package com.socrata.util.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.ArrayList;

public class FlatteningIterator<T> implements Iterator<T> {
    private final Iterator<Iterator<T>> underlying;
    private Iterator<T> current;

    public FlatteningIterator(Iterator<Iterator<T>> underlying) {
        this.underlying = underlying;
        this.current = null;
    }

    public boolean hasNext() {
        if(current == null || !current.hasNext()) advance();
        return current != null;
    }

    private void advance() {
        current = null;
        while(current == null && underlying.hasNext()) {
            Iterator<T> nextChunk = underlying.next();
            if(nextChunk.hasNext()) current = nextChunk;
        }
    }

    public void remove() { throw new UnsupportedOperationException(); }

    public T next() {
        if(!hasNext()) throw new NoSuchElementException();
        return current.next();
    }
}
