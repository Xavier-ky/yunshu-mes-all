package com.yunshu.mes.cal.compat.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

final class CalWorkdayPolicy {

    private CalWorkdayPolicy() {
    }

    static boolean isWorkday(LocalDate day, Map<LocalDate, String> overrides) {
        String explicit = overrides.get(day);
        if ("HOLIDAY".equalsIgnoreCase(explicit)) {
            return false;
        }
        if ("WORKDAY".equalsIgnoreCase(explicit)) {
            return true;
        }
        return day.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    static String displayType(LocalDate day, Map<LocalDate, String> overrides) {
        String explicit = overrides.get(day);
        if ("HOLIDAY".equalsIgnoreCase(explicit)) {
            return "HOLIDAY";
        }
        if ("WORKDAY".equalsIgnoreCase(explicit)) {
            return "WORKDAY";
        }
        return day.getDayOfWeek() == DayOfWeek.SUNDAY ? "HOLIDAY" : null;
    }
}
