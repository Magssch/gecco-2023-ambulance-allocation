package no.ntnu.ambulanceallocation.optimization.ma;

import no.ntnu.ambulanceallocation.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class OperatorCritic {

    private final Map<String, Double> improvements = new HashMap<>();
    private final Map<String, Integer> trials = new HashMap<>();

    public String selectNext() {
        Map<String, Double> relativeImprovements = getRelativeImprovements();
        double totalRelativeImprovement = relativeImprovements.values().stream().reduce(0.0, Double::sum);
        double accumulatedRelativeImprovement = 0.0;
        double p = Utils.randomDouble() * totalRelativeImprovement;

        for (Map.Entry<String, Double> relativeImprovement : getRelativeImprovements().entrySet()) {
            accumulatedRelativeImprovement += relativeImprovement.getValue();
            if (p < accumulatedRelativeImprovement) {
                return relativeImprovement.getKey();
            }
        }
        return null;
    }

    public void assignCredit(String operator, double improvement) {
        improvements.put(operator, improvements.getOrDefault(operator, 0.0) + improvement);
        trials.put(operator, trials.getOrDefault(operator, 0) + 1);
    }


    public Map<String, Double> getRelativeImprovements() {
        Map<String, Double> relativeImprovements = new HashMap<>();
        for (String operator : improvements.keySet()) {
            relativeImprovements.put(operator, improvements.get(operator) / trials.get(trials));
        }
        return relativeImprovements;
    }

}
