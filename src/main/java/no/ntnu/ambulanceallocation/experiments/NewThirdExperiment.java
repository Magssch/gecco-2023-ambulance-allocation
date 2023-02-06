package no.ntnu.ambulanceallocation.experiments;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.ga.GeneticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.initializer.PopulationProportionate;
import no.ntnu.ambulanceallocation.optimization.ma.EvolutionStrategy;
import no.ntnu.ambulanceallocation.optimization.ma.MemeticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;

public class NewThirdExperiment extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(NewThirdExperiment.class);

    private final Result bestFitnessResult = new Result();
    private final Result responseTimes = new Result();
    private final Result runs = new Result();
    private static final PopulationProportionate populationProportionate = new PopulationProportionate();

    private final Map<String, List<Double>> averageResponseTimes = new HashMap<>();

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
            0.55
    // 0.6
    // 0.65,
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
        bestFitnessResult.saveColumn("ratio", ratioList);

        logger.info("Testing model 'Population Proportionate'");
        runPopulationProportionate();

        logger.info("Testing model 'GA'");
        Optimizer ga = new GeneticAlgorithm();
        runStochasticOptimizer(ga, "GA");
        Simulation.saveAllocationResults();

        logger.info("Testing model 'MA_lazy'");
        Optimizer ma = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN, NeighborhoodFunction.LAZY,
                Parameters.LAZY_NEIGHBOURHOOD_SIZE);
        runStochasticOptimizer(ma, "MA_lazy");
        Simulation.saveAllocationResults();
    }

    private void runPopulationProportionate() {
        logger.info("Testing model '{}'", "PopulationProportionate");

        final int totalNumAmbulances = Parameters.NUMBER_OF_AMBULANCES_DAY + Parameters.NUMBER_OF_AMBULANCES_NIGHT;
        List<Double> fitness = new ArrayList<>();

        List<Double> overallBestResponseTimeRatios = new ArrayList<>();
        List<LocalDateTime> overallBestResponseTimeTimestamps = new ArrayList<>();
        List<Integer> overallBestResponseTimeResponseTimes = new ArrayList<>();

        for (double ambulanceRatio : ratioList) {
            int numDayAmbulances = (int) Math.round(totalNumAmbulances * ambulanceRatio);
            int numNightAmbulances = (int) Math
                    .round(totalNumAmbulances * (1 - ambulanceRatio));
            Parameters.NUMBER_OF_AMBULANCES_DAY = numDayAmbulances;
            Parameters.NUMBER_OF_AMBULANCES_NIGHT = numNightAmbulances;

            logger.info("Running PopulationProportionate with {} day ambulances and {} night ambulances",
                    numDayAmbulances,
                    numNightAmbulances);

            Allocation allocation = new Allocation(List.of(
                    populationProportionate.initialize(numDayAmbulances),
                    populationProportionate.initialize(numNightAmbulances)));
            ResponseTimes results = Simulation.withDefaultConfig().simulate(allocation);

            averageResponseTimes.putIfAbsent("PopProp", new ArrayList<>());
            averageResponseTimes.get("PopProp").add(results.average());
            overallBestResponseTimeRatios.addAll(Collections.nCopies(results.getTimestamps().size(), ambulanceRatio));
            fitness.addAll(Collections.nCopies(Parameters.RUNS, results.average()));
            overallBestResponseTimeTimestamps.addAll(results.getTimestamps());
            overallBestResponseTimeResponseTimes.addAll(results.getResponseTimes());
        }

        runs.saveColumn("PopProp", fitness);
        responseTimes.saveColumn("ratio", overallBestResponseTimeRatios);
        responseTimes.saveColumn("timestamp", overallBestResponseTimeTimestamps);
        responseTimes.saveColumn("PopProp", overallBestResponseTimeResponseTimes);
    }

    private void runStochasticOptimizer(Optimizer optimizer, String name) {
        logger.info("Testing model '{}'", name);

        final int totalNumAmbulances = Parameters.NUMBER_OF_AMBULANCES_DAY + Parameters.NUMBER_OF_AMBULANCES_NIGHT;

        List<Double> bestFitnessAtTermination = new ArrayList<>();
        List<Double> overallBestResponseTimeRatios = new ArrayList<>();
        List<LocalDateTime> overallBestResponseTimeTimestamps = new ArrayList<>();
        List<Integer> overallBestResponseTimeResponseTimes = new ArrayList<>();

        for (double ambulanceRatio : ratioList) {
            int numDayAmbulances = (int) Math.round(totalNumAmbulances * ambulanceRatio);
            int numNightAmbulances = (int) Math
                    .round(totalNumAmbulances * (1 - ambulanceRatio));
            Parameters.NUMBER_OF_AMBULANCES_DAY = numDayAmbulances;
            Parameters.NUMBER_OF_AMBULANCES_NIGHT = numNightAmbulances;

            logger.info("Running optimizations with {} day ambulances and {} night ambulances",
                    numDayAmbulances,
                    numNightAmbulances);

            double overallBestFitness = Double.POSITIVE_INFINITY;

            for (int i = 0; i < Parameters.RUNS; i++) {
                logger.info("Starting {}... run {}/{}", name, i + 1, Parameters.RUNS);

                optimizer.optimize();
                Solution solution = optimizer.getOptimalSolution();
                bestFitnessAtTermination.add(solution.getFitness());

                if (solution.getFitness() < overallBestFitness) {
                    overallBestAllocation = solution.getAllocation();
                }

                logger.info("{} run {}/{} completed.", name, i + 1, Parameters.RUNS);
            }

            ResponseTimes overallBestResponseTimes = Simulation.withConfig(optimizer.getConfig())
                    .simulate(overallBestAllocation);
            averageResponseTimes.putIfAbsent(name, new ArrayList<>());
            averageResponseTimes.get(name).add(overallBestResponseTimes.average());
            overallBestResponseTimeRatios
                    .addAll(Collections.nCopies(overallBestResponseTimes.getTimestamps().size(), ambulanceRatio));
            overallBestResponseTimeTimestamps.addAll(overallBestResponseTimes.getTimestamps());
            overallBestResponseTimeResponseTimes.addAll(overallBestResponseTimes.getResponseTimes());
        }

        runs.saveColumn(name, bestFitnessAtTermination);
        responseTimes.saveColumn("timestamp", overallBestResponseTimeTimestamps);
        responseTimes.saveColumn(name, overallBestResponseTimeResponseTimes);
    }

    @Override
    public void saveResults() {
        bestFitnessResult.saveColumn("PopulationProportionate", averageResponseTimes.get("PopProp"));
        bestFitnessResult.saveColumn("GA", averageResponseTimes.get("GA"));
        bestFitnessResult.saveColumn("MA_lazy", averageResponseTimes.get("MA_lazy"));
        bestFitnessResult.saveResults("new_third_experiment_best_fitness");
        responseTimes.saveResults("new_third_experiment_response_times");
        runs.saveResults("new_third_experiment_runs");
    }

    public static void main(String[] args) {
        logger.info("Running new experiment 3 ...");
        NewThirdExperiment newThirdExperiment = new NewThirdExperiment();
        newThirdExperiment.run();
        logger.info("Done");

        logger.info("Saving results for experiment 3 ...");
        newThirdExperiment.saveResults();
        logger.info("Experiment 3 completed successfully.");
    }

}
