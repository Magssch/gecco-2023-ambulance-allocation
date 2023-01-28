package no.ntnu.ambulanceallocation.optimization.ga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.initializer.Initializer;
import no.ntnu.ambulanceallocation.optimization.ma.EvolutionStrategy;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.optimization.sls.SlsSolution;
import no.ntnu.ambulanceallocation.simulation.BaseStation;
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.utils.Tuple;
import no.ntnu.ambulanceallocation.utils.Utils;

public class Individual extends Solution {

    public Individual(List<List<Integer>> chromosomes) {
        super(chromosomes);
    }

    public Individual(Initializer initializer, Config config) {
        super(initializer, config);
    }

    public Individual(Solution solution) {
        super(solution);
    }

    public void mutate(double mutationProbability) {
        List<List<Integer>> dna = new ArrayList<>();
        for (List<Integer> chromosome : getAllocation()) {
            List<Integer> newChromosome = new ArrayList<>(chromosome);
            for (int locus = 0; locus < newChromosome.size(); locus++) {
                if (Utils.randomDouble() < mutationProbability) {
                    switch (Utils.randomInt(2)) {
                        case 0 -> swapMutation(locus, newChromosome);
                        case 1 -> bitFlipMutation(locus, newChromosome);
                    }
                }
            }
            dna.add(newChromosome);
        }
        setAllocation(dna);
    }

    public void swapMutation(int locus, List<Integer> newChromosome) {
        int baseStationId = newChromosome.get(locus);
        // Temporarily replace all numbers in newChromosome with value baseStationId
        // with -1
        Collections.replaceAll(newChromosome, baseStationId, -1);
        // Find one random index in newChromosome whose baseStationId is not the same as
        // the one we want to replace
        int randomLocus = Utils.randomInt(newChromosome.size());
        while (newChromosome.get(randomLocus) == baseStationId) {
            randomLocus = Utils.randomInt(newChromosome.size());
        }
        int otherBaseStationId = newChromosome.get(randomLocus);
        // Swap the allocation for the two base stations
        Collections.replaceAll(newChromosome, otherBaseStationId, baseStationId);
        Collections.replaceAll(newChromosome, -1, otherBaseStationId);
    }

    public void bitFlipMutation(int locus, List<Integer> newChromosome) {
        newChromosome.set(locus, Utils.randomInt(BaseStation.size()));
    }

    public Tuple<Individual> recombineWith(Individual individual, double crossoverProbability) {
        if (Utils.randomDouble() < crossoverProbability) {
            List<List<Integer>> childA = new ArrayList<>();
            List<List<Integer>> childB = new ArrayList<>();

            for (int chromosomeNumber = 0; chromosomeNumber < getAllocation().size(); chromosomeNumber++) {
                List<Integer> chromosomeFromA = new ArrayList<>(getAllocation().get(chromosomeNumber));
                List<Integer> chromosomeFromB = new ArrayList<>(individual.getAllocation().get(chromosomeNumber));

                int crossoverPoint = 1 + Utils.randomInt(chromosomeFromA.size() - 2);

                List<Integer> firstPartA = chromosomeFromA.subList(0, crossoverPoint);
                List<Integer> firstPartB = chromosomeFromB.subList(0, crossoverPoint);
                List<Integer> lastPartA = chromosomeFromA.subList(crossoverPoint, chromosomeFromA.size());
                List<Integer> lastPartB = chromosomeFromB.subList(crossoverPoint, chromosomeFromB.size());

                childA.add(Stream.concat(firstPartA.stream(), lastPartB.stream()).collect(Collectors.toList()));
                childB.add(Stream.concat(firstPartB.stream(), lastPartA.stream()).collect(Collectors.toList()));
            }
            return new Tuple<>(new Individual(childA), new Individual(childB));
        }
        return new Tuple<>(this, individual);
    }

    // Memetic method
    public void improve(EvolutionStrategy evolutionStrategy, NeighborhoodFunction neighborhoodFunction,
            double improveProbability) {
        if (Utils.randomDouble() < improveProbability) {
            // find best individual in population
            switch (evolutionStrategy) {
                case DARWINIAN -> {
                }
                case BALDWINIAN -> {
                    Individual bestNeighbor = improve();
                    if (bestNeighbor.getFitness() <= getFitness()) {
                        this.setFitness(bestNeighbor.getFitness());
                    }
                }
                case LAMARCKIAN -> {
                    Individual bestNeighbor = improve();
                    if (bestNeighbor.getFitness() <= getFitness()) {
                        copy(bestNeighbor);
                    }
                }
            }
        }
    }

