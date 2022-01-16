package no.ntnu.ambulanceallocation.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TimeSeries<T> {

    private final List<LocalDateTime> keys = new ArrayList<>();
    private final List<T> values = new ArrayList<>();

    public void add(LocalDateTime key, T value) {
        keys.add(key);
        values.add(value);
    }

    public List<LocalDateTime> keys() {
        return keys;
    }

    public List<T> values() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder pairs = new StringBuilder();
        for (int i = 0; i < keys.size() - 1; i++) {
            pairs.append(String.format("(%s, %s), ", keys.get(i), values.get(i)));
        }
        pairs.append(String.format("(%s, %s)", keys.get(keys.size() - 1), values.get(keys.size() - 1)));
        return String.format("TimeSeries[%s]", pairs);
    }
}
