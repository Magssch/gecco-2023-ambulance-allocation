package no.ntnu.ambulanceallocation.experiments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.ga.GeneticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.initializer.PopulationProportionate;
import no.ntnu.ambulanceallocation.simulation.Config;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;
import no.ntnu.ambulanceallocation.utils.Tuple;

public class FourthExperiment implements Experiment {

    private static final Logger logger = LoggerFactory.getLogger(FourthExperiment.class);

    private static final PopulationProportionate populationProportionate = new PopulationProportionate();
    private static final double DAY_TO_NIGHT_AMBULANCE_RATIO = 0.64;

    private static final int minAmbulances = 5;
    private static final int maxAmbulances = 71;

    private final Result averageResponseTimesResult1 = new Result();
    private final Result averageResponseTimesResult2 = new Result();
    private final Map<Integer, Double> averageResponseTimes1 = new HashMap<>();
    private final Map<Integer, Double> averageResponseTimes2 = new HashMap<>();
    private final Map<Tuple<Integer>, Double> averageResponseTimes3 = new HashMap<>();

    @Override
    public void run() {
        logger.info("Running fourth experiment...");

        logger.info("Testing day ambulances between {} and {} with day to night ambulance ratio {}...",
                minAmbulances,
                maxAmbulances, DAY_TO_NIGHT_AMBULANCE_RATIO);
        logger.info("Using model 'PopulationProportionate'");

        IntStream.range(minAmbulances,
                maxAmbulances).parallel().forEach(numDayAmbulances -> {
                    int numNightAmbulances = (int) Math.round(numDayAmbulances *
                            DAY_TO_NIGHT_AMBULANCE_RATIO);
                    logger.info("Running simulation with {} day ambulances and {} night ambulances",
                            numDayAmbulances,
                            numNightAmbulances);
                    Allocation allocation = new Allocation(List.of(
                            populationProportionate.initialize(numDayAmbulances),
                            populationProportionate.initialize(numNightAmbulances)));
                    ResponseTimes results = Simulation.withDefaultConfig().simulate(allocation);
                    averageResponseTimes1.put(numDayAmbulances, results.average());
                });

        logger.info("Using model 'GA'");
        IntStream.range(minAmbulances,
                maxAmbulances).forEach(numDayAmbulances -> {
                    int numNightAmbulances = (int) Math.round(numDayAmbulances *
                            DAY_TO_NIGHT_AMBULANCE_RATIO);
                    logger.info("Running simulation with {} day ambulances and {} night ambulances",
                            numDayAmbulances,
                            numNightAmbulances);
                    Optimizer ga = new GeneticAlgorithm(Config.withNumAmbulances(numDayAmbulances,
                            numNightAmbulances));
                    ga.optimize();
                    Solution solution = ga.getOptimalSolution();
                    ResponseTimes results = Simulation.withDefaultConfig()
                            .simulate(solution.getAllocation());
                    averageResponseTimes2.put(numDayAmbulances, results.average());
                });

        logger.info("Testing all day and night ambulance combinations between {} and {}...", minAmbulances,
                maxAmbulances);
        IntStream.range(minAmbulances,
                maxAmbulances).parallel().forEach(numDayAmbulances -> {
                    IntStream.range(minAmbulances,
                            maxAmbulances).parallel().forEach(numNightAmbulances -> {
                                logger.info("Running simulation with {} day ambulances and {} night ambulances",
                                        numDayAmbulances,
                                        numNightAmbulances);
                                Allocation allocation = new Allocation(
                                        List.of(populationProportionate
                                                .initialize(numDayAmbulances),
                                                populationProportionate
                                                        .initialize(numNightAmbulances)));
                                ResponseTimes results = Simulation.withDefaultConfig()
                                        .simulate(allocation);
                                averageResponseTimes3.put(
                                        new Tuple<>(numDayAmbulances,
                                                numNightAmbulances),
                                        results.average());
                            });
                });
    }

    @Override
    public void saveResults() {
        averageResponseTimesResult1.saveColumn("num_ambulances",
                IntStream.range(minAmbulances, maxAmbulances).boxed().toList());
        averageResponseTimesResult1.saveColumn("PopulationProportionate",
                IntStream.range(minAmbulances,
                        maxAmbulances).mapToObj(averageResponseTimes1::get).toList());
        averageResponseTimesResult1.saveColumn("GA",
                IntStream.range(minAmbulances,
                        maxAmbulances).mapToObj(averageResponseTimes2::get).toList());
        averageResponseTimesResult1
                .saveResults("fourth_experiment_average_response_times_ratio");

        List<Double> responseTimes = new ArrayList<>();
        List<Integer> key1 = new ArrayList<>();
        List<Integer> key2 = new ArrayList<>();
        for (int i = minAmbulances; i < maxAmbulances; i++) {
            for (int j = minAmbulances; j < maxAmbulances; j++) {
                responseTimes.add(averageResponseTimes2.get(new Tuple<>(i, j)));
                key1.add(i);
                key2.add(j);
            }
        }
        averageResponseTimesResult2.saveColumn("num_ambulances_day", key1);
        averageResponseTimesResult2.saveColumn("num_ambulances_night", key2);
        averageResponseTimesResult2.saveColumn("average_response_time", responseTimes);
        averageResponseTimesResult2.saveResults("fourth_experiment_average_response_times_all");
    }

    public static void main(String[] args) {
        logger.info("Running experiment 4 ...");
        FourthExperiment fourthExperiment = new FourthExperiment();
        fourthExperiment.run();
        logger.info("Done");

        logger.info("Saving results for experiment 4 ...");
        fourthExperiment.saveResults();
        logger.info("Experiment 4 completed successfully.");
    }

}