    // TODO: How to decide bounds for randomInt() calls?
    private Individual improve() {
        int numBaseStations = BaseStation.size();
        return switch (Utils.randomInt(6)) {
            case 0 -> robinHoodNeighborhoodSearch(Utils.randomInt(5), Utils.randomInt(5), Utils.randomInt(5),
                    Utils.randomInt(5), false);
            case 1 -> robinHoodNeighborhoodSearch(Utils.randomInt(5), Utils.randomInt(5), Utils.randomInt(5),
                    Utils.randomInt(5), true);
            // Allow full exhaustive chromosome search for lower proportionate base stations
            // (expensive!):
            case 2 ->
                robinHoodNeighborhoodSearch(Utils.randomInt(5), -1,
                        Utils.randomInt(5), -1, false);
            // Allow full exhaustive chromosome search for higher proportionate base
            // stations (expensive!):
            case 3 ->
                robinHoodNeighborhoodSearch(-1, Utils.randomInt(5),
                        -1, Utils.randomInt(5), false);
            // Allow full chromosome search for lower proportionate base stations (greedy):
            case 4 ->
                robinHoodNeighborhoodSearch(Utils.randomInt(5), -1,
                        Utils.randomInt(5), -1, true);
            // Allow full chromosome search for higher proportionate base stations (greedy):
            case 5 ->
                robinHoodNeighborhoodSearch(-1, Utils.randomInt(5),
                        -1, Utils.randomInt(5), true);
            default -> throw new IllegalArgumentException("Unexpected value");
        };
    }

    // Memetic method
    private Individual getBestNeighborSLS(NeighborhoodFunction neighborhoodFunction) {
        SlsSolution slsSolution = new SlsSolution(this);
        SlsSolution bestNeighborhood = slsSolution.greedyStep(neighborhoodFunction);
        return new Individual(bestNeighborhood);
    }

    private Individual robinHoodNeighborhoodSearch(int dayHighestN, int dayLowestN, int nightHighestN, int nightLowestN,
            boolean greedy) {
        List<BaseStation> baseStationDayAmbulanceProportionList = this.getAllocation()
                .getBaseStationDayAmbulanceProportionList();
        List<BaseStation> baseStationNightAmbulanceProportionList = this.getAllocation()
                .getBaseStationNightAmbulanceProportionList();

        if (dayHighestN == -1) {
            dayHighestN = baseStationDayAmbulanceProportionList.size();
        }
        if (dayLowestN == -1) {
            dayLowestN = baseStationDayAmbulanceProportionList.size();
        }
        if (nightHighestN == -1) {
            nightHighestN = baseStationNightAmbulanceProportionList.size();
        }
        if (nightLowestN == -1) {
            nightLowestN = baseStationNightAmbulanceProportionList.size();
        }

        List<List<Integer>> daySubChromosomeNeighbors = new ArrayList<>();
        daySubChromosomeNeighbors.add(this.getAllocation().getDayShiftAllocation());
        List<List<Integer>> nightSubChromosomeNeighbors = new ArrayList<>();
        nightSubChromosomeNeighbors.add(this.getAllocation().getNightShiftAllocation());

        for (BaseStation highStation : baseStationDayAmbulanceProportionList.subList(0, dayHighestN)) {
            for (BaseStation lowStation : baseStationDayAmbulanceProportionList.subList(
                    baseStationDayAmbulanceProportionList.size() - dayLowestN,
                    baseStationDayAmbulanceProportionList.size())) {
                List<Integer> newChromosome = new ArrayList<>(getAllocation().getDayShiftAllocation());
                newChromosome.set(newChromosome.indexOf(highStation.getId()), lowStation.getId());
                daySubChromosomeNeighbors.add(newChromosome);
            }
        }

        for (BaseStation highStation : baseStationNightAmbulanceProportionList.subList(0, nightHighestN)) {
            for (BaseStation lowStation : baseStationNightAmbulanceProportionList.subList(
                    baseStationNightAmbulanceProportionList.size() - nightLowestN,
                    baseStationNightAmbulanceProportionList.size())) {
                List<Integer> newChromosome = new ArrayList<>(getAllocation().getNightShiftAllocation());
                newChromosome.set(newChromosome.indexOf(highStation.getId()), lowStation.getId());
                nightSubChromosomeNeighbors.add(newChromosome);
            }
        }

        List<Individual> neighboorhood = new ArrayList<>();
        for (List<Integer> daySubChromosomeNeighbor : daySubChromosomeNeighbors) {
            for (List<Integer> nightSubChromosomeNeighbor : nightSubChromosomeNeighbors) {
                Individual neighbor = new Individual(List.of(daySubChromosomeNeighbor, nightSubChromosomeNeighbor));
                if (greedy && neighbor.getFitness() <= this.getFitness()) {
                    return neighbor;
                } else {
                    neighboorhood.add(neighbor);
                }
            }
        }

        return neighboorhood.stream().min(Comparator.comparingDouble(Individual::getFitness)).get();
    }
}
