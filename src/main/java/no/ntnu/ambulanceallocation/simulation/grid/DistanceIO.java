package no.ntnu.ambulanceallocation.simulation.grid;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import no.ntnu.ambulanceallocation.CSV;
import no.ntnu.ambulanceallocation.utils.Tuple;

public final class DistanceIO {

    public static final String distancesFilePath = new File("src/main/resources/od.json")
            .getAbsolutePath();
    public static final Set<Coordinate> uniqueGridCoordinates = new HashSet<>();
    public static final Map<Coordinate, List<Coordinate>> coordinateNeighbors = new HashMap<>();
    public static final Map<Tuple<Coordinate>, OneToManyRoutes> distances = new HashMap<>();
    public static final Map<String, Coordinate> coordinateStringCache = new HashMap<>();
    public static final Map<Long, Coordinate> coordinateLongCache = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(DistanceIO.class);
    public static final Map<Tuple<Double>, Coordinate> latLongToUtmMap = new HashMap<>();
    public static final Map<Long, Tuple<Double>> utmToLatLongMap = new HashMap<>();

    static {
        loadUTMToLatLongMap();
        loadDistancesFromFile();
        coordinateStringCache.clear();
    }

    public static OneToManyRoutes getRoute(Coordinate from, Coordinate to) {
        // TODO: What if origin and destination are the same?
        // if (from.equals(to)) {
        // return new OneToManyRoutes(from, to, 0, null);
        // }
        if (!distances.containsKey(new Tuple<>(from, to))) {
            logger.info("Failed to find distance from {} to {}",
                    from.id(), to.id());
            return null;
        }
        return distances.get(new Tuple<>(from, to));
    }

    public static void loadUTMToLatLongMap() {
        try {
            CSV.readCSVThenParse("utm_and_latlong.csv", values -> {
                Coordinate coordinate = new Coordinate(Double.valueOf(values[0]).intValue(),
                        Double.valueOf(values[1]).intValue());
                latLongToUtmMap.put(new Tuple<>(Double.valueOf(values[2]), Double.valueOf(values[3])), coordinate);
                utmToLatLongMap.put(coordinate.id(),
                        new Tuple<>(Double.valueOf(values[2]), Double.valueOf(values[3])));
            });
        } catch (IOException | NumberFormatException e) {
            logger.error("Failed to load utm and latlong map {}", e);
        }
    }

    private static Coordinate getCoordinateFromLong(Long gridId) {
        if (coordinateLongCache.containsKey(gridId)) {
            return coordinateLongCache.get(gridId);
        }
        Coordinate coordinate = new Coordinate(gridId);
        coordinateLongCache.put(gridId, coordinate);
        return coordinate;
    }

    private static Coordinate getCoordinateFromString(String coordinateString) {
        if (coordinateStringCache.containsKey(coordinateString)) {
            return coordinateStringCache.get(coordinateString);
        }
        Coordinate coordinate;
        try {
            long gridId = Long.parseLong(coordinateString);
            coordinate = getCoordinateFromLong(gridId);
        } catch (NumberFormatException e) {
            String[] utmCoordinates = coordinateString.split("_");
            int easting = Integer.parseInt(utmCoordinates[1]);
            int northing = Integer.parseInt(utmCoordinates[2]);
            coordinate = new Coordinate(easting, northing);
        }
        coordinateStringCache.put(coordinateString, coordinate);
        return coordinate;
    }

    private static void loadDistancesFromFile() {
        logger.info("Loading distances from file...");
        try (
                InputStream inputStream = Files.newInputStream(Path.of(distancesFilePath));
                JsonReader reader = new JsonReader(new InputStreamReader(inputStream));) {
            reader.beginObject();
            while (reader.hasNext()) {
                Coordinate origin = getCoordinateFromString(reader.nextName());
                uniqueGridCoordinates.add(origin);
                reader.beginObject();
                while (reader.hasNext()) {
                    Coordinate destination = getCoordinateFromString(reader.nextName());
                    JsonToken token = reader.peek();
                    reader.beginArray();
                    double duration = reader.nextDouble();
                    double distance = reader.nextDouble();
                    Coordinate[] arr = null;
                    if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                        List<Coordinate> route = new ArrayList<>();
                        reader.beginArray();
                        while (reader.hasNext() && reader.peek() == JsonToken.NUMBER) {
                            route.add(getCoordinateFromLong(reader.nextLong()));
                        }
                        arr = route.toArray(new Coordinate[route.size()]);
                        reader.endArray();
                    }
                    distances.put(new Tuple<>(origin, destination),
                            new OneToManyRoutes(origin, destination, duration, arr));
                    reader.endArray();

                }
                reader.endObject();
            }
            reader.endObject();
        } catch (IOException e) {
            logger.error("load {}", e);
        }
        logger.info(
                "Loaded {} distances.", distances.size());
    }

}
