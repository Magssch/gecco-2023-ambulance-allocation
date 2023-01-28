package no.ntnu.ambulanceallocation;

import java.time.LocalDateTime;
import java.time.LocalTime;

import no.ntnu.ambulanceallocation.optimization.initializer.Initializer;
import no.ntnu.ambulanceallocation.optimization.initializer.Random;
import no.ntnu.ambulanceallocation.simulation.DispatchPolicy;

public final class Parameters {
    // General
    public static int RUNS = 15;
    public static int MAX_RUNNING_TIME = (int) (4.0 * 60); // minutes

    // Simulation
    public static final int BUFFER_SIZE = 4; // hours
    public static final LocalDateTime START_DATE_TIME = LocalDateTime.of(2017, 8, 7, 0, 0, 0);
    public static final LocalDateTime END_DATE_TIME = LocalDateTime.of(2017, 8, 14, 0, 0, 0);

    public static final int NUMBER_OF_AMBULANCES_DAY = 39; // 45 * 87.1% H&A incidents
    public static final int NUMBER_OF_AMBULANCES_NIGHT = 22; // 29 * 74.5% H&A incidents

    public static final LocalTime DAY_SHIFT_START = LocalTime.of(8, 0);
    public static final LocalTime NIGHT_SHIFT_START = LocalTime.of(20, 0);

    public static final DispatchPolicy DISPATCH_POLICY = DispatchPolicy.Fastest;
    public static final int UPDATE_LOCATION_PERIOD = 5; // minutes

    // Simulation visualization
    public static final int GUI_UPDATE_INTERVAL = 400; // every x milliseconds

    // SLS
    public static final int MAX_TRIES = 999;
    public static final double RESTART_PROBABILITY = 0.025;
    public static final double NOISE_PROBABILITY = 0.8;
    public static final int LAZY_NEIGHBOURHOOD_SIZE = 5;

    // Genetic & Memetic Algorithm
    public static Initializer INITIALIZER = new Random();
    public static int POPULATIONPROPORTIONATE_SEEDING_SIZE = 1;
    public static int GENERATIONS = 999;
    public static int POPULATION_SIZE = 36;
    public static int ELITE_SIZE = 4;
    public static int TOURNAMENT_SIZE = 5;

    public static double CROSSOVER_PROBABILITY = 0.2;
    public static double MUTATION_PROBABILITY = 0.05;
    public static double IMPROVE_PROBABILITY = 0.25;

}
