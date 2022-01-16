package no.ntnu.ambulanceallocation.simulation.event;

import java.time.LocalDateTime;

import no.ntnu.ambulanceallocation.simulation.Ambulance;

public final class LocationUpdate extends Event {

    public final Ambulance ambulance;

    public LocationUpdate(LocalDateTime time, Ambulance ambulance) {
        super(time);
        this.ambulance = ambulance;
    }

}
