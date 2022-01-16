package no.ntnu.ambulanceallocation.optimization.ga;

import java.util.ArrayList;
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
        if (Utils.randomDouble() < improveProbability) {
            switch (evolutionStrategy) {
                case DARWINIAN -> {
                }
                case BALDWINIAN -> {
                    Individual bestNeighbor = getBestNeighbor(neighborhoodFunction);
                    if (bestNeighbor.getFitness() > getFitness()) {
                        this.setFitness(bestNeighbor.getFitness());
                    }
                }
                case LAMARCKIAN -> {
                    Individual bestNeighbor = getBestNeighbor(neighborhoodFunction);
                    if (bestNeighbor.getFitness() > getFitness()) {
                        copy(bestNeighbor);
                    }
                }
            }
        }
    }

    // Memetic method
    private Individual getBestNeighbor(NeighborhoodFunction neighborhoodFunction) {
        SlsSolution slsSolution = new SlsSolution(this);
        SlsSolution bestNeighborhood = slsSolution.greedyStep(neighborhoodFunction);
        return new Individual(bestNeighborhood);
    }

}
