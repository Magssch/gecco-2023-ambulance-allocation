package no.ntnu.ambulanceallocation.optimization.sls;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SlsSolutionTest {

    @Test
    public void comparableBasedOnFitnessShouldWork() {
        SlsSolution solution1 = new SlsSolution();
        SlsSolution solution2 = new SlsSolution();

        List<SlsSolution> solutions = Arrays.asList(solution1, solution2);
        Collections.sort(solutions);
        SlsSolution bestSolution = solutions.get(0);
        SlsSolution worstSolution = solutions.get(1);
        assertTrue(bestSolution.compareTo(worstSolution) <= 0);
    }

}
