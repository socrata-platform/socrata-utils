package com.socrata.util.iterators;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class GroupedIterator<T> implements Iterator<List<T>> {
    private final Iterator<T> underlying;
    private final int groupSize;

    public GroupedIterator(Iterator<T> underlying, int groupSize) {
        if(groupSize <= 0) throw new IllegalArgumentException("groupSize <= 0");
        this.underlying = underlying;
        this.groupSize = groupSize;
    }

    public boolean hasNext() { return underlying.hasNext(); }

    public void remove() { throw new UnsupportedOperationException(); }

    public List<T> next() {
        ArrayList<T> chunk = new ArrayList<T>(groupSize);
        int i = 0;
        // do/while instead of while/do so that if the iterator is
        // empty we get a NoSuchElementException
        do {
            chunk.add(underlying.next());
        } while(++i != groupSize && underlying.hasNext());
        return chunk;
    }
}
