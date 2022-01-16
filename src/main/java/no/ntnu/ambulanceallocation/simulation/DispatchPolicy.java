package no.ntnu.ambulanceallocation.simulation;

import java.util.Comparator;

import no.ntnu.ambulanceallocation.simulation.incident.Incident;

public enum DispatchPolicy {
    Euclidean {
        @Override
        public Comparator<Ambulance> useOn(Incident incident) {
            return Comparator.comparingDouble(
                    ambulance -> ambulance.getCurrentLocation().euclideanDistanceTo(incident.getLocation()));
        }
    },
    Manhattan {
        @Override
        public Comparator<Ambulance> useOn(Incident incident) {
            return Comparator.comparingDouble(
                    ambulance -> ambulance.getCurrentLocation().manhattanDistanceTo(incident.getLocation()));
        }
    },
    Fastest {
        @Override
        public Comparator<Ambulance> useOn(Incident incident) {
            return Comparator
                    .comparingDouble(ambulance -> ambulance.getCurrentLocation().timeTo(incident.getLocation()));
        }
    };

    public abstract Comparator<Ambulance> useOn(Incident incident);
}
