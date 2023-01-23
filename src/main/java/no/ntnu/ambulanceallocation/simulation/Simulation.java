package no.ntnu.ambulanceallocation.simulation;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.DoubleProperty;
import no.ntnu.ambulanceallocation.experiments.Result;
import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.simulation.event.Event;
import no.ntnu.ambulanceallocation.simulation.event.JobCompletion;
import no.ntnu.ambulanceallocation.simulation.event.LocationUpdate;
import no.ntnu.ambulanceallocation.simulation.event.NewCall;
import no.ntnu.ambulanceallocation.simulation.event.PartiallyRespondedCall;
import no.ntnu.ambulanceallocation.simulation.event.SceneDeparture;
import no.ntnu.ambulanceallocation.simulation.grid.Coordinate;
import no.ntnu.ambulanceallocation.simulation.incident.Incident;
import no.ntnu.ambulanceallocation.simulation.incident.IncidentIO;
import no.ntnu.ambulanceallocation.utils.TriConsumer;
import no.ntnu.ambulanceallocation.utils.Utils;

public final class Simulation {

    private static final Map<Config, List<NewCall>> memoizedEventList = new HashMap<>();
    private static final List<Allocation> allocationResults = new ArrayList<>();
    private static final List<Double> responseTimeResults = new ArrayList<>();

    private final DoubleProperty simulationUpdateInterval;
    private final TriConsumer<LocalDateTime, Collection<Ambulance>, Collection<NewCall>> onTimeUpdate;
    private final Config config;
    private final boolean visualizationMode;
    private final List<Ambulance> ambulances = new ArrayList<>();
    private final Queue<NewCall> callQueue = new LinkedList<>();
    private final PriorityQueue<Event> eventQueue = new PriorityQueue<>();
    private final Map<ShiftType, Map<BaseStation, Integer>> baseStationShiftCount = new HashMap<>();
    private final Map<BaseStation, List<Ambulance>> baseStationAmbulances = new HashMap<>();
    private final Map<BaseStation, Integer> remainingOffDutyAmbulances = new HashMap<>();
    private ResponseTimes responseTimes;
    private LocalDateTime time;
    private ShiftType currentShift;

    static {
        Thread allocationsSaveHook = new Thread(() -> {
            System.out.println("Saving allocation results, do not interrupt...");
            saveAllocationResults();
        });
        Runtime.getRuntime().addShutdownHook(allocationsSaveHook);
    }

    private static void saveAllocationResults() {
        Map<Integer, String> baseStationMap = BaseStation.ids().stream()
                .collect(Collectors.toMap(Function.identity(), v -> "0"));

        Result simulationsResult = new Result();
        simulationsResult.saveColumn("Count of ambulances per base station for the day shift",
                allocationResults.stream().map(a -> {
                    Map<Integer, String> baseStationMapCopy = new HashMap<>(baseStationMap);
                    a.getDayShiftAllocation().stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                            .forEach(
                                    (k, v) -> baseStationMapCopy.put(k, v.toString()));
                    return baseStationMapCopy.values().stream().collect(Collectors.joining(" "));
                }).toList());
        simulationsResult.saveColumn("Count of ambulances per base station for the night shift",
                allocationResults.stream().map(a -> {
                    Map<Integer, String> baseStationMapCopy = new HashMap<>(baseStationMap);
                    a.getNightShiftAllocation().stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                            .forEach(
                                    (k, v) -> baseStationMapCopy.put(k, v.toString()));
                    return baseStationMapCopy.values().stream().collect(Collectors.joining(" "));
                }).toList());
        simulationsResult.saveColumn("Response time", responseTimeResults);
        simulationsResult
                .saveResults("allocation_response_times/%s".formatted(LocalDateTime.now()));

    }

    public Simulation(final Config config) {
        this.config = config;
        this.visualizationMode = false;
        this.simulationUpdateInterval = null;
        this.onTimeUpdate = null;
    }

