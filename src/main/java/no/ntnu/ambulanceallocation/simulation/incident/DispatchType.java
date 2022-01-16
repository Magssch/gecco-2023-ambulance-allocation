package no.ntnu.ambulanceallocation.simulation.incident;

import java.util.HashMap;
import java.util.Map;

public enum DispatchType {
    AMBULANCE("Ambulanse"),
    OPERATIONS_MANAGER("Operativ Leder"),
    RESPONSE_VEHICLE_WITH_PHYSICIAN("Legebil"),
    PATIENT_TRANSPORT("Syketransport"),
    RAPID_RESPONSE_VEHICLE("Akuttbil");

    private final String nationalName;

    // Map for getting an dispatch type from the national name
    private static final Map<String, DispatchType> dispatchTypes = new HashMap<String, DispatchType>();

    static {
        for (DispatchType DispatchType : DispatchType.values()) {
            dispatchTypes.put(DispatchType.getNationalName(), DispatchType);
        }
    }

    DispatchType(String nationalName) {
        this.nationalName = nationalName;
    }

    public String getNationalName() {
        return nationalName;
    }

    public static DispatchType get(String nationalName) {
        return dispatchTypes.get(nationalName);
    }

}
