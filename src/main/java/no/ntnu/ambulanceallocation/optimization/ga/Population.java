package no.ntnu.ambulanceallocation.optimization.ga;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.initializer.Initializer;
import no.ntnu.ambulanceallocation.optimization.initializer.PopulationProportionate;
import no.ntnu.ambulanceallocation.simulation.BaseStation;
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.utils.Tuple;
import no.ntnu.ambulanceallocation.utils.Utils;

public class Population implements Iterable<Individual> {

    private final List<Individual> population;

    public Population(int populationSize, int seedingSize, Initializer initializer, Config config) {
        population = new ArrayList<>();
        for (int i = 0; i < populationSize - seedingSize; i++) {
            population.add(new Individual(initializer, config));
        }
        // Seed with close to optimal PopulationProportionate initializer strategy
        for (int i = 0; i < seedingSize; i++) {
            population.add(new Individual(new PopulationProportionate(), config));
        }
    }

    public Population(List<Individual> population) {
        this.population = population.stream().map(Individual::new).collect(Collectors.toList());
    }

    public void add(Individual individual) {
        population.add(new Individual(individual));
    }

    public Individual get(int index) {
        return population.get(index);
    }

    public int size() {
        return population.size();
    }

    public double getAverageFitness() {
        return population
                .stream()
                .mapToDouble(Individual::getFitness)
                .average()
                .orElseThrow();
    }

    public double getBestFitness() {
        return population
                .stream()
                .mapToDouble(Individual::getFitness)
                .min()
                .orElseThrow();
    }

    public double getDiversity() {
        int bins = BaseStation.size();
        double entropy = 0.0;
        int numberOfChromosomes = population.get(0).getAllocation().size();

        for (int chromosomeNumber = 0; chromosomeNumber < numberOfChromosomes; chromosomeNumber++) {
            double total = population.size() * population.get(0).getAllocation().get(chromosomeNumber).size();

            int finalChromosomeNumber = chromosomeNumber;
            Collection<Long> occurrences = population
                    .stream()
                    .flatMap(individual -> individual.getAllocation().get(finalChromosomeNumber).stream())
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .values();

            DoubleStream information = occurrences
                    .parallelStream()
                    .mapToDouble(occurrence -> occurrence / total)
                    .map(probability -> probability * Utils.logn(probability, bins));

            entropy += -information.sum();
        }
        return entropy / numberOfChromosomes;
    }

    public List<Individual> elite(int eliteSize) {
        Collections.sort(population);
        return population.subList(0, eliteSize);
    }

    public void evaluate() {
        population.parallelStream().forEach(Solution::getFitness);
    }

    public Tuple<Individual> selection(int tournamentSize) {
        List<Individual> tournament = Utils.randomChooseN(population, tournamentSize);
        Collections.sort(tournament);
        return new Tuple<>(tournament.subList(0, 2));
    }

    @Override
    public Iterator<Individual> iterator() {
        return population.iterator();
    }

}