    public Simulation(
            final Config config,
            final TriConsumer<LocalDateTime, Collection<Ambulance>, Collection<NewCall>> onTimeUpdate,
            final DoubleProperty simulationUpdateInterval) {
        this.config = config;
        this.visualizationMode = true;
        this.simulationUpdateInterval = simulationUpdateInterval;
        this.onTimeUpdate = onTimeUpdate;
    }

    public static Simulation withConfig(final Config config) {
        return new Simulation(config);
    }

    public static Simulation withDefaultConfig() {
        return new Simulation(Config.defaultConfig());
    }

    public static Simulation withinPeriod(final LocalDateTime start, final LocalDateTime end) {
        return new Simulation(Config.withinPeriod(start, end));
    }

    public static ResponseTimes visualizedSimulation(
            final Allocation allocation,
            final TriConsumer<LocalDateTime, Collection<Ambulance>, Collection<NewCall>> onTimeUpdate,
            final DoubleProperty simulationUpdateInterval) {
        return new Simulation(Config.defaultConfig(), onTimeUpdate, simulationUpdateInterval).simulate(allocation);
    }

    public static ResponseTimes simulate(
            final List<Integer> dayShiftAllocation,
            final List<Integer> nightShiftAllocation) {
        return withDefaultConfig().simulate(new Allocation(List.of(dayShiftAllocation, nightShiftAllocation)));
    }

    public ResponseTimes simulate(final Allocation allocation) {
        initialize(allocation);
        Event event = null;
        time = null;

        while (!eventQueue.isEmpty()) {
            event = eventQueue.poll();
            if (time != null && event.getTime().isBefore(time)) {
                throw new IllegalStateException("Event queue is not sorted");
            }

            time = event.getTime();
            setCurrentShift();

            switch (event) {
                case NewCall newCall -> {
                    List<Ambulance> dispatchedAmbulances = dispatch(newCall);
                    if (!dispatchedAmbulances.isEmpty()) {
                        if (newCall.incident.departureFromScene().isPresent()) {
                            long duration = newCall.incident.getDuration();
                            eventQueue.add(new SceneDeparture(time.plusSeconds(duration), newCall));
                            saveResponseTime(newCall, dispatchedAmbulances.get(0));
                        } else {
                            for (Ambulance ambulance : dispatchedAmbulances) {
                                long totalInterval = newCall.incident.getTotalIntervalNonTransport();
                                eventQueue.add(new JobCompletion(time.plusSeconds(totalInterval), ambulance));
                            }
                        }
                    }
                }
                case SceneDeparture sceneDeparture -> {
                    List<Ambulance> assignedAmbulances = Utils.filterList(ambulances,
                            (ambulance) -> ambulance.getIncident() == sceneDeparture.incident);

                    for (Ambulance ambulance : assignedAmbulances) {
                        if (ambulance.isTransport()) {
                            long transportTime = sceneDeparture.incident
                                    .getTimeFromDepartureToAvailableTransport();
                            ambulance.transport();
                            eventQueue.add(new JobCompletion(time.plusSeconds(transportTime), ambulance));
                        } else {
                            ambulance.flagAsAvailable();
                            int ambulancesToReturn = remainingOffDutyAmbulances
                                    .get(ambulance.getBaseStation());
                            if (ambulancesToReturn > 0) {
                                ambulance.finishShift();
                                remainingOffDutyAmbulances.put(ambulance.getBaseStation(),
                                        --ambulancesToReturn);
                            }
                            eventQueue.add(
                                    new LocationUpdate(time.plusSeconds(config.UPDATE_LOCATION_PERIOD()),
                                            ambulance));
                        }
                    }
                    checkQueue();
                }
                case JobCompletion jobCompletion -> {
                    if (jobCompletion.ambulance.isTransport()) {
                        jobCompletion.ambulance.arriveAtHospital();
                    }
                    jobCompletion.ambulance.flagAsAvailable();
                    int ambulancesToReturn = remainingOffDutyAmbulances.get(jobCompletion.ambulance.getBaseStation());
                    if (ambulancesToReturn > 0) {
                        jobCompletion.ambulance.finishShift();
                        remainingOffDutyAmbulances.put(jobCompletion.ambulance.getBaseStation(),
                                --ambulancesToReturn);
                    }
                    eventQueue.add(new LocationUpdate(time.plusMinutes(config.UPDATE_LOCATION_PERIOD()),
                            jobCompletion.ambulance));
                    checkQueue();
                }
                case LocationUpdate locationUpdate -> {
                    locationUpdate.ambulance.updateLocation(config.UPDATE_LOCATION_PERIOD());
                    if (!locationUpdate.ambulance.endOfJourney()) {
                        eventQueue.add(new LocationUpdate(time.plusMinutes(config.UPDATE_LOCATION_PERIOD()),
                                locationUpdate.ambulance));
                    }
                }
            }

            if (visualizationMode) {
                visualizationCallback();
            }

        }
        allocationResults.add(allocation);
        responseTimeResults.add(responseTimes.average());
        return responseTimes;
    }

