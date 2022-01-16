package no.ntnu.ambulanceallocation.optimization.initializer;

import java.util.List;

@FunctionalInterface
public interface Initializer {

    List<Integer> initialize(int numberOfAmbulances);
}
