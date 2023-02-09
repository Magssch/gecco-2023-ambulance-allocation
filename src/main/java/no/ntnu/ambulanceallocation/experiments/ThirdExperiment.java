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

    private final List<Double> ratioList = List.of(
            0.2,
            0.45,
            0.475,
            0.5,
            0.525,
            0.55,
            0.575,
            0.6,
            0.625,
            0.65,
            0.8);

    @Override
    public void run() {

        runs.saveColumn("ratio", ratioList.stream()
                .map(elt -> Collections.nCopies(Parameters.RUNS, elt))
                .flatMap(List::stream)
                .collect(Collectors.toList()));

        runs.saveColumn("run",
                Collections.nCopies(ratioList.size(), IntStream.rangeClosed(1, Parameters.RUNS)
                        .boxed().collect(Collectors.toList())).stream().flatMap(List::stream)
                        .toList());

        logger.info("Testing model 'Population Proportionate'");
        runPopulationProportionate();

        // logger.info("Testing model 'LazySLS'");
        // StochasticLocalSearch lazyStochasticLocalSearch = new
        // StochasticLocalSearch(NeighborhoodFunction.LAZY);
        // runStochasticOptimizer(lazyStochasticLocalSearch, "LazySLS");
        // Simulation.saveAllocationResults();

        logger.info("Testing model 'GA'");
        Optimizer ga = new GeneticAlgorithm();
        runStochasticOptimizer(ga, "GA");
        Simulation.saveAllocationResults();

        logger.info("Testing model 'MA_lazy'");
        Optimizer slsma = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN,
                ImproveOperator.SLS,
                NeighborhoodFunction.LAZY);
        runStochasticOptimizer(slsma, "MA_lazy");
        Simulation.saveAllocationResults();

        logger.info("Testing model 'RHMA'");
        Optimizer rhma = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN,
                ImproveOperator.ROBINHOOD,
                NeighborhoodFunction.LAZY);
        runStochasticOptimizer(rhma, "MA_lazy");
        Simulation.saveAllocationResults();
    }

    private void runPopulationProportionate() {
        logger.info("Testing model '{}'", "PopulationProportionate");
        final int totalNumAmbulances = Parameters.NUMBER_OF_AMBULANCES_DAY
                + Parameters.NUMBER_OF_AMBULANCES_NIGHT;
        List<Double> fitness = new ArrayList<>();

        for (double ambulanceRatio : ratioList) {
            int numDayAmbulances = (int) Math.round(totalNumAmbulances * ambulanceRatio);
            int numNightAmbulances = (int) Math
                    .round(totalNumAmbulances * (1 - ambulanceRatio));

            logger.info("Running PopulationProportionate with {} day ambulances and {} night ambulances (ratio {})",
                    numDayAmbulances,
                    numNightAmbulances, ambulanceRatio);

            Allocation allocation = new Allocation(List.of(
                    populationProportionate.initialize(numDayAmbulances),
                    populationProportionate.initialize(numNightAmbulances)));
            ResponseTimes results = Simulation
                    .withConfig(Config.withNumAmbulances(numDayAmbulances, numNightAmbulances))
                    .simulate(allocation);

            fitness.addAll(Collections.nCopies(Parameters.RUNS, results.average()));
        }

        runs.saveColumn("PopProp", fitness);
    }

    private void runStochasticOptimizer(Optimizer optimizer, String name) {
        logger.info("Testing model '{}'", name);
        final int totalNumAmbulances = Parameters.NUMBER_OF_AMBULANCES_DAY
                + Parameters.NUMBER_OF_AMBULANCES_NIGHT;
        List<Double> bestFitnessAtTermination = new ArrayList<>();

        for (double ambulanceRatio : ratioList) {
            int numDayAmbulances = (int) Math.round(totalNumAmbulances * ambulanceRatio);
            int numNightAmbulances = (int) Math
                    .round(totalNumAmbulances * (1 - ambulanceRatio));
            Parameters.NUMBER_OF_AMBULANCES_DAY = numDayAmbulances;
            Parameters.NUMBER_OF_AMBULANCES_NIGHT = numNightAmbulances;

            logger.info("Running optimizations with {} day ambulances and {} night ambulances (ratio {})",
                    numDayAmbulances,
                    numNightAmbulances, ambulanceRatio);

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
