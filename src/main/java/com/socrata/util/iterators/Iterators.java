package com.socrata.util.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Iterators {
    public static final Iterator EMPTY_ITERATOR = new Iterator() {
        public boolean hasNext() { return false; }
        public Object next() { throw new NoSuchElementException(); }
        public void remove() { throw new UnsupportedOperationException(); }
    };

    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> empty() { return (Iterator<T>)EMPTY_ITERATOR; }

    public static <T> Iterator<T> singleton(final T obj) {
        return new Iterator<T>() {
            private boolean used = false;
            
            public boolean hasNext() { return !used; }

            public T next() {
                if(used) throw new NoSuchElementException();
                used = true;
                return obj;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
