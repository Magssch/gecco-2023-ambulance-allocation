package no.ntnu.ambulanceallocation.simulation.grid;

public record OneToManyRoutes(Coordinate start, Coordinate end, double distance, Coordinate[] path) {

}
