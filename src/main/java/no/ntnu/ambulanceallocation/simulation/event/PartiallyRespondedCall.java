package no.ntnu.ambulanceallocation.simulation.event;

import java.time.LocalDateTime;

public final class PartiallyRespondedCall extends NewCall {

    private int numDispatchedTransportingVehicles = 0;
    private int numDispatchedNonTransportingVehicles = 0;

    public PartiallyRespondedCall(NewCall newCall) {
        super(newCall.incident, false);
        if (newCall instanceof PartiallyRespondedCall call) {
            this.numDispatchedTransportingVehicles = call.numDispatchedTransportingVehicles;
            this.numDispatchedNonTransportingVehicles = call.numDispatchedNonTransportingVehicles;
        }
    }

    public PartiallyRespondedCall(PartiallyRespondedCall call, LocalDateTime newTime) {
        super(call, newTime);
        this.numDispatchedNonTransportingVehicles = call.numDispatchedNonTransportingVehicles;
        this.numDispatchedTransportingVehicles = call.numDispatchedTransportingVehicles;
    }

    @Override
    public int getTransportingVehicleDemand() {
        return incident.transportingVehicles() - numDispatchedTransportingVehicles;
    }

    @Override
    public int getNonTransportingVehicleDemand() {
        return incident.nonTransportingVehicles() - numDispatchedNonTransportingVehicles;
    }

    public void respondWithTransportingVehicles(int numTransportingVehicles) {
        if (numTransportingVehicles > getTransportingVehicleDemand()) {
            throw new IllegalArgumentException(
                    "More transporting vehicles responded to incident " + incident + " than required");
        }
        numDispatchedTransportingVehicles += numTransportingVehicles;
    }

    public void respondWithNonTransportingVehicles(int numNonTransportingVehicles) {
        if (numNonTransportingVehicles > getNonTransportingVehicleDemand()) {
            throw new IllegalArgumentException(
                    "More non-transporting vehicles responded to incident " + incident + " than required");
        }
        numDispatchedNonTransportingVehicles += numNonTransportingVehicles;
    }

    @Override
    public String toString() {
        return String.format("%sEvent - %s - ID: %s", this.getClass().getSimpleName(), incident.callReceived(),
                incident.hashCode());
    }

}
