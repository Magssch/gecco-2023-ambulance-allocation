package no.ntnu.ambulanceallocation.experiments;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.ntnu.ambulanceallocation.Parameters;
import no.ntnu.ambulanceallocation.optimization.Optimizer;
import no.ntnu.ambulanceallocation.optimization.Solution;
import no.ntnu.ambulanceallocation.optimization.ga.GeneticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.ma.EvolutionStrategy;
import no.ntnu.ambulanceallocation.optimization.ma.MemeticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.optimization.sls.StochasticLocalSearch;

public class ParameterSearch extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(ParameterSearch.class);

    private final Result bestFitnessAtTerminationResult = new Result();
    private final Result overallBestResponseTimesResult = new Result();
    private final Result overallBestAllocationResult = new Result();

    private final List<Double> bestFitnessAtTermination = new ArrayList<>();

    @Override
    public void run() {
        // Setup
        Optimizer optimizer = switch (parameters.get("-optimizer")) {
            case "ga" -> new GeneticAlgorithm();
            case "ma" -> new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN,
                    NeighborhoodFunction.FORWARD);
            case "sls" -> new StochasticLocalSearch(NeighborhoodFunction.FORWARD);
            default -> throw new IllegalArgumentException("Unexpected value: " +
                    parameters.get("optimizer"));
        };

        Parameters.RUNS = Integer.parseInt(parameters.get("-numberOfRuns"));
        Parameters.MAX_RUNNING_TIME = Integer.parseInt(parameters.get("-runningTime"));

        for (String parameterConfig : parameters.get("-paramList").split(";")) {
            String[] params = parameterConfig.split(",");

            Parameters.POPULATIONPROPORTIONATE_SEEDING_SIZE = Integer.parseInt(params[0]);
            Parameters.POPULATION_SIZE = Integer.parseInt(params[1]);
            Parameters.ELITE_SIZE = Integer.parseInt(params[2]);
            Parameters.TOURNAMENT_SIZE = Integer.parseInt(params[3]);
            Parameters.CROSSOVER_PROBABILITY = Double.parseDouble(params[4]);
            Parameters.MUTATION_PROBABILITY = Double.parseDouble(params[5]);
            Parameters.IMPROVE_PROBABILITY = Double.parseDouble(params[6]);
            // System.out.println("Running " + optimizer.getAbbreviation() + " with
            // parameters: " + parameters.toString());
            runStochasticExperiment(optimizer);
        }
        System.out.print(String.join(",", bestFitnessAtTermination.stream().map(String::valueOf).toList()));
        System.exit(0);
    }

    @Override
    public void saveResults() {
        bestFitnessAtTerminationResult.saveResults("second_experiment_best_fitness_at_termination");
        overallBestResponseTimesResult.saveResults("second_experiment_response_times");
        overallBestAllocationResult.saveResults("second_experiment_allocations");
    }

    private void runStochasticExperiment(Optimizer optimizer) {
        String optimizerName = optimizer.getAbbreviation();

        for (int i = 0; i < Parameters.RUNS; i++) {
            logger.info("Starting {}... run {}/{}", optimizerName, i + 1,
                    Parameters.RUNS);
            optimizer.optimize();
            Solution solution = optimizer.getOptimalSolution();
            bestFitnessAtTermination.add(solution.getFitness());
        }
    }

    public static void main(String[] args) {
        setParameterValues(args);
        logger.info("Running parameter search optimization ...");
        ParameterSearch parameterSearch = new ParameterSearch();
        parameterSearch.run();
        logger.info("Optimization completed successfully.");
    }

}
