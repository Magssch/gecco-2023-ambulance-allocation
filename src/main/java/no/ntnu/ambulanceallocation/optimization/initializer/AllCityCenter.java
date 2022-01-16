package no.ntnu.ambulanceallocation.optimization.initializer;

import no.ntnu.ambulanceallocation.simulation.BaseStation;
import no.ntnu.ambulanceallocation.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllCityCenter implements  Initializer {

    @Override
    public List<Integer> initialize(int numberOfAmbulances) {
        return new ArrayList<>(Collections.nCopies(numberOfAmbulances, BaseStation.SENTRUM.getId()));
    }

}
