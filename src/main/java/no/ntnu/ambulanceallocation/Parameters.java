package no.ntnu.ambulanceallocation;

import java.time.LocalDateTime;
import java.time.LocalTime;

import no.ntnu.ambulanceallocation.optimization.initializer.Initializer;
import no.ntnu.ambulanceallocation.optimization.initializer.Random;
import no.ntnu.ambulanceallocation.optimization.ma.ImproveOperator;
import no.ntnu.ambulanceallocation.simulation.DispatchPolicy;

public final class Parameters {
    // General
    public static int RUNS = 15;
    public static int MAX_RUNNING_TIME = (int) (4 * 60); // minutes

    // ************************ Simulation ************************
    public static final int BUFFER_SIZE = 4; // hours
    public static final LocalTime DAY_SHIFT_START = LocalTime.of(8, 0);
    public static final LocalTime NIGHT_SHIFT_START = LocalTime.of(20, 0);
    public static final DispatchPolicy DISPATCH_POLICY = DispatchPolicy.Fastest;
    public static final int UPDATE_LOCATION_PERIOD = 5; // minutes

    // Average week (Week 2)
    public static final LocalDateTime START_DATE_TIME = LocalDateTime.of(2018, 1, 8, 0, 0, 0);
    public static final LocalDateTime END_DATE_TIME = LocalDateTime.of(2018, 1, 15, 0, 0, 0);

    public static int NUMBER_OF_AMBULANCES_DAY = 39; // 45 * 87.2% H&A incidents
    public static int NUMBER_OF_AMBULANCES_NIGHT = 22; // 29 * 75.6% H&A incidents

    // Simulation visualization
    public static final int GUI_UPDATE_INTERVAL = 400; // every x milliseconds

    // ************************ Optimizers ************************

    // SLS
    public static int MAX_TRIES = 999;
    public static double RESTART_PROBABILITY = 0.025;
    public static double NOISE_PROBABILITY = 0.65;
    public static int LAZY_NEIGHBOURHOOD_SIZE = 80;

    // Genetic / Memetic Algorithm
    public static Initializer INITIALIZER = new Random();
    public static int GENERATIONS = 999;
    public static int POPULATION_SIZE = 30;
    public static int ELITE_SIZE = 4;
    public static int TOURNAMENT_SIZE = 5;

    public static double CROSSOVER_PROBABILITY = 0.15;
    public static double MUTATION_PROBABILITY = 0.05;
    public static boolean USE_SWAP_MUTATION = true;

    // MA specific
    public static int POPULATION_PROPORTIONATE_SEEDING_SIZE = 3;
    public static double IMPROVE_PROBABILITY = 0.1;
    public static ImproveOperator IMPROVE_OPERATOR = ImproveOperator.OPERATORCRITIC;

}
