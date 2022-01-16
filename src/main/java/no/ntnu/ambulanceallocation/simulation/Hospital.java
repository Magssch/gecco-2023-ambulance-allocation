package no.ntnu.ambulanceallocation.simulation;

import java.util.Comparator;

import no.ntnu.ambulanceallocation.simulation.grid.Coordinate;
import no.ntnu.ambulanceallocation.simulation.incident.Incident;

public enum Hospital {

    STORGATA(262948, 6649765),
    AKER(265200, 6652210),
    ULLEVAAL(261774, 6652003),
    RIKSHOSPITALET(260789, 6653451),
    RADIUMHOSPITALET(257732, 6651563),
    NORDBYHAGEN(276381, 6650642),
    SKI(266359, 6628267),
    LOVISENBERG(262348, 6651667),
    DIAKONHJEMMET(260024, 6652122),
    BAERUM(248901, 6648585),
    ROMERIKE(278942, 6652867);

    private final Coordinate coordinate;

    Hospital(int easting, int northing) {
        coordinate = new Coordinate(easting, northing);
    }

    public static Comparator<Hospital> closestTo(Incident incident) {
        return Comparator.comparingDouble(hospital -> timeTo(hospital, incident));
    }

    public static double timeTo(Hospital hospital, Incident incident) {
        return hospital.coordinate.timeTo(incident.getLocation());
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

}
