package no.ntnu.ambulanceallocation.experiments;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.ga.GeneticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.initializer.AllCityCenter;
import no.ntnu.ambulanceallocation.optimization.initializer.Initializer;
import no.ntnu.ambulanceallocation.optimization.initializer.PopulationProportionate;
import no.ntnu.ambulanceallocation.optimization.initializer.Random;
import no.ntnu.ambulanceallocation.optimization.initializer.Uniform;
import no.ntnu.ambulanceallocation.optimization.initializer.UniformRandom;
import no.ntnu.ambulanceallocation.optimization.ma.EvolutionStrategy;
import no.ntnu.ambulanceallocation.optimization.ma.MemeticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.optimization.sls.StochasticLocalSearch;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;
import no.ntnu.ambulanceallocation.utils.Utils;

public class FirstExperiment extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(FirstExperiment.class);

    private final Result allocations = new Result();
    private final Result responseTimes = new Result();
    private final Result runs = new Result();

    private final List<Optimizer> optimizers = new ArrayList<>();

    public FirstExperiment() {
        StochasticLocalSearch hammingStochasticLocalSearch = new StochasticLocalSearch(NeighborhoodFunction.HAMMING);
        StochasticLocalSearch lazyStochasticLocalSearch = new StochasticLocalSearch(NeighborhoodFunction.LAZY);
        GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm();
        MemeticAlgorithm memeticAlgorithmSLS = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN,
                NeighborhoodFunction.LAZY);
        MemeticAlgorithm memeticAlgorithmRobinHood = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN,
                NeighborhoodFunction.LAZY);
        MemeticAlgorithm memeticAlgorithmOperatorCritic = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN,
                NeighborhoodFunction.LAZY);

        optimizers.add(hammingStochasticLocalSearch);
        optimizers.add(lazyStochasticLocalSearch);
        optimizers.add(geneticAlgorithm);
        optimizers.add(memeticAlgorithmSLS);
        optimizers.add(memeticAlgorithmRobinHood);
        optimizers.add(memeticAlgorithmOperatorCritic);
    }

    @Override
    public void run() {
        // Initializer (baselines)
        Random random = new Random();
        AllCityCenter allCityCenter = new AllCityCenter();
        Uniform uniform = new Uniform();
        UniformRandom uniformRandom = new UniformRandom();
        PopulationProportionate populationProportionate = new PopulationProportionate();

        runStochasticInitializer(random);
        runDeterministicInitializer(allCityCenter);
        runDeterministicInitializer(uniform);
        runStochasticInitializer(uniformRandom);
        runDeterministicInitializer(populationProportionate);

        // Optimizers (AI methods)
        for (Optimizer optimizer : optimizers) {
            runStochasticOptimizer(optimizer);
            Simulation.saveAllocationResults();
        }
    }

    @Override
    public void saveResults() {
        allocations.saveResults("first_experiment_allocations");
        responseTimes.saveResults("first_experiment_response_times");
        runs.saveResults("first_experiment_runs");
    }

    private void runDeterministicInitializer(Initializer initializer) {
        String name = initializer.getClass().getSimpleName();
        logger.info("Running {} ...", name);

        List<Integer> dayShiftAllocation = initializer.initialize(Parameters.NUMBER_OF_AMBULANCES_DAY);
        List<Integer> nightShiftAllocation = initializer.initialize(Parameters.NUMBER_OF_AMBULANCES_NIGHT);
        ResponseTimes responseTimes = Simulation.simulate(dayShiftAllocation, nightShiftAllocation);

        allocations.saveColumn(name + "_d", dayShiftAllocation.stream().sorted().toList());
        allocations.saveColumn(name + "_n", nightShiftAllocation.stream().sorted().toList());
        this.responseTimes.saveColumn("timestamp", responseTimes.getTimestamps());
        this.responseTimes.saveColumn("coords", responseTimes.getCoordinates());
        this.responseTimes.saveColumn(name, responseTimes.getResponseTimes());
        runs.saveColumn(name, Collections.nCopies(Parameters.RUNS, responseTimes.average()));

        logger.info("Done");
    }

    private void runStochasticInitializer(Initializer initializer) {
        String name = initializer.getClass().getSimpleName();
        logger.info("Running {} ...", name);

        List<Double> fitness = new ArrayList<>();
        List<Allocation> allocations = new ArrayList<>();

        for (int i = 0; i < Parameters.RUNS; i++) {
            List<Integer> dayShiftAllocation = initializer.initialize(Parameters.NUMBER_OF_AMBULANCES_DAY);
            List<Integer> nightShiftAllocation = initializer.initialize(Parameters.NUMBER_OF_AMBULANCES_NIGHT);
            allocations.add(new Allocation(List.of(dayShiftAllocation, nightShiftAllocation)));
            ResponseTimes responseTimes = Simulation.simulate(dayShiftAllocation, nightShiftAllocation);
            fitness.add(responseTimes.average());
        }

        int medianIndex = Utils.medianIndexOf(fitness);
        Allocation medianAllocation = allocations.get(medianIndex);
        ResponseTimes medianResponseTimes = Simulation.withDefaultConfig().simulate(medianAllocation);

        this.allocations.saveColumn(name + "_d", medianAllocation.getDayShiftAllocationSorted());
        this.allocations.saveColumn(name + "_n", medianAllocation.getNightShiftAllocationSorted());
        responseTimes.saveColumn("timestamp", medianResponseTimes.getTimestamps());
        responseTimes.saveColumn("coords", medianResponseTimes.getCoordinates());
        responseTimes.saveColumn(name, medianResponseTimes.getResponseTimes());
        runs.saveColumn(name, fitness);

        logger.info("Done");
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
        overallBestRunStatistics.saveResults(String.format("first_experiment_%s", optimizerName.toLowerCase()));
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
        logger.info("Running experiment 1 ...");
        FirstExperiment firstExperiment = new FirstExperiment();
        firstExperiment.getTimeEstimate();
        firstExperiment.run();
        logger.info("Done");

        logger.info("Saving results for experiment 1 ...");
        firstExperiment.saveResults();
        logger.info("Experiment 1 completed successfully.");
    }

}
