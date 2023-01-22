package no.ntnu.ambulanceallocation.optimization.ga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
                    switch (Utils.randomInt(3)) {
                        case 0 -> swapMutation(locus, newChromosome);
                        case 1 -> bitFlipMutation(locus, newChromosome);
                        case 2 -> inversionMutation(locus, newChromosome);
                        // case 3 -> redistributionMutation(locus, newChromosome);
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

    public void inversionMutation(int locus, List<Integer> newChromosome) {
        // Reverse a random subset of the chromosome between two random indices
        int randomLocus = Utils.randomInt(newChromosome.size());
        int start = Math.min(locus, randomLocus);
        int end = Math.max(locus, randomLocus);
        Collections.reverse(newChromosome.subList(start, end));

    }

    public void bitFlipMutation(int locus, List<Integer> newChromosome) {
        newChromosome.set(locus, Utils.randomInt(BaseStation.size()));
    }

    // get most frequent integer value from list
    private Individual redistributionMutation() {
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
        return bestNeighbor;
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

    private Individual improve() {
        return switch (Utils.randomInt(9)) {
            case 0 ->
                getBestPopPropFromMaxNeighbor(this.getAllocation().getDayShiftAllocation(), "day", "highest", false);
            case 1 ->
                getBestPopPropFromMaxNeighbor(this.getAllocation().getDayShiftAllocation(), "day", "lowest", false);
            case 2 -> getBestPopPropFromMaxNeighbor(this.getAllocation().getNightShiftAllocation(), "night", "highest",
                    false);
            case 3 ->
                getBestPopPropFromMaxNeighbor(this.getAllocation().getNightShiftAllocation(), "night", "lowest", false);
            case 4 ->
                getBestPopPropFromMaxNeighbor(this.getAllocation().getDayShiftAllocation(), "day", "highest", true);
            case 5 ->
                getBestPopPropFromMaxNeighbor(this.getAllocation().getDayShiftAllocation(), "day", "lowest", true);
            case 6 -> getBestPopPropFromMaxNeighbor(this.getAllocation().getNightShiftAllocation(), "night", "highest",
                    true);
            case 7 ->
                getBestPopPropFromMaxNeighbor(this.getAllocation().getNightShiftAllocation(), "night", "lowest", true);
            case 8 -> getBestPopPropMaxMinNeighbor();
            default -> throw new IllegalArgumentException("Unexpected value");
        };
    }

    // Memetic method
    private Individual getBestNeighborSLS(NeighborhoodFunction neighborhoodFunction) {
        SlsSolution slsSolution = new SlsSolution(this);
        SlsSolution bestNeighborhood = slsSolution.greedyStep(neighborhoodFunction);
        return new Individual(bestNeighborhood);
    }

    private Individual getBestPopPropFromMaxNeighbor(List<Integer> chromosome, String shift, String from,
            boolean earlyStopping) {
        Map<Integer, Long> ambulanceStationFrequency = chromosome.stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        BaseStation fromBaseStation = (from == "highest" ? ambulanceStationFrequency.keySet().stream()
                .map(BaseStation::get)
                .max(Comparator.comparingDouble(baseStation -> (double) baseStation.getPopulation()
                        / ambulanceStationFrequency.get(baseStation.getId())))
                : ambulanceStationFrequency.keySet().stream()
                        .map(BaseStation::get)
                        .min(Comparator.comparingDouble(baseStation -> (double) baseStation.getPopulation()
                                / ambulanceStationFrequency.get(baseStation.getId()))))
                .orElse(null);

        List<Individual> neighboorhood = new ArrayList<>();
        for (Integer baseStationId : chromosome.stream().distinct().toList()) {
            if (!baseStationId.equals(fromBaseStation.getId())) {
                List<Integer> newChromosome = new ArrayList<>(chromosome);
                newChromosome.set(chromosome.indexOf(fromBaseStation.getId()), baseStationId);
                Individual neighbor = new Individual(
                        shift.equals("day")
                                ? List.of(newChromosome, this.getAllocation().getNightShiftAllocation())
                                : List.of(this.getAllocation().getDayShiftAllocation(), newChromosome));
                if (earlyStopping && neighbor.getFitness() <= this.getFitness()) {
                    return neighbor;
                }
                neighboorhood.add(neighbor);
            }
        }
        return neighboorhood.stream().min(Comparator.comparingDouble(Individual::getFitness)).get();
    }

    private Individual getBestPopPropMaxMinNeighbor() {
        Map<Integer, Long> dayAmbulanceStationFrequency = this.getAllocation().getDayShiftAllocation().stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        Map<Integer, Long> nightAmbulanceStationFrequency = this.getAllocation().getNightShiftAllocation().stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        BaseStation highestProportionBaseStationDay = dayAmbulanceStationFrequency.keySet().stream()
                .map(BaseStation::get)
                .max(Comparator.comparingDouble(baseStation -> (double) baseStation.getPopulation()
                        / dayAmbulanceStationFrequency.get(baseStation.getId())))
                .orElse(null);
        BaseStation lowestProportionBaseStationDay = dayAmbulanceStationFrequency.keySet().stream()
                .map(BaseStation::get)
                .min(Comparator.comparingDouble(baseStation -> (double) baseStation.getPopulation()
                        / dayAmbulanceStationFrequency.get(baseStation.getId())))
                .orElse(null);

        BaseStation highestProportionBaseStationNight = nightAmbulanceStationFrequency.keySet().stream()
                .map(BaseStation::get)
                .max(Comparator.comparingDouble(baseStation -> (double) baseStation.getPopulation()
                        / nightAmbulanceStationFrequency.get(baseStation.getId())))
                .orElse(null);
        BaseStation lowestProportionBaseStationNight = nightAmbulanceStationFrequency.keySet().stream()
                .map(BaseStation::get)
                .min(Comparator.comparingDouble(baseStation -> (double) baseStation.getPopulation()
                        / nightAmbulanceStationFrequency.get(baseStation.getId())))
                .orElse(null);

        List<Integer> dayAmbulanceAllocation = new ArrayList<>(this.getAllocation().getDayShiftAllocation());
        List<Integer> nightAmbulanceAllocation = new ArrayList<>(this.getAllocation().getNightShiftAllocation());

        dayAmbulanceAllocation.set(dayAmbulanceAllocation.indexOf(highestProportionBaseStationDay.getId()),
                lowestProportionBaseStationDay.getId());
        nightAmbulanceAllocation.set(nightAmbulanceAllocation.indexOf(highestProportionBaseStationNight.getId()),
                lowestProportionBaseStationNight.getId());

        Individual bestNeighbor = List.of(
                new Individual(List.of(this.getAllocation().getDayShiftAllocation(), nightAmbulanceAllocation)),
                new Individual(List.of(dayAmbulanceAllocation, this.getAllocation().getNightShiftAllocation())),
                new Individual(List.of(dayAmbulanceAllocation, nightAmbulanceAllocation)))
                .stream()
                .min(Comparator.comparingDouble(Individual::getFitness)).get();
        return bestNeighbor;
    }

}
