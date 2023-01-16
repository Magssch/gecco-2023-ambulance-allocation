package no.ntnu.ambulanceallocation.optimization.ga;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static Map<Individual, Boolean> neighborMap = new HashMap<>();

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
                    newChromosome.set(locus, Utils.randomInt(BaseStation.size()));
                }
            }
            dna.add(newChromosome);
        }
        setAllocation(dna);
    }

    public void mutateAlt(double mutationProbability) {
        List<List<Integer>> dna = new ArrayList<>();
        for (List<Integer> chromosome : getAllocation()) {
            List<Integer> newChromosome = new ArrayList<>(chromosome);
            for (int locus = 0; locus < newChromosome.size(); locus++) {
                if (Utils.randomDouble() < mutationProbability) {
                    newChromosome.set(locus, Utils.randomInt(BaseStation.size()));
                }
            }
            dna.add(newChromosome);
        }
        setAllocation(dna);
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
        if (neighborMap.containsKey(this) && !neighborMap.get(this)) {
            return;
        }
        if (Utils.randomDouble() < improveProbability) {
            // find best individual in population
            switch (evolutionStrategy) {
                case DARWINIAN -> {
                }
                case BALDWINIAN -> {
                    Individual bestNeighbor = getBestNeighbor();
                    if (bestNeighbor.getFitness() <= getFitness()) {
                        this.setFitness(bestNeighbor.getFitness());
                    }
                }
                case LAMARCKIAN -> {
                    Individual bestNeighbor = getBestNeighbor();
                    if (bestNeighbor.getFitness() <= getFitness()) {
                        copy(bestNeighbor);
                    }
                }
            }
        }
    }

    // Memetic method
    private Individual getBestNeighborSLS(NeighborhoodFunction neighborhoodFunction) {
        SlsSolution slsSolution = new SlsSolution(this);
        SlsSolution bestNeighborhood = slsSolution.greedyStep(neighborhoodFunction);
        return new Individual(bestNeighborhood);
    }

    // get most frequent integer value from list
    private Individual getBestNeighbor() {
        List<Integer> dayAmbulanceAllocation = new ArrayList<>(this.getAllocation().getDayShiftAllocation());
        List<Integer> nightAmbulanceAllocation = new ArrayList<>(this.getAllocation().getNightShiftAllocation());

        Map<Integer, Long> dayAmbulanceStationFrequency = this.getAllocation().getDayShiftAllocation().stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        int dayMostFrequent = dayAmbulanceStationFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();
        int dayLeastFrequent = dayAmbulanceStationFrequency.entrySet().stream()
                .min(Map.Entry.comparingByValue()).get().getKey();

        List<Integer> dayNeighboorhoodAllocation1 = new ArrayList<>(dayAmbulanceAllocation);
        dayNeighboorhoodAllocation1.set(dayAmbulanceAllocation.indexOf(dayMostFrequent), dayLeastFrequent);

        List<Integer> dayNeighboorhoodAllocation2 = new ArrayList<>(dayAmbulanceAllocation);
        dayNeighboorhoodAllocation2.set(dayAmbulanceAllocation.indexOf(dayLeastFrequent), dayMostFrequent);

        Map<Integer, Long> nightAmbulanceStationFrequency = this.getAllocation().getNightShiftAllocation().stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        int nightMostFrequent = nightAmbulanceStationFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();
        int nightLeastFrequent = nightAmbulanceStationFrequency.entrySet().stream()
                .min(Map.Entry.comparingByValue()).get().getKey();

        List<Integer> nightNeighboorhoodAllocation1 = new ArrayList<>(nightAmbulanceAllocation);
        nightNeighboorhoodAllocation1.set(nightAmbulanceAllocation.indexOf(nightMostFrequent), nightLeastFrequent);

        List<Integer> nightNeighboorhoodAllocation2 = new ArrayList<>(nightAmbulanceAllocation);
        nightNeighboorhoodAllocation2.set(nightAmbulanceAllocation.indexOf(nightLeastFrequent), nightMostFrequent);

        // find neighbor with lowest fitness and return it
        Individual bestNeighbor = List.of(
                new Individual(List.of(dayNeighboorhoodAllocation1, nightAmbulanceAllocation)),
                new Individual(List.of(dayNeighboorhoodAllocation2, nightAmbulanceAllocation)),
                new Individual(List.of(dayAmbulanceAllocation, nightNeighboorhoodAllocation1)),
                new Individual(List.of(dayAmbulanceAllocation, nightNeighboorhoodAllocation2))).stream()
                .min(Comparator.comparingDouble(Individual::getFitness)).get();
        if (bestNeighbor.getFitness() < this.getFitness()) {
            neighborMap.put(this, true);
        }
        return bestNeighbor;
    }
}
