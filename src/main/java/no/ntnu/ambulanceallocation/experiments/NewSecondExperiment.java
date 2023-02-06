package no.ntnu.ambulanceallocation.experiments;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.ga.GeneticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.initializer.Initializer;
import no.ntnu.ambulanceallocation.optimization.initializer.PopulationProportionate;
import no.ntnu.ambulanceallocation.optimization.ma.EvolutionStrategy;
import no.ntnu.ambulanceallocation.optimization.ma.MemeticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.optimization.sls.StochasticLocalSearch;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;
import no.ntnu.ambulanceallocation.utils.Tuple;

public class NewSecondExperiment extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(NewSecondExperiment.class);

    private Result allocations = new Result();
    private Result responseTimes = new Result();
    private Result runs = new Result();

    private final Map<String, Tuple<LocalDateTime>> simulations = Collections.synchronizedMap(Map.of(
            "quiet", // Week 2
            new Tuple<>(LocalDateTime.of(2018, 6, 9, 0, 0, 0), LocalDateTime.of(2018, 6, 16, 0, 0, 0)),
            "average", // Week 28
            new Tuple<>(LocalDateTime.of(2018, 1, 8, 0, 0, 0), LocalDateTime.of(2018, 1, 15, 0, 0, 0)),
            "busy", // Week 52
            new Tuple<>(LocalDateTime.of(2018, 12, 24, 0, 0, 0), LocalDateTime.of(2018, 12, 31, 0, 0, 0))));

    private final List<Optimizer> optimizers = new ArrayList<>();

    public NewSecondExperiment() {
        StochasticLocalSearch hammingStochasticLocalSearch = new StochasticLocalSearch(
                NeighborhoodFunction.HAMMING);
        StochasticLocalSearch lazyStochasticLocalSearch = new StochasticLocalSearch(NeighborhoodFunction.LAZY);
        GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm();
        MemeticAlgorithm lazyBaldwinianMemeticAlgorithm = new MemeticAlgorithm(EvolutionStrategy.BALDWINIAN,
                NeighborhoodFunction.LAZY);
        MemeticAlgorithm lazyLamarckianMemeticAlgorithm = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN,
                NeighborhoodFunction.LAZY);

        optimizers.add(hammingStochasticLocalSearch);
        optimizers.add(lazyStochasticLocalSearch);
        optimizers.add(geneticAlgorithm);
        optimizers.add(lazyBaldwinianMemeticAlgorithm);
        optimizers.add(lazyLamarckianMemeticAlgorithm);
    }

    @Override
    public void run() {

        simulations
                .keySet()
                .parallelStream()
                .forEach(simulationPeriod -> {
                    logger.info("Running {} simulations...", simulationPeriod);
                    allocations = new Result();
                    responseTimes = new Result();
                    runs = new Result();

                    PopulationProportionate populationProportionate = new PopulationProportionate();
                    runDeterministicInitializer(populationProportionate);

                    for (Optimizer optimizer : optimizers) {
                        runStochasticOptimizer(optimizer, simulationPeriod); // TODO: run with correct config
                        Simulation.saveAllocationResults();
                    }

                    saveResults(simulationPeriod);
                });
    }

    public void saveResults(String simulationPeriod) {
        runs.saveResults(String.format("second_experiment_%s_runs", simulationPeriod));
    }

    @Override
    public void saveResults() {

    }

    private void runDeterministicInitializer(Initializer initializer) {
        String name = initializer.getClass().getSimpleName();

        List<Integer> dayShiftAllocation = initializer.initialize(Parameters.NUMBER_OF_AMBULANCES_DAY);
        List<Integer> nightShiftAllocation = initializer.initialize(Parameters.NUMBER_OF_AMBULANCES_NIGHT);
        ResponseTimes responseTimes = Simulation.simulate(dayShiftAllocation, nightShiftAllocation);

        runs.saveColumn(name, Collections.nCopies(Parameters.RUNS, responseTimes.average()));
    }

    private void runStochasticOptimizer(Optimizer optimizer, String simulationPeriod) {
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
        overallBestRunStatistics
                .saveResults(String.format("second_experiment_%s_%s", simulationPeriod, optimizerName.toLowerCase()));
    }

    private void getTimeEstimate() {
        int extraTime = 3;
        int optimizers = this.optimizers.size();
        int durationInMinutes = Parameters.RUNS * optimizers * Parameters.MAX_RUNNING_TIME / 60 + extraTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String estimatedTimeOfCompletion = LocalDateTime.now().plus(Duration.of(durationInMinutes, ChronoUnit.MINUTES))
                .format(formatter);
        logger.info("Estimated experiment duration: {} minutes.", durationInMinutes);
        logger.info("You can come back at around: {}.", estimatedTimeOfCompletion);
        logger.info("Remember to keep the computer plugged in!");
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
