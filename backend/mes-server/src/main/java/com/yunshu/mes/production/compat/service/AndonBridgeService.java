package com.yunshu.mes.production.compat.service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Sync pro_andon_record ↔ andon_event for dashboard / line monitor. */
@Service
public class AndonBridgeService {

    private final JdbcTemplate jdbc;

    public AndonBridgeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void afterRecordCreated(Long recordId, Map<String, Object> record) {
        Long existing = bridgeEventId(record);
        if (existing != null) {
            return;
        }
        String andonNo = "AD-PRO-" + recordId;
        Long typeId = resolveTypeId(str(record.get("andonReason")));
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO andon_event (
                      andon_no, andon_type_id, reason_id,
                      work_order_id, line_id, station_id,
                      report_user_id, exception_desc, status, occur_time
                    ) VALUES (?,?,?,?,?,?,?,?,?,NOW(3))
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, andonNo);
            ps.setLong(i++, typeId);
            ps.setObject(i++, resolveReasonId(str(record.get("andonReason"))));
            ps.setObject(i++, record.get("workorderId"));
            ps.setObject(i++, resolveLineId(record.get("workstationId")));
            ps.setObject(i++, record.get("workstationId"));
            ps.setObject(i++, record.get("userId"));
            ps.setString(i++, str(record.get("andonReason")));
            ps.setString(i, mapStatus(str(record.get("status"))));
            return ps;
        }, kh);
        Number key = kh.getKey();
        if (key != null) {
            jdbc.update("UPDATE pro_andon_record SET attr1 = ? WHERE record_id = ?", key.longValue(), recordId);
        }
    }

    @Transactional
    public void afterRecordUpdated(Map<String, Object> record) {
        Long eventId = bridgeEventId(record);
        if (eventId == null) {
            afterRecordCreated(longVal(record.get("recordId")), record);
            eventId = bridgeEventId(record);
        }
        if (eventId == null) {
            return;
        }
        String status = mapStatus(str(record.get("status")));
        if ("CLOSED".equals(status)) {
            jdbc.update("UPDATE andon_event SET status='CLOSED', close_time=NOW(3) WHERE andon_id=?", eventId);
        } else if ("PROCESSING".equals(status)) {
            jdbc.update("UPDATE andon_event SET status='PROCESSING' WHERE andon_id=?", eventId);
        } else {
            jdbc.update("UPDATE andon_event SET status='OPEN', close_time=NULL WHERE andon_id=?", eventId);
        }
    }

    private Long bridgeEventId(Map<String, Object> record) {
        Object attr1 = record.get("attr1");
        if (attr1 == null || String.valueOf(attr1).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(attr1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long resolveTypeId(String reason) {
        List<Long> ids = jdbc.query("""
                SELECT andon_type_id FROM andon_type
                WHERE type_code IN ('MATERIAL_SHORTAGE','QUALITY_ABNORMAL','EQUIPMENT_FAULT','PROCESS_ABNORMAL')
                ORDER BY andon_type_id LIMIT 1
                """, (rs, n) -> rs.getLong("andon_type_id"));
        if (!ids.isEmpty()) {
            if (reason != null && (reason.contains("设备") || reason.contains("噪音"))) {
                return findTypeByCode("EQUIPMENT_FAULT").orElse(ids.get(0));
            }
            if (reason != null && (reason.contains("质量") || reason.contains("检验"))) {
                return findTypeByCode("QUALITY_ABNORMAL").orElse(ids.get(0));
            }
            if (reason != null && (reason.contains("缺") || reason.contains("料"))) {
                return findTypeByCode("MATERIAL_SHORTAGE").orElse(ids.get(0));
            }
            return ids.get(0);
        }
        return 1L;
    }

    private Optional<Long> findTypeByCode(String code) {
        List<Long> ids = jdbc.query("SELECT andon_type_id FROM andon_type WHERE type_code = ?",
                (rs, n) -> rs.getLong("andon_type_id"), code);
        return ids.stream().findFirst();
    }

    private Long resolveReasonId(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        List<Long> ids = jdbc.query("SELECT reason_id FROM andon_reason WHERE reason_name = ? OR reason_code = ? LIMIT 1",
                (rs, n) -> rs.getLong("reason_id"), reason, reason);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long resolveLineId(Object stationId) {
        if (stationId == null) {
            return null;
        }
        List<Long> ids = jdbc.query("SELECT line_id FROM workstation WHERE station_id = ?",
                (rs, n) -> rs.getLong("line_id"), stationId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static String mapStatus(String proStatus) {
        if ("HANDLED".equals(proStatus)) {
            return "CLOSED";
        }
        return "OPEN";
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static long longVal(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }
}
