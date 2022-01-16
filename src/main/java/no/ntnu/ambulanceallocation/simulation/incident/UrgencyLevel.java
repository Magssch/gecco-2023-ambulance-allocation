package no.ntnu.ambulanceallocation.simulation.incident;

import java.util.HashMap;
import java.util.Map;

public enum UrgencyLevel {
    ACUTE("A"),
    URGENT("H"),
    REGULAR("V"),
    REGULAR_UNPLANNED("V1"),
    REGULAR_PLANNED("V2");

    private final String symbol;

    // Map for getting an urgency level from a symbol
    private static final Map<String, UrgencyLevel> urgencyLevels = new HashMap<String, UrgencyLevel>();

    static {
        for (UrgencyLevel urgencyLevel : UrgencyLevel.values()) {
            urgencyLevels.put(urgencyLevel.getSymbol(), urgencyLevel);
        }
    }

    UrgencyLevel(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static UrgencyLevel get(String symbol) {
        return urgencyLevels.get(symbol);
    }

}
