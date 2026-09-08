package com.yunshu.mes.cal.compat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CalScheduleConflictDetectorTest {

    @Test
    void rejectsSameTeamAndDayFromEnabledConfirmedPlan() {
        LocalDate day = LocalDate.of(2026, 7, 8);
        List<CalScheduleConflictDetector.Assignment> candidate = List.of(
                assignment(51L, "新计划", day, 11L, "组装一班", "白班",
                        "08:00", "17:00", "CONFIRMED", "Y"));
        List<CalScheduleConflictDetector.Assignment> existing = List.of(
                assignment(61L, "已确认组装计划", day, 11L, "组装一班", "白班",
                        "08:00", "17:00", "CONFIRMED", "Y"));

        String error = CalScheduleConflictDetector.detect(
                candidate, existing, Map.of(11L, Set.of()), Map.of());

        assertEquals("排班冲突：组装一班在 2026-07-08 已由计划“已确认组装计划”排班", error);
    }

    @Test
    void priorDayOvernightAssignmentConflictsWithNextMorning() {
        List<CalScheduleConflictDetector.Assignment> candidate = List.of(
                assignment(51L, "新计划", LocalDate.of(2026, 7, 9), 11L, "组装一班",
                        "早班", "07:00", "09:00", "CONFIRMED", "Y"));
        List<CalScheduleConflictDetector.Assignment> existing = List.of(
                assignment(61L, "夜班计划", LocalDate.of(2026, 7, 8), 12L, "组装二班",
                        "夜班", "20:00", "08:00", "CONFIRMED", "Y"));

        String error = CalScheduleConflictDetector.detect(
                candidate, existing, Map.of(11L, Set.of(7L), 12L, Set.of(7L)),
                Map.of(7L, "生产主管"));

        assertEquals("人员冲突：生产主管在 2026-07-09 同时分配到组装一班和组装二班", error);
    }

    @Test
    void overnightAssignmentDoesNotConflictWithSameDateMorning() {
        List<CalScheduleConflictDetector.Assignment> candidate = List.of(
                assignment(51L, "新计划", LocalDate.of(2026, 7, 8), 11L, "组装一班",
                        "早班", "07:00", "09:00", "CONFIRMED", "Y"));
        List<CalScheduleConflictDetector.Assignment> existing = List.of(
                assignment(61L, "夜班计划", LocalDate.of(2026, 7, 8), 12L, "组装二班",
                        "夜班", "20:00", "08:00", "CONFIRMED", "Y"));

        String error = CalScheduleConflictDetector.detect(
                candidate, existing, Map.of(11L, Set.of(7L), 12L, Set.of(7L)),
                Map.of(7L, "生产主管"));

        assertNull(error);
    }

    @Test
    void sameTeamAdjacentDayOverlapDoesNotDependOnMemberData() {
        List<CalScheduleConflictDetector.Assignment> candidate = List.of(
                assignment(51L, "新计划", LocalDate.of(2026, 7, 9), 11L, "组装一班",
                        "早班", "07:00", "09:00", "CONFIRMED", "Y"));
        List<CalScheduleConflictDetector.Assignment> existing = List.of(
                assignment(61L, "夜班计划", LocalDate.of(2026, 7, 8), 11L, "组装一班",
                        "夜班", "20:00", "08:00", "CONFIRMED", "Y"));

        String error = CalScheduleConflictDetector.detect(candidate, existing, Map.of(), Map.of());

        assertEquals("班次冲突：组装一班的早班与夜班时间重叠", error);
    }

    @Test
    void rejectsSamePlanMemberAssignedToOverlappingTeams() {
        LocalDate day = LocalDate.of(2026, 7, 8);
        List<CalScheduleConflictDetector.Assignment> candidate = List.of(
                assignment(51L, "新计划", day, 11L, "组装一班", "白班", "08:00", "17:00", "CONFIRMED", "Y"),
                assignment(51L, "新计划", day, 12L, "组装二班", "临时班", "13:00", "21:00", "CONFIRMED", "Y"));

        String error = CalScheduleConflictDetector.detect(
                candidate, List.of(), Map.of(11L, Set.of(7L), 12L, Set.of(7L)), Map.of(7L, "生产主管"));

        assertEquals("人员冲突：生产主管在 2026-07-08 同时分配到组装一班和组装二班", error);
    }

    @Test
    void ignoresHolidaysDraftPlansDisabledPlansAndIrrelevantTeams() {
        LocalDate workday = LocalDate.of(2026, 7, 6);
        List<CalScheduleConflictDetector.Assignment> candidate = List.of(
                assignment(51L, "新计划", workday, 11L, "组装一班", "白班",
                        "08:00", "17:00", "CONFIRMED", "Y"));
        List<CalScheduleConflictDetector.Assignment> existing = List.of(
                assignment(61L, "草稿", workday, 11L, "组装一班", "白班",
                        "08:00", "17:00", "PREPARE", "Y"),
                assignment(62L, "停用计划", workday, 11L, "组装一班", "白班",
                        "08:00", "17:00", "CONFIRMED", "N"),
                assignment(63L, "无关班组", workday, 99L, "注塑一班", "白班",
                        "08:00", "17:00", "CONFIRMED", "Y"),
                assignment(64L, "周日计划", LocalDate.of(2026, 7, 5), 11L, "组装一班", "白班",
                        "08:00", "17:00", "CONFIRMED", "Y"));

        String error = CalScheduleConflictDetector.detect(
                candidate, existing, Map.of(11L, Set.of(7L), 99L, Set.of(8L)),
                Map.of(7L, "生产主管", 8L, "注塑工"));

        assertNull(error);
    }

    @Test
    void conflictCountUsesConcreteIntervalsAcrossAdjacentDays() {
        List<CalScheduleConflictDetector.Assignment> assignments = List.of(
                assignment(51L, "夜班计划", LocalDate.of(2026, 7, 8), 11L, "组装一班",
                        "夜班", "20:00", "08:00", "CONFIRMED", "Y"),
                assignment(52L, "早班计划", LocalDate.of(2026, 7, 9), 12L, "组装二班",
                        "早班", "07:00", "09:00", "CONFIRMED", "Y"),
                assignment(53L, "午班计划", LocalDate.of(2026, 7, 9), 13L, "组装三班",
                        "午班", "12:00", "16:00", "CONFIRMED", "Y"));

        int count = CalScheduleConflictDetector.countConflicts(
                assignments,
                Map.of(11L, Set.of(7L), 12L, Set.of(7L), 13L, Set.of(7L)),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, count);
    }

    @Test
    void conflictCountIgnoresSameDayMembersWhenIntervalsDoNotOverlap() {
        LocalDate day = LocalDate.of(2026, 7, 9);
        List<CalScheduleConflictDetector.Assignment> assignments = List.of(
                assignment(51L, "早班计划", day, 11L, "组装一班",
                        "早班", "07:00", "09:00", "CONFIRMED", "Y"),
                assignment(52L, "午班计划", day, 12L, "组装二班",
                        "午班", "12:00", "16:00", "CONFIRMED", "Y"));

        int count = CalScheduleConflictDetector.countConflicts(
                assignments, Map.of(11L, Set.of(7L), 12L, Set.of(7L)), day, day);

        assertEquals(0, count);
    }

    @Test
    void teamScopedCountIncludesConflictWithOtherTeam() {
        LocalDate day = LocalDate.of(2026, 7, 9);
        List<CalScheduleConflictDetector.Assignment> assignments = List.of(
                assignment(51L, "组装计划", day, 11L, "组装一班",
                        "白班", "08:00", "17:00", "CONFIRMED", "Y", "ZZ"),
                assignment(52L, "注塑计划", day, 21L, "注塑一班",
                        "白班", "09:00", "18:00", "CONFIRMED", "Y", "ZS"));

        int count = CalScheduleConflictDetector.countConflicts(
                assignments,
                Map.of(11L, Set.of(7L), 21L, Set.of(7L)),
                day, day,
                assignment -> assignment.teamId().equals(11L));

        assertEquals(1, count);
    }

    @Test
    void typeScopedCountIncludesConflictWithOtherCalendarType() {
        LocalDate day = LocalDate.of(2026, 7, 9);
        List<CalScheduleConflictDetector.Assignment> assignments = List.of(
                assignment(51L, "组装计划", day, 11L, "组装一班",
                        "白班", "08:00", "17:00", "CONFIRMED", "Y", "ZZ"),
                assignment(52L, "注塑计划", day, 21L, "注塑一班",
                        "白班", "09:00", "18:00", "CONFIRMED", "Y", "ZS"));

        int count = CalScheduleConflictDetector.countConflicts(
                assignments,
                Map.of(11L, Set.of(7L), 21L, Set.of(7L)),
                day, day,
                assignment -> "ZZ".equals(assignment.calendarType()));

        assertEquals(1, count);
    }

    private CalScheduleConflictDetector.Assignment assignment(Long planId, String planName, LocalDate day,
            Long teamId, String teamName, String shiftName, String start, String end,
            String status, String enableFlag) {
        return assignment(planId, planName, day, teamId, teamName, shiftName,
                start, end, status, enableFlag, "ZZ");
    }

    private CalScheduleConflictDetector.Assignment assignment(Long planId, String planName, LocalDate day,
            Long teamId, String teamName, String shiftName, String start, String end,
            String status, String enableFlag, String calendarType) {
        return new CalScheduleConflictDetector.Assignment(
                planId, planName, day, teamId, teamName, 1L, shiftName,
                1, LocalTime.parse(start), LocalTime.parse(end), status, enableFlag, calendarType);
    }
}
