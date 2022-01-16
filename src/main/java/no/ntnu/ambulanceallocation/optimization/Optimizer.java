package no.ntnu.ambulanceallocation.optimization;

import no.ntnu.ambulanceallocation.experiments.Result;

public interface Optimizer {

    void optimize();

    Solution getOptimalSolution();
    
    String getAbbreviation();

    Result getRunStatistics();

}
