package no.ntnu.ambulanceallocation.experiments;

import java.util.List;
import java.util.stream.IntStream;

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

    private static final PopulationProportionate populationProportionate = new PopulationProportionate();

    @Override
    public void run() {
        logger.info("Running new third experiment...");
        final int totalNumAmbulances = Parameters.NUMBER_OF_AMBULANCES_DAY + Parameters.NUMBER_OF_AMBULANCES_NIGHT;

        IntStream.range(1, 10).parallel().forEach(ambulanceRatio -> {

            int numDayAmbulances = (int) Math.round(totalNumAmbulances * (ambulanceRatio / 10));
            int numNightAmbulances = (int) Math
                    .round(Parameters.NUMBER_OF_AMBULANCES_NIGHT * ((1 - ambulanceRatio) / 10));
            logger.info("Running optimizations with {} day ambulances and {} night ambulances",
                    numDayAmbulances,
                    numNightAmbulances);

            logger.info("Testing model 'Population Proportionate'");
            Allocation allocation = new Allocation(List.of(
                    populationProportionate.initialize(numDayAmbulances),
                    populationProportionate.initialize(numNightAmbulances)));
            ResponseTimes results = Simulation.withDefaultConfig().simulate(allocation);

            logger.info("Testing model 'GA'");
            Optimizer ga = new GeneticAlgorithm(Config.withNumAmbulances(numDayAmbulances, numNightAmbulances));
            ga.optimize();
            Solution gaSolution = ga.getOptimalSolution();
            results = Simulation.withDefaultConfig().simulate(gaSolution.getAllocation());

            logger.info("Testing model 'MA'");
            Optimizer ma = new MemeticAlgorithm(EvolutionStrategy.LAMARCKIAN, NeighborhoodFunction.FORWARD,
                    Config.withNumAmbulances(numDayAmbulances, numNightAmbulances));
            ga.optimize();
            Solution maSolution = ma.getOptimalSolution();
            results = Simulation.withDefaultConfig().simulate(maSolution.getAllocation());

        });
    }

    @Override
    public void saveResults() {

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
