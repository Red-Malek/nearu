package com.nearu.backend.servicerequest;

import java.time.LocalTime;

/** Créneau (service_requests.preferred_time_slot, V1) déduit de l'heure choisie. */
public enum TimeSlot {
    MORNING,
    AFTERNOON,
    EVENING,
    ANY;

    public static TimeSlot of(LocalTime time) {
        if (time == null) {
            return ANY;
        }
        if (time.isBefore(LocalTime.NOON)) {
            return MORNING;
        }
        return time.isBefore(LocalTime.of(18, 0)) ? AFTERNOON : EVENING;
    }
}
