package no.ntnu.ambulanceallocation.optimization;

import no.ntnu.ambulanceallocation.simulation.BaseStation;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private Map<Integer, Long> getAmbulanceStationFrequency(List<Integer> allocation) {
        return allocation.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
    }

    public Map<Integer, Long> getDayAmbulanceStationFrequency() {
        return getAmbulanceStationFrequency(getDayShiftAllocation());
    }

    public Map<Integer, Long> getNightAmbulanceStationFrequency() {
        return getAmbulanceStationFrequency(getNightShiftAllocation());
    }

    private Stream<Integer> getBaseStationAmbulanceProportionStream(Map<Integer, Long> ambulanceStationFrequency) {
        return BaseStation.ids().stream()
                .sorted(Comparator
                        .comparingDouble(baseStation -> (double) ambulanceStationFrequency.getOrDefault(baseStation, 0L)
                                / BaseStation.get((int) baseStation).getPopulation())
                        .reversed());
    }

    public Stream<Integer> getBaseStationAmbulanceProportionStream(List<Integer> allocation) {
        return getBaseStationAmbulanceProportionStream(getAmbulanceStationFrequency(allocation));
    }

    public Stream<Integer> getBaseStationDayAmbulanceProportionStream() {
        return getBaseStationAmbulanceProportionStream(getDayShiftAllocation());
    }

    public Stream<Integer> getBaseStationNightAmbulanceProportionStream() {
        return getBaseStationAmbulanceProportionStream(getNightShiftAllocation());
    }

    public List<Integer> getBaseStationAmbulanceProportionList(List<Integer> allocation) {
        return getBaseStationAmbulanceProportionStream(allocation).toList();
    }

    public List<Integer> getBaseStationDayAmbulanceProportionList() {
        return getBaseStationAmbulanceProportionList(getDayShiftAllocation());
    }

    public List<Integer> getBaseStationNightAmbulanceProportionList() {
        return getBaseStationAmbulanceProportionList(getNightShiftAllocation());
    }

    @Override
    public String toString() {
        return String.format("Day shift: %s\nNight shift: %s", getDayShiftAllocation(), getNightShiftAllocation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Allocation allocation))
            return false;
        return hashCode() == allocation.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Stream.concat(getDayShiftAllocation().stream().sorted(), getNightShiftAllocation().stream().sorted()));
    }

}
