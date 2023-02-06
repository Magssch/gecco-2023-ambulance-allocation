package no.ntnu.ambulanceallocation.optimization.ga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.initializer.Initializer;
import no.ntnu.ambulanceallocation.optimization.ma.EvolutionStrategy;
import no.ntnu.ambulanceallocation.optimization.ma.OperatorCritic;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.optimization.sls.SlsSolution;
import no.ntnu.ambulanceallocation.simulation.BaseStation;
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.utils.Tuple;
import no.ntnu.ambulanceallocation.utils.Utils;

public class Individual extends Solution {

    private static final Logger logger = LoggerFactory.getLogger(Individual.class);

    public static final int NUMBER_OF_OPERATORS = 6;
    public static final OperatorCritic operatorCritic = new OperatorCritic(NUMBER_OF_OPERATORS);

    public Individual(List<List<Integer>> chromosomes) {
        super(chromosomes);
    }

    public Individual(Initializer initializer, Config config) {
        super(initializer, config);
    }

    public Individual(Solution solution) {
        super(solution);
    }

    private Individual(Individual root, int chromosomeNumber, int locus, int allele) {
        this(root);
        setAllocation(chromosomeNumber, locus, allele);
    }

    public void mutate(double mutationProbability) {
        List<List<Integer>> dna = new ArrayList<>();
        for (List<Integer> chromosome : getAllocation()) {
            List<Integer> newChromosome = new ArrayList<>(chromosome);
            for (int locus = 0; locus < newChromosome.size(); locus++) {
                if (Parameters.USE_SWAP_MUTATION && Utils.randomDouble() < mutationProbability) {
                    if (Utils.randomDouble() < 0.25) {
                        swapMutation(locus, newChromosome);
                    } else {
                        bitFlipMutation(locus, newChromosome);
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
            int neighborhoodSize,
            double improveProbability) {
        if (Utils.randomDouble() < improveProbability) {
            // find the best individual in population
            switch (evolutionStrategy) {
                case DARWINIAN -> {
                }
                case BALDWINIAN -> {
                    Individual bestNeighbor = improve(neighborhoodFunction, neighborhoodSize);
                    if (bestNeighbor.getFitness() <= getFitness()) {
                        this.setFitness(bestNeighbor.getFitness());
                    }
                }
                case LAMARCKIAN -> {
                    Individual bestNeighbor = improve(neighborhoodFunction, neighborhoodSize);
                    if (bestNeighbor.getFitness() <= getFitness()) {
                        copy(bestNeighbor);
                    }
                }
            }
        }
    }

    private int selectImproveOperator() {
        if (Parameters.USE_OPERATOR_CRITIC) {
            return operatorCritic.selectNext();
        }
        return Utils.randomInt(NUMBER_OF_OPERATORS);
    }

    private Individual improve(NeighborhoodFunction neighborhoodFunction, int neighborhoodSize) {
        int operator = selectImproveOperator();
        Individual individual = switch (operator) {
            case 0 -> robinHoodNeighborhoodSearch(Utils.randomInt(3) + 1, Utils.randomInt(3) + 1,
                    Utils.randomInt(3) + 1, Utils.randomInt(3) + 1, true);
            case 1 -> robinHoodNeighborhoodSearch(Utils.randomInt(3) + 1, -1, Utils.randomInt(3) + 1, -1, true);
            case 2 -> robinHoodNeighborhoodSearch(-1, Utils.randomInt(3) + 1, -1, Utils.randomInt(3) + 1, true);
            case 3 -> robinHoodTakeFirst(Utils.randomInt(2) + 1);
            case 4 -> robinHoodGiveFirst(Utils.randomInt(2) + 1);
            case 5 -> {
                SlsSolution slsSolution = new SlsSolution(this);
                SlsSolution bestNeighborhood = slsSolution.greedyStep(neighborhoodFunction, neighborhoodSize);
                yield new Individual(bestNeighborhood);
            }
            default ->
                throw new IllegalStateException(String.format("Operator %d not present. Illegal value.", operator));
        };

        operatorCritic.assignCredit(operator, this.getFitness() - individual.getFitness());
        return individual;
    }

    private Individual robinHoodTakeFirst(int takeFrom) {
        int chromosomeNumber = Utils.randomIndexOf(getAllocation().allocation());
        List<Integer> chromosome = getAllocation().get(chromosomeNumber);
        List<Integer> baseStationAmbulanceProportionList = getAllocation()
                .getBaseStationAmbulanceProportionList(chromosome);

        List<Individual> neighborhood = new ArrayList<>();
        Individual bestNeighbor = this;

        for (int i = 0; i < takeFrom; i++) {
            int overproportionateStation = baseStationAmbulanceProportionList.get(i);
            int overproportionateIndex = bestNeighbor.getAllocation().get(chromosomeNumber)
                    .indexOf(overproportionateStation);
            for (int baseStationId : BaseStation.ids()) {
                neighborhood.add(new Individual(bestNeighbor, chromosomeNumber, overproportionateIndex, baseStationId));
            }
            neighborhood.remove(overproportionateStation);
            bestNeighbor = neighborhood.stream().min(Comparator.comparingDouble(Individual::getFitness)).get();
        }
        return bestNeighbor;
    }

    private Individual robinHoodGiveFirst(int giveTo) {
        int chromosomeNumber = Utils.randomIndexOf(getAllocation().allocation());
        List<Integer> chromosome = getAllocation().get(chromosomeNumber);
        List<Integer> baseStationAmbulanceProportionList = getAllocation()
                .getBaseStationAmbulanceProportionList(chromosome);

        List<Individual> neighborhood = new ArrayList<>();
        Individual bestNeighbor = this;

        for (int i = 0; i < giveTo; i++) {
            int underproportionateStation = baseStationAmbulanceProportionList.get(BaseStation.size() - 1 - i);
            for (int baseStationId : BaseStation.ids()) {
                neighborhood
                        .add(new Individual(bestNeighbor, chromosomeNumber, baseStationId, underproportionateStation));
            }
            bestNeighbor = neighborhood.stream().min(Comparator.comparingDouble(Individual::getFitness)).get();
        }
        return bestNeighbor;
    }

    // Memetic method
    private Individual robinHoodNeighborhoodSearch(int dayHighestN, int dayLowestN, int nightHighestN, int nightLowestN,
            boolean greedy) {

        List<Integer> baseStationDayAmbulanceProportionList = this.getAllocation()
                .getBaseStationDayAmbulanceProportionList();
        List<Integer> baseStationNightAmbulanceProportionList = this.getAllocation()
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

        for (int highStation : baseStationDayAmbulanceProportionList.subList(0, dayHighestN)) {
            for (int lowStation : baseStationDayAmbulanceProportionList.subList(
                    baseStationDayAmbulanceProportionList.size() - dayLowestN,
                    baseStationDayAmbulanceProportionList.size())) {
                if (highStation == lowStation || !getAllocation().getDayShiftAllocation().contains(highStation)) {
                    continue;
                }
                List<Integer> newChromosome = new ArrayList<>(getAllocation().getDayShiftAllocation());
                newChromosome.set(newChromosome.indexOf(highStation), lowStation);
                daySubChromosomeNeighbors.add(newChromosome);
            }
        }

        for (int highStation : baseStationNightAmbulanceProportionList.subList(0, nightHighestN)) {
            for (int lowStation : baseStationNightAmbulanceProportionList.subList(
                    baseStationNightAmbulanceProportionList.size() - nightLowestN,
                    baseStationNightAmbulanceProportionList.size())) {
                if (highStation == lowStation || !getAllocation().getNightShiftAllocation().contains(highStation)) {
                    continue;
                }
                List<Integer> newChromosome = new ArrayList<>(getAllocation().getNightShiftAllocation());
                newChromosome.set(newChromosome.indexOf(highStation), lowStation);
                nightSubChromosomeNeighbors.add(newChromosome);
            }
        }

        int neighborhoodSize = daySubChromosomeNeighbors.size() * nightSubChromosomeNeighbors.size();

        List<Integer> randomSubset = new ArrayList<>();
        if (neighborhoodSize > Parameters.LOCAL_NEIGHBORHOOD_MAX_SIZE) {
            randomSubset = Utils.random.ints(0, neighborhoodSize).distinct()
                    .limit(Parameters.LOCAL_NEIGHBORHOOD_MAX_SIZE).boxed()
                    .collect(Collectors.toList());
        }

        List<Individual> neighborhood = new ArrayList<>();
        for (int i = 0; i < daySubChromosomeNeighbors.size(); i++) {
            for (int j = 0; j < nightSubChromosomeNeighbors.size(); j++) {
                if (neighborhoodSize <= Parameters.LOCAL_NEIGHBORHOOD_MAX_SIZE || randomSubset.contains(i * j)) {
                    Individual neighbor = new Individual(
                            List.of(daySubChromosomeNeighbors.get(i), nightSubChromosomeNeighbors.get(j)));
                    if (neighbor.equals(this)) {
                        throw new IllegalArgumentException("Neighbor is not a neighbor!");
                    }
                    if (greedy && neighbor.getFitness() <= this.getFitness()) {
                        return neighbor;
                    } else {
                        neighborhood.add(neighbor);
                    }
                }
            }
        }

        return neighborhood.stream().min(Comparator.comparingDouble(Individual::getFitness)).get();
    }
}