    private void createEventQueue() {
        if (memoizedEventList.containsKey(config)) {
            eventQueue.addAll(memoizedEventList.get(config));
        } else {
            List<NewCall> events = IncidentIO.incidents.stream()
                    .filter(incident -> incident.callReceived()
                            .isAfter(config.START_DATE_TIME().minusHours(config.BUFFER_SIZE()))
                            && incident.callReceived().isBefore(config.END_DATE_TIME()))
                    .map(incident -> new NewCall(incident, incident.callReceived().isAfter(config.START_DATE_TIME())))
                    .toList();
            eventQueue.addAll(events);
            memoizedEventList.put(config, events);
        }
    }

    private void initialize(final Allocation allocation) {
        responseTimes = new ResponseTimes();
        callQueue.clear();
        eventQueue.clear();
        createEventQueue();
        currentShift = ShiftType.get(config.START_DATE_TIME());
        baseStationShiftCount.clear();
        baseStationAmbulances.clear();
        remainingOffDutyAmbulances.clear();
        baseStationShiftCount.put(ShiftType.DAY, new HashMap<>());
        baseStationShiftCount.put(ShiftType.NIGHT, new HashMap<>());

        for (BaseStation baseStation : BaseStation.values()) {
            int dayShiftCount = Collections.frequency(allocation.getDayShiftAllocation(), baseStation.getId());
            int nightShiftCount = Collections.frequency(allocation.getNightShiftAllocation(), baseStation.getId());
            int maxBaseStationAmbulances = Math.max(dayShiftCount, nightShiftCount);
            baseStationAmbulances.put(baseStation, Stream.generate(() -> new Ambulance(baseStation))
                    .limit(maxBaseStationAmbulances).toList());
            baseStationShiftCount.get(ShiftType.DAY).put(baseStation, dayShiftCount);
            baseStationShiftCount.get(ShiftType.NIGHT).put(baseStation, nightShiftCount);
            ambulances.addAll(baseStationAmbulances.get(baseStation));
            remainingOffDutyAmbulances.put(baseStation, 0);
            baseStationAmbulances.get(baseStation).stream()
                    .limit(baseStationShiftCount.get(currentShift).get(baseStation)).forEach(Ambulance::startNewShift);
        }

    }

    private void setCurrentShift() {
        if (ShiftType.get(time) != currentShift) {
            currentShift = ShiftType.get(time);
            for (BaseStation baseStation : baseStationAmbulances.keySet()) {
                int ambulanceDifference = (baseStationShiftCount.get(currentShift.previous()).get(baseStation)
                        - baseStationShiftCount.get(currentShift).get(baseStation));
                if (ambulanceDifference > 0) {
                    List<Ambulance> availableAmbulances = baseStationAmbulances.get(baseStation)
                            .stream()
                            .filter(Ambulance::isAvailable)
                            .limit(ambulanceDifference)
                            .toList();
                    availableAmbulances.forEach(Ambulance::finishShift);
                    remainingOffDutyAmbulances.put(baseStation,
                            ambulanceDifference - availableAmbulances.size());
                } else if (ambulanceDifference < 0) {
                    baseStationAmbulances.get(baseStation).stream().filter(Ambulance::isOffDuty)
                            .limit(-ambulanceDifference).forEach(Ambulance::startNewShift);
                }
            }
        }
    }

