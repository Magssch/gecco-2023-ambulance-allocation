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

    // Only used for visualization
    private static LocalDateTime currentGlocalTime;
    private LocalDateTime travelStartTime;
    private Coordinate originatingLocation;

    private final BaseStation baseStation;

    private Coordinate destination = null;
    private int currentTimeToDestination = 0;
    private Coordinate currentLocation;
    private int currentLocationIndex = 0;
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
        currentLocationIndex = 0;
        currentTimeToDestination = currentLocation.timeTo(destination);
    }

    public void dispatch(Incident incident) {
        this.incident = incident;
        travelStartTime = currentGlocalTime;
        originatingLocation = currentLocation;
        currentLocationIndex = 0;
        destination = new Coordinate(incident.getLocation());
    }

    public void dispatchTransport(Incident incident, Coordinate hospitalLocation) {
        dispatch(incident);
        this.hospitalLocation = hospitalLocation;
    }

    // Only used for visualization
    public Coordinate getCurrentLocationInteractive(LocalDateTime currentTime) {
        if (isAvailable()) {
            return currentLocation;
        }
        if (travelStartTime == null) {
            return currentLocation;
        }
        int elapsedTime = (int) ChronoUnit.SECONDS.between(travelStartTime, currentTime);
        if (originatingLocation.timeTo(destination) - elapsedTime <= 0) {
            return destination;
        }
        if (originatingLocation.routeExists(destination) && originatingLocation.pathTo(destination) != null) {
            Coordinate[] path = originatingLocation.pathTo(destination);
            for (int i = 0; i < path.length; i++) {
                if (originatingLocation.timeTo(path[i]) <= elapsedTime) {
                    return path[i];
                }
            }
        } else if (destination.routeExists(originatingLocation) && destination.pathTo(originatingLocation) != null) {
            Coordinate[] path = destination.pathTo(originatingLocation);
            for (int i = path.length - 1; i >= 0; i--) {
                if (path[i].timeTo(originatingLocation) <= elapsedTime) {
                    return path[i];
                }
            }
        }
        return currentLocation;
    }

    public void updateLocation(int timePeriod) {
        currentTimeToDestination -= timePeriod * 60;
        if (currentTimeToDestination <= 0) {
            currentLocation = destination;
            currentTimeToDestination = 0;
        } else {
            Coordinate[] path = originatingLocation.pathTo(destination);
            if (path != null) {
                while (currentLocationIndex < path.length - 1
                        && path[currentLocationIndex + 1].timeTo(destination) > currentTimeToDestination) {
                    currentLocationIndex++;
                    if (!(currentLocationIndex <= path.length - 1)) {
                        System.out.println(currentLocationIndex);
                        System.out.println(path);
                    }
                }

                currentLocation = path[currentLocationIndex];
            }
        }
    }

    public boolean endOfJourney() {
        return currentLocation.equals(destination);
    }

    public void transport() {
        currentLocationIndex = 0;
        travelStartTime = currentGlocalTime;
        originatingLocation = currentLocation;
        destination = new Coordinate(hospitalLocation);
    }

    public void arriveAtHospital() {
        currentLocationIndex = 0;
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
