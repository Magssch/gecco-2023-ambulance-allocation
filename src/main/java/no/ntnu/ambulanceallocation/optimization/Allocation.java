package no.ntnu.ambulanceallocation.optimization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
}
