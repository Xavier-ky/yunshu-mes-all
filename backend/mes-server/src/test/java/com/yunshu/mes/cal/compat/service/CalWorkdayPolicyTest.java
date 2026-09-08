package com.yunshu.mes.cal.compat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalWorkdayPolicyTest {

    private static final LocalDate SUNDAY = LocalDate.of(2026, 7, 5);
    private static final LocalDate MONDAY = LocalDate.of(2026, 7, 6);

    @Test
    void sundayIsOffByDefault() {
        assertFalse(CalWorkdayPolicy.isWorkday(SUNDAY, Map.of()));
        assertEquals("HOLIDAY", CalWorkdayPolicy.displayType(SUNDAY, Map.of()));
    }

    @Test
    void explicitWorkdayOverridesSunday() {
        Map<LocalDate, String> overrides = Map.of(SUNDAY, "WORKDAY");

        assertTrue(CalWorkdayPolicy.isWorkday(SUNDAY, overrides));
        assertEquals("WORKDAY", CalWorkdayPolicy.displayType(SUNDAY, overrides));
    }

    @Test
    void explicitHolidayAlwaysWins() {
        Map<LocalDate, String> overrides = Map.of(MONDAY, "HOLIDAY");

        assertFalse(CalWorkdayPolicy.isWorkday(MONDAY, overrides));
        assertEquals("HOLIDAY", CalWorkdayPolicy.displayType(MONDAY, overrides));
    }

    @Test
    void ordinaryWeekdayIsWorkday() {
        assertTrue(CalWorkdayPolicy.isWorkday(MONDAY, Map.of()));
        assertNull(CalWorkdayPolicy.displayType(MONDAY, Map.of()));
    }
}
