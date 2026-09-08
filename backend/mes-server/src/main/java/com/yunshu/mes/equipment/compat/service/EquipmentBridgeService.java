package com.yunshu.mes.equipment.compat.service;

import com.yunshu.mes.production.compat.repository.ProAndonRecordRepository;
import com.yunshu.mes.production.compat.service.AndonBridgeService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Links dv_* equipment with production dispatch, andon, and workstation bindings. */
@Service
public class EquipmentBridgeService {

    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final JdbcTemplate jdbc;
    private final ProAndonRecordRepository andonRecordRepo;
    private final AndonBridgeService andonBridge;

    public EquipmentBridgeService(
            JdbcTemplate jdbc,
            ProAndonRecordRepository andonRecordRepo,
            AndonBridgeService andonBridge) {
        this.jdbc = jdbc;
        this.andonRecordRepo = andonRecordRepo;
        this.andonBridge = andonBridge;
    }

    @Transactional
    public void afterRepairSaved(Long repairId, Map<String, Object> repair) {
        if (repairId == null) {
            return;
        }
        Long machineryId = longObj(repair.get("machineryId"));
        if (machineryId == null) {
            return;
        }
        String status = str(repair.get("status"));
        if ("FINISHED".equals(status)) {
            jdbc.update("UPDATE dv_machinery SET status = 'WORKING', update_time = NOW(3) WHERE machinery_id = ?",
                    machineryId);
            resumeDispatchForMachinery(machineryId);
            closeLinkedAndonIfReady(repairId);
        } else if ("PREPARE".equals(status) || "CONFIRMED".equals(status) || status == null || status.isBlank()) {
            jdbc.update("UPDATE dv_machinery SET status = 'REPAIR', update_time = NOW(3) WHERE machinery_id = ?",
                    machineryId);
            pauseDispatchForMachinery(machineryId);
        }
    }

    @Transactional
    public Long afterAndonCreated(Long recordId, Map<String, Object> record) {
        if (recordId == null || !isEquipmentAndon(str(record.get("andonReason")))) {
            return null;
        }
        return createRepairFromAndonRecord(recordId, record, true);
    }

    @Transactional
    public Long createRepairFromAndonRecord(Long recordId, boolean auto) {
        return andonRecordRepo.findById(recordId)
                .map(rec -> createRepairFromAndonRecord(recordId, rec, auto))
                .orElse(null);
    }

