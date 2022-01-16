package no.ntnu.ambulanceallocation.simulation;

import java.util.Arrays;
import java.util.List;

import no.ntnu.ambulanceallocation.simulation.grid.Coordinate;

public enum BaseStation {

    EIDSVOLL(0, false, 287187, 6692448, 36967),
    NES(1, false, 304206, 6669953, 22392),
    ULLENSAKER(2, false, 286455, 6671754, 44736),
    AURSKOG_HOLAND(3, false, 307577, 6642937, 18098),
    LORENSKOG(4, false, 275840, 6650643, 102552),
    NITTEDAL(5, false, 270631, 6663254, 19432),
    BROBEKK(6, false, 267085, 6651035, 79480),
    SENTRUM(7, false, 262948, 6649765, 137493),
    ULLEVAAL(8, false, 261774, 6652003, 123820),
    NORDRE_FOLLO(9, false, 266827, 6627037, 47309),
    SONDRE_FOLLO(10, false, 259265, 6621267, 48164),
    PRINSDAL(11, false, 265048, 6640259, 94080),
    ASKER(12, false, 244478, 6641283, 49820),
    BAERUM(13, false, 248901, 6648585, 65986),
    SMESTAD(14, false, 259127, 6652543, 99298),
    RYEN(15, true, 265439, 6646945, 94507),
    GRORUD(16, true, 270248, 6654139, 81692),
    SKEDSMOKORSET(17, true, 279154, 6657789, 51117),
    BEKKESTUA(18, true, 253295, 6650494, 83909);

    private final int id;
    private final boolean isStandbyPoint;
    private final Coordinate coordinate;
    private final int population;

    BaseStation(int id, boolean isStandbyPoint, int easting, int northing, int population) {
        this.id = id;
        this.isStandbyPoint = isStandbyPoint;
        coordinate = new Coordinate(easting, northing);
        this.population = population;
    }

    public static List<Integer> ids() {
        return Arrays.stream(BaseStation.values()).mapToInt(BaseStation::getId).boxed().toList();
    }

    public static BaseStation get(int index) {
        if (index >= size()) {
            throw new IllegalArgumentException("Index out of bonds.");
        }
        return BaseStation.values()[index];
    }

    public static int size() {
        return BaseStation.values().length;
    }

    public int getId() {
        return id;
    }

    public boolean isStandbyPoint() {
        return isStandbyPoint;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public int getPopulation() {
        return population;
    }

    public static List<Double> getPopulationDistribution() {
        double totalPopulation = Arrays.stream(BaseStation.values())
                .map(BaseStation::getPopulation)
                .reduce(0, Integer::sum);
        return Arrays.stream(BaseStation.values())
                .map(BaseStation::getPopulation)
                .map(population -> population / totalPopulation)
                .toList();
    }

}
