package no.ntnu.ambulanceallocation.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Memorandum<T, U> {

    private final Map<T, U> cache = new ConcurrentHashMap<>();

    private Memorandum() {
    }

    private Function<T, U> doMemoize(final Function<T, U> function) {
        return input -> cache.computeIfAbsent(input, function);
    }

    public static <T, U> Function<T, U> memoize(final Function<T, U> function) {
        return new Memorandum<T, U>().doMemoize(function);
    }

}