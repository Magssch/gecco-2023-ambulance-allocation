package no.ntnu.ambulanceallocation.optimization.ga;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.experiments.Result;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.utils.Tuple;
import no.ntnu.ambulanceallocation.utils.Utils;

public class GeneticAlgorithm implements Optimizer {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private final Logger logger = LoggerFactory.getLogger(GeneticAlgorithm.class);

    private final List<Double> bestFitness = new ArrayList<>();
    private final List<Double> averageFitness = new ArrayList<>();
    private final List<Double> diversity = new ArrayList<>();
    private long executionTime = System.nanoTime();

    protected Config config;
    protected Population population;

    public GeneticAlgorithm() {
        this.config = Config.defaultConfig();
    }

    public GeneticAlgorithm(Config config) {
        this.config = config;
    }

    @Override
    public Solution getOptimalSolution() {
        return population.elite(Parameters.ELITE_SIZE).get(0);
    }

    @Override
    public void optimize() {
        clearRunStatistics();
        population = new Population(Parameters.POPULATION_SIZE, Parameters.INITIALIZER, config);

        Runnable optimizationWrapper = () -> {
            logger.info("Starting GA optimizer...");

            population.evaluate();

            int generation = 0;
            long startTime = System.nanoTime();

            while (elapsedTime(startTime) < Parameters.MAX_RUNNING_TIME && generation < Parameters.GENERATIONS) {
                printAndSaveSummary(logger, generation, population);
                executionTime = System.nanoTime();

                List<Individual> elite = population.elite(Parameters.ELITE_SIZE);
                Population nextPopulation = new Population(elite);

                CountDownLatch countDownLatch = new CountDownLatch(
                        Parameters.POPULATION_SIZE - Parameters.ELITE_SIZE);
                for (int i = 0; i < ((Parameters.POPULATION_SIZE - Parameters.ELITE_SIZE) / 2); i++) {
                    executor.execute(() -> {
                        Tuple<Individual> parents = population.selection(Parameters.TOURNAMENT_SIZE);
                        Individual offspringA = parents.first();
                        Individual offspringB = parents.second();

                        Tuple<Individual> offspring = offspringA.recombineWith(offspringB,
                                Parameters.CROSSOVER_PROBABILITY);
                        offspringA = offspring.first();
                        offspringB = offspring.second();

                        offspringA.mutate(Parameters.MUTATION_PROBABILITY);
                        offspringB.mutate(Parameters.MUTATION_PROBABILITY);

                        synchronized (nextPopulation) {
                            if (nextPopulation.size() < Parameters.POPULATION_SIZE) {
                                nextPopulation.add(offspringA);
                                countDownLatch.countDown();
                                if (nextPopulation.size() < Parameters.POPULATION_SIZE) {
                                    nextPopulation.add(offspringB);
                                    countDownLatch.countDown();
                                }
                            }
                        }
                    });
                }
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                population = nextPopulation;
                population.evaluate();
                generation++;
            }

            logger.info("GA finished successfully.");
        };

        long optimizationTime = Utils.timeIt(optimizationWrapper,
                false);
        logger.info("Total GA optimization time: " + optimizationTime + " seconds");
    }

    @Override
    public Result getRunStatistics() {
        Result runStatistics = new Result();
        runStatistics.saveColumn("best", bestFitness);
        runStatistics.saveColumn("average", averageFitness);
        runStatistics.saveColumn("diversity", diversity);
        return runStatistics;
    }

    @Override
    public String getAbbreviation() {
        return "GA";
    }

    protected long elapsedTime(long startTime) {
        return TimeUnit.SECONDS.convert((System.nanoTime() - startTime), TimeUnit.NANOSECONDS);
    }

    protected void printAndSaveSummary(Logger logger, int generation, Population population) {
        logger.info("{} generation: {}", getAbbreviation(), generation);
        double bestFitness = population.getBestFitness();
        double averageFitness = population.getAverageFitness();
        double diversity = population.getDiversity();
        logger.info("Best fitness: {}", bestFitness);
        logger.info("Average fitness: {}", averageFitness);
        logger.info("Diversity: {}", diversity);
        this.bestFitness.add(bestFitness);
        this.averageFitness.add(averageFitness);
        this.diversity.add(diversity);
    }

    protected void clearRunStatistics() {
        bestFitness.clear();
        averageFitness.clear();
        diversity.clear();
    }

}
