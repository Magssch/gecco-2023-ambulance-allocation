package no.ntnu.ambulanceallocation.optimization.sls;

import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.initializer.Initializer;
import no.ntnu.ambulanceallocation.optimization.initializer.Random;
import no.ntnu.ambulanceallocation.simulation.BaseStation;
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SlsSolution extends Solution {

    private static final Initializer initializer = new Random();
    private static final int MAX_VALUE = BaseStation.size();

    private final Logger logger = LoggerFactory.getLogger(SlsSolution.class);

    public SlsSolution() {
        super(initializer);
    }

    public SlsSolution(Config config) {
        super(initializer, config);
    }

    public SlsSolution(Solution solution) {
        super(solution);
    }

    private SlsSolution(SlsSolution root, int variableSet, int variable) {
        this(root);
        List<List<Integer>> allocation = new ArrayList<>(getAllocation().allocation());
        allocation.set(variableSet, forwardStep(getAllocation().get(variableSet), variable));
        setAllocation(allocation);
    }

    private SlsSolution(SlsSolution root, int variableSet, int variable, int variableValue) {
        this(root);
        setAllocation(variableSet, variable, variableValue);
    }

    public SlsSolution noiseStep() {
        int randomVariableSet = Utils.randomInt(getAllocation().size());
        int randomVariable = Utils.randomIndexOf(getAllocation().get(randomVariableSet));
        int randomVariableValue = Utils.randomInt(MAX_VALUE);

        int currentValue = getAllocation().get(randomVariableSet).get(randomVariable);
        while (randomVariableValue == currentValue) {
            randomVariableValue = Utils.randomInt(MAX_VALUE);
        }

        return new SlsSolution(this, randomVariableSet, randomVariable, randomVariableValue);
    }

    public SlsSolution greedyStep(NeighborhoodFunction neighborhoodFunction, int neighborhoodSize) {
        List<SlsSolution> neighborhood = switch (neighborhoodFunction) {
            case FORWARD -> getForwardNeighborhood();
            case HAMMING -> getHammingNeighborhood();
            case LAZY -> getLazyNeighborhood(neighborhoodSize);
        };
        logger.info("Neighbourhood size was: {}", neighborhood.size());
        neighborhood.parallelStream().forEach(Solution::getFitness);
        Collections.sort(neighborhood);
        return neighborhood.get(0);
    }

    public void restartStep() {
        copy(new SlsSolution());
    }

    private List<SlsSolution> getForwardNeighborhood() {
        List<SlsSolution> neighborhood = new ArrayList<>();
        for (int variableSet = 0; variableSet < getAllocation().size(); variableSet++) {
            for (int rootVariable = 0; rootVariable < getAllocation().get(variableSet).size(); rootVariable++) {
                neighborhood.add(new SlsSolution(this, variableSet, rootVariable));
            }
        }
        return neighborhood;
    }

    private List<SlsSolution> getHammingNeighborhood() {
        List<SlsSolution> neighborhood = new ArrayList<>();
        for (int variableSet = 0; variableSet < getAllocation().size(); variableSet++) {
            for (int variable = 0; variable < getAllocation().get(variableSet).size(); variable++) {
                for (int variableValue = 0; variableValue < MAX_VALUE; variableValue++) {

                    int currentValue = getAllocation().get(variableSet).get(variable);
                    if (variableValue != currentValue) {
                        neighborhood.add(new SlsSolution(this, variableSet, variable, variableValue));
                    }
                }
            }
        }
        return neighborhood;
    }

    private List<SlsSolution> getLazyNeighborhood(int neighborhoodSize) {
        return Stream.generate(this::noiseStep).limit(neighborhoodSize).collect(Collectors.toList());
    }

    private List<Integer> forwardStep(List<Integer> variableSet, int variable) {
        List<Integer> newVariableSet = new ArrayList<>(variableSet);
        Integer value = newVariableSet.get(variable);
        value = (value + 1) % MAX_VALUE;
        newVariableSet.set(variable, value);
        return newVariableSet;
    }

}
