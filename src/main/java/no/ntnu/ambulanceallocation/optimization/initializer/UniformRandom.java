package no.ntnu.ambulanceallocation.optimization.initializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import no.ntnu.ambulanceallocation.simulation.BaseStation;
import no.ntnu.ambulanceallocation.utils.Utils;

public class UniformRandom implements Initializer {

    @Override
    public List<Integer> initialize(int numberOfAmbulances) {
        List<Integer> ambulanceAllocation = new ArrayList<>();
        List<Integer> ids = Arrays.stream(BaseStation.values()).map(BaseStation::getId).toList();
        while (ambulanceAllocation.size() < numberOfAmbulances) {
            if (ambulanceAllocation.size() + ids.size() <= numberOfAmbulances) {
                ambulanceAllocation.addAll(ids);
            } else {
                int remaining = numberOfAmbulances - ambulanceAllocation.size();
                List<Integer> rest = Utils.randomChooseN(ids, remaining);
                ambulanceAllocation.addAll(rest);
            }
        }
        return ambulanceAllocation;
    }

}
