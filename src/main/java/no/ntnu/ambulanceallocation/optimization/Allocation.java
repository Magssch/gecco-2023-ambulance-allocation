package no.ntnu.ambulanceallocation.optimization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.ntnu.ambulanceallocation.simulation.BaseStation;

public record Allocation(List<List<Integer>> allocation) implements Iterable<List<Integer>> {

    public Allocation(List<List<Integer>> allocation) {
        List<List<Integer>> allocationCopy = new ArrayList<>();
        for (List<Integer> subAllocation : allocation) {
            allocationCopy.add(new ArrayList<>(subAllocation));
        }
        this.allocation = allocationCopy;
    }

    public Allocation() {
        this(List.of(new ArrayList<>(), new ArrayList<>()));
    }

    public Allocation(Allocation allocation) {
        this(allocation.allocation);
    }

    public List<Integer> getDayShiftAllocation() {
        return allocation.get(0);
    }

    public List<Integer> getNightShiftAllocation() {
        return allocation.get(1);
    }

    public List<Integer> getDayShiftAllocationSorted() {
        return getDayShiftAllocation().stream().sorted().collect(Collectors.toList());
    }

    public List<Integer> getNightShiftAllocationSorted() {
        return getNightShiftAllocation().stream().sorted().collect(Collectors.toList());
    }

    public int size() {
        return allocation.size();
    }

    public List<Integer> get(int index) {
        if (index > allocation.size()) {
            throw new IndexOutOfBoundsException(String.format("no allocation at index %d", index));
        }
        return allocation.get(index);
    }

    @Override
    public Iterator<List<Integer>> iterator() {
        return allocation.iterator();
    }

    public Stream<List<Integer>> stream() {
        return allocation.stream();
    }

    public Map<Integer, Long> getDayAmbulanceStationFrequency() {
        return getDayShiftAllocation().stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
    }

    public Map<Integer, Long> getNightAmbulanceStationFrequency() {
        return getNightShiftAllocation().stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
    }

    private Stream<Integer> getBaseStationAmbulanceProportionStream(
            Map<Integer, Long> ambulanceStationFrequency) {
        return BaseStation.ids().stream()
                .sorted(Comparator
                        .comparingDouble(baseStation -> (double) ambulanceStationFrequency.getOrDefault(baseStation, 0L)
                                / BaseStation.get((int) baseStation).getPopulation())
                        .reversed());
    }

    public Stream<Integer> getBaseStationDayAmbulanceProportionStream() {
        return getBaseStationAmbulanceProportionStream(getDayAmbulanceStationFrequency());
    }

    public Stream<Integer> getBaseStationNightAmbulanceProportionStream() {
        return getBaseStationAmbulanceProportionStream(getNightAmbulanceStationFrequency());
    }

    public List<Integer> getBaseStationDayAmbulanceProportionList() {
        return getBaseStationDayAmbulanceProportionStream().toList();
    }

    public List<Integer> getBaseStationNightAmbulanceProportionList() {
        return getBaseStationNightAmbulanceProportionStream().toList();
    }

    @Override
    public String toString() {
        return String.format("Day shift: %s\nNight shift: %s", getDayShiftAllocation(), getNightShiftAllocation());
    }

}
