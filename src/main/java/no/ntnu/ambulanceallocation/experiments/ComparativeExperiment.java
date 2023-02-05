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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class ComparativeExperiment extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(SecondExperiment.class);

    private final Result allocations = new Result();
    private final Result responseTimes = new Result();
    private final Result runs = new Result();
    private final List<Optimizer> optimizers = new ArrayList<>();

    public ComparativeExperiment() {
        // Setup
        StochasticLocalSearch forwardStochasticLocalSearch = new StochasticLocalSearch(NeighborhoodFunction.FORWARD);
        StochasticLocalSearch hammingStochasticLocalSearch = new StochasticLocalSearch(NeighborhoodFunction.HAMMING);
        StochasticLocalSearch lazyStochasticLocalSearch = new StochasticLocalSearch(NeighborhoodFunction.LAZY, 40);

        optimizers.add(forwardStochasticLocalSearch);
        optimizers.add(hammingStochasticLocalSearch);
        optimizers.add(lazyStochasticLocalSearch);
    }

    @Override
    public void run() {
        for (Optimizer optimizer : optimizers) {
            runStochasticOptimizer(optimizer);
        }
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

    private void getTimeEstimate() {
        int setupTime = 1;
        int optimizers = this.optimizers.size();
        int durationInMinutes = Parameters.RUNS * optimizers * Parameters.MAX_RUNNING_TIME / 60 + setupTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String estimatedTimeOfCompletion = LocalDateTime.now().plus(Duration.of(durationInMinutes, ChronoUnit.MINUTES)).format(formatter);
        logger.info("Estimated experiment duration: {} minutes.", durationInMinutes);
        logger.info("You can come back at around: {}.", estimatedTimeOfCompletion);
        logger.info("Remember to keep the computer plugged in!");
    }

    public static void main(String[] args) {
        logger.info("Running comparative SLS experiment ...");
        ComparativeExperiment comparativeExperiment = new ComparativeExperiment();
        comparativeExperiment.getTimeEstimate();
        comparativeExperiment.run();
        logger.info("Done");

        logger.info("Saving results for comparative SLS experiment ...");
        comparativeExperiment.saveResults();
        logger.info("Comparative SLS experiment completed successfully.");
    }

}
