package no.ntnu.ambulanceallocation.optimization;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import no.ntnu.ambulanceallocation.optimization.initializer.Initializer;
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;

public abstract class Solution implements Comparable<Solution> {

    private Allocation allocation;
    private double fitness = 0.0;
    private boolean hasAllocationChanged = true;
    private Config config = Config.defaultConfig();

    public Solution(Initializer initializer, Config config) {
        this.config = config;
        setAllocation(List.of(initializer.initialize(config.NUMBER_OF_AMBULANCES_DAY()),
                initializer.initialize(config.NUMBER_OF_AMBULANCES_NIGHT())));
    }

    protected Solution(Initializer initializer) {
        setAllocation(List.of(initializer.initialize(config.NUMBER_OF_AMBULANCES_DAY()),
                initializer.initialize(config.NUMBER_OF_AMBULANCES_NIGHT())));
    }

    public Solution(List<List<Integer>> allocations) {
        setAllocation(allocations);
    }

    public Solution(Solution solution) {
        config = solution.config;
        allocation = new Allocation(solution.allocation);
        fitness = solution.fitness;
        hasAllocationChanged = solution.hasAllocationChanged;
    }

    public void copy(Solution solution) {
        config = solution.config;
        allocation = new Allocation(solution.allocation);
        fitness = solution.fitness;
        hasAllocationChanged = solution.hasAllocationChanged;
    }

    public double getFitness() {
        if (hasAllocationChanged) {
            calculateFitness();
            hasAllocationChanged = false;
        }
        return fitness;
    }

    private void calculateFitness() {
        ResponseTimes responseTimes = Simulation.withConfig(config).simulate(allocation);
        fitness = responseTimes.average();
    }

    public Allocation getAllocation() {
        return allocation;
    }

    public List<Integer> getDayShiftAllocation() {
        return allocation.getDayShiftAllocation();
    }

    public List<Integer> getNightShiftAllocation() {
        return allocation.getNightShiftAllocation();
    }

    protected void setFitness(double fitness) {
        this.fitness = fitness;
    }

    protected void setAllocation(int subAllocation, int variable, int variableValue) {
        Integer previousValue = this.allocation.get(subAllocation).set(variable, variableValue);
        hasAllocationChanged = !previousValue.equals(variableValue);
    }

    protected void setAllocation(List<List<Integer>> allocation) {
        Allocation newAllocation = new Allocation(allocation);
        if (this.allocation != null && this.allocation.equals(newAllocation)) {
            return;
        }
        this.allocation = newAllocation;
        hasAllocationChanged = true;
    }

    @Override
    public int compareTo(Solution otherSolution) {
        return Comparator.comparing(Solution::getFitness).compare(this, otherSolution);
    }

    @Override
    public String toString() {
        return String.format("Solution(f=%.1f, a_day=%s, a_night=%s)", getFitness(), getDayShiftAllocation(),
                getNightShiftAllocation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Solution solution))
            return false;
        return Stream.concat(getDayShiftAllocation().stream().sorted(), getNightShiftAllocation().stream().sorted())
                .equals(Stream.concat(solution.getDayShiftAllocation().stream().sorted(),
                        solution.getNightShiftAllocation().stream().sorted()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Stream.concat(getDayShiftAllocation().stream().sorted(), getNightShiftAllocation().stream().sorted()));
    }
}