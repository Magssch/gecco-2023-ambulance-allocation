package no.ntnu.ambulanceallocation.simulation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import no.ntnu.ambulanceallocation.simulation.grid.Coordinate;
import no.ntnu.ambulanceallocation.utils.Utils;

public class ResponseTimes {

    private final List<LocalDateTime> timestamps = new ArrayList<>();
    private final List<Coordinate> coordinates = new ArrayList<>();
    private final List<Integer> responseTimes = new ArrayList<>();

    public void add(LocalDateTime timestamp, Coordinate coordinate, int responseTime) {
        timestamps.add(timestamp);
        coordinates.add(coordinate);
        responseTimes.add(responseTime);
    }

    public List<LocalDateTime> getTimestamps() {
        return timestamps;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public List<Integer> getResponseTimes() {
        return responseTimes;
    }

    @Override
    public String toString() {
        StringBuilder pairs = new StringBuilder();
        for (int i = 0; i < timestamps.size(); i++) {
            pairs.append(String.format("(%s, %s, %s), ", timestamps.get(i), coordinates.get(i), responseTimes.get(i)));
        }
        return String.format("TimeSeries[%s]", pairs);
    }

    public double average() {
        return Utils.average(getResponseTimes());
    }

    public double median() {
        return Utils.median(getResponseTimes());
    }

}
