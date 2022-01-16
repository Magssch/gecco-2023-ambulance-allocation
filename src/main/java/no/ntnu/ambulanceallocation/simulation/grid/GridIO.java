package no.ntnu.ambulanceallocation.simulation.grid;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class GridIO {

    private static final List<Coordinate> gridCoordinates = new ArrayList<>();

    static {
        List<String> coordinatesFilesList = List.of("oslo.csv", "akershus.csv");
        for (String coordinatesFile : coordinatesFilesList) {
            loadCoordinatesFile(GridIO.class.getClassLoader().getResource(coordinatesFile));
        }
    }

    private static void loadCoordinatesFile(URL coordinatesFilePath) {
        if (coordinatesFilePath == null) {
            throw new IllegalArgumentException("Coordinates file not found!");
        }

        try (Scanner scanner = new Scanner(new File(coordinatesFilePath.toURI()))) {
            scanner.nextLine(); // Skip header row
            String line = scanner.nextLine();

            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                List<String> values = Arrays.asList(line.split(","));

                Coordinate gridCoordinate = new Coordinate((int) Double.parseDouble(values.get(0)),
                        (int) Double.parseDouble(values.get(1)), Long.parseLong(values.get(4)));
                gridCoordinates.add(gridCoordinate);
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static List<Coordinate> getGridCoordinates() {
        return gridCoordinates;
    }

}
