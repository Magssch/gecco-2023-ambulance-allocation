package no.ntnu.ambulanceallocation.optimization.ma;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.ga.GeneticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.ga.Individual;
import no.ntnu.ambulanceallocation.optimization.ga.Population;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.utils.Tuple;
import no.ntnu.ambulanceallocation.utils.Utils;

public class MemeticAlgorithm extends GeneticAlgorithm {

    static private final AtomicInteger instanceCount = new AtomicInteger();

    {
        instanceCount.getAndIncrement();
    }

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private final Logger logger = LoggerFactory.getLogger(MemeticAlgorithm.class);

    private final EvolutionStrategy evolutionStrategy;
    private final NeighborhoodFunction neighborhoodFunction;
    private final ImproveOperator improveOperator;
    private final int neighborhoodSize;

    public MemeticAlgorithm(EvolutionStrategy evolutionStrategy, ImproveOperator improveOperator,
            NeighborhoodFunction neighborhoodFunction,
            Config config) {
        this(evolutionStrategy, improveOperator, neighborhoodFunction, Parameters.LAZY_NEIGHBOURHOOD_SIZE, config);

    }

    public MemeticAlgorithm(EvolutionStrategy evolutionStrategy, ImproveOperator improveOperator,
            NeighborhoodFunction neighborhoodFunction,
            int neighborhoodSize,
            Config config) {
        super(config);
        this.evolutionStrategy = evolutionStrategy;
        this.neighborhoodFunction = neighborhoodFunction;
        this.neighborhoodSize = neighborhoodSize;
        this.improveOperator = improveOperator;
    }

    public MemeticAlgorithm(EvolutionStrategy evolutionStrategy, ImproveOperator improveOperator,
            NeighborhoodFunction neighborhoodFunction) {
        this(evolutionStrategy, improveOperator, neighborhoodFunction, Parameters.LAZY_NEIGHBOURHOOD_SIZE,
                Config.defaultConfig());
    }

    @Override
    public void optimize() {
        clearRunStatistics();
        population = new Population(Parameters.POPULATION_SIZE, Parameters.POPULATION_PROPORTIONATE_SEEDING_SIZE,
                Parameters.INITIALIZER, config);

        Runnable optimizationWrapper = () -> {
            logger.info("Starting {} optimizer...", getAbbreviation());

            population.evaluate();

            int generation = 0;
            long startTime = System.nanoTime();

            while (elapsedTime(startTime) < Parameters.MAX_RUNNING_TIME && generation < Parameters.GENERATIONS) {
                printAndSaveSummary(logger, generation, population);

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

                        // MA step
                        offspringA.improve(evolutionStrategy, improveOperator, neighborhoodFunction, neighborhoodSize,
                                Parameters.IMPROVE_PROBABILITY);
                        offspringB.improve(evolutionStrategy, improveOperator, neighborhoodFunction, neighborhoodSize,
                                Parameters.IMPROVE_PROBABILITY);

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

            logger.info("{} finished successfully.", getAbbreviation());
        };

        long optimizationTime = Utils.timeIt(optimizationWrapper, false);
        logger.info("Total {} optimization time: {} s", getAbbreviation(), optimizationTime);
    }

    @Override
    public String getAbbreviation() {
        if (instanceCount.get() == 1) {
            return "MA";
        }

        return switch (improveOperator) {
            case ROBINHOOD -> "FRB";
            case SLS -> switch (neighborhoodFunction) {
                case FORWARD -> "FMA";
                case HAMMING -> "HMA";
                case LAZY -> String.format("MA_LazySLS_%d", neighborhoodSize);
            };
            case OPERATORCRITIC -> "OCMA";
        };
    }

    @Override
    public Config getConfig() {
        return config;
    }

}
