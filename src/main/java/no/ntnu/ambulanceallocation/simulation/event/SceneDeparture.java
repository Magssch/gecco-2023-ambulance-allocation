package no.ntnu.ambulanceallocation.simulation.event;

import java.time.LocalDateTime;

import no.ntnu.ambulanceallocation.simulation.incident.Incident;

public final class SceneDeparture extends Event {

    public final Incident incident;

    public SceneDeparture(LocalDateTime time, NewCall newCall) {
        super(time);
        this.incident = newCall.incident;
    }

}
