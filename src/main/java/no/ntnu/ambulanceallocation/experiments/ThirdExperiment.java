package no.ntnu.ambulanceallocation.experiments;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;
import no.ntnu.ambulanceallocation.utils.Tuple;

public class ThirdExperiment implements Experiment {

    private static final Logger logger = LoggerFactory.getLogger(ThirdExperiment.class);

    private final Map<String, Allocation> allocations = Collections.synchronizedMap(Map.of(
            "Random",
            new Allocation(List.of(
                    List.of(1, 13, 2, 9, 18, 1, 14, 17, 5, 6, 1, 10, 13, 7, 11, 16, 8, 13, 12, 4,
                            18, 12, 4, 13, 15, 8,
                            11, 0, 7, 18, 17, 5, 3, 13, 0, 11, 6, 14, 13, 10, 1, 15, 11, 3,
                            5),
                    List.of(0, 18, 13, 0, 10, 14, 13, 15, 8, 4, 8, 1, 10, 7, 16, 14, 0, 6, 9, 18, 5,
                            6, 3, 18, 2, 4, 15, 7, 8))),
            "AllCityCenter",
            new Allocation(List.of(
                    List.of(7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
                            7, 7, 7, 7, 7, 7, 7,
                            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7),
                    List.of(7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
                            7, 7, 7, 7, 7))),
            "Uniform",
            new Allocation(List.of(
                    List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 0, 1,
                            2, 3, 4, 5, 6, 7, 8,
                            9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 0, 1, 2, 3, 4, 5, 6),
                    List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 0, 1,
                            2, 3, 4, 5, 6, 7, 8, 9))),
            "UniformRandom",
            new Allocation(List.of(
                    List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 0, 1,
                            2, 3, 4, 5, 6, 7, 8,
                            9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 4, 15, 6, 12, 16, 1, 8),
                    List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 16,
                            15, 8, 7, 10, 5, 3, 0, 13, 17))),
            "PopulationProportionate",
            new Allocation(List.of(
                    List.of(0, 1, 2, 3, 4, 4, 4, 5, 6, 6, 6, 7, 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 10,
                            10, 11, 11, 11, 12,
                            12, 13, 13, 14, 14, 14, 15, 15, 15, 16, 16, 16, 17, 17, 18, 18,
                            18),
                    List.of(0, 2, 4, 4, 6, 6, 7, 7, 7, 8, 8, 8, 9, 10, 11, 11, 12, 13, 14, 14, 15,
                            15, 16, 16, 17, 18,
                            18, 1, 13))),
            "SLS",
            new Allocation(List.of(
                    List.of(0, 1, 2, 2, 2, 3, 3, 4, 4, 4, 4, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 8, 8, 9, 9, 9, 10, 10,
                            11, 12, 12, 12, 13, 13, 14, 14, 14, 15, 15, 15, 15, 16, 17),
                    List.of(0, 0, 1, 2, 4, 6, 6, 7, 7, 7, 7, 7, 8, 9, 9, 10, 10, 10, 11, 12, 14, 14, 15, 16, 16, 17, 17,
                            18))),
            "GA (1 week)",
            new Allocation(List.of(
                    List.of(0, 0, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 6, 7, 7, 7, 7, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 11,
                            12, 12, 13, 14, 14, 14, 15, 15, 15, 15, 16, 16, 16, 17, 17, 18, 18),
                    List.of(0, 0, 1, 2, 3, 4, 4, 6, 7, 7, 8, 8, 8, 8, 9, 10, 10, 11, 12, 13, 14, 15, 15, 15, 16, 16, 17,
                            17, 18))),
            "MA",
            new Allocation(List.of(
                    List.of(0, 0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 11, 11,
                            12, 12, 13, 13, 14, 15, 15, 15, 15, 16, 16, 16, 17, 17, 18, 18),
                    List.of(0, 0, 1, 2, 3, 4, 4, 5, 5, 6, 6, 7, 7, 7, 8, 8, 9, 10, 10, 11, 11, 13, 13, 14, 15, 15, 17,
                            17, 18))),
            "GA (1 year)",
            new Allocation(List.of(
                    List.of(0, 0, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 9, 9,
                            10, 10, 10, 11, 11, 12, 12, 13, 14, 14, 15, 15, 15, 15, 16, 16,
                            16, 17, 17, 18, 18),
                    List.of(0, 1, 2, 2, 3, 4, 4, 4, 6, 6, 7, 7, 7, 8, 8, 9, 9, 10, 10, 11, 12, 12,
                            14, 14, 15, 16, 16, 17, 18)))

    ));

    private final Map<String, Tuple<LocalDateTime>> simulations = Collections.synchronizedMap(Map.of(
            "one_week",
            new Tuple<>(LocalDateTime.of(2018, 8, 6, 0, 0, 0), LocalDateTime.of(2018, 8, 13, 0, 0, 0)),
            "two_weeks",
            new Tuple<>(LocalDateTime.of(2018, 8, 6, 0, 0, 0), LocalDateTime.of(2018, 8, 20, 0, 0, 0)),
            "one_month",
            new Tuple<>(LocalDateTime.of(2018, 8, 1, 0, 0, 0), LocalDateTime.of(2018, 9, 1, 0, 0, 0)),
            "three_months",
            new Tuple<>(LocalDateTime.of(2018, 7, 1, 0, 0, 0), LocalDateTime.of(2018, 10, 1, 0, 0, 0)),
            "one_year",
            new Tuple<>(LocalDateTime.of(2018, 1, 1, 0, 0, 0), LocalDateTime.of(2019, 1, 1, 0, 0, 0))));

    @Override
    public void run() {
        logger.info("Running third experiment...");

        simulations.keySet().parallelStream().forEach(simulationPeriod -> {
            logger.info("Running {} simulations...", simulationPeriod);
            Result periodResponseTimeResults = new Result();
            allocations.keySet().parallelStream().forEach(allocationName -> {
                Allocation allocation = allocations.get(allocationName);
                ResponseTimes results = Simulation
                        .withinPeriod(simulations.get(simulationPeriod).first(),
                                simulations.get(simulationPeriod).second())
                        .simulate(allocation);
                logger.info("{} simulation finished for {}", simulationPeriod, allocationName);
                periodResponseTimeResults.saveColumn(allocationName, results.values());
            });
            periodResponseTimeResults.saveResults("third_experiment_response_times_" + simulationPeriod);
        });
    }

    @Override
    public void saveResults() {
        // bestFitnessAtTerminationResult.saveResults("second_experiment_best_fitness_at_termination");
        // overallBestResponseTimesResult.saveResults("third_experiment_response_times");
        // overallBestAllocationResult.saveResults("second_experiment_allocations");
    }

    public static void main(String[] args) {
        logger.info("Running experiment 3 ...");
        ThirdExperiment thirdExperiment = new ThirdExperiment();
        thirdExperiment.run();
        logger.info("Done");

        logger.info("Saving results for experiment 3 ...");
        thirdExperiment.saveResults();
        logger.info("Experiment 3 completed successfully.");
    }

}
