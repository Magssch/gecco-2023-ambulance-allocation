package no.ntnu.ambulanceallocation.experiments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;

public class NewThirdExperiment extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(NewThirdExperiment.class);

    private final Result responseTimeResult = new Result();
    private static final PopulationProportionate populationProportionate = new PopulationProportionate();
    private final Map<String, List<Double>> averageResponseTimes = new HashMap<>();

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
            0.65,
            // 0.7,
            // 0.75,
            // 0.8,
            // 0.85,
            // 0.9,
            // 0.95,
            1.0);

    @Override
    public void run() {
        final int totalNumAmbulances = Parameters.NUMBER_OF_AMBULANCES_DAY + Parameters.NUMBER_OF_AMBULANCES_NIGHT;

        ratioList.forEach(ambulanceRatio -> {
            int numDayAmbulances = (int) Math.round(totalNumAmbulances * ambulanceRatio);
            int numNightAmbulances = (int) Math
                    .round(totalNumAmbulances * (1 - ambulanceRatio));
            logger.info("Running optimizations with {} day ambulances and {} night ambulances",
                    numDayAmbulances,
                    numNightAmbulances);

            logger.info("Testing model 'Population Proportionate'");
            Allocation allocation = new Allocation(List.of(
                    populationProportionate.initialize(numDayAmbulances),
                    populationProportionate.initialize(numNightAmbulances)));
            ResponseTimes results = Simulation
                    .withConfig(Config.withNumAmbulances(numDayAmbulances, numNightAmbulances)).simulate(allocation);
            averageResponseTimes.putIfAbsent("PopProp", new ArrayList<>());
            averageResponseTimes.get("PopProp").add(results.average());

            logger.info("Testing model 'GA'");
            Optimizer ga = new GeneticAlgorithm(Config.withNumAmbulances(numDayAmbulances, numNightAmbulances));
            runStochasticOptimizer(ga);

            logger.info("Testing model 'MA'");
            Optimizer ma = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN, NeighborhoodFunction.FORWARD,
                    Parameters.LAZY_NEIGHBOURHOOD_SIZE,
                    Config.withNumAmbulances(numDayAmbulances, numNightAmbulances));
            runStochasticOptimizer(ma);

        });
    }

    private void runStochasticOptimizer(Optimizer optimizer) {
        logger.info("Testing model 'GA'");
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

        ResponseTimes overallBestResponseTimes = Simulation.withConfig(optimizer.getConfig())
                .simulate(overallBestAllocation);
        averageResponseTimes.putIfAbsent(optimizerName, new ArrayList<>());
        averageResponseTimes.get(optimizerName).add(overallBestResponseTimes.average());
    }

    @Override
    public void saveResults() {
        responseTimeResult.saveColumn("ambulanceRatio", ratioList);
        responseTimeResult.saveColumn("PopulationProportionate", averageResponseTimes.get("PopProp"));
        responseTimeResult.saveColumn("GA", averageResponseTimes.get("GA"));
        responseTimeResult.saveColumn("MA", averageResponseTimes.get("MA"));
        responseTimeResult.saveResults("new_third_experiment_response_times");
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