    private List<Ambulance> dispatch(NewCall newCall) {
        List<Ambulance> availableAmbulances = Utils.filterList(ambulances,
                Ambulance::isAvailable);

        int supply = availableAmbulances.size();

        if (supply == 0) {
            callQueue.add(newCall);
            return Collections.emptyList();
        }

        int numberOfTransportAmbulances = newCall.getTransportingVehicleDemand();
        int numberOfNonTransportAmbulances = newCall.getNonTransportingVehicleDemand();
        Coordinate hospitalLocation = findNearestHospital(newCall.incident);

        // Sort based on proximity
        List<Ambulance> nearestAmbulances = new ArrayList<>(availableAmbulances);
        nearestAmbulances.sort(config.DISPATCH_POLICY().useOn(newCall.incident));

        // Transport ambulances first
        List<Ambulance> transportAmbulances = nearestAmbulances.subList(0,
                Math.min(supply, numberOfTransportAmbulances));
        transportAmbulances.forEach((ambulance) -> ambulance.dispatchTransport(newCall.incident, hospitalLocation));
        List<Ambulance> dispatchedAmbulances = new ArrayList<>(transportAmbulances);

        // Remove transport ambulances from the pool
        nearestAmbulances = nearestAmbulances.subList(Math.min(supply, numberOfTransportAmbulances), supply);

        // Non-transport ambulances second
        List<Ambulance> nonTransportAmbulances = nearestAmbulances.subList(0,
                Math.min(supply - transportAmbulances.size(), numberOfNonTransportAmbulances));
        nonTransportAmbulances.forEach((ambulance) -> ambulance.dispatch(newCall.incident));
        dispatchedAmbulances.addAll(nonTransportAmbulances);

        if (transportAmbulances.size() < newCall.getTransportingVehicleDemand()
                || nonTransportAmbulances.size() < newCall.getNonTransportingVehicleDemand()) {
            PartiallyRespondedCall partiallyRespondedCall = new PartiallyRespondedCall(newCall);
            partiallyRespondedCall.respondWithTransportingVehicles(transportAmbulances.size());
            partiallyRespondedCall.respondWithNonTransportingVehicles(nonTransportAmbulances.size());
            callQueue.add(partiallyRespondedCall);
        }

        return dispatchedAmbulances;
    }

    private void checkQueue() {
        int availableAmbulances = (int) ambulances.stream().filter(Ambulance::isAvailable)
                .count();
        while (!callQueue.isEmpty() && availableAmbulances > 0) {
            NewCall newCall = callQueue.poll();
            if (newCall instanceof PartiallyRespondedCall call) {
                eventQueue.add(new PartiallyRespondedCall(call, time));
            } else {
                eventQueue.add(new NewCall(newCall, time));
            }
            availableAmbulances -= newCall.getNonTransportingVehicleDemand() + newCall.getTransportingVehicleDemand();
        }
    }

    private void saveResponseTime(NewCall newCall, Ambulance firstResponder) {
        if (newCall.providesResponseTime && newCall.incident.arrivalAtScene().isPresent()) {
            int simulatedDispatchTime = (int) ChronoUnit.SECONDS.between(newCall.incident.callReceived(),
                    newCall.getTime());
            int dispatchTime = Math.max(simulatedDispatchTime, newCall.incident.getDispatchDelay());

            int travelTime = firstResponder.timeTo(newCall.incident);

            int responseTime = dispatchTime + travelTime;
            if (responseTime < 0) {
                throw new IllegalStateException("Response time should never be negative");
            }
            responseTimes.add(newCall.incident.callReceived(), newCall.incident.getLocation(), responseTime);
        }
    }

    private void visualizationCallback() {
        Ambulance.setCurrentGlobalTime(time);
        onTimeUpdate.accept(time, ambulances, callQueue);
        try {
            Thread.sleep(simulationUpdateInterval.longValue());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Coordinate findNearestHospital(Incident incident) {
        List<Hospital> nearestHospitals = Arrays.asList(Hospital.values());
        nearestHospitals.sort(Hospital.closestTo(incident));
        return nearestHospitals.get(0).getCoordinate();
    }

}
