package com.socrata.util.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class TakeIterator<T> implements Iterator<T> {
    private int remaining;
    private final Iterator<T> underlying;

    public TakeIterator(int howMany, Iterator<T> underlying) {
        this.remaining = howMany;
        this.underlying = underlying;
    }

    public boolean hasNext() {
        return remaining > 0 && underlying.hasNext();
    }

    public T next() {
        if(remaining <= 0) throw new NoSuchElementException();
        T result = underlying.next();
        remaining -= 1;
        return result;
    }

    public void remove() {
        underlying.remove();
    }
}
