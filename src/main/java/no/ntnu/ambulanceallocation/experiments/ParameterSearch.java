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
import no.ntnu.ambulanceallocation.optimization.ma.ImproveOperator;
import no.ntnu.ambulanceallocation.optimization.ma.MemeticAlgorithm;
import no.ntnu.ambulanceallocation.optimization.sls.NeighborhoodFunction;
import no.ntnu.ambulanceallocation.optimization.sls.StochasticLocalSearch;

public class ParameterSearch extends Experiment {

    private static final Logger logger = LoggerFactory.getLogger(ParameterSearch.class);

    private final Optimizer optimizer;
    private final List<Double> bestFitnessAtTermination = new ArrayList<>();

    public ParameterSearch() {
        optimizer = switch (parameters.get("-optimizer")) {
            case "ga" -> new GeneticAlgorithm();
            case "ma_operatorcritic" -> new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN,
                    ImproveOperator.OPERATORCRITIC, NeighborhoodFunction.FORWARD);
            case "ma_sls" ->
                new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN, ImproveOperator.SLS, NeighborhoodFunction.FORWARD);
            case "ma_robinhood" -> new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN, ImproveOperator.ROBINHOOD,
                    NeighborhoodFunction.FORWARD);
            case "sls" -> new StochasticLocalSearch(NeighborhoodFunction.LAZY);
            default -> throw new IllegalArgumentException("Unexpected value: " + parameters.get("optimizer"));
        };

        Parameters.RUNS = Integer.parseInt(parameters.get("-numberOfRuns"));
        Parameters.MAX_RUNNING_TIME = Integer.parseInt(parameters.get("-runningTime"));
    }

    @Override
    public void run() {
        for (String parameterConfig : parameters.get("-paramList").split(";")) {
            String[] params = parameterConfig.split(",");

            Parameters.POPULATION_PROPORTIONATE_SEEDING_SIZE = Integer.parseInt(params[0]);
            Parameters.POPULATION_SIZE = Integer.parseInt(params[1]);
            Parameters.ELITE_SIZE = Integer.parseInt(params[2]);
            Parameters.TOURNAMENT_SIZE = Integer.parseInt(params[3]);
            Parameters.CROSSOVER_PROBABILITY = Double.parseDouble(params[4]);
            Parameters.MUTATION_PROBABILITY = Double.parseDouble(params[5]);

            Parameters.IMPROVE_PROBABILITY = Double.parseDouble(params[6]);
            Parameters.USE_SWAP_MUTATION = Boolean.parseBoolean(params[7]);

            Parameters.RESTART_PROBABILITY = Double.parseDouble(params[8]);
            Parameters.NOISE_PROBABILITY = Double.parseDouble(params[9]);
            Parameters.LAZY_NEIGHBOURHOOD_SIZE = Integer.parseInt(params[10]);

            System.err.println("Running with parameters: " + parameterConfig);

            runStochasticOptimizer(optimizer);
        }
        saveResults();
    }

    @Override
    public void saveResults() {
        System.out.print(String.join(",", bestFitnessAtTermination.stream().map(String::valueOf).toList()));
        System.exit(0);
    }

    private void runStochasticOptimizer(Optimizer optimizer) {
        String optimizerName = optimizer.getAbbreviation();
        double overallBestFitness = Double.POSITIVE_INFINITY;

        for (int i = 0; i < Parameters.RUNS; i++) {
            logger.info("Starting {}... run {}/{}", optimizerName, i + 1, Parameters.RUNS);
            optimizer.optimize();
            Solution solution = optimizer.getOptimalSolution();
            overallBestFitness = Math.min(overallBestFitness, solution.getFitness());
        }
        bestFitnessAtTermination.add(overallBestFitness);
    }

    public static void main(String[] args) {
        setParameterValues(args);
        logger.info("Running parameter search optimization ...");
        ParameterSearch parameterSearch = new ParameterSearch();
        parameterSearch.run();
        logger.info("Parameter search completed successfully.");
    }

}
