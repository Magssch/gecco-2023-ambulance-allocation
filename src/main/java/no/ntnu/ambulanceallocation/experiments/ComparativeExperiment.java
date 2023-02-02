package no.ntnu.ambulanceallocation.experiments;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.ga.Individual;
import no.ntnu.ambulanceallocation.optimization.ma.EvolutionStrategy;
import no.ntnu.ambulanceallocation.optimization.ma.MemeticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
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
        // StochasticLocalSearch forwardStochasticLocalSearch = new StochasticLocalSearch(NeighborhoodFunction.FORWARD);
        // StochasticLocalSearch lazyStochasticLocalSearchA = new StochasticLocalSearch(NeighborhoodFunction.LAZY, 10);
        // StochasticLocalSearch lazyStochasticLocalSearchB = new StochasticLocalSearch(NeighborhoodFunction.LAZY, 30);
        // StochasticLocalSearch lazyStochasticLocalSearchC = new StochasticLocalSearch(NeighborhoodFunction.LAZY, 60);

        MemeticAlgorithm forwardLamarckianMemeticAlgorithm = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN, NeighborhoodFunction.FORWARD);
        MemeticAlgorithm lazyBaldwinianMemeticAlgorithm = new MemeticAlgorithm(EvolutionStrategy.BALDWINIAN, NeighborhoodFunction.LAZY);
        MemeticAlgorithm lazyLamarckianMemeticAlgorithm = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN, NeighborhoodFunction.LAZY);

        optimizers.add(forwardLamarckianMemeticAlgorithm);
        optimizers.add(lazyBaldwinianMemeticAlgorithm);
        optimizers.add(lazyLamarckianMemeticAlgorithm);
    }

    @Override
    public void run() {
        for (Optimizer optimizer : optimizers) {
            runStochasticOptimizer(optimizer);
            logger.info("Operator critic distributions: {}", Individual.operatorCritic.getRelativeImprovements());
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
        logger.info("Running comparative MA experiment ...");
        ComparativeExperiment comparativeExperiment = new ComparativeExperiment();
        comparativeExperiment.getTimeEstimate();
        comparativeExperiment.run();
        logger.info("Done");

        logger.info("Saving results for comparative SLS experiment ...");
        comparativeExperiment.saveResults();
        logger.info("Comparative SLS experiment completed successfully.");
    }

}
