package no.ntnu.ambulanceallocation.utils;

import java.util.List;

public record Tuple<T> (T first, T second) {

    public Tuple(List<T> list) {
        this(list.get(0), list.get(1));
    }

};