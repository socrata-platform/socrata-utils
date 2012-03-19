package com.socrata.util.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class FilteredIterator<T> implements Iterator<T> {
    private final Iterator<T> underlying;
    private T next;
    private boolean nextIsReady = false;

    public FilteredIterator(Iterator<T> underlying) {
        this.underlying = underlying;
    }

    public boolean hasNext() {
        if(nextIsReady) return true;
        while(underlying.hasNext()) {
            T potential = underlying.next();
            if(accept(potential)) {
                next = potential;
                nextIsReady = true;
                return true;
            }
        }
        return false;
    }

    public T next() {
        if(!hasNext()) throw new NoSuchElementException("No more elements in filtered iterator");
        T result = next;
        next = null;
        nextIsReady = false;
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("No remove on filtered iterators");
    }

    public abstract boolean accept(T item);
}
