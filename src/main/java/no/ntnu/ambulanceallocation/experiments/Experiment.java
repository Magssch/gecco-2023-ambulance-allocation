package no.ntnu.ambulanceallocation.experiments;

import java.util.HashMap;
import java.util.Map;

public abstract class Experiment {

    protected static final Map<String, String> parameters = new HashMap<>();

    abstract void run();

    abstract void saveResults();

    protected static void setParameterValues(String[] args) {
        for (String arg : args) {
            String[] parts = arg.split("=");
            if (parts.length == 2) {
                parameters.put(parts[0], parts[1]);
            }
        }
    }

}