    @Transactional
    public Long createRepairFromAndonRecord(Long recordId, Map<String, Object> record, boolean auto) {
        if (existingRepairId(record) != null) {
            return existingRepairId(record);
        }
        Long stationId = longObj(record.get("workstationId"));
        Map<String, Object> machinery = findPrimaryMachineryByStation(stationId).orElse(null);
        if (machinery == null && !auto) {
            machinery = findAnyMachineryForEquipmentAndon(stationId).orElse(null);
        }
        if (machinery == null) {
            return null;
        }
        Long machineryId = longObj(machinery.get("machineryId"));
        String repairCode = "REP-AD-" + recordId + "-" + CODE_FMT.format(LocalDateTime.now());
        if (repairCodeExists(repairCode)) {
            repairCode = repairCode + "-" + System.currentTimeMillis() % 1000;
        }
        String reason = str(record.get("andonReason"));
        String repairName = StringUtils.hasText(reason) ? reason : "安灯设备维修";
        String remark = "来源安灯 recordId=" + recordId;
        if (StringUtils.hasText(str(record.get("remark")))) {
            remark = remark + "；" + str(record.get("remark"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("repairCode", repairCode);
        body.put("repairName", repairName);
        body.put("machineryId", machineryId);
        body.put("machineryCode", machinery.get("machineryCode"));
        body.put("machineryName", machinery.get("machineryName"));
        body.put("machineryBrand", machinery.get("machineryBrand"));
        body.put("machinerySpec", machinery.get("machinerySpec"));
        body.put("machineryTypeId", machinery.get("machineryTypeId"));
        body.put("requireDate", Timestamp.valueOf(LocalDateTime.now()));
        body.put("sourceDocType", "ANDON");
        body.put("sourceDocId", recordId);
        body.put("sourceDocCode", "AD-PRO-" + recordId);
        body.put("status", "PREPARE");
        body.put("remark", remark);
        body.put("createBy", str(record.get("userName")));

        Long repairId = insertRepair(body);
        if (repairId == null) {
            return null;
        }
        jdbc.update("UPDATE pro_andon_record SET attr2 = ? WHERE record_id = ?", String.valueOf(repairId), recordId);
        body.put("repairId", repairId);
        afterRepairSaved(repairId, body);
        return repairId;
    }

    public Optional<Map<String, Object>> findPrimaryMachineryByStation(Long stationId) {
        if (stationId == null) {
            return Optional.empty();
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT m.machinery_id AS machineryId, m.machinery_code AS machineryCode,
                       m.machinery_name AS machineryName, m.machinery_brand AS machineryBrand,
                       m.machinery_spec AS machinerySpec, m.machinery_type_id AS machineryTypeId,
                       m.status, wsm.workstation_id AS workstationId
                FROM md_workstation_machine wsm
                JOIN dv_machinery m ON m.machinery_id = wsm.machinery_id
                WHERE wsm.workstation_id = ?
                ORDER BY CASE WHEN m.status IN ('REPAIR','STOP') THEN 0 ELSE 1 END, wsm.record_id
                LIMIT 1
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("machineryId", rs.getLong("machineryId"));
            row.put("machineryCode", rs.getString("machineryCode"));
            row.put("machineryName", rs.getString("machineryName"));
            row.put("machineryBrand", rs.getString("machineryBrand"));
            row.put("machinerySpec", rs.getString("machinerySpec"));
            row.put("machineryTypeId", rs.getObject("machineryTypeId"));
            row.put("status", rs.getString("status"));
            row.put("workstationId", rs.getLong("workstationId"));
            return row;
        }, stationId);
        return rows.stream().findFirst();
    }

    public Optional<Map<String, Object>> findStationContextByMachinery(Long machineryId) {
        if (machineryId == null) {
            return Optional.empty();
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT wsm.workstation_id AS workstationId, ws.station_code AS stationCode,
                       ws.station_name AS stationName, pl.line_id AS lineId, pl.line_name AS lineName,
                       dt.dispatch_id AS dispatchId, dt.dispatch_no AS dispatchNo,
                       wo.work_order_id AS workOrderId, wo.work_order_no AS workOrderNo,
                       p.product_name AS productName
                FROM md_workstation_machine wsm
                JOIN workstation ws ON ws.station_id = wsm.workstation_id
                LEFT JOIN production_line pl ON pl.line_id = ws.line_id
                LEFT JOIN dispatch_task dt ON dt.station_id = ws.station_id
                    AND dt.status IN ('RUNNING','DISPATCHED','PAUSED','CREATED')
                LEFT JOIN work_order wo ON wo.work_order_id = dt.work_order_id
                LEFT JOIN product p ON p.product_id = wo.product_id
                WHERE wsm.machinery_id = ?
                ORDER BY dt.dispatch_id DESC
                LIMIT 1
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workstationId", rs.getObject("workstationId"));
            row.put("stationCode", rs.getString("stationCode"));
            row.put("stationName", rs.getString("stationName"));
            row.put("lineId", rs.getObject("lineId"));
            row.put("lineName", rs.getString("lineName"));
            row.put("dispatchId", rs.getObject("dispatchId"));
            row.put("dispatchNo", rs.getString("dispatchNo"));
            row.put("workOrderId", rs.getObject("workOrderId"));
            row.put("workOrderNo", rs.getString("workOrderNo"));
            row.put("productName", rs.getString("productName"));
            return row;
        }, machineryId);
        return rows.stream().findFirst();
    }

    public void enrichPendingRow(Map<String, Object> row) {
        Long machineryId = longObj(row.get("machineryId"));
        findStationContextByMachinery(machineryId).ifPresent(ctx -> row.putAll(ctx));
    }

    private Optional<Map<String, Object>> findAnyMachineryForEquipmentAndon(Long stationId) {
        return findPrimaryMachineryByStation(stationId);
    }

    private void pauseDispatchForMachinery(Long machineryId) {
        jdbc.update("""
                UPDATE dispatch_task dt
                JOIN md_workstation_machine wsm ON wsm.workstation_id = dt.station_id
                SET dt.status = 'PAUSED', dt.updated_at = NOW(3)
                WHERE wsm.machinery_id = ? AND dt.status IN ('RUNNING','DISPATCHED')
                """, machineryId);
    }

    private void resumeDispatchForMachinery(Long machineryId) {
        List<Long> stationIds = jdbc.query("""
                SELECT DISTINCT wsm.workstation_id FROM md_workstation_machine wsm WHERE wsm.machinery_id = ?
                """, (rs, n) -> rs.getLong(1), machineryId);
        for (Long stationId : stationIds) {
            if (stationHasFaultMachinery(stationId)) {
                continue;
            }
            jdbc.update("""
                    UPDATE dispatch_task SET status = 'RUNNING', updated_at = NOW(3)
                    WHERE station_id = ? AND status = 'PAUSED'
                    """, stationId);
        }
    }

    private boolean stationHasFaultMachinery(Long stationId) {
        Long c = jdbc.queryForObject("""
                SELECT COUNT(*) FROM md_workstation_machine wsm
                JOIN dv_machinery m ON m.machinery_id = wsm.machinery_id
                WHERE wsm.workstation_id = ? AND m.status IN ('REPAIR','STOP')
                """, Long.class, stationId);
        return c != null && c > 0;
    }

    private void closeLinkedAndonIfReady(Long repairId) {
        Optional<Map<String, Object>> repair = jdbc.query("""
                SELECT source_doc_type, source_doc_id FROM dv_repair WHERE repair_id = ?
                """, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sourceDocType", rs.getString("source_doc_type"));
            m.put("sourceDocId", rs.getObject("source_doc_id"));
            return Optional.of(m);
        }, repairId);

        Long recordId = repair
                .filter(r -> "ANDON".equals(str(r.get("sourceDocType"))))
                .map(r -> longObj(r.get("sourceDocId")))
                .orElse(null);
        if (recordId == null) {
            List<Long> ids = jdbc.query(
                    "SELECT record_id FROM pro_andon_record WHERE attr2 = ? LIMIT 1",
                    (rs, n) -> rs.getLong(1),
                    String.valueOf(repairId));
            recordId = ids.isEmpty() ? null : ids.get(0);
        }
        if (recordId == null) {
            return;
        }
        Long finalRecordId = recordId;
        andonRecordRepo.findById(finalRecordId).ifPresent(rec -> {
            if ("HANDLED".equals(str(rec.get("status")))) {
                return;
            }
            rec.put("status", "HANDLED");
            rec.put("handleTime", LocalDateTime.now().toString().replace("T", " ").substring(0, 19));
            andonRecordRepo.update(rec);
            andonBridge.afterRecordUpdated(rec);
        });
    }

