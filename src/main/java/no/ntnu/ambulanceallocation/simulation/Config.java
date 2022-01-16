package no.ntnu.ambulanceallocation.simulation;

import java.time.LocalDateTime;

import no.ntnu.ambulanceallocation.Parameters;

public record Config(
        LocalDateTime START_DATE_TIME,
        LocalDateTime END_DATE_TIME,
        int NUMBER_OF_AMBULANCES_DAY,
        int NUMBER_OF_AMBULANCES_NIGHT,
        DispatchPolicy DISPATCH_POLICY,
        int BUFFER_SIZE,
        int UPDATE_LOCATION_PERIOD) {

    public static Config defaultConfig() {
        return new Config(
                Parameters.START_DATE_TIME,
                Parameters.END_DATE_TIME,
                Parameters.NUMBER_OF_AMBULANCES_DAY,
                Parameters.NUMBER_OF_AMBULANCES_NIGHT,
                Parameters.DISPATCH_POLICY,
                Parameters.BUFFER_SIZE,
                Parameters.UPDATE_LOCATION_PERIOD);
    }

    public static Config withinPeriod(LocalDateTime start, LocalDateTime end) {
        return new Config(
                start,
                end,
                Parameters.NUMBER_OF_AMBULANCES_DAY,
                Parameters.NUMBER_OF_AMBULANCES_NIGHT,
                Parameters.DISPATCH_POLICY,
                Parameters.BUFFER_SIZE,
                Parameters.UPDATE_LOCATION_PERIOD);
    }

    public static Config withNumAmbulances(int day, int night) {
        return new Config(
                Parameters.START_DATE_TIME,
                Parameters.END_DATE_TIME,
                day,
                night,
                Parameters.DISPATCH_POLICY,
                Parameters.BUFFER_SIZE,
                Parameters.UPDATE_LOCATION_PERIOD);
    }
}
