package com.yunshu.mes.cal.compat.service;

import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CalPlanMutationService {

    static final String CONFIRMATION_LOCK = "mes:calendar:confirmation";
    private static final int LOCK_TIMEOUT_SECONDS = 10;

    private final JdbcTemplate jdbc;
    private final CalTeamshiftService teamshiftService;

    public CalPlanMutationService(JdbcTemplate jdbc, CalTeamshiftService teamshiftService) {
        this.jdbc = jdbc;
        this.teamshiftService = teamshiftService;
    }

    @Transactional
    public Result edit(Map<String, Object> body) {
        boolean acquired = acquireConfirmationLock();
        if (!acquired) {
            return new Result(0, "排班确认繁忙，请稍后重试");
        }
        boolean releaseHere = registerReleaseAfterTransaction();
        try {
            long planId = longValue(body.get("planId"));
            Map<String, Object> persisted;
            try {
                persisted = jdbc.queryForMap("""
                        SELECT plan_id, plan_code, plan_name, calendar_type, start_date, end_date,
                               shift_type, shift_method, shift_count, status, remark, enable_flag
                        FROM cal_plan WHERE plan_id = ? FOR UPDATE
                        """, planId);
            } catch (DataAccessException ex) {
                return new Result(0, "排班计划不存在");
            }

            Map<String, Object> candidate = mergeCandidate(persisted, body);
            String newStatus = String.valueOf(candidate.get("status"));
            if ("CONFIRMED".equals(newStatus)) {
                String error = teamshiftService.validateForConfirm(planId, candidate);
                if (error != null) {
                    return new Result(0, error);
                }
            }

            int rows = jdbc.update("""
                    UPDATE cal_plan SET plan_code=?, plan_name=?, calendar_type=?, start_date=?, end_date=?,
                      shift_type=?, shift_method=?, shift_count=?, status=?, remark=?, enable_flag=?, update_time=NOW(3)
                    WHERE plan_id=?
                    """,
                    candidate.get("plan_code"), candidate.get("plan_name"), candidate.get("calendar_type"),
                    candidate.get("start_date"), candidate.get("end_date"), candidate.get("shift_type"),
                    candidate.get("shift_method"), candidate.get("shift_count"), newStatus,
                    candidate.get("remark"), candidate.get("enable_flag"), planId);

            if (rows > 0) {
                if ("CONFIRMED".equals(newStatus)) {
                    teamshiftService.genRecords(planId);
                } else if ("CONFIRMED".equals(String.valueOf(persisted.get("status")))) {
                    teamshiftService.deleteRecords(planId);
                }
            }
            return new Result(rows, null);
        } finally {
            if (releaseHere) {
                releaseConfirmationLock();
            }
        }
    }

    private boolean acquireConfirmationLock() {
        Integer acquired = jdbc.queryForObject(
                "SELECT GET_LOCK(?, ?)", Integer.class,
                CONFIRMATION_LOCK, LOCK_TIMEOUT_SECONDS);
        return acquired != null && acquired == 1;
    }

    private boolean registerReleaseAfterTransaction() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return true;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releaseConfirmationLock();
            }
        });
        return false;
    }

    private void releaseConfirmationLock() {
        try {
            jdbc.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, CONFIRMATION_LOCK);
        } catch (DataAccessException ignored) {
            // The database also releases named locks when the transaction connection closes.
        }
    }

    private Map<String, Object> mergeCandidate(Map<String, Object> persisted, Map<String, Object> body) {
        Map<String, Object> candidate = new LinkedHashMap<>(persisted);
        put(candidate, "plan_code", body, "planCode");
        put(candidate, "plan_name", body, "planName");
        put(candidate, "calendar_type", body, "calendarType");
        putDate(candidate, "start_date", body, "startDate");
        putDate(candidate, "end_date", body, "endDate");
        put(candidate, "shift_type", body, "shiftType");
        put(candidate, "shift_method", body, "shiftMethod");
        putInteger(candidate, "shift_count", body, "shiftCount");
        put(candidate, "status", body, "status");
        put(candidate, "remark", body, "remark");
        put(candidate, "enable_flag", body, "enableFlag");
        return candidate;
    }

    private void put(Map<String, Object> target, String targetKey, Map<String, Object> source, String sourceKey) {
        if (source.containsKey(sourceKey)) {
            target.put(targetKey, source.get(sourceKey));
        }
    }

    private void putDate(Map<String, Object> target, String targetKey,
            Map<String, Object> source, String sourceKey) {
        if (!source.containsKey(sourceKey)) {
            return;
        }
        Object value = source.get(sourceKey);
        target.put(targetKey, value instanceof Date ? value : Date.valueOf(String.valueOf(value).substring(0, 10)));
    }

    private void putInteger(Map<String, Object> target, String targetKey,
            Map<String, Object> source, String sourceKey) {
        if (!source.containsKey(sourceKey)) {
            return;
        }
        Object value = source.get(sourceKey);
        target.put(targetKey, value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value)));
    }

    private long longValue(Object value) {
        return value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value));
    }

    public record Result(int rows, String error) {
    }
}