    private Long insertRepair(Map<String, Object> body) {
        org.springframework.jdbc.support.GeneratedKeyHolder kh = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("""
                    INSERT INTO dv_repair (
                      repair_code, repair_name,
                      machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec, machinery_type_id,
                      require_date, source_doc_type, source_doc_id, source_doc_code, status, remark, create_by, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(3))
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, str(body.get("repairCode")));
            ps.setString(i++, str(body.get("repairName")));
            ps.setObject(i++, body.get("machineryId"));
            ps.setString(i++, str(body.get("machineryCode")));
            ps.setString(i++, str(body.get("machineryName")));
            ps.setString(i++, str(body.get("machineryBrand")));
            ps.setString(i++, str(body.get("machinerySpec")));
            ps.setObject(i++, body.get("machineryTypeId"));
            ps.setTimestamp(i++, body.get("requireDate") instanceof Timestamp ts ? ts : Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(i++, str(body.get("sourceDocType")));
            ps.setObject(i++, body.get("sourceDocId"));
            ps.setString(i++, str(body.get("sourceDocCode")));
            ps.setString(i++, strOr(body.get("status"), "PREPARE"));
            ps.setString(i++, str(body.get("remark")));
            ps.setString(i++, str(body.get("createBy")));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    private boolean repairCodeExists(String code) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM dv_repair WHERE repair_code = ?", Long.class, code);
        return c != null && c > 0;
    }

    private Long existingRepairId(Map<String, Object> record) {
        Object attr2 = record.get("attr2");
        if (attr2 == null || String.valueOf(attr2).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(attr2));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isEquipmentAndon(String reason) {
        if (!StringUtils.hasText(reason)) {
            return false;
        }
        String t = reason.trim();
        return t.contains("设备") || t.contains("噪音") || t.contains("故障") || t.contains("老化")
                || t.contains("机") && (t.contains("异常") || t.contains("维修") || t.contains("停"));
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static String strOr(Object v, String def) {
        String s = str(v);
        return (s == null || s.isBlank()) ? def : s;
    }

    private static Long longObj(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }
}
