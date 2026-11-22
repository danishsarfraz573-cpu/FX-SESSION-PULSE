package com.basics.sessionpulse;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Market {
    private final String name;
    private final ZoneId zone;
    private final LocalTime open;
    private final LocalTime close;

    public Market(String name, ZoneId zone, LocalTime open, LocalTime close) {
        this.name = name;
        this.zone = zone;
        this.open = open;
        this.close = close;
    }

    public String getName() {
        return name;
    }

    public ZoneId getZone() {
        return zone;
    }

    public boolean isOpen(Instant now) {
        ZonedDateTime local = now.atZone(zone);
        DayOfWeek day = local.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime t = local.toLocalTime();
        return !t.isBefore(open) && t.isBefore(close);
    }
}