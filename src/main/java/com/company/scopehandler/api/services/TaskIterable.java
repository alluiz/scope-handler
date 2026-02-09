package com.company.scopehandler.api.services;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

public final class TaskIterable<T, R> implements Iterable<Callable<R>> {
    private final Iterable<T> source;
    private final Function<T, Callable<R>> mapper;

    public TaskIterable(Iterable<T> source, Function<T, Callable<R>> mapper) {
        this.source = Objects.requireNonNull(source, "source");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Iterator<Callable<R>> iterator() {
        return new TaskIterator<>(source.iterator(), mapper);
    }

    private static final class TaskIterator<T, R> implements Iterator<Callable<R>> {
        private final Iterator<T> iterator;
        private final Function<T, Callable<R>> mapper;

        private TaskIterator(Iterator<T> iterator, Function<T, Callable<R>> mapper) {
            this.iterator = iterator;
            this.mapper = mapper;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Callable<R> next() {
            return mapper.apply(iterator.next());
        }
    }
}
