package com.yunshu.mes.cal.compat.service;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalTeamshiftService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JdbcTemplate jdbc;

    public CalTeamshiftService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Validate team/shift counts before confirming a plan. Returns error message or null. */
    public String validateForConfirm(Long planId) {
        Map<String, Object> plan = loadPlan(planId);
        if (plan == null) {
            return "排班计划不存在";
        }
        return validateForConfirm(planId, plan);
    }

    public String validateForConfirm(Long planId, Map<String, Object> candidatePlan) {
        if (candidatePlan == null) {
            return "排班计划不存在";
        }
        List<Map<String, Object>> teams = loadTeams(planId);
        List<Map<String, Object>> shifts = loadShifts(planId);
        String shiftType = String.valueOf(candidatePlan.getOrDefault("shift_type", "SHIFT"));
        int teamCount = teams.size();
        int shiftCount = shifts.size();
        if (shiftCount < 1) {
            return "请先为计划配置班次";
        }
        if (teamCount < 1) {
            return "请先为计划分配班组";
        }
        if (("SHIFT_TWO".equals(shiftType) || "TWO".equals(shiftType)) && (teamCount < 2 || shiftCount < 2)) {
            return "两班倒需要至少 2 个班次和 2 个班组";
        }
        if (("SHIFT_THREE".equals(shiftType) || "THREE".equals(shiftType)) && (teamCount < 3 || shiftCount < 3)) {
            return "三班倒需要至少 3 个班次和 3 个班组";
        }
        for (int i = 0; i < shifts.size(); i++) {
            for (int j = i + 1; j < shifts.size(); j++) {
                Map<String, Object> first = shifts.get(i);
                Map<String, Object> second = shifts.get(j);
                if (CalScheduleConflictDetector.overlaps(
                        (LocalTime) first.get("startTime"), (LocalTime) first.get("endTime"),
                        (LocalTime) second.get("startTime"), (LocalTime) second.get("endTime"))) {
                    return "班次冲突：" + first.get("shiftName") + "与"
                            + second.get("shiftName") + "时间重叠";
                }
            }
        }

        LocalDate start = toLocalDate(candidatePlan.get("start_date"));
        LocalDate end = toLocalDate(candidatePlan.get("end_date"));
        if (start == null || end == null || end.isBefore(start)) {
            return "排班计划日期范围无效";
        }
        Map<LocalDate, String> calendarOverrides = loadCalendarOverrides(start, end);
        List<CalScheduleConflictDetector.Assignment> candidate =
                buildAssignments(planId, candidatePlan, teams, shifts, calendarOverrides);
        List<CalScheduleConflictDetector.Assignment> existing =
                loadExistingAssignments(planId, start, end);
        MemberIndex members = loadMemberIndex();
        return CalScheduleConflictDetector.detect(candidate, existing, members.teamMembers(), members.memberNames());
    }

    /** Seed default shifts when a plan is created. */
    @Transactional
    public void addDefaultShifts(Long planId, String shiftType) {
        if (!tableExists("cal_shift") || planId == null) {
            return;
        }
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM cal_shift WHERE plan_id = ?", Integer.class, planId);
        if (existing != null && existing > 0) {
            return;
        }
        List<String[]> defaults = new ArrayList<>();
        if ("SHIFT_THREE".equals(shiftType) || "THREE".equals(shiftType)) {
            defaults.add(new String[] {"白班", "08:00:00", "16:00:00", "1"});
            defaults.add(new String[] {"中班", "16:00:00", "00:00:00", "2"});
            defaults.add(new String[] {"夜班", "00:00:00", "08:00:00", "3"});
        } else if ("SHIFT_TWO".equals(shiftType) || "TWO".equals(shiftType)) {
            defaults.add(new String[] {"白班", "08:00:00", "20:00:00", "1"});
            defaults.add(new String[] {"夜班", "20:00:00", "08:00:00", "2"});
        } else {
            defaults.add(new String[] {"白班", "08:00:00", "17:00:00", "1"});
        }
        for (String[] s : defaults) {
            jdbc.update("""
                    INSERT INTO cal_shift (plan_id, shift_name, start_time, end_time, order_num, create_time)
                    VALUES (?, ?, ?, ?, ?, NOW(3))
                    """, planId, s[0], s[1], s[2], Integer.parseInt(s[3]));
        }
    }

    @Transactional
    public void genRecords(Long planId) {
        if (!tableExists("cal_teamshift")) {
            return;
        }
        Map<String, Object> plan = loadPlan(planId);
        if (plan == null) {
            return;
        }
        List<Map<String, Object>> shifts = loadShifts(planId);
        List<Map<String, Object>> teams = loadTeams(planId);

        if (shifts.isEmpty() || teams.isEmpty()) {
            return;
        }

        LocalDate start = toLocalDate(plan.get("start_date"));
        LocalDate end = toLocalDate(plan.get("end_date"));
        if (start == null || end == null) {
            start = LocalDate.now().withDayOfMonth(1);
            end = start.plusMonths(1).minusDays(1);
        }
        List<CalScheduleConflictDetector.Assignment> assignments =
                buildAssignments(planId, plan, teams, shifts, loadCalendarOverrides(start, end));
        deleteRecords(planId);
        for (CalScheduleConflictDetector.Assignment assignment : assignments) {
            insertShift(assignment, String.valueOf(plan.get("calendar_type")),
                    String.valueOf(plan.get("shift_type")));
        }
    }

    public void deleteRecords(Long planId) {
        jdbc.update("DELETE FROM cal_teamshift WHERE plan_id = ?", planId);
    }

    private List<CalScheduleConflictDetector.Assignment> buildAssignments(Long planId,
            Map<String, Object> plan, List<Map<String, Object>> teams,
            List<Map<String, Object>> shifts, Map<LocalDate, String> calendarOverrides) {
        List<CalScheduleConflictDetector.Assignment> result = new ArrayList<>();
        LocalDate start = toLocalDate(plan.get("start_date"));
        LocalDate end = toLocalDate(plan.get("end_date"));
        String shiftType = String.valueOf(plan.getOrDefault("shift_type", "SHIFT"));
        String shiftMethod = plan.get("shift_method") == null ? "DAY" : String.valueOf(plan.get("shift_method"));
        int rotateEvery = plan.get("shift_count") instanceof Number n ? n.intValue() : 1;
        if (rotateEvery < 1) {
            rotateEvery = 1;
        }
        int shiftIndex = 0;
        int dayIndex = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1), dayIndex++) {
            if (!CalWorkdayPolicy.isWorkday(day, calendarOverrides)) {
                continue;
            }
            if (shouldRotate(day, start, dayIndex, shiftMethod, rotateEvery)) {
                shiftIndex++;
            }
            addDayAssignments(result, planId, plan, day, teams, shifts, shiftType, shiftIndex);
        }
        return result;
    }

    private boolean shouldRotate(LocalDate day, LocalDate planStart, int dayIndex, String method, int rotateEvery) {
        if (dayIndex == 0) {
            return false;
        }
        String m = method == null ? "DAY" : method.toUpperCase();
        if ("QUARTER".equals(m) || "CAL_SHIFT_METHOD_QUARTER".equals(m)) {
            return day.equals(day.with(IsoFields.DAY_OF_QUARTER, 1))
                    && !sameQuarter(day, planStart);
        }
        if ("MONTH".equals(m) || "CAL_SHIFT_METHOD_MONTH".equals(m)) {
            return day.getDayOfMonth() == 1 && !yearMonthKey(day).equals(yearMonthKey(planStart));
        }
        if ("WEEK".equals(m) || "CAL_SHIFT_METHOD_WEEK".equals(m)) {
            return day.getDayOfWeek() == DayOfWeek.MONDAY
                    && !weekStart(day).equals(weekStart(planStart));
        }
        // DAY / default
        return dayIndex % rotateEvery == 0;
    }

    private static String yearMonthKey(LocalDate d) {
        return d.getYear() + "-" + d.getMonthValue();
    }

    private static boolean sameQuarter(LocalDate a, LocalDate b) {
        return a.get(IsoFields.QUARTER_OF_YEAR) == b.get(IsoFields.QUARTER_OF_YEAR) && a.getYear() == b.getYear();
    }

    private static LocalDate weekStart(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private void addDayAssignments(List<CalScheduleConflictDetector.Assignment> result, Long planId,
            Map<String, Object> plan, LocalDate day, List<Map<String, Object>> teams,
            List<Map<String, Object>> shifts, String shiftType, int shiftIndex) {
        if ("SHIFT".equals(shiftType) || "SHIFT_SINGLE".equals(shiftType) || "SINGLE".equals(shiftType)) {
            result.add(toAssignment(planId, plan, day, teams.get(0), shifts.get(0)));
            return;
        }
        if (("SHIFT_TWO".equals(shiftType) || "TWO".equals(shiftType)) && teams.size() >= 2 && shifts.size() >= 2) {
            if (shiftIndex % 2 == 0) {
                result.add(toAssignment(planId, plan, day, teams.get(0), shifts.get(0)));
                result.add(toAssignment(planId, plan, day, teams.get(1), shifts.get(1)));
            } else {
                result.add(toAssignment(planId, plan, day, teams.get(0), shifts.get(1)));
                result.add(toAssignment(planId, plan, day, teams.get(1), shifts.get(0)));
            }
            return;
        }
        if (("SHIFT_THREE".equals(shiftType) || "THREE".equals(shiftType)) && teams.size() >= 3 && shifts.size() >= 3) {
            int phase = shiftIndex % 3;
            // Rotate team→shift mapping: team i gets shift (i+phase)%3
            for (int i = 0; i < 3; i++) {
                int shiftPos = (i + phase) % 3;
                result.add(toAssignment(planId, plan, day, teams.get(i), shifts.get(shiftPos)));
            }
            return;
        }
        result.add(toAssignment(planId, plan, day, teams.get(0), shifts.get(0)));
    }

    private CalScheduleConflictDetector.Assignment toAssignment(Long planId, Map<String, Object> plan,
            LocalDate day, Map<String, Object> team, Map<String, Object> shift) {
        return new CalScheduleConflictDetector.Assignment(
                planId,
                String.valueOf(plan.getOrDefault("plan_name", "计划" + planId)),
                day,
                ((Number) team.get("teamId")).longValue(),
                String.valueOf(team.get("teamName")),
                ((Number) shift.get("shiftId")).longValue(),
                String.valueOf(shift.get("shiftName")),
                ((Number) shift.get("orderNum")).intValue(),
                (LocalTime) shift.get("startTime"),
                (LocalTime) shift.get("endTime"),
                String.valueOf(plan.getOrDefault("status", "CONFIRMED")),
                String.valueOf(plan.getOrDefault("enable_flag", "Y")),
                String.valueOf(plan.get("calendar_type")));
    }

    private void insertShift(CalScheduleConflictDetector.Assignment assignment,
            String calendarType, String shiftType) {
        jdbc.update("""
                INSERT INTO cal_teamshift (the_day, team_id, team_name, shift_id, shift_name, order_num,
                  plan_id, calendar_type, shift_type, create_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(3))
                """,
                assignment.day().format(DAY_FMT),
                assignment.teamId(),
                assignment.teamName(),
                assignment.shiftId(),
                assignment.shiftName(),
                assignment.orderNum(),
                assignment.planId(),
                calendarType,
                shiftType);
    }

    private List<Map<String, Object>> loadShifts(Long planId) {
        return jdbc.query("""
                SELECT shift_id, shift_name, order_num, start_time, end_time
                FROM cal_shift WHERE plan_id = ? ORDER BY order_num, shift_id
                """, (rs, n) -> {
            Map<String, Object> shift = new LinkedHashMap<>();
            shift.put("shiftId", rs.getLong("shift_id"));
            shift.put("shiftName", rs.getString("shift_name"));
            shift.put("orderNum", rs.getInt("order_num"));
            shift.put("startTime", rs.getTime("start_time").toLocalTime());
            shift.put("endTime", rs.getTime("end_time").toLocalTime());
            return shift;
        }, planId);
    }

    private List<Map<String, Object>> loadTeams(Long planId) {
        return jdbc.query("""
                SELECT team_id, team_code, team_name FROM cal_plan_team
                WHERE plan_id = ? ORDER BY record_id
                """, (rs, n) -> {
            Map<String, Object> team = new LinkedHashMap<>();
            team.put("teamId", rs.getLong("team_id"));
            team.put("teamName", rs.getString("team_name"));
            return team;
        }, planId);
    }

    private Map<LocalDate, String> loadCalendarOverrides(LocalDate start, LocalDate end) {
        if (!tableExists("cal_holiday")) {
            return Map.of();
        }
        Map<LocalDate, String> result = new HashMap<>();
        jdbc.query("""
                SELECT the_day, holiday_type FROM cal_holiday
                WHERE the_day BETWEEN ? AND ?
                """, rs -> {
            while (rs.next()) {
                result.put(rs.getDate("the_day").toLocalDate(), rs.getString("holiday_type"));
            }
            return null;
        }, Date.valueOf(start), Date.valueOf(end));
        return result;
    }

    private List<CalScheduleConflictDetector.Assignment> loadExistingAssignments(
            Long planId, LocalDate start, LocalDate end) {
        return jdbc.query("""
                SELECT ts.plan_id, p.plan_name, ts.the_day, ts.team_id, ts.team_name,
                       ts.shift_id, ts.shift_name, ts.order_num, s.start_time, s.end_time,
                       p.status, p.enable_flag, ts.calendar_type
                FROM cal_teamshift ts
                JOIN cal_plan p ON p.plan_id = ts.plan_id
                  AND p.status = 'CONFIRMED' AND p.enable_flag = 'Y'
                JOIN cal_shift s ON s.shift_id = ts.shift_id
                WHERE ts.plan_id <> ? AND ts.the_day BETWEEN ? AND ?
                ORDER BY ts.the_day, ts.team_id, ts.order_num
                """, (rs, n) -> new CalScheduleConflictDetector.Assignment(
                        rs.getLong("plan_id"),
                        rs.getString("plan_name"),
                        LocalDate.parse(rs.getString("the_day"), DAY_FMT),
                        rs.getLong("team_id"),
                        rs.getString("team_name"),
                        rs.getLong("shift_id"),
                        rs.getString("shift_name"),
                        rs.getInt("order_num"),
                        rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime(),
                        rs.getString("status"),
                        rs.getString("enable_flag"),
                        rs.getString("calendar_type")),
                planId, start.minusDays(1).format(DAY_FMT), end.plusDays(1).format(DAY_FMT));
    }

    private MemberIndex loadMemberIndex() {
        Map<Long, Set<Long>> teamMembers = new HashMap<>();
        Map<Long, String> memberNames = new HashMap<>();
        if (!tableExists("cal_team_member")) {
            return new MemberIndex(teamMembers, memberNames);
        }
        jdbc.query("""
                SELECT team_id, user_id, user_name, nick_name FROM cal_team_member
                """, rs -> {
            while (rs.next()) {
                long teamId = rs.getLong("team_id");
                long userId = rs.getLong("user_id");
                teamMembers.computeIfAbsent(teamId, ignored -> new HashSet<>()).add(userId);
                String nickName = rs.getString("nick_name");
                memberNames.putIfAbsent(userId,
                        nickName == null || nickName.isBlank() ? rs.getString("user_name") : nickName);
            }
            return null;
        });
        return new MemberIndex(teamMembers, memberNames);
    }

    private Map<String, Object> loadPlan(Long planId) {
        try {
            return jdbc.queryForMap("""
                    SELECT plan_id, plan_name, calendar_type, shift_type, shift_method, shift_count,
                           start_date, end_date, status, enable_flag
                    FROM cal_plan WHERE plan_id = ?
                    """, planId);
        } catch (DataAccessException ex) {
            return null;
        }
    }

    private LocalDate toLocalDate(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDate ld) {
            return ld;
        }
        if (v instanceof Date d) {
            return d.toLocalDate();
        }
        if (v instanceof java.util.Date d) {
            return new Date(d.getTime()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(v).substring(0, 10));
    }

    private boolean tableExists(String table) {
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

    private record MemberIndex(Map<Long, Set<Long>> teamMembers, Map<Long, String> memberNames) {
    }
}
