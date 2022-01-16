package no.ntnu.ambulanceallocation.simulation;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.ntnu.ambulanceallocation.simulation.grid.Coordinate;
import no.ntnu.ambulanceallocation.simulation.incident.Incident;

public class Ambulance {

    private final static double CONVERSION_FACTOR = 3.6;

    // Only used for visualization
    private static LocalDateTime currentGlocalTime;
    private LocalDateTime travelStartTime;
    private Coordinate originatingLocation;

    private final BaseStation baseStation;

    private Coordinate destination = null;
    private int timeToDestination = 0;
    private Coordinate currentLocation;
    private Coordinate hospitalLocation = null;
    private boolean isOffDuty = true;

    private Incident incident;

    public Ambulance(BaseStation baseStation) {
        this.baseStation = baseStation;
        this.currentLocation = baseStation.getCoordinate();
    }

    public static List<Ambulance> generateFromAllocation(Collection<Integer> allocation) {
        return allocation
                .stream()
                .map(id -> new Ambulance(BaseStation.get(id)))
                .collect(Collectors.toList());
    }

    public static void setCurrentGlobalTime(LocalDateTime time) {
        currentGlocalTime = time;
    }

    public void startNewShift() {
        isOffDuty = false;
    }

    public void finishShift() {
        isOffDuty = true;
    }

    public boolean isOffDuty() {
        return isOffDuty;
    }

    public boolean isAtBaseStation() {
        return currentLocation.equals(baseStation.getCoordinate());
    }

    public BaseStation getBaseStation() {
        return baseStation;
    }

    public Coordinate getBaseStationLocation() {
        return baseStation.getCoordinate();
    }

    public Coordinate getHospitalLocation() {
        return hospitalLocation;
    }

    public Coordinate getDestination() {
        return destination;
    }

    public Coordinate getCurrentLocation() {
        return currentLocation;
    }

    public Incident getIncident() {
        return incident;
    }

    public boolean isAvailable() {
        return incident == null && !isOffDuty;
    }

    public boolean isTransport() {
        return hospitalLocation != null;
    }

    public boolean isNonTransport() {
        return !isTransport();
    }

    public void flagAsAvailable() {
        incident = null;
        hospitalLocation = null;
        destination = baseStation.getCoordinate();
        travelStartTime = currentGlocalTime;
        originatingLocation = currentLocation;
        timeToDestination = currentLocation.timeTo(destination);
    }

    public void dispatch(Incident incident) {
        this.incident = incident;
        travelStartTime = currentGlocalTime;
        originatingLocation = currentLocation;
        destination = new Coordinate(incident.getLocation());
    }

    public void dispatchTransport(Incident incident, Coordinate hospitalLocation) {
        dispatch(incident);
        this.hospitalLocation = hospitalLocation;
    }

    // Only used for visualization
    public Coordinate getCurrentLocationVisualized(LocalDateTime currentTime) {
        if (isAvailable()) {
            return currentLocation;
        }
        if (travelStartTime == null) {
            return currentLocation;
        }
        int elapsedTime = (int) ChronoUnit.SECONDS.between(travelStartTime, currentTime);
        int originTimeToDestination = originatingLocation.timeTo(destination);
        if (elapsedTime >= originTimeToDestination) {
            return destination;
        } else {
            int timeDelta = originTimeToDestination - elapsedTime;
            List<Coordinate> closeNeighbors = currentLocation.getNeighbors().stream()
                    .sorted(Comparator
                            .comparingInt(c -> Math.abs(c.getNearbyAverageTravelTimeTo(destination) - timeDelta)))
                    .toList();
            for (Coordinate closeNeighbor : closeNeighbors) {
                if (Math.abs(
                        closeNeighbor.getNearbyAverageTravelTimeTo(destination) - timeDelta) < Math
                                .abs(currentLocation.timeTo(destination) - timeDelta)) {
                    return closeNeighbor;
                }
            }
        }
        return currentLocation;
    }

    public void updateLocation(int timePeriod) {
        timeToDestination -= timePeriod * 60;
        if (timeToDestination <= 0) {
            currentLocation = destination;
            timeToDestination = 0;
        } else {
            int previousTimeToDestination = currentLocation.timeTo(destination);
            List<Coordinate> closeNeighbors = currentLocation.getNeighbors().stream()
                    .sorted(Comparator.comparingInt(c -> Math.abs(c.timeTo(destination) - timeToDestination)))
                    .toList();
            for (Coordinate closeNeighbor : closeNeighbors) {
                if (Math.abs(closeNeighbor.timeTo(destination) - timeToDestination) < Math
                        .abs(previousTimeToDestination - timeToDestination)) {
                    currentLocation = closeNeighbor;
                    return;
                }
            }
        }
    }

    public boolean endOfJourney() {
        return currentLocation.equals(destination);
    }

    public void transport() {
        travelStartTime = currentGlocalTime;
        originatingLocation = currentLocation;
        destination = new Coordinate(hospitalLocation);
    }

    public void arriveAtHospital() {
        currentLocation = new Coordinate(hospitalLocation);
    }

    public int timeTo(Incident incident) {
        return (int) Math.round(this.currentLocation.timeTo(incident.getLocation()));
    }

    public static Comparator<Ambulance> closestTo(Incident incident) {
        return Comparator.comparingDouble(ambulance -> ambulance.timeTo(incident));
    }

    @Override
    public String toString() {
        return String.format("Ambulance[baseStation=%s, destination=%s, currentLocation=%s, hospitalLocation=%s]",
                baseStation, destination, currentLocation, hospitalLocation);
    }
}
