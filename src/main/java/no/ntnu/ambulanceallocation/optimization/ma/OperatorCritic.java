package no.ntnu.ambulanceallocation.optimization.ma;

import no.ntnu.ambulanceallocation.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class OperatorCritic {

    private final Map<Integer, Double> improvements = new HashMap<>();
    private final Map<Integer, Integer> trials = new HashMap<>();

    public OperatorCritic(int numberOfOperators) {
        for (int i = 0; i < numberOfOperators; i++) {
            assignCredit(i, Utils.randomDouble() / 100.0);
        }
    }

    public int selectNext() {
        Map<Integer, Double> relativeImprovements = getRelativeImprovements();
        double totalRelativeImprovement = relativeImprovements.values().stream().reduce(0.0, Double::sum);
        double accumulatedRelativeImprovement = 0.0;
        double p = Utils.randomDouble() * totalRelativeImprovement;

        for (Map.Entry<Integer, Double> relativeImprovement : getRelativeImprovements().entrySet()) {
            accumulatedRelativeImprovement += relativeImprovement.getValue();
            if (p <= accumulatedRelativeImprovement) {
                return relativeImprovement.getKey();
            }
        }
        return 0;
    }

    public void assignCredit(Integer operator, double improvement) {
        improvements.put(operator, improvements.getOrDefault(operator, 0.0) + improvement);
        trials.put(operator, trials.getOrDefault(operator, 0) + 1);
    }


    public Map<Integer, Double> getRelativeImprovements() {
        Map<Integer, Double> relativeImprovements = new HashMap<>();
        for (Integer operator : improvements.keySet()) {
            relativeImprovements.put(operator, improvements.get(operator) / trials.get(operator));
        }
        return relativeImprovements;
    }

}
