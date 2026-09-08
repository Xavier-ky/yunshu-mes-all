package com.yunshu.mes.cal.compat.service;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Service;

@Service
public class CalCalendarService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JdbcTemplate jdbc;
    private final Map<String, Boolean> tableAvailability = new ConcurrentHashMap<>();

    public CalCalendarService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(String queryType, String dateStr, String calendarType, Long teamId, Long userId) {
        requireValidQuery(queryType, calendarType, teamId, userId);
        LocalDate anchor = parseDate(dateStr);
        YearMonth ym = YearMonth.from(anchor);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Map<LocalDate, String> calendarOverrides = loadHolidayTypes(start, end);

        List<Map<String, Object>> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> cal = new LinkedHashMap<>();
            String theDay = d.format(DAY_FMT);
            cal.put("theDay", theDay);
            cal.put("workday", CalWorkdayPolicy.isWorkday(d, calendarOverrides));
            cal.put("holidayType", CalWorkdayPolicy.displayType(d, calendarOverrides));
            List<Map<String, Object>> teamShifts = loadTeamShifts(theDay, queryType, calendarType, teamId, userId);
            cal.put("teamShifts", teamShifts);
            if (!teamShifts.isEmpty()) {
                cal.put("shiftType", teamShifts.get(0).get("shiftType"));
            }
            days.add(cal);
        }
        return days;
    }

    private List<Map<String, Object>> loadTeamShifts(String theDay, String queryType, String calendarType,
            Long teamId, Long userId) {
        if (!tableExists("cal_teamshift")) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder("""
                SELECT ts.record_id, ts.the_day, ts.team_id, ts.team_name, ts.shift_id, ts.shift_name,
                       ts.order_num, ts.plan_id, ts.calendar_type, ts.shift_type,
                       s.start_time AS shift_start_time, s.end_time AS shift_end_time,
                       p.plan_name, COALESCE(mc.member_count, 0) AS member_count
                FROM cal_teamshift ts
                LEFT JOIN cal_shift s ON s.shift_id = ts.shift_id
                LEFT JOIN cal_plan p ON p.plan_id = ts.plan_id
                LEFT JOIN (
                  SELECT team_id, COUNT(DISTINCT user_id) AS member_count
                  FROM cal_team_member GROUP BY team_id
                ) mc ON mc.team_id = ts.team_id
                WHERE ts.the_day = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(theDay);

        appendFilter(sql, args, queryType, calendarType, teamId, userId, "ts");
        sql.append(" ORDER BY ts.order_num, ts.record_id");

        return jdbc.query(sql.toString(), (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("recordId", rs.getLong("record_id"));
            m.put("theDay", rs.getString("the_day"));
            m.put("teamId", rs.getLong("team_id"));
            m.put("teamName", rs.getString("team_name"));
            m.put("shiftId", rs.getLong("shift_id"));
            m.put("shiftName", rs.getString("shift_name"));
            m.put("orderNum", rs.getObject("order_num"));
            m.put("planId", rs.getObject("plan_id"));
            m.put("calendarType", rs.getString("calendar_type"));
            m.put("shiftType", rs.getString("shift_type"));
            m.put("shiftStartTime", rs.getTime("shift_start_time"));
            m.put("shiftEndTime", rs.getTime("shift_end_time"));
            m.put("planName", rs.getString("plan_name"));
            m.put("memberCount", rs.getInt("member_count"));
            return m;
        }, args.toArray());
    }

    public Map<String, Object> summary(String queryType, String dateStr, String calendarType,
            Long teamId, Long userId) {
        requireValidQuery(queryType, calendarType, teamId, userId);
        LocalDate anchor = parseDate(dateStr);
        YearMonth month = YearMonth.from(anchor);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        Map<LocalDate, String> exceptions = loadHolidayTypes(start, end);
        int workdayCount = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (CalWorkdayPolicy.isWorkday(day, exceptions)) {
                workdayCount++;
            }
        }

        int scheduledDays = 0;
        int todayOnDutyCount = 0;
        int conflictCount = 0;
        if (tableExists("cal_teamshift")) {
            List<Object> args = new ArrayList<>(List.of(start.format(DAY_FMT), end.format(DAY_FMT)));
            boolean hasHolidayTable = tableExists("cal_holiday");
            StringBuilder scheduledSql = new StringBuilder("""
                    SELECT /* calendar_scheduled_workdays */ COUNT(DISTINCT ts.the_day)
                    FROM cal_teamshift ts
                    """);
            if (hasHolidayTable) {
                scheduledSql.append(" LEFT JOIN cal_holiday h ON h.the_day = ts.the_day\n");
            }
            scheduledSql.append(" WHERE ts.the_day BETWEEN ? AND ?\n");
            if (hasHolidayTable) {
                scheduledSql.append("""
                         AND (
                           UPPER(COALESCE(h.holiday_type, '')) = 'WORKDAY'
                           OR (
                             DAYOFWEEK(ts.the_day) <> 1
                             AND UPPER(COALESCE(h.holiday_type, '')) <> 'HOLIDAY'
                           )
                         )
                        """);
            } else {
                scheduledSql.append(" AND DAYOFWEEK(ts.the_day) <> 1\n");
            }
            appendFilter(scheduledSql, args, queryType, calendarType, teamId, userId, "ts");
            int reportedScheduledDays = intValue(jdbc.queryForObject(
                    scheduledSql.toString(), Integer.class, args.toArray()));
            scheduledDays = Math.min(workdayCount, Math.max(0, reportedScheduledDays));

            List<Object> todayArgs = new ArrayList<>(List.of(LocalDate.now().format(DAY_FMT)));
            StringBuilder todaySql = new StringBuilder("""
                    SELECT COUNT(DISTINCT ctm.user_id) FROM cal_teamshift ts
                    JOIN cal_team_member ctm ON ctm.team_id = ts.team_id
                    WHERE ts.the_day = ?
                    """);
            appendFilter(todaySql, todayArgs, queryType, calendarType, teamId, userId, "ts");
            todayOnDutyCount = intValue(jdbc.queryForObject(
                    todaySql.toString(), Integer.class, todayArgs.toArray()));

            conflictCount = countConflicts(
                    start, end, queryType, calendarType, teamId, userId);
        }

        int unscheduled = Math.max(0, workdayCount - scheduledDays);
        double coverage = workdayCount == 0 ? 100.0
                : Math.min(100.0, Math.round(scheduledDays * 1000.0 / workdayCount) / 10.0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month.toString());
        result.put("workdayCount", workdayCount);
        result.put("scheduledDays", scheduledDays);
        result.put("coverageRate", coverage);
        result.put("unscheduledWorkdays", unscheduled);
        result.put("conflictCount", conflictCount);
        result.put("todayOnDutyCount", todayOnDutyCount);
        return result;
    }

    private int countConflicts(LocalDate start, LocalDate end, String queryType,
            String calendarType, Long teamId, Long userId) {
        List<Object> args = new ArrayList<>(List.of(
                start.minusDays(1).format(DAY_FMT), end.plusDays(1).format(DAY_FMT)));
        StringBuilder sql = new StringBuilder("""
                SELECT /* calendar_conflict_assignments */
                       ts.plan_id, p.plan_name, ts.the_day, ts.team_id, ts.team_name,
                       ts.shift_id, ts.shift_name, ts.order_num, s.start_time, s.end_time,
                       p.status, p.enable_flag, ts.calendar_type
                FROM cal_teamshift ts
                JOIN cal_plan p ON p.plan_id = ts.plan_id
                JOIN cal_shift s ON s.shift_id = ts.shift_id
                WHERE ts.the_day BETWEEN ? AND ?
                  AND p.status = 'CONFIRMED' AND p.enable_flag = 'Y'
                """);
        sql.append(" ORDER BY ts.the_day, ts.team_id, ts.order_num");
        List<CalScheduleConflictDetector.Assignment> assignments = jdbc.query(
                sql.toString(), (rs, n) -> new CalScheduleConflictDetector.Assignment(
                        rs.getLong("plan_id"),
                        rs.getString("plan_name"),
                        rs.getDate("the_day").toLocalDate(),
                        rs.getLong("team_id"),
                        rs.getString("team_name"),
                        rs.getLong("shift_id"),
                        rs.getString("shift_name"),
                        rs.getInt("order_num"),
                        localTime(rs.getTime("start_time")),
                        localTime(rs.getTime("end_time")),
                        rs.getString("status"),
                        rs.getString("enable_flag"),
                        rs.getString("calendar_type")),
                args.toArray());
        Map<Long, Set<Long>> teamMembers = loadTeamMembers();
        return CalScheduleConflictDetector.countConflicts(
                assignments, teamMembers, start, end,
                assignment -> matchesQueryScope(
                        assignment, teamMembers, queryType, calendarType, teamId, userId));
    }

    private boolean matchesQueryScope(CalScheduleConflictDetector.Assignment assignment,
            Map<Long, Set<Long>> teamMembers, String queryType, String calendarType,
            Long teamId, Long userId) {
        return switch (queryType.toUpperCase()) {
            case "TYPE" -> calendarType.equals(assignment.calendarType());
            case "TEAM" -> teamId.equals(assignment.teamId());
            case "USER" -> teamMembers.getOrDefault(assignment.teamId(), Set.of()).contains(userId);
            default -> false;
        };
    }

    private Map<Long, Set<Long>> loadTeamMembers() {
        Map<Long, Set<Long>> members = new HashMap<>();
        if (!tableExists("cal_team_member")) {
            return members;
        }
        jdbc.query("SELECT team_id, user_id FROM cal_team_member", rs -> {
            while (rs.next()) {
                members.computeIfAbsent(rs.getLong("team_id"), ignored -> new HashSet<>())
                        .add(rs.getLong("user_id"));
            }
            return null;
        });
        return members;
    }

    private LocalTime localTime(java.sql.Time value) {
        return value == null ? LocalTime.MIDNIGHT : value.toLocalTime();
    }

    public Map<String, Object> day(String queryType, String dateStr, String calendarType,
            Long teamId, Long userId) {
        requireValidQuery(queryType, calendarType, teamId, userId);
        LocalDate date = parseDate(dateStr);
        String day = date.format(DAY_FMT);
        List<Map<String, Object>> shifts = loadTeamShifts(day, queryType, calendarType, teamId, userId);
        for (Map<String, Object> shift : shifts) {
            shift.put("members", loadMembers(((Number) shift.get("teamId")).longValue()));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        Map<LocalDate, String> calendarOverrides = loadHolidayTypes(date, date);
        result.put("theDay", day);
        result.put("dayOfWeek", date.getDayOfWeek().name());
        result.put("workday", CalWorkdayPolicy.isWorkday(date, calendarOverrides));
        result.put("holidayType", CalWorkdayPolicy.displayType(date, calendarOverrides));
        result.put("teamShifts", shifts);
        return result;
    }

    public Map<String, Object> week(String queryType, String dateStr, String calendarType,
            Long teamId, Long userId) {
        requireValidQuery(queryType, calendarType, teamId, userId);
        LocalDate anchor = parseDate(dateStr);
        LocalDate start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(6);
        Map<LocalDate, String> calendarOverrides = loadHolidayTypes(start, end);
        Map<String, Map<String, Object>> dayTypes = new LinkedHashMap<>();
        Map<Long, Map<String, Object>> teams = new LinkedHashMap<>();
        for (Map<String, Object> applicableTeam
                : loadApplicableTeams(queryType, calendarType, teamId, userId)) {
            Long id = ((Number) applicableTeam.get("teamId")).longValue();
            Map<String, Object> row = new LinkedHashMap<>(applicableTeam);
            row.put("days", new LinkedHashMap<String, List<Map<String, Object>>>());
            teams.put(id, row);
        }
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String day = date.format(DAY_FMT);
            Map<String, Object> dayType = new LinkedHashMap<>();
            dayType.put("workday", CalWorkdayPolicy.isWorkday(date, calendarOverrides));
            dayType.put("holidayType", CalWorkdayPolicy.displayType(date, calendarOverrides));
            dayTypes.put(day, dayType);
            for (Map<String, Object> shift : loadTeamShifts(day, queryType, calendarType, teamId, userId)) {
                Long id = ((Number) shift.get("teamId")).longValue();
                Map<String, Object> team = teams.computeIfAbsent(id, ignored -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("teamId", id);
                    row.put("teamName", shift.get("teamName"));
                    row.put("calendarType", shift.get("calendarType"));
                    row.put("days", new LinkedHashMap<String, List<Map<String, Object>>>());
                    return row;
                });
                @SuppressWarnings("unchecked")
                Map<String, List<Map<String, Object>>> days =
                        (Map<String, List<Map<String, Object>>>) team.get("days");
                days.computeIfAbsent(day, ignored -> new ArrayList<>()).add(shift);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", start);
        result.put("endDate", end);
        result.put("dayTypes", dayTypes);
        result.put("teams", new ArrayList<>(teams.values()));
        return result;
    }

    private List<Map<String, Object>> loadApplicableTeams(String queryType, String calendarType,
            Long teamId, Long userId) {
        if (!tableExists("cal_team")) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder("""
                SELECT t.team_id, t.team_code, t.team_name, t.calendar_type
                FROM cal_team t
                WHERE (t.enable_flag = 'Y' OR t.enable_flag IS NULL)
                """);
        List<Object> args = new ArrayList<>();
        if ("TYPE".equalsIgnoreCase(queryType) && calendarType != null && !calendarType.isBlank()) {
            sql.append(" AND t.calendar_type = ?");
            args.add(calendarType);
        } else if ("TEAM".equalsIgnoreCase(queryType) && teamId != null) {
            sql.append(" AND t.team_id = ?");
            args.add(teamId);
        } else if ("USER".equalsIgnoreCase(queryType) && userId != null) {
            sql.append("""
                     AND EXISTS (
                       SELECT 1 FROM cal_team_member filter_member
                       WHERE filter_member.team_id = t.team_id AND filter_member.user_id = ?
                     )
                    """);
            args.add(userId);
        }
        sql.append(" ORDER BY t.team_id");
        return jdbc.query(sql.toString(), (rs, n) -> {
            Map<String, Object> team = new LinkedHashMap<>();
            team.put("teamId", rs.getLong("team_id"));
            team.put("teamCode", rs.getString("team_code"));
            team.put("teamName", rs.getString("team_name"));
            team.put("calendarType", rs.getString("calendar_type"));
            return team;
        }, args.toArray());
    }

    private List<Map<String, Object>> loadMembers(Long teamId) {
        if (!tableExists("cal_team_member")) {
            return List.of();
        }
        return jdbc.query("""
                SELECT member_id, user_id, user_name, nick_name, tel
                FROM cal_team_member WHERE team_id = ? ORDER BY member_id
                """, (rs, n) -> {
            Map<String, Object> member = new LinkedHashMap<>();
            member.put("memberId", rs.getLong("member_id"));
            member.put("userId", rs.getLong("user_id"));
            member.put("userName", rs.getString("user_name"));
            member.put("nickName", rs.getString("nick_name"));
            member.put("tel", rs.getString("tel"));
            return member;
        }, teamId);
    }

    public List<String> listHolidayDays() {
        if (!tableExists("cal_holiday")) {
            return List.of();
        }
        return jdbc.query("""
                SELECT DATE_FORMAT(the_day, '%Y-%m-%d') AS the_day FROM cal_holiday
                WHERE holiday_type = 'HOLIDAY' OR holiday_type IS NULL
                """, (rs, n) -> rs.getString("the_day"));
    }

    private Map<LocalDate, String> loadHolidayTypes(LocalDate start, LocalDate end) {
        Map<LocalDate, String> result = new LinkedHashMap<>();
        if (!tableExists("cal_holiday")) {
            return result;
        }
        jdbc.query("""
                SELECT the_day, holiday_type FROM cal_holiday
                WHERE the_day BETWEEN ? AND ?
                """, (PreparedStatementSetter) ps -> {
            ps.setDate(1, java.sql.Date.valueOf(start));
            ps.setDate(2, java.sql.Date.valueOf(end));
        }, rs -> {
            while (rs.next()) {
                result.put(rs.getDate("the_day").toLocalDate(), rs.getString("holiday_type"));
            }
            return null;
        });
        return result;
    }

    private void appendFilter(StringBuilder sql, List<Object> args, String queryType,
            String calendarType, Long teamId, Long userId, String alias) {
        if ("TYPE".equalsIgnoreCase(queryType) && calendarType != null && !calendarType.isBlank()) {
            sql.append(" AND ").append(alias).append(".calendar_type = ?");
            args.add(calendarType);
        } else if ("TEAM".equalsIgnoreCase(queryType) && teamId != null) {
            sql.append(" AND ").append(alias).append(".team_id = ?");
            args.add(teamId);
        } else if ("USER".equalsIgnoreCase(queryType) && userId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM cal_team_member filter_member")
                    .append(" WHERE filter_member.team_id = ").append(alias)
                    .append(".team_id AND filter_member.user_id = ?)");
            args.add(userId);
        }
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    public static void requireValidQuery(String queryType, String calendarType,
            Long teamId, Long userId) {
        String normalized = queryType == null ? "" : queryType.trim().toUpperCase();
        switch (normalized) {
            case "TYPE" -> {
                if (calendarType == null || calendarType.isBlank()) {
                    throw new IllegalArgumentException("TYPE查询必须提供calendarType");
                }
            }
            case "TEAM" -> {
                if (teamId == null) {
                    throw new IllegalArgumentException("TEAM查询必须提供teamId");
                }
            }
            case "USER" -> {
                if (userId == null) {
                    throw new IllegalArgumentException("USER查询必须提供userId");
                }
            }
            default -> throw new IllegalArgumentException("不支持的queryType: " + queryType);
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        String s = dateStr.trim();
        if (s.length() >= 10) {
            s = s.substring(0, 10);
        }
        return LocalDate.parse(s, DAY_FMT);
    }

    private boolean tableExists(String table) {
        return tableAvailability.computeIfAbsent(table, this::queryTableExists);
    }

    private boolean queryTableExists(String table) {
        try {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = ?
                    """, Integer.class, table);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }
}
