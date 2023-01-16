package no.ntnu.ambulanceallocation.experiments;

import java.util.HashMap;
import java.util.Map;

public abstract class Experiment {

    static Map<String, String> parameters = new HashMap<>();

    abstract void run();

    abstract void saveResults();

    protected static void setParameterValues(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String[] parts = args[i].split("=");
            if (parts.length == 2) {
                parameters.put(parts[0], parts[1]);
            }
        }
    }

}
