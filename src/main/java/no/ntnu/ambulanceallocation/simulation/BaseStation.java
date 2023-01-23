package no.ntnu.ambulanceallocation.simulation;

import java.util.Arrays;
import java.util.List;

import no.ntnu.ambulanceallocation.simulation.grid.Coordinate;

public enum BaseStation {

    EIDSVOLL(0, false, 287187, 6692448, 33402),
    NES(1, false, 304206, 6669953, 22355),
    ULLENSAKER(2, false, 286455, 6671754, 42865),
    AURSKOG_HOLAND(3, false, 307577, 6642937, 18130),
    LORENSKOG(4, false, 275840, 6650643, 117049),
    NITTEDAL(5, false, 270631, 6663254, 20280),
    BROBEKK(6, false, 267085, 6651035, 76221),
    SENTRUM(7, false, 262948, 6649765, 110350),
    ULLEVAAL(8, false, 261774, 6652003, 80759),
    NORDRE_FOLLO(9, false, 266827, 6627037, 44551),
    SONDRE_FOLLO(10, false, 259265, 6621267, 64139),
    PRINSDAL(11, false, 265048, 6640259, 68728),
    ASKER(12, false, 244478, 6641283, 57008),
    BAERUM(13, false, 248901, 6648585, 58896),
    SMESTAD(14, false, 259127, 6652543, 146058),
    RYEN(15, true, 265439, 6646945, 127670),
    GRORUD(16, true, 270248, 6654139, 74892),
    SKEDSMOKORSET(17, true, 279154, 6657789, 46995),
    BEKKESTUA(18, true, 253295, 6650494, 54784);

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
