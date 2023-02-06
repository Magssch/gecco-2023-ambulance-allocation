package no.ntnu.ambulanceallocation.optimization;

import no.ntnu.ambulanceallocation.experiments.Result;
import no.ntnu.ambulanceallocation.simulation.Config;

public interface Optimizer {

    void optimize();

    Solution getOptimalSolution();

    String getAbbreviation();

    Result getRunStatistics();

    Config getConfig();

}
