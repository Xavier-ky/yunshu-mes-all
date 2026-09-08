package com.yunshu.mes.cal.compat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Time;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class CalCalendarServiceTest {

    @Test
    void serviceRejectsInvalidQueryScopeBeforeDatabaseAccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        CalCalendarService service = new CalCalendarService(jdbc);

        assertThrows(IllegalArgumentException.class,
                () -> service.list("TYPE", "2026-07-01", " ", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.summary("TEAM", "2026-07-01", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.day("USER", "2026-07-01", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.week("ALL", "2026-07-01", null, null, null));
    }

    @Test
    void emptyCalendarTableIsDetectedThroughInformationSchema() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.tables"), eq(Integer.class), eq("cal_teamshift")))
                .thenReturn(1);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        CalCalendarService service = new CalCalendarService(jdbc);
        List<Map<String, Object>> result = service.list("TYPE", "2026-07-01", "ZZ", null, null);

        assertEquals(31, result.size());
        verify(jdbc).queryForObject(contains("information_schema.tables"), eq(Integer.class), eq("cal_teamshift"));
        verify(jdbc, atLeastOnce()).query(
                contains("FROM cal_teamshift"), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listIncludesShiftTimesPlanNameAndMemberCount() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.tables"), eq(Integer.class), eq("cal_teamshift")))
                .thenReturn(1);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("record_id")).thenReturn(91L);
        when(rs.getString("the_day")).thenReturn("2026-07-01");
        when(rs.getLong("team_id")).thenReturn(11L);
        when(rs.getString("team_name")).thenReturn("组装一班");
        when(rs.getLong("shift_id")).thenReturn(21L);
        when(rs.getString("shift_name")).thenReturn("白班");
        when(rs.getObject("order_num")).thenReturn(1);
        when(rs.getObject("plan_id")).thenReturn(31L);
        when(rs.getString("calendar_type")).thenReturn("ZZ");
        when(rs.getString("shift_type")).thenReturn("SHIFT_TWO");
        when(rs.getTime("shift_start_time")).thenReturn(Time.valueOf("08:00:00"));
        when(rs.getTime("shift_end_time")).thenReturn(Time.valueOf("20:00:00"));
        when(rs.getString("plan_name")).thenReturn("2026年7月组装两班倒");
        when(rs.getInt("member_count")).thenReturn(2);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenAnswer(invocation -> {
            RowMapper<Map<String, Object>> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(rs, 0));
        });

        CalCalendarService service = new CalCalendarService(jdbc);
        List<Map<String, Object>> shifts = (List<Map<String, Object>>) service
                .list("TYPE", "2026-07-01", "ZZ", null, null).get(0).get("teamShifts");
        Map<String, Object> shift = shifts.get(0);
        assertNotNull(shift);
        assertEquals(Time.valueOf("08:00:00"), shift.get("shiftStartTime"));
        assertEquals(Time.valueOf("20:00:00"), shift.get("shiftEndTime"));
        assertEquals("2026年7月组装两班倒", shift.get("planName"));
        assertEquals(2, shift.get("memberCount"));
    }

    @Test
    void summaryReportsCoverageAndUnscheduledWorkdays() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.tables"), eq(Integer.class), anyString())).thenReturn(1);
        when(jdbc.queryForObject(contains("COUNT(DISTINCT ts.the_day)"), eq(Integer.class), any(Object[].class)))
                .thenReturn(25);
        when(jdbc.queryForObject(contains("COUNT(DISTINCT ctm.user_id)"), eq(Integer.class), any(Object[].class)))
                .thenReturn(4);
        ResultSet overnight = conflictRow(
                31L, "夜班计划", "2026-07-08", 11L, "组装一班", "夜班", "20:00:00", "08:00:00");
        ResultSet morning = conflictRow(
                32L, "早班计划", "2026-07-09", 11L, "组装一班", "早班", "07:00:00", "09:00:00");
        when(jdbc.query(contains("calendar_conflict_assignments"),
                any(RowMapper.class), any(Object[].class))).thenAnswer(invocation -> {
                    RowMapper<CalScheduleConflictDetector.Assignment> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(overnight, 0), mapper.mapRow(morning, 1));
                });

        CalCalendarService service = new CalCalendarService(jdbc);
        Map<String, Object> summary = service.summary("TYPE", "2026-07-01", "ZZ", null, null);

        assertEquals(27, summary.get("workdayCount"));
        assertEquals(25, summary.get("scheduledDays"));
        assertEquals(2, summary.get("unscheduledWorkdays"));
        assertEquals(1, summary.get("conflictCount"));
        assertTrue(((Number) summary.get("coverageRate")).doubleValue() > 92.5);
    }

    @Test
    void summaryCountsScheduledWorkdaysOnlyAndCapsCoverage() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.tables"), eq(Integer.class), anyString())).thenReturn(1);
        when(jdbc.queryForObject(contains("COUNT(DISTINCT ts.the_day)"), eq(Integer.class), any(Object[].class)))
                .thenReturn(99);
        when(jdbc.queryForObject(contains("COUNT(DISTINCT ctm.user_id)"), eq(Integer.class), any(Object[].class)))
                .thenReturn(0);

        CalCalendarService service = new CalCalendarService(jdbc);
        Map<String, Object> summary = service.summary("TYPE", "2026-07-01", "ZZ", null, null);

        assertEquals(27, summary.get("scheduledDays"));
        assertEquals(100.0, summary.get("coverageRate"));
        assertEquals(0, summary.get("unscheduledWorkdays"));
        verify(jdbc).queryForObject(
                contains("calendar_scheduled_workdays"), eq(Integer.class), any(Object[].class));
        verify(jdbc).queryForObject(
                contains("DAYOFWEEK(ts.the_day)"), eq(Integer.class), any(Object[].class));
    }

    @Test
    void weekUsesIsoMondayThroughSundayRange() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.tables"), eq(Integer.class), anyString())).thenReturn(1);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        CalCalendarService service = new CalCalendarService(jdbc);
        Map<String, Object> week = service.week("TYPE", "2026-07-08", "ZZ", null, null);

        assertEquals(LocalDate.of(2026, 7, 6), week.get("startDate"));
        assertEquals(LocalDate.of(2026, 7, 12), week.get("endDate"));
        assertNotNull(week.get("teams"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void weekIncludesApplicableEnabledTeamsWithoutShifts() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.tables"), eq(Integer.class), anyString())).thenReturn(1);
        ResultSet teamRow = mock(ResultSet.class);
        when(teamRow.getLong("team_id")).thenReturn(12L);
        when(teamRow.getString("team_code")).thenReturn("TEAM-12");
        when(teamRow.getString("team_name")).thenReturn("未排班组");
        when(teamRow.getString("calendar_type")).thenReturn("ZZ");
        when(jdbc.query(contains("FROM cal_team t"), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper<Map<String, Object>> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(teamRow, 0));
                });
        when(jdbc.query(contains("FROM cal_teamshift ts"), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        CalCalendarService service = new CalCalendarService(jdbc);
        Map<String, Object> week = service.week("TYPE", "2026-07-08", "ZZ", null, null);
        List<Map<String, Object>> teams = (List<Map<String, Object>>) week.get("teams");

        assertEquals(1, teams.size());
        assertEquals(12L, teams.get(0).get("teamId"));
        assertEquals("未排班组", teams.get(0).get("teamName"));
        assertEquals(Map.of(), teams.get(0).get("days"));
    }

    private ResultSet conflictRow(long planId, String planName, String day, long teamId,
            String teamName, String shiftName, String start, String end) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("plan_id")).thenReturn(planId);
        when(rs.getString("plan_name")).thenReturn(planName);
        when(rs.getDate("the_day")).thenReturn(java.sql.Date.valueOf(day));
        when(rs.getLong("team_id")).thenReturn(teamId);
        when(rs.getString("team_name")).thenReturn(teamName);
        when(rs.getLong("shift_id")).thenReturn(planId);
        when(rs.getString("shift_name")).thenReturn(shiftName);
        when(rs.getInt("order_num")).thenReturn(1);
        when(rs.getTime("start_time")).thenReturn(Time.valueOf(start));
        when(rs.getTime("end_time")).thenReturn(Time.valueOf(end));
        when(rs.getString("status")).thenReturn("CONFIRMED");
        when(rs.getString("enable_flag")).thenReturn("Y");
        when(rs.getString("calendar_type")).thenReturn("ZZ");
        return rs;
    }
}
