package no.ntnu.ambulanceallocation.simulation.incident;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.utils.Utils;

public class IncidentIO {

    private static final Logger logger = LoggerFactory.getLogger(IncidentIO.class);

    public static final String incidentsFilePath = new File("src/main/resources/incidents.csv").getAbsolutePath();
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final List<Incident> incidents;

    static {
        incidents = loadIncidentsFromFile();
    }

    public static List<Incident> loadIncidentsFromFile() {

        List<Incident> incidents = new ArrayList<>();

        logger.info("Loading incidents from file...");

        int processedLines = 0;
        int skippedLines = 0;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(incidentsFilePath))) {
            String header = bufferedReader.readLine();

            logger.info("Incident CSV header: {}", header); // tidspunkt,xcoor,ycoor,hastegrad,tiltak_type,rykker_ut,ank_hentested,avg_hentested,ledig,total_vehicles_assigned,transporting_vehicles,cancelled_vehicles

            String line = bufferedReader.readLine();

            while (line != null) {
                List<String> values = Arrays.asList(line.split(","));

                LocalDateTime callReceived = LocalDateTime.parse(values.get(0), dateTimeFormatter);

                if (isValid(values)) {

                    int xCoordinate = Integer.parseInt(values.get(1));
                    int yCoordinate = Integer.parseInt(values.get(2));

                    String urgencyLevel = values.get(3);
                    // String dispatchType = values.get(4); // Always ambulance

                    LocalDateTime dispatched = LocalDateTime.parse(values.get(5), dateTimeFormatter);
                    Optional<LocalDateTime> arrivalAtScene = parseDateTime(values.get(6));
                    Optional<LocalDateTime> departureFromScene = parseDateTime(values.get(7));
                    LocalDateTime availableNonTransport = LocalDateTime.parse(values.get(8), dateTimeFormatter);
                    LocalDateTime availableTransport = LocalDateTime.parse(values.get(9), dateTimeFormatter);

                    int nonTransportingVehicles = Integer.parseInt(values.get(10));
                    int transportingVehicles = Integer.parseInt(values.get(11));

                    incidents.add(new Incident(
                            callReceived,
                            xCoordinate,
                            yCoordinate,
                            UrgencyLevel.get(urgencyLevel),
                            dispatched,
                            arrivalAtScene,
                            departureFromScene,
                            availableNonTransport,
                            availableTransport,
                            nonTransportingVehicles,
                            transportingVehicles));

                    processedLines++;
                } else {
                    // Skip line
                    skippedLines++;
                }

                line = bufferedReader.readLine();
            }

        } catch (IOException exception) {
            exception.printStackTrace();
            logger.error("An IOException occurred while loading incidents from file: {}", exception);
            System.exit(1);
        }

        logger.info("Loading incidents from file was successful.");

        double percentageSkipped = 100 * Utils.round((double) skippedLines / (double) processedLines, 6);
        logger.info("{} incidents were successfully processed", processedLines);
        logger.info("{} incidents were skipped ({}%)", skippedLines, percentageSkipped);

        return incidents;
    }

    private static boolean isValid(List<String> values) {
        return !values.get(5).isBlank() && !values.get(8).isBlank();
    }

    private static Optional<LocalDateTime> parseDateTime(String dateTime) {
        if (dateTime.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(LocalDateTime.parse(dateTime, dateTimeFormatter));
    }

}
