package no.ntnu.ambulanceallocation.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Allocation;
import no.ntnu.ambulanceallocation.optimization.initializer.AllCityCenter;
import no.ntnu.ambulanceallocation.optimization.initializer.Initializer;
import no.ntnu.ambulanceallocation.optimization.initializer.PopulationProportionate;
import no.ntnu.ambulanceallocation.optimization.initializer.Random;
import no.ntnu.ambulanceallocation.optimization.initializer.Uniform;
import no.ntnu.ambulanceallocation.optimization.initializer.UniformRandom;
import no.ntnu.ambulanceallocation.simulation.ResponseTimes;
import no.ntnu.ambulanceallocation.simulation.Simulation;
import no.ntnu.ambulanceallocation.utils.Utils;

public class FirstExperiment extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(FirstExperiment.class);

    private final Result allocationResult = new Result();
    private final Result responseTimeResult = new Result();
    private final Result stochasticResponseTimeResult = new Result();

    @Override
    public void run() {
        // Setup
        Random random = new Random();
        AllCityCenter allCityCenter = new AllCityCenter();
        Uniform uniform = new Uniform();
        UniformRandom uniformRandom = new UniformRandom();
        PopulationProportionate populationProportionate = new PopulationProportionate();

        // Partial experiments
        runStochasticExperiment(random);
        runDeterministicExperiment(allCityCenter);
        runDeterministicExperiment(uniform);
        runStochasticExperiment(uniformRandom);
        runDeterministicExperiment(populationProportionate);
    }

    @Override
    public void saveResults() {
        responseTimeResult.saveResults("first_experiment_response_times");
        allocationResult.saveResults("first_experiment_allocations");
        stochasticResponseTimeResult.saveResults("first_experiment_distribution");
    }

    private void runDeterministicExperiment(Initializer initializer) {
        String name = initializer.getClass().getSimpleName();
        logger.info("Running {} ...", name);

        List<Integer> dayShiftAllocation = initializer.initialize(Parameters.NUMBER_OF_AMBULANCES_DAY);
        List<Integer> nightShiftAllocation = initializer.initialize(Parameters.NUMBER_OF_AMBULANCES_NIGHT);
        ResponseTimes responseTimes = Simulation.simulate(dayShiftAllocation, nightShiftAllocation);

        allocationResult.saveColumn(name + "_d", dayShiftAllocation.stream().sorted().toList());
        allocationResult.saveColumn(name + "_n", nightShiftAllocation.stream().sorted().toList());
        responseTimeResult.saveColumn("timestamp", responseTimes.getTimestamps());
        responseTimeResult.saveColumn("coords", responseTimes.getCoordinates());
        responseTimeResult.saveColumn(name, responseTimes.getResponseTimes());
        stochasticResponseTimeResult.saveColumn(name, Collections.nCopies(Parameters.RUNS, responseTimes.average()));

        logger.info("Done");
    }

    private void runStochasticExperiment(Initializer initializer) {
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

        allocationResult.saveColumn(name + "_d", medianAllocation.getDayShiftAllocationSorted());
        allocationResult.saveColumn(name + "_n", medianAllocation.getNightShiftAllocationSorted());
        responseTimeResult.saveColumn("timestamp", medianResponseTimes.getTimestamps());
        responseTimeResult.saveColumn("coords", medianResponseTimes.getCoordinates());
        responseTimeResult.saveColumn(name, medianResponseTimes.getResponseTimes());
        stochasticResponseTimeResult.saveColumn(name, fitness);

        logger.info("Done");
    }

    public static void main(String[] args) {
        setParameterValues(args);
        logger.info("Running experiment 1 ...");
        FirstExperiment firstExperiment = new FirstExperiment();
        firstExperiment.run();
        logger.info("Done");

        logger.info("Saving results for experiment 1 ...");
        firstExperiment.saveResults();
        logger.info("Experiment 1 completed successfully.");
    }

}
