package no.ntnu.ambulanceallocation.simulation.event;

import java.time.LocalDateTime;

import no.ntnu.ambulanceallocation.simulation.incident.Incident;

public sealed class NewCall extends Event permits PartiallyRespondedCall {

    public final Incident incident;
    public final boolean providesResponseTime;

    public NewCall(Incident incident, boolean providesResponseTime) {
        super(incident.callReceived());
        this.incident = incident;
        this.providesResponseTime = providesResponseTime;
    }

    public NewCall(NewCall newCall, LocalDateTime newTime) {
        super(newTime);
        this.incident = newCall.incident;
        this.providesResponseTime = newCall.providesResponseTime;
    }

    public int getTransportingVehicleDemand() {
        return incident.transportingVehicles();
    }

    public int getNonTransportingVehicleDemand() {
        return incident.nonTransportingVehicles();
    }

    @Override
    public String toString() {
        return String.format("%sEvent - %s - ID: %s", this.getClass().getSimpleName(), incident.callReceived(),
                incident.hashCode());
    }

}
