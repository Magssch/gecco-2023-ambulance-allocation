package no.ntnu.ambulanceallocation.simulation;

import java.util.Arrays;
import java.util.List;

import no.ntnu.ambulanceallocation.simulation.grid.Coordinate;

public enum BaseStation {

    EIDSVOLL(0, false, 287187, 6692448, 34399),
    NES(1, false, 304206, 6669953, 22986),
    ULLENSAKER(2, false, 286455, 6671754, 46566),
    AURSKOG_HOLAND(3, false, 307577, 6642937, 18528),
    LORENSKOG(4, false, 275840, 6650643, 122609),
    NITTEDAL(5, false, 270631, 6663254, 21117),
    BROBEKK(6, false, 267085, 6651035, 79743),
    SENTRUM(7, false, 262948, 6649765, 113998),
    ULLEVAAL(8, false, 261774, 6652003, 83078),
    NORDRE_FOLLO(9, false, 266827, 6627037, 45070),
    SONDRE_FOLLO(10, false, 259265, 6621267, 66265),
    PRINSDAL(11, false, 265048, 6640259, 69424),
    ASKER(12, false, 244478, 6641283, 57732),
    BAERUM(13, false, 248901, 6648585, 59736),
    SMESTAD(14, false, 259127, 6652543, 150191),
    RYEN(15, true, 265439, 6646945, 130185),
    GRORUD(16, true, 270248, 6654139, 75225),
    SKEDSMOKORSET(17, true, 279154, 6657789, 48536),
    BEKKESTUA(18, true, 253295, 6650494, 55464);

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
