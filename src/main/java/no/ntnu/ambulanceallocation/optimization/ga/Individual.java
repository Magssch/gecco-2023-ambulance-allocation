package no.ntnu.ambulanceallocation.optimization.ga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static final int NUMBER_OF_OPERATORS = 3;
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
        return operatorCritic.selectNext();
    }

    private Individual improve(NeighborhoodFunction neighborhoodFunction, int neighborhoodSize) {
        return switch (Parameters.IMPROVE_OPERATOR) {
            case OPERATORCRITIC -> improveWithCritic(neighborhoodFunction, neighborhoodSize);
            case SLS -> improveWithSLS(neighborhoodFunction, neighborhoodSize);
            case ROBINHOOD -> improveWithRobinHood();
        };
    }

    private Individual improveWithCritic(NeighborhoodFunction neighborhoodFunction, int neighborhoodSize) {
        int operator = selectImproveOperator();
        Individual individual = switch (operator) {
            case 0 -> robinHoodTakeFirst(Utils.randomInt(2) + 1);
            case 1 -> robinHoodGiveFirst(Utils.randomInt(2) + 1);
            case 2 -> {
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

    private Individual improveWithSLS(NeighborhoodFunction neighborhoodFunction, int neighborhoodSize) {
        SlsSolution slsSolution = new SlsSolution(this);
        SlsSolution bestNeighborhood = slsSolution.greedyStep(neighborhoodFunction, neighborhoodSize);
        return new Individual(bestNeighborhood);
    }

    private Individual improveWithRobinHood() {
        Individual individual = switch (Utils.randomInt(2)) {
            case 0 -> robinHoodTakeFirst(Utils.randomInt(2) + 1);
            case 1 -> robinHoodGiveFirst(Utils.randomInt(2) + 1);
            default ->
                throw new IllegalStateException(String.format("Operator %d not present. Illegal value."));
        };
        return individual;
    }

    private Individual robinHoodTakeFirst(int takeFrom) {
        int chromosomeNumber = Utils.randomIndexOf(getAllocation().allocation());
        List<Integer> chromosome = getAllocation().get(chromosomeNumber);
        List<Integer> baseStationAmbulanceProportionList = getAllocation()
                .getBaseStationAmbulanceProportionList(chromosome);

        List<Individual> neighborhood = new ArrayList<>();

        int overproportionateStation = baseStationAmbulanceProportionList.get(takeFrom);
        int overproportionateIndex = chromosome.indexOf(overproportionateStation);
        for (int baseStationId : BaseStation.ids()) {
            if (baseStationId == overproportionateStation) {
                continue;
            }
            neighborhood.add(new Individual(this, chromosomeNumber, overproportionateIndex, baseStationId));
        }
        System.out.println(neighborhood);
        Individual bestNeighbor = neighborhood.stream().min(Comparator.comparingDouble(Individual::getFitness)).get();

        return bestNeighbor;
    }

    private Individual robinHoodGiveFirst(int giveTo) {
        int chromosomeNumber = Utils.randomIndexOf(getAllocation().allocation());
        List<Integer> chromosome = getAllocation().get(chromosomeNumber);
        List<Integer> baseStationAmbulanceProportionList = getAllocation()
                .getBaseStationAmbulanceProportionList(chromosome);

        List<Individual> neighborhood = new ArrayList<>();

        int underproportionateStation = baseStationAmbulanceProportionList.get(BaseStation.size() - 1 - giveTo);
        for (int baseStationId : BaseStation.ids()) {
            if (baseStationId == underproportionateStation) {
                continue;
            }
            int baseStationIndex = chromosome.indexOf(baseStationId);
            if (baseStationIndex == -1) {
                continue;
            }
            neighborhood
                    .add(new Individual(this, chromosomeNumber, baseStationIndex,
                            underproportionateStation));
        }
        System.out.println(neighborhood);
        Individual bestNeighbor = neighborhood.stream().min(Comparator.comparingDouble(Individual::getFitness)).get();

        return bestNeighbor;
    }
}
