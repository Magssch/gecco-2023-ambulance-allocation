package no.ntnu.ambulanceallocation.experiments;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.optimization.sls.StochasticLocalSearch;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class ComparativeExperiment extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(SecondExperiment.class);

    private final Result allocations = new Result();
    private final Result responseTimes = new Result();
    private final Result runs = new Result();

    @Override
    public void run() {
        // Setup
        StochasticLocalSearch forwardStochasticLocalSearch = new StochasticLocalSearch(NeighborhoodFunction.FORWARD);
        StochasticLocalSearch hammingStochasticLocalSearch = new StochasticLocalSearch(NeighborhoodFunction.HAMMING);
        StochasticLocalSearch lazyStochasticLocalSearchA = new StochasticLocalSearch(NeighborhoodFunction.LAZY, 5);
        StochasticLocalSearch lazyStochasticLocalSearchB = new StochasticLocalSearch(NeighborhoodFunction.LAZY, 10);
        StochasticLocalSearch lazyStochasticLocalSearchC = new StochasticLocalSearch(NeighborhoodFunction.LAZY, 15);


        // Partial experiments
        runStochasticOptimizer(forwardStochasticLocalSearch);
        runStochasticOptimizer(hammingStochasticLocalSearch);
        runStochasticOptimizer(lazyStochasticLocalSearchA);
        runStochasticOptimizer(lazyStochasticLocalSearchB);
        runStochasticOptimizer(lazyStochasticLocalSearchC);

    }

    @Override
    public void saveResults() {
        allocations.saveResults("comparative_experiment_allocations");
        responseTimes.saveResults("comparative_experiment_response_times");
        runs.saveResults("comparative_experiment_runs");
    }

    private void runStochasticOptimizer(Optimizer optimizer) {
        String optimizerName = optimizer.getAbbreviation();
        double overallBestFitness = Double.POSITIVE_INFINITY;
        Allocation overallBestAllocation = new Allocation();
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
        runs.saveColumn(optimizerName, bestFitnessAtTermination);
        responseTimes.saveColumn("timestamp", overallBestResponseTimes.getTimestamps());
        responseTimes.saveColumn("coords", overallBestResponseTimes.getCoordinates());
        responseTimes.saveColumn(optimizerName, overallBestResponseTimes.getResponseTimes());
        allocations.saveColumn(optimizerName + "_d", overallBestAllocation.getDayShiftAllocationSorted());
        allocations.saveColumn(optimizerName + "_n", overallBestAllocation.getNightShiftAllocationSorted());
        overallBestRunStatistics.saveResults(String.format("comparative_experiment_%s", optimizerName.toLowerCase()));
    }

}
