package no.ntnu.ambulanceallocation.simulation;

import java.time.LocalDateTime;
import java.time.LocalTime;

import no.ntnu.ambulanceallocation.Parameters;

public enum ShiftType {
    DAY,
    NIGHT;

    public ShiftType previous() {
        return this == DAY ? NIGHT : DAY;
    }

    public static ShiftType get(LocalDateTime dateTime) {
        return get(dateTime.toLocalTime());
    }

    public static ShiftType get(LocalTime time) {
        if (time.isAfter(Parameters.NIGHT_SHIFT_START) || time.isBefore(Parameters.DAY_SHIFT_START)) {
            return NIGHT;
        } else {
            return DAY;
        }
    }
}
