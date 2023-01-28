package no.ntnu.ambulanceallocation.optimization.sls;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.experiments.Result;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.utils.Utils;

public class StochasticLocalSearch implements Optimizer {

    static private final AtomicInteger instanceCount = new AtomicInteger(1);

    static {
        instanceCount.getAndIncrement();
    }

    private final Logger logger = LoggerFactory.getLogger(StochasticLocalSearch.class);
    private final List<Integer> tries = new ArrayList<>();
    private final List<Integer> flips = new ArrayList<>();
    private final List<Double> current = new ArrayList<>();
    private final List<Double> best = new ArrayList<>();

    private final NeighborhoodFunction neighborhoodFunction;
    private final int neighborhoodSize;

    private final Config config;

    private SlsSolution bestSolution;
    private SlsSolution solution;

    public StochasticLocalSearch(NeighborhoodFunction neighborhoodFunction) {
        this.neighborhoodFunction = neighborhoodFunction;
        this.neighborhoodSize = Parameters.LAZY_NEIGHBOURHOOD_SIZE;
        this.config = Config.defaultConfig();
        bestSolution = new SlsSolution(config);
        solution = new SlsSolution(config);
    }

    public StochasticLocalSearch(NeighborhoodFunction neighborhoodFunction, Config config) {
        this.neighborhoodFunction = neighborhoodFunction;
        this.neighborhoodSize = Parameters.LAZY_NEIGHBOURHOOD_SIZE;
        this.config = config;
        bestSolution = new SlsSolution(config);
        solution = new SlsSolution(config);
    }

    public StochasticLocalSearch(NeighborhoodFunction neighborhoodFunction, int neighborhoodSize) {
        this.neighborhoodFunction = neighborhoodFunction;
        this.neighborhoodSize = neighborhoodSize;
        this.config = Config.defaultConfig();
        bestSolution = new SlsSolution(config);
        solution = new SlsSolution(config);
    }

    @Override
    public Solution getOptimalSolution() {
        return bestSolution;
    }

    @Override
    public void optimize() {
        clearRunStatistics();

        bestSolution = new SlsSolution(config);
        solution = new SlsSolution(config);

        Runnable optimizationWrapper = () -> {

            logger.info("Starting {} optimizer...", getAbbreviation());
            int tries = 0;
            int flips = 0;
            long startTime = System.nanoTime();

            logger.info("{} tries: {}", getAbbreviation(), tries);
            logger.info("{} best fitness: {}", getAbbreviation(), bestSolution.getFitness());
            while (elapsedTime(startTime) < Parameters.MAX_RUNNING_TIME && tries < Parameters.MAX_TRIES) {

                if (Utils.randomDouble() < Parameters.RESTART_PROBABILITY) {
                    solution.restartStep();
                    tries++;
                    flips = 0;
                    logger.info("{} tries: {}", getAbbreviation(), tries);
                    logger.info("{} best fitness: {}", getAbbreviation(), bestSolution.getFitness());
                } else {
                    if (Utils.randomDouble() < Parameters.NOISE_PROBABILITY) {
                        logger.info("{} flips: {} (n)", getAbbreviation(), flips);
                        solution.noiseStep();
                    } else {
                        logger.info("{} flips: {} (g)", getAbbreviation(), flips);
                        solution.greedyStep(neighborhoodFunction, neighborhoodSize);
                    }
                }

                if (solution.compareTo(bestSolution) <= 0) {
                    bestSolution = new SlsSolution(solution);
                }

                saveSummary(tries, flips++);
            }
        };

        long optimizationTime = Utils.timeIt(optimizationWrapper, false);
        logger.info("Total {} optimization time: {} s", getAbbreviation(), optimizationTime);
    }

    @Override
    public Result getRunStatistics() {
        Result runStatistics = new Result();
        runStatistics.saveColumn("tries", tries);
        runStatistics.saveColumn("flips", flips);
        runStatistics.saveColumn("current", current);
        runStatistics.saveColumn("best", best);
        return runStatistics;
    }

    @Override
    public String getAbbreviation() {
        if (instanceCount.get() == 1) {
            return "SLS";
        }
        return switch (neighborhoodFunction) {
            case FORWARD -> "FSLS";
            case HAMMING -> "HSLS";
            case LAZY -> String.format("LazySLS_%d", neighborhoodSize);
        };
    }

    private void saveSummary(int tries, int flips) {
        this.tries.add(tries);
        this.flips.add(flips);
        this.current.add(solution.getFitness());
        this.best.add(bestSolution.getFitness());
    }

    private long elapsedTime(long startTime) {
        return TimeUnit.SECONDS.convert((System.nanoTime() - startTime), TimeUnit.NANOSECONDS);
    }

    private void clearRunStatistics() {
        tries.clear();
        flips.clear();
        current.clear();
        best.clear();
    }

}
