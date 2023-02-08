package no.ntnu.ambulanceallocation.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.ga.GeneticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.initializer.PopulationProportionate;
import no.ntnu.ambulanceallocation.optimization.ma.EvolutionStrategy;
import no.ntnu.ambulanceallocation.optimization.ma.ImproveOperator;
import no.ntnu.ambulanceallocation.optimization.ma.MemeticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;

public class ThirdExperiment extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(ThirdExperiment.class);

    private final Result runs = new Result();
    private static final PopulationProportionate populationProportionate = new PopulationProportionate();

    private Allocation overallBestAllocation = new Allocation();

    private final List<Double> ratioList = List.of(
            // 0.1,
            // 0.15,
            // 0.2,
            // 0.25,
            // 0.3,
            // 0.35,
            0.4,
            0.45,
            0.5,
            0.55,
            0.6,
            0.65
    // 0.7,
    // 0.75,
    // 0.8,
    // 0.85,
    // 0.9
    );

    @Override
    public void run() {

        runs.saveColumn("ratio", ratioList.stream()
                .map(elt -> Collections.nCopies(Parameters.RUNS, elt))
                .flatMap(List::stream)
                .collect(Collectors.toList()));

        runs.saveColumn("run",
                IntStream.range(1, Parameters.RUNS + 1).boxed().map(elt -> Collections.nCopies(ratioList.size(), elt))
                        .flatMap(List::stream)
                        .collect(Collectors.toList()));

        logger.info("Testing model 'Population Proportionate'");
        runPopulationProportionate();

        logger.info("Testing model 'GA'");
        Optimizer ga = new GeneticAlgorithm();
        runStochasticOptimizer(ga, "GA");
        Simulation.saveAllocationResults();

        logger.info("Testing model 'MA_lazy'");
        Optimizer ma = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN,
                ImproveOperator.OPERATORCRITIC,
                NeighborhoodFunction.LAZY);
        runStochasticOptimizer(ma, "MA_lazy");
        Simulation.saveAllocationResults();
    }

    private void runPopulationProportionate() {
        logger.info("Testing model '{}'", "PopulationProportionate");
        final int totalNumAmbulances = Parameters.NUMBER_OF_AMBULANCES_DAY + Parameters.NUMBER_OF_AMBULANCES_NIGHT;
        List<Double> fitness = new ArrayList<>();

        for (double ambulanceRatio : ratioList) {
            int numDayAmbulances = (int) Math.round(totalNumAmbulances * ambulanceRatio);
            int numNightAmbulances = (int) Math
                    .round(totalNumAmbulances * (1 - ambulanceRatio));

            logger.info("Running PopulationProportionate with {} day ambulances and {} night ambulances",
                    numDayAmbulances,
                    numNightAmbulances);

            Allocation allocation = new Allocation(List.of(
                    populationProportionate.initialize(numDayAmbulances),
                    populationProportionate.initialize(numNightAmbulances)));
            ResponseTimes results = Simulation
                    .withConfig(Config.withNumAmbulances(numDayAmbulances, numNightAmbulances)).simulate(allocation);

            fitness.addAll(Collections.nCopies(Parameters.RUNS, results.average()));
        }

        runs.saveColumn("PopProp", fitness);
    }

    private void runStochasticOptimizer(Optimizer optimizer, String name) {
        logger.info("Testing model '{}'", name);
        final int totalNumAmbulances = Parameters.NUMBER_OF_AMBULANCES_DAY + Parameters.NUMBER_OF_AMBULANCES_NIGHT;
        List<Double> bestFitnessAtTermination = new ArrayList<>();

        for (double ambulanceRatio : ratioList) {
            int numDayAmbulances = (int) Math.round(totalNumAmbulances * ambulanceRatio);
            int numNightAmbulances = (int) Math
                    .round(totalNumAmbulances * (1 - ambulanceRatio));
            Parameters.NUMBER_OF_AMBULANCES_DAY = numDayAmbulances;
            Parameters.NUMBER_OF_AMBULANCES_NIGHT = numNightAmbulances;

            logger.info("Running optimizations with {} day ambulances and {} night ambulances",
                    numDayAmbulances,
                    numNightAmbulances);

            for (int i = 0; i < Parameters.RUNS; i++) {
                logger.info("Starting {}... run {}/{}", name, i + 1, Parameters.RUNS);
                optimizer.optimize();
                Solution solution = optimizer.getOptimalSolution();
                bestFitnessAtTermination.add(solution.getFitness());
                logger.info("{} run {}/{} completed.", name, i + 1, Parameters.RUNS);
            }
        }

        runs.saveColumn(name, bestFitnessAtTermination);
    }

    @Override
    public void saveResults() {
        runs.saveResults("third_experiment_runs");
    }

    public static void main(String[] args) {
        logger.info("Running experiment 3 ...");
        ThirdExperiment newThirdExperiment = new ThirdExperiment();
        newThirdExperiment.run();
        logger.info("Done");

        logger.info("Saving results for experiment 3 ...");
        newThirdExperiment.saveResults();
        logger.info("Experiment 3 completed successfully.");
    }

}
