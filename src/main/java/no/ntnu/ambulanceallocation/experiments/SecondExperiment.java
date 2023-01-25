package no.ntnu.ambulanceallocation.experiments;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.ga.GeneticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.ma.EvolutionStrategy;
import no.ntnu.ambulanceallocation.optimization.ma.MemeticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.optimization.sls.StochasticLocalSearch;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;

public class SecondExperiment extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(SecondExperiment.class);

    private final Result bestFitnessAtTerminationResult = new Result();
    private final Result overallBestResponseTimesResult = new Result();
    private final Result overallBestAllocationResult = new Result();

    @Override
    public void run() {
        // Setup
        StochasticLocalSearch forwardStochasticLocalSearch = new StochasticLocalSearch(NeighborhoodFunction.FORWARD);
        GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm();
        MemeticAlgorithm forwardMemeticAlgorithm = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN,
                NeighborhoodFunction.FORWARD);

        // Partial experiments
        runStochasticExperiment(forwardStochasticLocalSearch);
        runStochasticExperiment(geneticAlgorithm);
        runStochasticExperiment(forwardMemeticAlgorithm);
    }

    @Override
    public void saveResults() {
        bestFitnessAtTerminationResult.saveResults("second_experiment_best_fitness_at_termination");
        overallBestResponseTimesResult.saveResults("second_experiment_response_times");
        overallBestAllocationResult.saveResults("second_experiment_allocations");
    }

    private void runStochasticExperiment(Optimizer optimizer) {
        String optimizerName = optimizer.getAbbreviation();
        double overallBestFitness = Double.MAX_VALUE;
        Allocation overallBestAllocation = null;
        Result overallBestRunStatistics = new Result();

        List<Double> bestFitnessAtTermination = new ArrayList<>();

        for (int i = 0; i < Parameters.RUNS; i++) {
            logger.info("Starting {}... run {}/{}", optimizerName, i + 1, Parameters.RUNS);

            optimizer.optimize();
            Solution solution = optimizer.getOptimalSolution();
            bestFitnessAtTermination.add(solution.getFitness());

            if (solution.getFitness() < overallBestFitness) {
                overallBestAllocation = solution.getAllocation();
                overallBestRunStatistics = optimizer.getRunStatistics();
            }

            logger.info("{} run {}/{} completed.", optimizerName, i + 1, Parameters.RUNS);
        }

        ResponseTimes overallBestResponseTimes = Simulation.withDefaultConfig().simulate(overallBestAllocation);
        bestFitnessAtTerminationResult.saveColumn(optimizerName, bestFitnessAtTermination);
        overallBestResponseTimesResult.saveColumn("timestamp", overallBestResponseTimes.getTimestamps());
        overallBestResponseTimesResult.saveColumn(optimizerName, overallBestResponseTimes.getResponseTimes());
        overallBestAllocationResult.saveColumn(optimizerName + "_d",
                overallBestAllocation.getDayShiftAllocationSorted());
        overallBestAllocationResult.saveColumn(optimizerName + "_n",
                overallBestAllocation.getNightShiftAllocationSorted());
        overallBestRunStatistics.saveResults(String.format("second_experiment_%s", optimizerName.toLowerCase()));
    }

    public static void main(String[] args) {
        logger.info("Running experiment 2 ...");
        SecondExperiment secondExperiment = new SecondExperiment();
        secondExperiment.run();
        logger.info("Done");

        logger.info("Saving results for experiment 2 ...");
        secondExperiment.saveResults();
        logger.info("Experiment 2 completed successfully.");
    }

}
